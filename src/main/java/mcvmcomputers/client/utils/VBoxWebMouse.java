package mcvmcomputers.client.utils;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;

/**
 * Mouse input for the VM via the VirtualBox web service (SOAP).
 *
 * <p>Background: starting with VirtualBox 7.2 the {@code VBoxManage controlvm
 * mouseputmevent} CLI sub-command was removed, so the mouse could no longer be
 * driven from the command line (the keyboard {@code keyboardputscancode} command
 * still exists, which is why typing worked but the cursor did not). The
 * {@code IMouse} interface is still available through the VirtualBox web service
 * ({@code VBoxWebSrv}), which ships with every VirtualBox installation. This class
 * talks to that web service over plain SOAP using only JDK built-ins
 * ({@link java.net.http.HttpClient}) - no extra dependencies and no RDP - so it
 * works on the latest VirtualBox without the user installing anything.</p>
 *
 * <p>All methods are best-effort: any failure leaves the mouse inoperative but
 * never throws into the VM update loop.</p>
 */
public class VBoxWebMouse {

    private static final String NS = "http://www.virtualbox.org/";
    private static final String HOST = "localhost";
    private static final int PORT = 18083;
    private static final String URL = "http://" + HOST + ":" + PORT + "/";

    private final String vboxDirectory;
    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private Process webSrvProcess;

    // Managed object references for the current session.
    private volatile String vboxRef;
    private volatile String sessionRef;
    private volatile String mouseRef;
    private volatile boolean connected;

    public VBoxWebMouse(String vboxDirectory) {
        this.vboxDirectory = vboxDirectory;
        // Ensure the session is released and the helper process is stopped if the
        // game exits, otherwise a lingering Shared lock blocks the next "modifyvm"
        // (Write lock) on startup with "machine is already locked for a session".
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "VBoxWebMouse-cleanup"));
    }

    /**
     * Sends a relative mouse event to the VM. Lazily (re)connects to the web
     * service as needed. Best-effort: failures are swallowed and trigger a
     * reconnect on the next call.
     *
     * @param dx          relative X movement
     * @param dy          relative Y movement
     * @param dz          vertical scroll delta
     * @param buttonState bit mask of pressed buttons (1=left, 2=right, 4=middle)
     */
    public synchronized void putMouseEvent(int dx, int dy, int dz, int buttonState) {
        if (!connected) {
            if (!connect()) {
                return;
            }
        }
        try {
            String r = soap("<vbox:IMouse_putMouseEvent><_this>" + mouseRef + "</_this>"
                    + "<dx>" + dx + "</dx><dy>" + dy + "</dy><dz>" + dz + "</dz>"
                    + "<dw>0</dw><buttonState>" + buttonState + "</buttonState>"
                    + "</vbox:IMouse_putMouseEvent>");
            if (isFault(r)) {
                // Session likely went stale (VM restarted, etc.) - drop and retry next time.
                connected = false;
            }
        } catch (Exception e) {
            connected = false;
        }
    }

    /**
     * Establishes a shared-lock session against the running VM and resolves the
     * mouse object. Starts the web service first if necessary.
     *
     * @return true if the mouse object was resolved successfully
     */
    private boolean connect() {
        try {
            if (!ensureWebServiceRunning()) {
                return false;
            }

            // 1. logon -> IVirtualBox
            String r = soap("<vbox:IWebsessionManager_logon><username></username>"
                    + "<password></password></vbox:IWebsessionManager_logon>");
            vboxRef = extract(r, "returnval");
            if (vboxRef == null) return false;

            // 2. session object
            r = soap("<vbox:IWebsessionManager_getSessionObject><refIVirtualBox>"
                    + vboxRef + "</refIVirtualBox></vbox:IWebsessionManager_getSessionObject>");
            sessionRef = extract(r, "returnval");
            if (sessionRef == null) return false;

            // 3. find the VM
            r = soap("<vbox:IVirtualBox_findMachine><_this>" + vboxRef + "</_this>"
                    + "<nameOrId>VmComputersVm</nameOrId></vbox:IVirtualBox_findMachine>");
            String machineRef = extract(r, "returnval");
            if (machineRef == null) return false;

            // 4. attach to the already-running VM with a shared lock
            r = soap("<vbox:IMachine_lockMachine><_this>" + machineRef + "</_this>"
                    + "<session>" + sessionRef + "</session><lockType>Shared</lockType>"
                    + "</vbox:IMachine_lockMachine>");
            if (isFault(r)) return false;

            // 5. console
            r = soap("<vbox:ISession_getConsole><_this>" + sessionRef + "</_this>"
                    + "</vbox:ISession_getConsole>");
            String consoleRef = extract(r, "returnval");
            if (consoleRef == null) return false;

            // 6. mouse
            r = soap("<vbox:IConsole_getMouse><_this>" + consoleRef + "</_this>"
                    + "</vbox:IConsole_getMouse>");
            mouseRef = extract(r, "returnval");
            if (mouseRef == null) return false;

            connected = true;
            return true;
        } catch (Exception e) {
            connected = false;
            return false;
        }
    }

    /**
     * Releases the session and shuts down the web service. Safe to call multiple
     * times.
     */
    public synchronized void disconnect() {
        try {
            if (sessionRef != null) {
                soap("<vbox:ISession_unlockMachine><_this>" + sessionRef + "</_this>"
                        + "</vbox:ISession_unlockMachine>");
            }
            if (vboxRef != null) {
                soap("<vbox:IWebsessionManager_logoff><refIVirtualBox>" + vboxRef
                        + "</refIVirtualBox></vbox:IWebsessionManager_logoff>");
            }
        } catch (Exception ignored) {
        } finally {
            connected = false;
            vboxRef = null;
            sessionRef = null;
            mouseRef = null;
        }
    }

    /** Stops the web service process if this instance started it. */
    public synchronized void shutdown() {
        disconnect();
        if (webSrvProcess != null) {
            try {
                webSrvProcess.destroy();
            } catch (Exception ignored) {
            }
            webSrvProcess = null;
        }
    }

    /**
     * Ensures {@code VBoxWebSrv} is reachable, starting it if needed. Retries the
     * logon a few times to give the freshly-started service time to bind.
     */
    private boolean ensureWebServiceRunning() {
        if (pingLogon()) {
            return true;
        }
        startWebService();
        for (int i = 0; i < 20; i++) {
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                return false;
            }
            if (pingLogon()) {
                return true;
            }
        }
        return false;
    }

    /** Quick connectivity check: a successful logon round-trip. */
    private boolean pingLogon() {
        try {
            String r = soap("<vbox:IWebsessionManager_logon><username></username>"
                    + "<password></password></vbox:IWebsessionManager_logon>");
            String ref = extract(r, "returnval");
            if (ref != null) {
                // Don't leak this probe session.
                soap("<vbox:IWebsessionManager_logoff><refIVirtualBox>" + ref
                        + "</refIVirtualBox></vbox:IWebsessionManager_logoff>");
                return true;
            }
        } catch (Exception ignored) {
        }
        return false;
    }

    private void startWebService() {
        try {
            String exe;
            if (SystemUtils.IS_OS_WINDOWS) {
                exe = vboxDirectory + "\\VBoxWebSrv.exe";
            } else if (SystemUtils.IS_OS_MAC) {
                exe = vboxDirectory + "/vboxwebsrv";
            } else {
                exe = "vboxwebsrv";
            }
            // On non-Windows the binary may not be at vboxDirectory; fall back to PATH.
            if (!SystemUtils.IS_OS_LINUX && !new File(exe).exists()) {
                return;
            }
            ProcessBuilder pb = new ProcessBuilder(exe,
                    "--host", HOST,
                    "--port", String.valueOf(PORT),
                    "--authentication", "null");
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            webSrvProcess = pb.start();
        } catch (Exception e) {
            webSrvProcess = null;
        }
    }

    // ---------------------------------------------------------------------
    // SOAP plumbing
    // ---------------------------------------------------------------------

    private String soap(String body) throws Exception {
        String env = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" "
                + "xmlns:vbox=\"" + NS + "\"><SOAP-ENV:Body>" + body
                + "</SOAP-ENV:Body></SOAP-ENV:Envelope>";
        HttpRequest req = HttpRequest.newBuilder(URI.create(URL))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "")
                .POST(HttpRequest.BodyPublishers.ofString(env))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        return resp.body();
    }

    private static final Pattern RETURN_PATTERN = Pattern.compile(
            "<(?:[\\w-]+:)?returnval[^>]*>(.*?)</(?:[\\w-]+:)?returnval>", Pattern.DOTALL);

    private static String extract(String xml, String tag) {
        if (xml == null) return null;
        Matcher m = RETURN_PATTERN.matcher(xml);
        return m.find() ? m.group(1).trim() : null;
    }

    private static boolean isFault(String xml) {
        return xml == null || xml.contains("Fault") || xml.contains("fault>");
    }
}

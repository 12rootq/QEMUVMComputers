package mcvmcomputers.client.utils;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
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
    private static final String HOST = "127.0.0.1";
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
    private volatile String keyboardRef;
    private volatile String displayRef;
    private volatile String consoleRef;
    private volatile boolean connected;
    private volatile boolean webServiceFailed;
    private boolean wasConnectedOnce;

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
        if (webServiceFailed) return;
        if (!connected) {
            if (!connect()) return;
        }
        try {
            String r = soap("<vbox:IMouse_putMouseEvent><_this>" + mouseRef + "</_this>"
                    + "<dx>" + dx + "</dx><dy>" + dy + "</dy><dz>" + dz + "</dz>"
                    + "<dw>0</dw><buttonState>" + buttonState + "</buttonState>"
                    + "</vbox:IMouse_putMouseEvent>");
            if (isFault(r)) {
                connected = false;
            }
        } catch (Exception e) {
            connected = false;
        }
    }

    /**
     * Sends keyboard scancodes via web service.
     * Much faster than spawning VBoxManage.exe CLI for each batch.
     */
    public synchronized void putScancodes(List<Integer> scancodes) {
        if (scancodes == null || scancodes.isEmpty()) return;
        if (webServiceFailed) return;
        if (!connected) {
            if (!connect()) return;
        }
        try {
            StringBuilder hex = new StringBuilder();
            for (int sc : scancodes) {
                if (hex.length() > 0) hex.append(" ");
                hex.append(String.format("%02x", sc));
            }
            String r = soap("<vbox:IKeyboard_putScancodes><_this>" + keyboardRef + "</_this>"
                    + "<scancodes>" + hex.toString() + "</scancodes>"
                    + "<codesStored>" + scancodes.size() + "</codesStored>"
                    + "</vbox:IKeyboard_putScancodes>");
            if (isFault(r)) {
                System.err.println("[VM Computers] VBoxWebMouse: putScancodes fault, reconnecting");
                connected = false;
            }
        } catch (Exception e) {
            System.err.println("[VM Computers] VBoxWebMouse: putScancodes error: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Takes a PNG screenshot via web service. Much faster than CLI.
     * width/height=0 means current VM resolution.
     *
     * @return PNG bytes or null on failure
     */
    public synchronized byte[] takeScreenshotPNG() {
        if (webServiceFailed) return null;
        if (!connected) {
            if (!connect()) return null;
        }
        try {
            String r = soap("<vbox:IDisplay_takeScreenShotPNGToArray><_this>" + displayRef + "</_this>"
                    + "<width>0</width><height>0</height><screenId>0</screenId>"
                    + "</vbox:IDisplay_takeScreenShotPNGToArray>");
            if (isFault(r)) {
                return null;
            }
            String b64 = extract(r, "returnval");
            if (b64 == null || b64.isEmpty()) return null;
            return Base64.getDecoder().decode(b64);
        } catch (Exception e) {
            return null;
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
                System.err.println("[VM Computers] VBoxWebMouse: web service not running");
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
            consoleRef = extract(r, "returnval");
            if (consoleRef == null) return false;

            // 6. mouse
            r = soap("<vbox:IConsole_getMouse><_this>" + consoleRef + "</_this>"
                    + "</vbox:IConsole_getMouse>");
            mouseRef = extract(r, "returnval");
            if (mouseRef == null) return false;

            // 7. keyboard
            r = soap("<vbox:IConsole_getKeyboard><_this>" + consoleRef + "</_this>"
                    + "</vbox:IConsole_getKeyboard>");
            keyboardRef = extract(r, "returnval");
            if (keyboardRef == null) return false;

            // 8. display
            r = soap("<vbox:IConsole_getDisplay><_this>" + consoleRef + "</_this>"
                    + "</vbox:IConsole_getDisplay>");
            displayRef = extract(r, "returnval");
            if (displayRef == null) return false;

            connected = true;
            if (!wasConnectedOnce) {
                wasConnectedOnce = true;
                System.out.println("[VM Computers] VBoxWebMouse connected OK");
            }
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
            keyboardRef = null;
            displayRef = null;
            consoleRef = null;
        }
    }

    /**
     * Resets the connection state so that the next mouse/keyboard/screenshot call
     * will attempt a fresh connection to the web service. Call this when the VM is
     * being (re)started to clear any previous failure state.
     */
    public synchronized void resetConnectionState() {
        disconnect();
        webServiceFailed = false;
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
        if (webServiceFailed) return false;
        if (pingLogon()) return true;
        startWebService();
        for (int i = 0; i < 40; i++) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                return false;
            }
            if (i == 4 && webSrvProcess != null && !webSrvProcess.isAlive()) {
                System.err.println("[VM Computers] VBoxWebSrv.exe died immediately, check log above");
            }
            if (pingLogon()) {
                System.out.println("[VM Computers] Web service ready after " + (i * 250) + "ms");
                return true;
            }
        }
        webServiceFailed = true;
        System.err.println("[VM Computers] VBoxWebMouse: giving up - VBoxWebSrv didn't respond in 10s");
        return false;
    }

    /** Quick connectivity check: a successful logon round-trip. */
    private boolean pingLogon() {
        try {
            String r = soap("<vbox:IWebsessionManager_logon><username></username>"
                    + "<password></password></vbox:IWebsessionManager_logon>");
            String ref = extract(r, "returnval");
            if (ref != null) {
                soap("<vbox:IWebsessionManager_logoff><refIVirtualBox>" + ref
                        + "</refIVirtualBox></vbox:IWebsessionManager_logoff>");
                return true;
            }
            System.err.println("[VM Computers] pingLogon: no returnval in response. First 300 chars: " + r.substring(0, Math.min(300, r.length())));
        } catch (Exception e) {
            System.err.println("[VM Computers] pingLogon SOAP error: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
        return false;
    }

    private void startWebService() {
        try {
            String exe = findVBoxWebSrv();
            if (exe == null) {
                System.err.println("[VM Computers] VBoxWebSrv.exe not found (looked in: " + vboxDirectory + ", C:\\Program Files\\Oracle\\VirtualBox)");
                return;
            }
            System.out.println("[VM Computers] Starting web service: " + exe);
            ProcessBuilder pb = new ProcessBuilder(exe,
                    "--host", "127.0.0.1",
                    "--port", String.valueOf(PORT),
                    "-A", "null");
            pb.redirectErrorStream(true);
            File logFile = new File(System.getProperty("java.io.tmpdir"), "vboxwebsrv.log");
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile));
            webSrvProcess = pb.start();
        } catch (Exception e) {
            System.err.println("[VM Computers] Failed to start VBoxWebSrv: " + e.getMessage());
            webSrvProcess = null;
        }
    }

    private String findVBoxWebSrv() {
        String[] candidates;
        if (SystemUtils.IS_OS_WINDOWS) {
            candidates = new String[] {
                vboxDirectory + "\\VBoxWebSrv.exe",
                vboxDirectory + "VBoxWebSrv.exe",
                "C:\\Program Files\\Oracle\\VirtualBox\\VBoxWebSrv.exe",
                vboxDirectory + "\\vboxwebsrv.exe",
            };
        } else if (SystemUtils.IS_OS_MAC) {
            candidates = new String[] { vboxDirectory + "/vboxwebsrv" };
        } else {
            candidates = new String[] { "vboxwebsrv" };
        }
        for (String path : candidates) {
            if (new File(path).exists()) return path;
        }
        return SystemUtils.IS_OS_LINUX ? "vboxwebsrv" : null;
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

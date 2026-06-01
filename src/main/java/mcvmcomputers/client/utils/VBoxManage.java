package mcvmcomputers.client.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.commons.lang3.SystemUtils;


public class VBoxManage {
    private final String vboxmanageCmd;
    private final File screenshotFile;
    private String vboxDirectory;


    private int cachedHostCpuCount = -1;
    private String cachedVersion = null;

    // Mouse input is delivered through the VirtualBox web service because the
    // CLI "controlvm mouseputmevent" sub-command was removed in VirtualBox 7.2.
    private final VBoxWebMouse webMouse;

    public VBoxManage(String vboxDirectory) {
        this.vboxDirectory = vboxDirectory;
        this.webMouse = new VBoxWebMouse(vboxDirectory);
        if (SystemUtils.IS_OS_WINDOWS) {
            vboxmanageCmd = vboxDirectory + "\\VBoxManage.exe";
        } else if (SystemUtils.IS_OS_MAC) {
            vboxmanageCmd = vboxDirectory + "/VBoxManage";
        } else {

            vboxmanageCmd = "VBoxManage";
        }

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "mcvmcomputers");
        tempDir.mkdirs();
        screenshotFile = new File(tempDir, "screenshot.png");
    }

    public String getVBoxManagePath() {
        return vboxmanageCmd;
    }

    public String getVboxDirectory() {
        return vboxDirectory;
    }


    private List<String> execute(String... args) throws Exception {
        String[] cmd = new String[args.length + 1];
        cmd[0] = vboxmanageCmd;
        System.arraycopy(args, 0, cmd, 1, args.length);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process proc = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        int exitCode = proc.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("VBoxManage error (exit " + exitCode + "): " + String.join("\n", lines));
        }

        return lines;
    }


    private List<String> executeSilent(String... args) {
        try {
            return execute(args);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }


    public boolean vmExists(String name) {
        try {
            List<String> lines = execute("showvminfo", name, "--machinereadable");
            return !lines.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public String getVmState(String name) {
        try {
            List<String> lines = execute("showvminfo", name, "--machinereadable");
            for (String line : lines) {
                if (line.startsWith("VMState=")) {
                    return line.substring("VMState=".length()).replace("\"", "").trim();
                }
            }
        } catch (Exception e) {}
        return "unknown";
    }

    public void startVm(String name) throws Exception {
        execute("startvm", name, "--type", "headless");
    }

    public void powerOffVm(String name) {
        // Drop the web-service mouse session before the VM stops so a stale
        // session can't block the next start.
        webMouse.disconnect();
        executeSilent("controlvm", name, "poweroff");

        try { Thread.sleep(500); } catch (InterruptedException e) {}
    }

    public void discardSavedState(String name) {
        executeSilent("discardstate", name);
    }

    public void createVm(String name, String osType) throws Exception {
        execute("createvm", "--name", name, "--ostype", osType, "--register");
    }


    public void modifyVm(String name, String... options) throws Exception {
        String[] cmd = new String[options.length + 2];
        cmd[0] = "modifyvm";
        cmd[1] = name;
        System.arraycopy(options, 0, cmd, 2, options.length);
        execute(cmd);
    }

    /**
     * Returns true when {@code message} is non-null and contains the VirtualBox
     * error markers that indicate the host graphics controller does not support
     * 3D acceleration ({@code VBOX_E_NOT_SUPPORTED} or
     * {@code "does not support the given feature"}).
     *
     * This is a pure, side-effect-free helper intended for unit and property testing.
     */
    public static boolean isAccelerate3dUnsupportedError(String message) {
        if (message == null) return false;
        return message.contains("VBOX_E_NOT_SUPPORTED")
                || message.contains("does not support the given feature");
    }

    /**
     * Calls {@link #modifyVm(String, String...)} with the supplied options (which
     * must end with {@code "--accelerate3d", "on"}).  If the call throws and the
     * exception message indicates that the host graphics controller does not support
     * 3D acceleration, the trailing {@code "on"} value is swapped to {@code "off"}
     * and {@code modifyVm} is retried exactly once.  If the retry succeeds the
     * method returns normally.  If the original exception is NOT the 3D-unsupported
     * error it is re-thrown unchanged so the caller's existing error handler runs.
     *
     * @param name                    VM name passed to {@code modifyvm}
     * @param optionsEndingWith3dAccel options array whose last two elements are
     *                                {@code "--accelerate3d"} and {@code "on"}
     * @throws Exception the original exception when it is not a 3D-unsupported error,
     *                   or any exception thrown by the fallback {@code modifyVm} call
     */
    public void modifyVmWith3dAccelFallback(String name, String... optionsEndingWith3dAccel) throws Exception {
        try {
            modifyVm(name, optionsEndingWith3dAccel);
        } catch (Exception ex) {
            if (!isAccelerate3dUnsupportedError(ex.getMessage())) {
                throw ex;
            }
            // Swap the trailing "--accelerate3d" value from "on" to "off" and retry once.
            String[] fallbackOptions = optionsEndingWith3dAccel.clone();
            fallbackOptions[fallbackOptions.length - 1] = "off";
            modifyVm(name, fallbackOptions);
        }
    }


    public void modifyVmSilent(String name, String... options) {
        try {
            modifyVm(name, options);
        } catch (Exception e) {

        }
    }


    public void addStorageController(String vmName, String controllerName, String bus) throws Exception {
        execute("storagectl", vmName, "--name", controllerName, "--add", bus);
    }

    public void removeStorageController(String vmName, String controllerName) {
        executeSilent("storagectl", vmName, "--name", controllerName, "--remove");
    }

    public void storageAttach(String vmName, String controller, int port, int device, String type, String medium) throws Exception {
        execute("storageattach", vmName,
                "--storagectl", controller,
                "--port", String.valueOf(port),
                "--device", String.valueOf(device),
                "--type", type,
                "--medium", medium);
    }

    public void storageAttachSilent(String vmName, String controller, int port, int device, String type, String medium) {
        try {
            storageAttach(vmName, controller, port, device, type, medium);
        } catch (Exception e) {}
    }


    public void mountMedium(String vmName, String controller, int port, int device, String mediumPath) {
        executeSilent("controlvm", vmName, "mountmedium", controller,
                String.valueOf(port), String.valueOf(device), mediumPath);
    }


    public void unmountMedium(String vmName, String controller, int port, int device) {
        executeSilent("controlvm", vmName, "unmountmedium", controller,
                String.valueOf(port), String.valueOf(device));
    }


    public void createHardDisk(String path, long sizeMB, String format) throws Exception {
        execute("createhd", "--filename", path, "--size", String.valueOf(sizeMB), "--format", format);
    }


    public int getHostProcessorCount() {
        if (cachedHostCpuCount > 0) return cachedHostCpuCount;
        try {
            List<String> lines = execute("list", "hostinfo");
            for (String line : lines) {
                if (line.startsWith("Host number of online CPUs:") || line.startsWith("Processor count:")) {
                    String[] parts = line.split(":\\s*");
                    if (parts.length >= 2) {
                        cachedHostCpuCount = Integer.parseInt(parts[1].trim());
                        return cachedHostCpuCount;
                    }
                }
            }
        } catch (Exception e) {}
        cachedHostCpuCount = Runtime.getRuntime().availableProcessors();
        return cachedHostCpuCount;
    }

    public String getVersion() {
        if (cachedVersion != null) return cachedVersion;
        try {
            List<String> lines = execute("--version");
            if (!lines.isEmpty()) {

                cachedVersion = lines.get(0).trim();
                return cachedVersion;
            }
        } catch (Exception e) {}
        return "unknown";
    }

    public String getDefaultAdditionsISO() {
        try {
            List<String> lines = execute("list", "systemproperties");
            for (String line : lines) {
                if (line.startsWith("Default Additions ISO:") || line.contains("Additions ISO")) {
                    String[] parts = line.split(":\\s*");
                    if (parts.length >= 2) {
                        return parts[1].trim();
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }


    public void putScancodes(String vmName, List<Integer> scancodes) {
        if (scancodes == null || scancodes.isEmpty()) return;
        String[] args = new String[3 + scancodes.size()];
        args[0] = "controlvm";
        args[1] = vmName;
        args[2] = "keyboardputscancode";
        for (int i = 0; i < scancodes.size(); i++) {
            args[3 + i] = String.format("%02x", scancodes.get(i));
        }
        executeSilent(args);
    }


    public void putMouseEvent(String vmName, int dx, int dy, int dz, int buttons) {
        // VirtualBox 7.2 removed the "controlvm mouseputmevent" CLI command, so the
        // mouse is driven through the web service instead (see VBoxWebMouse).
        webMouse.putMouseEvent(dx, dy, dz, buttons);
    }

    /** Releases the web-service mouse session (called when the VM powers off). */
    public void releaseMouse() {
        webMouse.disconnect();
    }

    /** Stops the web-service mouse session and its helper process. */
    public void shutdownMouse() {
        webMouse.shutdown();
    }


    public byte[] takeScreenshot(String vmName) {
        try {
            execute("controlvm", vmName, "screenshotpng", screenshotFile.getAbsolutePath());
            if (screenshotFile.exists()) {
                byte[] data = Files.readAllBytes(screenshotFile.toPath());
                return data;
            }
        } catch (Exception e) {}
        return null;
    }


    public boolean isMediumEjected(String vmName, String controller, int port, int device) {
        try {
            List<String> lines = execute("showvminfo", vmName, "--machinereadable");

            String key = "\"" + controller + "-" + port + "-" + device + "\"";
            for (String line : lines) {
                if (line.startsWith(key + "=")) {
                    String value = line.substring(key.length() + 1).replace("\"", "").trim();
                    return value.isEmpty() || value.equals("none") || value.equals("emptydrive") || value.equals("empty");
                }
            }
        } catch (Exception e) {}
        return false;
    }


    public String getMediumPath(String vmName, String controller, int port, int device) {
        try {
            List<String> lines = execute("showvminfo", vmName, "--machinereadable");
            String key = "\"" + controller + "-" + port + "-" + device + "\"";
            for (String line : lines) {
                if (line.startsWith(key + "=")) {
                    String value = line.substring(key.length() + 1).replace("\"", "").trim();
                    if (!value.isEmpty() && !value.equals("none") && !value.equals("emptydrive") && !value.equals("empty")) {
                        return value;
                    }
                }
            }
        } catch (Exception e) {}
        return null;
    }


    public String testConnection() {
        try {
            return getVersion();
        } catch (Exception e) {
            return null;
        }
    }


    public void setWebsrvAuthNull() {
        executeSilent("setproperty", "websrvauthlibrary", "null");
    }
}

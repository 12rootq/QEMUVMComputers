package newvmcomputers.provider;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class VMwareProvider implements IVMProvider {
    private final String VMRUN_PATH = "vmrun";
    @Override
    public boolean isInstalled() {
        try {
            ProcessBuilder pb = new ProcessBuilder(VMRUN_PATH, "-v");
            Process process = pb.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            System.out.println("[VMwareProvider] VMware не найдена или не установлен VIX API / Workstation.");
            return false;
        }
    }

    @Override
    public List<String> getAvailableVMs() {
        List<String> vms = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(VMRUN_PATH, "list");
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith(".vmx")) {
                    vms.add(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return vms;
    }

    @Override
    public boolean startVM(String vmId) {
        try {

            ProcessBuilder pb = new ProcessBuilder(VMRUN_PATH, "-T", "ws", "start", vmId, "nogui");
            Process process = pb.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean stopVM(String vmId) {
        try {
            ProcessBuilder pb = new ProcessBuilder(VMRUN_PATH, "-T", "ws", "stop", vmId, "soft");
            Process process = pb.start();
            process.waitFor();
            return process.exitValue() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int getDisplayPort(String vmId) {
        return 5900;
    }
}
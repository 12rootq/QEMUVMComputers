package newvmcomputers.provider; // замени на свой пакет

import java.util.List;

public interface IVMProvider {
    boolean isInstalled();
    List<String> getAvailableVMs();
    boolean startVM(String vmId);
    boolean stopVM(String vmId);
    int getDisplayPort(String vmId);
}
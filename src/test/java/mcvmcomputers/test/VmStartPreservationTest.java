package mcvmcomputers.test;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Preservation Property Tests - Task 2
 *
 * Property 2: Preservation - Supported Hosts and Unrelated Failures Unchanged
 *
 * These tests capture the BASELINE behavior of the UNFIXED code for all inputs
 * where isBugCondition returns false. They MUST PASS on unfixed code.
 *
 * After the fix (Task 3.1), these same tests must still pass (no regressions).
 *
 * Validates: Requirements 3.1, 3.2, 3.3
 */
public class VmStartPreservationTest {

    // -------------------------------------------------------------------------
    // Re-use the same FakeVBoxManage and runUnfixedVmStartSequence from Task 1
    // (copied here so this file is self-contained and can run independently)
    // -------------------------------------------------------------------------

    static boolean isVBoxE_NotSupported(String message) {
        if (message == null) return false;
        return message.contains("VBOX_E_NOT_SUPPORTED")
                || message.contains("does not support the given feature");
    }

    static boolean isBugCondition(boolean requested3dOn, Exception modifyVmError) {
        return requested3dOn
                && modifyVmError != null
                && isVBoxE_NotSupported(modifyVmError.getMessage());
    }

    static class FakeVBoxManage {
        final boolean vmExists;
        final boolean fail3dOn;
        final String failureMessage;
        // For unrelated-failure simulation
        final boolean failStartVm;
        final String startVmFailureMessage;
        final boolean failModifyVmUnrelated;
        final String modifyVmUnrelatedMessage;

        final List<String[]> modifyVmCalls = new ArrayList<>();
        final List<String[]> modifyVmSilentCalls = new ArrayList<>();
        boolean startVmCalled = false;
        boolean createVmCalled = false;
        int hostCpuCount = 4;

        FakeVBoxManage(boolean vmExists, boolean fail3dOn, String failureMessage,
                       boolean failStartVm, String startVmFailureMessage,
                       boolean failModifyVmUnrelated, String modifyVmUnrelatedMessage) {
            this.vmExists = vmExists;
            this.fail3dOn = fail3dOn;
            this.failureMessage = failureMessage;
            this.failStartVm = failStartVm;
            this.startVmFailureMessage = startVmFailureMessage;
            this.failModifyVmUnrelated = failModifyVmUnrelated;
            this.modifyVmUnrelatedMessage = modifyVmUnrelatedMessage;
        }

        boolean vmExists(String name) { return vmExists; }
        String getVmState(String name) { return "poweroff"; }
        int getHostProcessorCount() { return hostCpuCount; }

        void createVm(String name, String osType) throws Exception {
            createVmCalled = true;
        }

        void modifyVm(String name, String... options) throws Exception {
            modifyVmCalls.add(options.clone());
            // Check for --accelerate3d on failure
            for (int i = 0; i < options.length - 1; i++) {
                if ("--accelerate3d".equals(options[i]) && "on".equals(options[i + 1])) {
                    if (fail3dOn) {
                        throw new RuntimeException(failureMessage);
                    }
                }
            }
            // Check for unrelated modifyVm failure (only on first call, before 3d check)
            if (failModifyVmUnrelated && !fail3dOn) {
                throw new RuntimeException(modifyVmUnrelatedMessage);
            }
        }

        void modifyVmSilent(String name, String... options) {
            modifyVmSilentCalls.add(options.clone());
        }

        void removeStorageController(String vmName, String controllerName) { }
        void addStorageController(String vmName, String controllerName, String bus) throws Exception { }
        void storageAttachSilent(String vmName, String controller, int port, int device,
                                 String type, String medium) { }
        void powerOffVm(String name) { }
        void discardSavedState(String name) { }

        void startVm(String name) throws Exception {
            startVmCalled = true;
            if (failStartVm) {
                throw new RuntimeException(startVmFailureMessage);
            }
        }
    }

    static class VmStartResult {
        boolean vmStarted;
        boolean failedToStartMessageShown;
        String failedToStartError;
        List<String[]> modifyVmCalls;
        List<String[]> modifyVmSilentCalls;
        boolean retriedWith3dOff;
        boolean has3dOnCall;
    }

    /**
     * Runs the UNFIXED VM-start sequence (mirrors GuiPCEditing.turnOnPC worker thread).
     * Identical to the one in VmStartSequenceTest - no fallback for 3D errors.
     */
    static VmStartResult runUnfixedVmStartSequence(
            FakeVBoxManage vbox,
            long ramMB,
            int cpuDividedBy,
            int videoMem,
            boolean is64Bit,
            String hardDriveFileName,
            String isoFileName
    ) {
        VmStartResult result = new VmStartResult();
        result.vmStarted = false;
        result.failedToStartMessageShown = false;
        result.failedToStartError = null;

        try {
            boolean vmExists = vbox.vmExists("VmComputersVm");

            if (vmExists) {
                String state = vbox.getVmState("VmComputersVm");
                if ("running".equals(state) || "firstonline".equals(state) || "starting".equals(state)) {
                    vbox.powerOffVm("VmComputersVm");
                }
                if ("saved".equals(state)) {
                    vbox.discardSavedState("VmComputersVm");
                }

                String OSType = "Other";
                if (is64Bit) OSType += "_64";
                int cpuCount = Math.max(1, vbox.getHostProcessorCount() / cpuDividedBy);

                vbox.modifyVm("VmComputersVm",
                        "--ostype", OSType,
                        "--memory", String.valueOf(ramMB),
                        "--cpus", String.valueOf(cpuCount),
                        "--vram", String.valueOf(videoMem),
                        "--accelerate3d", "on");

                vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");
                vbox.removeStorageController("VmComputersVm", "IDE Controller");
                vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

                if (!hardDriveFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd",
                            "/fake/" + hardDriveFileName);
                }
                if (isoFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0,
                            "dvddrive", "emptydrive");
                }

            } else {
                String OSType = "Other";
                if (is64Bit) OSType += "_64";
                vbox.createVm("VmComputersVm", OSType);

                int cpuCount = Math.max(1, vbox.getHostProcessorCount() / cpuDividedBy);

                vbox.modifyVm("VmComputersVm",
                        "--memory", String.valueOf(ramMB),
                        "--cpus", String.valueOf(cpuCount),
                        "--vram", String.valueOf(videoMem),
                        "--accelerate3d", "on");

                vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");
                vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

                if (!hardDriveFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd",
                            "/fake/" + hardDriveFileName);
                }
                if (isoFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0,
                            "dvddrive", "emptydrive");
                }
            }

            vbox.startVm("VmComputersVm");
            result.vmStarted = true;

        } catch (Exception ex) {
            result.failedToStartMessageShown = true;
            result.failedToStartError = ex.getMessage();
        }

        result.modifyVmCalls = vbox.modifyVmCalls;
        result.modifyVmSilentCalls = vbox.modifyVmSilentCalls;

        result.retriedWith3dOff = vbox.modifyVmCalls.stream().anyMatch(opts -> {
            for (int i = 0; i < opts.length - 1; i++) {
                if ("--accelerate3d".equals(opts[i]) && "off".equals(opts[i + 1])) return true;
            }
            return false;
        });

        result.has3dOnCall = vbox.modifyVmCalls.stream().anyMatch(opts -> {
            for (int i = 0; i < opts.length - 1; i++) {
                if ("--accelerate3d".equals(opts[i]) && "on".equals(opts[i + 1])) return true;
            }
            return false;
        });

        return result;
    }

    // -------------------------------------------------------------------------
    // Helper: build a FakeVBoxManage for a supported host (3D succeeds)
    // -------------------------------------------------------------------------

    static FakeVBoxManage supportedHost(boolean vmExists) {
        return new FakeVBoxManage(vmExists, false, null, false, null, false, null);
    }

    static FakeVBoxManage startVmFailure(boolean vmExists, String errorMsg) {
        return new FakeVBoxManage(vmExists, false, null, true, errorMsg, false, null);
    }

    static FakeVBoxManage unrelatedModifyVmFailure(boolean vmExists, String errorMsg) {
        return new FakeVBoxManage(vmExists, false, null, false, null, true, errorMsg);
    }

    // -------------------------------------------------------------------------
    // Test Case P1: Supported host - vmExists=true branch
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 3.1
     *
     * When the host supports 3D acceleration (modifyvm --accelerate3d on succeeds),
     * the VM starts with 3D on and no --accelerate3d off retry occurs.
     * This is the baseline behavior that must be preserved.
     */
    @Test
    void supportedHost_vmExists_vmStartsWith3dOn_noRetry() {
        FakeVBoxManage vbox = supportedHost(true);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, 1024L, 2, 128, true, "", "");

        assertTrue(result.vmStarted,
                "Supported host: VM should start successfully");
        assertFalse(result.failedToStartMessageShown,
                "Supported host: 'Failed to start VM' should NOT be shown");
        assertTrue(result.has3dOnCall,
                "Supported host: --accelerate3d on should be called");
        assertFalse(result.retriedWith3dOff,
                "Supported host: --accelerate3d off retry should NOT occur");
    }

    // -------------------------------------------------------------------------
    // Test Case P2: Supported host - vmExists=false (create-VM) branch
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 3.1
     *
     * Same as P1 but for the create-VM branch.
     */
    @Test
    void supportedHost_createVmBranch_vmStartsWith3dOn_noRetry() {
        FakeVBoxManage vbox = supportedHost(false);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, 2048L, 4, 64, false, "", "");

        assertTrue(result.vmStarted,
                "Supported host (create branch): VM should start successfully");
        assertFalse(result.failedToStartMessageShown,
                "Supported host (create branch): 'Failed to start VM' should NOT be shown");
        assertTrue(result.has3dOnCall,
                "Supported host (create branch): --accelerate3d on should be called");
        assertFalse(result.retriedWith3dOff,
                "Supported host (create branch): --accelerate3d off retry should NOT occur");
    }

    // -------------------------------------------------------------------------
    // Test Case P3: Unrelated failure - startVm fails
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 3.2
     *
     * When startVm fails with an unrelated error, the sequence shows
     * "Failed to start VM" (failedToStartMessageShown=true) and does NOT retry
     * with --accelerate3d off.
     */
    @Test
    void unrelatedFailure_startVmFails_showsFailedMessage_noRetry() {
        FakeVBoxManage vbox = startVmFailure(true,
                "VBoxManage error (exit 1): startvm failed: VHD not found");

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, 1024L, 2, 128, true, "", "");

        assertFalse(result.vmStarted,
                "Unrelated startVm failure: VM should NOT be marked as started");
        assertTrue(result.failedToStartMessageShown,
                "Unrelated startVm failure: 'Failed to start VM' SHOULD be shown");
        assertFalse(result.retriedWith3dOff,
                "Unrelated startVm failure: --accelerate3d off retry should NOT occur");
    }

    // -------------------------------------------------------------------------
    // Test Case P4: Unrelated modifyVm failure (message lacks 3D markers)
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 3.2
     *
     * When modifyVm fails with an error that does NOT contain the 3D markers,
     * the sequence shows "Failed to start VM" and does NOT retry with off.
     */
    @Test
    void unrelatedModifyVmFailure_showsFailedMessage_noRetry() {
        String unrelatedError = "VBoxManage error (exit 1): modifyvm failed: invalid parameter --memory";
        FakeVBoxManage vbox = unrelatedModifyVmFailure(true, unrelatedError);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, 1024L, 2, 128, true, "", "");

        assertFalse(result.vmStarted,
                "Unrelated modifyVm failure: VM should NOT be marked as started");
        assertTrue(result.failedToStartMessageShown,
                "Unrelated modifyVm failure: 'Failed to start VM' SHOULD be shown");
        assertFalse(result.retriedWith3dOff,
                "Unrelated modifyVm failure: --accelerate3d off retry should NOT occur");
        // Confirm the error is NOT a 3D error
        assertFalse(isVBoxE_NotSupported(result.failedToStartError),
                "The error should not be recognized as a 3D-unsupported error");
    }

    // -------------------------------------------------------------------------
    // Test Case P5: Other settings - exact command sequence preserved (vmExists=true)
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 3.3
     *
     * When the host supports 3D, the exact modifyVm call includes --ostype, --memory,
     * --cpus, --vram, --accelerate3d on in the correct order.
     * The modifyVmSilent call for --accelerate2dvideo on is also made.
     */
    @Test
    void otherSettings_vmExists_exactCommandSequencePreserved() {
        FakeVBoxManage vbox = supportedHost(true);
        vbox.hostCpuCount = 4;

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, 1024L, 2, 128, true, "", "");

        assertTrue(result.vmStarted, "VM should start");
        assertEquals(1, result.modifyVmCalls.size(),
                "Exactly one modifyVm call should be made (no retry)");

        String[] opts = result.modifyVmCalls.get(0);
        // Expected: --ostype Other_64 --memory 1024 --cpus 2 --vram 128 --accelerate3d on
        assertEquals("--ostype",       opts[0]);
        assertEquals("Other_64",       opts[1]);
        assertEquals("--memory",       opts[2]);
        assertEquals("1024",           opts[3]);
        assertEquals("--cpus",         opts[4]);
        assertEquals("2",              opts[5]); // 4 / 2 = 2
        assertEquals("--vram",         opts[6]);
        assertEquals("128",            opts[7]);
        assertEquals("--accelerate3d", opts[8]);
        assertEquals("on",             opts[9]);

        // modifyVmSilent for --accelerate2dvideo on
        assertEquals(1, result.modifyVmSilentCalls.size(),
                "Exactly one modifyVmSilent call should be made");
        String[] silentOpts = result.modifyVmSilentCalls.get(0);
        assertEquals("--accelerate2dvideo", silentOpts[0]);
        assertEquals("on",                  silentOpts[1]);
    }

    /**
     * Validates: Requirements 3.3
     *
     * Same as above but for the create-VM branch (no --ostype in modifyVm).
     */
    @Test
    void otherSettings_createVmBranch_exactCommandSequencePreserved() {
        FakeVBoxManage vbox = supportedHost(false);
        vbox.hostCpuCount = 4;

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, 512L, 4, 64, false, "", "");

        assertTrue(result.vmStarted, "VM should start");
        assertEquals(1, result.modifyVmCalls.size(),
                "Exactly one modifyVm call should be made (no retry)");

        String[] opts = result.modifyVmCalls.get(0);
        // Create-VM branch: --memory --cpus --vram --accelerate3d on (no --ostype)
        assertEquals("--memory",       opts[0]);
        assertEquals("512",            opts[1]);
        assertEquals("--cpus",         opts[2]);
        assertEquals("1",              opts[3]); // max(1, 4/4) = 1
        assertEquals("--vram",         opts[4]);
        assertEquals("64",             opts[5]);
        assertEquals("--accelerate3d", opts[6]);
        assertEquals("on",             opts[7]);

        assertEquals(1, result.modifyVmSilentCalls.size());
        String[] silentOpts = result.modifyVmSilentCalls.get(0);
        assertEquals("--accelerate2dvideo", silentOpts[0]);
        assertEquals("on",                  silentOpts[1]);
    }

    // -------------------------------------------------------------------------
    // Property P1: Supported host - VM always starts with 3D on, no retry
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 3.1**
     *
     * Property 2 (Preservation): For ALL inputs where the bug condition does NOT hold
     * (modifyvm --accelerate3d on succeeds), the VM starts with 3D on and no
     * --accelerate3d off retry occurs.
     *
     * Generates many test cases across varied VM configs and both branches.
     * MUST PASS on unfixed code (this is the baseline behavior).
     */
    @Property(tries = 100)
    void property_supportedHost_vmAlwaysStartsWith3dOn_noRetry(
            @ForAll @IntRange(min = 64, max = 8192) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll @IntRange(min = 16, max = 256) int videoMem,
            @ForAll boolean is64Bit,
            @ForAll boolean vmExists
    ) {
        FakeVBoxManage vbox = supportedHost(vmExists);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, (long) ramMB, cpuDividedBy, videoMem, is64Bit, "", "");

        // Preservation: VM starts, no failure message, 3D on used, no off retry
        assertTrue(result.vmStarted,
                String.format("Supported host: VM should start. ramMB=%d, cpuDividedBy=%d, " +
                        "vram=%d, is64Bit=%b, vmExists=%b", ramMB, cpuDividedBy, videoMem, is64Bit, vmExists));
        assertFalse(result.failedToStartMessageShown,
                String.format("Supported host: 'Failed to start VM' should NOT be shown. " +
                        "ramMB=%d, cpuDividedBy=%d, vram=%d, is64Bit=%b, vmExists=%b",
                        ramMB, cpuDividedBy, videoMem, is64Bit, vmExists));
        assertTrue(result.has3dOnCall,
                "Supported host: --accelerate3d on must be called");
        assertFalse(result.retriedWith3dOff,
                "Supported host: --accelerate3d off retry must NOT occur");
        assertEquals(1, result.modifyVmCalls.size(),
                "Supported host: exactly one modifyVm call (no retry)");
    }

    // -------------------------------------------------------------------------
    // Property P2: Unrelated startVm failure - always shows failure, no retry
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 3.2**
     *
     * Property 2 (Preservation): For ALL inputs where startVm fails with an error
     * that is NOT the 3D-unsupported error, the sequence shows "Failed to start VM"
     * and does NOT retry with --accelerate3d off.
     *
     * MUST PASS on unfixed code.
     */
    @Property(tries = 80)
    void property_unrelatedStartVmFailure_alwaysShowsFailedMessage_noRetry(
            @ForAll @IntRange(min = 64, max = 4096) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll boolean vmExists,
            @ForAll @StringLength(min = 5, max = 80) @AlphaChars String errorSuffix
    ) {
        // Ensure the error message does NOT contain 3D markers
        String unrelatedError = "VBoxManage error (exit 1): startvm failed: " + errorSuffix;
        // Guard: skip if the random string accidentally contains a 3D marker
        Assume.that(!isVBoxE_NotSupported(unrelatedError));

        FakeVBoxManage vbox = startVmFailure(vmExists, unrelatedError);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, (long) ramMB, cpuDividedBy, 128, true, "", "");

        assertFalse(result.vmStarted,
                "Unrelated startVm failure: VM should NOT be started");
        assertTrue(result.failedToStartMessageShown,
                "Unrelated startVm failure: 'Failed to start VM' MUST be shown");
        assertFalse(result.retriedWith3dOff,
                "Unrelated startVm failure: --accelerate3d off retry must NOT occur");
    }

    // -------------------------------------------------------------------------
    // Property P3: Unrelated modifyVm failure - always shows failure, no retry
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 3.2**
     *
     * Property 2 (Preservation): For ALL inputs where modifyVm fails with an error
     * that does NOT contain the 3D-unsupported markers, the sequence shows
     * "Failed to start VM" and does NOT retry with --accelerate3d off.
     *
     * MUST PASS on unfixed code.
     */
    @Property(tries = 80)
    void property_unrelatedModifyVmFailure_alwaysShowsFailedMessage_noRetry(
            @ForAll @IntRange(min = 64, max = 4096) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll boolean vmExists,
            @ForAll @StringLength(min = 5, max = 80) @AlphaChars String errorSuffix
    ) {
        String unrelatedError = "VBoxManage error (exit 1): modifyvm failed: " + errorSuffix;
        Assume.that(!isVBoxE_NotSupported(unrelatedError));

        FakeVBoxManage vbox = unrelatedModifyVmFailure(vmExists, unrelatedError);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, (long) ramMB, cpuDividedBy, 128, true, "", "");

        assertFalse(result.vmStarted,
                "Unrelated modifyVm failure: VM should NOT be started");
        assertTrue(result.failedToStartMessageShown,
                "Unrelated modifyVm failure: 'Failed to start VM' MUST be shown");
        assertFalse(result.retriedWith3dOff,
                "Unrelated modifyVm failure: --accelerate3d off retry must NOT occur");
    }

    // -------------------------------------------------------------------------
    // Property P4: Other settings - ostype/memory/cpus/vram preserved exactly
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 3.3**
     *
     * Property 2 (Preservation): For ALL supported-host inputs, the modifyVm call
     * contains --ostype, --memory, --cpus, --vram, --accelerate3d on in the correct
     * order with the correct computed values.
     *
     * MUST PASS on unfixed code.
     */
    @Property(tries = 100)
    void property_otherSettings_vmExists_commandValuesPreserved(
            @ForAll @IntRange(min = 64, max = 8192) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll @IntRange(min = 16, max = 256) int videoMem,
            @ForAll boolean is64Bit
    ) {
        FakeVBoxManage vbox = supportedHost(true);
        vbox.hostCpuCount = 4;

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, (long) ramMB, cpuDividedBy, videoMem, is64Bit, "", "");

        assertTrue(result.vmStarted, "VM should start");
        assertEquals(1, result.modifyVmCalls.size(), "Exactly one modifyVm call");

        String[] opts = result.modifyVmCalls.get(0);
        String expectedOsType = is64Bit ? "Other_64" : "Other";
        int expectedCpuCount = Math.max(1, 4 / cpuDividedBy);

        assertEquals("--ostype",       opts[0], "First option must be --ostype");
        assertEquals(expectedOsType,   opts[1], "OSType must match is64Bit flag");
        assertEquals("--memory",       opts[2], "Third option must be --memory");
        assertEquals(String.valueOf(ramMB), opts[3], "Memory value must match ramMB");
        assertEquals("--cpus",         opts[4], "Fifth option must be --cpus");
        assertEquals(String.valueOf(expectedCpuCount), opts[5], "CPU count must be hostCpuCount/cpuDividedBy");
        assertEquals("--vram",         opts[6], "Seventh option must be --vram");
        assertEquals(String.valueOf(videoMem), opts[7], "VRAM value must match videoMem");
        assertEquals("--accelerate3d", opts[8], "Ninth option must be --accelerate3d");
        assertEquals("on",             opts[9], "3D accel must be 'on' for supported host");
    }

    /**
     * **Validates: Requirements 3.3**
     *
     * Same as above but for the create-VM branch (no --ostype in modifyVm call).
     */
    @Property(tries = 100)
    void property_otherSettings_createVmBranch_commandValuesPreserved(
            @ForAll @IntRange(min = 64, max = 8192) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll @IntRange(min = 16, max = 256) int videoMem,
            @ForAll boolean is64Bit
    ) {
        FakeVBoxManage vbox = supportedHost(false);
        vbox.hostCpuCount = 4;

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, (long) ramMB, cpuDividedBy, videoMem, is64Bit, "", "");

        assertTrue(result.vmStarted, "VM should start");
        assertEquals(1, result.modifyVmCalls.size(), "Exactly one modifyVm call");

        String[] opts = result.modifyVmCalls.get(0);
        int expectedCpuCount = Math.max(1, 4 / cpuDividedBy);

        // Create-VM branch: --memory --cpus --vram --accelerate3d on (no --ostype)
        assertEquals("--memory",       opts[0], "First option must be --memory");
        assertEquals(String.valueOf(ramMB), opts[1], "Memory value must match ramMB");
        assertEquals("--cpus",         opts[2], "Third option must be --cpus");
        assertEquals(String.valueOf(expectedCpuCount), opts[3], "CPU count must be hostCpuCount/cpuDividedBy");
        assertEquals("--vram",         opts[4], "Fifth option must be --vram");
        assertEquals(String.valueOf(videoMem), opts[5], "VRAM value must match videoMem");
        assertEquals("--accelerate3d", opts[6], "Seventh option must be --accelerate3d");
        assertEquals("on",             opts[7], "3D accel must be 'on' for supported host");
    }

    // -------------------------------------------------------------------------
    // Property P5: accelerate2dvideo on is always called silently
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 3.3**
     *
     * The modifyVmSilent call for --accelerate2dvideo on is always made after
     * the main modifyVm call, regardless of VM config.
     *
     * MUST PASS on unfixed code.
     */
    @Property(tries = 60)
    void property_accelerate2dVideo_alwaysCalledSilently(
            @ForAll @IntRange(min = 64, max = 4096) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll boolean vmExists
    ) {
        FakeVBoxManage vbox = supportedHost(vmExists);

        VmStartResult result = runUnfixedVmStartSequence(
                vbox, (long) ramMB, cpuDividedBy, 128, true, "", "");

        assertTrue(result.vmStarted, "VM should start");
        assertEquals(1, result.modifyVmSilentCalls.size(),
                "Exactly one modifyVmSilent call should be made");
        String[] silentOpts = result.modifyVmSilentCalls.get(0);
        assertEquals("--accelerate2dvideo", silentOpts[0],
                "modifyVmSilent must use --accelerate2dvideo");
        assertEquals("on", silentOpts[1],
                "modifyVmSilent --accelerate2dvideo value must be 'on'");
    }
}

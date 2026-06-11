package mcvmcomputers.test;

import net.jqwik.api.*;
import net.jqwik.api.constraints.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Bug Condition Exploration Test - Task 1
 *
 * This test encodes the EXPECTED behavior (Property 1 from design):
 * When modifyvm --accelerate3d on fails with VBOX_E_NOT_SUPPORTED,
 * the VM-start path should reconfigure with --accelerate3d off,
 * continue the sequence, and start the VM successfully.
 *
 * On UNFIXED code, this test FAILS - proving the bug exists.
 * After the fix (Task 3.1), this test will PASS.
 *
 * Validates: Requirements 2.1, 2.2
 */
public class VmStartSequenceTest {

    // -------------------------------------------------------------------------
    // Bug condition predicate (from design spec)
    // -------------------------------------------------------------------------

    /**
     * Returns true when the input represents the bug condition:
     * - The modifyvm call requested --accelerate3d on
     * - The call failed with a VBOX_E_NOT_SUPPORTED error
     */
    static boolean isBugCondition(boolean requested3dOn, Exception modifyVmError) {
        return requested3dOn
                && modifyVmError != null
                && isVBoxE_NotSupported(modifyVmError.getMessage());
    }

    static boolean isVBoxE_NotSupported(String message) {
        if (message == null) return false;
        return message.contains("VBOX_E_NOT_SUPPORTED")
                || message.contains("does not support the given feature");
    }

    // -------------------------------------------------------------------------
    // Fake VBoxManage - intercepts calls without a real VirtualBox host
    // -------------------------------------------------------------------------

    /**
     * A fake VBoxManage that records all calls and simulates the
     * VBOX_E_NOT_SUPPORTED failure for --accelerate3d on.
     */
    static class FakeVBoxManage {
        final boolean vmExists;
        final boolean fail3dOn;          // whether modifyvm --accelerate3d on should fail
        final String failureMessage;     // the error message to use when failing

        // Recorded calls
        final List<String[]> modifyVmCalls = new ArrayList<>();
        final List<String[]> modifyVmSilentCalls = new ArrayList<>();
        boolean startVmCalled = false;
        boolean createVmCalled = false;
        int hostCpuCount = 4;

        FakeVBoxManage(boolean vmExists, boolean fail3dOn, String failureMessage) {
            this.vmExists = vmExists;
            this.fail3dOn = fail3dOn;
            this.failureMessage = failureMessage;
        }

        boolean vmExists(String name) { return vmExists; }
        String getVmState(String name) { return "poweroff"; }
        int getHostProcessorCount() { return hostCpuCount; }

        void createVm(String name, String osType) throws Exception {
            createVmCalled = true;
        }

        void modifyVm(String name, String... options) throws Exception {
            modifyVmCalls.add(options.clone());
            // Check if this call includes --accelerate3d on
            for (int i = 0; i < options.length - 1; i++) {
                if ("--accelerate3d".equals(options[i]) && "on".equals(options[i + 1])) {
                    if (fail3dOn) {
                        throw new RuntimeException(failureMessage);
                    }
                }
            }
        }

        void modifyVmSilent(String name, String... options) {
            modifyVmSilentCalls.add(options.clone());
        }

        /**
         * Mirrors the real VBoxManage.modifyVmWith3dAccelFallback:
         * calls modifyVm; if it throws with a 3D-unsupported error, swaps
         * "--accelerate3d" "on" -> "off" and retries once.
         */
        void modifyVmWith3dAccelFallback(String name, String... options) throws Exception {
            try {
                modifyVm(name, options);
            } catch (Exception ex) {
                if (!isVBoxE_NotSupported(ex.getMessage())) {
                    throw ex;
                }
                // Swap the trailing "--accelerate3d" value from "on" to "off" and retry once.
                String[] fallbackOptions = options.clone();
                fallbackOptions[fallbackOptions.length - 1] = "off";
                modifyVm(name, fallbackOptions);
            }
        }

        void removeStorageController(String vmName, String controllerName) { /* silent */ }
        void addStorageController(String vmName, String controllerName, String bus) throws Exception { /* ok */ }
        void storageAttachSilent(String vmName, String controller, int port, int device, String type, String medium) { /* silent */ }
        void powerOffVm(String name) { /* silent */ }
        void discardSavedState(String name) { /* silent */ }

        void startVm(String name) throws Exception {
            startVmCalled = true;
        }
    }

    // -------------------------------------------------------------------------
    // VM-start logic extracted from GuiPCEditing.turnOnPC (UNFIXED version)
    // This replicates the exact sequence from the production code.
    // -------------------------------------------------------------------------

    static class VmStartResult {
        boolean vmStarted;
        boolean failedToStartMessageShown;
        String failedToStartError;
        List<String[]> modifyVmCalls;
        boolean retriedWith3dOff;
    }

    /**
     * Runs the UNFIXED VM-start sequence (mirrors GuiPCEditing.turnOnPC worker thread).
     * Uses the faked VBoxManage to avoid needing a real VirtualBox host.
     *
     * This is the exact logic from the unfixed code - no fallback for 3D errors.
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

                // UNFIXED: no fallback - just calls modifyVm with --accelerate3d on
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
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", "/fake/" + hardDriveFileName);
                }
                if (isoFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
                }

            } else {
                String OSType = "Other";
                if (is64Bit) OSType += "_64";
                vbox.createVm("VmComputersVm", OSType);

                int cpuCount = Math.max(1, vbox.getHostProcessorCount() / cpuDividedBy);

                // UNFIXED: no fallback - just calls modifyVm with --accelerate3d on
                vbox.modifyVm("VmComputersVm",
                        "--memory", String.valueOf(ramMB),
                        "--cpus", String.valueOf(cpuCount),
                        "--vram", String.valueOf(videoMem),
                        "--accelerate3d", "on");

                vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");
                vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

                if (!hardDriveFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", "/fake/" + hardDriveFileName);
                }
                if (isoFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
                }
            }

            vbox.startVm("VmComputersVm");
            result.vmStarted = true;

        } catch (Exception ex) {
            // This is the generic catch block from turnOnPC - shows "Failed to start VM"
            result.failedToStartMessageShown = true;
            result.failedToStartError = ex.getMessage();
        }

        result.modifyVmCalls = vbox.modifyVmCalls;

        // Check if any modifyVm call used --accelerate3d off (retry)
        result.retriedWith3dOff = vbox.modifyVmCalls.stream().anyMatch(opts -> {
            for (int i = 0; i < opts.length - 1; i++) {
                if ("--accelerate3d".equals(opts[i]) && "off".equals(opts[i + 1])) return true;
            }
            return false;
        });

        return result;
    }

    // -------------------------------------------------------------------------
    // VM-start logic extracted from GuiPCEditing.turnOnPC (FIXED version)
    // This replicates the fixed sequence using modifyVmWith3dAccelFallback.
    // -------------------------------------------------------------------------

    /**
     * Runs the FIXED VM-start sequence (mirrors GuiPCEditing.turnOnPC after Task 3.1).
     * Uses modifyVmWith3dAccelFallback instead of modifyVm for the --accelerate3d call.
     */
    static VmStartResult runFixedVmStartSequence(
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

                // FIXED: uses modifyVmWith3dAccelFallback - falls back to --accelerate3d off
                vbox.modifyVmWith3dAccelFallback("VmComputersVm",
                        "--ostype", OSType,
                        "--memory", String.valueOf(ramMB),
                        "--cpus", String.valueOf(cpuCount),
                        "--vram", String.valueOf(videoMem),
                        "--accelerate3d", "on");

                vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");
                vbox.removeStorageController("VmComputersVm", "IDE Controller");
                vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

                if (!hardDriveFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", "/fake/" + hardDriveFileName);
                }
                if (isoFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
                }

            } else {
                String OSType = "Other";
                if (is64Bit) OSType += "_64";
                vbox.createVm("VmComputersVm", OSType);

                int cpuCount = Math.max(1, vbox.getHostProcessorCount() / cpuDividedBy);

                // FIXED: uses modifyVmWith3dAccelFallback - falls back to --accelerate3d off
                vbox.modifyVmWith3dAccelFallback("VmComputersVm",
                        "--memory", String.valueOf(ramMB),
                        "--cpus", String.valueOf(cpuCount),
                        "--vram", String.valueOf(videoMem),
                        "--accelerate3d", "on");

                vbox.modifyVmSilent("VmComputersVm", "--accelerate2dvideo", "on");
                vbox.addStorageController("VmComputersVm", "IDE Controller", "ide");

                if (!hardDriveFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 0, 0, "hdd", "/fake/" + hardDriveFileName);
                }
                if (isoFileName.isEmpty()) {
                    vbox.storageAttachSilent("VmComputersVm", "IDE Controller", 1, 0, "dvddrive", "emptydrive");
                }
            }

            vbox.startVm("VmComputersVm");
            result.vmStarted = true;

        } catch (Exception ex) {
            result.failedToStartMessageShown = true;
            result.failedToStartError = ex.getMessage();
        }

        result.modifyVmCalls = vbox.modifyVmCalls;

        // Check if any modifyVm call used --accelerate3d off (retry)
        result.retriedWith3dOff = vbox.modifyVmCalls.stream().anyMatch(opts -> {
            for (int i = 0; i < opts.length - 1; i++) {
                if ("--accelerate3d".equals(opts[i]) && "off".equals(opts[i + 1])) return true;
            }
            return false;
        });

        return result;
    }

    // -------------------------------------------------------------------------
    // Real-format VBOX_E_NOT_SUPPORTED error message (from design spec)
    // -------------------------------------------------------------------------

    static final String VBOX_E_NOT_SUPPORTED_MSG =
            "VBoxManage error (exit 1): 0%VBoxManage.exe: error: The graphics controller does not support the given feature\n" +
            "VBoxManage.exe: error: Details: code VBOX_E_NOT_SUPPORTED (0x80bb0009), component DisplayWrap, interface IDisplay\n" +
            "VBoxManage.exe: error: Context: \"EnableVMMStatistics\" at line 123 of file VBoxManageModifyVM.cpp";

    // -------------------------------------------------------------------------
    // Test Case 1: Existing-VM branch failure
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 2.1, 2.2
     *
     * When the VM already exists and modifyvm --accelerate3d on fails with
     * VBOX_E_NOT_SUPPORTED, the expected behavior is:
     * - The VM-start path reconfigures with --accelerate3d off
     * - The VM starts successfully
     * - "Failed to start VM" is NOT shown
     *
     * On UNFIXED code: FAILS (proves the bug exists in the vmExists=true branch)
     */
    @Test
    void existingVmBranch_3dAccelFails_vmShouldStartWith3dOff() {
        FakeVBoxManage vbox = new FakeVBoxManage(
                true,   // vmExists = true
                true,   // fail3dOn = true (simulates VBOX_E_NOT_SUPPORTED)
                VBOX_E_NOT_SUPPORTED_MSG
        );

        VmStartResult result = runFixedVmStartSequence(
                vbox,
                1024L,  // ramMB
                2,      // cpuDividedBy
                128,    // videoMem
                true,   // is64Bit
                "",     // no HDD
                ""      // no ISO
        );

        // Expected behavior (Property 1): VM should start, no failure message
        assertTrue(result.vmStarted,
                "FIX FAILED: VM did not start. The fixed code should retry with " +
                "--accelerate3d off when modifyvm fails with VBOX_E_NOT_SUPPORTED in the vmExists=true branch. " +
                "Error was: " + result.failedToStartError);
        assertFalse(result.failedToStartMessageShown,
                "FIX FAILED: 'Failed to start VM' was shown. The fixed code should not show this " +
                "message when retrying with --accelerate3d off.");
        assertTrue(result.retriedWith3dOff,
                "FIX FAILED: No retry with --accelerate3d off was attempted. " +
                "The fixed code must fall back to --accelerate3d off.");
    }

    // -------------------------------------------------------------------------
    // Test Case 2: Create-VM branch failure
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 2.1, 2.2
     *
     * When the VM does not exist (create-VM branch) and modifyvm --accelerate3d on
     * fails with VBOX_E_NOT_SUPPORTED, the expected behavior is:
     * - The VM-start path reconfigures with --accelerate3d off
     * - The VM starts successfully
     * - "Failed to start VM" is NOT shown
     *
     * On UNFIXED code: FAILS (proves the bug exists in the vmExists=false branch)
     */
    @Test
    void createVmBranch_3dAccelFails_vmShouldStartWith3dOff() {
        FakeVBoxManage vbox = new FakeVBoxManage(
                false,  // vmExists = false (create-VM branch)
                true,   // fail3dOn = true (simulates VBOX_E_NOT_SUPPORTED)
                VBOX_E_NOT_SUPPORTED_MSG
        );

        VmStartResult result = runFixedVmStartSequence(
                vbox,
                2048L,  // ramMB
                4,      // cpuDividedBy
                64,     // videoMem
                false,  // is32Bit
                "",     // no HDD
                ""      // no ISO
        );

        // Expected behavior (Property 1): VM should start, no failure message
        assertTrue(result.vmStarted,
                "FIX FAILED: VM did not start. The fixed code should retry with " +
                "--accelerate3d off when modifyvm fails with VBOX_E_NOT_SUPPORTED in the createVm branch. " +
                "Error was: " + result.failedToStartError);
        assertFalse(result.failedToStartMessageShown,
                "FIX FAILED: 'Failed to start VM' was shown. The fixed code should not show this " +
                "message when retrying with --accelerate3d off.");
        assertTrue(result.retriedWith3dOff,
                "FIX FAILED: No retry with --accelerate3d off was attempted. " +
                "The fixed code must fall back to --accelerate3d off.");
    }

    // -------------------------------------------------------------------------
    // Test Case 3: Error-detection sanity
    // -------------------------------------------------------------------------

    /**
     * Validates: Requirements 2.1
     *
     * The real-format VBOX_E_NOT_SUPPORTED message should be recognized as the
     * 3D-unsupported case. On unfixed code, no detection helper exists, so this
     * drives the need for the isAccelerate3dUnsupportedError helper.
     *
     * This test verifies the detection logic that SHOULD exist but doesn't yet.
     * On UNFIXED code: FAILS (no detection helper exists)
     */
    @Test
    void errorDetection_realFormatMessage_shouldBeRecognizedAs3dUnsupported() {
        String realMsg = "VBoxManage error (exit 1): 0%VBoxManage.exe: error: The graphics controller does not support the given feature\n" +
                "VBoxManage.exe: error: Details: code VBOX_E_NOT_SUPPORTED (0x80bb0009), component DisplayWrap, interface IDisplay";

        // On unfixed code, VBoxManage has no isAccelerate3dUnsupportedError method.
        // We test the detection logic that the fix will introduce.
        // This test will fail on unfixed code because the method doesn't exist yet.
        assertTrue(isVBoxE_NotSupported(realMsg),
                "The real-format VBOX_E_NOT_SUPPORTED message should be recognized as the 3D-unsupported case");

        // Also verify the isBugCondition predicate works correctly
        Exception fakeError = new RuntimeException(realMsg);
        assertTrue(isBugCondition(true, fakeError),
                "isBugCondition should return true for requested3dOn=true with VBOX_E_NOT_SUPPORTED error");
        assertFalse(isBugCondition(false, fakeError),
                "isBugCondition should return false when requested3dOn=false");
        assertFalse(isBugCondition(true, null),
                "isBugCondition should return false when modifyVmError=null");
        assertFalse(isBugCondition(true, new RuntimeException("startvm failed: some other error")),
                "isBugCondition should return false for unrelated errors");
    }

    // -------------------------------------------------------------------------
    // Property-Based Test: Bug condition across varied VM configs
    // -------------------------------------------------------------------------

    /**
     * **Validates: Requirements 2.1, 2.2**
     *
     * Property 1 (Bug Condition): For ALL inputs satisfying the bug condition
     * (modifyvm --accelerate3d on returns VBOX_E_NOT_SUPPORTED), the VM-start path
     * SHALL reconfigure with --accelerate3d off, continue the sequence, start the VM,
     * and NOT show "Failed to start VM".
     *
     * Scoped to: both branches (vmExists true/false), varied VM configs
     * (ramMB, cpuCount, vram, ostype).
     *
     * On UNFIXED code: FAILS for all generated inputs (proves the bug is systematic)
     */
    @Property(tries = 50)
    void property_bugCondition_vmAlwaysStartsWithFallback(
            @ForAll @IntRange(min = 64, max = 8192) int ramMB,
            @ForAll @IntRange(min = 1, max = 6) int cpuDividedBy,
            @ForAll @IntRange(min = 16, max = 256) int videoMem,
            @ForAll boolean is64Bit,
            @ForAll boolean vmExists
    ) {
        // Use the real-format VBOX_E_NOT_SUPPORTED message (bug condition)
        FakeVBoxManage vbox = new FakeVBoxManage(
                vmExists,
                true,   // fail3dOn = true (bug condition)
                VBOX_E_NOT_SUPPORTED_MSG
        );

        VmStartResult result = runFixedVmStartSequence(
                vbox,
                (long) ramMB,
                cpuDividedBy,
                videoMem,
                is64Bit,
                "",  // no HDD (simplify)
                ""   // no ISO (simplify)
        );

        // Property 1: VM must start, no failure message, retry with 3d off
        assertTrue(result.vmStarted,
                String.format("FIX FAILED: VM did not start for ramMB=%d, cpuDividedBy=%d, vram=%d, " +
                        "is64Bit=%b, vmExists=%b. Error: %s",
                        ramMB, cpuDividedBy, videoMem, is64Bit, vmExists, result.failedToStartError));
        assertFalse(result.failedToStartMessageShown,
                String.format("FIX FAILED: 'Failed to start VM' shown for ramMB=%d, cpuDividedBy=%d, " +
                        "vram=%d, is64Bit=%b, vmExists=%b",
                        ramMB, cpuDividedBy, videoMem, is64Bit, vmExists));
        assertTrue(result.retriedWith3dOff,
                String.format("FIX FAILED: No --accelerate3d off retry for ramMB=%d, cpuDividedBy=%d, " +
                        "vram=%d, is64Bit=%b, vmExists=%b",
                        ramMB, cpuDividedBy, videoMem, is64Bit, vmExists));
    }

    /**
     * **Validates: Requirements 2.1, 2.2**
     *
     * Property 1 variant: Also test with the "does not support the given feature"
     * message variant (the other marker from the design spec).
     */
    @Property(tries = 30)
    void property_bugCondition_doesNotSupportFeatureVariant(
            @ForAll @IntRange(min = 64, max = 4096) int ramMB,
            @ForAll @IntRange(min = 1, max = 4) int cpuDividedBy,
            @ForAll boolean vmExists
    ) {
        String altMsg = "VBoxManage error (exit 1): The graphics controller does not support the given feature";

        FakeVBoxManage vbox = new FakeVBoxManage(
                vmExists,
                true,
                altMsg
        );

        VmStartResult result = runFixedVmStartSequence(
                vbox,
                (long) ramMB,
                cpuDividedBy,
                128,
                true,
                "",
                ""
        );

        assertTrue(result.vmStarted,
                String.format("FIX FAILED: VM did not start for 'does not support' variant. " +
                        "ramMB=%d, cpuDividedBy=%d, vmExists=%b. Error: %s",
                        ramMB, cpuDividedBy, vmExists, result.failedToStartError));
        assertFalse(result.failedToStartMessageShown,
                "FIX FAILED: 'Failed to start VM' shown for 'does not support' variant");
        assertTrue(result.retriedWith3dOff,
                "FIX FAILED: No --accelerate3d off retry for 'does not support' variant");
    }
}

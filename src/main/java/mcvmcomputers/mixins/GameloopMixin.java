package mcvmcomputers.mixins;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.gui.screens.Screen;


import static mcvmcomputers.client.ClientMod.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcvmcomputers.client.gui.setup.GuiSetup;
import mcvmcomputers.client.tablet.TabletOS;
import mcvmcomputers.client.utils.VMRunnable;
import mcvmcomputers.entities.EntityItemPreview;
import mcvmcomputers.item.ItemList;
import mcvmcomputers.item.ItemOrderingTablet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(Minecraft.class)
public class GameloopMixin {
    @Shadow
    public LocalPlayer player;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public ClientLevel level;

    @Shadow
    public net.minecraft.client.gui.screens.Screen screen;

    private boolean vmInitDone = false;
    private boolean wasInWorld = false;

    @Inject(at = @At("HEAD"), method = "runTick")
    private void runTick(boolean renderLevel, CallbackInfo info) {
        Minecraft mcc = Minecraft.getInstance();

        if (!vmInitDone) {
            vmInitDone = true;
            vhdDirectory = new File(mcc.gameDirectory, "vm_computers/vhds");
            vhdDirectory.mkdirs();
            isoDirectory = new File(mcc.gameDirectory, "vm_computers/isos");
            isoDirectory.mkdirs();

            File num = new File(vhdDirectory.getParentFile(), "vhdnum");
            if (num.exists()) {
                try {
                    List<String> lines = Files.readAllLines(num.toPath());
                    latestVHDNum = Integer.parseInt(lines.get(0));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (vbox == null && !(this.screen instanceof GuiSetup)) {
            Minecraft.getInstance().setScreen(new GuiSetup());
        }

        // Detect (re)entering a world. After a server reconnect the previous
        // VirtualBox web-service session is stale, so force a fresh connection on
        // the next mouse/keyboard call. This recovers mouse capture without having
        // to manually restart the VM, even when the VM keeps running across the
        // reconnect (in which case startVm() - which also resets - is not called).
        boolean inWorld = this.player != null && this.level != null;        if (inWorld && !wasInWorld && vbox != null) {
            vbox.resetMouseConnection();
        }
        wasInWorld = inWorld;

        if (lastDeltaTimeTime == 0) {
            lastDeltaTimeTime = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            long diff = now - lastDeltaTimeTime;
            lastDeltaTimeTime = now;
            deltaTime = (float) diff / 1000f;
        }

        if (tabletOS != null) {
            tabletOS.generateTexture();
        } else {
            try {
                tabletOS = new TabletOS();
                tabletThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (true) {
                            try {
                                tabletOS.render();
                                Thread.sleep(33);
                            } catch (ConcurrentModificationException e) {
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }, "Tablet Renderer");
                tabletThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (vmTurnedOn) {
            if (player == null) {
                if (vmUpdateThread != null) {
                    vmUpdateThread.interrupt();
                }
                if (vbox != null) {
                    vbox.powerOffVm("VmComputersVm");
                }
                vmTurnedOn = false;
                vmTurningOff = false;
                vmTurningOn = false;
            } else {
                if (vmUpdateThread == null) {
                    vmUpdateThread = new Thread(new VMRunnable(), "VM Update Thread");
                    vmUpdateThread.start();
                }
                generatePCScreen();
            }
        }

        if (player != null) {
            boolean tabletOut = false;
            for (ItemStack is : player.getHandSlots()) {
                if (is.getItem() != null) {
                    if (is.getItem() instanceof ItemOrderingTablet) {
                        tabletOut = true;
                        break;
                    }
                }
            }

            if (tabletOut != tabletOS.tabletOn) {
                tabletOS.tabletOn = tabletOut;
                if (tabletOut) {
                    tabletOS.tabletTakenOut();
                } else {
                    tabletOS.tabletUnequipped();
                }
            }

            for (ItemStack is : player.getHandSlots()) {
                if (is.getItem() != null) {
                    if (ItemList.isPlacableItem(is.getItem())) {
                        if (thePreviewEntity != null) {
                            thePreviewEntity.setItem(is);
                            if (hitResult != null) {
                                Vec3 hit = hitResult.getLocation();
                                thePreviewEntity.setPos(hit.x, hit.y, hit.z);
                            } else {
                                break;
                            }
                        } else {
                            if (hitResult != null) {
                                Vec3 hit = hitResult.getLocation();
                                thePreviewEntity = new EntityItemPreview(level, hit.x, hit.y, hit.z, is);
                                ((ClientLevel)this.level).addEntity(thePreviewEntity);
                            }
                        }
                    } else {
                        break;
                    }
                } else {
                    break;
                }
                return;
            }
            if (thePreviewEntity != null) {
                thePreviewEntity.kill();
                thePreviewEntity = null;
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "destroy")
    private void destroy(CallbackInfo info) {
        vmTurningOn = false;
        vmTurnedOn = false;
        vmTurningOff = false;

        if (vbox != null) {
            boolean vmExists = vbox.vmExists("VmComputersVm");
            if (vmExists) {
                String state = vbox.getVmState("VmComputersVm");
                if ("running".equals(state) || "starting".equals(state) || "firstonline".equals(state)) {
                    vbox.powerOffVm("VmComputersVm");
                } else if ("saved".equals(state)) {
                    vbox.discardSavedState("VmComputersVm");
                }
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "close")
    private void close(CallbackInfo info) {
        System.out.println("Stopping VM Computers Mod...");
        for (var nibt : vmScreenTextureNIBT.values()) {
            nibt.close();
        }
        for (var ni : vmScreenTextureNI.values()) {
            ni.close();
        }
        for (var i : vmScreenTextures.values()) {
            Minecraft.getInstance().getTextureManager().release(i);
        }
        if (vmUpdateThread != null) {
            vmUpdateThread.interrupt();
        }
        if (tabletThread != null) {
            tabletThread.interrupt();
        }

        vmTurningOn = false;
        vmTurnedOn = false;
        vmTurningOff = false;

        if (vbox != null) {
            boolean vmExists = vbox.vmExists("VmComputersVm");
            if (vmExists) {
                String state = vbox.getVmState("VmComputersVm");
                if ("running".equals(state) || "starting".equals(state) || "firstonline".equals(state)) {
                    vbox.powerOffVm("VmComputersVm");
                } else if ("saved".equals(state)) {
                    vbox.discardSavedState("VmComputersVm");
                }
            }
        }
        System.out.println("Stopped VM Computers Mod.");
    }
}

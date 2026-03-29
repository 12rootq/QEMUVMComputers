package newvmcomputers.client.utils;

import static newvmcomputers.client.ClientMod.*;

import java.util.Arrays;
import java.util.List;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import org.virtualbox_6_1.BitmapFormat;
import org.virtualbox_6_1.GuestMonitorStatus;
import org.virtualbox_6_1.Holder;
import org.virtualbox_6_1.IConsole;
import org.virtualbox_6_1.IMachine;
import org.virtualbox_6_1.IProgress;
import org.virtualbox_6_1.ISession;
import org.virtualbox_6_1.LockType;
import org.virtualbox_6_1.MachineState;

import newvmcomputers.client.ClientMod;
import newvmcomputers.client.gui.GuiFocus;
import net.minecraft.client.Minecraft;

public class VMRunnable implements Runnable {
	private int lastMouseButtons = 0;
	private long lastMousePacketTime = 0; // Р СњР С›Р вЂ™Р С›Р вЂў
	@Override
	public void run() {
		if (ClientMod.useVmware) {
			runVmwareVNC();
		} else {
			runVirtualBox();
		}
	}
	private int mapToKeysym(int base, boolean extended) {
		if (extended) {
			switch (base) {
				case 0x1C: return 0xFF8D; case 0x1D: return 0xFFE4; case 0x38: return 0xFFEA;
				case 0x48: return 0xFF52; case 0x4B: return 0xFF51; case 0x4D: return 0xFF53;
				case 0x50: return 0xFF54; case 0x49: return 0xFF55; case 0x51: return 0xFF56;
				case 0x4F: return 0xFF57; case 0x47: return 0xFF50; case 0x52: return 0xFF63;
				case 0x53: return 0xFFFF;
				case 0x35: return 0xFFAF;
			}
			return 0;
		}
		switch (base) {
			case 1: return 0xFF1B; // Esc
			case 2: return 0x0031; case 3: return 0x0032; case 4: return 0x0033;
			case 5: return 0x0034; case 6: return 0x0035; case 7: return 0x0036;
			case 8: return 0x0037; case 9: return 0x0038; case 10: return 0x0039; case 11: return 0x0030;
			case 12: return 0x002D; case 13: return 0x003D; case 14: return 0xFF08; case 15: return 0xFF09;
			case 16: return 0x0071; case 17: return 0x0077; case 18: return 0x0065; case 19: return 0x0072;
			case 20: return 0x0074; case 21: return 0x0079; case 22: return 0x0075; case 23: return 0x0069;
			case 24: return 0x006f; case 25: return 0x0070; case 26: return 0x005B; case 27: return 0x005D;
			case 28: return 0xFF0D; // Enter
			case 29: return 0xFFE3; case 30: return 0x0061; case 31: return 0x0073; case 32: return 0x0064;
			case 33: return 0x0066; case 34: return 0x0067; case 35: return 0x0068; case 36: return 0x006A;
			case 37: return 0x006B; case 38: return 0x006C; case 39: return 0x003B; case 40: return 0x0027;
			case 41: return 0x0060; case 42: return 0xFFE1; case 43: return 0x005C; case 44: return 0x007A;
			case 45: return 0x0078; case 46: return 0x0063; case 47: return 0x0076; case 48: return 0x0062;
			case 49: return 0x006E; case 50: return 0x006D; case 51: return 0x002C; case 52: return 0x002E;
			case 53: return 0x002F; case 54: return 0xFFE2; case 56: return 0xFFE9; case 57: return 0x0020;
			case 58: return 0xFFE5; case 59: return 0xFFBE; case 60: return 0xFFBF; case 61: return 0xFFC0;
			case 62: return 0xFFC1; case 63: return 0xFFC2; case 64: return 0xFFC3; case 65: return 0xFFC4;
			case 66: return 0xFFC5; case 67: return 0xFFC6; case 68: return 0xFFC7; case 87: return 0xFFC8;
			case 88: return 0xFFC9;
			case 69: return 0xFF7F; // NumLock
			case 70: return 0xFF14; // ScrollLock
			case 71: return 0xFFB7; // KP 7
			case 72: return 0xFFB8; // KP 8
			case 73: return 0xFFB9; // KP 9
			case 74: return 0xFFAD; // KP Subtract
			case 75: return 0xFFB4; // KP 4
			case 76: return 0xFFB5; // KP 5
			case 77: return 0xFFB6; // KP 6
			case 78: return 0xFFAB; // KP Add
			case 79: return 0xFFB1; // KP 1
			case 80: return 0xFFB2; // KP 2
			case 81: return 0xFFB3; // KP 3
			case 82: return 0xFFB0; // KP 0
			case 83: return 0xFFAE; // KP Decimal
		}
		return 0;
	}

	private void runVmwareVNC() {
		try {
			Thread.sleep(3000);

			Socket socket = null;
			long lastLogTime = 0L;
			while (ClientMod.vmTurnedOn && !ClientMod.vmTurningOff) {
				try {
					System.out.println("VMware: Connecting to VNC (127.0.0.1:5900)...");
					socket = new Socket("127.0.0.1", 5900);
					break;
				} catch (Exception ex) {
					long now = System.currentTimeMillis();
					if (now - lastLogTime >= 2000L) {
						System.err.println("VMware VNC Error: " + ex.getMessage());
						lastLogTime = now;
					}
					Thread.sleep(1000);
				}
			}

			if (socket == null) {
				vmUpdateThread = null;
				return;
			}

			socket.setTcpNoDelay(true);
			DataInputStream in = new DataInputStream(socket.getInputStream());
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());

			byte[] version = new byte[12];
			in.readFully(version);
			out.write("RFB 003.008\n".getBytes());

			int numSec = in.readUnsignedByte();
			if (numSec == 0) throw new Exception("VNC Server failed");
			byte[] secTypes = new byte[numSec];
			in.readFully(secTypes);
			out.writeByte(1);

			int secResult = in.readInt();
			if (secResult != 0) throw new Exception("VNC Auth Failed");

			out.writeByte(1); // Shared

			int width = in.readUnsignedShort();
			int height = in.readUnsignedShort();
			byte[] serverPixelFormat = new byte[16];
			in.readFully(serverPixelFormat);
			int nameLen = in.readInt();
			byte[] name = new byte[nameLen];
			in.readFully(name);
			System.out.println("VMware VNC: Screen captured! Resolution " + width + "x" + height);

			out.writeByte(0); // SetPixelFormat
			out.writeByte(0); out.writeShort(0); // padding
			out.writeByte(32); // bpp
			out.writeByte(24); // depth
			out.writeByte(0); // big-endian
			out.writeByte(1); // true-color
			out.writeShort(255); out.writeShort(255); out.writeShort(255); // RGB max
			out.writeByte(0); out.writeByte(8); out.writeByte(16); // RGB shift
			out.writeByte(0); out.writeByte(0); out.writeByte(0); // padding

			out.writeByte(2); // SetEncodings
			out.writeByte(0); // padding
			out.writeShort(1);
			out.writeInt(0); // RAW

			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			int absX = width / 2;
			int absY = height / 2;
			long lastRequest = 0;
			boolean waitingForUpdate = false;
			int lastMouseButtons = 0;
			long lastMousePacketTime = 0;
			while (ClientMod.vmTurnedOn && !ClientMod.vmTurningOff) {
				if (Minecraft.getInstance().screen instanceof GuiFocus) {
					boolean moved = false;
					if (ClientMod.mouseCurX != ClientMod.mouseLastX || ClientMod.mouseCurY != ClientMod.mouseLastY) {
						absX += (ClientMod.mouseCurX - ClientMod.mouseLastX);
						absY += (ClientMod.mouseCurY - ClientMod.mouseLastY);
						absX = Math.max(0, Math.min(width - 1, absX));
						absY = Math.max(0, Math.min(height - 1, absY));

						ClientMod.mouseLastX = ClientMod.mouseCurX;
						ClientMod.mouseLastY = ClientMod.mouseCurY;
						moved = true;
					}

					int mask = 0;
					if (ClientMod.leftMouseButton) mask |= 1;
					if (ClientMod.middleMouseButton) mask |= 2;
					if (ClientMod.rightMouseButton) mask |= 4;

					long now = System.currentTimeMillis();
					if ((moved || mask != lastMouseButtons) && (now - lastMousePacketTime >= 30)) {
						out.writeByte(5); // PointerEvent
						out.writeByte(mask);
						out.writeShort(absX);
						out.writeShort(absY);
						out.flush();

						lastMouseButtons = mask;
						lastMousePacketTime = now;
					}
				}
				if (ClientMod.releaseKeys) {
					ClientMod.releaseKeys = false;
					ClientMod.vmKeyboardScancodes.clear();
				} else if (!ClientMod.vmKeyboardScancodes.isEmpty()) {
					int i = 0;
					while (i < ClientMod.vmKeyboardScancodes.size()) {
						int code = ClientMod.vmKeyboardScancodes.get(i);
						i++;
						boolean extended = false;
						if (code == 0xE0) {
							extended = true;
							if (i < ClientMod.vmKeyboardScancodes.size()) {
								code = ClientMod.vmKeyboardScancodes.get(i);
								i++;
							} else {
								break;
							}
						}
						boolean down = (code & 0x80) == 0;
						int base = code & 0x7F;
						int keysym = mapToKeysym(base, extended);
						if (keysym != 0) {
							out.writeByte(4); // KeyEvent
							out.writeByte(down ? 1 : 0);
							out.writeShort(0); // padding
							out.writeInt(keysym);
						}
					}
					out.flush();
					ClientMod.vmKeyboardScancodes.clear();
				}
				if (!waitingForUpdate && (System.currentTimeMillis() - lastRequest > 50)) {
					out.writeByte(3); // FramebufferUpdateRequest
					out.writeByte(1); // Incremental
					out.writeShort(0);
					out.writeShort(0);
					out.writeShort(width);
					out.writeShort(height);
					out.flush();
					waitingForUpdate = true;
					lastRequest = System.currentTimeMillis();
				}
				if (in.available() > 0) {
					int msgType = in.readUnsignedByte();
					if (msgType == 0) {
						in.readByte();
						int numRects = in.readUnsignedShort();
						boolean updated = false;

						for (int i = 0; i < numRects; i++) {
							int rx = in.readUnsignedShort();
							int ry = in.readUnsignedShort();
							int rw = in.readUnsignedShort();
							int rh = in.readUnsignedShort();
							int enc = in.readInt();

							if (enc == 0) {
								int[] pixels = new int[rw * rh];
								for (int p = 0; p < pixels.length; p++) {
									int r = in.readUnsignedByte();
									int g = in.readUnsignedByte();
									int b = in.readUnsignedByte();
									in.readUnsignedByte();
									pixels[p] = (b << 16) | (g << 8) | r;
								}
								if (rx + rw <= width && ry + rh <= height) {
									img.setRGB(rx, ry, rw, rh, pixels, 0, rw);
									updated = true;
								}
							} else if (enc == -223) {
								width = rw;
								height = rh;
								img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
								absX = width / 2;
								absY = height / 2;
							}
						}

						if (updated) {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(img, "png", baos);
							ClientMod.vmTextureBytes = baos.toByteArray();
							ClientMod.vmTextureBytesSize = ClientMod.vmTextureBytes.length;
						}
						waitingForUpdate = false;
					} else if (msgType == 2) {
					} else if (msgType == 3) {
						in.skipBytes(3);
						int len = in.readInt();
						in.skipBytes(len);
					}
				} else {
					Thread.sleep(5);
				}
			}
			socket.close();
			System.out.println("VMware VNC: Disconnected.");
			vmUpdateThread = null;

		} catch (java.io.EOFException eof) {
			System.out.println("VMware VNC: Broadcast completed (PC turned off).");
			vmUpdateThread = null;
		} catch (Exception ex) {
			System.err.println("VMware VNC Error: " + ex.getMessage());
			vmUpdateThread = null;
		}
	}
	private void runVirtualBox() {
		Minecraft mcc = Minecraft.getInstance();
		long lastErrorLogTime = 0L;
		try {
			while (vmTurnedOn && !vmTurningOff) {
				ISession ns = null;
				boolean machineLocked = false;

				try {
					double deltaX = mouseCurX - mouseLastX;
					double deltaY = mouseCurY - mouseLastY;
					mouseLastX = mouseCurX;
					mouseLastY = mouseCurY;

					IMachine machine = vb.findMachine("VmComputersVm");
					if (machine.getState() == MachineState.PoweredOff) {
						if (!vmTurningOff) {
							System.out.println("VMComputers: VirtualBox VM is powered off, stopping screen capture.");
						}
						vmTurnedOn = false;
						vmEntityID = -1;
						return;
					}

					ns = vbManager.getSessionObject();
					machine.lockMachine(ns, LockType.Shared);
					machineLocked = true;

					IConsole console = ns.getConsole();
					if (mcc.screen instanceof GuiFocus) {
						int val = 0x00;
						if (leftMouseButton) val += 0x01;
						if (middleMouseButton) val += 0x04;
						if (rightMouseButton) val += 0x02;
						console.getMouse().putMouseEvent((int) deltaX, (int) deltaY, mouseDeltaScroll, 0, val);
					}

					if (releaseKeys) {
						try {
							java.util.List<Integer> releaseCodes = new java.util.ArrayList<>();
							int[] currentKeys = {
									newvmcomputers.client.ClientMod.glfwUnfocusKey1,
									newvmcomputers.client.ClientMod.glfwUnfocusKey2,
									newvmcomputers.client.ClientMod.glfwUnfocusKey3,
									newvmcomputers.client.ClientMod.glfwUnfocusKey4
							};

							for (int k : currentKeys) {
								if (k > 0) {
									java.util.List<Integer> codes = newvmcomputers.client.utils.KeyConverter.toVBKey(k, org.lwjgl.glfw.GLFW.GLFW_RELEASE);
									codes.removeIf(val -> val == 0x80 || val == 0x00);
									releaseCodes.addAll(codes);
								}
							}

							if (!releaseCodes.isEmpty()) {
								console.getKeyboard().putScancodes(releaseCodes);
							}
						} finally {
							synchronized (vmKeyboardScancodes) {
								vmKeyboardScancodes.clear();
							}
							releaseKeys = false;
						}
					} else {
						java.util.List<Integer> pressedScancodes;
						synchronized (vmKeyboardScancodes) {
							pressedScancodes = new java.util.ArrayList<>(vmKeyboardScancodes);
							vmKeyboardScancodes.clear();
						}
						if (!pressedScancodes.isEmpty()) {
							console.getKeyboard().putScancodes(pressedScancodes);
						}
					}

					Holder<Long> width = new Holder<>();
					Holder<Long> height = new Holder<>();
					Holder<Long> bitsPP = new Holder<>();
					Holder<Integer> xOrigin = new Holder<>();
					Holder<Integer> yOrigin = new Holder<>();
					Holder<GuestMonitorStatus> status = new Holder<>();
					console.getDisplay().getScreenResolution(0L, width, height, bitsPP, xOrigin, yOrigin, status);

					Long w = width.value;
					Long h = height.value;
					if (w != null && h != null && w > 0 && h > 0) {
						byte[] image = console.getDisplay().takeScreenShotToArray(0L, w, h, BitmapFormat.PNG);
						vmTextureBytesSize = image.length;
						vmTextureBytes = image;
					}
				} catch (Exception ex) {
					long now = System.currentTimeMillis();
					if (!vmTurningOff && now - lastErrorLogTime >= 1000L) {
						System.err.println("VMComputers: VirtualBox screen update failed: " + ex.getMessage());
						lastErrorLogTime = now;
					}
				} finally {
					if (ns != null && machineLocked) {
						try {
							ns.unlockMachine();
						} catch (Exception ignored) {
						}
					}
				}

				Thread.sleep(50L);
			}
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} finally {
			vmUpdateThread = null;
		}
	}
}


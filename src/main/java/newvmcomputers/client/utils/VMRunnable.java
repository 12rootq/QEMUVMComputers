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
import net.minecraft.client.MinecraftClient;

public class VMRunnable implements Runnable {

	@Override
	public void run() {
		if (ClientMod.useVmware) {
			runVmwareVNC();
		} else {
			runVirtualBox();
		}
	}

	// ----------------------------------------------------
	// НОВЫЙ БЛОК: Свой VNC Клиент для VMware
	// ----------------------------------------------------
	private void runVmwareVNC() {
		try {
			Thread.sleep(3000);

			System.out.println("VMware: Подключение к VNC (127.0.0.1:5900)...");
			Socket socket = new Socket("127.0.0.1", 5900);
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
			System.out.println("VMware VNC: Экран захвачен! Разрешение " + width + "x" + height);

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

			while (ClientMod.vmTurnedOn && !ClientMod.vmTurningOff) {
				// 1. Отправляем мышку ВСЕГДА, не блокируя поток
				if (MinecraftClient.getInstance().currentScreen instanceof GuiFocus) {
					if (ClientMod.mouseCurX != ClientMod.mouseLastX || ClientMod.mouseCurY != ClientMod.mouseLastY || ClientMod.leftMouseButton || ClientMod.rightMouseButton || ClientMod.middleMouseButton) {
						absX += (ClientMod.mouseCurX - ClientMod.mouseLastX);
						absY += (ClientMod.mouseCurY - ClientMod.mouseLastY);
						absX = Math.max(0, Math.min(width - 1, absX));
						absY = Math.max(0, Math.min(height - 1, absY));

						ClientMod.mouseLastX = ClientMod.mouseCurX;
						ClientMod.mouseLastY = ClientMod.mouseCurY;

						int mask = 0;
						if (ClientMod.leftMouseButton) mask |= 1;
						if (ClientMod.middleMouseButton) mask |= 2;
						if (ClientMod.rightMouseButton) mask |= 4;

						out.writeByte(5); // PointerEvent
						out.writeByte(mask);
						out.writeShort(absX);
						out.writeShort(absY);
						out.flush();
					}
				}

				if (ClientMod.releaseKeys) {
					ClientMod.releaseKeys = false;
					ClientMod.vmKeyboardScancodes.clear();
				}

				// 2. Запрашиваем новый кадр (только если не ждем предыдущий)
				if (!waitingForUpdate && (System.currentTimeMillis() - lastRequest > 50)) {
					out.writeByte(3); // FramebufferUpdateRequest
					out.writeByte(1); // Incremental (только изменения)
					out.writeShort(0);
					out.writeShort(0);
					out.writeShort(width);
					out.writeShort(height);
					out.flush();
					waitingForUpdate = true;
					lastRequest = System.currentTimeMillis();
				}

				// 3. Читаем ответ АСИНХРОННО (только если данные уже пришли!)
				if (in.available() > 0) {
					int msgType = in.readUnsignedByte();
					if (msgType == 0) { // Пришел новый кадр
						in.readByte(); // padding
						int numRects = in.readUnsignedShort();
						boolean updated = false;

						for (int i = 0; i < numRects; i++) {
							int rx = in.readUnsignedShort();
							int ry = in.readUnsignedShort();
							int rw = in.readUnsignedShort();
							int rh = in.readUnsignedShort();
							int enc = in.readInt();

							if (enc == 0) { // RAW пиксели
								int[] pixels = new int[rw * rh];
								for (int p = 0; p < pixels.length; p++) {
									int r = in.readUnsignedByte();
									int g = in.readUnsignedByte();
									int b = in.readUnsignedByte();
									in.readUnsignedByte(); // alpha padding
									pixels[p] = (r << 16) | (g << 8) | b;
								}
								if (rx + rw <= width && ry + rh <= height) {
									img.setRGB(rx, ry, rw, rh, pixels, 0, rw);
									updated = true;
								}
							} else if (enc == -223) { // DesktopSize (Смена разрешения)
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
						// Кадр получен, можем запрашивать следующий
						waitingForUpdate = false;
					} else if (msgType == 2) {
						// Bell
					} else if (msgType == 3) {
						// ServerCutText
						in.skipBytes(3);
						int len = in.readInt();
						in.skipBytes(len);
					}
				} else {
					// Небольшая пауза, чтобы не перегружать процессор
					Thread.sleep(5);
				}
			}

			socket.close();
			System.out.println("VMware VNC: Отключено.");
			vmUpdateThread = null;

		} catch (java.io.EOFException eof) {
			// Игнорируем эту ошибку, она означает, что ВМ просто выключилась
			System.out.println("VMware VNC: Трансляция завершена (ПК выключен).");
			vmUpdateThread = null;
		} catch (Exception ex) {
			System.err.println("VMware VNC Ошибка: " + ex.getMessage());
			vmUpdateThread = null;
		}
	}

	// ----------------------------------------------------
	// СТАРЫЙ БЛОК: VirtualBox (оставляем, чтобы ничего не сломать)
	// ----------------------------------------------------
	private void runVirtualBox() {
		MinecraftClient mcc = MinecraftClient.getInstance();
		while(true) {
			try {
				double deltaX = 0;
				double deltaY = 0;

				deltaX = mouseCurX - mouseLastX;
				deltaY = mouseCurY - mouseLastY;
				mouseLastX = mouseCurX;
				mouseLastY = mouseCurY;

				IMachine m = vb.findMachine("VmComputersVm");
				if(m.getState() == MachineState.PoweredOff) {
					if(!vmTurningOff && vmTurnedOn) {
						IProgress pr = m.launchVMProcess(vbManager.getSessionObject(), "headless", List.of());
						pr.waitForCompletion(-1);
					}else {
						vmUpdateThread = null;
						return;
					}
				}
				ISession ns = vbManager.getSessionObject();
				m.lockMachine(ns, LockType.Shared);
				IConsole console = ns.getConsole();
				if(mcc.currentScreen instanceof GuiFocus) {
					int val = 0x00;
					if(leftMouseButton) {
						val += 0x01;
					}
					if(middleMouseButton) {
						val += 0x04;
					}
					if(rightMouseButton) {
						val += 0x02;
					}
					console.getMouse().putMouseEvent((int)deltaX, (int)deltaY, mouseDeltaScroll, 0, val);
				}
				if(releaseKeys) {
					try {
						java.util.List<Integer> releaseCodes = new java.util.ArrayList<>();
						int[] currentKeys = { newvmcomputers.client.ClientMod.glfwUnfocusKey1,
								newvmcomputers.client.ClientMod.glfwUnfocusKey2,
								newvmcomputers.client.ClientMod.glfwUnfocusKey3,
								newvmcomputers.client.ClientMod.glfwUnfocusKey4 };

						for(int k : currentKeys) {
							if(k > 0) {
								java.util.List<Integer> codes = newvmcomputers.client.utils.KeyConverter.toVBKey(k, org.lwjgl.glfw.GLFW.GLFW_RELEASE);
								codes.removeIf(val -> val == 0x80 || val == 0x00);
								releaseCodes.addAll(codes);
							}
						}

						if (!releaseCodes.isEmpty()) {
							console.getKeyboard().putScancodes(releaseCodes);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						vmKeyboardScancodes.clear();
						releaseKeys = false;
					}
				}else {
					console.getKeyboard().putScancodes(vmKeyboardScancodes);
					vmKeyboardScancodes.clear();
				}
				Holder<Long> width = new Holder<Long>();
				Holder<Long> height = new Holder<Long>();
				Holder<Long> bitsPP = new Holder<Long>();
				Holder<Integer> xOrigin = new Holder<Integer>();
				Holder<Integer> yOrigin = new Holder<Integer>();
				Holder<GuestMonitorStatus> status = new Holder<GuestMonitorStatus>();
				console.getDisplay().getScreenResolution(0L, width, height, bitsPP, xOrigin, yOrigin, status);
				Long w = width.value;
				Long h = height.value;
				byte[] image = null;
				try {
					image = console.getDisplay().takeScreenShotToArray(0L, w, h, BitmapFormat.PNG);
				}catch(Exception ex) {
					ns.unlockMachine();
					continue;
				}
				ns.unlockMachine();
				vmTextureBytesSize = image.length;
				vmTextureBytes = image;
			}catch(Exception ex) {}
		}
	}
}
package newvmcomputers.client.gui;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import newvmcomputers.client.ClientMod;
import newvmcomputers.client.utils.KeyConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

public class GuiFocus extends Screen {
	private String keyString;
	private List<Integer> keys;
	
	private final Set<Integer> pressedKeys = new HashSet<>();
	private final Language lang = Language.getInstance();
	private final Minecraft minecraft = Minecraft.getInstance();

	public GuiFocus() {
		super(Component.translatable("Focus"));
	}

	@Override
	protected void init() {
		ClientMod.releaseKeys = false;

		int[] unfocusKeys = new int[] {
				ClientMod.glfwUnfocusKey1,
				ClientMod.glfwUnfocusKey2,
				ClientMod.glfwUnfocusKey3,
				ClientMod.glfwUnfocusKey4
		};

		keys = new ArrayList<>();
		pressedKeys.clear(); 

		for (int key : unfocusKeys) {
			if (key > 0) {
				keys.add(key);
			}
		}

		StringBuilder sb = new StringBuilder();
		boolean plus = false;
		for (int key : keys) {
			if (plus) {
				sb.append(" + ");
			}
			sb.append(ClientMod.getKeyName(key));
			plus = true;
		}
		keyString = sb.toString();

		Timer serverAddressTimer = new Timer();
		class CheckAddress extends TimerTask {
			@Override
			public void run() {
				if (minecraft.getWindow() == null) return;
				long window = minecraft.getWindow().getWindow();

				if (minecraft.getCurrentServer() == null && !minecraft.hasSingleplayerServer()) {
					GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
					serverAddressTimer.cancel();
				}
			}
		}
		serverAddressTimer.schedule(new CheckAddress(), 0, 1000);
	}

	
	private void queueVmKeyAction(int keyCode, int action) {
		List<Integer> scanCodes = KeyConverter.toVBKey(keyCode, action);
		scanCodes.removeIf(code -> code == 0x00 || code == 0x80);
		if (scanCodes.isEmpty()) {
			return;
		}

		synchronized (ClientMod.vmKeyboardScancodes) {
			ClientMod.vmKeyboardScancodes.addAll(scanCodes);
		}
	}

	private boolean tryUnfocus() {
		if (keys == null || keys.isEmpty()) {
			return false; 
		}

		for (int key : keys) {
			if (!pressedKeys.contains(key)) {
				return false; 
			}
		}

		
		minecraft.setScreen(null);
		return true;
	}

	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		boolean firstPress = pressedKeys.add(keyCode);
		if (tryUnfocus()) {
			return true;
		}
		if (firstPress) {
			queueVmKeyAction(keyCode, GLFW.GLFW_PRESS);
		}
		return true;
	}

	
	@Override
	public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
		pressedKeys.remove(keyCode);
		queueVmKeyAction(keyCode, GLFW.GLFW_RELEASE);
		return true;
	}

	@Override
	public void render(GuiGraphics context, int mouseXGUI, int mouseYGUI, float delta) {
		long window = minecraft.getWindow().getWindow();
		DoubleBuffer mX = BufferUtils.createDoubleBuffer(1);
		DoubleBuffer mY = BufferUtils.createDoubleBuffer(1);
		GLFW.glfwGetCursorPos(window, mX, mY);
		double mouseX = mX.get();
		double mouseY = mY.get();
		mX.clear();
		mY.clear();

		ClientMod.mouseCurX = mouseX;
		ClientMod.mouseCurY = mouseY;
		ClientMod.leftMouseButton = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		ClientMod.middleMouseButton = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS;
		ClientMod.rightMouseButton = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

		context.drawString(this.font, lang.getOrDefault("newvmcomputers.focus.lose").replace("%s", keyString), 4, 4, -1, false);
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

		

		super.render(context, mouseXGUI, mouseYGUI, delta);
	}

	@Override
	public void removed() {
		for (int key : new ArrayList<>(pressedKeys)) {
			queueVmKeyAction(key, GLFW.GLFW_RELEASE);
		}
		ClientMod.releaseKeys = false;
		pressedKeys.clear();
		ClientMod.leftMouseButton = false;
		ClientMod.middleMouseButton = false;
		ClientMod.rightMouseButton = false;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}


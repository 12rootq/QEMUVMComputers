package mcvmcomputers.client.gui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;


import java.nio.DoubleBuffer;
import java.util.ArrayList;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import mcvmcomputers.client.ClientMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;

/**
 * The virtual machine "focus" screen.
 *
 * <p>While this screen is open and the VM is on, keyboard and mouse input is
 * redirected to the guest OS (see {@link mcvmcomputers.mixins.KeyboardMixin} and
 * {@link mcvmcomputers.mixins.MouseMixin}) instead of controlling the Minecraft
 * player. The screen captures the cursor and shows the VM screen frame; leaving is
 * done with the configured unfocus key combination ({@code ClientMod.glfwUnfocusKey*}).</p>
 */
public class GuiFocus extends net.minecraft.client.gui.screens.Screen{
	private String keyString;
	private ArrayList<Integer> keys;
	private final Language lang = Language.getInstance();
	private Minecraft minecraft = Minecraft.getInstance();


	public GuiFocus() {
		super(Component.translatable("Focus"));
	}

	@Override
	protected void init() {
		ClientMod.releaseKeys = false;

		keyString = "";

		int[] unfocusKeys = new int[] {ClientMod.glfwUnfocusKey1,
									   ClientMod.glfwUnfocusKey2,
									   ClientMod.glfwUnfocusKey3,
									   ClientMod.glfwUnfocusKey4};

		keys = new ArrayList<>();
		for(int key : unfocusKeys) {
			if(key > 0) {
				keys.add(key);
			}
		}

		boolean plus = false;
		for(int key : keys) {
			if(plus) {
				keyString += " + ";
			}
			keyString += ClientMod.getKeyName(key);
			plus = true;
		}


	}

	@Override
	public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta) {
	}

	@Override
	public void render(GuiGraphics context, int wmouseX, int wmouseY, float delta) {
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
		// Poll real-time held state (covers holding a button across frames). The press
		// latch itself is set by MouseMixin's event handler so fast clicks aren't lost.
		int mask = 0;
		if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) mask |= 0x01;
		if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) mask |= 0x02;
		if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS) mask |= 0x04;
		ClientMod.mouseButtonMask = mask;

		context.drawString(this.font, lang.getOrDefault("mcvmcomputers.focus.lose").replace("%s", keyString), 4, 4, -1);
		if (GLFW.glfwGetInputMode(window, GLFW.GLFW_CURSOR) != GLFW.GLFW_CURSOR_DISABLED) {
			GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
		}

		boolean pressed = true;
		for(int key : keys) {
			if(GLFW.glfwGetKey(window, key) == GLFW.GLFW_RELEASE) {
				pressed = false;
				break;
			}
		}

		if(pressed) {
			minecraft.setScreen(null);
		}

		super.render(context, wmouseX, wmouseY, delta);
	}



	@Override
	public void removed() {
		ClientMod.releaseKeys = true;
		long window = minecraft.getWindow().getWindow();
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
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

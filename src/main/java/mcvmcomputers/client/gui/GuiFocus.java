package mcvmcomputers.client.gui;

import java.nio.DoubleBuffer;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;

import mcvmcomputers.client.ClientMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Language;

public class GuiFocus extends Screen {
	private String keyString;
	private ArrayList<Integer> keys;
	private final Language lang = Language.getInstance();
	private final MinecraftClient minecraft = MinecraftClient.getInstance();

	public GuiFocus() {
		super(Text.translatable("Focus"));
	}

	@Override
	protected void init() {
		ClientMod.releaseKeys = false;

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

		StringBuilder sb = new StringBuilder();
		boolean plus = false;
		for(int key : keys) {
			if(plus) {
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
				long window = minecraft.getWindow().getHandle();

				// Если мы не на сервере или в одиночной игре, освобождаем курсор
				if (minecraft.getCurrentServerEntry() == null && !minecraft.isInSingleplayer()) {
					GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
					serverAddressTimer.cancel();
				}
			}
		}
		serverAddressTimer.schedule(new CheckAddress(), 0, 1000);
	}

	@Override
	public void render(DrawContext context, int mouseXGUI, int mouseYGUI, float delta) {
		long window = minecraft.getWindow().getHandle();
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

		context.drawText(this.textRenderer, lang.get("mcvmcomputers.focus.lose").replace("%s", keyString), 4, 4, -1, false);
		GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);

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

		super.render(context, mouseXGUI, mouseYGUI, delta);
	}

	@Override
	public void removed() {
		ClientMod.releaseKeys = true;
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
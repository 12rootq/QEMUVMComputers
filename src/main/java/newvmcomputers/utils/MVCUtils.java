package newvmcomputers.utils;

import org.joml.Quaternionf;
import net.minecraft.world.phys.Vec3;

public class MVCUtils {
	public static Quaternionf lookAt(Vec3 src, Vec3 dest) {
		Vec3 forward = new Vec3(0, 0, 1);
		Vec3 fwd = src.subtract(dest).normalize();
		double dot = forward.dot(fwd);
		Vec3 up = new Vec3(0, 1, 0);

		if (Math.abs(dot - (-1.0f)) < 0.000001f) {
			return new Quaternionf().fromAxisAngleRad(
					(float) up.x,
					(float) up.y,
					(float) up.z,
					(float) Math.PI
			);
		}

		if (Math.abs(dot - (1.0f)) < 0.000001f) {
			return new Quaternionf(); 
		}

		double rotAngle = Math.acos(dot);
		Vec3 rotAxis = forward.cross(fwd).normalize();
		return createFromAxisAngle(rotAxis, rotAngle);
	}

	public static Quaternionf createFromAxisAngle(Vec3 axis, double angle) {
		return new Quaternionf().fromAxisAngleRad(
				(float) axis.x,
				(float) axis.y,
				(float) axis.z,
				(float) angle
		);
	}

	public static float lerp(float a, float b, float t) {
		return a + (b - a) * t;
	}

	public static double lerp(double a, double b, double t) {
		return a + (b - a) * t;
	}

	public static final char COLOR_CHAR = (char) (0xfeff00a7);

	public static String getColorChar(char color) {
		return COLOR_CHAR + "" + color;
	}
}


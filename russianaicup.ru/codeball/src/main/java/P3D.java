import model.Ball;
import model.Robot;

import java.text.DecimalFormat;

/**
 * @author Vasilii Stepanov.
 * @since 20.12.2018
 */
public class P3D {
	private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

	private double x;
	private double y;
	private double z;

	static P3D zero = new P3D(0, 0, 0);

	public P3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public P3D(Robot robot) {
		this(robot.x, robot.y, robot.z);
	}

	public P3D(Ball ball) {
		this(ball.x, ball.y, ball.z);
	}

	public P3D plus(Vec3D vector) {
		return new P3D(x + vector.getX(), y + vector.getY(), z + vector.getZ());
	}

	public double distanceTo(P3D other) {
		double dx = (x - other.x);
		double dy = (y - other.y);
		double dz = (y - other.y);
		return FastMath.hypot(dx, dy, dz);
	}

	public String toString() {
		return DECIMAL_FORMAT.format(x) + "," + DECIMAL_FORMAT.format(y) + "," + DECIMAL_FORMAT.format(z);
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getZ() {
		return z;
	}
}

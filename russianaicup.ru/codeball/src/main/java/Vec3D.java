import model.Action;
import model.Ball;
import model.Game;
import model.Robot;

/**
 * @author Vasilii Stepanov.
 * @since 20.12.2018
 */
public class Vec3D {
	private final double NOT_INITIALIZED_ANGLE = 10000;

	private double x;
	private double y;
	private double z;
	private double angle = NOT_INITIALIZED_ANGLE;

	public Vec3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3D(Vec3D v) {
		this(v.x, v.y, v.z);
	}

	public Vec3D(P3D start, P3D end) {
		this.x = end.getX() - start.getX();
		this.y = end.getY() - start.getY();
		this.z = end.getZ() - start.getZ();
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

	public double getAngle() {
		return angle;
	}

	public static Vec3D velocity(Robot r) {
		return new Vec3D(r.velocity_x, r.velocity_y, r.velocity_z);
	}

    public static Vec3D velocity(Ball ball) {
        return new Vec3D(ball.velocity_x, ball.velocity_y, ball.velocity_z);
    }

    public static Vec3D touch(Robot r) {
		return new Vec3D(r.touch_normal_x, r.touch_normal_y, r.touch_normal_z);
	}

	public double length() {
		return FastMath.hypot(x, y, z);
	}

	public Vec3D setLength(double length) {
		if (length() == 0) {
			if (length == 0) {
				return new Vec3D(this);
			} else {
				throw new IllegalArgumentException("Unable to set non zero length to vector with length = 0!");
			}
		}
		Vec3D res = unit().multiply(length);
		x = res.x;
		y = res.y;
		return res;
	}

	public Vec3D unit() {
		double length = length();
		return new Vec3D(x / length, y / length, z / length);
	}

	public Vec3D multiply(double scale) {
		return new Vec3D(x * scale, y * scale, z * scale);
	}

	public void apply(Action action) {
		action.target_velocity_x = x;
		action.target_velocity_y = y;
		action.target_velocity_z = z;
	}
}

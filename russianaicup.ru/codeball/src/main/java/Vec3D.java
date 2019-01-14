import model.Action;
import model.Ball;
import model.Game;
import model.Robot;

import java.util.Objects;

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

	public static Vec3D zero = new Vec3D(0, 0, 0);

	public Vec3D(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vec3D(Vec3D v) {
		this(v.x, v.y, v.z);
	}

	public Vec3D(Robot robot) {
		this(robot.x, robot.y, robot.z);
	}

	public Vec3D(Ball ball) {
		this(ball.x, ball.y, ball.z);
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

	public double groundLength() {
		return FastMath.hypot(x, 0, z);
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

	public Vec3D unitGround() {
		double length = length();
		return new Vec3D(x / length, 0, z / length);
	}

	public Vec3D multiply(double scale) {
		return new Vec3D(x * scale, y * scale, z * scale);
	}

	public void apply(Action action) {
		action.target_velocity_x = x;
		action.target_velocity_y = y;
		action.target_velocity_z = z;
	}

	public double dot(Vec3D other) {
		return x*other.getX() + y * other.getY() + z * other.getZ();
	}

	/**
	 * @param other another vector
	 * @return angle in radians in range [-PI, PI] on which current vector should be rotated
	 * to have the same direction as <b>other</b> vector.
	 */
	public double angleToVector(Vec3D other) {
		double a1 = angle();
		double a2 = other.angle();

		return FastMath.normalizeAngle(a2 - a1);
	}

	/**
	 * @return angle of vector in radians in range [-PI, PI)
	 */
	public double angle() {
		if (angle < NOT_INITIALIZED_ANGLE) {
			return angle;
		}

		double resAngle = FastMath.atan2(this.y, this.x);

		angle = FastMath.normalizeAngle(resAngle);
		return angle;
	}

	public Vec3D plus(Vec3D other) {
		return new Vec3D(x + other.x, y + other.y, z + other.z);
	}

	public Vec3D minus(Vec3D other) {
		return this.plus(other.negate());
	}

	public Vec3D negate() {
		return new Vec3D(-x, -y, -z);
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public void setZ(double z) {
		this.z = z;
	}

	public String toString() {
		return "[x=" + x + "][y=" + y + "][z=" + z + "]";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vec3D vec3D = (Vec3D) o;
		return Double.compare(vec3D.x, x) == 0 &&
				Double.compare(vec3D.y, y) == 0 &&
				Double.compare(vec3D.z, z) == 0 &&
				Double.compare(vec3D.angle, angle) == 0;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y, z, angle);
	}
}

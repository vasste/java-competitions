import java.io.Serializable;

public class Vec2D implements Cloneable, Serializable {
    private final double NOT_INITIALIZED_ANGLE = 10000;

    public static Vec2D fromAngleAndLength(double angle, double length) {
        double x = length * Math.cos(angle);
        double y = length * Math.sin(angle);

        Vec2D res = new Vec2D(x, y);
        res.angle = FastMath.normalizeAngle(angle);
        return res;
    }

    private double x;
    private double y;
    private double angle = NOT_INITIALIZED_ANGLE;

    public Vec2D() {
        x = 0;
        y = 0;
    }

    public Vec2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2D(Vec2D other) {
        this.x = other.x;
        this.y = other.y;
        this.angle = other.angle;
    }

    public Vec2D(P2D start, P2D end) {
        this.x = end.getX() - start.getX();
        this.y = end.getY() - start.getY();
    }

    public Vec2D(P2D end) {
        this.x = end.getX();
        this.y = end.getY();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Vec2D setLength(double length) {
        if (length() == 0) {
            if (length == 0) {
                return new Vec2D(this);
            } else {
                throw new IllegalArgumentException("Unable to set non zero length to vector with length = 0!");
            }
        }
        Vec2D res = unit().multiply(length);
        x = res.x;
        y = res.y;
        return res;
    }

    public double length() {
        return FastMath.hypot(x, y);
    }

    public boolean isZero() {
        return FastMath.equals(x, 0) && FastMath.equals(y, 0);
    }

    public Vec2D unit() {
        double length = length();
        return new Vec2D(x / length, y / length);
    }

    public Vec2D multiply(double scale) {
        return new Vec2D(x * scale, y * scale);
    }

    public double scalarMultiply(Vec2D other) {
        double len1 = length();
        double len2 = other.length();
        return len1 * len2 * FastMath.cos(angleToVector(other));
    }

    public Vec2D plus(Vec2D other) {
        return new Vec2D(x + other.x, y + other.y);
    }

    public Vec2D plus(double dx, double dy) {
        return new Vec2D(x + dx, y + dy);
    }

    public Vec2D minus(Vec2D other) {
        return this.plus(other.negate());
    }

    public Vec2D negate() {
        return new Vec2D(-x, -y);
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

    /**
     * @param other another vector
     * @return angle in radians in range [-PI, PI] on which current vector should be rotated
     * to have the same direction as <b>other</b> vector.
     */
    public double angleToVector(Vec2D other) {
        double a1 = angle();
        double a2 = other.angle();

        return FastMath.normalizeAngle(a2 - a1);
    }

    public double angleTo(double angle) {
        return FastMath.normalizeAngle(angle - angle());
    }

    public Vec2D rotate(double dAngle) {
        double newAngle = angle() + dAngle;
        return fromAngleAndLength(newAngle, length());
    }

    @Override
    public Vec2D clone() {
        return new Vec2D(this);
    }


    public String toString() {
        return "[x=" + x + "][y=" + y + "]";
    }
}
import java.io.Serializable;

public class Vector2D implements Cloneable, Serializable {
    private final double NOT_INITIALIZED_ANGLE = 10000;

    public static Vector2D fromAngleAndLength(double angle, double length) {
        double x = length * Math.cos(angle);
        double y = length * Math.sin(angle);

        Vector2D res = new Vector2D(x, y);
        res.angle = Utils.normalizeAngle(angle);
        return res;
    }

    private double x;
    private double y;
    private double angle = NOT_INITIALIZED_ANGLE;

    public Vector2D() {
        x = 0;
        y = 0;
    }

    public Vector2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2D(Vector2D other) {
        this.x = other.x;
        this.y = other.y;
        this.angle = other.angle;
    }

    public Vector2D(Point start, Point end) {
        this.x = end.getX() - start.getX();
        this.y = end.getY() - start.getY();
    }

    public Vector2D(Point end) {
        this.x = end.getX();
        this.y = end.getY();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Vector2D setLength(double length) {
        if (length() == 0) {
            if (length == 0) {
                return new Vector2D(this);
            } else {
                throw new IllegalArgumentException("Unable to set non zero length to vector with length = 0!");
            }
        }
        Vector2D res = unit().multiply(length);
        x = res.x;
        y = res.y;
        return res;
    }

    public double length() {
        return Utils.hypot(x, y);
    }

    public boolean isZero() {
        return Utils.equals(x, 0) && Utils.equals(y, 0);
    }

    public Vector2D unit() {
        double length = length();
        return new Vector2D(x / length, y / length);
    }

    public Vector2D multiply(double scale) {
        return new Vector2D(x * scale, y * scale);
    }

    public double scalarMultiply(Vector2D other) {
        double len1 = length();
        double len2 = other.length();
        return len1 * len2 * FastMath.cos(angleToVector(other));
    }

    public Vector2D plus(Vector2D other) {
        return new Vector2D(x + other.x, y + other.y);
    }

    public Vector2D plus(double dx, double dy) {
        return new Vector2D(x + dx, y + dy);
    }

    public Vector2D minus(Vector2D other) {
        return this.plus(other.negate());
    }

    public Vector2D negate() {
        return new Vector2D(-x, -y);
    }

    /**
     * @return angle of vector in radians in range [-PI, PI)
     */
    public double angle() {
        if (angle < NOT_INITIALIZED_ANGLE) {
            return angle;
        }

        double resAngle = FastMath.atan2(this.y, this.x);

        angle = Utils.normalizeAngle(resAngle);
        return angle;
    }

    /**
     * @param other another vector
     * @return angle in radians in range [-PI, PI] on which current vector should be rotated
     * to have the same direction as <b>other</b> vector.
     */
    public double angleToVector(Vector2D other) {
        double a1 = angle();
        double a2 = other.angle();

        return Utils.normalizeAngle(a2 - a1);
    }

    public double angleTo(double angle) {
        return Utils.normalizeAngle(angle - angle());
    }

    public Vector2D rotate(double dAngle) {
        double newAngle = angle() + dAngle;
        return fromAngleAndLength(newAngle, length());
    }

    @Override
    public Vector2D clone() {
        return new Vector2D(this);
    }


    public String toString() {
        return "[x=" + x + "][y=" + y + "]";
    }
}
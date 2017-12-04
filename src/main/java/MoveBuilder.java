import model.ActionType;
import model.Move;
import model.VehicleType;

public class MoveBuilder {
    private ActionType action;

    private int group;

    private double left;
    private double top;
    private double right;
    private double bottom;

    private double x;
    private double y;
    private double angle;
    private double factor;

    private double maxSpeed;
    private double maxAngularSpeed;

    private VehicleType vehicleType;

    private long facilityId = -1L;
    private long vehicleId = -1L;

    static MoveBuilder c(ActionType action) {
        return new MoveBuilder(action);
    }

    public MoveBuilder() { }
    public MoveBuilder(ActionType action) { this.action = action; }

    MoveBuilder setAction(ActionType action) { this.action = action; return this;}
    MoveBuilder group(int group) { this.group = group; return this; }
    MoveBuilder left(double left) { this.left = left; return this; }
    MoveBuilder top(double top) { this.top = top;return this; }
    MoveBuilder right(double right) { this.right = right;  return this; }
    MoveBuilder bottom(double bottom) { this.bottom = bottom; return this; }
    MoveBuilder x(double x) { this.x = x; return this; }
    MoveBuilder y(double y) { this.y = y; return this; }
    MoveBuilder dfCToXY(P2D current, double x, double y) {
        this.y = y - current.y;
        this.x = x - current.x;
        return this;
    }
    MoveBuilder dfCToXY(Rectangle current, double x, double y) {
        this.y = y - current.cY();
        this.x = x - current.cX();
        return this;
    }
    MoveBuilder angle(double angle) { this.angle = angle; return this; }
    MoveBuilder factor(double factor) { this.factor = factor; return this; }
    MoveBuilder maxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; return this; }
    MoveBuilder maxAngularSpeed(double maxAngularSpeed) { this.maxAngularSpeed = maxAngularSpeed; return this; }
    MoveBuilder vehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; return this; }
    MoveBuilder facilityId(long facilityId) { this.facilityId = facilityId; return this; }
    MoveBuilder vehicleId(long vehicleId) { this.vehicleId = vehicleId; return this; }
    Move setMove(Move dst) {
        dst.setGroup(group);

        dst.setLeft(left);
        dst.setBottom(bottom);
        dst.setTop(top);
        dst.setRight(right);

        dst.setAction(action);
        dst.setX(x);
        dst.setY(y);

        dst.setAngle(angle);
        dst.setFactor(factor);
        dst.setFacilityId(facilityId);

        dst.setMaxSpeed(maxSpeed);
        dst.setMaxAngularSpeed(maxAngularSpeed);

        dst.setVehicleType(vehicleType);
        dst.setVehicleId(vehicleId);
        return dst;
    }

    MoveBuilder setRect(Rectangle rectangle) {
        left(rectangle.l);
        right(rectangle.r);
        top(rectangle.t);
        bottom(rectangle.b);
        return this;
    }

    @Override
    public String toString() {
        return "MB{" +
                "a=" + action +
                ", g=" + group +
                ", l=" + left +
                ", t=" + top +
                ", r=" + right +
                ", b=" + bottom +
                ", x=" + x +
                ", y=" + y +
                ", a=" + angle +
                ", f=" + factor +
                ", mS=" + maxSpeed +
                ", mAS=" + maxAngularSpeed +
                ", vt=" + vehicleType +
                ", fId=" + facilityId +
                ", vId=" + vehicleId +
                '}';
    }
}

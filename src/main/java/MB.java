import model.ActionType;
import model.Move;
import model.VehicleType;

public class MB {
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

    static MB c(ActionType action) { return new MB(action); }

    public MB() { }
    public MB(ActionType action) { this.action = action; }

    MB setAction(ActionType action) { this.action = action; return this;}
    MB group(int group) { this.group = group; return this; }
    MB left(double left) { this.left = left; return this; }
    MB top(double top) { this.top = top;return this; }
    MB right(double right) { this.right = right;  return this; }
    MB bottom(double bottom) { this.bottom = bottom; return this; }
    MB x(double x) { this.x = x; return this; }
    MB y(double y) { this.y = y; return this; }
    MB dfCToXY(Rect current, double x, double y) {
        this.y = y - current.cY();
        this.x = x - current.cX();
        return this;
    }
    MB angle(double angle) { this.angle = angle; return this; }
    MB factor(double factor) { this.factor = factor; return this; }
    MB maxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; return this; }
    MB maxAngularSpeed(double maxAngularSpeed) { this.maxAngularSpeed = maxAngularSpeed; return this; }
    MB vehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; return this; }
    MB facilityId(long facilityId) { this.facilityId = facilityId; return this; }
    MB vehicleId(long vehicleId) { this.vehicleId = vehicleId; return this; }
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

    MB setRect(Rect rect) {
        left(rect.l);
        right(rect.r);
        top(rect.t);
        bottom(rect.b);
        return this;
    }
}

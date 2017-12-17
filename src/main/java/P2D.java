import model.Facility;
import model.Unit;
import model.Vehicle;
import model.World;

import java.util.Objects;

import static java.lang.StrictMath.hypot;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.round;

public class P2D implements Comparable<P2D> {
    double x;
    double y;
    long id;
    int tick;
    double speed;

    static final P2D Z = new P2D(0, 0);

    public P2D() {
        this(0,0);
    }

    P2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    P2D(double x, double y, int tick) {
        this.x = x;
        this.y = y;
        this.tick = tick;
    }

    public P2D(double x, double y, int tick, double speed) {
        this.x = x;
        this.y = y;
        this.tick = tick;
        this.speed = speed;
    }

    P2D(P2D p) {
        this.x = p.x;
        this.y = p.y;
        this.id = p.id;
        this.tick = p.tick;
    }

    P2D(Vehicle vehicle) {
        this.x = vehicle.getX();
        this.y = vehicle.getY();
        this.id = vehicle.getId();
    }

    P2D(VehicleTick vehicle) {
        this.x = vehicle.x();
        this.y = vehicle.y();
        this.id = vehicle.v.getId();
    }

    P2D(Facility facility) {
        this.x = facility.getLeft() + U.PALE_SIDE;
        this.y = facility.getTop() + U.PALE_SIDE;
    }

    @Override
    public int compareTo(P2D o) {
        int is = U.cD(speed,o.speed);
        if (is == 0) {
            int ix = U.cD(x, o.x);
            if (ix == 0) return U.cD(y, o.y);
            return ix;
        } else return is;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        P2D p2D = (P2D) o;
        return Double.compare(p2D.x, x) == 0 &&
                Double.compare(p2D.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    boolean less(P2D p) { return compareTo(p) < 0; }

    @Override
    public String toString() {
        return "(" + round(x) + "," + round(y) + ")";
    }

    int[] inWorld(World world, int factor) {
        int xx = (int) StrictMath.floor(StrictMath.min(world.getWidth()/U.PALE_SIDE/factor, x/U.PALE_SIDE/factor));
        int yy = (int) StrictMath.floor(StrictMath.min(world.getHeight()/U.PALE_SIDE/factor, y/U.PALE_SIDE/factor));
        return new int[]{Math.min(xx, U.PALE_SIDE/factor - 1), Math.min(yy, U.PALE_SIDE/factor - 1)};
    }

    P2D add(double radius) {
        return new P2D(x + radius, y + radius);
    }

    static double det(double a, double b, double c, double d) { return a*d - b*c; }
    static double distanceTo(P2D a, P2D b) { return hypot(a.x - b.x, a.y - b.y); }
    static double distanceTo(P2D a, Unit b) { return hypot(a.x - b.getX(), a.y - b.getY()); }
    static double distanceTo(P2D a, VehicleTick b) { return hypot(a.x - b.x(), a.y - b.y()); }
    static double distanceTo(VehicleTick a, VehicleTick b) { return hypot(a.x() - b.x(), a.y() - b.y()); }
    static double distanceTo(P2D a, Vehicle b) { return hypot(a.x - b.getX(), a.y - b.getY()); }

    static boolean intersect(P2D a1, P2D b1, P2D a2, P2D b2) { return intersect(new Line(a1, b1), new Line(a2, b2)); }
    static boolean intersect(P2D pa1, P2D pb1, P2D[] side) { return intersect(pa1, pb1, side[0], side[1]); }
    static boolean intersect(P2D[] aside, P2D[] bside) { return intersect(aside[0], aside[1], bside[0], bside[1]); }
    static boolean intersect(Line aside, Line bside) { return intersect(aside.ps, bside.ps); }

    static VehicleTick closedTo(VehicleTick a, VehicleTick b, P2D center) { return distanceTo(center, a) > distanceTo(center, b) ? b : a; }
    static P2D closedTo(P2D a, P2D b, P2D center) { return distanceTo(center, a) > distanceTo(center, b) ? b : a; }
    static Vehicle closedTo(Vehicle a, Vehicle b, P2D center) { return distanceTo(center, new P2D(a)) > distanceTo(center, new P2D(b)) ? b : a; }

    static Vehicle futherTo(Vehicle a, Vehicle b, P2D center) { return distanceTo(center, a) < distanceTo(center, b) ? b : a; }
    static VehicleTick futherTo(VehicleTick a, VehicleTick b, P2D center) { return distanceTo(center, a) < distanceTo(center, b) ? b : a; }

}

import model.Unit;
import model.Vehicle;
import model.World;

import static java.lang.StrictMath.hypot;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.round;

public class P2D implements Comparable<P2D> {
    double x;
    double y;

    public P2D() {
        this(0,0);
    }

    P2D(double x, double y) {
        this.x = x;
        this.y = y;
    }

    P2D(Vehicle vehicle) {
        this.x = vehicle.getX();
        this.y = vehicle.getY();
    }

    P2D(VeE vehicle) {
        this.x = vehicle.x();
        this.y = vehicle.y();
    }

    @Override
    public int compareTo(P2D o) {
        int ix = U.cD(x,o.x);
        if (ix == 0) return U.cD(y,o.y);
        return ix;
    }

    boolean less(P2D p) { return compareTo(p) < 0; }

    @Override
    public String toString() {
        return "(" + round(x) + "," + round(y) + ")";
    }

    int[] inWorld(World world) {
        return new int[]{(int)round(min(world.getWidth()/U.PALE_SIDE, x/U.PALE_SIDE)),
                (int)round(min(world.getHeight()/U.PALE_SIDE, y/U.PALE_SIDE))};
    }

    static double det(double a, double b, double c, double d) { return a*d - b*c; }
    static double distanceTo(P2D a, P2D b) { return hypot(a.x - b.x, a.y - b.y); }
    static double distanceTo(P2D a, Unit b) { return hypot(a.x - b.getX(), a.y - b.getX()); }
    static boolean intersect(P2D a1, P2D b1, P2D a2, P2D b2) { return intersect(new L(a1, b1), new L(a2, b2)); }
    static boolean intersect(P2D pa1, P2D pb1, P2D[] side) { return intersect(pa1, pb1, side[0], side[1]); }
    static boolean intersect(P2D[] aside, P2D[] bside) { return intersect(aside[0], aside[1], bside[0], bside[1]); }
    static boolean intersect(L aside, L bside) { return intersect(aside.ps, bside.ps); }
    public static P2D closedTo(P2D a, P2D b, P2D center) {
        if (distanceTo(center, a) > distanceTo(center, b)) return b;
        else return a;
    }
}

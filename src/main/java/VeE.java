import model.*;

import java.util.Arrays;

public class VeE implements Comparable<VeE> {
    final Vehicle v;
    final int tI;
    final int[] gs;

    VeE(Vehicle v, int tI) {
        this.v = v;
        this.tI = tI;
        this.gs = v.getGroups();
        Arrays.sort(this.gs);
    }

    VeE(VeE veEx, VehicleUpdate vu, int tI) {
        this(new Vehicle(veEx.v, vu), tI);
    }

    boolean ofVT(VehicleType type) { return v.getType() == type; }
    boolean ofVT(VehicleType[] types) {
        for (VehicleType type : types) if (v.getType() == type) return true;
        return false;
    }

    boolean isSelected() { return v.isSelected(); }
    boolean m(Player me) { return v.getPlayerId() == me.getId(); }
    boolean e(Player me) { return v.getPlayerId() != me.getId(); }
    boolean inG(int id) { return Arrays.binarySearch(gs, id) >= 0; }
    public String toString() { return "V{" + "g" + Arrays.toString(gs) + "}"; }
    VehicleType type() { return v.getType(); }
    long id() { return v.getId(); }
    boolean air() { return v.getType() == VehicleType.HELICOPTER || v.getType() == VehicleType.FIGHTER; }
    P2D point() { return new P2D(v.getX(), v.getY()); }
    double x() {return v.getX(); }
    double y() {return v.getY(); }
    double r() {return v.getRadius(); }
    double s() {return v.getMaxSpeed(); }
    boolean see(P2D p) { return P2D.distanceTo(p, v) < v.getVisionRange(); }
    boolean see(Unit u) { return u.getDistanceTo(v) < v.getVisionRange(); }
    boolean see(VeE p) { return P2D.distanceTo(p, this) < v.getVisionRange(); }
    boolean attack(VeE u) {
        if (type() == VehicleType.ARRV) return false;
        if (type() == VehicleType.FIGHTER && u.v.isAerial()) return P2D.distanceTo(u, this) <= v.getAerialAttackRange();
        double duthis = P2D.distanceTo(u, this);
        return duthis <= v.getAerialAttackRange() || duthis <= v.getGroundAttackRange();

    }
    double distanceTo(double x, double y) { return v.getDistanceTo(x, y); }

    @Override
    public int compareTo(VeE o) {
        int ix = U.cD(x(),o.x());
        if (ix == 0) return U.cD(y(),o.y());
        return ix;
    }
}

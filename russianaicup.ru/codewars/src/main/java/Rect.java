import model.Vehicle;
import model.VehicleType;
import model.World;

import static java.lang.Double.isNaN;
import static java.lang.StrictMath.*;
import static java.lang.StrictMath.hypot;
import static java.lang.StrictMath.round;

public class Rect implements Comparable<Rect> {
    public static Rect ORDER = new Rect(14,14,220,220);
    public double l = Double.NaN,t = Double.NaN,b = Double.NaN,r = Double.NaN;
    public VehicleType vt;
    public int g;

    Rect() {}
    Rect(World world) { this(0, 0, world.getHeight(), world.getWidth()); }
    Rect(double l, double t, double b, double r) { this.l = l;this.t = t;this.b = b;this.r = r; }
    Rect(Vehicle v) { this(v.getX() - v.getRadius(), v.getY() - v.getRadius(), v.getY() + v.getRadius(), v.getX() + v.getRadius()); }

    Rect update(Vehicle v) {
        l = StrictMath.min(isNaN(l) ? Double.MAX_VALUE : l, v.getX() - v.getRadius());
        r = StrictMath.max(isNaN(r) ? 0 : r, v.getX()  + v.getRadius());
        t = StrictMath.min(isNaN(t) ? Double.MAX_VALUE : t, v.getY()  - v.getRadius());
        b = StrictMath.max(isNaN(b) ? 0 : b, v.getY() + v.getRadius());
        return this;
    }

    Rect update(VehicleTick veEx) {
        return update(veEx.v);
    }

    Rect combine(Rect rect) {
        l = min(isNaN(l) ? Double.MAX_VALUE : l, isNaN(rect.l) ? Double.MAX_VALUE : rect.l);
        this.r = max(isNaN(this.r) ? 0 : this.r, isNaN(rect.r) ? 0: rect.r);
        t = min(isNaN(t) ? Double.MAX_VALUE : t, isNaN(rect.t) ? Double.MAX_VALUE : rect.t);
        b = max(isNaN(b) ? 0 : b, isNaN(rect.b) ? 0 : rect.b);
        return this;
    }

    boolean isRectNaN() { return Double.isNaN(l) && Double.isNaN(t) && Double.isNaN(b) && Double.isNaN(r);}
    Rect add(double range) { return new Rect(max(0, l - range),max(0, t - range),b+range, r+range); }
    double square() { return abs(r - l)*abs(b - t); }
    double dflt(double x, double y) { return hypot(x - l, y - t); }
    double dfct(double x, double y) { return hypot(x - cX(), y - cY()); }
    double dfct(P2D p) { return hypot(p.x - cX(), p.y - cY()); }
    double dfct(Rect rect) { return hypot(rect.cX() - cX(), rect.cY() - cY()); }
    P2D c() { return new P2D(cX(), cY()); }
    P2D tl() { return new P2D(l, t); }
    double cX() { return (l + r)/2; }
    double cY() { return (b + t)/2; }
    double linew() { return r - l;}
    double lineh() { return b - t;}
    boolean include(Vehicle vehicle) { return include(vehicle.getX(), vehicle.getY()); }
    Rect scale(double factor) { return new Rect(l, t, t + (b-t)*factor, l + (r-l)*factor); }
    boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
    boolean include(VehicleTick v) { return v.x() >= l && v.x() <= r && v.y() >= t && v.y() <= b; }
    Rect square(int i, int j, int n, double side) { return new Rect(l + j*side/n, t + i*side/n,
            t + (i+1)*side/n, l + (j+1)*side/n); }
    L[] sidesw(World world) { return new L[]{tlw(world), rlw(world), blw(world), llw(world)}; }
    Rect leftHalf() { return new Rect(l, t, b, (l+r)/2); }
    Rect rightHalf() { return new Rect((l+r)/2 + 0.5, t, b, r); }

    L tlw(World world) { return new L(new P2D(0,t),new P2D(world.getWidth(),t)); }
    L rlw(World world) { return new L(new P2D(r,0),new P2D(r, world.getHeight())); }
    L blw(World world) { return new L(new P2D(world.getWidth(), b),new P2D(0, b)); }
    L llw(World world) { return new L(new P2D(l, world.getHeight()),new P2D(l, 0)); }
    public boolean intersects(Rect rect) {
        int tw = (int)round(r - l);
        int th = (int)round(b - t);
        int rw = (int)round(rect.r - rect.l);
        int rh = (int)round(rect.b - rect.t);
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int tx = (int)this.l;
        int ty = (int)this.t;
        int rx = (int)rect.l;
        int ry = (int)rect.t;
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        //      overflow || intersect
        return ((rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry));
    }

    @Override
    public int compareTo(Rect o) {
        int cxc = U.cD(cX(),o.cX());
        if (cxc == 0) return U.cD(cY(),o.cY());
        return cxc;
    }

    @Override
    public String toString() {
        return "Rect{l:" + round(l) + ",t:" + round(t) + ",b:" + round(b) + ",r:" + round(r) + ",[" + c() + "]" + "}";
    }
}


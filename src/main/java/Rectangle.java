import model.Vehicle;
import model.VehicleType;
import model.World;

import java.util.Deque;

import static java.lang.Double.isNaN;
import static java.lang.StrictMath.*;
import static java.lang.StrictMath.hypot;
import static java.lang.StrictMath.round;

public class Rectangle implements Comparable<Rectangle> {
    public static Rectangle ORDER = new Rectangle(14,14,220,220);
    public double l = Double.NaN,t = Double.NaN,b = Double.NaN,r = Double.NaN;
    public VehicleType vt;
    public int g;
    public Deque<MoveBuilder> commands;

    Rectangle() {}
    Rectangle(VehicleType vt) { this.vt = vt; }
    Rectangle(World world) { this(0, 0, world.getHeight(), world.getWidth()); }
    Rectangle(double l, double t, double b, double r) { this.l = l;this.t = t;this.b = b;this.r = r; }
    Rectangle(Vehicle v) { this(v.getX() - v.getRadius(), v.getY() - v.getRadius(), v.getY() + v.getRadius(), v.getX() + v.getRadius()); }

    Rectangle update(Vehicle v) {
        l = StrictMath.min(isNaN(l) ? Double.MAX_VALUE : l, v.getX() - v.getRadius());
        r = StrictMath.max(isNaN(r) ? 0 : r, v.getX()  + v.getRadius());
        t = StrictMath.min(isNaN(t) ? Double.MAX_VALUE : t, v.getY()  - v.getRadius());
        b = StrictMath.max(isNaN(b) ? 0 : b, v.getY() + v.getRadius());
        return this;
    }

    Rectangle update(VehicleTick veEx) {
        return update(veEx.v);
    }

    Rectangle combine(Rectangle rectangle) {
        l = min(isNaN(l) ? Double.MAX_VALUE : l, isNaN(rectangle.l) ? Double.MAX_VALUE : rectangle.l);
        this.r = max(isNaN(this.r) ? 0 : this.r, isNaN(rectangle.r) ? 0: rectangle.r);
        t = min(isNaN(t) ? Double.MAX_VALUE : t, isNaN(rectangle.t) ? Double.MAX_VALUE : rectangle.t);
        b = max(isNaN(b) ? 0 : b, isNaN(rectangle.b) ? 0 : rectangle.b);
        return this;
    }

    boolean isRectNaN() { return Double.isNaN(l) && Double.isNaN(t) && Double.isNaN(b) && Double.isNaN(r);}
    double square() { return abs(r - l)*abs(b - t); }
    double dfct(Rectangle rectangle) { return hypot(rectangle.cX() - cX(), rectangle.cY() - cY()); }
    P2D c() { return new P2D(cX(), cY()); }
    double cX() { return (l + r)/2; }
    double cY() { return (b + t)/2; }
    double linew() { return r - l;}
    double lineh() { return b - t;}
    Rectangle scale(double factor) { return new Rectangle(l, t, t + (b-t)*factor, l + (r-l)*factor); }
    boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
    Rectangle square(int i, int j, int n, double side) { return new Rectangle(l + j*side/n, t + i*side/n,
            t + (i+1)*side/n, l + (j+1)*side/n); }
            
    public boolean intersects(Rectangle rectangle) {
        int tw = (int)round(r - l);
        int th = (int)round(b - t);
        int rw = (int)round(rectangle.r - rectangle.l);
        int rh = (int)round(rectangle.b - rectangle.t);
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) {
            return false;
        }
        int tx = (int)this.l;
        int ty = (int)this.t;
        int rx = (int) rectangle.l;
        int ry = (int) rectangle.t;
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
    public int compareTo(Rectangle o) {
        int cxc = U.cD(cX(),o.cX());
        if (cxc == 0) return U.cD(cY(),o.cY());
        return cxc;
    }

    @Override
    public String toString() {
        return "Rectangle{l:" + round(l) + ",t:" + round(t) + ",b:" + round(b) + ",r:" + round(r) + ",[" + c() + "]" + "}";
    }
}


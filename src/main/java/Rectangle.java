import model.Facility;
import model.Vehicle;
import model.VehicleType;
import model.World;

import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import static java.lang.StrictMath.*;
import static java.lang.StrictMath.hypot;
import static java.lang.StrictMath.round;

public class Rectangle implements Comparable<Rectangle> {
    static Rectangle ORDER = new Rectangle(16,16,222,222);
    double ax = Double.NaN,bx = Double.NaN,cx = Double.NaN,dx = Double.NaN;
    double ay = Double.NaN,by = Double.NaN,cy = Double.NaN,dy = Double.NaN;
    double l = Double.NaN,r = Double.NaN,t = Double.NaN,b = Double.NaN;
    VehicleType vt;
    Set<VehicleType> vts = new HashSet<>();
    int g;
    Deque<MoveBuilder> commands;
    double speed = Double.MAX_VALUE;
    double angle;
    P2D nsp;

    Rectangle() {}
    Rectangle(World world) { this(0, 0, world.getHeight(), world.getWidth()); }
    Rectangle(double x, double y, World world, double range) {
        this(Math.max(0, x - range), Math.max(0, y - range), Math.min(y + range, world.getHeight()), Math.min(x + range, world.getWidth()));
    }
    Rectangle(double l, double t, double b, double r) {
        ax = dx = l; ay = by = t; cx = bx = r;dy = cy = b; this.l = l;this.r = r;this.t = t;this.b = b;
    }
    Rectangle(Vehicle v) { this(v.getX() - v.getRadius(), v.getY() - v.getRadius(), v.getY() + v.getRadius(), v.getX() + v.getRadius()); }
    Rectangle(Facility facility) {
        this(facility.getLeft(), facility.getTop(), facility.getTop() + 64, facility.getLeft() + 64);
    }
    Rectangle(int[] xy, double factor) {
        this(xy[0] * U.PALE_SIDE * factor, xy[1] * U.PALE_SIDE * factor,
                U.PALE_SIDE * factor * (xy[1] + 1), U.PALE_SIDE * factor * (xy[0] + 1));
    }

    boolean isNaN() {
        return l == Double.MAX_VALUE || r == Double.MAX_VALUE || t == Double.MAX_VALUE || b == Double.MAX_VALUE;
    }
    boolean nonIsNaN() {
        return !isNaN();
    }

    double square() {
        if (vts.isEmpty()) return 0;
        return abs(r - l)*abs(b - t);
    }
    double dfct(Rectangle rectangle) { return hypot(rectangle.cX() - cX(), rectangle.cY() - cY()); }
    P2D c() { return new P2D(cX(), cY()); }
    double cX() { return (l + r)/2; }
    double cY() { return (t + b)/2; }

    boolean include(P2D p2D) {
        return include(p2D.x, p2D.y);
    }

    boolean include(double x, double y) {
        if (U.eD(angle, 0.01) || U.eD(angle, PI, 0.01)) return x >= l && x <= r && y >= t && y <= b;
        Line line = new Line(new P2D(x, y), new P2D(0,0));
        int intersects = 0;
        for (Line edge : edges()) {
            if (Line.intersect(edge.ps[0], edge.ps[1], line.ps[0], line.ps[1])) intersects++;
        }
        return intersects == 1;
    }
    
    P2D square(int i, int j) { return new P2D(l + j*58 + j*16 + 58/2, t + i*58 + i*16 + 58/2); }
    double side() {return max(P2D.distanceTo(new P2D(bx, by),new P2D(ax, ay)), P2D.distanceTo(new P2D(bx, by),new P2D(cx, cy))); }
    Line[] sightLines() {
        if (P2D.distanceTo(new P2D(bx, by), new P2D(ax, ay)) > P2D.distanceTo(new P2D(bx, by),new P2D(cx, cy)))
            return new Line[]{new Line(new P2D((ax+bx)/2, (ay+by)/2), c()), new Line(c(), new P2D((ax+bx)/2, (ay+by)/2))};
        else return new Line[]{new Line(new P2D((bx+cx)/2, (by+cy)/2), c()), new Line(c(), new P2D((bx+cx)/2, (by+cy)/2))};
    }
    P2D[] points() { return new P2D[]{new P2D(ax, ay), new P2D(bx, by), new P2D(cx, cy), new P2D(dx, dy)}; }
    boolean rotation(double width, double height) {
        double radius = 0;
        P2D center = c();
        for (P2D d : points()) {
            radius = Math.max(radius, P2D.distanceTo(center, d));
        }
        return center.x - radius >= 0 && center.x + radius <= width && center.y - radius >= 0 && center.y + radius <= height;
    }
    Line[] edges() {
        P2D[] points = points();
        return new Line[]{new Line(points[0], points[1]), new Line(points[1], points[2]), new Line(points[2], points[3]),
                new Line(points[3], points[0])};
    }

    static Rectangle nsRectangle(P2D nsp) {
        return new Rectangle(Math.max(0, nsp.x - 90/2), Math.max(0, nsp.y - 90/2), Math.max(0, nsp.y + 90/2), Math.max(0, nsp.x + 90/2));
    }

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

    static class Builder {
        VehicleType vt;
        int g;
        double angle;
        double speed = Double.MAX_VALUE;
        double ax = Double.MAX_VALUE, ay = 0;
        double cx = 0, cy = 0;
        double by = Double.MAX_VALUE, bx = 0;
        double dy = 0, dx = 0;
        double range = Double.MAX_VALUE;
        Set<VehicleType> vts = new HashSet<>();

        public Builder() {}

        public Builder(int g) {
            this.g = g;
        }

        public Builder(VehicleType vt) {
            this.vt = vt;
        }

        public Builder(Rectangle rect) {
            this.vt = rect.vt;
            this.g = rect.g;
            this.ax = rect.ax;
            this.ay = rect.ay;
            this.cx = rect.cx;
            this.cy = rect.cy;
            this.by = rect.by;
            this.bx = rect.bx;
            this.dy = rect.dy;
            this.dx = rect.dx;
        }

        Builder update(VehicleTick v) {
            return update(v.v);
        }

        Builder update(VehicleTick v, double range) {
            this.range = min(range, this.range);
            vts.add(v.type());
            return update(v.x(), v.y(), this.range);
        }

        Builder update(Vehicle v) {
            vts.add(v.getType());
            return update(v.getX(), v.getY(), v.getRadius());
        }

        Builder combine(Rectangle rectangle) {
            return combine(new Builder(rectangle));
        }

        Builder update(double x, double y, double range) {
            double xMr = x - range;
            double xPr = x + range;
            double yMr = y - range;
            double yPr = y + range;

            //red
            if (ax > xMr || U.eD(ax, xMr, 0.01 ) && ay > yMr) {
                ax = xMr;
                ay = yMr;
            }
            // black
            if (by > yMr || U.eD(by, yMr, 0.01) && bx < xPr) {
                by = yMr;
                bx = xPr;
            }
            // blue
            if (cx < xPr || U.eD(cx, xPr, 0.01) && cy < yPr) {
                cx = xPr;
                cy = yPr;
            }
            // orange
            if (dy < yPr || U.eD(dy, yPr, 0.01) && dx > xMr) {
                dy = yPr;
                dx = xMr;
            }
            return this;
        }

        Builder combine(Builder builder) {
            if (ax > builder.ax || U.eD(ax, builder.ax, 0.01) && builder.ax < ax) {
                ax = builder.ax;
                ay = builder.ay;
            }
            if (cx < builder.cx || U.eD(cx, builder.cx, 0.01) && builder.cy < cy) {
                cx = builder.cx;
                cy = builder.cy;
            }
            if (by > builder.by || U.eD(by, builder.by, 0.01) && builder.bx > bx) {
                by = builder.by;
                bx = builder.bx;
            }
            if (dy < builder.dy || U.eD(dy, builder.dy, 0.01) && builder.dx > dx) {
                dy = builder.dy;
                dx = builder.dx;
            }
            return this;
        }

        Rectangle build() {
            Rectangle rectangle = new Rectangle();
            rectangle.ax = round(ax); rectangle.ay = round(ay);
            rectangle.bx = round(bx); rectangle.by = round(by);
            rectangle.cx = round(cx); rectangle.cy = round(cy);
            rectangle.dx = round(dx); rectangle.dy = round(dy);
            Line xline = new Line(new P2D(0, 0), new P2D(1024, 0));
                if (P2D.distanceTo(new P2D(bx, by),new P2D(ax, ay)) > P2D.distanceTo(new P2D(bx, by),new P2D(cx, cy))) {
                angle = Line.angle(xline, new Line(new P2D(bx, by),new P2D(ax, ay)));
            } else {
                angle = Line.angle(xline, new Line(new P2D(bx, by),new P2D(cx, cy)));
            }
            rectangle.l = min(ax, min(bx, min(cx, dx)));
            rectangle.r = max(ax, max(bx, max(cx, dx)));
            rectangle.t = min(ay, min(by, min(cy, dy)));
            rectangle.b = max(ay, max(by, max(cy, dy)));
            rectangle.g = g;
            rectangle.vt = vt;
            rectangle.speed = speed;
            rectangle.angle = angle;
            rectangle.vts = vts;
            return rectangle;
        }
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


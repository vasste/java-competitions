import model.Vehicle;
import model.VehicleType;
import model.World;

import java.util.Arrays;

import static java.lang.StrictMath.*;

public class Rect implements Comparable<Rect> {
    public static Rect ORDER = new Rect(14,14,222,222);
    public double ax = Double.NaN,bx = Double.NaN,cx = Double.NaN,dx = Double.NaN;
    public double ay = Double.NaN,by = Double.NaN,cy = Double.NaN,dy = Double.NaN;
    double l = Double.NaN,r = Double.NaN,t = Double.NaN,b = Double.NaN;
    public VehicleType vt;
    public int g;
    public double angle;

    Rect() {}

    public Rect(double ax, double dx, double bx, double cx, double ay, double dy, double by, double cy) {
        this.ax = ax;
        this.bx = bx;
        this.cx = cx;
        this.dx = dx;
        this.ay = ay;
        this.by = by;
        this.cy = cy;
        this.dy = dy;
    }
    Rect(double l, double t, double b, double r) {
        ax = dx = l; ay = by = t; cx = bx = r;dy = cy = b; this.l = l;this.r = r;this.t = t;this.b = b;
    }

    Rect(World world) { this(0, 0, world.getHeight(), world.getWidth()); }
    Rect(Vehicle v) { this(v.getX() - v.getRadius(), v.getY() - v.getRadius(), v.getY() + v.getRadius(), v.getX() + v.getRadius()); }
    Rect(double x, double y, World world, double range) {
        this(Math.max(0, x - range), Math.max(0, y - range), Math.min(y + range, world.getHeight()), Math.min(x + range, world.getWidth()));
    }

    Rect add(double range) {
        return new Rect(max(0, ax - range), max(0, dx - range),
                min(1024, bx + range), min(1024, cx + range),
                max(0, ay - range), max(0, dy - range),
                min(1024, cy + range), min(1024, by + range));
    }
//    double square() { return abs(r - l)*abs(b - t); }
//    double dflt(double x, double y) { return hypot(x - l, y - t); }
//    double dfct(double x, double y) { return hypot(x - cX(), y - cY()); }
//    double dfct(P2D p) { return hypot(p.x - cX(), p.y - cY()); }
//    double dfct(Rect rectangle) { return hypot(rectangle.cX() - cX(), rectangle.cY() - cY()); }
    P2D c() { return new P2D(cX(), cY()); }
    P2D tl() { return new P2D(ax, ay); }
    P2D tr() { return new P2D(bx, by); }
    double cX() { return (l + r)/2; }
    double cY() { return (t + b)/2; }
//    boolean include(Vehicle vehicle) { return include(vehicle.getX(), vehicle.getY()); }
//    Rect scale(double factor) { return new Rect(l, t, t + (b-t)*factor, l + (r-l)*factor); }
//    boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
    boolean include(double x, double y) {
        if (U.eD(angle, 0, 0.01) || U.eD(angle, PI, 0.01)) return x >= l && x <= r && y >= t && y <= b;
        Line line = new Line(new P2D(x, y), new P2D(1024,1024));
        int intersects = 0;
        for (Line edge : edges()) {
            if (Line.intersect(edge.ps[0], edge.ps[1], line.ps[0], line.ps[1])) intersects++;
        }
        return intersects == 1;
    }
    Line sideW() {
        if (P2D.distanceTo(new P2D(bx, by),new P2D(ax, ay)) > P2D.distanceTo(new P2D(bx, by),new P2D(cx, cy)))
            return new Line(new P2D(bx, by),new P2D(ax, ay));
        else return new Line(new P2D(bx, by),new P2D(cx, cy));
    }

    P2D[] points() { return new P2D[]{new P2D(ax, ay), new P2D(bx, by), new P2D(cx, cy), new P2D(dx, dy)}; }
    Line[] edges() {
        P2D[] points = points();
        return new Line[]{new Line(points[0], points[1]), new Line(points[1], points[2]), new Line(points[2], points[3]),
                new Line(points[3], points[0])};
    }


    @Override
    public int compareTo(Rect o) {
        int cxc = U.cD(cX(),o.cX());
        if (cxc == 0) return U.cD(cY(),o.cY());
        return cxc;
    }

    @Override
    public String toString() {
        return "Rect{l:" + Arrays.toString(points()) + ",[" + c() + "]" + "}";
    }

    static class Builder {
        public VehicleType vt;
        public int g;
        double ax = Double.MAX_VALUE, ay = 0;
        double ax2 = Double.MAX_VALUE, ay2 = 0;
        double cx = 0, cy = 0;
        double cx2 = 0, cy2 = 0;
        double by = Double.MAX_VALUE, bx = 0;
        double by2 = Double.MAX_VALUE, bx2 = 0;
        double dy = 0, dx = 0;
        double dy2 = 0, dx2 = 0;

        public Builder() {
        }

        public Builder(Rect rect) {
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
            this.ax2 = rect.ax;
            this.ay2 = rect.ay;
            this.cx2 = rect.cx;
            this.cy2 = rect.cy;
            this.by2 = rect.by;
            this.bx2 = rect.bx;
            this.dy2 = rect.dy;
            this.dx2 = rect.dx;

        }

        Builder update(Vehicle v) {
            double range = 18;
            double xMr = max(0, v.getX() - range);
            double xPr = v.getX() + range;
            double yMr = max(0, v.getY() - range);
            double yPr = v.getY() + range;

            if (ax > xMr || U.eD(ax, xMr) && ay > yPr) {
                ax = xMr;
                ay = yPr;
            }
            if (by > yMr || U.eD(by, yMr) && bx < xMr ) {
                by = yMr;
                bx = xMr;
            }
            if (cx < xPr || U.eD(cx, xPr) && cy < yMr) {
                cx = xPr;
                cy = yMr;
            }
            if (dy < yPr || U.eD(dy, yPr) && dx > xPr) {
                dy = yPr;
                dx = xPr;
            }
            update2(v);
            return this;
        }

        Builder update2(Vehicle v) {
            double range = 18;
            double xMr = max(0, v.getX() - range);
            double xPr = v.getX() + range;
            double yMr = max(0, v.getY() - range);
            double yPr = v.getY() + range;

            if (ax2 > xMr || U.eD(ax2, xMr) && ay2 > yMr) {
                ax2 = xMr;
                ay2 = yMr;
            }
            if (by2 > yMr || U.eD(by2, yMr) && bx2 < xMr ) {
                by2 = yMr;
                bx2 = xMr;
            }
            if (cx2 < xPr || U.eD(cx2, xPr) && cy2 < yMr) {
                cx2 = xPr;
                cy2 = yMr;
            }
            if (dy2 < yPr || U.eD(dy2, yPr) && dx2 > xMr) {
                dy2 = yPr;
                dx2 = xMr;
            }
            return this;
        }

        Builder combine(Builder builder) {
            if (ax > builder.ax) {
                ax = builder.ax;
                ay = builder.ay;
            }
            if (cx < builder.cx) {
                cx = builder.cx;
                cy = builder.cy;
            }
            if (by > builder.by) {
                by = builder.by;
                bx = builder.bx;
            }
            if (dy < builder.dy) {
                dy = builder.dy;
                dx = builder.dx;
            }

            if (ax2 > builder.ax2) {
                ax2 = builder.ax2;
                ay2 = builder.ay2;
            }
            if (cx2 < builder.cx2) {
                cx2 = builder.cx2;
                cy2 = builder.cy2;
            }
            if (by2 > builder.by2) {
                by2 = builder.by2;
                bx2 = builder.bx2;
            }
            if (dy2 < builder.dy2) {
                dy2 = builder.dy2;
                dx2 = builder.dx2;
            }
            return this;
        }

        Rect build() {
            Rect rect = new Rect();
            Line xline = new Line(new P2D(0, 0), new P2D(1024, 0));
            if (P2D.distanceTo(new P2D(bx, by),new P2D(ax, ay)) > P2D.distanceTo(new P2D(bx, by),new P2D(cx, cy))) {
                rect.angle = Line.angle(xline, new Line(new P2D(bx, by),new P2D(ax, ay)));
            } else {
                rect.angle = Line.angle(xline, new Line(new P2D(bx, by),new P2D(cx, cy)));
            }
            if (rect.angle < PI/2) {
                ax = round(ax2);
                ay = round(ay2);
                bx = round(bx2);
                by = round(by2);
                cx = round(cx2);
                cy = round(cy2);
                dx = round(dx2);
                dy = round(dy2);
            }

            rect.ax = round(ax);
            rect.ay = round(ay);
            rect.bx = round(bx);
            rect.by = round(by);
            rect.cx = round(cx);
            rect.cy = round(cy);
            rect.dx = round(dx);
            rect.dy = round(dy);
            rect.l = min(ax, min(bx, min(cx, dx)));
            rect.r = max(ax, max(bx, max(cx, dx)));
            rect.t = min(ay, min(by, min(cy, dy)));
            rect.b = max(ay, max(by, max(cy, dy)));
            return rect;
        }
    }
}

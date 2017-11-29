import static java.lang.StrictMath.*;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class Line {
    double a, b, c;
    P2D[] ps;

    Line(P2D[] pq) { this(pq[0], pq[1]);}
    Line(P2D p, P2D q) {
        a = p.y - q.y;
        b = q.x - p.x;
        c = - a * p.x - b * p.y;
        norm();
        ps = new P2D[]{p, q};
    }

    void norm() {
        double z = sqrt (a*a + b*b);
        if (abs(z) > U.EPS) a /= z;b /= z;c /= z;
    }

    double dist(P2D p) {
        return a * p.x + b * p.y + c;
    }
    P2D[] ps() { return ps;}

    static boolean betw (double l, double r, double x) {
        return min(l,r) <= x + U.EPS && x <= max(l,r) + U.EPS;
    }
    static boolean intersect_1d (double a, double b, double c, double d) {
        double[] array = new double[]{a,b,c,d};
        if (a > b)  swap (array, 0, 1);
        if (c > d)  swap (array, 2, 3);
        return max (array[0], array[2]) <= min (array[1], array[3]) + U.EPS;
    }

    private static void swap(double[] x, int a, int b) {
        double t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    static double angle(Line a, Line b) {
        return acos(a.a*b.a + a.b*b.b);
    }

    static boolean intersect(P2D a, P2D b, P2D c, P2D d, P2D left, P2D right) {
        if (!intersect_1d (a.x, b.x, c.x, d.x) || ! intersect_1d (a.y, b.y, c.y, d.y)) return false;
        Line m = new Line(a, b);
        Line n = new Line(c, d);
        double zn = P2D.det(m.a, m.b, n.a, n.b);
        if (abs (zn) < U.EPS) {
            if (abs (m.dist (c)) > U.EPS || abs (n.dist (a)) > U.EPS)
                return false;
            return true;
        } else {
            left.x = right.x = - P2D.det(m.c, m.b, n.c, n.b) / zn;
            left.y = right.y = - P2D.det(m.a, m.c, n.a, n.c) / zn;
            return betw (a.x, b.x, left.x)
                    && betw (a.y, b.y, left.y)
                    && betw (c.x, d.x, left.x)
                    && betw (c.y, d.y, left.y);
        }
    }
}
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

//    Line rotate(double angle) {
//        if (angle > 0) {
//            double[][] m = new double[][]{new double[]{cos(angle),-sin(angle)}, new double[]{sin(angle),cos(angle)}};
//            for (int i = 0; i < m.length; i++) {
//                double[] doubles = m[0][0]*;
//            }
//        } else {
//            double[][] m = new double[][]{new double[]{cos(angle),sin(angle)}, new double[]{-sin(angle),cos(angle)}};
//        }
//    }

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

    static boolean intersect(P2D a, P2D b, P2D c, P2D d) {
        Line m = new Line(a, b);
        Line n = new Line(c, d);
        double zn = P2D.det(m.a, m.b, n.a, n.b);
        if (zn != 0) {
            double x = - P2D.det(m.c, m.b, n.c, n.b) * 1. / zn;
            double y = - P2D.det(m.a, m.c, n.a, n.c) * 1. / zn;
            return betw (a.x, b.x, x) && betw (a.y, b.y, y)
                    && betw (c.x, d.x, x) && betw (c.y, d.y, y);
        }
        else
            return U.eD(P2D.det(m.a, m.c, n.a, n.c), 0) && U.eD(P2D.det(m.b, m.c, n.b, n.c), 0)
                    && intersect_1d (a.x, b.x, c.x, d.x)
                    && intersect_1d (a.y, b.y, c.y, d.y);
    }
}
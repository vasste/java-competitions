import static java.lang.StrictMath.abs;

public class U {
    public static final double EPS = 0.5;
    public static final int PALE_SIDE = 32;

    public static boolean eD(double a, double b) { return eD(a, b, EPS);}
    public static boolean eD(double a, double b, double eps) { return abs(a - b) < eps;}
    public static int cD(double a, double b) { return abs(a - b) > EPS ? Double.compare(a, b) :  0; }
}

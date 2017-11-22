import static java.lang.StrictMath.abs;

public class U {
    public static final double EPS = 0.5;
    public static final double PALE_SIDE = 32.0;

    public static boolean eD(double a, double b) { return cD(a, b) == 0;}
    public static int cD(double a, double b) { return abs(a - b) > EPS ? Double.compare(a, b) :  0; }
}

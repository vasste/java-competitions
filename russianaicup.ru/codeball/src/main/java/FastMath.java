public class FastMath {

    /** Tangent table, used by atan() (high bits). */
    private static final double TANGENT_TABLE_A[] =
            {
                    +0.0d,
                    +0.1256551444530487d,
                    +0.25534194707870483d,
                    +0.3936265707015991d,
                    +0.5463024377822876d,
                    +0.7214844226837158d,
                    +0.9315965175628662d,
                    +1.1974215507507324d,
                    +1.5574076175689697d,
                    +2.092571258544922d,
                    +3.0095696449279785d,
                    +5.041914939880371d,
                    +14.101419448852539d,
                    -18.430862426757812d,
            };

    /** Tangent table, used by atan() (low bits). */
    private static final double TANGENT_TABLE_B[] =
            {
                    +0.0d,
                    -7.877917738262007E-9d,
                    -2.5857668567479893E-8d,
                    +5.2240336371356666E-9d,
                    +5.206150291559893E-8d,
                    +1.8307188599677033E-8d,
                    -5.7618793749770706E-8d,
                    +7.848361555046424E-8d,
                    +1.0708593250394448E-7d,
                    +1.7827257129423813E-8d,
                    +2.893485277253286E-8d,
                    +3.1660099222737955E-7d,
                    +4.983191803254889E-7d,
                    -3.356118100840571E-7d,
            };

    /** Eighths.
     * This is used by sinQ, because its faster to do a table lookup than
     * a multiply in this time-critical routine
     */
    private static final double EIGHTHS[] = {0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0, 1.125, 1.25, 1.375, 1.5, 1.625};

    /*
     *  There are 52 bits in the mantissa of a double.
     *  For additional precision, the code splits double numbers into two parts,
     *  by clearing the low order 30 bits if possible, and then performs the arithmetic
     *  on each half separately.
     */

    /**
     * 0x40000000 - used to split a double into two parts, both with the low order bits cleared.
     * Equivalent to 2^30.
     */
    private static final long HEX_40000000 = 0x40000000L; // 1073741824L

    /** Mask used to clear low order 30 bits */
    private static final long MASK_30BITS = -1L - (HEX_40000000 -1); // 0xFFFFFFFFC0000000L;


    /** Constant: {@value}. */
    private static final double F_3_4 = 3d / 4d;
    /** Constant: {@value}. */
    private static final double F_1_2 = 1d / 2d;
    /** Constant: {@value}. */
    private static final double F_1_4 = 1d / 4d;


    /**
     * Returns the first argument with the sign of the second argument.
     * A NaN {@code sign} argument is treated as positive.
     *
     * @param magnitude the value to return
     * @param sign the sign for the returned value
     * @return the magnitude with the same sign as the {@code sign} argument
     */
    public static double copySign(double magnitude, double sign){
        // The highest order bit is going to be zero if the
        // highest order bit of m and s is the same and one otherwise.
        // So (m^s) will be positive if both m and s have the same sign
        // and negative otherwise.
        final long m = Double.doubleToRawLongBits(magnitude); // don't care about NaN
        final long s = Double.doubleToRawLongBits(sign);
        if ((m^s) >= 0) {
            return magnitude;
        }
        return -magnitude; // flip sign
    }

    /**
     * Two arguments arctangent function
     * @param y ordinate
     * @param x abscissa
     * @return phase angle of point (x,y) between {@code -PI} and {@code PI}
     */
    public static double atan2(double y, double x) {
        if (x != x || y != y) {
            return Double.NaN;
        }

        if (y == 0) {
            final double result = x * y;
            final double invx = 1d / x;
            final double invy = 1d / y;

            if (invx == 0) { // X is infinite
                if (x > 0) {
                    return y; // return +/- 0.0
                } else {
                    return copySign(Math.PI, y);
                }
            }

            if (x < 0 || invx < 0) {
                if (y < 0 || invy < 0) {
                    return -Math.PI;
                } else {
                    return Math.PI;
                }
            } else {
                return result;
            }
        }

        // y cannot now be zero

        if (y == Double.POSITIVE_INFINITY) {
            if (x == Double.POSITIVE_INFINITY) {
                return Math.PI * F_1_4;
            }

            if (x == Double.NEGATIVE_INFINITY) {
                return Math.PI * F_3_4;
            }

            return Math.PI * F_1_2;
        }

        if (y == Double.NEGATIVE_INFINITY) {
            if (x == Double.POSITIVE_INFINITY) {
                return -Math.PI * F_1_4;
            }

            if (x == Double.NEGATIVE_INFINITY) {
                return -Math.PI * F_3_4;
            }

            return -Math.PI * F_1_2;
        }

        if (x == Double.POSITIVE_INFINITY) {
            if (y > 0 || 1 / y > 0) {
                return 0d;
            }

            if (y < 0 || 1 / y < 0) {
                return -0d;
            }
        }

        if (x == Double.NEGATIVE_INFINITY)
        {
            if (y > 0.0 || 1 / y > 0.0) {
                return Math.PI;
            }

            if (y < 0 || 1 / y < 0) {
                return -Math.PI;
            }
        }

        // Neither y nor x can be infinite or NAN here

        if (x == 0) {
            if (y > 0 || 1 / y > 0) {
                return Math.PI * F_1_2;
            }

            if (y < 0 || 1 / y < 0) {
                return -Math.PI * F_1_2;
            }
        }

        // Compute ratio r = y/x
        final double r = y / x;
        if (Double.isInfinite(r)) { // bypass calculations that can create NaN
            return atan(r, 0, x < 0);
        }

        double ra = doubleHighPart(r);
        double rb = r - ra;

        // Split x
        final double xa = doubleHighPart(x);
        final double xb = x - xa;

        rb += (y - ra * xa - ra * xb - rb * xa - rb * xb) / x;

        final double temp = ra + rb;
        rb = -(temp - ra - rb);
        ra = temp;

        if (ra == 0) { // Fix up the sign so atan works correctly
            ra = copySign(0d, y);
        }

        // Call atan
        final double result = atan(ra, rb, x < 0);

        return result;
    }

    /**
     * Arctangent function
     *  @param x a number
     *  @return atan(x)
     */
    public static double atan(double x) {
        return atan(x, 0.0, false);
    }

    /** Internal helper function to compute arctangent.
     * @param xa number from which arctangent is requested
     * @param xb extra bits for x (may be 0.0)
     * @param leftPlane if true, result angle must be put in the left half plane
     * @return atan(xa + xb) (or angle shifted by {@code PI} if leftPlane is true)
     */
    private static double atan(double xa, double xb, boolean leftPlane) {
        if (xa == 0.0) { // Matches +/- 0.0; return correct sign
            return leftPlane ? copySign(Math.PI, xa) : xa;
        }

        final boolean negate;
        if (xa < 0) {
            // negative
            xa = -xa;
            xb = -xb;
            negate = true;
        } else {
            negate = false;
        }

        if (xa > 1.633123935319537E16) { // Very large input
            return (negate ^ leftPlane) ? (-Math.PI * F_1_2) : (Math.PI * F_1_2);
        }

        /* Estimate the closest tabulated arctan value, compute eps = xa-tangentTable */
        final int idx;
        if (xa < 1) {
            idx = (int) (((-1.7168146928204136 * xa * xa + 8.0) * xa) + 0.5);
        } else {
            final double oneOverXa = 1 / xa;
            idx = (int) (-((-1.7168146928204136 * oneOverXa * oneOverXa + 8.0) * oneOverXa) + 13.07);
        }

        final double ttA = TANGENT_TABLE_A[idx];
        final double ttB = TANGENT_TABLE_B[idx];

        double epsA = xa - ttA;
        double epsB = -(epsA - xa + ttA);
        epsB += xb - ttB;

        double temp = epsA + epsB;
        epsB = -(temp - epsA - epsB);
        epsA = temp;

        /* Compute eps = eps / (1.0 + xa*tangent) */
        temp = xa * HEX_40000000;
        double ya = xa + temp - temp;
        double yb = xb + xa - ya;
        xa = ya;
        xb += yb;

        //if (idx > 8 || idx == 0)
        if (idx == 0) {
            /* If the slope of the arctan is gentle enough (< 0.45), this approximation will suffice */
            //double denom = 1.0 / (1.0 + xa*tangentTableA[idx] + xb*tangentTableA[idx] + xa*tangentTableB[idx] + xb*tangentTableB[idx]);
            final double denom = 1d / (1d + (xa + xb) * (ttA + ttB));
            //double denom = 1.0 / (1.0 + xa*tangentTableA[idx]);
            ya = epsA * denom;
            yb = epsB * denom;
        } else {
            double temp2 = xa * ttA;
            double za = 1d + temp2;
            double zb = -(za - 1d - temp2);
            temp2 = xb * ttA + xa * ttB;
            temp = za + temp2;
            zb += -(temp - za - temp2);
            za = temp;

            zb += xb * ttB;
            ya = epsA / za;

            temp = ya * HEX_40000000;
            final double yaa = (ya + temp) - temp;
            final double yab = ya - yaa;

            temp = za * HEX_40000000;
            final double zaa = (za + temp) - temp;
            final double zab = za - zaa;

            /* Correct for rounding in division */
            yb = (epsA - yaa * zaa - yaa * zab - yab * zaa - yab * zab) / za;

            yb += -epsA * zb / za / za;
            yb += epsB / za;
        }


        epsA = ya;
        epsB = yb;

        /* Evaluate polynomial */
        final double epsA2 = epsA * epsA;

        /*
    yb = -0.09001346640161823;
    yb = yb * epsA2 + 0.11110718400605211;
    yb = yb * epsA2 + -0.1428571349122913;
    yb = yb * epsA2 + 0.19999999999273194;
    yb = yb * epsA2 + -0.33333333333333093;
    yb = yb * epsA2 * epsA;
         */

        yb = 0.07490822288864472;
        yb = yb * epsA2 - 0.09088450866185192;
        yb = yb * epsA2 + 0.11111095942313305;
        yb = yb * epsA2 - 0.1428571423679182;
        yb = yb * epsA2 + 0.19999999999923582;
        yb = yb * epsA2 - 0.33333333333333287;
        yb = yb * epsA2 * epsA;


        ya = epsA;

        temp = ya + yb;
        yb = -(temp - ya - yb);
        ya = temp;

        /* Add in effect of epsB.   atan'(x) = 1/(1+x^2) */
        yb += epsB / (1d + epsA * epsA);

        final double eighths = EIGHTHS[idx];

        //result = yb + eighths[idx] + ya;
        double za = eighths + ya;
        double zb = -(za - eighths - ya);
        temp = za + yb;
        zb += -(temp - za - yb);
        za = temp;

        double result = za + zb;

        if (leftPlane) {
            // Result is in the left plane
            final double resultb = -(result - za - zb);
            final double pia = 1.5707963267948966 * 2;
            final double pib = 6.123233995736766E-17 * 2;

            za = pia - result;
            zb = -(za - pia + result);
            zb += pib - resultb;

            result = za + zb;
        }


        if (negate ^ leftPlane) {
            result = -result;
        }

        return result;
    }

    public static final double SAFE_MIN = 0x1.0p-1022;

    /**
     * Get the high order bits from the mantissa.
     * Equivalent to adding and subtracting HEX_40000 but also works for very large numbers
     *
     * @param d the value to split
     * @return the high order part of the mantissa
     */
    private static double doubleHighPart(double d) {
        if (d > -SAFE_MIN && d < SAFE_MIN){
            return d; // These are un-normalised - don't try to convert
        }
        long xl = Double.doubleToRawLongBits(d); // can take raw bits because just gonna convert it back
        xl &= MASK_30BITS; // Drop low order bits
        return Double.longBitsToDouble(xl);
    }


    static double[]             table = null;
    static double               step;
    static double               invStep;
    static int                  size  = 0;

    static
    {
        size = 10000;
        table = new double[size];
        step = 2d * Math.PI / size;
        invStep = 1.0f / step;
        for (int i = 0; i < size; ++i)
        {
            table[i] = Math.cos(step * i);
        }
    }

    /** Find a linear interpolation from the table
     *
     * @param ang
     *            angle in radians
     * @return cos of angle a
     */
    private final static double pi2   = Math.PI * 2;

    public static double cos(double ang)
    {
        ang += pi2;
        double t = ang % pi2;
        int indexA = (int) (t / step);
        int indexB = indexA + 1;
        if (indexB >= size) return table[indexA];
        double a = table[indexA];
        return a + (table[indexB] - a) * (t - (indexA * step)) * invStep;

    }
}

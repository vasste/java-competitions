import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class P2D implements Cloneable, Serializable {
   private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

   public static P2D plus(P2D p, Vec2D v) {
      P2D res = new P2D(p);
      return res.plus(v);
   }

   private double x;
   private double y;

   private long ix;
   private long iy;

   public P2D() {

   }

   public P2D(double x, double y) {
      this.x = x;
      this.y = y;
      init();
   }

   public P2D(P2D other) {
      this.x = other.x;
      this.y = other.y;
      init();
   }

   private void init() {
      ix = Math.round(x * 10000);
      iy = Math.round(y * 10000);
   }

   public double getX() {
      return x;
   }

   public void setX(double x) {
      this.x = x;
   }

   public double getY() {
      return y;
   }

   public void setY(double y) {
      this.y = y;
   }

   public P2D plus(Vec2D vector) {
      return new P2D(x + vector.getX(), y + vector.getY());
   }

   public double distanceTo(P2D other) {
      double dx = (x - other.x);
      double dy = (y - other.y);
      return FastMath.hypot(dx, dy);
   }

   public double distanceTo(double xPos, double yPos) {
      double dx = (x - xPos);
      double dy = (y - yPos);
      return FastMath.hypot(dx, dy);
   }

   public List<P2D> inRange(List<P2D> otherP2DS, double range) {
      List<P2D> res = new ArrayList<>();
      for (P2D p : otherP2DS) {
         if (distanceTo(p) < range) {
            res.add(p);
         }
      }
      return res;
   }

   public P2D findNearestPoint(Collection<P2D> otherP2DS) {
      P2D res = null;
      double minDist = Integer.MAX_VALUE;
      for (P2D p : otherP2DS) {
         double dist = distanceTo(p);
         if (dist < minDist) {
            res = p;
            minDist = dist;
         }
      }
      return res;
   }

   public P2D findMostFarPoint(Collection<P2D> otherP2DS) {
      P2D res = null;
      double maxDist = Integer.MIN_VALUE;
      for (P2D p : otherP2DS) {
         double dist = distanceTo(p);
         if (dist > maxDist) {
            res = p;
            maxDist = dist;
         }
      }
      return res;
   }

   @Override
   public P2D clone() {
      return new P2D(this);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      P2D p2D = (P2D) o;

      if (ix != p2D.ix)
         return false;
      return iy == p2D.iy;
   }

   @Override
   public int hashCode() {
      int result = (int) (ix ^ (ix >>> 32));
      result = 31 * result + (int) (iy ^ (iy >>> 32));
      return result;
   }

   public String toString() {
      return "[x=" + DECIMAL_FORMAT.format(x) + "][y=" +
              DECIMAL_FORMAT.format(y) + "]";
   }
}

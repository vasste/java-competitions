import model.CircularUnit;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Point implements Cloneable, Serializable {
   private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.###");

   public static Point plus(Point p, Vector2D v) {
      Point res = new Point(p);
      return res.plus(v);
   }

   private double x;
   private double y;

   private long ix;
   private long iy;

   public Point() {

   }

   public Point(double x, double y) {
      this.x = x;
      this.y = y;
      init();
   }

   public Point(Point other) {
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

   public Point plus(Vector2D vector) {
      return new Point(x + vector.getX(), y + vector.getY());
   }

   public double distanceTo(Point other) {
      double dx = (x - other.x);
      double dy = (y - other.y);
      return Utils.hypot(dx, dy);
   }

   public double distanceTo(double xPos, double yPos) {
      double dx = (x - xPos);
      double dy = (y - yPos);
      return Utils.hypot(dx, dy);
   }

   public double distanceTo(CircularUnit other) {
      return distanceTo(other.getX(), other.getY());
   }

   public List<Point> inRange(List<Point> otherPoints, double range) {
      List<Point> res = new ArrayList<>();
      for (Point p : otherPoints) {
         if (distanceTo(p) < range) {
            res.add(p);
         }
      }
      return res;
   }

   public Point findNearestPoint(Collection<Point> otherPoints) {
      Point res = null;
      double minDist = Integer.MAX_VALUE;
      for (Point p : otherPoints) {
         double dist = distanceTo(p);
         if (dist < minDist) {
            res = p;
            minDist = dist;
         }
      }
      return res;
   }

   public Point findMostFarPoint(Collection<Point> otherPoints) {
      Point res = null;
      double maxDist = Integer.MIN_VALUE;
      for (Point p : otherPoints) {
         double dist = distanceTo(p);
         if (dist > maxDist) {
            res = p;
            maxDist = dist;
         }
      }
      return res;
   }

   @Override
   public Point clone() {
      return new Point(this);
   }

   @Override
   public boolean equals(Object o) {
      if (this == o)
         return true;
      if (o == null || getClass() != o.getClass())
         return false;

      Point point = (Point) o;

      if (ix != point.ix)
         return false;
      return iy == point.iy;
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

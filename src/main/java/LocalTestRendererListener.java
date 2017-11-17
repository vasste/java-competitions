import model.Game;
import model.Vehicle;
import model.VehicleType;
import model.World;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.StrictMath.*;

public final class LocalTestRendererListener {
    private Graphics graphics;
    private World world;
    private Game game;

    private int canvasWidth;
    private int canvasHeight;

    private double left;
    private double top;
    private double width;
    private double height;
    private Vehicle[] vehicles;

    public void beforeDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                                double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);
        vehicles = world.getVehicles();
        Map<Long, Vehicle> d = new HashMap<>();
        for (int i = 0; i < vehicles.length; i++) {
             d.put(vehicles[i].getId(), vehicles[i]);
        }
        Rect rect = rectOfVehicleGroup(1, 3);
        Rect rect1 = rectOfVehicleGroup(1);
        Rect rect3 = rectOfVehicleGroup(3);
        graphics.setColor(Color.CYAN);
        fillRect(rect);
        graphics.setColor(Color.YELLOW);
        fillRect(rect1);
        graphics.setColor(Color.GRAY);
        fillRect(rect3);

        graphics.setColor(Color.GREEN);
        fillCircle(rect3.centerX(), rect3.centerY(), 5);
        fillCircle(rect1.centerX(), rect1.centerY(), 5);
        fillCircle(rect.centerX(), rect.centerY(), 5);

        P2D to = getPoint2D(4, 5);
        graphics.setColor(Color.RED);
        fillCircle(to.x, to.y, 5);
        to = getPoint2D(1, 3);
        graphics.setColor(Color.RED);
        fillCircle(to.x, to.y, 10);
    }

    private P2D getPoint2D(int ag, int bg) {
        Rect rect = rectOfVehicleGroup(ag, bg);
        P2D center = new P2D(rect.centerX(), rect.centerY());
        return enemyVehicles().reduce(new P2D(Double.MAX_VALUE, Double.MAX_VALUE),
                (p, vehicle) -> closedTo(p, new P2D(vehicle), center), (a, b) -> closedTo(a, b, center));
    }

    static P2D closedTo(P2D a, P2D b, P2D center) {
        if (distanceTo(center, a) > distanceTo(center, b)) return b;
        else return a;
    }
    static double distanceTo(P2D a, P2D b) { return hypot(a.x - b.x, a.y - b.y); }

    public void afterDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                               double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);
    }

    private void updateFields(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                              double left, double top, double width, double height) {
        this.graphics = graphics;
        this.world = world;
        this.game = game;

        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    Stream<Vehicle> myTANK() { return myVehicleTyped(VehicleType.TANK); }
    Stream<Vehicle> myARRV() { return myVehicleTyped(VehicleType.ARRV); }
    Stream<Vehicle> myFIGHTER() { return myVehicleTyped(VehicleType.FIGHTER); }
    Stream<Vehicle> myHELICOPTER() { return myVehicleTyped(VehicleType.HELICOPTER); }
    Stream<Vehicle> myIFV() { return myVehicleTyped(VehicleType.IFV); }
    Stream<Vehicle> myVehicles() { return Arrays.stream(vehicles).filter(vh -> vh.getPlayerId() == 1); }
    Stream<Vehicle> myVehicleTyped(VehicleType type) { return myVehicles().filter(vt -> vt.getType() == type); }
    Rect rectOfVehicles(Stream<Vehicle> vhStream) { return vhStream.reduce(new Rect(), Rect::update, Rect::combine); }
    Stream<Vehicle> groupVehicles(int id) { return myVehicles().filter(vt -> Arrays.stream(vt.getGroups()).anyMatch(v -> v == id)); }
    Rect rectOfVehicleGroup(int... ids) {
        return myVehicles().reduce(new Rect(), (rect, v) -> {
            for (int i = 0; i < ids.length; i++) {
                int id = ids[i];
                if (Arrays.stream(v.getGroups()).anyMatch(vid -> vid == id)) rect.update(v);
            }
            return rect;
        }, Rect::combine);
    }
    Stream<Vehicle> enemyVehicles() { return Arrays.stream(vehicles).filter(vh -> vh.getPlayerId() != 1); }

    private void drawLine(double x1, double y1, double x2, double y2) {
        Point2I lineBegin = toCanvasPosition(x1, y1);
        Point2I lineEnd = toCanvasPosition(x2, y2);

        graphics.drawLine(lineBegin.getX(), lineBegin.getY(), lineEnd.getX(), lineEnd.getY());
    }

    private void fillCircle(double centerX, double centerY, double radius) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.fillOval(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void drawCircle(double centerX, double centerY, double radius) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.drawOval(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void fillArc(double centerX, double centerY, double radius, int startAngle, int arcAngle) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.fillArc(topLeft.getX(), topLeft.getY(), size.getX(), size.getY(), startAngle, arcAngle);
    }

    private void drawArc(double centerX, double centerY, double radius, int startAngle, int arcAngle) {
        Point2I topLeft = toCanvasPosition(centerX - radius, centerY - radius);
        Point2I size = toCanvasOffset(2.0D * radius, 2.0D * radius);

        graphics.drawArc(topLeft.getX(), topLeft.getY(), size.getX(), size.getY(), startAngle, arcAngle);
    }

    private void fillRect(double left, double top, double width, double height) {
        Point2I topLeft = toCanvasPosition(left, top);
        Point2I size = toCanvasOffset(width, height);

        graphics.fillRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void fillRect(Rect rect) {
        fillRect(rect.l, rect.t, rect.r - rect.l, rect.b - rect.t);
    }

    private void drawRect(double left, double top, double width, double height) {
        Point2I topLeft = toCanvasPosition(left, top);
        Point2I size = toCanvasOffset(width, height);

        graphics.drawRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void drawPolygon(Point2D... points) {
        int pointCount = points.length;

        for (int pointIndex = 1; pointIndex < pointCount; ++pointIndex) {
            Point2D pointA = points[pointIndex];
            Point2D pointB = points[pointIndex - 1];
            drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
        }

        Point2D pointA = points[0];
        Point2D pointB = points[pointCount - 1];
        drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
    }

    private Point2I toCanvasOffset(double x, double y) {
        return new Point2I(x * canvasWidth / width, y * canvasHeight / height);
    }

    private Point2I toCanvasPosition(double x, double y) {
        return new Point2I((x - left) * canvasWidth / width, (y - top) * canvasHeight / height);
    }

    private static final class Point2I {
        private int x;
        private int y;

        private Point2I(double x, double y) {
            this.x = toInt(round(x));
            this.y = toInt(round(y));
        }

        private Point2I(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private Point2I() {
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public int getY() {
            return y;
        }

        public void setY(int y) {
            this.y = y;
        }
        
        private static int toInt(double value) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int intValue = (int) value;
            if (abs((double) intValue - value) < 1.0D) {
                return intValue;
            }
            throw new IllegalArgumentException("Can't convert double " + value + " to int.");
        }
    }

    private static final class Point2D {
        private double x;
        private double y;

        private Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        private Point2D() {
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
    }
}

import model.Ball;

import java.awt.*;

public class DrawUtils {

    static class TH {
        String Text;

        public TH(String text) {
            Text = text;
        }
    }

    static class SH {
        Sphere Sphere;

        public SH(Sphere sphere) {
            this.Sphere = sphere;
        }
    }

    public static Sphere s() {
        return null;
    }

    static class Sphere {
        double x, y, z, radius, r, g, b, a;

        public Sphere(Vec3D p, double radius, Color color) {
            this(p.getX(), p.getY(), p.getZ(), radius, color);
        }

        public Sphere(Ball ball, Color color) {
            this(ball.x, ball.y, ball.z, ball.radius, color);
        }

        public Sphere(double x, double y, double z, double radius, Color color) {
            this(x, y, z, radius, color.getRed(), color.getGreen(), color.getBlue());
        }

        private Sphere(double x, double y, double z, double radius, double r, double g, double b) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.radius = radius;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = 0.5;
        }

        public SH h() {
            return new SH(this);
        }
    }

    static class LH {
        Line Line;

        public LH(Line line) {
            this.Line = line;
        }
    }

    static class Line {
        double x1, y1, z1, x2, y2, z2, width, r, g, b, a;

        public Line(Vec3D point, Vec3D velocity) {
            this(point, point.plus(velocity), 1, Color.YELLOW);
        }

        public Line(Vec3D p1, Vec3D p2, double width, Color color) {
            this(p1.getX(), p1.getY(), p1.getZ(), p2.getX(), p2.getY(), p2.getZ(), width, color.getRed(),
            color.getGreen(), color.getBlue());
        }

        public Line(double x1, double y1, double z1, double x2, double y2, double z2, double width,
                    double r, double g, double b) {
            this.x1 = x1;
            this.y1 = y1;
            this.z1 = z1;
            this.x2 = x2;
            this.y2 = y2;
            this.z2 = z2;
            this.width = width;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = 0.5;
        }

        public LH h() {
            return new LH(this);
        }
    }
}

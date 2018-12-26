import com.google.gson.Gson;
import model.*;
import model.Robot;

import java.awt.*;

public final class MyStrategy implements Strategy {

    Game game;

    @Override
    public void act(Robot me, Rules rules, Game game, Action action) {
        this.game = game;
    }
    
    @Override
    public String customRendering() {
        if (game == null) return "";
        Gson gson = new Gson();
        Object[] items = new Object[]{new Sphere(game.ball, Color.BLACK).h(), new TH("tes")};
         return gson.toJson(items);
    }

    class TH {
        String Text;

        public TH(String text) {
            Text = text;
        }
    }

    class SH {
        Sphere Sphere;

        public SH(Sphere sphere) {
            this.Sphere = sphere;
        }
    }

    class Sphere {
        double x, y, z, radius, r, g, b, a;

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

    class LH {
        Line Line;

        public LH(Line line) {
            this.Line = line;
        }
    }

    class Line {
        double x1, y1, z1, x2, y2, z2, width, r, g, b, a;

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

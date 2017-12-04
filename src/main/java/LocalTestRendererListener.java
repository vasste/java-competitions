import model.*;

import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Stream;

import static java.lang.StrictMath.*;
import static model.VehicleType.*;

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
    private Player me;
    private Player enemy;
    WeatherType[][] weatherTypes;
    TerrainType[][] terrainTypes;

    public void beforeDrawScene(Graphics graphics, World world, Game game, int canvasWidth, int canvasHeight,
                                double left, double top, double width, double height) {
        updateFields(graphics, world, game, canvasWidth, canvasHeight, left, top, width, height);
        Color color = graphics.getColor();
        try {
            vehicles = world.getVehicles();
            drawCircle(new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY()), 10);
            graphics.setColor(Color.RED);
            Vehicle vehicle = cEP(true, FIGHTER);
            drawCircle(vehicle, 10);
            Rect fr = OfVG(FIGHTER);
            drawRect(fr);
            drawLine(vehicle.getX(), vehicle.getY(), fr.cX(), fr.cY());
            Line line = fr.sideW();
            drawLine(line.ps[0].x, line.ps[0].y, line.ps[1].x, line.ps[1].y);
            graphics.setColor(Color.BLACK);
            Vehicle vARRV = cEP(false, ARRV);
            drawCircle(vARRV, 10);
            Rect ar = OfVG(ARRV);
            drawRect(ar);
            drawLine(vARRV.getX(), vARRV.getY(), ar.cX(), ar.cY());
            Line aline = ar.sideW();
            drawLine(aline.ps[0].x, aline.ps[0].y, aline.ps[1].x, aline.ps[1].y);
            mkNS(graphics);
        } catch (Throwable t) {
            System.out.printf(t.getMessage());
        }
        graphics.setColor(color);
    }

    Vehicle cEP(boolean arial, VehicleType... vehicleTypes) {
        P2D center = OfVG(vehicleTypes).c();
        Vehicle ctlev = null;
        for (Vehicle v : (Iterable<Vehicle>) eV()::iterator) {
            if (arial && !v.isAerial()) continue;
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev, v, center);
        }
        return ctlev;
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
        this.me = world.getPlayers()[0];
        this.enemy = world.getPlayers()[1];

        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;

        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
        weatherTypes = world.getWeatherByCellXY();
        terrainTypes = world.getTerrainByCellXY();
    }

    Stream<Vehicle> mV() { return Arrays.stream(vehicles).filter(v -> v.getPlayerId() == me.getId()); }
    Stream<Vehicle> mVt(VehicleType... vts) { return mV().filter(v -> Arrays.stream(vts).anyMatch(vt -> v.getType() == vt)); }
    Rect OfV(Stream<Vehicle> vhs) { return vhs.reduce(new Rect.Builder(), Rect.Builder::update, Rect.Builder::combine).build(); }
    Stream<Vehicle> gV(int id) { return mV().filter(vt -> includedOr(vt, id)); }
    Stream<Vehicle> eV() { return Arrays.stream(vehicles).filter(v -> v.getPlayerId() != me.getId()); }

    static boolean includedOr(Vehicle vehicle, int... ids) {
        int[] groups = vehicle.getGroups();
        for (int j = 0; j < ids.length; j++) {
            for (int i = 0; i < groups.length; i++)
                if (ids[j] == groups[i])
                    return true;
        }
        return false;
    }

    Vehicle mkNS(Graphics graphics) {
        Rect.Builder vRB = new Rect.Builder();
        for (Vehicle fv : (Iterable<Vehicle>) mVt(FIGHTER, HELICOPTER)::iterator) {
            vRB.combine(new Rect.Builder(see(fv, game, world, weatherTypes, terrainTypes)));
        }

        Rect vR = vRB.build();
        Vehicle eVns = null;
        Vehicle cff = null;
        for (Vehicle eV : (Iterable<Vehicle>) eV().filter(v -> vR.include(v.getX(), v.getY()))::iterator) {
            for (Vehicle mV : (Iterable<Vehicle>) mVt(FIGHTER, HELICOPTER)::iterator) {
                if (see(mV, eV, world, game, weatherTypes, terrainTypes)) {
                    if (eVns == null) {
                        eVns = eV;
                        cff = mV;
                    }
                    if (cmp.compare(eVns, eV) < 0) {
                        eVns = eV;
                        cff = mV;
                    }
                }
            }
        }
        if (eVns != null) {
            graphics.setColor(Color.BLUE);
            drawCircle(cff, 10);
            graphics.setColor(Color.GREEN);
            drawCircle(eVns, 10);
            return eVns;
        }
        return null;
    }

    static Comparator<Vehicle> cmp = (o1, o2) -> {
        int ix = U.cD(o1.getX(),o2.getX());
        if (ix == 0) return U.cD(o1.getY(),o2.getY());
        return ix;
    };

    static Rect see(Vehicle v, Game game, World world, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        int[] wij = new P2D(v).inWorld(world);
        double vF = v.isAerial() ? wVf(game, weatherTypes[wij[0]][wij[1]]): tVf(game, terrainTypes[wij[0]][wij[1]]);
        double visionRange = v.getVisionRange() * vF;
        return new Rect(v.getX(), v.getY(), world, visionRange);
    }

    static boolean see(Vehicle v, Vehicle u, World world, Game game, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        int[] wij = new P2D(v).inWorld(world);
        int[] uij = new P2D(u).inWorld(world);
        double shF = u.isAerial() ? wShf(game, weatherTypes[uij[0]][uij[1]]) : tShf(game, terrainTypes[uij[0]][uij[1]]);
        double vF = v.isAerial() ? wVf(game, weatherTypes[wij[0]][wij[1]]): tVf(game, terrainTypes[wij[0]][wij[1]]);
        return v.getDistanceTo(u) <= v.getVisionRange() * shF * vF;
    }

    static boolean see(Vehicle v, Vehicle u) {
        return u.getDistanceTo(v) < v.getVisionRange();
    }
    static boolean see(P2D p, Vehicle v) { return P2D.distanceTo(p, v) < v.getVisionRange(); }
    static boolean see(Unit u, Vehicle v) { return u.getDistanceTo(v) < v.getVisionRange(); }
    static boolean attack(P2D u, Vehicle v) {
        return v.getType() == FIGHTER || v.getType() == HELICOPTER ?
                P2D.distanceTo(u, v) < v.getAerialAttackRange() : P2D.distanceTo(u, v) < v.getGroundAttackRange();
    }

    static double tSf(Game game, TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return game.getPlainTerrainSpeedFactor();
            case SWAMP:
                return game.getSwampTerrainSpeedFactor();
            case FOREST:
                return game.getForestTerrainSpeedFactor();
        }
        return 1;
    }

    static double wSf(Game game, WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return game.getClearWeatherSpeedFactor();
            case CLOUD:
                return game.getCloudWeatherSpeedFactor();
            case RAIN:
                return game.getRainWeatherSpeedFactor();
        }
        return 1;
    }

    static double wVf(Game game, WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return game.getClearWeatherVisionFactor();
            case CLOUD:
                return game.getCloudWeatherVisionFactor();
            case RAIN:
                return game.getRainWeatherVisionFactor();
        }
        return 1;
    }

    static double tVf(Game game, TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return game.getPlainTerrainVisionFactor();
            case SWAMP:
                return game.getSwampTerrainVisionFactor();
            case FOREST:
                return game.getForestTerrainVisionFactor();
        }
        return 1;
    }

    static double wShf(Game game, WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return game.getClearWeatherStealthFactor();
            case CLOUD:
                return game.getClearWeatherStealthFactor();
            case RAIN:
                return game.getRainWeatherSpeedFactor();
        }
        return 1;
    }

    static double tShf(Game game, TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return game.getPlainTerrainStealthFactor();
            case FOREST:
                return game.getForestTerrainStealthFactor();
            case SWAMP:
                return game.getSwampTerrainStealthFactor();
        }
        return 1;
    }

    Rect[] sOfVG(int... ids) {
        Rect.Builder[] initial = new Rect.Builder[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect.Builder(); initial[i].g = ids[i]; }
        Rect.Builder[] builders = mV().reduce(initial, (rects, VeEx) -> {
            for (int i = 0; i < ids.length; i++) if (includedOr(VeEx, ids[i])) rects[i].update(VeEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
        Rect[] rects = new Rect[ids.length];
        for (int i = 0; i < initial.length; i++) { rects[i] = builders[i].build(); }
        return rects;
    }

    Rect[] sOfVT(VehicleType... types) {
        Rect.Builder[] initial = new Rect.Builder[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect.Builder(); initial[i].vt = types[i]; }
        Rect.Builder[] builders = mV().reduce(initial, (rects, VeEx) -> {
            for (int i = 0; i < types.length; i++) if (VeEx.getType() == types[i]) rects[i].update(VeEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
        Rect[] rects = new Rect[types.length];
        for (int i = 0; i < initial.length; i++) { rects[i] = builders[i].build(); }
        return rects;
    }

    Rect OfVG(int... ids) {
        return mV().reduce(new Rect.Builder(), (rectangle, vehicle) -> {
            for (int id : ids) if (includedOr(vehicle, id)) rectangle.update(vehicle);
            return rectangle;
        }, Rect.Builder::combine).build();
    }

    Rect OfVG(VehicleType... types) {
        Rect.Builder builder = new Rect.Builder();
        Rect[] rects = sOfVT(types);
        for (int i = 0; i < rects.length; i++) {
            builder.combine(new Rect.Builder(rects[i]));
        }
        return builder.build();
    }

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

    private void drawCircle(Unit p, double radius) {
        drawCircle(p.getX(), p.getY(), radius);
    }

    private void drawCircle(P2D p, double radius) {
        drawCircle(p.x, p.y, radius);
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

    private void drawRect(Rect rect) {
        Point2I[] ps = Arrays.stream(rect.points()).map(p -> new Point2I(p.x, p.y)).toArray(Point2I[]::new);
        drawPolygon(ps);
        Color[] colors = new Color[]{Color.RED, Color.BLACK, Color.BLUE, Color.ORANGE};
        for (int i = 0; i < ps.length; i++) {
            graphics.setColor(colors[i]);
            drawCircle(ps[i].to(), 5);
        }

    }

    private void drawRect(double left, double top, double width, double height) {
        Point2I topLeft = toCanvasPosition(left, top);
        Point2I size = toCanvasOffset(width, height);

        graphics.drawRect(topLeft.getX(), topLeft.getY(), size.getX(), size.getY());
    }

    private void drawPolygon(Point2I... points) {
        int pointCount = points.length;

        for (int pointIndex = 1; pointIndex < pointCount; ++pointIndex) {
            Point2I pointA = points[pointIndex];
            Point2I pointB = points[pointIndex - 1];
            drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
        }

        Point2I pointA = points[0];
        Point2I pointB = points[pointCount - 1];
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

        P2D to() {
            return new P2D(x, y);
        }

        private static int toInt(double value) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int intValue = (int) value;
            if (abs((double) intValue - value) < 1.0D) {
                return intValue;
            }
            throw new IllegalArgumentException("Can't convert double " + value + " to int.");
        }
    }
}


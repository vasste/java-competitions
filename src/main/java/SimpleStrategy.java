import model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.StrictMath.hypot;

public final class SimpleStrategy implements Strategy {
    private final Map<Long, VehicleEx> vehiclesById = new HashMap<>();
    private Player me;
    private World world;
    private Queue<MoveBuild> moves = new LinkedList<>();
    private double committedMoves = 0;

    private int GT = 1;
    private int GA = 2;
    private int GF = 3;
    private int GH = 4;
    private int GI = 5;

    Map<Integer, Point2D> groupsDestination = new HashMap<>();

    private boolean initialization = false;
    private Random random;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        if (committedMoves/world.getTickIndex() >= 0.18) return;
        this.me = me;
        this.world = world;
        initializeVehicles(world);
        if (random == null) random = new Random(game.getRandomSeed());

        MoveBuild nextMove = moves.poll();
        if (nextMove == null) {
            if (!initialization) {
                Rect rectArrv = rectOfVehicles(myARRV());
                Rect rectTank = rectOfVehicles(myTANK());
                Rect rectFighter = rectOfVehicles(myFIGHTER());
                Rect rectHelicopter = rectOfVehicles(myHELICOPTER());
                Rect rectIfv = rectOfVehicles(myIFV());

                createGroup(GA, rectArrv, 1);
                createGroup(GT, rectTank, 1.5);
                createGroup(GF, rectFighter, 1);
                createGroup(GH, rectHelicopter, 1);
                createGroup(GI, rectIfv, 1.5);

                initialization = true;
            } else {
                keepMoving(GF, GT);
                keepMoving(GH, GI);
                keepMoving(GT, GA);
            }
        } else {
            committedMoves++;
            nextMove.setMove(move);
        }
    }

    private void keepMoving(int a, int b) {
        if (!keepingInGroup(a, b)) {
            if (!synchronizeGroupMove(a, b)) {
                Point2D closed = getPoint2D();
                makeGroupsMove(closed.x, closed.y, a);
                makeGroupsMove(closed.x, closed.y, b);
            }
        }
    }

    private Point2D getPoint2D() {
        Rect rect = rectOfVehicleGroup(GT, GF);
        Point2D center = new Point2D(rect.centerX(), rect.centerY());
        return enemyVehicles().reduce(new Point2D(Double.MAX_VALUE, Double.MAX_VALUE),
                (p, vehicleEx) -> closedTo(p, new Point2D(vehicleEx.vehicle), center), (a, b) -> closedTo(a, b, center));
    }

    boolean synchronizeGroupMove(int ga, int gb) {
        Rect rects[] = rectsOfVehicleGroup(ga, gb);
        boolean slow[] = new boolean[2];
        int i = 0, j = 1;
        if (rects[i].distanceTo(rects[j]) > 50) {
            if (groupsDestination.get(ga) == null) return false;
            if (groupsDestination.get(gb) == null) return false;
            if (rects[i].distanceTo(groupsDestination.get(ga)) > rects[j].distanceTo(groupsDestination.get(gb))) {
                if (slow[i]) return false;
                moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(ga));
                moves.add(new MoveBuild().setAction(ActionType.MOVE).setY(0).setY(0));
                slow[i] = true;
            } else {
                if (slow[j]) return false;
                moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(gb));
                moves.add(new MoveBuild().setAction(ActionType.MOVE).setY(0).setY(0));
                slow[j] = true;
            }
        }
        for (boolean b : slow) {
            if (b) return true;
        }
        return false;
    }

    boolean keepingInGroup(int... groups) {
        Rect rects[] = rectsOfVehicleGroup(groups);
        boolean gathering = false;
        for (int i = 0; i < rects.length; i+=2) {
            Rect iRect = rectOfVehicles(groupVehicles(groups[i]));
            Rect iPlusOneRect = rectOfVehicles(groupVehicles(groups[i + 1]));
            if (iRect.square() > 15000) {
                makeGroupMove(groups[i], iRect.centerX(), iRect.centerY());
            }
            if (iPlusOneRect.square() > 15000) {
                makeGroupMove(groups[i], iPlusOneRect.centerX(), iPlusOneRect.centerY());
            }
            if (iRect.distanceTo(iPlusOneRect) > 50) {
                double iLastTick = groupAverageLastTick(groups[i]);
                double iPlusOneLastTick = groupAverageLastTick(groups[i + 1]);
                if (world.getTickIndex() - iPlusOneLastTick > 100) rotateNeeded(groups[i + 1]);
                if (world.getTickIndex() - iLastTick > 100) rotateNeeded(groups[i]);
                if (iPlusOneLastTick < iLastTick)  {
                    makeGroupMove(groups[i + 1], iPlusOneRect, iRect.centerX(), iRect.centerY());
                    makeGroupMove(groups[i], iRect.centerX(), iRect.centerY());
                } else {
                    makeGroupMove(groups[i], iRect, iPlusOneRect.centerX(), iPlusOneRect.centerY());
                    makeGroupMove(groups[i + 1], iPlusOneRect.centerX(), iPlusOneRect.centerY());
                }
                gathering |= true;
            }
        }
        return gathering;
    }

    boolean rotateNeeded(int id) {
        double lastTick = groupVehicles(id).mapToDouble(ve -> ve.tickIndex).average().orElse(world.getTickIndex());
        if (world.getTickIndex() - lastTick > 100) {
            switch (random.nextInt(4)) {
                case 0: rotateNeeded(id, -StrictMath.PI);
                break;
                case 1: rotateNeeded(id, StrictMath.PI);
                    break;
                case 2: rotateNeeded(id, -StrictMath.PI/2);
                    break;
                case 3: rotateNeeded(id, StrictMath.PI/2);
                    break;
            }

            return true;
        }
        return false;
    }

    void rotateNeeded(int id, double angle) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(id));
        moves.add(new MoveBuild().setAction(ActionType.ROTATE).setAngle(angle));
    }

    void makeGroupsMove(double x, double y, int... ids) {
        for (int id : ids) makeGroupMove(id, x, y);
    }

    void makeGroupMove(int id, double x, double y) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(id));
        Rect rect = rectOfVehicles(groupVehicles(id));
        groupsDestination.put(id, new Point2D(x, y));
        if (rect.distanceTo(x, y) > 0)
            moves.add(new MoveBuild().setAction(ActionType.MOVE).setDistanceToXY(rect, x, y));
        else
            System.out.printf("");
    }

    void makeGroupMove(int id, Rect groupRect, double x, double y) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(id));
        moves.add(new MoveBuild().setAction(ActionType.MOVE).setDistanceToXY(groupRect, x, y));
    }

    void createGroup(int id, Rect rect, double scale) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setRect(rect));
        myVehiclesInsideRect(rect).forEach(ve -> ve.groups.add(id));
        moves.add(new MoveBuild().setAction(ActionType.ASSIGN).setGroup(id));
        moves.add(new MoveBuild().setAction(ActionType.SCALE).setFactor(scale));
    }

    void initializeVehicles(World world) {
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehiclesById.put(vehicle.getId(), new VehicleEx(vehicle, world.getTickIndex(), new HashSet<>()));
        }

        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                vehiclesById.computeIfPresent(vu.getId(),
                        (id, vehicleEx) ->
                                new VehicleEx(vehicleEx, vu, world.getTickIndex()));
            }
        }
    }

    double groupAverageLastTick(int id) { return groupVehicles(id).mapToDouble(ve -> ve.tickIndex).average().orElse(world.getTickIndex()); }
    Stream<VehicleEx> myTANK() { return myVehicleTyped(VehicleType.TANK); }
    Stream<VehicleEx> myARRV() { return myVehicleTyped(VehicleType.ARRV); }
    Stream<VehicleEx> myFIGHTER() { return myVehicleTyped(VehicleType.FIGHTER); }
    Stream<VehicleEx> myHELICOPTER() { return myVehicleTyped(VehicleType.HELICOPTER); }
    Stream<VehicleEx> myIFV() { return myVehicleTyped(VehicleType.IFV); }
    Stream<VehicleEx> myVehicles() { return vehiclesById.values().stream().filter(VehicleEx::my); }
    Stream<VehicleEx> myVehicleTyped(VehicleType type) { return myVehicles().filter(vt -> vt.ofVehicleType(type)); }
    Rect rectOfVehicles(Stream<VehicleEx> vhStream) { return vhStream.reduce(new Rect(), Rect::update, Rect::combine); }
    Stream<VehicleEx> groupVehicles(int id) { return myVehicles().filter(vt -> vt.inGroup(id)); }
    Stream<VehicleEx> mySelectedVehicle() { return myVehicles().filter(VehicleEx::isSelected).filter(VehicleEx::isSelected); }
    Stream<VehicleEx> enemyVehicles() { return vehiclesById.values().stream().filter(VehicleEx::enemy); }
    Stream<VehicleEx> enemyVehiclesOfTypes(VehicleType[] types) { return enemyVehicles().filter(ve -> ve.ofVehicleTypes(types)); }
    Stream<VehicleEx> myVehiclesInsideRect(Rect rect) { return myVehicles().filter(ve -> rect.include(ve.vehicle)); }

    Rect[] rectsOfVehicleGroup(int... ids) {
        Rect[] initial = new Rect[ids.length];
        for (int i = 0; i < initial.length; i++) initial[i] = new Rect();
        return myVehicles().reduce(initial, (rects, vehicleEx) -> {
            for (int i = 0; i < ids.length; i++)
                if (vehicleEx.inGroup(ids[i]))
                    rects[i].update(vehicleEx);
            return rects;
        }, (rl, rr) -> {
            for (int i = 0; i < rl.length; i++)
                rl[i] = rl[i].combine(rr[i]);
            return rl;
        });
    }

    Rect rectOfVehicleGroup(int... ids) {
        return myVehicles().reduce(new Rect(), (rect, vehicleEx) -> {
            for (int i = 0; i < ids.length; i++)
                if (vehicleEx.inGroup(ids[i]))
                    rect.update(vehicleEx);
            return rect;
        }, Rect::combine);
    }

    class VehicleEx {
        final Vehicle vehicle;
        final int tickIndex;
        Set<Integer> groups;

        VehicleEx(Vehicle vehicle, int tickIndex, Set<Integer> groups) {
            this.vehicle = vehicle;
            this.tickIndex = tickIndex;
            this.groups = groups;
        }

        VehicleEx(VehicleEx vehicleEx, VehicleUpdate vehicleUpdate, int tickIndex) {
            this(new Vehicle(vehicleEx.vehicle, vehicleUpdate), tickIndex, vehicleEx.groups);
        }

        boolean ofVehicleType(VehicleType type) { return vehicle.getType() == type; }

        boolean ofVehicleTypes(VehicleType[] types) {
            for (VehicleType type : types) {
                if (vehicle.getType() == type) return true;
            }
            return false;
        }

        boolean isSelected() { return vehicle.isSelected(); }
        boolean my() { return vehicle.getPlayerId() == me.getId(); }
        boolean enemy() { return vehicle.getPlayerId() != me.getId(); }
        boolean inGroup(int id) { return groups.contains(id); }
        public String toString() { return "V{" + "g:" + groups + '}'; }
    }

    static class Point2D {
        private double x;
        private double y;

        Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        Point2D(Vehicle vehicle) {
            this.x = vehicle.getX();
            this.y = vehicle.getY();
        }
    }

    static Point2D closedTo(Point2D a, Point2D b, Point2D center) {
        if (distanceTo(center, a) > distanceTo(center, b)) return b;
        else return a;
    }


    static double distanceTo(Point2D a, Point2D b) { return hypot(a.x - b.x, a.y - b.y); }
    static double distanceTo(Point2D a, Unit b) { return hypot(a.x - b.getX(), a.y - b.getX()); }

    static class Rect {
        double l = Double.NaN,t = Double.NaN,b = Double.NaN,r = Double.NaN;

        Rect() {}
        Rect(double l, double t, double b, double r) { this.l = l;this.t = t;this.b = b;this.r = r; }
        Rect(Vehicle v) { this(v.getX(), v.getY(), v.getY(), v.getX()); }

        Rect update(VehicleEx vehicleEx) {
            Vehicle vehicle = vehicleEx.vehicle;
            l = Math.min(Double.isNaN(l) ? Double.MAX_VALUE : l, vehicle.getX());
            r = Math.max(Double.isNaN(r) ? 0 : r, vehicle.getX());
            t = Math.min(Double.isNaN(t) ? Double.MAX_VALUE : t, vehicle.getY());
            b = Math.max(Double.isNaN(b) ? 0 : b, vehicle.getY());
            return this;
        }

        Rect combine(Rect rect) {
            l = Math.min(Double.isNaN(l) ? Double.MAX_VALUE : l, Double.isNaN(rect.l) ? Double.MAX_VALUE : rect.l);
            r = Math.max(Double.isNaN(r) ? 0 : r, Double.isNaN(rect.r) ? 0: rect.r);
            t = Math.min(Double.isNaN(t) ? Double.MAX_VALUE : t, Double.isNaN(rect.t) ? Double.MAX_VALUE : rect.t);
            b = Math.max(Double.isNaN(b) ? 0 : b, Double.isNaN(rect.b) ? 0 : rect.b);
            return this;
        }

        double square() { return Math.abs(r - l)*Math.abs(b - t); }
        double distanceTo(double x, double y) { return hypot(x - centerX(), y - centerY()); }
        double distanceTo(Point2D p) { return hypot(p.x - centerX(), p.y - centerY()); }
        double distanceTo(Rect rect) { return hypot(rect.centerX() - centerX(), rect.centerY() - centerY()); }
        double centerX() { return (l + r)/2; }
        double centerY() { return (b + t)/2; }
        boolean include(Vehicle vehicle) { return include(vehicle.getX(), vehicle.getY()); }
        boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
        Rect square(int i) { return new Rect(l, t, b, (l+r)/2); }
    }

    static class MoveBuild {
        private ActionType action;

        private int group;

        private double left;
        private double top;
        private double right;
        private double bottom;

        private double x;
        private double y;
        private double angle;
        private double factor;

        private double maxSpeed;
        private double maxAngularSpeed;

        private VehicleType vehicleType;

        private long facilityId = -1L;
        private long vehicleId = -1L;

        MoveBuild setAction(ActionType action) { this.action = action; return this;}
        MoveBuild setGroup(int group) { this.group = group; return this; }
        MoveBuild setLeft(double left) { this.left = left; return this; }
        MoveBuild setTop(double top) { this.top = top;return this; }
        MoveBuild setRight(double right) { this.right = right;  return this; }
        MoveBuild setBottom(double bottom) { this.bottom = bottom; return this; }
        MoveBuild setX(double x) { this.x = x; return this; }
        MoveBuild setY(double y) { this.y = y; return this; }
        MoveBuild setDistanceToXY(Rect current, double x, double y) {
            this.y = y - current.centerY();
            this.x = x - current.centerX();
            return this;
        }
        MoveBuild setAngle(double angle) { this.angle = angle; return this; }
        MoveBuild setFactor(double factor) { this.factor = factor; return this; }
        MoveBuild setMaxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; return this; }
        MoveBuild setMaxAngularSpeed(double maxAngularSpeed) { this.maxAngularSpeed = maxAngularSpeed; return this; }
        MoveBuild setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; return this; }
        MoveBuild setFacilityId(long facilityId) { this.facilityId = facilityId; return this; }
        MoveBuild setVehicleId(long vehicleId) { this.vehicleId = vehicleId; return this; }
        Move setMove(Move dst) {
            dst.setGroup(group);

            dst.setLeft(left);
            dst.setBottom(bottom);
            dst.setTop(top);
            dst.setRight(right);

            dst.setAction(action);
            dst.setX(x);
            dst.setY(y);

            dst.setAngle(angle);
            dst.setFactor(factor);
            dst.setFacilityId(facilityId);

            dst.setMaxSpeed(maxSpeed);
            dst.setMaxAngularSpeed(maxAngularSpeed);

            dst.setVehicleType(vehicleType);
            dst.setVehicleId(vehicleId);
            return dst;
        }

        MoveBuild setRect(Rect rect) {
            setLeft(rect.l);
            setRight(rect.r);
            setTop(rect.t);
            setBottom(rect.b);
            return this;
        }
    }
}

import model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.StrictMath.*;
import static model.ActionType.*;

public final class SimpleStrategy implements Strategy {
    private final Map<Long, VehicleEx> vehiclesById = new HashMap<>();
    private Player me;
    private World world;
    private Queue<MB> moves = new LinkedList<>();
    private double committedMoves = 0;
    private static double EPS = 0.000001;

    private int GT = 1;
    private int GA = 2;
    private int GF = 3;
    private int GH = 4;
    private int GI = 5;

    Map<Integer, P2D> groupsDestination = new HashMap<>();

    private boolean initialization = false;
    private Random random;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        initializeVehicles(world);
        if (committedMoves/world.getTickIndex() >= 0.18) return;
        this.me = me;
        this.world = world;
        if (random == null) random = new Random(game.getRandomSeed());

        MB nextMove = moves.poll();
        if (nextMove == null) {
            if (!initialization) {
                Rect rectArrv = rectOfVehicles(myARRV());
                Rect rectTank = rectOfVehicles(myTANK());
                Rect rectFighter = rectOfVehicles(myFIGHTER());
                Rect rectHelicopter = rectOfVehicles(myHELICOPTER());
                Rect rectIfv = rectOfVehicles(myIFV());

                createGroup(GT, rectArrv, 1);
                createGroup(GF, rectFighter, 1);

                createGroup(GA, rectTank, 1);
                createGroup(GH, rectHelicopter, 1);
                createGroup(GI, rectIfv, 2);

                initialization = true;
            } else {
                keepMoving(GF, GT);
                keepMoving(GH, GI);
                keepMoving(GF, GA);
            }
        } else {
            committedMoves++;
            nextMove.setMove(move);
        }
    }

    private void keepMoving(int a, int b) {
        if (!keepingInGroup(a, b)) {
      //      if (!synchronizeGroupMove(a, b)) {
                P2D closed = getPoint2D(a, b);
                makeGsM(closed.x, closed.y, a);
                makeGsM(closed.x, closed.y, b);
        //    }
        }
    }

    private P2D getPoint2D(int ag, int bg) {
        Rect rect = rectOfVehicleGroup(ag, bg);
        P2D center = new P2D(rect.centerX(), rect.centerY());
        return enemyVehicles().reduce(new P2D(Double.MAX_VALUE, Double.MAX_VALUE),
                (p, vehicleEx) -> closedTo(p, new P2D(vehicleEx.vehicle), center), (a, b) -> closedTo(a, b, center));
    }

    boolean synchronizeGroupMove(int ga, int gb) {
        Rect rects[] = rectsOfVehicleGroup(ga, gb);
        boolean slow[] = new boolean[2];
        int i = 0, j = 1;
        if (rects[i].distanceFromCenterTo(rects[j]) > 50) {
            if (groupsDestination.get(ga) == null) return false;
            if (groupsDestination.get(gb) == null) return false;
            if (rects[i].distanceFromCenterTo(groupsDestination.get(ga)) > rects[j].distanceFromCenterTo(groupsDestination.get(gb))) {
                if (slow[i]) return false;
                moves.add(MB.c(CLEAR_AND_SELECT).group(ga));
                moves.add(MB.c(MOVE).y(0).y(0));
                slow[i] = true;
            } else {
                if (slow[j]) return false;
                moves.add(MB.c(CLEAR_AND_SELECT).group(gb));
                moves.add(MB.c(MOVE).y(0).y(0));
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
            Rect joinded = iRect.combine(iPlusOneRect);
            double iLastTick = groupAverageLastTick(groups[i]);
            double iPlusOneLastTick = groupAverageLastTick(groups[i + 1]);
            if (iRect.distanceFromCenterTo(iPlusOneRect) > 10) {
                if (world.getTickIndex() - iPlusOneLastTick > 60) {
                    P2D[] line = new P2D[]{iRect.center(), iPlusOneRect.center()};
                    Rect rectArrv = rectOfVehicles(myARRV());
                    Rect rectIfv = rectOfVehicles(myIFV());
                    boolean rotated = false;
                    for (P2D[] straight : rectArrv.sides()) {
                        if (intersect(straight, line)) {
                            rotateNeeded(groups[i + 1], -angle(new L(line), new L(straight)));
                            rotated = true;
                            break;
                        }
                    }
                    if (!rotated)
                        for (P2D[] straight : rectIfv.sides()) {
                            if (intersect(straight, line)) {
                                rotateNeeded(groups[i + 1], angle(new L(line), new L(straight)));
                                break;
                            }
                        }
                }
                if (world.getTickIndex() - iLastTick > 60) {
                    P2D[] line = new P2D[]{iRect.center(), iPlusOneRect.center()};
                    Rect rectHelicopter = rectOfVehicles(myHELICOPTER());
                    for (P2D[] straight : rectHelicopter.sides()) {
                        if (intersect(straight, line)) {
                            rotateNeeded(groups[i], angle(new L(line), new L(straight)));
                            break;
                        }
                    }
                }
                if (joinded.square() > 20_000) {
                    makeGS(groups[i + 1]);
                    makeGS(groups[i]);
               } else {
                    makeGM(groups[i + 1], iPlusOneRect, iRect.centerX(), iRect.centerY());
                    makeGM(groups[i], iRect, iRect.centerX(), iRect.centerY());
                }
                gathering |= true;
            }
        }
        return gathering;
    }

    void rotateNeeded(int id, double angle) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(ROTATE).angle(angle));
    }

    void makeGsM(double x, double y, int... ids) {
        for (int id : ids) makeGM(id, x, y);
    }

    void makeGM(int id, double x, double y) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        Rect rect = rectOfVehicles(groupVehicles(id));
        groupsDestination.put(id, new P2D(x, y));
        if (rect.distanceFromCenterTo(x, y) > 1) {
            moves.add(MB.c(MOVE).distanceToXY(rect, x, y));
            return;
        }
        if (rect.distanceFromRightBottomTo(x, y) > .5) moves.add(MB.c(MOVE).distanceFromRBToXY(rect, x, y));
    }

    void makeGM(int id, Rect groupRect, double x, double y) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(SCALE).factor(1));
        moves.add(MB.c(MOVE).distanceToXY(groupRect, x, y));
    }

    void makeGS(int id) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(SCALE).factor(0.1));
    }

    void createGroup(int id, Rect rect, double scale) {
        moves.add(new MB(CLEAR_AND_SELECT).setRect(rect));
        myVehiclesInsideRect(rect).forEach(ve -> ve.groups.add(id));
        moves.add(MB.c(ASSIGN).group(id));
        moves.add(MB.c(SCALE).factor(scale));
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

    static class P2D {
        private double x;
        private double y;

        public P2D() {}

        P2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        P2D(Vehicle vehicle) {
            this.x = vehicle.getX();
            this.y = vehicle.getY();
        }

        boolean less(P2D p) { return x < p.x- EPS || abs(x-p.x) < EPS && y < p.y - EPS; }

        @Override
        public String toString() {
            return "(" + round(x) + "," + round(y) + ")";
        }
    }

    static class L {
        double a, b, c;

        L() {}
        L(P2D[] pq) { this(pq[0], pq[1]);}
        L(P2D p, P2D q) {
            a = p.y - q.y;
            b = q.x - p.x;
            c = - a * p.x - b * p.y;
            norm();
        }

        void norm() {
            double z = sqrt (a*a + b*b);
            if (abs(z) > EPS) a /= z;b /= z;c /= z;
        }

        double dist(P2D p) {
            return a * p.x + b * p.y + c;
        }
    }

    static double angle(L a, L b) {
        return acos(a.a*b.a + a.b*b.b);
    }

    static P2D closedTo(P2D a, P2D b, P2D center) {
        if (distanceTo(center, a) > distanceTo(center, b)) return b;
        else return a;
    }

    static boolean betw (double l, double r, double x) {
        return min(l,r) <= x + EPS && x <= max(l,r) + EPS;
    }

    static boolean intersect_1d (double a, double b, double c, double d) {
        double[] array = new double[]{a,b,c,d};
        if (a > b)  swap (array, 0, 1);
        if (c > d)  swap (array, 2, 3);
        return max (array[0], array[2]) <= min (array[1], array[3]) + EPS;
    }

    private static void swap(double[] x, int a, int b) {
        double t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    static boolean intersect(P2D a, P2D b, P2D c, P2D d, P2D left, P2D right) {
        if (!intersect_1d (a.x, b.x, c.x, d.x) || ! intersect_1d (a.y, b.y, c.y, d.y)) return false;
        L m = new L(a, b);
        L n = new L(c, d);
        double zn = det(m.a, m.b, n.a, n.b);
        if (abs (zn) < EPS) {
            if (abs (m.dist (c)) > EPS || abs (n.dist (a)) > EPS)
                return false;
            return true;
        } else {
            left.x = right.x = - det(m.c, m.b, n.c, n.b) / zn;
            left.y = right.y = - det(m.a, m.c, n.a, n.c) / zn;
            return betw (a.x, b.x, left.x)
                    && betw (a.y, b.y, left.y)
                    && betw (c.x, d.x, left.x)
                    && betw (c.y, d.y, left.y);
        }
    }


    static double det(double a, double b, double c, double d) { return a*d - b*c; }
    static double distanceTo(P2D a, P2D b) { return hypot(a.x - b.x, a.y - b.y); }
    static double distanceTo(P2D a, Unit b) { return hypot(a.x - b.getX(), a.y - b.getX()); }
    static boolean intersect(P2D a1, P2D b1, P2D a2, P2D b2) { return intersect(a1, b1, a2, b2, new P2D(), new P2D()); }
    static boolean intersect(P2D pa1, P2D pb1, P2D[] side) { return intersect(pa1, pb1, side[0], side[1]); }
    static boolean intersect(P2D[] aside, P2D[] bside) { return intersect(aside[0], aside[1], bside[0], bside[1]); }

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
        double distanceFromRightBottomTo(double x, double y) { return hypot(x - r, y - b); }
        double distanceFromCenterTo(double x, double y) { return hypot(x - centerX(), y - centerY()); }
        double distanceFromCenterTo(P2D p) { return hypot(p.x - centerX(), p.y - centerY()); }
        double distanceFromCenterTo(Rect rect) { return hypot(rect.centerX() - centerX(), rect.centerY() - centerY()); }
        P2D center() { return new P2D(centerX(), centerY()); };
        double centerX() { return (l + r)/2; }
        double centerY() { return (b + t)/2; }
        boolean include(Vehicle vehicle) { return include(vehicle.getX(), vehicle.getY()); }
        boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
        Rect square(int i) { return new Rect(l, t, b, (l+r)/2); }
        P2D[][] sides() {
            return new P2D[][]{{new P2D(l,t),new P2D(r,t)},{new P2D(r,t),new P2D(r,b)},{new P2D(r,b),new P2D(l,b)},
                    {new P2D(l, b),new P2D(l, t)}};
        }

        @Override
        public String toString() {
            return "R{l:" + round(l) + ",t:" + round(t) + ",b:" + round(b) + ",r:" + round(r) + ",[" + center() + "]" + "}";
        }
    }

    static class MB {
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

        static MB c(ActionType action) { return new MB(action); }

        public MB() { }
        public MB(ActionType action) { this.action = action; }

        MB setAction(ActionType action) { this.action = action; return this;}
        MB group(int group) { this.group = group; return this; }
        MB left(double left) { this.left = left; return this; }
        MB top(double top) { this.top = top;return this; }
        MB right(double right) { this.right = right;  return this; }
        MB bottom(double bottom) { this.bottom = bottom; return this; }
        MB x(double x) { this.x = x; return this; }
        MB y(double y) { this.y = y; return this; }
        MB distanceToXY(Rect current, double x, double y) {
            this.y = y - current.centerY();
            this.x = x - current.centerX();
            return this;
        }
        MB distanceFromRBToXY(Rect current, double x, double y) {
            this.y = y - current.centerY();
            this.x = x - current.centerX();
            return this;
        }
        MB angle(double angle) { this.angle = angle; return this; }
        MB factor(double factor) { this.factor = factor; return this; }
        MB maxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; return this; }
        MB maxAngularSpeed(double maxAngularSpeed) { this.maxAngularSpeed = maxAngularSpeed; return this; }
        MB vehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; return this; }
        MB facilityId(long facilityId) { this.facilityId = facilityId; return this; }
        MB vehicleId(long vehicleId) { this.vehicleId = vehicleId; return this; }
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

        MB setRect(Rect rect) {
            left(rect.l);
            right(rect.r);
            top(rect.t);
            bottom(rect.b);
            return this;
        }
    }
}

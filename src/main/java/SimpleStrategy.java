import model.*;

import java.util.*;
import java.util.stream.Stream;

public final class SimpleStrategy implements Strategy {
    private final Map<Long, VehicleEx> vehiclesById = new HashMap<>();
    private Player me;
    private World world;
    private Queue<MoveBuild> moves = new LinkedList<>();
    private double committedMoves = 0;

    private int G1T = 11;
    private int G1A = 12;
    private int G1F = 13;
    private int G1H = 14;
    private int G1I = 15;
    int[] groups1 = new int[]{G1A, G1F, G1H, G1I, G1T};

    private int G2T = 21;
    private int G2A = 22;
    private int G2F = 23;
    private int G2H = 24;
    private int G2I = 25;
    int[] groups2 = new int[]{G1A, G1F, G1H, G1I, G1T};

    private boolean initialization = false;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        if (committedMoves/world.getTickIndex() >= 0.1) return;
        this.me = me;
        this.world = world;
        initializeVehicles(world);

        MoveBuild nextMove = moves.poll();
        if (nextMove == null) {
            if (!initialization) {
                Rect rectArrv = rectOfVehicles(myARRV());
                Rect rectTank = rectOfVehicles(myTANK());
                Rect rectFighter = rectOfVehicles(myFIGHTER());
                Rect rectHelicopter = rectOfVehicles(myHELICOPTER());
                Rect rectIfv = rectOfVehicles(myIFV());

                createGroup(G1A, rectArrv.leftHalf());
                createGroup(G2A, rectArrv.rightHalf());

                createGroup(G1T, rectTank.leftHalf());
                createGroup(G2T, rectTank.rightHalf());

                createGroup(G1F, rectFighter.leftHalf());
                createGroup(G2F, rectFighter.rightHalf());

                createGroup(G1H, rectHelicopter.leftHalf());
                createGroup(G2H, rectHelicopter.rightHalf());

                createGroup(G1I, rectIfv.leftHalf());
                createGroup(G2I, rectIfv.rightHalf());

                initialization = true;
            } else {
                if (syncronizeGroupMove(groups1) || syncronizeGroupMove(groups2)) return;

                makeGroupsMove(VehicleType.TANK, G1A, G2A);
                makeGroupsMove(VehicleType.TANK, G1T, G2T);
                makeGroupsMove(VehicleType.FIGHTER, G1F, G2F);
                makeGroupsMove(VehicleType.HELICOPTER, G1H, G2H);
                makeGroupsMove(VehicleType.IFV, G1I, G2I);
            }
        } else {
            committedMoves++;
            nextMove.setMove(world, move);
        }
    }

    boolean syncronizeGroupMove(int[] groups) {
        Rect[] rects = rectsOfVehicleGroup(groups);
        boolean[] used = new boolean[groups.length];
        boolean syncronizeRequired = false;
        for (int i = 0; i < rects.length; i++) {
            for (int j = 0; j < rects.length; j++) {
                if (i > j && rects[i].distanceTo(rects[j]) > 200 && !used[i]) {
                    moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(groups[i]));
                    moves.add(new MoveBuild().setAction(ActionType.MOVE).setX(rects[j].centerX()).setY(rects[j].centerY()));
                    used[i] = true;
                    syncronizeRequired = true;
                }
            }
        }
        return syncronizeRequired;
    }

    void makeGroupsMove(double x, double y, int... ids) {
        for (int id : ids) makeGroupMove(id, x, y);
    }

    void makeGroupMove(int id, double x, double y) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(id));
        Rect rect = rectOfVehicles(groupVehicles(id));
        if (rect.distanceTo(x, y) > 20) moves.add(new MoveBuild().setAction(ActionType.MOVE).setY(y).setX(x));
    }

    void makeGroupsMove(VehicleType type, int... ids) {
        Point2D closed = enemyVehiclesOfTypes(pTTByVT.get(type)).reduce(new Point2D(Double.MAX_VALUE, Double.MAX_VALUE),
                (p, vehicleEx) -> p.min(new Point2D(vehicleEx.vehicle)), Point2D::min);
        makeGroupsMove(closed.x, closed.y, ids);
    }

    void createGroup(int id, Rect rect) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setRect(rect));
        myVehiclesInsideRect(rect).forEach(ve -> ve.groups.add(id));
        moves.add(new MoveBuild().setAction(ActionType.ASSIGN).setGroup(id));
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

    Stream<VehicleEx> myTANK() { return myVehicleTyped(VehicleType.TANK); }
    Stream<VehicleEx> myARRV() { return myVehicleTyped(VehicleType.ARRV); }
    Stream<VehicleEx> myFIGHTER() { return myVehicleTyped(VehicleType.FIGHTER); }
    Stream<VehicleEx> myHELICOPTER() { return myVehicleTyped(VehicleType.HELICOPTER); }
    Stream<VehicleEx> myIFV() { return myVehicleTyped(VehicleType.IFV); }
    Stream<VehicleEx> myVehicles() { return vehiclesById.values().stream().filter(VehicleEx::my); }
    Stream<VehicleEx> myVehicleTyped(VehicleType type) { return myVehicles().filter(vt -> vt.ofVehicleType(type)); }
    Rect rectOfVehicles(Stream<VehicleEx> vhStream) {
        return vhStream.reduce(new Rect(), Rect::update, Rect::combine); }
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

        boolean ofVehicleType(VehicleType type) {
            return vehicle.getType() == type;
        }

        boolean ofVehicleTypes(VehicleType[] types) {
            for (VehicleType type : types) {
                if (vehicle.getType() == type) return true;
            }
            return false;
        }

        boolean isSelected() {
            return vehicle.isSelected();
        }

        boolean my() {
            return vehicle.getPlayerId() == me.getId();
        }

        boolean enemy() {
            return vehicle.getPlayerId() != me.getId();
        }

        boolean inGroup(int id) {
            return groups.contains(id);
        }

        @Override
        public String toString() {
            return "V{" + "g:" + groups + '}';
        }
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

        Point2D min(Point2D d) {
            if (x < d.x) return this;
            if (y < d.y) return this;
            return d;
        }
    }

    static class Rect {
        double l = Double.NaN,t = Double.NaN,b = Double.NaN,r = Double.NaN;

        Rect() {}
        Rect(double l, double t, double b, double r) { this.l = l;this.t = t;this.b = b;this.r = r; }
        Rect(Vehicle v) { this(v.getX(), v.getY(), v.getY(), v.getX()); }

        Rect update(VehicleEx vehicleEx) {
            Vehicle vehicle = vehicleEx.vehicle;
            l = Math.min(Double.isNaN(l) ? Double.MAX_VALUE : l, vehicle.getX());
            r = Math.max(Double.isNaN(r) ? Double.MIN_VALUE : r, vehicle.getX());
            t = Math.min(Double.isNaN(t) ? Double.MAX_VALUE : t, vehicle.getY());
            b = Math.max(Double.isNaN(b) ? Double.MIN_VALUE : b, vehicle.getY());
            return this;
        }

        Rect combine(Rect rect) {
            l = Math.min(Double.isNaN(l) ? Double.MAX_VALUE : l, Double.isNaN(rect.l) ? Double.MAX_VALUE : rect.l);
            r = Math.max(Double.isNaN(r) ? Double.MIN_VALUE : r, Double.isNaN(rect.r) ? Double.MIN_VALUE : rect.r);
            t = Math.min(Double.isNaN(t) ? Double.MAX_VALUE : t, Double.isNaN(rect.t) ? Double.MAX_VALUE : rect.t);
            b = Math.max(Double.isNaN(b) ? Double.MIN_VALUE : b, Double.isNaN(rect.b) ? Double.MIN_VALUE : rect.b);
            return this;
        }

        double square() { return Math.abs(r - l)*Math.abs(b - t); }

        double distanceTo(double x, double y) {
            double rx = x - centerX();
            double ry = y - centerY();
            return Math.sqrt(rx*rx + ry*ry);
        }

        double distanceTo(Rect rect) {
            double x = rect.centerX() - centerX();
            double y = rect.centerY() - centerY();
            return Math.sqrt(x*x + y*y);
        }

        double centerX() { return (l + r)/2; }
        double centerY() { return (b + t)/2; }
        boolean include(Vehicle vehicle) { return include(vehicle.getX(), vehicle.getY()); }
        boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
        Rect leftHalf() { return new Rect(l, t, b, (l+r)/2); }
        Rect rightHalf() { return new Rect((l+r)/2 + 0.5, t, b, r); }
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
        MoveBuild setAngle(double angle) { this.angle = angle; return this; }
        MoveBuild setFactor(double factor) { this.factor = factor; return this; }
        MoveBuild setMaxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; return this; }
        MoveBuild setMaxAngularSpeed(double maxAngularSpeed) { this.maxAngularSpeed = maxAngularSpeed; return this; }
        MoveBuild setVehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; return this; }
        MoveBuild setFacilityId(long facilityId) { this.facilityId = facilityId; return this; }
        MoveBuild setVehicleId(long vehicleId) { this.vehicleId = vehicleId; return this; }
        Move setMove(World world, Move dst) {
            dst.setGroup(group);

            dst.setLeft(left);
            dst.setBottom(bottom);
            dst.setTop(top);
            dst.setRight(right);

            dst.setAction(action);
            dst.setX(world.getWidth() - x);
            dst.setY(world.getHeight() - y);

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

    private static final Map<VehicleType, VehicleType[]> pTTByVT;
    static {
        pTTByVT = new EnumMap<>(VehicleType.class);
        pTTByVT.put(VehicleType.FIGHTER,
                new VehicleType[] {VehicleType.HELICOPTER, VehicleType.FIGHTER});
        pTTByVT.put(VehicleType.HELICOPTER,
                new VehicleType[] {VehicleType.TANK, VehicleType.ARRV});
        pTTByVT.put(VehicleType.IFV,
                new VehicleType[] {VehicleType.HELICOPTER, VehicleType.FIGHTER});
        pTTByVT.put(VehicleType.TANK,
                new VehicleType[] {VehicleType.IFV, VehicleType.ARRV, VehicleType.TANK});
    }
}

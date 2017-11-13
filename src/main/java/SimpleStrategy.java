import model.*;
import org.jcp.xml.dsig.internal.dom.DOMUtils;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;

public final class SimpleStrategy implements Strategy {
    private final Map<Long, VehicleEx> vehiclesById = new HashMap<>();
    private Player me;
    private Queue<MoveBuild> moves = new LinkedList<>();
    private double committedMoves = 0;

    private int G1T = 11;
    private int G1A = 12;
    private int G1F = 13;
    private int G1H = 14;
    private int G1I = 15;

    private int G2T = 21;
    private int G2A = 22;
    private int G2F = 23;
    private int G2H = 24;
    private int G2I = 25;

    private boolean initialization = false;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        if (committedMoves/world.getTickIndex() >= 5) return;
        this.me = me;
        initializeVehicles(world);

        MoveBuild nextMove = moves.poll();
        if (nextMove == null) {
            if (!initialization) {
                Rect rectArrv = rectOfVehicles(myARRV());
                Rect rectTank = rectOfVehicles(myTANK());
                Rect rectFighter = rectOfVehicles(myFIGHTER());
                Rect rectHelicopter = rectOfVehicles(myHELICOPTER());
                Rect rectIfv = rectOfVehicles(myIFV());

                createGroup(G1A, rectArrv, Side.LEFT);
                createGroup(G2A, rectArrv, Side.RIGHT); // brown

                createGroup(G1T, rectTank, Side.LEFT);
                createGroup(G2T, rectTank, Side.RIGHT); // red

                createGroup(G1F, rectFighter, Side.LEFT); // yellow
                createGroup(G2F, rectFighter, Side.RIGHT);

                createGroup(G1H, rectHelicopter, Side.LEFT);
                createGroup(G2H, rectHelicopter, Side.RIGHT);

                createGroup(G1I, rectIfv, Side.LEFT); // orange
                createGroup(G2I, rectIfv, Side.RIGHT);

                initialization = true;
            } else {
//                Rect[] rects1 = rectsOfVehicleGroup(G1A, G1F, G1H, G1I, G1T);
//                Rect[] rects2 = rectsOfVehicleGroup(G2A, G2F, G2H, G2I, G2T);

                 makeGroupsMove(world.getWidth(), 0,
                        G2I);
            }
        } else {
            committedMoves++;
            nextMove.setMove(move);
        }
    }

    void makeGroupsMove(double x, double y, int... ids) {
        for (int id : ids) makeGroupMove(id, x, y);
    }

    void makeGroupMove(int id, double x, double y) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setGroup(id));
        moves.add(new MoveBuild().setAction(ActionType.MOVE).setY(y).setX(x));
    }

    void createGroup(int id, Rect rect, Side side) {
        moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setHalf(rect, side));
        moves.add(new MoveBuild().setAction(ActionType.ASSIGN).setGroup(id));
    }

    void initializeVehicles(World world) {
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehiclesById.put(vehicle.getId(), new VehicleEx(vehicle, world.getTickIndex()));
        }

        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                vehiclesById.computeIfPresent(vu.getId(),
                        (id, vehicleEx) -> new VehicleEx(vehicleEx, vu, world.getTickIndex()));
            }
        }
    }

    Stream<VehicleEx> myTANK() { return myVehicleTyped(VehicleType.TANK); }
    Stream<VehicleEx> myARRV() { return myVehicleTyped(VehicleType.ARRV); }
    Stream<VehicleEx> myFIGHTER() {
        return myVehicleTyped(VehicleType.FIGHTER);
    }
    Stream<VehicleEx> myHELICOPTER() {
        return myVehicleTyped(VehicleType.HELICOPTER);
    }
    Stream<VehicleEx> myIFV() {
        return myVehicleTyped(VehicleType.IFV);
    }
    Stream<VehicleEx> myVehicles() { return vehiclesById.values().stream().filter(VehicleEx::my); }
    Stream<VehicleEx> enemyVehicles() { return vehiclesById.values().stream().filter(VehicleEx::enemy); }
    Stream<VehicleEx> myVehicleTyped(VehicleType type) { return myVehicles().filter(vt -> vt.ofVehicleType(type)); }
    Rect rectOfVehicles(Stream<VehicleEx> vhStream) { return vhStream.reduce(new Rect(), Rect::update, Rect::combine); }

    Rect[] rectsOfVehicleGroup(int... ids) {
        return myVehicles().reduce(new Rect[ids.length], (rects, vehicleEx) -> {
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

        VehicleEx(Vehicle vehicle, int tickIndex) {
            this.vehicle = vehicle;
            this.tickIndex = tickIndex;
            this.groups = new HashSet<>();
            for (Integer gid : vehicle.getGroups())
                this.groups.add(gid);
        }

        VehicleEx(VehicleEx vehicleEx, VehicleUpdate vehicleUpdate, int tickIndex) {
            this(new Vehicle(vehicleEx.vehicle, vehicleUpdate), tickIndex);
        }

        boolean ofVehicleType(VehicleType type) {
            return vehicle.getType() == type;
        }

        double getMinAttackRange() {
            return Math.min(vehicle.getAerialAttackRange(), vehicle.getGroundAttackRange());
        }

        boolean my() {
            return vehicle.getPlayerId() == me.getId();
        }

        boolean enemy() {
            return vehicle.getPlayerId() == me.getId();
        }

        boolean inGroup(int id) {
            return groups.contains(id);
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

        double square() {
            return Math.abs(r - l)*Math.abs(b - t);
        }

        double distanceTo(Rect rect) {
            double x = rect.centerX() - centerX();
            double y = rect.centerY() - centerY();
            return x*x + y*y;
        }

        double centerX() {
            return (l + r)/2;
        }

        double centerY() {
            return (b + t)/2;
        }
    }

    class MoveBuild {
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

        MoveBuild setHalf(Rect rect, Side side) {
            if (side == Side.LEFT) {
                setLeft(rect.l);
                setRight((rect.l + rect.r) / 2);
            } else {
                setLeft((rect.l + rect.r) / 2);
                setRight(rect.r);
            }

            setTop(rect.t);
            setBottom(rect.b);

            return this;
        }
    }

    enum Side { LEFT, RIGHT }

    private static final Map<VehicleType, VehicleType[]> preferredTargetTypesByVehicleType;
    static {
        preferredTargetTypesByVehicleType = new EnumMap<>(VehicleType.class);

        preferredTargetTypesByVehicleType.put(VehicleType.FIGHTER, new VehicleType[] {
                VehicleType.HELICOPTER, VehicleType.FIGHTER
        });

        preferredTargetTypesByVehicleType.put(VehicleType.HELICOPTER, new VehicleType[] {
                VehicleType.TANK, VehicleType.ARRV, VehicleType.HELICOPTER, VehicleType.IFV, VehicleType.FIGHTER
        });

        preferredTargetTypesByVehicleType.put(VehicleType.IFV, new VehicleType[] {
                VehicleType.HELICOPTER, VehicleType.ARRV, VehicleType.IFV, VehicleType.FIGHTER, VehicleType.TANK
        });

        preferredTargetTypesByVehicleType.put(VehicleType.TANK, new VehicleType[] {
                VehicleType.IFV, VehicleType.ARRV, VehicleType.TANK, VehicleType.FIGHTER, VehicleType.HELICOPTER
        });
    }
}

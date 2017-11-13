import model.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

public final class SimpleStrategy implements Strategy {
    private final Map<Long, VehicleTick> vehiclesById = new HashMap<>();
    private Player me;
    private Queue<MoveBuild> moves = new LinkedList<>();

    private int G1 = 1;
    private int G2 = 2;
    private boolean initialization = false;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        initializeVehicles(world);

        MoveBuild nextMove = moves.poll();
        if (nextMove == null) {
            if (!initialization) {
                Rect rectArrv = myARRV().reduce(new Rect(), Rect::update, Rect::combine);
                Rect rectTank = myTANK().reduce(new Rect(), Rect::update, Rect::combine);
                Rect rectFighter = myFIGHTER().reduce(new Rect(), Rect::update, Rect::combine);
                Rect rectHelicopter = myHELICOPTER().reduce(new Rect(), Rect::update, Rect::combine);
                Rect rectIfv = myIFV().reduce(new Rect(), Rect::update, Rect::combine);
                moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setLeftHalf(rectArrv));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setLeftHalf(rectTank));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setLeftHalf(rectFighter));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setLeftHalf(rectHelicopter));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setLeftHalf(rectIfv));
                moves.add(new MoveBuild().setAction(ActionType.ASSIGN).setGroup(G1));

                moves.add(new MoveBuild().setAction(ActionType.CLEAR_AND_SELECT).setRightHalf(rectArrv));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setRightHalf(rectTank));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setRightHalf(rectFighter));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setRightHalf(rectHelicopter));
                moves.add(new MoveBuild().setAction(ActionType.ADD_TO_SELECTION).setRightHalf(rectIfv));
                moves.add(new MoveBuild().setAction(ActionType.ASSIGN).setGroup(G2));
                initialization = true;
            } else {
                move.setAction(ActionType.MOVE);
                Rect g1Rect = rectOfVehileGroup(G1);
                Rect g2Rect = rectOfVehileGroup(G2);
                if ()
                move.setGroup(ThreadLocalRandom.current().nextBoolean() ? G1 : G2);

                move.setY(world.getHeight()/2);
                move.setX(world.getWidth()/2);
            }
        } else {
            nextMove.setMove(move);
        }
    }

    private void initializeVehicles(World world) {
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehiclesById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex()));
        }

        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                vehiclesById.computeIfPresent(vu.getId(),
                        (id, vehicleTick) -> new VehicleTick(vehicleTick, vu, world.getTickIndex()));
            }
        }
    }

    Stream<VehicleTick> myTANK() {
        return myVehicles().filter(vt -> vt.ofVehicleType(VehicleType.TANK));
    }

    Stream<VehicleTick> myARRV() {
        return myVehicles().filter(vt -> vt.ofVehicleType(VehicleType.ARRV));
    }

    Stream<VehicleTick> myFIGHTER() {
        return myVehicles().filter(vt -> vt.ofVehicleType(VehicleType.FIGHTER));
    }

    Stream<VehicleTick> myHELICOPTER() {
        return myVehicles().filter(vt -> vt.ofVehicleType(VehicleType.HELICOPTER));
    }

    Stream<VehicleTick> myIFV() {
        return myVehicles().filter(vt -> vt.ofVehicleType(VehicleType.IFV));
    }

    Stream<VehicleTick> myVehicles() {
        return vehiclesById.values().stream().filter(vt -> vt.vehicle.getPlayerId() == me.getId());
    }

    Stream<VehicleTick> enemyVehicles() {
        return vehiclesById.values().stream().filter(vt -> vt.vehicle.getPlayerId() != me.getId());
    }

    Rect rectOfVehileGroup(int groupId) {
        return myVehicles().filter(vh -> vh.groups.contains(groupId)).reduce(new Rect(), Rect::update, Rect::combine);
    }

    static class VehicleTick {
        final Vehicle vehicle;
        final int tickIndex;
        Set<Integer> groups;

        VehicleTick(Vehicle vehicle, int tickIndex) {
            this.vehicle = vehicle;
            this.tickIndex = tickIndex;
            this.groups = new HashSet<>();
            for (Integer gid : vehicle.getGroups()) {
                this.groups.add(gid);
            }
        }

        VehicleTick(VehicleTick vehicleTick, VehicleUpdate vehicleUpdate, int tickIndex) {
            this(new Vehicle(vehicleTick.vehicle, vehicleUpdate), tickIndex);
        }

        boolean ofVehicleType(VehicleType type) {
            return vehicle.getType() == type;
        }
    }

    static class Rect {
        double l,t,b,r;

        Rect() {}

        Rect(double l, double t, double b, double r) {
            this.l = l;
            this.t = t;
            this.b = b;
            this.r = r;
        }

        Rect(Vehicle v) {
            this(v.getX(), v.getY(), v.getY(), v.getX());
        }

        Rect update(VehicleTick vehicleTick) {
            Vehicle vehicle = vehicleTick.vehicle;
            l = Math.min(l, vehicle.getX());
            r = Math.max(r, vehicle.getX());
            t = Math.min(t, vehicle.getY());
            b = Math.max(b, vehicle.getY());
            return this;
        }

        Rect combine(Rect rect) {
            l = Math.min(l, rect.l);
            r = Math.max(r, rect.r);
            t = Math.min(t, rect.t);
            b = Math.max(b, rect.b);
            return this;
        }

        double square() {
            return Math.abs(r - l)*Math.abs(b - t);
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

        MoveBuild setLeftHalf(Rect rect) {
            setLeft(rect.l);
            setRight(rect.r/2);

            setTop(rect.t);
            setBottom(rect.b);

            return this;
        }

        MoveBuild setRightHalf(Rect rect) {
            setLeft(rect.r/2);
            setRight(rect.r);

            setTop(rect.t);
            setBottom(rect.b);

            return this;
        }
    }

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

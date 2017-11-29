import model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.PI;
import static java.lang.Math.round;
import static model.ActionType.*;
import static model.ActionType.ASSIGN;
import static model.VehicleType.*;
import static model.VehicleType.ARRV;

/**
 * @author Vasilii Stepanov.
 * @since 01.12.2017
 */
public class StrategyLogic {
    Player me;
    Player enemy;
    World world;
    Game game;
    WeatherType[][] weatherTypes;
    TerrainType[][] terrainTypes;
    final Map<Long, VehicleTick> mVById = new HashMap<>();
    final Map<Long, VehicleTick> eVById = new HashMap<>();
    final Map<VehicleType, Accumulator> vehicles = new HashMap<>();
    Queue<MoveBuilder> moves = new LinkedList<>();
    Map<Integer, P2D> vGd = new HashMap<>();
    P2D nuclearStrikePoint;
    Map<Integer, Rectangle> nuclearStrikeGroupRects = new HashMap<>();
    boolean noArial = false;
    Queue<MoveBuilder> positioningMoves = new LinkedList<>();
    Map<VehicleType, MyStrategy.VehicleTypeState> vehicleTypeStateMap = new HashMap<>();
    Map<Integer, Boolean> gatheredHorizontally = new HashMap<>();
    VehicleType[] mainOrder;
    Random random;

    Map<VehicleType, Accumulator> update(Player me, World world, Game game) {
        this.me = me;
        this.world = world;
        this.game = game;
        Map<VehicleType, Accumulator> vu = initVehicles(world);
        if (random == null) random = new Random(game.getRandomSeed());
        Player[] players = world.getPlayers();
        for (int i = 0; i < players.length; i++) if (players[i].getId() != me.getId()) enemy = players[i];
        weatherTypes = world.getWeatherByCellXY();
        terrainTypes = world.getTerrainByCellXY();
        if (enemy.getNextNuclearStrikeTickIndex() > 0) {
            nuclearStrikePoint = new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
        }
        return vu;
    }

    void updateNuclearStrikeGroupPoint(int gid, Rectangle nuclearStrikeGroupRect) {
        nuclearStrikeGroupRects.put(gid, nuclearStrikeGroupRect);
    }

    Rectangle nuclearStrikeGroupPoint(int gid) {
        return nuclearStrikeGroupRects.get(gid);
    }

    MoveBuilder nextMove() {
        return moves.poll();
    }

    void addNextMove(MoveBuilder builder) {
        if (builder == null) return;
        moves.add(builder);
    }

    Accumulator acc(Map<VehicleType, Accumulator> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, Accumulator.ZERO); }
    Accumulator sum(Map<VehicleType, Accumulator> vu, VehicleType... vehicleType) {
        return new Accumulator(Arrays.stream(vehicleType).map(vt -> acc(vu, vt)).mapToLong(Accumulator::v).sum());
    }

    void zipGroup(int id, double speed, VehicleType... types) {
        Rectangle r = new Rectangle();
        Set<Long> coordinates = mVg(id).map(v -> {
            r.update(v);
            if (gatheredHorizontally.get(id)) return round(v.x());
            else return round(v.y());
        }).collect(Collectors.toSet());
        List<Long> sorted = new ArrayList<>(coordinates);
        sorted.sort(Long::compareTo);
        for (int i = 1; i < sorted.size(); i++) {
            if (gatheredHorizontally.get(id)) {
                double rx = sorted.get(i);
                Rectangle vr = new Rectangle(rx - 2, r.t, r.b, rx + 2);
                moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(types[0]).setRect(vr));
                for (int j = 1; j < types.length; j++) {
                    moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(types[j]).setRect(vr));
                }
                moves.add(MoveBuilder.c(MOVE).x(round(sorted.get(0) - sorted.get(i) + 7*i)).maxSpeed(speed));
            } else {
                double ry = sorted.get(i);
                Rectangle vr = new Rectangle(r.l, ry - 2, ry + 2, r.r);
                moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(types[0]).setRect(vr));
                for (int j = 1; j < types.length; j++) {
                    moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(types[j]).setRect(vr));
                }
                moves.add(MoveBuilder.c(MOVE).y(round(sorted.get(0) - sorted.get(i) + 7*i)).maxSpeed(speed));
            }
        }
    }

    void unzipGroup(int id, double speed, VehicleType... types) {
        Rectangle r = new Rectangle();
        Set<Long> coordinates = mVg(id).map(v -> {
            r.update(v);
            if (gatheredHorizontally.get(id)) return round(v.y());
            else return round(v.x());
        }).collect(Collectors.toSet());
        List<Long> sorted = new ArrayList<>(coordinates);
        sorted.sort(Long::compareTo);
        for (int i = sorted.size() - 1; i >= 1; i--) {
            if (gatheredHorizontally.get(id)) {
                double ry = sorted.get(i);
                Rectangle vr = new Rectangle(r.l, ry - 2, ry + 2, r.r);
                moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(types[0]).setRect(vr));
                for (int j = 1; j < types.length; j++) {
                    moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(types[j]).setRect(vr));
                }
                moves.add(MoveBuilder.c(MOVE).y(i*8 + .5).maxSpeed(speed));
            } else {
                double rx = sorted.get(i);
                Rectangle vr = new Rectangle(rx - 2, r.t, r.b, rx + 2);
                moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(types[0]).setRect(vr));
                for (int j = 1; j < types.length; j++) {
                    moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(types[j]).setRect(vr));
                }
                moves.add(MoveBuilder.c(MOVE).x(i*8 + .5).maxSpeed(speed));
            }
        }
    }

    Map<VehicleType, Accumulator> initVehicles(World world) {
        Map<VehicleType, Accumulator> vehicleUpdates = new HashMap<>();
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicles.computeIfAbsent(vehicle.getType(), t -> new Accumulator()).inc();
            if (vehicle.getPlayerId() == me.getId()) {
                mVById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex()));
            } else {
                eVById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex()));
            }
        }

        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            if (vu.getDurability() == 0) {
                VehicleTick vt = mVById.remove(vu.getId());
                if (vt != null) vehicles.get(vt.type()).dec();
                eVById.remove(vu.getId());
            } else {
                VehicleTick oV = mVById.get(vu.getId());
                VehicleTick nV = mVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex()));
                if (oV != null && (Double.compare(oV.y(), nV.y()) != 0 || Double.compare(oV.x(), nV.x()) != 0)) {
                    vehicleUpdates.computeIfAbsent(oV.type(), t -> new Accumulator(0)).inc();
                }
                eVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex()));
            }
        }
        return vehicleUpdates;
    }

    void protectGround(int arial, int ground) {
        Rectangle srs = OfVG(arial);
        Rectangle drs = OfVG(ground);
        if (srs.dfct(drs) > U.EPS) {
            makeGroupMove(arial, srs, drs.cX(), drs.cY(), 0, true);
        }
    }

    Rectangle cFlp(VehicleTick lc) {
        return new Rectangle(lc.x() - 2.0, lc.y() - 2.0, lc.y() + 84.0, lc.x() + 84.0);
    }

    boolean gatherAG(double scale, MyStrategy.GroupGameState[] ggs, int i, Rectangle... rectangles) {
        Rectangle rectangleA = rectangles[0]; Rectangle rectangleB = rectangles[1];
        boolean gd = ggs[i] == MyStrategy.GroupGameState.F || rectangles[0].dfct(rectangles[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rectangleA.cX() != rectangleB.cX() && rectangleA.cY() != rectangleB.cY()) {
                    ggs[i] = rectangleA.b > rectangleB.t ? MyStrategy.GroupGameState.INY : MyStrategy.GroupGameState.INX;
                }
                else if (rectangleA.cX() != rectangleB.cX()) ggs[i] = MyStrategy.GroupGameState.INY;
                else if (rectangleA.cY() != rectangleB.cY()) ggs[i] = MyStrategy.GroupGameState.INX;
                break;
            case INX:
                if (U.eD(rectangleA.cX(), rectangleB.cX() + scale)) ggs[i] = MyStrategy.GroupGameState.NY;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleB.cX() + scale, rectangleA.cY());
                break;
            case INY:
                if (U.eD(rectangleA.cY(), rectangleB.cY() + scale)) ggs[i] = MyStrategy.GroupGameState.NX;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleA.cX(), rectangleB.cY() + scale);
                break;
            case NX:
                if (U.eD(rectangleA.cX(), rectangleB.cX())) ggs[i] = MyStrategy.GroupGameState.F;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleB.cX(), rectangleA.cY());
                break;
            case NY:
                if (U.eD(rectangleA.cY(), rectangleB.cY())) ggs[i] = MyStrategy.GroupGameState.F;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleA.cX(), rectangleB.cY());
                break;
        }
        return gd;
    }

    private VehicleTick cEP(boolean arial, int... groups) {
        P2D center = OfVG(groups).c();
        VehicleTick ctlev = null;
        for (VehicleTick v : (Iterable<VehicleTick>) eV()::iterator) {
            if (arial && !v.v.isAerial()) continue;
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev,v, center);
        }
        return ctlev;
    }

    boolean setupNuclearStrike(int id) {
        if (me.getNextNuclearStrikeTickIndex() > 0 || me.getRemainingNuclearStrikeCooldownTicks() > 0) return false;
        P2D eRc = OfV(eV()).c();
        VehicleTick cff = null;
        for (VehicleTick fv : (Iterable<VehicleTick>) mVt(FIGHTER, HELICOPTER)::iterator) {
            if (cff == null) cff = fv;
            else cff = P2D.closedTo(fv, cff, eRc);
        }
        VehicleTick eVns = null;
        if (cff != null) {
            P2D pcff = new P2D(cff);
            for (VehicleTick eV : (Iterable<VehicleTick>) eV()::iterator) {
                if (cff.see(eV, game, world, weatherTypes, terrainTypes)) {
                    if (eVns == null) eVns = eV;
                    else eVns = P2D.futherTo(eVns, eV, pcff);
                }
            }
            if (eVns != null) {
                Rectangle rectangle = sOfVG(id)[0];
                makeGroupMove(id, rectangle, rectangle.cX(), rectangle.cY(), 0, true);
                moves.add(new MoveBuilder(TACTICAL_NUCLEAR_STRIKE).vehicleId(cff.id()).x(eVns.x()).y(eVns.y()));
                return true;
            }
        }
        return false;
    }

    boolean makeTacticalGroupMove(int id, double x, double y, double ms, long updates) {
        P2D destination = vGd.get(id);
        if (destination != null && destination.compareTo(new P2D(x, y)) == 0 && updates > 0)
            return true;
        Rectangle rectangle = OfV(gV(id));
        if (rectangle.include(x, y)) {
            vGd.remove(id);
            if (rectangle.square() > 15_000) zipGroup(id, 0/*, rectangle.cX(), rectangle.cY()*/);
            else {
                P2D center = new P2D(x, y);
                VehicleTick ctlev = null;
                for (VehicleTick v : (Iterable<VehicleTick>) mV()::iterator) {
                    if (ctlev == null) ctlev = v;
                    else ctlev = P2D.closedTo(ctlev, v, center);
                }
                if (ctlev == null || U.eD(x, ctlev.x()) && U.eD(y, ctlev.y())) rotateGroup(id, PI / 4, rectangle.cX(), rectangle.cY(), 0);
                else {
                    moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
                    moves.add(MoveBuilder.c(MOVE).x(x - ctlev.x()).y(y - ctlev.y()).maxSpeed(ms));
                    vGd.put(id, new P2D(x, y, world.getTickIndex()));
                }
            }
            return false;
        } else {
            moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
            if ((rectangle.r - rectangle.l)/2 + x >= world.getWidth()) x = world.getWidth() - (rectangle.r - rectangle.l)/2;
            if ((rectangle.b - rectangle.t)/2 + y >= world.getHeight()) y = world.getHeight() - (rectangle.b - rectangle.t)/2;
            moves.add(MoveBuilder.c(MOVE).dfCToXY(rectangle, x, y).maxSpeed(ms));
            vGd.put(id, new P2D(x, y, world.getTickIndex()));
            return true;
        }
    }

    void makeTypedMove(VehicleType type, Rectangle gr, double x, double y) {
        makeTypedMove(type, gr, x, y, 0, true);
    }

    void makeTypedMove(VehicleType type, Rectangle groupRectangle, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(type).setRect(new Rectangle(0,0,world.getHeight(),world.getWidth())));
        moves.add(MoveBuilder.c(MOVE).dfCToXY(groupRectangle, x, y).maxSpeed(ms));
    }

    void makeGroupMove(int id, Rectangle groupRectangle, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(MOVE).dfCToXY(groupRectangle, x, y).maxSpeed(ms));
    }

    void positionGroup(VehicleType vehicleType, Rectangle rectangle, boolean vertically, boolean horizontally, double factor) {
        if (!horizontally && !vertically) return;
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(vehicleType).setRect(new Rectangle(world)));
        if (horizontally && vertically) moves.add(MoveBuilder.c(MOVE).x(factor*((int) rectangle.l/74)).y(factor*((int) rectangle.t/74)));
        else if (horizontally) moves.add(MoveBuilder.c(MOVE).x(factor*((int) rectangle.l/74)));
        else  moves.add(MoveBuilder.c(MOVE).y(factor*((int) rectangle.t/74)));
        vehicleTypeStateMap.put(vehicleType, MyStrategy.VehicleTypeState.MOVING);
    }

    void scaleVehicles(VehicleType vehicleType, double scale, Rectangle rectangle) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(vehicleType).setRect(new Rectangle(world)));
        moves.add(MoveBuilder.c(SCALE).factor(scale).x(rectangle.l).y(rectangle.t));
        vehicleTypeStateMap.put(vehicleType, MyStrategy.VehicleTypeState.SCALING);
    }

    void scaleGroup(double scale, double x, double y, double speed, int id) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(SCALE).factor(scale).x(nuclearStrikePoint.x).y(nuclearStrikePoint.y).maxSpeed(speed));
    }

    void rotateGroup(int id, double angle, double x, double y, double speed) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(ROTATE).angle(angle).x(x).y(y).maxAngularSpeed(speed));
    }

    void createGroupFH(int id) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(HELICOPTER).setRect(new Rectangle(world)));
        moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(FIGHTER).setRect(new Rectangle(world)));
        moves.add(MoveBuilder.c(ASSIGN).group(id));
    }

    void createGroupTIA(int id) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(TANK).setRect(new Rectangle(world)));
        moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(IFV).setRect(new Rectangle(world)));
        moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(ARRV).setRect(new Rectangle(world)));
        moves.add(MoveBuilder.c(ASSIGN).group(id));
    }

    void createGroupTIAFH(int arial, int ground, int id) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(arial));
        moves.add(new MoveBuilder(ADD_TO_SELECTION).group(ground));
        moves.add(MoveBuilder.c(ASSIGN).group(id));
    }

    Stream<VehicleTick> mV() { return mVById.values().stream(); }
    Stream<VehicleTick> mVt(VehicleType... vts) { return mV().filter(v -> Arrays.stream(vts).anyMatch(vt -> v.type() == vt)); }
    Stream<VehicleTick> mVg(int... ids) { return mV().filter(v -> v.inGs(ids)); }
    Rectangle OfV(Stream<VehicleTick> vhs) { return vhs.reduce(new Rectangle(), Rectangle::update, Rectangle::combine); }
    Stream<VehicleTick> gV(int id) { return mV().filter(vt -> vt.inG(id)); }
    Stream<VehicleTick> eV() { return eVById.values().stream(); }

    double[] minVehicleSpeed(int... ids) {
        double[] speeds = new double[ids.length];
        Arrays.fill(speeds, Double.MAX_VALUE);
        return mV().reduce(speeds, (spds, v) -> {
            for (int i = 0; i < spds.length; i++) {
                if (v.inG(ids[i])) spds[i] = Math.min(spds[i], vehicleSpeed(game, v, world, weatherTypes, terrainTypes));
            }
            return spds;
        }, (a, b) -> { for (int i = 0; i < a.length; i++) a[i] = Math.min(a[i], b[i]); return a; });
    }

    Rectangle[] sOfVG(int... ids) {
        Rectangle[] initial = new Rectangle[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle();initial[i].g = ids[i]; }
        return mV().reduce(initial, (rects, v) -> {
            for (int i = 0; i < ids.length; i++)
                if (v.inG(ids[i])) {
                    rects[i].update(v);
                    rects[i].speed = Math.min(rects[i].speed, vehicleSpeed(game, v, world, weatherTypes, terrainTypes));
                }
            return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Map<VehicleType, Rectangle> mOfVT(VehicleType... types) {
        return Arrays.stream(sOfVT(types)).collect(Collectors.toMap(rectangle -> rectangle.vt, rectangle -> rectangle));
    }

    Rectangle[] sOfVT(Stream<VehicleTick> vehicle, VehicleType... types) {
        Rectangle[] initial = new Rectangle[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle(types[i]); }
        return vehicle.reduce(initial, (rects, v) -> {
            for (int i = 0; i < types.length; i++)
                if (v.type() == types[i]) {
                    rects[i].update(v);
                    rects[i].speed = Math.min(rects[i].speed, vehicleSpeed(game, v, world, weatherTypes, terrainTypes));
                }
            return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rectangle[] sOfVT(VehicleType... types) {
        return sOfVT(mV(), types);
    }

    Rectangle OfVG(int... ids) {
        return mV().reduce(new Rectangle(), (rect, v) -> {
            for (int id : ids)
                if (v.inG(id)) {
                    rect.update(v);
                    rect.speed = Math.min(rect.speed, vehicleSpeed(game, v, world, weatherTypes, terrainTypes));
                }
            return rect;
        }, Rectangle::combine);
    }

    Rectangle OfVG(VehicleType... types) {
        return mV().reduce(new Rectangle(), (rect, v) -> {
            for (int i = 0; i < types.length; i++)
                if (v.type() == types[i]) {
                    rect.update(v);
                    rect.speed = Math.min(rect.speed, vehicleSpeed(game, v, world, weatherTypes, terrainTypes));
                }
            return rect;
        }, Rectangle::combine);
    }


    double vehicleSpeed(Game game, VehicleTick vehicle, World world, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        return vehicle.speed(game, world, weatherTypes, terrainTypes);
    }
}

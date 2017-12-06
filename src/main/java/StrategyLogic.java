import model.*;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.PI;
import static java.lang.Math.round;
import static model.ActionType.*;
import static model.VehicleType.*;

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
    Map<Integer, Rectangle> nuclearStrikeGroupRects = new HashMap<>();
    Queue<MoveBuilder> positioningMoves = new LinkedList<>();
    Map<VehicleType, MyStrategy.VehicleTypeState> vehicleTypeStateMap = new HashMap<>();
    Map<Integer, Boolean> gatheredHorizontally = new HashMap<>();
    VehicleType[] mainOrder;
    Random random;
    double[][] worldSpeedFactors;
    Map<Long, FacilityPoint> facilitiesPoint = new HashMap<>();
    Map<Long, Facility> facilityMap = new HashMap<>();

    Map<VehicleType, Accumulator> update(Player me, World world, Game game) {
        this.me = me;
        this.world = world;
        this.game = game;
        Map<VehicleType, Accumulator> vu = initVehicles(world);
        if (random == null) random = new Random(game.getRandomSeed());
        Player[] players = world.getPlayers();
        for (int i = 0; i < players.length; i++) if (players[i].getId() != me.getId()) enemy = players[i];
        Facility[] facilities = world.getFacilities();
        for (int i = 0; i < facilities.length; i++) {
            Facility facility = facilities[i];
            facilityMap.put(facility.getId(), facility);
        }
        if (worldSpeedFactors == null) {
            weatherTypes = world.getWeatherByCellXY();
            terrainTypes = world.getTerrainByCellXY();
            worldSpeedFactors = new double[(int)(world.getHeight()/U.PALE_SIDE)][(int)(world.getWidth()/U.PALE_SIDE)];
            for (int i = 0; i < worldSpeedFactors.length; i++) {
                for (int j = 0; j < worldSpeedFactors[i].length; j++) {
                    worldSpeedFactors[i][j] = game.getPlainTerrainSpeedFactor() - VehicleTick.tSf(game, terrainTypes[i][j]);
                }
            }
            for (int i = 0; i < facilities.length; i++) {
                Facility from = facilities[i];
                int[] fij = new P2D(from.getLeft() + 32, from.getTop() + 32).inWorld(world);
                FactoriesRoute route = new FactoriesRoute(worldSpeedFactors, fij[0], fij[1]);
                FacilityPoint point = new FacilityPoint();
                for (int j = 0; j < facilities.length; j++) {
                    if (j == i) continue;
                    Facility to = facilities[j];
                    int[] tij = new P2D(to.getLeft() + 32, to.getTop() + 32).inWorld(world);
                    point.facilities.put(to.getId(), route.pathTo(tij[0], tij[1]));
                }
                point.build(facilitiesPoint);
                if (point.facilityRouteTo == 0) point.facilityRouteTo = facilities[0].getId();
                facilitiesPoint.put(from.getId(), point);
            }
        }
        return vu;
    }

    void setupUnitProduction() {
        for (Facility facility : facilityMap.values()) {
            if (facility.getOwnerPlayerId() == me.getId() && facility.getProductionProgress() == 0) {
                if (facility.getType() == FacilityType.VEHICLE_FACTORY)
                    moves.add(MoveBuilder.c(SETUP_VEHICLE_PRODUCTION)
                            .vehicleType(VehicleType.values()[random.nextInt(5)]).facilityId(facility.getId()));
            }
        }
    }

    int captureNearestFactory(int gid) {
        if (facilityMap.isEmpty()) return 0;

        Rectangle rectangle = sOfVG(gid)[0];
        int[] ij = rectangle.c().inWorld(world);

        Facility overFacility = null;
        for (Facility facility : facilityMap.values()) {
            Rectangle facilityRectangle = new Rectangle(facility.getLeft(), facility.getTop(), facility.getLeft() + 64, facility.getTop() + 64);
            if (rectangle.intersects(facilityRectangle)) {
                overFacility = facility;
                if (facility.getOwnerPlayerId() == me.getId()) {
                    break;
                } else {
                    return -1;
                }
            }
        }

        Stack<FactoriesRoute.N> minRoute = null;
        if (overFacility != null) {
            minRoute = facilitiesPoint.get(overFacility.getId()).pathToNext();
        } else {
            FactoriesRoute route = new FactoriesRoute(worldSpeedFactors, ij[0], ij[1]);
            double minStepsCost = Double.MAX_VALUE;
            for (Facility to : facilityMap.values()) {
                if (to.getOwnerPlayerId() == me.getId()) continue;
                int[] fij = new P2D(to.getLeft() + 32, to.getTop() + 32).inWorld(world);
                Stack<FactoriesRoute.N> steps = route.pathTo(fij[0], fij[0]);
                double cost = stepsCost(steps);
                if (minStepsCost > cost) {
                    minStepsCost = cost;
                    minRoute = steps;
                }
            }
        }

        if (minRoute != null) {
            FactoriesRoute.N from = minRoute.pop();
            moves.add(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).group(gid));
            P2D fromP = new P2D(from.x * U.PALE_SIDE, from.y * U.PALE_SIDE);
            moves.add(MoveBuilder.c(ActionType.MOVE).dfCToXY(rectangle, fromP.x, fromP.y));
            for (FactoriesRoute.N to : minRoute) {
                moves.add(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).group(gid));
                P2D dst = new P2D(to.x * U.PALE_SIDE, to.y * U.PALE_SIDE);
                moves.add(MoveBuilder.c(ActionType.MOVE).dfCToXY(fromP, dst.x, dst.y));
                fromP = dst;
            }
        }
        return 1;
    }

    static double stepsCost(Stack<FactoriesRoute.N> steps) {
        double cost = 0;
        for (FactoriesRoute.N step : steps) {
            cost += step.cost;
        }
        return cost;
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

    boolean restore() {
        Rectangle[] rectangles = sOfVG(MyStrategy.GFH, MyStrategy.GIAT);
        double angleA = rectangles[0].angle;
        double angleG = rectangles[1].angle;
        if (!U.eD(angleG, angleA)) {
            rotateGroup(MyStrategy.GFH, angleG - angleA, rectangles[0].cX(), rectangles[0].cY(), rectangles[1].speed);
            return true;
        }
        return false;
    }

    Accumulator acc(Map<VehicleType, Accumulator> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, Accumulator.ZERO); }
    Accumulator sum(Map<VehicleType, Accumulator> vu, VehicleType... vehicleTypes) {
        return new Accumulator(Arrays.stream(vehicleTypes).map(vt -> acc(vu, vt)).mapToLong(Accumulator::v).sum());
    }
    Accumulator sum(Map<VehicleType, Accumulator> vu, Set<VehicleType> vehicleTypes) {
        return new Accumulator(vehicleTypes.stream().map(vt -> acc(vu, vt)).mapToLong(Accumulator::v).sum());
    }

    void zipGroup(int id, double speed, VehicleType... types) {
        Rectangle.Builder rb = new Rectangle.Builder();
        Set<Long> coordinates = mVg(id).map(v -> {
            rb.update(v);
            if (gatheredHorizontally.get(id)) return round(v.x());
            else return round(v.y());
        }).collect(Collectors.toSet());
        Rectangle r = rb.build();
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
        Rectangle.Builder rb = new Rectangle.Builder();
        Set<Long> coordinates = mVg(id).map(v -> {
            rb.update(v);
            if (gatheredHorizontally.get(id)) return round(v.y());
            else return round(v.x());
        }).collect(Collectors.toSet());
        List<Long> sorted = new ArrayList<>(coordinates);
        sorted.sort(Long::compareTo);
        Rectangle r = rb.build();
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
                mVById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex(), world));
            } else {
                eVById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex(), world));
            }
        }

        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            if (vu.getDurability() == 0) {
                VehicleTick vt = mVById.remove(vu.getId());
                if (vt != null) vehicles.get(vt.type()).dec();
                eVById.remove(vu.getId());
            } else {
                VehicleTick oV = mVById.get(vu.getId());
                VehicleTick nV = mVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex(), world));
                if (oV != null && (Double.compare(oV.y(), nV.y()) != 0 || Double.compare(oV.x(), nV.x()) != 0)) {
                    vehicleUpdates.computeIfAbsent(oV.type(), t -> new Accumulator(0)).inc();
                }
                eVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex(), world));
            }
        }
        return vehicleUpdates;
    }

    boolean protectGround(int arial, int ground) {
        Rectangle srs = OfVG(arial);
        Rectangle drs = OfVG(ground);
        if (srs.dfct(drs) > U.EPS) {
            makeGroupMove(arial, srs, drs.cX(), drs.cY(), 0, true);
            return true;
        }
        return false;
    }

    boolean gatherAG(double scale, MyStrategy.GroupOrderState[] ggs, int i, Rectangle... rectangles) {
        Rectangle rectangleA = rectangles[0]; Rectangle rectangleB = rectangles[1];
        boolean gd = ggs[i] == MyStrategy.GroupOrderState.F || rectangles[0].dfct(rectangles[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rectangleA.cX() != rectangleB.cX() && rectangleA.cY() != rectangleB.cY()) {
                    ggs[i] = rectangleA.b > rectangleB.t ? MyStrategy.GroupOrderState.INY : MyStrategy.GroupOrderState.INX;
                }
                else if (rectangleA.cX() != rectangleB.cX()) ggs[i] = MyStrategy.GroupOrderState.INY;
                else if (rectangleA.cY() != rectangleB.cY()) ggs[i] = MyStrategy.GroupOrderState.INX;
                break;
            case INX:
                if (U.eD(rectangleA.cX(), rectangleB.cX() + scale)) ggs[i] = MyStrategy.GroupOrderState.NY;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleB.cX() + scale, rectangleA.cY());
                break;
            case INY:
                if (U.eD(rectangleA.cY(), rectangleB.cY() + scale)) ggs[i] = MyStrategy.GroupOrderState.NX;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleA.cX(), rectangleB.cY() + scale);
                break;
            case NX:
                if (U.eD(rectangleA.cX(), rectangleB.cX())) ggs[i] = MyStrategy.GroupOrderState.F;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleB.cX(), rectangleA.cY());
                break;
            case NY:
                if (U.eD(rectangleA.cY(), rectangleB.cY())) ggs[i] = MyStrategy.GroupOrderState.F;
                else makeTypedMove(rectangleA.vt, rectangleA, rectangleA.cX(), rectangleB.cY());
                break;
        }
        return gd;
    }

    VehicleTick cEP(boolean arial, int... groups) {
        P2D center = OfVG(groups).c();
        VehicleTick ctlev = null;
        for (VehicleTick v : (Iterable<VehicleTick>) eV()::iterator) {
            if (arial && !v.v.isAerial()) continue;
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev, v, center);
        }
        return ctlev;
    }

    long myVehicleReadyAttack(int... gid) {
        int attack = 0;
        Rectangle[] aR = sOfVG(9, gid);
        for (VehicleTick m : (Iterable<VehicleTick>) eV()::iterator) {
            for (Rectangle anAR : aR) {
                if (anAR.include(m.x(), m.y())) attack++;
            }
        }
        return attack;
    }

    void setupNuclearStrike(int id) {
        if (me.getNextNuclearStrikeTickIndex() > 0 || me.getRemainingNuclearStrikeCooldownTicks() > 0) return;
        Rectangle.Builder vRB = new Rectangle.Builder();
        for (VehicleTick fv : (Iterable<VehicleTick>) mVt(FIGHTER, HELICOPTER)::iterator) {
            vRB.combine(fv.see(game, world, weatherTypes, terrainTypes));
        }

        Rectangle vR = vRB.build();
        VehicleTick eVns = null;
        VehicleTick cff = null;
        for (VehicleTick eV : (Iterable<VehicleTick>) eV().filter(v -> vR.include(v.x(), v.y()))::iterator) {
            for (VehicleTick mV : (Iterable<VehicleTick>) mVt(FIGHTER, HELICOPTER)::iterator) {
                if (mV.see(eV, game, weatherTypes, terrainTypes)) {
                    if (eVns == null) {
                        eVns = eV;
                        cff = mV;
                    }
                    if (eVns.compareTo(eV) < 0) {
                        eVns = eV;
                        cff = mV;
                    }
                }
            }
        }
        if (eVns != null) {
            Rectangle rectangle = sOfVG(id)[0];
            makeGroupMove(id, rectangle, rectangle.cX(), rectangle.cY(), 0, true);
            moves.add(new MoveBuilder(TACTICAL_NUCLEAR_STRIKE).vehicleId(cff.id()).x(eVns.x()).y(eVns.y()));
        }
    }

    boolean makeTacticalGroupMove(int id, double x, double y, double ms, long updates) {
        P2D destination = vGd.get(id);
        if (destination != null && destination.compareTo(new P2D(x, y)) == 0 && updates > 0)
            return true;
        Rectangle rectangle = sOfVG(id)[0];
        if (rectangle.include(x, y)) {
            vGd.remove(id);
            if (rectangle.square() > 25_000)
                scaleGroup(0.8, rectangle.cX(), rectangle.cX(), rectangle.speed, id);
            else {
                P2D center = new P2D(x, y);
                VehicleTick ctlev = null;
                for (VehicleTick v : (Iterable<VehicleTick>) mV()::iterator) {
                    if (ctlev == null) ctlev = v;
                    else ctlev = P2D.closedTo(ctlev, v, center);
                }
                if (ctlev == null || U.eD(x, ctlev.x()) && U.eD(y, ctlev.y())) rotateGroup(id, PI / 4, rectangle.cX(), rectangle.cY(), ms);
                else {
                    moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
                    moves.add(MoveBuilder.c(MOVE).x(x - ctlev.x()).y(y - ctlev.y()).maxSpeed(ms));
                    vGd.put(id, new P2D(x, y, world.getTickIndex(), ms));
                }
            }
            return false;
        } else {
            moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
            if ((rectangle.r - rectangle.l)/2 + x >= world.getWidth()) x = world.getWidth() - (rectangle.r - rectangle.l)/2;
            if ((rectangle.b - rectangle.t)/2 + y >= world.getHeight()) y = world.getHeight() - (rectangle.b - rectangle.t)/2;
            moves.add(MoveBuilder.c(MOVE).dfCToXY(rectangle, x, y).maxSpeed(ms));
            vGd.put(id, new P2D(x, y, world.getTickIndex(), ms));
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
        moves.add(MoveBuilder.c(SCALE).factor(scale).x(x).y(y).maxSpeed(speed));
    }

    void rotateGroup(int id, double angle, double x, double y, double speed) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(ROTATE).angle(angle).x(x).y(y).maxSpeed(speed).maxAngularSpeed(speed));
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
    Rectangle OfV(Stream<VehicleTick> vhs) { return vhs.reduce(new Rectangle.Builder(), Rectangle.Builder::update, Rectangle.Builder::combine).build(); }
    Stream<VehicleTick> gV(int id) { return mV().filter(vt -> vt.inG(id)); }
    Stream<VehicleTick> eV() { return eVById.values().stream(); }

    double[] minVehicleSpeed(int... ids) {
        double[] speeds = new double[ids.length];
        Arrays.fill(speeds, Double.MAX_VALUE);
        return mV().reduce(speeds, (spds, v) -> {
            for (int i = 0; i < spds.length; i++) {
                if (v.inG(ids[i])) spds[i] = Math.min(spds[i], vehicleSpeed(game, v, weatherTypes, terrainTypes));
            }
            return spds;
        }, (a, b) -> { for (int i = 0; i < a.length; i++) a[i] = Math.min(a[i], b[i]); return a; });
    }

    Rectangle[] sOfVG(int... ids) {
        return sOfVG(2, ids);
    }

    Rectangle[] sOfVG(double range, int... ids) {
        Rectangle.Builder[] initial = new Rectangle.Builder[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle.Builder();initial[i].g = ids[i]; }
        initial = mV().reduce(initial, (rects, v) -> {
            for (int i = 0; i < ids.length; i++)
                if (v.inG(ids[i])) {
                    rects[i].update(v, range);
                    rects[i].speed = Math.min(rects[i].speed, vehicleSpeed(game, v, weatherTypes, terrainTypes));
                }
            return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
        Rectangle[] rectangles = new Rectangle[initial.length];
        for (int i = 0; i < rectangles.length; i++) rectangles[i] = initial[i].build();
        return rectangles;
    }

    Map<VehicleType, Rectangle> mOfVT(VehicleType... types) {
        return Arrays.stream(sOfVT(types)).collect(Collectors.toMap(rectangle -> rectangle.vt, rectangle -> rectangle));
    }

    Rectangle[] sOfVT(Stream<VehicleTick> vehicle, VehicleType... types) {
        Rectangle.Builder[] initial = new Rectangle.Builder[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle.Builder(types[i]); }
        initial = vehicle.reduce(initial, (rects, v) -> {
            for (int i = 0; i < types.length; i++)
                if (v.type() == types[i]) {
                    rects[i].update(v);
                    rects[i].speed = Math.min(rects[i].speed, vehicleSpeed(game, v, weatherTypes, terrainTypes));
                }
            return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
        Rectangle[] rectangles = new Rectangle[initial.length];
        for (int i = 0; i < rectangles.length; i++) rectangles[i] = initial[i].build();
        return rectangles;
    }

    Rectangle[] sOfVT(VehicleType... types) {
        return sOfVT(mV(), types);
    }

    Rectangle OfVG(int... ids) {
        return mV().reduce(new Rectangle.Builder(), (rect, v) -> {
            for (int id : ids)
                if (v.inG(id)) {
                    rect.update(v);
                    rect.speed = Math.min(rect.speed, vehicleSpeed(game, v, weatherTypes, terrainTypes));
                }
            return rect;
        }, Rectangle.Builder::combine).build();
    }


    double vehicleSpeed(Game game, VehicleTick vehicle, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        return vehicle.speed(game, weatherTypes, terrainTypes);
    }

    static class FacilityPoint {
        long facilityRouteTo;
        Map<Long, Stack<FactoriesRoute.N>> facilities = new HashMap<>();

        void build(Map<Long, FacilityPoint> facilitiesPoint) {
            double minCost = Double.MAX_VALUE;
            for (Long id : facilities.keySet()) {
                Stack<FactoriesRoute.N> route = facilities.get(id);
                double routeCost = stepsCost(route);
                if (minCost > routeCost && facilitiesPoint.get(id) == null) {
                    minCost = routeCost;
                    facilityRouteTo = id;
                }
            }
        }

        Stack<FactoriesRoute.N> pathToNext() {
            return facilities.get(facilityRouteTo);
        }
    }
}

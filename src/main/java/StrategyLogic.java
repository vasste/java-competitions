import model.*;

import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Math.PI;
import static java.lang.Math.min;
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
    List<VehicleTick> eVSortedByPointZero;
    final Map<VehicleType, Accumulator> vehicles = new HashMap<>();
    Queue<MoveBuilder> moves = new LinkedList<>();
    Map<Integer, P2D> vGd = new HashMap<>();
    Map<Integer, Rectangle> nuclearStrikeGroupRects = new HashMap<>();
    Queue<MoveBuilder> positioningMoves = new LinkedList<>();
    Map<Integer, Queue<MoveBuilder>> groupPositioningMoves = new HashMap<>();
    Map<VehicleType, MyStrategy.VehicleTypeState> vehicleTypeStateMap = new HashMap<>();
    Map<Integer, MyStrategy.VehicleTypeState> vehicleGroupStateMap = new HashMap<>();
    Map<Integer, Boolean> gatheredHorizontally = new HashMap<>();
    VehicleType[] mainOrder;
    Random random;
    double[][][] worldSpeedFactors;
    Map<Long, Facility> facilityMap = new HashMap<>();
    Map<Long, Integer> facilityGroupNext = new HashMap<>();
    final int factor = 2;
    Map<Integer, Accumulator> groupUpdates = new HashMap<>();
    static final VehicleType[] ARIAL_TYPES = new VehicleType[]{FIGHTER, HELICOPTER};
    static final VehicleType[] GROUND_TYPES = new VehicleType[]{TANK, ARRV, IFV};

    Map<VehicleType, Accumulator> update(Player me, World world, Game game) {
        this.me = me;
        this.world = world;
        this.game = game;
        Map<VehicleType, Accumulator> vu = initVehicles(world);
        if (random == null) random = new Random(game.getRandomSeed());
        Player[] players = world.getPlayers();
        for (int i = 0; i < players.length; i++) if (players[i].getId() != me.getId()) enemy = players[i];
        Facility[] facilities = world.getFacilities();
        for (int i = 0; i < facilities.length; i++) facilityMap.put(facilities[i].getId(), facilities[i]);
        if (terrainTypes == null || weatherTypes == null) {
            weatherTypes = world.getWeatherByCellXY();
            terrainTypes = world.getTerrainByCellXY();
        }

        if (worldSpeedFactors == null && facilities.length > 0) {
            worldSpeedFactors = new double[(int)(world.getHeight()/U.PALE_SIDE)/factor][(int)(world.getWidth()/U.PALE_SIDE)/factor][2];
            for (int i = 0; i < worldSpeedFactors.length * factor; i++) {
                for (int j = 0; j < worldSpeedFactors.length * factor; j++) {
                    worldSpeedFactors[i/factor][j/factor] = new double[2];
                    worldSpeedFactors[i/factor][j/factor][0] += 2 - VehicleTick.wSf(game, weatherTypes[i][j]);
                    worldSpeedFactors[i/factor][j/factor][1] += 2 - VehicleTick.tSf(game, terrainTypes[j][i]);
                }
            }
        }
        return vu;
    }

    void setupUnitProduction(Facility facility, int gid) {
        if (facility.getOwnerPlayerId() == me.getId() && facility.getProductionProgress() == 0) {
            if (facility.getType() == FacilityType.VEHICLE_FACTORY)
                groupPositioningMoves.get(gid).add(MoveBuilder.c(SETUP_VEHICLE_PRODUCTION)
                        .vehicleType(TANK).facilityId(facility.getId()));
        }
    }


    int captureNearestFactory(int gid, List<Rectangle> rectangles, List<Rectangle> facilities) {
        int[][] gpt = rectangles.stream().map(Rectangle::c)
                .map(pt -> pt.inWorld(world, factor)).toArray(value -> new int[value][2]);
        int[][] fpt = facilities.stream().map(Rectangle::c)
                .map(pt -> pt.inWorld(world, factor)).toArray(value -> new int[value][2]);
        return captureNearestFactory(gid, gpt, fpt, new int[0][2]);
    }

    int[][] enemyMap() {
        Map<FactoriesRoute.N, Accumulator> map = new HashMap<>();
        eV().map(P2D::new).map(pt -> new FactoriesRoute.N(pt.inWorld(world, factor)))
                .forEach(n -> map.computeIfAbsent(n, k -> new Accumulator(0)).inc());
        return map.entrySet().stream().filter(kv -> kv.getValue().value > 10).map(kv -> new int[]{kv.getKey().x, kv.getKey().y})
                .toArray(value -> new int[value][2]);
    }

    int captureNearestFactory(int gid, int[][] gpt, int[][] fpt, int[][] empt) {
        groupPositioningMoves.computeIfAbsent(gid, k -> new LinkedList<>());
        if (facilityMap.isEmpty()) return 0;

        Rectangle rectangle = sOfVG(gid)[0];

        int mine = 0;
        Facility overFacility = null;
        boolean capturing = false;
        for (Long fid : facilityMap.keySet()) {
            Facility facility = facilityMap.get(fid);
            if (facility.getOwnerPlayerId() == me.getId() && facility.getCapturePoints() == game.getMaxFacilityCapturePoints()) {
                mine++;
                setupUnitProduction(facility, gid);
            }
            if (facilityGroupNext.containsKey(facility.getId())) mine++;
            Rectangle facilityRectangle = new Rectangle(facility);
            if (rectangle.dfct(facilityRectangle) < 1) {
                overFacility = facility;
                if (facility.getOwnerPlayerId() == me.getId() && facility.getCapturePoints() == game.getMaxFacilityCapturePoints()) {
                    if (facilityGroupNext.containsKey(facility.getId()) && facilityGroupNext.get(facility.getId()) == gid) {
                        facilityGroupNext.remove(facility.getId());
                    }
                } else {
                    capturing = true;
                }
            }
        }

        if (capturing) return -1;

        Rectangle facilityRect = Optional.ofNullable(overFacility).map(Rectangle::new).orElse(new Rectangle(0, 0,0,0));
        long minRouteFactoryId = 0;
        Stack<FactoriesRoute.N> minRoute = null;
        int[] ij = rectangle.c().inWorld(world, factor);
        FactoriesRoute routes = new FactoriesRoute(worldSpeedFactors, ij[0], ij[1],
                U.PALE_SIDE / factor, U.PALE_SIDE / factor, 1, gpt, fpt, facilityRect.c().inWorld(world, factor), empt);
        double minCost = Double.MAX_VALUE;
        for (Long fid : facilityMap.keySet()) {
            Facility facility = facilityMap.get(fid);
            if (facilityGroupNext.containsKey(fid) && facilityGroupNext.get(fid) != gid) continue;
            if (facility.getOwnerPlayerId() == me.getId() && facility.getCapturePoints() == game.getMaxFacilityCapturePoints()) continue;
            Facility to = facilityMap.get(fid);
            int[] tij = new P2D(to.getLeft(), to.getTop()).inWorld(world, factor);
            Stack<FactoriesRoute.N> route = routes.pathTo(tij[0], tij[1], worldSpeedFactors[tij[0]][tij[1]][1]);
            double routeCost = stepsCost(route);
            if (minCost > routeCost) {
                minCost = routeCost;
                minRouteFactoryId = to.getId();
                minRoute = route;
            }
        }
        if (minRoute != null && !minRoute.isEmpty()) {
            Facility facility = facilityMap.get(minRouteFactoryId);
            facilityGroupNext.put(facility.getId(), gid);
            return moveToNextFacility(gid, rectangle, minRoute, facility);
        }

        return mine == facilityMap.size() ? 1 : 0;
    }

    private void moveToNext(int gid, Rectangle rectangle, Stack<FactoriesRoute.N> minRoute, long updates) {
        if (minRoute == null || minRoute.isEmpty()) return;
        P2D fromP = rectangle.c();
        if (minRoute.size() > 1) {
            minRoute.pop();
        }
        FactoriesRoute.N to = minRoute.pop();
        P2D dst = new P2D(factor * to.x * U.PALE_SIDE + U.PALE_SIDE * factor/2, factor * to.y * U.PALE_SIDE + U.PALE_SIDE * factor/2);
        if (P2D.distanceTo(fromP, dst) > .5) {
            if (updates > 0 && dst.equals(vGd.get(gid))) return;
            groupPositioningMoves.get(gid).add(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).group(gid));
            groupPositioningMoves.get(gid).add(MoveBuilder.c(ActionType.MOVE).dfCToXY(fromP, dst.x, dst.y, rectangle, world).maxSpeed(rectangle.speed));
            vGd.put(gid, dst);
        }
    }

    private int moveToNextFacility(int gid, Rectangle rectangle, Stack<FactoriesRoute.N> minRoute, Facility facility) {
        if (minRoute == null || minRoute.isEmpty()) return 1;
        P2D fromP = rectangle.c();
        if (minRoute.size() > 1) {
            minRoute.pop();
            FactoriesRoute.N to = minRoute.pop();
            P2D dst = new P2D(factor * to.x * U.PALE_SIDE + U.PALE_SIDE * factor/2, factor * to.y * U.PALE_SIDE + U.PALE_SIDE * factor/2);
            if (P2D.distanceTo(fromP, dst) > .5) {
                groupPositioningMoves.get(gid).add(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).group(gid));
                if (minRoute.isEmpty()) {
                    dst = new P2D(facility.getLeft() + U.PALE_SIDE, facility.getTop() + U.PALE_SIDE);
                    groupPositioningMoves.get(gid).add(
                            MoveBuilder.c(ActionType.MOVE).dfCToXY(fromP, dst.x, dst.y, rectangle, world).maxSpeed(rectangle.speed));
                } else {
                    groupPositioningMoves.get(gid).add(
                            MoveBuilder.c(ActionType.MOVE).dfCToXY(fromP, dst.x, dst.y, rectangle, world).maxSpeed(rectangle.speed));
                }
            }
        } else {
            groupPositioningMoves.get(gid).add(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).group(gid));
            Rectangle facilityRectangle = new Rectangle(facility);
            FactoriesRoute.N to = minRoute.pop();
            P2D dst = new P2D(factor * to.x * U.PALE_SIDE + U.PALE_SIDE * factor/2, factor * to.y * U.PALE_SIDE + U.PALE_SIDE * factor/2);
            if (facilityRectangle.include(dst)) {
                dst = new P2D(facility.getLeft() + U.PALE_SIDE, facility.getTop() + U.PALE_SIDE);
            }
            groupPositioningMoves.get(gid).add(MoveBuilder.c(ActionType.MOVE).dfCToXY(fromP, dst.x, dst.y));
        }
        return 0;
    }

    static double stepsCost(Stack<FactoriesRoute.N> steps) {
        if (steps.isEmpty()) return Double.MAX_VALUE;
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
    Accumulator accGroups(Map<Integer, Accumulator> vu, Integer gid) { return vu.getOrDefault(gid, Accumulator.ZERO); }
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
                moves.add(MoveBuilder.c(MOVE).x(round(sorted.get(0) - sorted.get(i) + 5*i)).maxSpeed(speed));
            } else {
                double ry = sorted.get(i);
                Rectangle vr = new Rectangle(r.l, ry - 2, ry + 2, r.r);
                moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(types[0]).setRect(vr));
                for (int j = 1; j < types.length; j++) {
                    moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(types[j]).setRect(vr));
                }
                moves.add(MoveBuilder.c(MOVE).y(round(sorted.get(0) - sorted.get(i) + 5*i)).maxSpeed(speed));
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
        groupUpdates.clear();
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
                    for (Integer gid : nV.gs) {
                        groupUpdates.computeIfAbsent(gid, t -> new Accumulator(0)).inc();
                    }
                }
                eVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex(), world));
            }
        }
        eVSortedByPointZero = new ArrayList<>(eVById.values());
        Collections.sort(eVSortedByPointZero, Comparator.comparingDouble(o -> P2D.distanceTo(new P2D(o), P2D.Z)));
        return vehicleUpdates;
    }

    boolean protectGround(int arial, int ground, double range) {
        Rectangle srs = OfVG(arial);
        Rectangle drs = OfVG(ground);
        if (srs.dfct(drs) > range) {
            makeGroupMove(arial, srs, drs.cX(), drs.cY(), 0, true);
            return true;
        }
        return false;
    }

    boolean gatherAG(double scale, MyStrategy.GroupOrderState[] ggs, int i, Rectangle... rectangles) {
        Rectangle rectangleA = rectangles[0]; Rectangle rectangleB = rectangles[1];
        boolean gd = ggs[i] == MyStrategy.GroupOrderState.F && rectangles[0].dfct(rectangles[1]) <= Math.abs(scale);
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

    VehicleTick cEP(P2D center) {
        VehicleTick ctlev = null;
        for (VehicleTick v : (Iterable<VehicleTick>) eV()::iterator) {
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev, v, center);
        }
        return ctlev;
    }

    VehicleTick cEP(boolean arial, int... groups) {
        P2D center = OfVG(groups).c();
        VehicleTick ctlev = null;
        for (VehicleTick v : (Iterable<VehicleTick>) eV()::iterator) {
            if (arial && v.type() != HELICOPTER) continue;
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev, v, center);
        }
        return ctlev;
    }

    long myVehicleReadyAttack(int gid) {
        int attack = 0;
        if (sOfVG(gid)[0].intersects(OfV(eV()))) {
            for (VehicleTick m : (Iterable<VehicleTick>) gV(gid)::iterator) {
                for (VehicleTick e : (Iterable<VehicleTick>) eV()::iterator) {
                    if (m.attackRange()*m.attackRange() >= m.getSquaredDistanceTo(e)) attack++;
                }
            }
        }
        return attack;
    }

    boolean setupNuclearStrike(int gid) {
        if (me.getNextNuclearStrikeTickIndex() > 0 || me.getRemainingNuclearStrikeCooldownTicks() > 0) return false;
        VehicleTick eVns = null;
        VehicleTick cff = null;
        for (VehicleTick mV : (Iterable<VehicleTick>) mVg(gid)::iterator) {
            int i = Collections.binarySearch(eVSortedByPointZero, mV, Comparator.comparingDouble(o -> P2D.distanceTo(new P2D(o), P2D.Z)));
            if (i < 0)
                i = -i - 1;
            VehicleTick eV = eVSortedByPointZero.get(Math.min(eVSortedByPointZero.size() - 1, i));
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
        if (eVns != null) {
            groupPositioningMoves.get(gid).add(new MoveBuilder(TACTICAL_NUCLEAR_STRIKE).vehicleId(cff.id()).x(eVns.x()).y(eVns.y()));
            return true;
        }
        return false;
    }

    boolean makeTacticalGroupMove(int id, double x, double y, double ms) {
        long updates = groupUpdates.getOrDefault(id, Accumulator.ZERO).value;
        vehicleGroupStateMap.put(id, MyStrategy.VehicleTypeState.MOVING);
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
                if (ctlev == null || U.eD(x, ctlev.x()) && U.eD(y, ctlev.y()))
                    rotateGroup(id, PI / 4, rectangle.cX(), rectangle.cY(), ms);
                else {
                    moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
                    moves.add(MoveBuilder.c(MOVE).x(x - ctlev.x()).y(y - ctlev.y()).maxSpeed(ms));
                    vGd.put(id, new P2D(x, y, world.getTickIndex(), ms));
                }
            }
            return false;
        } else {
            moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
            moves.add(MoveBuilder.c(MOVE).dfCToXY(rectangle, x, y, rectangle, world).maxSpeed(ms));
            vGd.put(id, new P2D(x, y, world.getTickIndex(), ms));
            return true;
        }
    }

    void makeGroundTacticalGroupMove(int id, double x, double y, Rectangle rectangle, int[][] gpt, int[][] fpt) {
        vehicleGroupStateMap.put(id, MyStrategy.VehicleTypeState.MOVING);
        if (rectangle.include(x, y) && rectangle.square() > 25_000) {
            scaleGroup(0.8, rectangle.cX(), rectangle.cX(), rectangle.speed, id);
        } else {
            int[] ij = rectangle.c().inWorld(world, factor);
            FactoriesRoute routes =
                    new FactoriesRoute(worldSpeedFactors, ij[0], ij[1],
                            U.PALE_SIDE / factor, U.PALE_SIDE / factor, 1, gpt, fpt, new int[]{0, 0}, new int[0][2]);
            int[] tij = new P2D(x, y).inWorld(world, factor);
            Stack<FactoriesRoute.N> route = routes.pathTo(tij[0], tij[1], worldSpeedFactors[tij[0]][tij[1]][1]);
            moveToNext(id, rectangle, route, groupUpdates.getOrDefault(id, Accumulator.ZERO).value);
            if (route.empty()) {
                makeTacticalGroupMove(id, x, y, rectangle.speed);
            }
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
        groupPositioningMoves.get(id).add(MoveBuilder.c(MOVE).dfCToXY(groupRectangle, x, y).maxSpeed(ms));
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
        vehicleGroupStateMap.put(id, MyStrategy.VehicleTypeState.SCALING);
    }

    void rotateGroup(int id, double angle, double x, double y, double speed) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(ROTATE).angle(angle).x(x).y(y).maxSpeed(speed).maxAngularSpeed(speed));
        vehicleGroupStateMap.put(id, MyStrategy.VehicleTypeState.ROTATING);
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

    void createTypeGroup(VehicleType type, int id) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(type).setRect(new Rectangle(world)));
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
    Stream<VehicleTick> eVt(VehicleType... vts) { return eV().filter(v -> Arrays.stream(vts).anyMatch(vt -> v.type() == vt)); }

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

    Rectangle[] sOfVG(Set<Integer> groups) {
        return sOfVG(groups.stream().mapToInt(Integer::intValue).toArray());
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
}

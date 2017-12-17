import model.*;

import java.util.*;

import static java.lang.Math.PI;
import static model.ActionType.ASSIGN;
import static model.ActionType.CLEAR_AND_SELECT;
import static model.VehicleType.*;

public final class MyStrategy implements Strategy {
    public enum VehicleTypeState {MOVING, WAITING, SCALING, ROTATING, ATTACK, ESCAPE, ZIPPING}
    public enum GameState {ORDER_CREATION, ORDER_POSITIONING, ORDER_ARIAL_POSITIONING, ORDER_SCALING,
        ORDER_GROUPING, ORDER_ROTATION, TACTICAL_EXECUTION, TACTICAL_EXECUTION_GROUPS, TACTICAL_EXECUTION_SINGLE,
        TACTICAL_EXECUTION_CHANGE, TACTICAL_EXECUTION_FACILITIES, NUCLEAR_STRIKE}
    public enum GroupGameState {NUCLEAR_STRIKE, NUCLEAR_STRIKE_RECOVERY, WAIT_COMMAND_FINISH, TACTICAL_EXECUTION}
    public enum GroupOrderState { I, INX, INY, NX, NY, F }
    static final Boolean DEBUG = false;
    private final StrategyLogic logic = new StrategyLogic();
    GameState gameState = GameState.ORDER_CREATION;
    GameState beforeGameState = GameState.ORDER_CREATION;

    GroupOrderState[] ggs = new GroupOrderState[]{GroupOrderState.I, GroupOrderState.I, GroupOrderState.I};
    Map<Integer, GroupGameState> groupGameStateMap = new HashMap<>();
    Map<Integer, Set<VehicleType>> groupTypesMap = new HashMap<>();
    Set<Integer> groups = new HashSet<>();
    boolean horizontally = true;

    public static final int GIAT    = 1;
    public static final int GFH     = 2;
    public static final int GFHTAI  = 3;

    public static final int GA  = 7;
    public static final int GT  = 8;
    public static final int GI  = 9;

    static final int[] FACILITY_GROUPS = new int[]{GA, GT, GI};
    static Set<Integer> CAPTURING_GROUPS = new HashSet<>(Arrays.asList(GA, GT, GI));

    int manufacturedGroupIndex = 11;
    Set<Integer> manufacturedEmptyGroupIds = new HashSet<>();

    @Override
    public void move(Player me, World world, Game game, Move move) {
        Map<VehicleType, Accumulator> vu = logic.update(me, world, game);
        if (logic.enemy.getNextNuclearStrikeTickIndex() > 0) {
            if (gameState != GameState.NUCLEAR_STRIKE) beforeGameState = gameState;
            gameState = GameState.NUCLEAR_STRIKE;
            for (Integer gid : groupGameStateMap.keySet()) {
                if (groupGameStateMap.get(gid) != GroupGameState.NUCLEAR_STRIKE_RECOVERY)
                    groupGameStateMap.put(gid, GroupGameState.NUCLEAR_STRIKE);
            }
        } else {
            boolean nuclearStrikeRecovered = true;
            for (Integer gid : groups) {
                GroupGameState state = groupGameStateMap.get(gid);
                if (state == GroupGameState.NUCLEAR_STRIKE_RECOVERY || state == GroupGameState.NUCLEAR_STRIKE) nuclearStrikeRecovered &= false;
            }
            if (gameState == GameState.NUCLEAR_STRIKE && nuclearStrikeRecovered)
                gameState = beforeGameState;
        }

        if (me.getRemainingActionCooldownTicks() > 0) return;
        MoveBuilder nextMove = logic.nextMove();
        if (nextMove == null) {
            switch (gameState) {
                case ORDER_CREATION:
                    Rectangle[] rs = logic.sOfVT(StrategyLogic.GROUND_TYPES);
                    for (int i = 0; i < rs.length; i++) {
                        rs[i].commands = new ArrayDeque<>();
                        rs[i].g = i + 1;
                    }
                    Rectangle[] ars = logic.sOfVT(StrategyLogic.ARIAL_TYPES);
                    Arrays.sort(ars);
                    OrderGraph.setupGroupingMoves(rs, world);
                    for (Rectangle ar : ars) logic.positionGroup(ar.vt, ar, true, true, 20);
                    for (Rectangle r : rs) {
                        while (!r.commands.isEmpty()) logic.positioningMoves.add(r.commands.pollLast());
                    }
                    logic.createGroupFH(GFH);
                    logic.vehicleGroupStateMap.put(GFH, VehicleTypeState.ATTACK);
                    groupGameStateMap.put(GFH, GroupGameState.TACTICAL_EXECUTION);
                    groupTypesMap.put(GFH, EnumSet.of(FIGHTER, HELICOPTER));

                    if (logic.facilityMap.isEmpty()) {

                        logic.createGroupTIA(GIAT);
                        gameState = GameState.ORDER_ARIAL_POSITIONING;

                        groupTypesMap.put(GIAT, EnumSet.of(TANK, IFV, ARRV));
                        groupTypesMap.put(GFHTAI, EnumSet.of(FIGHTER, HELICOPTER, TANK, IFV, ARRV));

                        groupGameStateMap.put(GIAT, GroupGameState.TACTICAL_EXECUTION);
                        groupGameStateMap.put(GFHTAI, GroupGameState.TACTICAL_EXECUTION);

                        logic.vehicleGroupStateMap.put(GFHTAI, VehicleTypeState.ATTACK);
                        logic.vehicleGroupStateMap.put(GIAT, VehicleTypeState.ATTACK);
                    }
                    else {
                        logic.createTypeGroup(TANK, GT);
                        logic.createTypeGroup(ARRV, GA);
                        logic.createTypeGroup(IFV, GI);
                        int[] typedGroups = new int[]{GA, GT, GI};
                        for (int id : typedGroups) {
                            groupGameStateMap.put(id, GroupGameState.WAIT_COMMAND_FINISH);
                            logic.vehicleGroupStateMap.put(id, VehicleTypeState.ATTACK);
                        }
                        gameState = GameState.ORDER_ARIAL_POSITIONING;
                        groupTypesMap.put(GT, EnumSet.of(TANK));
                        groupTypesMap.put(GA, EnumSet.of(ARRV));
                        groupTypesMap.put(GI, EnumSet.of(IFV));
                    }
                    break;
                case ORDER_ARIAL_POSITIONING:
                    boolean orderArial = false;
                    if (logic.sum(vu, StrategyLogic.ARIAL_TYPES).zero()) {
                        if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.MOVING) {
                            ars = logic.sOfVT(StrategyLogic.ARIAL_TYPES);
                            logic.scaleVehicles(FIGHTER, 1.6, ars[0]);
                            logic.scaleVehicles(HELICOPTER, 1.6, ars[1]);
                        } else if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.SCALING) {
                            if (ggs[0] == GroupOrderState.NX) logic.gatheredHorizontally.put(GFH, true);
                            else if (ggs[0] == GroupOrderState.NY) logic.gatheredHorizontally.put(GFH, false);
                            Rectangle[] rectangles = logic.sOfVT(StrategyLogic.ARIAL_TYPES);
                            if (logic.gatherAG(5, ggs, 0, rectangles)) {
                                logic.vehicleTypeStateMap.put(FIGHTER, VehicleTypeState.ZIPPING);
                                double[] speeds = logic.minVehicleSpeed(GFH);
                                logic.zipGroup(GFH, speeds[0], StrategyLogic.ARIAL_TYPES);
                            }
                        } else if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.ZIPPING) {
                            orderArial = true;
                        }
                    }
                    if (!logic.facilityMap.isEmpty()) {
                        if (groups.isEmpty()) {
                            Rectangle[] rectangles = logic.sOfVG(GA, GT, GI);
                            groups = new HashSet<>(Arrays.asList(GA, GT, GI));
                            for (int id : groups) {
                                logic.captureNearestFactory(id, Arrays.asList(rectangles), Collections.emptyList());
                            }
                        } else {
                            extractNextMoves();
                        }
                        if (orderArial) {
                            gameState = GameState.TACTICAL_EXECUTION_FACILITIES;
                            logic.groupPositioningMoves.computeIfAbsent(GFH, k -> new LinkedList<>());
                            groups.add(GFH);
                        }
                        break;
                    }
                case ORDER_POSITIONING:
                    boolean arial = false;
                    if (logic.facilityMap.isEmpty()) {
                        Rectangle[] rectangles = logic.sOfVT(StrategyLogic.ARIAL_TYPES);
                        arial = logic.gatherAG(5, ggs, 0, rectangles);
                    }
                    boolean ground = false;
                    if (logic.sum(vu, StrategyLogic.GROUND_TYPES).zero()) {
                        nextMove = logic.positioningMoves.poll();
                        if (nextMove == null) ground = true;
                        if (move.getAction() == CLEAR_AND_SELECT)
                            logic.addNextMove(logic.positioningMoves.poll());
                    }
                    if (arial && ground) {
                        double[] speeds = logic.minVehicleSpeed(GFH, GIAT);
                        Rectangle[] order = logic.sOfVT(StrategyLogic.GROUND_TYPES);
                        Arrays.sort(order);
                        for (int i = 1; i < order.length; i++) horizontally &= U.eD(order[i - 1].t, order[i].t, 0.5);
                        logic.gatheredHorizontally.put(GIAT, horizontally);
                        logic.unzipGroup(GIAT, speeds[1], StrategyLogic.GROUND_TYPES);
                        logic.zipGroup(GFH, speeds[0], StrategyLogic.ARIAL_TYPES);
                        gameState = GameState.ORDER_SCALING;
                    }
                    break;
                case ORDER_SCALING:
                    if (logic.sum(vu, StrategyLogic.ARIAL_TYPES).zero()) logic.protectGround(GFH, GIAT, U.EPS);
                    if (logic.sum(vu, StrategyLogic.GROUND_TYPES).zero()) {
                        Rectangle[] order = logic.sOfVT(StrategyLogic.GROUND_TYPES);
                        Arrays.sort(order);
                        logic.mainOrder = new VehicleType[order.length];
                        for (int i = 0; i < logic.mainOrder.length; i++) {
                            logic.mainOrder[i] = order[i].vt;
                        }
                        gameState = GameState.ORDER_GROUPING;
                    }
                    break;
                case ORDER_GROUPING:
                    boolean one = false, zero = false;
                    if (logic.sum(vu, StrategyLogic.ARIAL_TYPES).zero()) logic.protectGround(GFH, GIAT, U.EPS);
                    Map<VehicleType, Rectangle> typeR = logic.mOfVT(StrategyLogic.GROUND_TYPES);
                    if (logic.sum(vu, logic.mainOrder[0]).zero())
                        one = logic.gatherAG(4.5, ggs, 1, typeR.get(logic.mainOrder[0]), typeR.get(logic.mainOrder[1]));
                    if (logic.sum(vu, logic.mainOrder[2]).zero())
                        zero = logic.gatherAG(-4.5, ggs, 2, typeR.get(logic.mainOrder[2]), typeR.get(logic.mainOrder[1]));
                    if (one && zero) {
                        gameState = GameState.ORDER_ROTATION;
                    }
                    break;
                case ORDER_ROTATION:
                    if (logic.sum(vu, StrategyLogic.ARIAL_TYPES).zero()) {
                        if (!logic.protectGround(GFH, GIAT, U.EPS) && !logic.restore()) {
                            logic.createGroupTIAFH(GFH, GIAT, GFHTAI);
                            gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                        }
                    }
                    break;
                case TACTICAL_EXECUTION:
                    double evRaS = Arrays.stream(logic.sOfVT(logic.eV(), StrategyLogic.ARIAL_TYPES)).filter(Rectangle::nonIsNaN).mapToDouble(Rectangle::square).sum();
                    double evRgS = Arrays.stream(logic.sOfVT(logic.eV(), StrategyLogic.GROUND_TYPES)).filter(Rectangle::nonIsNaN).mapToDouble(Rectangle::square).sum();
                    if (!Double.isInfinite(evRaS / evRgS) && evRaS / evRgS > 1.2) {
                        gameState = GameState.TACTICAL_EXECUTION_GROUPS;
                        groups = new HashSet<>(Arrays.asList(GFH, GIAT));
                    } else {
                        groups = new HashSet<>(Collections.singletonList(GFHTAI));
                        gameState = GameState.TACTICAL_EXECUTION_SINGLE;
                    }
                    break;
                case TACTICAL_EXECUTION_GROUPS:
                    evRaS = Arrays.stream(logic.sOfVT(logic.eV(), StrategyLogic.ARIAL_TYPES)).filter(Rectangle::nonIsNaN).mapToDouble(Rectangle::square).sum();
                    evRgS = Arrays.stream(logic.sOfVT(logic.eV(), StrategyLogic.GROUND_TYPES)).filter(Rectangle::nonIsNaN).mapToDouble(Rectangle::square).sum();;
                    if (Double.isInfinite(evRaS/evRgS) || evRaS/evRgS < 1.2) {
                        groups = new HashSet<>(Collections.singletonList(GFHTAI));
                        if (logic.protectGround(GFH, GIAT, U.EPS)) {
                            gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                        }
                    }
                    break;
                case TACTICAL_EXECUTION_CHANGE:
                    if (groups.contains(GFHTAI) && logic.sum(vu, StrategyLogic.ARIAL_TYPES).zero()) {
                        if (!logic.protectGround(GFH, GIAT, U.EPS) && !logic.restore()) gameState = GameState.TACTICAL_EXECUTION;
                    }
                    if (logic.sum(vu, VehicleType.values()).zero()) {
                        gameState = GameState.TACTICAL_EXECUTION;
                    } else return;
                    break;
                case TACTICAL_EXECUTION_SINGLE:
                    if (groupGameStateMap.get(GFHTAI) != GroupGameState.WAIT_COMMAND_FINISH) {
                        if (logic.protectGround(GFH, GIAT, U.EPS) || logic.restore()) {
                            Rectangle gR = logic.sOfVG(GIAT)[0];
                            logic.makeGroupMove(GIAT, gR, gR.cX(), gR.cY(), 0, true);
                            gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                            return;
                        } else {
                            evRaS = Arrays.stream(logic.sOfVT(logic.eV(), StrategyLogic.ARIAL_TYPES)).filter(Rectangle::nonIsNaN).mapToDouble(Rectangle::square).sum();
                            evRgS = Arrays.stream(logic.sOfVT(logic.eV(), StrategyLogic.GROUND_TYPES)).filter(Rectangle::nonIsNaN).mapToDouble(Rectangle::square).sum();;
                            if (!Double.isInfinite(evRaS/evRgS) && evRaS/evRgS > 1.2)
                                gameState = GameState.TACTICAL_EXECUTION;
                        }
                    }
                    break;
                case TACTICAL_EXECUTION_FACILITIES:
                    Facility[] facilities = world.getFacilities();
                    Rectangle[] evR = logic.sOfVG(FACILITY_GROUPS);
                    Map<Integer, Accumulator> accumulatorMap = new HashMap<>();
                    logic.mV().flatMap(v -> v.gs.stream()).forEach(gid -> accumulatorMap.computeIfAbsent(gid, k -> new Accumulator()).inc());
                    Iterator<Integer> git = groups.iterator();
                    while (git.hasNext()) {
                        Integer gid = git.next();
                        if (!accumulatorMap.containsKey(gid) || accumulatorMap.get(gid).zero()) {
                            manufacturedEmptyGroupIds.add(gid);
                            git.remove();
                            logic.groupPositioningMoves.remove(gid);
                        }
                    }
                    groups.addAll(accumulatorMap.keySet());
                    for (Integer group : groups) {
                        logic.groupPositioningMoves.computeIfAbsent(group, k -> new LinkedList<>());
                    }
                    F:for (Facility facility : facilities) {
                        if (facility.getOwnerPlayerId() != me.getId()) continue;
                        if (facility.getType() == FacilityType.CONTROL_CENTER) continue;
                        Rectangle facilityRect = new Rectangle(facility);
                        for (Rectangle anEvR : evR) {
                            if (anEvR.intersects(facilityRect)) continue F;
                        }
                        if (logic.mV().filter(v -> v.gs.isEmpty() && facilityRect.include(v.x(), v.y())).count() > 20) {
                            logic.addNextMove(MoveBuilder.c(CLEAR_AND_SELECT).setRect(facilityRect).vehicleType(TANK));
                            Iterator<Integer> it = manufacturedEmptyGroupIds.iterator();
                            int gid;
                            if (it.hasNext()) {
                                gid = it.next();
                                it.remove();
                            } else gid = manufacturedGroupIndex++;
                            logic.addNextMove(MoveBuilder.c(ASSIGN).group(gid));
                            groups.add(gid);
                            groupGameStateMap.put(gid, GroupGameState.WAIT_COMMAND_FINISH);
                            groupTypesMap.put(gid, EnumSet.of(TANK));
                            logic.groupPositioningMoves.computeIfAbsent(gid, k -> new LinkedList<>());

                        }
                    }
                    extractNextMoves();
                    break;
            }

            boolean setupNuclearStrike = false;
            if (logic.moves.isEmpty()) {
                int[][] gpt = Arrays.stream(logic.sOfVG(groups)).map(Rectangle::c)
                        .map(pt -> pt.inWorld(logic.world, logic.factor)).toArray(value -> new int[value][2]);
                int[][] fpt = logic.facilityMap.values().stream().filter(f -> f.getOwnerPlayerId() == me.getId()).map(Rectangle::new)
                        .map(Rectangle::c).map(pt -> pt.inWorld(logic.world, logic.factor)).toArray(value -> new int[value][2]);
                int[][] empt = logic.enemyMap();
                for (Integer gid : groups) {
                    GroupGameState groupGameState = groupGameStateMap.get(gid);
                    Queue<MoveBuilder> mb = logic.groupPositioningMoves.get(gid);
                    if (!setupNuclearStrike) setupNuclearStrike = logic.setupNuclearStrike(gid);
                    switch (groupGameState) {
                        case TACTICAL_EXECUTION:
                            int captured = gid == GFH ? 1 : logic.captureNearestFactory(gid, gpt, fpt, empt);
                            if (captured > 0) {
                                VehicleTick ep = logic.cEP(gid == GFH, gid);
                                if (ep == null) {
                                    ep = logic.cEP(false, gid);
                                    if (ep == null) continue;
                                }
                                Rectangle rectangle = logic.sOfVG(gid)[0];
                                if (logic.myVehicleReadyAttack(gid) < 60) {
                                    Line line = new Line(new P2D(ep.x(), ep.y()), new P2D(rectangle.cX(), rectangle.cY()));
                                    double angle = Line.angle(line, rectangle.sightLines()[0]);
                                    double angle2 = Line.angle(line, rectangle.sightLines()[1]);
                                    if (logic.facilityMap.isEmpty() && logic.vehicleGroupStateMap.get(gid) != VehicleTypeState.ROTATING &&
                                            Math.min(angle, angle2) > PI / 4 && rectangle.rotation(world.getWidth(), world.getHeight())) {
                                        logic.scaleGroup(1.2, rectangle.cX(), rectangle.cY(), rectangle.speed, gid);
                                        logic.rotateGroup(rectangle.g, Math.min(angle, angle2) - PI / 4, rectangle.cX(), rectangle.cY(), rectangle.speed);
                                    } else {
                                        if (logic.vehicleGroupStateMap.get(gid) == VehicleTypeState.ROTATING)
                                            logic.scaleGroup(.8, rectangle.cX(), rectangle.cY(), rectangle.speed, gid);
                                        if (groupTypesMap.get(gid).contains(FIGHTER) || groupTypesMap.get(gid).contains(HELICOPTER)) {
                                            logic.makeTacticalGroupMove(gid, ep.x(), ep.y(), rectangle.speed);
                                        } else {
                                            logic.makeGroundTacticalGroupMove(gid, ep.x(), ep.y(), rectangle, gpt, fpt);
                                        }
                                    }
                                } else {
                                    logic.vehicleGroupStateMap.put(gid, VehicleTypeState.ATTACK);
                                }
                            }
                            if (gid != GFH) groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                            break;
                        case WAIT_COMMAND_FINISH:
                            if ((mb == null || mb.isEmpty()) && logic.accGroups(logic.groupUpdates, gid).zero())
                                groupGameState = GroupGameState.TACTICAL_EXECUTION;
                            break;
                        case NUCLEAR_STRIKE:
                            Rectangle rectangle = logic.sOfVG(gid)[0];
                            Player enemy = logic.enemy;
                            double y = enemy.getNextNuclearStrikeY();
                            double x = enemy.getNextNuclearStrikeX();
                            Rectangle nsRectangle = Rectangle.nsRectangle(new P2D(x, y));
                            if (rectangle.intersects(nsRectangle)) {
                                rectangle.nsp = new P2D(x, y);
                                logic.updateNuclearStrikeGroupPoint(rectangle.g, rectangle);
                                logic.scaleGroup(10, x, y, 0, rectangle.g);
                                groupGameState = GroupGameState.NUCLEAR_STRIKE_RECOVERY;
                            } else {
                                logic.updateNuclearStrikeGroupPoint(rectangle.g, null);
                                groupGameState = GroupGameState.TACTICAL_EXECUTION;
                            }
                            break;
                        case NUCLEAR_STRIKE_RECOVERY:
                            if (logic.enemy.getNextNuclearStrikeTickIndex() < 0) {
                                rectangle = logic.sOfVG(gid)[0];
                                Rectangle nsR = logic.nuclearStrikeGroupPoint(rectangle.g);
                                if (nsR != null) {
                                    logic.scaleGroup(nsR.square() / rectangle.square(), nsR.nsp.x, nsR.nsp.y, 0, rectangle.g);
                                    groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                                } else {
                                    groupGameState = GroupGameState.TACTICAL_EXECUTION;
                                }
                            }
                            break;
                    }
                    groupGameStateMap.put(gid, groupGameState);
                }
            }
        }
        nextMove = nextMove == null ? logic.nextMove() : nextMove;
        if (nextMove != null) {
            if (DEBUG) System.out.print(world.getTickIndex() + " ");
            if (DEBUG) System.out.println(nextMove);
            nextMove.setMove(move);
        }
    }

    private void extractNextMoves() {
        for (Integer gid : groups) {
            if (logic.accGroups(logic.groupUpdates, gid).zero()) {
                Queue<MoveBuilder> mb = logic.groupPositioningMoves.get(gid);
                MoveBuilder nextMove = mb.poll();
                if (nextMove != null) {
                    logic.addNextMove(nextMove);
                    if (nextMove.isSelect())
                        logic.addNextMove(mb.poll());
                    groupGameStateMap.put(gid, MyStrategy.GroupGameState.WAIT_COMMAND_FINISH);
                }
            }
        }
    }
}

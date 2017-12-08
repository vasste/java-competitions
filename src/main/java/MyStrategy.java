import model.*;

import java.util.*;

import static java.lang.Math.PI;
import static model.ActionType.CLEAR_AND_SELECT;
import static model.VehicleType.*;

public final class MyStrategy implements Strategy {
    final VehicleType[] ARIAL_TYPES = new VehicleType[]{FIGHTER, HELICOPTER};
    final VehicleType[] GROUND_TYPES = new VehicleType[]{TANK, ARRV, IFV};
    public enum VehicleTypeState {MOVING, WAITING, SCALING}
    public enum GameState {ORDER_CREATION, ORDER_POSITIONING, ORDER_SCALING,
        ORDER_GROUPING, ORDER_ROTATION, TACTICAL_EXECUTION, TACTICAL_EXECUTION_GROUPS, TACTICAL_EXECUTION_SINGLE,
        TACTICAL_EXECUTION_CHANGE, TACTICAL_EXECUTION_FACILITES}
    public enum GroupGameState {NUCLEAR_STRIKE, NUCLEAR_STRIKE_RECOVERY, WAIT_COMMAND_FINISH, TACTICAL_EXECUTION}
    public enum GroupOrderState { I, INX, INY, NX, NY, F }
    static final Boolean DEBUG = true;
    private final StrategyLogic logic = new StrategyLogic();
    GameState gameState = GameState.ORDER_CREATION;
    GroupOrderState[] ggs = new GroupOrderState[]{GroupOrderState.I, GroupOrderState.I, GroupOrderState.I};
    Map<Integer, GroupGameState> groupGameStateMap = new HashMap<>();
    Map<Integer, Set<VehicleType>> groupTypesMap = new HashMap<>();
    List<Integer> groups = new ArrayList<>();
    boolean horizontally = true;

    public static final int GIAT    = 1;
    public static final int GFH     = 2;
    public static final int GFHTAI  = 3;

    public static final int GFHTAI1  = 4;
    public static final int GFHTAI2  = 5;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        Map<VehicleType, Accumulator> vu = logic.update(me, world, game);
        if (logic.enemy.getNextNuclearStrikeTickIndex() > 0) {
            for (Integer gid : groupGameStateMap.keySet()) {
                if (groupGameStateMap.get(gid) != GroupGameState.NUCLEAR_STRIKE_RECOVERY)
                    groupGameStateMap.put(gid, GroupGameState.NUCLEAR_STRIKE);
            }
        }
        if (me.getRemainingActionCooldownTicks() > 0) return;
        MoveBuilder nextMove = logic.nextMove();
        if (nextMove == null) {
            double wordedSquare = world.getHeight() * world.getWidth();
            switch (gameState) {
                case ORDER_CREATION:
                    Rectangle[] rs = logic.sOfVT(GROUND_TYPES);
                    for (int i = 0; i < rs.length; i++) {
                        rs[i].commands = new ArrayDeque<>();
                        rs[i].g = i + 1;
                    }
                    Rectangle[] ars = logic.sOfVT(ARIAL_TYPES);
                    Arrays.sort(ars);
                    OrderGraph.setupGroupingMoves(rs, world);
                    logic.createGroupFH(GFH);
                    logic.createGroupTIA(GIAT);
                    for (Rectangle ar : ars) logic.positionGroup(ar.vt, ar, true, true, 20);
                    for (Rectangle r : rs) {
                        while (!r.commands.isEmpty()) logic.positioningMoves.add(r.commands.pollLast());
                    }
                    gameState = GameState.ORDER_POSITIONING;
                    groupTypesMap.put(GFH, EnumSet.of(FIGHTER, HELICOPTER));
                    groupTypesMap.put(GIAT, EnumSet.of(TANK, IFV, ARRV));
                    groupTypesMap.put(GFHTAI, EnumSet.of(FIGHTER, HELICOPTER, TANK, IFV, ARRV));
                    groupTypesMap.put(GFHTAI1, EnumSet.of(FIGHTER, HELICOPTER, TANK, IFV, ARRV));
                    groupTypesMap.put(GFHTAI2, EnumSet.of(FIGHTER, HELICOPTER, TANK, IFV, ARRV));
                    groupGameStateMap.put(GFH, GroupGameState.TACTICAL_EXECUTION);
                    groupGameStateMap.put(GIAT, GroupGameState.TACTICAL_EXECUTION);
                    groupGameStateMap.put(GFHTAI, GroupGameState.TACTICAL_EXECUTION);
                    groupGameStateMap.put(GFHTAI1, GroupGameState.TACTICAL_EXECUTION);
                    groupGameStateMap.put(GFHTAI2, GroupGameState.TACTICAL_EXECUTION);
                    break;
                case ORDER_POSITIONING:
                    boolean arial = false, ground = false;
                    if (logic.sum(vu, ARIAL_TYPES).zero()) {
                        if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.MOVING) {
                            ars = logic.sOfVT(ARIAL_TYPES);
                            logic.scaleVehicles(FIGHTER, 1.6, ars[0]);
                            logic.scaleVehicles(HELICOPTER, 1.6, ars[1]);
                        } else if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.SCALING) {
                            if (ggs[0] == GroupOrderState.NX) logic.gatheredHorizontally.put(GFH, true);
                            else if (ggs[0] == GroupOrderState.NY) logic.gatheredHorizontally.put(GFH, false);
                            Rectangle[] rectangles = logic.sOfVT(ARIAL_TYPES);
                            arial = logic.gatherAG(5, ggs, 0, rectangles);
                        }
                    }
                    if (logic.sum(vu, GROUND_TYPES).zero()) {
                        nextMove = logic.positioningMoves.poll();
                        if (nextMove == null) ground = true;
                        if (move.getAction() == CLEAR_AND_SELECT)
                            logic.addNextMove(logic.positioningMoves.poll());
                    }
                    if (arial && ground) {
                        double[] speeds = logic.minVehicleSpeed(GFH, GIAT);
                        Rectangle[] order = logic.sOfVT(GROUND_TYPES);
                        Arrays.sort(order);
                        for (int i = 1; i < order.length; i++) horizontally &= U.eD(order[i - 1].t, order[i].t, 0.5);
                        logic.gatheredHorizontally.put(GIAT, horizontally);
                        logic.unzipGroup(GIAT, speeds[1], GROUND_TYPES);
                        logic.zipGroup(GFH, speeds[0], ARIAL_TYPES);
                        gameState = GameState.ORDER_SCALING;
                    }
                    break;
                case ORDER_SCALING:
                    if (logic.sum(vu, ARIAL_TYPES).zero()) logic.protectGround(GFH, GIAT);
                    if (logic.sum(vu, GROUND_TYPES).zero()) {
                        Rectangle[] order = logic.sOfVT(GROUND_TYPES);
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
                    if (logic.sum(vu, ARIAL_TYPES).zero()) logic.protectGround(GFH, GIAT);
                    Map<VehicleType, Rectangle> typeR = logic.mOfVT(GROUND_TYPES);
                    if (logic.sum(vu, logic.mainOrder[0]).zero())
                        one = logic.gatherAG(4.5, ggs, 1, typeR.get(logic.mainOrder[0]), typeR.get(logic.mainOrder[1]));
                    if (logic.sum(vu, logic.mainOrder[2]).zero()) zero =
                            logic.gatherAG(-4.5, ggs, 2, typeR.get(logic.mainOrder[2]), typeR.get(logic.mainOrder[1]));
                    if (one && zero) {
                        gameState = GameState.ORDER_ROTATION;
                    }
                    break;
                case ORDER_ROTATION:
                    if (logic.sum(vu, ARIAL_TYPES).zero()) {
                        if (!logic.protectGround(GFH, GIAT)) {
                            if (!logic.restore()) {
                                logic.createGroupTIAFH(GFH, GIAT, GFHTAI);
                                if (!logic.facilityMap.isEmpty()) {
                                    logic.createGroupTIAFH(horizontally, GFHTAI1, GFHTAI2);
                                }
                                gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                            }
                        }
                    }
                    break;
                case TACTICAL_EXECUTION:
                    if (!logic.facilityMap.isEmpty()) {
                        if (logic.groupFromFacilityMap.isEmpty()) {
                            Rectangle[] evR = logic.sOfVG(GFHTAI1, GFHTAI2);
                            Facility[] startFacilities = new Facility[]{logic.facilityMap.get(logic.facilityIdStart),
                                    logic.facilityMap.get(logic.facilityIdEnd)};
                            if (Line.intersect(new P2D(startFacilities[0]), evR[0].c(), new P2D(startFacilities[1]), evR[1].c())) {
                                logic.groupFromFacilityMap.put(GFHTAI1, startFacilities[1]);
                                logic.groupFromFacilityMap.put(GFHTAI2, startFacilities[0]);
                                logic.groupFromFacilityPoint.put(GFHTAI1, logic.reverseFacilitiesTerrainPoint);
                                logic.groupFromFacilityPoint.put(GFHTAI2, logic.facilitiesTerrainPoint);
                            } else {
                                logic.groupFromFacilityMap.put(GFHTAI1, startFacilities[0]);
                                logic.groupFromFacilityMap.put(GFHTAI2, startFacilities[1]);
                                logic.groupFromFacilityPoint.put(GFHTAI2, logic.reverseFacilitiesTerrainPoint);
                                logic.groupFromFacilityPoint.put(GFHTAI1, logic.facilitiesTerrainPoint);
                            }
                        }
                        gameState = GameState.TACTICAL_EXECUTION_FACILITES;
                        groups = new LinkedList<>(Arrays.asList(GFHTAI1, GFHTAI2));
                    } else {
                        Rectangle[] evR = logic.sOfVT(logic.eV(), FIGHTER, HELICOPTER, TANK);
                        if ((evR[0].square() / wordedSquare >= 0.5 || evR[1].square() / wordedSquare >= 0.5) && evR[2].square() / wordedSquare < 0.1) {
                            gameState = GameState.TACTICAL_EXECUTION_GROUPS;
                            groups =  new LinkedList<>(Arrays.asList(GFH, GIAT));
                        } else {
                            groups =  new LinkedList<>(Collections.singletonList(GFHTAI));
                            gameState = GameState.TACTICAL_EXECUTION_SINGLE;
                        }
                    }
                    break;
                case TACTICAL_EXECUTION_GROUPS:
                    Rectangle[] evR = logic.sOfVT(logic.eV(), FIGHTER, HELICOPTER);
                    if (evR[0].square() / wordedSquare < 0.15 && evR[1].square() / wordedSquare < 0.15) {
                        if (logic.protectGround(GFH, GIAT)) gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                    }
                    break;
                case TACTICAL_EXECUTION_CHANGE:
                    if (logic.sum(vu, VehicleType.values()).zero()) {
                        gameState = GameState.TACTICAL_EXECUTION;
                    } else return;
                    break;
                case TACTICAL_EXECUTION_SINGLE:
                    if (groupGameStateMap.get(GFHTAI) != GroupGameState.WAIT_COMMAND_FINISH) {
                        if (logic.protectGround(GFH, GIAT) || logic.restore()) {
                            Rectangle gR = logic.sOfVG(GIAT)[0];
                            logic.makeGroupMove(GIAT, gR, gR.cX(), gR.cY(), 0, true);
                            gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                            return;
                        } else
                            evR = logic.sOfVT(logic.eV(), FIGHTER, HELICOPTER, TANK);
                        if (evR[0].square() / wordedSquare < 0.15 && evR[1].square() / wordedSquare < 0.15)
                            gameState = GameState.TACTICAL_EXECUTION;
                    }
                    break;
                case TACTICAL_EXECUTION_FACILITES:
                    Iterator<Integer> itg = groups.iterator();
                    while (itg.hasNext()) {
                        int gid = itg.next();
                        GroupGameState groupGameState = groupGameStateMap.get(gid);
                        if (logic.accGroups(logic.groupUpdates, gid).zero()) {
                            Queue<MoveBuilder> mb = logic.groupPositioningMoves.get(gid);
                            nextMove = mb.poll();
                            if (nextMove == null) groupGameState = GroupGameState.TACTICAL_EXECUTION;
                            else {
                                nextMove.setMove(move);
                                if (move.getAction() == CLEAR_AND_SELECT)
                                    logic.addNextMove(mb.poll());
                                itg.remove();
                                groups.add(gid);
                                break;
                            }
                        }
                        groupGameStateMap.put(gid, groupGameState);
                    }
                    break;
            }

            for (Integer gid : groups) {
                GroupGameState groupGameState = groupGameStateMap.get(gid);
                switch (groupGameState) {
                    case TACTICAL_EXECUTION:
                        logic.setupNuclearStrike(gid);
                        int captured = logic.captureNearestFactory(gid);
                        if (captured > 0) {
                            VehicleTick ep = logic.cEP(false, gid);
                            if (ep == null) return;
                            if (logic.myVehicleReadyAttack(gid) < 20) {
                                Rectangle  rectangle = logic.sOfVG(gid)[0];
                                Line line = new Line(new P2D(ep.x(), ep.y()), new P2D(rectangle.cX(), rectangle.cY()));
                                double angle =  Line.angle(line, rectangle.sightLines()[0]);
                                double angle2 =  Line.angle(line, rectangle.sightLines()[1]);
                                if (Math.min(angle, angle2) > PI/8 && rectangle.rotation(world.getWidth(), world.getHeight())) {
                                    logic.rotateGroup(rectangle.g, Math.min(angle, angle2) - PI/8 , rectangle.cX(), rectangle.cY(), rectangle.speed);
                                    groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                                } else {
                                    if (!logic.makeTacticalGroupMove(gid, ep.x(), ep.y(), rectangle.speed,
                                            logic.sum(vu, groupTypesMap.get(gid)).value)) {
                                        groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                                    }
                                }
                            }
                        } else {
                            groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                        }
                        break;
                    case WAIT_COMMAND_FINISH:
                        if (logic.accGroups(logic.groupUpdates, gid).zero()) groupGameState = GroupGameState.TACTICAL_EXECUTION;
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
                                logic.scaleGroup(nsR.square()/rectangle.square(), nsR.nsp.x, nsR.nsp.y,0, rectangle.g);
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
        nextMove = nextMove == null ? logic.nextMove() : nextMove;
        if (nextMove != null) {
            if (DEBUG) System.out.print(world.getTickIndex() + " ");
            if (DEBUG) System.out.println(nextMove);
            nextMove.setMove(move);
        }
    }
}

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
        TACTICAL_EXECUTION_CHANGE}
    public enum GroupGameState {NUCLEAR_STRIKE, NUCLEAR_STRIKE_RECOVERY, WAIT_COMMAND_FINISH, TACTICAL_EXECUTION}
    public enum GroupOrderState { I, INX, INY, NX, NY, F }
    static final Boolean DEBUG = true;
    private final StrategyLogic logic = new StrategyLogic();
    GameState gameState = GameState.ORDER_CREATION;
    GroupOrderState[] ggs = new GroupOrderState[]{GroupOrderState.I, GroupOrderState.I, GroupOrderState.I};
    Map<Integer, GroupGameState> groupGameStateMap = new HashMap<>();
    Map<Integer, Set<VehicleType>> groupTypesMap = new HashMap<>();
    Set<Integer> groups = new HashSet<>();

    public static final int GIAT    = 1;
    public static final int GFH     = 2;
    public static final int GFHTAI  = 3;

    boolean restore() {
        Rectangle[] rectangles = logic.sOfVG(GFH, GIAT);
        double angleA = rectangles[0].angle;
        double angleG = rectangles[1].angle;
        if (!U.eD(angleG, angleA)) {
            logic.rotateGroup(GFH, angleA - angleG, rectangles[0].cX(), rectangles[0].cY(), rectangles[0].speed);
            return true;
        }
        return false;
    }

    @Override
    public void move(Player me, World world, Game game, Move move) {
        Map<VehicleType, Accumulator> vu = logic.update(me, world, game);
        if (me.getRemainingActionCooldownTicks() > 0) return;
        if (logic.enemy.getNextNuclearStrikeTickIndex() > 0) {
            for (Integer gid : groupGameStateMap.keySet()) {
                if (groupGameStateMap.get(gid) != GroupGameState.NUCLEAR_STRIKE_RECOVERY)
                    groupGameStateMap.put(gid, GroupGameState.NUCLEAR_STRIKE);
            }
        }
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
                    groupGameStateMap.put(GFH, GroupGameState.TACTICAL_EXECUTION);
                    groupGameStateMap.put(GIAT, GroupGameState.TACTICAL_EXECUTION);
                    groupGameStateMap.put(GFHTAI, GroupGameState.TACTICAL_EXECUTION);
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
                        boolean horizontally = true;
                        for (int i = 1; i < order.length; i++) horizontally &= order[i - 1].t == order[i].t;
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
                            if (!restore()) {
                                logic.createGroupTIAFH(GFH, GIAT, GFHTAI);
                                gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                            }
                        }
                    }
                    break;
                case TACTICAL_EXECUTION:
                    Rectangle[] evR = logic.sOfVT(logic.eV(), FIGHTER, HELICOPTER, TANK);
                    if ((evR[0].square() / wordedSquare >= 0.5 || evR[1].square() / wordedSquare >= 0.5) && evR[2].square() / wordedSquare < 0.1) {
                        gameState = GameState.TACTICAL_EXECUTION_GROUPS;
                        groups = new HashSet<>(Arrays.asList(GFH, GIAT));
                    } else if (evR[0].square() / wordedSquare < 0.15 && evR[1].square() / wordedSquare < 0.15) {
                        logic.protectGround(GFH, GIAT);
                        groups = new HashSet<>(Arrays.asList(GFHTAI));
                        gameState = GameState.TACTICAL_EXECUTION_SINGLE;
                    }
                    break;
                case TACTICAL_EXECUTION_GROUPS:
                    evR = logic.sOfVT(logic.eV(), FIGHTER, HELICOPTER, TANK);
                    if (evR[0].square() / wordedSquare < 0.15 && evR[1].square() / wordedSquare < 0.15) {
                        if (logic.protectGround(GFH, GIAT)) gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                    }
                    break;
                case TACTICAL_EXECUTION_CHANGE:
                    if (logic.sum(vu, VehicleType.values()).zero()) {
                        gameState = GameState.TACTICAL_EXECUTION;
                    } else return;
                case TACTICAL_EXECUTION_SINGLE:
                    if (restore() || logic.protectGround(GFH, GIAT)) gameState = GameState.TACTICAL_EXECUTION_CHANGE;
                    break;
            }

            for (Integer gid : groups) {
                GroupGameState groupGameState = groupGameStateMap.get(gid);
                switch (groupGameState) {
                    case TACTICAL_EXECUTION:
                        if (!logic.setupNuclearStrike(gid)) {
                            VehicleTick ep = logic.cEP(false, gid);
                            if (ep == null) return;
                            if (logic.myVehicleReadyAttack(gid) < 20) {
                                Rectangle rectangle = logic.sOfVG(gid)[0];
                                Line line = new Line(new P2D(ep.x(), ep.y()), new P2D(rectangle.cX(), rectangle.cY()));
                                double angle = Line.angle(line, rectangle.sideW());
                                if (Math.abs(PI/2 - angle) > 0.01) {
                                    logic.rotateGroup(rectangle.g, -PI/2 + angle, rectangle.cX(), rectangle.cY(), rectangle.speed);
                                    groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                                } else {
                                    if (!logic.makeTacticalGroupMove(gid, ep.x(), ep.y(), rectangle.speed,
                                            logic.sum(vu, groupTypesMap.get(gid)).value)) {
                                        groupGameState = GroupGameState.WAIT_COMMAND_FINISH;
                                    }
                                }
                            }
                        }
                        break;
                    case WAIT_COMMAND_FINISH:
                        if (logic.sum(vu, groupTypesMap.get(gid)).zero()) groupGameState = GroupGameState.TACTICAL_EXECUTION;
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
                            logic.scaleGroup(10, x, y, rectangle.speed, rectangle.g);
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
                                P2D center = rectangle.c();
                                double dx = nsR.cX() - nsR.nsp.x;
                                double dy = nsR.cY() - nsR.nsp.y;
                                logic.scaleGroup(nsR.square() / rectangle.square(), center.x + dx, center.y + dy,
                                        rectangle.speed, rectangle.g);
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

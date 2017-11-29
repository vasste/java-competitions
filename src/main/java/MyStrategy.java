import model.*;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;

import static java.lang.Math.PI;
import static model.ActionType.CLEAR_AND_SELECT;
import static model.VehicleType.*;

public final class MyStrategy implements Strategy {
    final VehicleType[] ARIAL_TYPES = new VehicleType[]{FIGHTER, HELICOPTER};
    final VehicleType[] GROUND_TYPES = new VehicleType[]{TANK, ARRV, IFV};
    public enum VehicleTypeState {MOVING, WAITING, SCALING}
    public enum GameState {ORDER_CREATION, ORDER_POSITIONING, ORDER_SCALING,
        ORDER_GROUPING, ORDER_ROTATION, NUCLEAR_STRIKE, NUCLEAR_STRIKE_RECOVERY,
        NUCLEAR_STRIKE_FINISH_RECOVERY, TACTICAL_EXECUTION, WAIT_COMMAND_FINISH,
        TACTICAL_EXECUTION_SINGLE, TACTICAL_EXECUTION_MULTI, ENSF, FHFG, FHNSD, FHBNSD, END}
    public enum GroupGameState { I, INX, INY, NX, NY, F }
    static final Boolean DEBUG = true;
    private final StrategyLogic logic = new StrategyLogic();
    GameState gameState = GameState.ORDER_CREATION;
    GroupGameState[] ggs = new GroupGameState[]{GroupGameState.I, GroupGameState.I, GroupGameState.I};
    GameState gameStateBeforeNuclearStrike;

    public static final int GIAT    = 1;
    public static final int GFH     = 2;
    public static final int GFHTAI  = 3;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        Map<VehicleType, Accumulator> vu = logic.update(me, world, game);
        if (me.getRemainingActionCooldownTicks() > 0) return;
        if (logic.enemy.getNextNuclearStrikeTickIndex() > 0 && gameState != GameState.NUCLEAR_STRIKE_RECOVERY) {
            gameStateBeforeNuclearStrike = gameState;
            gameState = GameState.NUCLEAR_STRIKE;
        }
        MoveBuilder nextMove = logic.nextMove();
        if (nextMove == null) {
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
                    break;
                case ORDER_POSITIONING:
                    boolean arial = false, ground = false;
                    if (logic.sum(vu, ARIAL_TYPES).zero()) {
                        if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.MOVING) {
                            ars = logic.sOfVT(ARIAL_TYPES);
                            logic.scaleVehicles(FIGHTER, 1.6, ars[0]);
                            logic.scaleVehicles(HELICOPTER, 1.6, ars[1]);
                        } else if (logic.vehicleTypeStateMap.get(FIGHTER) == VehicleTypeState.SCALING) {
                            if (ggs[0] == GroupGameState.NX) logic.gatheredHorizontally.put(GFH, true);
                            else if (ggs[0] == GroupGameState.NY) logic.gatheredHorizontally.put(GFH, false);
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
                    if (logic.sum(vu, ARIAL_TYPES).zero()) logic.protectGround(GFH, GIAT);
                    if (logic.sum(vu, GROUND_TYPES).zero()) {
                        Rectangle[] rectangles = logic.sOfVG(GFH, GIAT);
                        Rectangle rectangle = logic.OfV(logic.eV());
                        double[] speeds = logic.minVehicleSpeed(GFH, GIAT);
                        double angleG = Line.angle(rectangles[0].sidew(), rectangle.sidew());
                        double angleA = Line.angle(rectangles[1].sidew(), rectangle.sidew());
                        if (!U.eD(0, angleA))
                            logic.rotateGroup(GFH, angleA, rectangles[0].cX(), rectangles[0].cY(), speeds[0]);
                        if (!U.eD(0, angleG))
                            logic.rotateGroup(GIAT, angleG, rectangles[1].cX(), rectangles[1].cY(), speeds[1]);
                        logic.createGroupTIAFH(GFH, GIAT, GFHTAI);
                        gameState = GameState.TACTICAL_EXECUTION;
                    }
                    break;
                case TACTICAL_EXECUTION:
                    Rectangle[] evR = logic.sOfVT(logic.eV(), FIGHTER, HELICOPTER, TANK);
                    double wordedSquare = world.getHeight()*world.getWidth();
                    if ((evR[0].square()/wordedSquare >= 0.5 || evR[1].square()/wordedSquare >= 0.5) && evR[2].square()/wordedSquare < 0.1) {
                        gameState = GameState.TACTICAL_EXECUTION_MULTI;
                    } else {
                        gameState = GameState.TACTICAL_EXECUTION_SINGLE;
                    }
//                case M:
//                    Rectangle[] evR = sOfVT(eV(), ARIAL_TYPES, TANK);
//                    if ((evR[0].square()/wordedSquare >= 0.5 || evR[1].square()/wordedSquare >= 0.5) && evR[2].square()/wordedSquare < 0.1) {
//                        Rectangle rectangle = OfVG(GG);
//                        makeGroupMove(GG, rectangle, rectangle.cX(), rectangle.cY(), 0, true);
//                        Rectangle rectangleFH = sOfVG(GFH)[0];
//                        scaleGroup(GFH, 1.4, rectangleFH.l, rectangleFH.t);
//                        gameState = GameState.FHFG;
//                    } else {
//                        if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GG, move, enemy, GameState.BNSD)) return;
//                        if (!makeNuclearStrike(GG)) {
//                            VehicleTick ep = cEP(false, GG);
//                            if (ep == null) return;
//                            if (mV().filter(v -> v.attack(ep)).count() < 5) {
//                                if (!makeGroupMove(GG, ep.x(), ep.y(), minGSpeed * tS(game, OfVG(GT, GA, GI), world, terrainTypes) * 0.65,
//                                        logic.sum(vu, ARIAL_TYPES, TANK, IFV, ARRV).value)) {
//                                    gameState = GameState.FG;
//                                }
//                            }
//                        }
//                    }
                    break;
                case TACTICAL_EXECUTION_MULTI:
                    break;
                case TACTICAL_EXECUTION_SINGLE:
                    break;
                case WAIT_COMMAND_FINISH:
                    if (logic.sum(vu, VehicleType.values()).zero()) gameState = GameState.TACTICAL_EXECUTION;
                    break;
//                case FHFG:
//                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GFH, move, enemy, GameState.FHNSD)) return;
//                    if (logic.sum(vu, ARIAL_TYPES).zero()) gameState = GameState.ENSF;
//                    break;
//                case ENSF:
//                    Rectangle rectangle = OfVG(GFH);
//                    if (rectangle.isRectNaN()) {
//                        noArial = true;
//                        gameState = GameState.M;
//                        return;
//                    }
//                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GFH, move, enemy, GameState.FHNSD)) return;
//                    evR = sOfVT(eV(), ARIAL_TYPES);
//                    if (evR[0].square()/wordedSquare < 0.15 && evR[1].square()/wordedSquare < 0.15) {
//                        Rectangle drs = OfVG(GG);
//                        if (rectangle.dfct(drs) > U.EPS) {
//                            makeGroupMove(GFH, rectangle, drs.cX(), drs.cY(), minASpeed, true);
//                            gameState = GameState.FHFG;
//                        } else {
//                            gameState = GameState.FG;
//                        }
//                    } else
//                        if (!makeNuclearStrike(GFH)) {
//                            VehicleTick ep = cEP(true, GFH);
//                            if (ep == null) return;
//                            if (mVt(ARIAL_TYPES).filter(v -> v.attack(ep)).count() < 5) {
//                                makeGroupMove(GFH, ep.x(), ep.y(), minASpeed * wS(game, rectangle, world, weatherTypes) * 0.75, (vu, ARIAL_TYPES).v());
//                            }
//                        }
//                    break;
                case NUCLEAR_STRIKE:
                    if (logic.sum(vu, VehicleType.values()).zero()) {
                        Rectangle[] rectangles = logic.sOfVG(GFH, GIAT);
                        Player enemy = logic.enemy;
                        for (int i = 0; i < rectangles.length; i++) {
                            double y = enemy.getNextNuclearStrikeY();
                            double x = enemy.getNextNuclearStrikeX();
                            Rectangle rectangle = Rectangle.nsRectangle(new P2D(x, y));
                            if (rectangles[i].intersects(rectangle)) {
                                logic.updateNuclearStrikeGroupPoint(rectangles[i].g, rectangles[i]);
                                logic.scaleGroup(10, x, y, rectangles[i].speed, rectangles[i].g);
                            }
                        }
                        gameState = GameState.NUCLEAR_STRIKE_RECOVERY;
                    }
                    break;
                case NUCLEAR_STRIKE_RECOVERY:
                    if (logic.enemy.getNextNuclearStrikeTickIndex() < 0) {
                        Rectangle[] rectangles = logic.sOfVG(GFH, GIAT);
                        for (int i = 0; i < rectangles.length; i++) {
                            Rectangle nsp = logic.nuclearStrikeGroupPoint(rectangles[i].g);
                            if (nsp != null) {
                                P2D center = rectangles[i].c();
                                logic.scaleGroup(nsp.square()/rectangles[i].square(), center.x, center.y, rectangles[i].speed, rectangles[i].g);
                            }
                        }
                        gameState = GameState.NUCLEAR_STRIKE_FINISH_RECOVERY;
                    }
                    break;
                case NUCLEAR_STRIKE_FINISH_RECOVERY:
                    if (logic.sum(vu, VehicleType.values()).zero()) {
                        gameState = gameStateBeforeNuclearStrike;
                        Rectangle[] rectangles = logic.sOfVG(GFH, GIAT);
                        logic.rotateGroup(GFH, PI/4, rectangles[0].cX(), rectangles[0].cY(), rectangles[0].speed);
                        logic.rotateGroup(GIAT, PI/4, rectangles[1].cX(), rectangles[1].cY(), rectangles[1].speed);
                    }
                    break;
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

import model.*;

import java.util.*;
import java.util.stream.Stream;

import static model.ActionType.*;

/**
 * @author Vasilii Stepanov.
 * @since 24.11.2017
 */
public class WorldDominationStrategy implements Strategy {

    final Map<Long, Vehicle> mVById = new HashMap<>();
    final Map<Long, Vehicle> eVById = new HashMap<>();
    final Map<Long, Integer> vehiclesTick = new HashMap<>();
    Player me;
    Queue<MoveBuilder> moves = new LinkedList<>();
    int finished;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        Random random = new Random(game.getRandomSeed());

        Map<VehicleType, Accumulator> vuc = new HashMap<>();
        for (Vehicle vehicle : world.getNewVehicles()) {
            if (vehicle.getPlayerId() == me.getId()) {
                mVById.put(vehicle.getId(), vehicle);
            } else {
                eVById.put(vehicle.getId(), vehicle);
            }
        }
        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            Vehicle oV = mVById.get(vu.getId());
            Vehicle nV = mVById.computeIfPresent(vu.getId(), (key, vehicle) -> new Vehicle(vehicle, vu));
            if (oV != null && nV.getDurability() > 0 && oV.getDistanceTo(nV) > U.EPS/10) {
                vuc.computeIfAbsent(oV.getType(), t -> new Accumulator(0)).inc();
            }
            eVById.computeIfPresent(vu.getId(), (key, vehicle) -> new Vehicle(vehicle, vu));
            if (vu.getDurability() == 0) {
                mVById.remove(vu.getId());
                eVById.remove(vu.getId());
            } else {
                vehiclesTick.put(vu.getId(), world.getTickIndex());
            }
        }

        if (me.getRemainingActionCooldownTicks() > 0) return;
        MoveBuilder nextMove = moves.poll();
        if (nextMove == null) {
            if (finished < 4) {
                if (!sum(vuc, VehicleType.HELICOPTER, VehicleType.FIGHTER).zero()) return;
                if (finished == 0) {
                    moves.add(MoveBuilder.c(CLEAR_AND_SELECT).vehicleType(VehicleType.FIGHTER).setRect(new Rectangle(world)));
                    moves.add(MoveBuilder.c(ASSIGN).group(GF));
                    moves.add(MoveBuilder.c(SCALE).factor(1.6));
                    moves.add(MoveBuilder.c(CLEAR_AND_SELECT).vehicleType(VehicleType.HELICOPTER).setRect(new Rectangle(world)));
                    moves.add(MoveBuilder.c(ASSIGN).group(GH));
                    moves.add(MoveBuilder.c(SCALE).factor(1.6));
                    moves.add(MoveBuilder.c(CLEAR_AND_SELECT).vehicleType(VehicleType.FIGHTER).setRect(new Rectangle(world)));
                    moves.add(MoveBuilder.c(ADD_TO_SELECTION).vehicleType(VehicleType.HELICOPTER).setRect(new Rectangle(world)));
                    moves.add(MoveBuilder.c(ASSIGN).group(GFH));
                    finished++;
                } else if (finished == 1) {
                    Rectangle[] rectangles = sOfVG(GF, GH, GH);
                    gatherAG(5, ggs, 0, MyStrategy.GGS.INX, rectangles);
                    if (ggs[0] == MyStrategy.GGS.F) finished++;
                } else if (finished == 2) {
                    moves.add(MoveBuilder.c(CLEAR_AND_SELECT).vehicleType(VehicleType.FIGHTER).setRect(new Rectangle(world)));
                    moves.add(MoveBuilder.c(ADD_TO_SELECTION).vehicleType(VehicleType.HELICOPTER).setRect(new Rectangle(world)));
                    Rectangle rectangle = sOfVG(GFH)[0];
                    moves.add(MoveBuilder.c(MOVE).x(-rectangle.l).y(-rectangle.t));
                    finished++;
                } else if (finished == 3) {
                    Rectangle rects = sOfVG(GFH)[0];
                    moves.add(MoveBuilder.c(SCALE).factor(9).x(rects.l).y(rects.t));
                    finished++;
                }
            } else {
                if (me.getRemainingNuclearStrikeCooldownTicks() == 0 && me.getNextNuclearStrikeTickIndex() == -1) {
                    Rectangle mVr = OfVG(mV());
                    Rectangle eVr = OfVG(eV());
                    if (mVr.intersects(eVr)) {
                        OUT:
                        for (Vehicle v : mVById.values()) {
                            if (v.isAerial()) {
                                for (Vehicle ev : eVById.values()) {
                                    if (see(v, ev)) {
                                        moves.add(new MoveBuilder(ActionType.TACTICAL_NUCLEAR_STRIKE).vehicleId(v.getId()).x(ev.getX()).y(ev.getY()));
                                        break OUT;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            nextMove.setMove(move);
        }
    }

    private boolean gatherAG(double scale, MyStrategy.GGS[] ggs, int i, MyStrategy.GGS bne, Rectangle... rectangles) {
        Rectangle rectangleA = rectangles[0]; Rectangle rectangleB = rectangles[1];
        boolean gd = ggs[i] == MyStrategy.GGS.F || rectangles[0].dfct(rectangles[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rectangleA.cX() != rectangleB.cX() && rectangleA.cY() != rectangleB.cY()) ggs[i] = bne;
                else if (rectangleA.cX() != rectangleB.cX()) ggs[i] = MyStrategy.GGS.INY;
                else if (rectangleA.cY() != rectangleB.cY()) ggs[i] = MyStrategy.GGS.INX;
                break;
            case INX:
                if (rectangles[2].l > rectangleB.l) scale *= -1;
                if (U.eD(rectangleA.cX(), rectangleB.cX() + scale)) ggs[i] = MyStrategy.GGS.NY;
                else mkGM(rectangleA.g, rectangleA, rectangleB.cX() + scale, rectangleA.cY());
                break;
            case INY:
                if (rectangles[2].t > rectangleB.t) scale *= -1;
                if (U.eD(rectangleA.cY(), rectangleB.cY() + scale)) ggs[i] = MyStrategy.GGS.NX;
                else mkGM(rectangleA.g, rectangleA, rectangleA.cX(), rectangleB.cY() + scale);
                break;
            case NX:
                if (U.eD(rectangleA.cX(), rectangleB.cX())) ggs[i] = MyStrategy.GGS.F;
                else mkGM(rectangleA.g, rectangleA, rectangleB.cX(), rectangleA.cY());
                break;
            case NY:
                if (U.eD(rectangleA.cY(), rectangleB.cY())) ggs[i] = MyStrategy.GGS.F;
                else mkGM(rectangleA.g, rectangleA, rectangleA.cX(), rectangleB.cY());
                break;
        }
        return gd;
    }

    MyStrategy.GGS[] ggs = new MyStrategy.GGS[]{MyStrategy.GGS.I};
    void mkGM(int id, Rectangle gr, double x, double y) {
        mkGM(id, gr, x, y, 0, true);
    }

    void mkGM(int id, Rectangle groupRectangle, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(MOVE).dfCToXY(groupRectangle, x, y).maxSpeed(ms));
    }

    static boolean see(Vehicle v, Vehicle u) {
        return u.getDistanceTo(v) < v.getVisionRange();
    }

    static Accumulator acc(Map<VehicleType, Accumulator> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, Accumulator.ZERO); }
    static Accumulator sum(Map<VehicleType, Accumulator> vu, VehicleType... vehicleType) {
        return new Accumulator(Arrays.stream(vehicleType).map(vt -> acc(vu, vt)).mapToLong(Accumulator::v).sum());
    }

    Stream<Vehicle> eV() { return eVById.values().stream().filter(v -> v.getPlayerId() != me.getId()); }
    Stream<Vehicle> eVt(VehicleType vt) {
        return eV().filter(v -> v.getType() == vt);
    }
    Rectangle OfVG(Stream<Vehicle> stream) {
        return stream.reduce(new Rectangle(), (rect, v) -> rect.combine(new Rectangle(v)), Rectangle::combine);
    }
    Stream<Vehicle> mV() { return mVById.values().stream().filter(v -> v.getPlayerId() == me.getId()); }

    Rectangle[] sOfVT(VehicleType... types) {
        Rectangle[] initial = new Rectangle[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle(); }
        return mV().reduce(initial, (rects, v) -> {
            for (int i = 0; i < types.length; i++) if (v.getType() == types[i]) rects[i].update(v);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rectangle[] sOfVG(int... ids) {
        Rectangle[] initial = new Rectangle[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle();initial[i].g = ids[i]; }
        return mV().reduce(initial, (rects, v) -> {
            int[] gs = v.getGroups();
            for (int i = 0; i < ids.length; i++) for (int id : gs) if (id == ids[i]) rects[i].update(v);
            return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    public final static int GF = 1;
    public final static int GH = 2;
    public final static int GFH = 3;
}

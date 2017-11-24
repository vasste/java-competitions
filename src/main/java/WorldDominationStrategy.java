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
    Queue<MB> moves = new LinkedList<>();
    int finished;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        Random random = new Random(game.getRandomSeed());

        Map<VehicleType, IA> vuc = new HashMap<>();
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
                vuc.computeIfAbsent(oV.getType(), t -> new IA(0)).inc();
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
        MB nextMove = moves.poll();
        if (nextMove == null) {
            if (finished < 4) {
                if (!sum(vuc, VehicleType.HELICOPTER, VehicleType.FIGHTER).zero()) return;
                if (finished == 0) {
                    moves.add(MB.c(CLEAR_AND_SELECT).vehicleType(VehicleType.FIGHTER).setRect(new Rect(world)));
                    moves.add(MB.c(ASSIGN).group(GF));
                    moves.add(MB.c(SCALE).factor(1.6));
                    moves.add(MB.c(CLEAR_AND_SELECT).vehicleType(VehicleType.HELICOPTER).setRect(new Rect(world)));
                    moves.add(MB.c(ASSIGN).group(GH));
                    moves.add(MB.c(SCALE).factor(1.6));
                    moves.add(MB.c(CLEAR_AND_SELECT).vehicleType(VehicleType.FIGHTER).setRect(new Rect(world)));
                    moves.add(MB.c(ADD_TO_SELECTION).vehicleType(VehicleType.HELICOPTER).setRect(new Rect(world)));
                    moves.add(MB.c(ASSIGN).group(GFH));
                    finished++;
                } else if (finished == 1) {
                    Rect[] rects = sOfVG(GF, GH, GH);
                    gatherAG(5, ggs, 0, MyStrategy.GGS.INX, rects);
                    if (ggs[0] == MyStrategy.GGS.F) finished++;
                } else if (finished == 2) {
                    moves.add(MB.c(CLEAR_AND_SELECT).vehicleType(VehicleType.FIGHTER).setRect(new Rect(world)));
                    moves.add(MB.c(ADD_TO_SELECTION).vehicleType(VehicleType.HELICOPTER).setRect(new Rect(world)));
                    Rect rect = sOfVG(GFH)[0];
                    moves.add(MB.c(MOVE).x(-rect.l).y(-rect.t));
                    finished++;
                } else if (finished == 3) {
                    Rect rects = sOfVG(GFH)[0];
                    moves.add(MB.c(SCALE).factor(9).x(rects.l).y(rects.t));
                    finished++;
                }
            } else {
                if (me.getRemainingNuclearStrikeCooldownTicks() == 0 && me.getNextNuclearStrikeTickIndex() == -1) {
                    Rect mVr = OfVG(mV());
                    Rect eVr = OfVG(eV());
                    if (mVr.intersects(eVr)) {
                        OUT:
                        for (Vehicle v : mVById.values()) {
                            if (v.isAerial()) {
                                for (Vehicle ev : eVById.values()) {
                                    if (see(v, ev)) {
                                        moves.add(new MB(ActionType.TACTICAL_NUCLEAR_STRIKE).vehicleId(v.getId()).x(ev.getX()).y(ev.getY()));
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

    private boolean gatherAG(double scale, MyStrategy.GGS[] ggs, int i, MyStrategy.GGS bne, Rect... rects) {
        Rect rectA = rects[0]; Rect rectB = rects[1];
        boolean gd = ggs[i] == MyStrategy.GGS.F || rects[0].dfct(rects[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rectA.cX() != rectB.cX() && rectA.cY() != rectB.cY()) ggs[i] = bne;
                else if (rectA.cX() != rectB.cX()) ggs[i] = MyStrategy.GGS.INY;
                else if (rectA.cY() != rectB.cY()) ggs[i] = MyStrategy.GGS.INX;
                break;
            case INX:
                if (rects[2].l > rectB.l) scale *= -1;
                if (U.eD(rectA.cX(), rectB.cX() + scale)) ggs[i] = MyStrategy.GGS.NY;
                else mkGM(rectA.g, rectA, rectB.cX() + scale, rectA.cY());
                break;
            case INY:
                if (rects[2].t > rectB.t) scale *= -1;
                if (U.eD(rectA.cY(), rectB.cY() + scale)) ggs[i] = MyStrategy.GGS.NX;
                else mkGM(rectA.g, rectA, rectA.cX(), rectB.cY() + scale);
                break;
            case NX:
                if (U.eD(rectA.cX(), rectB.cX())) ggs[i] = MyStrategy.GGS.F;
                else mkGM(rectA.g, rectA, rectB.cX(), rectA.cY());
                break;
            case NY:
                if (U.eD(rectA.cY(), rectB.cY())) ggs[i] = MyStrategy.GGS.F;
                else mkGM(rectA.g, rectA, rectA.cX(), rectB.cY());
                break;
        }
        return gd;
    }

    MyStrategy.GGS[] ggs = new MyStrategy.GGS[]{MyStrategy.GGS.I};
    void mkGM(int id, Rect gr, double x, double y) {
        mkGM(id, gr, x, y, 0, true);
    }

    void mkGM(int id, Rect groupRect, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(MOVE).dfCToXY(groupRect, x, y).maxSpeed(ms));
    }

    static boolean see(Vehicle v, Vehicle u) {
        return u.getDistanceTo(v) < v.getVisionRange();
    }

    static IA acc(Map<VehicleType, IA> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, IA.ZERO); }
    static IA sum(Map<VehicleType, IA> vu, VehicleType... vehicleType) {
        return new IA(Arrays.stream(vehicleType).map(vt -> acc(vu, vt)).mapToLong(IA::v).sum());
    }

    Stream<Vehicle> eV() { return eVById.values().stream().filter(v -> v.getPlayerId() != me.getId()); }
    Stream<Vehicle> eVt(VehicleType vt) {
        return eV().filter(v -> v.getType() == vt);
    }
    Rect OfVG(Stream<Vehicle> stream) {
        return stream.reduce(new Rect(), (rect, v) -> rect.combine(new Rect(v)), Rect::combine);
    }
    Stream<Vehicle> mV() { return mVById.values().stream().filter(v -> v.getPlayerId() == me.getId()); }

    Rect[] sOfVT(VehicleType... types) {
        Rect[] initial = new Rect[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect(); }
        return mV().reduce(initial, (rects, v) -> {
            for (int i = 0; i < types.length; i++) if (v.getType() == types[i]) rects[i].update(v);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rect[] sOfVG(int... ids) {
        Rect[] initial = new Rect[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect();initial[i].g = ids[i]; }
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

import model.*;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.min;
import static model.ActionType.*;
import static model.VehicleType.*;

public final class MyStrategy implements Strategy {

    GS gameState = GS.I;
    GGS[] ggs = new GGS[]{GGS.I, GGS.I, GGS.I};
    GGS[] ggsI = new GGS[]{GGS.I, GGS.I};
    double minGSpeed = 0;
    double minASpeed = 0;
    final Map<Long, VeE> vehiclesById = new HashMap<>();
    Player me;
    World world;
    Queue<MB> moves = new LinkedList<>();
    double cdMs = 0;
    int[] pG = new int[3];
    VehicleType[] pT = new VehicleType[3];
    Deque<MB> gm1 = new LinkedList<>();
    Deque<MB> gm2 = new LinkedList<>();
    int lastNSTick = 0;
    Map<VehicleType, VeE> fvId = new HashMap<>();

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        Map<VehicleType, IA> vu = initV(world);
        if (cdMs /world.getTickIndex() >= 0.198) return;
        if (random == null) random = new Random(game.getRandomSeed());

        MB nextMove = moves.poll();
        if (nextMove == null) {
            switch (gameState) {
                case I:
                    for (VeE veE : vehiclesById.values()) {
                        if (veE.m(me)) fvId.compute(veE.type(), (vt, old) -> old == null ? veE : world.getTickIndex() > old.tI ? veE : old);
                    }
                    ctG(GT, TANK, 1.6);
                    ctG(GA, ARRV, 1.6);
                    ctG(GI, IFV, 1.6);
                    ctG(GH, HELICOPTER, 1.4);
                    ctG(GF, FIGHTER, 1.4);
                    VehicleType[] vs = new VehicleType[]{TANK, ARRV, IFV};
                    int[] groups = new int[]{GT, GA, GI};
                    Rect gr[] = sOfVT(TANK, ARRV, IFV);
                    Rect order = Rect.ORDER;
                    for (int i = 0; i < gr.length; i++) { gr[i].g = groups[i]; gr[i].vt = vs[i]; }
                    Arrays.sort(gr, (o1, o2) -> U.cD(o1.dfct(order), o2.dfct(order)));
                    for (int i = 0; i < gr.length; i++) { pG[i] = gr[i].g;pT[i] = gr[i].vt; }
                    gameState = GS.G;
                    break;
                case G:
                    if (sum(vu, ARRV, TANK, IFV).zero()) {
                        int[] ait = new int[]{GA,GI,GT};
                        Rect[] rs = sOfVT(ARRV, IFV, TANK);
                        G.setupGroupingMoves(gm1, gm2, rs, pG, ggsI, ait);
                        gameState = GS.GG;
                    }
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) gatherFH(vu);
                    break;
                case GG:
                    boolean fh = false, at = false, it = false;
                    if (sum(vu, pT[1], pT[0]).zero()) {
                        MB mb = gm1.pollLast();
                        if (mb != null) {
                            moves.add(mb);
                            moves.add(gm1.pollLast());
                        }
                        else
                            if (ggs[2].ordinal() <= GGS.INX.ordinal() || ggs[2] == GGS.F)
                                at = gatherAG(pG[1], pG[0], 5, ggs, 1, ggsI[0]);
                    }
                    if (sum(vu, pT[2], pT[0]).zero()) {
                        MB mb = gm2.pollLast();
                        if (mb != null) {
                            moves.add(mb);
                            moves.add(gm2.pollLast());
                        }
                        else
                            if (ggs[1].ordinal() <= GGS.INX.ordinal() || ggs[1] == GGS.F)
                                it = gatherAG(pG[2], pG[0], 5, ggs, 2, ggsI[1]);
                    }
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) fh = gatherFH(vu);
                    if (fh && at && it) {
                        moves.add(new MB(CLEAR_AND_SELECT).setRect(new Rect(world)));
                        moves.add(new MB(ASSIGN).group(GG));
                        gameState = GS.M;
                    }
                    break;
                case M:
                    if (lastNSTick > 0 && world.getTickIndex() - lastNSTick <= 30) return;
                    if (!mkNS(GG)) {
                        P2D ep = cEP(GG);
                        if (!mkGM(GG, ep.x, ep.y, minGSpeed * tS(OfVG(GT, GA, GI), world))) {
                            if (eV().noneMatch(v -> v.attack(ep))) {
                                gameState = GS.R;
                                rt(GG, PI / 32);
                            }
                        }
                    }
                    break;
                case R:
                    if (sum(vu, ARRV, TANK, IFV, FIGHTER, HELICOPTER).zero()) gameState = GS.M;
            }
        } else {
            cdMs++;
            nextMove.setMove(move);
        }
    }

    private boolean gatherFH(Map<VehicleType, IA> vu) {
        boolean fh = gatherAG(GF, GH, 4, ggs, 0, GGS.INX);
        if (fh && fvId.get(pT[0]).tI < world.getTickIndex()) {
            Rect srs = GFH == 0 ? OfVG(GF, GH) : OfVG(GFH);
            Rect drs = sum(vu, pT[0]).zero() ? OfVG(pG[0]) : cFlp(fvId.get(pT[0]));
            if (GFH == 0) ctGFH(GFH = 6);
            if (srs.dfct(drs) >= 0.1) {
                mkGM(GFH, srs, drs.cX(), drs.cY(), minASpeed, false);
            }
        }
        return fh;
    }

    Rect cFlp(VeE lc) {
        return new Rect(lc.x() - 2.0, lc.y() - 2.0, lc.y() + 84.0, lc.x() + 84.0);
    }

    static double tS(Rect rs, World world) {
        int[] wij = rs.c().inWorld(world);
        return wt(world.getTerrainByCellXY()[wij[0]][wij[0]]);
    }

    static double tS(double x, double y, World world) {
        int[] wij = new P2D(x, y).inWorld(world);
        return wt(world.getTerrainByCellXY()[wij[0]][wij[0]]);
    }

    static double wt(TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return 1;
            case SWAMP:
                return 0.6;
            case FOREST:
                return 0.7;
        }
        return 1;
    }

    static IA acc(Map<VehicleType, IA> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, IA.ZERO); }
    static IA sum(Map<VehicleType, IA> vu, VehicleType... vehicleType) {
        return new IA(Arrays.stream(vehicleType).map(vt -> acc(vu, vt)).mapToLong(IA::v).sum());
    }

    private boolean gatherAG(int a, int b, int scale, GGS[] ggs, int i, GGS bne) {
        Rect[] rects = sOfVG(a, b);
        Rect rectA = rects[0]; Rect rectB = rects[1];
        boolean gd = ggs[i] == GGS.F || rects[0].dfct(rects[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rectA.cX() != rectB.cX() && rectA.cY() != rectB.cY()) ggs[i] = bne;
                else if (rectA.cX() != rectB.cX()) ggs[i] = GGS.INY;
                else if (rectA.cY() != rectB.cY()) ggs[i] = GGS.INX;
                break;
            case INX:
                if (U.eD(rectA.cX(), rectB.cX() + scale)) ggs[i] = GGS.NY;
                else mkGM(a, rectA, rectB.cX() + scale, rectA.cY(), true);
                break;
            case INY:
                if (U.eD(rectA.cY(), rectB.cY() + scale)) ggs[i] = GGS.NX;
                else mkGM(a, rectA, rectA.cX(), rectB.cY() + scale, true);
                break;
            case NX:
                if (U.eD(rectA.cX(), rectB.cX())) ggs[i] = GGS.F;
                else mkGM(a, rectA, rectB.cX(), rectA.cY(), true);
                break;
            case NY:
                if (U.eD(rectA.cY(), rectB.cY())) ggs[i] = GGS.F;
                else mkGM(a, rectA, rectA.cX(), rectB.cY(), true);
                break;
        }
        return gd;
    }

    private P2D cEP(int... groups) {
        P2D center = OfVG(groups).c();
        return eV().reduce(new P2D(Double.MAX_VALUE, Double.MAX_VALUE), (p, VeEx) ->
                P2D.closedTo(p, new P2D(VeEx.v), center), (a, b) -> P2D.closedTo(a, b, center));
    }

    void rt(int id, double angle) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(ROTATE).angle(angle));
    }

    void mkGM(int id, double x, double y) {
        mkGM(id, x, y, 0);
    }

    boolean mkNS(int id) {
        if (lastNSTick > 0 && world.getTickIndex() - lastNSTick <= 1200) return false;
        Rect fr = sOfVT(FIGHTER)[0].add(120);
        Optional<VeE> efeV = eV().filter(veE -> fr.include(veE.x(), veE.y())).findFirst();
        if (efeV.isPresent()) {
            Vehicle ensv = efeV.get().v;
            Optional<VeE> tfeV = mVt(FIGHTER).filter(v -> v.see(ensv)).findFirst();
            if (tfeV.isPresent()) {
                Rect rect = sOfVG(id)[0];
                mkGM(id, rect, rect.cX(), rect.cY(), 0, false);
                Vehicle mtnsv = tfeV.get().v;
                moves.add(new MB(TACTICAL_NUCLEAR_STRIKE).vehicleId(mtnsv.getId()).x(ensv.getX()).y(ensv.getY()));
                lastNSTick = world.getTickIndex();
                return true;
            }
        }
        return false;
    }

    boolean mkGM(int id, double x, double y, double ms) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        Rect rect = OfV(gV(id));
        if (rect.dfct(x, y) < 0.5) {
            return false;
        } else moves.add(MB.c(MOVE).dfCToXY(rect, x, y).maxSpeed(ms));
        return true;
    }

    void mkGM(int id, Rect gr, double x, double y, boolean select) {
        mkGM(id, gr, x, y, 0, select);
    }

    void mkGM(int id, Rect groupRect, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(MOVE).dfCToXY(groupRect, x, y).maxSpeed(ms));
    }

    void ctG(int id, VehicleType vehicleType, double scale) {
        moves.add(new MB(CLEAR_AND_SELECT).vehicleType(vehicleType).setRect(new Rect(0,0,world.getHeight(),world.getWidth())));
        moves.add(MB.c(ASSIGN).group(id));
        moves.add(MB.c(SCALE).factor(scale));
    }

    void ctGFH(int id) {
        moves.add(new MB(CLEAR_AND_SELECT).vehicleType(HELICOPTER).setRect(new Rect(world)));
        moves.add(new MB(ADD_TO_SELECTION).vehicleType(FIGHTER).setRect(new Rect(world)));
        moves.add(MB.c(ASSIGN).group(id));
    }

    Map<VehicleType, IA> initV(World world) {
        Map<VehicleType, IA> vehicleUpdates = new HashMap<>();
        for (Vehicle vehicle : world.getNewVehicles()) {
            VeE ve = new VeE(vehicle, world.getTickIndex());
            vehiclesById.put(vehicle.getId(), ve);
        }

        if (minGSpeed == 0) {
            minGSpeed = Double.MAX_VALUE;
            minASpeed = Double.MAX_VALUE;
            for (VeE ve : vehiclesById.values()) {
                if (ve.m(me))
                    if (ve.type() != HELICOPTER && ve.type() != FIGHTER) minGSpeed = min(minGSpeed, ve.v.getMaxSpeed());
                    else minASpeed = min(minASpeed, ve.v.getMaxSpeed());
            }
        }
        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            VeE vehicle = vehiclesById.get(vu.getId());
            if (vehicle.m(me)) fvId.compute(vehicle.type(), (vt, old) -> old == null ? vehicle : world.getTickIndex() > old.tI ? vehicle : old);
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                if (vehicle.m(me)) {
                    vehicleUpdates.computeIfAbsent(vehicle.type(), t -> new IA(0)).inc();
                }
                vehiclesById.computeIfPresent(vu.getId(), (id, VeE) -> new VeE(VeE, vu, world.getTickIndex()));
            }
        }
        return vehicleUpdates;
    }


    Stream<VeE> mV() { return vehiclesById.values().stream().filter(v -> v.m(me)); }
    Stream<VeE> mVt(VehicleType vt) { return mV().filter(v -> v.type() == vt); }
    Rect OfV(Stream<VeE> vhs) { return vhs.reduce(new Rect(), Rect::update, Rect::combine); }
    Stream<VeE> gV(int id) { return mV().filter(vt -> vt.inG(id)); }
    Stream<VeE> eV() { return vehiclesById.values().stream().filter(v -> v.e(me)); }

    Rect[] sOfVG(int... ids) {
        Rect[] initial = new Rect[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect();initial[i].g = ids[i]; }
        return mV().reduce(initial, (rects, VeEx) -> {
            for (int i = 0; i < ids.length; i++) if (VeEx.inG(ids[i])) rects[i].update(VeEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rect[] sOfVT(VehicleType... types) {
        Rect[] initial = new Rect[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect(); }
        return mV().reduce(initial, (rects, VeEx) -> {
            for (int i = 0; i < types.length; i++) if (VeEx.type() == types[i]) rects[i].update(VeEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rect OfVG(int... ids) {
        return mV().reduce(new Rect(), (rect, VeEx) -> {
            for (int id : ids) if (VeEx.inG(id)) rect.update(VeEx);
            return rect;
        }, Rect::combine);
    }

    public final static int GT = 1;
    public final static int GA = 2;
    public final static int GH = 3;
    public final static int GI = 4;
    public final static int GF = 5;
    public final static int GG = 10;

    public int GFH = 0;
    public final static int GA2 = 7;
    public final static int GG1 = 8;
    public final static int GG2 = 9;

    private Random random;
    public enum GS {I, G, GG, M, R}
    public enum GGS { I, INX, INY, NX, NY, F }
}

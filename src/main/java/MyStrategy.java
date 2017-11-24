import model.*;

import java.util.*;
import java.util.stream.Stream;

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
    int[] pG = new int[3];
    VehicleType[] pT = new VehicleType[3];
    Deque<MB> gm1 = new LinkedList<>();
    Deque<MB> gm2 = new LinkedList<>();
    Map<VehicleType, Long> fvId = new HashMap<>();
    Map<Integer, P2D> vGd = new HashMap<>();
    static final Boolean DEBUG = false;
    P2D nuclearStrikePoint;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        Map<VehicleType, IA> vu = initV(world);
        if (me.getRemainingActionCooldownTicks() > 0) return;
        if (random == null) random = new Random(game.getRandomSeed());
        Player[] players = world.getPlayers();
        Player enemy = null;
        for (int i = 0; i < players.length; i++) if (players[i].getId() != me.getId()) enemy = players[i];

        MB nextMove = moves.poll();
        if (nextMove == null) {
            switch (gameState) {
                case I:
                    for (VeE veE : vehiclesById.values()) {
                        if (veE.m(me)) {
                            Long id = fvId.get(veE.type());
                            VeE ve ;
                            if (id == null) ve = veE;
                            else ve = vehiclesById.get(id);
                            ve = P2D.closedTo(ve, veE, P2D.Z);
                            fvId.put(ve.type(), ve.id());
                        }
                    }

                    Rect[] rects = sOfVT(TYPES);
                    for (int i = 0; i < rects.length; i++) {
                        ctG(GROUPS[i], TYPES[i], rects[i]);
                    }
                    pT = new VehicleType[]{TANK, ARRV, IFV};
                    pG = new int[]{GT, GA, GI};
                    gameState = GS.A;
                    break;
                case A:
                    if (sum(vu, TYPES).zero()) {
                        rects = sOfVT(TYPES);
                        for (int i = 0; i < rects.length; i++) {
                            sG(GROUPS[i], 1.6, rects[i].l, rects[i].t);
                        }
                        gameState = GS.G;
                    }
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
                    Rect[] groupRects = sOfVG(pG);
                    if (sum(vu, pT[1], pT[0]).zero()) {
                        MB mb = gm1.pollLast();
                        if (mb != null) {
                            moves.add(mb);
                            moves.add(gm1.pollLast());
                        }
                        else if (ggs[2].ordinal() <= GGS.INX.ordinal() || ggs[2] == GGS.F)
                                at = gatherAG(5, ggs, 1, ggsI[0], groupRects[1], groupRects[0], groupRects[2]);
                    }
                    if (sum(vu, pT[2], pT[0]).zero()) {
                        MB mb = gm2.pollLast();
                        if (mb != null) {
                            moves.add(mb);
                            moves.add(gm2.pollLast());
                        }
                        else if (ggs[1].ordinal() <= GGS.INX.ordinal() || ggs[1] == GGS.F) {
                            it = gatherAG( 5, ggs, 2, ggsI[1], groupRects[2], groupRects[0], groupRects[1]);
                        }
                    }
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) fh = gatherFH(vu);
                    if (fh && at && it) {
                        moves.add(new MB(CLEAR_AND_SELECT).setRect(new Rect(world)));
                        moves.add(new MB(ASSIGN).group(GG));
                        gameState = GS.M;
                    }
                    break;
                case FHNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0) {
                        Rect rect = OfV(gV(GFH));
                        if (rect.include(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY())) {
                            nuclearStrikePoint = new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            sG(GFH, 10, enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            gameState = GS.FHBNSD;
                            applyNextCommand(move);
                        } else gameState = GS.ENSF;
                    }
                    break;
                case NSD:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0) {
                        Rect rect = OfV(gV(GG));
                        if (rect.include(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY())) {
                            nuclearStrikePoint = new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            sG(GG, 10, enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            gameState = GS.BNSD;
                            applyNextCommand(move);
                        } else gameState = GS.M;
                        break;
                    }
                case M:
                    if (OfV(eV()).square()/(world.getHeight()*world.getWidth()) >= 0.5) {
                        Rect rect = OfVG(GG);
                        mkGM(GG, rect, rect.cX(), rect.cY(), 0, true);
                        sG(GFH, 1.5, rect.l, rect.t);
                        gameState = GS.FHFG;
                    } else {
                        if (enemy.getNextNuclearStrikeTickIndex() > 0) {
                            gameState = GS.NSD;
                            break;
                        }
                        if (!mkNS(GG)) {
                            VeE ep = cEP(GG);
                            if (ep == null) return;
                            if (!mkGM(GG, ep.x(), ep.y(), minGSpeed * tS(game, OfVG(GT, GA, GI), world) * 0.65)) {
                                if ((double) mV().filter(v -> v.attack(ep)).count() / vehiclesById.size() < 0.4) {
                                    Rect rect = OfV(gV(GG));
                                    sG(GG, 0.4, rect.l, rect.t);
                                    gameState = GS.FG;
                                }
                            }
                        } else gameState = GS.NSD;
                    }
                    break;
                case FG:
                    if (sum(vu, FIGHTER, HELICOPTER, TANK, IFV, ARRV).zero()) gameState = GS.M;
                    break;
                case FHFG:
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) gameState = GS.ENSF;
                    break;
                case ENSF:
                    if (OfV(eV()).square()/(world.getHeight()*world.getWidth()) < 0.3) {
                        Rect srs = OfVG(GFH);
                        Rect drs = OfVG(GG);
                        if (srs.dfct(drs) > U.EPS) {
                            mkGM(GFH, srs, drs.cX(), drs.cY(), minASpeed, true);
                        }
                        gameState = GS.FG;
                    } else if (enemy.getNextNuclearStrikeTickIndex() > 0) {
                        gameState = GS.FHNSD;
                    } else
                        if (!mkNS(GFH)) {
                            VeE ep = cEP(GFH);
                            if (ep == null) return;
                            if (!mkGM(GFH, ep.x(), ep.y(), minASpeed * aS(game, OfVG(GFH), world) * 0.65)) {
                                Rect rect = OfV(gV(GFH));
                                sG(GFH, 0.4, rect.l, rect.t);
                                gameState = GS.FHFG;
                            }
                        }
                    break;
                case BNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() < 0) {
                        sG(GG, 0.5, nuclearStrikePoint.x, nuclearStrikePoint.y);
                        applyNextCommand(move);
                        gameState = GS.FG;
                    }
                    break;
                case FHBNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() < 0) {
                        sG(GFH, 0.8, nuclearStrikePoint.x, nuclearStrikePoint.y);
                        applyNextCommand(move);
                        gameState = GS.FHFG;
                    }
                    break;
            }
        } else {
            nextMove.setMove(move);
        }
    }

    void applyNextCommand(Move move) {
        if (me.getRemainingActionCooldownTicks() > 0) return;
        MB nextMove = moves.poll();
        nextMove.setMove(move);
    }

    private boolean gatherFH(Map<VehicleType, IA> vu) {
        Rect[] rects = sOfVG(GF, GH, pG[0]);
        boolean fh = gatherAG(5, ggs, 0, GGS.INX, rects);
        Long id = fvId.get(pT[0]);
        VeE ve = vehiclesById.get(id);
        if (fh && ve.tI < world.getTickIndex()) {
            Rect srs = GFH == 0 ? OfVG(GF, GH) : OfVG(GFH);
            Rect drs = sum(vu, pT[0]).zero() ? OfVG(pG[0]) : cFlp(ve);
            if (GFH == 0) ctGFH(GFH = 6);
            if (srs.dfct(drs) > U.EPS) {
                mkGM(GFH, srs, drs.cX(), drs.cY(), minASpeed, true);
            }
        }
        return fh;
    }

    Rect cFlp(VeE lc) {
        return new Rect(lc.x() - 2.0, lc.y() - 2.0, lc.y() + 84.0, lc.x() + 84.0);
    }

    static double aS(Game game, Rect rs, World world) {
        int[] wij = rs.c().inWorld(world);
        return wtF(game, world.getWeatherByCellXY()[wij[0]][wij[0]]);
    }

    static double tS(Game game, Rect rs, World world) {
        int[] wij = rs.c().inWorld(world);
        return ttF(game, world.getTerrainByCellXY()[wij[0]][wij[0]]);
    }

    static double ttF(Game game, TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return game.getPlainTerrainSpeedFactor();
            case SWAMP:
                return game.getSwampTerrainSpeedFactor();
            case FOREST:
                return game.getForestTerrainSpeedFactor();
        }
        return 1;
    }

    static double wtF(Game game, WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return game.getClearWeatherSpeedFactor();
            case CLOUD:
                return game.getCloudWeatherSpeedFactor();
            case RAIN:
                return game.getRainWeatherSpeedFactor();
        }
        return 1;
    }

    static IA acc(Map<VehicleType, IA> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, IA.ZERO); }
    static IA sum(Map<VehicleType, IA> vu, VehicleType... vehicleType) {
        return new IA(Arrays.stream(vehicleType).map(vt -> acc(vu, vt)).mapToLong(IA::v).sum());
    }

    private boolean gatherAG(double scale, GGS[] ggs, int i, GGS bne, Rect... rects) {
        Rect rectA = rects[0]; Rect rectB = rects[1];
        boolean gd = ggs[i] == GGS.F || rects[0].dfct(rects[1]) <= scale;
        if (!gd) System.out.println(i + ":" + ggs[i]);
        switch (ggs[i]) {
            case I:
                if (rectA.cX() != rectB.cX() && rectA.cY() != rectB.cY()) ggs[i] = bne;
                else if (rectA.cX() != rectB.cX()) ggs[i] = GGS.INY;
                else if (rectA.cY() != rectB.cY()) ggs[i] = GGS.INX;
                break;
            case INX:
                if (rects[2].l > rectB.l) scale *= -1;
                if (U.eD(rectA.cX(), rectB.cX() + scale)) ggs[i] = GGS.NY;
                else mkGM(rectA.g, rectA, rectB.cX() + scale, rectA.cY());
                break;
            case INY:
                if (rects[2].t > rectB.t) scale *= -1;
                if (U.eD(rectA.cY(), rectB.cY() + scale)) ggs[i] = GGS.NX;
                else mkGM(rectA.g, rectA, rectA.cX(), rectB.cY() + scale);
                break;
            case NX:
                if (U.eD(rectA.cX(), rectB.cX())) ggs[i] = GGS.F;
                else mkGM(rectA.g, rectA, rectB.cX(), rectA.cY());
                break;
            case NY:
                if (U.eD(rectA.cY(), rectB.cY())) ggs[i] = GGS.F;
                else mkGM(rectA.g, rectA, rectA.cX(), rectB.cY());
                break;
        }
        return gd;
    }

    private VeE cEP(int... groups) {
        P2D tl = OfVG(groups).tl();
        VeE ctlev = null;
        for (VeE v : (Iterable<VeE>) eV()::iterator) {
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev,v, tl);
        }
        return ctlev;
    }

    boolean mkNS(int id) {
        if (me.getNextNuclearStrikeTickIndex() > 0 || me.getRemainingNuclearStrikeCooldownTicks() > 0) return false;
        P2D eRc = OfV(eV()).c();
        VeE cff = null;
        for (VeE fv : (Iterable<VeE>) mVt(FIGHTER)::iterator) {
            if (cff == null) cff = fv;
            else cff = P2D.closedTo(fv, cff, eRc);
        }
        VeE eVns = null;
        P2D pcff = new P2D(cff);
        for (VeE eV : (Iterable<VeE>) eV()::iterator) {
            if (cff.see(eV)) {
                if (eVns == null) eVns = eV;
                else eVns = P2D.futherTo(eVns, eV, pcff);
            }
        }
        if (eVns != null) {
            Rect rect = sOfVG(id)[0];
            mkGM(id, rect, rect.cX(), rect.cY(), 0, true);
            moves.add(new MB(TACTICAL_NUCLEAR_STRIKE).vehicleId(cff.id()).x(eVns.x()).y(eVns.y()));
            return true;
        }
        return false;
    }

    boolean mkGM(int id, double x, double y, double ms) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        Rect rect = OfV(gV(id));
        double dinstance = rect.dflt(x, y);
        if (dinstance < 0.5 || (rect.r - rect.l) + x >= world.getWidth() || (rect.b - rect.t) + y >= world.getHeight()) {
            moves.add(MB.c(MOVE).dfCToXY(rect, x, y).maxSpeed(ms));
            if (rect.dfct(x, y) < 0.5) return false;
        } else moves.add(MB.c(MOVE).dfLtToXY(rect, x, y).maxSpeed(ms));
        vGd.put(id, new P2D(x, y));
        return true;
    }

    void mkGM(int id, Rect gr, double x, double y) {
        mkGM(id, gr, x, y, 0, true);
    }

    void mkGM(int id, Rect groupRect, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(MOVE).dfCToXY(groupRect, x, y).maxSpeed(ms));
    }

    void ctG(int id, VehicleType vehicleType, Rect rect) {
        moves.add(new MB(CLEAR_AND_SELECT).vehicleType(vehicleType).setRect(new Rect(0,0,world.getHeight(),world.getWidth())));
        moves.add(MB.c(ASSIGN).group(id));
        MB mb = MB.c(MOVE);
        mb.x(20*((int)rect.l/74));
        mb.y(20*((int)rect.t/74));
        moves.add(mb);
    }

    void ctG(int id, VehicleType... vehicleType) {
        moves.add(new MB(CLEAR_AND_SELECT).vehicleType(vehicleType[0]).setRect(new Rect(0,0,world.getHeight(),world.getWidth())));
        for (int i = 1; i < vehicleType.length; i++) {
            moves.add(new MB(ADD_TO_SELECTION).vehicleType(vehicleType[i]).setRect(new Rect(0,0,world.getHeight(),world.getWidth())));
        }
        moves.add(MB.c(ASSIGN).group(id));
    }

    void sG(int id, double scale, double x, double y) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(SCALE).factor(scale).x(x).y(y));
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
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                if (vehicle.m(me) && vehicle.distanceTo(vu.getX(), vu.getY()) > U.EPS/10) {
                    vehicleUpdates.computeIfAbsent(vehicle.type(), t -> new IA(0)).inc();
                }
                vehiclesById.put(vu.getId(), new VeE(vehicle, vu, world.getTickIndex()));
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

    final static VehicleType[] TYPES = new VehicleType[]{TANK, ARRV, IFV, HELICOPTER, FIGHTER};
    public final static int GT = 1;
    public final static int GA = 2;
    public final static int GI = 3;
    public final static int GH = 4;
    public final static int GF = 5;
    public final static int GG = 10;
    final static int[] GROUPS = new int[]{GT, GA, GI, GH, GF};

    public int GFH = 0;

    private Random random;
    public enum GS {I, A, G, GG, M, NS, FG, NSD, BNSD, ENSF, FHFG, FHNSD, FHBNSD}
    public enum GGS { I, INX, INY, NX, NY, F }
}

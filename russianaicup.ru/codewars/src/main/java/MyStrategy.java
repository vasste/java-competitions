import model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Math.PI;
import static java.lang.StrictMath.min;
import static model.ActionType.*;
import static model.VehicleType.*;

public final class MyStrategy implements Strategy {

    GS gameState = GS.I;
    GGS[] ggs = new GGS[]{GGS.I, GGS.I, GGS.I};
    GGS[] ggsI = new GGS[]{GGS.I, GGS.I};
    double minGSpeed = 0;
    double minASpeed = 0;
    Player me;
    World world;
    Game game;
    WeatherType[][] weatherTypes;
    TerrainType[][] terrainTypes;
    final Map<Long, VehicleTick> mVById = new HashMap<>();
    final Map<Long, VehicleTick> eVById = new HashMap<>();
    final Map<VehicleType, IA> vehicles = new HashMap<>();
    Queue<MB> moves = new LinkedList<>();
    int[] pG = new int[3];
    VehicleType[] pT = new VehicleType[3];
    Deque<MB> gm1 = new LinkedList<>();
    Deque<MB> gm2 = new LinkedList<>();
    Map<VehicleType, Long> fvId = new HashMap<>();
    Map<Integer, P2D> vGd = new HashMap<>();
    P2D nuclearStrikePoint;
    P2D nuclearStrikeGroupPoint;
    boolean noArial = false;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        this.game = game;
        Map<VehicleType, IA> vu = initVehicles(world);
        if (random == null) random = new Random(game.getRandomSeed());
        Player[] players = world.getPlayers();
        Player enemy = null;
        for (int i = 0; i < players.length; i++) if (players[i].getId() != me.getId()) enemy = players[i];
        if (me.getRemainingActionCooldownTicks() > 0) return;
        weatherTypes = world.getWeatherByCellXY();
        terrainTypes = world.getTerrainByCellXY();

        MB nextMove = moves.poll();
        if (nextMove == null) {
            double wordedSquare = world.getHeight()*world.getWidth();
            switch (gameState) {
                case I:
                    for (VehicleTick vehicleTick : mVById.values()) {
                        if (vehicleTick.m(me)) {
                            Long id = fvId.get(vehicleTick.type());
                            VehicleTick ve ;
                            if (id == null) ve = vehicleTick;
                            else ve = mVById.get(id);
                            ve = P2D.closedTo(ve, vehicleTick, P2D.Z);
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
                            nuclearStrikeGroupPoint = rect.c();
                            sG(GFH, 10, enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            gameState = GS.FHBNSD;
                            applyNextCommand(move);
                        } else gameState = GS.ENSF;
                    } else gameState = GS.ENSF;
                    break;
                case M:
                    Rect[] evR = sOfVT(eV(), FIGHTER, HELICOPTER, TANK);
                    if ((evR[0].square()/wordedSquare >= 0.5 || evR[1].square()/wordedSquare >= 0.5) && evR[2].square()/wordedSquare < 0.1) {
                        Rect rect = OfVG(GG);
                        mkGM(GG, rect, rect.cX(), rect.cY(), 0, true);
                        Rect rectFH = sOfVG(GFH)[0];
                        sG(GFH, 1.4, rectFH.l, rectFH.t);
                        gameState = GS.FHFG;
                    } else {
                        if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GG, move, enemy, GS.BNSD)) return;
                        if (!mkNS(GG)) {
                            VehicleTick ep = cEP(false, GG);
                            if (ep == null) return;
                            if (mV().filter(v -> v.attack(ep)).count() < 5) {
                                if (!mkGM(GG, ep.x(), ep.y(), minGSpeed * tS(game, OfVG(GT, GA, GI), world, terrainTypes) * 0.65,
                                        sum(vu, FIGHTER, HELICOPTER, TANK, IFV, ARRV).value)) {
                                    gameState = GS.FG;
                                }
                            }
                        }
                    }
                    break;
                case FG:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GG, move, enemy, GS.BNSD)) return;
                    if (sum(vu, FIGHTER, HELICOPTER, TANK, IFV, ARRV).zero()) gameState = GS.M;
                    break;
                case FHFG:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GFH, move, enemy, GS.FHNSD)) return;
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) gameState = GS.ENSF;
                    break;
                case ENSF:
                    Rect rect = OfVG(GFH);
                    if (rect.isRectNaN()) {
                        noArial = true;
                        gameState = GS.M;
                        return;
                    }
                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GFH, move, enemy, GS.FHNSD)) return;
                    evR = sOfVT(eV(), FIGHTER, HELICOPTER);
                    if (evR[0].square()/wordedSquare < 0.15 && evR[1].square()/wordedSquare < 0.15) {
                        Rect drs = OfVG(GG);
                        if (rect.dfct(drs) > U.EPS) {
                            mkGM(GFH, rect, drs.cX(), drs.cY(), minASpeed, true);
                            gameState = GS.FHFG;
                        } else {
                            gameState = GS.FG;
                        }
                    } else
                        if (!mkNS(GFH)) {
                            VehicleTick ep = cEP(true, GFH);
                            if (ep == null) return;
                            if (mVt(FIGHTER, HELICOPTER).filter(v -> v.attack(ep)).count() < 5) {
                                mkGM(GFH, ep.x(), ep.y(), minASpeed * wS(game, rect, world, weatherTypes) * 0.75, sum(vu, FIGHTER, HELICOPTER).v());
                            }
                        }
                    break;
                case BNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() < 0) {
                        P2D center = OfVG(GG).c();
                        sG(GG, 0.2, center.x + (nuclearStrikePoint.x - nuclearStrikeGroupPoint.x),
                                center.y + (nuclearStrikePoint.y - nuclearStrikeGroupPoint.y));
                        applyNextCommand(move);
                        gameState = GS.FG;
                    }
                    break;
                case FHBNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() < 0) {
                        P2D center = OfVG(GFH).c();
                        sG(GFH, 0.2, center.x + (nuclearStrikePoint.x - nuclearStrikeGroupPoint.x),
                                center.y + (nuclearStrikePoint.y - nuclearStrikeGroupPoint.y));
                        applyNextCommand(move);
                        gameState = GS.FHFG;
                    }
                    break;
            }
        } else {
            nextMove.setMove(move);
        }
    }

    private boolean nuclearStrikeDetected(int id, Move move, Player enemy, GS nextState) {
        Rect rect = OfV(gV(id));
        if (rect.include(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY())) {
            nuclearStrikePoint = new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
            nuclearStrikeGroupPoint = rect.c();
            sG(id, 10, enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
            gameState = nextState;
            applyNextCommand(move);
            return true;
        }
        return false;
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
        VehicleTick ve = mVById.get(id);
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

    Rect cFlp(VehicleTick lc) {
        return new Rect(lc.x() - 2.0, lc.y() - 2.0, lc.y() + 84.0, lc.x() + 84.0);
    }
    static double wS(Game game, Rect rs, World world, WeatherType[][] weatherTypes) {
        int[] wij = rs.c().inWorld(world);
        return wsF(game, weatherTypes[wij[0]][wij[0]]);
    }

    static double wV(Game game, P2D vehicle, World world, WeatherType[][] weatherTypes) {
        int[] wij = vehicle.inWorld(world);
        return wvF(game, weatherTypes[wij[0]][wij[0]]);
    }

    static double tS(Game game, Rect rs, World world, TerrainType[][] terrainTypes) {
        int[] wij = rs.c().inWorld(world);
        return ttF(game, terrainTypes[wij[0]][wij[0]]);
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

    static double wsF(Game game, WeatherType weatherType) {
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

    static double wvF(Game game, WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return game.getClearWeatherVisionFactor();
            case CLOUD:
                return game.getCloudWeatherVisionFactor();
            case RAIN:
                return game.getRainWeatherVisionFactor();
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

    private VehicleTick cEP(boolean arial, int... groups) {
        P2D center = OfVG(groups).c();
        VehicleTick ctlev = null;
        for (VehicleTick v : (Iterable<VehicleTick>) eV()::iterator) {
            if (arial && !v.v.isAerial()) continue;
            if (ctlev == null) ctlev = v;
            else ctlev = P2D.closedTo(ctlev,v, center);
        }
        return ctlev;
    }

    boolean mkNS(int id) {
        if (me.getNextNuclearStrikeTickIndex() > 0 || me.getRemainingNuclearStrikeCooldownTicks() > 0) return false;
        P2D eRc = OfV(eV()).c();
        VehicleTick cff = null;
        for (VehicleTick fv : (Iterable<VehicleTick>) mVt(FIGHTER)::iterator) {
            if (cff == null) cff = fv;
            else cff = P2D.closedTo(fv, cff, eRc);
        }
        VehicleTick eVns = null;
        P2D pcff = new P2D(cff);
        for (VehicleTick eV : (Iterable<VehicleTick>) eV()::iterator) {
            if (cff.see(eV, wV(game, pcff, world, weatherTypes))) {
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

    boolean mkGM(int id, double x, double y, double ms, long updates) {
        P2D destination = vGd.get(id);
        if (destination != null && destination.compareTo(new P2D(x, y)) == 0 && updates > 0)
            return true;
        Rect rect = OfV(gV(id));
        if (rect.include(x, y)) {
            vGd.remove(id);
            if (rect.square() > 15_000) sG(id, 0.9, rect.cX(), rect.cY());
            else {
                P2D center = new P2D(x, y);
                VehicleTick ctlev = null;
                for (VehicleTick v : (Iterable<VehicleTick>) mV()::iterator) {
                    if (ctlev == null) ctlev = v;
                    else ctlev = P2D.closedTo(ctlev, v, center);
                }
                if (ctlev == null || U.eD(x, ctlev.x()) && U.eD(y, ctlev.y())) rG(id, PI / 4, rect.cX(), rect.cY());
                else {
                    moves.add(new MB(CLEAR_AND_SELECT).group(id));
                    moves.add(MB.c(MOVE).x(x - ctlev.x()).y(y - ctlev.y()).maxSpeed(ms));
                    vGd.put(id, new P2D(x, y, world.getTickIndex()));
                }
            }
            return false;
        } else {
            moves.add(new MB(CLEAR_AND_SELECT).group(id));
            if ((rect.r - rect.l)/2 + x >= world.getWidth()) x = world.getWidth() - (rect.r - rect.l)/2;
            if ((rect.b - rect.t)/2 + y >= world.getHeight()) y = world.getHeight() - (rect.b - rect.t)/2;
            moves.add(MB.c(MOVE).dfCToXY(rect, x, y).maxSpeed(ms));
            vGd.put(id, new P2D(x, y, world.getTickIndex()));
            return true;
        }
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

    void rG(int id, double angle, double x, double y) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(ROTATE).angle(angle).x(x).y(y));
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

    Map<VehicleType, IA> initVehicles(World world) {
        Map<VehicleType, IA> vehicleUpdates = new HashMap<>();
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicles.computeIfAbsent(vehicle.getType(), t -> new IA()).inc();
            if (vehicle.getPlayerId() == me.getId()) {
                mVById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex()));
            } else {
                eVById.put(vehicle.getId(), new VehicleTick(vehicle, world.getTickIndex()));
            }
        }

        if (minGSpeed == 0) {
            minGSpeed = Double.MAX_VALUE;
            minASpeed = Double.MAX_VALUE;
            for (VehicleTick ve : mVById.values()) {
                if (ve.type() != HELICOPTER && ve.type() != FIGHTER) minGSpeed = min(minGSpeed, ve.v.getMaxSpeed());
                else minASpeed = min(minASpeed, ve.v.getMaxSpeed());
            }
        }

        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            if (vu.getDurability() == 0) {
                VehicleTick vt = mVById.remove(vu.getId());
                if (vt != null) vehicles.get(vt.type()).dec();
                eVById.remove(vu.getId());
            } else {
                VehicleTick oV = mVById.get(vu.getId());
                VehicleTick nV = mVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex()));
                if (oV != null && oV.getDistanceTo(nV) > U.EPS/10) {
                    vehicleUpdates.computeIfAbsent(oV.type(), t -> new IA(0)).inc();
                }
                eVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex()));
            }
        }
        return vehicleUpdates;
    }

    Stream<VehicleTick> mV() { return mVById.values().stream(); }
    Stream<VehicleTick> mVt(VehicleType... vts) { return mV().filter(v -> Arrays.stream(vts).anyMatch(vt -> v.type() == vt)); }
    Rect OfV(Stream<VehicleTick> vhs) { return vhs.reduce(new Rect(), Rect::update, Rect::combine); }
    Stream<VehicleTick> gV(int id) { return mV().filter(vt -> vt.inG(id)); }
    Stream<VehicleTick> eV() { return eVById.values().stream(); }

    Rect[] sOfVG(int... ids) {
        Rect[] initial = new Rect[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect();initial[i].g = ids[i]; }
        return mV().reduce(initial, (rects, veEx) -> {
            for (int i = 0; i < ids.length; i++) if (veEx.inG(ids[i])) rects[i].update(veEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rect[] sOfVT(VehicleType... types) { return sOfVT(mV(), types); }
    Rect[] sOfVT(Stream<VehicleTick> stream, VehicleType... types) {
        Rect[] initial = new Rect[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rect(); }
        return stream.reduce(initial, (rects, veEx) -> {
            for (int i = 0; i < types.length; i++) if (veEx.type() == types[i]) rects[i].update(veEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rect OfVG(int... ids) {
        return mV().reduce(new Rect(), (rect, veEx) -> {
            for (int id : ids) if (veEx.inG(id)) rect.update(veEx);
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
    public enum GS {I, A, G, GG, M, FG, BNSD, ENSF, FHFG, FHNSD, FHBNSD}
    public enum GGS { I, INX, INY, NX, NY, F }
    static final Boolean DEBUG = false;
}

import model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Math.PI;
import static java.lang.StrictMath.min;
import static model.ActionType.*;
import static model.VehicleType.*;

public final class MyStrategy implements Strategy {

    GameState gameState = GameState.ORDER_CREATION;
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
    final Map<VehicleType, Accumulator> vehicles = new HashMap<>();
    Queue<MoveBuilder> moves = new LinkedList<>();
    Map<VehicleType, Long> fvId = new HashMap<>();
    Map<Integer, P2D> vGd = new HashMap<>();
    P2D nuclearStrikePoint;
    P2D nuclearStrikeGroupPoint;
    boolean noArial = false;
    VehicleType centralType;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        this.game = game;
        Map<VehicleType, Accumulator> vu = initVehicles(world);
        if (random == null) random = new Random(game.getRandomSeed());
        Player[] players = world.getPlayers();
        Player enemy = null;
        for (int i = 0; i < players.length; i++) if (players[i].getId() != me.getId()) enemy = players[i];
        if (me.getRemainingActionCooldownTicks() > 0) return;
        weatherTypes = world.getWeatherByCellXY();
        terrainTypes = world.getTerrainByCellXY();

        MoveBuilder nextMove = moves.poll();
        if (nextMove == null) {
            double wordedSquare = world.getHeight()*world.getWidth();
            switch (gameState) {
                case ORDER_CREATION:
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
                    int[] ait = new int[]{GA,GI,GT};
                    VehicleType[] vts = new VehicleType[]{ARRV, IFV, TANK};
                    Rectangle[] rs = sOfVT(vts);
                    for (int i = 0; i < rs.length; i++) {
                        rs[i].g = ait[i];
                        rs[i].commands = new ArrayDeque<>();
                    }
                    Rectangle[] ars = sOfVT(FIGHTER, HELICOPTER);
                    OrderGraph.setupGroupingMoves(rs, world);
                    for (int i = 0; i < rs.length; i++) {
                        while (!rs[i].commands.isEmpty()) {
                            MoveBuilder moveBuilder = rs[i].commands.pollLast();
                            if (moveBuilder != null) {
                                moves.add(moveBuilder);
                            }
                        }
                    }
                    scaleVehicles(FIGHTER, 1.6, ars[0].l, ars[0].t, ars[0]);
                    scaleVehicles(HELICOPTER, 1.6, ars[1].l, ars[1].t, ars[1]);
                    gameState = GameState.ORDER_POSITION;
                    break;
                case ORDER_POSITION:
                    if (sum(vu, ARRV, TANK, IFV).zero()) {
                        vts = new VehicleType[]{ARRV, IFV, TANK};
                        rs = sOfVT(ARRV, IFV, TANK);
                        Arrays.stream(rs);
                        centralType = rs[1].vt;
                        for (int i = 0; i < rs.length; i++) {
                           positionGroup(vts[i], rs[i]);
                        }
                        gameState = GameState.ORDER_GROUPING;
                    }
                    break;
                case ORDER_GROUPING:
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) gatherFH(vu, centralType);
                    if (sum(vu, ARRV, TANK, IFV).zero()) {
                        vts = new VehicleType[]{ARRV, IFV, TANK};
                        rs = sOfVT(vts);
                        for (int i = 0; i < vts.length; i++) {
                        //    scaleVehicles(vts[i], 2, rs[i].l, rs[i].t, null);
                        }
                        gameState = GameState.END;
                    }
                    break;
//                case GG:
//                    boolean fh = false, at = false, it = false;
//                    Rectangle[] groupRectangles = sOfVG(pG);
//                    if (sum(vu, pT[1], pT[0]).zero()) {
//                        MoveBuilder moveBuilder = tankGroupingMoves.pollLast();
//                        if (moveBuilder != null) {
//                            moves.add(moveBuilder);
//                            moves.add(tankGroupingMoves.pollLast());
//                        }
//                        else if (ggs[2].ordinal() <= GGS.INX.ordinal() || ggs[2] == GGS.F)
//                                at = gatherAG(5, ggs, 1, ggsI[0], groupRectangles[1], groupRectangles[0], groupRectangles[2]);
//                    }
//                    if (sum(vu, pT[2], pT[0]).zero()) {
//                        MoveBuilder moveBuilder = gm2.pollLast();
//                        if (moveBuilder != null) {
//                            moves.add(moveBuilder);
//                            moves.add(gm2.pollLast());
//                        }
//                        else if (ggs[1].ordinal() <= GGS.INX.ordinal() || ggs[1] == GGS.F) {
//                            it = gatherAG( 5, ggs, 2, ggsI[1], groupRectangles[2], groupRectangles[0], groupRectangles[1]);
//                        }
//                    }
//                    if (sum(vu, FIGHTER, HELICOPTER).zero()) fh = gatherFH(vu);
//                    if (fh && at && it) {
//                        moves.add(new MoveBuilder(CLEAR_AND_SELECT).setRect(new Rectangle(world)));
//                        moves.add(new MoveBuilder(ASSIGN).group(GG));
//                        gameState = GameState.M;
//                    }
//                    break;
                case FHNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0) {
                        Rectangle rectangle = OfV(gV(GFH));
                        if (rectangle.include(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY())) {
                            nuclearStrikePoint = new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            nuclearStrikeGroupPoint = rectangle.c();
                            sG(GFH, 10, enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
                            gameState = GameState.FHBNSD;
                            applyNextCommand(move);
                        } else gameState = GameState.ENSF;
                    } else gameState = GameState.ENSF;
                    break;
                case M:
                    Rectangle[] evR = sOfVT(eV(), FIGHTER, HELICOPTER, TANK);
                    if ((evR[0].square()/wordedSquare >= 0.5 || evR[1].square()/wordedSquare >= 0.5) && evR[2].square()/wordedSquare < 0.1) {
                        Rectangle rectangle = OfVG(GG);
                        mkGM(GG, rectangle, rectangle.cX(), rectangle.cY(), 0, true);
                        Rectangle rectangleFH = sOfVG(GFH)[0];
                        sG(GFH, 1.4, rectangleFH.l, rectangleFH.t);
                        gameState = GameState.FHFG;
                    } else {
                        if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GG, move, enemy, GameState.BNSD)) return;
                        if (!mkNS(GG)) {
                            VehicleTick ep = cEP(false, GG);
                            if (ep == null) return;
                            if (mV().filter(v -> v.attack(ep)).count() < 5) {
                                if (!mkGM(GG, ep.x(), ep.y(), minGSpeed * tS(game, OfVG(GT, GA, GI), world, terrainTypes) * 0.65,
                                        sum(vu, FIGHTER, HELICOPTER, TANK, IFV, ARRV).value)) {
                                    gameState = GameState.FG;
                                }
                            }
                        }
                    }
                    break;
                case FG:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GG, move, enemy, GameState.BNSD)) return;
                    if (sum(vu, FIGHTER, HELICOPTER, TANK, IFV, ARRV).zero()) gameState = GameState.M;
                    break;
                case FHFG:
                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GFH, move, enemy, GameState.FHNSD)) return;
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) gameState = GameState.ENSF;
                    break;
                case ENSF:
                    Rectangle rectangle = OfVG(GFH);
                    if (rectangle.isRectNaN()) {
                        noArial = true;
                        gameState = GameState.M;
                        return;
                    }
                    if (enemy.getNextNuclearStrikeTickIndex() > 0 && nuclearStrikeDetected(GFH, move, enemy, GameState.FHNSD)) return;
                    evR = sOfVT(eV(), FIGHTER, HELICOPTER);
                    if (evR[0].square()/wordedSquare < 0.15 && evR[1].square()/wordedSquare < 0.15) {
                        Rectangle drs = OfVG(GG);
                        if (rectangle.dfct(drs) > U.EPS) {
                            mkGM(GFH, rectangle, drs.cX(), drs.cY(), minASpeed, true);
                            gameState = GameState.FHFG;
                        } else {
                            gameState = GameState.FG;
                        }
                    } else
                        if (!mkNS(GFH)) {
                            VehicleTick ep = cEP(true, GFH);
                            if (ep == null) return;
                            if (mVt(FIGHTER, HELICOPTER).filter(v -> v.attack(ep)).count() < 5) {
                                mkGM(GFH, ep.x(), ep.y(), minASpeed * wS(game, rectangle, world, weatherTypes) * 0.75, sum(vu, FIGHTER, HELICOPTER).v());
                            }
                        }
                    break;
                case BNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() < 0) {
                        P2D center = OfVG(GG).c();
                        sG(GG, 0.2, center.x + (nuclearStrikePoint.x - nuclearStrikeGroupPoint.x),
                                center.y + (nuclearStrikePoint.y - nuclearStrikeGroupPoint.y));
                        applyNextCommand(move);
                        gameState = GameState.FG;
                    }
                    break;
                case FHBNSD:
                    if (enemy.getNextNuclearStrikeTickIndex() < 0) {
                        P2D center = OfVG(GFH).c();
                        sG(GFH, 0.2, center.x + (nuclearStrikePoint.x - nuclearStrikeGroupPoint.x),
                                center.y + (nuclearStrikePoint.y - nuclearStrikeGroupPoint.y));
                        applyNextCommand(move);
                        gameState = GameState.FHFG;
                    }
                    break;
            }
        } else {
            nextMove.setMove(move);
        }
    }

    private boolean nuclearStrikeDetected(int id, Move move, Player enemy, GameState nextState) {
        Rectangle rectangle = OfV(gV(id));
        if (rectangle.include(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY())) {
            nuclearStrikePoint = new P2D(enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
            nuclearStrikeGroupPoint = rectangle.c();
            sG(id, 10, enemy.getNextNuclearStrikeX(), enemy.getNextNuclearStrikeY());
            gameState = nextState;
            applyNextCommand(move);
            return true;
        }
        return false;
    }

    void applyNextCommand(Move move) {
        if (me.getRemainingActionCooldownTicks() > 0) return;
        MoveBuilder nextMove = moves.poll();
        nextMove.setMove(move);
    }

    private boolean gatherFH(Map<VehicleType, Accumulator> vu, VehicleType cT) {
        Rectangle[] rectangles = sOfVT(FIGHTER, HELICOPTER, cT);
        boolean fh = gatherAG(5, ggs, 0, GGS.INX, rectangles);
        Long id = fvId.get(cT);
        VehicleTick ve = mVById.get(id);
        if (fh && ve.tI < world.getTickIndex()) {
            Rectangle srs = GFH == 0 ? OfVG(GF, GH) : OfVG(GFH);
            Rectangle drs = sum(vu, cT).zero() ? OfVG(cT) : cFlp(ve);
            if (GFH == 0) ctGFH(GFH = 6);
            if (srs.dfct(drs) > U.EPS) {
                mkGM(GFH, srs, drs.cX(), drs.cY(), minASpeed, true);
            }
        }
        return fh;
    }

    Rectangle cFlp(VehicleTick lc) {
        return new Rectangle(lc.x() - 2.0, lc.y() - 2.0, lc.y() + 84.0, lc.x() + 84.0);
    }
    static double wS(Game game, Rectangle rs, World world, WeatherType[][] weatherTypes) {
        int[] wij = rs.c().inWorld(world);
        return wsF(game, weatherTypes[wij[0]][wij[0]]);
    }

    static double wV(Game game, P2D vehicle, World world, WeatherType[][] weatherTypes) {
        int[] wij = vehicle.inWorld(world);
        return wvF(game, weatherTypes[wij[0]][wij[0]]);
    }

    static double tS(Game game, Rectangle rs, World world, TerrainType[][] terrainTypes) {
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

    static Accumulator acc(Map<VehicleType, Accumulator> vu, VehicleType vehicleType) { return vu.getOrDefault(vehicleType, Accumulator.ZERO); }
    static Accumulator sum(Map<VehicleType, Accumulator> vu, VehicleType... vehicleType) {
        return new Accumulator(Arrays.stream(vehicleType).map(vt -> acc(vu, vt)).mapToLong(Accumulator::v).sum());
    }

    private boolean gatherAG(double scale, GGS[] ggs, int i, GGS bne, Rectangle... rectangles) {
        Rectangle rectangleA = rectangles[0]; Rectangle rectangleB = rectangles[1];
        boolean gd = ggs[i] == GGS.F || rectangles[0].dfct(rectangles[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rectangleA.cX() != rectangleB.cX() && rectangleA.cY() != rectangleB.cY()) ggs[i] = bne;
                else if (rectangleA.cX() != rectangleB.cX()) ggs[i] = GGS.INY;
                else if (rectangleA.cY() != rectangleB.cY()) ggs[i] = GGS.INX;
                break;
            case INX:
                if (rectangles[2].l > rectangleB.l) scale *= -1;
                if (U.eD(rectangleA.cX(), rectangleB.cX() + scale)) ggs[i] = GGS.NY;
                else mkGM(rectangleA.g, rectangleA, rectangleB.cX() + scale, rectangleA.cY());
                break;
            case INY:
                if (rectangles[2].t > rectangleB.t) scale *= -1;
                if (U.eD(rectangleA.cY(), rectangleB.cY() + scale)) ggs[i] = GGS.NX;
                else mkGM(rectangleA.g, rectangleA, rectangleA.cX(), rectangleB.cY() + scale);
                break;
            case NX:
                if (U.eD(rectangleA.cX(), rectangleB.cX())) ggs[i] = GGS.F;
                else mkGM(rectangleA.g, rectangleA, rectangleB.cX(), rectangleA.cY());
                break;
            case NY:
                if (U.eD(rectangleA.cY(), rectangleB.cY())) ggs[i] = GGS.F;
                else mkGM(rectangleA.g, rectangleA, rectangleA.cX(), rectangleB.cY());
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
            Rectangle rectangle = sOfVG(id)[0];
            mkGM(id, rectangle, rectangle.cX(), rectangle.cY(), 0, true);
            moves.add(new MoveBuilder(TACTICAL_NUCLEAR_STRIKE).vehicleId(cff.id()).x(eVns.x()).y(eVns.y()));
            return true;
        }
        return false;
    }

    boolean mkGM(int id, double x, double y, double ms, long updates) {
        P2D destination = vGd.get(id);
        if (destination != null && destination.compareTo(new P2D(x, y)) == 0 && updates > 0)
            return true;
        Rectangle rectangle = OfV(gV(id));
        if (rectangle.include(x, y)) {
            vGd.remove(id);
            if (rectangle.square() > 15_000) sG(id, 0.9, rectangle.cX(), rectangle.cY());
            else {
                P2D center = new P2D(x, y);
                VehicleTick ctlev = null;
                for (VehicleTick v : (Iterable<VehicleTick>) mV()::iterator) {
                    if (ctlev == null) ctlev = v;
                    else ctlev = P2D.closedTo(ctlev, v, center);
                }
                if (ctlev == null || U.eD(x, ctlev.x()) && U.eD(y, ctlev.y())) rG(id, PI / 4, rectangle.cX(), rectangle.cY());
                else {
                    moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
                    moves.add(MoveBuilder.c(MOVE).x(x - ctlev.x()).y(y - ctlev.y()).maxSpeed(ms));
                    vGd.put(id, new P2D(x, y, world.getTickIndex()));
                }
            }
            return false;
        } else {
            moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
            if ((rectangle.r - rectangle.l)/2 + x >= world.getWidth()) x = world.getWidth() - (rectangle.r - rectangle.l)/2;
            if ((rectangle.b - rectangle.t)/2 + y >= world.getHeight()) y = world.getHeight() - (rectangle.b - rectangle.t)/2;
            moves.add(MoveBuilder.c(MOVE).dfCToXY(rectangle, x, y).maxSpeed(ms));
            vGd.put(id, new P2D(x, y, world.getTickIndex()));
            return true;
        }
    }

    void mkGM(int id, Rectangle gr, double x, double y) {
        mkGM(id, gr, x, y, 0, true);
    }

    void mkGM(int id, Rectangle groupRectangle, double x, double y, double ms, boolean select) {
        if (select) moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(MOVE).dfCToXY(groupRectangle, x, y).maxSpeed(ms));
    }

    void positionGroup(VehicleType vehicleType, Rectangle rectangle) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(vehicleType).setRect(new Rectangle(0,0,world.getHeight(),world.getWidth())));
        moves.add(MoveBuilder.c(MOVE).x(40*((int) rectangle.l/74)).y(40*((int) rectangle.t/74)));
    }

    void rG(int id, double angle, double x, double y) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(ROTATE).angle(angle).x(x).y(y));
    }

    void scaleVehicles(VehicleType vehicleType, double scale, double x, double y, Rectangle rectangle) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(vehicleType).setRect(new Rectangle(0,0,world.getHeight(),world.getWidth())));
        if (rectangle != null)
            moves.add(MoveBuilder.c(MOVE).x(20*((int) rectangle.l/74)).y(20*((int) rectangle.t/74)));
        moves.add(MoveBuilder.c(SCALE).factor(scale).x(x).y(y));
    }

    void sG(int id, double scale, double x, double y) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).group(id));
        moves.add(MoveBuilder.c(SCALE).factor(scale).x(x).y(y));
    }

    void ctGFH(int id) {
        moves.add(new MoveBuilder(CLEAR_AND_SELECT).vehicleType(HELICOPTER).setRect(new Rectangle(world)));
        moves.add(new MoveBuilder(ADD_TO_SELECTION).vehicleType(FIGHTER).setRect(new Rectangle(world)));
        moves.add(MoveBuilder.c(ASSIGN).group(id));
    }

    Map<VehicleType, Accumulator> initVehicles(World world) {
        Map<VehicleType, Accumulator> vehicleUpdates = new HashMap<>();
        for (Vehicle vehicle : world.getNewVehicles()) {
            vehicles.computeIfAbsent(vehicle.getType(), t -> new Accumulator()).inc();
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
                    vehicleUpdates.computeIfAbsent(oV.type(), t -> new Accumulator(0)).inc();
                }
                eVById.computeIfPresent(vu.getId(), (key, vehicle) -> new VehicleTick(vehicle, vu, world.getTickIndex()));
            }
        }
        return vehicleUpdates;
    }

    Stream<VehicleTick> mV() { return mVById.values().stream(); }
    Stream<VehicleTick> mVt(VehicleType... vts) { return mV().filter(v -> Arrays.stream(vts).anyMatch(vt -> v.type() == vt)); }
    Rectangle OfV(Stream<VehicleTick> vhs) { return vhs.reduce(new Rectangle(), Rectangle::update, Rectangle::combine); }
    Stream<VehicleTick> gV(int id) { return mV().filter(vt -> vt.inG(id)); }
    Stream<VehicleTick> eV() { return eVById.values().stream(); }

    Rectangle[] sOfVG(int... ids) {
        Rectangle[] initial = new Rectangle[ids.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle();initial[i].g = ids[i]; }
        return mV().reduce(initial, (rects, veEx) -> {
            for (int i = 0; i < ids.length; i++) if (veEx.inG(ids[i])) rects[i].update(veEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rectangle[] sOfVT(VehicleType... types) { return sOfVT(mV(), types); }
    Rectangle[] sOfVT(Stream<VehicleTick> stream, VehicleType... types) {
        Rectangle[] initial = new Rectangle[types.length];
        for (int i = 0; i < initial.length; i++) { initial[i] = new Rectangle(types[i]); }
        return stream.reduce(initial, (rects, veEx) -> {
            for (int i = 0; i < types.length; i++) if (veEx.type() == types[i]) rects[i].update(veEx);return rects;
        }, (rl, rr) -> { for (int i = 0; i < rl.length; i++) rl[i] = rl[i].combine(rr[i]); return rl; });
    }

    Rectangle OfVG(int... ids) {
        return mV().reduce(new Rectangle(), (rect, veEx) -> {
            for (int id : ids) if (veEx.inG(id)) rect.update(veEx);
            return rect;
        }, Rectangle::combine);
    }

    Rectangle OfVG(VehicleType... types) {
        return mV().reduce(new Rectangle(), (rect, v) -> {
            for (int i = 0; i < types.length; i++) if (v.type() == types[i]) rect.update(v);
            return rect;
        }, Rectangle::combine);
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
    public enum GameState {ORDER_CREATION, ORDER_POSITION, ORDER_GROUPING, GG, M, FG, BNSD, ENSF, FHFG, FHNSD, FHBNSD, END}
    public enum GGS { I, INX, INY, NX, NY, F }
    static final Boolean DEBUG = true;
}

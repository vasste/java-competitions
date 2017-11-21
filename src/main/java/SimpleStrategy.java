import model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Double.isNaN;
import static java.lang.StrictMath.*;
import static model.ActionType.*;
import static model.VehicleType.*;

public final class SimpleStrategy implements Strategy {

    GS gameState = GS.I;
    GGS[] ggs = new GGS[]{GGS.I, GGS.I, GGS.I};
    double minGSpeed = 0;
    double minASpeed = 0;
    final Map<Long, VeEx> vehiclesById = new HashMap<>();
    Player me;
    World world;
    Queue<MB> moves = new LinkedList<>();
    double cdMs = 0;
    int[] pG = new int[3];
    VehicleType[] pT = new VehicleType[3];
    Map<VehicleType, P2D> tlP = new HashMap<>();
    Map<VehicleType, Boolean> tlPc = new HashMap<>();

    public SimpleStrategy() {
        for (VehicleType vehicleType : VehicleType.values()) {
            tlP.put(vehicleType, new P2D(Double.MAX_VALUE, Double.MAX_VALUE));
        }
    }

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        this.world = world;
        Map<VehicleType, IA> vu = initV(world);
        if (cdMs /world.getTickIndex() >= 0.2) return;
        if (random == null) random = new Random(game.getRandomSeed());

        MB nextMove = moves.poll();
        if (nextMove == null) {
            switch (gameState) {
                case I:
                    ctG(GT, TANK, 1.6);
                    ctG(GA, ARRV, 1.6);
                    ctG(GI, IFV, 1.6);
                    ctG(GH, HELICOPTER, 1.4);
                    ctG(GF, FIGHTER, 1.4);
                    VehicleType[] vs = new VehicleType[]{TANK, ARRV, IFV};
                    int[] groups = new int[]{GT, GA, GI};
                    Rect gr[] = sOfVT(TANK, ARRV, IFV);
                    for (int i = 0; i < gr.length; i++) { gr[i].g = groups[i]; gr[i].vt = vs[i]; }
                    Arrays.sort(gr);
                    for (int i = 0; i < gr.length; i++) { pG[i] = gr[i].g;pT[i] = gr[i].vt; }
                    gameState = GS.G;
                    break;
                case G:
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) gatherAG(GF, GH, 4, ggs, 0, GGS.INX);
                    if (sum(vu, ARRV, TANK, IFV).zero()) {
                        setupGroupingMoves();
                        gameState = GS.GG;
                    }
                    break;
                case GG:
                    boolean fh = false, at = false, it = false;
                    if (sum(vu, pT[0], pT[1]).zero()) {
                        MB mb = gm0.pollLast();
                        if (mb != null) {
                            moves.add(mb);
                            moves.add(gm0.pollLast());
                        }
                        else
                            if (ggs[2].ordinal() <= GGS.INX.ordinal() || ggs[2] == GGS.F) at = gatherAG(pG[0], pG[1], 5, ggs, 1, null);
                    }
                    if (sum(vu, pT[2], pT[1]).zero()) {
                        MB mb = gm2.pollLast();
                        if (mb != null) {
                            moves.add(mb);
                            moves.add(gm2.pollLast());
                        }
                        else
                            if (ggs[1].ordinal() <= GGS.INX.ordinal() || ggs[1] == GGS.F) it = gatherAG(pG[2], pG[1], 5, ggs, 2, null);
                    }
                    if (sum(vu, FIGHTER, HELICOPTER).zero()) {
                        fh = gatherAG(GF, GH, 4, ggs, 0, GGS.INX);
                        if (fh && !tlPc.getOrDefault(pT[1], true)) {
                            Rect srs = GFH == 0 ? OfVG(GF, GH) : OfVG(GFH);
                            Rect drs = sum(vu, pT[1]).zero() ? OfVG(pG[1]) : cFlp(tlP.get(pT[1]));
                            if (GFH == 0) ctGFH(GFH = 6);
                            if (srs.dfct(drs) >= 0.1) {
                                mkGM(GFH, srs, drs.cX(), drs.cY(), minASpeed, false);
                            }
                        }
                    }
                    if (fh && at && it) {
                        moves.add(new MB(CLEAR_AND_SELECT).setRect(new Rect(world)));
                        moves.add(new MB(ASSIGN).group(GG));
                        gameState = GS.M;
                    }
                    break;
                case M:
                    P2D ep = cEP(GG);
                    mkGM(GG, ep.x, ep.y, minGSpeed * tS(OfVG(GT,GA,GI), world));
                    break;
            }
        } else {
            cdMs++;
            nextMove.setMove(move);
        }
    }

    Rect cFlp(P2D lc) {
        return new Rect(lc.x - 2.0, lc.y - 2.0, lc.y + 84.0, lc.x + 84.0);
    }

    static double tS(Rect rs, World world) {
        int[] wij = rs.c().inWorld(world);
        switch (world.getTerrainByCellXY()[wij[0]][wij[0]]) {
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
        Rect[] rs = sOfVG(a, b);
        Rect rA = rs[0]; Rect rB = rs[1];
        boolean gd = ggs[i] == GGS.F || rs[0].dfct(rs[1]) <= scale;
        switch (ggs[i]) {
            case I:
                if (rA.cX() != rB.cX() && rA.cY() != rB.cY()) ggs[i] = bne;
                else if (rA.cX() != rB.cX()) ggs[i] = GGS.INY;
                else if (rA.cY() != rB.cY()) ggs[i] = GGS.INX;
                break;
            case INX:
                if (eD(rA.cX(), rB.cX() + scale)) ggs[i] = GGS.NY;
                else mkGM(a, rA, rB.cX() + scale, rA.cY(), true);
                break;
            case INY:
                if (eD(rA.cY(), rB.cY() + scale)) ggs[i] = GGS.NX;
                else mkGM(a, rA, rA.cX(), rB.cY() + scale, true);
                break;
            case NX:
                if (eD(rA.cX(), rB.cX())) ggs[i] = GGS.F;
                else mkGM(a, rA, rB.cX(), rA.cY(), true);
                break;
            case NY:
                if (eD(rA.cY(),rB.cY())) ggs[i] = GGS.F;
                else mkGM(a, rA, rA.cX(), rB.cY(), true);
                break;
        }
        return gd;
    }

    private P2D cEP(int... groups) {
        P2D center = OfVG(groups).c();
        return eV().reduce(new P2D(Double.MAX_VALUE, Double.MAX_VALUE), (p, VeEx) -> closedTo(p, new P2D(VeEx.v), center), (a, b) -> closedTo(a, b, center));
    }

    void rt(int id, double angle) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        moves.add(MB.c(ROTATE).angle(angle));
    }

    void mkGM(int id, double x, double y) {
        mkGM(id, x, y, 0);
    }

    void mkGM(int id, double x, double y, double ms) {
        moves.add(new MB(CLEAR_AND_SELECT).group(id));
        Rect rect = OfV(gV(id));
        if (rect.dfct(x, y) < 0.5) {
            double rand = 2*random.nextDouble();
            moves.add(MB.c(MOVE).dfCToXY(rect, x + (rect.r - rect.l)*rand, y + (rect.b - rect.t)*rand).maxSpeed(ms));
        } else moves.add(MB.c(MOVE).dfCToXY(rect, x, y).maxSpeed(ms));
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
        for (Vehicle vehicle : world.getNewVehicles()) vehiclesById.put(vehicle.getId(), new VeEx(vehicle, world.getTickIndex()));

        if (minGSpeed == 0) {
            minGSpeed = Double.MAX_VALUE;
            minASpeed = Double.MAX_VALUE;
            for (VeEx ve : vehiclesById.values()) {
                if (ve.m())
                    if (ve.type() != HELICOPTER && ve.type() != FIGHTER) minGSpeed = min(minGSpeed, ve.v.getMaxSpeed());
                    else minASpeed = min(minASpeed, ve.v.getMaxSpeed());
            }
        }
        Map<VehicleType, P2D> vtP = new HashMap<>();
        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            VeEx vehicle = vehiclesById.get(vu.getId());
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                if (vehicle.m()) {
                    vehicleUpdates.computeIfAbsent(vehicle.type(), t -> new IA(0)).inc();
                    P2D mp = vtP.computeIfAbsent(vehicle.type(), t -> new P2D(Double.MAX_VALUE, Double.MAX_VALUE));
                    P2D vmp = new P2D(vehicle);
                    if (vmp.compareTo(mp) < 0) vtP.put(vehicle.type(), vmp);

                }
                vehiclesById.computeIfPresent(vu.getId(), (id, VeEx) -> new VeEx(VeEx, vu, world.getTickIndex()));
            }
        }
        for (VehicleType vt : VehicleType.values()) {
            tlPc.put(vt, true);
            P2D cp = tlP.get(vt);
            P2D np = vtP.get(vt);
            tlPc.put(vt, np != null && np.compareTo(cp) != 0);
            if (np != null && np.compareTo(cp) < 0  ) tlP.put(vt, np);
        }
        return vehicleUpdates;
    }


    Stream<VeEx> mV() { return vehiclesById.values().stream().filter(VeEx::m); }
    Rect OfV(Stream<VeEx> vhs) { return vhs.reduce(new Rect(), Rect::update, Rect::combine); }
    Stream<VeEx> gV(int id) { return mV().filter(vt -> vt.inG(id)); }
    Stream<VeEx> eV() { return vehiclesById.values().stream().filter(VeEx::e); }

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

    class VeEx {
        final Vehicle v;
        final int tI;
        final int[] gs;

        VeEx(Vehicle v, int tI) {
            this.v = v;
            this.tI = tI;
            this.gs = v.getGroups();
            Arrays.sort(this.gs);
        }

        VeEx(VeEx veEx, VehicleUpdate vu, int tI) {
            this(new Vehicle(veEx.v, vu), tI);
        }

        boolean ofVT(VehicleType type) { return v.getType() == type; }
        boolean ofVT(VehicleType[] types) {
            for (VehicleType type : types) if (v.getType() == type) return true;
            return false;
        }

        boolean isSelected() { return v.isSelected(); }
        boolean m() { return v.getPlayerId() == me.getId(); }
        boolean e() { return v.getPlayerId() != me.getId(); }
        boolean inG(int id) { return Arrays.binarySearch(gs, id) >= 0; }
        public String toString() { return "V{" + "g" + Arrays.toString(gs) + "}"; }
        VehicleType type() { return v.getType(); }
        P2D point() { return new P2D(v.getX(), v.getY()); }
        double x() {return v.getX(); }
        double y() {return v.getY(); }
        double r() {return v.getRadius(); }
    }

    static class P2D implements Comparable<P2D> {
        private double x;
        private double y;

        P2D() {}

        P2D(double x, double y) {
            this.x = x;
            this.y = y;
        }

        P2D(Vehicle vehicle) {
            this.x = vehicle.getX();
            this.y = vehicle.getY();
        }

        P2D(VeEx vehicle) {
            this.x = vehicle.x();
            this.y = vehicle.y();
        }

        @Override
        public int compareTo(P2D o) {
            int ix = cD(x,o.x);
            if (ix == 0) return cD(y,o.y);
            return ix;
        }

        boolean less(P2D p) { return compareTo(p) < 0; }

        @Override
        public String toString() {
            return "(" + round(x) + "," + round(y) + ")";
        }

        int[] inWorld(World world) {
            return new int[]{(int)round(min(world.getWidth()/PALE_SIDE, x/PALE_SIDE)),
                    (int)round(min(world.getHeight()/PALE_SIDE, y/PALE_SIDE))};
        }
    }

    static P2D closedTo(P2D a, P2D b, P2D center) {
        if (distanceTo(center, a) > distanceTo(center, b)) return b;
        else return a;
    }

    class Rect implements Comparable<Rect> {
        double l = Double.NaN,t = Double.NaN,b = Double.NaN,r = Double.NaN;
        VehicleType vt;
        int g;

        Rect() {}
        Rect(World world) { this(0, 0, world.getHeight(), world.getWidth()); }
        Rect(double l, double t, double b, double r) { this.l = l;this.t = t;this.b = b;this.r = r; }
        Rect(Vehicle v) { this(v.getX(), v.getY(), v.getY(), v.getX()); }

        Rect update(VeEx VeEx) {
            l = min(isNaN(l) ? Double.MAX_VALUE : l, VeEx.x());
            r = max(isNaN(r) ? 0 : r, VeEx.x());
            t = min(isNaN(t) ? Double.MAX_VALUE : t, VeEx.y());
            b = max(isNaN(b) ? 0 : b, VeEx.y());
            return this;
        }

        Rect combine(Rect rect) {
            l = min(isNaN(l) ? Double.MAX_VALUE : l, isNaN(rect.l) ? Double.MAX_VALUE : rect.l);
            r = max(isNaN(r) ? 0 : r, isNaN(rect.r) ? 0: rect.r);
            t = min(isNaN(t) ? Double.MAX_VALUE : t, isNaN(rect.t) ? Double.MAX_VALUE : rect.t);
            b = max(isNaN(b) ? 0 : b, isNaN(rect.b) ? 0 : rect.b);
            return this;
        }

        double square() { return abs(r - l)*abs(b - t); }
        double dfrb(double x, double y) { return hypot(x - r, y - b); }
        double dfct(double x, double y) { return hypot(x - cX(), y - cY()); }
        double dfct(P2D p) { return hypot(p.x - cX(), p.y - cY()); }
        double dfct(Rect rect) { return hypot(rect.cX() - cX(), rect.cY() - cY()); }
        P2D c() { return new P2D(cX(), cY()); };
        double cX() { return (l + r)/2; }
        double cY() { return (b + t)/2; }
        double linew() { return r - l;}
        double lineh() { return b - t;}
        boolean include(Vehicle vehicle) { return include(vehicle.getX(), vehicle.getY()); }
        boolean include(double x, double y) { return x >= l && x <= r && y >= t && y <= b; }
        Rect square(int i, int j, int n, double side) { return new Rect(l + j*side/n, t + i*side/n,
                t + (i+1)*side/n, l + (j+1)*side/n); }
        L[] sidesw() { return new L[]{tlw(), rlw(), blw(), llw()}; }
        Rect leftHalf() { return new Rect(l, t, b, (l+r)/2); }
        Rect rightHalf() { return new Rect((l+r)/2 + 0.5, t, b, r); }

        L tlw() { return new L(new P2D(0,t),new P2D(world.getWidth(),t)); }
        L rlw() { return new L(new P2D(r,0),new P2D(r, world.getHeight())); }
        L blw() { return new L(new P2D(world.getWidth(), b),new P2D(0, b)); }
        L llw() { return new L(new P2D(l, world.getHeight()),new P2D(l, 0)); }

        @Override
        public int compareTo(Rect o) {
            int cxc = cD(cX(),o.cX());
            if (cxc == 0) return cD(cY(),o.cY());
            return cxc;
        }

        @Override
        public String toString() {
            return "R{l:" + round(l) + ",t:" + round(t) + ",b:" + round(b) + ",r:" + round(r) + ",[" + c() + "]" + "}";
        }
    }

    static boolean eD(double a, double b) { return cD(a, b) == 0;}
    static int cD(double a, double b) { return abs(a - b) > EPS ? Double.compare(a, b) :  0; }

    static class L {
        double a, b, c;
        P2D[] ps;

        L(P2D[] pq) { this(pq[0], pq[1]);}
        L(P2D p, P2D q) {
            a = p.y - q.y;
            b = q.x - p.x;
            c = - a * p.x - b * p.y;
            norm();
            ps = new P2D[]{p, q};
        }

        void norm() {
            double z = sqrt (a*a + b*b);
            if (abs(z) > EPS) a /= z;b /= z;c /= z;
        }

        double dist(P2D p) {
            return a * p.x + b * p.y + c;
        }
        P2D[] ps() { return ps;}
    }

    static double angle(L a, L b) {
        return acos(a.a*b.a + a.b*b.b);
    }

    static boolean betw (double l, double r, double x) {
        return min(l,r) <= x + EPS && x <= max(l,r) + EPS;
    }

    static boolean intersect_1d (double a, double b, double c, double d) {
        double[] array = new double[]{a,b,c,d};
        if (a > b)  swap (array, 0, 1);
        if (c > d)  swap (array, 2, 3);
        return max (array[0], array[2]) <= min (array[1], array[3]) + EPS;
    }

    private static void swap(double[] x, int a, int b) {
        double t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

    static boolean intersect(P2D a, P2D b, P2D c, P2D d, P2D left, P2D right) {
        if (!intersect_1d (a.x, b.x, c.x, d.x) || ! intersect_1d (a.y, b.y, c.y, d.y)) return false;
        L m = new L(a, b);
        L n = new L(c, d);
        double zn = det(m.a, m.b, n.a, n.b);
        if (abs (zn) < EPS) {
            if (abs (m.dist (c)) > EPS || abs (n.dist (a)) > EPS)
                return false;
            return true;
        } else {
            left.x = right.x = - det(m.c, m.b, n.c, n.b) / zn;
            left.y = right.y = - det(m.a, m.c, n.a, n.c) / zn;
            return betw (a.x, b.x, left.x)
                    && betw (a.y, b.y, left.y)
                    && betw (c.x, d.x, left.x)
                    && betw (c.y, d.y, left.y);
        }
    }

    static double det(double a, double b, double c, double d) { return a*d - b*c; }
    static double distanceTo(P2D a, P2D b) { return hypot(a.x - b.x, a.y - b.y); }
    static double distanceTo(P2D a, Unit b) { return hypot(a.x - b.getX(), a.y - b.getX()); }
    static boolean intersect(P2D a1, P2D b1, P2D a2, P2D b2) { return intersect(a1, b1, a2, b2, new P2D(), new P2D()); }
    static boolean intersect(P2D pa1, P2D pb1, P2D[] side) { return intersect(pa1, pb1, side[0], side[1]); }
    static boolean intersect(P2D[] aside, P2D[] bside) { return intersect(aside[0], aside[1], bside[0], bside[1]); }
    static boolean intersect(L aside, L bside) { return intersect(aside.ps, bside.ps); }

    static class MB {
        private ActionType action;

        private int group;

        private double left;
        private double top;
        private double right;
        private double bottom;

        private double x;
        private double y;
        private double angle;
        private double factor;

        private double maxSpeed;
        private double maxAngularSpeed;

        private VehicleType vehicleType;

        private long facilityId = -1L;
        private long vehicleId = -1L;

        static MB c(ActionType action) { return new MB(action); }

        public MB() { }
        public MB(ActionType action) { this.action = action; }

        MB setAction(ActionType action) { this.action = action; return this;}
        MB group(int group) { this.group = group; return this; }
        MB left(double left) { this.left = left; return this; }
        MB top(double top) { this.top = top;return this; }
        MB right(double right) { this.right = right;  return this; }
        MB bottom(double bottom) { this.bottom = bottom; return this; }
        MB x(double x) { this.x = x; return this; }
        MB y(double y) { this.y = y; return this; }
        MB dfCToXY(Rect current, double x, double y) {
            this.y = y - current.cY();
            this.x = x - current.cX();
            return this;
        }
        MB angle(double angle) { this.angle = angle; return this; }
        MB factor(double factor) { this.factor = factor; return this; }
        MB maxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; return this; }
        MB maxAngularSpeed(double maxAngularSpeed) { this.maxAngularSpeed = maxAngularSpeed; return this; }
        MB vehicleType(VehicleType vehicleType) { this.vehicleType = vehicleType; return this; }
        MB facilityId(long facilityId) { this.facilityId = facilityId; return this; }
        MB vehicleId(long vehicleId) { this.vehicleId = vehicleId; return this; }
        Move setMove(Move dst) {
            dst.setGroup(group);

            dst.setLeft(left);
            dst.setBottom(bottom);
            dst.setTop(top);
            dst.setRight(right);

            dst.setAction(action);
            dst.setX(x);
            dst.setY(y);

            dst.setAngle(angle);
            dst.setFactor(factor);
            dst.setFacilityId(facilityId);

            dst.setMaxSpeed(maxSpeed);
            dst.setMaxAngularSpeed(maxAngularSpeed);

            dst.setVehicleType(vehicleType);
            dst.setVehicleId(vehicleId);
            return dst;
        }

        MB setRect(Rect rect) {
            left(rect.l);
            right(rect.r);
            top(rect.t);
            bottom(rect.b);
            return this;
        }
    }

    static class IA implements Comparable<IA> {
        static IA ZERO = new IA(0);
        long value;
        long n;

        public IA() {}

        public IA(long value) {
            this.value = value;
            this.n = 1;
        }
        void inc() {add(1);}
        void add(long i) { value += i; n++;}
        boolean zero() { return value == 0;}
        long v() { return value; }

        @Override
        public int compareTo(IA o) { return Long.compare(value, o.value); }
    }

    static class GP {
        public GP(int i, int j, GP prev) {
            this.i = i;
            this.j = j;
            this.prev = prev;
        }

        int i,j;
        GP prev;
    }

    Deque<MB> gm0 = new LinkedList<>();
    Deque<MB> gm2 = new LinkedList<>();

    void setupGroupingMoves() {
        Rect[] rs = sOfVT(ARRV, IFV, TANK);
        Rect order = new Rect();
        int[] groups = new int[]{GA,GI,GT};
        for (Rect r : rs) order = order.combine(r);
        int[][] field = new int[3][3];
        int[][] mD = new int[3][3];
        for (int i = 0; i < mD.length; i++) Arrays.fill(mD[i], A);
        int[] ij2 = null;
        int[] ij0 = null;
        double side = max(order.linew(), order.lineh());
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field.length; j++) {
                for (int k = 0; k < rs.length; k++) {
                    if (rs[k].cX() >= order.l + j*side/3 && rs[k].cX() <= order.l + (j+1)*side/3 &&
                        rs[k].cY() >= order.t + i*side/3 && rs[k].cY() <= order.t + (i+1)*side/3) {
                        field[i][j] = groups[k];
                        if (groups[k] == pG[0]) ij0 = new int[]{i,j};
                        if (groups[k] == pG[2]) ij2 = new int[]{i,j};
                    }
                }
            }
        }
        GP pkGr0 = bfs(field, mD, ij0[0], ij0[1], pG[1]);
        GP pkGr1 = pkGr0;
        pkGr0 = pkGr1.prev;
        // block the same side attachment
        if (pkGr0.i == pkGr1.i) { mij(pkGr1.i, pkGr1.j - 1, mD, L+R+U); mij(pkGr1.i, pkGr1.j + 1, mD, L+R+D); ggs[2] = GGS.INX; ggs[1] = GGS.INY; }
        else { mij(pkGr1.i - 1, pkGr1.j, mD, L+D+U); mij(pkGr1.i + 1, pkGr1.j, mD, R+D+U); ggs[2] = GGS.INY; ggs[1] = GGS.INX;}
        for (;pkGr0 != null && pkGr0.prev != null;pkGr0 = pkGr0.prev) {
            fM(pG[0], pkGr0.prev.i, pkGr0.prev.j, pkGr0.i, pkGr0.j, order, gm0, side);
            field[pkGr0.i][pkGr0.j] = Integer.MAX_VALUE;
        }
        GP pkG2 = bfs(field, mD, ij2[0], ij2[1], pG[1]);
        for (pkG2 = pkG2.prev; pkG2 != null && pkG2.prev != null;pkG2 = pkG2.prev)
            fM(pG[2], pkG2.prev.i, pkG2.prev.j, pkG2.i, pkG2.j, order, gm2, side);
    }

    void mij(int i, int j, int[][] md, int ms) {
        if (i >= 3 || i < 0 || j < 0 || j >=3) return;
        md[i][j] = ms;
    }

    GP bfs(int[][] field, int[][] mD, int si, int sj, int dgid) {
        Queue<GP> qp = new LinkedList<>();
        qp.add(new GP(si, sj, null));
        while (!qp.isEmpty()) {
            GP ij = qp.poll();
            int i = ij.i;
            int j = ij.j;
            if (field[i][j] == dgid) {
                return ij;
            }
            if (cij(i+1,j,field, dgid) && (mD[i][j] & R) > 0) qp.add(new GP(i+1,j, ij));
            if (cij(i,j+1,field, dgid) && (mD[i][j] & D) > 0) qp.add(new GP(i,j+1, ij));
            if (cij(i-1,j,field, dgid) && (mD[i][j] & L) > 0) qp.add(new GP(i-1,j, ij));
            if (cij(i,j-1,field, dgid) && (mD[i][j] & U) > 0) qp.add(new GP(i,j-1, ij));
        }
        return null;
    }

    boolean cij(int i, int j, int[][] field, int dgid) {
        if (i >= 3 || i < 0 || j < 0 || j >=3) return false;
        if (field[i][j] == dgid || field[i][j] == 0) return true;
        return false;
    }

    boolean fM(int gid, int si, int sj, int di, int dj, Rect order, Deque<MB> deque, double side) {
        Rect dst = order.square(di, dj, 3, side);
        deque.add(MB.c(MOVE).dfCToXY(order.square(si, sj, 3, side), dst.cX(), dst.cY()));
        deque.addLast(MB.c(CLEAR_AND_SELECT).group(gid));
        return true;
    }

    private static double EPS = 0.000001;
    private static double PALE_SIDE = 32.0;

    final static int L = 1;
    final static int R = 2;
    final static int U = 4;
    final static int D = 8;
    final static int A = L+R+U+D;

    final static int GT = 1;
    final static int GA = 2;
    final static int GH = 3;
    final static int GI = 4;
    final static int GF = 5;
    final static int GG = 10;

    int GFH = 0;
    final static int GA2 = 7;
    final static int GG1 = 8;
    final static int GG2 = 9;

    private Random random;
    enum GS {I, G, GG, M}
    enum GGS { I, INX, INY, NX, NY, F }
}

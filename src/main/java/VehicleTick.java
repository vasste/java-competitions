import model.*;

import java.util.Arrays;

public class VehicleTick implements Comparable<VehicleTick> {
    final Vehicle v;
    final int tI;
    final int[] gs;

    VehicleTick(Vehicle v, int tI) {
        this.v = v;
        this.tI = tI;
        this.gs = v.getGroups();
        Arrays.sort(this.gs);
    }

    VehicleTick(VehicleTick veEx, VehicleUpdate vu, int tI) {
        this(new Vehicle(veEx.v, vu), tI);
    }

    boolean ofVT(VehicleType type) { return v.getType() == type; }
    boolean ofVT(VehicleType[] types) {
        for (VehicleType type : types) if (v.getType() == type) return true;
        return false;
    }

    double getDistanceTo(VehicleTick vehicleTick) { return v.getDistanceTo(vehicleTick.v); }
    boolean isSelected() { return v.isSelected(); }
    boolean m(Player me) { return v.getPlayerId() == me.getId(); }
    boolean e(Player me) { return v.getPlayerId() != me.getId(); }
    boolean inG(int id) { return Arrays.binarySearch(gs, id) >= 0; }
    boolean inGs(int... ids) {
        for (int i = 0; i < ids.length; i++) {
            if (Arrays.binarySearch(gs, ids[i]) >= 0) return true;
        }
        return false;
    }
    public String toString() { return "V{" + "g" + Arrays.toString(gs) + "}"; }
    VehicleType type() { return v.getType(); }
    long id() { return v.getId(); }
    boolean isAerial() { return v.isAerial(); }
    P2D point() { return new P2D(v.getX(), v.getY()); }
    double x() {return v.getX(); }
    double y() {return v.getY(); }
    double r() {return v.getRadius(); }

    double speed(Game game, World world, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        int[] wij = inWorld(world);
        if (isAerial()) {
            return v.getMaxSpeed() * wSf(game, weatherTypes[wij[0]][wij[0]]);
        } else {
            return v.getMaxSpeed() * tSf(game, terrainTypes[wij[0]][wij[0]]);
        }
    }

    boolean see(VehicleTick u, Game game, World world, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        int[] uij = u.inWorld(world);
        double shF = u.isAerial() ? wShf(game, weatherTypes[uij[0]][uij[1]]) : tShf(game, terrainTypes[uij[0]][uij[1]]);
        double vF = isAerial() ? wVf(game, weatherTypes[uij[0]][uij[1]]): tVf(game, terrainTypes[uij[0]][uij[1]]);
        return P2D.distanceTo(u, this) <= v.getVisionRange() * shF * vF;
    }

    boolean attack(VehicleTick u) {
        if (type() == VehicleType.ARRV) return false;
        if (type() == VehicleType.FIGHTER && u.v.isAerial()) return P2D.distanceTo(u, this) <= v.getAerialAttackRange();
        if (type() == VehicleType.FIGHTER && !u.v.isAerial()) return false;
        double duthis = P2D.distanceTo(u, this);
        return duthis <= v.getAerialAttackRange() || duthis <= v.getGroundAttackRange();
    }

    double distanceTo(double x, double y) { return v.getDistanceTo(x, y); }

    @Override
    public int compareTo(VehicleTick o) {
        int ix = U.cD(x(),o.x());
        if (ix == 0) return U.cD(y(),o.y());
        return ix;
    }

    int[] inWorld(World world) {
        return new int[]{(int) StrictMath.round(StrictMath.min(world.getWidth()/U.PALE_SIDE, x()/U.PALE_SIDE)),
                (int) StrictMath.round(StrictMath.min(world.getHeight()/U.PALE_SIDE, y()/U.PALE_SIDE))};
    }

    static double tSf(Game game, TerrainType terrainType) {
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

    static double wSf(Game game, WeatherType weatherType) {
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

    static double wVf(Game game, WeatherType weatherType) {
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

    static double tVf(Game game, TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return game.getPlainTerrainVisionFactor();
            case SWAMP:
                return game.getSwampTerrainVisionFactor();
            case FOREST:
                return game.getForestTerrainVisionFactor();
        }
        return 1;
    }

    static double wShf(Game game, WeatherType weatherType) {
        switch (weatherType) {
            case CLEAR:
                return game.getClearWeatherStealthFactor();
            case CLOUD:
                return game.getClearWeatherStealthFactor();
            case RAIN:
                return game.getRainWeatherSpeedFactor();
        }
        return 1;
    }

    static double tShf(Game game, TerrainType terrainType) {
        switch (terrainType) {
            case PLAIN:
                return game.getPlainTerrainStealthFactor();
            case FOREST:
                return game.getForestTerrainStealthFactor();
            case SWAMP:
                return game.getSwampTerrainStealthFactor();
        }
        return 1;
    }
}

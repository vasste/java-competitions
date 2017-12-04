import model.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class VehicleTick implements Comparable<VehicleTick> {
    final Vehicle v;
    final int tI;
    final Set<Integer> gs;
    final int[] wij;

    VehicleTick(Vehicle v, int tI, World world) {
        this.v = v;
        this.tI = tI;
        this.gs = Arrays.stream(v.getGroups()).boxed().collect(Collectors.toSet());
        wij = inWorld(world);
    }

    VehicleTick(VehicleTick veEx, VehicleUpdate vu, int tI, World world) {
        this(new Vehicle(veEx.v, vu), tI, world);
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
    boolean inG(int id) { return gs.contains(id); }
    boolean inGs(int... ids) {
        for (int id : ids) if (gs.contains(id)) return true;
        return false;
    }
    public String toString() { return "V{" + "g" + gs + "}"; }
    VehicleType type() { return v.getType(); }
    long id() { return v.getId(); }
    boolean isAerial() { return v.isAerial(); }
    double attackRange() {
        return isAerial() ? v.getAerialAttackRange() : v.getGroundAttackRange();
    }
    P2D point() { return new P2D(v.getX(), v.getY()); }
    double x() {return v.getX(); }
    double y() {return v.getY(); }
    double r() {return v.getRadius(); }

    double speed(Game game, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        if (isAerial()) {
            return v.getMaxSpeed() * wSf(game, weatherTypes[wij[0]][wij[1]]);
        } else {
            return v.getMaxSpeed() * tSf(game, terrainTypes[wij[0]][wij[1]]);
        }
    }

    Rectangle see(Game game, World world, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        double vF = isAerial() ? wVf(game, weatherTypes[wij[0]][wij[1]]): tVf(game, terrainTypes[wij[0]][wij[1]]);
        double visionRange = v.getVisionRange() * vF;
        return new Rectangle(x(), y(), world, visionRange);
    }

    boolean see(VehicleTick u, Game game, WeatherType[][] weatherTypes, TerrainType[][] terrainTypes) {
        double shF = u.isAerial() ? wShf(game, weatherTypes[u.wij[0]][u.wij[1]]) : tShf(game, terrainTypes[u.wij[0]][u.wij[1]]);
        double vF = isAerial() ? wVf(game, weatherTypes[wij[0]][wij[1]]): tVf(game, terrainTypes[wij[0]][wij[1]]);
        return P2D.distanceTo(u, this) <= v.getVisionRange() * shF * vF;
    }

    boolean attack(VehicleTick u) {
        if (type() == VehicleType.ARRV) return false;
        if (type() == VehicleType.FIGHTER && !u.v.isAerial()) return false;
        if (type() == VehicleType.FIGHTER && u.v.isAerial()) return P2D.distanceTo(u, this) <= v.getAerialAttackRange();
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

    private int[] inWorld(World world) {
        return new int[]{(int) StrictMath.floor(StrictMath.min(world.getWidth()/U.PALE_SIDE, x()/U.PALE_SIDE)),
                (int) StrictMath.floor(StrictMath.min(world.getHeight()/U.PALE_SIDE, y()/U.PALE_SIDE))};
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

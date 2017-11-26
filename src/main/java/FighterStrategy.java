import model.*;

import java.util.*;
import java.util.stream.Stream;

import static java.lang.Math.PI;
import static model.ActionType.*;

/**
 * @author Vasilii Stepanov.
 * @since 24.11.2017
 */
public class FighterStrategy implements Strategy {

    final Map<Long, Vehicle> vehiclesById = new HashMap<>();
    final Map<Long, Integer> vehiclesTick = new HashMap<>();
    Player me;
    Queue<MB> moves = new LinkedList<>();
    Long gunnerId;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        Player enemy = Arrays.stream(world.getPlayers()).filter(p -> p.getId() != me.getId()).findFirst().orElse(me);

        for (Vehicle vehicle : world.getNewVehicles()) {
            vehiclesById.put(vehicle.getId(), vehicle);
        }
        for (VehicleUpdate vu : world.getVehicleUpdates()) {
            Vehicle vehicle = vehiclesById.get(vu.getId());
            if (vu.getDurability() == 0) {
                vehiclesById.remove(vu.getId());
            } else {
                vehiclesById.put(vu.getId(), new Vehicle(vehicle, vu));
                vehiclesTick.put(vu.getId(), world.getTickIndex());
            }
        }
        if (me.getRemainingActionCooldownTicks() > 0) return;
        if (world.getTickIndex() < 1000) return;
        MB nextMove = moves.poll();
        if (nextMove == null) {
            Rect rect = OfVG(eVt(VehicleType.TANK));
            if (gunnerId == null || vehiclesById.get(gunnerId) == null) {
                Vehicle gunnerFighter = mVt(VehicleType.FIGHTER).reduce(null, (a, b) -> {
                    if (a == null) return b;
                    if (b == null) return a;
                    return P2D.closedTo(a, b, rect.c());
                });
                if (gunnerFighter != null) {
                    moves.add(new MB(CLEAR_AND_SELECT).setRect(new Rect(gunnerFighter)).vehicleType(VehicleType.FIGHTER));
                    gunnerId = gunnerFighter.getId();
                    moves.add(new MB(MOVE)
                            .x(rect.r - gunnerFighter.getVisionRange() - gunnerFighter.getX())
                            .y(rect.b - gunnerFighter.getVisionRange() - gunnerFighter.getY()));
                }
            } else {
                Vehicle gunnerFighter = vehiclesById.get(gunnerId);
                int lastTick = vehiclesTick.get(gunnerId);
                if (world.getTickIndex() - lastTick > 60) {
                    moves.add(new MB(ROTATE).angle(PI));
                } else {
                    if (rect.cX() - 0.7*gunnerFighter.getVisionRange() - gunnerFighter.getX() > 0.5 ||
                        rect.cY() - 0.7*gunnerFighter.getVisionRange() - gunnerFighter.getY() > 0.5) {
                        moves.add(new MB(MOVE)
                                .x(rect.cX() - 0.7*gunnerFighter.getVisionRange() - gunnerFighter.getX())
                                .y(rect.cY() - 0.7*gunnerFighter.getVisionRange() - gunnerFighter.getY()));
                    } else {
                        if (eV().noneMatch(v -> v.getDistanceTo(gunnerFighter) < gunnerFighter.getVisionRange() + 10)) {
                            moves.add(new MB(MOVE)
                                    .x(rect.cX() - 0.7*gunnerFighter.getX())
                                    .y(rect.cY() - 0.7*gunnerFighter.getY()));
                        } else {
                            if (me.getRemainingNuclearStrikeCooldownTicks() == 0 && me.getNextNuclearStrikeTickIndex() == -1) {
                                Vehicle eVns = null;
                                P2D pgF = new P2D(gunnerFighter);
                                for (Vehicle eV : (Iterable<Vehicle>) eV()::iterator) {
                                    if (see(gunnerFighter, eV, game, world)) {
                                        if (eVns == null) eVns = eV;
                                        else eVns = P2D.futherTo(eVns, eV, pgF);
                                    }
                                }
                                if (eVns != null)
                                    moves.add(new MB(ActionType.TACTICAL_NUCLEAR_STRIKE).vehicleId(gunnerId).x(eVns.getX()).y(eVns.getY()));
                            }
                            moves.add(new MB(MOVE).x(0).y(0));
                        }
                    }
                }
            }
        } else {
            nextMove.setMove(move);
        }
    }

    static double wtVF(Game game, WeatherType weatherType) {
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


    static boolean see(Vehicle v, Vehicle u, Game game, World world) {
        int[] wij = new P2D(v).inWorld(world);
        return u.getDistanceTo(v) < v.getVisionRange() * wtVF(game, world.getWeatherByCellXY()[wij[0]][wij[1]]);
    }

    Stream<Vehicle> eV() { return vehiclesById.values().stream().filter(v -> v.getPlayerId() != me.getId()); }
    Stream<Vehicle> eVt(VehicleType vt) { return eV().filter(v -> v.getType() == vt); }
    Rect OfVG(Stream<Vehicle> stream) {
        return stream.reduce(new Rect(), (rect, v) -> rect.combine(new Rect(v)), Rect::combine);
    }
    Stream<Vehicle> mV() { return vehiclesById.values().stream().filter(v -> v.getPlayerId() == me.getId()); }
    Stream<Vehicle> mVt(VehicleType vt) { return mV().filter(v -> v.getType() == vt); }
}

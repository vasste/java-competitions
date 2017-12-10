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
    Queue<MoveBuilder> moves = new LinkedList<>();
    Long gunnerId;
    WeatherType[][] weatherTypes;
    TerrainType[][] terrainTypes;

    @Override
    public void move(Player me, World world, Game game, Move move) {
        this.me = me;
        Player enemy = Arrays.stream(world.getPlayers()).filter(p -> p.getId() != me.getId()).findFirst().orElse(me);
        terrainTypes = world.getTerrainByCellXY();
        weatherTypes = world.getWeatherByCellXY();

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
        //if (world.getTickIndex() < 1000) return;
        MoveBuilder nextMove = moves.poll();
        if (nextMove == null) {
            Rectangle rectangle = OfVG(eVt(VehicleType.TANK));
            if (gunnerId == null || vehiclesById.get(gunnerId) == null) {
                Vehicle gunnerFighter = mVt(VehicleType.FIGHTER).reduce(null, (a, b) -> {
                    if (a == null) return b;
                    if (b == null) return a;
                    return P2D.closedTo(a, b, rectangle.c());
                });
                if (gunnerFighter != null) {
                    moves.add(new MoveBuilder(CLEAR_AND_SELECT).setRect(new Rectangle(gunnerFighter)).vehicleType(VehicleType.FIGHTER));
                    gunnerId = gunnerFighter.getId();
                    moves.add(new MoveBuilder(MOVE)
                            .x(rectangle.r - gunnerFighter.getVisionRange() - gunnerFighter.getX())
                            .y(rectangle.b - gunnerFighter.getVisionRange() - gunnerFighter.getY()));
                }
            } else {
                Vehicle gunnerFighter = vehiclesById.get(gunnerId);
                int lastTick = vehiclesTick.get(gunnerId);
                if (world.getTickIndex() - lastTick > 60) {
                    moves.add(new MoveBuilder(ROTATE).angle(PI));
                } else {
                    double closeFactor = 0.6;
                    if (rectangle.cX() - closeFactor*gunnerFighter.getVisionRange() - gunnerFighter.getX() > closeFactor ||
                        rectangle.cY() - closeFactor*gunnerFighter.getVisionRange() - gunnerFighter.getY() > closeFactor) {
                        moves.add(new MoveBuilder(MOVE)
                                .x(rectangle.cX() - closeFactor*gunnerFighter.getVisionRange() - gunnerFighter.getX())
                                .y(rectangle.cY() - closeFactor*gunnerFighter.getVisionRange() - gunnerFighter.getY()));
                    } else {
                        if (eV().noneMatch(v -> v.getDistanceTo(gunnerFighter) < gunnerFighter.getVisionRange() + 10)) {
                            moves.add(new MoveBuilder(MOVE)
                                    .x(rectangle.cX() - closeFactor*gunnerFighter.getX())
                                    .y(rectangle.cY() - closeFactor*gunnerFighter.getY()));
                        } else {
                            if (me.getRemainingNuclearStrikeCooldownTicks() == 0 && me.getNextNuclearStrikeTickIndex() == -1) {
                                Vehicle eVns = null;
                                P2D pgF = new P2D(OfVG(eV()).c());
                                for (Vehicle eV : (Iterable<Vehicle>) eV()::iterator) {
                                    if (see(gunnerFighter, eV, game, world)) {
                                        if (eVns == null) eVns = eV;
                                        else eVns = P2D.closedTo(eVns, eV, pgF);
                                    }
                                }
                                if (eVns != null)
                                    moves.add(new MoveBuilder(ActionType.TACTICAL_NUCLEAR_STRIKE).vehicleId(gunnerId).x(eVns.getX() - 10).y(eVns.getY() - 10));
                            }
                            moves.add(new MoveBuilder(MOVE).x(0).y(0));
                        }
                    }
                }
            }
        } else {
            nextMove.setMove(move);
        }
    }

    boolean see(Vehicle v, Vehicle u, Game game, World world) {
        return new VehicleTick(v, 0, world).see(new VehicleTick(u, 0, world), game, weatherTypes, terrainTypes);
    }

    Stream<Vehicle> eV() { return vehiclesById.values().stream().filter(v -> v.getPlayerId() != me.getId()); }
    Stream<Vehicle> eVt(VehicleType vt) { return eV().filter(v -> v.getType() == vt); }
    Rectangle OfVG(Stream<Vehicle> stream) {
        return stream.reduce(new Rectangle.Builder(), (rect, v) -> rect.combine(new Rectangle(v)), Rectangle.Builder::combine).build();
    }
    Stream<Vehicle> mV() { return vehiclesById.values().stream().filter(v -> v.getPlayerId() == me.getId()); }
    Stream<Vehicle> mVt(VehicleType vt) { return mV().filter(v -> v.getType() == vt); }
}

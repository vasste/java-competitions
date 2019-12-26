import model.*;
import strategy.world.World;

import java.util.HashMap;
import java.util.Map;

public class StrategyAttackRecovery {

    private final boolean debugEnabled = false;
	private Map<Integer, Unit> unitsInTick = new HashMap<>();
	private Map<Integer, Integer> opponents = new HashMap<>();
	private int tick;

	private UnitStrategy initGame(Unit me, Game game) {
		UnitStrategy strategy = new StrategyAttack();
		if (0.5 * game.getProperties().getUnitMaxHealth() > me.getHealth())
			strategy = new StrategyRecovery();
		return strategy;
	}

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		UnitStrategy strategy = initGame(unit, game);
		Unit unitInTick = initMove(unit, game);
		JumpState state = unit.getJumpState();
		double horzUnitSpeed = Math.abs(unit.getPosition().getX() - unitInTick.getPosition().getX());
		double vertUnitSpeed = Math.abs(unit.getPosition().getY() - unitInTick.getPosition().getY());
		Vec2Double unitSpeed = new Vec2Double(horzUnitSpeed, Math.max(state.getSpeed(), vertUnitSpeed));

		debug.draw(DebugUtils.write(horzUnitSpeed + "", 1, 10));
		debug.draw(DebugUtils.write(vertUnitSpeed + "", 1, 11f));

		Level level = game.getLevel();
		Tile[][] tiles = level.getTiles();
		Unit teamMateUnit = null;
		for (Unit gameUnit : game.getUnits()) {
			boolean teamMate = unit.getPlayerId() == gameUnit.getPlayerId();
			if (teamMate && gameUnit.getId() != unit.getId()) {
				teamMateUnit = gameUnit;
				break;
			}
		}
		World world = new World(unit.getPosition(), unitSpeed, tiles, game.getProperties(), 50,
				teamMateUnit == null ? null : teamMateUnit.getPosition(), debugEnabled);
		UnitAction unitAction;
		if (strategy.feasible(world, unit, game, debug, unitInTick)) {
			unitAction = strategy.getUnitAction(world, unit, game, debug, unitInTick, this);
		} else {
			unitAction = new StrategyAttack().getUnitAction(world, unit, game, debug, unitInTick, this);
		}
		saveMove(unit, game);
		return unitAction;
	}

	private Unit initMove(Unit unit, Game game) {
		if (tick == 0)
			tick = game.getCurrentTick();
		return unitsInTick.getOrDefault(unit.getId(), unit);
	}

	private void saveMove(Unit unit, Game game) {
		tick = game.getCurrentTick();
		unitsInTick.put(unit.getId(), unit);
	}

	Unit findOpponent(Unit me, Game game) {
		Integer opponentPlayerId = opponents.get(me.getId());
		for (Unit gameUnit : game.getUnits()) {
			boolean teamMate = gameUnit.getPlayerId() == me.getPlayerId();
			if (!teamMate) {
				if (opponentPlayerId == null) {
					opponents.put(me.getId(), gameUnit.getId());
					return gameUnit;
				} else if (opponentPlayerId.equals(gameUnit.getId())) {
					return gameUnit;
				}
			}
		}
		return null;
	}
}

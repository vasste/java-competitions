import model.*;
import strategy.world.World;

import java.util.HashMap;
import java.util.Map;

class StrategyAttackRecovery {

	public static final boolean debugEnabled = true;
	private Map<Integer, Unit> unitsInTick = new HashMap<>();
	private Map<Integer, Integer> opponents = new HashMap<>();
	private int tick;
	private StrategyAttack strategyAttack = new StrategyAttack();
	private StrategyRecovery strategyRecovery = new StrategyRecovery();

	private UnitStrategy initGame(Unit me, Game game) {
		if (0.6 * game.getProperties().getUnitMaxHealth() > me.getHealth())
			return strategyRecovery;
		return strategyAttack;
	}

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		UnitStrategy strategy = initGame(unit, game);
		Unit unitInTick = initMove(unit, game);
		JumpState state = unit.getJumpState();
		double horzUnitSpeed = unit.getPosition().getX() - unitInTick.getPosition().getX();
		double vertUnitSpeed = unit.getPosition().getY() - unitInTick.getPosition().getY();
		Vec2Double unitSpeed = new Vec2Double(horzUnitSpeed,
				!unit.isOnGround() && !state.isCanJump() ? vertUnitSpeed : state.getSpeed());

		if (debugEnabled) {
			debug.draw(DebugUtils.write(horzUnitSpeed + "", 1, 10));
			debug.draw(DebugUtils.write(vertUnitSpeed + "", 1, 11f));
		}

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
		World world = new World(unit.getPosition(), unitSpeed, tiles, game.getProperties(), 30,
				teamMateUnit == null ? null : teamMateUnit.getPosition(), unit.isOnGround(), state);
		DebugUtils.drawGrid(debug, game, debugEnabled);
		UnitAction unitAction;
		if (strategy.feasible(world, unit, game, debug, unitInTick)) {
			unitAction = strategy.getUnitAction(world, unit, game, debug, unitInTick, this);
		} else {
			unitAction = strategyAttack.getUnitAction(world, unit, game, debug, unitInTick, this);
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
					if (!opponents.containsValue(gameUnit.getId())) {
						opponents.put(me.getId(), gameUnit.getId());
						return gameUnit;
					}
				} else if (opponentPlayerId.equals(gameUnit.getId())) {
					return gameUnit;
				}
			}
		}
		return null;
	}

	boolean isWeaponType(Unit unit, WeaponType weaponType) {
		return unit.getWeapon() != null && unit.getWeapon().getTyp() == weaponType;
	}

}

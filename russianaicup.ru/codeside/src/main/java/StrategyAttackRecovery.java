import model.*;
import strategy.Vec2Int;
import strategy.world.World;
import strategy.world.WorldUtils;

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
		Unit unitInTick = initMove(unit);
		JumpState state = unit.getJumpState();

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
		Unit opponent = findOpponent(unit, game);
		if (opponent == null && teamMateUnit != null)
			opponent = findOpponent(teamMateUnit, game);

		int radius = Math.min(tick, 30);
		if (opponent != null && unit.getWeapon() != null)
			radius = Math.min(radius,
					WorldUtils.distanceManhattan(new Vec2Int(opponent.getPosition()), new Vec2Int(unit.getPosition())));

		World world = new World(unit.getPosition(), tiles, game.getProperties(), radius,
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

	private Unit initMove(Unit unit) {
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

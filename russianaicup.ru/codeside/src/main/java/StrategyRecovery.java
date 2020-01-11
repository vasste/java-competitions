import model.*;
import strategy.Action;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static strategy.world.WorldUtils.distanceSqr;

public 	class StrategyRecovery implements UnitStrategy {
	private Unit teamMateUnit;
	private Map<Integer, Vec2Double> teamMateLoot = new HashMap<>();

	public UnitAction getUnitAction(World world, Unit me, Game game, Debug debug, Unit before, StrategyAttackRecovery manager) {
		Vec2Double nearestHealth = teamMateLoot.get(me.getId());
		if (nearestHealth != null) {
			Path paths = new Path(world);
			List<Edge> destinationPath = paths.find(nearestHealth);

			Level level = game.getLevel();
			Draw draw = new Draw(StrategyAttackRecovery.debugEnabled, world, level.getTiles());

			double direction = Math.signum(nearestHealth.getX() - me.getPosition().getX());
			double velocity = game.getProperties().getUnitMaxHorizontalSpeed();
			boolean jumpUp = false;
			boolean jumpDown = false;

			Unit nearestEnemy = manager.findOpponent(me, game);
			Vec2Double aim = new Vec2Double(0, 0);
			if (nearestEnemy != null) {
				aim = new Vec2Double(nearestEnemy.getPosition().getX() - me.getPosition().getX(),
						nearestEnemy.getPosition().getY() - me.getPosition().getY());
			}

			draw.paths(destinationPath, debug);
			if (!destinationPath.isEmpty()) {
				Edge firstStride = destinationPath.iterator().next();
				velocity = Math.max(5, firstStride.maxSpeed);
				jumpUp = firstStride.action == Action.JUMP_UP;
				jumpDown = firstStride.action == Action.JUMP_DOWN;
				direction = firstStride.toD.getX() - me.getPosition().getX();
			}
			return new UnitAction(direction * velocity, jumpUp, jumpDown, aim,
					true, false,
					manager.isWeaponType(me, WeaponType.ROCKET_LAUNCHER) ||
							manager.isWeaponType(me, WeaponType.PISTOL), false);
		}
		return NO_ACTION;
	}

	@Override
	public boolean feasible(World world, Unit me, Game game, Debug debug, Unit before) {
		for (Unit unit : game.getUnits()) {
			if (unit.getPlayerId() == me.getPlayerId() && unit.getId() != me.getId()) {
				this.teamMateUnit = unit;
				break;
			}
		}

		Vec2Double nearestHealth = teamMateLoot.get(me.getId());
		if (nearestHealth != null) {
			for (LootBox lootBox : game.getLootBoxes()) {
				if (WorldUtils.VDC.compare(lootBox.getPosition(), nearestHealth) == 0)
					return true;
			}
		}
		teamMateLoot.remove(me.getId());
		nearestHealth = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			Vec2Double partnerLoot = teamMateUnit == null ? null : teamMateLoot.get(teamMateUnit.getId());
			if (lootBox.getItem() instanceof Item.HealthPack &&
					Comparator.nullsLast(WorldUtils.VDC).compare(partnerLoot, lootBox.getPosition()) != 0) {
				if (nearestHealth == null ||
					distanceSqr(me.getPosition(), lootBox.getPosition()) < distanceSqr(me.getPosition(), nearestHealth)) {
					nearestHealth = world.accessible(lootBox.getPosition()) ? lootBox.getPosition() : nearestHealth;
				}
			}
		}
		if (nearestHealth != null)
			teamMateLoot.put(me.getId(), nearestHealth);

		return nearestHealth != null;
	}
}
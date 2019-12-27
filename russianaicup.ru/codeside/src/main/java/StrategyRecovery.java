import model.*;
import strategy.Action;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;

import java.util.List;

import static strategy.world.WorldUtils.distanceSqr;

public 	class StrategyRecovery implements UnitStrategy {
	LootBox nearestHealth;
	private boolean debugEnabled = true;

	public UnitAction getUnitAction(World world, Unit me, Game game, Debug debug, Unit before, StrategyAttackRecovery manager) {
		if (nearestHealth != null) {
			Path paths = new Path(world);
			List<Edge> destinationPath = paths.find(nearestHealth.getPosition());

			Level level = game.getLevel();
			Draw draw = new Draw(debugEnabled, world, level.getTiles());

			double direction = Math.signum(nearestHealth.getPosition().getX() - me.getPosition().getX());
			double velocity = game.getProperties().getUnitMaxHorizontalSpeed();
			boolean jumpUp = false;
			boolean jumpDown = false;

			draw.paths(destinationPath, debug);
			if (!destinationPath.isEmpty()) {
				Edge firstStride = destinationPath.iterator().next();
				velocity = firstStride.action.jump() ? game.getProperties().getJumpPadJumpSpeed() : firstStride.maxSpeed;
				direction = Math.signum(nearestHealth.getPosition().getX() - me.getPosition().getX());
				jumpUp = firstStride.action == Action.JUMP_UP;
				jumpDown = firstStride.action == Action.JUMP_DOWN;
			}
			return new UnitAction(direction * velocity, jumpUp, jumpDown, ZERO,
					false, false, false, false);
		}
		return NO_ACTION;
	}

	@Override
	public boolean feasible(World world, Unit unit, Game game, Debug debug, Unit before) {
		nearestHealth = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.HealthPack) {
				if (nearestHealth == null ||
					distanceSqr(unit.getPosition(), lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHealth.getPosition())) {
					nearestHealth = world.accessible(lootBox.getPosition()) ? lootBox : nearestHealth;
				}
			}
		}
		return nearestHealth != null;
	}
}
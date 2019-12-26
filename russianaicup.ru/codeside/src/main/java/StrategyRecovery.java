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

			draw.paths(destinationPath, debug);
			if (!destinationPath.isEmpty()) {
				Edge firstStride = destinationPath.iterator().next();
				double velocity = firstStride.action.jump() ? game.getProperties().getJumpPadJumpSpeed() : firstStride.maxSpeed;
				double direction = Math.signum(nearestHealth.getPosition().getX() - me.getPosition().getX());
				return new UnitAction(direction * velocity,
						firstStride.action == Action.JUMP_UP,
						firstStride.action == Action.JUMP_DOWN, ZERO,
						false, false, false, false);
			}
		}
		return NO_ACTION;
	}

	@Override
	public boolean feasible(World world, Unit unit, Game game, Debug debug, Unit before) {
		nearestHealth = null;
		for (LootBox lootBox : game.getLootBoxes()) {
			if (lootBox.getItem() instanceof Item.HealthPack) {
				if (nearestHealth == null || distanceSqr(unit.getPosition(),
						lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHealth.getPosition())) {
					nearestHealth = lootBox;
				}
			}
		}
		return nearestHealth != null;
	}
}
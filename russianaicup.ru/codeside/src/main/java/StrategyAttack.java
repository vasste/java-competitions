import model.*;
import model.Properties;
import strategy.Action;
import strategy.Vec2Int;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.util.*;

public class StrategyAttack implements UnitStrategy {

	private Unit teamMateUnit;
	private Map<Integer, Vec2Double> teamMateLoot = new HashMap<>();

	public UnitAction getUnitAction(World world, Unit me, Game game, Debug debug, Unit meBefore, StrategyAttackRecovery manager) {
		UnitAction unitAction = NO_ACTION;
		List<Edge> destinationPath = Collections.emptyList();
		Vec2Double destination = null;

		Level level = game.getLevel();
		Draw draw = new Draw(StrategyAttackRecovery.debugEnabled, world, level.getTiles());
		Path path = new Path(world);

		// find weapon
		// find opponent
		// attack
		Vec2Double closestWeapon = teamMateLoot.get(me.getId());
		if (closestWeapon == null && me.getWeapon() == null) {
			closestWeapon = closestWeapon(me, game, closestWeapon);
		} else if (closestWeapon != null && me.getWeapon() != null) {
			teamMateLoot.remove(me.getId());
			closestWeapon = null;
		}

		if (closestWeapon != null) {
			teamMateLoot.putIfAbsent(me.getId(), closestWeapon);

			// TODO check the weapon is available
			destinationPath = path.find(closestWeapon);
			if (!destinationPath.isEmpty()) {
				destination = closestWeapon;
			}
		}

		if (me.getWeapon() != null && destinationPath.isEmpty()) {
			Unit opponent = manager.findOpponent(me, game);
			if (opponent == null)
				opponent = manager.findOpponent(teamMateUnit, game);

			if (opponent != null) {
				destination = opponent.getPosition();
				destinationPath = path.find(destination);
			}
		}

		draw.paths(destinationPath, debug);
		double direction = 0;
		double velocity = game.getProperties().getUnitMaxHorizontalSpeed()/2;
		boolean shoot = false;
		Vec2Double aim = ZERO;
		if (destination != null) {
			aim = shootAt(me, destinationPath, game, destination, world.getAverageTileLength());
			shoot = WorldUtils.VDC.compare(aim, ZERO) != 0;
			direction = Math.signum(destination.getX() - me.getPosition().getX());
		}


		if (!destinationPath.isEmpty()) {
			if (shoot) {
				unitAction = new UnitAction(0,
						false,
						false, aim,
						shoot, simpleReloadStrategy(me),
						manager.isWeaponType(me, WeaponType.ROCKET_LAUNCHER) ||
								manager.isWeaponType(me, WeaponType.PISTOL), false);
			} else {
				Edge firstStride = destinationPath.iterator().next();
				direction = firstStride.toD.getX() - me.getPosition().getX();
				unitAction = new UnitAction(direction * Math.max(.8, firstStride.maxSpeed),
						 firstStride.action == Action.JUMP_UP,
						firstStride.action == Action.JUMP_DOWN, aim,
						shoot, simpleReloadStrategy(me),
						manager.isWeaponType(me, WeaponType.ROCKET_LAUNCHER) ||
								manager.isWeaponType(me, WeaponType.PISTOL), false);
			}
		} else {
			unitAction = new UnitAction(direction * velocity,
					false,
					false, aim,
					shoot, simpleReloadStrategy(me),
					manager.isWeaponType(me, WeaponType.ROCKET_LAUNCHER) ||
							manager.isWeaponType(me, WeaponType.PISTOL), false);
		}

		return unitAction;

	}

	private Vec2Double closestWeapon(Unit me, Game game, Vec2Double closestWeapon) {
		for (LootBox lootBox : game.getLootBoxes()) {
			if (me.getWeapon() == null && lootBox.getItem() instanceof Item.Weapon) {
				if (teamMateUnit != null &&
						WorldUtils.VDC.compare(teamMateLoot.get(teamMateUnit.getId()), lootBox.getPosition()) == 0)
					continue;
				if (closestWeapon == null) closestWeapon = lootBox.getPosition();
				if (WorldUtils.distanceManhattan(new Vec2Int(lootBox.getPosition()), new Vec2Int(me.getPosition())) <
						WorldUtils.distanceManhattan(new Vec2Int(closestWeapon), new Vec2Int(me.getPosition()))) {
					closestWeapon = lootBox.getPosition();
				}
			}
		}
		return closestWeapon;
	}

	Vec2Double shootAt(Unit unit, List<Edge> destinationPath, Game game, Vec2Double opponent, double averageTileLength) {
		boolean shoot = unit.getWeapon() != null;
		Vec2Double myPosition = unit.getPosition();
		Tile[][] tiles = game.getLevel().getTiles();
		Vec2Double aim = ZERO;

		for (Edge edge : destinationPath)
			shoot &= WorldUtils.unitTile(edge.to, tiles) != Tile.WALL;

		if (shoot) {
			double minTilesToShot =
					getWeaponRadius(game.getProperties(), unit.getWeapon().getTyp()) / averageTileLength;
			double manhattanDistance = WorldUtils.distanceManhattan(new Vec2Int(opponent), new Vec2Int(myPosition));
			if (manhattanDistance < 8 && manhattanDistance > minTilesToShot)
				aim = new Vec2Double(opponent.getX() - myPosition.getX(), opponent.getY() - myPosition.getY());
		}

		return shoot ? aim : ZERO;
	}

	@Override
	public boolean feasible(World world, Unit me, Game game, Debug debug, Unit before) {
		for (Unit unit : game.getUnits()) {
			if (unit.getPlayerId() == me.getPlayerId() && unit.getId() != me.getId())
				this.teamMateUnit = unit;
		}
		return true;
	}

	private double getWeaponRadius(Properties properties, WeaponType weaponType) {
		WeaponParams weaponParams = properties.getWeaponParams().get(weaponType);
		switch (weaponType) {
			case ROCKET_LAUNCHER:
				return weaponParams.getExplosion().getRadius();
			default:
				return 0;
		}
	}

	private static boolean simpleReloadStrategy(Unit unit) {
		return unit.getWeapon() != null && unit.getWeapon().getMagazine() == 0;
	}
}

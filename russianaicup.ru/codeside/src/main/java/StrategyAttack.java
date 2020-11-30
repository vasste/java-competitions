import model.*;
import model.Properties;
import strategy.Action;
import strategy.Vec2Int;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.awt.*;
import java.util.*;
import java.util.List;

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
		// TODO weapon path is blocked by teammate
		Vec2Double closestWeapon = teamMateLoot.get(me.getId());
		if (closestWeapon == null && me.getWeapon() == null) {
			closestWeapon = closestWeapon(me, game, closestWeapon);
		} else if (closestWeapon != null && me.getWeapon() != null) {
			teamMateLoot.remove(me.getId());
			closestWeapon = null;
		} else {
			boolean weaponIsHere = false;
			for (LootBox lootBox : game.getLootBoxes()) {
				if (lootBox.getItem() instanceof Item.Weapon)
					weaponIsHere |= WorldUtils.VDC.compare(closestWeapon, lootBox.getPosition()) == 0;
			}
			if (!weaponIsHere) {
				teamMateLoot.remove(me.getId());
				closestWeapon = null;
			}
		}

		if (closestWeapon != null) {
			teamMateLoot.putIfAbsent(me.getId(), closestWeapon);
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
			aim = shootAt(me, game, destination, world.getAverageTileLength(), debug);
			shoot = WorldUtils.VDC.compare(aim, ZERO) != 0;
			direction = Math.signum(destination.getX() - me.getPosition().getX());
		}


		// TODO possible to dodge a bullet
		if (!destinationPath.isEmpty()) {
			if (shoot && destinationPath.size() < 8) {
				unitAction = new UnitAction(0,
						false,
						false, aim,
						shoot, simpleReloadStrategy(me),
						manager.isWeaponType(me, WeaponType.ROCKET_LAUNCHER) ||
								manager.isWeaponType(me, WeaponType.PISTOL), false);
			} else {
				Edge firstStride = destinationPath.iterator().next();
				direction = firstStride.toD.getX() - me.getPosition().getX();
				unitAction = new UnitAction(direction * Math.max(5, firstStride.maxSpeed),
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

	private List<Edge> findDodgePath() {

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

	Vec2Double shootAt(Unit me, Game game, Vec2Double opp, double averageTileLength, Debug debug) {
		boolean shoot = me.getWeapon() != null;
		if (!shoot)
			return ZERO;
		Tile[][] tiles = game.getLevel().getTiles();

		Vec2Double size = game.getProperties().getUnitSize();
		Vec2Double mePos = me.getPosition();
		Vec2Double weapPos = new Vec2Double(mePos.getX(), mePos.getY() + size.getY()/2);
		Vec2Double toShoot = new Vec2Double(opp.getX(), opp.getY() + size.getY()/2);
		double dX = (toShoot.getX() - weapPos.getX());
		double dY = (toShoot.getY() - weapPos.getY());
		double tan = Math.abs(dY/dX);

		if (StrategyAttackRecovery.debugEnabled)
			debug.draw(new CustomData.Line(WorldUtils.toFloat(weapPos), WorldUtils.toFloat(toShoot), .1f,
					DebugUtils.toColorFloat(Color.CYAN)));

		if (WorldUtils.distanceSqr(weapPos, toShoot) > 300)
			return ZERO;

		weapPos = new Vec2Double(mePos.getX() * -Math.signum(dX)*size.getX()/2, mePos.getY() + size.getY() / 2);
		while (WorldUtils.distanceSqr(weapPos, toShoot) > averageTileLength*averageTileLength) {
			weapPos = new Vec2Double(weapPos.getX() + Math.signum(dX), weapPos.getY() + Math.signum(dY)*tan);
			Vec2Int ij = new Vec2Int(weapPos);
			if (ij.x < 0 || ij.y < 0 || ij.x >= tiles.length || ij.y >= tiles[0].length || tiles[ij.x][ij.y] == Tile.WALL) {
				shoot = false;
				break;
			}
		}

		double minTilesToShot =
				getWeaponRadius(game.getProperties(), me.getWeapon().getTyp()) / averageTileLength;
		weapPos = new Vec2Double(mePos.getX(), mePos.getY() + size.getY() / 2);
		WeaponParams weaponParams = game.getProperties().getWeaponParams().get(me.getWeapon().getTyp());
		double avrSpread = (weaponParams.getMaxSpread() + weaponParams.getMinSpread())/2;
		shoot &= minTilesToShot*minTilesToShot < WorldUtils.distanceSqr(weapPos, toShoot) &&
				me.getWeapon().getSpread() <= avrSpread;
		return shoot  ? new Vec2Double(toShoot.getX() - weapPos.getX(), toShoot.getY() - weapPos.getY()) : ZERO;
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

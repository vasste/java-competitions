import model.*;
import strategy.Action;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.util.Collections;
import java.util.List;

public class StrategyAttack implements UnitStrategy {

	private Unit teamMateUnit;

	public UnitAction getUnitAction(World world, Unit me, Game game, Debug debug, Unit meBefore, StrategyAttackRecovery manager) {
		for (Unit unit : game.getUnits()) {
			if (unit.getPlayerId() == me.getPlayerId() && unit.getId() != me.getId())
				this.teamMateUnit = unit;
		}

		UnitAction unitAction = NO_ACTION;
		List<Edge> destinationPath = Collections.emptyList();
		Vec2Double destination = null;

		Level level = game.getLevel();
		Draw draw = new Draw(StrategyAttackRecovery.debugEnabled, world, level.getTiles());
		Path path = new Path(world);

		// find weapon
		// find opponent
		// attack
		for (LootBox lootBox : game.getLootBoxes()) {
			if (me.getWeapon() == null && lootBox.getItem() instanceof Item.Weapon)
				destinationPath = path.find(lootBox.getPosition());

			if (!destinationPath.isEmpty()) {
				destination = lootBox.getPosition();
				break;
			}
		}

		if (me.getWeapon() != null && destinationPath.isEmpty()) {
			Unit opponent = manager.findOpponent(me, game);
			if (opponent == null)
				opponent = manager.findOpponent(teamMateUnit, game);

			destination = opponent.getPosition();
			destinationPath = path.find(destination);
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
			Edge firstStride = destinationPath.iterator().next();
			direction = firstStride.toD.getX() - me.getPosition().getX();
			unitAction = new UnitAction(direction * Math.max(.8d, firstStride.maxSpeed),
					firstStride.action == Action.JUMP_UP,
					firstStride.action == Action.JUMP_DOWN, aim,
					shoot, simpleReloadStrategy(me),
					isWeaponType(me, WeaponType.ROCKET_LAUNCHER), false);
		} else {
			unitAction = new UnitAction(direction * velocity,
					false,
					false, aim,
					shoot, simpleReloadStrategy(me),
					isWeaponType(me, WeaponType.ROCKET_LAUNCHER), false);
		}

		return unitAction;

	}

	Vec2Double shootAt(Unit unit, List<Edge> destinationPath, Game game, Vec2Double opponent, double averageTileLength) {
		boolean shoot = unit.getWeapon() != null;
		Vec2Double myPosition = unit.getPosition();
		Tile[][] tiles = game.getLevel().getTiles();
		Vec2Double aim = ZERO;

		for (Edge edge : destinationPath)
			shoot &= WorldUtils.unitTile(edge.to, tiles) == Tile.EMPTY;

		if (shoot && destinationPath.size() < 4) {
			double minTilesToShot =
					getWeaponRadius(game.getProperties(), unit.getWeapon().getTyp())/averageTileLength;
			shoot = minTilesToShot <= destinationPath.size();
			aim = new Vec2Double(opponent.getX() - myPosition.getX(), opponent.getY() - myPosition.getY());
		}

		return shoot ? aim : ZERO;
	}

	@Override
	public boolean feasible(World world, Unit unit, Game game, Debug debug, Unit before) {
		return true;
	}

	private boolean isWeaponType(Unit unit, WeaponType weaponType) {
		return unit.getWeapon() != null && unit.getWeapon().getTyp() == weaponType;
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

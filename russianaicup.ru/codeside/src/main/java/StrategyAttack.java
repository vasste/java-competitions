import model.*;
import strategy.Action;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.util.Collections;
import java.util.List;

public class StrategyAttack implements UnitStrategy {

	private boolean debugEnabled = true;
	private Unit teamMateUnit;

	public UnitAction getUnitAction(World world, Unit me, Game game, Debug debug, Unit meBefore, StrategyAttackRecovery manager) {
		for (Unit unit : game.getUnits()) {
			if (unit.getPlayerId() == me.getPlayerId() && unit.getId() != me.getId())
				this.teamMateUnit = unit;
		}

		int tick = game.getCurrentTick();
		UnitAction unitAction = NO_ACTION;
		List<Edge> destinationPath = Collections.emptyList();
		Vec2Double destination = null;

		Level level = game.getLevel();
		Draw draw = new Draw(debugEnabled, world, level.getTiles());
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

		if (destinationPath.isEmpty()) {
			Unit opponent = manager.findOpponent(me, game);
			if (opponent != null)
				destination = opponent.getPosition();
			destinationPath = path.find(destination);
		}

		draw.paths(destinationPath, debug);
		double direction = 0;
		double velocity = game.getProperties().getUnitMaxHorizontalSpeed();
		boolean shoot = false;
		Vec2Double aim = ZERO;
		if (destination != null) {
			aim = shootAt(me, destinationPath, game, destination, world.getAverageTileLength());
			shoot = WorldUtils.VDC.compare(aim, ZERO) != 0;
			direction = Math.signum(destination.getX() - me.getPosition().getX());
		}
		if (!destinationPath.isEmpty()) {
			Edge firstStride = destinationPath.iterator().next();
			velocity = firstStride.action.jump() ? game.getProperties().getJumpPadJumpSpeed() : firstStride.maxSpeed;
			double pathDirection = 0;
			for (Edge edge : destinationPath) {
				pathDirection = edge.horzDirection();
				if (pathDirection != 0)
					break;
			}
			direction = firstStride.horzDirection() == 0 ? direction : pathDirection;
			unitAction = new UnitAction(direction * velocity,
					firstStride.action == Action.JUMP_UP || (shoot && tick % 2 == 0),
					firstStride.action == Action.JUMP_DOWN, aim,
					false, simpleReloadStrategy(me),
					isWeaponType(me, WeaponType.ROCKET_LAUNCHER), false);
		} else {
			unitAction = new UnitAction(direction * velocity,
					false,
					false, aim,
					false, simpleReloadStrategy(me),
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
			shoot &= WorldUtils.unitTile(edge.to, tiles) == Tile.EMPTY && !isPartnerInLine(myPosition, opponent);

		if (shoot) {
			double minTilesToShot =
					getWeaponRadius(game.getProperties(), unit.getWeapon().getTyp())/averageTileLength;
			shoot = minTilesToShot <= destinationPath.size();
			aim = new Vec2Double(opponent.getX() - myPosition.getX(), opponent.getY() - myPosition.getY());
		}

		return shoot ? aim : ZERO;
	}

	// TODO might be incorrect
	private boolean isPartnerInLine(Vec2Double me, Vec2Double opponent) {
		if (teamMateUnit == null)
			return false;
		Vec2Double partnerPos = teamMateUnit.getPosition();
		double toPartnerX = Math.signum(me.getX() - partnerPos.getX());
		double toOpponentX = Math.signum(me.getX() - opponent.getX());
		double toPartnerY = Math.signum(me.getY() - partnerPos.getY());
		double toOpponentY = Math.signum(me.getY() - opponent.getY());
		return toPartnerX == toOpponentX ^ toPartnerY == toOpponentY;
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

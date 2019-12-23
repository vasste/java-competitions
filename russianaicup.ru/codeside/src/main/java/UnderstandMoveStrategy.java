import model.Properties;
import model.*;
import strategy.Action;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.util.*;

public class UnderstandMoveStrategy {

    private final boolean debugEnabled = false;
    private final Vec2Double ZERO = new Vec2Double(0, 0);
	private final UnitAction NO_ACTION = new UnitAction(0, false, false, ZERO, false,
			false, false, false);

	private Map<Integer, Integer> opponents = new HashMap<>();
	private UnitStrategy[] strategies;
	private Set<Integer> team = new HashSet<>();
	private int playerId;

	public UnderstandMoveStrategy() {
		strategies = new UnitStrategy[]{new UnitStrategy(), new UnitStrategy()};
		opponents = new HashMap<>();
	}

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		playerId = playerId == 0 ? unit.getPlayerId() : playerId;
		UnitStrategy strategy = initGame(unit, game);
		team.add(unit.getId());
		for (Unit gameUnit : game.getUnits()) {
			boolean teamMate = playerId == gameUnit.getPlayerId();
			if (teamMate)
				team.add(gameUnit.getId());
			if (teamMate && gameUnit.getId() != unit.getId()) {
				strategy.partner = gameUnit;
				break;
			}
		}
		return strategy.getUnitAction(unit, game, debug);
	}

	class UnitStrategy {
		private Unit unitInTick;
		private int tick;
		private Unit partner;

		UnitAction getUnitAction(Unit unit, Game game, Debug debug) {
 			initMove(unit, game);
			UnitAction unitAction = NO_ACTION;

			JumpState state = unit.getJumpState();
			double horzUnitSpeed = Math.abs(unit.getPosition().getX() - unitInTick.getPosition().getX());
			double vertUnitSpeed = Math.abs(unit.getPosition().getY() - unitInTick.getPosition().getY());
			Vec2Double unitSpeed = new Vec2Double(horzUnitSpeed, Math.max(state.getSpeed(), vertUnitSpeed));

			debug.draw(DebugUtils.write(horzUnitSpeed + "", 1, 10));
			debug.draw(DebugUtils.write(vertUnitSpeed + "", 1, 11f));

			Level level = game.getLevel();
			Tile[][] tiles = level.getTiles();

			World world = new World(unit.getPosition(), unitSpeed, tiles, game.getProperties(), 50,
					partner == null ? null : partner.getPosition(), debugEnabled);
			Draw draw = new Draw(debugEnabled, world, level.getTiles());
			Path path = new Path(world);
			List<Edge> destinationPath = Collections.emptyList();

			Vec2Double destination = null;

			for (LootBox lootBox : game.getLootBoxes()) {
				if (unit.getWeapon() == null && !isWeaponType(lootBox, WeaponType.ROCKET_LAUNCHER))
					destinationPath = path.find(lootBox.getPosition());

				if (!destinationPath.isEmpty()) {
					destination = lootBox.getPosition();
					break;
				}
			}

			if (destinationPath.isEmpty()) {
				Unit opponent = findOpponent(unit, game);
				destination = opponent.getPosition();
				if (destination != null && destinationPath.isEmpty())
					destinationPath = path.find(destination);
			}


			draw.paths(destinationPath, debug);
			if (!destinationPath.isEmpty() && destination != null) {
				Edge firstStride = destinationPath.iterator().next();
				Vec2Double aim = shootAt(unit, destinationPath, game, destination, world.getAverageTileLength());
				double velocity = firstStride.action.jump() ? game.getProperties().getJumpPadJumpSpeed() : firstStride.maxSpeed;
				double direction = Math.signum(destination.getX() - unit.getPosition().getX());
				unitAction = new UnitAction(direction * velocity,
						firstStride.action == Action.JUMP_UP,
						firstStride.action == Action.JUMP_DOWN, aim,
						WorldUtils.VDC.compare(aim, ZERO) != 0, simpleReloadStrategy(unit),
						isWeaponType(unit, WeaponType.ROCKET_LAUNCHER), false);
			}

			saveMove(unit, game);
			return unitAction;

		}

		Unit findOpponent(Unit me, Game game) {
			Integer opponentPlayerId = opponents.get(me.getId());
			for (Unit gameUnit : game.getUnits()) {
				if (!team.contains(gameUnit.getId())) {
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

		private boolean isPartnerInLine(Vec2Double me, Vec2Double opponent) {
			if (partner == null)
				return false;
			Vec2Double partnerPos = partner.getPosition();
			double toPartnerX = Math.signum(me.getX() - partnerPos.getX());
			double toOpponentX = Math.signum(me.getX() - opponent.getX());
			double toPartnerY = Math.signum(me.getY() - partnerPos.getY());
			double toOpponentY = Math.signum(me.getY() - opponent.getY());
			return toPartnerX == toOpponentX ^ toPartnerY == toOpponentY;
		}

		private void initMove(Unit unit, Game game) {
			if (tick == 0)
				tick = game.getCurrentTick();
			if (unitInTick == null)
				this.unitInTick = unit;
		}

		private void saveMove(Unit unit, Game game) {
			tick = game.getCurrentTick();
			this.unitInTick = unit;
		}
	}

	private boolean isWeaponType(LootBox lootBox, WeaponType weaponType) {
		return lootBox.getItem() instanceof Item.Weapon && ((Item.Weapon)lootBox.getItem()).getWeaponType() == weaponType;
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

	private UnitStrategy initGame(Unit me, Game game) {
		Unit[] units = game.getUnits();
		for (int i = 0; i < units.length; i++) {
			if (me.getPlayerId() == units[i].getPlayerId()) {
				return strategies[i];
			}
		}
		return null;
	}


	private static boolean simpleReloadStrategy(Unit unit) {
  		return unit.getWeapon() != null && unit.getWeapon().getMagazine() == 0;
	}
}

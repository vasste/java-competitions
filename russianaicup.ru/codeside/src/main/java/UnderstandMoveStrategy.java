import model.*;
import strategy.Action;
import strategy.world.Edge;
import strategy.world.Path;
import strategy.world.World;
import strategy.world.WorldUtils;

import java.util.Collections;
import java.util.List;

public class UnderstandMoveStrategy {

    private final boolean debugEnabled = true;
    private final Vec2Double ZERO = new Vec2Double(0, 0);
	private final UnitAction NO_ACTION = new UnitAction(0, false, false, ZERO, false, false, false, false);
    private Unit unitInTick;
	private int tick;
	private UnitAction unitAction;

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		initMove(unit, game);
		unitAction = NO_ACTION;

		double ticksInSec = game.getProperties().getTicksPerSecond();
		double horzUnitSpeed = Math.abs(unit.getPosition().getX() - unitInTick.getPosition().getX());
		double vertUnitSpeed = Math.abs(unit.getPosition().getY() - unitInTick.getPosition().getY());
		Vec2Double unitSpeed = new Vec2Double(horzUnitSpeed, vertUnitSpeed);

		debug.draw(DebugUtils.write(horzUnitSpeed + "", 1, 10));
		debug.draw(DebugUtils.write(vertUnitSpeed + "", 1, 11f));

		Level level = game.getLevel();
		World world = new World(unit.getPosition(), unitSpeed,
				level.getTiles(), game.getProperties(), 30, debugEnabled);
		Draw draw = new Draw(debugEnabled, world, level.getTiles());
		Path path = new Path(world);
		List<Edge> edgeList = Collections.emptyList();

		for (LootBox lootBox : game.getLootBoxes()) {
			if (unit.getWeapon() == null && lootBox.getItem() instanceof Item.Weapon)
			 	edgeList = path.find(lootBox.getPosition());
			 if (!edgeList.isEmpty())
			 	break;
		}

		Unit opponent = DebugUtils.findOpponent(unit, game);
		boolean shoot = unit.getWeapon() != null;
		if (edgeList.isEmpty())
			edgeList = path.find(opponent.getPosition());

		draw.paths(edgeList, debug);
		if (!edgeList.isEmpty()) {
			Edge firstStride = edgeList.iterator().next();
			for (Edge edge : edgeList)
				shoot &= WorldUtils.unitTile(edge.to, game.getLevel().getTiles()) == Tile.EMPTY;

			Vec2Double aim = ZERO;
			if (shoot) {
				aim = opponent.getPosition();
				aim = new Vec2Double(firstStride.horzDirection() * aim.getX(), aim.getY() - opponent.getSize().getY());
			}

			double velocity = firstStride.action.jump() ? game.getProperties().getJumpPadJumpSpeed() : firstStride.maxSpeed;
			double direction = firstStride.horzDirection();
			if (firstStride.action.jump())
			  	direction = direction == 0 ? firstStride.vertDirection() : direction;

			unitAction = new UnitAction(direction * velocity,
					firstStride.action == Action.JUMP_UP,
					firstStride.action == Action.JUMP_DOWN, aim,
					shoot, simpleReloadStrategy(unit), false, false);
		}

		saveMove(unit, game);
		return unitAction;
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

	private static boolean simpleReloadStrategy(Unit unit) {
  		return unit.getWeapon() != null && unit.getWeapon().getMagazine() == 0;
	}
}

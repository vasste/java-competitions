import model.*;

import java.util.List;

public class UnderstandMoveStrategy {
	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		Level level = game.getLevel();
		Tile unitTile = unitTile(unit.getPosition(), level.getTiles());
		LootBox loot = findClosest(unit, game.getLootBoxes());
		return walk(xL(loot) - xU(unit), new Vec2Double(0, 0));
	}

	UnitAction walk(double velocity, Vec2Double aim) {
		return new UnitAction(velocity, false, false, aim, false, false, false);
	}

	List<Tile> buildPathFromTo(Unit unit, LootBox loot, Tile[][] tiles) {
		Tile unitTile = unitTile(unit, tiles);
		double directionX = Math.signum(xL(loot) - xU(unit));
		if (directionX > 0) {

		} else {

		}
	}

	LootBox findClosest(Unit unit, LootBox[] boxes) {
		double ux = xU(unit);
		int minIx = 0;
		double minX = Integer.MAX_VALUE;
		for (int i = 0; i < boxes.length; i++) {
			double x = xL(boxes[i]);
			if (Math.abs(x - ux) < minX) {
				minX = Math.abs(x - ux);
				minIx = i;
			}
		}
		return boxes[minIx];
	}

	static Tile vecTile(Vec2Double vec2, Tile[][] level) {
		return level[(int)vec2.getX()][(int)vec2.getY()];
	}

	static Tile unitTile(Unit unit, Tile[][] level) {
		return vecTile(unit.getPosition(), level);
	}

	static double xU(Unit obj) {
		return obj.getPosition().getX();
	}

	static double yU(Unit obj) {
		return obj.getPosition().getY();
	}

	static double xL(LootBox obj) {
		return obj.getPosition().getX();
	}

	static double yL(LootBox obj) {
		return obj.getPosition().getY();
	}
}

import model.*;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class UnderstandMoveStrategy {

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		Level level = game.getLevel();
		//LootBox loot = findClosest(unit, game.getLootBoxes());
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream("level");
			level.writeTo(stream);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		World world = new World(unit.getPosition(), level.getTiles(), game.getProperties());
		world.drawPaths(debug);
		Tile[][] tiles = game.getLevel().getTiles();

		for (int i = 0; i < tiles.length; i++) {
			debug.draw(new CustomData.Line(new Vec2Float(i, 0), new Vec2Float(i, tiles[i].length), .1f,
					Utils.toColorFloat(Color.BLACK)));
		}
		for (int i = 0; i <= tiles[0].length; i++) {
			debug.draw(new CustomData.Line(new Vec2Float(0, i), new Vec2Float(tiles.length, i), .1f,
					Utils.toColorFloat(Color.BLACK)));
		}
		//Tile tile = Utils.unitTile(unit.getPosition(), level);
		return walk(1, new Vec2Double(0, 0));
	}

	UnitAction walk(double velocity, Vec2Double aim) {
		return new UnitAction(velocity, false, false, aim, false, false, false);
	}

	LootBox findClosest(Unit unit, LootBox[] boxes) {
		double ux = Utils.xU(unit);
		int minIx = 0;
		double minX = Integer.MAX_VALUE;
		for (int i = 0; i < boxes.length; i++) {
			double x = Utils.xL(boxes[i]);
			if (Math.abs(x - ux) < minX) {
				minX = Math.abs(x - ux);
				minIx = i;
			}
		}
		return boxes[minIx];
	}
}

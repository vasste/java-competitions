import model.*;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class UnderstandMoveStrategy {

    private final boolean debugEnabled = false;

	public UnitAction getAction(Unit unit, Game game, Debug debug) {
		Level level = game.getLevel();
		World world = new World(unit.getPosition(), level.getTiles(), game.getProperties(), debugEnabled);
		world.drawPaths(debug);
        drawGrid(debug, game);
        LootBox loot = findClosest(unit, game.getLootBoxes());

		return walk(2, new Vec2Double(0, 0));
	}

    private void drawGrid(Debug debug, Game game) {
	    if (!debugEnabled)
	        return;
        Tile[][] tiles = game.getLevel().getTiles();
        for (int i = 0; i < tiles.length; i++) {
            debug.draw(new CustomData.Line(new Vec2Float(i, 0), new Vec2Float(i, tiles[i].length), .1f,
                    Utils.toColorFloat(Color.BLACK)));
        }
        for (int i = 0; i <= tiles[0].length; i++) {
            debug.draw(new CustomData.Line(new Vec2Float(0, i), new Vec2Float(tiles.length, i), .1f,
                    Utils.toColorFloat(Color.BLACK)));
        }
    }

    UnitAction walk(double velocity, Vec2Double aim) {
		return new UnitAction(velocity, true, false, aim, false, false, false, false);
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

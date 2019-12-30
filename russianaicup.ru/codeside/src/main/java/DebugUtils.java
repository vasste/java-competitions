import model.*;
import strategy.Vec2Int;
import strategy.world.World;

import java.awt.*;

import static strategy.world.WorldUtils.toFloat;

public class DebugUtils {
	static ColorFloat toColorFloat(Color color) {
		return new ColorFloat(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	public static void drawGrid(Debug debug, Game game, boolean debugEnabled) {
		if (!debugEnabled)
			return;
		Tile[][] tiles = game.getLevel().getTiles();
		for (float i = 1.0f; i < tiles.length; i++) {
			debug.draw(new CustomData.Line(new Vec2Float(i, 0), new Vec2Float(i, tiles[(int)i].length), .1f,
					DebugUtils.toColorFloat(Color.BLACK)));
		}
		for (int i = 0; i <= tiles[0].length; i++) {
			debug.draw(new CustomData.Line(new Vec2Float(0, i), new Vec2Float(tiles.length, i), .1f,
					DebugUtils.toColorFloat(Color.BLACK)));
		}
	}

	static CustomData.Line createLine(World.TilePoint from, World.TilePoint to) {
		return new CustomData.Line(toFloat(new Vec2Int(from)), toFloat(new Vec2Int(to)), .2f, toColorFloat(Color.GREEN));
	}

	static CustomData.PlacedText write(String text, float x, float y) {
		return new CustomData.PlacedText(text, new Vec2Float(x, y), TextAlignment.CENTER, 20, toColorFloat(Color.RED));
	}
}

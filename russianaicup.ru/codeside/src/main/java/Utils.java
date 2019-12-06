import model.*;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;

public class Utils {
	static Comparator<Vec2Double> VC =
			Comparator.comparingDouble(Vec2Double::getX).thenComparingDouble(Vec2Double::getY);

	static Vec2Double left(Vec2Double from) {
		return new Vec2Double(xU(from, -1), from.getY());
	}

	static Vec2Double right(Vec2Double from) {
		return new Vec2Double(xU(from, 1), from.getY());
	}

	static Vec2Double up(Vec2Double from) {
		return new Vec2Double(from.getX(), yU(from, 1));
	}

	static Vec2Double down(Vec2Double from) {
		return new Vec2Double(from.getX(), yU(from, -1));
	}

	static Tile unitTile(Vec2Double unit, Tile[][] tiles) {
		return tiles[(int)xU(unit, 0)][(int)yU(unit, 0)];
	}

	static ColorFloat toColorFloat(Color color) {
		return new ColorFloat(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
	}

	static Vec2Float toFloat(Vec2Double vec2Double) {
		return new Vec2Float((float)vec2Double.getX(), (float)vec2Double.getY());
	}

	static double xU(Unit obj) {
		return obj.getPosition().getX();
	}

	static double xU(Vec2Double obj, double stride) {
		return Math.max(0, obj.getX() + stride);
	}

	static double yU(Vec2Double obj, double stride) {
		return Math.max(0, obj.getY() + stride);
	}

	static double xUr(Unit obj) {
		return Math.max(0, obj.getPosition().getX() + 1);
	}

	static double yU(Vec2Double obj) {
		return obj.getY();
	}

	static double xL(LootBox obj) {
		return obj.getPosition().getX();
	}

	static Tile[][] readTiles(String fileName) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(fileName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

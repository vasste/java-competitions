import model.*;

import java.awt.*;
import java.util.List;
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

	static Vec2Double jump(Vec2Double from, int height) {
		return new Vec2Double(from.getX(), yU(from, height));
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

	static void print(char[][] level) {
		for (int j = level[1].length - 1; j >= 0 ; j--) {
			for (int i = 0; i < level.length; i++) {
				System.out.print(level[i][j]);
			}
			System.out.println();
		}
	}

	static char[][] fromTiles(Tile[][] tiles) {
		char[][] level = new char[tiles.length][];
		for (int i = 0; i < tiles.length; i++) {
			level[i] = new char[tiles[i].length];
			for (int j = 0; j < tiles[i].length; j++) {
				switch (tiles[i][j]) {
					case EMPTY:
						level[i][j] = '.';
						break;
					case WALL:
						level[i][j] = '#';
						break;
					case PLATFORM:
						level[i][j] = '^';
						break;
					case LADDER:
						level[i][j] = 'H';
						break;
				}
			}
		}
		return level;
	}

	static Tile[][] readTiles(String fileName) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(fileName)); // y
			int xl = lines.get(0).length();
			int yl = lines.size();
			Tile[][] tiles = new Tile[xl][yl];
			int y = 0;
			for (int j = lines.size() - 1; j >= 0; j--) {
				String line = lines.get(j);
				for (int i = 0; i < line.length(); i++) {
					Tile tile = Tile.EMPTY;
					switch (line.charAt(i)) {
						case '.':
							tile = Tile.EMPTY;
							break;
						case 'H':
							tile = Tile.LADDER;
							break;
						case '^':
							tile = Tile.PLATFORM;
							break;
						case '#':
							tile = Tile.WALL;
							break;
					}
					tiles[i][y] = tile;
				}
				y++;
			}
			return tiles;
		} catch (IOException e) {
			return null;
		}
	}
}

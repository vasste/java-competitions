package strategy.world;

import model.*;
import strategy.Vec2Int;

import java.util.Arrays;
import java.util.Comparator;

public class WorldUtils {
	public static boolean[][] startDetour(Object[][] tiles) {
		boolean[][] visited = new boolean[tiles.length][];
		for (int i = 0; i < tiles.length; i++)
			visited[i] = new boolean[tiles[i].length];
		return visited;
	}

	public static double[][] startDetourValues(Object[][] tiles, int fillInValue) {
		double[][] visited = new double[tiles.length][];
		for (int i = 0; i < tiles.length; i++) {
			visited[i] = new double[tiles[i].length];
			Arrays.fill(visited[i], fillInValue);
		}
		return visited;
	}

	public static double distanceSqr(Vec2Double a, Vec2Double b) {
		return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
	}

	public static int distanceManhattan(Vec2Int a, Vec2Int b) {
		return Math.abs(a.x - b.x) + Math.abs(a.y - b.y);
	}

	public static Comparator<Vec2Double> VDC =
			Comparator.comparingDouble(Vec2Double::getX).thenComparingDouble(Vec2Double::getY);

	public static Vec2Int left(Vec2Int from, int stride) {
		return new Vec2Int(xU(from, -stride), from.y);
	}

	public static Vec2Int left(Vec2Int from) {
		return left(from, 1);
	}

	public static Vec2Int right(Vec2Int from, int stride) {
		return new Vec2Int(xU(from, stride), from.y);
	}

	public static Vec2Int right(Vec2Int from) {
		return right(from, 1);
	}

	public static Vec2Int up(Vec2Int from) {
		return new Vec2Int(from.x, yU(from, 1));
	}

	public static Vec2Int jump(Vec2Int from, int height) {
		return new Vec2Int(from.x, yU(from, height));
	}

	public static Vec2Double changeX(Vec2Double a, double x) {
		return new Vec2Double(a.getX() + x, a.getY());
	}

	public static Vec2Int down(Vec2Int from) {
		return new Vec2Int(from.x, yU(from, -1));
	}

//	public static Tile unitTile(Vec2Double unit, Tile[][] tiles) {
//		return tiles[(int) xU(unit, 0)][(int) yU(unit, 0)];
//	}

	public static Tile unitTile(Vec2Int unit, Tile[][] tiles) {
		return tiles[xU(unit, 0)][yU(unit, 0)];
	}

	public static Vec2Float toFloat(Vec2Double vec2Double) {
		return new Vec2Float((float)vec2Double.getX(), (float)vec2Double.getY());
	}

	public static Vec2Float toFloat(Vec2Int vec2Double) {
		return new Vec2Float((float)vec2Double.x, (float)vec2Double.y);
	}

	public static double xU(Vec2Double position) {
		return position.getX();
	}

//	public static double xU(Vec2Double obj, double stride) {
//		return Math.max(0, obj.getX() + stride);
//	}

	public static int xU(Vec2Int obj, int stride) {
		return Math.max(0, obj.x + stride);
	}

	public static int yU(Vec2Int obj, int stride) {
		return Math.max(0, obj.y + stride);
	}

//	public static double yU(Vec2Double obj, int stride) {
//		return Math.max(0, obj.getY() + stride);
//	}
//
//	public static double xUr(Unit obj) {
//		return Math.max(0, obj.getPosition().getX() + 1);
//	}
//
//	public static double yU(Vec2Double obj) {
//		return obj.getY();
//	}

	public static double xL(LootBox obj) {
		return obj.getPosition().getX();
	}
}
import model.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class World {

	private Tile[][] tiles;
	private TilePoint start;
	int jumpHeight;
	int jumpPadHeight;
	int maxSpeed;
	boolean debugEnabled;

	public World(Vec2Double unit, Tile[][] tiles, Properties properties, boolean debug) {
		this.tiles = tiles;
		this.jumpPadHeight = (int)(properties.getJumpPadJumpSpeed()*properties.getJumpPadJumpTime());
		this.jumpHeight = (int)(properties.getUnitJumpSpeed()*properties.getUnitJumpTime());
		this.maxSpeed = 10;
		this.debugEnabled = debug;
		start = buildPaths(unit);
	}

	void drawPaths(Debug debug) {
		drawPaths(start, debug);
	}

	void drawPaths(char[][] debug) {
		drawPaths(start, debug);
	}

	private void drawPaths(TilePoint from, char[][] level) {
		boolean[][] visited = startDetour();
		if (!debugEnabled)
			return;
		for (TilePoint point : from.reached) {
			if (!visited[point.x()][point.y()]) {
				visited[point.x()][point.y()] = true;
				if (point.y() == from.point.getY())
					level[from.x()][point.y()] = '-';
				if (point.x() == from.point.getX())
					level[from.x()][point.y()] = '|';
				drawPaths(point, level);
			}
		}
	}

	private void drawPaths(TilePoint from, Debug debug) {
		if (!this.debugEnabled)
			return;
		boolean[][] visited = startDetour();
		for (TilePoint point : from.reached) {
			if (!visited[point.x()][point.y()]) {
				visited[point.x()][point.y()] = true;
				debug.draw(createLine(from, point));
				drawPaths(point, debug);
			}
		}
	}

	private List<TilePoint> findPath(Vec2Double to) {
		boolean[][] visited = startDetour();
		Queue<TilePoint> tilePoints = new LinkedList<>();
		tilePoints.add(start);
		while (!tilePoints.isEmpty()) {
			TilePoint from = tilePoints.remove();
			if (Utils.VC.compare(to, from.point) == 0)
				return null;
			if (!visited[from.x()][from.y()]) {
				visited[from.x()][from.y()] = true;
				tilePoints.addAll(from.reached);
			}
		}
	}

	// bfs
	private TilePoint buildPaths(Vec2Double unit) {
		TilePoint startPoint = new TilePoint(unit);
		Queue<TilePoint> tilePoints = new LinkedList<>();
		tilePoints.add(startPoint);

		boolean[][] visited = startDetour();
		while (!tilePoints.isEmpty()) {
			TilePoint from = tilePoints.remove();
			if (!visited[from.x()][from.y()]) {
				visited[from.x()][from.y()] = true;
				Tile tileBelowUnit = Utils.unitTile(Utils.down(from.point), tiles);
				Tile unitTile = Utils.unitTile(from.point, tiles);
				switch (unitTile) {
					case LADDER:
						for (int i = 1; i <= jumpHeight; i++)
							addPoint(tilePoints, from, Utils.jump(from.point, i), Action.JUMP_UP);
						for (int i = 1; i <= jumpHeight; i++)
							addPoint(tilePoints, from, Utils.jump(from.point, -i), Action.JUMP_DOWN);
						break;
					case JUMP_PAD:
						for (int i = 1; i <= jumpPadHeight; i++)
							addPoint(tilePoints, from, Utils.jump(from.point, i), Action.JUMP_UP);
						break;
					case EMPTY:
						if (tileBelowUnit == Tile.EMPTY) {
							int stride = 1;
							while (addPoint(tilePoints, from, new Vec2Double(from.x(),
									Utils.yU(from.point, -stride++)), Action.FALL));
						}
				}
				switch (tileBelowUnit) {
					case LADDER:
						for (int i = 1; i <= jumpHeight; i++)
							addPoint(tilePoints, from, Utils.jump(from.point, i), Action.JUMP_UP);
						for (int i = 1; i <= jumpHeight; i++)
							addPoint(tilePoints, from, Utils.jump(from.point, -i), Action.JUMP_DOWN);
					case PLATFORM:
					case WALL:
						addPoint(tilePoints, from, Utils.left(from.point), Action.WALK);
						addPoint(tilePoints, from, Utils.right(from.point), Action.WALK);
						for (int i = 1; i <= jumpHeight; i++)
							addPoint(tilePoints, from, Utils.jump(from.point, i), Action.JUMP_UP);
						// maxSpeed = 1
						addPoint(tilePoints, from, Utils.up(Utils.left(from.point)), Action.WALK);
						addPoint(tilePoints, from, Utils.up(Utils.right(from.point)), Action.WALK);
				}
			}
		}
		return startPoint;
	}

	private boolean[][] startDetour() {
		boolean[][] visited = new boolean[tiles.length][];
		for (int i = 0; i < tiles.length; i++)
			visited[i] = new boolean[tiles[i].length];
		return visited;
	}

	private boolean addPoint(Queue<TilePoint> tilePoints, TilePoint from, Vec2Double to, Action action) {
		if (tiles.length <= to.getX() || tiles[0].length <= to.getY() || to.getX() < 0 || to.getY() < 0)
			return false;
		Tile unitTile = Utils.unitTile(to, tiles);
		switch (unitTile) {
			case WALL:
			case PLATFORM:
				return false;
		}
		if (Utils.VC.compare(to, from.point) != 0) {
			TilePoint point = new TilePoint(to);
			point.action = action;
			from.reached.add(point);
			tilePoints.add(point);
			return true;
		}
		return false;
	}

	static CustomData.Line createLine(TilePoint from, TilePoint to) {
		return new CustomData.Line(Utils.toFloat(from.point), Utils.toFloat(to.point), .2f, Utils.toColorFloat(Color.GREEN));
	}

	static class TilePoint {
		TilePoint(Vec2Double point) {
			this.point = point;
		}

		boolean visited(boolean[][] visited) {
			return visited[(int)point.getX()][(int)point.getY()];
		}

		int x() {
			return (int)point.getX();
		}

		int y() {
			return (int)point.getY();
		}

		@Override
		public String toString() {
			return "[" + x() + "," + y() + "]";
		}

		Vec2Double point;
		Action action;
		List<TilePoint> reached = new ArrayList<>();
	}
}

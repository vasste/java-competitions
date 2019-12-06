import model.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class World {

	private Vec2Double unit;
	private Tile[][] tiles;
	private TilePoint start;
	double jumpHeight;
	double jumpPadHeight;

	public World(Vec2Double unit, Tile[][] tiles, Properties properties) {
		this.unit = unit;
		this.tiles = tiles;
		this.jumpPadHeight = properties.getJumpPadJumpSpeed()*properties.getJumpPadJumpTime();
		this.jumpHeight = properties.getUnitJumpSpeed()*properties.getUnitJumpTime();
		start = buildPaths(unit);
	}

	void drawPaths(Debug debug) {
		drawPaths(start, debug);
	}

	private void drawPaths(TilePoint from, Debug debug) {
		boolean[][] visited = startDetour();
		for (TilePoint point : from.reached) {
			if (!visited[point.x()][point.y()]) {
				visited[point.x()][point.y()] = true;
				debug.draw(createLine(from, point));
				drawPaths(point, debug);
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
				Tile unitTile = Utils.unitTile(Utils.down(from.point), tiles);
				switch (unitTile) {
					case LADDER:
						addPoint(tilePoints, from, Utils.up(from.point));
						addPoint(tilePoints, from, Utils.down(from.point));
						break;
					case JUMP_PAD:
						addPoint(tilePoints, from, Utils.up(from.point));
						break;
					case EMPTY:
						if (tileBelowUnit == Tile.PLATFORM)
							addPoint(tilePoints, from, Utils.up(from.point));
				}
				addPoint(tilePoints, from, Utils.left(from.point));
				addPoint(tilePoints, from, Utils.right(from.point));
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

	private void addPoint(Queue<TilePoint> tilePoints, TilePoint from, Vec2Double to) {
		Tile unitTile = Utils.unitTile(to, tiles);
		switch (unitTile) {
			case WALL:
			case PLATFORM:
				return;
		}
		if (Utils.VC.compare(to, from.point) != 0) {
			TilePoint point = new TilePoint(to);
			from.reached.add(point);
			tilePoints.add(point);
		}
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
		List<TilePoint> reached = new ArrayList<>();
	}
}

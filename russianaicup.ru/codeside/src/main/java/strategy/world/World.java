package strategy.world;

import model.*;
import model.Properties;
import strategy.Action;
import strategy.Vec2Int;

import javax.rmi.CORBA.Util;
import java.util.*;

public class World {

	public Tile[][] tiles;
	public TilePoint[][] tilePoints;
	public int startX;
	public int startY;
	public int jumpHeight;
	public int jumpPadHeight;
	public int maxHorizontalSpeed;
	public boolean debugEnabled;
	public int V;
	public int maxDistance;

	double maxSpeedStride = 1;
	double minSpeedStride = .5;
	double averageTileLength;

	public World(Vec2Double unit, Vec2Double unitSpeed, Tile[][] tiles, Properties properties, boolean debug) {
		this(unit, unitSpeed, tiles, properties, Integer.MAX_VALUE, debug);
	}

	public World(Vec2Double unit, Vec2Double unitSpeed, Tile[][] tiles, Properties properties,
				 int maxDistance, boolean debug) {
		this.tiles = tiles;
		this.jumpPadHeight = (int)(properties.getJumpPadJumpSpeed()*properties.getJumpPadJumpTime());
		this.jumpHeight = (int)(properties.getUnitJumpSpeed()*properties.getUnitJumpTime());
		this.averageTileLength = properties.getUnitSize().getY()/1.5; // 2 tiles
		this.maxHorizontalSpeed = 5;
		this.debugEnabled = debug;
		this.maxDistance = maxDistance;
		buildPaths(unit, unitSpeed);
	}

	// bfs
	private TilePoint buildPaths(Vec2Double unit, Vec2Double unitSpeed) {
		tilePoints = new TilePoint[tiles.length][];
		for (int i = 0; i < tiles.length; i++)
			tilePoints[i] = new TilePoint[tiles[i].length];

		Queue<TilePoint> queue = new LinkedList<>();
		TilePoint startPoint = new TilePoint(new Vec2Int(unit), 0);
		startX = startPoint.x;
		startY = startPoint.y;
		tilePoints[startX][startY] = startPoint;
		queue.add(startPoint);

//		Vec2Int unitStart = new Vec2Int(unit);
//		int distanceInTiles = (int) (unitSpeed.getY() / averageTileLength);
//		for (int i = 1; i <= Math.abs(distanceInTiles); i++)
//			addEdge(queue, unitStart, WorldUtils.jump(unitStart, (int)(Math.signum(distanceInTiles) * i)),
//					distanceInTiles > 0 ? Action.JUMP_UP : Action.FALL, 0, unitSpeed.getX());

		Vec2Double startSpeed = unitSpeed;
		boolean[][] visited = WorldUtils.startDetour(tiles);
		while (!queue.isEmpty()) {
			TilePoint fromPoint = queue.remove();
			Vec2Int from = new Vec2Int(fromPoint);
			if (!visited[from.x][from.y]) {
				visited[from.x][from.y] = true;
				Tile tileBelowUnit = WorldUtils.unitTile(WorldUtils.down(from), tiles);
				Tile unitTile = WorldUtils.unitTile(from, tiles);
				switch (unitTile) {
					case LADDER:
						for (int i = 0; i <= jumpHeight; i++) {
							for (int j = 0; j <= jumpHeight; j++) {
								if (!addEdge(queue, from, WorldUtils.jump(from, i), strategy.Action.JUMP_UP,
										minSpeedStride * j, maxSpeedStride * i, startSpeed)) break;
							}
						}
						break;
				}
				switch (tileBelowUnit) {
					case EMPTY:
//						int stride = 1;
//						// TODO add left, right fall
//						while (addEdge(queue, from, new Vec2Int(from.x,
//										WorldUtils.yU(from, -stride++)), Action.FALL, 0,
//								maxSpeedStride * stride, startSpeed));
//						boolean[] direction = new boolean[2];
//						for (int i = 1; i <= jumpHeight; i++) {
//							for (int j = 1; j <= jumpHeight; j++) {
//								Arrays.fill(direction, true);
//								for (int k = 1; k <= 2; k++) {
//									if (direction[0])
//										direction[0] = addEdge(queue, from, WorldUtils.left(from, k), Action.JUMP_UP,
//												minSpeedStride * j, maxSpeedStride * i, startSpeed);
//									if (direction[1])
//										direction[1] = addEdge(queue, from, WorldUtils.right(from, k), Action.JUMP_UP,
//												minSpeedStride * j, maxSpeedStride * i, startSpeed);
//								}
//							}
//						}
						break;
					case LADDER:
						addEdge(queue, from, WorldUtils.left(from), Action.WALK, 0, maxHorizontalSpeed, startSpeed);
						addEdge(queue, from, WorldUtils.right(from), Action.WALK, 0, maxHorizontalSpeed, startSpeed);
						for (int i = 0; i <= jumpHeight; i++)
							if (!addEdge(queue, from, WorldUtils.jump(from, -i), strategy.Action.JUMP_DOWN,
									0, maxSpeedStride * i, startSpeed)) break;
						break;
					case PLATFORM:
					case WALL:
						addEdge(queue, from, WorldUtils.left(from), Action.WALK, 0, maxHorizontalSpeed, startSpeed);
						addEdge(queue, from, WorldUtils.right(from), Action.WALK, 0, maxHorizontalSpeed, startSpeed);
						parabolaMove(queue, from, startSpeed, jumpHeight);
						break;
					case JUMP_PAD:
						parabolaMove(queue, from, startSpeed, jumpPadHeight);
				}
				startSpeed = null;
			}
		}
		return startPoint;
	}

	private void parabolaMove(Queue<TilePoint> queue, Vec2Int from, Vec2Double startSpeed, double height) {
		boolean[] direction = new boolean[3];
		for (int i = 0; i <= height; i++) {
			for (int j = 0; j <= height; j++) {
				Arrays.fill(direction, true);
				for (int k = 1; k <= height; k++) {
					if (direction[0])
						direction[0] = addEdge(queue, from, WorldUtils.jump(from, k),
								Action.JUMP_UP, minSpeedStride * j, maxSpeedStride * i, startSpeed);
					if (direction[1])
						direction[1] = addEdge(queue, from, WorldUtils.jump(WorldUtils.left(from, k), k),
								Action.JUMP_UP, minSpeedStride * j, maxSpeedStride * i, startSpeed);
					if (direction[2])
						direction[2] = addEdge(queue, from, WorldUtils.jump(WorldUtils.right(from, k), k),
								Action.JUMP_UP, minSpeedStride * j, maxSpeedStride * i, startSpeed);
				}
			}
		}
	}

	private boolean addEdge(Queue<TilePoint> queue, Vec2Int from, Vec2Int to, Action action,
							double minSpeed, double maxSpeed, Vec2Double startSpeed) {
		if (tiles.length <= to.x || tiles[0].length <= to.y || to.x < 0 || to.y < 0 || minSpeed > maxSpeed ||
			(VIC.compare(to, from) == 0))
			return false;
		switch (WorldUtils.unitTile(to, tiles)) {
			case WALL:
				return false;
			case PLATFORM:
				if (action != Action.JUMP_UP && action != Action.JUMP_DOWN)
					return false;
		}

		TilePoint fromPoint = tilePoints[from.x][from.y];
		TilePoint toPoint = createTilePoint(to, fromPoint, queue);
		Edge edgeToAdd = new Edge(fromPoint, toPoint);
		// cannot jump up vertically
		boolean jump = edgeToAdd.vertDelta() >= 1;
		if (jump && maxSpeed < edgeToAdd.horDelta() + 1) {
			return true;
		}

		// cannot jump up horizontally
		if (jump && minSpeed < (edgeToAdd.horDelta() + 1)/2.0) {
			return true;
		}
		// cannot jump up horizontally
		if (jump && minSpeed > edgeToAdd.horDelta() + 1) {
			return true;
		}

		edgeToAdd.action = action;
		edgeToAdd.cost = (10 - WorldUtils.distanceManhattan(from, to))/maxSpeed;
		edgeToAdd.minSpeed = minSpeed;
		edgeToAdd.maxSpeed = maxSpeed;

		for (Edge edge : fromPoint.adj) {
			if (EC.compare(edge, edgeToAdd) == 0)
				return true;
		}

		if (startSpeed != null && (startSpeed.getX() < edgeToAdd.minSpeed || startSpeed.getX() > edgeToAdd.maxSpeed))
			return true;

		fromPoint.adj.add(edgeToAdd);
		return true;
	}

	private TilePoint createTilePoint(Vec2Int to, TilePoint from, Queue<TilePoint> queue) {
		int x = to.x;
		int y = to.y;
		TilePoint toPoint = tilePoints[x][y];
		if (toPoint == null && maxDistance >= from.distance + 1) {
			V++;
			toPoint = new TilePoint(to, from.distance);
			tilePoints[x][y] = toPoint;
			queue.add(toPoint);
		}
		return toPoint;
	}

	public static class TilePoint {
		TilePoint(Vec2Int point, int distance) {
			this.x = point.x;
			this.y = point.y;
			this.distance = distance + 1;
		}

		@Override
		public String toString() {
			return "[" + x + "," + y + "]";
		}

		private int distance = 0;
		public int x,y;
		public List<Edge> adj = new ArrayList<>();
	}

	static Comparator<Vec2Int> VIC = (o1, o2) -> o1.x == o2.x ? Integer.compare(o1.y, o2.y) : Integer.compare(o1.x, o2.x);
	static Comparator<Edge> EC = (o1, o2) -> VIC.compare(o1.to, o2.to) + VIC.compare(o1.from, o2.from) +
			Double.compare(o1.maxSpeed, o2.maxSpeed) + Double.compare(o1.minSpeed, o2.minSpeed) +
			o1.action.compareTo(o2.action);
}

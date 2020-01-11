package strategy.world;

import model.JumpState;
import model.Properties;
import model.Tile;
import model.Vec2Double;
import strategy.Action;
import strategy.Vec2Int;

import java.util.*;

public class World {

	public Tile[][] tiles;
	public TilePoint[][] tilePoints;
	public int startX;
	public int startY;
	public int jumpHeight;
	public int jumpPadHeight;
	public int maxHorizontalSpeed;
	public int maxDistance;
	private Vec2Int partner;
	private Vec2Double unitSize;

	double maxSpeedStride = 1;
	double minSpeedStride = .5;
	double averageTileLength;

	boolean onGround;

	public World(Vec2Double unit, Vec2Double unitSpeed, Tile[][] tiles, Properties properties,
				 int maxDistance, Vec2Double partner, boolean onGround, JumpState jumpState) {
		this.tiles = tiles;
		this.jumpPadHeight = (int)(properties.getJumpPadJumpSpeed()*properties.getJumpPadJumpTime());
		this.jumpHeight = (int)(properties.getUnitJumpSpeed()*properties.getUnitJumpTime());
		this.averageTileLength = properties.getUnitSize().getY()/1.5; // 2 tiles
		this.unitSize = properties.getUnitSize();
		this.maxHorizontalSpeed = 7;
		this.maxDistance = maxDistance;
		this.partner = partner == null ? new Vec2Int(0, 0) : new Vec2Int(partner);
		this.onGround = onGround;
		buildPaths(unit);
	}

	// bfs
	private void buildPaths(Vec2Double unit) {
		tilePoints = new TilePoint[tiles.length][];
		for (int i = 0; i < tiles.length; i++)
			tilePoints[i] = new TilePoint[tiles[i].length];

		Queue<TilePoint> queue = new LinkedList<>();
		Tile unitTile = WorldUtils.unitTile(new Vec2Int(unit), tiles);
		TilePoint startPoint = new TilePoint(new Vec2Int(unit), unitTile, null);

		startX = startPoint.x;
		startY = startPoint.y;
		tilePoints[startX][startY] = startPoint;
		queue.add(startPoint);

		Vec2Int unitStart = new Vec2Int(unit);
		if (onGround ||
			WorldUtils.unitTile(WorldUtils.down(new Vec2Int(WorldUtils.changeX(unit, unitSize.getX()))), tiles) != Tile.EMPTY ||
			WorldUtils.unitTile(WorldUtils.down(new Vec2Int(WorldUtils.changeX(unit, -unitSize.getX()))), tiles) != Tile.EMPTY) {
			addEdge(queue, unitStart, WorldUtils.left(unitStart), Action.WALK, 0, maxHorizontalSpeed, jumpHeight);
			addEdge(queue, unitStart, WorldUtils.right(unitStart), Action.WALK, 0, maxHorizontalSpeed, jumpHeight);
		}

		boolean[][] visited = WorldUtils.startDetour(tiles);
		while (!queue.isEmpty()) {
			TilePoint fromPoint = queue.remove();
			Vec2Int from = new Vec2Int(fromPoint);
			if (!visited[from.x][from.y]) {
				visited[from.x][from.y] = true;
				if (maxDistance < fromPoint.distance + 1)
					continue;
				Tile tileBelowUnit = WorldUtils.unitTile(WorldUtils.down(from), tiles);
				unitTile = WorldUtils.unitTile(from, tiles);
				switch (unitTile) {
					case LADDER:
						addEdge(queue, from, WorldUtils.left(from), Action.WALK, 0, maxSpeedStride, jumpHeight);
						addEdge(queue, from, WorldUtils.right(from), Action.WALK, 0, maxSpeedStride, jumpHeight);
						for (int j = 0; j < 2; j++)
							for (int i = 1; i <= jumpHeight; i++)
								if (!addEdge(queue, from, WorldUtils.jump(from, i), Action.JUMP_UP,
										0, maxSpeedStride * j, jumpHeight)) break;
						break;
				}
				switch (tileBelowUnit) {
					case EMPTY:
						for (int i = 1; i <= maxDistance; i++) {
							if (!addEdge(queue, from, WorldUtils.jump(from, -i), Action.JUMP_DOWN, 0,
									0, maxDistance)) {
								break;
							}
						}
						for (int i = 1; i <= jumpHeight - fromPoint.distanceFromGround; i++) {
							if (!addEdge(queue, from, WorldUtils.jump(from, i), Action.JUMP_UP, 0,
									0, jumpHeight)) {
								break;
							}
						}
						boolean[] direction = new boolean[2];
						for (int i = 0; i <= maxHorizontalSpeed; i++) {
							for (int j = 0; j <= maxHorizontalSpeed; j++) {
								Arrays.fill(direction, true);
								for (int k = 1; k <= 3; k++) {
									if (direction[0])
										direction[0] = addEdge(queue, from, WorldUtils.left(from, k), Action.JUMP_UP,
												minSpeedStride * j, maxSpeedStride * i, jumpHeight);
									if (direction[1])
										direction[1] = addEdge(queue, from, WorldUtils.right(from, k), Action.JUMP_UP,
												minSpeedStride * j, maxSpeedStride * i, jumpHeight);
								}
							}
						}
						break;
					case LADDER:
						leftRight(queue, from);
						jumpDown(queue, from, jumpHeight);
						break;
					case PLATFORM:
						leftRight(queue, from);
						jumpDown(queue, from, maxDistance);
						parabolaMove(queue, from, jumpHeight);
						break;
					case WALL:
						leftRight(queue, from);
						parabolaMove(queue, from, jumpHeight);
						break;
					case JUMP_PAD:
						parabolaMove(queue, from, jumpPadHeight);
						break;
				}
			}
		}
	}

	private void jumpDown(Queue<TilePoint> queue, Vec2Int from, int maxDistance) {
		for (int j = 0; j < 2; j++)
			for (int i = 1; i <= maxDistance; i++) {
				if (!addEdge(queue, from, WorldUtils.jump(from, -i), Action.JUMP_DOWN, 0,
						maxSpeedStride * j, maxDistance)) {
					break;
				}
			}
	}

	private void leftRight(Queue<TilePoint> queue, Vec2Int from) {
		boolean[] direction = new boolean[2];
		Arrays.fill(direction, true);
		for (int k = 1; k <= 3; k++) {
			if (direction[0])
				direction[0] = addEdge(queue, from, WorldUtils.left(from, k), Action.WALK,
						0, maxHorizontalSpeed, jumpHeight);

			if (direction[1])
				direction[1] = addEdge(queue, from, WorldUtils.right(from, k), Action.WALK,
						0, maxHorizontalSpeed, jumpHeight);
		}
	}

	public double getAverageTileLength() {
		return averageTileLength;
	}

	private void parabolaMove(Queue<TilePoint> queue, Vec2Int from, double height) {
		boolean[] direction = new boolean[3];
		for (int i = 0; i <= maxHorizontalSpeed; i++) {
			for (int j = 0; j <= maxHorizontalSpeed; j++) {
				Arrays.fill(direction, true);
				for (int k = 1; k <= height; k++) {
					if (direction[0])
						direction[0] = addEdge(queue, from, WorldUtils.jump(from, k),
								Action.JUMP_UP, minSpeedStride * j, maxSpeedStride * i, height);
//					if (direction[1])
//						direction[1] = addEdge(queue, from, WorldUtils.jump(WorldUtils.left(from, k), k),
//								Action.JUMP_UP, minSpeedStride * j, maxSpeedStride * i, startSpeed);
//					if (direction[2])
//						direction[2] = addEdge(queue, from, WorldUtils.jump(WorldUtils.right(from, k), k),
//								Action.JUMP_UP, minSpeedStride * j, maxSpeedStride * i, startSpeed);
				}
			}
		}
	}

	// true looking for edges is possible
	private boolean addEdge(Queue<TilePoint> queue, Vec2Int from, Vec2Int to, Action action,
							double minSpeed, double maxSpeed, double maxHeight) {
		TilePoint fromPoint = tilePoints[from.x][from.y];
		if (tiles.length <= to.x || tiles[0].length <= to.y || to.x < 0 || to.y < 0)
			return false;

		if (to.x == partner.x && to.y == partner.y)
			return false;

		if (VIC.compare(to, from) == 0 || minSpeed > maxSpeed)
			return true;

		switch (WorldUtils.unitTile(to, tiles)) {
			case WALL:
			case JUMP_PAD:
				return false;
			case PLATFORM:
				if (!action.jump())
					return false;
				break;
		}

		Edge edgeToAdd = new Edge(from, to);
		boolean jump = action == Action.JUMP_UP || action == Action.JUMP_DOWN;

		// cannot jump up vertically
		if (jump && edgeToAdd.vertDelta() > 2 && maxSpeed > .5) {
			return true;
		}

		// cannot jump up horizontally
		if (jump && (minSpeed < edgeToAdd.horDelta()/1.5 || minSpeed > edgeToAdd.horDelta() ||
				fromPoint.distanceFromGround < edgeToAdd.horDelta())) {
			return true;
		}

		// cannot move in jump
		if (jump && fromPoint.distanceFromGround > jumpHeight && edgeToAdd.horDelta() > 1) {
			return true;
		}

		if (action == Action.WALK && fromPoint.distanceFromGround > jumpHeight) {
			return true;
		}

		if (action == Action.JUMP_UP && edgeToAdd.vertDelta() > maxHeight) {
			return true;
		}

		edgeToAdd.action = action;
		edgeToAdd.cost = (1/maxSpeed + (action.jump() ? .1 : 0));
				///(edgeToAdd.horDelta() == 0 ? edgeToAdd.vertDelta() : edgeToAdd.horDelta());
		if (action.jump() && maxSpeed == 0) edgeToAdd.cost = .3;
		edgeToAdd.minSpeed = minSpeed;
		edgeToAdd.maxSpeed = maxSpeed;

		for (Edge edge : fromPoint.adj) {
			if (EC.compare(edge, edgeToAdd) == 0)
				return true;
		}

		TilePoint tilePoint = createTilePoint(to, fromPoint, queue);
		if (tilePoint != null)
			fromPoint.adj.add(edgeToAdd);

		return tilePoint != null;
	}

	public TilePoint getStartPoint() {
		return tilePoints[startX][startY];
	}

	public boolean accessible(Vec2Double position) {
		Vec2Int vec2Int = new Vec2Int(position);
		return tilePoints[vec2Int.x][vec2Int.y] != null;
	}

	private TilePoint createTilePoint(Vec2Int to, TilePoint from, Queue<TilePoint> queue) {
		int x = to.x;
		int y = to.y;
		TilePoint toPoint = tilePoints[x][y];
		TilePoint fromPoint = tilePoints[from.x][from.y];
		Tile toTile = tiles[x][y];
		if (toPoint == null && maxDistance >= from.distance + 1) {
			toPoint = new TilePoint(to, toTile, fromPoint);
			tilePoints[x][y] = toPoint;
			queue.add(toPoint);
		}
		return toPoint;
	}

	public class TilePoint {
		TilePoint(Vec2Int point, Tile tile, TilePoint from) {
			this.x = point.x;
			this.y = point.y;
			this.tile = tile;
			if (from != null)
				this.distance = from.distance + 1;
			Vec2Int p = new Vec2Int(point);
			for(;p.y > 0 && WorldUtils.unitTile(p, tiles) == Tile.EMPTY; p.y--);
			this.distanceFromGround = Math.max(0, point.y - p.y - 1);
		}

		@Override
		public String toString() {
			return "[" + x + "," + y + "]";
		}

		private int distance = 0;
		private Tile tile;
		public int distanceFromGround;
		public int x,y;
		public List<Edge> adj = new ArrayList<>();

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			TilePoint tilePoint = (TilePoint) o;
			return x == tilePoint.x &&
					y == tilePoint.y;
		}

		@Override
		public int hashCode() {
			return Objects.hash(x, y);
		}
	}

	static Comparator<Vec2Int> VIC = (o1, o2) -> o1.x == o2.x ? Integer.compare(o1.y, o2.y) : Integer.compare(o1.x, o2.x);
	static Comparator<Edge> EC = (o1, o2) -> {
		int cmp = VIC.compare(o1.to, o2.to);
		if (cmp == 0)
			cmp = VIC.compare(o1.from, o2.from);
		if (cmp == 0)
			cmp = Double.compare(o1.maxSpeed, o2.maxSpeed);
		if (cmp == 0)
			cmp = Double.compare(o1.minSpeed, o2.minSpeed);
		if (cmp == 0)
			cmp = o1.action.compareTo(o2.action);
		return cmp;
	};
}

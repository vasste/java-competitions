import model.Tile;
import strategy.Action;
import strategy.world.WorldUtils;
import strategy.world.Edge;
import strategy.world.World;

import java.util.List;

public class Draw {
	private boolean debugEnabled;
	private World.TilePoint[][] tilePoints;
	private int startX, startY;
	private Tile[][] tiles;

	public Draw(boolean debugEnabled, World world, Tile[][] tiles) {
		this.debugEnabled = debugEnabled;
		this.startX = world.startX;
		this.startY = world.startY;
		this.tilePoints = world.tilePoints;
		this.tiles = tiles;
	}

	public void paths(Debug debug) {
		if (!this.debugEnabled)
			return;
		paths(tilePoints[startX][startY], debug, WorldUtils.startDetour(tiles));
	}

	public void paths(char[][] debug) {
		if (!this.debugEnabled)
			return;
		paths(tilePoints[startX][startY], debug);
	}

	public void paths(List<Edge> path, Debug debug) {
		if (!this.debugEnabled)
			return;
		paths(path, debug, WorldUtils.startDetour(tiles));
	}

	public void paths(List<Edge> path, char[][] level) {
		for (Edge e : path) {
			for (int i = e.from.y; i <= e.to.y; i++) {
				int sx = Math.min(e.from.x, e.to.x);
				int fx = Math.max(e.from.x, e.to.x);
				for (int j = sx; j <= fx; j +=1) {
					if (level[j][i] != '.') continue;
					if (i == e.to.y && e.action == Action.WALK) level[j][i] = '-';
					else if (j == e.to.x)
						level[j][i] = i > e.to.y ? '`' : ',';
					else level[j][i] = sx == e.from.x ? '/' : '\\';
				}
			}
		}
	}

	private void paths(World.TilePoint from, char[][] level) {
		if (!debugEnabled)
			return;
		for (int i = 0; i < level.length; i++) {
			for (int j = 0; j < level[i].length; j++) {
				World.TilePoint to = tilePoints[i][j];
				if (to != null)
					level[to.x][to.y] = '$';
			}
		}
	}

	private void paths(World.TilePoint from, Debug debug, boolean[][] visited) {
		for (Edge e : from.adj) {
			World.TilePoint point = tilePoints[e.to.x][e.to.y];
			if (!visited[point.x][point.y]) {
				visited[point.x][point.y] = true;
				debug.draw(DebugUtils.createLine(from, point));
				paths(point, debug, visited);
			}
		}
	}

	private void paths(List<Edge> path, Debug debug, boolean[][] visited) {
		for (Edge e : path) {
			World.TilePoint to = tilePoints[e.to.x][e.to.y];
			World.TilePoint from = tilePoints[e.from.x][e.from.y];
			debug.draw(DebugUtils.createLine(from, to));
		}
	}
}

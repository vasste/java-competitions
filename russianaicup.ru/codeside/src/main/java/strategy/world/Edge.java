package strategy.world;

import strategy.Action;
import strategy.Vec2Int;

public class Edge {
	public Vec2Int from, to;
	public double cost = 0;
	public Action action;
	public double minSpeed = 0;
	public double maxSpeed = 10;

	public Edge(World.TilePoint from, World.TilePoint to) {
		this.from = new Vec2Int(from);
		this.to = new Vec2Int(to);
	}

	public int horDelta() {
		return Math.abs(to.x - from.x);
	}

	public int vertDelta() {
		return Math.abs(to.y - from.y);
	}

	public int horzDirection() {
		return Integer.compare(to.x, from.x);
	}

	public int vertDirection() {
		return Integer.compare(to.y, from.y);
	}


	@Override
	public String toString() {
		return "E{" +
				"f=" + from.x + "," + from.y +
				",t=" + to.x + "," + to.y +
				",a=" + action +
				",minSpeed=" + minSpeed +
				",maxSpeed=" + maxSpeed +
				",cost=" + cost +
				'}';
	}
}
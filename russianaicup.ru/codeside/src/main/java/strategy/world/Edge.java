package strategy.world;

import strategy.Action;
import strategy.Vec2Int;

public class Edge {
	public Vec2Int from, to;
	public double cost = 0;
	public Action action;
	public double minSpeed = 0;
	public double maxSpeed = 10;

	public Edge(Vec2Int from, Vec2Int to) {
		this.from = from;
		this.to = to;
	}

	public int horDelta() {
		return Math.abs(to.x - from.x);
	}

	public double horzDirection() {
		return Math.signum(to.x - from.x);
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
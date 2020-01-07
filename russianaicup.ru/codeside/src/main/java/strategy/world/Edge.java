package strategy.world;

import model.Vec2Double;
import strategy.Action;
import strategy.Vec2Int;

public class Edge {
	public Vec2Int from, to;
	public double cost = 0;
	public Action action;
	public double minSpeed = 0;
	public double maxSpeed = 10;
	public Vec2Double fromD, toD;

	public Edge(Vec2Int from, Vec2Int to) {
		this.from = from;
		this.to = to;
		fromD = new Vec2Double(from.x + .5, from.y + .5);
		toD = new Vec2Double(to.x + .5, to.y + .5);
	}

	public int horDelta() {
		return Math.abs(to.x - from.x);
	}

	public int vertDelta() {
		return Math.abs(to.y - from.y);
	}

	public double horzDirectionD() {
		return toD.getX() - fromD.getX();
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
package strategy;

import model.Vec2Double;
import strategy.world.World;

import java.util.Objects;

public class Vec2Int {
	public int x,y;

	public Vec2Int(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public Vec2Int(Vec2Int v) {
		this.x = v.x;
		this.y = v.y;
	}

	public Vec2Int(World.TilePoint tilePoint) {
		this.x = tilePoint.x;
		this.y = tilePoint.y;
	}

	public Vec2Int(Vec2Double vec2Double) {
		this.x = (int)vec2Double.getX();
		this.y = (int)vec2Double.getY();
	}

	@Override
	public String toString() {
		return "{" +
				"x=" + x +
				", y=" + y +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Vec2Int vec2Int = (Vec2Int) o;
		return x == vec2Int.x &&
				y == vec2Int.y;
	}

	@Override
	public int hashCode() {
		return Objects.hash(x, y);
	}
}

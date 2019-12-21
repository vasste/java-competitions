package strategy;

import model.LootBox;
import model.Vec2Double;
import strategy.world.WorldUtils;

public class StrategyUtils {

	static LootBox findClosest(Vec2Double unit, LootBox[] boxes) {
		double ux = WorldUtils.xU(unit);
		int minIx = 0;
		double minX = Integer.MAX_VALUE;
		for (int i = 0; i < boxes.length; i++) {
			double x = WorldUtils.xL(boxes[i]);
			if (Math.abs(x - ux) < minX) {
				minX = Math.abs(x - ux);
				minIx = i;
			}
		}
		return boxes[minIx];
	}
}

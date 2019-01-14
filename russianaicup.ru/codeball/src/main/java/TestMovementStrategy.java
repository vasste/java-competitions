import model.Action;
import model.Game;
import model.Robot;
import model.Rules;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author Vasilii Stepanov.
 * @since 14.01.2019
 */
public class TestMovementStrategy implements RobotStrategy {

	public int actionTick = 0;
	public int possibleActionTick = 0;

	@Override
	public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
					Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {

		if (actionTick == 0) {
			new Vec3D(0, 0, 0).apply(action);
			action.jump_speed = 15;
		}
//		Vec3D mp = new Vec3D(me);
		Vec3D mv = Vec3D.velocity(me);
		me.velocity_y = 15;
//
		Entity re = new Entity(me, rules);
		int i = 1;
		double t = 1.0 / rules.TICKS_PER_SECOND * i;
		SimulationUtils.move(re, t, rules);
		if (mv.getY() < 0) {
			renderingCollection.put(random.nextDouble(), new DrawUtils.TH(game.current_tick + ""));
			actionTick = 0;
		}
	}
}

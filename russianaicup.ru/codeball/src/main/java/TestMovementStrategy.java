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
			action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED;
		}
		Vec3D mp = new Vec3D(me);
		Vec3D mv = Vec3D.velocity(me);

		Entity re = new Entity(me, rules);
		if (re.velocity.getY() > 0) {
			for (int i = 0; i < 60; i++) {
				double t = 1.0 / rules.TICKS_PER_SECOND * i;
				SimulationUtils.move(re, t, rules);
				if (re.velocity.getY() < 0) {
					renderingCollection.put(random.nextDouble(), new DrawUtils.TH((i + game.current_tick) + ""));
					possibleActionTick = i + game.current_tick;
					break;
				}
			}
		}
		if (mv.getY() < 0) {
			renderingCollection.put(random.nextDouble(), new DrawUtils.TH(game.current_tick + ""));
			actionTick = 0;
		}
	}
}

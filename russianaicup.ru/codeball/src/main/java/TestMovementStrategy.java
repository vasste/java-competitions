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
	public int id;
	public double jumpSpeed = 0;
	public double jumpTick = 0;
	Entity re;

	@Override
	public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
					Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {

		double t = 1.0 / rules.TICKS_PER_SECOND;
		if (game.current_tick >= jumpTick + jumpSpeed/rules.GRAVITY*rules.TICKS_PER_SECOND) {
			System.out.println();
		}

		if (me.touch) {
			new Vec3D(0, 0, -30).apply(action);
			jumpSpeed = action.jump_speed = random.nextInt(16);
			jumpTick = game.current_tick;
			if (id == 0) id = me.id;
		}
		if (id != me.id) return;

		Vec3D mp = new Vec3D(me);
		Vec3D mv = Vec3D.velocity(me);
		if (re != null) {
			//System.out.println(mp.getY() - re.position.getY());
		}
		re = new Entity(me, rules);
		SimulationUtils.move(re, t, rules);

		System.out.println(mp.getY() + " " + me.velocity_y);
		if (me.velocity_y < 0) {
			System.out.println();
		}

//		me.velocity_y = 15;
//
//		int i = 1;
//
//		SimulationUtils.move(re, t, rules);
//		if (mv.getY() < 0) {
//			renderingCollection.put(random.nextDouble(), new DrawUtils.TH(game.current_tick + ""));
//			actionTick = 0;
//		}
	}
}

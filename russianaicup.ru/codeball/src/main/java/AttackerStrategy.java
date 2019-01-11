import model.Action;
import model.Game;
import model.Robot;
import model.Rules;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AttackerStrategy implements RobotStrategy {

    @Override
    public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
                    Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {

        Vec3D mp = new Vec3D(me);
        Vec3D bp = new Vec3D(game.ball);
        Vec3D velocity = null;
        if (bp.getZ() > mp.getZ()) {
            for (PointWithTime ballPoint : ballPoints) {
                Vec3D bpS = ballPoint.v;
                if (bpS.getZ() > mp.getZ()) {
                    double needSpeed = ballPoint.v.minus(mp).length();
                    if (ballPoint.t > 0) needSpeed /= ballPoint.t;
                    if (needSpeed < rules.ROBOT_MAX_GROUND_SPEED) {
                        boolean possibleGoal =
                                SimulationUtils.simulateGoalHit(ballPoint.vl.plus(mp.unit().multiply(needSpeed)),
                                        bpS, rules, -leftRight);
                        if (possibleGoal) {
                            renderingCollection.put(ballPoint.t + random.nextDouble(),
                                    new DrawUtils.Line(mp, ballPoint.v, 5, Color.ORANGE).h());
                            velocity = bpS.minus(mp).unit().multiply(SimulationUtils.clamp(needSpeed,
                                    0.5*rules.ROBOT_MAX_GROUND_SPEED, rules.ROBOT_MAX_GROUND_SPEED));
                            break;
                        }
                    }
                }
            }
            if (velocity == null)
                velocity = bp.minus(mp).unit().multiply(rules.ROBOT_MAX_GROUND_SPEED);
        } else {
            for (PointWithTime ballPoint : ballPoints) {
                Vec3D bpS = ballPoint.v;
                if (bpS.getZ() > mp.getZ()) {
                    double needSpeed = ballPoint.v.minus(mp).length();
                    if (ballPoint.t > 0) needSpeed /= ballPoint.t;
                    if (needSpeed < rules.ROBOT_MAX_GROUND_SPEED) {
                        renderingCollection.put(ballPoint.t + random.nextDouble(),
                                new DrawUtils.Line(mp, ballPoint.v, 5, Color.ORANGE).h());
                        velocity = bp.minus(mp).unit().multiply(SimulationUtils.clamp(needSpeed,
                                0.5*rules.ROBOT_MAX_GROUND_SPEED, rules.ROBOT_MAX_GROUND_SPEED));
                        break;
                    }
                }
            }
        }
        if (velocity == null)
            velocity = new Vec3D(0, me.radius, -(rules.arena.depth / 2.0) + 2*rules.arena.bottom_radius).minus(mp)
                    .unit().multiply(rules.ROBOT_MAX_GROUND_SPEED);

        velocity.apply(action);
        boolean jump = mp.minus(bp).length() < rules.BALL_RADIUS + rules.ROBOT_MAX_RADIUS && me.z <= game.ball.z;
        if (jump)
            action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED/2;
    }
}

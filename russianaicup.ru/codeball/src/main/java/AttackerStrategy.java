import model.*;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class AttackerStrategy implements RobotStrategy {

    AttackerStates attackerStates;

    @Override
    public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
                    Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {
        if (restart) {
            attackerStates = AttackerStates.prepare;
        }

        Vec3D mePos = new Vec3D(me);
        double speed = 100*rules.ROBOT_MAX_GROUND_SPEED;
        for (PointWithTime ballPoint : ballPoints) {
            Vec3D ballPos = ballPoint.v;
            if (ballPos.getY() > mePos.getY() &&
                Math.abs(ballPos.getX()) < (rules.arena.width / 2.0) &&
                Math.abs(ballPos.getZ()) < (rules.arena.depth / 2.0))
            {
                Vec3D deltaPos = ballPos.minus(mePos);
                double need_speed = deltaPos.length() / ballPoint.t;
                if (speed > need_speed) {
                    need_speed = speed;
                    Vec3D targetVelocity = deltaPos.unit().multiply(need_speed);
                    targetVelocity.apply(action);
                }
            }
        }
    }

    enum AttackerStates {
        prepare, attack
    }
}

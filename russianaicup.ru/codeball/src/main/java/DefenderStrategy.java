import model.*;
import model.Robot;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DefenderStrategy implements RobotStrategy {

    DefenderStates defenderState = DefenderStates.init;
    PointWithTime goalPointAndTime = null;

    @Override
    public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
                    Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {
        if (restart) {
            defenderState = DefenderStates.init;
        }
        Vec3D mp = new Vec3D(me);
        Vec3D bp = new Vec3D(game.ball);

        Vec3D velocity = null;
        goalPointAndTime = evaluation(game.ball, mp, rules, ballPoints, leftRight, renderingCollection, random);

        switch (defenderState) {
            case init:
                double goalZ = leftRight * rules.arena.depth / 2 + rules.arena.bottom_radius;
                double goalKeeperX = SimulationUtils.clamp(bp.getX(), -rules.arena.goal_width/2, rules.arena.goal_width/2);
                velocity = new Vec3D(goalKeeperX, 0, goalZ).minus(mp).unit().multiply(rules.ROBOT_MAX_GROUND_SPEED);
                goalPointAndTime = null;
                break;
            case defend:
                if (goalPointAndTime != null) {
                    Vec3D deltaPos = goalPointAndTime.v.minus(new Vec3D(mp));
                    double speed = deltaPos.length();
                    if (goalPointAndTime.t > 0) speed /= goalPointAndTime.t;
                    velocity = deltaPos.unit().multiply(SimulationUtils.clamp(speed,
                            0.5*rules.ROBOT_MAX_GROUND_SPEED, rules.ROBOT_MAX_GROUND_SPEED));
                    renderingCollection.put(random.nextDouble(), new DrawUtils.Line(mp, velocity).h());
                }
        }
        if (velocity != null) {
            SimulationUtils.clamp(velocity, -rules.ROBOT_MAX_GROUND_SPEED, rules.ROBOT_MAX_GROUND_SPEED);
            velocity.apply(action);
            boolean jump = mp.minus(bp).length() < rules.BALL_RADIUS + rules.ROBOT_MAX_RADIUS && me.z <= game.ball.z;
            if (jump)
                action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED;
        }
    }

    PointWithTime evaluation(Ball ball, Vec3D me, Rules rules,
                             List<PointWithTime> ballPoints, int leftRight,
                             Map<Double, Object> renderingCollection, Random random) {
        int possibleGoal = -1;
        for (int i = 0; i < ballPoints.size(); i++) {
            PointWithTime pWt = ballPoints.get(i);
            if (SimulationUtils.goal(pWt.v, rules.arena, ball.radius, leftRight)) {
                possibleGoal = i;
                break;
            }
        }
        double minRequiredSpeed = Double.MAX_VALUE;
        PointWithTime minSpeedPwt = null;
        defenderState = DefenderStates.init;
        for (int j = 0; j < possibleGoal; j++) {
            PointWithTime pWt = ballPoints.get(j);
            if (Math.abs(pWt.v.getZ()) <= rules.arena.depth/4) continue;
            defenderState = DefenderStates.defend;
            renderingCollection.put(pWt.t + random.nextDouble(),
                    new DrawUtils.Line(me, pWt.v, 5, Color.GREEN).h());
            double needSpeed = pWt.v.minus(me).length();
            if (pWt.t > 0) needSpeed /= pWt.t;
            if (needSpeed < rules.ROBOT_MAX_GROUND_SPEED) {
                defenderState = DefenderStates.defend;
                renderingCollection.put(pWt.t, new DrawUtils.Sphere(pWt.v, ball.radius, Color.GREEN).h());
                return pWt;
            } else {
                if (minRequiredSpeed > needSpeed) {
                    minRequiredSpeed = needSpeed;
                    minSpeedPwt = pWt;
                }
            }
        }
        return minSpeedPwt;
    }

    enum DefenderStates {
        init, defend
    }
}

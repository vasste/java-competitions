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

        Vec3D velocity = null;
        goalPointAndTime = evaluation(game.ball, mp, rules, ballPoints, leftRight, renderingCollection, random);

        switch (defenderState) {
            case init:
                double goalZ = leftRight * rules.arena.depth / 2 + rules.arena.bottom_radius;
                velocity = new Vec3D(0, 0, goalZ).minus(mp).unit().multiply(rules.ROBOT_MAX_GROUND_SPEED);
                goalPointAndTime = null;
                break;
            case defend:
                if (goalPointAndTime != null) {
                    Vec3D deltaPos = goalPointAndTime.v.minus(new Vec3D(mp));
                    double speed = deltaPos.length()/goalPointAndTime.t;
                    velocity = deltaPos.unit().multiply(speed);
                    renderingCollection.put(random.nextDouble(), new DrawUtils.Line(mp, velocity).h());
                }
        }
        if (velocity != null) {
            SimulationUtils.clamp(velocity, -rules.ROBOT_MAX_GROUND_SPEED, rules.ROBOT_MAX_GROUND_SPEED);
            velocity.apply(action);
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
            defenderState = DefenderStates.defend;
            renderingCollection.put(pWt.t + random.nextDouble(),
                    new DrawUtils.Line(me, pWt.v, 5, Color.GREEN).h());
            double needSpeed = pWt.v.minus(me).length() / pWt.t;
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

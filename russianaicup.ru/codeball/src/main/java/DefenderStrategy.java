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
        goalPointAndTime = evaluation(game.ball, mp, rules, goalPointAndTime, ballPoints, leftRight,
                renderingCollection, random);

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

    PointWithTime evaluation(Ball ball, Vec3D me, Rules rules, PointWithTime goalPointAndTime,
                             List<PointWithTime> ballPoints, int leftRight,
                             Map<Double, Object> renderingCollection, Random random) {
        int possibleGoal = 0;
        for (; possibleGoal < ballPoints.size(); possibleGoal++) {
            PointWithTime pWt = ballPoints.get(possibleGoal);
            if (SimulationUtils.goal(pWt.v, rules.arena, ball.radius, leftRight))
                break;
        }
        if (ballPoints.isEmpty() || possibleGoal == ballPoints.size()) {
            defenderState = DefenderStates.init;
            return null;
        }

        for (int i = 0; i < possibleGoal; i++) {
            PointWithTime pWt = ballPoints.get(i);
            renderingCollection.put(pWt.t + random.nextDouble(),
                    new DrawUtils.Line(me, pWt.v, 5, Color.GREEN).h());
            double needSpeed = pWt.v.minus(me).length() / pWt.t;
            if (needSpeed < rules.ROBOT_MAX_GROUND_SPEED) {
                defenderState = DefenderStates.defend;
                renderingCollection.put(pWt.t, new DrawUtils.Sphere(pWt.v, ball.radius, Color.GREEN).h());
                return pWt;
            }
        }
        defenderState = DefenderStates.init;
        return null;
    }

    enum DefenderStates {
        init, defend
    }
}

import model.*;
import model.Robot;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DefenderStrategy implements RobotStrategy {

    DefenderStates state = DefenderStates.defend;
    DefenderStates actionState = DefenderStates.defend;
    ActionData actionData = null;

    @Override
    public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
                    Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {
        if (restart) {
            actionState = DefenderStates.defend;
        }
        Vec3D mp = new Vec3D(me);
        Vec3D mv = Vec3D.velocity(me);
        Vec3D bp = new Vec3D(game.ball);

        Vec3D velocity = null;
        actionData = evaluation(game.ball, mp, mv, rules, ballPoints, leftRight, renderingCollection, random, game);

        switch (state) {
            case defend:
                double goalZ = leftRight * rules.arena.depth / 2 + rules.arena.bottom_radius;
                double goalKeeperX = SimulationUtils.clamp(bp.getX(), -rules.arena.goal_width/2, rules.arena.goal_width/2);
                velocity = new Vec3D(goalKeeperX, 0, goalZ).minus(mp).unit().multiply(rules.ROBOT_MAX_GROUND_SPEED);
                actionData = null;
                for (PointWithTime ballPoint : ballPoints) {
                    if (ballPoint.v.minus(mp).groundLength() < rules.arena.depth / 10) {
                        renderingCollection.put(ballPoint.t, new DrawUtils.Sphere(ballPoint.v, rules.BALL_RADIUS, Color.PINK).h());
                        Entity hitPosition =
                                SimulationUtils.simulateDefenceHit(me, new Entity(ballPoint, rules), rules, random);
                        if (hitPosition == null) continue;
                        Vec3D deltaPos = hitPosition.position.minus(mp);
                        double V0 = mv.groundLength();
                        actionState = DefenderStates.defend;
                        //velocity = deltaPos.unitGround().multiply(2*V0);
                        break;
                    }
                }
                break;
            case attack_defend:
                if (actionData != null) {
                    Vec3D deltaPos = actionData.goalPosition.minus(mp);
                    double S = deltaPos.groundLength();
                    velocity = deltaPos.unitGround();
                    if (actionData.deltaAccTime > 0)
                        velocity = velocity.multiply(actionData.deltaAccTime * rules.ROBOT_ACCELERATION);
                    else {
                        //if (actionData.deltaBreakTime > 0)
                           // velocity = velocity.multiply(0);
                        //0felse
                            velocity = velocity.multiply(actionData.groundV0);
                    }
					double SY = Math.abs(deltaPos.getY());
                    if (S < rules.ROBOT_MIN_RADIUS &&
                        SY > rules.ROBOT_MIN_RADIUS &&
                        actionData.deltaJumpTime < actionData.deltaTime) {
                        action.jump_speed = StrictMath.sqrt(2*SY*rules.GRAVITY);
                    }
                    renderingCollection.put(random.nextDouble(), new DrawUtils.Line(mp, velocity).h());
                }
                break;
        }
        state = actionState;
        if (velocity != null)
            velocity.apply(action);
    }

    ActionData evaluation(Ball ball, Vec3D position, Vec3D velocity, Rules rules,
                          List<PointWithTime> ballPoints, int leftRight,
                          Map<Double, Object> renderingCollection, Random random,
                          Game game) {
        int possibleGoal = -1;
        for (int i = 0; i < ballPoints.size(); i++) {
            PointWithTime pWt = ballPoints.get(i);
            if (SimulationUtils.goal(pWt.v, rules.arena, ball.radius, leftRight)) {
                possibleGoal = i;
                break;
            }
        }
        actionState = DefenderStates.defend;
        for (int j = 0; j <= possibleGoal ; j++) {
            PointWithTime pWt = ballPoints.get(j);
            if (Math.abs(pWt.v.getZ()) < rules.arena.depth / 4) continue;
			Vec3D deltaPos = pWt.v.minus(position);

            double time = pWt.t;
            renderingCollection.put(time, new DrawUtils.Sphere(pWt.v, ball.radius, Color.RED).h());
			double SY = Math.abs(deltaPos.getY());
            double jumpTime = 0;
            double ballMeAngle = Math.abs(pWt.vl.angleToVector(velocity));
            if (SY > rules.ROBOT_MIN_RADIUS) {
                jumpTime = StrictMath.sqrt(2*SY/rules.GRAVITY);
//                groundTime -= jumpTime;
//                if (groundTime < 0) continue;
            }
            double S = deltaPos.groundLength();
            double V0 = velocity.groundLength();
            double accTime = Math.min((rules.ROBOT_MAX_GROUND_SPEED - V0)/rules.ROBOT_ACCELERATION, time);
            double breakTime = Math.min(V0/rules.ROBOT_ACCELERATION, time);
            //if (Math.abs(ballMeAngle) > Math.PI/2) breakTime = 0;
//            if (time - accTime - breakTime <= 0) accTime = 0;
//            if (time - breakTime > 0) breakTime = 0;

            double maxS = S(V0, accTime, rules.ROBOT_ACCELERATION) +
                    rules.ROBOT_MAX_GROUND_SPEED * Math.max(0, time - accTime);
                    //S(V0, breakTime, ruleaas.ROBOT_ACCELERATION);
            if (S < maxS)
            {
                pWt = ballPoints.get(Math.min(possibleGoal, j - 1));
                renderingCollection.put(pWt.t, new DrawUtils.Sphere(pWt.v, ball.radius, Color.BLACK).h());
                actionState = DefenderStates.attack_defend;
                return new ActionData(time, time, jumpTime, accTime, breakTime, V0, rules.GRAVITY * jumpTime, pWt.v, pWt.vl);
            }
        }
        return null;
    }

    public static double S(double V0, double t, double a) {
        return V0*t + a*t*t/2;
    }

    class ActionData {
        double deltaJumpTime;
        double deltaGroundTime;
        double deltaAccTime;
        double deltaBreakTime;
        double deltaTime;
        double groundV0;
        double jumpV0;
        Vec3D goalPosition;
        Vec3D ballVelocity;

        public ActionData(double deltaTime, double deltaGroundTime, double deltaJumpTime, double deltaAccTime,
                          double deltaBreakTime, double groundV0, double jumpV0, Vec3D goalPosition, Vec3D ballVelocity) {
            this.deltaTime = deltaTime;
            this.deltaGroundTime = deltaGroundTime;
            this.deltaJumpTime = deltaJumpTime;
            this.deltaAccTime = deltaAccTime;
            this.deltaBreakTime = deltaBreakTime;
            this.groundV0 = groundV0;
            this.jumpV0 = jumpV0;
            this.goalPosition = goalPosition;
            this.ballVelocity = ballVelocity;
        }
    }

    enum DefenderStates {
        attack_defend, defend
    }
}

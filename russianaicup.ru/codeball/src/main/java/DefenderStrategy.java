import model.*;
import model.Robot;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class DefenderStrategy implements RobotStrategy {

    DefenderStates state = DefenderStates.defend;
    ActionData actionData = null;

    @Override
    public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints,
                    Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {
        if (restart) {
            state = DefenderStates.defend;
        }
        Vec3D mp = new Vec3D(me);
        Vec3D mv = Vec3D.velocity(me);
        Vec3D bp = new Vec3D(game.ball);

        Vec3D velocity = null;
        double currentTime = game.current_tick/rules.TICKS_PER_SECOND;
        actionData = evaluation(game.ball, mp, mv, rules, ballPoints, leftRight, renderingCollection, random, game);

        switch (state) {
            case defend:
                double goalZ = leftRight * rules.arena.depth / 2 + rules.arena.bottom_radius;
                double goalKeeperX = SimulationUtils.clamp(bp.getX(), -rules.arena.goal_width/2, rules.arena.goal_width/2);
                velocity = new Vec3D(goalKeeperX, 0, goalZ).minus(mp).unit().multiply(rules.ROBOT_MAX_GROUND_SPEED);
                actionData = null;
                for (PointWithTime ballPoint : ballPoints) {
                    if (ballPoint.v.getZ() < leftRight*rules.arena.width/4) {
                        Vec3D deltaPos = ballPoint.v.minus(mp);
                        if (deltaPos.groundLength() <= S(mv.length(), ballPoint.t, rules.ROBOT_ACCELERATION)) {
                            renderingCollection.put(ballPoint.t, new DrawUtils.Sphere(ballPoint.v, rules.BALL_RADIUS, Color.PINK).h());
                            velocity = deltaPos.unitGround().multiply(ballPoint.t * rules.ROBOT_ACCELERATION);
                            break;
                        }
                    }
                }
                break;
            case attack_defend:
                if (actionData != null) {
                    Vec3D deltaPos = actionData.goalPosition.minus(mp);
                    velocity = deltaPos.unitGround();
                    if (actionData.deltaAccTime > 0)
                        velocity = velocity.multiply(actionData.deltaAccTime * rules.ROBOT_ACCELERATION);
                    else
                        velocity = velocity.multiply(actionData.groundV0);
					double SY = Math.abs(deltaPos.getY());
                    if (deltaPos.groundLength() < rules.BALL_RADIUS) {
                        action.jump_speed = StrictMath.sqrt(2*SY*rules.GRAVITY);
                    }
                    renderingCollection.put(random.nextDouble(), new DrawUtils.Line(mp, velocity).h());
                }
                break;
        }
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
        state = DefenderStates.defend;
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
            double accTime = (rules.ROBOT_MAX_GROUND_SPEED - V0)/rules.ROBOT_ACCELERATION;
            double breakTime = V0/rules.ROBOT_ACCELERATION;
            //if (Math.abs(ballMeAngle) > Math.PI/2) breakTime = 0;
            if (time - accTime - breakTime <= 0) accTime = 0;

            double maxS = S(V0, accTime, rules.ROBOT_ACCELERATION) +
                    rules.ROBOT_MAX_GROUND_SPEED * Math.max(0, time - accTime - breakTime) +
                    S(V0, breakTime, rules.ROBOT_ACCELERATION);
            if (S < maxS)
            {
                pWt = ballPoints.get(Math.max(0, j - 1));
                renderingCollection.put(pWt.t, new DrawUtils.Sphere(pWt.v, ball.radius, Color.BLACK).h());
                state = DefenderStates.attack_defend;
                return new ActionData(time, jumpTime, accTime, V0, rules.GRAVITY * jumpTime, pWt.v, pWt.vl);
            }
        }
        return null;
    }

    public static double S(double V0, double t, double a) {
        return V0*t + a*t*t/2;
    }

    class ActionData {
        double deltaTime;
        double deltaJumpTime;
        double deltaAccTime;
        double groundV0;
        double jumpV0;
        Vec3D goalPosition;
        Vec3D ballVelocity;

        public ActionData(double deltaTime, double deltaJumpTime, double deltaAccTime,
                          double groundV0, double jumpV0, Vec3D goalPosition, Vec3D ballVelocity) {
            this.deltaTime = deltaTime;
            this.deltaJumpTime = deltaJumpTime;
            this.deltaAccTime = deltaAccTime;
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

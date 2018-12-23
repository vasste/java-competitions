import model.*;

import java.util.Arrays;

public final class MyStrategy implements Strategy {

    int defender_goalkeeper = 0;
    int attacker = 0;
    DefenderStates defenderStates;
    P3D[] tenLastBallPoints = new P3D[10];

    Vec3D defenderVelocityVector;
    Vec3D attackerVelocityVector;

    public MyStrategy() {
        Arrays.fill(tenLastBallPoints, P3D.zero);
    }

    @Override
    public void act(Robot me, Rules rules, Game game, Action action) {
        if (!me.is_teammate) return;
        if (defender_goalkeeper == 0) {
            defenderStates = DefenderStates.init;
            defender_goalkeeper = me.id;
        }
        else if (attacker == 0) attacker = me.id;

        if (isGoalie(me)) playDefenderGoalkeeper(me, rules, game, action);
        else playAttacker(me, rules, game, action);
    }

    void playDefenderGoalkeeper(Robot me, Rules rules, Game game, Action action) {
        P3D mp = new P3D(me);
        Vec3D velocity = null;
        switch (defenderStates) {
            case init:
                velocity = new Vec3D(mp, new P3D(0, 0, -rules.arena.depth/4));
                if (velocity.length() < 0.1) defenderStates = DefenderStates.defend;
                break;
            case defend:
                Vec3D ballVelocity = Vec3D.velocity(game.ball);
                P3D ballPosition = new P3D(game.ball);
                double t = (mp.getZ() - ballPosition.getZ())/ballVelocity.getZ();
                double

        }
        if (velocity != null)
            velocity.apply(action);
        defenderVelocityVector = velocity;
    }

    void playAttacker(Robot me, Rules rules, Game game, Action action) {
        P3D mp = new P3D(me);
        P3D bp = new P3D(game.ball);
        Vec3D velocity = new Vec3D(mp, bp);
        velocity.setLength(velocity.length() * 10);
        if (game.ball.z > mp.getZ()) {
            attackerVelocityVector = velocity;
        } else {
            velocity = new Vec3D(mp, P3D.zero);
        }
        velocity.apply(action);
    }

    boolean isGoalie(Robot robot) {
        return robot.id == defender_goalkeeper;
    }


    enum DefenderStates {
        init, defend
    }


    enum AttakerStates {
        attack
    }

    @Override
    public String customRendering() {
        return null;
    }
}

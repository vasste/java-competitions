import model.*;

public final class MyStrategy implements Strategy {

    @Override
    public void act(Robot me, Rules rules, Game game, Action action) {
        if (!me.is_teammate) return;
        P3D mp = new P3D(me);
//        P3D bp = new P3D(game.ball);
//        Vec3D vel = new Vec3D(mp, bp);
//        vel.apply(action);
//        action.jump_speed = rules.ROBOT_MAX_JUMP_SPEED*ThreadLocalRandom.current().nextDouble();


        Vec3D vel2 = new Vec3D(mp, new P3D(0, 0, 0));
        vel2.apply(action);
    }

    static Vec3D goal(Arena arena, Robot me) {
        return new Vec3D(new P3D(me), new P3D(0, ));
    }
}

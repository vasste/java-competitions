import model.*;

public final class MyStrategy implements Strategy {
    @Override
    public void act(Robot me, Rules rules, Game game, Action action) {
        P3D d = new P3D(me);
        Vec3D vel = Vec3D.velocity(me);
        if (vel.length() < 10) {
            vel.setLength(vel.length() * .1);
            action.
        }
    }
}

import model.Action;
import model.Game;
import model.Robot;
import model.Rules;

public class InstantanAttack implements Strategy {
    @Override
    public void act(Robot me, Rules rules, Game game, Action action) {
        if (!me.is_teammate) return;
        P3D mp = new P3D(me);
        P3D bp = new P3D(game.ball);
        Vec3D hit = new Vec3D(mp, bp);
        hit.setLength(hit.length()*10);
        hit.apply(action);
    }

    @Override
    public String customRendering() {
        return null;
    }
}

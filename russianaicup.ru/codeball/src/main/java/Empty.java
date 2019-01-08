import model.Action;
import model.Game;
import model.Robot;
import model.Rules;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class Empty implements RobotStrategy {
    @Override
    public void act(Robot me, Rules rules, Game game, Action action, List<PointWithTime> ballPoints, Map<Double, Object> renderingCollection, Random random, int leftRight, boolean restart) {

    }
}

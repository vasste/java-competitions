import com.google.gson.Gson;
import model.Action;
import model.Game;
import model.Robot;
import model.Rules;

import java.util.*;

public final class MyStrategy implements Strategy {

    private final RobotStrategy[] strategies = {new DefenderStrategy(), new AttackerStrategy()};
    Map<Double, Object> renderingCollection = new HashMap<>();
    List<PointWithTime> ballPoints = new ArrayList<>();
    int simulationTick = 0;
    Random random = new Random();
    int leftRight = -1;
    int currentGoals = -1;


    @Override
    public void act(Robot me, Rules rules, Game game, Action action) {
        if (game.current_tick == 0) {
            leftRight = (int)Math.signum(me.z);
        }
        if (simulationTick != game.current_tick) renderingCollection.clear();
        boolean restart = currentGoals != game.players[0].score + game.players[1].score;
        if (restart)
            currentGoals = game.players[0].score + game.players[1].score;

        simulationTick = SimulationUtils.simulate(game.ball, rules, game, me, renderingCollection,
                random, ballPoints, simulationTick);

        strategies[me.id % 2].act(me, rules, game, action, ballPoints, renderingCollection, random, leftRight, restart);
    }

    @Override
    public String customRendering() {
        Gson gson = new Gson();
        return gson.toJson(renderingCollection.values().toArray(new Object[0]));
    }
}

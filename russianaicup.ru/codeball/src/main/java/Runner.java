import model.Action;
import model.Game;
import model.Robot;
import model.Rules;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class Runner {
    private final String token;

    public static void main(String[] args) throws IOException {
        new Runner().run();
    }

    private Runner() throws IOException {
        token = "0000000000000000";
    }

    public void run() throws IOException {
        String host = "127.0.0.1";
        //new Thread(() -> runStrategy(new DefenderStrategy(), new RemoteProcessClient(host, Integer.parseInt("31002")))).start();
        runStrategy(new MyStrategy(), new RemoteProcessClient(host, Integer.parseInt("31001")));
    }

    private void runStrategy(Strategy strategy, RemoteProcessClient remoteProcessClient) {
        Map<Integer, Action> actions = new HashMap<>();
        Game game;
        try {
            remoteProcessClient.writeToken(token);
            Rules rules = remoteProcessClient.readRules();
            while ((game = remoteProcessClient.readGame()) != null) {
                actions.clear();
                for (Robot robot : game.robots) {
                    if (robot.is_teammate) {
                        Action action = new Action();
                        strategy.act(robot, rules, game, action);
                        actions.put(robot.id, action);
                    }
                }
                remoteProcessClient.write(actions, strategy.customRendering());
            }
        } catch (IOException e) {
        }
    }
}

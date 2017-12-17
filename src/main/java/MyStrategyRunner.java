import model.*;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public final class MyStrategyRunner {
    private final RemoteProcessClient remoteProcessClient;
    private final String token;
    private static volatile boolean connected;

    public static void main(String[] args) throws IOException, InterruptedException {
        Thread[] threads = new Thread[]{
                new Thread(() -> {
                    try {
                        MyStrategyRunner runner =
                                new MyStrategyRunner(args.length == 3 ? args : new String[] {"127.0.0.1", "31002", "0000000000000000"});
                        runner.run(new MyStrategy());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
//                ,new Thread(() -> {
//                    try {
//                        while (!connected);
//                        Thread.sleep(1000);
//                        MyStrategyRunner runner =
//                                new MyStrategyRunner(args.length == 3 ? args : new String[]{"127.0.0.1", "31002", "0000000000000000"});
//                        runner.run(new MyStrategy());
//                    } catch (IOException | InterruptedException e) {
//                        e.printStackTrace();
//                    }
//        })
        };
        for (int i = 0; i < threads.length; i++) {
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
    }

    private MyStrategyRunner(String[] args) throws IOException {
        remoteProcessClient = new RemoteProcessClient(args[0], Integer.parseInt(args[1]));
        token = args[2];
    }

    @SuppressWarnings("WeakerAccess")
    public void run(Strategy strategy) throws IOException {
        try {
            remoteProcessClient.writeTokenMessage(token);
            remoteProcessClient.writeProtocolVersionMessage();
            connected = true;
            remoteProcessClient.readTeamSizeMessage();
            Game game = remoteProcessClient.readGameContextMessage();

            PlayerContext playerContext;

            while ((playerContext = remoteProcessClient.readPlayerContextMessage()) != null) {
                Player player = playerContext.getPlayer();
                if (player == null) {
                    break;
                }

                Move move = new Move();
                strategy.move(player, playerContext.getWorld(), game, move);

                remoteProcessClient.writeMoveMessage(move);
            }
        } finally {
            remoteProcessClient.close();
        }
    }
}

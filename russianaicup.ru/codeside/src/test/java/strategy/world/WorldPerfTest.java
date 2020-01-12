package strategy.world;

import model.JumpState;
import model.Properties;
import model.Tile;
import model.Vec2Double;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.List;
import java.util.concurrent.TimeUnit;

@State(value = Scope.Benchmark)
public class WorldPerfTest {

    Tile[][] tiles;
    Vec2Double unit;
    Vec2Double unitSpeed;
    Properties properties = new Properties();
    Vec2Double opp;

    @Setup
    public void setup() {
        String fileName = "/level.txt";
        tiles = TestUtils.readTiles(fileName);
        unit = TestUtils.findPosition(fileName, 'P');
        properties.setJumpPadJumpSpeed(1);
        properties.setJumpPadJumpTime(10);
        properties.setUnitJumpSpeed(1);
        properties.setUnitJumpTime(5);
        properties.setUnitSize(new Vec2Double(0.9, 1.8));
        unitSpeed = new Vec2Double(0, 0);
        opp = TestUtils.findPosition(fileName, '*');
    }

    @Benchmark
    @BenchmarkMode({Mode.AverageTime})
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    @Warmup(iterations = 2)
    @Fork(1)
    @Threads(1)
    public void build(Blackhole bh) {
        World world = new World(unit, tiles,
                properties, 30, null, true, new JumpState());
        Path path = new Path(world);
        List<Edge> edgeList = path.find(opp);
        bh.consume(edgeList);
    }
}

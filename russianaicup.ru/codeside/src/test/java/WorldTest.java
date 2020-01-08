import model.JumpState;
import model.Properties;
import model.Tile;
import model.Vec2Double;
import org.junit.Assert;
import org.junit.Test;
import strategy.Action;
import strategy.world.Path;
import strategy.world.Edge;
import strategy.world.World;

import java.io.IOException;
import java.util.List;

public class WorldTest {

	@Test
	public void buildWorldTest() throws IOException {
		String fileName = "level-jump3.txt";
		Tile[][] tiles = TestUtils.readTiles(fileName);
		Vec2Double unit = TestUtils.findPosition(fileName, 'P');
		Properties properties = new Properties();
		properties.setJumpPadJumpSpeed(1);
		properties.setJumpPadJumpTime(10);
		properties.setUnitJumpSpeed(1);
		properties.setUnitJumpTime(5);
		properties.setUnitSize(new Vec2Double(0.9, 1.8));
		Vec2Double unitSpeed = new Vec2Double(0 ,0);
		World world = new World(unit, unitSpeed, tiles, properties, 50, new Vec2Double(0, 0),
				true, new JumpState());
		//List<LootBox> boxes = Utils.readLootBoxes(fileName);
		World.TilePoint point = world.getStartPoint();
		//Assert.assertTrue(point.adj.stream().anyMatch(e -> e.action == Action.JUMP_UP));
		char[][] level = TestUtils.fromTiles(tiles);
		Vec2Double to = TestUtils.findPosition(fileName, '*');
		level[(int)to.getX()][(int)to.getY()] = '*';
		Path path = new Path(world);
		Draw draw = new Draw(true, world, tiles);
		draw.paths(level);
		TestUtils.print(level);
		double unitStartSpeed = 1;
		List<Edge> edges = path.find(to);
		Assert.assertFalse(edges.isEmpty());
		Assert.assertTrue(unitStartSpeed >= edges.iterator().next().minSpeed);
		level = TestUtils.fromTiles(tiles);
		draw.paths(edges, level);
		TestUtils.print(level);
//		strategy.world.drawPaths(level);
//		for (LootBox box : boxes) {
//			System.out.println();
//		}
	}
}

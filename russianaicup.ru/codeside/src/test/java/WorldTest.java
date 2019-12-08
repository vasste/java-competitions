import model.Properties;
import model.Tile;
import model.Vec2Double;
import org.junit.Test;

import java.io.IOException;

public class WorldTest {

	@Test
	public void buildWorldTest() throws IOException {
		Tile[][] tiles = Utils.readTiles("level-wall.txt");
		Vec2Double unit = new Vec2Double();
		unit.setX(1);
		unit.setY(1);
		Properties properties = new Properties();
		properties.setJumpPadJumpSpeed(1);
		properties.setJumpPadJumpTime(1);
		properties.setUnitJumpSpeed(1);
		properties.setUnitJumpTime(5);
		World world = new World(unit, tiles, properties);
		char[][] level = Utils.fromTiles(tiles);
		world.drawPaths(level);
		Utils.print(level);
	}
}

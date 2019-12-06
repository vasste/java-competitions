import model.Level;
import model.Properties;
import model.Tile;
import model.Vec2Double;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class WorldTest {

	@Test
	public void buildWorldTest() throws IOException {
		Level level = Level.readFrom(new FileInputStream("level"));
		Tile[][] tiles = level.getTiles();
		Vec2Double unit = new Vec2Double();
		for (int i = 0; i < tiles.length; i++) {
			for (int j = 0; j < tiles[j].length; j++) {
				if (tiles[i][j] == Tile.PLATFORM) {
					unit.setX(i);
					unit.setY(j - 1);
				}
			}
		}
		Properties properties = new Properties();
		properties.setJumpPadJumpSpeed(1);
		properties.setJumpPadJumpTime(1);
		properties.setUnitJumpSpeed(1);
		properties.setUnitJumpTime(1);
		World world = new World(unit, level.getTiles(), properties);
	}


}

import model.Item;
import model.LootBox;
import model.Tile;
import model.Vec2Double;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestUtils {

	public static void print(char[][] level) {
		for (int j = level[1].length - 1; j >= 0 ; j--) {
			for (int i = 0; i < level.length; i++) {
				System.out.print(level[i][j]);
			}
			System.out.println();
		}
	}

	public static char[][] fromTiles(Tile[][] tiles) {
		char[][] level = new char[tiles.length][];
		for (int i = 0; i < tiles.length; i++) {
			level[i] = new char[tiles[i].length];
			for (int j = 0; j < tiles[i].length; j++) {
				switch (tiles[i][j]) {
					case EMPTY:
						level[i][j] = '.';
						break;
					case JUMP_PAD:
						level[i][j] = 'T';
						break;
					case WALL:
						level[i][j] = '#';
						break;
					case PLATFORM:
						level[i][j] = '^';
						break;
					case LADDER:
						level[i][j] = 'H';
						break;
				}
			}
		}
		return level;
	}

	public static List<LootBox> readLootBoxes(String fileName) {
		try {
			URL url = WorldTest.class.getResource(fileName);
			List<String> lines = Files.readAllLines(Paths.get(url.toURI())); // y
			List<LootBox> lootBoxes = new ArrayList<>();
			Vec2Double size = new Vec2Double(1, 1);
			for (int j = lines.size() - 1; j >= 0; j--) {
				String line = lines.get(j);
				for (int i = 0; i < line.length(); i++) {
					Vec2Double position = new Vec2Double(i, lines.size() - j - 1);
					switch (line.charAt(i)) {
						case 'L':
							lootBoxes.add(new LootBox(position, size, new Item.HealthPack(100)));
					}
				}
			}
			return lootBoxes;
		} catch (IOException | URISyntaxException e) {
			return Collections.emptyList();
		}
	}

	public static Vec2Double findPosition(String fileName, char marker) {
		try {
			URL url = WorldTest.class.getResource(fileName);
			List<String> lines = Files.readAllLines(Paths.get(url.toURI())); // y
			for (int j = lines.size() - 1; j >= 0; j--) {
				String line = lines.get(j);
				for (int i = 0; i < line.length(); i++) {
					if (line.charAt(i) == marker)
						return new Vec2Double(i, lines.size() - j - 1);
				}
			}
		} catch (IOException | URISyntaxException e) {
		}
		return null;
	}

	public static Tile[][] readTiles(String fileName) {
		try {
			URL url = WorldTest.class.getResource(fileName);
			List<String> lines = Files.readAllLines(Paths.get(url.toURI())); // y
			int xl = lines.get(0).length();
			int yl = lines.size();
			Tile[][] tiles = new Tile[xl][yl];
			for (int j = lines.size() - 1; j >= 0; j--) {
				String line = lines.get(j);
				for (int i = 0; i < line.length(); i++) {
					Tile tile;
					switch (line.charAt(i)) {
						case 'H':
							tile = Tile.LADDER;
							break;
						case '^':
							tile = Tile.PLATFORM;
							break;
						case '#':
							tile = Tile.WALL;
							break;
						case 'T':
							tile = Tile.JUMP_PAD;
							break;
						default:
							tile = Tile.EMPTY;
					}
					tiles[i][lines.size() - j - 1] = tile;
				}
			}
			return tiles;
		} catch (IOException | URISyntaxException e) {
			return null;
		}
	}
}

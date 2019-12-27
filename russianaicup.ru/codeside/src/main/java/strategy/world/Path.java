package strategy.world;

import model.Vec2Double;

import java.util.*;

public class Path {
	public int startX;
	public int startY;
	public World.TilePoint[][] tilePoints;

	public Path(World world) {
		this.startX = world.startX;
		this.startY = world.startY;
		this.tilePoints = world.tilePoints;
	}

	// dijkstra
	public List<Edge> find(Vec2Double to) {
		return tryFind(to);
	}

	private List<Edge> tryFind(Vec2Double to) {
		PriorityQueue<Node> pq = new PriorityQueue<>();
		Set<Node> settled = new HashSet<>();
		double[][] cost = WorldUtils.startDetourValues(tilePoints, Integer.MAX_VALUE);
		Edge[][] edges = new Edge[tilePoints.length][];

		for (int i = 0; i < tilePoints.length; i++)
			edges[i] = new Edge[tilePoints[i].length];

		pq.add(new Node(tilePoints[startX][startY], 0));

		// Distance to the source is 0
		cost[startX][startY] = 0;
		while (!pq.isEmpty()) {

			// remove the minimum distance node
			// from the priority queue
			Node u = pq.remove();

			// adding the node whose distance is
			// finalized
			if (settled.add(u))
				e_Neighbours(u, settled, cost, pq, edges);
		}
		Deque<Edge> path = new LinkedList<>();
		int x = (int)to.getX();
		int y = (int)to.getY();
		for (Edge e = edges[x][y]; e != null; e = edges[e.from.x][e.from.y])
			path.addFirst(e);

		return new ArrayList<>(path);
	}

	// Function to process all the neighbours
	// of the passed node
	private void e_Neighbours(Node u, Set<Node> settled, double[][] cost, PriorityQueue<Node> pq, Edge[][] edges)
	{
		double edgeCost = -1;
		double newCost = -1;

		// All the neighbors of v
		for (Edge v : u.levelPoint.adj) {
			// If current node hasn't already been processed
			if (!settled.contains(new Node(tilePoints[v.to.x][v.to.y], 0))) {
				edgeCost = v.cost;
				newCost = cost[u.x()][u.y()] + edgeCost;

				// If new distance is cheaper in cost
				if (newCost < cost[v.to.x][v.to.y]) {
					cost[v.to.x][v.to.y] = newCost;
					edges[v.to.x][v.to.y] = v;
				}

				// Add the current node to the queue
				pq.add(new Node(tilePoints[v.to.x][v.to.y], cost[v.to.x][v.to.y]));
			}
		}
	}

	static class Node implements Comparable<Node> {
		public World.TilePoint levelPoint;
		public double cost = 0;

		public Node(World.TilePoint levelPoint, double cost) {
			this.levelPoint = levelPoint;
			this.cost = cost;
		}

		int x() {
			return levelPoint.x;
		}

		int y() {
			return levelPoint.y;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Node node = (Node) o;
			return Objects.equals(levelPoint, node.levelPoint);
		}

		@Override
		public int hashCode() {
			return Objects.hash(levelPoint);
		}

		@Override
		public int compareTo(Node o) {
			return Double.compare(cost, o.cost);
		}
	}
}

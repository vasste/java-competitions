import java.util.*;

/**
 * @author Vasilii Stepanov.
 * @since 06.12.2017
 */
public class FactoriesRoute {
    private PriorityQueue<N> pq;
    private N[] edgeTo;
    private double[] distTo;
    private int width;
    private int height;
    public double[][] edges;

    public FactoriesRoute(double[][] worldSpeedFactor, int sx, int sy) {
        int titles = width * height;
        this.width = 32;
        this.height = 32;
        edges = new double[width * height][width * height];
        for (int i = 0; i < width * height; i++) {
            for (int j = 0; j < width * height; j++) {
                int jy = j/width;
                int jx = j - j/width;
                edges[i][j] = worldSpeedFactor[jx][jy];
            }
        }
        for (double[] edge : edges) {
            Arrays.fill(edge, 1);
        }
        distTo = new double[titles];
        edgeTo = new N[titles];
        Arrays.fill(distTo, Integer.MAX_VALUE);
        pq = new PriorityQueue<>();
        N sn = new N(sx, sy, 0);
        distTo[sn.index()] = 0;
        pq.add(sn);
        while (!pq.isEmpty()) {
            N from = pq.remove();
            Set<N> adj = evaluateNeighbours(from);
            for (N to : adj)
                relax(from, to);
        }
    }

    // relax edge e and update pq if changed
    private void relax(N v, N w) {
        int wi = w.index();
        int vi = v.index();
        if (distTo[wi] > distTo[vi] + edges[wi][vi]) {
            distTo[wi] = distTo[vi] + edges[wi][vi];
            edgeTo[wi] = v;

            if (pq.remove(w)) {
                w.cost = distTo[wi];
                pq.add(w);
            } else {
                w.cost = distTo[wi];
                pq.add(w);
            }
        }
    }

    public static void cij(int i, int j, int width, int height, Set<N> adj) {
        if (i >= height || i < 0 || j < 0 || j >= width) return;
        adj.add(new N(i, j, Double.MAX_VALUE));
    }

    private Set<N> evaluateNeighbours(N n) {
        Set<N> adj = new HashSet<>();
        int i = n.x;
        int j = n.y;
        cij(i, j - 1, width, height, adj);
        cij(i - 1, j - 1, width, height, adj);
        cij(i - 1, j, width, height, adj);
        cij(i - 1, j + 1, width, height, adj);
        cij(i, j + 1, width, height, adj);
        cij(i + 1, j + 1, width, height, adj);
        cij(i + 1, j, width, height, adj);
        return adj;
    }

    public Stack<N> pathTo(int x, int y) {
        int v = y * 32 + x;
        Stack<N> path = new Stack<>();
        for (N e = edgeTo[v]; e != null; e = edgeTo[e.index()]) {
            path.push(e);
        }
        return path;
    }

    static class N implements Comparable<N> {
        int x, y, i;
        double cost;

        public N(int x, int y, double cost) {
            this.x = x;
            this.y = y;
            this.cost = cost;
        }

        int index() {
            return y * 32 + x;
        }

        @Override
        public int compareTo(N o2) {
            return Double.compare(cost, o2.cost);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            N n = (N) o;
            return Objects.equals(x, n.x) && Objects.equals(y, n.y);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return x + "|" + y + "|" + cost;
        }
    }
}

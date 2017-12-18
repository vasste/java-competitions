import java.util.*;

import static java.lang.StrictMath.round;

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
    public boolean[] noEdges;

    public FactoriesRoute(double[][][] worldSpeedFactor, int sx, int sy, int width, int height, int vti,
                          int[][] otherGroups, int[][] facility, int[][] excludeFacility, int[][] emp, int[][] dfpt) {
        this(worldSpeedFactor, sx, sy, width, height, vti, otherGroups, facility, excludeFacility, emp, true,
                new int[][]{new int[]{sx, sy}}, dfpt);
    }

    public FactoriesRoute(double[][][] worldSpeedFactor, int sx, int sy, int width, int height, int vti,
                          int[][] otherGroups, int[][] facility, int[][] excludeFacility, int[][] emp,
                          boolean excludeAdj, int[][] currentGroup, int[][] dfpt) {
        this.width = width;
        this.height = height;
        int titles = width * height;
        edges = new double[titles][titles];
        noEdges = new boolean[titles];
        N sn = new N(sx, sy, 0, width);
        Set<N> adj = evaluateNeighbours(sn);
        for (int i = 0; i < titles; i++) {
            for (int j = 0; j < titles; j++) {
                int jy = j/width;
                int jx = j - jy*width;
                int iy = i/width;
                int ix = i - iy*width;
                edges[i][j] += worldSpeedFactor[jx][jy][vti] + worldSpeedFactor[ix][iy][vti];
                G: for (int[] otherGroup : otherGroups) {
                    if (sx == otherGroup[0] && sy == otherGroup[1]) continue;
                    for (int[] xy : currentGroup) {
                        if (xy[0] == otherGroup[0] && xy[1] == otherGroup[1]) continue G;
                    }
                    if (jx == otherGroup[0] && jy == otherGroup[1]) {
                        noEdges[j] = true;
                        if (excludeAdj) {
                            for (N n : evaluateNeighbours(new N(jx, jy, 0, width))) {
                                if (adj.contains(n))
                                    noEdges[n.index()] = true;
                            }
                        }
                    }
                }
                FI:
                for (int[] fpt : facility) {
                    for (int l = 0; l < excludeFacility.length; l++) {
                        if (excludeFacility[l][0] == fpt[0] && fpt[1] == excludeFacility[l][1])
                            continue FI;
                    }
                    if (jx == fpt[0] && jy == fpt[1]) {
                        noEdges[j] = true;
                    }
                }
                EF: for (int k = 0; k < emp.length; k++) {
                    if (jx == emp[k][0] && jy == emp[k][1]) {
                        for (int[] aDfpt : dfpt) if (emp[k][0] == aDfpt[0] && emp[k][1] == aDfpt[1]) continue EF;
                        noEdges[j] = true;
                        if (excludeAdj) {
                            for (N n : evaluateNeighbours(new N(jx, jy, 0, width))) {
                                for (int[] aDfpt : dfpt)
                                    if (n.x == aDfpt[0] && n.y == aDfpt[1])
                                        continue EF;
                                noEdges[n.index()] = true;
                            }
                        }
                    }
                }
            }
        }
        distTo = new double[titles];
        edgeTo = new N[titles];
        Arrays.fill(distTo, Double.MAX_VALUE);
        pq = new PriorityQueue<>();
        distTo[sn.index()] = 0;
        pq.add(sn);
        while (!pq.isEmpty()) {
            N from = pq.remove();
            adj = evaluateNeighbours(from);
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

    private void cij(int i, int j, int width, int height, Set<N> adj) {
        if (i >= width || i < 0 || j < 0 || j >= height || noEdges[j * width + i]) return;
        adj.add(new N(i, j, Double.MAX_VALUE, width));
    }

    private Set<N> evaluateNeighbours(N n) {
        Set<N> adj = new HashSet<>();
        int i = n.x; // 1
        int j = n.y; // 1
        cij(i - 1, j, width, height, adj); // 0,1
        cij(i - 1, j - 1, width, height, adj); // 0,0
        cij(i, j - 1, width, height, adj); // 1,0
        cij(i + 1, j - 1, width, height, adj); // 2,0
        cij(i + 1, j, width, height, adj); // 2,1
        cij(i + 1, j + 1, width, height, adj); // 2,2
        cij(i, j + 1, width, height, adj); //1,2
        cij(i - 1, j + 1, width, height, adj); //0,2
        return adj;
    }

    public Stack<N> pathTo(int x, int y, double cost) {
        int v = y * width + x;
        Stack<N> path = new Stack<>();
        boolean lastStep = true;
        for (N e = edgeTo[v]; e != null; e = edgeTo[e.index()]) {
            if (lastStep) {
                path.push(new N(x, y, cost, width));
                lastStep = false;
            }
            path.push(e);
        }
        return path;
    }

    static class N implements Comparable<N> {
        int x, y;
        double cost;
        int width;

        public N(int[] xy) {
            this.x = xy[0];
            this.y = xy[1];
        }

        public N(int x, int y, double cost, int width) {
            this.x = x;
            this.y = y;
            this.cost = cost;
            this.width = width;
        }

        int index() {
            return y * width + x;
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
            return x + "|" + y + "|" + round(cost);
        }
    }
}

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
        N sn = new N(sx, sy, 0, width);
        noEdges = buildMovementGraph(worldSpeedFactor, sx, sy, width, vti, otherGroups,
                facility, excludeFacility, emp, excludeAdj, currentGroup, dfpt, height, edges);

        distTo = new double[titles];
        edgeTo = new N[titles];
        Arrays.fill(distTo, Double.MAX_VALUE);
        pq = new PriorityQueue<>();
        distTo[sn.index()] = 0;
        pq.add(sn);
        while (!pq.isEmpty()) {
            N from = pq.remove();
            Set<N> adj = evaluateNeighbours(from, width, height, noEdges);
            for (N to : adj)
                relax(from, to);
        }
    }

    public static boolean[] buildMovementGraph(double[][][] worldSpeedFactor, int sx, int sy, int width, int vti,
                                               int[][] otherGroups, int[][] facility, int[][] excludeFacility, int[][] emp,
                                               boolean excludeAdj, int[][] currentGroup, int[][] dfpt, int height,
                                               double[][] edges) {
        int titles = width * height;
        boolean[] noEdges = new boolean[titles];
        N sn = new N(sx, sy, 0, width);
        Set<N> adj = evaluateNeighbours(sn, width, height, null);
        for (int i = 0; i < titles; i++) {
                int iy = i/width;
                int ix = i - iy*width;
                N in = new N(ix, iy, 0, width);
                Set<N> adjI = evaluateNeighbours(in, width, height, noEdges);
            for (N n : adjI) {
                int jx = n.x;
                int jy = n.y;
                int j = n.index();
                edges[i][j] += worldSpeedFactor[ix][iy][vti] + worldSpeedFactor[jx][jy][vti];
                G: for (int[] otherGroup : otherGroups) {
                    for (int[] xy : currentGroup) {
                        if (xy[0] == otherGroup[0] && xy[1] == otherGroup[1]) continue G;
                    }
                    if (jx == otherGroup[0] && jy == otherGroup[1]) {
                        noEdges[j] = true;
//                        if (excludeAdj) {
//                            for (N oGadj : evaluateNeighbours(new N(otherGroup[0], otherGroup[1], 0, width), width, height, noEdges)) {
//                                if (adj.contains(oGadj))
//                                    noEdges[oGadj.index()] = true;
//                            }
//                        }
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
//                    if (excludeAdj) {
//                        FI2: for (N oGadj : evaluateNeighbours(new N(fpt[0], fpt[1], 0, width), width, height, noEdges)) {
//                            for (int[] anExcludeFacility : excludeFacility) {
//                                if (anExcludeFacility[0] == oGadj.x && oGadj.y == anExcludeFacility[1])
//                                    continue FI2;
//                            }
//                            if (adj.contains(oGadj) && (oGadj.x != sx || oGadj.y != sy)) {
//                                noEdges[oGadj.index()] = true;
//                            }
//
//                        }
//                    }
                }
                EF: for (int k = 0; k < emp.length; k++) {
                    if (jx == emp[k][0] && jy == emp[k][1]) {
                        for (int[] aDfpt : dfpt) if (emp[k][0] == aDfpt[0] && emp[k][1] == aDfpt[1]) continue EF;
                        noEdges[j] = true;
//                        if (excludeAdj) {
//                            for (N adjn : evaluateNeighbours(new N(jx, jy, 0, width), width, height, noEdges)) {
//                                for (int[] aDfpt : dfpt)
//                                    if (adjn.x == aDfpt[0] && adjn.y == aDfpt[1])
//                                        continue EF;
//                                noEdges[adjn.index()] = true;
//                            }
//                        }
                    }
                }
            }
        }
        return noEdges;
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

    private static void cij(int i, int j, int width, int height, Set<N> adj, boolean[] noEdges) {
        if (i >= width || i < 0 || j < 0 || j >= height || (noEdges != null && noEdges[j * width + i])) return;
        adj.add(new N(i, j, Double.MAX_VALUE, width));
    }

    private static Set<N> evaluateNeighbours(N n, int width, int height, boolean[] noEdges) {
        Set<N> adj = new HashSet<>();
        int i = n.x; // 1
        int j = n.y; // 1
        cij(i - 1, j, width, height, adj, noEdges); // 0,1
        //cij(i - 1, j - 1, width, height, adj, noEdges); // 0,0
        cij(i, j - 1, width, height, adj, noEdges); // 1,0
        //cij(i + 1, j - 1, width, height, adj, noEdges); // 2,0
        cij(i + 1, j, width, height, adj, noEdges); // 2,1
        //cij(i + 1, j + 1, width, height, adj, noEdges); // 2,2
        cij(i, j + 1, width, height, adj, noEdges); //1,2
        //cij(i - 1, j + 1, width, height, adj, noEdges); //0,2
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

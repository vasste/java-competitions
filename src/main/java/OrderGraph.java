import model.ActionType;
import model.VehicleType;
import model.World;

import java.util.*;

import static java.lang.StrictMath.max;

public class OrderGraph {
    public static GraphVertex bfs(int[][] field, int si, int sj, int di, int dj) {
        Queue<GraphVertex> qp = new LinkedList<>();
        qp.add(new GraphVertex(si, sj, null));
        boolean[][] touched = new boolean[3][3];
        while (!qp.isEmpty()) {
            GraphVertex ij = qp.poll();
            int i = ij.i;
            int j = ij.j;
            if (touched[i][j]) continue;
            if (i == di && j == dj) {
                return ij;
            }
            touched[i][j] = true;
            if (cij(i+1,j,field)) qp.add(new GraphVertex(i+1,j, ij));
            if (cij(i,j+1,field)) qp.add(new GraphVertex(i,j+1, ij));
            if (cij(i-1,j,field)) qp.add(new GraphVertex(i-1,j, ij));
            if (cij(i,j-1,field)) qp.add(new GraphVertex(i,j-1, ij));
        }
        return null;
    }

    public static boolean cij(int i, int j, int[][] field) {
        if (i >= 3 || i < 0 || j < 0 || j >=3) return false;
        if (field[i][j] == 0) return true;
        return false;
    }

    public static void setupGroupingMoves(Rectangle[] rectangles, World world) {
        Path path = new Path(rectangles);
        Map<Integer, Rectangle> idRectangle = new HashMap<>();
        for (int i = 0; i < rectangles.length; i++) {
            idRectangle.put(rectangles[i].g, rectangles[i]);
        }

        if (path.ends.size() == 0) return;
        for (Map.Entry<Integer, GraphVertex> e : path.ends.entrySet()) {
            Deque<MoveBuilder> commands = idRectangle.get(e.getKey()).commands;
            for (GraphVertex p = e.getValue(); p != null && p.prev != null; p = p.prev) {
                P2D dst = Rectangle.ORDER.square(p.i, p.j);
                P2D src = Rectangle.ORDER.square(p.prev.i, p.prev.j);
                commands.add(MoveBuilder.c(ActionType.MOVE).dfCToXY(src, dst.x, dst.y));
                commands.add(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).vehicleType(idRectangle.get(e.getKey()).vt).setRect(new Rectangle(world)));
            }
            if (MyStrategy.DEBUG) {
                for (MoveBuilder command : commands) {
                    System.out.println(command);
                }
            }
            System.out.println();
        }
    }

    static class Path {
        Map<Integer, GraphVertex> ends = new HashMap<>();
        private Map<Integer, Rectangle> idRectangle = new HashMap<>();

        public Path(Rectangle[] rectangles) {
            for (int i = 0; i < rectangles.length; i++) {
                idRectangle.put(rectangles[i].g, rectangles[i]);
            }

            Rectangle order = Rectangle.ORDER;
            int[][] field = new int[3][3];
            double side = max(order.linew(), order.lineh());
            for (int i = 0; i < field.length; i++) {
                for (int j = 0; j < field.length; j++) {
                    for (int k = 0; k < rectangles.length; k++) {
                        if (rectangles[k].cX() >= order.l + j*side/3 && rectangles[k].cX() <= order.l + (j+1)*side/3 &&
                                rectangles[k].cY() >= order.t + i*side/3 && rectangles[k].cY() <= order.t + (i+1)*side/3) {
                            field[i][j] = rectangles[k].g;
                        }
                    }
                }
            }

            int[][] groups = new int[3][2];
            int[][] weight = new int[6][3];
            for (int i = 0; i < field.length; i++) {
                for (int j = 0; j < field.length; j++) {
                    weight[i][0] += field[i][j] == 0 ? 0 : 1;
                    weight[i + 3][0] += field[j][i] == 0 ? 0 : 1;
                    if (field[i][j] == 0) weight[i][1] = j;
                    if (field[j][i] == 0) weight[i + 3][1] = j;
                    weight[i][2] += field[i][j];
                    weight[i + 3][2] += field[j][i];
                    if (field[i][j] != 0) groups[field[i][j] - 1] = new int[]{i,j};
                }
            }

            boolean lineFound = false;
            GraphVertex vehicleTypeEnd = null;
            int group = 0;
            for (int i = 0; i < weight.length; i++) {
                if (weight[i][0] == 3) {
                    lineFound = true;
                    break;
                }
                if (weight[i][0] == 2) {
                    // calc or recalculate cost to make line
                    int di = i >= 3 ? weight[i][1] : i;
                    int dj = i >= 3 ? i - 3 : weight[i][1];
                    int[] sij = groups[6 - weight[i][2] - 1];
                    if (vehicleTypeEnd == null) {
                        vehicleTypeEnd = bfs(field, sij[0], sij[1], di, dj);
                        group = 6 - weight[i][2];
                    }
                    else {
                        GraphVertex diff = bfs(field, sij[0], sij[1], di, dj);
                        double factor = idRectangle.get(group).vt == VehicleType.TANK ? 2 : 1;
                        if (count(diff) < factor * count(vehicleTypeEnd)) {
                            vehicleTypeEnd = diff;
                            group = 6 - weight[i][2];
                        }
                    }
                    lineFound = true;
                }
            }
            if (!lineFound) {
                if (field[0][0] != 0) ends.put(field[0][0], new GraphVertex(1,0, new GraphVertex(0,0, null)));
                if (field[2][2] != 0) ends.put(field[2][2], new GraphVertex(1,2, new GraphVertex(2,2, null)));
                if (field[2][0] != 0) ends.put(field[2][0], new GraphVertex(1,0, new GraphVertex(2,0, null)));
                if (field[0][2] != 0) ends.put(field[0][2], new GraphVertex(1,2, new GraphVertex(0,2, null)));
                if (field[0][1] != 0) ends.put(field[0][1], new GraphVertex(1,1, new GraphVertex(0,1, null)));
                if (field[2][1] != 0) ends.put(field[2][1], new GraphVertex(1,1, new GraphVertex(2,1, null)));
            } else {
                if (vehicleTypeEnd != null) ends.put(group, vehicleTypeEnd);
            }
            if (MyStrategy.DEBUG) for (GraphVertex graphVertex : ends.values()) System.out.println(OrderGraph.toString(graphVertex));
            if (MyStrategy.DEBUG) for (int i = 0; i < 3; i++) { System.out.println(Arrays.toString(field[i]));}
        }

        private int count(GraphVertex end) {
            int steps = 0;
            if (end == null) return Integer.MAX_VALUE;
            for (GraphVertex p = end; p != null; p = p.prev) steps++;
            return steps;
        }
    }

    static class GraphVertex {
        public GraphVertex(int i, int j, GraphVertex prev) {
            this.i = i;
            this.j = j;
            this.prev = prev;
        }

        int i,j;
        GraphVertex prev;

        @Override
        public String toString() {
            return "(" + i + "," + j + ")";
        }
    }

    public static String toString(GraphVertex graphVertex) {
        if (graphVertex == null) return "";
        StringBuilder sb = new StringBuilder();
        for (GraphVertex p = graphVertex; p != null; p = p.prev) sb.append(p.toString());
        return sb.toString();
    }
}

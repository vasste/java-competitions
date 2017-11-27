import model.ActionType;

import java.util.*;

import static java.lang.StrictMath.max;

public class OrderGraph {
    public static void mij(int i, int j, int[][] md, int ms) {
        if (i >= 3 || i < 0 || j < 0 || j >=3) return;
        md[i][j] = ms;
    }

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

    public static boolean fM(int gid, int si, int sj, int di, int dj, Rectangle order, Deque<MoveBuilder> deque, double side) {
        Rectangle dst = order.square(di, dj, 3, side);
        deque.add(MoveBuilder.c(ActionType.MOVE).dfCToXY(order.square(si, sj, 3, side), dst.cX(), dst.cY()));
        deque.addLast(MoveBuilder.c(ActionType.CLEAR_AND_SELECT).group(gid));
        return true;
    }

//    public static void setupGroupingMoves(Deque<MoveBuilder> tankGroupingMoves, Deque<MoveBuilder> arrvGroupingMoves,
//                                          Deque<MoveBuilder> ifvGroupingmoves,
//                                          Rectangle[] rectangles, int[] pG, MyStrategy.GGS[] ggsI, int[] groups) {
//        Path OT = new Path(rectangles, groups, pG, true);
//        Path TO = new Path(rectangles, groups, pG, false);
//        GraphVertex pkG1;
//        GraphVertex pkG2;
//        Path result;
//        result = OT.count() > TO.count() ? TO : OT;
//        pkG1 = result.end;
//        pkG2 = result.pkG2;
//        System.arraycopy(result.ggsI, 0, ggsI, 0, ggsI.length);
//        Rectangle order = Rectangle.ORDER.scale(1.367);
//        double side = max(order.linew(), order.lineh());
//        for (GraphVertex p = pkG1.prev; p != null && p.prev != null; p = p.prev) fM(pG[1], p.prev.i, p.prev.j, p.i, p.j, order, tankGroupingMoves, side);
//        for (GraphVertex p = pkG2.prev; p != null && p.prev != null; p = p.prev) fM(pG[2], p.prev.i, p.prev.j, p.i, p.j, order, arrvGroupingMoves, side);
//    }

    static class Path {
        GraphVertex end;

        public Path(int[][] field, int di, int dj, int[] ij) {
            end = bfs(field, ij[0], ij[1], di, dj);
        }

        public Path(Rectangle[] rectangles, int di, int dj, int dk, int... groups) {
            Rectangle order = Rectangle.ORDER;
            int[] ij = null;
            int[][] field = new int[3][3];
            double side = max(order.linew(), order.lineh());
            for (int i = 0; i < field.length; i++) {
                for (int j = 0; j < field.length; j++) {
                    for (int k = 0; k < rectangles.length; k++) {
                        if (rectangles[k].cX() >= order.l + j*side/3 && rectangles[k].cX() <= order.l + (j+1)*side/3 &&
                                rectangles[k].cY() >= order.t + i*side/3 && rectangles[k].cY() <= order.t + (i+1)*side/3) {
                            field[i][j] = k + 1;
                            if (k == dk) ij = new int[]{i,j};
                        }
                    }
                }
            }

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

            for (int i = 0; i < weight.length; i++) {
                if (weight[i][0] == 3) {
                    line = true;
                    break;
                }
                if (weight[i][0] == 2) {
                    // calc or recalculate cost to make line
                    int di = i >= 3 ? weight[i][1] : i;
                    int dj = i >= 3 ? i - 3 : weight[i][1];
                    if (path == null) path = new Path(field, di, dj, groups[6 - weight[i][2] - 1]);
                    else {
                        Path diff = new Path(field, di, dj, groups[6 - weight[i][2] - 1]);
                        if (diff.count() < path.count()) path = diff;
                    }
                    line = true;
                }
            }

            end = bfs(field, ij[0], ij[1], di, dj);
            if (MyStrategy.DEBUG) System.out.println(OrderGraph.toString(end));
            if (MyStrategy.DEBUG) for (int i = 0; i < 3; i++) { System.out.println(Arrays.toString(field[i]));}
        }

        int count() {
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

    public static void main(String[] args) {
        Random random = new Random();
        int[][] field = new int[3][3];
        int[][] groups = new int[3][2];
        int gn = 1;
        while (gn < 4) {
            int ij = random.nextInt(9);
            if (field[ij/3][ij % 3] == 0) field[ij/3][ij % 3] = gn++;
        }

        boolean line = false;
        Path path = null;
        for (int i = 0; i < field.length; i++) {
            System.out.println(Arrays.toString(field[i]));
        }

        if (!line) {
            System.out.println("no line");
        }
        if (path != null) System.out.println(OrderGraph.toString(path.end));
        System.out.println();
        for (int i = 0; i < weight.length; i++) {
            System.out.println(Arrays.toString(weight[i]));
        }
    }
}

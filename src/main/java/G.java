import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import static java.lang.StrictMath.max;
import static model.ActionType.CLEAR_AND_SELECT;
import static model.ActionType.MOVE;

public class G {
    public static String toString(GP gp) {
        StringBuilder sb = new StringBuilder();
        for (GP p = gp; p != null; p = p.prev) sb.append(p.toString());
        return sb.toString();
    }

    public static void mij(int i, int j, int[][] md, int ms) {
        if (i >= 3 || i < 0 || j < 0 || j >=3) return;
        md[i][j] = ms;
    }

    public static GP bfs(int[][] field, int[][] mD, int si, int sj, int dgid) {
        Queue<GP> qp = new LinkedList<>();
        qp.add(new GP(si, sj, null));
        while (!qp.isEmpty()) {
            GP ij = qp.poll();
            int i = ij.i;
            int j = ij.j;
            if (field[i][j] == dgid) {
                return ij;
            }
            if (cij(i+1,j,field, dgid) && (mD[i][j] & R) > 0) qp.add(new GP(i+1,j, ij));
            if (cij(i,j+1,field, dgid) && (mD[i][j] & D) > 0) qp.add(new GP(i,j+1, ij));
            if (cij(i-1,j,field, dgid) && (mD[i][j] & L) > 0) qp.add(new GP(i-1,j, ij));
            if (cij(i,j-1,field, dgid) && (mD[i][j] & U) > 0) qp.add(new GP(i,j-1, ij));
        }
        return null;
    }

    public static boolean cij(int i, int j, int[][] field, int dgid) {
        if (i >= 3 || i < 0 || j < 0 || j >=3) return false;
        if (field[i][j] == dgid || field[i][j] == 0) return true;
        return false;
    }

    public static boolean fM(int gid, int si, int sj, int di, int dj, Rect order, Deque<MB> deque, double side) {
        Rect dst = order.square(di, dj, 3, side);
        deque.add(MB.c(MOVE).dfCToXY(order.square(si, sj, 3, side), dst.cX(), dst.cY()));
        deque.addLast(MB.c(CLEAR_AND_SELECT).group(gid));
        return true;
    }

    public static void setupGroupingMoves(Deque<MB> gm1, Deque<MB> gm2, Rect[] rects, int[] pG, MyStrategy.GGS[] ggsI, int[] groups) {
        Rect order = Rect.ORDER.scale(1.6);
        int[][] field = new int[3][3];
        int[][] mD = new int[3][3];
        for (int i = 0; i < mD.length; i++) Arrays.fill(mD[i], A);
        int[] ij2 = null;
        int[] ij1 = null;
        double side = max(order.linew(), order.lineh());
        for (int i = 0; i < field.length; i++) {
            for (int j = 0; j < field.length; j++) {
                for (int k = 0; k < rects.length; k++) {
                    if (rects[k].cX() >= order.l + j*side/3 && rects[k].cX() <= order.l + (j+1)*side/3 &&
                            rects[k].cY() >= order.t + i*side/3 && rects[k].cY() <= order.t + (i+1)*side/3) {
                        field[i][j] = groups[k];
                        if (groups[k] == pG[1]) ij1 = new int[]{i,j};
                        if (groups[k] == pG[2]) ij2 = new int[]{i,j};
                    }
                }
            }
        }
        if (MyStrategy.DEBUG) for (int i = 0; i < 3; i++) { System.out.println(Arrays.toString(field[i]));}
        GP pkG1 = bfs(field, mD, ij1[0], ij1[1], pG[0]);
        GP pkG0 = pkG1;
        pkG1 = pkG0.prev;
        // block the same side attachment
        if (pkG1.i == pkG0.i) { mij(pkG0.i, pkG0.j - 1, mD, L+R+U); mij(pkG0.i, pkG0.j + 1, mD, L+R+D); ggsI[1] = MyStrategy.GGS.INX; ggsI[0] = MyStrategy.GGS.INY; }
        else { mij(pkG0.i - 1, pkG0.j, mD, L+D+U); mij(pkG0.i + 1, pkG0.j, mD, R+D+U); ggsI[1] = MyStrategy.GGS.INY; ggsI[0] = MyStrategy.GGS.INX;}
        for (GP p = pkG1; p != null && p.prev != null; p = p.prev) {
            fM(pG[1], p.prev.i, p.prev.j, p.i, p.j, order, gm1, side);
            field[p.i][p.j] = Integer.MAX_VALUE;
        }
        if (MyStrategy.DEBUG) for (int i = 0; i < 3; i++) { System.out.println(Arrays.toString(mD[i]));}
        GP pkG2 = bfs(field, mD, ij2[0], ij2[1], pG[0]);
        for (GP p = pkG2.prev; p != null && p.prev != null; p = p.prev)
            fM(pG[2], p.prev.i, p.prev.j, p.i, p.j, order, gm2, side);
        if (MyStrategy.DEBUG) System.out.println(toString(pkG1));
        if (MyStrategy.DEBUG) System.out.println(toString(pkG2));
    }

    static class GP {
        public GP(int i, int j, GP prev) {
            this.i = i;
            this.j = j;
            this.prev = prev;
        }

        int i,j;
        GP prev;

        @Override
        public String toString() {
            return "(" + i + "," + j + ")";
        }
    }

    final static int L = 1;
    final static int R = 2;
    final static int U = 4;
    final static int D = 8;
    final static int A = L+R+U+D;
}

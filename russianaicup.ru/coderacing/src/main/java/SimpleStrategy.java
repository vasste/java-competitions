import model.*;

import java.util.*;

import static java.lang.Math.*;
import static model.TileType.*;

public final class SimpleStrategy implements Strategy {
    public static final double EPS = .5;
    public int roadTileIndex = 0;
    Map<XY, List<N>> points;
    public N[] roadToWayPoint;

    Map<XY, List<N>> lostPoints;
    public N[] lostRoadToWayPoint;

    public int[][] edges;
    N current;
    N current_1;
    N current_2;
    double margin;
    double tileSize;
    int width, height;
    int lost_track = -1;
    int[][] waypoints;
    int lx = -1, ly = -1;

    private final double BREAK_VALUE = 6.5D * 6.5D * PI;
    private final double BREAK_VALUE_S = 15.5D * 15.5D * PI;

    @Override
    public void move(Car self, World world, Game game, Move move) {
        TileType[][] tileTypes = world.getTilesXY();
        margin = game.getTrackTileMargin();
        tileSize = game.getTrackTileSize();
        width = world.getWidth();
        height = world.getHeight();

        int cx = (int) (self.getX() / game.getTrackTileSize());
        int cy = (int) (self.getY() / game.getTrackTileSize());

        if (lx == -1) lx = cx;
        if (ly == -1) ly = cy;

        if (null == roadToWayPoint) {
            waypoints = world.getWaypoints();
            edges = new int[width*height][width*height];
            for (int j = 0; j < edges.length; j++) {
                Arrays.fill(edges[j], 1);
            }

            List<N> path = new ArrayList<>();
            OUT: while (true) {
                int x = cx;
                path.clear();
                int y = cy, k = 0;
                for (int[] xy : waypoints) {
                    k = buildSegment(tileTypes, world, xy, path, x, y, k);
                    x = xy[0];
                    y = xy[1];
                    N ni = null, ny = null;
                    for (N n : path) {
                        if (ni == null) {
                            ni = n;
                            continue;
                        }
                        if (ny == null) {
                            ny = n;
                            continue;
                        }
                        if (ny.equals(n)) {
                            edges[ni.index()][ny.index()] = 1000;
                            continue OUT;
                        }
                        ny = ni;
                        ni = n;
                    }
                }
                buildSegment(tileTypes, world, waypoints[0], path, x, y, k);
                break;
            }
            roadToWayPoint = path.toArray(new N[path.size()]);
            initPoints(path);
        } else {
            if (lost_track >= 0 && roadToWayPoint.length - 6 < roadTileIndex) {
                roadTileIndex = lost_track;
                lost_track = -1;
                roadToWayPoint = lostRoadToWayPoint;
                points = lostPoints;
            }

            int last = roadTileIndex;
            List<N> ns = points.get(new XY(cx, cy));
            if (ns != null) {
                for (N n : ns) {
                    if (roadTileIndex + 1 == n.i || roadTileIndex == roadToWayPoint.length - 1 && n.i == 0) {
                        roadTileIndex = n.i;
                        break;
                    }
                }
            }

            // lost track
            current = roadToWayPoint[roadTileIndex];
            if (current.x != cx || current.y != cy) {
                if (ns != null) {
                    int diff = Integer.MAX_VALUE;
                    int i = 0;
                    for (N point : ns) {
                        if (Math.abs(point.i - roadTileIndex) < diff) {
                            diff = Math.abs(point.i - roadTileIndex);
                            i = point.i;
                        }
                    }
                    roadTileIndex = i;
                }
            }

            // define turn back
            if (ns == null || last != roadToWayPoint.length - 1 && Math.abs(roadTileIndex - last) > 1) {
                if (lost_track < 0) {
                    lostRoadToWayPoint = roadToWayPoint;
                    lostPoints = points;
                }
                defineTurnBack(world, self, cx, cy, tileTypes);
                move.setBrake(true);
                return;
            }
        }

        if (cx != lx) lx = cx;
        if (cy != ly) ly = cy;

        current = roadToWayPoint[roadTileIndex];
        current_1 = nexti(1) < roadToWayPoint.length ? roadToWayPoint[nexti(1)] : current;
        current_2 = nexti(2) < roadToWayPoint.length ? roadToWayPoint[nexti(2)] : current;
        TileType nextTile = current_1.tt;
        MoveDirection md_current = defineMoveDirection(current, current_1);
        MoveDirection md_current_1 = defineMoveDirection(current_1, current_2);
        MoveDirection md_current_3 = defineMoveDirection(current_2, roadToWayPoint[nexti(3)]);

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        if (self.getRemainingNitroTicks() > 0 || speedModule > 30) {
            for (int i = 0; i < 3; i++) {
                if (md_current != defineMoveDirection(roadToWayPoint[nexti(i)], roadToWayPoint[nexti(i + 1)])) {
                    move.setBrake(true);
                    return;
                }
            }
        }

        move.setEnginePower(1);
        if (turnAround(cx, cy, tileTypes[cx][cy], md_current, self, move, world)) {
            return;
        }

        FB fb = carPoints(self, self.getAngle(), true);
        fire(world, move, self, fb);
        MoveDirection[] mds = defineCarDirection(self, fb);

        boolean choose = speedModule > 6 &&
                (compare(Math.abs(self.getAngle()), PI - PI/6, 0.15) > 0 ||
                        compare(Math.abs(self.getAngle()), PI/6, 0.15) < 0 ||
                        mds[0].vert &&
                                (compare(Math.abs(self.getAngle()), PI/2 + PI/6, 0.15) < 0 ||
                                        compare(Math.abs(self.getAngle()), PI/2 - PI/6, 0.15) > 0));

        MoveDirection md = choose ? md_current_1 : md_current;

        switch (nextTile) {
            case HORIZONTAL:
            case VERTICAL:
                move.setEnginePower(md_current != md_current_3 ? .2 : 1);
                move.setWheelTurn(0);
                if (straightCar(current_1, self, game, move, md_current)) {
                    move.setEnginePower(md_current != md_current_3 ? .2 : 1);
                    return;
                }
                break;
            case CROSSROADS:
                if (md_current != md_current_1) {
                    if (choose) {
                        switch (md) {
                            case Left:
                                if (mds[0] == MoveDirection.Up) nextTile = RIGHT_TOP_CORNER;
                                else if (mds[0] == MoveDirection.Down) nextTile = RIGHT_BOTTOM_CORNER;
                                break;
                            case Up:
                                if (mds[1] == MoveDirection.Right) nextTile = RIGHT_BOTTOM_CORNER;
                                else if (mds[1] == MoveDirection.Left) nextTile = LEFT_BOTTOM_CORNER;
                                break;
                            case Right:
                                if (mds[0] == MoveDirection.Up) nextTile = LEFT_TOP_CORNER;
                                else if (mds[0] == MoveDirection.Down) nextTile = LEFT_BOTTOM_CORNER;
                                break;
                            case Down:
                                if (mds[1] == MoveDirection.Right) nextTile = RIGHT_TOP_CORNER;
                                else if (mds[1] == MoveDirection.Left) nextTile = LEFT_TOP_CORNER;
                                break;
                        }
                        move.setBrake(useBreak(current_1, self));
                    }
                    current_1 = current_2;
                } else {
                    if (straightCar(current_1, self, game, move, md_current_1)) return;
                }
                break;
            case LEFT_HEADED_T:
                if (md_current != md_current_1) {
                    if (choose) {
                        switch (md) {
                            case Right:
                                if (mds[0] == MoveDirection.Down) nextTile = LEFT_BOTTOM_CORNER;
                                else if (mds[0] == MoveDirection.Up) nextTile = LEFT_TOP_CORNER;
                                break;
                            case Up:
                                nextTile = LEFT_TOP_CORNER;
                                break;
                            case Down:
                                nextTile = LEFT_BOTTOM_CORNER;
                                break;
                        }
                        move.setBrake(useBreak(current_1, self));
                    }
                    current_1 = current_2;
                    break;
                } else if (straightCar(current_1, self, game, move, md_current_1)) return;
                break;
            case RIGHT_HEADED_T:
                if (md_current != md_current_1) {
                    if (choose) {
                        switch (md) {
                            case Left:
                                if (mds[0] == MoveDirection.Down) nextTile = RIGHT_BOTTOM_CORNER;
                                else if (mds[0] == MoveDirection.Up) nextTile = RIGHT_TOP_CORNER;
                                break;
                            case Up:
                                nextTile = RIGHT_TOP_CORNER;
                                break;
                            case Down:
                                nextTile = RIGHT_BOTTOM_CORNER;
                                break;
                        }
                        move.setBrake(useBreak(current_1, self));
                    }
                    current_1 = current_2;
                    break;
                } else if (straightCar(current_1, self, game, move, md_current_1)) return;
                break;
            case TOP_HEADED_T:
                if (md_current != md_current_1) {
                    if (choose) {
                        switch (md) {
                            case Up:
                                if (mds[1] == MoveDirection.Left) nextTile = LEFT_BOTTOM_CORNER;
                                else if (mds[1] == MoveDirection.Right) nextTile = RIGHT_BOTTOM_CORNER;
                                break;
                            case Left:
                                nextTile = RIGHT_BOTTOM_CORNER;
                                break;
                            case Right:
                                nextTile = LEFT_BOTTOM_CORNER;
                                break;
                        }
                        move.setBrake(useBreak(current_1, self));
                    }
                    current_1 = current_2;
                    break;
                } else if (straightCar(current_1, self, game, move, md_current_1)) return;
                break;
            case BOTTOM_HEADED_T:
                if (md_current != md_current_1) {
                    if (choose) {
                        switch (md) {
                            case Down:
                                if (mds[1] == MoveDirection.Left) nextTile = LEFT_TOP_CORNER;
                                else if (mds[1] == MoveDirection.Right) nextTile = RIGHT_TOP_CORNER;
                                break;
                            case Left:
                                nextTile = RIGHT_TOP_CORNER;
                                break;
                            case Right:
                                if (mds[0] == MoveDirection.Down) nextTile = RIGHT_TOP_CORNER;
                                else if (mds[0] == MoveDirection.Up) nextTile = LEFT_TOP_CORNER;
                                break;
                        }
                        move.setBrake(useBreak(current_1, self));
                    }
                    current_1 = current_2;
                    break;
                } else if (straightCar(current_1, self, game, move, md_current_1)) return;
        }

        move.setEnginePower(1);
        boolean turn = false;
        double cornerTileOffset = 0.30D * game.getTrackTileSize();
        double nextWaypointX = (current_1.x + .5) * tileSize;
        double nextWaypointY = (current_1.y + .5) * tileSize;

        switch (nextTile) {
            case LEFT_TOP_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                turn = true;
                break;
            case RIGHT_TOP_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY += cornerTileOffset;
                turn = true;
                break;
            case LEFT_BOTTOM_CORNER:
                nextWaypointX += cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                turn = true;
                break;
            case RIGHT_BOTTOM_CORNER:
                nextWaypointX -= cornerTileOffset;
                nextWaypointY -= cornerTileOffset;
                turn = true;
                break;
        }

        if (turn) {
            double angleToWayPoint = self.getAngleTo(nextWaypointX, nextWaypointY);
            move.setWheelTurn(angleToWayPoint * 30 / Math.PI);
            move.setEnginePower(speedModule < 22 ? self.getEnginePower() + ttt : self.getEnginePower());

//            if (speedModule * speedModule * Math.abs(angleToWayPoint) > BREAK_VALUE) {
//                move.setBrake(true);
//            }
            return;
        }

        if (straightCar(current_1, self, game, move, md_current)) return;
        move.setUseNitro(useNitro(self));

        if (!move.isUseNitro() && current_1.tt == current_2.tt) {
            Bonus[] bonuses = world.getBonuses();
            for (Bonus bonus : bonuses) {
                if ((int) (bonus.getX() / tileSize) == current_2.x && (int) (bonus.getY() / tileSize) == current_2.y) {
                    move.setWheelTurn(self.getAngleTo(bonus) * 30.0D / Math.PI);
                }
            }
        }

        if (self.isFinishedTrack()) {
            System.out.println(world.getTick());
        }
    }

    double ttt = 0.035;

    void initPoints(List<N> path) {
        points = new HashMap<>();
        for (N n : path) {
            for (int j = 0; j < 5; j++) {
                XY xy = new XY(n);
                List<N> ns = points.get(xy);
                if (null == ns) {
                    ns = new ArrayList<>();
                    points.put(xy, ns);
                }
                ns.add(n);
            }
        }
    }

    void defineTurnBack(World world, Car self, int cx, int cy, TileType[][] tileTypes) {
        List<N> path = new ArrayList<>();
        buildSegment(tileTypes, world, new int[]{self.getNextWaypointX(), self.getNextWaypointY()}, path, cx, cy, 0);
        int i = 0;
        for (; i < lostRoadToWayPoint.length; i++) {
            N n = lostRoadToWayPoint[i];
            if (n.x == self.getNextWaypointX() && n.y == self.getNextWaypointY()) {
                lost_track = i;
                break;
            }
        }
        points = new HashMap<>();
        roadToWayPoint = path.toArray(new N[path.size() + 5]);
        for (int j = path.size(), k = 0; j < roadToWayPoint.length; j++, k++) {
            int l = lost_track + k < lostRoadToWayPoint.length ? lost_track + k :
                    -(lostRoadToWayPoint.length - lost_track - k);
            roadToWayPoint[j] = lostRoadToWayPoint[Math.max(0, l)];
        }
        initPoints(path);
        roadTileIndex = 0;
    }

    int nexti(int aj) {
        return roadTileIndex + aj < roadToWayPoint.length ? roadTileIndex + aj :
                -(roadToWayPoint.length - roadTileIndex - aj);
    }

    int buildSegment(TileType[][] tileTypes, World world, int[] xy, List<N> path, int x, int y, int k) {
        Road road = new Road(tileTypes, x, y);
        Stack<N> part = road.pathTo(xy[0], xy[1], world);
        N[] roadToWayPoint = new N[part.size()];
        int i = roadToWayPoint.length;
        for (N n : part) {
            roadToWayPoint[--i] = n;
            n.i = i + k;
        }
        Collections.addAll(path, roadToWayPoint);
        return k + part.size();
    }

    double sx = Double.NaN, sy = Double.NaN;
    MoveDirection lastmd = MoveDirection.None;
    int lastmd_i = 0;

    public boolean straightCar(N next, Car self, Game game, Move move, MoveDirection md) {
        if (self.getRemainingNitroTicks() > 0) return false;
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        boolean straight = md.vert && notVertical(self) || !md.vert && notHorizontal(self);
        int steps = speedModule > 20 ? 7 : 5;
        OUT: for (int i = Math.min(roadTileIndex + 1, roadToWayPoint.length), t = 0; t < steps; i++, t++) {
            if (i == roadToWayPoint.length) i = 0;
            MoveDirection fmd = defineMoveDirection(roadToWayPoint[Math.max(0, i-1)], roadToWayPoint[i]);
            straight |= fmd != md;
            if (fmd != md) {
                lastmd = fmd;
                lastmd_i = i-1;
                switch (fmd) {
                    case Right:
                        sx = .3;
                        sy = 0.5;
                        break OUT;
                    case Left:
                        sx = .7;
                        sy = 0.5;
                        break OUT;
                    case Up:
                        sx = .5;
                        sy = .7;
                        break OUT;
                    case Down:
                        sx = .5;
                        sy = .3;
                        break OUT;
                }
            }
        }

        FB fb = carPoints(self, self.getAngle(), true);
        MoveDirection[] mds = defineCarDirection(self, fb);

        if (lastmd_i == roadTileIndex) {
            if ((lastmd == MoveDirection.Right || lastmd == MoveDirection.Left)) {
                sx = .5;
                if (mds[0] == MoveDirection.Up) sy = .3;
                else if (mds[0] == MoveDirection.Down) sy = .7;
            } else if ((lastmd == MoveDirection.Down || lastmd == MoveDirection.Up)) {
                sy = .5;
                if (mds[1] == MoveDirection.Left) sx = .3;
                else if (mds[1] == MoveDirection.Right) sx = .7;
            }
        }

        double nextWX = (next.x + sx) * game.getTrackTileSize();
        double nextWY = (next.y + sy) * game.getTrackTileSize();
        if (straight) {
            double tu = steps == 7 ? 5 : 10;
            double angleToWayPoint = self.getAngleTo(nextWX, nextWY);
            move.setWheelTurn(angleToWayPoint * tu / Math.PI);
            move.setEnginePower(1);
            return true;
        }
        return false;
    }

    boolean turn(TileType tt) {
        return tt == LEFT_BOTTOM_CORNER || tt == RIGHT_BOTTOM_CORNER || tt == LEFT_TOP_CORNER ||
                tt == RIGHT_TOP_CORNER;
    }

    boolean useBreak(N n, Car self) {
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        double x = (n.x + 0.50D) * tileSize;
        double y = (n.y + 0.50D) * tileSize;
        double angleToWayPoint = self.getAngleTo(x, y);
        return speedModule * speedModule * Math.abs(angleToWayPoint) > BREAK_VALUE;
    }

    public int compare(double a, double b, double eps) {
        double r = a - b;
        if (Math.abs(r) < eps) return 0;
        return (int) Math.signum(r);
    }

    public int compare(double a, double b) {
        double r = a - b;
        if (Math.abs(r) < EPS) return 0;
        return (int) Math.signum(r);
    }

    Side[] intersect = new Side[0];

    enum Side {Right, Left, Top, Bottom, None}

    enum MoveDirection {
        Right(false), Left(false), Down(true), Up(true), None(false);

        boolean vert;

        MoveDirection(boolean vert) {
            this.vert = vert;
        }
    }

    MoveDirection defineMoveDirection(N from, N next) {
        MoveDirection md = MoveDirection.None;
        if (from.x > next.x) md = MoveDirection.Left;
        if (from.x < next.x) md = MoveDirection.Right;
        if (from.y < next.y) md = MoveDirection.Down;
        if (from.y > next.y) md = MoveDirection.Up;

        return md;
    }

    boolean notHorizontal(Car self) {
        return !(compare(Math.abs(self.getAngle()), 0, 5e-2) == 0 ||
                compare(Math.abs(self.getAngle()), PI, 5e-2) == 0);
    }

    boolean notVertical(Car self) {
        return compare(Math.abs(self.getAngle()), Math.PI / 2, 5e-2) != 0;
    }

    public double turn;
    public int tick_turn;

    public boolean turnAround(int x, int y, TileType tt, MoveDirection md, Car self, Move move, World world) {
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        FB fb = carPoints(self, self.getAngle(), false);
        List<Point> front = fb.front;

        if (speedModule < 0.5 && intersect.length == 0) {
            intersect = intersect(x, y, tt, front);

            if (intersect.length == 0) {
                double radius = hypot(self.getHeight(), self.getWidth());
                for (Car player : world.getCars()) {
                    if (!player.isTeammate() && roadTileIndex != 0) {
                        double psx = player.getX() - self.getX();
                        double psy = player.getY() - self.getY();
                        double psr = hypot(psx, psy);
                        if (radius >= psr) {
                            FB playerFb = carPoints(player, player.getAngle(), false);
                            intersect = intersect(x, y, tt, playerFb.front);
                        }
                    }
                }
            }
        }

        if (tick_turn < 400 && speedModule < 10 && intersect.length > 0 && (speedModule < 4 || up(md, self) || down(md, self)
                || left(md, self) || right(md, self))) {
            move.setEnginePower(-.8);
            turn = 0;
            switch (md) {
                case Right:
                    if (intersect[0] == Side.Bottom) turn = -1;
                    if (intersect[0] == Side.Top || intersect[1] == Side.Left) turn = 1;
                    if (intersect[0] == Side.Top && intersect[1] == Side.Right) turn = 1;
                    if (intersect[0] == Side.Top && intersect[1] == Side.Left) turn = 1;
                    if (intersect[0] == Side.Bottom && intersect[1] == Side.Right) turn = -1;
                    break;
                case Left:
                    if (intersect[0] == Side.Bottom) turn = 1;
                    if (intersect[0] == Side.Top || intersect[1] == Side.Right || intersect[1] == Side.Left) turn = -1;
                    if (intersect[0] == Side.Bottom && intersect[1] == Side.Left) turn = 1;
                    if (intersect[0] == Side.Top && intersect[1] == Side.Left) turn = -1;
                    if (intersect[0] == Side.Bottom && intersect[1] == Side.Right) turn = 1;
                    break;
                case Down:
                    if (intersect[0] == Side.Bottom || intersect[1] == Side.Left) turn = 1;
                    if (intersect[1] == Side.Right) turn = -1;
                    if (intersect[0] == Side.Top && intersect[1] == Side.Left) turn = 1;
                    break;
                case Up:
                    if (intersect[1] == Side.Left) turn = -1;
                    if (intersect[1] == Side.Right || intersect[0] == Side.Top) turn = 1;
                    if (intersect[0] == Side.Bottom && intersect[1] == Side.Left) turn = 1;
                    break;
            }

            move.setWheelTurn(turn);
            tick_turn++;
            if (intersect(x, y, tt, fb.back).length > 0) {
                intersect = new Side[0];
                move.setBrake(true);
                tick_turn = 0;
                return false;
            }
            return true;
        }
        if (intersect.length > 0) {
            move.setBrake(true);
            move.setWheelTurn(-turn);
            intersect = new Side[0];
            tick_turn = 0;
            return true;
        }
        tick_turn = 0;
        return false;
    }

    FB carPoints(Car self, double angle, boolean frontBack) {
        double h = self.getHeight();
        double w = self.getWidth();
        double radius = hypot(h, w) / 2;
        double ta = Math.asin(h / (2 * radius));

        double fxa = self.getX() + cos(-ta + angle) * radius;
        double fya = self.getY() + sin(-ta + angle) * radius;

        double fxb = self.getX() + cos(ta + angle) * radius;
        double fyb = self.getY() + sin(ta + angle) * radius;

        List<Point> front;
        if (frontBack) {
            front = Arrays.asList(new Point(fxa, fya), new Point(fxb, fyb));
        } else front = points(fxa, fya, fxb, fyb);

        double bxa = self.getX() + cos(PI + ta + angle) * radius;
        double bya = self.getY() + sin(PI + ta + angle) * radius;

        double bxb = self.getX() + cos(PI - ta + angle) * radius;
        double byb = self.getY() + sin(PI - ta + angle) * radius;

        List<Point> back;
        if (frontBack) {
            back = Arrays.asList(new Point(bxa, bya), new Point(bxb, byb));
        } else back = points(bxa, bya, bxb, byb);

        return new FB(back, front);
    }

    Side[] intersect(int x, int y, TileType tt, List<Point> points) {
        Side[] intersect = new Side[0];
        A:
        for (Rect rect : rectangles(tt, x, y)) {
            for (Point point : points) {
                intersect = rect.contains(point);
                if (intersect.length > 0)
                    break A;
            }
        }
        return intersect;
    }

    List<Point> points(double fxa, double fya, double fxb, double fyb) {

        List<Point> points = new ArrayList<>();
        double dx  = Math.abs(fxa - fxb) / 10;
        double dy = Math.abs(fya - fyb) / 10;

        for (int i = 2; i <= 10; i++) {
            points.add(new Point(Math.min(fxa, fxb) + dx*i, Math.min(fya, fyb) + dy*i));
        }
        points.add(new Point(fxa, fya));
        points.add(new Point(fxb, fyb));
        return points;
    }

    boolean useNitro(Car self) {
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        MoveDirection md = defineMoveDirection(current, current_1);
        if (notVertical(self) && notHorizontal(self)) return false;
        for (int i = roadTileIndex + 1; i - roadTileIndex < 5 && i < roadToWayPoint.length; i++) {
            if (md != defineMoveDirection(roadToWayPoint[Math.max(0, i - 1)], roadToWayPoint[i])) {
                return false;
            }
        }

        return false;
//        return compare(self.getWheelTurn(), 0, 1e-1) == 0 &&
//                !(md.vert && notVertical(self) || !md.vert && notHorizontal(self)) &&
//                speedModule > .5 && self.getNitroChargeCount() > 0 && self.getRemainingOiledTicks() == 0;
    }

    boolean up(MoveDirection md, Car self) {
        return md == MoveDirection.Up && (!(Math.abs(self.getAngle()) < PI / 2 + PI / 6 &&
                Math.abs(self.getAngle()) > PI / 2 - PI / 6));
    }

    boolean down(MoveDirection md, Car self) {
        return md == MoveDirection.Down && (!(Math.abs(self.getAngle()) < PI / 2 + PI / 6 &&
                Math.abs(self.getAngle()) > PI / 2 - PI / 6));
    }

    boolean left(MoveDirection md, Car self) {
        return md == MoveDirection.Left && (!(Math.abs(self.getAngle()) > PI - PI / 6 &&
                Math.abs(self.getAngle()) < PI + PI / 6));
    }

    boolean right(MoveDirection md, Car self) {
        return md == MoveDirection.Right && (!(self.getAngle() < PI / 6 &&
                self.getAngle() > -PI / 6));
    }

    MoveDirection[] defineCarDirection(Car self, FB fb) {
        MoveDirection up = MoveDirection.None;
        MoveDirection lr = MoveDirection.None;
        boolean dy = Math.abs(fb.fA().y - fb.bA().y) > self.getWidth()/4;
        boolean dx = Math.abs(fb.fA().x - fb.bA().x) > self.getWidth()/4;
        if (fb.fA().y > fb.bA().y && dy) up = MoveDirection.Down;
        if (fb.fA().y < fb.bA().y && dy) up = MoveDirection.Up;
        if (fb.fA().x < fb.bA().x && dx) lr = MoveDirection.Left;
        if (fb.fA().x > fb.bA().x && dx) lr = MoveDirection.Right;
        return new MoveDirection[]{up, lr};
    }

    void fire(World world, Move move, Car self, FB fb) {
        MoveDirection[] mds = defineCarDirection(self, fb);
        MoveDirection up = mds[0];
        MoveDirection lr = mds[1];
        for (Car player : world.getCars()) {
            if (!player.isTeammate()) {
                if (lr == MoveDirection.Left &&
                        player.getX() - self.getX() > self.getWidth() &&
                        player.getX() - self.getX() < tileSize &&
                        include(player.getY(), fb.bA().y, fb.bB().y)) {

                    if (self.getOilCanisterCount() > 0) move.setSpillOil(true);
                }
                if (lr == MoveDirection.Right &&
                        self.getX() - player.getX() > self.getWidth() &&
                        self.getX() - player.getX() < tileSize &&
                        include(player.getY(), fb.bA().y, fb.bB().y)) {

                    if (self.getOilCanisterCount() > 0) move.setSpillOil(true);
                }
                if (lr == MoveDirection.Left &&
                        player.getX() < self.getX() &&
                        self.getX() - player.getX() < tileSize
                        && include(player.getY(), fb.fA().y, fb.fB().y) &&
                        compare(self.getAngleTo(player), 0.01) == 0) {

                    if (self.getProjectileCount() > 0) move.setThrowProjectile(true);
                }
                if (lr == MoveDirection.Right &&
                        player.getX() > self.getX() &&
                        player.getX() - self.getX() < tileSize &&
                        include(player.getY(), fb.fA().y, fb.fB().y) &&
                        compare(self.getAngleTo(player), 0.01) == 0) {

                    if (self.getProjectileCount() > 0) move.setThrowProjectile(true);
                }
                if (up == MoveDirection.Up &&
                        player.getY() - self.getY() > self.getWidth() &&
                        player.getY() - self.getY() < tileSize  &&
                        include(player.getX(), fb.bA().x, fb.bB().x)) {

                    if (self.getOilCanisterCount() > 0) move.setSpillOil(true);
                }
                if (up == MoveDirection.Down &&
                        self.getY() - player.getY() > self.getWidth() &&
                        self.getY() - player.getY() < tileSize &&
                        include(player.getX(), fb.bA().x, fb.bB().x)) {

                    if (self.getOilCanisterCount() > 0) move.setSpillOil(true);
                }

                if (up == MoveDirection.Up && self.getY() > player.getY() &&
                        self.getY() - player.getY() < tileSize &&
                        include(player.getX(), fb.fA().x, fb.fB().x) &&
                        compare(self.getAngleTo(player), 0, 0.01) == 0) {

                    if (self.getProjectileCount() > 0) move.setThrowProjectile(true);
                }
                if (up == MoveDirection.Down && player.getY() > self.getY() &&
                        player.getY() - self.getY() < tileSize &&
                        include(player.getX(), fb.fA().x, fb.fB().x) &&
                        compare(self.getAngleTo(player), 0.01) == 0) {

                    if (self.getProjectileCount() > 0) move.setThrowProjectile(true);
                }
            }
        }
        if (self.isFinishedTrack()) {
            System.out.println(world.getTick());
        }
    }

    boolean include(double x, double a, double b) {
        return x >= Math.min(a, b) - width/2 && x <= Math.max(a, b) + width/2;
    }

    Rect[] rectangles(TileType tt, int x, int y) {
        double lx = x * tileSize;
        double ly = y * tileSize;

        double rx = lx + tileSize;
        double ry = ly + tileSize;

        switch (tt) {
            case RIGHT_TOP_CORNER:
                return new Rect[]{bottom(lx, ly, rx, margin),
                        right(ly, rx, ry, margin),
                        leftTopSquare(lx, ry, margin)};
            case RIGHT_BOTTOM_CORNER:
                return new Rect[]{right(ly, rx, ry, margin),
                        top(lx, rx, ry, margin),
                        leftBottomSquare(lx, ly, margin)};
            case LEFT_TOP_CORNER:
                return new Rect[]{left(lx, ly, ry, margin),
                        bottom(lx, ly, rx, margin),
                        rightTopSquare(rx, ry, margin)};
            case LEFT_BOTTOM_CORNER:
                return new Rect[]{left(lx, ly, ry, margin),
                        top(lx, rx, ry, margin),
                        rightBottomSquare(rx, ly, margin)};
            case HORIZONTAL:
                return new Rect[]{top(lx, rx, ry, margin),
                        bottom(lx, ly, rx, margin)};
            case VERTICAL:
                return new Rect[]{right(ly, rx, ry, margin),
                        left(lx, ly, ry, margin)};
            case TOP_HEADED_T:
                return new Rect[]{top(lx, rx, ry, margin),
                        leftBottomSquare(lx, ly, margin),
                        rightBottomSquare(rx, ly, margin)};
            case BOTTOM_HEADED_T:
                return new Rect[]{bottom(lx, ly, rx, margin),
                        leftTopSquare(lx, ry, margin),
                        rightTopSquare(rx, ry, margin)};
            case CROSSROADS:
                return new Rect[]{leftBottomSquare(lx, ly, margin),
                        rightBottomSquare(rx, ly, margin),
                        leftTopSquare(lx, ry, margin),
                        rightTopSquare(rx, ry, margin)};
            case LEFT_HEADED_T:
                return new Rect[]{right(ly, rx, ry, margin),
                        leftBottomSquare(lx, ly, margin),
                        leftTopSquare(lx, ry, margin)};
            case RIGHT_HEADED_T:
                return new Rect[]{left(lx, ly, ry, margin),
                        rightBottomSquare(rx, ly, margin),
                        rightTopSquare(rx, ry, margin)};
        }
        return null;
    }

    Rect leftBottomSquare(double lx, double ly, double m) {
        return new Rect(lx, ly, lx + m, ly + m, Side.Bottom, Side.Left);
    }

    Rect rightTopSquare(double rx, double ry, double m) {
        return new Rect(rx - m, ry - m, rx, ry, Side.Top, Side.Right);
    }

    Rect rightBottomSquare(double rx, double ly, double m) {
        return new Rect(rx - m, ly, rx, ly + m, Side.Bottom, Side.Right);
    }

    Rect leftTopSquare(double lx, double ry, double m) {
        return new Rect(lx, ry - m, lx + m, ry, Side.Top, Side.Left);
    }

    Rect bottom(double lx, double ly, double rx, double m) {
        return new Rect(lx, ly, rx, ly + m, Side.Bottom, null);
    }

    Rect right(double ly, double rx, double ry, double m) {
        return new Rect(rx - m, ly, rx, ry, null, Side.Right);
    }

    Rect left(double lx, double ly, double ry, double m) {
        return new Rect(lx, ly, lx + m, ry, null, Side.Left);
    }

    Rect top(double lx, double rx, double ry, double m) {
        return new Rect(lx, ry - m, rx, ry, Side.Top, null);
    }

    class FB {
        List<Point> front;
        List<Point> back;

        public FB(List<Point> back, List<Point> front) {
            this.back = back;
            this.front = front;
        }

        Point fA() {
            return front.get(0);
        }

        Point fB() {
            return front.get(front.size() - 1);
        }

        Point bA() {
            return back.get(0);
        }

        Point bB() {
            return back.get(back.size() - 1);
        }
    }

    class Rect {
        Side lr;
        Side up;
        double lx, ly, rx, ry;

        public Rect(double lx, double ly, double rx, double ry, Side up, Side lr) {
            this.lx = lx;
            this.ly = ly;
            this.rx = rx;
            this.ry = ry;
            this.lr = lr == null ? Side.None : lr;
            this.up = up == null ? Side.None : up;
        }

        public Side[] contains(Point point) {
            double x0 = lx;
            double y0 = ly;
            boolean contains = (compare(point.x, x0) >= 0 &&
                    compare(point.y, y0) >= 0 && compare(point.x, rx) <= 0 &&
                    compare(point.y, ry) <= 0);
            if (contains) {
                return new Side[]{up, lr};
            }
            return new Side[0];
        }
    }

    class Point {
        double x, y;

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    class Road {
        private PriorityQueue<N> pq;
        private N[] edgeTo;
        private int[] distTo;
        private TileType[][] tileTypes;

        public Road(TileType[][] tileTypes, int sx, int sy) {
            int titles = width * height;
            distTo = new int[titles];
            edgeTo = new N[titles];
            this.tileTypes = tileTypes;
            Arrays.fill(distTo, Integer.MAX_VALUE);
            pq = new PriorityQueue<>();
            N sn = new N(sx, sy, 0, tileTypes[sx][sy]);
            distTo[sn.index()] = 0;
            pq.add(sn);
            while (!pq.isEmpty()) {
                N from = pq.remove();
                Set<N> adj = evaluateNeighbours(from, width, height);
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
                    w.c = distTo[wi];
                    pq.add(w);
                } else {
                    w.c = distTo[wi];
                    pq.add(w);
                }
            }
        }

        private Set<N> evaluateNeighbours(N n, int w, int h) {
            Set<N> adj = new HashSet<N>(4);
            switch (tileTypes[n.x][n.y]) {
                case VERTICAL:
                    top(n, h, adj);
                    bottom(n, adj);
                    break;
                case HORIZONTAL:
                    left(n, adj);
                    right(n, w, adj);
                    break;
                case TOP_HEADED_T:
                    left(n, adj);
                    right(n, w, adj);
                    bottom(n, adj);
                    break;
                case BOTTOM_HEADED_T:
                    left(n, adj);
                    right(n, w, adj);
                    top(n, h, adj);
                    break;
                case RIGHT_TOP_CORNER:
                    top(n, h, adj);
                    left(n, adj);
                    break;
                case LEFT_TOP_CORNER:
                    top(n, h, adj);
                    right(n, w, adj);
                    break;
                case RIGHT_BOTTOM_CORNER:
                    left(n, adj);
                    bottom(n, adj);
                    break;
                case LEFT_BOTTOM_CORNER:
                    right(n, w, adj);
                    bottom(n, adj);
                    break;
                case CROSSROADS:
                    left(n, adj);
                    right(n, w, adj);
                    top(n, h, adj);
                    bottom(n, adj);
                    break;
                case LEFT_HEADED_T:
                    left(n, adj);
                    top(n, h, adj);
                    bottom(n, adj);
                    break;
                case RIGHT_HEADED_T:
                    right(n, w, adj);
                    top(n, h, adj);
                    bottom(n, adj);
                    break;
            }
            return adj;
        }

        public Stack<N> pathTo(int x, int y, World world) {
            int v = y * world.getWidth() + x;
            Stack<N> path = new Stack<N>();
            for (N e = edgeTo[v]; e != null; e = edgeTo[e.index()]) {
                path.push(e);
            }
            return path;
        }

        private void top(N n, int h, Set<N> adj) {
            int t = Math.min(h - 1, n.y + 1);
            if (t != n.y && !isH(n.x, t) && !isE(n.x, t))
                adj.add(new N(n.x, t, Integer.MAX_VALUE, tileTypes[n.x][t]));
        }

        private void bottom(N n, Set<N> adj) {
            int b = Math.max(0, n.y - 1);
            if (b != n.y && !isH(n.x, b) && !isE(n.x, b))
                adj.add(new N(n.x, b, Integer.MAX_VALUE, tileTypes[n.x][b]));
        }

        private void left(N n, Set<N> adj) {
            int l = Math.max(0, n.x - 1);
            if (l != n.x && !isV(l, n.y) && !isE(l, n.y))
                adj.add(new N(l, n.y, Integer.MAX_VALUE, tileTypes[l][n.y]));
        }

        private void right(N n, int w, Set<N> adj) {
            int r = Math.min(w - 1, n.x + 1);
            if (r != n.x && !isV(r, n.y) && !isE(r, n.y))
                adj.add(new N(r, n.y, Integer.MAX_VALUE, tileTypes[r][n.y]));
        }

        private boolean isH(int x, int y) {
            return tileTypes[x][y] == HORIZONTAL;
        }

        private boolean isV(int x, int y) {
            return tileTypes[x][y] == VERTICAL;
        }

        private boolean isE(int x, int y) {
            return tileTypes[x][y] == EMPTY;
        }
    }

    class XY {
        final int x,y;

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        public XY(N n) {
            this.x = n.x;
            this.y = n.y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XY xy = (XY) o;
            return Objects.equals(x, xy.x) &&
                    Objects.equals(y, xy.y);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    class N implements Comparable<N> {
        int x, y, c, i;
        TileType tt;

        public N(int x, int y, int c, TileType tt) {
            this.x = x;
            this.y = y;
            this.c = c;
            this.tt = tt;
        }

        int index() {
            return y * width + x;
        }

        @Override
        public int compareTo(N o2) {
            return Integer.compare(c, o2.c);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            N n = (N) o;
            return Objects.equals(x, n.x) &&
                    Objects.equals(y, n.y);
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        @Override
        public String toString() {
            return x + "|" + y + "|" + tt;
        }
    }
}

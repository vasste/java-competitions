import model.*;

import java.util.*;

import static java.lang.Math.*;
import static model.TileType.*;

public final class MyStrategy implements Strategy {
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
    PathPoint[] waypoints;
    int lx = -1, ly = -1;
    TileType[][] tileTypes;

    int ladder_start;
    int foundWayPoints = 0;

    @Override
    public void move(Car self, World world, Game game, Move move) {
        tileTypes = world.getTilesXY();
        margin = game.getTrackTileMargin();
        tileSize = game.getTrackTileSize();
        width = world.getWidth();
        height = world.getHeight();

        int cx = (int) (self.getX() / game.getTrackTileSize());
        int cy = (int) (self.getY() / game.getTrackTileSize());

        if (lx == -1) lx = cx;
        if (ly == -1) ly = cy;

        if (null == roadToWayPoint) {
            int[][] wp = world.getWaypoints();
            waypoints = new PathPoint[wp.length];
            for (int i = 0; i < waypoints.length; i++) {
                waypoints[i] = new PathPoint(wp[i][0], wp[i][1]);
            }
            edges = new int[width * height][width * height];
            for (int[] edge : edges) {
                Arrays.fill(edge, 1);
            }
        }

//        if (null == roadToWayPoint ||
//                roadTileIndex == roadToWayPoint.length - 1 && foundWayPoints != world.getWaypoints().length) {
            List<N> path = buildPath(tileTypes, waypoints, world, waypoints[0].x, waypoints[0].y);
            // improve path
            initPoints(path);
            roadToWayPoint = path.toArray(new N[path.size()]);
//        } else {
//            if (lost_track >= 0 && roadToWayPoint.length - 6 < roadTileIndex) {
//                roadTileIndex = lost_track;
//                lost_track = -1;
//                roadToWayPoint = lostRoadToWayPoint;
//                points = lostPoints;
//            }
//
//            int last = roadTileIndex;
            List<N> ns = points.get(new XY(cx, cy));
//            if (ns != null) {
//                for (N n : ns) {
//                    if (roadTileIndex + 1 == n.i || roadTileIndex == roadToWayPoint.length - 1 && n.i == 0) {
//                        roadTileIndex = n.i;
//                        break;
//                    }
//                }
//            }
//
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
//
//            // define turn back
//            if (ns == null || last != roadToWayPoint.length - 1 && Math.abs(roadTileIndex - last) > 1) {
//                if (lost_track < 0) {
//                    lostRoadToWayPoint = roadToWayPoint;
//                    lostPoints = points;
//                }
//                defineTurnBack(world, self, new PathPoint(cx, cy), tileTypes);
//                move.setBrake(true);
//                return;
//            }
//        }

        if (cx != lx) lx = cx;
        if (cy != ly) ly = cy;

        current = roadToWayPoint[roadTileIndex];
        current_1 = nexti(1) < roadToWayPoint.length ? roadToWayPoint[nexti(1)] : current;
        current_2 = nexti(2) < roadToWayPoint.length ? roadToWayPoint[nexti(2)] : current;
        N current_3 = nexti(3) < roadToWayPoint.length ? roadToWayPoint[nexti(3)] : current;
        TileType nextTile = current_1.tt;
        MoveDirection md_current = defineMoveDirection(current, current_1);
        MoveDirection md_current_1 = defineMoveDirection(current_1, current_2);
        MoveDirection md_current_2 = defineMoveDirection(current_2, current_3);

        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        move.setEnginePower(1);

        FB fb = carPoints(self, self.getAngle(), true);
        fire(world, move, self, fb, false);
        MoveDirection[] mds = defineCarDirection(self, fb, .5);

        if (turnAround(md_current, self, move, world, mds)) {
            return;
        }

        double speed_to_break = self.getType() == CarType.BUGGY ? 30 : 25;
        int before = self.getType() == CarType.BUGGY ? 3 : 4;
        if (self.getRemainingNitroTicks() > 0 || speedModule > speed_to_break) {
            for (int i = 0; i < before; i++) {
                if (md_current != defineMoveDirection(roadToWayPoint[nexti(i)], roadToWayPoint[nexti(i + 1)])) {
                    move.setBrake(speedModule > 20);
                }
            }
        }

        // define ladder
        int ladder_start_x = -1, ladder_start_y = -1;
        int[] x = new int[roadToWayPoint.length];
        int[] y = new int[roadToWayPoint.length];

        int ladder_size = 0;
        int lsx = 0, lsy = 0;
        if (roadToWayPoint[roadTileIndex].tt != VERTICAL && roadToWayPoint[roadTileIndex].tt != HORIZONTAL) {
            for (int i = roadTileIndex; true; i++) {
                if (i == roadToWayPoint.length) i = 0;
                if (-1 == ladder_start_x) {
                    ladder_start_x = roadToWayPoint[i].x;
                    ladder_start_y = roadToWayPoint[i].y;
                    continue;
                }

                x[ladder_size] = roadToWayPoint[i].x - ladder_start_x;
                y[ladder_size] = roadToWayPoint[i].y - ladder_start_y;

                if (lsx == 0) lsx = roadToWayPoint[i].x - ladder_start_x;
                if (lsy == 0) lsy = roadToWayPoint[i].y - ladder_start_y;

                if (ladder_size > 0) {
                    if (x[ladder_size - 1] == x[ladder_size] || (x[ladder_size] != 0 && lsx != x[ladder_size])) break;
                    if (y[ladder_size - 1] == y[ladder_size] || (y[ladder_size] != 0 && lsy != y[ladder_size])) break;
                }

                ladder_start_x = roadToWayPoint[i].x;
                ladder_start_y = roadToWayPoint[i].y;
                ladder_size++;
            }
        }

        ladder_size--;
        if (ladder_size > 2 && ladder_start == 0) {
            ladder_start = roadTileIndex;
        }

        if (ladder_size > 2) {
            MoveDirection[] mds_ladder = defineCarDirection(self, fb, 0.5);
            N ladder_finish = roadToWayPoint[roadTileIndex + ladder_size];
            double sx = 0, sy = 0;
            switch (ladder_finish.tt) {
                case LEFT_TOP_CORNER:
                case BOTTOM_HEADED_T:
                    if (mds_ladder[1] == MoveDirection.Left) {sx = .5; sy = 1;}
                    if (mds_ladder[1] == MoveDirection.Right) {sx = 1; sy = .5;}
                    break;
                case TOP_HEADED_T:
                    sx = .5; sy = 0;
                    break;
                case LEFT_BOTTOM_CORNER:
                case RIGHT_HEADED_T:
                    if (mds_ladder[1] == MoveDirection.Left) {sx = .5; sy = 0;}
                    if (mds_ladder[1] == MoveDirection.Right) {sx = 1; sy = .5;}
                    break;
                case RIGHT_BOTTOM_CORNER:
                    if (mds_ladder[1] == MoveDirection.Left) {sx = 0; sy = .5;}
                    if (mds_ladder[1] == MoveDirection.Right) {sx = .5; sy = 0;}
                    break;
                case LEFT_HEADED_T:
                case RIGHT_TOP_CORNER:
                    if (mds_ladder[1] == MoveDirection.Left) {sx = 0; sy = .5;}
                    if (mds_ladder[1] == MoveDirection.Right) {sx = .5; sy = 1;}
                    break;
            }

            double nextWaypointX = (ladder_finish.x + sx) * tileSize;
            double nextWaypointY = (ladder_finish.y + sy) * tileSize;
            double angleToWayPoint = self.getAngleTo(nextWaypointX, nextWaypointY);
            move.setWheelTurn(angleToWayPoint * 5 / Math.PI);
            move.setEnginePower(1);

            move.setUseNitro(!move.isBrake() && ladder_size > 4 &&
                    compare(self.getWheelTurn(), 0, 1e-1) == 0 && roadTileIndex >= ladder_start + 2);

            if (speedModule * speedModule * Math.abs(angleToWayPoint) > getBreak(self, 2)) {
                move.setBrake(!move.isUseNitro());
            }
            return;
        } else {
            ladder_start = 0;
        }

        move.setEnginePower(1);
        switch (nextTile) {
            case HORIZONTAL:
            case VERTICAL:
                move.setWheelTurn(0);
                if (straightCar(self, world, game, move, md_current, current, current_1, current_2)) return;
                break;
            case CROSSROADS:
                if (md_current != md_current_1) {
                    switch (md_current_1) {
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
                } else {
                    if (straightCar(self, world, game, move, md_current_1, current, current_1, current_2)) return;
                }
                break;
            case LEFT_HEADED_T:
                if (md_current != md_current_1) {
                    switch (md_current_1) {
                        case Left:
                            if (mds[0] == MoveDirection.Down) nextTile = RIGHT_BOTTOM_CORNER;
                            else if (mds[0] == MoveDirection.Up) nextTile = RIGHT_TOP_CORNER;
                            break;
                        case Up:
                            nextTile = RIGHT_BOTTOM_CORNER;
                            break;
                        case Down:
                            nextTile = RIGHT_TOP_CORNER;
                            break;
                    }
                } else if (straightCar(self, world, game, move, md_current_1, current, current_1, current_2)) return;
                break;
            case RIGHT_HEADED_T:
                if (md_current != md_current_1) {
                    switch (md_current_1) {
                        case Right:
                            if (mds[0] == MoveDirection.Down) nextTile = LEFT_BOTTOM_CORNER;
                            else if (mds[0] == MoveDirection.Up) nextTile = LEFT_TOP_CORNER;
                            break;
                        case Up:
                            nextTile = LEFT_BOTTOM_CORNER;
                            break;
                        case Down:
                            nextTile = LEFT_TOP_CORNER;
                            break;
                    }
                } else if (straightCar(self, world, game, move, md_current_1, current, current_1, current_2)) return;
                break;
            case TOP_HEADED_T:
                if (md_current != md_current_1) {
                    switch (md_current_1) {
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

                } else if (straightCar(self, world, game, move, md_current_1, current, current_1, current_2)) return;
                break;
            case BOTTOM_HEADED_T:
                if (md_current != md_current_1) {
                    switch (md_current_1) {
                        case Down:
                            if (mds[1] == MoveDirection.Left) nextTile = LEFT_TOP_CORNER;
                            else if (mds[1] == MoveDirection.Right) nextTile = RIGHT_TOP_CORNER;
                            break;
                        case Left:
                            nextTile = RIGHT_TOP_CORNER;
                            break;
                        case Right:
                            nextTile = LEFT_TOP_CORNER;
                            break;
                    }
                    break;
                } else if (straightCar(self, world, game, move, md_current_1, current, current_1, current_2)) return;
        }

        int turns = md_current_1 != md_current && md_current_1 != md_current_2 ? 2 : 1;
        double[] nextWaypoint = angle(nextTile, current_1, .3);
        turnTicks = nextWaypoint.length == 2 ? 1 : 0;
        double nextWaypointX = 0, nextWaypointY = 0;
        if (nextWaypoint.length > 0) {
            nextWaypointX = nextWaypoint[0];
            nextWaypointY = nextWaypoint[1];
        }

        double turn_at_speed = self.getType() == CarType.BUGGY ? 1.2 : 1.2;
        double angle_at_turn = self.getType() == CarType.BUGGY ? 28.0 : 20;
        double turn_at_2 = self.getType() == CarType.BUGGY ? 1.4 : 1.5;
        if (turnTicks > 0) {
            double angleToWayPoint = self.getAngleTo(nextWaypointX, nextWaypointY);
            helpTurn = turnTicks == 1 && (notHorizontal(self) || notVertical(self)) ?
                    (turns == 2 && speedModule > 15 ? turn_at_2 : turn_at_speed) : 1;
            move.setWheelTurn(angleToWayPoint * helpTurn * angle_at_turn / Math.PI);
            move.setEnginePower(1);

            if (speedModule * speedModule * Math.abs(angleToWayPoint) > getBreak(self, turns)) {
                move.setBrake(true);
            }
            turnTicks++;
            return;
        }

        useNitro(self, world, fb, move);
        if (straightCar(self, world, game, move, md_current, current, current_1, current_2)) return;

        if (!move.isUseNitro() && current_1.tt == current_2.tt) {
            Bonus[] bonuses = world.getBonuses();
            for (Bonus bonus : bonuses) {
                if ((int) (bonus.getX() / tileSize) == current_2.x && (int) (bonus.getY() / tileSize) == current_2.y) {
                    move.setWheelTurn(self.getAngleTo(bonus) * 60.0D / Math.PI);
                }
            }
        }
    }

    int turnTicks = 0;
    double helpTurn = 1;

    int turnsInPath(TileType[][] tileTypes, List<N> path) {
        int turns = 0;
        for (int i = 1; i < path.size() - 1; i++) {
            int ito = i + 1 == path.size() ? 0 : i + 1;
            if (defineMoveDirection(path.get(i - 1), path.get(i)) !=
                    defineMoveDirection(path.get(i), path.get(ito))) {
                N from = path.get(i);
                N to = path.get(ito);
                turns++;
                if (edges[from.y * width  + from.x][to.y * width  + to.x] != 1000)
                    edges[from.y * width  + from.x][to.y * width  + to.x] = 2;
            }
        }
        return turns;
    }

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

    boolean defineTurn(List<N> path) {
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
                return true;
            }
            ny = ni;
            ni = n;
        }
        return false;
    }

    double getBreak(Car self, int turns) {
        if (turns == 1) return self.getType() == CarType.BUGGY ? 5.5D * 6.5D * PI : 5.5D * 5.5D * PI;
        return 3.5D * 3.5D * PI;
    }

    double[] angle(TileType nextTile, N n, double turnAngle) {
        double cornerTileOffset = turnAngle * tileSize;
        double nextWaypointX = (n.x + .5) * tileSize;
        double nextWaypointY = (n.y + .5) * tileSize;
        boolean turn = false;

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
        return turn ? new double[]{nextWaypointX, nextWaypointY} : new double[0];
    }

    void defineTurnBack(World world, Car self, PathPoint from, TileType[][] tileTypes) {
        List<N> path = new ArrayList<>();
        buildSegment(tileTypes, world, new PathPoint(self.getNextWaypointX(), self.getNextWaypointY()), path, from, 0);
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

    int buildSegment(TileType[][] tileTypes, World world, PathPoint to, List<N> path, PathPoint from, int k) {
        Road road = new Road(tileTypes, from);
        Stack<N> part = road.pathTo(to, world);
        N[] roadToWayPoint = new N[part.size()];
        int i = roadToWayPoint.length;
        for (N n : part) {
            roadToWayPoint[--i] = n;
            n.i = i + k;
        }
        Collections.addAll(path, roadToWayPoint);
        return k + part.size();
    }

    List<N> buildPath(TileType[][] tileTypes, PathPoint[] waypoints, World world, int cx, int cy) {
        List<N> path = new ArrayList<>();
        boolean turnFound = false;
        OUT:
        for (int j = 0; j < 5; j++) {
            path.clear();
            int k = 0;
            PathPoint from = new PathPoint(cx, cy);
            for (int i = 1; i < waypoints.length; i++) {
                PathPoint xy = waypoints[i];
                k = buildSegment(tileTypes, world, xy, path, from, k);
                from = xy;
                turnFound = defineTurn(path);
                if (turnFound) continue OUT;
            }
            buildSegment(tileTypes, world, waypoints[0], path, from, k);
            if (defineTurn(path)) continue;
            break;
        }
        return path;
    }

    double sx = Double.NaN, sy = Double.NaN;

    public boolean straightCar(Car self, World world, Game game, Move move, MoveDirection md, N... n) {
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());
        int steps = speedModule > 25 ? 7 : 5;
        int t = 0;
        boolean straight = md.vert && notVertical(self) || !md.vert && notHorizontal(self);
        straight |= (t = straightAngle(n[1], game, speedModule, md)) > 0;

        if (straight) {
            double angleToWayPoint = self.getAngleTo(sx, sy);
            double tu = steps == 7 ? 10 : t == 0 ? 25 : 15;
            if (self.getRemainingNitroTicks() > 0) tu = 1;
            move.setWheelTurn(angleToWayPoint * tu / Math.PI);
            move.setEnginePower(1);

            if (speedModule * speedModule * Math.abs(angleToWayPoint) > 7.5D * 7.5D * PI) {
                move.setBrake(self.getRemainingNitroTicks() == 0);
            }
            return true;
        }
        return false;
    }

    int straightAngle(N next, Game game, double speedModule, MoveDirection md) {
        int steps = speedModule > 25 ? 7 : 5;
        OUT: for (int i = Math.min(roadTileIndex + 1, roadToWayPoint.length), t = 0; t < steps; i++, t++) {
            if (i == roadToWayPoint.length) i = 0;
            MoveDirection fmd = defineMoveDirection(roadToWayPoint[Math.max(0, i-1)], roadToWayPoint[i]);
            if (fmd != md) {
                switch (fmd) {
                    case Right:
                        sx = (next.x + .3) * tileSize;
                        sy = (next.y + 0.5D) * tileSize;
                        return t;
                    case Left:
                        sx = (next.x + .7) * tileSize;
                        sy = (next.y + 0.5D) * tileSize;
                        return t;
                    case Up:
                        sx = (next.x + 0.5D) * tileSize;
                        sy = (next.y + .7) * tileSize;
                        return t;
                    case Down:
                        sx = (next.x + 0.5D) * tileSize;
                        sy = (next.y + .3) * tileSize;
                        return t;
                }
            }
        }
        sx = (next.x + 0.50D) * tileSize;
        sy = (next.y + 0.50D) * tileSize;

        return 0;
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
        return !(compare(Math.abs(self.getAngle()), 0, 5e-3) == 0 ||
               compare(Math.abs(self.getAngle()), PI, 5e-3) == 0);
    }

    boolean notVertical(Car self) {
        return compare(Math.abs(self.getAngle()), Math.PI / 2, 5e-3) != 0;
    }

    public double turn;
    public int tick_turn;
    public int tick_after;
    // up, down, right, left
    Point[] velocity = new Point[]{new Point(0, -1), new Point(0, 1), new Point(1, 0), new Point(-1, 0)};

    public boolean turnAround(MoveDirection md, Car self, Move move, World world, MoveDirection[] mds) {
        double speedModule = hypot(self.getSpeedX(), self.getSpeedY());

        FB fb = carPoints(self, self.getAngle(), true);
        if (tick_after > 100 && speedModule < 0.5 && intersect.length == 0 && world.getTick() > 300) {
            intersect = intersect((int)(fb.fA().x/tileSize), (int)(fb.fA().y/tileSize), self, fb);
            if (intersect.length == 0) {
                intersect = intersect((int)(fb.fB().x/tileSize), (int)(fb.fB().y/tileSize), self, fb);
            }

            if (intersect.length == 0) {
                double radius = hypot(self.getHeight(), self.getWidth());
                CarRectangle car = new CarRectangle(self, fb);
                for (Car player : world.getCars()) {
                    if (world.getTick() > 300) {
                        double psx = player.getX() - self.getX();
                        double psy = player.getY() - self.getY();
                        double psr = hypot(psx, psy);
                        if (radius >= psr) {
                            FB playerFb = carPoints(player, player.getAngle(), false);
                            Side lr = Side.None;
                            Side up = Side.None;
                            Result result = collision(car, new CarRectangle(player, playerFb), velocity[0]); //up
                            if (result.maybe) up = Side.Top;
                            result = collision(car, new CarRectangle(player, playerFb), velocity[1]); // down
                            if (result.maybe) up = Side.Bottom;
                            result = collision(car, new CarRectangle(player, playerFb), velocity[2]); // right
                            if (result.maybe) lr = Side.Right;
                            result = collision(car, new CarRectangle(player, playerFb), velocity[3]); // left
                            if (result.maybe) lr = Side.Left;
                            if (lr != Side.None || up != Side.None) intersect = new Side[]{up, lr};
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
                    if (intersect[0] == Side.Top && intersect[1] == Side.Right) turn = -1;
                    break;
                case Up:
                    if (intersect[1] == Side.Left) turn = -1;
                    if (intersect[1] == Side.Right || intersect[0] == Side.Top) turn = 1;
                    if (intersect[0] == Side.Bottom && intersect[1] == Side.Left) turn = -1;
                    if (intersect[0] == Side.Bottom && intersect[1] == Side.Right) turn = 1;
                    break;
            }

            if ((bump(intersect((int)(fb.bA().x/tileSize), (int)(fb.bA().y/tileSize), self, fb), mds) ||
                bump(intersect((int)(fb.bB().x/tileSize), (int)(fb.bB().y/tileSize), self, fb), mds))) {
                intersect = new Side[0];
                move.setBrake(tick_turn > 0);
                tick_turn = 0;
                return false;
            }
            move.setWheelTurn(turn);
            tick_turn++;
            tick_after = 0;
            return true;
        }
        if (intersect.length > 0) {
            move.setBrake(true);
            move.setWheelTurn(-turn);
            intersect = new Side[0];
            tick_turn = 0;
            tick_after++;
            return true;
        }
        tick_turn = 0;
        tick_after++;
        return false;
    }

    boolean bump(Side[] sides, MoveDirection[] mds) {
        return sides.length > 0 && (sides[0] == Side.Bottom && mds[0] == MoveDirection.Down ||
                sides[0] == Side.Top && mds[0] == MoveDirection.Up ||
                sides[1] == Side.Right && mds[1] == MoveDirection.Left ||
                sides[1] == Side.Left && mds[1] == MoveDirection.Right);
    }

    FB carPoints(Car self, double angle, boolean frontBack) {
        double h = self.getHeight();
        double w = self.getWidth();
        double radius = Math.sqrt(h * h + w * w) / 2;
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

    Side[] intersect(int x, int y, Car self, FB fb) {
        CarRectangle car = new CarRectangle(self, fb);
        TileType tt = tileTypes[x][y];
        Side[] intersect = new Side[0];
        OUT: for (Rect rect : rectangles(tt, x, y)) {
            Result result = null;
            switch (rect.lr) {
                case Left:
                    result = collision(car, new CarRectangle(rect), velocity[3]); //left
                    break;
                case Right:
                    result = collision(car, new CarRectangle(rect), velocity[2]); // right
                    break;
            }
            switch (rect.up) {
                case Top:
                    result = collision(car, new CarRectangle(rect), velocity[0]); //up
                    break;
                case Bottom:
                    result = collision(car, new CarRectangle(rect), velocity[1]); //down
                    break;
            }
            if (result.maybe) {
                intersect = rect.sides();
                break;
            }
        }
        return intersect;
    }

    List<Point> points(double fxa, double fya, double fxb, double fyb) {

        List<Point> points = new ArrayList<>();
        double dx  = Math.abs(fxa - fxb) / 5;
        double dy = Math.abs(fya - fyb) / 5;

        for (int i = 2; i <= 5; i++) {
            points.add(new Point(Math.min(fxa, fxb) + dx*i, Math.min(fya, fyb) + dy*i));
        }
        points.add(new Point(fxa, fya));
        points.add(new Point(fxb, fyb));
        return points;
    }

    void useNitro(Car self, World world, FB fb, Move move) {
        MoveDirection md = defineMoveDirection(current, current_1);
        if (notVertical(self) && notHorizontal(self)) return;
        for (int i = roadTileIndex + 1; i - roadTileIndex < 4; i++) {
            if (i == roadToWayPoint.length) i = 0;
            if (md != defineMoveDirection(roadToWayPoint[i],
                    roadToWayPoint[i + 1 == roadToWayPoint.length ? 0 : i + 1])) {
                return;
            }
        }

        move.setUseNitro(compare(self.getWheelTurn(), 0, 1e-1) == 0 && !fire(world, move, self, fb, true) &&
                !(md.vert && notVertical(self) || !md.vert && notHorizontal(self)) &&
                world.getTick() > 300 &&
                self.getRemainingNitroTicks() == 0 && self.getRemainingOiledTicks() == 0);
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

    MoveDirection[] defineCarDirection(Car self, FB fb, double coeff) {
        MoveDirection up = MoveDirection.None;
        MoveDirection lr = MoveDirection.None;
        boolean dy = Math.abs(fb.fA().y - fb.bA().y) > coeff * self.getWidth();
        boolean dx = Math.abs(fb.fA().x - fb.bA().x) > coeff * self.getWidth();
        if (fb.fA().y > fb.bA().y && dy) up = MoveDirection.Down;
        if (fb.fA().y < fb.bA().y && dy) up = MoveDirection.Up;
        if (fb.fA().x < fb.bA().x && dx) lr = MoveDirection.Left;
        if (fb.fA().x > fb.bA().x && dx) lr = MoveDirection.Right;
        return new MoveDirection[]{up, lr};
    }

    boolean fire(World world, Move move, Car self, FB fb, boolean check_nitro) {
        MoveDirection[] mds = defineCarDirection(self, fb, .5);
        MoveDirection up = mds[0];
        MoveDirection lr = mds[1];

        boolean doThrowProjectile = !check_nitro;
        boolean doOldSpile = !check_nitro;
        boolean throwProjectile = false;

        double angleWide = PI/20;
        double right_border = self.getType() == CarType.BUGGY ? Integer.MAX_VALUE : 3*tileSize;
        double left_border = self.getWidth();
        left_border = check_nitro ? 0 : left_border;
        double eps = 0;

        for (Car player : world.getCars()) {
            if (!player.isTeammate()) {
                FB playerFB = carPoints(player, player.getAngle(), true);
                double playerAngle = Math.abs(self.getAngleTo(player));
                double diffx = Math.max(fb.fA().x - playerFB.fA().x, fb.fB().x - playerFB.fB().x);
                double diffy = Math.max(fb.fA().y - playerFB.fA().y, fb.fB().y - playerFB.fB().y);
                double angleWidex = angleWide/(Math.abs(diffx)/tileSize);
                double angleWidey = angleWide/(Math.abs(diffy)/tileSize);
                switch (lr) {
                    case Left:
                        if (between(-diffx, self.getWidth(), tileSize, eps) &&
                            between(player.getY(), fb.bA().y, fb.bB().y, self.getHeight()/2)) {
                            if (self.getOilCanisterCount() > 0) move.setSpillOil(doOldSpile);
                        }

                        if (between(diffx, left_border, right_border, eps) &&
                                between(player.getY(), fb.fA().y, fb.fB().y, self.getWidth()) &&
                            between(playerAngle, angleWidex, 0, 0)) {
                            throwProjectile = true;
                            if (self.getProjectileCount() > 0) move.setThrowProjectile(doThrowProjectile);
                        }
                        break;
                    case Right:
                        if (between(diffx, self.getWidth(), tileSize, eps) &&
                            between(player.getY(), fb.bA().y, fb.bB().y, self.getHeight()/2)) {
                            if (self.getOilCanisterCount() > 0) move.setSpillOil(doOldSpile);
                        }
                        if (between(-diffx, left_border, right_border, eps) &&
                            between(player.getY(), fb.fA().y, fb.fB().y, self.getWidth()) &&
                            between(playerAngle, angleWidex, 0, 0)) {

                            throwProjectile = true;
                            if (self.getProjectileCount() > 0) move.setThrowProjectile(doThrowProjectile);
                        }
                        break;
                }
                switch (up) {
                    case Up:
                        if (between(-diffy, self.getWidth(), tileSize, eps) &&
                                between(player.getX(), fb.bA().x, fb.bB().x, self.getHeight()/2)) {
                            if (self.getOilCanisterCount() > 0) move.setSpillOil(doOldSpile);
                        }

                        if (between(diffy, left_border, right_border, eps) &&
                                between(player.getX(), fb.bA().x, fb.bB().x, self.getWidth()) &&
                                between(playerAngle, angleWidey, 0, 0)) {
                            throwProjectile = true;
                            if (self.getProjectileCount() > 0) move.setThrowProjectile(doThrowProjectile);
                        }
                        break;
                    case Down:
                        if (between(diffy, self.getWidth(), tileSize, eps) &&
                                between(player.getX(), fb.bA().x, fb.bB().x, self.getHeight()/2)) {
                            if (self.getOilCanisterCount() > 0) move.setSpillOil(doOldSpile);
                        }

                        if (between(-diffy, left_border, right_border, eps) &&
                        between(player.getX(), fb.bA().x, fb.bB().x, self.getWidth()) &&
                            between(playerAngle, angleWidey, 0, 0)) {
                            throwProjectile = true;
                            if (self.getProjectileCount() > 0) move.setThrowProjectile(doThrowProjectile);
                        }
                        break;
                }
            }
        }
        return throwProjectile;
    }

    boolean between(double x, double a, double b, double width) {
        return x >= Math.min(a, b) - width && x <= Math.max(a, b) + width;
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
            return front.get(front.size() - 2);
        }

        Point fB() {
            return front.get(front.size() - 1);
        }

        Point bA() {
            return back.get(front.size() - 2);
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

        Side[] sides() {
            return new Side[]{up, lr};
        }
    }

    class PathPoint implements Comparable<PathPoint> {
        int i = -1, x, y;

        public PathPoint(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int compareTo(PathPoint o) {
            return i - o.i;
        }
    }

    class Point {
        double x, y;

        public Point() {
        }

        public Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double dot(Point v) {
            return this.x * v.x + this.y * v.y;
        }

        public Point minus() {
            return new Point(-x, -y);
        }

        public Point sub(Point a) {
            return new Point(x - a.x, y - a.y);
        }

        public void normalize() {
            double magnitude = Math.hypot(x, y);
            x = x / magnitude;
            y = y / magnitude;
        }

        public Point multB(double b) {
            return new Point(x * b, y * b);
        }
    }

    class Road {
        private PriorityQueue<N> pq;
        private N[] edgeTo;
        private int[] distTo;
        private TileType[][] tileTypes;

        public Road(TileType[][] tileTypes, PathPoint start) {
            int sx = start.x, sy = start.y;
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
            if (distTo[wi] > distTo[vi] + edges[vi][wi]) {
                distTo[wi] = distTo[vi] + edges[vi][wi];
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
            Set<N> adj = new HashSet<>(4);
            switch (tileTypes[n.x][n.y]) {
                case VERTICAL:
                    top(n, h, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
                case HORIZONTAL:
                    left(n, adj, Integer.MAX_VALUE);
                    right(n, w, adj, Integer.MAX_VALUE);
                    break;
                case TOP_HEADED_T:
                    left(n, adj, Integer.MAX_VALUE);
                    right(n, w, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
                case BOTTOM_HEADED_T:
                    left(n, adj, Integer.MAX_VALUE);
                    right(n, w, adj, Integer.MAX_VALUE);
                    top(n, h, adj, Integer.MAX_VALUE);
                    break;
                case RIGHT_TOP_CORNER:
                    top(n, h, adj, Integer.MAX_VALUE);
                    left(n, adj, Integer.MAX_VALUE);
                    break;
                case LEFT_TOP_CORNER:
                    top(n, h, adj, Integer.MAX_VALUE);
                    right(n, w, adj, Integer.MAX_VALUE);
                    break;
                case RIGHT_BOTTOM_CORNER:
                    left(n, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
                case LEFT_BOTTOM_CORNER:
                    right(n, w, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
                case CROSSROADS:
                    left(n, adj, Integer.MAX_VALUE);
                    right(n, w, adj, Integer.MAX_VALUE);
                    top(n, h, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
                case LEFT_HEADED_T:
                    left(n, adj, Integer.MAX_VALUE);
                    top(n, h, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
                case RIGHT_HEADED_T:
                    right(n, w, adj, Integer.MAX_VALUE);
                    top(n, h, adj, Integer.MAX_VALUE);
                    bottom(n, adj, Integer.MAX_VALUE);
                    break;
            }
            return adj;
        }

        public Stack<N> pathTo(PathPoint to, World world) {
            int x = to.x, y = to.y;
            int v = y * world.getWidth() + x;
            if (edgeTo[v] == null) {
                int min_md = Integer.MAX_VALUE, md;
                for (int i = x; i >= 0; i--) {
                    for (int j = y; j >= 0; j--) {
                        int t = j * world.getWidth() + i;
                        md = Math.abs(x - i) + Math.abs(y - j);
                        if (null != edgeTo[t] && min_md > md) {
                            min_md = md;
                            v = t;
                        }
                    }
                }
                return pathTo(v);
            } else {
                return pathTo(v);
            }
        }

        public Stack<N> pathTo(int v) {
            Stack<N> path = new Stack<N>();
            for (N e = edgeTo[v]; e != null; e = edgeTo[e.index()]) {
                path.push(e);
            }
            return path;
        }

        public int distTo(PathPoint to, World world) {
            int dist = 0;
            int x = to.x, y = to.y;
            int v = y * world.getWidth() + x;
            for (N e = edgeTo[v]; e != null; e = edgeTo[e.index()]) {
                dist += distTo[e.index()];
            }
            return dist;
        }

        private void top(N n, int h, Set<N> adj, int c) {
            int t = Math.min(h - 1, n.y + 1);
            if (t != n.y && !isH(n.x, t) && !isE(n.x, t))
                adj.add(new N(n.x, t, c, tileTypes[n.x][t]));
        }

        private void bottom(N n, Set<N> adj, int c) {
            int b = Math.max(0, n.y - 1);
            if (b != n.y && !isH(n.x, b) && !isE(n.x, b))
                adj.add(new N(n.x, b, c, tileTypes[n.x][b]));
        }

        private void left(N n, Set<N> adj, int c) {
            int l = Math.max(0, n.x - 1);
            if (l != n.x && !isV(l, n.y) && !isE(l, n.y))
                adj.add(new N(l, n.y, c, tileTypes[l][n.y]));
        }

        private void right(N n, int w, Set<N> adj, int c) {
            int r = Math.min(w - 1, n.x + 1);
            if (r != n.x && !isV(r, n.y) && !isE(r, n.y))
                adj.add(new N(r, n.y, c, tileTypes[r][n.y]));
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

    // Calculate the projection of a polygon on an axis and returns it as a [min, max] interval
    public double[] projectPolygon(Point axis, CarRectangle polygon) {
        // To project a point on an axis use the dot product
        double d = axis.dot(polygon.points[0]);
        double min = d, max = d;
        for (int i = 0; i < polygon.points.length; i++) {
            d = polygon.points[i].dot(axis);
            if (d < min) {
                min = d;
            } else {
                if (d > max) {
                    max = d;
                }
            }
        }
        return new double[]{min, max};
    }

    class CarRectangle {
        private final double x,y;
        private final Point[] points;
        private final Point[] edges = new Point[4];

        public CarRectangle(Car self, FB fb) {
            this(new Point[]{fb.fA(), fb.fB(), fb.bA(), fb.bB()}, self.getX(), self.getY());
        }

        public CarRectangle(Rect rect) {
            this(new Point[]{new Point(rect.lx, rect.ly), new Point(rect.lx, rect.ry),
                    new Point(rect.rx, rect.ly), new Point(rect.rx, rect.ry)}, (rect.rx - rect.lx)/2, (rect.ry - rect.ly)/2);
        }

        public CarRectangle(Point[] points, double x, double y) {
            this.points = points;
            this.x = x;
            this.y = y;
            Point p1 , p2;
            for (int i = 0; i < points.length; i++) {
                p1 = points[i];
                if (i + 1 >= points.length) {
                    p2 = points[0];
                } else {
                    p2 = points[i + 1];
                }
                edges[i] = p2.sub(p1);
            }
        }

        public Point center() {
            return new Point(x, y);
        }
    }

    public double intervalDistance(double[] a, double[] b) {
        if (a[0] < b[0]) {
            return b[0] - a[1];
        } else {
            return a[0] - b[1];
        }
    }

    // Structure that stores the results of the PolygonCollision function
    public class Result {
        public boolean maybe; // Are the polygons going to intersect forward in time?
        public boolean yes; // Are the polygons currently intersecting
        public Point mtv; // The translation to apply to polygon A to push the polygons appart.
    }

    // Check if polygon A is going to collide with polygon B for the given velocity
    public Result collision(CarRectangle polygonA, CarRectangle polygonB, Point velocity) {
        Result result = new Result();
        result.yes = true;
        result.maybe = true;

        int edgeCountA = polygonA.edges.length;
        int edgeCountB = polygonB.edges.length;
        double minIntervalDistance = Double.MAX_VALUE;
        Point translationAxis = new Point();
        Point edge;

        // Loop through all the edges of both polygons
        for (int edgeIndex = 0; edgeIndex < edgeCountA + edgeCountB; edgeIndex++) {
            if (edgeIndex < edgeCountA) {
                edge = polygonA.edges[edgeIndex];
            } else {
                edge = polygonB.edges[edgeIndex - edgeCountA];
            }

            // ===== 1. Find if the polygons are currently intersecting =====

            // Find the axis perpendicular to the current edge
            Point axis = new Point(-edge.y, edge.x);
            axis.normalize();

            // Find the projection of the polygon on the current axis
            double[] a = projectPolygon(axis, polygonA);
            double[] b = projectPolygon(axis, polygonB);

            // Check if the polygon projections are currentlty intersecting
            if (intervalDistance(a, b) > 0) result.yes = false;

            // ===== 2. Now find if the polygons *will* intersect =====

            // Project the velocity on the current axis
            double velocityProjection = axis.dot(velocity);

            // Get the projection of polygon A during the movement
            if (velocityProjection < 0) {
                a[0] += velocityProjection;
            } else {
                a[1] += velocityProjection;
            }

            // Do the same test as above for the new projection
            double intervalDistance = intervalDistance(a, b);
            if (intervalDistance > 0) result.maybe = false;

            // If the polygons are not intersecting and won't intersect, exit the loop
            if (!result.yes && !result.maybe) break;

            // Check if the current interval distance is the minimum one. If so store
            // the interval distance and the current distance.
            // This will be used to calculate the minimum translation Point
            intervalDistance = Math.abs(intervalDistance);
            if (intervalDistance < minIntervalDistance) {
                minIntervalDistance = intervalDistance;
                translationAxis = axis;

                Point d = polygonA.center().sub(polygonB.center());
                if (d.dot(translationAxis) < 0) translationAxis = translationAxis.minus();
            }
        }

        // The minimum translation vector can be used to push the polygons appart.
        // First moves the polygons by their velocity
        // then move polygonA by mtv.
        if (result.maybe) result.mtv = translationAxis.multB(minIntervalDistance);

        return result;
    }
}

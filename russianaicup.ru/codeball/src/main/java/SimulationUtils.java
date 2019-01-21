import model.*;
import model.Robot;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class SimulationUtils {

    public static Dan danToPlane(Vec3D point, Vec3D point_on_plane, Vec3D plane_normal) {
        return new Dan(point.minus(point_on_plane).dot(plane_normal), plane_normal);
    }

    public static Dan danToSphereInner(Vec3D point, Vec3D sphere_center, double sphereRadius) {
        return new Dan(sphereRadius - point.minus(sphere_center).length(),
                sphere_center.minus(point).unit());
    }

    public static Dan danToSphereOuter(Vec3D point, Vec3D sphere_center, double sphereRadius) {
        return new Dan(point.minus(sphere_center).length() - sphereRadius,
                point.minus(sphere_center).unit());
    }

    public static Dan danToArena(Vec3D point, Arena arena, Map<Double, Object> rendering) {
        int downUp = (int)Math.signum(point.getX());
        int leftRight = (int)Math.signum(point.getZ());
        downUp = downUp == 0 ? 1 : downUp;
        leftRight = leftRight == 0 ? 1 : leftRight;
        Dan dan = groundAndCeiling(point, arena);
        dan = xz(point, arena, dan, downUp, leftRight);
        dan = sizeZ(point, arena, dan, leftRight*arena.depth, leftRight);
        dan = xCeiling(point, arena, dan, downUp);
        dan = goalBackCorners(point, arena, dan, downUp, leftRight);
        dan = bottomCorners(point, arena, dan, downUp, leftRight);
        return ceilingCorners(point, arena, dan, downUp, leftRight);
    }

    public static boolean goal(Vec3D ball, Arena arena, double radius, int leftRight) {
        return Math.abs(ball.getZ()) > arena.depth/2 + radius && Math.signum(ball.getZ()) == leftRight &&
               Math.abs(ball.getX()) <= arena.goal_width/2 + radius;
    }

    public static Dan min(Dan dan1, Dan dan2) {
        return dan1.distance > dan2.distance ? dan2 : dan1;
    }

    static double clamp(double v, double min, double max) {
        return v < min ? min : (v > max ? max : v);
    }

    static void clamp(Vec3D v, double min, double max) {
        v.setX(v.getX() < min ? min : (v.getX() > max ? max : v.getX()));
        v.setY(v.getY() < min ? min : (v.getY() > max ? max : v.getY()));
        v.setZ(v.getZ() < min ? min : (v.getZ() > max ? max : v.getZ()));
    }

    public static void move(Entity e, double deltaTime, Rules rules) {
        clamp(e.velocity, -rules.MAX_ENTITY_SPEED, rules.MAX_ENTITY_SPEED);
        e.updatePosition(deltaTime);
        e.position.setY(clamp(e.position.getY() - rules.GRAVITY * deltaTime * deltaTime / 2, e.radius, rules.arena.height - e.radius));
        e.velocity.setY(clamp(e.velocity.getY() - rules.GRAVITY * deltaTime, -rules.ROBOT_MAX_JUMP_SPEED,
                rules.ROBOT_MAX_JUMP_SPEED));
    }

    public static double robotRadious(Rules rules, double jumpSpeed) {
        return rules.ROBOT_MIN_RADIUS + (rules.ROBOT_MAX_RADIUS - rules.ROBOT_MIN_RADIUS)*jumpSpeed/rules.ROBOT_MAX_JUMP_SPEED;
    }

    public static int simulate(Ball ball, Rules rules, Game game, Map<Double, Object> renderingCollection,
                               Random random, List<PointWithTime> ballPoints, int simulationTick) {
        if (simulationTick == game.current_tick) return simulationTick;
        ballPoints.clear();
        Entity ballEntity = new Entity(ball, rules);
        Entity[] robotEntities = new Entity[3];
        for (int i = 0; i < game.robots.length; i++) {
            Robot enemy = game.robots[i];
            if (!enemy.is_teammate)
                robotEntities[enemy.id % 2] = new Entity(enemy, rules);
            else if (enemy.id % 2 == 1)
                robotEntities[2] = new Entity(enemy, rules);
        }
        for (int i = 0; i < 60; i++) {
            double t = 1.0/rules.TICKS_PER_SECOND * i;
            move(ballEntity, t, rules);
            for (int j = 0; j < robotEntities.length; j++) {
                move(robotEntities[j], t, rules);
                renderingCollection.put(t + random.nextDouble(), new DrawUtils.Sphere(robotEntities[j].position,
                        robotEntities[j].radius, Color.ORANGE).h());
            }
            for (int j = 0; j < robotEntities.length; j++) {
                Entity robotEntity = robotEntities[j];
                SimulationUtils.collideEntities(ballEntity, robotEntity, random, rules);
                renderingCollection.put(t + random.nextDouble(), new DrawUtils.Sphere(robotEntities[j].position,
                        robotEntities[j].radius, Color.ORANGE).h());
            }
            Vec3D normal = SimulationUtils.collideWithArena(ballEntity, rules.arena, renderingCollection);
            ballPoints.add(new PointWithTime(ballEntity.position, t, ballEntity.velocity));
            renderingCollection.put(t, new DrawUtils.Sphere(ballEntity.position, ball.radius, Color.GREEN).h());
        }
        return game.current_tick;
    }

    public static boolean simulateGoalHit(Vec3D velocity, Vec3D position, Rules rules, int leftRight) {
        Entity ballEntity = new Entity(rules.BALL_RADIUS, velocity.getY(), velocity, position, rules.BALL_ARENA_E, rules.BALL_MASS);
        for (int i = 0; i < 30; i++) {
            double t = 1.0 / rules.TICKS_PER_SECOND * i;
            move(ballEntity, t, rules);
            if (SimulationUtils.goal(ballEntity.position, rules.arena, rules.BALL_RADIUS, leftRight))
                return true;
        }
        return false;
    }

    public static Entity simulateDefenceHit(Robot me, Entity ballEntity, Rules rules, Random random) {
        Entity robotEntity = new Entity(me, rules);
        for (int i = 0; i < 30; i++) {
            double t = 1.0 / rules.TICKS_PER_SECOND * i;
            move(robotEntity, t, rules);
            if (SimulationUtils.collideEntities(ballEntity, robotEntity, random, rules))
                return robotEntity;
        }
        return null;
    }


    public static Vec3D collideWithArena(Entity entity, Arena arena, Map<Double, Object> rendering) {
        Vec3D point = entity.position;
        if (Math.abs(point.getX()) >= arena.width/2 ||
            Math.abs(point.getZ()) >= arena.depth/2 + arena.goal_depth  ||
            Math.abs(point.getY()) >= arena.height)
            return null;

        Dan dan = danToArena(point, arena, rendering);
        if (dan == null) return Vec3D.zero;
        double penetration = entity.radius - dan.distance;
        if (penetration >= 0) {
            entity.position = entity.position.plus(dan.normal.multiply(penetration));
            double velocity = entity.velocity.dot(dan.normal) - entity.radius_change_speed;
            if (velocity <= 0) {
                entity.velocity = entity.velocity.minus(dan.normal.multiply((1 + entity.arena_e)* velocity));
                return dan.normal;
            }
        }
        return null;
    }

    public static boolean collideEntities(Entity a, Entity b, Random random, Rules rules) {
        Vec3D delta_position = b.position.minus(a.position);
        double distance = delta_position.length();
        double penetration = a.radius + b.radius - distance;
        if (penetration > 0) {
            double k_a = (1 / a.mass) / ((1 / a.mass) + (1 / b.mass));
            double k_b = (1 / b.mass) / ((1 / a.mass) + (1 / b.mass));
            Vec3D normal = delta_position.unit();
            a.position = a.position.minus(normal.multiply(penetration * k_a));
            b.position = b.position.plus(normal.multiply(penetration * k_b));
            double delta_velocity = b.velocity.minus(a.velocity).dot(normal)
                    - b.radius_change_speed - a.radius_change_speed;
            if (delta_velocity < 0){
                Vec3D impulse = normal.multiply((1 + random(rules.MIN_HIT_E, rules.MAX_HIT_E, random)) * delta_velocity);
                a.velocity = a.velocity.plus(impulse.multiply(k_a));
                b.velocity = b.velocity.minus(impulse.multiply(k_b));
            }
            return true;
        }
        return false;
    }

    private static double random(double rangeMin, double rangeMax, Random random) {
        return rangeMin + (rangeMax - rangeMin) * random.nextDouble();
    }

    private static Dan groundAndCeiling(Vec3D point, Arena arena) {
        // Ground
        Dan dan = danToPlane(point, new Vec3D(0, 0, 0), new Vec3D(0, 1, 0));
        // Ceiling
        dan = min(dan, danToPlane(point, new Vec3D(0, arena.height, 0), new Vec3D(0, -1, 0)));
        return dan;
    }

    private static Dan sizeZ(Vec3D point, Arena arena, Dan dan, double depth, int leftRight) {
        // Side z
        Vec3D z = new Vec3D(arena.goal_width / 2 - arena.goal_top_radius,
                arena.goal_height - arena.goal_top_radius, 0);
        Vec3D v = new Vec3D(point.getX(), point.getY(), 0).minus(z);
        if (Math.abs(point.getX()) >= arena.goal_width / 2 + arena.goal_side_radius ||
            point.getY() >= arena.goal_height + arena.goal_side_radius ||
            (v.getX() > 0 && v.getY() > 0 && v.length() >= arena.goal_top_radius + arena.goal_side_radius)) {
            dan = min(dan, danToPlane(point, new Vec3D(0, 0, depth / 2), new Vec3D(0, 0, -leftRight)));
        }
        return dan;
    }

    public static Dan bottomCorners(Vec3D point, Arena arena, Dan dan, int downUp, int leftRight) {
        // Bottom corners
        if (point.getY() < arena.bottom_radius) {
            // Side x
            if (Math.abs(point.getX()) > arena.width / 2 - arena.bottom_radius)
                dan = min(dan, danToSphereInner(point,
                        new Vec3D(downUp*(arena.width / 2 - arena.bottom_radius),
                                arena.bottom_radius, point.getZ()), arena.bottom_radius));
            // Side z
            if (Math.abs(point.getZ()) > arena.depth / 2 - arena.bottom_radius &&
                Math.abs(point.getX()) >= arena.goal_width/2 + arena.goal_side_radius) {
                dan = min(dan, danToSphereInner(point,
                        new Vec3D(point.getX(), arena.bottom_radius,
                                leftRight*(arena.depth / 2 - arena.bottom_radius)),
                        arena.bottom_radius));
            }

            // Side z (goal)
            if (Math.abs(point.getZ()) > arena.depth / 2 + arena.goal_depth - arena.bottom_radius)
                dan = min(dan, danToSphereInner(point,
                        new Vec3D(point.getX(), arena.bottom_radius,
                                leftRight*(arena.depth / 2 + arena.goal_depth - arena.bottom_radius)),
                        arena.bottom_radius));

            // Goal outer corner
            Vec3D o = new Vec3D(downUp*(arena.goal_width / 2 + arena.goal_side_radius), 0,
                    leftRight*(arena.depth / 2 + arena.goal_side_radius));
            Vec3D v = new Vec3D(point.getX(), 0, point.getZ()).minus(o);
            if (v.getX() < 0 && v.getY() < 0 && v.length() < arena.goal_side_radius + arena.bottom_radius) {
                o = o.plus(v.unit()).multiply(arena.goal_side_radius + arena.bottom_radius);
                dan = min(dan, danToSphereInner(
                        point,
                        new Vec3D(o.getX(), arena.bottom_radius, o.getZ()),
                        arena.bottom_radius));
            }

            // Side x (goal)
            if (Math.abs(point.getZ()) >= arena.depth / 2 + arena.goal_side_radius
                && Math.abs(point.getX()) > arena.goal_width / 2 - arena.bottom_radius) {
                dan = min(dan, danToSphereInner(
                        point,
                        new Vec3D(downUp*(arena.goal_width / 2 - arena.bottom_radius),
                                arena.bottom_radius, point.getZ()), arena.bottom_radius));
            }

            // Corner
            if (Math.abs(point.getX()) > arena.width / 2 - arena.corner_radius &&
                Math.abs(point.getZ()) > arena.depth / 2 - arena.corner_radius)
            {
                Vec3D corner_o = new Vec3D(downUp*(arena.width / 2 - arena.corner_radius),
                        0, leftRight*(arena.depth / 2 - arena.corner_radius));
                Vec3D n = new Vec3D(point.getX(), 0, point.getZ()).minus(corner_o);
                double dist = n.length();
                if (dist > arena.corner_radius - arena.bottom_radius) {
                    Vec3D o2 = corner_o.plus(n.unit().multiply(arena.corner_radius - arena.bottom_radius));
                    dan = min(dan, danToSphereInner(
                            point,
                            new Vec3D(o2.getX(), arena.bottom_radius, o2.getZ()),
                            arena.bottom_radius));
                }
            }
        }
        return dan;
    }

    private static Dan xz(Vec3D point, Arena arena, Dan dan, int downUp, int leftRight) {
        // Side x
        dan = min(dan, danToPlane(point, new Vec3D(downUp * arena.width / 2, 0, 0), new Vec3D(-downUp, 0, 0)));
        // Side z (goal)
        dan = min(dan, danToPlane(point, new Vec3D(0, 0, leftRight * (arena.depth / 2 + arena.goal_depth)),
                new Vec3D(0, 0, -leftRight)));
        return dan;
    }

    private static Dan xCeiling(Vec3D point, Arena arena, Dan dan, int downUp) {
        // Side x & ceiling (goal)
        if (Math.abs(point.getZ()) >= arena.depth / 2 + arena.goal_side_radius) {
            // x
            dan = min(dan, danToPlane(point, new Vec3D(arena.goal_width / 2, 0, 0), new Vec3D(-downUp, 0, 0)));
            // y
            dan = min(dan, danToPlane(point, new Vec3D(0, arena.goal_height, 0), new Vec3D(0, -1, 0)));
        }
        return dan;
    }

    public static Dan ceilingCorners(Vec3D point, Arena arena, Dan dan, int downUp, int leftRight) {
        // Ceiling corners
        if (point.getY() > arena.height - arena.top_radius) {
            // Side x
            if (Math.abs(point.getX()) > arena.width / 2 - arena.top_radius)
                dan = min(dan, danToSphereInner(
                        point,
                        new Vec3D(
                                downUp*(arena.width / 2 - arena.top_radius),
                                arena.height - arena.top_radius,
                                point.getZ()),
                        arena.top_radius));

            // Side z
            if (Math.abs(point.getZ()) > arena.depth / 2 - arena.top_radius)
                dan = min(dan, danToSphereInner(
                        point,
                        new Vec3D(point.getX(), arena.height - arena.top_radius,
                                leftRight*(arena.depth / 2 - arena.top_radius)),
                        arena.top_radius));

            // Corner
            if (Math.abs(point.getX()) > arena.width / 2 - arena.corner_radius &&
                Math.abs(point.getZ()) > arena.depth / 2 - arena.corner_radius) {
                Vec3D corner_o = new Vec3D(arena.width / 2 - arena.corner_radius,
                        0, arena.depth / 2 - arena.corner_radius);
                Vec3D dv = new Vec3D(Math.abs(point.getX()), 0, Math.abs(point.getZ())).minus(corner_o);
                dv = new Vec3D(downUp*dv.getX(), 0, leftRight*dv.getZ());
                if (dv.length() > arena.corner_radius - arena.top_radius) {
                    Vec3D n = dv.unit();
                    Vec3D o2 = corner_o.plus(n.multiply(arena.corner_radius - arena.top_radius));
                    dan = min(dan, danToSphereInner(
                            point,
                            new Vec3D(o2.getX(), o2.getZ(), arena.height - arena.top_radius),
                            arena.top_radius));
                }
            }
        }
        return dan;
    }

    private static Dan goalBackCorners(Vec3D point, Arena arena, Dan dan, int downUp, int leftRight) {
        // Goal back corners
        assert arena.bottom_radius == arena.goal_top_radius;
        if (Math.abs(point.getZ()) > arena.depth / 2 + arena.goal_depth - arena.bottom_radius)
            dan = min(dan, danToSphereInner(point,
                    new Vec3D(
                        clamp(point.getX(), arena.bottom_radius - arena.goal_width/2,
                                arena.goal_width/2 - arena.bottom_radius),
                        clamp(point.getY(), arena.bottom_radius, arena.goal_height - arena.goal_top_radius),
                            leftRight*(arena.depth / 2 + arena.goal_depth - arena.bottom_radius)),
                    arena.bottom_radius));

        // Corner
        if (Math.abs(point.getX()) > arena.width/2 - arena.corner_radius &&
            Math.abs(point.getZ()) > arena.depth/2 - arena.corner_radius)
            dan = min(dan, danToSphereInner(point,
                    new Vec3D(downUp*(arena.width/2 - arena.corner_radius), point.getX(),
                            leftRight*(arena.depth/2 - arena.corner_radius)),
                    arena.corner_radius));

        // Goal outer corner
        if (Math.abs(point.getZ()) < arena.depth/2 + arena.goal_side_radius) {
            // Side x
            if (Math.abs(point.getX()) < arena.goal_width/2 + arena.goal_side_radius) {
                dan = min(dan, danToSphereOuter(point,
                        new Vec3D(downUp*(arena.goal_width / 2 + arena.goal_side_radius), point.getY(),
                                leftRight*(arena.depth / 2 + arena.goal_side_radius)),
                        arena.goal_side_radius));
            }

            // Ceiling
            if (point.getY() < arena.goal_height + arena.goal_side_radius)
                dan = min(dan,
                        danToSphereOuter(point,
                                new Vec3D(point.getX(),
                                        arena.goal_height + arena.goal_side_radius,
                                        leftRight*(arena.depth / 2 + arena.goal_side_radius)),
                                arena.goal_side_radius));

            // Top corner
            Vec3D o = new Vec3D((arena.goal_width / 2) - arena.goal_top_radius, arena.goal_height - arena.goal_top_radius, 0);
            Vec3D v = new Vec3D(Math.abs(point.getX()), point.getY(), 0).minus(o);
            if (v.getX() > 0 && v.getY() > 0) {
                o = o.plus(v.unit()).multiply(arena.goal_top_radius + arena.goal_side_radius);
                dan = min(dan, danToSphereOuter(
                        point, new Vec3D(downUp*o.getX(), o.getY(),
                                leftRight*(arena.depth / 2 + arena.goal_side_radius)),
                        arena.goal_side_radius));
            }
        }

        // Goal inside top corners
        if (Math.abs(point.getZ()) > arena.depth/2 + arena.goal_side_radius &&
            point.getY() > arena.goal_height - arena.goal_top_radius) {
            // Side x
            if (Math.abs(point.getX()) > arena.goal_width/2 - arena.goal_top_radius) {
                dan = min(dan, danToSphereInner(point,
                        new Vec3D(downUp*(arena.goal_width / 2 - arena.goal_top_radius),
                                arena.goal_height - arena.goal_top_radius,
                                point.getZ()), arena.goal_top_radius));

                // Side z
                if (point.getZ() > (arena.depth / 2) + arena.goal_depth - arena.goal_top_radius) {
                    dan = min(dan, danToSphereInner(point,
                                new Vec3D(point.getX(), arena.goal_height - arena.goal_top_radius,
                                        leftRight*(arena.depth / 2 + arena.goal_depth - arena.goal_top_radius)),
                                arena.goal_top_radius));
                }
            }
        }
        return dan;
    }
}
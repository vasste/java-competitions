import model.Ball;
import model.Robot;
import model.Rules;

public class Entity {
    double radius;
    double radius_change_speed;
    Vec3D velocity;
    Vec3D position;
    double arena_e; // ROBOT_ARENA_E or BALL_ARENA_E
    double mass;

    public Entity(double radius, double radius_change_speed, Vec3D velocity, Vec3D position, double arena_e, double mass) {
        this.radius = radius;
        this.radius_change_speed = radius_change_speed;
        this.velocity = velocity;
        this.position = position;
        this.arena_e = arena_e;
        this.mass = mass;
    }

    public Entity(Robot robot, Rules rules) {
        this(robot.radius, robot.velocity_y, Vec3D.velocity(robot), new Vec3D(robot), rules.ROBOT_ARENA_E, rules.ROBOT_MASS);
    }

    public Entity(Ball ball, Rules rules) {
        this(ball.radius, ball.velocity_y, Vec3D.velocity(ball), new Vec3D(ball), rules.BALL_ARENA_E, rules.BALL_MASS);
    }

    public void updatePosition(double deltaTime) {
        position = position.plus(velocity.multiply(deltaTime));
    }

    @Override
    public String toString() {
        return "E{" +
                "R=" + radius +
                ", Rcs=" + radius_change_speed +
                ", V=" + velocity +
                ", P=" + position +
                ", Ae=" + arena_e +
                ", M=" + mass +
                '}';
    }
}

public class PointWithTime {
    Vec3D v;
    double t;

    public PointWithTime(Vec3D v, double t) {
        this.v = v;
        this.t = t;
    }

    @Override
    public String toString() {
        return "PointWithTime{" + v + "," + t + "}";
    }
}

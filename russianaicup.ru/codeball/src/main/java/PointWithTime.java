public class PointWithTime {
    Vec3D v;
    double t;
    Vec3D vl;

    public PointWithTime(Vec3D v, double t, Vec3D vl) {
        this.v = v;
        this.t = t;
        this.vl = vl;
    }

    @Override
    public String toString() {
        return "PointWithTime{" + v + "," + t + "}";
    }
}

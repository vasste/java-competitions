// DistanceAndNormal
public class Dan {
    double distance;
    Vec3D normal;

    public Dan(double distance, Vec3D normal) {
        this.distance = Math.abs(distance);
        this.normal = normal;
    }

    @Override
    public String toString() {
        return "Dan{" +
                "d=" + distance +
                ", n=" + normal +
                '}';
    }
}

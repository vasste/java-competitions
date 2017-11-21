public class IA implements Comparable<IA> {
    static IA ZERO = new IA(0);
    long value;
    long n;

    public IA() {}

    public IA(long value) {
        this.value = value;
        this.n = 1;
    }
    void inc() {add(1);}
    void add(long i) { value += i; n++;}
    boolean zero() { return value == 0;}
    long v() { return value; }

    @Override
    public int compareTo(IA o) { return Long.compare(value, o.value); }
}

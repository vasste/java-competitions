public class Accumulator implements Comparable<Accumulator> {
    static Accumulator ZERO = new Accumulator(0);
    long value;
    long n;

    public Accumulator() {}

    public Accumulator(long value) {
        this.value = value;
        this.n = 1;
    }
    void inc() {add(1);}
    void dec() {add(-1);}
    void add(long i) { value += i; n++;}
    boolean zero() { return value == 0;}
    long v() { return value; }

    @Override
    public int compareTo(Accumulator o) { return Long.compare(value, o.value); }
}

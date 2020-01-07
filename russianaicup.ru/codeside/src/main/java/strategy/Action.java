package strategy;

public enum Action {
    WALK, JUMP_UP, JUMP_DOWN;

    public boolean jump() {
        return this == JUMP_UP || this == JUMP_DOWN;
    }
}

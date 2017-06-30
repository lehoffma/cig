package entrants.ghosts.gzae;

public class PacmanInfo {
    private int lastIndex = -1;
    private int tickSeen = -1;
    private int livesLeft = -1;

    public int getLastIndex() {
        return lastIndex;
    }

    public PacmanInfo setLastIndex(int lastIndex) {
        this.lastIndex = lastIndex;
        return this;
    }

    public int getTickSeen() {
        return tickSeen;
    }

    public PacmanInfo setTickSeen(int tickSeen) {
        this.tickSeen = tickSeen;
        return this;
    }

    public int getLivesLeft() {
        return livesLeft;
    }

    public PacmanInfo setLivesLeft(int livesLeft) {
        this.livesLeft = livesLeft;
        return this;
    }
}

package entrants.pacman.gzae;

public class GhostInfo {
    private int ghostPos = -1;
    private byte lastSeen = 0;
    private int edibleTime = 0;

    public int getGhostPos() {
        return ghostPos;
    }

    public GhostInfo setGhostPos(int ghostPos) {
        this.ghostPos = ghostPos;
        return this;
    }

    public byte getLastSeen() {
        return lastSeen;
    }

    public GhostInfo setLastSeen(byte lastSeen) {
        this.lastSeen = lastSeen;
        return this;
    }

    public GhostInfo incrementLastSeen(){
        this.lastSeen++;
        return this;
    }

    public int getEdibleTime() {
        return edibleTime;
    }

    public GhostInfo setEdibleTime(int edibleTime) {
        this.edibleTime = edibleTime;
        return this;
    }
}

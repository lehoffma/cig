package entrants.pacman.gzae;

public class GhostInfo {
    private int position = -1;
    private byte lastSeen = 0;
    private int edibleTime = 0;

    public int getPosition() {
        return position;
    }

    public GhostInfo setPosition(int position) {
        this.position = position;
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

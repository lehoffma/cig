package entrants.pacman.gzae;

import pacman.game.Constants;

import java.util.Arrays;
import java.util.List;

public class Util {
    private static final List<Constants.GHOST> ghostList = Arrays.asList(
            Constants.GHOST.BLINKY,
            Constants.GHOST.INKY,
            Constants.GHOST.PINKY,
            Constants.GHOST.SUE
    );

    /**
     *
     * @param index the index of the ghost
     * @return the ghost associated with the given index
     */
    public static Constants.GHOST getGhostByIndex(int index) {
        return ghostList.get(index);
    }

    /**
     *
     * @param ghost the ghost we want to know the index of
     * @return a number between 0 and 3 or -1 if the given ghost is not part of the list of supported ghosts
     */
    public static int getGhostIndex(Constants.GHOST ghost) {
        return ghostList.indexOf(ghost);
    }
}

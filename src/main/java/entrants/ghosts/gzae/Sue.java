package entrants.ghosts.gzae;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;


public class Sue extends IndividualGhostController {

    public Sue() {
        super(Constants.GHOST.SUE);
    }

    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        return null;
    }
}

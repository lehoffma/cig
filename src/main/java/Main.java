
import entrants.ghosts.gzae.Blinky;
import entrants.ghosts.gzae.Inky;
import entrants.ghosts.gzae.Pinky;
import entrants.ghosts.gzae.Sue;
import entrants.pacman.gzae.MyPacMan;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants.GHOST;

import java.util.EnumMap;

public class Main {

    public static void main(String[] args) {

        Executor executor = new Executor(true, true);

        EnumMap<GHOST, IndividualGhostController> controllers = new EnumMap<>(GHOST.class);

        controllers.put(GHOST.INKY, new Inky());
        controllers.put(GHOST.BLINKY, new Blinky());
        controllers.put(GHOST.PINKY, new Pinky());
        controllers.put(GHOST.SUE, new Sue());

        executor.runGameTimed(new MyPacMan(), new MASController(controllers), true);
    }
}

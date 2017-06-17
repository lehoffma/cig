package entrants.ghosts.gzae;


import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;


/**
 * Created by Gzae on 17.06.2017.
 */

public class GenericGhost extends IndividualGhostController {


    private int[] ghostPos = {-1,-1,-1,-1};
    private int[] messageTick = {-1,-1,-1,-1};

    private int TICK_THRESHOLD = 10;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;



    public GenericGhost(Constants.GHOST ghostType)
    {
        super(ghostType);
    }



    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {

        // Housekeeping - throw out old info
        int edibleTime = game.getGhostEdibleTime(ghost);
        int currentTick = game.getCurrentLevelTime();
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
        }

        // Can we see PacMan? If so tell people and update our info
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();
        // Send my Pos
        messenger.addMessage(new BasicMessage(ghost,null,BasicMessage.MessageType.I_AM,currentIndex,game.getCurrentLevelTime()));

        if (pacmanIndex != -1) {
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
            if (messenger != null) {
                messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex, game.getCurrentLevelTime()));
            }
        }

        // Has anybody else seen PacMan if we haven't?
        if (pacmanIndex == -1 && game.getMessenger() != null) {
            for (Message message : messenger.getMessages(ghost)) {
                if (message.getType() == BasicMessage.MessageType.PACMAN_SEEN) {
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) { // Only if it is newer information
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                }else
                    // receive positions
                    if (message.getType() == BasicMessage.MessageType.I_AM)
                    {
                        int sender = getGhost(message.getSender());
                        if (message.getTick()> messageTick[sender] && message.getTick() < currentTick)
                            messageTick[sender] = message.getTick();
                            ghostPos[sender] = message.getData();
                    }
            }
        }

        if (pacmanIndex == -1) {
            pacmanIndex = lastPacmanIndex;
        }

        Boolean requiresAction = game.doesGhostRequireAction(ghost);
        if (requiresAction != null && requiresAction)        //if ghost requires an action
        {
            if (edibleTime>0)
            {
                return runAway(game);
            }
            if (lastPacmanIndex>=0)
            {
                //we know where he is hiding
                return chasePacman(game);
            }
            else
            {
                return findPacman(game);
            }

        }
        return null;
    }


    public static Constants.GHOST getGhost(int i) {
        switch (i) {
            case 0:
                int rescueMeFromIntelliJ;
                return Constants.GHOST.BLINKY;
            case 1:
                return Constants.GHOST.INKY;
            case 2:
                return Constants.GHOST.PINKY;
            case 3:
                return Constants.GHOST.SUE;
            default:
                return null;
        }
    }

    public static int getGhost(Constants.GHOST ghost){
        switch (ghost) {
            case BLINKY:
                return 0;
            case INKY:
                return 1;
            case PINKY:
                return 2;
            case SUE:
                return 3;
            default:
                return -1;
        }
    }

    private Constants.MOVE runAway(Game game)
    {   //TODO: Run Away
        return Constants.MOVE.NEUTRAL;
    }

    private Constants.MOVE chasePacman(Game game)
    {
        return game.getApproximateNextMoveTowardsTarget(ghostPos[getGhost(ghost)],lastPacmanIndex,game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
    }

    private Constants.MOVE findPacman(Game game)
    {
        // TODO: Find him
        return Constants.MOVE.NEUTRAL;
    }
}



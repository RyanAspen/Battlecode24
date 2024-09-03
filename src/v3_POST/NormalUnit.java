package v3_POST;

import battlecode.common.*;

public class NormalUnit {



    /**
     * This is the primary function of NormalUnit. It is run exactly once per turn and controls
     * all aspects of the bot once spawned. Right now, it does the following:
     * <ul>
     * <li>If the robot has the flag, try to bring it home
     * <li>Try to attack enemies if possible
     * <li>If the robot can pick up an enemy flag, do so
     * <li>If the robot knows where an enemy flag is, go towards it
     * <li>Otherwise, randomly walk
     * </ul>
     */
    public static void run() throws GameActionException {
        RobotController rc = SharedVariables.rc;

        if (rc.hasFlag())
        {
            Micro.tacticalRetreat();
            Movement.goHome();
        }
        else
        {
            Micro.attackv2();
            Micro.defend();
            Micro.tacticalRetreat();
            Micro.seekCrumbs();
            Micro.heal();
            Micro.pickUpEnemyFlag();
            Macro.seekEnemyFlags();
            Movement.randomWalk();
        }
        /*
            - If I'm holding a flag, go towards the closest spawnLoc
            - If I can pick up a flag, do so
            - If there's a known enemy flag location, go towards the closest one
            - Otherwise, random walk
         */
    }





}

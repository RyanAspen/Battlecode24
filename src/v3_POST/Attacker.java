package v3_POST;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Attacker {

    /**
     * This is the primary function of Attacker, which is meant to
     * prioritize advancing into enemy territory and stealing enemy flags.
     * It is run exactly once per turn and controls
     * all aspects of the bot once spawned. Ideally, it does the following:
     * <ul>
     * <li>If the robot can pick up an enemy flag, do so
     * <li>Linger near friendly flags and make it difficult to take them
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
            Macro.cluster();
            Macro.seekEnemyFlags();
            Movement.randomWalk();
        }
    }
}

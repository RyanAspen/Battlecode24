package v3_POST;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public class Defender {

    /**
     * This is the primary function of Defender, which is meant to
     * prioritize defending friendly territory and flags.
     * It is run exactly once per turn and controls
     * all aspects of the bot once spawned. Ideally, it does the following:
     * <ul>
     * <li>If the robot can pick up an enemy flag, do so
     * <li>Linger near friendly flags and make it difficult to take them
     * <li>Trap the region to make it harder for enemies to invade
     * </ul>
     */
    public static void run() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (rc.hasFlag())
        {
            Movement.goHome();
        }
        Micro.pickUpEnemyFlag();
        Micro.defend();
        Micro.attackv2();
        Micro.heal();
        Macro.protectFriendlyFlags();
        Macro.trapRegion();
        Movement.randomWalk();
    }
}

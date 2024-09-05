package v5_POST;

import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.RobotController;

public class Role {
    public static boolean isAttacker()
    {
        RobotController rc = SharedVariables.rc;
        return SharedVariables.standardID < SharedVariables.numAttackers;
    }

    public static boolean isDefender()
    {
        RobotController rc = SharedVariables.rc;
        return SharedVariables.standardID >= SharedVariables.numAttackers;
    }

    public static boolean isBuilder()
    {
        RobotController rc = SharedVariables.rc;
        return SharedVariables.standardID % (GameConstants.ROBOT_CAPACITY / SharedVariables.numBuilders) == 0;
    }

    public static FlagStatus getFlagToDefend() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!isDefender()) return null;
        FlagStatus[] flagStatuses = Communication.getAllFlagStatuses();

        int friendlyFlagCount = 0;
        for (FlagStatus flagStatus : flagStatuses)
        {
            if (flagStatus.friendlyTeam && !flagStatus.notFound)
            {
                friendlyFlagCount++;
            }
        }
        int flagIndexToTake = (SharedVariables.standardID - SharedVariables.numAttackers) % friendlyFlagCount;
        int j = 0;
        for (FlagStatus flagStatus : flagStatuses)
        {
            if (flagStatus.friendlyTeam && !flagStatus.notFound)
            {
                if (j == flagIndexToTake)
                {
                    return flagStatus;
                }
                j++;
            }
        }
        return null;
    }

}

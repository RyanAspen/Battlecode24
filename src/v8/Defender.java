package v8;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

import java.util.Random;

/*
    This class defines how a bot with the Defender role behaves. They should
    - Choose a friendly flag and stay near it
    - Prioritize attacking enemy flag carriers
    - Prioritize attacking over other actions
 */
public class Defender {

    private static MapLocation flagToDefend = null;
    static Random rng = null;

    private static boolean isFlagActive(RobotController rc)
    {
        MapLocation[] friendlyFlags = Communication.getFriendlyFlagLocations();
        for (int i = 0; i < friendlyFlags.length; i++)
        {
            if (flagToDefend.isWithinDistanceSquared(friendlyFlags[i],10))
            {
                return true;
            }
        }
        return false;
    }
    public static void runDefender(RobotController rc) throws GameActionException {
        // If we have no flag to defend, choose one
        if (flagToDefend == null || !isFlagActive(rc))
        {
            rng = new Random(rc.getID());
            MapLocation[] friendlyFlags = Communication.getFriendlyFlagLocations();
            if (friendlyFlags.length > 0)
            {
                flagToDefend = friendlyFlags[rng.nextInt(friendlyFlags.length)];
            }
        }

        //If there is an enemy flag carrier, chase it down!
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (int i = 0; i < enemies.length; i++)
        {
            if (enemies[i].hasFlag() && rc.getMovementCooldownTurns() < 10)
            {
                Pathing.moveTowardsFilling(rc, enemies[i].getLocation());
                break;
            }
        }

        //Attack if able, then heal
        Combat.micro(rc);

        //If there is a flag to protect, linger near it
        if (flagToDefend != null)
        {
            Pathing.lingerTowards(rc, flagToDefend, 15);
        }
        // If there is no known flag to protect, explore
        else
        {
            if (rc.getMovementCooldownTurns() < 10)
            {
                Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
            }
        }


    }
}

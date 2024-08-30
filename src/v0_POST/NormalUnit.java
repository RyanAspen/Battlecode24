package v0_POST;

import battlecode.common.*;

import java.util.Random;

public class NormalUnit {

    public static MapLocation broadcastedLocationToPursue = null;

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
        updateBroadcasted();
        if (rc.hasFlag())
        {
            Movement.goHome();
        }
        else
        {
            attack();
            FlagInfo[] nearFlags = rc.senseNearbyFlags(2, rc.getTeam().opponent());
            for (int i = 0; i < nearFlags.length; i++)
            {
                if (rc.canPickupFlag(nearFlags[i].getLocation()))
                {
                    rc.pickupFlag(nearFlags[i].getLocation());
                }
            }
            FlagStatus flagStatuses[] = Communication.getAllFlagStatuses();
            MapLocation closestLoc = null;
            int closestDist = 9999;
            for (int i = 0; i < flagStatuses.length; i++)
            {
                if (!flagStatuses[i].friendlyTeam && !flagStatuses[i].notFound)
                {
                    int dist = rc.getLocation().distanceSquaredTo(flagStatuses[i].location);
                    if (dist < closestDist)
                    {
                        closestDist = dist;
                        closestLoc = flagStatuses[i].location;
                    }
                }
            }
            if (closestLoc != null)
            {
                //Go towards it
                rc.setIndicatorString("Going Towards Enemy Flag " + closestLoc);
                PathFinder.move(closestLoc);
            }
            else if (broadcastedLocationToPursue != null)
            {
                rc.setIndicatorString("Going Towards Broadcasted Enemy Flag " + broadcastedLocationToPursue);
                PathFinder.move(broadcastedLocationToPursue);
            }
            else
            {
                Movement.randomWalk();
            }
        }
        /*
            - If I'm holding a flag, go towards the closest spawnLoc
            - If I can pick up a flag, do so
            - If there's a known enemy flag location, go towards the closest one
            - Otherwise, random walk
         */
    }

    /**
     * This is the general attacking function. If a robot can attack and can see nearby robots,
     * it will attempt to attack them at random.
     */
    public static void attack() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        Random rng = SharedVariables.rng;
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        if (enemies.length == 0) return;
        for (int i = 0; i < 4; i++)
        {
            MapLocation loc = enemies[(rng.nextInt() & Integer.MAX_VALUE) % enemies.length].getLocation();
            if (rc.canAttack(loc))
            {
                rc.attack(loc);
            }
        }
    }

    /**
     * This function updates the broadcasted location to go towards if there's no better option.
     */
    public static void updateBroadcasted() {
        RobotController rc = SharedVariables.rc;
        if (broadcastedLocationToPursue == null || rc.getLocation().isWithinDistanceSquared(broadcastedLocationToPursue, GameConstants.VISION_RADIUS_SQUARED))
        {
            MapLocation[] broadcastFlags = rc.senseBroadcastFlagLocations();
            if (broadcastFlags.length == 0) return;
            int furthestDist = 0;
            MapLocation furthestFlagLoc = broadcastFlags[0];
            for (MapLocation flagLoc : broadcastFlags)
            {
                int dist = flagLoc.distanceSquaredTo(rc.getLocation());
                if (dist > furthestDist)
                {
                    furthestDist = dist;
                    furthestFlagLoc = flagLoc;
                }
            }
            broadcastedLocationToPursue = furthestFlagLoc;
        }


    }

}

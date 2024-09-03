package v3_POST;

import battlecode.common.*;
import battlecode.world.Trap;

import java.util.Random;

public class Macro {

    public static MapLocation broadcastedLocationToPursue = null;
    public static MapLocation locationToTrapNear = null;
    public static int roundToResetTrapLocation = 0;

    /**
     * This function updates the broadcasted location to go towards if there's no better option.
     */
    private static void updateBroadcasted() {
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

    /**
     * Go towards the nearest known enemy flag, or the nearest broadcasted location
     * if no enemy flags are known.
     */
    public static void seekEnemyFlags() throws GameActionException {
        updateBroadcasted();
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
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
    }

    /**
     * Linger near the nearest known enemy flag
     */
    public static void protectFriendlyFlags() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        FlagStatus flagStatuses[] = Communication.getAllFlagStatuses();
        MapLocation closestLoc = null;
        int closestDist = 9999;
        for (int i = 0; i < flagStatuses.length; i++)
        {
            if (flagStatuses[i].friendlyTeam && !flagStatuses[i].notFound)
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
            Movement.lingerNear(closestLoc);
        }
    }

    /**
     * If there is no cluster, signal to start a cluster at current location.
     * If there is a cluster, go towards it.
     */
    public static void cluster() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        RobotInfo[] nearAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (nearAllies.length > 5) return; //TODO: Magic number
        MapLocation clusterLoc = Communication.getClusterPoint(rc.getLocation());
        if (clusterLoc == null)
        {
            Communication.addClusterPoint(rc.getLocation(), Constants.TIME_FOR_CLUSTER);
        }
        else
        {
            PathFinder.move(clusterLoc);
        }
    }

    /**
     * If possible, trap the area near the robot.
     */
    public static void trapRegion() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        if (rc.getRoundNum() >= roundToResetTrapLocation)
        {
            locationToTrapNear = null;
            roundToResetTrapLocation = 0;
        }
        if (locationToTrapNear == null)
        {
            locationToTrapNear = rc.getLocation();
            roundToResetTrapLocation = Constants.TIME_TO_RESET_TRAP_REGION + rc.getRoundNum();
        }
        Movement.lingerNear(locationToTrapNear);
        if (rc.getLocation().isWithinDistanceSquared(locationToTrapNear, 20)) return;
        if (rc.getCrumbs() < TrapType.EXPLOSIVE.buildCost) return;
        Random rng = SharedVariables.rng;
        int startIdx = (rng.nextInt() & Integer.MAX_VALUE) % Constants.directions.length;
        for (int i = 0; i < Constants.directions.length; i++)
        {
            Direction dir = Constants.directions[(startIdx+i) % Constants.directions.length];
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(dir)))
            {
                rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(dir));
                return;
            }
        }
    }
}

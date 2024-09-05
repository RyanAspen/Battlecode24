package v5_POST;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Movement {

    /**
     * This function controls the robot to move towards the closest friendly spawn point.
     */
    public static void goHome() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        int closestDist = 9999;
        MapLocation closestLoc = null;
        for (int i = 0; i < spawnLocs.length; i++)
        {
            int dist = spawnLocs[i].distanceSquaredTo(rc.getLocation());
            if (dist < closestDist)
            {
                closestDist = dist;
                closestLoc = spawnLocs[i];
            }
        }
        PathFinder.move(closestLoc);
    }

    /**
     * This function causes the robot to random walk. In other words, the robot will choose to move
     * in a random direction.
     */
    public static void randomWalk() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        PathFinder.randomMove();
    }

    /**
     * Linger around the given location.
     * @param lingerLoc MapLocation to linger towards
     */
    public static void lingerNear(MapLocation lingerLoc) throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        if (lingerLoc.isWithinDistanceSquared(rc.getLocation(), 9))
        {
            int newX = lingerLoc.x + SharedVariables.rng.nextInt(5) - 3;
            int newY = lingerLoc.y + SharedVariables.rng.nextInt(5) - 3;
            MapLocation randomNearLoc = new MapLocation(newX, newY);
            PathFinder.move(randomNearLoc);
        }
        else
        {
            PathFinder.move(lingerLoc);
        }
    }
}

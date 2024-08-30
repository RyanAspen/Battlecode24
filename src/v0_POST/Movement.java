package v0_POST;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Random;

public class Movement {

    /**
     * This function controls the robot to move towards the closest friendly spawn point.
     */
    public static void goHome() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        rc.setIndicatorString("Going Home");
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
        rc.setIndicatorString("Random Walk");
        PathFinder.randomMove();
    }
}

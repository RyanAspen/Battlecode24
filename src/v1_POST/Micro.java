package v1_POST;

import battlecode.common.*;

import java.util.Random;

public class Micro {

    public static void seekCrumbs() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length == 0) return;
        MapLocation closestLoc = crumbLocs[0];
        for (MapLocation crumbLoc : crumbLocs)
        {
            int dist = rc.getLocation().distanceSquaredTo(crumbLoc);
            if (dist < closestLoc.distanceSquaredTo(rc.getLocation()))
            {
                closestLoc = crumbLoc;
            }
        }
        PathFinder.move(closestLoc);
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
     * This is meant to be an improved version of attack(). When possible, always attack the enemy
     * with the lowest health remaining.
     */
    public static void attackv2() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        if (enemies.length == 0) return;
        RobotInfo bestEnemy = enemies[0];
        for (RobotInfo enemy : enemies)
        {
            if (enemy.getHealth() < bestEnemy.getHealth())
            {
                bestEnemy = enemy;
            }

        }
        if (rc.canAttack(bestEnemy.getLocation()))
        {
            rc.attack(bestEnemy.getLocation());
        }
    }
}

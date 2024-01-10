package v8;

import battlecode.common.*;

import java.util.Random;

public class Pathing {
    // Basic bug nav - Bug 0

    public static Direction currentDirection = null;

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    static Random rng = null;

    public static int getClosestEnemyDist(RobotController rc, MapLocation location) throws GameActionException {
        //MapLocation[] enemyLocations = Communication.getEnemyLocations();
        RobotInfo[] enemies = rc.senseNearbyRobots(20, rc.getTeam().opponent());
        int minDist = 9999;
        int dist;
        for (int i = 0; i < enemies.length; i++)
        {
            dist = enemies[i].getLocation().distanceSquaredTo(location);
            if (dist < minDist)
            {
                minDist = dist;
            }
        }
        return minDist;
    }

    static Direction moveTowards(RobotController rc, MapLocation target) throws GameActionException {
        Direction movedIn = null;
        if (rc.getLocation().equals(target)) {
            return null;
        }
        if (!rc.isMovementReady()) {
            return null;
        }
        Direction d = rc.getLocation().directionTo(target);
        if (rc.canMove(d)) {
            rc.move(d);
            movedIn = d;
            currentDirection = null; // there is no obstacle we're going around
        } else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    movedIn = currentDirection;
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
        return movedIn;
    }

    static Direction moveTowardsFlee(RobotController rc, MapLocation target, int minDist) throws GameActionException {
        Direction movedIn = null;
        if (rc.getLocation().equals(target)) {
            return null;
        }
        if (!rc.isMovementReady()) {
            return null;
        }
        int minDistToEnemy = getClosestEnemyDist(rc, rc.getLocation());
        Direction d = rc.getLocation().directionTo(target);
        int minDistToEnemyNew = getClosestEnemyDist(rc, rc.getLocation().add(d));
        if (rc.canMove(d) && (minDistToEnemyNew > minDist || minDistToEnemyNew > minDistToEnemy))
        {
            rc.move(d);
            movedIn = d;
            currentDirection = null; // there is no obstacle we're going around
        } else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                minDistToEnemyNew = getClosestEnemyDist(rc, rc.getLocation().add(currentDirection));
                if (rc.canMove(currentDirection) && (minDistToEnemyNew > minDist || minDistToEnemyNew > minDistToEnemy)) {
                    rc.move(currentDirection);
                    movedIn = currentDirection;
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
        return movedIn;
    }

    static Direction moveTowardsFilling(RobotController rc, MapLocation target) throws GameActionException {
        Direction movedIn = null;
        if (rc.getLocation().equals(target)) {
            return null;
        }
        if (!rc.isMovementReady()) {
            return null;
        }
        Direction d = rc.getLocation().directionTo(target);
        if (rc.canFill(target))
        {
            rc.fill(target);
        }
        if (rc.canMove(d)) {
            rc.move(d);
            movedIn = d;
            currentDirection = null; // there is no obstacle we're going around
        }
        else {
            // Going around some obstacle: can't move towards d because there's an obstacle there
            // Idea: keep the obstacle on our right hand

            if (currentDirection == null) {
                currentDirection = d;
            }
            // Try to move in a way that keeps the obstacle on our right
            for (int i = 0; i < 8; i++) {
                if (rc.canFill(rc.getLocation().add(currentDirection)))
                {
                    rc.fill(rc.getLocation().add(currentDirection));
                }
                if (rc.canMove(currentDirection)) {
                    rc.move(currentDirection);
                    movedIn = currentDirection;
                    currentDirection = currentDirection.rotateRight();
                    break;
                } else {
                    currentDirection = currentDirection.rotateLeft();
                }
            }
        }
        return movedIn;
    }

    //Randomly walk if close to the target,moveTowards it otherwise
    static Direction lingerTowards(RobotController rc, MapLocation target, int lingerDist) throws GameActionException {
        if (rng == null)
        {
            rng = new Random(rc.getID());
        }
        if (rc.getLocation().isWithinDistanceSquared(target, lingerDist))
        {
            //Random walk
            for (int i = 0; i < directions.length; i++)
            {
                Direction newDir = directions[(i + rng.nextInt(20)) % directions.length];
                if (rc.canMove(newDir))
                {
                    rc.move(newDir);
                    return newDir;
                }
            }
            return Direction.CENTER;
        }
        else
        {
            return moveTowards(rc, target);
        }

    }
}
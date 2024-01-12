package v10;

import battlecode.common.*;

import java.util.Random;

public class Pathing {

    private static MapLocation prevLoc = null;
    private static final int MAX_DIST_TO_RESET = 20;
    private static final int MIN_BYTECODE_LEFT_AFTER_SETUP = 1000;
    private static boolean usingBug = false;
    private static boolean ready = false;
    private static int columnsDone = 0;
    private static int[][] lastLoc = null;
    private static MapLocation bugTarget = null;
    private static int bestSoFar = 0;
    private static Direction startDir = null;
    private static int unitObstacle = 0;
    private static int startDirMissingInARow = 0;
    private static int goalRound = 0;
    private static boolean isClockwise = false;
    private static final int MAX_TURNS_STATIONARY = 3;
    private static Random rng = null;
    private static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    private static void setup(RobotController rc)
    {
        if (!ready)
        {
            int start = Clock.getBytecodesLeft();
            if (lastLoc == null)
            {
                lastLoc = new int[rc.getMapWidth()][];
            }
            while (columnsDone < rc.getMapWidth())
            {
                if (start - Clock.getBytecodesLeft() > MIN_BYTECODE_LEFT_AFTER_SETUP)
                    return;
                lastLoc[columnsDone] = new int[rc.getMapHeight()];
                columnsDone++;
            }
            ready = true;
        }
    }

    public static Direction moveTowards(RobotController rc, MapLocation loc) throws GameActionException {
        if (rng == null)
        {
            rng = new Random(rc.getID());
        }
        if (!rc.isMovementReady())
        {
            return null;
        }
        else if (rc.getLocation().equals(loc))
        {
            return Direction.CENTER;
        }
        else if (prevLoc == null || !loc.isWithinDistanceSquared(prevLoc, MAX_DIST_TO_RESET))
        {
            prevLoc = loc;
            //reset
            resetGreedy(rc, loc);
            resetBug(rc, loc);
            usingBug = false;
        }
        setup(rc);
        if (!usingBug)
        {
            //Use greedy
            return greedy(rc, loc);
        }
        else
        {
            //Use bug
            return bug(rc, loc);
        }
    }

    private static int getSpecialDist(MapLocation loc1, MapLocation loc2)
    {
        return Math.abs(loc1.x-loc2.x) + Math.abs(loc1.y-loc2.y);
    }

    private static void resetGreedy(RobotController rc, MapLocation target)
    {
        goalRound = rc.getRoundNum();
    }

    private static void resetBug(RobotController rc, MapLocation target)
    {
        bugTarget = target;
        bestSoFar = getSpecialDist(rc.getLocation(), target);
        startDir = rc.getLocation().directionTo(target);
        unitObstacle = 0;
        startDirMissingInARow = 0;
        goalRound = rc.getRoundNum();
        isClockwise = rng.nextBoolean();
    }

    private static boolean isCycle(RobotController rc)
    {
        if (!ready)
        {
            return false;
        }
        int lastLocCurrent = lastLoc[rc.getLocation().x][rc.getLocation().y];
        return (ready && lastLocCurrent >= goalRound);
    }

    private static boolean isTooCloseToFlagCarrier(RobotController rc, MapLocation location) throws GameActionException {
        RobotInfo[] closeAllies = rc.senseNearbyRobots(location, 2, rc.getTeam());
        for (int i = 0; i < closeAllies.length; i++)
        {
            if (closeAllies[i].hasFlag())
            {
                return true;
            }
        }
        return false;
    }

    private static void mark(RobotController rc)
    {
        if (ready)
        {
            MapLocation loc = rc.getLocation();
            lastLoc[loc.x][loc.y] = rc.getRoundNum();
        }
    }

    private static boolean tryMove(RobotController rc, Direction dir) throws GameActionException {
        if (!rc.canSenseLocation(rc.adjacentLocation(dir)))
        {
            return false;
        }
        else if ((!dir.equals(Direction.CENTER)) && rc.canMove(dir) && !isTooCloseToFlagCarrier(rc, rc.adjacentLocation(dir)))
        {
            rc.move(dir);
            return true;
        }
        else if ((!dir.equals(Direction.CENTER)) && (rc.canFill(rc.adjacentLocation(dir)) && rc.getCrumbs() > 1000) && !isTooCloseToFlagCarrier(rc, rc.adjacentLocation(dir)))
        {
            rc.fill(rc.adjacentLocation(dir));
            return false;
        }
        return false;
    }

    private static Direction bug(RobotController rc, MapLocation loc) throws GameActionException {
        int dist = getSpecialDist(rc.getLocation(), bugTarget);
        if (rc.getRoundNum() - 5 == goalRound)
        {
            bestSoFar = dist;
        }
        if ((dist < bestSoFar || isCycle(rc)) && rc.getRoundNum() - 5 >= goalRound)
        {
            usingBug = false;
            resetGreedy(rc, loc);
            return greedy(rc, loc);
        }
        mark(rc);
        Direction dir = startDir;
        for (int i = 0; i < directions.length; i++)
        {
            MapLocation next = rc.getLocation().add(dir);
            if (dir == startDir && rc.canSenseRobotAtLocation(next))
            {
                unitObstacle++;
            }
            if (dir == startDir && rc.canSenseLocation(next) && !rc.canSenseRobotAtLocation(next))
            {
                unitObstacle = 0;
            }
            if (unitObstacle == 2)
            {
                bugTarget = loc;
                goalRound = rc.getRoundNum();
                startDir = rc.getLocation().directionTo(bugTarget);
                bestSoFar = getSpecialDist(rc.getLocation(), bugTarget);
                startDirMissingInARow = 0;
                unitObstacle = 0;
                i = 0;
                dir = startDir;
                next = rc.getLocation().add(dir);
                if (!rc.canSenseLocation(next))
                    break;
                if (rc.canSenseRobotAtLocation(next))
                    unitObstacle++;
            }
            if (!rc.onTheMap(next))
            {
                isClockwise = !isClockwise;
                dir = startDir;
            }
            //Try Move
            if (tryMove(rc, dir))
            {
                if (!dir.equals(startDir))
                {
                    if (isClockwise)
                    {
                        dir = dir.rotateRight().rotateRight();
                    }
                    else
                    {
                        dir = dir.rotateLeft().rotateLeft();
                    }
                    startDirMissingInARow = 0;
                }
                else if (++startDirMissingInARow == 3)
                {
                    startDir = rc.getLocation().directionTo(bugTarget);
                    startDirMissingInARow = 0;
                }
                else
                {
                    if (isClockwise)
                    {
                        dir = dir.rotateRight().rotateRight();
                    }
                    else
                    {
                        dir = dir.rotateLeft().rotateLeft();
                    }
                }
                if (!rc.onTheMap(rc.getLocation().add(startDir)))
                {
                    startDir = rc.getLocation().directionTo(bugTarget);
                }
                return dir;
            }
            if (isClockwise)
            {
                dir = dir.rotateRight();
            }
            else
            {
                dir = dir.rotateLeft();
            }
        }
        return null;
    }

    private static Direction greedy(RobotController rc, MapLocation loc) throws GameActionException {
        if (isCycle(rc))
        {
            usingBug = true;
            resetBug(rc, loc);
            return bug(rc, loc);
        }
        mark(rc);
        int closestDistToGoal = 99999;
        Direction bestDirToGoal = Direction.CENTER;
        Direction dir = directions[rng.nextInt(directions.length)];
        for (int i = 0; i < directions.length; i++)
        {
            MapLocation next = rc.getLocation().add(dir);
            if (!rc.onTheMap(next)) continue;
            if (rc.canMove(dir) && !isTooCloseToFlagCarrier(rc, next))
            {
                if (loc.distanceSquaredTo(next) < closestDistToGoal)
                {
                    closestDistToGoal = loc.distanceSquaredTo(next);
                    bestDirToGoal = dir;
                }
            }
            dir = dir.rotateRight();
        }
        if (!bestDirToGoal.equals(Direction.CENTER) && rc.getLocation().distanceSquaredTo(loc) >= rc.getLocation().add(bestDirToGoal).distanceSquaredTo(loc))
        {
            rc.move(bestDirToGoal);
            return bestDirToGoal;
        }
        MapLocation block1 = rc.getLocation().add(bestDirToGoal.rotateLeft());
        MapLocation block2 = rc.getLocation().add(bestDirToGoal);
        MapLocation block3 = rc.getLocation().add(bestDirToGoal.rotateRight());
        if (
                (rc.canSenseLocation(block1) && rc.sensePassability(block1) && rc.canSenseRobotAtLocation(block1)) &&
                (rc.canSenseLocation(block2) && rc.sensePassability(block2) && rc.canSenseRobotAtLocation(block2)) &&
                (rc.canSenseLocation(block3) && rc.sensePassability(block3) && rc.canSenseRobotAtLocation(block3))
        )
        {
            usingBug = true;
            resetBug(rc, loc);
            return bug(rc, loc);
        }
        return null;
    }

    public static Direction lingerTowards(RobotController rc, MapLocation target, int lingerDist) throws GameActionException {
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
                if (rc.canMove(newDir) && !isTooCloseToFlagCarrier(rc, rc.getLocation().add(newDir)))
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
}

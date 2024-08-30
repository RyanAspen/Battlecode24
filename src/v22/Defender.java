package v22;

import battlecode.common.*;


//Every so often, go towards a lost flag's spawn to see if it's back.
public class Defender {
    private static void assignPosition(RobotController rc) throws GameActionException {
        SharedVariables.flagIdToProtect = Communication.getDefenderFlagId(rc, SharedVariables.flagIdToProtect);
        SharedVariables.flagToProtect = Communication.getFriendlyFlagLocationFromId(rc, SharedVariables.flagIdToProtect);
    }

    private static void macro(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;
        assignPosition(rc);
        if (SharedVariables.flagToProtect != null)
        {
            SharedVariables.goalDestination = SharedVariables.flagToProtect;
        }
        else
        {
            SharedVariables.goalDestination = null;
        }
        rc.setIndicatorString("Defender Goal = " + SharedVariables.goalDestination);
        if (Micro.interceptFlagStealing(rc)) return;
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length > 0)
        {
            int minDist = 9999;
            MapLocation closestCrumb = null;
            for (int i = 0; i < crumbLocs.length; i++)
            {
                int dist = crumbLocs[i].distanceSquaredTo(rc.getLocation());
                if (dist < minDist)
                {
                    minDist = dist;
                    closestCrumb = crumbLocs[i];
                }
            }
            if (closestCrumb != null)
            {
                Pathing.moveTowards(rc, closestCrumb);
            }
            return;
        }

        //If allies aren't nearby, go towards them
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (SharedVariables.goalDestination != null /* && allies.length >= 3*/)
        {
            Pathing.moveTowards(rc, SharedVariables.goalDestination);
        }
        else if (SharedVariables.goalDestination != null && allies.length > 0)
        {
            //Move to minimize average distance to allies while still approaching target
            double minAvgDist = 9999;
            Direction dirToTarget = rc.getLocation().directionTo(SharedVariables.goalDestination);
            Direction[] dirs = {dirToTarget, dirToTarget.rotateLeft(), dirToTarget.rotateRight()};
            Direction bestDir = null;
            for (int i = 0; i < dirs.length; i++)
            {
                if (!rc.canMove(dirs[i])) continue;
                if (Pathing.isTooCloseToFlagCarrier(rc, rc.getLocation().add(dirs[i]))) continue;
                double avgDist = Micro.getAverageDistanceToAlly(rc, allies, rc.getLocation().add(dirs[i]));
                if (avgDist < minAvgDist)
                {
                    minAvgDist = avgDist;
                    bestDir = dirs[i];
                }
            }
            if (bestDir != null)
            {
                rc.move(bestDir);
            }
        }
        else if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length == 0)
        {
            Exploration.explore(rc);
        }
        /*
            1) Update our flagToProtect
            2) Choose goalDestination
                a) If our flagToProtect is not null, goalDestination=flagToProtect
            3) If we can block a flag capture, do it
                a) Otherwise, if crumbs are close, go towards them
                    i) Otherwise, if goalDestination is not null, go towards goalDestination
                        *) Otherwise, explore
         */
    }

    public static void run(RobotController rc) throws GameActionException {
        Micro.micro(rc);
        macro(rc);
        if (rc.isActionReady())
        {
            Micro.micro(rc);
        }
        Micro.trap(rc);
        Micro.heal(rc);
    }
}

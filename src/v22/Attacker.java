package v22;

import battlecode.common.*;

/*
    This class defines how a bot with the Attacker role behaves at the macro level.
    - If holding a flag, send a signal to all close attackers to protect it
    - If a friendly flag carrier is close, move towards it
    - Once an enemy flag is known, mark it as the flag to focus for all other attackers
        * After a set number of turns, choose another flag (Maybe?)
    - Attackers should place explosive traps near the flag they are targeting

 */
public class Attacker {

    private static void macro(RobotController rc) throws GameActionException {
        //System.out.println("MACRO");
        MapLocation rendezvous = Communication.getRendezvous(rc);
        if (!rc.isMovementReady()) return;
        if (rc.getRoundNum() < 100) //To force exploration in early rounds
        {
            rc.setIndicatorString("Forced Explore");
            SharedVariables.goalDestination = null;
        }
        //If there is a flag carrier, go towards it
        else if (Communication.getFlagCarrierLoc(rc) != null)
        {

            SharedVariables.goalDestination = Communication.getFlagCarrierLoc(rc);
            rc.setIndicatorString("Flag Carrier " + SharedVariables.goalDestination);
        }
        else if (rendezvous != null)
        {

            SharedVariables.goalDestination = rendezvous;
            rc.setIndicatorString("Rendezvous " + SharedVariables.goalDestination);
        }
        else
        {
            //goalDestination is nearest enemy flag

            SharedVariables.goalDestination = Communication.getClosestEnemyFlagLoc(rc);
            rc.setIndicatorString("Closest Enemy Flag " + SharedVariables.goalDestination);
        }

        //rc.setIndicatorString("Attacker Goal = " + SharedVariables.goalDestination);

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
        if (SharedVariables.goalDestination != null/* && allies.length >= 5*/)
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
        (How do we do clustering in this framework?)

            1) Update Rendezvous info
            2) Choose goalDestination
                a) If roundNum < 100, goalDestination=null
                    i) If there's an active rendezvous point, go there
                        *) Otherwise, go to the nearest enemy flag
            3) If crumbs are close, go towards them
                a) Otherwise, if goalDestination is not null, go towards goalDestination
                    i) Otherwise, explore
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

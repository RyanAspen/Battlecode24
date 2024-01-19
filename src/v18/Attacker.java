package v18;

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

    private static MapLocation flagToAttack = null;

    //Go for the flag closest to our spawn points (least defensible?)
    private static void chooseNewFlagToAttack(RobotController rc) throws GameActionException {
        MapLocation[] enemyFlags = Communication.getEnemyFlagLocations(rc);
        MapLocation[] spawnPoints = rc.getAllySpawnLocations();

        int minDist = 9999;
        MapLocation preferedSpawn = null;
        MapLocation preferedFlag = null;
        for (int i = 0; i < enemyFlags.length; i++)
        {
            for (int j = 0; j < spawnPoints.length; j++)
            {
                int dist = enemyFlags[i].distanceSquaredTo(spawnPoints[j]);
                if (dist < minDist)
                {
                    minDist = dist;
                    preferedFlag = enemyFlags[i];
                    preferedSpawn = spawnPoints[j];
                }
            }
        }
        if (preferedFlag != null)
            Communication.focusFlag(rc, preferedFlag, preferedSpawn);

        flagToAttack = Communication.getAttackerFlag(rc);
    }

    public static void runAttacker(RobotController rc) throws GameActionException {
        MapLocation flagCarrierLoc = Communication.getFlagCarrierLoc(rc);
        if (flagCarrierLoc != null)
        {
            rc.setIndicatorString("Attacker - Moving to protect flag carrier at " + flagCarrierLoc);
        }
        else if (flagToAttack == null)
        {
            rc.setIndicatorString("Attacker - No Targets");
        }
        else
        {
            rc.setIndicatorString("Attacker - Targetting " + flagToAttack);
        }

        boolean dontMove = Combat.interceptFlagStealing(rc); //Try to intercept a possible steal
        Combat.micro(rc, dontMove); //Attack nearby and move strategically
        //If holding a flag, send a signal to all close attackers to protect it

        //Once an enemy flag is known, mark it as the flag to focus for all other attackers
        chooseNewFlagToAttack(rc); //Updates flagToAttack

        //If a friendly flag carrier is close, move towards it
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (rc.isMovementReady() && !dontMove)
        {
            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].hasFlag())
                {
                    Pathing.lingerTowards(rc, allies[i].getLocation(), 9);
                    break;
                }
            }
        }

        //If there's a flag carrier signal and there's no enemies that we need to engage with, move towards it
        RobotInfo[] enemiesToBeAwareOf = rc.senseNearbyRobots(16, rc.getTeam().opponent());
        if (rc.isMovementReady() && flagCarrierLoc != null && enemiesToBeAwareOf.length == 0 && !dontMove)
        {
            Pathing.moveTowards(rc, flagCarrierLoc);
        }


        // Attackers should place explosive traps near the flag they are targeting
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE)) && flagToAttack != null && rc.getLocation().isWithinDistanceSquared(flagToAttack, 20))
        {
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
            {
                Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE));
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }

        //If we aren't in a cluster, try to go towards the closest one if one exists
        MapLocation closestCluster = Communication.getClosestFriendlyCluster(rc);
        boolean canMoveAsCluster = rc.senseNearbyRobots(-1, rc.getTeam()).length >= Constants.MIN_COUNT_FOR_CLUSTER_MOVE - 1;
        if (closestCluster != null && !canMoveAsCluster)
        {
            Pathing.moveTowards(rc, closestCluster);
        }

        if (!dontMove) {
            if (flagToAttack != null) {
                Pathing.moveTowards(rc, flagToAttack);
            } else {
                Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
            }
        }
    }
}

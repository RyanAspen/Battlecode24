package v21;

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

        boolean canMove = !Micro.micro(rc);
        //boolean canMove = Combat.micro(rc, true);

        //Once an enemy flag is known, mark it as the flag to focus for all other attackers
        chooseNewFlagToAttack(rc); //Updates flagToAttack

        //ALWAYS GO TOWARDS ALLIES provided that there aren't already many nearby allies
        Micro.goNearAllies(rc, canMove);

        //If there's a flag carrier signal and there's no enemies that we need to engage with, move towards it
        RobotInfo[] enemiesToBeAwareOf = rc.senseNearbyRobots(16, rc.getTeam().opponent());
        if (rc.isMovementReady() && flagCarrierLoc != null && enemiesToBeAwareOf.length == 0 && canMove)
        {
            Pathing.moveTowards(rc, flagCarrierLoc);
        }

        /*
        // Attackers should place explosive traps near the flag they are targeting
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE)) && flagToAttack != null && rc.getLocation().isWithinDistanceSquared(flagToAttack, 20))
        {
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
            {
                //Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE));
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }

         */

        // Attackers should place stun traps wherever possible in enemy territory
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.STUN)) && !rc.senseMapInfo(rc.getLocation()).getTeamTerritory().isPlayer() && TrapExploiter.canPlaceTrap(rc, rc.getLocation()))
        {
            if (rc.canBuild(TrapType.STUN, rc.getLocation()))
            {
                //Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.STUN));
                rc.build(TrapType.STUN, rc.getLocation());
            }
        }




        //If we aren't in a cluster, try to go towards the closest one if one exists
        MapLocation closestCluster = Communication.getClosestFriendlyCluster(rc);
        boolean canMoveAsCluster = rc.senseNearbyRobots(-1, rc.getTeam()).length >= Constants.MIN_COUNT_FOR_CLUSTER_MOVE - 1;
        if (closestCluster != null && !canMoveAsCluster && canMove)
        {
            Pathing.moveTowards(rc, closestCluster);
        }

        if (canMove) {
            if (flagToAttack != null) {
                Pathing.moveTowards(rc, flagToAttack);
            } else {
                Exploration.explore(rc);
            }
        }
    }
}

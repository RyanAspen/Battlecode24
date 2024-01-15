package v15;

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
    private static int clusterCooldown = 0;
    private static boolean clustering = false;
    private static MapLocation myCluster = null;
    private static boolean goingTowardCluster = false;
    private static int clusterTimer = 0;

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
        if (flagToAttack == null)
        {
            rc.setIndicatorString("Attacker - No Targets - Cluster = " + goingTowardCluster + ", " + clustering);
        }
        else
        {
            rc.setIndicatorString("Attacker - Targetting " + flagToAttack + " - Cluster = " + goingTowardCluster + ", " + clustering);
        }

        Combat.micro(rc); //Attack nearby and move strategically
        //If holding a flag, send a signal to all close attackers to protect it

        //Once an enemy flag is known, mark it as the flag to focus for all other attackers
        chooseNewFlagToAttack(rc); //Updates flagToAttack

        //If a friendly flag carrier is close, move towards it
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (rc.isMovementReady())
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


        // Attackers should place explosive traps near the flag they are targeting
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE)) && flagToAttack != null && rc.getLocation().isWithinDistanceSquared(flagToAttack, 20))
        {
            if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
            {
                Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE));
                rc.build(TrapType.EXPLOSIVE, rc.getLocation());
            }
        }

        //If there is a cluster point, chose one to go toward
        MapLocation[] clusters = Communication.getClusters();

        //If bot is close enough to a cluster, linger towards it
        int minDist = 9999;
        MapLocation bestCluster = null;
        for (int i = 0; i < clusters.length; i++)
        {
            int dist = clusters[i].distanceSquaredTo(rc.getLocation());
            if (dist < minDist)
            {
                minDist = dist;
                bestCluster = clusters[i];
            }
        }
        if (clustering)
        {
            clusterTimer++;
        }
        if (minDist <= Constants.MAX_DIST_TO_CLUSTER)
        {
            goingTowardCluster = true;
            Pathing.lingerTowards(rc, bestCluster, Constants.CLUSTER_LINGER_DIST);
            if (!clustering)
            {
                return;
            }
        }
        else
        {
            if (goingTowardCluster)
            {
                chooseNewFlagToAttack(rc);
            }
            goingTowardCluster = false;
        }

        // If we aren't going towards a cluster and there's not enough allies nearby, attempt a cluster comm
        if (allies.length < Constants.MIN_CLUSTER_SIZE && clusterTimer <= Constants.MAX_CLUSTER_TIME)
        {
            if (clusterCooldown <= 0)
            {
                if (Communication.createClusterPoint(rc, rc.getLocation()))
                {
                    clusterCooldown = Constants.CLUSTER_CALL_COOLDOWN;
                    myCluster = rc.getLocation();
                    clustering = true;
                }
            }

            clusterCooldown--;
        }
        if (clustering)
        {
            if (allies.length >= Constants.MIN_CLUSTER_SIZE || clusterTimer >= Constants.MAX_CLUSTER_TIME)
            {
                clustering = false;
                clusterTimer = 0;
                Communication.removeClusterPoint(rc, myCluster);
                myCluster = null;

            }
        }

        if (flagToAttack != null)
        {
            Pathing.moveTowards(rc, flagToAttack);
        }
        else
        {
            Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
        }
    }
}

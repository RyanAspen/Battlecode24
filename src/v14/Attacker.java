package v14;

import battlecode.common.*;

/*
    This class defines how a bot with the Attacker role behaves. They should
    - If there is a friendly flag carrier, prioritize being near it
    - Otherwise, if an enemy flag carrier is known, prioritize attacking them
    - Otherwise, if an enemy flag location is known, move toward that location
    - Otherwise, explore aggressively.

 */
public class Attacker {

    private static MapLocation flagToAttack = null;
    private static int clusterCooldown = 0;
    private static boolean clustering = false;
    private static MapLocation myCluster = null;
    private static boolean goingTowardCluster = false;
    private static int clusterTimer = 0;
    private static boolean isFlagActive()
    {
        MapLocation[] enemyFlags = Communication.getEnemyFlagLocations();
        for (int i = 0; i < enemyFlags.length; i++)
        {
            if (flagToAttack.isWithinDistanceSquared(enemyFlags[i],10))
            {
                return true;
            }
        }
        return false;
    }

    private static void chooseNewFlagToAttack(RobotController rc)
    {
        MapLocation[] enemyFlags = Communication.getEnemyFlagLocations();
        if (enemyFlags.length > 0)
        {
            if (enemyFlags.length == 1)
            {
                flagToAttack = enemyFlags[0];
            }
            else //Chose the closest flag that isn't the current flagToAttack
            {
                int closestDist = 9999;
                MapLocation newFlag = null;
                for (int i = 0; i < enemyFlags.length; i++)
                {
                    if (!enemyFlags[i].equals(flagToAttack))
                    {
                        int dist = enemyFlags[i].distanceSquaredTo(rc.getLocation());
                        if (dist < closestDist)
                        {
                            closestDist = dist;
                            newFlag = enemyFlags[i];
                        }
                    }
                }
                flagToAttack = newFlag;
            }
            flagToAttack = enemyFlags[SharedVariables.rng.nextInt(enemyFlags.length)];
        }
    }

    public static void runAttacker(RobotController rc) throws GameActionException {
        Combat.micro(rc); //Attack nearby and move strategically

        //Should mainly due this to exert a numbers advantage. Don't do this when there's nothing around
        //If we have crumbs to spend and we're near an enemy flag and we didn't act yet, place a trap
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE)))
        {
            if (!rc.senseMapInfo(rc.getLocation()).getTeamTerritory().isPlayer())
            {
                if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
                {
                    Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE));
                    rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                }
            }
            /*
            MapLocation[] enemyFlags = Communication.getEnemyFlagLocations();
            for (int i = 0; i < enemyFlags.length; i++)
            {
                if (rc.getLocation().isWithinDistanceSquared(enemyFlags[i], 20))
                {
                    if (rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation()))
                    {
                        Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE));
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation());
                        break;
                    }
                }
            }

             */

        }

        /* Hurts effectiveness, don't use this unless we change strategy


        //If we have crumbs to spend and we're near an allied flag (but not too close) and we didn't act yet, place a trap
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.WATER)))
        {
            MapLocation[] friendlyFlags = Communication.getFriendlyFlagLocations();
            for (int i = 0; i < friendlyFlags.length; i++)
            {
                if (rc.getLocation().isWithinDistanceSquared(friendlyFlags[i], 30) && !rc.getLocation().isWithinDistanceSquared(friendlyFlags[i], 10))
                {
                    if (rc.canBuild(TrapType.WATER, rc.getLocation()))
                    {
                        Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.WATER));
                        rc.build(TrapType.WATER, rc.getLocation());
                        break;
                    }
                }
            }

        }

         */

        //Linger toward a friendly flag carrier if possible
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

        //If there's a distress call, go towards it if we're close
        /*
        MapLocation distressCall = Communication.getDistressLoc(rc);
        if (rc.isMovementReady() && distressCall != null && distressCall.isWithinDistanceSquared(rc.getLocation(), Constants.MAX_DIST_FOR_DISTRESS))
        {
            Pathing.lingerTowards(rc, distressCall, 15);
        }

         */

        // Choose a flag to attack
        if (flagToAttack == null || !isFlagActive())
        {
            chooseNewFlagToAttack(rc);
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

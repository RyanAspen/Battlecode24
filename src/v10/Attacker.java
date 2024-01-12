package v10;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

import java.util.Random;

/*
    This class defines how a bot with the Attacker role behaves. They should
    - If there is a friendly flag carrier, prioritize being near it
    - Otherwise, if an enemy flag carrier is known, prioritize attacking them
    - Otherwise, if an enemy flag location is known, move toward that location
    - Otherwise, explore aggressively.

 */
public class Attacker {

    private static MapLocation flagToAttack = null;
    static Random rng = null;

    private static final int MAX_DIST_TO_CLUSTER = 150;
    private static final int CLUSTER_LINGER_DIST = 20;

    private static final int MIN_CLUSTER_SIZE = 6;
    private static final int CLUSTER_CALL_COOLDOWN = 150;
    private static int cluster_cooldown = 0;

    private static boolean clustering = false;

    private static MapLocation myCluster = null;
    private static boolean goingTowardCluster = false;
    private static final int MAX_CLUSTER_TIME = 50;
    private static int clusterTimer = 0;
    private static boolean isFlagActive(RobotController rc)
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
            flagToAttack = enemyFlags[rng.nextInt(enemyFlags.length)];
        }
    }

    public static void runAttacker(RobotController rc) throws GameActionException {
        //Linger toward a friendly flag carrier if possible

        if (rng == null)
        {
            rng = new Random(rc.getID());
        }

        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (rc.getMovementCooldownTurns() < 10)
        {

            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].hasFlag())
                {
                    Pathing.lingerTowards(rc, allies[i].getLocation(), 9);
                    break;
                }
            }

            //Pursue enemy flag carriers
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            for (int i = 0; i < enemies.length; i++)
            {
                if (enemies[i].hasFlag())
                {
                    Pathing.moveTowards(rc, enemies[i].getLocation());
                    break;
                }
            }
        }

        Combat.micro(rc); //Attack nearby and move strategically

        // Choose a flag to attack
        if (flagToAttack == null || !isFlagActive(rc))
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
        if (minDist <= MAX_DIST_TO_CLUSTER)
        {
            goingTowardCluster = true;
            Pathing.lingerTowards(rc, bestCluster, CLUSTER_LINGER_DIST);
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
        if (allies.length < MIN_CLUSTER_SIZE && clusterTimer <= MAX_CLUSTER_TIME)
        {
            if (cluster_cooldown <= 0)
            {
                if (Communication.createClusterPoint(rc, rc.getLocation()))
                {
                    cluster_cooldown = CLUSTER_CALL_COOLDOWN;
                    myCluster = rc.getLocation();
                    clustering = true;
                }
            }

            cluster_cooldown--;
        }
        if (clustering)
        {
            if (allies.length >= MIN_CLUSTER_SIZE || clusterTimer >= MAX_CLUSTER_TIME)
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

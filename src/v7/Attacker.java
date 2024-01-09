package v7;

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

    public static void runAttacker(RobotController rc) throws GameActionException {
        //Linger toward a friendly flag carrier if possible
        if (rc.getMovementCooldownTurns() < 10)
        {
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
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
                    Pathing.moveTowardsFilling(rc, enemies[i].getLocation());
                    break;
                }
            }
        }

        Combat.micro(rc); //Attack nearby and move strategically

        // Choose a flag to attack
        if (flagToAttack == null || !isFlagActive(rc))
        {
            rng = new Random(rc.getID());
            MapLocation[] enemyFlags = Communication.getEnemyFlagLocations();
            if (enemyFlags.length > 0)
            {
                flagToAttack = enemyFlags[rng.nextInt(enemyFlags.length)];
            }
        }

        //If there is a known enemy flag location, go there
        if (flagToAttack != null)
        {
            Pathing.moveTowardsFilling(rc, flagToAttack);
        }
        //Otherwise, explore
        else
        {
            Pathing.moveTowardsFilling(rc, Exploration.getExploreTarget(rc));
        }


    }
}

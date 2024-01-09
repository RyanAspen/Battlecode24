package v7;

import battlecode.common.*;

public class Combat {

    static final Direction[] cardinalDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };

    // Get all enemies that could be oneshot
    public static RobotInfo[] findOneShots(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(9, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(20, rc.getTeam());
        RobotInfo[] oneShots = new RobotInfo[enemies.length];
        int l = 0;
        for (int i = 0; i < enemies.length; i++)
        {
            int enemyHealth = enemies[i].getHealth() - 150;
            for (int j = 0; j < allies.length; j++)
            {
                // For now we assume all allies are level 0, could change later if teh devs add the ability
                if (allies[j].getLocation().isWithinDistanceSquared(enemies[i].getLocation(), 9))
                {
                    enemyHealth -= 150;
                }
            }
            if (enemyHealth <= 0)
            {
                oneShots[l] = enemies[i];
                l++;
            }
        }
        RobotInfo[] oneShotsFinal = new RobotInfo[l];
        System.arraycopy(oneShots, 0, oneShotsFinal, 0, l);
        return oneShotsFinal;
    }


    //Always target the unit with a low amount of health
    public static void micro(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(9, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(9, rc.getTeam());

        //If there is a flag carrier that needs healing, prioritize them
        if (allies.length > 0 && rc.getActionCooldownTurns() < 10)
        {
            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].hasFlag() && allies[i].getHealth() < 800 && rc.canHeal(allies[i].getLocation()))
                {
                    rc.heal(allies[i].getLocation());
                    break;
                }
            }
        }

        // If we can attack, attack!
        if (enemies.length > 0 && rc.getActionCooldownTurns() < 10)
        {
            MapLocation bestEnemy = null;
            RobotInfo[] oneShots = findOneShots(rc);
            if (oneShots.length > 0)
            {

                int minId = 99999;
                MapLocation bestLoc = null;
                for (int i = 0; i < oneShots.length; i++)
                {
                    int id = oneShots[i].getID();
                    if (id < minId)
                    {
                        minId = id;
                        bestLoc = oneShots[i].getLocation();
                    }
                }
                bestEnemy = bestLoc;
            }
            else
            {
                int minHealth = 99999;
                MapLocation bestLoc = null;
                for (int i = 0; i < enemies.length; i++)
                {
                    int health = enemies[i].getHealth();
                    if (health < minHealth)
                    {
                        minHealth = health;
                        bestLoc = enemies[i].getLocation();
                    }
                }
                bestEnemy = bestLoc;
            }
            if (rc.canAttack(bestEnemy))
            {
                rc.attack(bestEnemy);
            }
        }

        // Heal when given the chance and shouldn't attack
        if (rc.getActionCooldownTurns() < 10 && allies.length > 0)
        {
            int minHealth = 10000;
            MapLocation bestLoc = null;
            for (int i = 0; i < allies.length; i++)
            {
                int health = allies[i].getHealth();
                if (health < minHealth)
                {
                    minHealth = health;
                    bestLoc = allies[i].getLocation();
                }
            }
            if (bestLoc != null)
            {
                if (rc.canHeal(bestLoc))
                {
                    rc.heal(bestLoc);
                }
            }
        }

        if (rc.getMovementCooldownTurns() < 10 && enemies.length > 0)
        {
            int minDist = 9999;
            for (int i = 0; i < cardinalDirections.length; i++)
            {
                if (rc.canMove(cardinalDirections[i]))
                {
                    int dist = Pathing.getClosestEnemyDist(rc, rc.getLocation().add(cardinalDirections[i]));
                    if (dist > 8 && dist < 12)
                    {
                        rc.move(cardinalDirections[i]);
                    }
                }
            }

        }



    }

}

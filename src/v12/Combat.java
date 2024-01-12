package v12;

import battlecode.common.*;

public class Combat {

    //Get Damage Per Round of Ally
    private static float getDamage(RobotInfo bot)
    {
        return Math.round(SkillType.ATTACK.skillEffect * ((float) SkillType.ATTACK.getSkillEffect(bot.getAttackLevel()) / 100 + 1));
    }
    //Get Damage Per Round of Self
    private static float getDamage(RobotController rc)
    {
        return Math.round(SkillType.ATTACK.skillEffect * ((float) SkillType.ATTACK.getSkillEffect(rc.getLevel(SkillType.ATTACK)) / 100 + 1));
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

    // Get all enemies that could be oneshot
    public static void calculateOneShots(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        int l = 0;
        for (int i = 0; i < enemies.length; i++)
        {
            float enemyHealth = enemies[i].getHealth() - getDamage(rc);
            for (int j = 0; j < allies.length; j++)
            {
                if (canAttackLocation(rc, allies[j], enemies[i].getLocation()) != null)
                {
                    enemyHealth -= getDamage(allies[j]);
                }
            }
            if (enemyHealth <= 0)
            {
                Communication.markEnemy(rc, enemies[i]);
                l++;
            }
        }
    }

    private static Direction canAttackLocation(RobotController rc, MapLocation loc)
    {
        if (rc.canAttack(loc))
        {
            return Direction.CENTER;
        }
        if (rc.getHealth() < Constants.MIN_HEALTH_FRACTION_BEFORE_RETREAT * GameConstants.DEFAULT_HEALTH)
        {
            return null;
        }
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (rc.canMove(Constants.directions[i]))
            {
                if (rc.getLocation().add(Constants.directions[i]).isWithinDistanceSquared(loc, GameConstants.ATTACK_RADIUS_SQUARED))
                {
                    return Constants.directions[i];
                }
            }
        }
        return null;
    }

    private static Direction canAttackLocation(RobotController rc, RobotInfo ally, MapLocation loc) throws GameActionException {
        if (ally.getLocation().isWithinDistanceSquared(loc, GameConstants.ATTACK_RADIUS_SQUARED))
        {
            return Direction.CENTER;
        }
        if (ally.getHealth() < Constants.MIN_HEALTH_FRACTION_BEFORE_RETREAT * GameConstants.DEFAULT_HEALTH)
        {
            return null;
        }
        for (int i = 0; i < Constants.directions.length; i++)
        {
            MapLocation newLoc = ally.getLocation().add(Constants.directions[i]);
            if (rc.canSenseLocation(newLoc) && !rc.canSenseRobotAtLocation(newLoc) && rc.sensePassability(newLoc))
            {
                if (newLoc.isWithinDistanceSquared(loc, GameConstants.ATTACK_RADIUS_SQUARED))
                {
                    return Constants.directions[i];
                }
            }
        }
        return null;
    }

    //Always target the unit with a low amount of health
    public static void micro(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam());

        //Update one-shots
        calculateOneShots(rc);

        //Free to attack if we can attack in sync
        if (rc.getRoundNum() > GameConstants.SETUP_ROUNDS && (rc.getRoundNum() % Constants.ATTACK_FREQUENCY) == 0 && rc.isActionReady())
        {
            //If there's an enemy flag carrier, prioritize it
            boolean carrierAttacked = false;
            for (int i = 0; i < enemies.length; i++)
            {
                if (enemies[i].hasFlag())
                {
                    rc.attack(enemies[i].getLocation());
                    carrierAttacked = true;
                    break;
                }
            }
            if (!carrierAttacked)
            {
                //See if we can attack a one-shottable enemy
                int[] oneShotIds = Communication.getMarkedIds();
                MapLocation bestLoc = null;
                Direction bestDir = null;
                if (oneShotIds.length > 0)
                {
                    //Get lowest oneShotId that can be moved toward and attacked
                    int lowestId = 99999;
                    for (int i = 0; i < oneShotIds.length; i++)
                    {
                        try {
                            if (rc.canSenseRobot(oneShotIds[i])) //rc.canSenseRobot() is bugged atm, change once its working
                            {
                                MapLocation robotLoc = rc.senseRobot(oneShotIds[i]).getLocation();
                                Direction dir = canAttackLocation(rc, robotLoc);
                                if (dir != null) {
                                    if (oneShotIds[i] < lowestId) {
                                        bestDir = dir;
                                        bestLoc = robotLoc;
                                        lowestId = oneShotIds[i];
                                    }

                                }
                            }
                        }
                        catch (NullPointerException e)
                        {
                            Communication.unmarkEnemy(rc, oneShotIds[i]);
                        }
                    }

                    if (bestDir == Direction.CENTER)
                    {
                        //Don't move, then attack
                        rc.attack(bestLoc);
                    }
                    else if (bestDir != null)
                    {
                        //Move toward bestDir, then attack
                        rc.move(bestDir);
                        rc.attack(bestLoc);
                    }
                }

                //If we haven't attacked yet, attack a different enemy
                if (rc.isActionReady() && enemies.length > 0)
                {
                    //Attack the enemy with the least health
                    int minHealth = 99999;
                    MapLocation bestEnemy = null;
                    for (int i = 0; i < enemies.length; i++)
                    {
                        if (enemies[i].getHealth() < minHealth)
                        {
                            minHealth = enemies[i].getHealth();
                            bestEnemy = enemies[i].getLocation();
                        }
                    }
                    if (bestEnemy != null)
                    {
                        rc.attack(bestEnemy);
                    }
                }
            }
        }

        //If there are more enemies than allies and we have enough crumbs and we're in enemy territory, lay a trap
        RobotInfo[] farEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] farAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        if (rc.isActionReady() && farEnemies.length > farAllies.length && rc.getCrumbs() >= Constants.CRUMBS_TO_RESERVE_HIGH && rc.senseMapInfo(rc.getLocation()).getTeamTerritory().equals(rc.getTeam().opponent()))
        {
            MapLocation closestEnemy = null;
            int closestDist = 9999;
            for (int i = 0; i < farEnemies.length; i++)
            {
                int dist = farEnemies[i].getLocation().distanceSquaredTo(rc.getLocation());
                if (dist < closestDist)
                {
                    closestDist = dist;
                    closestEnemy = farEnemies[i].getLocation();
                }
            }
            if (closestEnemy != null)
            {
                Direction trapDir = rc.getLocation().directionTo(closestEnemy);
                MapLocation trapLoc = rc.getLocation().add(trapDir);
                if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc))
                {
                    rc.build(TrapType.EXPLOSIVE, trapLoc);
                }
            }
        }

        //If no enemies are seen, heal an ally
        if (farEnemies.length == 0 && allies.length > 0 && rc.isActionReady())
        {
            int minHealth = 9999;
            MapLocation bestLoc = null;
            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].getHealth() < minHealth)
                {
                    minHealth = allies[i].getHealth();
                    bestLoc = allies[i].getLocation();
                }
            }
            if (rc.canHeal(bestLoc))
                rc.heal(bestLoc);
        }

        //If there's an enemy flag carrier, move towards it
        if (farEnemies.length > 0 && rc.isMovementReady())
        {
            for (int i = 0; i < farEnemies.length; i++)
            {
                if (farEnemies[i].hasFlag())
                {
                    Pathing.moveTowards(rc, farEnemies[i].getLocation());
                }
            }
        }
        //If we can still move and enemies are near and we don't have a numbers advantage, kite them
        if (farEnemies.length > farAllies.length && rc.isMovementReady())
        {
            for (int i = 0; i < Constants.cardinalDirections.length; i++)
            {
                if (rc.canMove(Constants.cardinalDirections[i]))
                {
                    int dist = getClosestEnemyDist(rc, rc.getLocation().add(Constants.cardinalDirections[i]));
                    if (dist > Constants.MAX_UNSAFE_DIST)
                    {
                        rc.move(Constants.cardinalDirections[i]);
                    }
                }
            }
        }

        //If we can still move and crumbs are nearby, go to get them
        MapLocation[] nearCrumbs = rc.senseNearbyCrumbs(-1);
        if (nearCrumbs.length > 0 && rc.isMovementReady())
        {
            int closestDist = 9999;
            MapLocation crumbLoc = null;
            for (int i = 0; i < nearCrumbs.length; i++)
            {
                int dist = nearCrumbs[i].distanceSquaredTo(rc.getLocation());
                if (dist < closestDist)
                {
                    closestDist = dist;
                    crumbLoc = nearCrumbs[i];
                }
            }
            Pathing.moveTowards(rc, crumbLoc);
        }
    }

}

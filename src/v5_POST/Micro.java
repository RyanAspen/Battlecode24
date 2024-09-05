package v5_POST;

import battlecode.common.*;

import java.util.Random;

public class Micro {

    private static int getMinEnemyDist(MapLocation loc, RobotInfo[] enemies)
    {
        RobotController rc = SharedVariables.rc;
        int minDist = 9999;
        for (RobotInfo enemy : enemies)
        {
            int dist = loc.distanceSquaredTo(enemy.getLocation());
            if (dist < minDist)
            {
                minDist = dist;
            }
        }
        return minDist;
    }

    /**
     * If the bot is holding an enemy flag
     * -	If a friendly spawn point is visible, go towards it
     * -	Avoiding the attack range of visible bots, head home
     * Else if there is a friendly flag nearby that is not where its supposed to be (for now, if its on a friendly spawn point):
     * -	Immediately move towards that flag
     * -	Do normal combat micro
     * Else if there is an enemy flag nearby:
     * -	Immediately move towards that flag
     * -	If the flag can be picked up, do so
     * -	Do normal combat micro
     * Else
     * -	Do normal combat micro
     * -	Seek crumbs
     * -	Go towards an enemy flag
     */
    public static void fullMicro() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        if (rc.hasFlag())
        {
            // If a spawnLoc is visible, go towards it. Then, avoiding the attack range of
            // visible bots, head home
            MapLocation closestSpawnLoc = null;
            for (MapLocation spawnLoc : spawnLocs)
            {
                if (closestSpawnLoc == null || rc.getLocation().distanceSquaredTo(spawnLoc) < rc.getLocation().distanceSquaredTo(closestSpawnLoc))
                {
                    closestSpawnLoc = spawnLoc;
                }
            }
            if (closestSpawnLoc != null) {
                if (rc.getLocation().isWithinDistanceSquared(closestSpawnLoc, GameConstants.VISION_RADIUS_SQUARED)) {
                    PathFinder.move(closestSpawnLoc);
                    return;
                } else {
                    RobotInfo[] nearEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                    if (nearEnemies.length == 0) {
                        PathFinder.move(closestSpawnLoc);
                    } else {
                        /*
                            Choose the direction that gets closest to closestSpawnLoc that
                            - Can be moved to
                            - Is not within attack range of any enemy OR the closest distance to an enemy does not decrease                     */
                        int currentMinDistEnemy = getMinEnemyDist(rc.getLocation(), nearEnemies);
                        int distToSpawn = rc.getLocation().distanceSquaredTo(closestSpawnLoc);
                        Direction dir = null;
                        int minDistToSpawn = 99999;
                        for (Direction newDir : Constants.MOVEABLE_DIRECTIONS) {
                            if (rc.canMove(newDir)) {
                                int newMinDistEnemy = getMinEnemyDist(rc.getLocation().add(newDir), nearEnemies);
                                if (newMinDistEnemy > 9 || newMinDistEnemy >= currentMinDistEnemy) {
                                    int newDistToSpawn = rc.getLocation().add(newDir).distanceSquaredTo(closestSpawnLoc);
                                    if (newDistToSpawn < minDistToSpawn)
                                    {
                                        minDistToSpawn = distToSpawn;
                                        dir = newDir;
                                    }
                                }
                            }
                        }
                        if (dir != null)
                        {
                            rc.move(dir);
                        }
                    }
                }
            }
        }

        FlagInfo[] nearFriendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        if (nearFriendlyFlags.length > 0)
        {
            FlagInfo flagOutOfPlace = null;
            for (FlagInfo flagLoc : nearFriendlyFlags) {
                boolean inSpawn = false;
                for (MapLocation spawnLoc : spawnLocs) {
                    if (flagLoc.getLocation().equals(spawnLoc))
                    {
                        inSpawn = true;
                        break;
                    }
                }
                if (!inSpawn)
                {
                    flagOutOfPlace = flagLoc;
                    break;
                }
            }
            if (flagOutOfPlace != null)
            {
                // Move towards flagOutOfPlace, then do normal micro
                PathFinder.move(flagOutOfPlace.getLocation());
                Combat.combatMicro();
                return;
            }
        }

        FlagInfo[] nearEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        if (nearEnemyFlags.length > 0)
        {
            // Go toward flag, attempt to pick it up, then do combat micro
            FlagInfo closestFlag = nearEnemyFlags[0];
            for (FlagInfo flag : nearEnemyFlags)
            {
                if (rc.getLocation().distanceSquaredTo(flag.getLocation()) < rc.getLocation().distanceSquaredTo(closestFlag.getLocation()))
                {
                    closestFlag = flag;
                }
            }
            PathFinder.move(closestFlag.getLocation());
            if (rc.canPickupFlag(closestFlag.getLocation()))
            {
                rc.pickupFlag(closestFlag.getLocation());
            }
            Combat.combatMicro();
            return;
        }

        Combat.combatMicro();
        Micro.seekCrumbs();
        if (Role.isAttacker())
        {
            Macro.seekEnemyFlags();
        }
        else
        {
            Macro.protectFriendlyFlags();
        }
    }

    /**
     * If possible, try to pick up an enemy flag.
     */
    public static void pickUpEnemyFlag() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        FlagInfo[] nearFlags = rc.senseNearbyFlags(2, rc.getTeam().opponent());
        for (int i = 0; i < nearFlags.length; i++)
        {
            if (rc.canPickupFlag(nearFlags[i].getLocation()))
            {
                rc.pickupFlag(nearFlags[i].getLocation());
            }
        }
    }

    /**
     * If movement is available and crumbs are sensed, attempt to collect them.
     */
    public static void seekCrumbs() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        MapLocation[] crumbLocs = rc.senseNearbyCrumbs(-1);
        if (crumbLocs.length == 0) return;
        MapLocation closestLoc = crumbLocs[0];
        for (MapLocation crumbLoc : crumbLocs)
        {
            int dist = rc.getLocation().distanceSquaredTo(crumbLoc);
            if (dist < closestLoc.distanceSquaredTo(rc.getLocation()))
            {
                closestLoc = crumbLoc;
            }
        }
        PathFinder.move(closestLoc);
    }

    /**
     * If there are more enemies nearby than allies (including self), retreat
     */
    public static void tacticalRetreat() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (allies.length >= enemies.length) return;

        RobotInfo closestEnemy = enemies[0];
        for (RobotInfo enemy : enemies)
        {
            int dist = enemy.getLocation().distanceSquaredTo(rc.getLocation());
            if (closestEnemy.getLocation().distanceSquaredTo(rc.getLocation()) < dist)
            {
                closestEnemy = enemy;
            }
        }
        PathFinder.retreatFrom(closestEnemy.getLocation());
    }

    /**
     * If the robot has no enemies nearby but it does have injured allies nearby, heal them.
     */
    public static void heal() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam());
        if (allies.length == 0) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        if (enemies.length > 0) return;
        RobotInfo toHeal = allies[0];
        for (RobotInfo ally : allies)
        {
            //Override if the flag carrier needs help
            if (ally.hasFlag() && toHeal.getHealth() < GameConstants.DEFAULT_HEALTH-50 && rc.canHeal(ally.getLocation()))
            {
                rc.heal(ally.getLocation());
                return;
            }
            if (ally.getHealth() < toHeal.getHealth())
            {
                toHeal = ally;
            }
        }
        if (toHeal.getHealth() < GameConstants.DEFAULT_HEALTH-50 && rc.canHeal(toHeal.getLocation()))
        {
            rc.heal(toHeal.getLocation());
        }
    }

    /**
     * This is the general attacking function. If a robot can attack and can see nearby robots,
     * it will attempt to attack them at random.
     */
    public static void attack() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        Random rng = SharedVariables.rng;
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        if (enemies.length == 0) return;
        for (int i = 0; i < 4; i++)
        {
            MapLocation loc = enemies[(rng.nextInt() & Integer.MAX_VALUE) % enemies.length].getLocation();
            if (rc.canAttack(loc))
            {
                rc.attack(loc);
            }
        }
    }

    /**
     * This is meant to be an improved version of attack(). When possible, always attack the enemy
     * with the lowest health remaining.
     */
    public static void attackv2() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        if (enemies.length == 0) return;
        RobotInfo bestEnemy = enemies[0];
        for (RobotInfo enemy : enemies)
        {
            //Override if the flag carrier exists
            if (enemy.hasFlag() && rc.canAttack(enemy.getLocation()))
            {
                rc.attack(enemy.getLocation());
                return;
            }
            if (enemy.getHealth() < bestEnemy.getHealth())
            {
                bestEnemy = enemy;
            }


        }
        if (rc.canAttack(bestEnemy.getLocation()))
        {
            rc.attack(bestEnemy.getLocation());
        }
    }

    /**
     * This is a further improved version of attack(). Use a more complex
     * robot ranking system to choose what to attack.
     */
    public static void attackv3() throws GameActionException
    {
        RobotController rc = SharedVariables.rc;
        if (!rc.isActionReady()) return;
        RobotInfo bestTarget = null;
        double bestScore = -999999;
        for (RobotInfo r : rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent()))
        {
            double score = Combat.getAttackPriority(r);
            if (score > bestScore)
            {
                bestScore = score;
                bestTarget = r;
            }
        }
        if (bestTarget != null && rc.canAttack(bestTarget.getLocation()))
        {
            rc.attack(bestTarget.getLocation());
            if (rc.isActionReady())
            {
                attackv3();
            }
        }

    }

    /**
     * If it can, the robot approaches the closest friendly flag
     * and moves to intercept enemies going toward that flag.
     */
    public static void defend() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        if (!rc.isMovementReady()) return;
        FlagInfo[] nearFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        if (nearFlags.length == 0) return;
        FlagInfo closestFlag = nearFlags[0];
        for (FlagInfo flag : nearFlags)
        {
            int dist = flag.getLocation().distanceSquaredTo(rc.getLocation());
            if (closestFlag.getLocation().distanceSquaredTo(rc.getLocation()) > dist)
            {
                closestFlag = flag;
            }
        }
        RobotInfo[] enemiesCloseToFlag = rc.senseNearbyRobots(closestFlag.getLocation(), -1, rc.getTeam().opponent());
        if (enemiesCloseToFlag.length == 0) return;
        if (closestFlag.isPickedUp())
        {
            PathFinder.move(closestFlag.getLocation());
        }
        else
        {
            // Try to move to prevent the nearby enemies from getting close to the flag
            for (RobotInfo enemy : enemiesCloseToFlag)
            {
                boolean correctRange = enemy.getLocation().distanceSquaredTo(closestFlag.getLocation()) > 2 && enemy.getLocation().distanceSquaredTo(closestFlag.getLocation()) <= 8;
                if (correctRange)
                {
                    MapLocation betweenEnemyAndFlag = enemy.getLocation().add(enemy.getLocation().directionTo(closestFlag.getLocation()));
                    if (betweenEnemyAndFlag.isWithinDistanceSquared(rc.getLocation(), 2) && rc.sensePassability(betweenEnemyAndFlag))
                    {
                        if (rc.canMove(rc.getLocation().directionTo(betweenEnemyAndFlag)))
                        {
                            rc.move(rc.getLocation().directionTo(betweenEnemyAndFlag));
                            return;
                        }
                    }
                }
            }
            PathFinder.move(closestFlag.getLocation());
        }
    }
}

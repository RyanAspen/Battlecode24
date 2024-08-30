package v22;

import battlecode.common.*;

public class Micro {

    /*
    Algo:
    - Get a target A that is immediately attackable (don't move to attack)
    - If A exists, attack it
    - Find the closest enemy alive B
    - Cache B's location, then kite B if movement is available
    - If our health is low, retreat
    - If we have movement and action ready and B exists,
        * If we have more health than the target or our team is strong, chase B
        * Otherwise, kite B

     Overall should be
     micro()
     macro()
     if we have an action, micro() again
     */

    public static double getAverageDistanceToAlly(RobotController rc, RobotInfo[] allies, MapLocation loc)
    {
        if (allies.length == 0)
        {
            return 9999;
        }
        double totalDist = 0;
        for (int i = 0; i < allies.length; i++)
        {
            totalDist += Math.sqrt(allies[i].getLocation().distanceSquaredTo(loc));
        }
        return totalDist / allies.length;
    }

    private static MapLocation couldAttemptSteal(RobotController rc, RobotInfo enemy, FlagInfo[] flags) throws GameActionException {
        for (int j = 0; j < flags.length; j++) {
            if (enemy.getLocation().isAdjacentTo(flags[j].getLocation())) {
                //No point in trying to block it
                return null;
            }
            for (int i = 0; i < Constants.directions.length; i++) {
                MapLocation newLoc = enemy.getLocation().add(Constants.directions[i]);
                if (!newLoc.isAdjacentTo(flags[j].getLocation())) continue;
                if (newLoc.equals(rc.getLocation())) return newLoc;
                if (!rc.canSenseLocation(newLoc)) continue;
                MapInfo newLocInfo = rc.senseMapInfo(newLoc);
                if (newLocInfo.isPassable() && rc.senseRobotAtLocation(newLoc) == null) {
                    //The enemy bot could move here
                    return newLoc;
                }
            }
        }
        return null;
    }
    public static Direction flee(RobotController rc) throws GameActionException {
        int enemyCentroidX = rc.getLocation().x;
        int enemyCentroidY = rc.getLocation().y;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (int i = 0; i < enemies.length; i++)
        {
            enemyCentroidX += enemies[i].getLocation().x - rc.getLocation().x;
            enemyCentroidY += enemies[i].getLocation().y - rc.getLocation().y;
        }
        int maxDist = 0;
        Direction bestDir = null;
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (rc.canMove(Constants.directions[i])) {
                int minDist = 9999;
                MapLocation potentialLoc = rc.getLocation().add(Constants.directions[i]);
                for (int j = 0; j < enemies.length; j++) {
                    int dist = potentialLoc.distanceSquaredTo(enemies[j].getLocation());
                    if (dist < minDist) {
                        minDist = dist;
                    }
                }
                if (minDist > maxDist) {
                    maxDist = minDist;
                    bestDir = Constants.directions[i];
                }
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
        }
        return bestDir;
    }
    public static boolean interceptFlagStealing(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return false;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        FlagInfo[] closeFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        Direction possibleMove = null;
        for (int i = 0; i < enemies.length; i++)
        {
            MapLocation possibleStealLoc = couldAttemptSteal(rc, enemies[i], closeFlags);
            if (possibleStealLoc == null) continue;
            if (rc.getLocation().equals(possibleStealLoc))
            {
                //Don't Move!
                return true;
            }
            if (!possibleStealLoc.isAdjacentTo(rc.getLocation())) continue;
            if (rc.canMove(rc.getLocation().directionTo(possibleStealLoc)))
            {
                possibleMove = rc.getLocation().directionTo(possibleStealLoc);
            }
        }
        if (possibleMove != null)
        {
            rc.move(possibleMove);
            return true;
        }
        else {
            return false;
        }
    }

    private static boolean isDiagonal(Direction dir)
    {
        if (dir == null) return false;
        switch (dir)
        {
            case CENTER:
            case EAST:
            case WEST:
            case SOUTH:
            case NORTH:
                return false;
            default:
                return true;
        }
    }

    public static void micro(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) return;
        attackClose(rc);
        RobotInfo closestEnemy = getClosestEnemy(rc);
//        if (closestEnemy != null)
//            kite(rc, closestEnemy);
        if (rc.getHealth() < Constants.MIN_HEALTH_FRACTION_BEFORE_RETREAT * GameConstants.DEFAULT_HEALTH)
        {
            retreat(rc);
        }
        if (rc.isMovementReady() && rc.isActionReady() && closestEnemy != null)
        {
            //Check if we should chase or kite
            RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            if ((rc.getHealth() >= closestEnemy.getHealth() && (allies.length + 1 >= enemies.length)) || allies.length + 1 > enemies.length+2)
            {
                chase(rc, closestEnemy);
            }
            else
            {
                kite(rc, closestEnemy);
            }
        }
    }

    public static void heal(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length > 0) return;
        RobotInfo[] nearAllies = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam());
        int minHealth = GameConstants.DEFAULT_HEALTH - 100;
        RobotInfo healTarget = null;
        for (int i = 0; i < nearAllies.length; i++)
        {
            if (nearAllies[i].getHealth() < minHealth)
            {
                minHealth = nearAllies[i].getHealth();
                healTarget = nearAllies[i];
            }
        }
        if (healTarget != null)
        {
            rc.heal(healTarget.getLocation());
        }
    }

    public static void trap(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        if (rc.getCrumbs() < Economy.getTrapCost(rc, TrapType.STUN)) return;
        RobotInfo closestEnemy = getClosestEnemy(rc);
        if (closestEnemy == null) return;
        Direction closestDir = rc.getLocation().directionTo(closestEnemy.getLocation());
        Direction[] dirs = {closestDir, closestDir.rotateRight(), closestDir.rotateLeft(), closestDir.rotateRight().rotateRight(), closestDir.rotateLeft().rotateLeft(), Direction.CENTER};
        for (int i = 0; i < dirs.length; i++)
        {
            MapLocation newLoc = rc.getLocation().add(dirs[i]);
            if (TrapExploiter.canPlaceTrap(rc, newLoc) && rc.canBuild(TrapType.STUN, newLoc))
            {
                rc.build(TrapType.STUN, newLoc);
                return;
            }
        }
    }

    private static RobotInfo getClosestEnemy(RobotController rc) throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        int minDist = 9999;
        RobotInfo closestEnemy = null;
        for (int i = 0; i < enemies.length; i++)
        {
            if (enemies[i].getLocation().distanceSquaredTo(rc.getLocation()) < minDist)
            {
                minDist = enemies[i].getLocation().distanceSquaredTo(rc.getLocation());
                closestEnemy = enemies[i];
            }
        }
        return closestEnemy;
    }

    private static void attackClose(RobotController rc) throws GameActionException {
        if (!rc.isActionReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.ATTACK_RADIUS_SQUARED, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        int minRoundsRequired = 9999;
        RobotInfo bestTarget = null;
        for (int i = 0; i < enemies.length; i++)
        {
            if (enemies[i].getHealth() <= getDamage(rc))
            {
                rc.attack(enemies[i].getLocation());
                return;
            }
            else
            {
                //Get the enemy that could be taken out the quickest
                int possibleDamage = getDamage(rc);
                int canAttack = 1;
                for (int j = 0; j < allies.length; j++)
                {
                    if (allies[j].getLocation().isWithinDistanceSquared(enemies[i].getLocation(), GameConstants.ATTACK_RADIUS_SQUARED))
                    {
                        possibleDamage += getDamage(rc, allies[j]);
                        canAttack++;
                    }
                }
                double avgDamagePerBot = (double) possibleDamage / canAttack;
                int roundsRequired = (int) (enemies[i].getHealth() * canAttack / avgDamagePerBot);
                if (roundsRequired < minRoundsRequired)
                {
                    minRoundsRequired = roundsRequired;
                    bestTarget = enemies[i];
                }
            }
        }
        if (bestTarget != null)
        {
            rc.attack(bestTarget.getLocation());
        }
    }

    //Move in a direction to minimize the number of enemies that can see us
    private static void kite(RobotController rc, RobotInfo target) throws GameActionException {
        if (!rc.isMovementReady()) return;
        rc.setIndicatorString("Kiting " + target.getLocation());
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Direction backDir = rc.getLocation().directionTo(target.getLocation()).opposite();
        Direction[] dirs = {Direction.CENTER, backDir, backDir.rotateLeft(), backDir.rotateRight(),
                backDir.rotateLeft().rotateLeft(), backDir.rotateRight().rotateRight()};
        Direction bestDir = null;
        int minEnemiesClose = 9999;
        for (int i = 0; i < dirs.length; i++)
        {
            if (!rc.canMove(dirs[i])) continue;
            MapLocation newLoc = rc.getLocation().add(dirs[i]);
            int enemiesClose = 0;
            for (int j = 0; j < enemies.length; j++)
            {
                if (newLoc.isWithinDistanceSquared(enemies[j].getLocation(), 9))
                {
                    enemiesClose++;
                }
            }
            if (enemiesClose < minEnemiesClose)
            {
                bestDir = dirs[i];
                minEnemiesClose = enemiesClose;
            }
            else if (enemiesClose == minEnemiesClose && isDiagonal(dirs[i]) && !isDiagonal(bestDir))
            {
                bestDir = dirs[i];
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
        }
    }

    //Move in a direction to minimize the number of enemies that can see us while getting within attack range of the target
    private static void chase(RobotController rc, RobotInfo target) throws GameActionException {
        rc.setIndicatorString("Chasing " + target.getLocation());
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        Direction forwardDir = rc.getLocation().directionTo(target.getLocation());
        Direction[] dirs = {forwardDir, forwardDir.rotateLeft(), forwardDir.rotateRight(),
                forwardDir.rotateLeft().rotateLeft(), forwardDir.rotateRight().rotateRight()};
        Direction bestDir = null;
        int minEnemiesClose = 9999;
        for (int i = 0; i < dirs.length; i++)
        {
            if (!rc.canMove(dirs[i])) continue;
            MapLocation newLoc = rc.getLocation().add(dirs[i]);
            if (!newLoc.isWithinDistanceSquared(target.getLocation(), GameConstants.ATTACK_RADIUS_SQUARED)) continue;
            int enemiesClose = 0;
            for (int j = 0; j < enemies.length; j++)
            {
                if (enemies[j].getID() == target.getID()) continue;
                if (newLoc.isWithinDistanceSquared(enemies[j].getLocation(), 9))
                {
                    enemiesClose++;
                }
            }
            if (enemiesClose < minEnemiesClose)
            {
                bestDir = dirs[i];
                minEnemiesClose = enemiesClose;
            }
            else if (enemiesClose == minEnemiesClose && isDiagonal(dirs[i]) && !isDiagonal(bestDir))
            {
                bestDir = dirs[i];
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
        }
    }

    private static void retreat(RobotController rc) throws GameActionException
    {
        rc.setIndicatorString("Retreating ");
        if (!rc.isMovementReady()) return;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allBots = rc.senseNearbyRobots(-1);
        int currMinEnemyDist = 9999;
        for (int j = 0; j < enemies.length; j++)
        {
            int dist = enemies[j].getLocation().distanceSquaredTo(rc.getLocation());
            if (dist < currMinEnemyDist)
            {
                currMinEnemyDist = dist;
            }
        }
        if (currMinEnemyDist <= GameConstants.ATTACK_RADIUS_SQUARED)
        {
            //Try to get to a safe spot if we aren't in one, preferring spots near allies
            Direction bestDir = null;
            int minAvgDistToAllies = 9999;
            for (int i = 0; i < Constants.directions.length; i++)
            {
                if (!rc.canMove(Constants.directions[i])) continue;
                int minEnemyDist = 9999;
                int avgAllyDist = 0;
                int allyCount = 0;
                for (int j = 0; j < allBots.length; j++)
                {
                    int dist = allBots[j].getLocation().distanceSquaredTo(rc.getLocation().add(Constants.directions[i]));
                    if (allBots[j].getTeam().isPlayer())
                    {
                        avgAllyDist += dist;
                        allyCount++;
                    }
                    else
                    {
                        if (dist < minEnemyDist)
                        {
                            minEnemyDist = dist;
                        }
                    }
                }
                if (allyCount > 0)
                {
                    avgAllyDist /= allyCount;
                }
                else
                {
                    avgAllyDist = 9999;
                }
                if (minEnemyDist > GameConstants.ATTACK_RADIUS_SQUARED)
                {
                    if (avgAllyDist  < minAvgDistToAllies) {
                        bestDir = Constants.directions[i];
                        minAvgDistToAllies = avgAllyDist;
                    }
                }
            }
            if (bestDir != null)
            {
                rc.move(bestDir);
            }
        }
        if (rc.isMovementReady())
        {
            //Go towards the nearest flag
            MapLocation target = null;
            int closestDist = 9999;
            for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
            {
                int dist = SharedVariables.flagStatuses[i].location.distanceSquaredTo(rc.getLocation());
                if (dist < closestDist)
                {
                    closestDist =  dist;
                    target = SharedVariables.flagStatuses[i].location;
                }
            }
            if (target != null)
            {
                Pathing.moveTowards(rc, target);
            }
        }
    }

    private static int startRound = -1;
    private static int startBytecode = 0;

    //Get Damage Per Round of Self
    private static int getDamage(RobotController rc)
    {
        int baseAttack = SkillType.ATTACK.skillEffect;
        GlobalUpgrade[] upgrades = rc.getGlobalUpgrades(rc.getTeam());
        for (int i = 0; i < upgrades.length; i++)
        {
            if (upgrades[i].equals(GlobalUpgrade.ATTACK))
            {
                baseAttack += GlobalUpgrade.ATTACK.baseAttackChange;
                break;
            }
        }
        return Math.round(baseAttack * ((float) SkillType.ATTACK.getSkillEffect(rc.getLevel(SkillType.ATTACK)) / 100 + 1));
    }

    //Get Damage Per Round of Other Bot
    private static int getDamage(RobotController rc, RobotInfo bot)
    {
        int baseAttack = SkillType.ATTACK.skillEffect;
        GlobalUpgrade[] upgrades = rc.getGlobalUpgrades(bot.getTeam());
        for (int i = 0; i < upgrades.length; i++)
        {
            if (upgrades[i].equals(GlobalUpgrade.ATTACK))
            {
                baseAttack += GlobalUpgrade.ATTACK.baseAttackChange;
                break;
            }
        }
        return Math.round(baseAttack * ((float) SkillType.ATTACK.getSkillEffect(bot.getAttackLevel()) / 100 + 1));
    }
}



package v5_POST;

import battlecode.common.*;

public class Combat {

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
     * This function gives an approximate value of a robot in terms of its
     * experience points. Taken directly from GoneSharkin.
     * @param r RobotInfo describing the robot to get priority
     * @return the approximate value of the robot r
     */
    private static double getRobotScore(RobotInfo r) {
        // output is between 3 and 10
        double score = 0;
        switch (r.getAttackLevel()) { // according to DPS
            case 0: score += 1; break;
            case 1: score += 1.1; break;
            case 2: score += 1.22; break;
            case 3: score += 1.35; break;
            case 4: score += 1.5; break;
            case 5: score += 1.85; break;
            case 6: score += 2.5; break;
        }
        switch (r.getHealLevel()) { // according to DPS
            case 0: score += 1; break;
            case 1: score += 1.08; break;
            case 2: score += 1.16; break;
            case 3: score += 1.26; break;
            case 4: score += 1.3; break;
            case 5: score += 1.35; break;
            case 6: score += 1.66; break;
        }
        switch (r.getBuildLevel()) { // according to cost of building
            case 0: score += 1; break;
            case 1: score += 1 / 0.9; break;
            case 2: score += 1 / 0.85; break;
            case 3: score += 1 / 0.8; break;
            case 4: score += 1 / 0.7; break;
            case 5: score += 1 / 0.6; break;
            case 6: score += 1 / 0.5; break;
        }
        return score;
    }

    /**
     * This function gets the priority score of an enemy robot for
     * the purposes of attacking.
     *
     * @param r RobotInfo describing the robot to get priority
     * @return the priority score of r, higher is more important
     */
    public static double getAttackPriority(RobotInfo r)
    {
        double score = 0;
        RobotController rc = SharedVariables.rc;
        if (r.getHealth() <= rc.getAttackDamage())
        {
            score += 1e9;
        }
        if (r.hasFlag())
        {
            score += 1e8;
        }
        int timeToKill = (r.getHealth() + rc.getAttackDamage() - 1) / rc.getAttackDamage();
        score += getRobotScore(r) / timeToKill;
        return score;
    }

    /**
     * This function handles all movement and attacking when directly in combat.
     * 1) If there are no enemies around, return.
     * 2) Attack the best target, if a target exists. Otherwise, heal the best target, if a target exists.
     * 3) Determine if the bot should move and where
     * 4) If the bot should move, move
     * 5) Attack the best target, if we can still attack.
     */
    public static void combatMicro() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        Micro.attackv3();
        FlagInfo[] nearFlags = rc.senseNearbyFlags(-1);
        FlagInfo closestFlag = null;
        for (FlagInfo flag : nearFlags) {
            if ((flag.isPickedUp() && flag.getTeam().isPlayer()) || (!flag.getTeam().isPlayer()) && !flag.isPickedUp()) {
                if (closestFlag == null || (rc.getLocation().distanceSquaredTo(flag.getLocation()) < rc.getLocation().distanceSquaredTo(closestFlag.getLocation()))) {
                    closestFlag = flag;
                }
            }
        }
        if (Role.isBuilder() && rc.isActionReady() && closestFlag != null)
        {
            if (rc.getCrumbs() >= TrapType.EXPLOSIVE.buildCost)
            {
                MapLocation bestTrapLoc = null;
                for (Direction dir : Constants.directions)
                {
                    MapLocation newLoc = rc.getLocation().add(dir);
                    if (!rc.canBuild(TrapType.EXPLOSIVE, newLoc)) continue;
                    int dist = newLoc.distanceSquaredTo(closestFlag.getLocation());
                    if (bestTrapLoc == null || dist < bestTrapLoc.distanceSquaredTo(closestFlag.getLocation()))
                    {
                        bestTrapLoc = newLoc;
                    }
                }
                if (bestTrapLoc != null)
                {
                    rc.build(TrapType.EXPLOSIVE, bestTrapLoc);
                }
            }
        }
        Micro.heal();
        if (rc.isActionReady() && rc.isMovementReady())
        {
            // Check if we need to move to destroy a bot
            Direction dir = null;
            for (RobotInfo enemy : rc.senseNearbyRobots(-1, rc.getTeam().opponent()))
            {
                if (enemy.getHealth() <= rc.getAttackDamage() && rc.getLocation().isWithinDistanceSquared(enemy.getLocation(), GameConstants.ATTACK_RADIUS_SQUARED))
                {
                    //No need to move
                    dir = null;
                    break;
                }
                else if (enemy.getHealth() <= rc.getAttackDamage())
                {
                    for (Direction newDir : Constants.MOVEABLE_DIRECTIONS)
                    {
                        if (rc.canMove(newDir) && rc.getLocation().add(newDir).isWithinDistanceSquared(enemy.getLocation(), GameConstants.ATTACK_RADIUS_SQUARED))
                        {
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
        if (rc.isMovementReady())
        {
            if (closestFlag != null) {
                PathFinder.move(closestFlag.getLocation());
            }
        }
        if (rc.isMovementReady())
        {
            //If health is low and allies are nearby, get away from enemies.
            if (rc.getHealth() <= Constants.IN_DANGER_HEALTH)
            {
                RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
                RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
                if (allies.length > 0 && enemies.length > 0)
                {
                    int distToEnemy = getMinEnemyDist(rc.getLocation(), enemies);
                    Direction dir = null;
                    for (Direction newDir : Constants.MOVEABLE_DIRECTIONS)
                    {
                        if (!rc.canMove(newDir)) continue;
                        int newDistToEnemy = getMinEnemyDist(rc.getLocation().add(newDir), enemies);
                        if (newDistToEnemy < distToEnemy)
                        {
                            distToEnemy = newDistToEnemy;
                            dir = newDir;
                        }
                    }
                    if (dir != null)
                    {
                        rc.move(dir);
                    }
                }
            }
        }

        Micro.attackv3();
    }
}

package v21;

import battlecode.common.*;

public class Micro {

    /*
    Algo:
IF ENEMIES ARE VISIBLE,
- Calculate if we need to retreat. If we do need to, retreat!
	* Calculate the number of turns it would take to destroy all visible enemies given visible allies, assuming just attacking
	* Calculate the number of turns it would take for all allies to be destroyed given visible enemies, assuming just attacking
	* If we lose this calculation, RETREAT
- For all attackable enemies at the current position,
	* If we need to move to attack the target, calculate if we could jail the target before being jailed. If we can't, skip.
	* If the target is a flag carrier or is close to a flag, set superrank=A. Otherwise, superrank=B.
	* Calculate the number of allies needed to jail the target a.
	* If the attacker and nearby allies could jail the target, set rank = a+1
	* If not, set the rank = 99
- Attack (and move if necessary) the best target
- If we can still move and we aren't in a safe spot, RETREAT
IF ENEMIES AREN'T VISIBLE,
- heal the most damaged ally that can be healed
     */

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

    private static float getDamagePerRound(RobotController rc)
    {
        int damage = getDamage(rc);
        int cooldown = (int) Math.round(GameConstants.ATTACK_COOLDOWN*(1+.01*SkillType.ATTACK.getCooldown(rc.getLevel(SkillType.ATTACK))));
        return (float) damage / ((float) cooldown / 10);
    }

    private static float getDamagePerRound(RobotController rc, RobotInfo bot)
    {
        int damage = getDamage(rc, bot);
        int cooldown = (int) Math.round(GameConstants.ATTACK_COOLDOWN*(1+.01*SkillType.ATTACK.getCooldown(bot.getAttackLevel())));
        return (float) damage / ((float) cooldown / 10);
    }

    public static boolean areOutgunned(RobotController rc)
    {
        float enemyHealth = 0;
        float enemyDamage = 0;
        float allyHealth = rc.getHealth();
        float allyDamage = getDamagePerRound(rc);
        RobotInfo[] allBots = rc.senseNearbyRobots();
        int enemies = 0;
        int allies = 0;
        for (int i = 0; i < allBots.length; i++)
        {
            if (rc.getTeam().equals(allBots[i].getTeam()))
            {
                allies++;
                allyHealth += allBots[i].getHealth();
                allyDamage += getDamagePerRound(rc, allBots[i]);
            }
            else if (TrapExploiter.isLocationAffectedByTrap(rc, allBots[i].getLocation()))
            {
                enemies++;
                enemyHealth += allBots[i].getHealth();
            }
            else
            {
                enemies++;
                enemyHealth += allBots[i].getHealth();
                enemyDamage += getDamagePerRound(rc, allBots[i]);
            }
        }
        if (enemyDamage == 0)
        {
            if (rc.getID() == 12907)
                System.out.println("Don't Retreat! Enemy can't do damage");
            return false;
        }
        if (allyDamage == 0)
        {
            if (rc.getID() == 12907)
                System.out.println("Retreat! We can't do damage");
            return true;
        }
        float turnsUntilEnemiesJailed = enemyHealth / allyDamage;
        float turnsUntilAlliesJailed = allyHealth / enemyDamage;

        if (rc.getID() == 12907) {
            System.out.println("Number of Enemies = " + enemies + " Number of Allies = " + allies);
            System.out.println("Turns Until Enemies Jailed = " + turnsUntilEnemiesJailed + ", Turns Until Allies Jailed = " + turnsUntilAlliesJailed);
        }
        return turnsUntilEnemiesJailed > turnsUntilAlliesJailed;
    }

    public static boolean couldBeJailed(RobotController rc) throws GameActionException {
        float enemyDamage = 0;
        float allyHealth = rc.getHealth();
        RobotInfo[] allBots = rc.senseNearbyRobots(9);
        for (int i = 0; i < allBots.length; i++)
        {
            if (!rc.getTeam().equals(allBots[i].getTeam()))
            {
                if (TrapExploiter.isLocationAffectedByTrap(rc, allBots[i].getLocation())) continue;
                enemyDamage += getDamage(rc, allBots[i]);
            }
        }
        return enemyDamage >= allyHealth;
    }

    public static boolean shouldRetreat(RobotController rc) throws GameActionException
    {
        if (!rc.isMovementReady()) return false;
        if (couldBeJailed(rc)) return true;
        if (areOutgunned(rc)) return true;
        return false;
    }

    public static boolean canJailBeforeBeingJailed(RobotController rc, RobotInfo target, MapLocation newLoc)
    {
        float enemyHealth = target.getHealth();
        float enemyDamage = 0;
        float allyHealth = rc.getHealth();
        float allyDamage = getDamagePerRound(rc);
        RobotInfo[] allBots = rc.senseNearbyRobots();
        for (int i = 0; i < allBots.length; i++)
        {
            if (rc.getTeam().equals(allBots[i].getTeam()) && allBots[i].getLocation().isWithinDistanceSquared(target.getLocation(), 9))
            {
                allyDamage += getDamagePerRound(rc, allBots[i]);
            }
            else if (!rc.getTeam().equals(allBots[i].getTeam()) && allBots[i].getLocation().isWithinDistanceSquared(newLoc, 9))
            {
                enemyDamage += getDamagePerRound(rc, allBots[i]);
            }
        }
        if (enemyDamage == 0)
        {
            return true;
        }
        if (allyDamage == 0)
        {
            return false;
        }
        float turnsUntilEnemiesJailed = enemyHealth / allyDamage;
        float turnsUntilAlliesJailed = allyHealth / enemyDamage;
        return turnsUntilEnemiesJailed > turnsUntilAlliesJailed;
    }

    public static boolean nearFlag(RobotController rc, RobotInfo bot)
    {
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (SharedVariables.flagStatuses[i].location.isAdjacentTo(bot.getLocation()))
            {
                return true;
            }
        }
        return false;

    }

    public static int getAlliesToJailImmediately(RobotController rc, RobotInfo[] allies, RobotInfo target)
    {
        int enemyHealth = target.getHealth();
        int damage = getDamage(rc);
        if (damage >= enemyHealth)
        {
            return 0;
        }
        int alliesNeeded = 0;
        for (int i = 0; i < allies.length; i++)
        {
            if (allies[i].getLocation().isWithinDistanceSquared(target.getLocation(), 9))
            {
                alliesNeeded++;
                damage += getDamage(rc, allies[i]);
                if (damage >= enemyHealth)
                {
                    return alliesNeeded;
                }
            }
        }
        return -1;
    }

    private static Direction getAttackLocation(RobotController rc, RobotInfo bot) throws GameActionException {
        if (rc.canAttack(bot.getLocation()))
        {
            return Direction.CENTER;
        }
        if (!rc.isMovementReady())
        {
            return null;
        }
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (rc.canMove(Constants.directions[i]))
            {
                if (rc.getLocation().add(Constants.directions[i]).isWithinDistanceSquared(bot.getLocation(), GameConstants.ATTACK_RADIUS_SQUARED))
                {
                    return Constants.directions[i];
                }
            }
        }
        return null;
    }

    public static boolean isLocSafe(RobotController rc, RobotInfo[] enemies)
    {
        for (int i = 0; i < enemies.length; i++)
        {
            if (enemies[i].getLocation().isWithinDistanceSquared(rc.getLocation(), 9))
            {
                if (TrapExploiter.isLocationAffectedByTrap(rc, enemies[i].getLocation())) continue;
                return false;
            }
        }
        return true;
    }
    public static boolean isLocSafe(RobotController rc, RobotInfo[] enemies, MapLocation loc)
    {
        for (int i = 0; i < enemies.length; i++)
        {
            if (enemies[i].getLocation().isWithinDistanceSquared(loc, 9))
            {
                if (TrapExploiter.isLocationAffectedByTrap(rc, enemies[i].getLocation())) continue;
                return false;
            }
        }
        return true;
    }

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

    public static int getDistToClosestFlag(RobotController rc, MapLocation loc)
    {
        int minDist = 9999;
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (!SharedVariables.flagStatuses[i].isLost)
            {
                int dist = loc.distanceSquaredTo(SharedVariables.flagStatuses[i].location);
                if (dist < minDist)
                {
                    minDist = dist;
                }
            }
        }
        return minDist;
    }

    public static void retreat(RobotController rc, RobotInfo[] allies, RobotInfo[] enemies) throws GameActionException {

        if (!rc.isMovementReady()) return;
        double currentAvgDistToAllies = getAverageDistanceToAlly(rc, allies, rc.getLocation());
        double avgDistToAlliesDirs[] = new double[Constants.directions.length];
        boolean isSafeDirs[] = new boolean[Constants.directions.length];
        boolean tooCloseToFlagCarrierDirs[] = new boolean[Constants.directions.length];
        double minAvgDist = 9999;
        Direction bestDir = null;
        //Iterate through all safeLocs closer to allies and get the one with the minAvgDist
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (!rc.canMove(Constants.directions[i])) continue;
            MapLocation newLoc = rc.getLocation().add(Constants.directions[i]);
            if (Pathing.isTooCloseToFlagCarrier(rc, newLoc))
            {
                tooCloseToFlagCarrierDirs[i] = true;
                continue;
            }
            isSafeDirs[i] = isLocSafe(rc, enemies, newLoc);
            avgDistToAlliesDirs[i] = getAverageDistanceToAlly(rc, allies, newLoc);
            if (!isSafeDirs[i]) continue;
            if (avgDistToAlliesDirs[i] > currentAvgDistToAllies) continue;
            if (avgDistToAlliesDirs[i] < minAvgDist)
            {
                minAvgDist = avgDistToAlliesDirs[i];
                bestDir = Constants.directions[i];
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
            return;
        }
        //Iterate through all safeLocs and get the one with the minAvgDist
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (!rc.canMove(Constants.directions[i])) continue;
            if (!isSafeDirs[i]) continue;
            if (tooCloseToFlagCarrierDirs[i]) continue;
            if (avgDistToAlliesDirs[i] < minAvgDist)
            {
                minAvgDist = avgDistToAlliesDirs[i];
                bestDir = Constants.directions[i];
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
            return;
        }
        /*
        //Try to get as close as possible to the ally
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (!rc.canMove(Constants.directions[i])) continue;
            if (tooCloseToFlagCarrierDirs[i]) continue;
            if (avgDistToAlliesDirs[i] < minAvgDist)
            {
                minAvgDist = avgDistToAlliesDirs[i];
                bestDir = Constants.directions[i];
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
            return;
        }

         */
        //Go toward the nearest friendly flag
        for (int i = 0; i < Constants.directions.length; i++)
        {
            if (!rc.canMove(Constants.directions[i])) continue;
            if (tooCloseToFlagCarrierDirs[i]) continue;
            MapLocation newLoc = rc.getLocation().add(Constants.directions[i]);
            int distToFlag = getDistToClosestFlag(rc, newLoc);
            if (distToFlag < minAvgDist)
            {
                minAvgDist = avgDistToAlliesDirs[i];
                bestDir = Constants.directions[i];
            }
        }
        if (bestDir != null)
        {
            rc.move(bestDir);
        }

        /*
        - Move towards the nearest ally while staying out of extended attack range
- If we can't safely approach an ally, try to move to avoid extended attack range
- If this isn't possible, go towards the nearest ally
- If this isn't possible, go towards the nearest friendly flag
         */
    }

    //Return a location the enemy could move to try to steal
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

    public static boolean micro(RobotController rc) throws GameActionException {
        //if (rc.getRoundNum() > 500) rc.resign();
        boolean dontMove = interceptFlagStealing(rc);
        if (rc.getID() == 12907)
            System.out.println("Bytecode Left Going into Micro = " + Clock.getBytecodesLeft());
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) return false;
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        RobotInfo[] closeEnemies = rc.senseNearbyRobots(9, rc.getTeam().opponent());
        if (enemies.length > 0)
        {
            boolean shouldRetreat = shouldRetreat(rc);

            if (shouldRetreat && !dontMove)
            {
                //Retreat immediately
                retreat(rc, allies, enemies);
                rc.setIndicatorString("Retreating Immediately");
                if (rc.getID() == 12907)
                    System.out.println("Retreating immediately!");
            }

            goNearAllies(rc, !dontMove);

            if (!rc.isActionReady()) return dontMove || !rc.isMovementReady();

            if (rc.getID() == 12907)
                System.out.println("Number of close enemies = " + closeEnemies.length);
            int minRank = 99999;
            RobotInfo bestTarget = null;
            Direction bestTargetDir = null;
            for (int i = 0; i < closeEnemies.length; i++)
            {
                if (Clock.getBytecodesLeft() < 8000) //If we don't have much bytecode left, break so we can at least attack something!
                {
                    break;
                }
                int startBytecodePerInstance = Clock.getBytecodeNum();
                //if we can't attack this enemy, skip
                Direction attackDir = getAttackLocation(rc, closeEnemies[i]);
                if (attackDir == null)
                {
                    if (rc.getID() == 12907)
                        System.out.println("Can't attack " + closeEnemies[i].getLocation());
                    continue;
                }

                //If we would have to leave a flag to attack or get too close to a flag carrier, skip
                if (!attackDir.equals(Direction.CENTER) && (dontMove || Pathing.isTooCloseToFlagCarrier(rc, rc.getLocation().add(attackDir))))
                {
                    if (rc.getID() == 12907)
                        System.out.println("Shouldn't move to attack " + closeEnemies[i].getLocation());
                    continue;
                }

                //if we can't jail the enemy before being jailed, skip
                /*
                if (!canJailBeforeBeingJailed(rc, closeEnemies[i], rc.getLocation().add(attackDir)))
                {
                    if (rc.getID() == 12907)
                        System.out.println("Too dangerous to attack " + closeEnemies[i].getLocation());
                    continue;
                }
                 */
                int rank = 0;
                //If we can jail a bot without help, it has the lowest rank
                //If a bot is near a flag, it should be prioritized over other bots
                //If a bot is affected by a trap, it should be prioritized over other bots
                //Remaining bots are ordered by number of allies needed to jail
                int alliesNeeded = getAlliesToJailImmediately(rc, allies, closeEnemies[i]);
                if (alliesNeeded > 0)
                {
                    rank += alliesNeeded;
                }
                else if (alliesNeeded == -1)
                {
                    rank += closeEnemies[i].getHealth();
                }
                //Prioritize enemies near flags
                if (!nearFlag(rc, closeEnemies[i]))
                {
                    rank += 10000;
                }
                //Prioritize enemies hit by traps
                if (!TrapExploiter.isLocationAffectedByTrap(rc, closeEnemies[i].getLocation()))
                {
                    rank += 1000;
                }
                if (alliesNeeded == 0)
                {
                    rank = 0;
                }
                if (rc.getID() == 12907)
                    System.out.println("Target = " + closeEnemies[i].getLocation() + ", rank = " + rank);
                if (rank < minRank)
                {
                    minRank = rank;
                    bestTarget = closeEnemies[i];
                    bestTargetDir = attackDir;
                }
                //System.out.println("Bytecode used for this instance = " + (Clock.getBytecodeNum() - startBytecodePerInstance));
                //Worst case is about 1.8k bytecode per instance
            }
            // Attackers should place stun traps wherever possible in enemy territory
            if (rc.isActionReady() && minRank > 11003 && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.STUN)) && !rc.senseMapInfo(rc.getLocation()).getTeamTerritory().isPlayer() && TrapExploiter.canPlaceTrap(rc, rc.getLocation()) && rc.canBuild(TrapType.STUN, rc.getLocation()))
            {
                rc.build(TrapType.STUN, rc.getLocation());
            }
            //If we have no targets, make traps if we have extra crumbs
            else if (rc.isActionReady() && bestTarget == null && rc.getCrumbs() > 2000 && TrapExploiter.canPlaceTrap(rc, rc.getLocation()) && rc.canBuild(TrapType.STUN, rc.getLocation()))
            {
                rc.build(TrapType.STUN, rc.getLocation());
            }
            else if (rc.isActionReady() && bestTarget != null)
            {
                if (rc.getID() == 12907)
                    System.out.println("Attacking target at " + bestTarget.getLocation());
                if (!bestTargetDir.equals(Direction.CENTER))
                {
                    rc.move(bestTargetDir);
                }
                rc.attack(bestTarget.getLocation());
                rc.setIndicatorString("Attacking " + bestTarget.getLocation());
            }
            //Fail case if we are low on bytecode, just attack the weakest bot or the flag carrier
            else if (rc.isActionReady())
            {
                int minHealth = 9999;
                RobotInfo bestTarget2 = null;
                for (int i = 0; i < closeEnemies.length; i++)
                {
                    if (rc.canAttack(closeEnemies[i].getLocation()))
                    {
                        if (closeEnemies[i].hasFlag())
                        {
                            bestTarget2 = closeEnemies[i];
                            break;
                        }
                        if (closeEnemies[i].getHealth() < minHealth)
                        {
                            minHealth = closeEnemies[i].getHealth();
                            bestTarget2 = closeEnemies[i];
                        }

                    }
                }
                if (bestTarget2 != null)
                {
                    rc.attack(bestTarget2.getLocation());
                    if (rc.getID() == 12907)
                        System.out.println("Attacking fail case target at " + bestTarget2.getLocation());
                    rc.setIndicatorString("Attacking (fail case) " + bestTarget2.getLocation());
                }
            }
            //If an enemy flag is close, go for it regardless of role
            //If we can still move and we aren't in a safe spot, RETREAT
            if (!dontMove && !isLocSafe(rc, enemies))
            {
                FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                if (enemyFlags.length > 0)
                {
                    Pathing.moveTowards(rc, enemyFlags[0].getLocation());
                }
                //rc.setIndicatorString("Retreating After Attack");
                retreat(rc, allies, enemies);
            }
            if (rc.getID() == 12907)
                System.out.println("Bytecode Left Going out of Micro = " + Clock.getBytecodesLeft());

        }
        RobotInfo[] enemiesToBeAwareOf = rc.senseNearbyRobots(16, rc.getTeam().opponent());
        if (enemiesToBeAwareOf.length == 0 && allies.length > 0 && rc.isActionReady())
        {
            int minHealth = 1000;
            RobotInfo weakestAlly = null;
            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].getLocation().isWithinDistanceSquared(rc.getLocation(), GameConstants.HEAL_RADIUS_SQUARED))
                {
                    if (allies[i].getHealth() < minHealth)
                    {
                        minHealth = allies[i].getHealth();
                        weakestAlly = allies[i];
                    }
                }
            }
            if (weakestAlly != null)
            {
                rc.heal(weakestAlly.getLocation());
                rc.setIndicatorString("Healing " + weakestAlly.getLocation());
            }
        }
        return dontMove || !rc.isMovementReady();
    }

    public static void goNearAllies(RobotController rc, boolean canMove) throws GameActionException {
        if (!canMove) return;
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        double bestDist = getAverageDistanceToAlly(rc, allies, rc.getLocation()) - 3;
        if (allies.length < 8)
        {
            Direction dir = null;
            for (int i = 0; i < Constants.directions.length; i++)
            {
                if (!rc.canMove(Constants.directions[i])) continue;
                double avgDist = getAverageDistanceToAlly(rc, allies, rc.getLocation().add(Constants.directions[i]));
                if (avgDist < bestDist)
                {
                    bestDist = avgDist;
                    dir = Constants.directions[i];
                }
            }
            if (dir != null)
            {
                rc.move(dir);
            }
        }
    }
}



package v21;

import battlecode.common.*;

// Never move to attack if a flag carrier is present, unless the target is the flag carrier.

class BotProfile {
    MapLocation location;
    int id;
    int health;
    boolean isMarked;
    boolean isCloseRange;
    boolean isClustered;
    int attackLevel;
    int healLevel;
    boolean holdingFlag;
    boolean closeToFlag;
    int rank = -1;
    Direction attackDir;
    boolean affectedByTrap;

    BotProfile() {}

    //This is what determines attack priorities
    void updateRank(int attackPower)
    {
        /*
            Rank Order:
            - Always attack enemies that could be destroyed this turn
            - Bots near friendly flags
            - Bots affected by traps
            - Flag holders
            - Close Range Bots
            - Unclustered Bots

            1) Weak enemies not close
            2) Weak enemies close

            Otherwise, rank = 2
            + (Near friendly flag) * 16
            + (Affected by traps) * 8
            + (Holding Flag) * 4
            + (Close Range) * 2
            + (Unclustered)
         */

        if (health <= attackPower)
        {
            if (isCloseRange)
            {
                rank = 2;
            }
            else
            {
                rank = 1;
            }
            return;
        }
        /*
        Otherwise, rank = 2
            + (Near friendly flag) * 16
            + (Affected by traps) * 8
            + (Holding Flag) * 4
            + (Close Range) * 2
            + (Unclustered)
         */
        rank = 2;
        if (!closeToFlag)
        {
            rank += 16;
        }
        if (!holdingFlag)
        {
            rank += 8;
        }
        if (!affectedByTrap)
        {
            rank += 4;
        }
        if (!isCloseRange)
        {
            rank += 2;
        }
        if (isClustered)
        {
            rank += 1;
        }
    }

    void printProfile()
    {
        System.out.println("PROFILE REPORT");
        System.out.println("---------------------");
        System.out.println("Location = " + location);
        System.out.println("ID = " + id);
        System.out.println("Attack Direction = " + attackDir);
        System.out.println("Current Health = " + health);
        System.out.println("Marked? = " + isMarked);
        System.out.println("Close Range? = " + isCloseRange);
        System.out.println("Clustered? = " + isClustered);
        System.out.println("Attack Level = " + attackLevel);
        System.out.println("Heal Level = " + healLevel);
        System.out.println("Holding Flag? = " + holdingFlag);
        System.out.println("Rank = " + rank);
        System.out.println("---------------------");
    }
}

class Order
{
    Direction attackMovement = null;
    MapLocation attackLoc = null;
    Order(){}
    boolean targetExists()
    {
        return attackLoc != null;
    }
    boolean moveNecessary()
    {
        return (attackMovement != null) && !attackMovement.equals(Direction.CENTER);
    }
}

public class Combat {

    //Don't allow movement near flags unless there aren't many enemies near that flag
    public static boolean canApproachSiege(RobotController rc, MapLocation loc) throws GameActionException {
        FlagInfo[] nearEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        RobotInfo[] nearEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = 0; i < nearEnemyFlags.length; i++)
        {
            if (nearEnemies.length > nearAllies.length + 1 && !nearEnemyFlags[i].isPickedUp())
            {
                return false;
            }
        }
        return true;
    }

    public static void siegeCheck(RobotController rc) throws GameActionException {
        FlagInfo[] nearEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        RobotInfo[] nearEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] nearAllies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = 0; i < nearEnemyFlags.length; i++)
        {
            if (nearEnemies.length > nearAllies.length + 1 && !nearEnemyFlags[i].isPickedUp())
            {
                flee(rc);
                break;
            }
        }
    }

    private static void evade(RobotController rc) throws GameActionException {
        if (!rc.isMovementReady()) return;
        int closestDist = getClosestEnemyDist(rc, rc.getLocation());
        if (closestDist <= 9)
        {
            //Move to a safe space
            int bestDist = closestDist;
            Direction bestDir = null;
            for (int i = 0; i < Constants.cardinalDirections.length; i++)
            {
                /*
                if (!canApproachSiege(rc, rc.getLocation().add(Constants.cardinalDirections[i])))
                {
                    return;
                }

                 */
                if (rc.canMove(Constants.cardinalDirections[i]))
                {
                    closestDist = getClosestEnemyDist(rc, rc.getLocation().add(Constants.cardinalDirections[i]));
                    if (closestDist > bestDist)
                    {
                        bestDist = closestDist;
                        bestDir = Constants.cardinalDirections[i];
                    }
                }
            }
            if (bestDir != null)
            {
                rc.move(bestDir);
            }
        }
    }

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

    private static Direction canAttackLocation(RobotController rc, MapLocation loc) throws GameActionException {
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
            /*
            if (!canApproachSiege(rc, rc.getLocation().add(Constants.directions[i])))
            {
                return null;
            }

             */
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

    //Get the direction that gets us furthest from the dams
    private static int distToDam(RobotController rc, MapLocation loc) throws GameActionException {
        MapInfo[] mapInfos = rc.senseNearbyMapInfos();
        int minDist = 9999;
        for (int i = 0; i < mapInfos.length; i++)
        {
            if (mapInfos[i].isDam())
            {
                int dist = loc.distanceSquaredTo(mapInfos[i].getMapLocation());
                if (dist < minDist)
                {
                    minDist = dist;
                }
            }
        }
        return minDist;
    }

    //TODO: Significant Bytecode cost (sets total to about 10k) when its damSetup rounds
    private static void damSetup(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() > Constants.MIN_ROUND_RANGE_FROM_DAMS && rc.getRoundNum() < GameConstants.SETUP_ROUNDS && rc.senseNearbyCrumbs(-1).length == 0)
        {
            RobotInfo[] farEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            int distToDam = distToDam(rc, rc.getLocation());
            if (farEnemies.length > 0)
            {
                //We should set up traps and get away from the dams
                if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.STUN)) && distToDam < 3 && rc.canBuild(TrapType.STUN, rc.getLocation()))
                {
                    //Lay trap
                    //Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.STUN));
                    rc.build(TrapType.STUN, rc.getLocation());
                }
            }
            if (rc.isMovementReady() && distToDam <= GameConstants.VISION_RADIUS_SQUARED)
            {
                //Get away from the dams
                int maxDist = distToDam;
                Direction bestDir = null;
                for (int i = 0; i < Constants.directions.length; i++)
                {
                    if (rc.canMove(Constants.directions[i]))
                    {
                        MapLocation next = rc.getLocation().add(Constants.directions[i]);
                        int newDist = distToDam(rc, next);
                        if (newDist > maxDist)
                        {
                            maxDist = newDist;
                            bestDir = Constants.directions[i];
                        }
                    }
                }
                if (bestDir != null)
                {
                    rc.move(bestDir);
                }
            }
        }
    }

    private static Order giveOrder(RobotController rc, boolean dontMove, boolean needToFlee, boolean isFlagCarrierVisible) throws GameActionException {
        //System.out.println("Starting at round " + rc.getRoundNum() + " with " + Clock.getBytecodesLeft() + " bytecode left.");
        if (!rc.isActionReady() || rc.getRoundNum() <= GameConstants.SETUP_ROUNDS)
        {
            return new Order();
        }

        RobotInfo[] profileEnemies = rc.senseNearbyRobots(9, rc.getTeam().opponent());
        RobotInfo[] allEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        boolean dontMoveToAttack = dontMove || needToFlee;

        boolean tryStunTrap = false;
        if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.STUN)) && !rc.senseMapInfo(rc.getLocation()).getTeamTerritory().isPlayer() && TrapExploiter.canPlaceTrap(rc, rc.getLocation()))
        {
            tryStunTrap = true;
        }

        int[] markedIds = Communication.getMarkedIds();
        BotProfile[] profiles = new BotProfile[profileEnemies.length];
        int numProfiles = 0;
        int bestRank = 9999;
        for (int i = 0; i < profileEnemies.length; i++)
        {
            Direction attackDir = canAttackLocation(rc, profileEnemies[i].getLocation());
            if (attackDir != null)
            {
                //Compute Profile
                BotProfile profile = new BotProfile();
                profile.isCloseRange = attackDir.equals(Direction.CENTER);
                profile.isMarked = false;
                for (int j = 0; j < markedIds.length; j++)
                {
                    if (markedIds[j] == profileEnemies[i].getID())
                    {
                        profile.isMarked = true;
                        break;
                    }
                }
                profile.closeToFlag = false;
                for (int j = 0; j < SharedVariables.flagStatuses.length; j++)
                {
                    if (!SharedVariables.flagStatuses[j].isLost && profileEnemies[i].getLocation().isAdjacentTo(SharedVariables.flagStatuses[j].location))
                    {
                        profile.closeToFlag = true;
                        break;
                    }
                }

                profile.attackLevel = profileEnemies[i].getAttackLevel();
                profile.healLevel = profileEnemies[i].getHealLevel();
                profile.health = profileEnemies[i].getHealth();
                profile.location = profileEnemies[i].getLocation();
                profile.affectedByTrap = TrapExploiter.isLocationAffectedByTrap(rc, profile.location);
                profile.id = profileEnemies[i].getID();
                profile.holdingFlag = profileEnemies[i].hasFlag();
                profile.attackDir = attackDir;

                //Made a fix to this logic
                if (!attackDir.equals(Direction.CENTER) && (dontMoveToAttack || (isFlagCarrierVisible && !profile.holdingFlag)))
                {
                    continue;
                }
                int numCloseEnemies = 0;
                profile.isClustered = false;
                for (int j = 0; j < allEnemies.length; j++)
                {
                    if (allEnemies[j].getLocation().isWithinDistanceSquared(profile.location,9))
                    {
                        numCloseEnemies++;
                        if (numCloseEnemies >= Constants.MIN_ENEMIES_FOR_CLUSTER)
                        {
                            profile.isClustered = true;
                            break;
                        }
                    }
                }
                profile.updateRank(getDamage(rc));
                if (profile.rank < bestRank)
                {
                    bestRank = profile.rank;
                }
                profiles[numProfiles] = profile;
                numProfiles++;
                //Profile is fully generated
            }
        }

        //If we don't have a high priority target, try to pass and place a stun trap instead
        if (bestRank > 29)
        {
            return new Order();
        }

        //Check for profile with the lowest id within the lowest rank
        Order order = new Order();
        BotProfile bestProfile = null;
        int lowestId = 99999;
        for (int j = 0; j < numProfiles; j++)
        {
            if (profiles[j].rank == bestRank)
            {
                if (profiles[j].id < lowestId)
                {
                    bestProfile = profiles[j];
                    lowestId = profiles[j].id;
                }
            }
        }
        if (bestProfile != null)
        {
            order.attackMovement = bestProfile.attackDir;
            order.attackLoc = bestProfile.location;
        }
        //System.out.println("Ending at round " + rc.getRoundNum() + " with " + Clock.getBytecodesLeft() + " bytecode left.");
        return order;
    }

    //Return true if we could still move after this
    public static boolean micro(RobotController rc, boolean canMove) throws GameActionException {
        //Try to intercept flag stealing
        int bytecode = Clock.getBytecodeNum();
        boolean dontMove = interceptFlagStealing(rc);
        if (!dontMove)
        {
            dontMove = !canMove;
        }

        //Try the dam setup strategy
        damSetup(rc);

        //Determine if we need to flee
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        MapLocation enemyFlagCarrierLoc = null;
        if (enemies.length > 0)
        {
            for (int i = 0; i < enemies.length; i++)
            {
                if (enemies[i].hasFlag())
                {
                    enemyFlagCarrierLoc = enemies[i].getLocation();
                    break;
                }
            }
        }
        boolean enemyFlagCarrierVisible = enemyFlagCarrierLoc != null;
        MapLocation friendlyFlagCarrierLoc = null;
        if (allies.length > 0)
        {
            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].hasFlag())
                {
                    friendlyFlagCarrierLoc = allies[i].getLocation();
                    break;
                }
            }
        }
        boolean friendlyFlagCarrierVisible = friendlyFlagCarrierLoc != null;
        boolean flagCarrierVisible = friendlyFlagCarrierVisible || enemyFlagCarrierVisible;

        boolean lowHealth = rc.getHealth() < Constants.MIN_HEALTH_FRACTION_BEFORE_RETREAT * GameConstants.DEFAULT_HEALTH;
        boolean outnumbered = enemies.length > allies.length + 1;
        boolean canSeeEnemyFlag = rc.senseNearbyFlags(-1, rc.getTeam().opponent()).length > 0;
        boolean needToFlee = lowHealth || (outnumbered && !canSeeEnemyFlag);

        Order myOrder = giveOrder(rc, dontMove, needToFlee, flagCarrierVisible);
        if (myOrder.targetExists())
        {
            if (myOrder.moveNecessary())
            {
                rc.move(myOrder.attackMovement);
            }
            rc.attack(myOrder.attackLoc);
        }

        //If we need to flee, flee
        if (needToFlee)
        {
            flee(rc); //TODO: Rework fleeing
        }

        //Try to generate another order to attack
        Order myOrder2 = giveOrder(rc, dontMove, needToFlee, flagCarrierVisible);
        if (myOrder2.targetExists())
        {
            if (myOrder2.moveNecessary())
            {
                rc.move(myOrder2.attackMovement);
            }
            rc.attack(myOrder2.attackLoc);
        }

        //If no enemies are visible, try to heal the ally with the lowest health
        RobotInfo[] alliesToHeal = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam());
        if (enemies.length == 0 && alliesToHeal.length > 0 && rc.isActionReady())
        {
            int minHealth = 9999;
            MapLocation bestLoc = null;
            for (int i = 0; i < alliesToHeal.length; i++)
            {
                if (alliesToHeal[i].getHealth() < minHealth && rc.canHeal(alliesToHeal[i].getLocation()))
                {
                    minHealth = alliesToHeal[i].getHealth();
                    bestLoc = alliesToHeal[i].getLocation();
                }
            }
            if (bestLoc != null)
                rc.heal(bestLoc);
        }

        //If there's an enemy flag carrier visible, move towards it
        if (enemyFlagCarrierVisible)
        {
            Pathing.moveTowards(rc, enemyFlagCarrierLoc);
        }

        //If there's a friendly flag carrier visible, move towards it
        if (friendlyFlagCarrierVisible)
        {
            Pathing.moveTowards(rc, friendlyFlagCarrierLoc);
        }

        //If no flag carrier is visible, evade if possible
        if (!flagCarrierVisible)
        {
            evade(rc);
        }

        //If we can still move and crumbs are nearby, go to get them
        MapLocation[] nearCrumbs = rc.senseNearbyCrumbs(-1);
        if (nearCrumbs.length > 0 && rc.isMovementReady() && !dontMove)
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
        return rc.isMovementReady() && !dontMove;
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

    //If we see an enemy able to get within range to grab the flag, move into a space
    //that would block that movement if possible. Return true if we shouldn't move.
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

}

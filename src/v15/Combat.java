package v15;

import battlecode.common.*;

class BotProfile {
    static final int MIN_RANK = 1;
    static final int MAX_RANK = 12;
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

    BotProfile() {}

    //This is what determines attack priorities
    void updateRank(int attackPower)
    {
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
        }
        else if (holdingFlag)
        {
            rank = 3;
        }
        else if (closeToFlag)
        {
            rank = 4;
        }
        else if (isMarked)
        {
            if (isClustered)
            {
                if (isCloseRange)
                {
                    rank = 7;
                }
                else
                {
                    rank = 8;
                }
            }
            else
            {
                if (isCloseRange)
                {
                    rank = 5;
                }
                else
                {
                    rank = 6;
                }
            }
        }
        else {
            if (isClustered)
            {
                if (isCloseRange)
                {
                    rank = 11;
                }
                else
                {
                    rank = 12;
                }
            }
            else
            {
                if (isCloseRange)
                {
                    rank = 9;
                }
                else
                {
                    rank = 10;
                }
            }
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

    private static Order giveOrder(RobotController rc) throws GameActionException {
        //System.out.println("Starting at round " + rc.getRoundNum() + " with " + Clock.getBytecodesLeft() + " bytecode left.");
        RobotInfo[] profileEnemies = rc.senseNearbyRobots(9, rc.getTeam().opponent());
        RobotInfo[] allEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());

        /*
            An enemy should be marked if it has a lot of exp or if
            it is close to an allied flag. For now, we don't use marks
         */
        if (!rc.isActionReady() || rc.getRoundNum() <= GameConstants.SETUP_ROUNDS)
        {
            return new Order();
        }
        int[] markedIds = Communication.getMarkedIds();
        BotProfile[] profiles = new BotProfile[profileEnemies.length];
        int numProfiles = 0;
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
                profile.id = profileEnemies[i].getID();
                profile.holdingFlag = profileEnemies[i].hasFlag();
                profile.attackDir = attackDir;
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
                profiles[numProfiles] = profile;
                numProfiles++;
                //Profile is fully generated
            }
        }

        //Check for profile with the lowest id within the lowest rank
        Order order = new Order();
        BotProfile bestProfile = null;
        for (int rank = BotProfile.MIN_RANK; rank < BotProfile.MAX_RANK + 1; rank++)
        {
            int lowestId = 99999;
            for (int j = 0; j < numProfiles; j++)
            {
                if (profiles[j].rank == rank)
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
                //bestProfile.printProfile();
                break;
            }
        }
        //System.out.println("Ending at round " + rc.getRoundNum() + " with " + Clock.getBytecodesLeft() + " bytecode left.");
        return order;
    }

    //Get Damage Per Round of Self
    private static int getDamage(RobotController rc)
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
        if (rc.getRoundNum() > Constants.MIN_ROUND_RANGE_FROM_DAMS && rc.getRoundNum() < GameConstants.SETUP_ROUNDS)
        {
            RobotInfo[] farEnemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            int distToDam = distToDam(rc, rc.getLocation());
            if (farEnemies.length > 0)
            {
                //We should set up traps and get away from the dams
                if (rc.isActionReady() && Economy.canSpendAttack(rc, Economy.getTrapCost(rc, TrapType.STUN)) && distToDam < 3 && rc.canBuild(TrapType.STUN, rc.getLocation()))
                {
                    //Lay trap
                    Communication.logCrumbsAttack(rc, Economy.getTrapCost(rc, TrapType.STUN));
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

    public static void micro(RobotController rc) throws GameActionException {
        damSetup(rc);
        if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS)
        {
            Order myOrder = giveOrder(rc);
            if (myOrder.targetExists())
            {
                if (myOrder.moveNecessary())
                {
                    rc.move(myOrder.attackMovement);
                }
                rc.attack(myOrder.attackLoc);
            }
        }


        //If there are more enemies than allies and we have enough crumbs and we're in enemy territory, lay a trap
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        RobotInfo[] allies = rc.senseNearbyRobots(GameConstants.HEAL_RADIUS_SQUARED, rc.getTeam());

        //If no enemies are seen, heal an ally
        if (enemies.length == 0 && allies.length > 0 && rc.isActionReady())
        {
            int minHealth = 9999;
            MapLocation bestLoc = null;
            for (int i = 0; i < allies.length; i++)
            {
                if (allies[i].getHealth() < minHealth && rc.canHeal(allies[i].getLocation()))
                {
                    minHealth = allies[i].getHealth();
                    bestLoc = allies[i].getLocation();
                }
            }
            if (bestLoc != null)
                rc.heal(bestLoc);
        }

        //If there's an enemy flag carrier, move towards it
        if (enemies.length > 0 && rc.isMovementReady())
        {
            for (int i = 0; i < enemies.length; i++)
            {
                if (enemies[i].hasFlag())
                {
                    Pathing.moveTowards(rc, enemies[i].getLocation());
                }
            }
        }
        //If we can still move and enemies are near and we don't have a numbers advantage, kite them
        if (enemies.length > allies.length && rc.isMovementReady())
        {
            evade(rc);
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

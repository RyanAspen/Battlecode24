package v18;

import battlecode.common.*;

public class Economy {

    public static int getTotalCrumbsSpent(RobotController rc) throws GameActionException {
        return Communication.getCrumbsSpentAttacking(rc) + Communication.getCrumbsSpentAllDefense(rc) + Communication.getCrumbsSpentPathing(rc) + Communication.getCrumbsSpentFree(rc);
    }
    public static boolean canSpendAttack(RobotController rc, int cost) throws GameActionException {
        int attackAvailable = getCrumbsForAttack(rc);
        int freeAvailable = getCrumbsForFree(rc);
        return cost <= attackAvailable + freeAvailable;
    }
    public static boolean canSpendDefense(RobotController rc, int cost,MapLocation base) throws GameActionException {
        int defenseAvailable = getCrumbsForDefense(rc, base);
        int freeAvailable = getCrumbsForFree(rc);
        return cost <= defenseAvailable + freeAvailable;
    }
    public static boolean canSpendPathing(RobotController rc, int cost) throws GameActionException {
        int pathingAvailable = getCrumbsForPathing(rc);
        int freeAvailable = getCrumbsForFree(rc);
        return cost <= pathingAvailable + freeAvailable;
    }

    public static int getCrumbsForAttack(RobotController rc) throws GameActionException {
        int totalCrumbsGained = getTotalCrumbsSpent(rc) + rc.getCrumbs();
        int attackBudget = (int)(totalCrumbsGained * Constants.CRUMB_PROPORTION_ATTACK);
        return attackBudget - Communication.getCrumbsSpentAttacking(rc);
    }
    public static int getCrumbsForDefense(RobotController rc, MapLocation base) throws GameActionException {
        int totalCrumbsGained = getTotalCrumbsSpent(rc) + rc.getCrumbs();
        FlagStatus[] friendlyFlags = Communication.getFriendlyFlagStatuses(rc);
        int defenseBudget, defenseAvailable;
        for (int i = 0; i < friendlyFlags.length; i++)
        {
            if (friendlyFlags[i].location != null && friendlyFlags[i].location.equals(base))
            {
                defenseBudget = (int)(totalCrumbsGained * Constants.CRUMB_PROPORTION_DEFENSE_PER_FLAG);
                defenseAvailable = defenseBudget - Communication.getCrumbsSpentDefense(rc, base);
                return defenseAvailable;
            }
        }
        return 0;
    }
    public static int getCrumbsForPathing(RobotController rc) throws GameActionException {
        int totalCrumbsGained = getTotalCrumbsSpent(rc) + rc.getCrumbs();
        int pathingBudget = (int)(totalCrumbsGained * Constants.CRUMB_PROPORTION_PATHING);
        return pathingBudget - Communication.getCrumbsSpentPathing(rc);
    }
    public static int getCrumbsForFree(RobotController rc) throws GameActionException {
        int totalCrumbsGained = getTotalCrumbsSpent(rc) + rc.getCrumbs();
        int freeBudget = (int)(totalCrumbsGained * Constants.CRUMB_PROPORTION_FREE);
        return freeBudget - Communication.getCrumbsSpentFree(rc);
    }
    public static int getTrapCost(RobotController rc, TrapType type)
    {
        return (int) Math.round(type.buildCost*(1+0.01* SkillType.BUILD.getSkillEffect(rc.getLevel(SkillType.BUILD))));
    }
    public static int getFillCost(RobotController rc)
    {
        return (int) Math.round(GameConstants.FILL_COST*(1+0.01* SkillType.BUILD.getSkillEffect(rc.getLevel(SkillType.BUILD))));
    }
    public static void printBudgets(RobotController rc) throws GameActionException {
        FlagStatus[] friendlyFlags = Communication.getFriendlyFlagStatuses(rc);
        System.out.println("--------");
        System.out.println("Current Crumbs = " + rc.getCrumbs());
        int fullBudget = 0;
        System.out.println("Attacking Budget = " + getCrumbsForAttack(rc));
        fullBudget += getCrumbsForAttack(rc);
        for (int i = 0; i < friendlyFlags.length; i++)
        {
            System.out.println("Defense Budget for Flag " + friendlyFlags[i] + " = " + getCrumbsForDefense(rc, friendlyFlags[i].location));
            fullBudget += getCrumbsForDefense(rc, friendlyFlags[i].location);
        }
        System.out.println("Pathing Budget = " + getCrumbsForPathing(rc));
        fullBudget += getCrumbsForPathing(rc);
        System.out.println("Free Budget = " + getCrumbsForFree(rc));
        fullBudget += getCrumbsForFree(rc);
        System.out.println("Full Budget = " + fullBudget);
        System.out.println("Attacking Crumbs Spent = " + Communication.getCrumbsSpentAttacking(rc));
        for (int i = 0; i < friendlyFlags.length; i++)
        {
            System.out.println("Defense Crumbs Spent for Flag " + friendlyFlags[i] + " = " + Communication.getCrumbsSpentDefense(rc, friendlyFlags[i].location));
        }
        System.out.println("Pathing Crumbs Spent = " + Communication.getCrumbsSpentPathing(rc));
        System.out.println("Free Crumbs Spent = " + Communication.getCrumbsSpentFree(rc));
        System.out.println("Total Crumbs Spent = " + getTotalCrumbsSpent(rc));
        int error = fullBudget - rc.getCrumbs();
        System.out.println("Budget Error = " + error);
        System.out.println("--------");
    }

}

package v22;

import battlecode.common.*;

public class Economy {

    //EXPERIMENT: Remove this

    public static boolean canSpendAttack(RobotController rc, int cost)
    {
        return rc.getCrumbs() >= cost + 500;
    }
    public static boolean canSpendDefense(RobotController rc, int cost)
    {
        return rc.getCrumbs() >= cost + 200;
    }
    public static boolean canSpendPathing(RobotController rc, int cost)
    {
        return rc.getCrumbs() >= cost;
    }

    public static int getTrapCost(RobotController rc, TrapType type)
    {
        return (int) Math.round(type.buildCost*(1+0.01* SkillType.BUILD.getSkillEffect(rc.getLevel(SkillType.BUILD))));
    }
    public static int getFillCost(RobotController rc)
    {
        return (int) Math.round(GameConstants.FILL_COST*(1+0.01* SkillType.BUILD.getSkillEffect(rc.getLevel(SkillType.BUILD))));
    }

}

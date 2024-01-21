package v19;

/*
    This file is meant for macro-level strategy. For example, decide whether to specialize in a
    particular area, whether to mainly be a defender or attacker, etc.
 */


import battlecode.common.GameActionException;
import battlecode.common.GlobalUpgrade;
import battlecode.common.RobotController;
import battlecode.common.SkillType;

public class Strategy {

    private static void changeRole(RobotController rc, Role role) throws GameActionException {
        if (SharedVariables.currentRole == null)
        {
            if (role == Role.Defender)
            {
                //Reset flagToDefend variables and increment counters once we get a new flag
                SharedVariables.flagIdToProtect = -1;
                SharedVariables.flagToProtect = null;
            }
            Communication.initializeRole(rc, role);
            SharedVariables.currentRole = role;
        }
        else if (role != SharedVariables.currentRole)
        {
            if (SharedVariables.currentRole == Role.Defender)
            {
                //Remove from comms
                //Decrement live counters and temp live counters (within removeDefenderPosition)
                Communication.removeDefenderPosition(rc, SharedVariables.flagIdToProtect);
            }
            else if (role == Role.Defender)
            {
                //Reset flagToDefend variables and increment counters once we get a new flag
                SharedVariables.flagIdToProtect = -1;
                SharedVariables.flagToProtect = null;
            }
            Communication.reportRoleSwitch(rc, SharedVariables.currentRole, role);
            SharedVariables.currentRole = role;
        }
    }

    public static void changeStrategy(RobotController rc) throws GameActionException {
        int currentStrategy = Communication.getStrategy(rc);
        switch (currentStrategy)
        {
            case (Constants.AGGRESSIVE_STRATEGY):
                if (rc.canBuyGlobal(GlobalUpgrade.ATTACK))
                {
                    rc.buyGlobal(GlobalUpgrade.ATTACK);
                }
                else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
                {
                    rc.buyGlobal(GlobalUpgrade.CAPTURING);
                }
                else if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
                {
                    rc.buyGlobal(GlobalUpgrade.HEALING);
                }
                break;
            case (Constants.BALANCED_STRATEGY):
                if (rc.canBuyGlobal(GlobalUpgrade.ATTACK))
                {
                    rc.buyGlobal(GlobalUpgrade.ATTACK);
                }
                else if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
                {
                    rc.buyGlobal(GlobalUpgrade.HEALING);
                }
                else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
                {
                    rc.buyGlobal(GlobalUpgrade.CAPTURING);
                }
                break;
            case (Constants.TURTLE_STRATEGY):
                if (rc.canBuyGlobal(GlobalUpgrade.HEALING))
                {
                    rc.buyGlobal(GlobalUpgrade.HEALING);
                }
                else if (rc.canBuyGlobal(GlobalUpgrade.ATTACK))
                {
                    rc.buyGlobal(GlobalUpgrade.ATTACK);
                }
                else if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
                {
                    rc.buyGlobal(GlobalUpgrade.CAPTURING);
                }
                break;
        }
        int friendlyFlagCount = Communication.getFriendlyFlagsCount(rc);
        int enemyFlagCount = Communication.getEnemyFlagsCount(rc);


        //Overall defensive map
        if (false)
        {
            if (friendlyFlagCount < enemyFlagCount && currentStrategy != Constants.AGGRESSIVE_STRATEGY)
            {
                //Very Offensive Strategy
                Communication.setStrategy(rc, Constants.AGGRESSIVE_STRATEGY);
                System.out.println("New Strategy = AGGRESSIVE");
            }
            else if (friendlyFlagCount >= enemyFlagCount && currentStrategy != Constants.TURTLE_STRATEGY)
            {
                //Defensive Strategy (TURTLE)
                Communication.setStrategy(rc, Constants.TURTLE_STRATEGY);
                System.out.println("New Strategy = TURTLE");
            }
        }
        else
        {
            //More aggressive map
            if (friendlyFlagCount == 1 && enemyFlagCount == 1 && currentStrategy != Constants.TURTLE_STRATEGY)
            {
                //Defensive Strategy (TURTLE)
                Communication.setStrategy(rc, Constants.TURTLE_STRATEGY);
                System.out.println("New Strategy = TURTLE");
            }
            else if (friendlyFlagCount > 1 && friendlyFlagCount == enemyFlagCount && currentStrategy != Constants.BALANCED_STRATEGY)
            {
                //Offensive Strategy
                Communication.setStrategy(rc, Constants.BALANCED_STRATEGY);
                System.out.println("New Strategy = BALANCED");
            }
            else if (friendlyFlagCount > enemyFlagCount && currentStrategy != Constants.TURTLE_STRATEGY)
            {
                //Defensive Strategy (TURTLE)
                Communication.setStrategy(rc, Constants.TURTLE_STRATEGY);
                System.out.println("New Strategy = TURTLE");
            }
            else if (friendlyFlagCount < enemyFlagCount && currentStrategy != Constants.AGGRESSIVE_STRATEGY)
            {
                //Very Offensive Strategy
                Communication.setStrategy(rc, Constants.AGGRESSIVE_STRATEGY);
                System.out.println("New Strategy = AGGRESSIVE");
            }
        }
    }

    public static void updateRole(RobotController rc) throws GameActionException {
        int attackExp = rc.getExperience(SkillType.ATTACK);
        int buildExp = rc.getExperience(SkillType.BUILD);
        int healExp = rc.getExperience(SkillType.HEAL);
        int attackerRoleCount = Communication.getRoleCount(rc, Role.Attacker);
        int defenderRoleCount = Communication.getRoleCount(rc, Role.Defender);
        int desiredAttackers = Communication.getDesiredAttackers(rc);
        int desiredDefenders = Communication.getDesiredDefenders(rc);
        boolean canSwitchRole = true;
        if (SharedVariables.currentRole != null)
        {
            switch (SharedVariables.currentRole)
            {
                case Attacker:
                    canSwitchRole = (attackerRoleCount > desiredAttackers);
                    break;
                case Defender:
                    canSwitchRole = (defenderRoleCount > desiredDefenders);
                    break;
                default:
                    canSwitchRole = false;
            }
        }
        if (canSwitchRole)
        {
            if (defenderRoleCount < desiredDefenders)
            {
                changeRole(rc, Role.Defender);
            }
            else if (attackerRoleCount < desiredAttackers)
            {
                changeRole(rc, Role.Attacker);
            }
            else
            {
                changeRole(rc, Role.Attacker);
            }
        }
    }
}

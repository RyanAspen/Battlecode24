package v8;

/*
    This file is meant for macro-level strategy. For example, decide whether to specialize in a
    particular area, whether to mainly be a defender or attacker, etc.
 */

import battlecode.common.*;

enum Role {
    Normal,
    Attacker,
    Defender,
    Builder,
    Healer
}

public class Strategy {

    private static Role currentRole = Role.Normal;
    private static int MIN_DEFENDERS = 10;
    private static int MIN_ATTACKERS = 15;
    private static int MIN_HEALERS = 5;
    private static int MIN_BUILDERS = 15;

    private static void changeRole(RobotController rc, Role role) throws GameActionException {
        if (role != currentRole)
        {
            Communication.reportRoleSwitch(rc, currentRole, role);
            currentRole = role;

        }
    }

    public static void changeStrategy(RobotController rc) throws GameActionException {
        /*
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
        {
            rc.buyGlobal(GlobalUpgrade.ACTION);
        }
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
        {
            rc.buyGlobal(GlobalUpgrade.CAPTURING);
        }
         */
        MapLocation[] friendlyFlags = Communication.getFriendlyFlagLocations();
        MapLocation[] enemyFlags = Communication.getEnemyFlagLocations();
        System.out.println("Friendly Flags = " + friendlyFlags.length + " Enemy Flags = " + enemyFlags.length);
        if (friendlyFlags.length == 3)
        {
            //Offensive Strategy
            System.out.println("OFFENSE");
            MIN_DEFENDERS = 15;
            MIN_ATTACKERS = 20;
            MIN_HEALERS = 5;
            MIN_BUILDERS = 10;
        }
        else if (friendlyFlags.length > enemyFlags.length)
        {
            //Defensive Strategy (TURTLE)
            System.out.println("TURTLE");
            MIN_DEFENDERS = 30;
            MIN_ATTACKERS = 0;
            MIN_HEALERS = 5;
            MIN_BUILDERS = 15;
        }
        else if (enemyFlags.length > friendlyFlags.length)
        {
            //Very Offensive Strategy
            System.out.println("AGGRESSIVE");
            MIN_DEFENDERS = 10;
            MIN_ATTACKERS = 30;
            MIN_HEALERS = 5;
            MIN_BUILDERS = 5;
        }
    }

    public static Role getRole(RobotController rc) throws GameActionException {
        int attackExp = rc.getExperience(SkillType.ATTACK);
        int buildExp = rc.getExperience(SkillType.BUILD);
        int healExp = rc.getExperience(SkillType.HEAL);
        int normalRoleCount = Communication.getRoleCount(rc, Role.Normal);
        int attackerRoleCount = Communication.getRoleCount(rc, Role.Attacker);
        int defenderRoleCount = Communication.getRoleCount(rc, Role.Defender);
        int builderRoleCount = Communication.getRoleCount(rc, Role.Builder);
        int healerRoleCount = Communication.getRoleCount(rc, Role.Healer);
        if (rc.getID() == 11144)
        {
            System.out.println("Normal: " + normalRoleCount +
                    " Attackers: " + attackerRoleCount +
                    " Defenders: " + defenderRoleCount +
                    " Builders: " + builderRoleCount +
                    " Healers: " + healerRoleCount);
        }

        boolean canSwitchRole;
        switch (currentRole)
        {
            case Normal:
                canSwitchRole = true;
                break;
            case Attacker:
                canSwitchRole = (attackerRoleCount > MIN_ATTACKERS);
                break;
            case Builder:
                canSwitchRole = (builderRoleCount > MIN_BUILDERS);
                break;
            case Healer:
                canSwitchRole = (healerRoleCount > MIN_HEALERS);
                break;
            case Defender:
                canSwitchRole = (defenderRoleCount > MIN_DEFENDERS);
                break;
            default:
                canSwitchRole = false;
        }

        //Only change role from a non-Normal role if global role counts are imbalanced
        /*
        if (currentRole != Role.Normal)
        {
            // Check if there are enough defenders
            if (defenderRoleCount < MIN_DEFENDERS && currentRole != Role.Builder)
            {
                changeRole(rc, Role.Defender);
            }
            //Offense won't be strong enough unless we have enough attackers
            else if (healerRoleCount >= attackerRoleCount)
            {
                changeRole(rc, Role.Attacker);
            }
            else if (builderRoleCount < MIN_BUILDERS)
            {
                changeRole(rc, Role.Builder);
            }
        }

         */
        //else
        //{
            if (rc.getRoundNum() > 150 && canSwitchRole)
            {
                if (defenderRoleCount < MIN_DEFENDERS)
                {
                    changeRole(rc, Role.Defender);
                }
                else if (attackerRoleCount < MIN_ATTACKERS)
                {
                    changeRole(rc, Role.Attacker);
                }
                else if (healerRoleCount < MIN_HEALERS)
                {
                    changeRole(rc, Role.Healer);
                }
                else if (builderRoleCount < MIN_BUILDERS)
                {
                    changeRole(rc, Role.Builder);
                }
                else
                {
                    changeRole(rc, Role.Attacker);
                }
            }

        //}
        return currentRole;
    }
}

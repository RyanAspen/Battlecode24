package v4_POST;

import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

public class Combat {

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
}

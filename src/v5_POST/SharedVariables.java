package v5_POST;

import battlecode.common.GameConstants;
import battlecode.common.RobotController;

import java.util.Random;

public class SharedVariables {
    public static Random rng = null;
    public static RobotController rc = null;
    public static FlagStatus[] statuses = null;
    public static FlagHolder[] friendlyFlagCarrierLocs = null;
    public static int standardID = -1;
    static final int numAttackers = 50;
    static final int numDefenders = GameConstants.ROBOT_CAPACITY - numAttackers;
    static final int numBuilders = 5;
}

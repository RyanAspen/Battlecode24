package v14;

import java.util.Random;

enum Role {
    Attacker,
    Defender
}
public class SharedVariables
{
    public static int desiredDefenders = Constants.MIN_DEFENDERS_BALANCED;
    public static int desiredAttackers = Constants.MIN_ATTACKERS_BALANCED;
    public static Role currentRole = null;
    public static Random rng = null;
}

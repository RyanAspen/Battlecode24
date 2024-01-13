package v13;

import java.util.Random;

enum Role {
    Normal,
    Attacker,
    Defender
}
public class SharedVariables
{
    public static int desiredDefenders = Constants.MIN_DEFENDERS_BALANCED;
    public static int desiredAttackers = Constants.MIN_ATTACKERS_BALANCED;
    public static Role currentRole = Role.Normal;
    public static Random rng = null;
}

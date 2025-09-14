package materiallib.api.misc;

import net.minecraftforge.oredict.OreDictionary;

public class MaterialLibValues {


    /**
     * This is worth exactly one normal Item. This Constant can be divided by many commonly used Numbers such as 1, 2,
     * 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 20, 21, 24, ... 64 or 81 without losing precision and is for that
     * reason used as Unit of Amount. But it is also small enough to be multiplied with larger Numbers.
     * <p/>
     * This is used to determine the amount of Material contained inside a prefixed Ore. For example Nugget = M / 9 as
     * it contains out of 1/9 of an Ingot.
     */
    public static final long M = 3628800;

    public static final int INGOTS = 144;
    public static final int HALF_INGOTS = INGOTS / 2;
    public static final int QUARTER_INGOTS = INGOTS / 4;
    public static final int EIGHTH_INGOTS = INGOTS / 8;
    public static final int NUGGETS = INGOTS / 9;
    public static final int STACKS = 64 * INGOTS;

    public static final short WILDCARD = OreDictionary.WILDCARD_VALUE;
}

package materiallib.api.enums;

import materiallib.api.misc.MaterialLibValues;

public enum FluidState {

    GAS(1000),
    LIQUID(1000),
    MOLTEN(144),
    PLASMA(1000),
    SLURRY(1000);

    public static final FluidState[] VALID_STATES = new FluidState[] { SLURRY, LIQUID, GAS, PLASMA, MOLTEN };

    public final int liters;
    public final long materialAmount;

    FluidState(int liters) {
        this.liters = liters;
        this.materialAmount = liters * MaterialLibValues.M / MaterialLibValues.INGOTS;
    }
}

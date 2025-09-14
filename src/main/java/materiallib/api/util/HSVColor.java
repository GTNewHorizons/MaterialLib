package materiallib.api.util;

public class HSVColor implements ImmutableColor {

    public float hue, saturation, brightness;

    public HSVColor() { }

    public HSVColor(float hue, float saturation, float brightness) {
        this.hue = hue;
        this.saturation = saturation;
        this.brightness = brightness;
    }

    public Color toRGB() {
        return new Color(getRed(), getGreen(), getBlue());
    }

    @Override
    public int getRed() {
        if (saturation == 0.0F) {
            return (byte)((int)(brightness * 255.0F + 0.5F));
        } else {
            float f3 = (hue - (float) Math.floor(hue)) * 6.0F;
            float f4 = f3 - (float) Math.floor(f3);
            float f5 = brightness * (1.0F - saturation);
            float f6 = brightness * (1.0F - saturation * f4);
            float f7 = brightness * (1.0F - saturation * (1.0F - f4));
            switch ((int) f3) {
                case 0, 5 -> {
                    return (byte) ((int) (brightness * 255.0F + 0.5F));
                }
                case 1 -> {
                    return (byte) ((int) (f6 * 255.0F + 0.5F));
                }
                case 2, 3 -> {
                    return (byte) ((int) (f5 * 255.0F + 0.5F));
                }
                case 4 -> {
                    return (byte) ((int) (f7 * 255.0F + 0.5F));
                }
                default -> throw new IllegalStateException();
            }
        }
    }

    @Override
    public int getGreen() {
        if (saturation == 0.0F) {
            return (byte)((int)(brightness * 255.0F + 0.5F));
        } else {
            float f3 = (hue - (float) Math.floor(hue)) * 6.0F;
            float f4 = f3 - (float) Math.floor(f3);
            float f5 = brightness * (1.0F - saturation);
            float f6 = brightness * (1.0F - saturation * f4);
            float f7 = brightness * (1.0F - saturation * (1.0F - f4));
            switch ((int) f3) {
                case 0 -> {
                    return (byte) ((int) (f7 * 255.0F + 0.5F));
                }
                case 1, 2 -> {
                    return (byte) ((int) (brightness * 255.0F + 0.5F));
                }
                case 3 -> {
                    return (byte) ((int) (f6 * 255.0F + 0.5F));
                }
                case 4, 5 -> {
                    return (byte) ((int) (f5 * 255.0F + 0.5F));
                }
                default -> throw new IllegalStateException();
            }
        }
    }

    @Override
    public int getBlue() {
        if (saturation == 0.0F) {
            return (byte)((int)(brightness * 255.0F + 0.5F));
        } else {
            float f3 = (hue - (float) Math.floor(hue)) * 6.0F;
            float f4 = f3 - (float) Math.floor(f3);
            float f5 = brightness * (1.0F - saturation);
            float f6 = brightness * (1.0F - saturation * f4);
            float f7 = brightness * (1.0F - saturation * (1.0F - f4));
            switch ((int) f3) {
                case 0, 1 -> {
                    return (byte) ((int) (f5 * 255.0F + 0.5F));
                }
                case 2 -> {
                    return (byte) ((int) (f7 * 255.0F + 0.5F));
                }
                case 3, 4 -> {
                    return (byte) ((int) (brightness * 255.0F + 0.5F));
                }
                case 5 -> {
                    return (byte) ((int) (f6 * 255.0F + 0.5F));
                }
                default -> throw new IllegalStateException();
            }
        }
    }

    @Override
    public int getAlpha() {
        return 255;
    }
}

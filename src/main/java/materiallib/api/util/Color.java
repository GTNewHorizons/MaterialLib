package materiallib.api.util;

public class Color implements ImmutableColor {

    public static final ImmutableColor WHITE = new Color(255, 255, 255, 255);

    public int red, green, blue, alpha;

    public Color() { }

    public Color(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = 255;
    }

    public Color(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
    }

    public Color set(int red, int green, int blue) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = 255;
        return this;
    }

    public Color set(int red, int green, int blue, int alpha) {
        this.red = red;
        this.green = green;
        this.blue = blue;
        this.alpha = alpha;
        return this;
    }

    @Override
    public int getRed() {
        return red;
    }

    @Override
    public int getGreen() {
        return green;
    }

    @Override
    public int getBlue() {
        return blue;
    }

    @Override
    public int getAlpha() {
        return alpha;
    }

    public static Color fromRGB(int value) {
        return new Color((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, 255);
    }

    public static Color fromARGB(int value) {
        return new Color((value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF, (value >> 24) & 0xFF);
    }

    public static Color fromRGBA(int value) {
        return new Color((value >> 24) & 0xFF, (value >> 16) & 0xFF, (value >> 8) & 0xFF, value & 0xFF);
    }
}

package materiallib.api.util;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public interface ImmutableColor {

    int getRed();
    int getGreen();
    int getBlue();
    int getAlpha();

    default int toIntRGB() {
        return ((getRed() & 0xFF) << 16) | ((getGreen() & 0xFF) << 8) | (getBlue() & 0xFF);
    }

    default int toIntARGB() {
        return ((getAlpha() & 0xFF) << 24) | ((getRed() & 0xFF) << 16) | ((getGreen() & 0xFF) << 8) | (getBlue() & 0xFF);
    }

    default int toIntRGBA() {
        return  ((getRed() & 0xFF) << 24) | ((getGreen() & 0xFF) << 16) | ((getBlue() & 0xFF) << 8) | ((getAlpha() & 0xFF));
    }

    default Color toMutable() {
        return new Color(getRed(), getGreen(), getBlue(), getAlpha());
    }

    default HSVColor toHSV() {
        int r = this.getRed();
        int g = this.getGreen();
        int b = this.getBlue();

        int l = Math.max(r, g);
        if (b > l) {
            l = b;
        }

        int i1 = Math.min(r, g);
        if (b < i1) {
            i1 = b;
        }

        float brightness = (float)l / 255.0F;
        float saturation;
        if (l != 0) {
            saturation = (float)(l - i1) / (float)l;
        } else {
            saturation = 0.0F;
        }

        float hue;
        if (saturation == 0.0F) {
            hue = 0.0F;
        } else {
            float f3 = (float)(l - r) / (float)(l - i1);
            float f4 = (float)(l - g) / (float)(l - i1);
            float f5 = (float)(l - b) / (float)(l - i1);
            if (r == l) {
                hue = f5 - f4;
            } else if (g == l) {
                hue = 2.0F + f3 - f5;
            } else {
                hue = 4.0F + f4 - f3;
            }

            hue /= 6.0F;
            if (hue < 0.0F) {
                ++hue;
            }
        }

        return new HSVColor(hue, saturation, brightness);
    }

    @SideOnly(Side.CLIENT)
    default void makeActive() {
        GL11.glColor4f(getRed() / 256f, getGreen() / 256f, getBlue() / 256f, getAlpha() / 256f);
    }

    default short[] toShorts() {
        return new short[] { (short) getRed(), (short) getGreen(), (short) getBlue(), (short) getAlpha() };
    }

    static HSVColor lerp(ImmutableColor a, ImmutableColor b, float k) {
        HSVColor hsvA = a instanceof HSVColor hsv ? hsv : a.toHSV();
        HSVColor hsvB = b instanceof HSVColor hsv ? hsv : b.toHSV();

        return new HSVColor(
            DataUtils.lerp(hsvA.hue, hsvB.hue, k),
            DataUtils.lerp(hsvA.saturation, hsvB.saturation, k),
            DataUtils.lerp(hsvA.brightness, hsvB.brightness, k));
    }
}

package org.exthmui.theme.models;

public class ThemeAccent extends ThemeBase {

    private int mAccentColor;

    public ThemeAccent(String packageName) {
        super(packageName);
    }

    public void setAccentColor(int color) {
        mAccentColor = color;
    }

    public int getAccentColor() {
        return mAccentColor;
    }
}

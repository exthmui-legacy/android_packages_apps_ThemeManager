package org.exthmui.theme.models;

public class ThemeAccent extends ThemeBase {

    private int mAccentColor;
    private OverlayTarget mOverlayTarget;

    public ThemeAccent(String packageName) {
        super(packageName);
    }

    public void setAccentColor(int color) {
        mAccentColor = color;
    }

    public void setOverlayTarget(OverlayTarget overlayTarget) {
        mOverlayTarget = overlayTarget;
    }

    public int getAccentColor() {
        return mAccentColor;
    }

    public OverlayTarget getOverlayTarget() {
        return mOverlayTarget;
    }
}

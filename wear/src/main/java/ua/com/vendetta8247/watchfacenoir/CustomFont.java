package ua.com.vendetta8247.watchfacenoir;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;

public class CustomFont {
    public static final CustomFont  ASPERGIT_BOLD    = new CustomFont("aspergit_bold.otf");
    public static final CustomFont  BSTYLE_REGULAR = new CustomFont("bstyle.regular.otf");
    public static final CustomFont  BOBLIC_REGULAR = new CustomFont("boblic.ttf");
    private final String      assetName;
    private volatile Typeface typeface;

    public  CustomFont(String assetName) {
        this.assetName = assetName;
    }

    public void apply(Context context, Paint paint) {
        synchronized (this) {
                typeface = Typeface.createFromAsset(context.getAssets(), assetName);
                paint.setTypeface(typeface);

        }
    }
}
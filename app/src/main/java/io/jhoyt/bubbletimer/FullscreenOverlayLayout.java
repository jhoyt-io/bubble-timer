package io.jhoyt.bubbletimer;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.WindowManager;
import android.widget.RelativeLayout;

public class FullscreenOverlayLayout extends RelativeLayout {
    static final Point newPoint = new Point();

    public FullscreenOverlayLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        WindowManager windowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        Point screenSize = newPoint;
        windowManager.getDefaultDisplay().getRealSize(screenSize);

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(screenSize.x, MeasureSpec.getMode(widthMeasureSpec)),
                MeasureSpec.makeMeasureSpec(screenSize.y, MeasureSpec.getMode(heightMeasureSpec)));
    }
}

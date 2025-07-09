package org.telegram.ui.Components;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.DisplayMetrics;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ProfileActivity;

import java.util.Arrays;
import java.util.function.Function;

public class HeaderTransitionEffect {
    private final Paint blurCompositePaint = new Paint(Paint.DITHER_FLAG | Paint.ANTI_ALIAS_FLAG);
    private final Paint colorAdjustmentPaint = new Paint();
    private final Paint gradientEffectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect visibleArea = new Rect();
    private final Bitmap[] cachedBlurImages = new Bitmap[1];
    private final Canvas[] blurDrawingSurfaces = new Canvas[cachedBlurImages.length];
    private final View targetView;
    private float imageScale;
    private boolean contentReady;
    private boolean isBlurActive;
    private float transitionStateProgress = 0f;


    protected float getBottomOffset() {
        return targetView.getHeight() - getExtraHeight() * transitionStateProgress;
    }

    protected float getScaleFactor() {
        return 1f;
    }

    public HeaderTransitionEffect(View v) {
        targetView = v;
        colorAdjustmentPaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix()));
    }

    public void updateDimensions(int w, int h) {
        imageScale = 20f / getScaleFactor();
        contentReady = false;

        int dw = (int) Math.ceil(w / imageScale), dh = (int) Math.ceil(getExtraHeight() / imageScale);
        if (cachedBlurImages[0] != null && cachedBlurImages[0].getWidth() >= dw && cachedBlurImages[0].getHeight() >= dh) {
            return;
        }
        if (w == 0 || h == 0) return;
        for (Bitmap bm : cachedBlurImages) {
            if (bm != null) {
                bm.recycle();
            }
        }

        cachedBlurImages[0] = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
        blurDrawingSurfaces[0] = new Canvas(cachedBlurImages[0]);
        gradientEffectPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        gradientEffectPaint.setShader(new LinearGradient(w / 2f, AndroidUtilities.dp(32) / getScaleFactor(), w / 2f, 0, new int[]{0x00FFFFFF, 0xFFFFFFFF}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        targetView.invalidate();
    }

    public void setBlurActive(boolean enabled) {
        this.isBlurActive = enabled;
        targetView.invalidate();
    }

    public void updateTransitionState(float transitionStateProgress) {
        this.transitionStateProgress = transitionStateProgress;
        updateDimensions(targetView.getWidth(), targetView.getHeight());
        targetView.invalidate();
    }

    public void resetContent() {
        contentReady = false;
    }

    public void renderContent(Canvas canvas, Function<Canvas, Void> drawBlock) {
        if (!isBlurActive) {
            drawBlock.apply(canvas);
            return;
        }
        float verticalOffset = getBottomOffset();
        if (cachedBlurImages[0] == null) {
            updateDimensions(targetView.getWidth(), targetView.getHeight());
        }

        if (!contentReady) {
            for (Bitmap img : cachedBlurImages) {
                img.eraseColor(0);
            }
        }
        if (!contentReady) {
            blurDrawingSurfaces[0].save();
            blurDrawingSurfaces[0].scale(1f / imageScale, 1f / imageScale, 0, 0);
            blurDrawingSurfaces[0].translate(0, getExtraHeight() * transitionStateProgress - verticalOffset);
            drawBlock.apply(blurDrawingSurfaces[0]);
            blurDrawingSurfaces[0].restore();
            float blurRadius = 20;
            Utilities.stackBlurBitmap(cachedBlurImages[0], (int) (blurRadius / 8));

            DisplayMetrics metrics = targetView.getResources().getDisplayMetrics();
            targetView.getLocalVisibleRect(visibleArea);
            if (visibleArea.right > 0 && visibleArea.left < metrics.widthPixels) {
                contentReady = true;
            }
        }

        AndroidUtilities.rectTmp.set(0, verticalOffset, targetView.getWidth(), targetView.getHeight());
        canvas.saveLayer(AndroidUtilities.rectTmp, colorAdjustmentPaint, Canvas.ALL_SAVE_FLAG);
        canvas.translate(0, verticalOffset);
        canvas.scale(imageScale, imageScale, 0, 0);
        canvas.drawBitmap(cachedBlurImages[0], 0, 0, blurCompositePaint);
        canvas.restore();

        float compositeTop = verticalOffset + AndroidUtilities.dp(32) / getScaleFactor() - 1;
        AndroidUtilities.rectTmp.set(
                0,
                0,
                targetView.getWidth(),
                AndroidUtilities.lerp(compositeTop, targetView.getHeight(), 1f - transitionStateProgress));
        canvas.saveLayer(AndroidUtilities.rectTmp, null, Canvas.ALL_SAVE_FLAG);
        drawBlock.apply(canvas);
        float gradientOffset = AndroidUtilities.dp(32) / getScaleFactor() * (1f - transitionStateProgress);
        canvas.translate(0, verticalOffset + gradientOffset);
        canvas.drawRect(0, 0, targetView.getWidth(), AndroidUtilities.dp(32) / getScaleFactor() + 1, gradientEffectPaint);
        canvas.restore();

    }

    public void releaseResources() {
        for (Bitmap bm : cachedBlurImages) {
            if (bm != null) {
                bm.recycle();
            }
        }
        Arrays.fill(cachedBlurImages, null);
        Arrays.fill(blurDrawingSurfaces, null);
        contentReady = false;
    }

    private int getExtraHeight() {
        return (int) (AndroidUtilities.dp(32 + ProfileActivity.PROFILE_ACTIONS_HEIGHT + ProfileActivity.BUTTONS_SPACING * 2) / getScaleFactor());
    }

}

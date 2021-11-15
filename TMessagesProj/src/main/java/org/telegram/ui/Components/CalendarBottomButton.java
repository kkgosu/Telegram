package org.telegram.ui.Components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.View;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

public class CalendarBottomButton extends View {

    private final Theme.ResourcesProvider resourceProvider;
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int rippleColor;

    private StaticLayout textLayout;
    private StaticLayout textLayoutOut;
    private int layoutTextWidth;
    private final TextPaint layoutPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private Drawable selectableBackground;

    private ValueAnimator replaceAnimator;
    private float replaceProgress = 1f;
    private boolean animatedFromBottom;
    private int textColor;
    private int panelBackgroundColor;
    private CharSequence lastText;
    private boolean isEnabled = true;
    private int lastTextColor;

    public CalendarBottomButton(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourceProvider = resourcesProvider;
        textPaint.setTextSize(AndroidUtilities.dp(13));
        textPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        layoutPaint.setTextSize(AndroidUtilities.dp(15));
        layoutPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
    }

    public void setText(CharSequence text, boolean animatedFromBottom, String colorKey) {
        if (lastText == text) {
            return;
        }
        textColor = getThemedColor(colorKey);
        lastTextColor = textColor;
        lastText = text;
        this.animatedFromBottom = animatedFromBottom;
        textLayoutOut = textLayout;
        layoutTextWidth = (int) Math.ceil(layoutPaint.measureText(text, 0, text.length()));
        textLayout = new StaticLayout(text, layoutPaint, layoutTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        setContentDescription(text);
        invalidate();

        if (textLayoutOut != null) {
            if (replaceAnimator != null) {
                replaceAnimator.cancel();
            }
            replaceProgress = 0;
            replaceAnimator = ValueAnimator.ofFloat(0, 1f);
            replaceAnimator.addUpdateListener(animation -> {
                replaceProgress = (float) animation.getAnimatedValue();
                invalidate();
            });
            replaceAnimator.setDuration(150);
            replaceAnimator.start();
        }
    }

    public void setText(CharSequence text, String colorKey) {
        textColor = getThemedColor(colorKey);
        lastTextColor = textColor;
        layoutTextWidth = (int) Math.ceil(layoutPaint.measureText(text, 0, text.length()));
        textLayout = new StaticLayout(text, layoutPaint, layoutTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, true);
        setContentDescription(text);
        invalidate();
    }

    public void setEnabled(boolean isEnabled) {
        this.isEnabled = isEnabled;
        if (isEnabled) {
            textColor = ColorUtils.setAlphaComponent(lastTextColor, 255);
        } else {
            textColor = ColorUtils.setAlphaComponent(textColor, (int) (255 * 0.4f));
        }
        selectableBackground = null;
        invalidate();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (selectableBackground != null) {
            selectableBackground.setState(getDrawableState());
        }
    }

    @Override
    public boolean verifyDrawable(Drawable drawable) {
        if (selectableBackground != null) {
            return selectableBackground == drawable || super.verifyDrawable(drawable);
        }
        return super.verifyDrawable(drawable);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (selectableBackground != null) {
            selectableBackground.jumpToCurrentState();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (textLayout != null) {
                int contentWidth;
                contentWidth = getMeasuredWidth() - AndroidUtilities.dp(96);
                int x = (getMeasuredWidth() - contentWidth) / 2;
                rect.set(
                        x, getMeasuredHeight() / 2f - contentWidth / 2f,
                        x + contentWidth, getMeasuredHeight() / 2f + contentWidth / 2f
                );
                if (!rect.contains(event.getX(), event.getY())) {
                    setPressed(false);
                    return false;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int color = textColor == 0 ? getThemedColor(Theme.key_chat_fieldOverlayTextWarning) : textColor;
        layoutPaint.setColor(textColor = color);
        color = getThemedColor(Theme.key_chat_messagePanelBackground);
        if (panelBackgroundColor != color) {
            textPaint.setColor(panelBackgroundColor = color);
        }
        if (getParent() != null) {
            if ((rippleColor != textColor || selectableBackground == null) && isEnabled) {
                selectableBackground = Theme.createSimpleSelectorRoundRectDrawable(AndroidUtilities.dp(0), 0, ColorUtils
                        .setAlphaComponent(rippleColor = textColor, (int) (255 * 0.33f)));
                selectableBackground.setCallback(this);
            }
            if (selectableBackground != null) {
                selectableBackground.setBounds(
                        AndroidUtilities.dp(8), AndroidUtilities.dp(4),
                        getMeasuredWidth() - AndroidUtilities.dp(8), getMeasuredHeight() - AndroidUtilities.dp(4)
                );
                selectableBackground.draw(canvas);
            }
        }
        if (textLayout != null) {
            canvas.save();
            if (replaceProgress != 1f && textLayoutOut != null) {
                int oldAlpha = layoutPaint.getAlpha();

                canvas.save();
                canvas.translate((getMeasuredWidth() - textLayoutOut.getWidth()) / 2,
                        (getMeasuredHeight() - textLayout.getHeight()) / 2);
                canvas.translate(0, (animatedFromBottom ? -1f : 1f) * AndroidUtilities.dp(18) * replaceProgress);
                layoutPaint.setAlpha((int) (oldAlpha * (1f - replaceProgress)));
                textLayoutOut.draw(canvas);
                canvas.restore();

                canvas.save();
                canvas.translate((getMeasuredWidth() - layoutTextWidth) / 2, (getMeasuredHeight() - textLayout.getHeight()) / 2);
                canvas.translate(0, (animatedFromBottom ? 1f : -1f) * AndroidUtilities.dp(18) * (1f - replaceProgress));
                layoutPaint.setAlpha((int) (oldAlpha * (replaceProgress)));
                textLayout.draw(canvas);
                canvas.restore();

                layoutPaint.setAlpha(oldAlpha);
            } else {
                canvas.translate((getMeasuredWidth() - layoutTextWidth) / 2, (getMeasuredHeight() - textLayout.getHeight()) / 2);
                textLayout.draw(canvas);
            }

            canvas.restore();
        }
    }

    private int getThemedColor(String key) {
        Integer color = resourceProvider != null ? resourceProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }
}

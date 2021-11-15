package org.telegram.ui.Components;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.View;

import androidx.annotation.Keep;

import org.telegram.messenger.AndroidUtilities;

import java.util.ArrayList;
import java.util.Locale;

public class TitleNumberTextView extends View {

    private final ArrayList<StaticLayout> letters = new ArrayList<>();
    private final ArrayList<StaticLayout> oldLetters = new ArrayList<>();
    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private ObjectAnimator animator;
    private float progress = 0.0f;
    private int currentNumber = 0;
    private boolean addNumber;
    private boolean center;
    private float textWidth;
    private float oldTextWidth;
    private final String titleTextZero;
    private String oldText = "";

    public TitleNumberTextView(Context context, String titleTextZero) {
        super(context);
        this.titleTextZero = titleTextZero;
    }

    @Keep
    public void setProgress(float value) {
        if (progress == value) {
            return;
        }
        progress = value;
        invalidate();
    }

    @Keep
    public float getProgress() {
        return progress;
    }

    public void setAddNumber() {
        addNumber = true;
    }

    public void setNumber(int number, String titleText, boolean animated) {
        if (number == currentNumber && number != 0) {
            return;
        }
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        if (number == 0) {
            letters.clear();
            letters.add(
                    new StaticLayout(titleTextZero, textPaint, (int) Math.ceil(textPaint.measureText(titleTextZero)), Layout.Alignment.ALIGN_NORMAL,
                            1.0f,
                            0.0f, false));
            currentNumber = 0;
            invalidate();
            return;
        }
        if (number == 1) {
            letters.clear();
            letters.add(new StaticLayout("", textPaint, 0, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false));
        }
        oldLetters.addAll(letters);
        letters.clear();
        String newText = titleText + " ";
        boolean forwardAnimation;
        if (addNumber) {
            forwardAnimation = number < currentNumber;
        } else {
            forwardAnimation = number > currentNumber;
        }
        boolean replace = false;
        if (center) {
            textWidth = textPaint.measureText(newText);
            oldTextWidth = textPaint.measureText(oldText);
            if (textWidth != oldTextWidth) {
                replace = true;
            }
        }

        currentNumber = number;
        progress = 0;
        for (int a = 0; a < newText.length(); a++) {
            String ch = newText.substring(a, a + 1);
            String oldCh = !oldLetters.isEmpty() && a + 1 < oldLetters.size() ? oldText.substring(a, a + 1) : null;
            if (!replace && oldCh != null && oldCh.equals(ch)) {
                letters.add(oldLetters.get(a));
                oldLetters.set(a, null);
            } else {
                if (replace && oldCh == null) {
                    oldLetters.add(new StaticLayout("", textPaint, 0, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false));
                }
                StaticLayout layout = new StaticLayout(ch, textPaint, (int) Math.ceil(textPaint.measureText(ch)), Layout.Alignment.ALIGN_NORMAL, 1.0f,
                        0.0f, false);
                letters.add(layout);
            }
        }
        if (animated && !oldLetters.isEmpty()) {
            animator = ObjectAnimator.ofFloat(this, "progress", forwardAnimation ? -1 : 1, 0);
            animator.setDuration(addNumber ? 180 : 150);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animator = null;
                    oldLetters.clear();
                }
            });
            animator.start();
        }
        oldText = titleText;
        invalidate();
    }

    public void setTextSize(int size) {
        textPaint.setTextSize(AndroidUtilities.dp(size));
        oldLetters.clear();
        letters.clear();
        setNumber(currentNumber, "", false);
    }

    public void setTextColor(int value) {
        textPaint.setColor(value);
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
        oldLetters.clear();
        letters.clear();
        setNumber(currentNumber, "", false);
    }

    public void setCenterAlign(boolean center) {
        this.center = center;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (letters.isEmpty()) {
            return;
        }
        float height = letters.get(0).getHeight();
        float translationHeight = addNumber ? AndroidUtilities.dp(4) : height;

        float x = 0;
        float oldDx = 0;
        if (center) {
            x = (getMeasuredWidth() - textWidth) / 2f;
            oldDx = (getMeasuredWidth() - oldTextWidth) / 2f - x;
        }
        canvas.save();
        canvas.translate(getPaddingLeft() + x, (getMeasuredHeight() - height) / 2);
        int count = Math.max(letters.size(), oldLetters.size());
        for (int a = 0; a < count; a++) {
            canvas.save();
            StaticLayout old = a < oldLetters.size() ? oldLetters.get(a) : null;
            StaticLayout layout = a < letters.size() ? letters.get(a) : null;
            if (progress > 0) {
                if (old != null) {
                    textPaint.setAlpha((int) (255 * progress));
                    canvas.save();
                    canvas.translate(oldDx, (progress - 1.0f) * translationHeight);
                    old.draw(canvas);
                    canvas.restore();
                    if (layout != null) {
                        textPaint.setAlpha((int) (255 * (1.0f - progress)));
                        canvas.translate(0, progress * translationHeight);
                    }
                } else {
                    textPaint.setAlpha(255);
                }
            } else if (progress < 0) {
                if (old != null) {
                    textPaint.setAlpha((int) (255 * -progress));
                    canvas.save();
                    canvas.translate(oldDx, (1.0f + progress) * translationHeight);
                    old.draw(canvas);
                    canvas.restore();
                }
                if (layout != null) {
                    if (a == count - 1 || old != null) {
                        textPaint.setAlpha((int) (255 * (1.0f + progress)));
                        canvas.translate(0, progress * translationHeight);
                    } else {
                        textPaint.setAlpha(255);
                    }
                }
            } else if (layout != null) {
                textPaint.setAlpha(255);
            }
            if (layout != null) {
                layout.draw(canvas);
            }
            canvas.restore();
            canvas.translate(layout != null ? layout.getLineWidth(0) : old.getLineWidth(0) + AndroidUtilities.dp(1), 0);
            if (layout != null && old != null) {
                oldDx += old.getLineWidth(0) - layout.getLineWidth(0);
            }
        }
        canvas.restore();
    }
}

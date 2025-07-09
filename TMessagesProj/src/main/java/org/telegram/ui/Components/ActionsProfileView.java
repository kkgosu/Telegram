package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;
import java.util.List;

public class ActionsProfileView extends FrameLayout {
    private final static int SPACING_DP = 8;
    private final List<ProfileButton> primaryActions = new ArrayList<>(4);
    private final List<ProfileButton> secondaryActions = new ArrayList<>();

    private float actionWidth;
    private final Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backgroundPaint = new Paint();
    private final Path shapePath = new Path();

    private OnProfileButtonClickListener onClickListener;
    private boolean blurEnabled;
    private float darkThemeFactor = -1;
    private float expansionProgress;
    private final int lightAlpha = 67;
    private final int darkAlpha = 33;

    public ActionsProfileView(Context context) {
        super(context);
        setWillNotDraw(false);
        backgroundPaint.setColor(Color.argb(lightAlpha, 0, 0, 0));
        setDarkThemeFactor(0f);
    }

    public void setExpansionProgress(float expansionProgress) {
        if (this.expansionProgress == expansionProgress) {
            return;
        }
        this.expansionProgress = expansionProgress;
        backgroundPaint.setColor(Color.argb((int) (lightAlpha * expansionProgress), 0, 0, 0));
        invalidate();
    }

    public void setDarkThemeFactor(float progress) {
        if (darkThemeFactor == progress) {
            return;
        }
        darkThemeFactor = progress;
        highlightPaint.setColor(
                ColorUtils.blendARGB(
                        Theme.getColor(Theme.key_listSelector),
                        Color.argb(darkAlpha, 0, 0, 0),
                        progress));
        invalidate();
    }

    public void setBlurEnabled(boolean enabled) {
        if (blurEnabled == enabled) {
            return;
        }
        blurEnabled = enabled;
        invalidate();
    }

    public void setButtonClickListener(OnProfileButtonClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            ProfileButton btn = ((ProfileActionButton) child).actionButton;
            int index = primaryActions.indexOf(btn);
            if (index >= 0) {
                float leftPos = getPaddingLeft() + (actionWidth + AndroidUtilities.dp(SPACING_DP)) * index;
                child.layout((int) leftPos, 0, (int) (leftPos + actionWidth), getMeasuredHeight());
            } else {
                removeViewAt(i);
                i--;
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int currentWidth = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(currentWidth, MeasureSpec.getSize(heightMeasureSpec));

        if (!primaryActions.isEmpty()) {
            int padding = getPaddingLeft() + getPaddingRight();
            int spacing = AndroidUtilities.dp(SPACING_DP) * (primaryActions.size() - 1);
            actionWidth = (float) (currentWidth - padding - spacing) / primaryActions.size();
        }
        int width = (int) (currentWidth * actionWidth);
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(
                    MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            );
        }
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        shapePath.rewind();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            AndroidUtilities.rectTmp.set(child.getLeft(), child.getTop() + child.getHeight() * (1f - child.getScaleY()), child.getRight(), child.getBottom());
            shapePath.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(12), AndroidUtilities.dp(12), Path.Direction.CW);
        }

        if (true) {
            canvas.drawPath(shapePath, backgroundPaint);
        }
        canvas.drawPath(shapePath, highlightPaint);
    }

    public void setExpandProgress(float factor) {
        setAlpha((MathUtils.clamp(factor, 0.1f, 0.4f) - 0.2f) / 0.2f);
        float scale = (MathUtils.clamp(factor, 0.1f, 0.4f) - 0.1f) / 0.3f;
        for (int i = 0; i < getChildCount(); i++) {
            ProfileActionButton btn = (ProfileActionButton) getChildAt(i);
            btn.setScaleX(AndroidUtilities.lerp(0.2f, 1f, scale));
            btn.setScaleY(AndroidUtilities.lerp(0.2f, 1f, scale));
            btn.setPivotX(btn.getWidth() / 2f);
            btn.setPivotY(btn.getHeight());
        }
        invalidate();
    }

    public View getButtonView(ProfileButton btn) {
        for (int i = 0; i < getChildCount(); i++) {
            ProfileActionButton buttonView = (ProfileActionButton) getChildAt(i);
            if (buttonView.actionButton == btn) {
                return buttonView;
            }
        }
        return this;
    }

    public void updateActions(List<ProfileButton> buttons) {
        primaryActions.clear();
        secondaryActions.clear();

        int maxVisible = buttons.contains(ProfileButton.REPORT) ? 3 : 4;
        for (ProfileButton btn : buttons) {
            if (primaryActions.size() >= maxVisible) {
                secondaryActions.add(btn);
            } else {
                primaryActions.add(btn);
            }
        }
        if (buttons.contains(ProfileButton.REPORT)) {
            primaryActions.add(ProfileButton.REPORT);
        }
        if (!secondaryActions.isEmpty()) {
            reorganizeSpecialActions(buttons);
        }

        createActionButtons();
        requestLayout();
    }

    private void createActionButtons() {
        for (ProfileButton action : primaryActions) {
            if (!hasButtonForAction(action)) {
                addView(new ProfileActionButton(getContext()).bind(action, () -> {
                    if (onClickListener != null) {
                        onClickListener.onClick(action);
                    }
                }));
            }
        }
    }

    private boolean hasButtonForAction(ProfileButton action) {
        for (int i = 0; i < getChildCount(); i++) {
            if (action == ((ProfileActionButton) getChildAt(i)).actionButton) {
                return true;
            }
        }
        return false;
    }

    private void reorganizeSpecialActions(List<ProfileButton> actions) {
        if (secondaryActions.contains(ProfileButton.GIFT) &&
                actions.contains(ProfileButton.DISCUSS)) {
            secondaryActions.remove(ProfileButton.GIFT);
            actions.remove(ProfileButton.DISCUSS);

            secondaryActions.add(ProfileButton.DISCUSS);
            actions.add(ProfileButton.GIFT);
        }
    }

    public List<ProfileButton> getSecondaryActions() {
        return secondaryActions;
    }

    public interface OnProfileButtonClickListener {
        void onClick(ProfileButton button);
    }

    public enum ProfileButton {
        ADD_STORY(R.drawable.ic_story, R.string.AddStory),
        BLOCK(R.drawable.ic_block, R.string.BizBotStop),
        CALL(R.drawable.ic_call_new, R.string.Call),
        CHANGE_AVATAR(R.drawable.msg_filled_data_sent, R.string.ChangeAvatar),
        DISCUSS(R.drawable.ic_message, R.string.Discuss),
        GIFT(R.drawable.ic_gift, R.string.ActionStarGift),
        JOIN(R.drawable.ic_join, R.string.VoipChatJoin),
        LEAVE(R.drawable.ic_leave, R.string.VoipGroupLeave),
        LIVE_STREAM(R.drawable.ic_live_stream, R.string.StartVoipChannelTitle),
        MESSAGE(R.drawable.ic_message, R.string.TypeMessage),
        MUTE(R.drawable.ic_mute, R.string.Mute),
        QR_CODE(R.drawable.msg_qr_mini, R.string.QrCode),
        REPORT(R.drawable.ic_report, R.string.ReportChat),
        SHARE(R.drawable.ic_share, R.string.VoipChatShare),
        UNMUTE(R.drawable.ic_unmute, R.string.Unmute),
        VIDEO(R.drawable.ic_video_new, R.string.GroupCallCreateVideo);

        public final int icon, title;

        ProfileButton(int icon, int title) {
            this.icon = icon;
            this.title = title;
        }
    }

    private final static class ProfileActionButton extends LinearLayout {
        ImageView actionIcon;
        TextView actionLabel;
        ProfileButton actionButton;

        ProfileActionButton(Context context) {
            super(context);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER);

            actionIcon = new ImageView(context);
            actionIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            addView(actionIcon, LayoutHelper.createLinear(28, 28));

            actionLabel = new TextView(context);
            actionLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            actionLabel.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            actionLabel.setTextColor(Color.WHITE);
            addView(actionLabel, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));
        }

        ProfileActionButton bind(ProfileButton button, Runnable onClick) {
            this.actionButton = button;
            actionIcon.setImageResource(button.icon);
            actionLabel.setText(button.title);
            setBackground(Theme.AdaptiveRipple.createRect(0, Theme.getColor(Theme.key_listSelector), 16));
            setOnClickListener(v -> onClick.run());
            return this;
        }
    }
}

package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Property;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.ui.ProfileActivity;

public class ProfileAvatarImageView extends BackupImageView {
    private final RectF boundsRect = new RectF();
    private final Path clippingPath = new Path();
    private Paint backgroundPaint;
    public boolean drawAvatar = true;
    private boolean drawPreview = true;
    private boolean hasStories;
    private float expandAnimationProgress;
    private float transitionProgress;
    private float fadeEffectProgress;
    private float insetFactor = 1f;
    private float previewAlpha;
    private float roundCornerRadius;
    private float imageInset;
    private ImageReceiver transitionImageReceiver;
    private ImageReceiver previewImageReceiver;
    private ImageReceiver.BitmapHolder bitmapHolder;
    private ProfileGalleryView avatarsGallery;
    private final HeaderTransitionEffect blurHelper;

    public float bounceScale = 1f;

    public static Property<ProfileAvatarImageView, Float> TRANSITION_PROGRESS = new AnimationProperties.FloatProperty<ProfileAvatarImageView>("transitionProgress") {
        @Override
        public void setValue(ProfileAvatarImageView object, float value) {
            object.setTransitionProgress(value);
        }
        @Override
        public Float get(ProfileAvatarImageView object) {
            return object.transitionProgress;
        }
    };

    public ProfileAvatarImageView(Context context) {
        super(context);
        previewImageReceiver = new ImageReceiver(this);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(Color.BLACK);
        blurHelper = new HeaderTransitionEffect(this) {
            @Override
            protected float getScaleFactor() {
                return ((View) getParent()).getScaleY();
            }

            @Override
            protected float getBottomOffset() {
                return getMeasuredHeight() - AndroidUtilities.dp(ProfileActivity.PROFILE_ACTIONS_HEIGHT + ProfileActivity.BUTTONS_SPACING * 2 + 32) / getScaleFactor() * fadeEffectProgress;
            }
        };
    }

    @Override
    public void setScaleY(float scaleY) {
        super.setScaleY(scaleY);
        invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (avatarsGallery != null) {
            avatarsGallery.invalidate();
        }
    }

    @Override
    public void setRoundRadius(int value) {
        roundCornerRadius = value;
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        previewImageReceiver.onAttachedToWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();

        imageInset = hasStories ? (int) AndroidUtilities.dpf2(3.5f) : 0;
        imageInset *= (1f - expandAnimationProgress);
        imageInset *= insetFactor * (1f - previewAlpha);

        float parentScale = ((View) getParent()).getScaleY();
        canvas.scale(bounceScale, bounceScale, getMeasuredWidth() / 2f, getMeasuredHeight() / 2f);

        clippingPath.rewind();
        float heightOffset = parentScale != 0 ? AndroidUtilities.dp(ProfileActivity.PROFILE_ACTIONS_HEIGHT + ProfileActivity.BUTTONS_SPACING * 2 + 32) / parentScale * expandAnimationProgress : 0;
        AndroidUtilities.rectTmp.set(imageInset, imageInset, getMeasuredWidth() - imageInset, getMeasuredHeight() - imageInset + heightOffset);
        clippingPath.addRoundRect(AndroidUtilities.rectTmp, roundCornerRadius, roundCornerRadius, Path.Direction.CW);
        canvas.clipPath(clippingPath);
        blurHelper.renderContent(canvas, canvas1 -> {
            ImageReceiver imageReceiver1 = animatedEmojiDrawable != null ? animatedEmojiDrawable.getImageReceiver() : this.imageReceiver;
            float alpha = 1.0f;
            int height1 = (int) (getMeasuredHeight() - AndroidUtilities.dp(ProfileActivity.PROFILE_ACTIONS_HEIGHT + ProfileActivity.BUTTONS_SPACING * 2) * fadeEffectProgress / ((View) getParent()).getScaleY());
            if (transitionImageReceiver != null) {
                alpha *= 1.0f - transitionProgress;
                if (transitionProgress > 0.0f) {
                    final float fromAlpha = transitionProgress;
                    final float wasImageX = transitionImageReceiver.getImageX();
                    final float wasImageY = transitionImageReceiver.getImageY();
                    final float wasImageW = transitionImageReceiver.getImageWidth();
                    final float wasImageH = transitionImageReceiver.getImageHeight();
                    final float wasAlpha = transitionImageReceiver.getAlpha();
                    transitionImageReceiver.setImageCoords(imageInset, imageInset, getMeasuredWidth() - imageInset * 2f, height1 - imageInset * 2f);
                    transitionImageReceiver.setAlpha(fromAlpha);
                    transitionImageReceiver.draw(canvas1);
                    transitionImageReceiver.setImageCoords(wasImageX, wasImageY, wasImageW, wasImageH);
                    transitionImageReceiver.setAlpha(wasAlpha);
                }
            }
            if (imageReceiver1 != null && alpha > 0 && (previewAlpha < 1f || !drawPreview)) {
                imageReceiver1.setImageCoords(imageInset, imageInset, getMeasuredWidth() - imageInset * 2f, height1 - imageInset * 2f);
                final float wasAlpha = imageReceiver1.getAlpha();
                imageReceiver1.setAlpha(wasAlpha * alpha);
                if (drawAvatar) {
                    imageReceiver1.draw(canvas1);
                }
                imageReceiver1.setAlpha(wasAlpha);
            }
            if (previewAlpha > 0f && drawPreview && alpha > 0) {
                if (previewImageReceiver.getDrawable() != null) {
                    previewImageReceiver.setImageCoords(imageInset, imageInset, getMeasuredWidth() - imageInset * 2f, height1 - imageInset * 2f);
                    previewImageReceiver.setAlpha(alpha * previewAlpha);
                    previewImageReceiver.draw(canvas1);
                } else {
                    boundsRect.set(0f, 0f, getMeasuredWidth(), getMeasuredHeight());
                    backgroundPaint.setAlpha((int) (alpha * previewAlpha * 255f));
                    canvas1.drawRoundRect(boundsRect, roundCornerRadius, roundCornerRadius, backgroundPaint);
                }
            }
            return null;
            });
        canvas.restore();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        blurHelper.updateDimensions(w,h);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blurHelper.releaseResources();
        previewImageReceiver.onDetachedFromWindow();
        if (bitmapHolder != null) {
            bitmapHolder.release();
            bitmapHolder = null;
        }
    }

    public void setTransitionImageReceiver(ImageReceiver imageReceiver) {
        this.transitionImageReceiver = imageReceiver;
    }

    public void setAvatarsGallery(ProfileGalleryView avatarsGallery) {
        this.avatarsGallery = avatarsGallery;
    }

    public void setTransitionProgress(float transitionProgress) {
        this.transitionProgress = transitionProgress;
        invalidate();
    }

    public void setForegroundImage(ImageLocation imageLocation, String imageFilter, Drawable thumb) {
        previewImageReceiver.setImage(imageLocation, imageFilter, thumb, 0, null, null, 0);
        if (bitmapHolder != null) {
            bitmapHolder.release();
            bitmapHolder = null;
        }
    }

    public void setForegroundImageDrawable(ImageReceiver.BitmapHolder holder) {
        if (holder != null) {
            previewImageReceiver.setImageBitmap(holder.drawable);
        }
        if (bitmapHolder != null) {
            bitmapHolder.release();
        }
        bitmapHolder = holder;
    }

    public void setPreviewAlpha(float value) {
        previewAlpha = value;
        invalidate();
    }

    public void clearForeground() {
        AnimatedFileDrawable drawable = previewImageReceiver.getAnimation();
        if (drawable != null) {
            drawable.removeSecondParentView(this);
        }
        previewImageReceiver.clearImage();
        if (bitmapHolder != null) {
            bitmapHolder.release();
            bitmapHolder = null;
        }
        previewAlpha = 0f;
        invalidate();
    }

    public float getRadius() {
        return roundCornerRadius;
    }

    public void setProgressToStoriesInsets(float progressToInsets) {
        if (progressToInsets == this.insetFactor) {
            return;
        }
        this.insetFactor = progressToInsets;
        invalidate();
    }

    public void drawForeground(boolean drawForeground) {
        this.drawPreview = drawForeground;
    }

    public boolean isHasStories() {
        return hasStories;
    }

    public void setHasStories(boolean hasStories) {
        if (this.hasStories == hasStories) {
            return;
        }
        this.hasStories = hasStories;
        invalidate();
    }

    public void setBlurEnabled(boolean enabled) {
        blurHelper.setBlurActive(enabled);
    }

    public void setFadeProgress(float fadeEffectProgress) {
        if (this.fadeEffectProgress == fadeEffectProgress) {
            return;
        }
        this.fadeEffectProgress = fadeEffectProgress;
        blurHelper.updateTransitionState(fadeEffectProgress);
    }

    public void setExpandProgress(float animatedFracture) {
        if (expandAnimationProgress == animatedFracture) {
            return;
        }
        expandAnimationProgress = animatedFracture;
        invalidate();
    }
}
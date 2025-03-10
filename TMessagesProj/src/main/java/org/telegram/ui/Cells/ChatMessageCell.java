/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Property;
import android.util.SparseArray;
import android.util.StateSet;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.animation.Interpolator;
import android.widget.Toast;

import androidx.core.graphics.ColorUtils;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.DocumentObject;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.WebFile;
import org.telegram.messenger.browser.Browser;
import org.telegram.messenger.video.VideoPlayerRewinder;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.AnimatedNumberLayout;
import org.telegram.ui.Components.AnimationProperties;
import org.telegram.ui.Components.AudioVisualizerDrawable;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackgroundGradientDrawable;
import org.telegram.ui.Components.CheckBoxBase;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.EmptyStubSpan;
import org.telegram.ui.Components.FloatSeekBarAccessibilityDelegate;
import org.telegram.ui.Components.InfiniteProgress;
import org.telegram.ui.Components.LinkPath;
import org.telegram.ui.Components.MediaActionDrawable;
import org.telegram.ui.Components.MessageBackgroundDrawable;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import org.telegram.ui.Components.MsgClockDrawable;
import org.telegram.ui.Components.Point;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RadialProgress2;
import org.telegram.ui.Components.RoundVideoPlayingDrawable;
import org.telegram.ui.Components.SeekBar;
import org.telegram.ui.Components.SeekBarAccessibilityDelegate;
import org.telegram.ui.Components.SeekBarWaveform;
import org.telegram.ui.Components.SlotsDrawable;
import org.telegram.ui.Components.StaticLayoutEx;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.TimerParticles;
import org.telegram.ui.Components.TypefaceSpan;
import org.telegram.ui.Components.URLSpanBotCommand;
import org.telegram.ui.Components.URLSpanBrowser;
import org.telegram.ui.Components.URLSpanMono;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.VideoForwardDrawable;
import org.telegram.ui.PhotoViewer;
import org.telegram.ui.PinchToZoomHelper;
import org.telegram.ui.SecretMediaViewer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;

public class ChatMessageCell extends BaseCell implements SeekBar.SeekBarDelegate, ImageReceiver.ImageReceiverDelegate, DownloadController.FileDownloadProgressListener, TextSelectionHelper.SelectableView {

    public RadialProgress2 getRadialProgress() {
        return radialProgress;
    }

    boolean enterTransitionInPorgress;
    public void setEnterTransitionInProgress(boolean b) {
        enterTransitionInPorgress = b;
        invalidate();
    }

    public interface ChatMessageCellDelegate {
        default void didPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY) {
        }

        default boolean didLongPressUserAvatar(ChatMessageCell cell, TLRPC.User user, float touchX, float touchY) {
            return false;
        }

        default void didPressHiddenForward(ChatMessageCell cell) {
        }

        default void didPressViaBot(ChatMessageCell cell, String username) {
        }

        default void didPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId, float touchX, float touchY) {
        }

        default boolean didLongPressChannelAvatar(ChatMessageCell cell, TLRPC.Chat chat, int postId, float touchX, float touchY) {
            return false;
        }

        default void didPressCancelSendButton(ChatMessageCell cell) {
        }

        default void didLongPress(ChatMessageCell cell, float x, float y) {
        }

        default void didPressReplyMessage(ChatMessageCell cell, int id) {
        }

        default void didPressUrl(ChatMessageCell cell, CharacterStyle url, boolean longPress) {
        }

        default void needOpenWebView(MessageObject message, String url, String title, String description, String originalUrl, int w, int h) {
        }

        default void didPressImage(ChatMessageCell cell, float x, float y) {
        }

        default void didPressSideButton(ChatMessageCell cell) {
        }

        default void didPressOther(ChatMessageCell cell, float otherX, float otherY) {
        }

        default void didPressTime(ChatMessageCell cell) {
        }

        default void didPressBotButton(ChatMessageCell cell, TLRPC.KeyboardButton button) {
        }

        default void didPressReaction(ChatMessageCell cell, TLRPC.TL_reactionCount reaction) {
        }

        default void didPressVoteButtons(ChatMessageCell cell, ArrayList<TLRPC.TL_pollAnswer> buttons, int showCount, int x, int y) {
        }

        default void didPressInstantButton(ChatMessageCell cell, int type) {
        }

        default void didPressCommentButton(ChatMessageCell cell) {
        }

        default void didPressHint(ChatMessageCell cell, int type) {
        }

        default String getAdminRank(long uid) {
            return null;
        }

        default boolean needPlayMessage(MessageObject messageObject) {
            return false;
        }

        default boolean canPerformActions() {
            return false;
        }

        default void videoTimerReached() {
        }

        default void didStartVideoStream(MessageObject message) {
        }

        default boolean shouldRepeatSticker(MessageObject message) {
            return true;
        }

        default void setShouldNotRepeatSticker(MessageObject message) {
        }

        default TextSelectionHelper.ChatListTextSelectionHelper getTextSelectionHelper() {
            return null;
        }

        default boolean hasSelectedMessages() {
            return false;
        }

        default void needReloadPolls() {

        }

        default void onDiceFinished() {

        }

        default boolean shouldDrawThreadProgress(ChatMessageCell cell) {
            return false;
        }

        default PinchToZoomHelper getPinchToZoomHelper() {
            return null;
        }

        default boolean keyboardIsOpened() {
            return false;
        }

        default boolean isLandscape() {
            return false;
        }
    }

    private final static int DOCUMENT_ATTACH_TYPE_NONE = 0;
    private final static int DOCUMENT_ATTACH_TYPE_DOCUMENT = 1;
    private final static int DOCUMENT_ATTACH_TYPE_GIF = 2;
    private final static int DOCUMENT_ATTACH_TYPE_AUDIO = 3;
    private final static int DOCUMENT_ATTACH_TYPE_VIDEO = 4;
    private final static int DOCUMENT_ATTACH_TYPE_MUSIC = 5;
    private final static int DOCUMENT_ATTACH_TYPE_STICKER = 6;
    private final static int DOCUMENT_ATTACH_TYPE_ROUND = 7;
    private final static int DOCUMENT_ATTACH_TYPE_WALLPAPER = 8;
    private final static int DOCUMENT_ATTACH_TYPE_THEME = 9;

    private static class BotButton {
        private int x;
        private int y;
        private int width;
        private int height;
        private StaticLayout title;
        private TLRPC.KeyboardButton button;
        private TLRPC.TL_reactionCount reaction;
        private int angle;
        private float progressAlpha;
        private long lastUpdateTime;
    }

    public static class PollButton {
        public int x;
        public int y;
        public int height;
        private int percent;
        private float decimal;
        private int prevPercent;
        private float percentProgress;
        private float prevPercentProgress;
        private boolean chosen;
        private int count;
        private boolean prevChosen;
        private boolean correct;
        private StaticLayout title;
        private TLRPC.TL_pollAnswer answer;
    }

    public boolean pinnedTop;
    public boolean pinnedBottom;
    private boolean drawPinnedTop;
    private boolean drawPinnedBottom;
    private MessageObject.GroupedMessages currentMessagesGroup;
    private MessageObject.GroupedMessagePosition currentPosition;
    private boolean groupPhotoInvisible;

    private int textX;
    private int unmovedTextX;
    private int textY;
    private int totalHeight;
    private int additionalTimeOffsetY;
    private int keyboardHeight;
    private int linkBlockNum;
    private int linkSelectionBlockNum;

    private boolean inLayout;

    private int currentMapProvider;

    private Rect scrollRect = new Rect();

    private int lastVisibleBlockNum;
    private int firstVisibleBlockNum;
    private int totalVisibleBlocksCount;
    private boolean needNewVisiblePart;
    private boolean fullyDraw;

    private int parentWidth;
    private int parentHeight;
    public float parentViewTopOffset;

    private boolean attachedToWindow;

    private boolean isUpdating;

    private RadialProgress2 radialProgress;
    private RadialProgress2 videoRadialProgress;
    private boolean drawRadialCheckBackground;
    private ImageReceiver photoImage;
    private AvatarDrawable contactAvatarDrawable;

    private boolean disallowLongPress;
    private float lastTouchX;
    private float lastTouchY;

    private boolean drawMediaCheckBox;
    private boolean drawSelectionBackground;
    private CheckBoxBase mediaCheckBox;
    private CheckBoxBase checkBox;
    private boolean checkBoxVisible;
    private boolean checkBoxAnimationInProgress;
    private float checkBoxAnimationProgress;
    private long lastCheckBoxAnimationTime;
    private int checkBoxTranslation;

    private boolean isSmallImage;
    private boolean drawImageButton;
    private boolean drawVideoImageButton;
    private long lastLoadingSizeTotal;
    private boolean drawVideoSize;
    private boolean canStreamVideo;
    private int animatingDrawVideoImageButton;
    private float animatingDrawVideoImageButtonProgress;
    private boolean animatingNoSoundPlaying;
    private int animatingNoSound;
    private float animatingNoSoundProgress;
    private int noSoundCenterX;
    private int forwardNameCenterX;
    private long lastAnimationTime;
    private long lastNamesAnimationTime;
    private int documentAttachType;
    private TLRPC.Document documentAttach;
    private boolean drawPhotoImage;
    private boolean hasLinkPreview;
    private boolean hasOldCaptionPreview;
    private boolean hasGamePreview;
    private boolean hasInvoicePreview;
    private int linkPreviewHeight;
    private int mediaOffsetY;
    private int descriptionY;
    private int durationWidth;
    private int photosCountWidth;
    private int descriptionX;
    private int titleX;
    private int authorX;
    private boolean siteNameRtl;
    private int siteNameWidth;
    private StaticLayout siteNameLayout;
    private StaticLayout titleLayout;
    private StaticLayout descriptionLayout;
    private StaticLayout videoInfoLayout;
    private StaticLayout photosCountLayout;
    private StaticLayout authorLayout;
    private StaticLayout instantViewLayout;
    private boolean drawInstantView;
    private int drawInstantViewType;
    private int imageBackgroundColor;
    private float imageBackgroundIntensity;
    private int imageBackgroundGradientColor1;
    private int imageBackgroundGradientColor2;
    private int imageBackgroundGradientColor3;
    private int imageBackgroundGradientRotation = 45;
    private LinearGradient gradientShader;
    private MotionBackgroundDrawable motionBackgroundDrawable;
    private int imageBackgroundSideColor;
    private int imageBackgroundSideWidth;
    private boolean drawJoinGroupView;
    private boolean drawJoinChannelView;
    private int instantTextX;
    private int instantTextLeftX;
    private int instantWidth;
    private boolean insantTextNewLine;
    private boolean instantPressed;
    private boolean instantButtonPressed;
    private Drawable[] selectorDrawable = new Drawable[2];
    private int[] selectorDrawableMaskType = new int[2];
    private RectF instantButtonRect = new RectF();
    private int[] pressedState = new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed};
    private float animatingLoadingProgressProgress;

    private RoundVideoPlayingDrawable roundVideoPlayingDrawable;

    private StaticLayout docTitleLayout;
    private int docTitleWidth;
    private int docTitleOffsetX;
    private boolean locationExpired;

    private StaticLayout captionLayout;
    private CharSequence currentCaption;
    private int captionOffsetX;
    private float captionX;
    private float captionY;
    private int captionHeight;
    private int captionWidth;
    private int addedCaptionHeight;

    private StaticLayout infoLayout;
    private StaticLayout loadingProgressLayout;
    private int infoX;
    private int infoWidth;

    private String currentUrl;
    private WebFile currentWebFile;
    private WebFile lastWebFile;
    private boolean addedForTest;

    private boolean hasEmbed;

    private boolean wasSending;
    private boolean checkOnlyButtonPressed;
    private int buttonX;
    private int buttonY;
    private int videoButtonX;
    private int videoButtonY;
    private int buttonState;
    private int buttonPressed;
    private int videoButtonPressed;
    private int miniButtonPressed;
    private int otherX;
    private int otherY;
    private int lastWidth;
    private int lastHeight;
    private int hasMiniProgress;
    private int miniButtonState;
    private boolean imagePressed;
    private boolean otherPressed;
    private boolean photoNotSet;
    private RectF deleteProgressRect = new RectF();
    private RectF rect = new RectF();
    private TLObject photoParentObject;
    private TLRPC.PhotoSize currentPhotoObject;
    private TLRPC.PhotoSize currentPhotoObjectThumb;
    private String currentPhotoFilter;
    private String currentPhotoFilterThumb;

    private boolean timePressed;

    private float timeAlpha = 1.0f;
    private float controlsAlpha = 1.0f;
    private long lastControlsAlphaChangeTime;
    private long totalChangeTime;
    private boolean mediaWasInvisible;
    private boolean timeWasInvisible;

    private CharacterStyle pressedLink;
    private int pressedLinkType;
    private boolean linkPreviewPressed;
    private boolean gamePreviewPressed;
    private ArrayList<LinkPath> urlPathCache = new ArrayList<>();
    private ArrayList<LinkPath> urlPath = new ArrayList<>();
    private ArrayList<LinkPath> urlPathSelection = new ArrayList<>();

    private Path rectPath = new Path();
    private static float[] radii = new float[8];

    private boolean useSeekBarWaweform;
    private SeekBar seekBar;
    private SeekBarWaveform seekBarWaveform;
    private SeekBarAccessibilityDelegate seekBarAccessibilityDelegate;
    private int seekBarX;
    private int seekBarY;

    private StaticLayout durationLayout;
    private int lastTime;
    private int timeWidthAudio;
    private int timeAudioX;

    private StaticLayout songLayout;
    private int songX;

    private StaticLayout performerLayout;
    private int performerX;

    private ArrayList<PollButton> pollButtons = new ArrayList<>();
    private float pollAnimationProgress;
    private float pollAnimationProgressTime;
    private boolean pollVoted;
    private boolean pollClosed;
    private long lastPollCloseTime;
    private String closeTimeText;
    private int closeTimeWidth;
    private boolean pollVoteInProgress;
    private boolean vibrateOnPollVote;
    private boolean pollUnvoteInProgress;
    private boolean animatePollAnswer;
    private boolean animatePollAvatars;
    private boolean animatePollAnswerAlpha;
    private int pollVoteInProgressNum;
    private long voteLastUpdateTime;
    private float voteRadOffset;
    private float voteCurrentCircleLength;
    private boolean firstCircleLength;
    private boolean voteRisingCircleLength;
    private float voteCurrentProgressTime;
    private int pressedVoteButton;
    private TLRPC.Poll lastPoll;
    private float timerTransitionProgress;
    private ArrayList<TLRPC.TL_pollAnswerVoters> lastPollResults;
    private int lastPollResultsVoters;
    private TimerParticles timerParticles;
    private int pollHintX;
    private int pollHintY;
    private boolean pollHintPressed;
    private boolean hintButtonVisible;
    private float hintButtonProgress;

    private String lastPostAuthor;
    private TLRPC.Message lastReplyMessage;

    private boolean hasPsaHint;
    private int psaHelpX;
    private int psaHelpY;
    private boolean psaHintPressed;
    private boolean psaButtonVisible;
    private float psaButtonProgress;

    private TLRPC.TL_messageReactions lastReactions;

    private boolean autoPlayingMedia;

    private ArrayList<BotButton> botButtons = new ArrayList<>();
    private HashMap<String, BotButton> botButtonsByData = new HashMap<>();
    private HashMap<String, BotButton> botButtonsByPosition = new HashMap<>();
    private String botButtonsLayout;
    private int widthForButtons;
    private int pressedBotButton;

    private MessageObject currentMessageObject;
    private MessageObject messageObjectToSet;
    private MessageObject.GroupedMessages groupedMessagesToSet;
    private boolean topNearToSet;
    private boolean bottomNearToSet;

    private AnimatorSet shakeAnimation;

    //
    private int TAG;
    private int currentAccount = UserConfig.selectedAccount;

    private boolean invalidatesParent;

    public boolean isChat;
    public boolean isBot;
    public boolean isMegagroup;
    public boolean isThreadChat;
    public boolean hasDiscussion;
    public boolean isPinned;
    private boolean wasPinned;
    public long linkedChatId;
    public boolean isRepliesChat;
    public boolean isPinnedChat;
    private boolean isPressed;
    private boolean forwardName;
    private boolean isHighlighted;
    private boolean isHighlightedAnimated;
    private int highlightProgress;
    private float currentSelectedBackgroundAlpha;
    private long lastHighlightProgressTime;
    private boolean mediaBackground;
    private boolean isMedia;
    private boolean isCheckPressed = true;
    private boolean wasLayout;
    private boolean isAvatarVisible;
    private boolean isThreadPost;
    private boolean drawBackground = true;
    private int substractBackgroundHeight;
    private boolean allowAssistant;
    private Theme.MessageDrawable currentBackgroundDrawable;
    private Theme.MessageDrawable currentBackgroundSelectedDrawable;
    private int backgroundDrawableLeft;
    private int backgroundDrawableRight;
    private int backgroundDrawableTop;
    private int backgroundDrawableBottom;
    private int viaWidth;
    private int viaNameWidth;
    private TypefaceSpan viaSpan1;
    private TypefaceSpan viaSpan2;
    private int availableTimeWidth;
    private int widthBeforeNewTimeLine;

    private int backgroundWidth = 100;
    private boolean hasNewLineForTime;

    private int layoutWidth;
    private int layoutHeight;

    private ImageReceiver[] pollAvatarImages;
    private AvatarDrawable[] pollAvatarDrawables;
    private boolean[] pollAvatarImagesVisible;
    private CheckBoxBase[] pollCheckBox;

    private InfiniteProgress commentProgress;
    private float commentProgressAlpha;
    private long commentProgressLastUpadteTime;
    private ImageReceiver[] commentAvatarImages;
    private AvatarDrawable[] commentAvatarDrawables;
    private boolean[] commentAvatarImagesVisible;
    private StaticLayout commentLayout;
    private AnimatedNumberLayout commentNumberLayout;
    private boolean drawCommentNumber;
    private int commentArrowX;
    private int commentUnreadX;
    private boolean commentDrawUnread;
    private int commentWidth;
    private int commentX;
    private int totalCommentWidth;
    private int commentNumberWidth;
    private boolean drawCommentButton;
    private Rect commentButtonRect = new Rect();
    private boolean commentButtonPressed;

    private ImageReceiver avatarImage;
    private AvatarDrawable avatarDrawable;
    private boolean avatarPressed;
    private boolean forwardNamePressed;
    private boolean forwardBotPressed;

    private ImageReceiver locationImageReceiver;

    public StaticLayout replyNameLayout;
    public StaticLayout replyTextLayout;
    public ImageReceiver replyImageReceiver;
    public int replyStartX;
    public int replyStartY;
    private int replyNameWidth;
    private int replyNameOffset;
    private int replyTextWidth;
    private int replyTextOffset;
    public boolean needReplyImage;
    private boolean replyPressed;
    private TLRPC.PhotoSize currentReplyPhoto;

    private int drawSideButton;
    private boolean sideButtonPressed;
    private float sideStartX;
    private float sideStartY;

    private StaticLayout nameLayout;
    private StaticLayout adminLayout;
    private int nameWidth;
    private float nameOffsetX;
    private float nameX;
    private float nameY;
    private boolean drawName;
    private boolean drawNameLayout;

    private StaticLayout[] forwardedNameLayout = new StaticLayout[2];
    private int forwardedNameWidth;
    private boolean drawForwardedName;
    private float forwardNameX;
    private int forwardNameY;
    private float[] forwardNameOffsetX = new float[2];

    private float drawTimeX;
    private float drawTimeY;
    private StaticLayout timeLayout;
    private int timeWidth;
    private int timeTextWidth;
    private int timeX;
    private String currentTimeString;
    private boolean drawTime = true;
    private boolean forceNotDrawTime;

    private StaticLayout viewsLayout;
    private int viewsTextWidth;
    private String currentViewsString;

    private StaticLayout repliesLayout;
    private int repliesTextWidth;
    private String currentRepliesString;

    private TLRPC.User currentUser;
    private TLRPC.Chat currentChat;
    private TLRPC.FileLocation currentPhoto;
    private String currentNameString;

    private TLRPC.User currentForwardUser;
    private TLRPC.User currentViaBotUser;
    private TLRPC.Chat currentForwardChannel;
    private String currentForwardName;
    private String currentForwardNameString;
    private boolean replyPanelIsForward;
    private boolean animationRunning;
    private boolean willRemoved;

    private ChatMessageCellDelegate delegate;

    private MessageBackgroundDrawable backgroundDrawable;

    private int namesOffset;

    private int lastSendState;
    private int lastDeleteDate;
    private int lastViewsCount;
    private int lastRepliesCount;
    private float selectedBackgroundProgress;

    private float viewTop;
    private int backgroundHeight;

    private boolean scheduledInvalidate;

    private final boolean ALPHA_PROPERTY_WORKAROUND = Build.VERSION.SDK_INT == 28;
    private float alphaInternal = 1f;

    private final TransitionParams transitionParams = new TransitionParams();
    private boolean edited;
    private boolean imageDrawn;

    private Runnable diceFinishCallback = new Runnable() {
        @Override
        public void run() {
            if (delegate != null) {
                delegate.onDiceFinished();
            }
        }
    };

    private int animateToStatusDrawableParams;
    private int animateFromStatusDrawableParams;
    private float statusDrawableProgress;
    private boolean statusDrawableAnimationInProgress;
    private ValueAnimator statusDrawableAnimator;

    private int overideShouldDrawTimeOnMedia;
    private float toSeekBarProgress;

    private Runnable invalidateRunnable = new Runnable() {
        @Override
        public void run() {
            checkLocationExpired();
            if (locationExpired) {
                invalidate();
                scheduledInvalidate = false;
            } else {
                invalidate((int) rect.left - 5, (int) rect.top - 5, (int) rect.right + 5, (int) rect.bottom + 5);
                if (scheduledInvalidate) {
                    AndroidUtilities.runOnUIThread(invalidateRunnable, 1000);
                }
            }
        }
    };
    private SparseArray<Rect> accessibilityVirtualViewBounds = new SparseArray<>();
    private boolean isRoundVideo;
    private boolean isPlayingRound;
    private float roundProgressAlpha;
    private float roundToPauseProgress;
    private float roundToPauseProgress2;
    private float roundPlayingDrawableProgress;
    private long lastSeekUpdateTime;

    float seekbarRoundX;
    float seekbarRoundY;
    float lastDrawingPlayPauseAlpha;
    int roundSeekbarTouched;
    float roundSeekbarOutProgress;
    float roundSeekbarOutAlpha;

    private float lastDrawingAudioProgress;
    private int currentFocusedVirtualView = -1;
    public boolean drawFromPinchToZoom;

    private Theme.MessageDrawable.PathDrawParams backgroundCacheParams = new Theme.MessageDrawable.PathDrawParams();

    VideoForwardDrawable videoForwardDrawable;
    VideoPlayerRewinder videoPlayerRewinder;

    private final Theme.ResourcesProvider resourcesProvider;
    private final boolean canDrawBackgroundInParent;

    public ChatMessageCell(Context context) {
        this(context, false, null);
    }

    public ChatMessageCell(Context context, boolean canDrawBackgroundInParent, Theme.ResourcesProvider resourcesProvider) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.canDrawBackgroundInParent = canDrawBackgroundInParent;

        backgroundDrawable = new MessageBackgroundDrawable(this);
        avatarImage = new ImageReceiver();
        avatarImage.setRoundRadius(AndroidUtilities.dp(21));
        avatarDrawable = new AvatarDrawable();
        replyImageReceiver = new ImageReceiver(this);
        replyImageReceiver.setRoundRadius(AndroidUtilities.dp(2));
        locationImageReceiver = new ImageReceiver(this);
        locationImageReceiver.setRoundRadius(AndroidUtilities.dp(26.1f));
        TAG = DownloadController.getInstance(currentAccount).generateObserverTag();

        contactAvatarDrawable = new AvatarDrawable();
        photoImage = new ImageReceiver(this);
        photoImage.setDelegate(this);
        radialProgress = new RadialProgress2(this, resourcesProvider);
        videoRadialProgress = new RadialProgress2(this, resourcesProvider);
        videoRadialProgress.setDrawBackground(false);
        videoRadialProgress.setCircleRadius(AndroidUtilities.dp(15));
        seekBar = new SeekBar(this);
        seekBar.setDelegate(this);
        seekBarWaveform = new SeekBarWaveform(context);
        seekBarWaveform.setDelegate(this);
        seekBarWaveform.setParentView(this);
        seekBarAccessibilityDelegate = new FloatSeekBarAccessibilityDelegate() {
            @Override
            public float getProgress() {
                if (currentMessageObject.isMusic()) {
                    return seekBar.getProgress();
                } else if (currentMessageObject.isVoice()) {
                    if (useSeekBarWaweform) {
                        return seekBarWaveform.getProgress();
                    } else {
                        return seekBar.getProgress();
                    }
                } else {
                    return 0f;
                }
            }

            @Override
            public void setProgress(float progress) {
                if (currentMessageObject.isMusic()) {
                    seekBar.setProgress(progress);
                } else if (currentMessageObject.isVoice()) {
                    if (useSeekBarWaweform) {
                        seekBarWaveform.setProgress(progress);
                    } else {
                        seekBar.setProgress(progress);
                    }
                } else {
                    return;
                }
                onSeekBarDrag(progress);
                invalidate();
            }
        };
        roundVideoPlayingDrawable = new RoundVideoPlayingDrawable(this, resourcesProvider);
    }

    private void createPollUI() {
        if (pollAvatarImages != null) {
            return;
        }
        pollAvatarImages = new ImageReceiver[3];
        pollAvatarDrawables = new AvatarDrawable[3];
        pollAvatarImagesVisible = new boolean[3];
        for (int a = 0; a < pollAvatarImages.length; a++) {
            pollAvatarImages[a] = new ImageReceiver(this);
            pollAvatarImages[a].setRoundRadius(AndroidUtilities.dp(8));
            pollAvatarDrawables[a] = new AvatarDrawable();
            pollAvatarDrawables[a].setTextSize(AndroidUtilities.dp(6));
        }
        pollCheckBox = new CheckBoxBase[10];
        for (int a = 0; a < pollCheckBox.length; a++) {
            pollCheckBox[a] = new CheckBoxBase(this, 20, resourcesProvider);
            pollCheckBox[a].setDrawUnchecked(false);
            pollCheckBox[a].setBackgroundType(9);
        }
    }

    private void createCommentUI() {
        if (commentAvatarImages != null) {
            return;
        }
        commentAvatarImages = new ImageReceiver[3];
        commentAvatarDrawables = new AvatarDrawable[3];
        commentAvatarImagesVisible = new boolean[3];
        for (int a = 0; a < commentAvatarImages.length; a++) {
            commentAvatarImages[a] = new ImageReceiver(this);
            commentAvatarImages[a].setRoundRadius(AndroidUtilities.dp(12));
            commentAvatarDrawables[a] = new AvatarDrawable();
            commentAvatarDrawables[a].setTextSize(AndroidUtilities.dp(8));
        }
    }

    private void resetPressedLink(int type) {
        if (pressedLink == null || pressedLinkType != type && type != -1) {
            return;
        }
        resetUrlPaths(false);
        pressedLink = null;
        pressedLinkType = -1;
        invalidate();
    }

    private void resetUrlPaths(boolean text) {
        if (text) {
            if (urlPathSelection.isEmpty()) {
                return;
            }
            urlPathCache.addAll(urlPathSelection);
            urlPathSelection.clear();
        } else {
            if (urlPath.isEmpty()) {
                return;
            }
            urlPathCache.addAll(urlPath);
            urlPath.clear();
        }
    }

    private LinkPath obtainNewUrlPath(boolean text) {
        LinkPath linkPath;
        if (!urlPathCache.isEmpty()) {
            linkPath = urlPathCache.get(0);
            urlPathCache.remove(0);
        } else {
            linkPath = new LinkPath();
        }
        linkPath.reset();
        if (text) {
            urlPathSelection.add(linkPath);
        } else {
            urlPath.add(linkPath);
        }
        return linkPath;
    }

    private int[] getRealSpanStartAndEnd(Spannable buffer, CharacterStyle link) {
        int start = 0;
        int end = 0;
        boolean ok = false;
        if (link instanceof URLSpanBrowser) {
            URLSpanBrowser span = (URLSpanBrowser) link;
            TextStyleSpan.TextStyleRun style = span.getStyle();
            if (style != null && style.urlEntity != null) {
                start = style.urlEntity.offset;
                end = style.urlEntity.offset + style.urlEntity.length;
                ok = true;
            }
        }
        if (!ok) {
            start = buffer.getSpanStart(link);
            end = buffer.getSpanEnd(link);
        }
        return new int[]{start, end};
    }

    private boolean checkTextBlockMotionEvent(MotionEvent event) {
        if (currentMessageObject.type != 0 || currentMessageObject.textLayoutBlocks == null || currentMessageObject.textLayoutBlocks.isEmpty() || !(currentMessageObject.messageText instanceof Spannable)) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_UP && pressedLinkType == 1) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x >= textX && y >= textY && x <= textX + currentMessageObject.textWidth && y <= textY + currentMessageObject.textHeight) {
                y -= textY;
                int blockNum = 0;
                for (int a = 0; a < currentMessageObject.textLayoutBlocks.size(); a++) {
                    if (currentMessageObject.textLayoutBlocks.get(a).textYOffset > y) {
                        break;
                    }
                    blockNum = a;
                }
                try {
                    MessageObject.TextLayoutBlock block = currentMessageObject.textLayoutBlocks.get(blockNum);
                    x -= textX - (block.isRtl() ? currentMessageObject.textXOffset : 0);
                    y -= block.textYOffset;
                    final int line = block.textLayout.getLineForVertical(y);
                    final int off = block.textLayout.getOffsetForHorizontal(line, x);

                    final float left = block.textLayout.getLineLeft(line);
                    if (left <= x && left + block.textLayout.getLineWidth(line) >= x) {
                        Spannable buffer = (Spannable) currentMessageObject.messageText;
                        CharacterStyle[] link = buffer.getSpans(off, off, ClickableSpan.class);
                        boolean isMono = false;
                        if (link == null || link.length == 0) {
                            link = buffer.getSpans(off, off, URLSpanMono.class);
                            isMono = true;
                        }
                        boolean ignore = false;
                        if (link.length == 0 || link[0] instanceof URLSpanBotCommand && !URLSpanBotCommand.enabled) {
                            ignore = true;
                        }
                        if (!ignore) {
                            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                                pressedLink = link[0];
                                linkBlockNum = blockNum;
                                pressedLinkType = 1;
                                resetUrlPaths(false);
                                try {
                                    LinkPath path = obtainNewUrlPath(false);
                                    int[] pos = getRealSpanStartAndEnd(buffer, pressedLink);
                                    path.setCurrentLayout(block.textLayout, pos[0], 0);
                                    block.textLayout.getSelectionPath(pos[0], pos[1], path);
                                    if (pos[1] >= block.charactersEnd) {
                                        for (int a = blockNum + 1; a < currentMessageObject.textLayoutBlocks.size(); a++) {
                                            MessageObject.TextLayoutBlock nextBlock = currentMessageObject.textLayoutBlocks.get(a);
                                            CharacterStyle[] nextLink;
                                            if (isMono) {
                                                nextLink = buffer.getSpans(nextBlock.charactersOffset, nextBlock.charactersOffset, URLSpanMono.class);
                                            } else {
                                                nextLink = buffer.getSpans(nextBlock.charactersOffset, nextBlock.charactersOffset, ClickableSpan.class);
                                            }
                                            if (nextLink == null || nextLink.length == 0 || nextLink[0] != pressedLink) {
                                                break;
                                            }
                                            path = obtainNewUrlPath(false);
                                            path.setCurrentLayout(nextBlock.textLayout, 0, nextBlock.textYOffset - block.textYOffset);
                                            nextBlock.textLayout.getSelectionPath(0, pos[1], path);
                                            if (pos[1] < nextBlock.charactersEnd - 1) {
                                                break;
                                            }
                                        }
                                    }
                                    if (pos[0] <= block.charactersOffset) {
                                        int offsetY = 0;
                                        for (int a = blockNum - 1; a >= 0; a--) {
                                            MessageObject.TextLayoutBlock nextBlock = currentMessageObject.textLayoutBlocks.get(a);
                                            CharacterStyle[] nextLink;
                                            if (isMono) {
                                                nextLink = buffer.getSpans(nextBlock.charactersEnd - 1, nextBlock.charactersEnd - 1, URLSpanMono.class);
                                            } else {
                                                nextLink = buffer.getSpans(nextBlock.charactersEnd - 1, nextBlock.charactersEnd - 1, ClickableSpan.class);
                                            }
                                            if (nextLink == null || nextLink.length == 0 || nextLink[0] != pressedLink) {
                                                break;
                                            }
                                            path = obtainNewUrlPath(false);
                                            offsetY -= nextBlock.height;
                                            path.setCurrentLayout(nextBlock.textLayout, pos[0], offsetY);
                                            nextBlock.textLayout.getSelectionPath(pos[0], pos[1], path);
                                            if (pos[0] > nextBlock.charactersOffset) {
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                                invalidate();
                                return true;
                            } else {
                                if (link[0] == pressedLink) {
                                    delegate.didPressUrl(this, pressedLink, false);
                                    resetPressedLink(1);
                                    return true;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else {
                resetPressedLink(1);
            }
        }
        return false;
    }

    private boolean checkCaptionMotionEvent(MotionEvent event) {
        if (!(currentCaption instanceof Spannable) || captionLayout == null) {
            return false;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN || (linkPreviewPressed || pressedLink != null) && event.getAction() == MotionEvent.ACTION_UP) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            if (x >= captionX && x <= captionX + captionWidth && y >= captionY && y <= captionY + captionHeight) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    try {
                        x -= captionX;
                        y -= captionY;
                        final int line = captionLayout.getLineForVertical(y);
                        final int off = captionLayout.getOffsetForHorizontal(line, x);

                        final float left = captionLayout.getLineLeft(line);
                        if (left <= x && left + captionLayout.getLineWidth(line) >= x) {
                            Spannable buffer = (Spannable) currentCaption;
                            CharacterStyle[] link = buffer.getSpans(off, off, ClickableSpan.class);
                            if (link == null || link.length == 0) {
                                link = buffer.getSpans(off, off, URLSpanMono.class);
                            }
                            boolean ignore = false;
                            if (link.length == 0 || link[0] instanceof URLSpanBotCommand && !URLSpanBotCommand.enabled) {
                                ignore = true;
                            }
                            if (!ignore) {
                                pressedLink = link[0];
                                pressedLinkType = 3;
                                resetUrlPaths(false);
                                try {
                                    LinkPath path = obtainNewUrlPath(false);
                                    int[] pos = getRealSpanStartAndEnd(buffer, pressedLink);
                                    path.setCurrentLayout(captionLayout, pos[0], 0);
                                    captionLayout.getSelectionPath(pos[0], pos[1], path);
                                } catch (Exception e) {
                                    FileLog.e(e);
                                }
                                invalidateWithParent();
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                } else if (pressedLinkType == 3) {
                    delegate.didPressUrl(this, pressedLink, false);
                    resetPressedLink(3);
                    return true;
                }
            } else {
                resetPressedLink(3);
            }
        }
        return false;
    }

    private boolean checkGameMotionEvent(MotionEvent event) {
        if (!hasGamePreview) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (drawPhotoImage && drawImageButton && buttonState != -1 && x >= buttonX && x <= buttonX + AndroidUtilities.dp(48) && y >= buttonY && y <= buttonY + AndroidUtilities.dp(48) && radialProgress.getIcon() != MediaActionDrawable.ICON_NONE) {
                buttonPressed = 1;
                invalidate();
                return true;
            } else if (drawPhotoImage && photoImage.isInsideImage(x, y)) {
                gamePreviewPressed = true;
                return true;
            } else if (descriptionLayout != null && y >= descriptionY) {
                try {
                    x -= unmovedTextX + AndroidUtilities.dp(10) + descriptionX;
                    y -= descriptionY;
                    final int line = descriptionLayout.getLineForVertical(y);
                    final int off = descriptionLayout.getOffsetForHorizontal(line, x);

                    final float left = descriptionLayout.getLineLeft(line);
                    if (left <= x && left + descriptionLayout.getLineWidth(line) >= x) {
                        Spannable buffer = (Spannable) currentMessageObject.linkDescription;
                        ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
                        boolean ignore = false;
                        if (link.length == 0 || link[0] instanceof URLSpanBotCommand && !URLSpanBotCommand.enabled) {
                            ignore = true;
                        }
                        if (!ignore) {
                            pressedLink = link[0];
                            linkBlockNum = -10;
                            pressedLinkType = 2;
                            resetUrlPaths(false);
                            try {
                                LinkPath path = obtainNewUrlPath(false);
                                int[] pos = getRealSpanStartAndEnd(buffer, pressedLink);
                                path.setCurrentLayout(descriptionLayout, pos[0], 0);
                                descriptionLayout.getSelectionPath(pos[0], pos[1], path);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            invalidate();
                            return true;
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (pressedLinkType == 2 || gamePreviewPressed || buttonPressed != 0) {
                if (buttonPressed != 0) {
                    buttonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressButton(true, false);
                    invalidate();
                } else if (pressedLink != null) {
                    if (pressedLink instanceof URLSpan) {
                        Browser.openUrl(getContext(), ((URLSpan) pressedLink).getURL());
                    } else if (pressedLink instanceof ClickableSpan) {
                        ((ClickableSpan) pressedLink).onClick(this);
                    }
                    resetPressedLink(2);
                } else {
                    gamePreviewPressed = false;
                    for (int a = 0; a < botButtons.size(); a++) {
                        BotButton button = botButtons.get(a);
                        if (button.button instanceof TLRPC.TL_keyboardButtonGame) {
                            playSoundEffect(SoundEffectConstants.CLICK);
                            delegate.didPressBotButton(this, button.button);
                            invalidate();
                            break;
                        }
                    }
                    resetPressedLink(2);
                    return true;
                }
            } else {
                resetPressedLink(2);
            }
        }
        return false;
    }

    private boolean checkLinkPreviewMotionEvent(MotionEvent event) {
        if (currentMessageObject.type != 0 || !hasLinkPreview) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (x >= unmovedTextX && x <= unmovedTextX + backgroundWidth && y >= textY + currentMessageObject.textHeight && y <= textY + currentMessageObject.textHeight + linkPreviewHeight + AndroidUtilities.dp(8 + (drawInstantView ? 46 : 0))) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (descriptionLayout != null && y >= descriptionY) {
                    try {
                        int checkX = x - (unmovedTextX + AndroidUtilities.dp(10) + descriptionX);
                        int checkY = y - descriptionY;
                        if (checkY <= descriptionLayout.getHeight()) {
                            final int line = descriptionLayout.getLineForVertical(checkY);
                            final int off = descriptionLayout.getOffsetForHorizontal(line, checkX);

                            final float left = descriptionLayout.getLineLeft(line);
                            if (left <= checkX && left + descriptionLayout.getLineWidth(line) >= checkX) {
                                Spannable buffer = (Spannable) currentMessageObject.linkDescription;
                                ClickableSpan[] link = buffer.getSpans(off, off, ClickableSpan.class);
                                boolean ignore = false;
                                if (link.length == 0 || link[0] instanceof URLSpanBotCommand && !URLSpanBotCommand.enabled) {
                                    ignore = true;
                                }
                                if (!ignore) {
                                    pressedLink = link[0];
                                    linkBlockNum = -10;
                                    pressedLinkType = 2;
                                    resetUrlPaths(false);
                                    startCheckLongPress();
                                    try {
                                        LinkPath path = obtainNewUrlPath(false);
                                        int[] pos = getRealSpanStartAndEnd(buffer, pressedLink);
                                        path.setCurrentLayout(descriptionLayout, pos[0], 0);
                                        descriptionLayout.getSelectionPath(pos[0], pos[1], path);
                                    } catch (Exception e) {
                                        FileLog.e(e);
                                    }
                                    invalidate();
                                    return true;
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (pressedLink == null) {
                    int side = AndroidUtilities.dp(48);
                    boolean area2 = false;
                    if (miniButtonState >= 0) {
                        int offset = AndroidUtilities.dp(27);
                        area2 = x >= buttonX + offset && x <= buttonX + offset + side && y >= buttonY + offset && y <= buttonY + offset + side;
                    }
                    if (area2) {
                        miniButtonPressed = 1;
                        invalidate();
                        return true;
                    } else if (drawVideoImageButton && buttonState != -1 && x >= videoButtonX && x <= videoButtonX + AndroidUtilities.dp(26 + 8) + Math.max(infoWidth, docTitleWidth) && y >= videoButtonY && y <= videoButtonY + AndroidUtilities.dp(30)) {
                        videoButtonPressed = 1;
                        invalidate();
                        return true;
                    } else if (drawPhotoImage && drawImageButton && buttonState != -1 && (!checkOnlyButtonPressed && photoImage.isInsideImage(x, y) || x >= buttonX && x <= buttonX + AndroidUtilities.dp(48) && y >= buttonY && y <= buttonY + AndroidUtilities.dp(48) && radialProgress.getIcon() != MediaActionDrawable.ICON_NONE)) {
                        buttonPressed = 1;
                        invalidate();
                        return true;
                    } else if (drawInstantView) {
                        instantPressed = true;
                        selectorDrawableMaskType[0] = 0;
                        if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                            if (selectorDrawable[0].getBounds().contains(x, y)) {
                                selectorDrawable[0].setHotspot(x, y);
                                selectorDrawable[0].setState(pressedState);
                                instantButtonPressed = true;
                            }
                        }
                        invalidate();
                        return true;
                    } else if (documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT && drawPhotoImage && photoImage.isInsideImage(x, y)) {
                        linkPreviewPressed = true;
                        TLRPC.WebPage webPage = currentMessageObject.messageOwner.media.webpage;
                        if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && buttonState == -1 && SharedConfig.autoplayGifs && (photoImage.getAnimation() == null || !TextUtils.isEmpty(webPage.embed_url))) {
                            linkPreviewPressed = false;
                            return false;
                        }
                        return true;
                    }
                }
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                if (instantPressed) {
                    if (delegate != null) {
                        delegate.didPressInstantButton(this, drawInstantViewType);
                    }
                    playSoundEffect(SoundEffectConstants.CLICK);
                    if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                        selectorDrawable[0].setState(StateSet.NOTHING);
                    }
                    instantPressed = instantButtonPressed = false;
                    invalidate();
                } else if (pressedLinkType == 2 || buttonPressed != 0 || miniButtonPressed != 0 || videoButtonPressed != 0 || linkPreviewPressed) {
                    if (videoButtonPressed == 1) {
                        videoButtonPressed = 0;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        didPressButton(true, true);
                        invalidate();
                    } else if (buttonPressed != 0) {
                        buttonPressed = 0;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (drawVideoImageButton) {
                            didClickedImage();
                        } else {
                            didPressButton(true, false);
                        }
                        invalidate();
                    } else if (miniButtonPressed != 0) {
                        miniButtonPressed = 0;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        didPressMiniButton(true);
                        invalidate();
                    } else if (pressedLink != null) {
                        if (pressedLink instanceof URLSpan) {
                            delegate.didPressUrl(this, pressedLink, false);
                        } else if (pressedLink instanceof ClickableSpan) {
                            ((ClickableSpan) pressedLink).onClick(this);
                        }
                        resetPressedLink(2);
                    } else {
                        if (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
                            if (!MediaController.getInstance().isPlayingMessage(currentMessageObject) || MediaController.getInstance().isMessagePaused()) {
                                delegate.needPlayMessage(currentMessageObject);
                            } else {
                                MediaController.getInstance().pauseMessage(currentMessageObject);
                            }
                        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && drawImageButton) {
                            if (buttonState == -1) {
                                if (SharedConfig.autoplayGifs) {
                                    delegate.didPressImage(this, lastTouchX, lastTouchY);
                                } else {
                                    buttonState = 2;
                                    currentMessageObject.gifState = 1;
                                    photoImage.setAllowStartAnimation(false);
                                    photoImage.stopAnimation();
                                    radialProgress.setIcon(getIconForCurrentState(), false, true);
                                    invalidate();
                                    playSoundEffect(SoundEffectConstants.CLICK);
                                }
                            } else if (buttonState == 2 || buttonState == 0) {
                                didPressButton(true, false);
                                playSoundEffect(SoundEffectConstants.CLICK);
                            }
                        } else {
                            TLRPC.WebPage webPage = currentMessageObject.messageOwner.media.webpage;
                            if (webPage != null && !TextUtils.isEmpty(webPage.embed_url)) {
                                delegate.needOpenWebView(currentMessageObject, webPage.embed_url, webPage.site_name, webPage.title, webPage.url, webPage.embed_width, webPage.embed_height);
                            } else if (buttonState == -1 || buttonState == 3) {
                                delegate.didPressImage(this, lastTouchX, lastTouchY);
                                playSoundEffect(SoundEffectConstants.CLICK);
                            } else if (webPage != null) {
                                Browser.openUrl(getContext(), webPage.url);
                            }
                        }
                        resetPressedLink(2);
                        return true;
                    }
                } else {
                    resetPressedLink(2);
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (instantButtonPressed && Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                    selectorDrawable[0].setHotspot(x, y);
                }
            }
        }
        return false;
    }

    private boolean checkPollButtonMotionEvent(MotionEvent event) {
        if (currentMessageObject.eventId != 0 || pollVoteInProgress || pollUnvoteInProgress || pollButtons.isEmpty() || currentMessageObject.type != 17 || !currentMessageObject.isSent()) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            pressedVoteButton = -1;
            pollHintPressed = false;
            if (hintButtonVisible && pollHintX != -1 && x >= pollHintX && x <= pollHintX + AndroidUtilities.dp(40) && y >= pollHintY && y <= pollHintY + AndroidUtilities.dp(40)) {
                pollHintPressed = true;
                result = true;
                selectorDrawableMaskType[0] = 3;
                if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                    selectorDrawable[0].setBounds(pollHintX - AndroidUtilities.dp(8), pollHintY - AndroidUtilities.dp(8), pollHintX + AndroidUtilities.dp(32), pollHintY + AndroidUtilities.dp(32));
                    selectorDrawable[0].setHotspot(x, y);
                    selectorDrawable[0].setState(pressedState);
                }
                invalidate();
            } else {
                for (int a = 0; a < pollButtons.size(); a++) {
                    PollButton button = pollButtons.get(a);
                    int y2 = button.y + namesOffset - AndroidUtilities.dp(13);
                    if (x >= button.x && x <= button.x + backgroundWidth - AndroidUtilities.dp(31) && y >= y2 && y <= y2 + button.height + AndroidUtilities.dp(26)) {
                        pressedVoteButton = a;
                        if (!pollVoted && !pollClosed) {
                            selectorDrawableMaskType[0] = 1;
                            if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                                selectorDrawable[0].setBounds(button.x - AndroidUtilities.dp(9), y2, button.x + backgroundWidth - AndroidUtilities.dp(22), y2 + button.height + AndroidUtilities.dp(26));
                                selectorDrawable[0].setHotspot(x, y);
                                selectorDrawable[0].setState(pressedState);
                            }
                            invalidate();
                        }
                        result = true;
                        break;
                    }
                }
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (pollHintPressed) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    delegate.didPressHint(this, 0);
                    pollHintPressed = false;
                    if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                        selectorDrawable[0].setState(StateSet.NOTHING);
                    }
                } else if (pressedVoteButton != -1) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                        selectorDrawable[0].setState(StateSet.NOTHING);
                    }
                    if (currentMessageObject.scheduled) {
                        Toast.makeText(getContext(), LocaleController.getString("MessageScheduledVote", R.string.MessageScheduledVote), Toast.LENGTH_LONG).show();
                    } else {
                        PollButton button = pollButtons.get(pressedVoteButton);
                        TLRPC.TL_pollAnswer answer = button.answer;
                        if (pollVoted || pollClosed) {
                            ArrayList<TLRPC.TL_pollAnswer> answers = new ArrayList<>();
                            answers.add(answer);
                            delegate.didPressVoteButtons(this, answers, button.count, button.x + AndroidUtilities.dp(50), button.y + namesOffset);
                        } else {
                            if (lastPoll.multiple_choice) {
                                if (currentMessageObject.checkedVotes.contains(answer)) {
                                    currentMessageObject.checkedVotes.remove(answer);
                                    pollCheckBox[pressedVoteButton].setChecked(false, true);
                                } else {
                                    currentMessageObject.checkedVotes.add(answer);
                                    pollCheckBox[pressedVoteButton].setChecked(true, true);
                                }
                            } else {
                                pollVoteInProgressNum = pressedVoteButton;
                                pollVoteInProgress = true;
                                vibrateOnPollVote = true;
                                voteCurrentProgressTime = 0.0f;
                                firstCircleLength = true;
                                voteCurrentCircleLength = 360;
                                voteRisingCircleLength = false;
                                ArrayList<TLRPC.TL_pollAnswer> answers = new ArrayList<>();
                                answers.add(answer);
                                delegate.didPressVoteButtons(this, answers, -1, 0, 0);
                            }
                        }
                    }
                    pressedVoteButton = -1;
                    invalidate();
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if ((pressedVoteButton != -1 || pollHintPressed) && Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                    selectorDrawable[0].setHotspot(x, y);
                }
            }
        }
        return result;
    }

    private boolean checkInstantButtonMotionEvent(MotionEvent event) {
        if (!currentMessageObject.isSponsored() && (!drawInstantView || currentMessageObject.type == 0)) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (drawInstantView && instantButtonRect.contains(x, y)) {
                selectorDrawableMaskType[0] = lastPoll != null ? 2 : 0;
                instantPressed = true;
                if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                    if (instantButtonRect.contains(x, y)) {
                        selectorDrawable[0].setHotspot(x, y);
                        selectorDrawable[0].setState(pressedState);
                        instantButtonPressed = true;
                    }
                }
                invalidate();
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (instantPressed) {
                if (delegate != null) {
                    if (lastPoll != null) {
                        if (currentMessageObject.scheduled) {
                            Toast.makeText(getContext(), LocaleController.getString("MessageScheduledVoteResults", R.string.MessageScheduledVoteResults), Toast.LENGTH_LONG).show();
                        } else {
                            if (pollVoted || pollClosed) {
                                delegate.didPressInstantButton(this, drawInstantViewType);
                            } else {
                                if (!currentMessageObject.checkedVotes.isEmpty()) {
                                    pollVoteInProgressNum = -1;
                                    pollVoteInProgress = true;
                                    vibrateOnPollVote = true;
                                    voteCurrentProgressTime = 0.0f;
                                    firstCircleLength = true;
                                    voteCurrentCircleLength = 360;
                                    voteRisingCircleLength = false;
                                }
                                delegate.didPressVoteButtons(this, currentMessageObject.checkedVotes, -1, 0, namesOffset);
                            }
                        }
                    } else {
                        delegate.didPressInstantButton(this, drawInstantViewType);
                    }
                }
                playSoundEffect(SoundEffectConstants.CLICK);
                if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                    selectorDrawable[0].setState(StateSet.NOTHING);
                }
                instantPressed = instantButtonPressed = false;
                invalidate();
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (instantButtonPressed && Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                selectorDrawable[0].setHotspot(x, y);
            }
        }
        return false;
    }

    private void invalidateWithParent() {
        if (currentMessagesGroup != null && getParent() != null) {
            ((ViewGroup) getParent()).invalidate();
        }
        invalidate();
    }

    private boolean checkCommentButtonMotionEvent(MotionEvent event) {
        if (!drawCommentButton) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0 && commentButtonRect.contains(x, y)) {
            ViewGroup parent = (ViewGroup) getParent();
            for (int a = 0, N = parent.getChildCount(); a < N; a++) {
                View view = parent.getChildAt(a);
                if (view != this && view instanceof ChatMessageCell) {
                    ChatMessageCell cell = (ChatMessageCell) view;
                    if (cell.drawCommentButton && cell.currentMessagesGroup == currentMessagesGroup && (cell.currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0) {
                        MotionEvent childEvent = MotionEvent.obtain(0, 0, event.getActionMasked(), event.getX() + getLeft() - cell.getLeft(), event.getY() + getTop() - cell.getTop(), 0);
                        cell.checkCommentButtonMotionEvent(childEvent);
                        childEvent.recycle();
                        break;
                    }
                }
            }
            return true;
        }
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (commentButtonRect.contains(x, y)) {
                if (currentMessageObject.isSent()) {
                    selectorDrawableMaskType[1] = 2;
                    commentButtonPressed = true;
                    if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[1] != null) {
                        selectorDrawable[1].setHotspot(x, y);
                        selectorDrawable[1].setState(pressedState);
                    }
                    invalidateWithParent();
                }
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (commentButtonPressed) {
                if (delegate != null) {
                    if (isRepliesChat) {
                        delegate.didPressSideButton(this);
                    } else {
                        delegate.didPressCommentButton(this);
                    }
                }
                playSoundEffect(SoundEffectConstants.CLICK);
                if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[1] != null) {
                    selectorDrawable[1].setState(StateSet.NOTHING);
                }
                commentButtonPressed = false;
                invalidateWithParent();
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            if (commentButtonPressed && Build.VERSION.SDK_INT >= 21 && selectorDrawable[1] != null) {
                selectorDrawable[1].setHotspot(x, y);
            }
        }
        return false;
    }

    private boolean checkOtherButtonMotionEvent(MotionEvent event) {
        if ((documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) && currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
            return false;
        }
        boolean allow = currentMessageObject.type == 16;
        if (!allow) {
            allow = !(documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT && currentMessageObject.type != 12 && documentAttachType != DOCUMENT_ATTACH_TYPE_MUSIC && documentAttachType != DOCUMENT_ATTACH_TYPE_VIDEO && documentAttachType != DOCUMENT_ATTACH_TYPE_GIF && currentMessageObject.type != 8 || hasGamePreview || hasInvoicePreview);
        }
        if (!allow) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentMessageObject.type == 16) {
                int idx = currentMessageObject.isVideoCall() ? 1 : 0;
                if (x >= otherX && x <= otherX + AndroidUtilities.dp(30 + (idx == 0 ? 202 : 200)) && y >= otherY - AndroidUtilities.dp(14) && y <= otherY + AndroidUtilities.dp(50)) {
                    otherPressed = true;
                    result = true;
                    selectorDrawableMaskType[0] = 4;
                    if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                        int cx = otherX + AndroidUtilities.dp(idx == 0 ? 202 : 200) + Theme.chat_msgInCallDrawable[idx].getIntrinsicWidth() / 2;
                        int cy = otherY + Theme.chat_msgInCallDrawable[idx].getIntrinsicHeight() / 2;
                        selectorDrawable[0].setBounds(cx - AndroidUtilities.dp(20), cy - AndroidUtilities.dp(20), cx + AndroidUtilities.dp(20), cy + AndroidUtilities.dp(20));
                        selectorDrawable[0].setHotspot(x, y);
                        selectorDrawable[0].setState(pressedState);
                    }
                    invalidate();
                }
            } else {
                if (x >= otherX - AndroidUtilities.dp(20) && x <= otherX + AndroidUtilities.dp(20) && y >= otherY - AndroidUtilities.dp(4) && y <= otherY + AndroidUtilities.dp(30)) {
                    otherPressed = true;
                    result = true;
                    invalidate();
                }
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (otherPressed) {
                    if (currentMessageObject.type == 16 && Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                        selectorDrawable[0].setState(StateSet.NOTHING);
                    }
                    otherPressed = false;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    delegate.didPressOther(this, otherX, otherY);
                    invalidate();
                    result = true;
                }
            } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                if (currentMessageObject.type == 16 && otherPressed && Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                    selectorDrawable[0].setHotspot(x, y);
                }
            }
        }
        return result;
    }

    private boolean checkDateMotionEvent(MotionEvent event) {
        if (!currentMessageObject.isImportedForward()) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (x >= drawTimeX && x <= drawTimeX + timeWidth && y >= drawTimeY && y <= drawTimeY + AndroidUtilities.dp(20)) {
                timePressed = true;
                result = true;
                invalidate();
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (timePressed) {
                    timePressed = false;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    delegate.didPressTime(this);
                    invalidate();
                    result = true;
                }
            }
        }
        return result;
    }

    private boolean checkRoundSeekbar(MotionEvent event) {
        if (!MediaController.getInstance().isPlayingMessage(currentMessageObject) || !MediaController.getInstance().isMessagePaused()) {
            return false;
        }
        int x = (int) event.getX();
        int y = (int) event.getY();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (x >= seekbarRoundX - AndroidUtilities.dp(20) && x <= seekbarRoundX + AndroidUtilities.dp(20) && y >= seekbarRoundY - AndroidUtilities.dp(20) && y <= seekbarRoundY + AndroidUtilities.dp(20)) {
                getParent().requestDisallowInterceptTouchEvent(true);
                cancelCheckLongPress();
                roundSeekbarTouched = 1;
                invalidate();
            } else {
                float localX = x - photoImage.getCenterX();
                float localY = y - photoImage.getCenterY();
                float r2 = (photoImage.getImageWidth() - AndroidUtilities.dp(64)) / 2;
                if (localX * localX + localY * localY < photoImage.getImageWidth() / 2 * photoImage.getImageWidth() / 2 && localX * localX + localY * localY > r2 * r2) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    cancelCheckLongPress();
                    roundSeekbarTouched = 1;
                    invalidate();
                }
            }
        } else if (roundSeekbarTouched == 1 && event.getAction() == MotionEvent.ACTION_MOVE) {
            float localX = x - photoImage.getCenterX();
            float localY = y - photoImage.getCenterY();
            float a = (float) Math.toDegrees(Math.atan2(localY, localX)) + 90;
            if (a < 0) {
                a += 360;
            }
            float p = a / 360f;
            if (Math.abs(currentMessageObject.audioProgress - p) > 0.9f) {
                if (roundSeekbarOutAlpha == 0) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
                roundSeekbarOutAlpha = 1f;
                roundSeekbarOutProgress = currentMessageObject.audioProgress;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastSeekUpdateTime > 100) {
                MediaController.getInstance().seekToProgress(currentMessageObject, p);
                lastSeekUpdateTime = currentTime;
            }
            currentMessageObject.audioProgress = p;
            updatePlayingMessageProgress();
        } if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
            if (roundSeekbarTouched != 0) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float localX = x - photoImage.getCenterX();
                    float localY = y - photoImage.getCenterY();
                    float a = (float) Math.toDegrees(Math.atan2(localY, localX)) + 90;
                    if (a < 0) {
                        a += 360;
                    }
                    float p = a / 360f;
                    currentMessageObject.audioProgress = p;
                    MediaController.getInstance().seekToProgress(currentMessageObject, p);
                    updatePlayingMessageProgress();
                }
                MediaController.getInstance().playMessage(currentMessageObject);
                roundSeekbarTouched = 0;
                getParent().requestDisallowInterceptTouchEvent(false);
            }
        }
        return roundSeekbarTouched != 0;
    }

    private boolean checkPhotoImageMotionEvent(MotionEvent event) {
        if (!drawPhotoImage && documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT || currentMessageObject.isSending() && buttonState != 1) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            boolean area2 = false;
            int side = AndroidUtilities.dp(48);

            if (miniButtonState >= 0) {
                int offset = AndroidUtilities.dp(27);
                area2 = x >= buttonX + offset && x <= buttonX + offset + side && y >= buttonY + offset && y <= buttonY + offset + side;
            }
            if (area2) {
                miniButtonPressed = 1;
                invalidate();
                result = true;
            } else if (buttonState != -1 && radialProgress.getIcon() != MediaActionDrawable.ICON_NONE && x >= buttonX && x <= buttonX + side && y >= buttonY && y <= buttonY + side) {
                buttonPressed = 1;
                invalidate();
                result = true;
            } else if (drawVideoImageButton && buttonState != -1 && x >= videoButtonX && x <= videoButtonX + AndroidUtilities.dp(26 + 8) + Math.max(infoWidth, docTitleWidth) && y >= videoButtonY && y <= videoButtonY + AndroidUtilities.dp(30)) {
                videoButtonPressed = 1;
                invalidate();
                result = true;
            } else {
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                    if (x >= photoImage.getImageX() && x <= photoImage.getImageX() + backgroundWidth - AndroidUtilities.dp(50) && y >= photoImage.getImageY() && y <= photoImage.getImageY() + photoImage.getImageHeight()) {
                        imagePressed = true;
                        result = true;
                    }
                } else if (!currentMessageObject.isAnyKindOfSticker() || currentMessageObject.getInputStickerSet() != null || currentMessageObject.isAnimatedEmoji() || currentMessageObject.isDice()) {
                    if (x >= photoImage.getImageX() && x <= photoImage.getImageX() + photoImage.getImageWidth() && y >= photoImage.getImageY() && y <= photoImage.getImageY() + photoImage.getImageHeight()) {
                        if (isRoundVideo) {
                            if ((x - photoImage.getCenterX()) * (x - photoImage.getCenterX()) + (y - photoImage.getCenterY()) * (y - photoImage.getCenterY()) < (photoImage.getImageWidth() / 2f) * (photoImage.getImageWidth() / 2)) {
                                imagePressed = true;
                                result = true;
                            }
                        } else {
                            imagePressed = true;
                            result = true;
                        }
                    }
                    if (currentMessageObject.type == 12) {
                        TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(currentMessageObject.messageOwner.media.user_id);
                        if (user == null) {
                            imagePressed = false;
                            result = false;
                        }
                    }
                }
            }
            if (imagePressed) {
                if (currentMessageObject.isSendError()) {
                    imagePressed = false;
                    result = false;
                } else if (currentMessageObject.type == 8 && buttonState == -1 && SharedConfig.autoplayGifs && photoImage.getAnimation() == null) {
                    imagePressed = false;
                    result = false;
                }
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (videoButtonPressed == 1) {
                    videoButtonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressButton(true, true);
                    invalidate();
                } else if (buttonPressed == 1) {
                    buttonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    if (drawVideoImageButton) {
                        didClickedImage();
                    } else {
                        didPressButton(true, false);
                    }
                    invalidate();
                } else if (miniButtonPressed == 1) {
                    miniButtonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressMiniButton(true);
                    invalidate();
                } else if (imagePressed) {
                    imagePressed = false;
                    if (buttonState == -1 || buttonState == 2 || buttonState == 3 || drawVideoImageButton) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                        didClickedImage();
                    } else if (buttonState == 0) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                        didPressButton(true, false);
                    }
                    invalidate();
                }
            }
        }
        return result;
    }

    private boolean checkAudioMotionEvent(MotionEvent event) {
        if (documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO && documentAttachType != DOCUMENT_ATTACH_TYPE_MUSIC) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();
        boolean result;
        if (useSeekBarWaweform) {
            result = seekBarWaveform.onTouch(event.getAction(), event.getX() - seekBarX - AndroidUtilities.dp(13), event.getY() - seekBarY);
        } else {
            if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                result = seekBar.onTouch(event.getAction(), event.getX() - seekBarX, event.getY() - seekBarY);
            } else {
                result = false;
            }
        }
        if (result) {
            if (!useSeekBarWaweform && event.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
            } else if (useSeekBarWaweform && !seekBarWaveform.isStartDraging() && event.getAction() == MotionEvent.ACTION_UP) {
                didPressButton(true, false);
            }
            disallowLongPress = true;
            invalidate();
        } else {
            int side = AndroidUtilities.dp(36);
            boolean area = false;
            boolean area2 = false;
            if (miniButtonState >= 0) {
                int offset = AndroidUtilities.dp(27);
                area2 = x >= buttonX + offset && x <= buttonX + offset + side && y >= buttonY + offset && y <= buttonY + offset + side;
            }
            if (!area2) {
                if (buttonState == 0 || buttonState == 1 || buttonState == 2) {
                    area = x >= buttonX - AndroidUtilities.dp(12) && x <= buttonX - AndroidUtilities.dp(12) + backgroundWidth && y >= (drawInstantView ? buttonY : namesOffset + mediaOffsetY) && y <= (drawInstantView ? buttonY + side : namesOffset + mediaOffsetY + AndroidUtilities.dp(82));
                } else {
                    area = x >= buttonX && x <= buttonX + side && y >= buttonY && y <= buttonY + side;
                }
            }
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (area || area2) {
                    if (area) {
                        buttonPressed = 1;
                    } else {
                        miniButtonPressed = 1;
                    }
                    invalidate();
                    result = true;
                }
            } else if (buttonPressed != 0) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    buttonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressButton(true, false);
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    buttonPressed = 0;
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!area) {
                        buttonPressed = 0;
                        invalidate();
                    }
                }
            } else if (miniButtonPressed != 0) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    miniButtonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressMiniButton(true);
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    miniButtonPressed = 0;
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!area2) {
                        miniButtonPressed = 0;
                        invalidate();
                    }
                }
            }
        }
        return result;
    }

    private boolean checkBotButtonMotionEvent(MotionEvent event) {
        if (botButtons.isEmpty() || currentMessageObject.eventId != 0) {
            return false;
        }

        int x = (int) event.getX();
        int y = (int) event.getY();

        boolean result = false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int addX;
            if (currentMessageObject.isOutOwner()) {
                addX = getMeasuredWidth() - widthForButtons - AndroidUtilities.dp(10);
            } else {
                addX = backgroundDrawableLeft + AndroidUtilities.dp(mediaBackground ? 1 : 7);
            }
            for (int a = 0; a < botButtons.size(); a++) {
                BotButton button = botButtons.get(a);
                int y2 = button.y + layoutHeight - AndroidUtilities.dp(2);
                if (x >= button.x + addX && x <= button.x + addX + button.width && y >= y2 && y <= y2 + button.height) {
                    pressedBotButton = a;
                    invalidate();
                    result = true;
                    break;
                }
            }
        } else {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (pressedBotButton != -1) {
                    playSoundEffect(SoundEffectConstants.CLICK);
                    if (currentMessageObject.scheduled) {
                        Toast.makeText(getContext(), LocaleController.getString("MessageScheduledBotAction", R.string.MessageScheduledBotAction), Toast.LENGTH_LONG).show();
                    } else {
                        BotButton button = botButtons.get(pressedBotButton);
                        if (button.button != null) {
                            delegate.didPressBotButton(this, button.button);
                        } else if (button.reaction != null) {
                            delegate.didPressReaction(this, button.reaction);
                        }
                    }
                    pressedBotButton = -1;
                    invalidate();
                }
            }
        }
        return result;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (currentMessageObject == null || !delegate.canPerformActions() || animationRunning) {
            checkTextSelection(event);
            return super.onTouchEvent(event);
        }

        if (checkTextSelection(event)) {
            return true;
        }

        if (checkRoundSeekbar(event)) {
            return true;
        }

        if (videoPlayerRewinder != null && videoPlayerRewinder.rewindCount > 0) {
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                getParent().requestDisallowInterceptTouchEvent(false);
                videoPlayerRewinder.cancelRewind();
                return false;
            }
            return true;
        }

        disallowLongPress = false;
        lastTouchX = event.getX();
        lastTouchY = event.getY();
        backgroundDrawable.setTouchCoords(lastTouchX, lastTouchY);

        boolean result = checkTextBlockMotionEvent(event);

        if (!result) {
            result = checkPinchToZoom(event);
        }
        if (!result) {
            result = checkDateMotionEvent(event);
        }
        if (!result) {
            result = checkTextSelection(event);
        }
        if (!result) {
            result = checkOtherButtonMotionEvent(event);
        }
        if (!result) {
            result = checkCaptionMotionEvent(event);
        }
        if (!result) {
            result = checkAudioMotionEvent(event);
        }
        if (!result) {
            result = checkLinkPreviewMotionEvent(event);
        }
        if (!result) {
            result = checkInstantButtonMotionEvent(event);
        }
        if (!result) {
            result = checkCommentButtonMotionEvent(event);
        }
        if (!result) {
            result = checkGameMotionEvent(event);
        }
        if (!result) {
            result = checkPhotoImageMotionEvent(event);
        }
        if (!result) {
            result = checkBotButtonMotionEvent(event);
        }
        if (!result) {
            result = checkPollButtonMotionEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            buttonPressed = 0;
            miniButtonPressed = 0;
            pressedBotButton = -1;
            pressedVoteButton = -1;
            pollHintPressed = false;
            psaHintPressed = false;
            linkPreviewPressed = false;
            otherPressed = false;
            sideButtonPressed = false;
            imagePressed = false;
            timePressed = false;
            gamePreviewPressed = false;
            instantPressed = instantButtonPressed = commentButtonPressed = false;
            if (Build.VERSION.SDK_INT >= 21) {
                for (int a = 0; a < selectorDrawable.length; a++) {
                    if (selectorDrawable[a] != null) {
                        selectorDrawable[a].setState(StateSet.NOTHING);
                    }
                }
            }
            result = false;
            resetPressedLink(-1);
        }
        updateRadialProgressBackground();
        if (!disallowLongPress && result && event.getAction() == MotionEvent.ACTION_DOWN) {
            startCheckLongPress();
        }

        if (event.getAction() != MotionEvent.ACTION_DOWN && event.getAction() != MotionEvent.ACTION_MOVE) {
            cancelCheckLongPress();
        }

        if (!result) {
            float x = event.getX();
            float y = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (delegate == null || delegate.canPerformActions()) {
                    if (isAvatarVisible && avatarImage.isInsideImage(x, y + getTop())) {
                        avatarPressed = true;
                        result = true;
                    } else if (psaButtonVisible && hasPsaHint && x >= psaHelpX && x <= psaHelpX + AndroidUtilities.dp(40) && y >= psaHelpY && y <= psaHelpY + AndroidUtilities.dp(40)) {
                        psaHintPressed = true;
                        createSelectorDrawable(0);
                        selectorDrawableMaskType[0] = 3;
                        if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                            selectorDrawable[0].setBounds(psaHelpX - AndroidUtilities.dp(8), psaHelpY - AndroidUtilities.dp(8), psaHelpX + AndroidUtilities.dp(32), psaHelpY + AndroidUtilities.dp(32));
                            selectorDrawable[0].setHotspot(x, y);
                            selectorDrawable[0].setState(pressedState);
                        }
                        result = true;
                        invalidate();
                    } else if (drawForwardedName && forwardedNameLayout[0] != null && x >= forwardNameX && x <= forwardNameX + forwardedNameWidth && y >= forwardNameY && y <= forwardNameY + AndroidUtilities.dp(32)) {
                        if (viaWidth != 0 && x >= forwardNameX + viaNameWidth + AndroidUtilities.dp(4)) {
                            forwardBotPressed = true;
                        } else {
                            forwardNamePressed = true;
                        }
                        result = true;
                    } else if (drawNameLayout && nameLayout != null && viaWidth != 0 && x >= nameX + viaNameWidth && x <= nameX + viaNameWidth + viaWidth && y >= nameY - AndroidUtilities.dp(4) && y <= nameY + AndroidUtilities.dp(20)) {
                        forwardBotPressed = true;
                        result = true;
                    } else if (drawSideButton != 0 && x >= sideStartX && x <= sideStartX + AndroidUtilities.dp(40) && y >= sideStartY && y <= sideStartY + AndroidUtilities.dp(32 + (drawSideButton == 3 && commentLayout != null ? 18 : 0))) {
                        if (currentMessageObject.isSent()) {
                            sideButtonPressed = true;
                        }
                        result = true;
                        invalidate();
                    } else if (replyNameLayout != null) {
                        int replyEnd;
                        if (currentMessageObject.shouldDrawWithoutBackground()) {
                            replyEnd = replyStartX + Math.max(replyNameWidth, replyTextWidth);
                        } else {
                            replyEnd = replyStartX + backgroundDrawableRight;
                        }
                        if (x >= replyStartX && x <= replyEnd && y >= replyStartY && y <= replyStartY + AndroidUtilities.dp(35)) {
                            replyPressed = true;
                            result = true;
                        }
                    }
                    if (result) {
                        startCheckLongPress();
                    }
                }
            } else {
                if (event.getAction() != MotionEvent.ACTION_MOVE) {
                    cancelCheckLongPress();
                }
                if (avatarPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        avatarPressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            if (currentUser != null) {
                                if (currentUser.id == 0) {
                                    delegate.didPressHiddenForward(this);
                                } else {
                                    delegate.didPressUserAvatar(this, currentUser, lastTouchX, lastTouchY);
                                }
                            } else if (currentChat != null) {
                                int id;
                                TLRPC.Chat chat = currentChat;
                                if (currentMessageObject.messageOwner.fwd_from != null) {
                                    if ((currentMessageObject.messageOwner.fwd_from.flags & 16) != 0) {
                                        id = currentMessageObject.messageOwner.fwd_from.saved_from_msg_id;
                                    } else {
                                        id = currentMessageObject.messageOwner.fwd_from.channel_post;
                                        chat = currentForwardChannel;
                                    }
                                } else {
                                    id = 0;
                                }
                                delegate.didPressChannelAvatar(this, chat != null ? chat : currentChat, id, lastTouchX, lastTouchY);
                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        avatarPressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (isAvatarVisible && !avatarImage.isInsideImage(x, y + getTop())) {
                            avatarPressed = false;
                        }
                    }
                } else if (psaHintPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        playSoundEffect(SoundEffectConstants.CLICK);
                        delegate.didPressHint(this, 1);
                        psaHintPressed = false;
                        if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null) {
                            selectorDrawable[0].setState(StateSet.NOTHING);
                        }
                        invalidate();
                    }
                } else if (forwardNamePressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        forwardNamePressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            if (currentForwardChannel != null) {
                                delegate.didPressChannelAvatar(this, currentForwardChannel, currentMessageObject.messageOwner.fwd_from.channel_post, lastTouchX, lastTouchY);
                            } else if (currentForwardUser != null) {
                                delegate.didPressUserAvatar(this, currentForwardUser, lastTouchX, lastTouchY);
                            } else if (currentForwardName != null) {
                                delegate.didPressHiddenForward(this);
                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        forwardNamePressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (!(x >= forwardNameX && x <= forwardNameX + forwardedNameWidth && y >= forwardNameY && y <= forwardNameY + AndroidUtilities.dp(32))) {
                            forwardNamePressed = false;
                        }
                    }
                } else if (forwardBotPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        forwardBotPressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            delegate.didPressViaBot(this, currentViaBotUser != null ? currentViaBotUser.username : currentMessageObject.messageOwner.via_bot_name);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        forwardBotPressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (drawForwardedName && forwardedNameLayout[0] != null) {
                            if (!(x >= forwardNameX && x <= forwardNameX + forwardedNameWidth && y >= forwardNameY && y <= forwardNameY + AndroidUtilities.dp(32))) {
                                forwardBotPressed = false;
                            }
                        } else {
                            if (!(x >= nameX + viaNameWidth && x <= nameX + viaNameWidth + viaWidth && y >= nameY - AndroidUtilities.dp(4) && y <= nameY + AndroidUtilities.dp(20))) {
                                forwardBotPressed = false;
                            }
                        }
                    }
                } else if (replyPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        replyPressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (replyPanelIsForward) {
                            if (delegate != null) {
                                if (currentForwardChannel != null) {
                                    delegate.didPressChannelAvatar(this, currentForwardChannel, currentMessageObject.messageOwner.fwd_from.channel_post, lastTouchX, lastTouchY);
                                } else if (currentForwardUser != null) {
                                    delegate.didPressUserAvatar(this, currentForwardUser, lastTouchX, lastTouchY);
                                } else if (currentForwardName != null) {
                                    delegate.didPressHiddenForward(this);
                                }
                            }
                        } else {
                            if (delegate != null && currentMessageObject.hasValidReplyMessageObject()) {
                                delegate.didPressReplyMessage(this, currentMessageObject.getReplyMsgId());
                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        replyPressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        int replyEnd;
                        if (currentMessageObject.shouldDrawWithoutBackground()) {
                            replyEnd = replyStartX + Math.max(replyNameWidth, replyTextWidth);
                        } else {
                            replyEnd = replyStartX + backgroundDrawableRight;
                        }
                        if (!(x >= replyStartX && x <= replyEnd && y >= replyStartY && y <= replyStartY + AndroidUtilities.dp(35))) {
                            replyPressed = false;
                        }
                    }
                } else if (sideButtonPressed) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        sideButtonPressed = false;
                        playSoundEffect(SoundEffectConstants.CLICK);
                        if (delegate != null) {
                            if (drawSideButton == 3) {
                                delegate.didPressCommentButton(this);
                            } else {
                                delegate.didPressSideButton(this);
                            }
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        sideButtonPressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        if (!(x >= sideStartX && x <= sideStartX + AndroidUtilities.dp(40) && y >= sideStartY && y <= sideStartY + AndroidUtilities.dp(32 + (drawSideButton == 3 && commentLayout != null ? 18 : 0)))) {
                            sideButtonPressed = false;
                        }
                    }
                    invalidate();
                }
            }
        }
        return result;
    }


    private boolean checkPinchToZoom(MotionEvent ev) {
        PinchToZoomHelper pinchToZoomHelper = delegate == null ? null : delegate.getPinchToZoomHelper();
        if (currentMessageObject == null || !photoImage.hasNotThumb() || pinchToZoomHelper == null || currentMessageObject.isSticker() ||
                currentMessageObject.isAnimatedEmoji() || (currentMessageObject.isVideo() && !autoPlayingMedia) ||
                isRoundVideo || currentMessageObject.isAnimatedSticker() || (currentMessageObject.isDocument() && !currentMessageObject.isGif()) || currentMessageObject.needDrawBluredPreview()) {
            return false;
        }
        return pinchToZoomHelper.checkPinchToZoom(ev, this, photoImage, currentMessageObject);
    }

    private boolean checkTextSelection(MotionEvent event) {
        TextSelectionHelper.ChatListTextSelectionHelper textSelectionHelper = delegate.getTextSelectionHelper();
        if (textSelectionHelper == null) {
            return false;
        }
        boolean hasTextBlocks = currentMessageObject.textLayoutBlocks != null && !currentMessageObject.textLayoutBlocks.isEmpty();
        if (!hasTextBlocks && !hasCaptionLayout()) {
            return false;
        }

        if ((!drawSelectionBackground && currentMessagesGroup == null) || (currentMessagesGroup != null && !delegate.hasSelectedMessages())) {
            return false;
        }

        if (currentMessageObject.hasValidGroupId() && currentMessagesGroup != null && !currentMessagesGroup.isDocuments) {
            ViewGroup parent = (ViewGroup) getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View v = parent.getChildAt(i);
                if (v instanceof ChatMessageCell) {
                    ChatMessageCell cell = (ChatMessageCell) v;
                    MessageObject.GroupedMessages group = cell.getCurrentMessagesGroup();
                    MessageObject.GroupedMessagePosition position = cell.getCurrentPosition();
                    if (group != null && group.groupId == currentMessagesGroup.groupId &&
                            (position.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0 &&
                            (position.flags & MessageObject.POSITION_FLAG_LEFT) != 0) {
                        textSelectionHelper.setMaybeTextCord((int) cell.captionX, (int) cell.captionY);
                        textSelectionHelper.setMessageObject(cell);
                        if (cell == this) {
                            return textSelectionHelper.onTouchEvent(event);
                        } else {
                            event.offsetLocation(this.getLeft() - cell.getLeft(), this.getTop() - cell.getTop());
                            boolean result = textSelectionHelper.onTouchEvent(event);
                            event.offsetLocation(-(this.getLeft() - cell.getLeft()), -(this.getTop() - cell.getTop()));
                            return result;
                        }
                    }
                }
            }
            return false;
        } else {
            if (hasCaptionLayout()) {
                textSelectionHelper.setIsDescription(false);
                textSelectionHelper.setMaybeTextCord((int) captionX, (int) captionY);
            } else {
                if (descriptionLayout != null && event.getY() > descriptionY) {
                    textSelectionHelper.setIsDescription(true);
                    int linkX;
                    if (hasGamePreview) {
                        linkX = unmovedTextX - AndroidUtilities.dp(10);
                    } else if (hasInvoicePreview) {
                        linkX = unmovedTextX + AndroidUtilities.dp(1);
                    } else {
                        linkX = unmovedTextX + AndroidUtilities.dp(1);
                    }
                    textSelectionHelper.setMaybeTextCord(linkX + AndroidUtilities.dp(10) + descriptionX, descriptionY);
                } else {
                    textSelectionHelper.setIsDescription(false);
                    textSelectionHelper.setMaybeTextCord(textX, textY);
                }
            }
            textSelectionHelper.setMessageObject(this);
        }

        return textSelectionHelper.onTouchEvent(event);
    }

    private void updateSelectionTextPosition() {
        if (getDelegate().getTextSelectionHelper() != null && getDelegate().getTextSelectionHelper().isSelected(currentMessageObject)) {
            int textSelectionType = getDelegate().getTextSelectionHelper().getTextSelectionType(this);
            if (textSelectionType == TextSelectionHelper.ChatListTextSelectionHelper.TYPE_DESCRIPTION) {
                int linkX;
                if (hasGamePreview) {
                    linkX = unmovedTextX - AndroidUtilities.dp(10);
                } else if (hasInvoicePreview) {
                    linkX = unmovedTextX + AndroidUtilities.dp(1);
                } else {
                    linkX = unmovedTextX + AndroidUtilities.dp(1);
                }
                getDelegate().getTextSelectionHelper().updateTextPosition(linkX + AndroidUtilities.dp(10) + descriptionX, descriptionY);
            } else if (textSelectionType == TextSelectionHelper.ChatListTextSelectionHelper.TYPE_CAPTION) {
                getDelegate().getTextSelectionHelper().updateTextPosition((int) captionX, (int) captionY);
            } else {
                getDelegate().getTextSelectionHelper().updateTextPosition(textX, textY);
            }
        }
    }

    public ArrayList<PollButton> getPollButtons() {
        return pollButtons;
    }

    public void updatePlayingMessageProgress() {
        if (currentMessageObject == null) {
            return;
        }
        if (videoPlayerRewinder != null && videoPlayerRewinder.rewindCount != 0 && videoPlayerRewinder.rewindByBackSeek) {
            currentMessageObject.audioProgress = videoPlayerRewinder.getVideoProgress();
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
            if (infoLayout != null && (PhotoViewer.isPlayingMessage(currentMessageObject) || MediaController.getInstance().isGoingToShowMessageObject(currentMessageObject))) {
                return;
            }
            int duration = 0;
            AnimatedFileDrawable animation = photoImage.getAnimation();
            if (animation != null) {
                duration = currentMessageObject.audioPlayerDuration = animation.getDurationMs() / 1000;
                if (currentMessageObject.messageOwner.ttl > 0 && currentMessageObject.messageOwner.destroyTime == 0 && !currentMessageObject.needDrawBluredPreview() && currentMessageObject.isVideo() && animation.hasBitmap()) {
                    delegate.didStartVideoStream(currentMessageObject);
                }
            }
            if (duration == 0) {
                duration = currentMessageObject.getDuration();
            }
            if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                duration -= duration * currentMessageObject.audioProgress;
            } else if (animation != null) {
                if (duration != 0) {
                    duration -= animation.getCurrentProgressMs() / 1000;
                }
                if (delegate != null && animation.getCurrentProgressMs() >= 3000) {
                    delegate.videoTimerReached();
                }
            }
            if (lastTime != duration) {
                String str = AndroidUtilities.formatShortDuration(duration);
                infoWidth = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
                infoLayout = new StaticLayout(str, Theme.chat_infoPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                lastTime = duration;
            }
        } else if (isRoundVideo) {
            int duration = 0;
            TLRPC.Document document = currentMessageObject.getDocument();
            for (int a = 0; a < document.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                    duration = attribute.duration;
                    break;
                }
            }
            if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                duration = Math.max(0, duration - currentMessageObject.audioProgressSec);
            }
            if (lastTime != duration) {
                lastTime = duration;
                String timeString = AndroidUtilities.formatLongDuration(duration);
                timeWidthAudio = (int) Math.ceil(Theme.chat_timePaint.measureText(timeString));
                durationLayout = new StaticLayout(timeString, Theme.chat_timePaint, timeWidthAudio, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            if (currentMessageObject.audioProgress != 0) {
                lastDrawingAudioProgress = currentMessageObject.audioProgress;
                if (lastDrawingAudioProgress > 0.9f) {
                    lastDrawingAudioProgress = 1f;
                }
            }
            invalidate();
        } else if (documentAttach != null) {
            if (useSeekBarWaweform) {
                if (!seekBarWaveform.isDragging()) {
                    seekBarWaveform.setProgress(currentMessageObject.audioProgress, true);
                }
            } else {
                if (!seekBar.isDragging()) {
                    seekBar.setProgress(currentMessageObject.audioProgress);
                    seekBar.setBufferedProgress(currentMessageObject.bufferedProgress);
                }
            }

            int duration = 0;
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
                if (!MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                    for (int a = 0; a < documentAttach.attributes.size(); a++) {
                        TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                        if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                            duration = attribute.duration;
                            break;
                        }
                    }
                } else {
                    duration = currentMessageObject.audioProgressSec;
                }

                if (lastTime != duration) {
                    lastTime = duration;
                    String timeString = AndroidUtilities.formatLongDuration(duration);
                    timeWidthAudio = (int) Math.ceil(Theme.chat_audioTimePaint.measureText(timeString));
                    durationLayout = new StaticLayout(timeString, Theme.chat_audioTimePaint, timeWidthAudio, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                }
            } else {
                int currentProgress = 0;
                duration = currentMessageObject.getDuration();
                if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                    currentProgress = currentMessageObject.audioProgressSec;
                }
                if (lastTime != currentProgress) {
                    lastTime = currentProgress;
                    String timeString = AndroidUtilities.formatShortDuration(currentProgress, duration);
                    int timeWidth = (int) Math.ceil(Theme.chat_audioTimePaint.measureText(timeString));
                    durationLayout = new StaticLayout(timeString, Theme.chat_audioTimePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                }
            }
            invalidate();
        }
    }

    public void setFullyDraw(boolean draw) {
        fullyDraw = draw;
    }

    public void setParentViewSize(int parentW, int parentH) {
        parentWidth = parentW;
        parentHeight = parentH;
        backgroundHeight = parentH;

        if (currentMessageObject != null && (hasGradientService() && currentMessageObject.shouldDrawWithoutBackground() || drawSideButton != 0 || !botButtons.isEmpty()) || currentBackgroundDrawable != null && currentBackgroundDrawable.getGradientShader() != null) {
            invalidate();
        }
    }

    public void setVisiblePart(int position, int height, int parent, float parentOffset, float visibleTop, int parentW, int parentH) {
        parentWidth = parentW;
        parentHeight = parentH;
        backgroundHeight = parentH;
        viewTop = visibleTop;
        if (parent != parentHeight || parentOffset != this.parentViewTopOffset) {
            this.parentViewTopOffset = parentOffset;
            parentHeight = parent;
        }
        if (currentMessageObject != null && (hasGradientService() && currentMessageObject.shouldDrawWithoutBackground() || drawSideButton != 0 || !botButtons.isEmpty()) ) {
            invalidate();
        }

        if (currentMessageObject == null || currentMessageObject.textLayoutBlocks == null) {
            return;
        }
        position -= textY;

        int newFirst = -1, newLast = -1, newCount = 0;

        int startBlock = 0;
        for (int a = 0; a < currentMessageObject.textLayoutBlocks.size(); a++) {
            if (currentMessageObject.textLayoutBlocks.get(a).textYOffset > position) {
                break;
            }
            startBlock = a;
        }

        for (int a = startBlock; a < currentMessageObject.textLayoutBlocks.size(); a++) {
            MessageObject.TextLayoutBlock block = currentMessageObject.textLayoutBlocks.get(a);
            float y = block.textYOffset;
            if (intersect(y, y + block.height, position, position + height)) {
                if (newFirst == -1) {
                    newFirst = a;
                }
                newLast = a;
                newCount++;
            } else if (y > position) {
                break;
            }
        }

        if (lastVisibleBlockNum != newLast || firstVisibleBlockNum != newFirst || totalVisibleBlocksCount != newCount) {
            lastVisibleBlockNum = newLast;
            firstVisibleBlockNum = newFirst;
            totalVisibleBlocksCount = newCount;
            invalidate();
        }
    }

    private boolean intersect(float left1, float right1, float left2, float right2) {
        if (left1 <= left2) {
            return right1 >= left2;
        }
        return left1 <= right2;
    }

    public static StaticLayout generateStaticLayout(CharSequence text, TextPaint paint, int maxWidth, int smallWidth, int linesCount, int maxLines) {
        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(text);
        int addedChars = 0;
        StaticLayout layout = new StaticLayout(text, paint, smallWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        for (int a = 0; a < linesCount; a++) {
            Layout.Directions directions = layout.getLineDirections(a);
            if (layout.getLineLeft(a) != 0 || layout.isRtlCharAt(layout.getLineStart(a)) || layout.isRtlCharAt(layout.getLineEnd(a))) {
                maxWidth = smallWidth;
            }
            int pos = layout.getLineEnd(a);
            if (pos == text.length()) {
                break;
            }
            pos--;
            if (stringBuilder.charAt(pos + addedChars) == ' ') {
                stringBuilder.replace(pos + addedChars, pos + addedChars + 1, "\n");
            } else if (stringBuilder.charAt(pos + addedChars) != '\n') {
                stringBuilder.insert(pos + addedChars, "\n");
                addedChars++;
            }
            if (a == layout.getLineCount() - 1 || a == maxLines - 1) {
                break;
            }
        }
        return StaticLayoutEx.createStaticLayout(stringBuilder, paint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, AndroidUtilities.dp(1), false, TextUtils.TruncateAt.END, maxWidth, maxLines, true);
    }

    private void didClickedImage() {
        if (currentMessageObject.type == MessageObject.TYPE_PHOTO || currentMessageObject.isAnyKindOfSticker()) {
            if (buttonState == -1) {
                delegate.didPressImage(this, lastTouchX, lastTouchY);
            } else if (buttonState == 0) {
                didPressButton(true, false);
            }
        } else if (currentMessageObject.type == 12) {
            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(currentMessageObject.messageOwner.media.user_id);
            delegate.didPressUserAvatar(this, user, lastTouchX, lastTouchY);
        } else if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
            if (buttonState != -1) {
                didPressButton(true, false);
            } else {
                if (!MediaController.getInstance().isPlayingMessage(currentMessageObject) || MediaController.getInstance().isMessagePaused()) {
                    delegate.needPlayMessage(currentMessageObject);
                } else {
                    MediaController.getInstance().pauseMessage(currentMessageObject);
                }
            }
        } else if (currentMessageObject.type == 8) {
            if (buttonState == -1 || buttonState == 1 && canStreamVideo && autoPlayingMedia) {
                //if (SharedConfig.autoplayGifs) {
                delegate.didPressImage(this, lastTouchX, lastTouchY);
                /*} else {
                    buttonState = 2;
                    currentMessageObject.gifState = 1;
                    photoImage.setAllowStartAnimation(false);
                    photoImage.stopAnimation();
                    radialProgress.setIcon(getIconForCurrentState(), false, true);
                    invalidate();
                }*/
            } else if (buttonState == 2 || buttonState == 0) {
                didPressButton(true, false);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
            if (buttonState == -1 || drawVideoImageButton && (autoPlayingMedia || SharedConfig.streamMedia && canStreamVideo)) {
                delegate.didPressImage(this, lastTouchX, lastTouchY);
            } else if (drawVideoImageButton) {
                didPressButton(true, true);
            } else if (buttonState == 0 || buttonState == 3) {
                didPressButton(true, false);
            }
        } else if (currentMessageObject.type == 4) {
            delegate.didPressImage(this, lastTouchX, lastTouchY);
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
            if (buttonState == -1) {
                delegate.didPressImage(this, lastTouchX, lastTouchY);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
            if (buttonState == -1) {
                TLRPC.WebPage webPage = currentMessageObject.messageOwner.media.webpage;
                if (webPage != null) {
                    if (webPage.embed_url != null && webPage.embed_url.length() != 0) {
                        delegate.needOpenWebView(currentMessageObject, webPage.embed_url, webPage.site_name, webPage.description, webPage.url, webPage.embed_width, webPage.embed_height);
                    } else {
                        Browser.openUrl(getContext(), webPage.url);
                    }
                }
            }
        } else if (hasInvoicePreview) {
            if (buttonState == -1) {
                delegate.didPressImage(this, lastTouchX, lastTouchY);
            }
        }
    }

    private void updateSecretTimeText(MessageObject messageObject) {
        if (messageObject == null || !messageObject.needDrawBluredPreview()) {
            return;
        }
        String str = messageObject.getSecretTimeString();
        if (str == null) {
            return;
        }
        infoWidth = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
        CharSequence str2 = TextUtils.ellipsize(str, Theme.chat_infoPaint, infoWidth, TextUtils.TruncateAt.END);
        infoLayout = new StaticLayout(str2, Theme.chat_infoPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        invalidate();
    }

    private boolean isPhotoDataChanged(MessageObject object) {
        if (object.type == 0 || object.type == 14) {
            return false;
        }
        if (object.type == 4) {
            if (currentUrl == null) {
                return true;
            }
            double lat = object.messageOwner.media.geo.lat;
            double lon = object.messageOwner.media.geo._long;
            String url;
            int provider;
            if ((int) object.getDialogId() == 0) {
                if (SharedConfig.mapPreviewType == 0) {
                    provider = -1;
                } else if (SharedConfig.mapPreviewType == 1) {
                    provider = 4;
                } else if (SharedConfig.mapPreviewType == 3) {
                    provider = 1;
                } else {
                    provider = -1;
                }
            } else {
                provider = -1;
            }
            if (object.messageOwner.media instanceof TLRPC.TL_messageMediaGeoLive) {
                int photoWidth = backgroundWidth - AndroidUtilities.dp(21);
                int photoHeight = AndroidUtilities.dp(195);

                int offset = 268435456;
                double rad = offset / Math.PI;
                double y = Math.round(offset - rad * Math.log((1 + Math.sin(lat * Math.PI / 180.0)) / (1 - Math.sin(lat * Math.PI / 180.0))) / 2) - (AndroidUtilities.dp(10.3f) << (21 - 15));
                lat = (Math.PI / 2.0 - 2 * Math.atan(Math.exp((y - offset) / rad))) * 180.0 / Math.PI;
                url = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), false, 15, provider);
            } else if (!TextUtils.isEmpty(object.messageOwner.media.title)) {
                int photoWidth = backgroundWidth - AndroidUtilities.dp(21);
                int photoHeight = AndroidUtilities.dp(195);
                url = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), true, 15, provider);
            } else {
                int photoWidth = backgroundWidth - AndroidUtilities.dp(12);
                int photoHeight = AndroidUtilities.dp(195);
                url = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), true, 15, provider);
            }
            return !url.equals(currentUrl);
        } else if (currentPhotoObject == null || currentPhotoObject.location instanceof TLRPC.TL_fileLocationUnavailable) {
            return object.type == MessageObject.TYPE_PHOTO || object.type == MessageObject.TYPE_ROUND_VIDEO || object.type == MessageObject.TYPE_VIDEO || object.type == 8 || object.isAnyKindOfSticker();
        } else if (currentMessageObject != null && photoNotSet) {
            File cacheFile = FileLoader.getPathToMessage(currentMessageObject.messageOwner);
            return cacheFile.exists();
        }
        return false;
    }

    private int getRepliesCount() {
        if (currentMessagesGroup != null && !currentMessagesGroup.messages.isEmpty()) {
            MessageObject messageObject = currentMessagesGroup.messages.get(0);
            return messageObject.getRepliesCount();
        }
        return currentMessageObject.getRepliesCount();
    }

    private ArrayList<TLRPC.Peer> getRecentRepliers() {
        if (currentMessagesGroup != null && !currentMessagesGroup.messages.isEmpty()) {
            MessageObject messageObject = currentMessagesGroup.messages.get(0);
            if (messageObject.messageOwner.replies != null) {
                return messageObject.messageOwner.replies.recent_repliers;
            }
        }
        if (currentMessageObject.messageOwner.replies != null) {
            return currentMessageObject.messageOwner.replies.recent_repliers;
        }
        return null;
    }

    private boolean isUserDataChanged() {
        if (currentMessageObject != null && (!hasLinkPreview && currentMessageObject.messageOwner.media != null && currentMessageObject.messageOwner.media.webpage instanceof TLRPC.TL_webPage)) {
            return true;
        }
        if (currentMessageObject == null || currentUser == null && currentChat == null) {
            return false;
        }
        if (lastSendState != currentMessageObject.messageOwner.send_state) {
            return true;
        }
        if (lastDeleteDate != currentMessageObject.messageOwner.destroyTime) {
            return true;
        }
        if (lastViewsCount != currentMessageObject.messageOwner.views) {
            return true;
        }
        if (lastRepliesCount != getRepliesCount()) {
            return true;
        }
        if (lastReactions != currentMessageObject.messageOwner.reactions) {
            return true;
        }

        updateCurrentUserAndChat();
        TLRPC.FileLocation newPhoto = null;

        if (isAvatarVisible) {
            if (currentUser != null && currentUser.photo != null) {
                newPhoto = currentUser.photo.photo_small;
            } else if (currentChat != null && currentChat.photo != null) {
                newPhoto = currentChat.photo.photo_small;
            }
        }

        if (replyTextLayout == null && currentMessageObject.replyMessageObject != null) {
            if (!isThreadChat || currentMessageObject.replyMessageObject.messageOwner.fwd_from == null || currentMessageObject.replyMessageObject.messageOwner.fwd_from.channel_post == 0) {
                return true;
            }
        }

        if (currentPhoto == null && newPhoto != null || currentPhoto != null && newPhoto == null || currentPhoto != null && (currentPhoto.local_id != newPhoto.local_id || currentPhoto.volume_id != newPhoto.volume_id)) {
            return true;
        }

        TLRPC.PhotoSize newReplyPhoto = null;

        if (replyNameLayout != null && currentMessageObject.replyMessageObject != null) {
            TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(currentMessageObject.replyMessageObject.photoThumbs, 40);
            if (photoSize != null && !currentMessageObject.replyMessageObject.isAnyKindOfSticker()) {
                newReplyPhoto = photoSize;
            }
        }

        if (currentReplyPhoto == null && newReplyPhoto != null) {
            return true;
        }

        String newNameString = isNeedAuthorName() ? getAuthorName() : null;
        if (currentNameString == null && newNameString != null || currentNameString != null && newNameString == null || currentNameString != null && !currentNameString.equals(newNameString)) {
            return true;
        }

        if (drawForwardedName && currentMessageObject.needDrawForwarded()) {
            newNameString = currentMessageObject.getForwardedName();
            return currentForwardNameString == null && newNameString != null || currentForwardNameString != null && newNameString == null || currentForwardNameString != null && !currentForwardNameString.equals(newNameString);
        }
        return false;
    }

    public ImageReceiver getPhotoImage() {
        return photoImage;
    }

    public int getNoSoundIconCenterX() {
        return noSoundCenterX;
    }

    public int getForwardNameCenterX() {
        if (currentUser != null && currentUser.id == 0) {
            return (int) avatarImage.getCenterX();
        }
        return (int) (forwardNameX + forwardNameCenterX);
    }

    public int getChecksX() {
        return layoutWidth - AndroidUtilities.dp(SharedConfig.bubbleRadius >= 10 ? 27.3f : 25.3f);
    }

    public int getChecksY() {
        if (currentMessageObject.shouldDrawWithoutBackground()) {
            return (int) (drawTimeY - getThemedDrawable(Theme.key_drawable_msgStickerCheck).getIntrinsicHeight());
        } else {
            return (int) (drawTimeY - Theme.chat_msgMediaCheckDrawable.getIntrinsicHeight());
        }
    }

    public TLRPC.User getCurrentUser() {
        return currentUser;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelShakeAnimation();
        if (animationRunning) {
            return;
        }
        if (checkBox != null) {
            checkBox.onDetachedFromWindow();
        }
        if (mediaCheckBox != null) {
            mediaCheckBox.onDetachedFromWindow();
        }
        if (pollCheckBox != null) {
            for (int a = 0; a < pollCheckBox.length; a++) {
                pollCheckBox[a].onDetachedFromWindow();
            }
        }
        attachedToWindow = false;
        radialProgress.onDetachedFromWindow();
        videoRadialProgress.onDetachedFromWindow();
        avatarImage.onDetachedFromWindow();
        if (pollAvatarImages != null) {
            for (int a = 0; a < pollAvatarImages.length; a++) {
                pollAvatarImages[a].onDetachedFromWindow();
            }
        }
        if (commentAvatarImages != null) {
            for (int a = 0; a < commentAvatarImages.length; a++) {
                commentAvatarImages[a].onDetachedFromWindow();
            }
        }
        replyImageReceiver.onDetachedFromWindow();
        locationImageReceiver.onDetachedFromWindow();
        photoImage.onDetachedFromWindow();
        if (addedForTest && currentUrl != null && currentWebFile != null) {
            ImageLoader.getInstance().removeTestWebFile(currentUrl);
            addedForTest = false;
        }
        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);

        if (getDelegate().getTextSelectionHelper() != null) {
            getDelegate().getTextSelectionHelper().onChatMessageCellDetached(this);
        }

        transitionParams.onDetach();
        if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
            Theme.getCurrentAudiVisualizerDrawable().setParentView(null);
        }

        if (statusDrawableAnimator != null) {
            statusDrawableAnimator.removeAllListeners();
            statusDrawableAnimator.cancel();
        }
        statusDrawableAnimationInProgress = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (currentMessageObject != null) {
            currentMessageObject.animateComments = false;
        }
        if (messageObjectToSet != null) {
            messageObjectToSet.animateComments = false;
            setMessageContent(messageObjectToSet, groupedMessagesToSet, bottomNearToSet, topNearToSet);
            messageObjectToSet = null;
            groupedMessagesToSet = null;
        }
        if (checkBox != null) {
            checkBox.onAttachedToWindow();
        }
        if (mediaCheckBox != null) {
            mediaCheckBox.onAttachedToWindow();
        }
        if (pollCheckBox != null) {
            for (int a = 0; a < pollCheckBox.length; a++) {
                pollCheckBox[a].onAttachedToWindow();
            }
        }

        attachedToWindow = true;

        animationOffsetX = 0;
        slidingOffsetX = 0;
        checkBoxTranslation = 0;
        updateTranslation();

        radialProgress.onAttachedToWindow();
        videoRadialProgress.onAttachedToWindow();
        avatarImage.onAttachedToWindow();
        avatarImage.setParentView((View) getParent());
        if (pollAvatarImages != null) {
            for (int a = 0; a < pollAvatarImages.length; a++) {
                pollAvatarImages[a].onAttachedToWindow();
            }
        }
        if (commentAvatarImages != null) {
            for (int a = 0; a < commentAvatarImages.length; a++) {
                commentAvatarImages[a].onAttachedToWindow();
            }
        }
        replyImageReceiver.onAttachedToWindow();
        locationImageReceiver.onAttachedToWindow();
        if (photoImage.onAttachedToWindow()) {
            if (drawPhotoImage) {
                updateButtonState(false, false, false);
            }
        } else {
            updateButtonState(false, false, false);
        }
        if (currentMessageObject != null && (isRoundVideo || currentMessageObject.isVideo())) {
            checkVideoPlayback(true, null);
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO && autoPlayingMedia) {
            animatingNoSoundPlaying = MediaController.getInstance().isPlayingMessage(currentMessageObject);
            animatingNoSoundProgress = animatingNoSoundPlaying ? 0.0f : 1.0f;
            animatingNoSound = 0;
        } else {
            animatingNoSoundPlaying = false;
            animatingNoSoundProgress = 0;
            animatingDrawVideoImageButtonProgress = (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) && drawVideoSize ? 1.0f : 0.0f;
        }

        if (getDelegate().getTextSelectionHelper() != null) {
            getDelegate().getTextSelectionHelper().onChatMessageCellAttached(this);
        }

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            boolean showSeekbar = MediaController.getInstance().isPlayingMessage(currentMessageObject);
            toSeekBarProgress = showSeekbar ? 1f : 0f;
        }
    }

    private void setMessageContent(MessageObject messageObject, MessageObject.GroupedMessages groupedMessages, boolean bottomNear, boolean topNear) {
        if (messageObject.checkLayout() || currentPosition != null && lastHeight != AndroidUtilities.displaySize.y) {
            currentMessageObject = null;
        }
        boolean widthChanged = lastWidth != getParentWidth();
        lastHeight = AndroidUtilities.displaySize.y;
        lastWidth = getParentWidth();
        isRoundVideo = messageObject != null && messageObject.isRoundVideo();
        TLRPC.Message newReply = messageObject.hasValidReplyMessageObject() ? messageObject.replyMessageObject.messageOwner : null;
        boolean messageIdChanged = currentMessageObject == null || currentMessageObject.getId() != messageObject.getId();
        boolean messageChanged = currentMessageObject != messageObject || messageObject.forceUpdate || (isRoundVideo && isPlayingRound != (MediaController.getInstance().isPlayingMessage(currentMessageObject) && delegate != null && !delegate.keyboardIsOpened()));
        boolean dataChanged = currentMessageObject != null && currentMessageObject.getId() == messageObject.getId() && lastSendState == MessageObject.MESSAGE_SEND_STATE_EDITING && messageObject.isSent() ||
                currentMessageObject == messageObject && (isUserDataChanged() || photoNotSet) ||
                lastPostAuthor != messageObject.messageOwner.post_author ||
                wasPinned != isPinned ||
                newReply != lastReplyMessage;
        boolean groupChanged = groupedMessages != currentMessagesGroup;
        boolean pollChanged = false;
        if (drawCommentButton || drawSideButton == 3 && !((hasDiscussion && messageObject.isLinkedToChat(linkedChatId) || isRepliesChat) && (currentPosition == null || currentPosition.siblingHeights == null && (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0 || currentPosition.siblingHeights != null && (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0))) {
            dataChanged = true;
        }
        if (!messageChanged && messageObject.isDice()) {
            setCurrentDiceValue(isUpdating);
        }
        if (!messageChanged && messageObject.isPoll()) {
            ArrayList<TLRPC.TL_pollAnswerVoters> newResults = null;
            TLRPC.Poll newPoll = null;
            int newVoters = 0;
            if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPoll) {
                TLRPC.TL_messageMediaPoll mediaPoll = (TLRPC.TL_messageMediaPoll) messageObject.messageOwner.media;
                newResults = mediaPoll.results.results;
                newPoll = mediaPoll.poll;
                newVoters = mediaPoll.results.total_voters;
            }
            if (newResults != null && lastPollResults != null && newVoters != lastPollResultsVoters) {
                pollChanged = true;
            }
            if (!pollChanged && newResults != lastPollResults) {
                pollChanged = true;
            }
            if (lastPoll != newPoll && lastPoll.closed != newPoll.closed) {
                pollChanged = true;
                if (!pollVoted) {
                    pollVoteInProgress = true;
                    vibrateOnPollVote = false;
                }
            }
            animatePollAvatars = false;
            if (pollChanged && attachedToWindow) {
                pollAnimationProgressTime = 0.0f;
                if (pollVoted && !messageObject.isVoted()) {
                    pollUnvoteInProgress = true;
                }
                animatePollAvatars = lastPollResultsVoters == 0 || lastPollResultsVoters != 0 && newVoters == 0;
            }
        }
        if (!groupChanged && groupedMessages != null) {
            MessageObject.GroupedMessagePosition newPosition;
            if (groupedMessages.messages.size() > 1) {
                newPosition = currentMessagesGroup.positions.get(currentMessageObject);
            } else {
                newPosition = null;
            }
            groupChanged = newPosition != currentPosition;
        }
        if (messageChanged || dataChanged || groupChanged || pollChanged || widthChanged && messageObject.isPoll() || isPhotoDataChanged(messageObject) || pinnedBottom != bottomNear || pinnedTop != topNear) {
            wasPinned = isPinned;
            pinnedBottom = bottomNear;
            pinnedTop = topNear;
            currentMessageObject = messageObject;
            currentMessagesGroup = groupedMessages;
            lastTime = -2;
            lastPostAuthor = messageObject.messageOwner.post_author;
            isHighlightedAnimated = false;
            widthBeforeNewTimeLine = -1;
            if (currentMessagesGroup != null && (currentMessagesGroup.posArray.size() > 1)) {
                currentPosition = currentMessagesGroup.positions.get(currentMessageObject);
                if (currentPosition == null) {
                    currentMessagesGroup = null;
                }
            } else {
                currentMessagesGroup = null;
                currentPosition = null;
            }
            if (currentMessagesGroup == null || currentMessagesGroup.isDocuments) {
                drawPinnedTop = pinnedTop;
                drawPinnedBottom = pinnedBottom;
            } else {
                drawPinnedTop = pinnedTop && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) != 0);
                drawPinnedBottom = pinnedBottom && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0);
            }

            isPlayingRound = isRoundVideo && MediaController.getInstance().isPlayingMessage(currentMessageObject) && delegate != null && !delegate.keyboardIsOpened() && !delegate.isLandscape();
            photoImage.setCrossfadeWithOldImage(false);
            photoImage.setCrossfadeDuration(ImageReceiver.DEFAULT_CROSSFADE_DURATION);
            photoImage.setGradientBitmap(null);
            lastSendState = messageObject.messageOwner.send_state;
            lastDeleteDate = messageObject.messageOwner.destroyTime;
            lastViewsCount = messageObject.messageOwner.views;
            lastRepliesCount = getRepliesCount();
            isPressed = false;
            gamePreviewPressed = false;
            sideButtonPressed = false;
            isCheckPressed = true;
            hasNewLineForTime = false;
            isThreadPost = isThreadChat && messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.channel_post != 0;
            isAvatarVisible = !isThreadPost && isChat && !messageObject.isOutOwner() && messageObject.needDrawAvatar() && (currentPosition == null || currentPosition.edge);
            boolean drawAvatar = isChat && !isThreadPost && !messageObject.isOutOwner() && messageObject.needDrawAvatar();
            wasLayout = false;
            groupPhotoInvisible = false;
            animatingDrawVideoImageButton = 0;
            drawVideoSize = false;
            canStreamVideo = false;
            animatingNoSound = 0;
            drawSideButton = !isRepliesChat && checkNeedDrawShareButton(messageObject) && (currentPosition == null || currentPosition.last) ? 1 : 0;
            if (isPinnedChat || drawSideButton == 1 && messageObject.messageOwner.fwd_from != null && !messageObject.isOutOwner() && messageObject.messageOwner.fwd_from.saved_from_peer != null && messageObject.getDialogId() == UserConfig.getInstance(currentAccount).getClientUserId()) {
                drawSideButton = 2;
            }
            replyNameLayout = null;
            adminLayout = null;
            checkOnlyButtonPressed = false;
            replyTextLayout = null;
            lastReplyMessage = null;
            hasEmbed = false;
            autoPlayingMedia = false;
            replyNameWidth = 0;
            replyTextWidth = 0;
            viaWidth = 0;
            viaNameWidth = 0;
            addedCaptionHeight = 0;
            currentReplyPhoto = null;
            currentUser = null;
            currentChat = null;
            currentViaBotUser = null;
            instantViewLayout = null;
            drawNameLayout = false;
            lastLoadingSizeTotal = 0;
            if (scheduledInvalidate) {
                AndroidUtilities.cancelRunOnUIThread(invalidateRunnable);
                scheduledInvalidate = false;
            }

            resetPressedLink(-1);
            messageObject.forceUpdate = false;
            drawPhotoImage = false;
            drawMediaCheckBox = false;
            hasLinkPreview = false;
            hasOldCaptionPreview = false;
            hasGamePreview = false;
            hasInvoicePreview = false;
            instantPressed = instantButtonPressed = commentButtonPressed = false;
            if (!pollChanged && Build.VERSION.SDK_INT >= 21) {
                for (int a = 0; a < selectorDrawable.length; a++) {
                    if (selectorDrawable[a] != null) {
                        selectorDrawable[a].setVisible(false, false);
                        selectorDrawable[a].setState(StateSet.NOTHING);
                    }
                }
            }
            linkPreviewPressed = false;
            buttonPressed = 0;
            additionalTimeOffsetY = 0;
            miniButtonPressed = 0;
            pressedBotButton = -1;
            pressedVoteButton = -1;
            pollHintPressed = false;
            psaHintPressed = false;
            linkPreviewHeight = 0;
            mediaOffsetY = 0;
            documentAttachType = DOCUMENT_ATTACH_TYPE_NONE;
            documentAttach = null;
            descriptionLayout = null;
            titleLayout = null;
            videoInfoLayout = null;
            photosCountLayout = null;
            siteNameLayout = null;
            authorLayout = null;
            captionLayout = null;
            captionWidth = 0;
            captionHeight = 0;
            captionOffsetX = 0;
            currentCaption = null;
            docTitleLayout = null;
            drawImageButton = false;
            drawVideoImageButton = false;
            currentPhotoObject = null;
            photoParentObject = null;
            currentPhotoObjectThumb = null;
            if (messageChanged || messageIdChanged || dataChanged) {
                currentPhotoFilter = null;
            }
            buttonState = -1;
            miniButtonState = -1;
            hasMiniProgress = 0;
            if (addedForTest && currentUrl != null && currentWebFile != null) {
                ImageLoader.getInstance().removeTestWebFile(currentUrl);
            }
            addedForTest = false;
            photoNotSet = false;
            drawBackground = true;
            drawName = false;
            useSeekBarWaweform = false;
            drawInstantView = false;
            drawInstantViewType = 0;
            drawForwardedName = false;
            drawCommentButton = false;
            photoImage.setSideClip(0);
            gradientShader = null;
            motionBackgroundDrawable = null;

            imageBackgroundColor = 0;
            imageBackgroundGradientColor1 = 0;
            imageBackgroundGradientColor2 = 0;
            imageBackgroundIntensity = 0;
            imageBackgroundGradientColor3 = 0;
            imageBackgroundGradientRotation = 45;
            imageBackgroundSideColor = 0;
            mediaBackground = false;
            isMedia = false;
            hasPsaHint = messageObject.messageOwner.fwd_from != null && !TextUtils.isEmpty(messageObject.messageOwner.fwd_from.psa_type);
            if (hasPsaHint) {
                createSelectorDrawable(0);
            }
            photoImage.setAlpha(1.0f);
            if ((messageChanged || dataChanged) && !pollUnvoteInProgress) {
                pollButtons.clear();
            }
            int captionNewLine = 0;
            availableTimeWidth = 0;
            lastReactions = messageObject.messageOwner.reactions;
            photoImage.setForceLoading(false);
            photoImage.setNeedsQualityThumb(false);
            photoImage.setShouldGenerateQualityThumb(false);
            photoImage.setAllowDecodeSingleFrame(false);
            photoImage.setColorFilter(null);
            photoImage.setMediaStartEndTime(-1, -1);
            boolean canChangeRadius = true;

            if (messageChanged) {
                firstVisibleBlockNum = 0;
                lastVisibleBlockNum = 0;
                if (currentMessageObject != null && currentMessageObject.textLayoutBlocks != null && currentMessageObject.textLayoutBlocks.size() > 1) {
                    needNewVisiblePart = true;
                }
            }

            boolean linked = false;
            if (currentMessagesGroup != null && currentMessagesGroup.messages.size() > 0) {
                MessageObject object = currentMessagesGroup.messages.get(0);
                if (object.isLinkedToChat(linkedChatId)) {
                    linked = true;
                }
            } else {
                linked = messageObject.isLinkedToChat(linkedChatId);
            }
            if ((hasDiscussion && linked || isRepliesChat && !messageObject.isOutOwner()) && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0)) {
                int commentCount = getRepliesCount();
                if (!messageObject.shouldDrawWithoutBackground() && !messageObject.isAnimatedEmoji()) {
                    drawCommentButton = true;
                    int avatarsOffset = 0;
                    String comment;
                    if (commentProgress == null) {
                        commentProgress = new InfiniteProgress(AndroidUtilities.dp(7));
                    }
                    if (isRepliesChat) {
                        comment = LocaleController.getString("ViewInChat", R.string.ViewInChat);
                    } else {
                        if (LocaleController.isRTL) {
                            comment = commentCount == 0 ? LocaleController.getString("LeaveAComment", R.string.LeaveAComment) : LocaleController.formatPluralString("CommentsCount", commentCount);
                        } else {
                            comment = commentCount == 0 ? LocaleController.getString("LeaveAComment", R.string.LeaveAComment) : LocaleController.getPluralString("CommentsNoNumber", commentCount);
                        }
                        ArrayList<TLRPC.Peer> recentRepliers = getRecentRepliers();
                        if (commentCount != 0 && recentRepliers != null && !recentRepliers.isEmpty()) {
                            createCommentUI();
                            int size = recentRepliers.size();
                            for (int a = 0; a < commentAvatarImages.length; a++) {
                                if (a < size) {
                                    commentAvatarImages[a].setImageCoords(0, 0, AndroidUtilities.dp(24), AndroidUtilities.dp(24));
                                    long id = MessageObject.getPeerId(recentRepliers.get(a));
                                    TLRPC.User user = null;
                                    TLRPC.Chat chat = null;
                                    if (DialogObject.isUserDialog(id)) {
                                        user = MessagesController.getInstance(currentAccount).getUser(id);
                                    } else if (DialogObject.isChatDialog(id)) {
                                        chat = MessagesController.getInstance(currentAccount).getChat(-id);
                                    }
                                    if (user != null) {
                                        commentAvatarDrawables[a].setInfo(user);
                                        commentAvatarImages[a].setForUserOrChat(user, commentAvatarDrawables[a]);
                                    } else if (chat != null) {
                                        commentAvatarDrawables[a].setInfo(chat);
                                        commentAvatarImages[a].setForUserOrChat(chat, commentAvatarDrawables[a]);
                                    } else {
                                        commentAvatarDrawables[a].setInfo(id, "", "");
                                    }
                                    commentAvatarImagesVisible[a] = true;
                                    avatarsOffset += a == 0 ? 2 : 17;
                                } else if (size != 0) {
                                    commentAvatarImages[a].setImageBitmap((Drawable) null);
                                    commentAvatarImagesVisible[a] = false;
                                }
                            }
                        } else if (commentAvatarImages != null) {
                            for (int a = 0; a < commentAvatarImages.length; a++) {
                                commentAvatarImages[a].setImageBitmap((Drawable) null);
                                commentAvatarImagesVisible[a] = false;
                            }
                        }
                    }
                    commentWidth = totalCommentWidth = (int) Math.ceil(Theme.chat_replyNamePaint.measureText(comment));
                    commentLayout = new StaticLayout(comment, Theme.chat_replyNamePaint, commentWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    if (commentCount != 0 && !LocaleController.isRTL) {
                        drawCommentNumber = true;
                        if (commentNumberLayout == null) {
                            commentNumberLayout = new AnimatedNumberLayout(this, Theme.chat_replyNamePaint);
                            commentNumberLayout.setNumber(commentCount, false);
                        } else {
                            commentNumberLayout.setNumber(commentCount, messageObject.animateComments);
                        }
                        messageObject.animateComments = false;
                        commentNumberWidth = commentNumberLayout.getWidth();
                        totalCommentWidth += commentNumberWidth + AndroidUtilities.dp(4);
                    } else {
                        drawCommentNumber = false;
                        if (commentNumberLayout != null) {
                            commentNumberLayout.setNumber(1, false);
                        }
                    }
                    totalCommentWidth += AndroidUtilities.dp(70 + avatarsOffset);
                } else {
                    if (!isRepliesChat && commentCount > 0) {
                        String comment = LocaleController.formatShortNumber(commentCount, null);
                        commentWidth = totalCommentWidth = (int) Math.ceil(Theme.chat_stickerCommentCountPaint.measureText(comment));
                        commentLayout = new StaticLayout(comment, Theme.chat_stickerCommentCountPaint, commentWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    } else {
                        commentLayout = null;
                    }
                    drawCommentNumber = false;
                    drawSideButton = isRepliesChat ? 2 : 3;
                }
            } else {
                commentLayout = null;
                drawCommentNumber = false;
            }

            if (messageObject.type == 0) {
                drawForwardedName = !isRepliesChat;

                int maxWidth;
                if (drawAvatar) {
                    if (AndroidUtilities.isTablet()) {
                        maxWidth = AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(122);
                    } else {
                        maxWidth = Math.min(getParentWidth(), AndroidUtilities.displaySize.y) - AndroidUtilities.dp(122);
                    }
                    drawName = true;
                } else {
                    if (AndroidUtilities.isTablet()) {
                        maxWidth = AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(80);
                    } else {
                        maxWidth = Math.min(getParentWidth(), AndroidUtilities.displaySize.y) - AndroidUtilities.dp(80);
                    }
                    drawName = isPinnedChat || messageObject.messageOwner.peer_id.channel_id != 0 && (!messageObject.isOutOwner() || messageObject.isSupergroup()) || messageObject.isImportedForward() && messageObject.messageOwner.fwd_from.from_id == null;
                }

                availableTimeWidth = maxWidth;
                if (messageObject.isRoundVideo()) {
                    availableTimeWidth -= Math.ceil(Theme.chat_audioTimePaint.measureText("00:00")) + (messageObject.isOutOwner() ? 0 : AndroidUtilities.dp(64));
                }
                measureTime(messageObject);
                int timeMore = timeWidth + AndroidUtilities.dp(6);
                if (messageObject.isOutOwner()) {
                    timeMore += AndroidUtilities.dp(20.5f);
                }
                timeMore += getExtraTimeX();

                hasGamePreview = messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame && messageObject.messageOwner.media.game instanceof TLRPC.TL_game;
                hasInvoicePreview = messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice;
                hasLinkPreview = !messageObject.isRestrictedMessage && messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage && messageObject.messageOwner.media.webpage instanceof TLRPC.TL_webPage;
                drawInstantView = hasLinkPreview && messageObject.messageOwner.media.webpage.cached_page != null;
                String siteName = hasLinkPreview ? messageObject.messageOwner.media.webpage.site_name : null;
                hasEmbed = hasLinkPreview && !TextUtils.isEmpty(messageObject.messageOwner.media.webpage.embed_url) && !messageObject.isGif() && !"instangram".equalsIgnoreCase(siteName);
                boolean slideshow = false;
                String webpageType = hasLinkPreview ? messageObject.messageOwner.media.webpage.type : null;
                TLRPC.Document androidThemeDocument = null;
                TLRPC.ThemeSettings androidThemeSettings = null;
                if (!drawInstantView) {
                    if ("telegram_livestream".equals(webpageType)) {
                        drawInstantView = true;
                        drawInstantViewType = 11;
                    } else if ("telegram_voicechat".equals(webpageType)) {
                        drawInstantView = true;
                        drawInstantViewType = 9;
                    } else if ("telegram_channel".equals(webpageType)) {
                        drawInstantView = true;
                        drawInstantViewType = 1;
                    } else if ("telegram_megagroup".equals(webpageType)) {
                        drawInstantView = true;
                        drawInstantViewType = 2;
                    } else if ("telegram_message".equals(webpageType)) {
                        drawInstantView = true;
                        drawInstantViewType = 3;
                    } else if ("telegram_theme".equals(webpageType)) {
                        for (int b = 0, N2 = messageObject.messageOwner.media.webpage.attributes.size(); b < N2; b++) {
                            TLRPC.TL_webPageAttributeTheme attribute = messageObject.messageOwner.media.webpage.attributes.get(b);
                            ArrayList<TLRPC.Document> documents = attribute.documents;
                            for (int a = 0, N = documents.size(); a < N; a++) {
                                TLRPC.Document document = documents.get(a);
                                if ("application/x-tgtheme-android".equals(document.mime_type)) {
                                    drawInstantView = true;
                                    drawInstantViewType = 7;
                                    androidThemeDocument = document;
                                    break;
                                }
                            }
                            if (drawInstantView) {
                                break;
                            }
                            if (attribute.settings != null) {
                                drawInstantView = true;
                                drawInstantViewType = 7;
                                androidThemeSettings = attribute.settings;
                                break;
                            }
                        }
                    } else if ("telegram_background".equals(webpageType)) {
                        drawInstantView = true;
                        drawInstantViewType = 6;
                        try {
                            Uri url = Uri.parse(messageObject.messageOwner.media.webpage.url);
                            imageBackgroundIntensity = Utilities.parseInt(url.getQueryParameter("intensity"));
                            String bgColor = url.getQueryParameter("bg_color");
                            String rotation = url.getQueryParameter("rotation");
                            if (rotation != null) {
                                imageBackgroundGradientRotation = Utilities.parseInt(rotation);
                            }
                            if (TextUtils.isEmpty(bgColor)) {
                                TLRPC.Document document = messageObject.getDocument();
                                if (document != null && "image/png".equals(document.mime_type)) {
                                    bgColor = "ffffff";
                                }
                                if (imageBackgroundIntensity == 0) {
                                    imageBackgroundIntensity = 50;
                                }
                            }
                            if (bgColor != null) {
                                imageBackgroundColor = Integer.parseInt(bgColor.substring(0, 6), 16) | 0xff000000;
                                int averageColor = imageBackgroundColor;
                                if (bgColor.length() >= 13 && AndroidUtilities.isValidWallChar(bgColor.charAt(6))) {
                                    imageBackgroundGradientColor1 = Integer.parseInt(bgColor.substring(7, 13), 16) | 0xff000000;
                                    averageColor = AndroidUtilities.getAverageColor(imageBackgroundColor, imageBackgroundGradientColor1);
                                }
                                if (bgColor.length() >= 20 && AndroidUtilities.isValidWallChar(bgColor.charAt(13))) {
                                    imageBackgroundGradientColor2 = Integer.parseInt(bgColor.substring(14, 20), 16) | 0xff000000;
                                }
                                if (bgColor.length() == 27 && AndroidUtilities.isValidWallChar(bgColor.charAt(20))) {
                                    imageBackgroundGradientColor3 = Integer.parseInt(bgColor.substring(21), 16) | 0xff000000;
                                }
                                if (imageBackgroundIntensity < 0) {
                                    imageBackgroundSideColor = 0xff111111;
                                } else {
                                    imageBackgroundSideColor = AndroidUtilities.getPatternSideColor(averageColor);
                                }
                                photoImage.setColorFilter(new PorterDuffColorFilter(AndroidUtilities.getPatternColor(averageColor), PorterDuff.Mode.SRC_IN));
                                photoImage.setAlpha(Math.abs(imageBackgroundIntensity) / 100.0f);
                            } else {
                                String color = url.getLastPathSegment();
                                if (color != null && color.length() >= 6) {
                                    imageBackgroundColor = Integer.parseInt(color.substring(0, 6), 16) | 0xff000000;
                                    if (color.length() >= 13 && AndroidUtilities.isValidWallChar(color.charAt(6))) {
                                        imageBackgroundGradientColor1 = Integer.parseInt(color.substring(7, 13), 16) | 0xff000000;
                                    }
                                    if (color.length() >= 20 && AndroidUtilities.isValidWallChar(color.charAt(13))) {
                                        imageBackgroundGradientColor2 = Integer.parseInt(color.substring(14, 20), 16) | 0xff000000;
                                    }
                                    if (color.length() == 27 && AndroidUtilities.isValidWallChar(color.charAt(20))) {
                                        imageBackgroundGradientColor3 = Integer.parseInt(color.substring(21), 16) | 0xff000000;
                                    }
                                    currentPhotoObject = new TLRPC.TL_photoSizeEmpty();
                                    currentPhotoObject.type = "s";
                                    currentPhotoObject.w = AndroidUtilities.dp(180);
                                    currentPhotoObject.h = AndroidUtilities.dp(150);
                                    currentPhotoObject.location = new TLRPC.TL_fileLocationUnavailable();
                                }
                            }
                        } catch (Exception ignore) {

                        }
                    }
                } else if (siteName != null) {
                    siteName = siteName.toLowerCase();
                    if ((siteName.equals("instagram") || siteName.equals("twitter") || "telegram_album".equals(webpageType)) && messageObject.messageOwner.media.webpage.cached_page instanceof TLRPC.TL_page &&
                            (messageObject.messageOwner.media.webpage.photo instanceof TLRPC.TL_photo || MessageObject.isVideoDocument(messageObject.messageOwner.media.webpage.document))) {
                        drawInstantView = false;
                        slideshow = true;
                        ArrayList<TLRPC.PageBlock> blocks = messageObject.messageOwner.media.webpage.cached_page.blocks;
                        int count = 1;
                        for (int a = 0; a < blocks.size(); a++) {
                            TLRPC.PageBlock block = blocks.get(a);
                            if (block instanceof TLRPC.TL_pageBlockSlideshow) {
                                TLRPC.TL_pageBlockSlideshow b = (TLRPC.TL_pageBlockSlideshow) block;
                                count = b.items.size();
                            } else if (block instanceof TLRPC.TL_pageBlockCollage) {
                                TLRPC.TL_pageBlockCollage b = (TLRPC.TL_pageBlockCollage) block;
                                count = b.items.size();
                            }
                        }
                        String str = LocaleController.formatString("Of", R.string.Of, 1, count);
                        photosCountWidth = (int) Math.ceil(Theme.chat_durationPaint.measureText(str));
                        photosCountLayout = new StaticLayout(str, Theme.chat_durationPaint, photosCountWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    }
                }
                backgroundWidth = maxWidth;
                if (hasLinkPreview || hasGamePreview || hasInvoicePreview || maxWidth - messageObject.lastLineWidth < timeMore) {
                    backgroundWidth = Math.max(backgroundWidth, messageObject.lastLineWidth) + AndroidUtilities.dp(31);
                    backgroundWidth = Math.max(backgroundWidth, timeWidth + AndroidUtilities.dp(31));
                } else {
                    int diff = backgroundWidth - messageObject.lastLineWidth;
                    if (diff >= 0 && diff <= timeMore) {
                        backgroundWidth = backgroundWidth + timeMore - diff + AndroidUtilities.dp(31);
                    } else {
                        backgroundWidth = Math.max(backgroundWidth, messageObject.lastLineWidth + timeMore) + AndroidUtilities.dp(31);
                    }
                }
                availableTimeWidth = backgroundWidth - AndroidUtilities.dp(31);
                if (messageObject.isRoundVideo()) {
                    availableTimeWidth -= Math.ceil(Theme.chat_audioTimePaint.measureText("00:00")) + (messageObject.isOutOwner() ? 0 : AndroidUtilities.dp(64));
                }

                setMessageObjectInternal(messageObject);

                backgroundWidth = messageObject.textWidth + getExtraTextX() * 2 + (hasGamePreview || hasInvoicePreview ? AndroidUtilities.dp(10) : 0);
                totalHeight = messageObject.textHeight + AndroidUtilities.dp(19.5f) + namesOffset;
                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }

                int maxChildWidth = Math.max(backgroundWidth, nameWidth);
                maxChildWidth = Math.max(maxChildWidth, forwardedNameWidth);
                maxChildWidth = Math.max(maxChildWidth, replyNameWidth);
                maxChildWidth = Math.max(maxChildWidth, replyTextWidth);
                if (commentLayout != null && drawSideButton != 3) {
                    maxChildWidth = Math.max(maxChildWidth, totalCommentWidth);
                }
                int maxWebWidth = 0;

                if (hasLinkPreview || hasGamePreview || hasInvoicePreview) {
                    int linkPreviewMaxWidth;
                    if (AndroidUtilities.isTablet()) {
                        if (drawAvatar) {
                            linkPreviewMaxWidth = AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(132);
                        } else {
                            linkPreviewMaxWidth = AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(80);
                        }
                    } else {
                        if (drawAvatar) {
                            linkPreviewMaxWidth = getParentWidth() - AndroidUtilities.dp(132);
                        } else {
                            linkPreviewMaxWidth = getParentWidth() - AndroidUtilities.dp(80);
                        }
                    }
                    if (drawSideButton != 0) {
                        linkPreviewMaxWidth -= AndroidUtilities.dp(20);
                    }
                    String site_name;
                    String title;
                    String author;
                    String description;
                    TLRPC.Photo photo;
                    TLRPC.Document document;
                    WebFile webDocument;
                    int duration;
                    boolean smallImage;
                    String type;
                    if (hasLinkPreview) {
                        TLRPC.TL_webPage webPage = (TLRPC.TL_webPage) messageObject.messageOwner.media.webpage;
                        site_name = webPage.site_name;
                        title = drawInstantViewType != 6 && drawInstantViewType != 7 ? webPage.title : null;
                        author = drawInstantViewType != 6 && drawInstantViewType != 7 ? webPage.author : null;
                        description = drawInstantViewType != 6 && drawInstantViewType != 7 ? webPage.description : null;
                        photo = webPage.photo;
                        webDocument = null;
                        if (drawInstantViewType == 7) {
                            if (androidThemeSettings != null) {
                                document = new DocumentObject.ThemeDocument(androidThemeSettings);
                            } else {
                                document = androidThemeDocument;
                            }
                        } else {
                            document = webPage.document;
                        }
                        type = webPage.type;
                        duration = webPage.duration;
                        if (site_name != null && photo != null && site_name.toLowerCase().equals("instagram")) {
                            linkPreviewMaxWidth = Math.max(AndroidUtilities.displaySize.y / 3, currentMessageObject.textWidth);
                        }
                        boolean isSmallImageType = "app".equals(type) || "profile".equals(type) || "article".equals(type) ||
                                "telegram_bot".equals(type) || "telegram_user".equals(type) || "telegram_channel".equals(type) || "telegram_megagroup".equals(type) || "telegram_voicechat".equals(type);
                        smallImage = !slideshow && (!drawInstantView || drawInstantViewType == 9 || drawInstantViewType == 11) && document == null && isSmallImageType;
                        isSmallImage = !slideshow && (!drawInstantView || drawInstantViewType == 9 || drawInstantViewType == 11) && document == null && description != null && type != null && isSmallImageType && currentMessageObject.photoThumbs != null;
                    } else if (hasInvoicePreview) {
                        TLRPC.TL_messageMediaInvoice invoice = (TLRPC.TL_messageMediaInvoice) messageObject.messageOwner.media;
                        site_name = messageObject.messageOwner.media.title;
                        title = null;
                        description = null;
                        photo = null;
                        author = null;
                        document = null;
                        if (invoice.photo instanceof TLRPC.TL_webDocument) {
                            webDocument = WebFile.createWithWebDocument(invoice.photo);
                        } else {
                            webDocument = null;
                        }
                        duration = 0;
                        type = "invoice";
                        isSmallImage = false;
                        smallImage = false;
                    } else {
                        TLRPC.TL_game game = messageObject.messageOwner.media.game;
                        site_name = game.title;
                        title = null;
                        webDocument = null;
                        description = TextUtils.isEmpty(messageObject.messageText) ? game.description : null;
                        photo = game.photo;
                        author = null;
                        document = game.document;
                        duration = 0;
                        type = "game";
                        isSmallImage = false;
                        smallImage = false;
                    }
                    if (drawInstantViewType == 11) {
                        site_name = LocaleController.getString("VoipChannelVoiceChat", R.string.VoipChannelVoiceChat);
                    } else if (drawInstantViewType == 9) {
                        site_name = LocaleController.getString("VoipGroupVoiceChat", R.string.VoipGroupVoiceChat);
                    } else if (drawInstantViewType == 6) {
                        site_name = LocaleController.getString("ChatBackground", R.string.ChatBackground);
                    } else if ("telegram_theme".equals(webpageType)) {
                        site_name = LocaleController.getString("ColorTheme", R.string.ColorTheme);
                    }

                    int additinalWidth = hasInvoicePreview ? 0 : AndroidUtilities.dp(10);
                    int restLinesCount = 3;
                    linkPreviewMaxWidth -= additinalWidth;

                    if (currentMessageObject.photoThumbs == null && photo != null) {
                        currentMessageObject.generateThumbs(true);
                    }

                    if (site_name != null) {
                        try {
                            int width = (int) Math.ceil(Theme.chat_replyNamePaint.measureText(site_name) + 1);
                            if (!isSmallImage || description == null) {
                                siteNameLayout = new StaticLayout(site_name, Theme.chat_replyNamePaint, Math.min(width, linkPreviewMaxWidth), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            } else {
                                siteNameLayout = generateStaticLayout(site_name, Theme.chat_replyNamePaint, linkPreviewMaxWidth, linkPreviewMaxWidth - AndroidUtilities.dp(48 + 4), restLinesCount, 1);
                                restLinesCount -= siteNameLayout.getLineCount();
                            }
                            siteNameRtl = Math.max(siteNameLayout.getLineLeft(0), 0) != 0;
                            int height = siteNameLayout.getLineBottom(siteNameLayout.getLineCount() - 1);
                            linkPreviewHeight += height;
                            totalHeight += height;
                            siteNameWidth = width = siteNameLayout.getWidth();
                            maxChildWidth = Math.max(maxChildWidth, width + additinalWidth);
                            maxWebWidth = Math.max(maxWebWidth, width + additinalWidth);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    boolean titleIsRTL = false;
                    if (title != null) {
                        try {
                            titleX = Integer.MAX_VALUE;
                            if (linkPreviewHeight != 0) {
                                linkPreviewHeight += AndroidUtilities.dp(2);
                                totalHeight += AndroidUtilities.dp(2);
                            }
                            int restLines = 0;
                            if (!isSmallImage || description == null) {
                                titleLayout = StaticLayoutEx.createStaticLayout(title, Theme.chat_replyNamePaint, linkPreviewMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, AndroidUtilities.dp(1), false, TextUtils.TruncateAt.END, linkPreviewMaxWidth, 4);
                            } else {
                                restLines = restLinesCount;
                                titleLayout = generateStaticLayout(title, Theme.chat_replyNamePaint, linkPreviewMaxWidth, linkPreviewMaxWidth - AndroidUtilities.dp(48 + 4), restLinesCount, 4);
                                restLinesCount -= titleLayout.getLineCount();
                            }
                            int height = titleLayout.getLineBottom(titleLayout.getLineCount() - 1);
                            linkPreviewHeight += height;
                            totalHeight += height;
                            for (int a = 0; a < titleLayout.getLineCount(); a++) {
                                int lineLeft = (int) Math.max(0, titleLayout.getLineLeft(a));
                                if (lineLeft != 0) {
                                    titleIsRTL = true;
                                }
                                if (titleX == Integer.MAX_VALUE) {
                                    titleX = -lineLeft;
                                } else {
                                    titleX = Math.max(titleX, -lineLeft);
                                }
                                int width;
                                if (lineLeft != 0) {
                                    width = titleLayout.getWidth() - lineLeft;
                                } else {
                                    int max = linkPreviewMaxWidth;
                                    if (a < restLines || lineLeft != 0 && isSmallImage) {
                                        max -= AndroidUtilities.dp(48 + 4);
                                    }
                                    width = (int) Math.min(max, Math.ceil(titleLayout.getLineWidth(a)));
                                }
                                if (a < restLines || lineLeft != 0 && isSmallImage) {
                                    width += AndroidUtilities.dp(48 + 4);
                                }
                                maxChildWidth = Math.max(maxChildWidth, width + additinalWidth);
                                maxWebWidth = Math.max(maxWebWidth, width + additinalWidth);
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                        if (titleIsRTL && isSmallImage) {
                            linkPreviewMaxWidth -= AndroidUtilities.dp(48);
                        }
                    }

                    boolean authorIsRTL = false;
                    if (author != null && title == null) {
                        try {
                            if (linkPreviewHeight != 0) {
                                linkPreviewHeight += AndroidUtilities.dp(2);
                                totalHeight += AndroidUtilities.dp(2);
                            }
                            if (restLinesCount == 3 && (!isSmallImage || description == null)) {
                                authorLayout = new StaticLayout(author, Theme.chat_replyNamePaint, linkPreviewMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            } else {
                                authorLayout = generateStaticLayout(author, Theme.chat_replyNamePaint, linkPreviewMaxWidth, linkPreviewMaxWidth - AndroidUtilities.dp(48 + 4), restLinesCount, 1);
                                restLinesCount -= authorLayout.getLineCount();
                            }
                            int height = authorLayout.getLineBottom(authorLayout.getLineCount() - 1);
                            linkPreviewHeight += height;
                            totalHeight += height;
                            int lineLeft = (int) Math.max(authorLayout.getLineLeft(0), 0);
                            authorX = -lineLeft;
                            int width;
                            if (lineLeft != 0) {
                                width = authorLayout.getWidth() - lineLeft;
                                authorIsRTL = true;
                            } else {
                                width = (int) Math.ceil(authorLayout.getLineWidth(0));
                            }
                            maxChildWidth = Math.max(maxChildWidth, width + additinalWidth);
                            maxWebWidth = Math.max(maxWebWidth, width + additinalWidth);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    if (description != null) {
                        try {
                            descriptionX = 0;
                            currentMessageObject.generateLinkDescription();
                            if (linkPreviewHeight != 0) {
                                linkPreviewHeight += AndroidUtilities.dp(2);
                                totalHeight += AndroidUtilities.dp(2);
                            }
                            int restLines = 0;
                            boolean allowAllLines = site_name != null && site_name.toLowerCase().equals("twitter");
                            if (restLinesCount == 3 && !isSmallImage) {
                                descriptionLayout = StaticLayoutEx.createStaticLayout(messageObject.linkDescription, Theme.chat_replyTextPaint, linkPreviewMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, AndroidUtilities.dp(1), false, TextUtils.TruncateAt.END, linkPreviewMaxWidth, allowAllLines ? 100 : 6);
                            } else {
                                restLines = restLinesCount;
                                descriptionLayout = generateStaticLayout(messageObject.linkDescription, Theme.chat_replyTextPaint, linkPreviewMaxWidth, linkPreviewMaxWidth - AndroidUtilities.dp(48 + 4), restLinesCount, allowAllLines ? 100 : 6);
                            }
                            int height = descriptionLayout.getLineBottom(descriptionLayout.getLineCount() - 1);
                            linkPreviewHeight += height;
                            totalHeight += height;

                            boolean hasRTL = false;
                            for (int a = 0; a < descriptionLayout.getLineCount(); a++) {
                                int lineLeft = (int) Math.ceil(descriptionLayout.getLineLeft(a));
                                if (lineLeft > 0) {
                                    hasRTL = true;
                                    if (descriptionX == 0) {
                                        descriptionX = -lineLeft;
                                    } else {
                                        descriptionX = Math.max(descriptionX, -lineLeft);
                                    }
                                }
                            }

                            int textWidth = descriptionLayout.getWidth();
                            for (int a = 0; a < descriptionLayout.getLineCount(); a++) {
                                int lineLeft = (int) Math.ceil(descriptionLayout.getLineLeft(a));
                                if (lineLeft == 0 && descriptionX != 0) {
                                    descriptionX = 0;
                                }

                                int width;
                                if (lineLeft > 0) {
                                    width = textWidth - lineLeft;
                                } else {
                                    if (hasRTL) {
                                        width = textWidth;
                                    } else {
                                        width = Math.min((int) Math.ceil(descriptionLayout.getLineWidth(a)), textWidth);
                                    }
                                }
                                if (a < restLines || restLines != 0 && lineLeft != 0 && isSmallImage) {
                                    width += AndroidUtilities.dp(48 + 4);
                                }
                                if (maxWebWidth < width + additinalWidth) {
                                    if (titleIsRTL) {
                                        titleX += (width + additinalWidth - maxWebWidth);
                                    }
                                    if (authorIsRTL) {
                                        authorX += (width + additinalWidth - maxWebWidth);
                                    }
                                    maxWebWidth = width + additinalWidth;
                                }
                                maxChildWidth = Math.max(maxChildWidth, width + additinalWidth);
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    if (smallImage && (descriptionLayout == null || titleLayout == null && descriptionLayout != null && descriptionLayout.getLineCount() == 1)) {
                        smallImage = false;
                        isSmallImage = false;
                    }
                    int maxPhotoWidth = smallImage ? AndroidUtilities.dp(48) : linkPreviewMaxWidth;

                    if (document != null) {
                        if (MessageObject.isRoundVideoDocument(document)) {
                            currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                            photoParentObject = document;
                            documentAttach = document;
                            documentAttachType = DOCUMENT_ATTACH_TYPE_ROUND;
                        } else if (MessageObject.isGifDocument(document, messageObject.hasValidGroupId())) {
                            if (!messageObject.isGame() && !SharedConfig.autoplayGifs) {
                                messageObject.gifState = 1;
                            }
                            photoImage.setAllowStartAnimation(messageObject.gifState != 1);
                            currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                            if (currentPhotoObject != null) {
                                photoParentObject = document;
                            } else if (photo != null) {
                                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 90);
                                photoParentObject = photo;
                            }
                            if (currentPhotoObject != null && (currentPhotoObject.w == 0 || currentPhotoObject.h == 0)) {
                                for (int a = 0; a < document.attributes.size(); a++) {
                                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                                    if (attribute instanceof TLRPC.TL_documentAttributeImageSize || attribute instanceof TLRPC.TL_documentAttributeVideo) {
                                        currentPhotoObject.w = attribute.w;
                                        currentPhotoObject.h = attribute.h;
                                        break;
                                    }
                                }
                                if (currentPhotoObject.w == 0 || currentPhotoObject.h == 0) {
                                    currentPhotoObject.w = currentPhotoObject.h = AndroidUtilities.dp(150);
                                }
                            }
                            documentAttach = document;
                            documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
                        } else if (MessageObject.isVideoDocument(document)) {
                            if (photo != null) {
                                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, AndroidUtilities.getPhotoSize(), true);
                                currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, 40);
                                photoParentObject = photo;
                            }
                            if (currentPhotoObject == null) {
                                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
                                currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 40);
                                photoParentObject = document;
                            }
                            if (currentPhotoObject == currentPhotoObjectThumb) {
                                currentPhotoObjectThumb = null;
                            }
                            if (currentPhotoObject == null) {
                                currentPhotoObject = new TLRPC.TL_photoSize();
                                currentPhotoObject.type = "s";
                                currentPhotoObject.location = new TLRPC.TL_fileLocationUnavailable();
                            }
                            if (currentPhotoObject != null && (currentPhotoObject.w == 0 || currentPhotoObject.h == 0 || currentPhotoObject instanceof TLRPC.TL_photoStrippedSize)) {
                                for (int a = 0; a < document.attributes.size(); a++) {
                                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                                    if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                                        if (currentPhotoObject instanceof TLRPC.TL_photoStrippedSize) {
                                            float scale = Math.max(attribute.w, attribute.w) / 50.0f;
                                            currentPhotoObject.w = (int) (attribute.w / scale);
                                            currentPhotoObject.h = (int) (attribute.h / scale);
                                        } else {
                                            currentPhotoObject.w = attribute.w;
                                            currentPhotoObject.h = attribute.h;
                                        }
                                        break;
                                    }
                                }
                                if (currentPhotoObject.w == 0 || currentPhotoObject.h == 0) {
                                    currentPhotoObject.w = currentPhotoObject.h = AndroidUtilities.dp(150);
                                }
                            }
                            createDocumentLayout(0, messageObject);
                        } else if (MessageObject.isStickerDocument(document) || MessageObject.isAnimatedStickerDocument(document, true)) {
                            currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 90);
                            photoParentObject = document;
                            if (currentPhotoObject != null && (currentPhotoObject.w == 0 || currentPhotoObject.h == 0)) {
                                for (int a = 0; a < document.attributes.size(); a++) {
                                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                                    if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                                        currentPhotoObject.w = attribute.w;
                                        currentPhotoObject.h = attribute.h;
                                        break;
                                    }
                                }
                                if (currentPhotoObject.w == 0 || currentPhotoObject.h == 0) {
                                    currentPhotoObject.w = currentPhotoObject.h = AndroidUtilities.dp(150);
                                }
                            }
                            documentAttach = document;
                            documentAttachType = DOCUMENT_ATTACH_TYPE_STICKER;
                        } else if (drawInstantViewType == 6) {
                            currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
                            photoParentObject = document;
                            if (currentPhotoObject != null && (currentPhotoObject.w == 0 || currentPhotoObject.h == 0)) {
                                for (int a = 0; a < document.attributes.size(); a++) {
                                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                                    if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                                        currentPhotoObject.w = attribute.w;
                                        currentPhotoObject.h = attribute.h;
                                        break;
                                    }
                                }
                                if (currentPhotoObject.w == 0 || currentPhotoObject.h == 0) {
                                    currentPhotoObject.w = currentPhotoObject.h = AndroidUtilities.dp(150);
                                }
                            }
                            documentAttach = document;
                            documentAttachType = DOCUMENT_ATTACH_TYPE_WALLPAPER;
                            String str = AndroidUtilities.formatFileSize(documentAttach.size);
                            durationWidth = (int) Math.ceil(Theme.chat_durationPaint.measureText(str));
                            videoInfoLayout = new StaticLayout(str, Theme.chat_durationPaint, durationWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                        } else if (drawInstantViewType == 7) {
                            currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 700);
                            currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 40);
                            photoParentObject = document;
                            if (currentPhotoObject != null && (currentPhotoObject.w == 0 || currentPhotoObject.h == 0)) {
                                for (int a = 0; a < document.attributes.size(); a++) {
                                    TLRPC.DocumentAttribute attribute = document.attributes.get(a);
                                    if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                                        currentPhotoObject.w = attribute.w;
                                        currentPhotoObject.h = attribute.h;
                                        break;
                                    }
                                }
                                if (currentPhotoObject.w == 0 || currentPhotoObject.h == 0) {
                                    currentPhotoObject.w = currentPhotoObject.h = AndroidUtilities.dp(150);
                                }
                            }
                            documentAttach = document;
                            documentAttachType = DOCUMENT_ATTACH_TYPE_THEME;
                        } else {
                            calcBackgroundWidth(maxWidth, timeMore, maxChildWidth);
                            if (backgroundWidth < maxWidth + AndroidUtilities.dp(20)) {
                                backgroundWidth = maxWidth + AndroidUtilities.dp(20);
                            }
                            if (MessageObject.isVoiceDocument(document)) {
                                createDocumentLayout(backgroundWidth - AndroidUtilities.dp(10), messageObject);
                                mediaOffsetY = currentMessageObject.textHeight + AndroidUtilities.dp(8) + linkPreviewHeight;
                                totalHeight += AndroidUtilities.dp(30 + 14);
                                linkPreviewHeight += AndroidUtilities.dp(44);

                                maxWidth = maxWidth - AndroidUtilities.dp(86);
                                if (AndroidUtilities.isTablet()) {
                                    maxChildWidth = Math.max(maxChildWidth, Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 52 : 0), AndroidUtilities.dp(220)) - AndroidUtilities.dp(30) + additinalWidth);
                                } else {
                                    maxChildWidth = Math.max(maxChildWidth, Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 52 : 0), AndroidUtilities.dp(220)) - AndroidUtilities.dp(30) + additinalWidth);
                                }
                                calcBackgroundWidth(maxWidth, timeMore, maxChildWidth);
                            } else if (MessageObject.isMusicDocument(document)) {
                                int durationWidth = createDocumentLayout(backgroundWidth - AndroidUtilities.dp(10), messageObject);
                                mediaOffsetY = currentMessageObject.textHeight + AndroidUtilities.dp(8) + linkPreviewHeight;
                                totalHeight += AndroidUtilities.dp(42 + 14);
                                linkPreviewHeight += AndroidUtilities.dp(56);

                                maxWidth = maxWidth - AndroidUtilities.dp(86);
                                maxChildWidth = Math.max(maxChildWidth, durationWidth + additinalWidth + AndroidUtilities.dp(86 + 8));
                                if (songLayout != null && songLayout.getLineCount() > 0) {
                                    maxChildWidth = (int) Math.max(maxChildWidth, songLayout.getLineWidth(0) + additinalWidth + AndroidUtilities.dp(86));
                                }
                                if (performerLayout != null && performerLayout.getLineCount() > 0) {
                                    maxChildWidth = (int) Math.max(maxChildWidth, performerLayout.getLineWidth(0) + additinalWidth + AndroidUtilities.dp(86));
                                }

                                calcBackgroundWidth(maxWidth, timeMore, maxChildWidth);
                            } else {
                                createDocumentLayout(backgroundWidth - AndroidUtilities.dp(86 + 24 + 58), messageObject);
                                drawImageButton = true;
                                if (drawPhotoImage) {
                                    totalHeight += AndroidUtilities.dp(86 + 14);
                                    linkPreviewHeight += AndroidUtilities.dp(86);
                                    photoImage.setImageCoords(0, totalHeight + namesOffset, AndroidUtilities.dp(86), AndroidUtilities.dp(86));
                                } else {
                                    mediaOffsetY = currentMessageObject.textHeight + AndroidUtilities.dp(8) + linkPreviewHeight;
                                    photoImage.setImageCoords(0, totalHeight + namesOffset - AndroidUtilities.dp(14), AndroidUtilities.dp(56), AndroidUtilities.dp(56));
                                    totalHeight += AndroidUtilities.dp(50 + 14);
                                    linkPreviewHeight += AndroidUtilities.dp(50);
                                    if (docTitleLayout != null && docTitleLayout.getLineCount() > 1) {
                                        int h = (docTitleLayout.getLineCount() - 1) * AndroidUtilities.dp(16);
                                        totalHeight += h;
                                        linkPreviewHeight += h;
                                    }
                                }
                            }
                        }
                    } else if (photo != null) {
                        boolean isPhoto = type != null && type.equals("photo");
                        currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, isPhoto || !smallImage ? AndroidUtilities.getPhotoSize() : maxPhotoWidth, !isPhoto);
                        photoParentObject = messageObject.photoThumbsObject;
                        checkOnlyButtonPressed = !isPhoto;
                        currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 40);
                        if (currentPhotoObjectThumb == currentPhotoObject) {
                            currentPhotoObjectThumb = null;
                        }
                    } else if (webDocument != null) {
                        if (!webDocument.mime_type.startsWith("image/")) {
                            webDocument = null;
                        }
                        drawImageButton = false;
                    }

                    if (documentAttachType != DOCUMENT_ATTACH_TYPE_MUSIC && documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO && documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        if (currentPhotoObject != null || webDocument != null || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER || documentAttachType == DOCUMENT_ATTACH_TYPE_THEME) {
                            drawImageButton = photo != null && !smallImage || type != null && (type.equals("photo") || type.equals("document") && documentAttachType != DOCUMENT_ATTACH_TYPE_STICKER || type.equals("gif") || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER);
                            if (linkPreviewHeight != 0) {
                                linkPreviewHeight += AndroidUtilities.dp(2);
                                totalHeight += AndroidUtilities.dp(2);
                            }

                            if (imageBackgroundSideColor != 0) {
                                maxPhotoWidth = AndroidUtilities.dp(208);
                            } else if (currentPhotoObject instanceof TLRPC.TL_photoSizeEmpty && currentPhotoObject.w != 0) {
                                maxPhotoWidth = currentPhotoObject.w;
                            } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER || documentAttachType == DOCUMENT_ATTACH_TYPE_THEME) {
                                if (AndroidUtilities.isTablet()) {
                                    maxPhotoWidth = (int) (AndroidUtilities.getMinTabletSide() * 0.5f);
                                } else {
                                    maxPhotoWidth = (int) (getParentWidth() * 0.5f);
                                }
                            } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
                                maxPhotoWidth = AndroidUtilities.roundMessageSize;
                                photoImage.setAllowDecodeSingleFrame(true);
                            }

                            maxChildWidth = Math.max(maxChildWidth, maxPhotoWidth - (hasInvoicePreview ? AndroidUtilities.dp(12) : 0) + additinalWidth);
                            if (currentPhotoObject != null) {
                                currentPhotoObject.size = -1;
                                if (currentPhotoObjectThumb != null) {
                                    currentPhotoObjectThumb.size = -1;
                                }
                            } else if (webDocument != null) {
                                webDocument.size = -1;
                            }
                            if (imageBackgroundSideColor != 0) {
                                imageBackgroundSideWidth = maxChildWidth - AndroidUtilities.dp(13);
                            }

                            int width;
                            int height;
                            if (smallImage || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
                                width = height = maxPhotoWidth;
                            } else {
                                if (hasGamePreview || hasInvoicePreview) {
                                    if (hasInvoicePreview) {
                                        width = 640;
                                        height = 360;
                                        for (int a = 0, N = webDocument.attributes.size(); a < N; a++) {
                                            TLRPC.DocumentAttribute attribute = webDocument.attributes.get(a);
                                            if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                                                width = attribute.w;
                                                height = attribute.h;
                                                break;
                                            }
                                        }
                                    } else {
                                        width = 640;
                                        height = 360;
                                    }
                                    float scale = width / (float) (maxPhotoWidth - AndroidUtilities.dp(2));
                                    width /= scale;
                                    height /= scale;
                                } else {
                                    if (drawInstantViewType == 7) {
                                        width = 560;
                                        height = 678;
                                    } else if (currentPhotoObject != null) {
                                        width = currentPhotoObject.w;
                                        height = currentPhotoObject.h;
                                    } else {
                                        width = 30;
                                        height = 50;
                                    }
                                    float scale = width / (float) (maxPhotoWidth - AndroidUtilities.dp(2));
                                    width /= scale;
                                    height /= scale;
                                    if (site_name == null || site_name != null && !site_name.toLowerCase().equals("instagram") && documentAttachType == 0) {
                                        if (height > AndroidUtilities.displaySize.y / 3) {
                                            height = AndroidUtilities.displaySize.y / 3;
                                        }
                                    } else {
                                        if (height > AndroidUtilities.displaySize.y / 2) {
                                            height = AndroidUtilities.displaySize.y / 2;
                                        }
                                    }
                                    if (imageBackgroundSideColor != 0) {
                                        scale = height / (float) AndroidUtilities.dp(160);
                                        width /= scale;
                                        height /= scale;
                                    }
                                    if (height < AndroidUtilities.dp(60)) {
                                        height = AndroidUtilities.dp(60);
                                    }
                                }
                            }
                            if (isSmallImage) {
                                if (AndroidUtilities.dp(50) > linkPreviewHeight) {
                                    totalHeight += AndroidUtilities.dp(50) - linkPreviewHeight + AndroidUtilities.dp(8);
                                    linkPreviewHeight = AndroidUtilities.dp(50);
                                }
                                linkPreviewHeight -= AndroidUtilities.dp(8);
                            } else {
                                totalHeight += height + AndroidUtilities.dp(12);
                                linkPreviewHeight += height;
                            }

                            if (documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER && imageBackgroundSideColor == 0) {
                                photoImage.setImageCoords(0, 0, Math.max(maxChildWidth - AndroidUtilities.dp(13), width), height);
                            } else {
                                photoImage.setImageCoords(0, 0, width, height);
                            }

                            int w = (int) (width / AndroidUtilities.density);
                            int h = (int) (height / AndroidUtilities.density);
                            currentPhotoFilter = String.format(Locale.US, "%d_%d", w, h);
                            currentPhotoFilterThumb = String.format(Locale.US, "%d_%d_b", w, h);

                            if (webDocument != null) {
                                /*TODO*/photoImage.setImage(ImageLocation.getForWebFile(webDocument), currentPhotoFilter, null, null, webDocument.size, null, messageObject, 1);
                            } else {
                                if (documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER) {
                                    if (messageObject.mediaExists) {
                                        photoImage.setImage(ImageLocation.getForDocument(documentAttach), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObject, document), "b1", 0, "jpg", messageObject, 1);
                                    } else {
                                        photoImage.setImage(null, null, ImageLocation.getForDocument(currentPhotoObject, document), "b1", 0, "jpg", messageObject, 1);
                                    }
                                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_THEME) {
                                    if (document instanceof DocumentObject.ThemeDocument) {
                                        photoImage.setImage(ImageLocation.getForDocument(document), currentPhotoFilter, null, "b1", 0, "jpg", messageObject, 1);
                                    } else {
                                        photoImage.setImage(ImageLocation.getForDocument(currentPhotoObject, document), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObjectThumb, document), "b1", 0, "jpg", messageObject, 1);
                                    }
                                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER) {
                                    boolean isWebpSticker = messageObject.isSticker();
                                    if (SharedConfig.loopStickers || isWebpSticker) {
                                        photoImage.setAutoRepeat(1);
                                    } else {
                                        currentPhotoFilter = String.format(Locale.US, "%d_%d_nr_%s", w, h, messageObject.toString());
                                        photoImage.setAutoRepeat(delegate != null && delegate.shouldRepeatSticker(messageObject) ? 2 : 3);
                                    }
                                    photoImage.setImage(ImageLocation.getForDocument(documentAttach), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObject, documentAttach), "b1", documentAttach.size, "webp", messageObject, 1);
                                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                                    photoImage.setNeedsQualityThumb(true);
                                    photoImage.setShouldGenerateQualityThumb(true);
                                    if (SharedConfig.autoplayVideo && (
                                            currentMessageObject.mediaExists ||
                                                    messageObject.canStreamVideo() && DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject)
                                    )) {
                                        photoImage.setAllowDecodeSingleFrame(true);
                                        photoImage.setAllowStartAnimation(true);
                                        photoImage.startAnimation();
                                        /*TODO*/photoImage.setImage(ImageLocation.getForDocument(documentAttach), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObjectThumb, documentAttach), currentPhotoFilterThumb, null, documentAttach.size, null, messageObject, 0);
                                        autoPlayingMedia = true;
                                    } else {
                                        if (currentPhotoObjectThumb != null) {
                                            /*TODO*/photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, messageObject, 0);
                                        } else {
                                            /*TODO*/photoImage.setImage(null, null, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoObject instanceof TLRPC.TL_photoStrippedSize || "s".equals(currentPhotoObject.type) ? currentPhotoFilterThumb : currentPhotoFilter, 0, null, messageObject, 0);
                                        }
                                    }
                                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
                                    photoImage.setAllowDecodeSingleFrame(true);
                                    boolean autoDownload = false;
                                    if (MessageObject.isRoundVideoDocument(document)) {
                                        photoImage.setRoundRadius(AndroidUtilities.roundMessageSize / 2);
                                        canChangeRadius = false;
                                        autoDownload = DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject);
                                    } else if (MessageObject.isGifDocument(document, messageObject.hasValidGroupId())) {
                                        autoDownload = DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject);
                                    }
                                    String filter = currentPhotoObject instanceof TLRPC.TL_photoStrippedSize || "s".equals(currentPhotoObject.type) ? currentPhotoFilterThumb : currentPhotoFilter;
                                    if (messageObject.mediaExists || autoDownload) {
                                        autoPlayingMedia = true;
                                        TLRPC.VideoSize videoSize = MessageObject.getDocumentVideoThumb(document);
                                        if (!messageObject.mediaExists && videoSize != null && (currentPhotoObject == null || currentPhotoObjectThumb == null)) {
                                            /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), document.size < 1024 * 32 ? null : ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForDocument(videoSize, documentAttach), null, ImageLocation.getForDocument(currentPhotoObject != null ? currentPhotoObject : currentPhotoObjectThumb, documentAttach), currentPhotoObject != null ? filter : currentPhotoFilterThumb, null, document.size, null, messageObject, 0);
                                        } else {
                                            /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), document.size < 1024 * 32 ? null : ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForDocument(currentPhotoObject, documentAttach), filter, ImageLocation.getForDocument(currentPhotoObjectThumb, documentAttach), currentPhotoFilterThumb, null, document.size, null, messageObject, 0);
                                        }
                                    } else {
                                        /*TODO*/photoImage.setImage(null, null, ImageLocation.getForDocument(currentPhotoObject, documentAttach), filter, 0, null, currentMessageObject, 0);
                                    }
                                } else {
                                    boolean photoExist = messageObject.mediaExists;
                                    String fileName = FileLoader.getAttachFileName(currentPhotoObject);
                                    if (hasGamePreview || photoExist || DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject) || FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                                        photoNotSet = false;
                                        /*TODO*/photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, messageObject, 0);
                                    } else {
                                        photoNotSet = true;
                                        if (currentPhotoObjectThumb != null) {
                                            /*TODO*/photoImage.setImage(null, null, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), String.format(Locale.US, "%d_%d_b", w, h), 0, null, messageObject, 0);
                                        } else {
                                            photoImage.setImageBitmap((Drawable) null);
                                        }
                                    }
                                }
                            }
                            drawPhotoImage = true;

                            if (type != null && type.equals("video") && duration != 0) {
                                String str = AndroidUtilities.formatShortDuration(duration);
                                durationWidth = (int) Math.ceil(Theme.chat_durationPaint.measureText(str));
                                videoInfoLayout = new StaticLayout(str, Theme.chat_durationPaint, durationWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            } else if (hasGamePreview) {
                                String str = LocaleController.getString("AttachGame", R.string.AttachGame).toUpperCase();
                                durationWidth = (int) Math.ceil(Theme.chat_gamePaint.measureText(str));
                                videoInfoLayout = new StaticLayout(str, Theme.chat_gamePaint, durationWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            }
                        } else {
                            photoImage.setImageBitmap((Drawable) null);
                            linkPreviewHeight -= AndroidUtilities.dp(6);
                            totalHeight += AndroidUtilities.dp(4);
                        }
                        if (hasInvoicePreview) {
                            CharSequence str;
                            if ((messageObject.messageOwner.media.flags & 4) != 0) {
                                str = LocaleController.getString("PaymentReceipt", R.string.PaymentReceipt).toUpperCase();
                            } else {
                                if (messageObject.messageOwner.media.test) {
                                    str = LocaleController.getString("PaymentTestInvoice", R.string.PaymentTestInvoice).toUpperCase();
                                } else {
                                    str = LocaleController.getString("PaymentInvoice", R.string.PaymentInvoice).toUpperCase();
                                }
                            }
                            String price = LocaleController.getInstance().formatCurrencyString(messageObject.messageOwner.media.total_amount, messageObject.messageOwner.media.currency);
                            SpannableStringBuilder stringBuilder = new SpannableStringBuilder(price + " " + str);
                            stringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf")), 0, price.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            durationWidth = (int) Math.ceil(Theme.chat_shipmentPaint.measureText(stringBuilder, 0, stringBuilder.length()));
                            videoInfoLayout = new StaticLayout(stringBuilder, Theme.chat_shipmentPaint, durationWidth + AndroidUtilities.dp(10), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            if (!drawPhotoImage) {
                                totalHeight += AndroidUtilities.dp(6);
                                int timeWidthTotal = timeWidth + AndroidUtilities.dp(14 + (messageObject.isOutOwner() ? 20 : 0));
                                if (durationWidth + timeWidthTotal > maxWidth) {
                                    maxChildWidth = Math.max(durationWidth, maxChildWidth);
                                    totalHeight += AndroidUtilities.dp(12);
                                } else {
                                    maxChildWidth = Math.max(durationWidth + timeWidthTotal, maxChildWidth);
                                }
                            }
                        }
                        if (hasGamePreview && messageObject.textHeight != 0) {
                            linkPreviewHeight += messageObject.textHeight + AndroidUtilities.dp(6);
                            totalHeight += AndroidUtilities.dp(4);
                        }
                        calcBackgroundWidth(maxWidth, timeMore, maxChildWidth);
                    }
                    createInstantViewButton();
                } else {
                    photoImage.setImageBitmap((Drawable) null);
                    calcBackgroundWidth(maxWidth, timeMore, maxChildWidth);
                }
            } else if (messageObject.type == 16) {
                createSelectorDrawable(0);
                drawName = false;
                drawForwardedName = false;
                drawPhotoImage = false;
                if (AndroidUtilities.isTablet()) {
                    backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                } else {
                    backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                }
                availableTimeWidth = backgroundWidth - AndroidUtilities.dp(31);

                int maxWidth = getMaxNameWidth() - AndroidUtilities.dp(50);
                if (maxWidth < 0) {
                    maxWidth = AndroidUtilities.dp(10);
                }

                String text;
                String time = LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000);
                TLRPC.TL_messageActionPhoneCall call = (TLRPC.TL_messageActionPhoneCall) messageObject.messageOwner.action;
                boolean isMissed = call.reason instanceof TLRPC.TL_phoneCallDiscardReasonMissed;
                if (messageObject.isOutOwner()) {
                    if (isMissed) {
                        if (call.video) {
                            text = LocaleController.getString("CallMessageVideoOutgoingMissed", R.string.CallMessageVideoOutgoingMissed);
                        } else {
                            text = LocaleController.getString("CallMessageOutgoingMissed", R.string.CallMessageOutgoingMissed);
                        }
                    } else {
                        if (call.video) {
                            text = LocaleController.getString("CallMessageVideoOutgoing", R.string.CallMessageVideoOutgoing);
                        } else {
                            text = LocaleController.getString("CallMessageOutgoing", R.string.CallMessageOutgoing);
                        }
                    }
                } else {
                    if (isMissed) {
                        if (call.video) {
                            text = LocaleController.getString("CallMessageVideoIncomingMissed", R.string.CallMessageVideoIncomingMissed);
                        } else {
                            text = LocaleController.getString("CallMessageIncomingMissed", R.string.CallMessageIncomingMissed);
                        }
                    } else if (call.reason instanceof TLRPC.TL_phoneCallDiscardReasonBusy) {
                        if (call.video) {
                            text = LocaleController.getString("CallMessageVideoIncomingDeclined", R.string.CallMessageVideoIncomingDeclined);
                        } else {
                            text = LocaleController.getString("CallMessageIncomingDeclined", R.string.CallMessageIncomingDeclined);
                        }
                    } else {
                        if (call.video) {
                            text = LocaleController.getString("CallMessageVideoIncoming", R.string.CallMessageVideoIncoming);
                        } else {
                            text = LocaleController.getString("CallMessageIncoming", R.string.CallMessageIncoming);
                        }
                    }
                }
                if (call.duration > 0) {
                    time += ", " + LocaleController.formatCallDuration(call.duration);
                }

                titleLayout = new StaticLayout(TextUtils.ellipsize(text, Theme.chat_audioTitlePaint, maxWidth, TextUtils.TruncateAt.END), Theme.chat_audioTitlePaint, maxWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                docTitleLayout = new StaticLayout(TextUtils.ellipsize(time, Theme.chat_contactPhonePaint, maxWidth, TextUtils.TruncateAt.END), Theme.chat_contactPhonePaint, maxWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                setMessageObjectInternal(messageObject);

                totalHeight = AndroidUtilities.dp(65) + namesOffset;
                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }
            } else if (messageObject.type == 12) {
                drawName = messageObject.isFromGroup() && messageObject.isSupergroup() || messageObject.isImportedForward() && messageObject.messageOwner.fwd_from.from_id == null;
                drawForwardedName = !isRepliesChat;
                drawPhotoImage = true;
                photoImage.setRoundRadius(AndroidUtilities.dp(22));
                canChangeRadius = false;
                if (AndroidUtilities.isTablet()) {
                    backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                } else {
                    backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                }
                availableTimeWidth = backgroundWidth - AndroidUtilities.dp(31);

                long uid = messageObject.messageOwner.media.user_id;
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(uid);

                int maxWidth = getMaxNameWidth() - AndroidUtilities.dp(80);
                if (maxWidth < 0) {
                    maxWidth = AndroidUtilities.dp(10);
                }
                boolean hasName;
                if (user != null) {
                    contactAvatarDrawable.setInfo(user);
                    hasName = true;
                } else if (!TextUtils.isEmpty(messageObject.messageOwner.media.first_name) || !TextUtils.isEmpty(messageObject.messageOwner.media.last_name)) {
                    contactAvatarDrawable.setInfo(0, messageObject.messageOwner.media.first_name, messageObject.messageOwner.media.last_name);
                    hasName = true;
                } else {
                    hasName = false;
                }
                photoImage.setForUserOrChat(user, hasName ? contactAvatarDrawable : Theme.chat_contactDrawable[messageObject.isOutOwner() ? 1 : 0], messageObject);

                CharSequence phone;
                if (!TextUtils.isEmpty(messageObject.vCardData)) {
                    phone = messageObject.vCardData;
                    drawInstantView = true;
                    drawInstantViewType = 5;
                } else {
                    if (user != null && !TextUtils.isEmpty(user.phone)) {
                        phone = PhoneFormat.getInstance().format("+" + user.phone);
                    } else {
                        phone = messageObject.messageOwner.media.phone_number;
                        if (!TextUtils.isEmpty(phone)) {
                            phone = PhoneFormat.getInstance().format((String) phone);
                        } else {
                            phone = LocaleController.getString("NumberUnknown", R.string.NumberUnknown);
                        }
                    }
                }

                CharSequence currentNameString = ContactsController.formatName(messageObject.messageOwner.media.first_name, messageObject.messageOwner.media.last_name).replace('\n', ' ');
                if (currentNameString.length() == 0) {
                    currentNameString = messageObject.messageOwner.media.phone_number;
                    if (currentNameString == null) {
                        currentNameString = "";
                    }
                }
                titleLayout = new StaticLayout(TextUtils.ellipsize(currentNameString, Theme.chat_contactNamePaint, maxWidth, TextUtils.TruncateAt.END), Theme.chat_contactNamePaint, maxWidth + AndroidUtilities.dp(4), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                docTitleLayout = new StaticLayout(phone, Theme.chat_contactPhonePaint, maxWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, AndroidUtilities.dp(1), false);

                setMessageObjectInternal(messageObject);

                if (drawForwardedName && messageObject.needDrawForwarded() && (currentPosition == null || currentPosition.minY == 0)) {
                    namesOffset += AndroidUtilities.dp(5);
                } else if (drawNameLayout && messageObject.getReplyMsgId() == 0) {
                    namesOffset += AndroidUtilities.dp(7);
                }

                totalHeight = AndroidUtilities.dp(70 - 15) + namesOffset + docTitleLayout.getHeight();
                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }
                if (drawInstantView) {
                    createInstantViewButton();
                } else {
                    if (docTitleLayout.getLineCount() > 0) {
                        int timeLeft = backgroundWidth - AndroidUtilities.dp(40 + 18 + 44 + 8) - (int) Math.ceil(docTitleLayout.getLineWidth(docTitleLayout.getLineCount() - 1));
                        if (timeLeft < timeWidth) {
                            totalHeight += AndroidUtilities.dp(8);
                        }
                    }
                }
            } else if (messageObject.type == 2) {
                drawForwardedName = !isRepliesChat;
                drawName = messageObject.isFromGroup() && messageObject.isSupergroup() || messageObject.isImportedForward() && messageObject.messageOwner.fwd_from.from_id == null;
                if (AndroidUtilities.isTablet()) {
                    backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                } else {
                    backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                }
                createDocumentLayout(backgroundWidth, messageObject);

                setMessageObjectInternal(messageObject);

                totalHeight = AndroidUtilities.dp(70) + namesOffset;
                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }
            } else if (messageObject.type == 14) {
                drawName = (messageObject.isFromGroup() && messageObject.isSupergroup() || messageObject.isImportedForward() && messageObject.messageOwner.fwd_from.from_id == null) && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) != 0);
                if (AndroidUtilities.isTablet()) {
                    backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                } else {
                    backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(270));
                }

                createDocumentLayout(backgroundWidth, messageObject);

                setMessageObjectInternal(messageObject);

                totalHeight = AndroidUtilities.dp(82) + namesOffset;
                if (currentPosition != null && currentMessagesGroup != null && currentMessagesGroup.messages.size() > 1) {
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                        totalHeight -= AndroidUtilities.dp(6);
                        mediaOffsetY -= AndroidUtilities.dp(6);
                    }
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                        totalHeight -= AndroidUtilities.dp(6);
                    }
                }
                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }
            } else if (messageObject.type == MessageObject.TYPE_POLL) {
                if (timerParticles == null) {
                    timerParticles = new TimerParticles();
                }
                createSelectorDrawable(0);
                drawName = true;
                drawForwardedName = !isRepliesChat;
                drawPhotoImage = false;
                int maxWidth = Math.min(AndroidUtilities.dp(500), messageObject.getMaxMessageTextWidth());
                backgroundWidth = maxWidth + AndroidUtilities.dp(31);

                TLRPC.TL_messageMediaPoll media = (TLRPC.TL_messageMediaPoll) messageObject.messageOwner.media;

                timerTransitionProgress = media.poll.close_date - ConnectionsManager.getInstance(currentAccount).getCurrentTime() < 60 ? 0.0f : 1.0f;
                pollClosed = media.poll.closed;
                pollVoted = messageObject.isVoted();
                if (pollVoted) {
                    messageObject.checkedVotes.clear();
                }
                titleLayout = new StaticLayout(Emoji.replaceEmoji(media.poll.question, Theme.chat_audioTitlePaint.getFontMetricsInt(), AndroidUtilities.dp(16), false), Theme.chat_audioTitlePaint, maxWidth + AndroidUtilities.dp(2) - getExtraTextX() * 2, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                boolean titleRtl = false;
                if (titleLayout != null) {
                    for (int a = 0, N = titleLayout.getLineCount(); a < N; a++) {
                        if (titleLayout.getLineLeft(a) > 0) {
                            titleRtl = true;
                            break;
                        }
                    }
                }
                String title;
                if (pollClosed) {
                    title = LocaleController.getString("FinalResults", R.string.FinalResults);
                } else {
                    if (media.poll.quiz) {
                        if (media.poll.public_voters) {
                            title = LocaleController.getString("QuizPoll", R.string.QuizPoll);
                        } else {
                            title = LocaleController.getString("AnonymousQuizPoll", R.string.AnonymousQuizPoll);
                        }
                    } else if (media.poll.public_voters) {
                        title = LocaleController.getString("PublicPoll", R.string.PublicPoll);
                    } else {
                        title = LocaleController.getString("AnonymousPoll", R.string.AnonymousPoll);
                    }
                }
                docTitleLayout = new StaticLayout(TextUtils.ellipsize(title, Theme.chat_timePaint, maxWidth, TextUtils.TruncateAt.END), Theme.chat_timePaint, maxWidth + AndroidUtilities.dp(2) - getExtraTextX() * 2, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (docTitleLayout != null && docTitleLayout.getLineCount() > 0) {
                    if (titleRtl && !LocaleController.isRTL) {
                        docTitleOffsetX = (int) Math.ceil(maxWidth - docTitleLayout.getLineWidth(0));
                    } else if (!titleRtl && LocaleController.isRTL) {
                        docTitleOffsetX = -(int) Math.ceil(docTitleLayout.getLineLeft(0));
                    } else {
                        docTitleOffsetX = 0;
                    }
                }
                int w = maxWidth - AndroidUtilities.dp(messageObject.isOutOwner() ? 28 : 8);

                if (!isBot) {
                    TextPaint textPaint = !media.poll.public_voters && !media.poll.multiple_choice ? Theme.chat_livePaint : Theme.chat_locationAddressPaint;
                    CharSequence votes;
                    if (media.poll.quiz) {
                        votes = TextUtils.ellipsize(media.results.total_voters == 0 ? LocaleController.getString("NoVotesQuiz", R.string.NoVotesQuiz) : LocaleController.formatPluralString("Answer", media.results.total_voters), textPaint, w, TextUtils.TruncateAt.END);
                    } else {
                        votes = TextUtils.ellipsize(media.results.total_voters == 0 ? LocaleController.getString("NoVotes", R.string.NoVotes) : LocaleController.formatPluralString("Vote", media.results.total_voters), textPaint, w, TextUtils.TruncateAt.END);
                    }
                    infoLayout = new StaticLayout(votes, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    if (infoLayout != null) {
                        if (!media.poll.public_voters && !media.poll.multiple_choice) {
                            infoX = (int) Math.ceil(infoLayout.getLineCount() > 0 ? -infoLayout.getLineLeft(0) : 0);
                            availableTimeWidth = (int) (maxWidth - infoLayout.getLineWidth(0) - AndroidUtilities.dp(16));
                        } else {
                            infoX = (int) ((backgroundWidth - AndroidUtilities.dp(28) - Math.ceil(infoLayout.getLineWidth(0))) / 2 - infoLayout.getLineLeft(0));
                            availableTimeWidth = maxWidth;
                        }
                    }
                }
                measureTime(messageObject);

                lastPoll = media.poll;
                lastPollResults = media.results.results;
                lastPollResultsVoters = media.results.total_voters;
                if (media.poll.multiple_choice && !pollVoted && !pollClosed || !isBot && (media.poll.public_voters && pollVoted || pollClosed && media.results != null && media.results.total_voters != 0 && media.poll.public_voters)) {
                    drawInstantView = true;
                    drawInstantViewType = 8;
                    createInstantViewButton();
                }
                if (media.poll.multiple_choice) {
                    createPollUI();
                }
                if (media.results != null) {
                    createPollUI();
                    int size = media.results.recent_voters.size();
                    for (int a = 0; a < pollAvatarImages.length; a++) {
                        if (!isBot && a < size) {
                            pollAvatarImages[a].setImageCoords(0, 0, AndroidUtilities.dp(16), AndroidUtilities.dp(16));
                            Long id = media.results.recent_voters.get(a);
                            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(id);
                            if (user != null) {
                                pollAvatarDrawables[a].setInfo(user);
                                pollAvatarImages[a].setForUserOrChat(user, pollAvatarDrawables[a]);
                            } else {
                                pollAvatarDrawables[a].setInfo(id, "", "");
                            }
                            pollAvatarImagesVisible[a] = true;
                        } else if (!pollUnvoteInProgress || size != 0) {
                            pollAvatarImages[a].setImageBitmap((Drawable) null);
                            pollAvatarImagesVisible[a] = false;
                        }
                    }
                } else if (pollAvatarImages != null) {
                    for (int a = 0; a < pollAvatarImages.length; a++) {
                        pollAvatarImages[a].setImageBitmap((Drawable) null);
                        pollAvatarImagesVisible[a] = false;
                    }
                }

                int maxVote = 0;
                if (!animatePollAnswer && pollVoteInProgress && vibrateOnPollVote) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
                animatePollAnswerAlpha = animatePollAnswer = attachedToWindow && (pollVoteInProgress || pollUnvoteInProgress);
                ArrayList<PollButton> previousPollButtons = null;
                ArrayList<PollButton> sortedPollButtons = new ArrayList<>();
                if (!pollButtons.isEmpty()) {
                    previousPollButtons = new ArrayList<>(pollButtons);
                    pollButtons.clear();
                    if (!animatePollAnswer) {
                        animatePollAnswer = attachedToWindow && (pollVoted || pollClosed);
                    }
                    if (pollAnimationProgress > 0 && pollAnimationProgress < 1.0f) {
                        for (int b = 0, N2 = previousPollButtons.size(); b < N2; b++) {
                            PollButton button = previousPollButtons.get(b);
                            button.percent = (int) Math.ceil(button.prevPercent + (button.percent - button.prevPercent) * pollAnimationProgress);
                            button.percentProgress = button.prevPercentProgress + (button.percentProgress - button.prevPercentProgress) * pollAnimationProgress;
                        }
                    }
                }

                pollAnimationProgress = animatePollAnswer ? 0.0f : 1.0f;
                byte[] votingFor;
                if (!animatePollAnswerAlpha) {
                    pollVoteInProgress = false;
                    pollVoteInProgressNum = -1;
                    votingFor = SendMessagesHelper.getInstance(currentAccount).isSendingVote(currentMessageObject);
                } else {
                    votingFor = null;
                }

                int height = titleLayout != null ? titleLayout.getHeight() : 0;
                int restPercent = 100;
                boolean hasDifferent = false;
                int previousPercent = 0;
                for (int a = 0, N = media.poll.answers.size(); a < N; a++) {
                    PollButton button = new PollButton();
                    button.answer = media.poll.answers.get(a);
                    button.title = new StaticLayout(Emoji.replaceEmoji(button.answer.text, Theme.chat_audioPerformerPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false), Theme.chat_audioPerformerPaint, maxWidth - AndroidUtilities.dp(33), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    button.y = height + AndroidUtilities.dp(52);
                    button.height = button.title.getHeight();
                    pollButtons.add(button);
                    sortedPollButtons.add(button);
                    height += button.height + AndroidUtilities.dp(26);
                    if (!media.results.results.isEmpty()) {
                        for (int b = 0, N2 = media.results.results.size(); b < N2; b++) {
                            TLRPC.TL_pollAnswerVoters answer = media.results.results.get(b);
                            if (Arrays.equals(button.answer.option, answer.option)) {
                                button.chosen = answer.chosen;
                                button.count = answer.voters;
                                button.correct = answer.correct;
                                if ((pollVoted || pollClosed) && media.results.total_voters > 0) {
                                    button.decimal = 100 * (answer.voters / (float) media.results.total_voters);
                                    button.percent = (int) button.decimal;
                                    button.decimal -= button.percent;
                                } else {
                                    button.percent = 0;
                                    button.decimal = 0;
                                }
                                if (previousPercent == 0) {
                                    previousPercent = button.percent;
                                } else if (button.percent != 0 && previousPercent != button.percent) {
                                    hasDifferent = true;
                                }
                                restPercent -= button.percent;
                                maxVote = Math.max(button.percent, maxVote);
                                break;
                            }
                        }
                    }
                    if (previousPollButtons != null) {
                        for (int b = 0, N2 = previousPollButtons.size(); b < N2; b++) {
                            PollButton prevButton = previousPollButtons.get(b);
                            if (Arrays.equals(button.answer.option, prevButton.answer.option)) {
                                button.prevPercent = prevButton.percent;
                                button.prevPercentProgress = prevButton.percentProgress;
                                button.prevChosen = prevButton.chosen;
                                break;
                            }
                        }
                    }
                    if (votingFor != null && button.answer.option.length > 0 && Arrays.binarySearch(votingFor, button.answer.option[0]) >= 0) {
                        pollVoteInProgressNum = a;
                        pollVoteInProgress = true;
                        vibrateOnPollVote = true;
                        votingFor = null;
                    }

                    if (currentMessageObject.checkedVotes.contains(button.answer)) {
                        pollCheckBox[a].setChecked(true, false);
                    } else {
                        pollCheckBox[a].setChecked(false, false);
                    }
                }
                if (hasDifferent && restPercent != 0) {
                    Collections.sort(sortedPollButtons, (o1, o2) -> {
                        if (o1.decimal > o2.decimal) {
                            return -1;
                        } else if (o1.decimal < o2.decimal) {
                            return 1;
                        }
                        if (o1.decimal == o2.decimal) {
                            if (o1.percent > o2.percent) {
                                return 1;
                            } else if (o1.percent < o2.percent) {
                                return -1;
                            }
                        }
                        return 0;
                    });
                    for (int a = 0, N = Math.min(restPercent, sortedPollButtons.size()); a < N; a++) {
                        sortedPollButtons.get(a).percent += 1;
                    }
                }
                int width = backgroundWidth - AndroidUtilities.dp(76);
                for (int b = 0, N2 = pollButtons.size(); b < N2; b++) {
                    PollButton button = pollButtons.get(b);
                    button.percentProgress = Math.max(AndroidUtilities.dp(5) / (float) width, maxVote != 0 ? button.percent / (float) maxVote : 0);
                }

                setMessageObjectInternal(messageObject);


                if (isBot && !drawInstantView) {
                    height -= AndroidUtilities.dp(10);
                } else if (media.poll.public_voters || media.poll.multiple_choice) {
                    height += AndroidUtilities.dp(13);
                }
                totalHeight = AndroidUtilities.dp(46 + 27) + namesOffset + height;
                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }
                insantTextNewLine = false;
                if (media.poll.public_voters || media.poll.multiple_choice) {
                    int instantTextWidth = 0;
                    for (int a = 0; a < 3; a++) {
                        String str;
                        if (a == 0) {
                            str = LocaleController.getString("PollViewResults", R.string.PollViewResults);
                        } else if (a == 1) {
                            str = LocaleController.getString("PollSubmitVotes", R.string.PollSubmitVotes);
                        } else {
                            str = LocaleController.getString("NoVotes", R.string.NoVotes);
                        }
                        instantTextWidth = Math.max(instantTextWidth, (int) Math.ceil(Theme.chat_instantViewPaint.measureText(str)));
                    }
                    int timeWidthTotal = timeWidth + (messageObject.isOutOwner() ? AndroidUtilities.dp(20) : 0) + getExtraTimeX();
                    if (timeWidthTotal >= (backgroundWidth - AndroidUtilities.dp(76) - instantTextWidth) / 2) {
                        totalHeight += AndroidUtilities.dp(18);
                        insantTextNewLine = true;
                    }
                }
            } else {
                drawForwardedName = messageObject.messageOwner.fwd_from != null && !(messageObject.isAnyKindOfSticker() && messageObject.isDice());
                if (!messageObject.isAnyKindOfSticker() && messageObject.type != MessageObject.TYPE_ROUND_VIDEO) {
                    drawName = (messageObject.isFromGroup() && messageObject.isSupergroup() || messageObject.isImportedForward() && messageObject.messageOwner.fwd_from.from_id == null) && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) != 0);
                }
                mediaBackground = isMedia = messageObject.type != 9;
                drawImageButton = true;
                drawPhotoImage = true;

                int photoWidth = 0;
                int photoHeight = 0;
                int additionHeight = 0;

                if (messageObject.gifState != 2 && !SharedConfig.autoplayGifs && (messageObject.type == 8 || messageObject.type == MessageObject.TYPE_ROUND_VIDEO)) {
                    messageObject.gifState = 1;
                }

                photoImage.setAllowDecodeSingleFrame(true);
                if (messageObject.isVideo()) {
                    photoImage.setAllowStartAnimation(true);
                } else if (messageObject.isRoundVideo()) {
                    MessageObject playingMessage = MediaController.getInstance().getPlayingMessageObject();
                    photoImage.setAllowStartAnimation(playingMessage == null || !playingMessage.isRoundVideo());
                } else {
                    photoImage.setAllowStartAnimation(messageObject.gifState == 0);
                }

                photoImage.setForcePreview(messageObject.needDrawBluredPreview());
                if (messageObject.type == 9) {
                    if (AndroidUtilities.isTablet()) {
                        backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(300));
                    } else {
                        backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(300));
                    }
                    if (checkNeedDrawShareButton(messageObject)) {
                        backgroundWidth -= AndroidUtilities.dp(20);
                    }
                    int maxTextWidth = 0;
                    int maxWidth = backgroundWidth - AndroidUtilities.dp(86 + 52);
                    int widthForCaption = 0;
                    createDocumentLayout(maxWidth, messageObject);
                    if (!messageObject.isRestrictedMessage && !TextUtils.isEmpty(messageObject.caption)) {
                        try {
                            currentCaption = messageObject.caption;
                            int width = backgroundWidth - AndroidUtilities.dp(31);
                            widthForCaption = width - AndroidUtilities.dp(10) - getExtraTextX() * 2;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                captionLayout = StaticLayout.Builder.obtain(messageObject.caption, 0, messageObject.caption.length(), Theme.chat_msgTextPaint, widthForCaption)
                                        .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                                        .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                        .build();
                            } else {
                                captionLayout = new StaticLayout(messageObject.caption, Theme.chat_msgTextPaint, widthForCaption, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    if (docTitleLayout != null) {
                        for (int a = 0, N = docTitleLayout.getLineCount(); a < N; a++) {
                            maxTextWidth = Math.max(maxTextWidth, (int) Math.ceil(docTitleLayout.getLineWidth(a) + docTitleLayout.getLineLeft(a)) + AndroidUtilities.dp(86 + (drawPhotoImage ? 52 : 22)));
                        }
                    }
                    if (infoLayout != null) {
                        for (int a = 0, N = infoLayout.getLineCount(); a < N; a++) {
                            maxTextWidth = Math.max(maxTextWidth, infoWidth + AndroidUtilities.dp(86 + (drawPhotoImage ? 52 : 22)));
                        }
                    }
                    if (captionLayout != null) {
                        for (int a = 0, N = captionLayout.getLineCount(); a < N; a++) {
                            int w = (int) Math.ceil(Math.min(widthForCaption, captionLayout.getLineWidth(a) + captionLayout.getLineLeft(a))) + AndroidUtilities.dp(31);
                            if (w > maxTextWidth) {
                                maxTextWidth = w;
                            }
                        }
                    }

                    if (maxTextWidth > 0 && currentPosition == null) {
                        backgroundWidth = maxTextWidth;
                        maxWidth = maxTextWidth - AndroidUtilities.dp(31);
                    }
                    availableTimeWidth = maxWidth;
                    if (drawPhotoImage) {
                        photoWidth = AndroidUtilities.dp(86);
                        photoHeight = AndroidUtilities.dp(86);
                        availableTimeWidth -= photoWidth;
                    } else {
                        photoWidth = AndroidUtilities.dp(56);
                        photoHeight = AndroidUtilities.dp(56);
                        if (docTitleLayout != null && docTitleLayout.getLineCount() > 1) {
                            photoHeight += (docTitleLayout.getLineCount() - 1) * AndroidUtilities.dp(16);
                        }
                        if (TextUtils.isEmpty(messageObject.caption) && infoLayout != null) {
                            int lineCount = infoLayout.getLineCount();
                            measureTime(messageObject);
                            int timeLeft = backgroundWidth - AndroidUtilities.dp(40 + 18 + 56 + 8) - infoWidth;
                            if (timeLeft < timeWidth) {
                                photoHeight += AndroidUtilities.dp(12);
                            } else if (lineCount == 1) {
                                photoHeight += AndroidUtilities.dp(4);
                            }
                        }
                    }
                } else if (messageObject.type == 4) { //geo
                    TLRPC.GeoPoint point = messageObject.messageOwner.media.geo;
                    double lat = point.lat;
                    double lon = point._long;

                    int provider;
                    if ((int) messageObject.getDialogId() == 0) {
                        if (SharedConfig.mapPreviewType == 0) {
                            provider = -1;
                        } else if (SharedConfig.mapPreviewType == 1) {
                            provider = 4;
                        } else if (SharedConfig.mapPreviewType == 3) {
                            provider = 1;
                        } else {
                            provider = -1;
                        }
                    } else {
                        provider = -1;
                    }

                    if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGeoLive) {
                        if (AndroidUtilities.isTablet()) {
                            backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(252 + 37));
                        } else {
                            backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(252 + 37));
                        }
                        backgroundWidth -= AndroidUtilities.dp(4);
                        if (checkNeedDrawShareButton(messageObject)) {
                            backgroundWidth -= AndroidUtilities.dp(20);
                        }
                        int maxWidth = backgroundWidth - AndroidUtilities.dp(37);
                        availableTimeWidth = maxWidth;
                        maxWidth -= AndroidUtilities.dp(54);

                        photoWidth = backgroundWidth - AndroidUtilities.dp(17);
                        photoHeight = AndroidUtilities.dp(195);

                        int offset = 268435456;
                        double rad = offset / Math.PI;
                        double y = Math.round(offset - rad * Math.log((1 + Math.sin(lat * Math.PI / 180.0)) / (1 - Math.sin(lat * Math.PI / 180.0))) / 2) - (AndroidUtilities.dp(10.3f) << (21 - 15));
                        lat = (Math.PI / 2.0 - 2 * Math.atan(Math.exp((y - offset) / rad))) * 180.0 / Math.PI;
                        currentUrl = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), false, 15, provider);
                        lastWebFile = currentWebFile;
                        currentWebFile = WebFile.createWithGeoPoint(lat, lon, point.access_hash, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), 15, Math.min(2, (int) Math.ceil(AndroidUtilities.density)));

                        if (!(locationExpired = isCurrentLocationTimeExpired(messageObject))) {
                            photoImage.setCrossfadeWithOldImage(true);
                            mediaBackground = false;
                            additionHeight = AndroidUtilities.dp(56);
                            AndroidUtilities.runOnUIThread(invalidateRunnable, 1000);
                            scheduledInvalidate = true;
                        } else {
                            backgroundWidth -= AndroidUtilities.dp(9);
                        }
                        docTitleLayout = new StaticLayout(TextUtils.ellipsize(LocaleController.getString("AttachLiveLocation", R.string.AttachLiveLocation), Theme.chat_locationTitlePaint, maxWidth, TextUtils.TruncateAt.END), Theme.chat_locationTitlePaint, maxWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                        updateCurrentUserAndChat();
                        if (currentUser != null) {
                            contactAvatarDrawable.setInfo(currentUser);
                            locationImageReceiver.setForUserOrChat(currentUser, contactAvatarDrawable);
                        } else if (currentChat != null) {
                            if (currentChat.photo != null) {
                                currentPhoto = currentChat.photo.photo_small;
                            }
                            contactAvatarDrawable.setInfo(currentChat);
                            locationImageReceiver.setForUserOrChat(currentChat, contactAvatarDrawable);
                        } else {
                            locationImageReceiver.setImage(null, null, contactAvatarDrawable, null, null, 0);
                        }
                        infoLayout = new StaticLayout(TextUtils.ellipsize(LocaleController.formatLocationUpdateDate(messageObject.messageOwner.edit_date != 0 ? messageObject.messageOwner.edit_date : messageObject.messageOwner.date), Theme.chat_locationAddressPaint, maxWidth + AndroidUtilities.dp(2), TextUtils.TruncateAt.END), Theme.chat_locationAddressPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    } else if (!TextUtils.isEmpty(messageObject.messageOwner.media.title)) {
                        if (AndroidUtilities.isTablet()) {
                            backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(252 + 37));
                        } else {
                            backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(252 + 37));
                        }
                        backgroundWidth -= AndroidUtilities.dp(4);
                        if (checkNeedDrawShareButton(messageObject)) {
                            backgroundWidth -= AndroidUtilities.dp(20);
                        }
                        int maxWidth = backgroundWidth - AndroidUtilities.dp(34);
                        availableTimeWidth = maxWidth;

                        photoWidth = backgroundWidth - AndroidUtilities.dp(17);
                        photoHeight = AndroidUtilities.dp(195);

                        mediaBackground = false;
                        currentUrl = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), true, 15, provider);
                        currentWebFile = WebFile.createWithGeoPoint(point, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), 15, Math.min(2, (int) Math.ceil(AndroidUtilities.density)));

                        docTitleLayout = StaticLayoutEx.createStaticLayout(messageObject.messageOwner.media.title, Theme.chat_locationTitlePaint, maxWidth + AndroidUtilities.dp(4), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.END, maxWidth, 1);
                        additionHeight += AndroidUtilities.dp(50);
                        int lineCount = docTitleLayout.getLineCount();
                        if (!TextUtils.isEmpty(messageObject.messageOwner.media.address)) {
                            infoLayout = StaticLayoutEx.createStaticLayout(messageObject.messageOwner.media.address, Theme.chat_locationAddressPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.END, maxWidth, 1);
                            measureTime(messageObject);
                            int timeLeft = backgroundWidth - (int) Math.ceil(infoLayout.getLineWidth(0)) - AndroidUtilities.dp(24);
                            boolean isRtl = infoLayout.getLineLeft(0) > 0;
                            if (isRtl || timeLeft < timeWidth + AndroidUtilities.dp(20 + (messageObject.isOutOwner() ? 20 : 0))) {
                                additionHeight += AndroidUtilities.dp(isRtl ? 10 : 8);
                            }
                        } else {
                            infoLayout = null;
                        }
                    } else {
                        if (AndroidUtilities.isTablet()) {
                            backgroundWidth = Math.min(AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(252 + 37));
                        } else {
                            backgroundWidth = Math.min(getParentWidth() - AndroidUtilities.dp(drawAvatar ? 102 : 50), AndroidUtilities.dp(252 + 37));
                        }
                        backgroundWidth -= AndroidUtilities.dp(4);
                        if (checkNeedDrawShareButton(messageObject)) {
                            backgroundWidth -= AndroidUtilities.dp(20);
                        }
                        availableTimeWidth = backgroundWidth - AndroidUtilities.dp(34);

                        photoWidth = backgroundWidth - AndroidUtilities.dp(8);
                        photoHeight = AndroidUtilities.dp(195);

                        currentUrl = AndroidUtilities.formapMapUrl(currentAccount, lat, lon, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), true, 15, provider);
                        currentWebFile = WebFile.createWithGeoPoint(point, (int) (photoWidth / AndroidUtilities.density), (int) (photoHeight / AndroidUtilities.density), 15, Math.min(2, (int) Math.ceil(AndroidUtilities.density)));
                    }
                    if ((int) messageObject.getDialogId() == 0) {
                        if (SharedConfig.mapPreviewType == 0) {
                            currentMapProvider = 2;
                        } else if (SharedConfig.mapPreviewType == 1) {
                            currentMapProvider = 1;
                        } else if (SharedConfig.mapPreviewType == 3) {
                            currentMapProvider = 1;
                        } else {
                            currentMapProvider = -1;
                        }
                    } else {
                        currentMapProvider = MessagesController.getInstance(messageObject.currentAccount).mapProvider;
                    }
                    if (currentMapProvider == -1) {
                        photoImage.setImage(null, null, null, null, messageObject, 0);
                    } else if (currentMapProvider == 2) {
                        if (currentWebFile != null) {
                            ImageLocation lastLocation = lastWebFile == null ? null : ImageLocation.getForWebFile(lastWebFile);
                            photoImage.setImage(ImageLocation.getForWebFile(currentWebFile), null, lastLocation, null, (Drawable) null, messageObject, 0);
                        }
                    } else {
                        if (currentMapProvider == 3 || currentMapProvider == 4) {
                            ImageLoader.getInstance().addTestWebFile(currentUrl, currentWebFile);
                            addedForTest = true;
                        }
                        if (currentUrl != null) {
                            photoImage.setImage(currentUrl, null, null, null, 0);
                        }
                    }
                } else if (messageObject.isAnyKindOfSticker()) {
                    drawBackground = false;
                    boolean isWebpSticker = messageObject.type == MessageObject.TYPE_STICKER;
                    for (int a = 0; a < messageObject.getDocument().attributes.size(); a++) {
                        TLRPC.DocumentAttribute attribute = messageObject.getDocument().attributes.get(a);
                        if (attribute instanceof TLRPC.TL_documentAttributeImageSize) {
                            photoWidth = attribute.w;
                            photoHeight = attribute.h;
                            break;
                        }
                    }
                    if (messageObject.isAnimatedSticker() && photoWidth == 0 && photoHeight == 0) {
                        photoWidth = photoHeight = 512;
                    }
                    float maxHeight;
                    float maxWidth;
                    if (AndroidUtilities.isTablet()) {
                        maxHeight = maxWidth = AndroidUtilities.getMinTabletSide() * 0.4f;
                    } else {
                        maxHeight = maxWidth = Math.min(getParentWidth(), AndroidUtilities.displaySize.y) * 0.5f;
                    }
                    String filter;
                    if (messageObject.isAnimatedEmoji() || messageObject.isDice()) {
                        float zoom = MessagesController.getInstance(currentAccount).animatedEmojisZoom;
                        photoWidth = (int) ((photoWidth / 512.0f) * maxWidth * zoom);
                        photoHeight = (int) ((photoHeight / 512.0f) * maxHeight * zoom);
                    } else {
                        if (photoWidth == 0) {
                            photoHeight = (int) maxHeight;
                            photoWidth = photoHeight + AndroidUtilities.dp(100);
                        }
                        photoHeight *= maxWidth / photoWidth;
                        photoWidth = (int) maxWidth;
                        if (photoHeight > maxHeight) {
                            photoWidth *= maxHeight / photoHeight;
                            photoHeight = (int) maxHeight;
                        }
                    }
                    Object parentObject = messageObject;
                    int w = (int) (photoWidth / AndroidUtilities.density);
                    int h = (int) (photoHeight / AndroidUtilities.density);
                    boolean shouldRepeatSticker = delegate != null && delegate.shouldRepeatSticker(messageObject);
                    currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 40);
                    photoParentObject = messageObject.photoThumbsObject;
                    if (messageObject.isDice()) {
                        filter = String.format(Locale.US, "%d_%d_dice_%s_%s", w, h, messageObject.getDiceEmoji(), messageObject.toString());
                        photoImage.setAutoRepeat(2);
                        String emoji = currentMessageObject.getDiceEmoji();
                        TLRPC.TL_messages_stickerSet stickerSet = MediaDataController.getInstance(currentAccount).getStickerSetByEmojiOrName(emoji);
                        if (stickerSet != null) {
                            if (stickerSet.documents.size() > 0) {
                                int value = currentMessageObject.getDiceValue();
                                if (value <= 0) {
                                    TLRPC.Document document = stickerSet.documents.get(0);
                                    if ("\uD83C\uDFB0".equals(emoji)) {
                                        currentPhotoObjectThumb = null;
                                    } else {
                                        currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 40);
                                    }
                                    photoParentObject = document;
                                }
                            }
                        }
                    } else if (messageObject.isAnimatedEmoji()) {
                        filter = String.format(Locale.US, "%d_%d_nr_%s" + messageObject.emojiAnimatedStickerColor, w, h, messageObject.toString());
                        photoImage.setAutoRepeat(shouldRepeatSticker ? 2 : 3);
                        parentObject = MessageObject.getInputStickerSet(messageObject.emojiAnimatedSticker);
                    } else if (SharedConfig.loopStickers || isWebpSticker) {
                        filter = String.format(Locale.US, "%d_%d", w, h);
                        photoImage.setAutoRepeat(1);
                    } else {
                        filter = String.format(Locale.US, "%d_%d_nr_%s", w, h, messageObject.toString());
                        photoImage.setAutoRepeat(shouldRepeatSticker ? 2 : 3);
                    }
                    documentAttachType = DOCUMENT_ATTACH_TYPE_STICKER;
                    availableTimeWidth = photoWidth - AndroidUtilities.dp(14);
                    backgroundWidth = photoWidth + AndroidUtilities.dp(12);

                    photoImage.setRoundRadius(0);
                    canChangeRadius = false;
                    if (messageObject.pathThumb != null) {
                        photoImage.setImage(ImageLocation.getForDocument(messageObject.getDocument()), filter,
                                messageObject.pathThumb,
                                messageObject.getDocument().size, isWebpSticker ? "webp" : null, parentObject, 1);
                    } else if (messageObject.attachPathExists) {
                        photoImage.setImage(ImageLocation.getForPath(messageObject.messageOwner.attachPath), filter,
                                ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), "b1",
                                messageObject.getDocument().size, isWebpSticker ? "webp" : null, parentObject, 1);
                    } else if (messageObject.getDocument().id != 0) {
                        photoImage.setImage(ImageLocation.getForDocument(messageObject.getDocument()), filter,
                                ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), "b1",
                                messageObject.getDocument().size, isWebpSticker ? "webp" : null, parentObject, 1);
                    } else {
                        photoImage.setImage(null, null, null, null, messageObject, 0);
                    }
                } else {
                    currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, AndroidUtilities.getPhotoSize());
                    photoParentObject = messageObject.photoThumbsObject;
                    boolean useFullWidth = false;
                    if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        documentAttach = messageObject.getDocument();
                        documentAttachType = DOCUMENT_ATTACH_TYPE_ROUND;
                    } else {
                        if (AndroidUtilities.isTablet()) {
                            photoWidth = (int) (AndroidUtilities.getMinTabletSide() * 0.7f);
                        } else {
                            if (currentPhotoObject != null && (messageObject.type == MessageObject.TYPE_PHOTO || messageObject.type == MessageObject.TYPE_VIDEO || messageObject.type == 8) && currentPhotoObject.w >= currentPhotoObject.h) {
                                photoWidth = Math.min(getParentWidth(), AndroidUtilities.displaySize.y) - AndroidUtilities.dp(64 + (checkNeedDrawShareButton(messageObject) ? 10 : 0));
                                useFullWidth = true;
                            } else {
                                photoWidth = (int) (Math.min(getParentWidth(), AndroidUtilities.displaySize.y) * 0.7f);
                            }
                        }
                    }
                    photoHeight = photoWidth + AndroidUtilities.dp(100);
                    if (!useFullWidth) {
                        if (messageObject.type != 5 && checkNeedDrawShareButton(messageObject)) {
                            photoWidth -= AndroidUtilities.dp(20);
                        }
                        if (photoWidth > AndroidUtilities.getPhotoSize()) {
                            photoWidth = AndroidUtilities.getPhotoSize();
                        }
                        if (photoHeight > AndroidUtilities.getPhotoSize()) {
                            photoHeight = AndroidUtilities.getPhotoSize();
                        }
                    } else if (drawAvatar) {
                        photoWidth -= AndroidUtilities.dp(52);
                    }

                    boolean needQualityPreview = false;

                    if (messageObject.type == MessageObject.TYPE_PHOTO) { //photo
                        updateSecretTimeText(messageObject);
                        currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 40);
                    } else if (messageObject.type == MessageObject.TYPE_VIDEO || messageObject.type == 8) { //video, gif
                        createDocumentLayout(0, messageObject);
                        currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 40);
                        updateSecretTimeText(messageObject);
                        needQualityPreview = true;
                    } else if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 40);
                        needQualityPreview = true;
                    }
                    int w;
                    int h;
                    if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        if (isPlayingRound) {
                            w = h = AndroidUtilities.roundPlayingMessageSize;
                        } else {
                            w = h = AndroidUtilities.roundMessageSize;
                        }
                    } else {
                        TLRPC.PhotoSize size = currentPhotoObject != null ? currentPhotoObject : currentPhotoObjectThumb;
                        int imageW = 0;
                        int imageH = 0;
                        if (size != null && !(size instanceof TLRPC.TL_photoStrippedSize)) {
                            imageW = size.w;
                            imageH = size.h;
                        } else if (documentAttach != null) {
                            for (int a = 0, N = documentAttach.attributes.size(); a < N; a++) {
                                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                                if (attribute instanceof TLRPC.TL_documentAttributeVideo) {
                                    imageW = attribute.w;
                                    imageH = attribute.h;
                                }
                            }
                        }
                        Point point = getMessageSize(imageW, imageH, photoWidth, photoHeight);
                        w = (int) point.x;
                        h = (int) point.y;
                    }
                    if (currentPhotoObject != null && "s".equals(currentPhotoObject.type)) {
                        currentPhotoObject = null;
                    }

                    if (currentPhotoObject != null && currentPhotoObject == currentPhotoObjectThumb) {
                        if (messageObject.type == MessageObject.TYPE_PHOTO) {
                            currentPhotoObjectThumb = null;
                        } else {
                            currentPhotoObject = null;
                        }
                    }

                    if (needQualityPreview) {
                        /*if ((DownloadController.getInstance(currentAccount).getAutodownloadMask() & DownloadController.AUTODOWNLOAD_TYPE_PHOTO) == 0) {
                            currentPhotoObject = null;
                        }*/
                        if (!messageObject.needDrawBluredPreview() && (currentPhotoObject == null || currentPhotoObject == currentPhotoObjectThumb) && (currentPhotoObjectThumb == null || !"m".equals(currentPhotoObjectThumb.type))) {
                            photoImage.setNeedsQualityThumb(true);
                            photoImage.setShouldGenerateQualityThumb(true);
                        }
                    }

                    if (currentMessagesGroup == null && messageObject.caption != null) {
                        mediaBackground = false;
                    }

                    if ((w == 0 || h == 0) && messageObject.type == 8) {
                        for (int a = 0; a < messageObject.getDocument().attributes.size(); a++) {
                            TLRPC.DocumentAttribute attribute = messageObject.getDocument().attributes.get(a);
                            if (attribute instanceof TLRPC.TL_documentAttributeImageSize || attribute instanceof TLRPC.TL_documentAttributeVideo) {
                                float scale = (float) attribute.w / (float) photoWidth;
                                w = (int) (attribute.w / scale);
                                h = (int) (attribute.h / scale);
                                if (h > photoHeight) {
                                    float scale2 = h;
                                    h = photoHeight;
                                    scale2 /= h;
                                    w = (int) (w / scale2);
                                } else if (h < AndroidUtilities.dp(120)) {
                                    h = AndroidUtilities.dp(120);
                                    float hScale = (float) attribute.h / h;
                                    if (attribute.w / hScale < photoWidth) {
                                        w = (int) (attribute.w / hScale);
                                    }
                                }
                                break;
                            }
                        }
                    }

                    if (w == 0 || h == 0) {
                        w = h = AndroidUtilities.dp(150);
                    }
                    if (messageObject.type == MessageObject.TYPE_VIDEO) {
                        if (w < infoWidth + AndroidUtilities.dp(16 + 24)) {
                            w = infoWidth + AndroidUtilities.dp(16 + 24);
                        }
                    }
                    if (commentLayout != null && drawSideButton != 3 && w < totalCommentWidth + AndroidUtilities.dp(10)) {
                        w = totalCommentWidth + AndroidUtilities.dp(10);
                    }

                    if (currentMessagesGroup != null) {
                        int firstLineWidth = 0;
                        int dWidth = getGroupPhotosWidth();
                        for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                            MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                            if (position.minY == 0) {
                                firstLineWidth += Math.ceil((position.pw + position.leftSpanOffset) / 1000.0f * dWidth);
                            } else {
                                break;
                            }
                        }
                        availableTimeWidth = firstLineWidth - AndroidUtilities.dp(35);
                    } else {
                        availableTimeWidth = photoWidth - AndroidUtilities.dp(14);
                    }

                    if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        availableTimeWidth = (int) (AndroidUtilities.roundMessageSize - Math.ceil(Theme.chat_audioTimePaint.measureText("00:00")) + AndroidUtilities.dp(40));
                    }
                    measureTime(messageObject);
                    int timeWidthTotal = timeWidth + AndroidUtilities.dp((SharedConfig.bubbleRadius >= 10 ? 22 : 18) + (messageObject.isOutOwner() ? 20 : 0));
                    if (w < timeWidthTotal) {
                        w = timeWidthTotal;
                    }

                    if (messageObject.isRoundVideo()) {
                        w = h = Math.min(w, h);
                        drawBackground = false;
                        photoImage.setRoundRadius(w / 2);
                        canChangeRadius = false;
                    } else if (messageObject.needDrawBluredPreview()) {
                        if (AndroidUtilities.isTablet()) {
                            w = h = (int) (AndroidUtilities.getMinTabletSide() * 0.5f);
                        } else {
                            w = h = (int) (Math.min(getParentWidth(), AndroidUtilities.displaySize.y) * 0.5f);
                        }
                    }

                    int widthForCaption = 0;
                    boolean fixPhotoWidth = false;
                    if (currentMessagesGroup != null) {
                        float maxHeight = Math.max(getParentWidth(), AndroidUtilities.displaySize.y) * 0.5f;
                        int dWidth = getGroupPhotosWidth();
                        w = (int) Math.ceil(currentPosition.pw / 1000.0f * dWidth);
                        if (currentPosition.minY != 0 && (messageObject.isOutOwner() && (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0 || !messageObject.isOutOwner() && (currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0)) {
                            int firstLineWidth = 0;
                            int currentLineWidth = 0;
                            for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                                MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                                if (position.minY == 0) {
                                    firstLineWidth += Math.ceil(position.pw / 1000.0f * dWidth) + (position.leftSpanOffset != 0 ? Math.ceil(position.leftSpanOffset / 1000.0f * dWidth) : 0);
                                } else if (position.minY == currentPosition.minY) {
                                    currentLineWidth += Math.ceil((position.pw) / 1000.0f * dWidth) + (position.leftSpanOffset != 0 ? Math.ceil(position.leftSpanOffset / 1000.0f * dWidth) : 0);
                                } else if (position.minY > currentPosition.minY) {
                                    break;
                                }
                            }
                            w += firstLineWidth - currentLineWidth;
                        }
                        w -= AndroidUtilities.dp(9);
                        if (isAvatarVisible) {
                            w -= AndroidUtilities.dp(48);
                        }
                        if (currentPosition.siblingHeights != null) {
                            h = 0;
                            for (int a = 0; a < currentPosition.siblingHeights.length; a++) {
                                h += (int) Math.ceil(maxHeight * currentPosition.siblingHeights[a]);
                            }
                            h += (currentPosition.maxY - currentPosition.minY) * Math.round(7 * AndroidUtilities.density); //TODO fix
                        } else {
                            h = (int) Math.ceil(maxHeight * currentPosition.ph);
                        }
                        backgroundWidth = w;
                        if ((currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0 && (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0) {
                            w -= AndroidUtilities.dp(8);
                        } else if ((currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) == 0 && (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                            w -= AndroidUtilities.dp(11);
                        } else if ((currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0) {
                            w -= AndroidUtilities.dp(10);
                        } else {
                            w -= AndroidUtilities.dp(9);
                        }
                        photoWidth = w;
                        if (!currentPosition.edge) {
                            photoWidth += AndroidUtilities.dp(10);
                        }
                        photoHeight = h;
                        widthForCaption += photoWidth - AndroidUtilities.dp(10);
                        if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0 || currentMessagesGroup.hasSibling && (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                            widthForCaption += getAdditionalWidthForPosition(currentPosition);
                            int count = currentMessagesGroup.messages.size();
                            for (int i = 0; i < count; i++) {
                                MessageObject m = currentMessagesGroup.messages.get(i);
                                MessageObject.GroupedMessagePosition rowPosition = currentMessagesGroup.posArray.get(i);
                                if (rowPosition != currentPosition && (rowPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0) {
                                    w = (int) Math.ceil(rowPosition.pw / 1000.0f * dWidth);
                                    if (rowPosition.minY != 0 && (messageObject.isOutOwner() && (rowPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0 || !messageObject.isOutOwner() && (rowPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0)) {
                                        int firstLineWidth = 0;
                                        int currentLineWidth = 0;
                                        for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                                            MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                                            if (position.minY == 0) {
                                                firstLineWidth += Math.ceil(position.pw / 1000.0f * dWidth) + (position.leftSpanOffset != 0 ? Math.ceil(position.leftSpanOffset / 1000.0f * dWidth) : 0);
                                            } else if (position.minY == rowPosition.minY) {
                                                currentLineWidth += Math.ceil((position.pw) / 1000.0f * dWidth) + (position.leftSpanOffset != 0 ? Math.ceil(position.leftSpanOffset / 1000.0f * dWidth) : 0);
                                            } else if (position.minY > rowPosition.minY) {
                                                break;
                                            }
                                        }
                                        w += firstLineWidth - currentLineWidth;
                                    }
                                    w -= AndroidUtilities.dp(9);
                                    if ((rowPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0 && (rowPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0) {
                                        w -= AndroidUtilities.dp(8);
                                    } else if ((rowPosition.flags & MessageObject.POSITION_FLAG_RIGHT) == 0 && (rowPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                                        w -= AndroidUtilities.dp(11);
                                    } else if ((rowPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0) {
                                        w -= AndroidUtilities.dp(10);
                                    } else {
                                        w -= AndroidUtilities.dp(9);
                                    }
                                    if (isChat && !isThreadPost && !m.isOutOwner() && m.needDrawAvatar() && (rowPosition == null || rowPosition.edge)) {
                                        w -= AndroidUtilities.dp(48);
                                    }
                                    w += getAdditionalWidthForPosition(rowPosition);
                                    if (!rowPosition.edge) {
                                        w += AndroidUtilities.dp(10);
                                    }
                                    widthForCaption += w;
                                    if (rowPosition.minX < currentPosition.minX || currentMessagesGroup.hasSibling && rowPosition.minY != rowPosition.maxY) {
                                        captionOffsetX -= w;
                                    }
                                }
                                if (m.caption != null) {
                                    if (currentCaption != null) {
                                        currentCaption = null;
                                        break;
                                    } else {
                                        currentCaption = m.caption;
                                    }
                                }
                            }
                        }
                    } else {
                        photoHeight = h;
                        photoWidth = w;
                        currentCaption = messageObject.caption;

                        int minCaptionWidth;
                        if (AndroidUtilities.isTablet()) {
                            minCaptionWidth = (int) (AndroidUtilities.getMinTabletSide() * 0.65f);
                        } else {
                            minCaptionWidth = (int) (Math.min(getParentWidth(), AndroidUtilities.displaySize.y) * 0.65f);
                        }
                        if (!messageObject.needDrawBluredPreview() && currentCaption != null && photoWidth < minCaptionWidth) {
                            widthForCaption = minCaptionWidth;
                            fixPhotoWidth = true;
                        } else {
                            widthForCaption = photoWidth - AndroidUtilities.dp(10);
                        }

                        backgroundWidth = photoWidth + AndroidUtilities.dp(8);
                        if (!mediaBackground) {
                            backgroundWidth += AndroidUtilities.dp(9);
                        }
                    }

                    if (currentCaption != null) {
                        try {
                            widthForCaption -= getExtraTextX() * 2;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                captionLayout = StaticLayout.Builder.obtain(currentCaption, 0, currentCaption.length(), Theme.chat_msgTextPaint, widthForCaption)
                                        .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                                        .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                        .build();
                            } else {
                                captionLayout = new StaticLayout(currentCaption, Theme.chat_msgTextPaint, widthForCaption, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                            }
                            int lineCount = captionLayout.getLineCount();
                            if (lineCount > 0) {
                                if (fixPhotoWidth) {
                                    captionWidth = 0;
                                    for (int a = 0; a < lineCount; a++) {
                                        captionWidth = (int) Math.max(captionWidth, Math.ceil(captionLayout.getLineWidth(a)));
                                        if (captionLayout.getLineLeft(a) != 0) {
                                            captionWidth = widthForCaption;
                                            break;
                                        }
                                    }
                                    if (captionWidth > widthForCaption) {
                                        captionWidth = widthForCaption;
                                    }
                                } else {
                                    captionWidth = widthForCaption;
                                }
                                captionHeight = captionLayout.getHeight();
                                addedCaptionHeight = captionHeight + AndroidUtilities.dp(9);
                                if (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0) {
                                    additionHeight += addedCaptionHeight;
                                    int widthToCheck = Math.max(captionWidth, photoWidth - AndroidUtilities.dp(10));
                                    float lastLineWidth = captionLayout.getLineWidth(captionLayout.getLineCount() - 1) + captionLayout.getLineLeft(captionLayout.getLineCount() - 1);
                                    if (!shouldDrawTimeOnMedia() && widthToCheck + AndroidUtilities.dp(2) - lastLineWidth < timeWidthTotal + getExtraTimeX()) {
                                        additionHeight += AndroidUtilities.dp(14);
                                        addedCaptionHeight += AndroidUtilities.dp(14);
                                        captionNewLine = 1;
                                    }
                                } else {
                                    captionLayout = null;
                                }
                            }
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                    }

                    int minWidth = (int) (Theme.chat_infoPaint.measureText("100%") + AndroidUtilities.dp(100/*48*/)/* + timeWidth*/);
                    if (currentMessagesGroup == null && (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) && photoWidth < minWidth) {
                        photoWidth = minWidth;
                        backgroundWidth = photoWidth + AndroidUtilities.dp(8);
                        if (!mediaBackground) {
                            backgroundWidth += AndroidUtilities.dp(9);
                        }
                    }

                    if (fixPhotoWidth && photoWidth < captionWidth + AndroidUtilities.dp(10)) {
                        photoWidth = captionWidth + AndroidUtilities.dp(10);
                        backgroundWidth = photoWidth + AndroidUtilities.dp(8);
                        if (!mediaBackground) {
                            backgroundWidth += AndroidUtilities.dp(9);
                        }
                    }
                    if (messageChanged || messageIdChanged || dataChanged) {
                        currentPhotoFilter = currentPhotoFilterThumb = String.format(Locale.US, "%d_%d", (int) (w / AndroidUtilities.density), (int) (h / AndroidUtilities.density));
                        if (messageObject.photoThumbs != null && messageObject.photoThumbs.size() > 1 || messageObject.type == MessageObject.TYPE_VIDEO || messageObject.type == 8 || messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                            if (messageObject.needDrawBluredPreview()) {
                                currentPhotoFilter += "_b2";
                                currentPhotoFilterThumb += "_b2";
                            } else {
                                currentPhotoFilterThumb += "_b";
                            }
                        }
                    } else {
                        String filterNew = String.format(Locale.US, "%d_%d", (int) (w / AndroidUtilities.density), (int) (h / AndroidUtilities.density));
                        if (!messageObject.needDrawBluredPreview() && !filterNew.equals(currentPhotoFilter)) {
                            ImageLocation location = ImageLocation.getForObject(currentPhotoObject, photoParentObject);
                            if (location != null) {
                                String key = location.getKey(photoParentObject, null, false) + "@" + currentPhotoFilter;
                                if (ImageLoader.getInstance().isInMemCache(key, false)) {
                                    currentPhotoObjectThumb = currentPhotoObject;
                                    currentPhotoFilterThumb = currentPhotoFilter;
                                    currentPhotoFilter = filterNew;
                                }
                            }
                        }
                    }

                    boolean noSize = false;
                    if (messageObject.type == MessageObject.TYPE_VIDEO || messageObject.type == 8 || messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        noSize = true;
                    }
                    if (currentPhotoObject != null && !noSize && currentPhotoObject.size == 0) {
                        currentPhotoObject.size = -1;
                    }
                    if (currentPhotoObjectThumb != null && !noSize && currentPhotoObjectThumb.size == 0) {
                        currentPhotoObjectThumb.size = -1;
                    }

                    if (SharedConfig.autoplayVideo && messageObject.type == MessageObject.TYPE_VIDEO && !messageObject.needDrawBluredPreview() &&
                            (currentMessageObject.mediaExists || messageObject.canStreamVideo() && DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject))
                    ) {
                        if (currentPosition != null) {
                            autoPlayingMedia = (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0 && (currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0;
                        } else {
                            autoPlayingMedia = true;
                        }
                    }

                    if (autoPlayingMedia) {
                        photoImage.setAllowStartAnimation(true);
                        photoImage.startAnimation();
                        TLRPC.Document document = messageObject.getDocument();

                        if (currentMessageObject.videoEditedInfo != null && currentMessageObject.videoEditedInfo.canAutoPlaySourceVideo()) {
                            /*TODO*/photoImage.setImage(ImageLocation.getForPath(currentMessageObject.videoEditedInfo.originalPath), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObjectThumb, document), currentPhotoFilterThumb, null, messageObject.getDocument().size, null, messageObject, 0);
                            photoImage.setMediaStartEndTime(currentMessageObject.videoEditedInfo.startTime / 1000, currentMessageObject.videoEditedInfo.endTime / 1000);
                        } else {
                            if (!messageIdChanged && !dataChanged) {
                                photoImage.setCrossfadeWithOldImage(true);
                            }
                            /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObjectThumb, document), currentPhotoFilterThumb, null, messageObject.getDocument().size, null, messageObject, 0);
                        }
                    } else if (messageObject.type == MessageObject.TYPE_PHOTO) {
                        if (messageObject.useCustomPhoto) {
                            photoImage.setImageBitmap(getResources().getDrawable(R.drawable.theme_preview_image));
                        } else {
                            if (currentPhotoObject != null) {
                                boolean photoExist = true;
                                String fileName = FileLoader.getAttachFileName(currentPhotoObject);
                                if (messageObject.mediaExists) {
                                    DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                                } else {
                                    photoExist = false;
                                }
                                if (photoExist || !currentMessageObject.loadingCancelled && DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject) || FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                                    photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, currentPhotoObject.size, null, currentMessageObject, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                                } else {
                                    photoNotSet = true;
                                    if (currentPhotoObjectThumb != null) {
                                        photoImage.setImage(null, null, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, currentMessageObject, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                                    } else {
                                        photoImage.setImageBitmap((Drawable) null);
                                    }
                                }
                            } else {
                                photoImage.setImageBitmap((Drawable) null);
                            }
                        }
                    } else if (messageObject.type == 8 || messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        String fileName = FileLoader.getAttachFileName(messageObject.getDocument());
                        int localFile = 0;
                        if (messageObject.attachPathExists) {
                            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                            localFile = 1;
                        } else if (messageObject.mediaExists) {
                            localFile = 2;
                        }
                        boolean autoDownload = false;
                        TLRPC.Document document = messageObject.getDocument();
                        if (MessageObject.isGifDocument(document, messageObject.hasValidGroupId()) || messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                            autoDownload = DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject);
                        }
                        TLRPC.VideoSize videoSize = MessageObject.getDocumentVideoThumb(document);
                        if (((MessageObject.isGifDocument(document, messageObject.hasValidGroupId()) && messageObject.videoEditedInfo == null) || (!messageObject.isSending() && !messageObject.isEditing())) && (localFile != 0 || FileLoader.getInstance(currentAccount).isLoadingFile(fileName) || autoDownload)) {
                            if (localFile != 1 && !messageObject.needDrawBluredPreview() && (localFile != 0 || messageObject.canStreamVideo() && autoDownload)) {
                                autoPlayingMedia = true;
                                if (!messageIdChanged) {
                                    photoImage.setCrossfadeWithOldImage(true);
                                    photoImage.setCrossfadeDuration(250);
                                }
                                if (localFile == 0 && videoSize != null && (currentPhotoObject == null || currentPhotoObjectThumb == null)) {
                                    /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForDocument(videoSize, documentAttach), null, ImageLocation.getForDocument(currentPhotoObject != null ? currentPhotoObject : currentPhotoObjectThumb, documentAttach), currentPhotoObject != null ? currentPhotoFilter : currentPhotoFilterThumb, null, document.size, null, messageObject, 0);
                                } else {
                                    if (isRoundVideo && !messageIdChanged && photoImage.hasStaticThumb()) {
                                        /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, null, null, photoImage.getStaticThumb(), document.size, null, messageObject, 0);
                                    } else {
                                        /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, null, document.size, null, messageObject, 0);
                                    }
                                }
                            } else if (localFile == 1) {
                                /*TODO*/photoImage.setImage(ImageLocation.getForPath(messageObject.isSendError() ? null : messageObject.messageOwner.attachPath), null, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, null, 0, null, messageObject, 0);
                            } else {
                                if (videoSize != null && (currentPhotoObject == null || currentPhotoObjectThumb == null)) {
                                    /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), null, ImageLocation.getForDocument(videoSize, documentAttach), null, ImageLocation.getForDocument(currentPhotoObject != null ? currentPhotoObject : currentPhotoObjectThumb, documentAttach), currentPhotoObject != null ? currentPhotoFilter : currentPhotoFilterThumb, null, document.size, null, messageObject, 0);
                                } else {
                                    /*TODO*/photoImage.setImage(ImageLocation.getForDocument(document), null, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, null, document.size, null, messageObject, 0);
                                }
                            }
                        } else {
                            if (messageObject.videoEditedInfo != null && messageObject.type == MessageObject.TYPE_ROUND_VIDEO && !currentMessageObject.needDrawBluredPreview()) {
                                /*TODO*/photoImage.setImage(ImageLocation.getForPath(messageObject.videoEditedInfo.originalPath), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, messageObject, 0);
                                photoImage.setMediaStartEndTime(currentMessageObject.videoEditedInfo.startTime / 1000, currentMessageObject.videoEditedInfo.endTime / 1000);
                            } else {
                                if (!messageIdChanged && !currentMessageObject.needDrawBluredPreview()) {
                                    photoImage.setCrossfadeWithOldImage(true);
                                    photoImage.setCrossfadeDuration(250);
                                }
                                /*TODO*/photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, messageObject, 0);
                            }
                        }
                    } else {
                        if (messageObject.videoEditedInfo != null && messageObject.type == MessageObject.TYPE_ROUND_VIDEO && !currentMessageObject.needDrawBluredPreview()) {
                            /*TODO*/photoImage.setImage(ImageLocation.getForPath(messageObject.videoEditedInfo.originalPath), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, messageObject, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                            photoImage.setMediaStartEndTime(currentMessageObject.videoEditedInfo.startTime / 1000, currentMessageObject.videoEditedInfo.endTime / 1000);
                        } else {
                            if (!messageIdChanged && !currentMessageObject.needDrawBluredPreview()) {
                                photoImage.setCrossfadeWithOldImage(true);
                                photoImage.setCrossfadeDuration(250);
                            }
                            /*TODO*/photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, messageObject, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                        }
                    }
                }
                setMessageObjectInternal(messageObject);

                if (drawForwardedName && messageObject.needDrawForwarded() && (currentPosition == null || currentPosition.minY == 0)) {
                    if (messageObject.type != 5) {
                        namesOffset += AndroidUtilities.dp(5);
                    }
                } else if (drawNameLayout && (messageObject.getReplyMsgId() == 0 || isThreadChat && messageObject.getReplyTopMsgId() == 0)) {
                    namesOffset += AndroidUtilities.dp(7);
                }
                totalHeight = photoHeight + AndroidUtilities.dp(14) + namesOffset + additionHeight;
                if (currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0 && !currentMessageObject.isDocument()) {
                    totalHeight -= AndroidUtilities.dp(3);
                }
                if (currentMessageObject.isDice()) {
                    totalHeight += AndroidUtilities.dp(21);
                    additionalTimeOffsetY = AndroidUtilities.dp(21);
                }

                int additionalTop = 0;
                if (currentPosition != null && !currentMessageObject.isDocument()) {
                    photoWidth += getAdditionalWidthForPosition(currentPosition);
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                        photoHeight += AndroidUtilities.dp(4);
                        additionalTop -= AndroidUtilities.dp(4);
                    }
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                        photoHeight += AndroidUtilities.dp(1);
                    }
                }

                if (drawPinnedTop) {
                    namesOffset -= AndroidUtilities.dp(1);
                }

                int y;
                if (namesOffset > 0) {
                    y = AndroidUtilities.dp(7);
                    totalHeight -= AndroidUtilities.dp(2);
                } else {
                    y = AndroidUtilities.dp(5);
                    totalHeight -= AndroidUtilities.dp(4);
                }
                if (currentPosition != null && currentMessagesGroup.isDocuments && currentMessagesGroup.messages.size() > 1) {
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                        totalHeight -= AndroidUtilities.dp(drawPhotoImage ? 3 : 6);
                        mediaOffsetY -= AndroidUtilities.dp(drawPhotoImage ? 3 : 6);
                        y -= AndroidUtilities.dp(drawPhotoImage ? 3 : 6);
                    }
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                        totalHeight -= AndroidUtilities.dp(drawPhotoImage ? 3 : 6);
                    }
                }
                photoImage.setImageCoords(0, y + namesOffset + additionalTop, photoWidth, photoHeight);
                invalidate();
            }

            if ((currentPosition == null || currentMessageObject.isMusic() || currentMessageObject.isDocument()) && !messageObject.isAnyKindOfSticker() && addedCaptionHeight == 0) {
                if (!messageObject.isRestrictedMessage && captionLayout == null && messageObject.caption != null) {
                    try {
                        currentCaption = messageObject.caption;
                        int width = backgroundWidth - AndroidUtilities.dp(31);
                        int widthForCaption = width - AndroidUtilities.dp(10) - getExtraTextX() * 2;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            captionLayout = StaticLayout.Builder.obtain(messageObject.caption, 0, messageObject.caption.length(), Theme.chat_msgTextPaint, widthForCaption)
                                    .setBreakStrategy(StaticLayout.BREAK_STRATEGY_HIGH_QUALITY)
                                    .setHyphenationFrequency(StaticLayout.HYPHENATION_FREQUENCY_NONE)
                                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                                    .build();
                        } else {
                            captionLayout = new StaticLayout(messageObject.caption, Theme.chat_msgTextPaint, widthForCaption, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (captionLayout != null) {
                    try {
                        int width = backgroundWidth - AndroidUtilities.dp(31);
                        if (captionLayout != null && captionLayout.getLineCount() > 0) {
                            captionWidth = width;
                            captionHeight = captionLayout.getHeight();
                            totalHeight += captionHeight + AndroidUtilities.dp(9);
                            if (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0) {
                                int timeWidthTotal = timeWidth + (messageObject.isOutOwner() ? AndroidUtilities.dp(20) : 0) + getExtraTimeX();
                                float lastLineWidth = captionLayout.getLineWidth(captionLayout.getLineCount() - 1) + captionLayout.getLineLeft(captionLayout.getLineCount() - 1);
                                if (width - AndroidUtilities.dp(8) - lastLineWidth < timeWidthTotal) {
                                    totalHeight += AndroidUtilities.dp(14);
                                    captionHeight += AndroidUtilities.dp(14);
                                    captionNewLine = 2;
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            }
            if ((currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0) && captionLayout == null && widthBeforeNewTimeLine != -1 && availableTimeWidth - widthBeforeNewTimeLine < timeWidth) {
                totalHeight += AndroidUtilities.dp(14);
            }

            if (currentMessageObject.eventId != 0 && !currentMessageObject.isMediaEmpty() && currentMessageObject.messageOwner.media.webpage != null) {
                int linkPreviewMaxWidth = backgroundWidth - AndroidUtilities.dp(41);
                hasOldCaptionPreview = true;
                linkPreviewHeight = 0;
                TLRPC.WebPage webPage = currentMessageObject.messageOwner.media.webpage;
                try {
                    int width = siteNameWidth = (int) Math.ceil(Theme.chat_replyNamePaint.measureText(webPage.site_name) + 1);
                    siteNameLayout = new StaticLayout(webPage.site_name, Theme.chat_replyNamePaint, Math.min(width, linkPreviewMaxWidth), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    siteNameRtl = siteNameLayout.getLineLeft(0) != 0;
                    int height = siteNameLayout.getLineBottom(siteNameLayout.getLineCount() - 1);
                    linkPreviewHeight += height;
                    totalHeight += height;
                } catch (Exception e) {
                    FileLog.e(e);
                }

                try {
                    descriptionX = 0;
                    if (linkPreviewHeight != 0) {
                        totalHeight += AndroidUtilities.dp(2);
                    }

                    descriptionLayout = StaticLayoutEx.createStaticLayout(webPage.description, Theme.chat_replyTextPaint, linkPreviewMaxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, AndroidUtilities.dp(1), false, TextUtils.TruncateAt.END, linkPreviewMaxWidth, 6);

                    int height = descriptionLayout.getLineBottom(descriptionLayout.getLineCount() - 1);
                    linkPreviewHeight += height;
                    totalHeight += height;

                    boolean hasNonRtl = false;

                    for (int a = 0; a < descriptionLayout.getLineCount(); a++) {
                        int lineLeft = (int) Math.ceil(descriptionLayout.getLineLeft(a));
                        if (lineLeft != 0) {
                            if (descriptionX == 0) {
                                descriptionX = -lineLeft;
                            } else {
                                descriptionX = Math.max(descriptionX, -lineLeft);
                            }
                        } else {
                            hasNonRtl = true;
                        }
                    }
                    if (hasNonRtl) {
                        descriptionX = 0;
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }

                if (messageObject.type == MessageObject.TYPE_PHOTO || messageObject.type  == MessageObject.TYPE_VIDEO) {
                    totalHeight += AndroidUtilities.dp(6);
                }

                totalHeight += AndroidUtilities.dp(17);
                if (captionNewLine != 0) {
                    totalHeight -= AndroidUtilities.dp(14);
                    if (captionNewLine == 2) {
                        captionHeight -= AndroidUtilities.dp(14);
                    }
                }
            }

            if (messageObject.isSponsored()) {
                drawInstantView = true;
                drawInstantViewType = 1;
                long id = MessageObject.getPeerId(messageObject.messageOwner.from_id);
                if (id > 0) {
                    TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(id);
                    if (user != null && user.bot) {
                        drawInstantViewType = 10;
                    }
                }
                createInstantViewButton();
            }

            botButtons.clear();
            if (messageIdChanged) {
                botButtonsByData.clear();
                botButtonsByPosition.clear();
                botButtonsLayout = null;
            }
            if (!messageObject.isRestrictedMessage && currentPosition == null && (messageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup || messageObject.messageOwner.reactions != null && !messageObject.messageOwner.reactions.results.isEmpty())) {
                int rows;

                if (messageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup) {
                    rows = messageObject.messageOwner.reply_markup.rows.size();
                } else {
                    rows = 1;
                }
                substractBackgroundHeight = keyboardHeight = AndroidUtilities.dp(44 + 4) * rows + AndroidUtilities.dp(1);
                widthForButtons = backgroundWidth - AndroidUtilities.dp(mediaBackground ? 0 : 9);
                boolean fullWidth = false;
                if (messageObject.wantedBotKeyboardWidth > widthForButtons) {
                    int maxButtonWidth = -AndroidUtilities.dp(drawAvatar ? 62 : 10);
                    if (AndroidUtilities.isTablet()) {
                        maxButtonWidth += AndroidUtilities.getMinTabletSide();
                    } else {
                        maxButtonWidth += Math.min(getParentWidth(), AndroidUtilities.displaySize.y) - AndroidUtilities.dp(5);
                    }
                    widthForButtons = Math.max(backgroundWidth, Math.min(messageObject.wantedBotKeyboardWidth, maxButtonWidth));
                }

                int maxButtonsWidth = 0;
                HashMap<String, BotButton> oldByData = new HashMap<>(botButtonsByData);
                HashMap<String, BotButton> oldByPosition;
                if (messageObject.botButtonsLayout != null && botButtonsLayout != null && botButtonsLayout.equals(messageObject.botButtonsLayout.toString())) {
                    oldByPosition = new HashMap<>(botButtonsByPosition);
                } else {
                    if (messageObject.botButtonsLayout != null) {
                        botButtonsLayout = messageObject.botButtonsLayout.toString();
                    }
                    oldByPosition = null;
                }
                botButtonsByData.clear();
                if (messageObject.messageOwner.reply_markup instanceof TLRPC.TL_replyInlineMarkup) {
                    for (int a = 0; a < rows; a++) {
                        TLRPC.TL_keyboardButtonRow row = messageObject.messageOwner.reply_markup.rows.get(a);
                        int buttonsCount = row.buttons.size();
                        if (buttonsCount == 0) {
                            continue;
                        }
                        int buttonWidth = (widthForButtons - AndroidUtilities.dp(5) * (buttonsCount - 1) - AndroidUtilities.dp(2)) / buttonsCount;
                        for (int b = 0; b < row.buttons.size(); b++) {
                            BotButton botButton = new BotButton();
                            botButton.button = row.buttons.get(b);
                            String key = Utilities.bytesToHex(botButton.button.data);
                            String position = a + "" + b;
                            BotButton oldButton;
                            if (oldByPosition != null) {
                                oldButton = oldByPosition.get(position);
                            } else {
                                oldButton = oldByData.get(key);
                            }
                            if (oldButton != null) {
                                botButton.progressAlpha = oldButton.progressAlpha;
                                botButton.angle = oldButton.angle;
                                botButton.lastUpdateTime = oldButton.lastUpdateTime;
                            } else {
                                botButton.lastUpdateTime = System.currentTimeMillis();
                            }
                            botButtonsByData.put(key, botButton);
                            botButtonsByPosition.put(position, botButton);
                            botButton.x = b * (buttonWidth + AndroidUtilities.dp(5));
                            botButton.y = a * AndroidUtilities.dp(44 + 4) + AndroidUtilities.dp(5);
                            botButton.width = buttonWidth;
                            botButton.height = AndroidUtilities.dp(44);
                            CharSequence buttonText;
                            TextPaint botButtonPaint = (TextPaint) getThemedPaint(Theme.key_paint_chatBotButton);
                            if (botButton.button instanceof TLRPC.TL_keyboardButtonBuy && (messageObject.messageOwner.media.flags & 4) != 0) {
                                buttonText = LocaleController.getString("PaymentReceipt", R.string.PaymentReceipt);
                            } else {
                                buttonText = Emoji.replaceEmoji(botButton.button.text, botButtonPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false);
                                buttonText = TextUtils.ellipsize(buttonText, botButtonPaint, buttonWidth - AndroidUtilities.dp(10), TextUtils.TruncateAt.END);
                            }
                            botButton.title = new StaticLayout(buttonText, botButtonPaint, buttonWidth - AndroidUtilities.dp(10), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                            botButtons.add(botButton);
                            if (b == row.buttons.size() - 1) {
                                maxButtonsWidth = Math.max(maxButtonsWidth, botButton.x + botButton.width);
                            }
                        }
                    }
                } else {
                    int buttonsCount = messageObject.messageOwner.reactions.results.size();
                    int buttonWidth = (widthForButtons - AndroidUtilities.dp(5) * (buttonsCount - 1) - AndroidUtilities.dp(2)) / buttonsCount;
                    for (int b = 0; b < buttonsCount; b++) {
                        TLRPC.TL_reactionCount reaction = messageObject.messageOwner.reactions.results.get(b);
                        BotButton botButton = new BotButton();
                        botButton.reaction = reaction;
                        String key = reaction.reaction;
                        String position = 0 + "" + b;
                        BotButton oldButton;
                        if (oldByPosition != null) {
                            oldButton = oldByPosition.get(position);
                        } else {
                            oldButton = oldByData.get(key);
                        }
                        if (oldButton != null) {
                            botButton.progressAlpha = oldButton.progressAlpha;
                            botButton.angle = oldButton.angle;
                            botButton.lastUpdateTime = oldButton.lastUpdateTime;
                        } else {
                            botButton.lastUpdateTime = System.currentTimeMillis();
                        }
                        botButtonsByData.put(key, botButton);
                        botButtonsByPosition.put(position, botButton);
                        botButton.x = b * (buttonWidth + AndroidUtilities.dp(5));
                        botButton.y = AndroidUtilities.dp(5);
                        botButton.width = buttonWidth;
                        botButton.height = AndroidUtilities.dp(44);

                        TextPaint botButtonPaint = (TextPaint) getThemedPaint(Theme.key_paint_chatBotButton);
                        CharSequence buttonText = Emoji.replaceEmoji(String.format("%d %s", reaction.count, reaction.reaction), botButtonPaint.getFontMetricsInt(), AndroidUtilities.dp(15), false);
                        buttonText = TextUtils.ellipsize(buttonText, botButtonPaint, buttonWidth - AndroidUtilities.dp(10), TextUtils.TruncateAt.END);

                        botButton.title = new StaticLayout(buttonText, botButtonPaint, buttonWidth - AndroidUtilities.dp(10), Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                        botButtons.add(botButton);
                        if (b == buttonsCount - 1) {
                            maxButtonsWidth = Math.max(maxButtonsWidth, botButton.x + botButton.width);
                        }
                    }
                }
                widthForButtons = maxButtonsWidth;
            } else {
                substractBackgroundHeight = 0;
                keyboardHeight = 0;
            }
            if (drawCommentButton) {
                totalHeight += AndroidUtilities.dp(shouldDrawTimeOnMedia() ? 41.3f : 43);
                createSelectorDrawable(1);
            }
            if (drawPinnedBottom && drawPinnedTop) {
                totalHeight -= AndroidUtilities.dp(2);
            } else if (drawPinnedBottom) {
                totalHeight -= AndroidUtilities.dp(1);
            } else if (drawPinnedTop && pinnedBottom && currentPosition != null && currentPosition.siblingHeights == null) {
                totalHeight -= AndroidUtilities.dp(1);
            }
            if (messageObject.isAnyKindOfSticker() && totalHeight < AndroidUtilities.dp(70)) {
                additionalTimeOffsetY = AndroidUtilities.dp(70) - totalHeight;
                totalHeight += additionalTimeOffsetY;
            } else if (messageObject.isAnimatedEmoji()) {
                additionalTimeOffsetY = AndroidUtilities.dp(16);
                totalHeight += AndroidUtilities.dp(16);
            }
            if (!drawPhotoImage) {
                photoImage.setImageBitmap((Drawable) null);
            }
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (MessageObject.isDocumentHasThumb(documentAttach)) {
                    TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(documentAttach.thumbs, 90);
                    radialProgress.setImageOverlay(thumb, documentAttach, messageObject);
                } else {
                    String artworkUrl = messageObject.getArtworkUrl(true);
                    if (!TextUtils.isEmpty(artworkUrl)) {
                        radialProgress.setImageOverlay(artworkUrl);
                    } else {
                        radialProgress.setImageOverlay(null, null, null);
                    }
                }
            } else {
                radialProgress.setImageOverlay(null, null, null);
            }


            if (canChangeRadius) {
                int tl, tr, bl, br;
                int minRad = AndroidUtilities.dp(4);
                int rad;
                if (SharedConfig.bubbleRadius > 2) {
                    rad = AndroidUtilities.dp(SharedConfig.bubbleRadius - 2);
                } else {
                    rad = AndroidUtilities.dp(SharedConfig.bubbleRadius);
                }
                int nearRad = Math.min(AndroidUtilities.dp(3), rad);
                tl = tr = bl = br = rad;
                if (minRad > tl) {
                    minRad = tl;
                }
                if (hasLinkPreview || hasGamePreview || hasInvoicePreview) {
                    tl = tr = bl = br = minRad;
                }
                if (forwardedNameLayout[0] != null || replyNameLayout != null || drawNameLayout) {
                    tl = tr = minRad;
                }
                if (captionLayout != null || drawCommentButton) {
                    bl = br = minRad;
                }
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                    tr = br = minRad;
                }
                if (currentPosition != null && currentMessagesGroup != null) {
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) == 0) {
                        tr = br = minRad;
                    }
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                        tl = bl = minRad;
                    }
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                        bl = br = minRad;
                    }
                    if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                        tl = tr = minRad;
                    }
                }
                if (pinnedTop) {
                    if (currentMessageObject.isOutOwner()) {
                        tr = nearRad;
                    } else {
                        tl = nearRad;
                    }
                }
                if (pinnedBottom) {
                    if (currentMessageObject.isOutOwner()) {
                        br = nearRad;
                    } else {
                        bl = nearRad;
                    }
                }
                if (!mediaBackground && !currentMessageObject.isOutOwner()) {
                    bl = nearRad;
                }
                photoImage.setRoundRadius(tl, tr, br, bl);
            }
        }
        if (messageIdChanged) {
            currentUrl = null;
            currentWebFile = null;
            lastWebFile = null;
            loadingProgressLayout = null;
            animatingLoadingProgressProgress = 0;
            lastLoadingSizeTotal = 0;
            selectedBackgroundProgress = 0f;
            if (statusDrawableAnimator != null) {
                statusDrawableAnimator.removeAllListeners();
                statusDrawableAnimator.cancel();
            }
            transitionParams.lastStatusDrawableParams = -1;
            statusDrawableAnimationInProgress = false;

            if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                boolean showSeekbar = MediaController.getInstance().isPlayingMessage(currentMessageObject);
                toSeekBarProgress = showSeekbar ? 1f : 0f;
            }

            seekBarWaveform.setProgress(0);
        }
        updateWaveform();
        updateButtonState(false, dataChanged && !messageObject.cancelEditing, true);

        if (!currentMessageObject.loadingCancelled && buttonState == 2 && documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO && DownloadController.getInstance(currentAccount).canDownloadMedia(messageObject)) {
            FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, 0);
            buttonState = 4;
            radialProgress.setIcon(getIconForCurrentState(), false, false);
        }

        if (delegate != null && delegate.getTextSelectionHelper() != null && !messageIdChanged && messageChanged && messageObject != null) {
            delegate.getTextSelectionHelper().checkDataChanged(messageObject);
        }
        accessibilityVirtualViewBounds.clear();
        transitionParams.updatePhotoImageX = true;
    }

    public void checkVideoPlayback(boolean allowStart, Bitmap thumb) {
        if (currentMessageObject.isVideo()) {
            if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                photoImage.setAllowStartAnimation(false);
                photoImage.stopAnimation();
            } else {
                photoImage.setAllowStartAnimation(true);
                photoImage.startAnimation();
            }
        } else {
            if (allowStart) {
                MessageObject playingMessage = MediaController.getInstance().getPlayingMessageObject();
                allowStart = playingMessage == null || !playingMessage.isRoundVideo();
            }
            photoImage.setAllowStartAnimation(allowStart);
            if (thumb != null) {
                photoImage.startCrossfadeFromStaticThumb(thumb);

            }
            if (allowStart) {
                photoImage.startAnimation();
            } else {
                photoImage.stopAnimation();
            }

        }
    }

    @Override
    protected boolean onLongPress() {
        if (isRoundVideo && isPlayingRound && MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
            float touchRadius = (lastTouchX - photoImage.getCenterX()) * (lastTouchX - photoImage.getCenterX()) + (lastTouchY - photoImage.getCenterY()) * (lastTouchY - photoImage.getCenterY());
            float r1 = (photoImage.getImageWidth() / 2f) * (photoImage.getImageWidth() / 2f);
            if (touchRadius < r1 && (lastTouchX > photoImage.getCenterX() + photoImage.getImageWidth() / 4f || lastTouchX < photoImage.getCenterX() - photoImage.getImageWidth() / 4f)) {
                boolean forward = lastTouchX > photoImage.getCenterX();
                if (videoPlayerRewinder == null) {
                    videoForwardDrawable = new VideoForwardDrawable(true);
                    videoPlayerRewinder = new VideoPlayerRewinder() {
                        @Override
                        protected void onRewindCanceled() {
                            onTouchEvent(MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0));
                            videoForwardDrawable.setShowing(false);
                        }

                        @Override
                        protected void updateRewindProgressUi(long timeDiff, float progress, boolean rewindByBackSeek) {
                            videoForwardDrawable.setTime(Math.abs(timeDiff));
                            if (rewindByBackSeek) {
                                currentMessageObject.audioProgress = progress;
                                updatePlayingMessageProgress();
                            }
                        }

                        @Override
                        protected void onRewindStart(boolean rewindForward) {
                            videoForwardDrawable.setDelegate(new VideoForwardDrawable.VideoForwardDrawableDelegate() {
                                @Override
                                public void onAnimationEnd() {

                                }

                                @Override
                                public void invalidate() {
                                    ChatMessageCell.this.invalidate();
                                }
                            });
                            videoForwardDrawable.setOneShootAnimation(false);
                            videoForwardDrawable.setLeftSide(!rewindForward);
                            videoForwardDrawable.setShowing(true);
                            invalidate();
                        }
                    };
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                videoPlayerRewinder.startRewind(MediaController.getInstance().getVideoPlayer(), forward, MediaController.getInstance().getPlaybackSpeed(false));
                return false;
            }
        }
        if (pressedLink instanceof URLSpanMono) {
            delegate.didPressUrl(this, pressedLink, true);
            return true;
        } else if (pressedLink instanceof URLSpanNoUnderline) {
            URLSpanNoUnderline url = (URLSpanNoUnderline) pressedLink;
            if (ChatActivity.isClickableLink(url.getURL()) || url.getURL().startsWith("/")) {
                delegate.didPressUrl(this, pressedLink, true);
                return true;
            }
        } else if (pressedLink instanceof URLSpan) {
            delegate.didPressUrl(this, pressedLink, true);
            return true;
        }
        resetPressedLink(-1);
        if (buttonPressed != 0 || miniButtonPressed != 0 || videoButtonPressed != 0 || pressedBotButton != -1) {
            buttonPressed = 0;
            miniButtonPressed = 0;
            videoButtonPressed = 0;
            pressedBotButton = -1;
            invalidate();
        }

        linkPreviewPressed = false;
        sideButtonPressed = false;
        imagePressed = false;
        timePressed = false;
        gamePreviewPressed = false;

        if (pressedVoteButton != -1 || pollHintPressed || psaHintPressed || instantPressed || otherPressed || commentButtonPressed) {
            instantPressed = instantButtonPressed = commentButtonPressed = false;
            pressedVoteButton = -1;
            pollHintPressed = false;
            psaHintPressed = false;
            otherPressed = false;
            if (Build.VERSION.SDK_INT >= 21) {
                for (int a = 0; a < selectorDrawable.length; a++) {
                    if (selectorDrawable[a] != null) {
                        selectorDrawable[a].setState(StateSet.NOTHING);
                    }
                }
            }
            invalidate();
        }
        if (delegate != null) {
            boolean handled = false;

            if (avatarPressed) {
                if (currentUser != null) {
                    if (currentUser.id != 0) {
                        handled = delegate.didLongPressUserAvatar(this, currentUser, lastTouchX, lastTouchY);
                    }
                } else if (currentChat != null) {
                    final int id;
                    if (currentMessageObject.messageOwner.fwd_from != null) {
                        if ((currentMessageObject.messageOwner.fwd_from.flags & 16) != 0) {
                            id = currentMessageObject.messageOwner.fwd_from.saved_from_msg_id;
                        } else {
                            id = currentMessageObject.messageOwner.fwd_from.channel_post;
                        }
                    } else {
                        id = 0;
                    }
                    handled = delegate.didLongPressChannelAvatar(this, currentChat, id, lastTouchX, lastTouchY);
                }
            }

            if (!handled) {
                delegate.didLongPress(this, lastTouchX, lastTouchY);
            }
        }
        return true;
    }

    public void showHintButton(boolean show, boolean animated, int type) {
        if (type == -1 || type == 0) {
            if (hintButtonVisible == show) {
                return;
            }
            hintButtonVisible = show;
            if (!animated) {
                hintButtonProgress = show ? 1.0f : 0.0f;
            } else {
                invalidate();
            }
        }
        if (type == -1 || type == 1) {
            if (psaButtonVisible == show) {
                return;
            }
            psaButtonVisible = show;
            if (!animated) {
                psaButtonProgress = show ? 1.0f : 0.0f;
            } else {
                setInvalidatesParent(true);
                invalidate();
            }
        }
    }

    public void setCheckPressed(boolean value, boolean pressed) {
        isCheckPressed = value;
        isPressed = pressed;
        updateRadialProgressBackground();
        if (useSeekBarWaweform) {
            seekBarWaveform.setSelected(isDrawSelectionBackground());
        } else {
            seekBar.setSelected(isDrawSelectionBackground());
        }
        invalidate();
    }

    public void setInvalidatesParent(boolean value) {
        invalidatesParent = value;
    }

    @Override
    public void invalidate() {
        if (currentMessageObject == null) {
            return;
        }
        super.invalidate();
        if (invalidatesParent && getParent() != null) {
            View parent = (View) getParent();
            if (parent.getParent() != null) {
                parent.invalidate();
                parent = (View) parent.getParent();
                parent.invalidate();
            }
        }
    }

    @Override
    public void invalidate(int l, int t, int r, int b) {
        if (currentMessageObject == null) {
            return;
        }
        super.invalidate(l, t, r, b);
        if (invalidatesParent) {
            if (getParent() != null) {
                View parent = (View) getParent();
                parent.invalidate((int) getX() + l, (int) getY() + t, (int) getX() + r, (int) getY() + b);
            }
        }
    }

    public boolean isHighlightedAnimated() {
        return isHighlightedAnimated;
    }

    public void setHighlightedAnimated() {
        isHighlightedAnimated = true;
        highlightProgress = 1000;
        lastHighlightProgressTime = System.currentTimeMillis();
        invalidate();
        if (getParent() != null) {
            ((View) getParent()).invalidate();
        }
    }

    public boolean isHighlighted() {
        return isHighlighted;
    }

    public void setHighlighted(boolean value) {
        if (isHighlighted == value) {
            return;
        }
        isHighlighted = value;
        if (!isHighlighted) {
            lastHighlightProgressTime = System.currentTimeMillis();
            isHighlightedAnimated = true;
            highlightProgress = 300;
        } else {
            isHighlightedAnimated = false;
            highlightProgress = 0;
        }

        updateRadialProgressBackground();
        if (useSeekBarWaweform) {
            seekBarWaveform.setSelected(isDrawSelectionBackground());
        } else {
            seekBar.setSelected(isDrawSelectionBackground());
        }
        invalidate();
        if (getParent() != null) {
            ((View) getParent()).invalidate();
        }
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        updateRadialProgressBackground();
        if (useSeekBarWaweform) {
            seekBarWaveform.setSelected(isDrawSelectionBackground());
        } else {
            seekBar.setSelected(isDrawSelectionBackground());
        }
        invalidate();
    }

    private void updateRadialProgressBackground() {
        if (drawRadialCheckBackground) {
            return;
        }
        boolean forcePressed = (isHighlighted || isPressed || isPressed()) && (!drawPhotoImage || !photoImage.hasBitmapImage());
        radialProgress.setPressed(forcePressed || buttonPressed != 0, false);
        if (hasMiniProgress != 0) {
            radialProgress.setPressed(forcePressed || miniButtonPressed != 0, true);
        }
        videoRadialProgress.setPressed(forcePressed || videoButtonPressed != 0, false);
    }

    @Override
    public void onSeekBarDrag(float progress) {
        if (currentMessageObject == null) {
            return;
        }
        currentMessageObject.audioProgress = progress;
        MediaController.getInstance().seekToProgress(currentMessageObject, progress);
        updatePlayingMessageProgress();
    }

    @Override
    public void onSeekBarContinuousDrag(float progress) {
        if (currentMessageObject == null) {
            return;
        }
        currentMessageObject.audioProgress = progress;
        currentMessageObject.audioProgressSec = (int) (currentMessageObject.getDuration() * progress);
        updatePlayingMessageProgress();
    }

    public boolean isAnimatingPollAnswer() {
        return animatePollAnswerAlpha;
    }

    private void updateWaveform() {
        if (currentMessageObject == null || documentAttachType != DOCUMENT_ATTACH_TYPE_AUDIO) {
            return;
        }
        for (int a = 0; a < documentAttach.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                if (attribute.waveform == null || attribute.waveform.length == 0) {
                    MediaController.getInstance().generateWaveform(currentMessageObject);
                }
                useSeekBarWaweform = attribute.waveform != null;
                seekBarWaveform.setWaveform(attribute.waveform);
                break;
            }
        }
    }

    private int createDocumentLayout(int maxWidth, MessageObject messageObject) {
        if (messageObject.type == 0) {
            documentAttach = messageObject.messageOwner.media.webpage.document;
        } else {
            documentAttach = messageObject.getDocument();
        }
        if (documentAttach == null) {
            return 0;
        }
        if (MessageObject.isVoiceDocument(documentAttach)) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_AUDIO;
            int duration = 0;
            for (int a = 0; a < documentAttach.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    duration = attribute.duration;
                    break;
                }
            }
            widthBeforeNewTimeLine = maxWidth - AndroidUtilities.dp(76 + 18) - (int) Math.ceil(Theme.chat_audioTimePaint.measureText("00:00"));
            availableTimeWidth = maxWidth - AndroidUtilities.dp(18);
            measureTime(messageObject);
            int minSize = AndroidUtilities.dp(40 + 14 + 20 + 90 + 10) + timeWidth;
            if (!hasLinkPreview) {
                String timeString = AndroidUtilities.formatLongDuration(duration);
                int w = (int) Math.ceil(Theme.chat_audioTimePaint.measureText(timeString));
                backgroundWidth = Math.min(maxWidth, minSize + w);
            }
            seekBarWaveform.setMessageObject(messageObject);
            return 0;
        } else if (MessageObject.isMusicDocument(documentAttach)) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_MUSIC;

            maxWidth = maxWidth - AndroidUtilities.dp(92);
            if (maxWidth < 0) {
                maxWidth = AndroidUtilities.dp(100);
            }

            CharSequence stringFinal = TextUtils.ellipsize(messageObject.getMusicTitle().replace('\n', ' '), Theme.chat_audioTitlePaint, maxWidth - AndroidUtilities.dp(12), TextUtils.TruncateAt.END);
            songLayout = new StaticLayout(stringFinal, Theme.chat_audioTitlePaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (songLayout.getLineCount() > 0) {
                songX = -(int) Math.ceil(songLayout.getLineLeft(0));
            }

            stringFinal = TextUtils.ellipsize(messageObject.getMusicAuthor().replace('\n', ' '), Theme.chat_audioPerformerPaint, maxWidth, TextUtils.TruncateAt.END);
            performerLayout = new StaticLayout(stringFinal, Theme.chat_audioPerformerPaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (performerLayout.getLineCount() > 0) {
                performerX = -(int) Math.ceil(performerLayout.getLineLeft(0));
            }

            int duration = 0;
            for (int a = 0; a < documentAttach.attributes.size(); a++) {
                TLRPC.DocumentAttribute attribute = documentAttach.attributes.get(a);
                if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                    duration = attribute.duration;
                    break;
                }
            }
            int durationWidth = (int) Math.ceil(Theme.chat_audioTimePaint.measureText(AndroidUtilities.formatShortDuration(duration, duration)));
            widthBeforeNewTimeLine = backgroundWidth - AndroidUtilities.dp(10 + 76) - durationWidth;
            availableTimeWidth = backgroundWidth - AndroidUtilities.dp(28);
            return durationWidth;
        } else if (MessageObject.isVideoDocument(documentAttach)) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_VIDEO;
            if (!messageObject.needDrawBluredPreview()) {
                updatePlayingMessageProgress();
                String str;
                str = String.format("%s", AndroidUtilities.formatFileSize(documentAttach.size));
                docTitleWidth = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
                docTitleLayout = new StaticLayout(str, Theme.chat_infoPaint, docTitleWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            return 0;
        } else if (MessageObject.isGifDocument(documentAttach, messageObject.hasValidGroupId())) {
            documentAttachType = DOCUMENT_ATTACH_TYPE_GIF;
            if (!messageObject.needDrawBluredPreview()) {

                String str = LocaleController.getString("AttachGif", R.string.AttachGif);
                infoWidth = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
                infoLayout = new StaticLayout(str, Theme.chat_infoPaint, infoWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

                str = String.format("%s", AndroidUtilities.formatFileSize(documentAttach.size));
                docTitleWidth = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
                docTitleLayout = new StaticLayout(str, Theme.chat_infoPaint, docTitleWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
            return 0;
        } else {
            drawPhotoImage = documentAttach.mime_type != null && (documentAttach.mime_type.toLowerCase().startsWith("image/") || documentAttach.mime_type.toLowerCase().startsWith("video/mp4")) || MessageObject.isDocumentHasThumb(documentAttach);
            if (!drawPhotoImage) {
                maxWidth += AndroidUtilities.dp(30);
            }
            documentAttachType = DOCUMENT_ATTACH_TYPE_DOCUMENT;
            String name = FileLoader.getDocumentFileName(documentAttach);
            if (name.length() == 0) {
                name = LocaleController.getString("AttachDocument", R.string.AttachDocument);
            }
            docTitleLayout = StaticLayoutEx.createStaticLayout(name, Theme.chat_docNamePaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false, TextUtils.TruncateAt.MIDDLE, maxWidth, 2, false);
            docTitleOffsetX = Integer.MIN_VALUE;
            int width;
            if (docTitleLayout != null && docTitleLayout.getLineCount() > 0) {
                int maxLineWidth = 0;
                for (int a = 0; a < docTitleLayout.getLineCount(); a++) {
                    maxLineWidth = Math.max(maxLineWidth, (int) Math.ceil(docTitleLayout.getLineWidth(a)));
                    docTitleOffsetX = Math.max(docTitleOffsetX, (int) Math.ceil(-docTitleLayout.getLineLeft(a)));
                }
                width = Math.min(maxWidth, maxLineWidth);
            } else {
                width = maxWidth;
                docTitleOffsetX = 0;
            }

            String str = AndroidUtilities.formatFileSize(documentAttach.size) + " " + FileLoader.getDocumentExtension(documentAttach);
            infoWidth = Math.min(maxWidth - AndroidUtilities.dp(30), (int) Math.ceil(Theme.chat_infoPaint.measureText("000.0 mm / " + AndroidUtilities.formatFileSize(documentAttach.size))));
            CharSequence str2 = TextUtils.ellipsize(str, Theme.chat_infoPaint, infoWidth, TextUtils.TruncateAt.END);
            try {
                if (infoWidth < 0) {
                    infoWidth = AndroidUtilities.dp(10);
                }
                infoLayout = new StaticLayout(str2, Theme.chat_infoPaint, infoWidth + AndroidUtilities.dp(6), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } catch (Exception e) {
                FileLog.e(e);
            }

            if (drawPhotoImage) {
                currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 320);
                currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 40);

                if ((DownloadController.getInstance(currentAccount).getAutodownloadMask() & DownloadController.AUTODOWNLOAD_TYPE_PHOTO) == 0) {
                    currentPhotoObject = null;
                }
                if (currentPhotoObject == null || currentPhotoObject == currentPhotoObjectThumb) {
                    currentPhotoObject = null;
                    photoImage.setNeedsQualityThumb(true);
                    photoImage.setShouldGenerateQualityThumb(true);
                }
                currentPhotoFilter = "86_86_b";
                photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "86_86", ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), currentPhotoFilter, 0, null, messageObject, 1);
            }
            return width;
        }
    }

    private void calcBackgroundWidth(int maxWidth, int timeMore, int maxChildWidth) {
        if (hasLinkPreview || hasOldCaptionPreview || hasGamePreview || hasInvoicePreview || maxWidth - currentMessageObject.lastLineWidth < timeMore || currentMessageObject.hasRtl) {
            totalHeight += AndroidUtilities.dp(14);
            hasNewLineForTime = true;
            backgroundWidth = Math.max(maxChildWidth, currentMessageObject.lastLineWidth) + AndroidUtilities.dp(31);
            backgroundWidth = Math.max(backgroundWidth, (currentMessageObject.isOutOwner() ? timeWidth + AndroidUtilities.dp(17) : timeWidth) + AndroidUtilities.dp(31));
        } else {
            int diff = maxChildWidth - getExtraTextX() - currentMessageObject.lastLineWidth;
            if (diff >= 0 && diff <= timeMore) {
                backgroundWidth = maxChildWidth + timeMore - diff + AndroidUtilities.dp(31);
            } else {
                backgroundWidth = Math.max(maxChildWidth, currentMessageObject.lastLineWidth + timeMore) + AndroidUtilities.dp(31);
            }
        }
    }

    public void setHighlightedText(String text) {
        MessageObject messageObject = messageObjectToSet != null ? messageObjectToSet : currentMessageObject;
        if (messageObject == null || messageObject.messageOwner.message == null || TextUtils.isEmpty(text)) {
            if (!urlPathSelection.isEmpty()) {
                linkSelectionBlockNum = -1;
                resetUrlPaths(true);
                invalidate();
            }
            return;
        }
        text = text.toLowerCase();
        String message = messageObject.messageOwner.message.toLowerCase();
        int start = -1;
        int length = -1;
        String punctuationsChars = " !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~\n";
        for (int a = 0, N1 = message.length(); a < N1; a++) {
            int currentLen = 0;
            for (int b = 0, N2 = Math.min(text.length(), N1 - a); b < N2; b++) {
                boolean match = message.charAt(a + b) == text.charAt(b);
                if (match) {
                    if (currentLen != 0 || a == 0 || punctuationsChars.indexOf(message.charAt(a - 1)) >= 0) {
                        currentLen++;
                    } else {
                        match = false;
                    }
                }
                if (!match || b == N2 - 1) {
                    if (currentLen > 0 && currentLen > length) {
                        length = currentLen;
                        start = a;
                    }
                    break;
                }
            }
        }
        if (start == -1) {
            if (!urlPathSelection.isEmpty()) {
                linkSelectionBlockNum = -1;
                resetUrlPaths(true);
                invalidate();
            }
            return;
        }
        for (int a = start + length, N = message.length(); a < N; a++) {
            if (punctuationsChars.indexOf(message.charAt(a)) < 0) {
                length++;
            } else {
                break;
            }
        }
        int end = start + length;
        if (captionLayout != null && !TextUtils.isEmpty(messageObject.caption)) {
            resetUrlPaths(true);
            try {
                LinkPath path = obtainNewUrlPath(true);
                path.setCurrentLayout(captionLayout, start, 0);
                captionLayout.getSelectionPath(start, end, path);
            } catch (Exception e) {
                FileLog.e(e);
            }
            invalidate();
        } else if (messageObject.textLayoutBlocks != null) {
            for (int c = 0; c < messageObject.textLayoutBlocks.size(); c++) {
                MessageObject.TextLayoutBlock block = messageObject.textLayoutBlocks.get(c);
                if (start >= block.charactersOffset && start < block.charactersEnd) {
                    linkSelectionBlockNum = c;
                    resetUrlPaths(true);
                    try {
                        LinkPath path = obtainNewUrlPath(true);
                        path.setCurrentLayout(block.textLayout, start, 0);
                        block.textLayout.getSelectionPath(start, end, path);
                        if (end >= block.charactersOffset + length) {
                            for (int a = c + 1; a < messageObject.textLayoutBlocks.size(); a++) {
                                MessageObject.TextLayoutBlock nextBlock = messageObject.textLayoutBlocks.get(a);
                                length = nextBlock.charactersEnd - nextBlock.charactersOffset;
                                path = obtainNewUrlPath(true);
                                path.setCurrentLayout(nextBlock.textLayout, 0, nextBlock.height);
                                nextBlock.textLayout.getSelectionPath(0, end - nextBlock.charactersOffset, path);
                                if (end < block.charactersOffset + length - 1) {
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    invalidate();
                    break;
                }
            }
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || who == selectorDrawable[0] || who == selectorDrawable[1];
    }

    @Override
    public void invalidateDrawable(Drawable drawable) {
        super.invalidateDrawable(drawable);
        if (currentMessagesGroup != null && drawable == selectorDrawable[1]) {
            invalidateWithParent();
        }
    }

    private boolean isCurrentLocationTimeExpired(MessageObject messageObject) {
        if (currentMessageObject.messageOwner.media.period % 60 == 0) {
            return Math.abs(ConnectionsManager.getInstance(currentAccount).getCurrentTime() - messageObject.messageOwner.date) > messageObject.messageOwner.media.period;
        } else {
            return Math.abs(ConnectionsManager.getInstance(currentAccount).getCurrentTime() - messageObject.messageOwner.date) > messageObject.messageOwner.media.period - 5;
        }
    }

    private void checkLocationExpired() {
        if (currentMessageObject == null) {
            return;
        }
        boolean newExpired = isCurrentLocationTimeExpired(currentMessageObject);
        if (newExpired != locationExpired) {
            locationExpired = newExpired;
            if (!locationExpired) {
                AndroidUtilities.runOnUIThread(invalidateRunnable, 1000);
                scheduledInvalidate = true;
                int maxWidth = backgroundWidth - AndroidUtilities.dp(37 + 54);
                docTitleLayout = new StaticLayout(TextUtils.ellipsize(LocaleController.getString("AttachLiveLocation", R.string.AttachLiveLocation), Theme.chat_locationTitlePaint, maxWidth, TextUtils.TruncateAt.END), Theme.chat_locationTitlePaint, maxWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                MessageObject messageObject = currentMessageObject;
                currentMessageObject = null;
                setMessageObject(messageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
            }
        }
    }

    public void setIsUpdating(boolean value) {
        isUpdating = true;
    }

    public void setMessageObject(MessageObject messageObject, MessageObject.GroupedMessages groupedMessages, boolean bottomNear, boolean topNear) {
        if (attachedToWindow) {
            setMessageContent(messageObject, groupedMessages, bottomNear, topNear);
        } else {
            messageObjectToSet = messageObject;
            groupedMessagesToSet = groupedMessages;
            bottomNearToSet = bottomNear;
            topNearToSet = topNear;
        }
    }

    private int getAdditionalWidthForPosition(MessageObject.GroupedMessagePosition position) {
        int w = 0;
        if (position != null) {
            if ((position.flags & MessageObject.POSITION_FLAG_RIGHT) == 0) {
                w += AndroidUtilities.dp(4);
            }
            if ((position.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                w += AndroidUtilities.dp(4);
            }
        }
        return w;
    }

    public void createSelectorDrawable(int num) {
        if (Build.VERSION.SDK_INT < 21) {
            return;
        }
        int color;
        if (psaHintPressed) {
            color = getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outViews : Theme.key_chat_inViews);
        } else {
            color = getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outPreviewInstantText : Theme.key_chat_inPreviewInstantText);
        }
        if (selectorDrawable[num] == null) {
            final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            maskPaint.setColor(0xffffffff);
            Drawable maskDrawable = new Drawable() {

                RectF rect = new RectF();
                Path path = new Path();

                @Override
                public void draw(Canvas canvas) {
                    android.graphics.Rect bounds = getBounds();
                    rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom);
                    if (selectorDrawableMaskType[num] == 3 || selectorDrawableMaskType[num] == 4) {
                        canvas.drawCircle(rect.centerX(), rect.centerY(), AndroidUtilities.dp(selectorDrawableMaskType[num] == 3 ? 16 : 20), maskPaint);
                    } else if (selectorDrawableMaskType[num] == 2) {
                        path.reset();
                        boolean out = currentMessageObject != null && currentMessageObject.isOutOwner();
                        for (int a = 0; a < 4; a++) {
                            if (!insantTextNewLine) {
                                if (a == 2 && !out) {
                                    radii[a * 2] = radii[a * 2 + 1] = AndroidUtilities.dp(SharedConfig.bubbleRadius);
                                    continue;
                                } else if (a == 3 && out) {
                                    radii[a * 2] = radii[a * 2 + 1] = AndroidUtilities.dp(SharedConfig.bubbleRadius);
                                    continue;
                                }
                                if ((mediaBackground || pinnedBottom) && (a == 2 || a == 3)) {
                                    radii[a * 2] = radii[a * 2 + 1] = AndroidUtilities.dp(pinnedBottom ? Math.min(5, SharedConfig.bubbleRadius) : SharedConfig.bubbleRadius);
                                    continue;
                                }
                            }
                            radii[a * 2] = radii[a * 2 + 1] = 0;
                        }
                        path.addRoundRect(rect, radii, Path.Direction.CW);
                        path.close();
                        canvas.drawPath(path, maskPaint);
                    } else {
                        canvas.drawRoundRect(rect, selectorDrawableMaskType[num] == 0 ? AndroidUtilities.dp(6) : 0, selectorDrawableMaskType[num] == 0 ? AndroidUtilities.dp(6) : 0, maskPaint);
                    }
                }

                @Override
                public void setAlpha(int alpha) {

                }

                @Override
                public void setColorFilter(ColorFilter colorFilter) {

                }

                @Override
                public int getOpacity() {
                    return PixelFormat.TRANSPARENT;
                }
            };
            ColorStateList colorStateList = new ColorStateList(
                    new int[][]{StateSet.WILD_CARD},
                    new int[]{getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outPreviewInstantText : Theme.key_chat_inPreviewInstantText) & 0x19ffffff}
            );
            selectorDrawable[num] = new RippleDrawable(colorStateList, null, maskDrawable);
            selectorDrawable[num].setCallback(this);
        } else {
            Theme.setSelectorDrawableColor(selectorDrawable[num], color & 0x19ffffff, true);
        }
        selectorDrawable[num].setVisible(true, false);
    }

    private void createInstantViewButton() {
        if (Build.VERSION.SDK_INT >= 21 && drawInstantView) {
            createSelectorDrawable(0);
        }
        if (drawInstantView && instantViewLayout == null) {
            String str;
            instantWidth = AndroidUtilities.dp(12 + 9 + 12);
            if (drawInstantViewType == 1) {
                str = LocaleController.getString("OpenChannel", R.string.OpenChannel);
            } else if (drawInstantViewType == 10) {
                str = LocaleController.getString("OpenBot", R.string.OpenBot);
            } else if (drawInstantViewType == 2) {
                str = LocaleController.getString("OpenGroup", R.string.OpenGroup);
            } else if (drawInstantViewType == 3) {
                str = LocaleController.getString("OpenMessage", R.string.OpenMessage);
            } else if (drawInstantViewType == 5) {
                str = LocaleController.getString("ViewContact", R.string.ViewContact);
            } else if (drawInstantViewType == 6) {
                str = LocaleController.getString("OpenBackground", R.string.OpenBackground);
            } else if (drawInstantViewType == 7) {
                str = LocaleController.getString("OpenTheme", R.string.OpenTheme);
            } else if (drawInstantViewType == 8) {
                if (pollVoted || pollClosed) {
                    str = LocaleController.getString("PollViewResults", R.string.PollViewResults);
                } else {
                    str = LocaleController.getString("PollSubmitVotes", R.string.PollSubmitVotes);
                }
            } else if (drawInstantViewType == 9 || drawInstantViewType == 11) {
                TLRPC.TL_webPage webPage = (TLRPC.TL_webPage) currentMessageObject.messageOwner.media.webpage;
                if (webPage != null && webPage.url.contains("voicechat=")) {
                    str = LocaleController.getString("VoipGroupJoinAsSpeaker", R.string.VoipGroupJoinAsSpeaker);
                } else {
                    str = LocaleController.getString("VoipGroupJoinAsLinstener", R.string.VoipGroupJoinAsLinstener);
                }
            } else {
                str = LocaleController.getString("InstantView", R.string.InstantView);
            }
            int mWidth = backgroundWidth - AndroidUtilities.dp(10 + 24 + 10 + 31);
            instantViewLayout = new StaticLayout(TextUtils.ellipsize(str, Theme.chat_instantViewPaint, mWidth, TextUtils.TruncateAt.END), Theme.chat_instantViewPaint, mWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (drawInstantViewType == 8) {
                instantWidth = backgroundWidth - AndroidUtilities.dp(13);
            } else {
                instantWidth = backgroundWidth - AndroidUtilities.dp(34);
            }
            totalHeight += AndroidUtilities.dp(46);
            if (currentMessageObject.type == 12) {
                totalHeight += AndroidUtilities.dp(14);
            }
            if (hasNewLineForTime) {
                totalHeight += AndroidUtilities.dp(16);
            }
            if (instantViewLayout != null && instantViewLayout.getLineCount() > 0) {
                instantTextX = (int) (instantWidth - Math.ceil(instantViewLayout.getLineWidth(0))) / 2 + (drawInstantViewType == 0 ? AndroidUtilities.dp(8) : 0);
                instantTextLeftX = (int) instantViewLayout.getLineLeft(0);
                instantTextX += -instantTextLeftX;
            }
        }
    }

    @Override
    public void requestLayout() {
        if (inLayout) {
            return;
        }
        super.requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (currentMessageObject != null && (currentMessageObject.checkLayout() || lastHeight != AndroidUtilities.displaySize.y)) {
            inLayout = true;
            MessageObject messageObject = currentMessageObject;
            currentMessageObject = null;
            setMessageObject(messageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
            inLayout = false;
        }
        updateSelectionTextPosition();
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), totalHeight + keyboardHeight);
    }

    public void forceResetMessageObject() {
        MessageObject messageObject = messageObjectToSet != null ? messageObjectToSet : currentMessageObject;
        currentMessageObject = null;
        setMessageObject(messageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
    }

    private int getGroupPhotosWidth() {
        int width = getParentWidth();
        if (currentMessageObject != null && currentMessageObject.preview) {
            width = parentWidth;
        }
        if (!AndroidUtilities.isInMultiwindow && AndroidUtilities.isTablet() && (!AndroidUtilities.isSmallTablet() || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)) {
            int leftWidth = width / 100 * 35;
            if (leftWidth < AndroidUtilities.dp(320)) {
                leftWidth = AndroidUtilities.dp(320);
            }
            return width - leftWidth;
        } else {
            return width;
        }
    }

    private int getExtraTextX() {
        if (SharedConfig.bubbleRadius >= 15) {
            return AndroidUtilities.dp(2);
        } else if (SharedConfig.bubbleRadius >= 11) {
            return AndroidUtilities.dp(1);
        }
        return 0;
    }

    private int getExtraTimeX() {
        if (!currentMessageObject.isOutOwner() && (!mediaBackground || captionLayout != null) && SharedConfig.bubbleRadius > 11) {
            return AndroidUtilities.dp((SharedConfig.bubbleRadius - 11) / 1.5f);
        }
        if (!currentMessageObject.isOutOwner() && isPlayingRound && isAvatarVisible && currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
            return (int) ((AndroidUtilities.roundPlayingMessageSize - AndroidUtilities.roundMessageSize) * 0.7f);
        }
        return 0;
    }

    int lastSize;
    @SuppressLint("DrawAllocation")
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (currentMessageObject == null) {
            return;
        }
        int currentSize = getMeasuredHeight() + (getMeasuredWidth() << 16);
        if (lastSize != currentSize || !wasLayout) {
            layoutWidth = getMeasuredWidth();
            layoutHeight = getMeasuredHeight() - substractBackgroundHeight;
            if (timeTextWidth < 0) {
                timeTextWidth = AndroidUtilities.dp(10);
            }
            timeLayout = new StaticLayout(currentTimeString, Theme.chat_timePaint, timeTextWidth + AndroidUtilities.dp(100), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (mediaBackground) {
                if (currentMessageObject.isOutOwner()) {
                    timeX = layoutWidth - timeWidth - AndroidUtilities.dp(42.0f);
                } else {
                    timeX = backgroundWidth - AndroidUtilities.dp(4) - timeWidth;
                    if (currentMessageObject.isAnyKindOfSticker()) {
                        timeX = Math.max(AndroidUtilities.dp(26), timeX);
                    }
                    if (isAvatarVisible) {
                        timeX += AndroidUtilities.dp(48);
                    }
                    if (currentPosition != null && currentPosition.leftSpanOffset != 0) {
                        timeX += (int) Math.ceil(currentPosition.leftSpanOffset / 1000.0f * getGroupPhotosWidth());
                    }
                    if (captionLayout != null && currentPosition != null) {
                        timeX += AndroidUtilities.dp(4);
                    }
                }
                if (SharedConfig.bubbleRadius >= 10 && captionLayout == null && documentAttachType != DOCUMENT_ATTACH_TYPE_ROUND && documentAttachType != DOCUMENT_ATTACH_TYPE_STICKER) {
                    timeX -= AndroidUtilities.dp(2);
                }
            } else {
                if (currentMessageObject.isOutOwner()) {
                    timeX = layoutWidth - timeWidth - AndroidUtilities.dp(38.5f);
                } else {
                    timeX = backgroundWidth - AndroidUtilities.dp(9) - timeWidth;
                    if (currentMessageObject.isAnyKindOfSticker()) {
                        timeX = Math.max(0, timeX);
                    }
                    if (isAvatarVisible) {
                        timeX += AndroidUtilities.dp(48);
                    }
                    if (shouldDrawTimeOnMedia()) {
                        timeX -= AndroidUtilities.dp(7);
                    }
                }
            }
            timeX -= getExtraTimeX();

            if ((currentMessageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                viewsLayout = new StaticLayout(currentViewsString, Theme.chat_timePaint, viewsTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                viewsLayout = null;
            }

            if (currentRepliesString != null && !currentMessageObject.scheduled) {
                repliesLayout = new StaticLayout(currentRepliesString, Theme.chat_timePaint, repliesTextWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            } else {
                repliesLayout = null;
            }

            if (isAvatarVisible) {
                avatarImage.setImageCoords(AndroidUtilities.dp(6), avatarImage.getImageY(), AndroidUtilities.dp(42), AndroidUtilities.dp(42));
            }

            wasLayout = true;
        }
        lastSize = currentSize;

        if (currentMessageObject.type == 0) {
            textY = AndroidUtilities.dp(10) + namesOffset;
        }
        if (isRoundVideo) {
            updatePlayingMessageProgress();
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            if (currentMessageObject.isOutOwner()) {
                seekBarX = layoutWidth - backgroundWidth + AndroidUtilities.dp(57);
                buttonX = layoutWidth - backgroundWidth + AndroidUtilities.dp(14);
                timeAudioX = layoutWidth - backgroundWidth + AndroidUtilities.dp(67);
            } else {
                if (isChat && !isThreadPost && currentMessageObject.needDrawAvatar()) {
                    seekBarX = AndroidUtilities.dp(114);
                    buttonX = AndroidUtilities.dp(71);
                    timeAudioX = AndroidUtilities.dp(124);
                } else {
                    seekBarX = AndroidUtilities.dp(66);
                    buttonX = AndroidUtilities.dp(23);
                    timeAudioX = AndroidUtilities.dp(76);
                }
            }
            if (hasLinkPreview) {
                seekBarX += AndroidUtilities.dp(10);
                buttonX += AndroidUtilities.dp(10);
                timeAudioX += AndroidUtilities.dp(10);
            }
            seekBarWaveform.setSize(backgroundWidth - AndroidUtilities.dp(92 + (hasLinkPreview ? 10 : 0)), AndroidUtilities.dp(30));
            seekBar.setSize(backgroundWidth - AndroidUtilities.dp(72 + (hasLinkPreview ? 10 : 0)), AndroidUtilities.dp(30));
            seekBarY = AndroidUtilities.dp(13) + namesOffset + mediaOffsetY;
            buttonY = AndroidUtilities.dp(13) + namesOffset + mediaOffsetY;
            radialProgress.setProgressRect(buttonX, buttonY, buttonX + AndroidUtilities.dp(44), buttonY + AndroidUtilities.dp(44));

            updatePlayingMessageProgress();
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.isOutOwner()) {
                seekBarX = layoutWidth - backgroundWidth + AndroidUtilities.dp(56);
                buttonX = layoutWidth - backgroundWidth + AndroidUtilities.dp(14);
                timeAudioX = layoutWidth - backgroundWidth + AndroidUtilities.dp(67);
            } else {
                if (isChat && !isThreadPost && currentMessageObject.needDrawAvatar()) {
                    seekBarX = AndroidUtilities.dp(113);
                    buttonX = AndroidUtilities.dp(71);
                    timeAudioX = AndroidUtilities.dp(124);
                } else {
                    seekBarX = AndroidUtilities.dp(65);
                    buttonX = AndroidUtilities.dp(23);
                    timeAudioX = AndroidUtilities.dp(76);
                }
            }
            if (hasLinkPreview) {
                seekBarX += AndroidUtilities.dp(10);
                buttonX += AndroidUtilities.dp(10);
                timeAudioX += AndroidUtilities.dp(10);
            }
            seekBar.setSize(backgroundWidth - AndroidUtilities.dp(65 + (hasLinkPreview ? 10 : 0)), AndroidUtilities.dp(30));
            seekBarY = AndroidUtilities.dp(29) + namesOffset + mediaOffsetY;
            buttonY = AndroidUtilities.dp(13) + namesOffset + mediaOffsetY;
            radialProgress.setProgressRect(buttonX, buttonY, buttonX + AndroidUtilities.dp(44), buttonY + AndroidUtilities.dp(44));

            updatePlayingMessageProgress();
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT && !drawPhotoImage) {
            if (currentMessageObject.isOutOwner()) {
                buttonX = layoutWidth - backgroundWidth + AndroidUtilities.dp(14);
            } else {
                if (isChat && !isThreadPost && currentMessageObject.needDrawAvatar()) {
                    buttonX = AndroidUtilities.dp(71);
                } else {
                    buttonX = AndroidUtilities.dp(23);
                }
            }
            if (hasLinkPreview) {
                buttonX += AndroidUtilities.dp(10);
            }
            buttonY = AndroidUtilities.dp(13) + namesOffset + mediaOffsetY;
            radialProgress.setProgressRect(buttonX, buttonY, buttonX + AndroidUtilities.dp(44), buttonY + AndroidUtilities.dp(44));
            photoImage.setImageCoords(buttonX - AndroidUtilities.dp(10), buttonY - AndroidUtilities.dp(10), photoImage.getImageWidth(), photoImage.getImageHeight());
        } else if (currentMessageObject.type == 12) {
            int x;

            if (currentMessageObject.isOutOwner()) {
                x = layoutWidth - backgroundWidth + AndroidUtilities.dp(14);
            } else {
                if (isChat && !isThreadPost && currentMessageObject.needDrawAvatar()) {
                    x = AndroidUtilities.dp(72);
                } else {
                    x = AndroidUtilities.dp(23);
                }
            }
            photoImage.setImageCoords(x, AndroidUtilities.dp(13) + namesOffset, AndroidUtilities.dp(44), AndroidUtilities.dp(44));
        } else {
            int x;
            if (currentMessageObject.type == 0 && (hasLinkPreview || hasGamePreview || hasInvoicePreview)) {
                int linkX;
                if (hasGamePreview) {
                    linkX = unmovedTextX - AndroidUtilities.dp(10);
                } else if (hasInvoicePreview) {
                    linkX = unmovedTextX + AndroidUtilities.dp(1);
                } else {
                    linkX = unmovedTextX + AndroidUtilities.dp(1);
                }
                if (isSmallImage) {
                    x = linkX + backgroundWidth - AndroidUtilities.dp(81);
                } else {
                    x = linkX + (hasInvoicePreview ? -AndroidUtilities.dp(6.3f) : AndroidUtilities.dp(10));
                }
            } else {
                if (currentMessageObject.isOutOwner()) {
                    if (mediaBackground) {
                        x = layoutWidth - backgroundWidth - AndroidUtilities.dp(3);
                    } else {
                        x = layoutWidth - backgroundWidth + AndroidUtilities.dp(6);
                    }
                } else {
                    if (isChat && isAvatarVisible && !isPlayingRound) {
                        x = AndroidUtilities.dp(63);
                    } else {
                        x = AndroidUtilities.dp(15);
                    }
                    if (currentPosition != null && !currentPosition.edge) {
                        x -= AndroidUtilities.dp(10);
                    }
                }
            }
            if (currentPosition != null) {
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                    x -= AndroidUtilities.dp(2);
                }
                if (currentPosition.leftSpanOffset != 0) {
                    x += (int) Math.ceil(currentPosition.leftSpanOffset / 1000.0f * getGroupPhotosWidth());
                }
            }
            if (currentMessageObject.type != 0) {
                x -= AndroidUtilities.dp(2);
            }
            if (!transitionParams.imageChangeBoundsTransition || transitionParams.updatePhotoImageX) {
                transitionParams.updatePhotoImageX = false;
                photoImage.setImageCoords((float) x, photoImage.getImageY(), photoImage.getImageWidth(), photoImage.getImageHeight());
            }
            buttonX = (int) (x + (photoImage.getImageWidth() - AndroidUtilities.dp(48)) / 2.0f);
            buttonY = (int) (photoImage.getImageY() + (photoImage.getImageHeight() - AndroidUtilities.dp(48)) / 2);
            radialProgress.setProgressRect(buttonX, buttonY, buttonX + AndroidUtilities.dp(48), buttonY + AndroidUtilities.dp(48));
            deleteProgressRect.set(buttonX + AndroidUtilities.dp(5), buttonY + AndroidUtilities.dp(5), buttonX + AndroidUtilities.dp(43), buttonY + AndroidUtilities.dp(43));
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                videoButtonX = (int) (photoImage.getImageX() + AndroidUtilities.dp(8));
                videoButtonY = (int) (photoImage.getImageY() + AndroidUtilities.dp(8));
                videoRadialProgress.setProgressRect(videoButtonX, videoButtonY, videoButtonX + AndroidUtilities.dp(24), videoButtonY + AndroidUtilities.dp(24));
            }
        }
    }

    public boolean needDelayRoundProgressDraw() {
        return (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) && currentMessageObject.type != 5 && MediaController.getInstance().isPlayingMessage(currentMessageObject);
    }

    public void drawRoundProgress(Canvas canvas) {
        float inset = isPlayingRound ? AndroidUtilities.dp(4) : 0;
        boolean drawPause = MediaController.getInstance().isPlayingMessage(currentMessageObject) && MediaController.getInstance().isMessagePaused();
        boolean drawTouchedSeekbar = drawPause && roundSeekbarTouched == 1;

        if (drawPause && roundToPauseProgress != 1f) {
            roundToPauseProgress += 16 / 220f;
            if (roundToPauseProgress > 1f) {
                roundToPauseProgress = 1f;
            } else {
                invalidate();
            }
        } else if (!drawPause && roundToPauseProgress != 0f){
            roundToPauseProgress -= 16 / 150f;
            if (roundToPauseProgress < 0) {
                roundToPauseProgress = 0f;
            } else {
                invalidate();
            }
        }

        if (drawTouchedSeekbar && roundToPauseProgress2 != 1f) {
            roundToPauseProgress2 += 16 / 150f;
            if (roundToPauseProgress2 > 1f) {
                roundToPauseProgress2 = 1f;
            } else {
                invalidate();
            }
        } else if (!drawTouchedSeekbar && roundToPauseProgress2 != 0f){
            roundToPauseProgress2 -= 16 / 150f;
            if (roundToPauseProgress2 < 0) {
                roundToPauseProgress2 = 0f;
            } else {
                invalidate();
            }
        }

        float pauseProgress = drawPause ? AndroidUtilities.overshootInterpolator.getInterpolation(roundToPauseProgress) : roundToPauseProgress;

        if (transitionParams.animatePlayingRound) {
            inset = (isPlayingRound ? transitionParams.animateChangeProgress : (1f - transitionParams.animateChangeProgress)) * AndroidUtilities.dp(4);
        }
        inset += AndroidUtilities.dp(16) * pauseProgress;

        if (roundToPauseProgress > 0) {
            float r = photoImage.getImageWidth() / 2f;
            Theme.getRadialSeekbarShadowDrawable().draw(canvas, photoImage.getCenterX(), photoImage.getCenterY(), r, roundToPauseProgress);
        }

        rect.set(photoImage.getImageX() + AndroidUtilities.dpf2(1.5f) + inset, photoImage.getImageY() + AndroidUtilities.dpf2(1.5f) + inset, photoImage.getImageX2() - AndroidUtilities.dpf2(1.5f) - inset, photoImage.getImageY2() - AndroidUtilities.dpf2(1.5f) - inset);
        int oldAplha = -1;
        if (roundProgressAlpha != 1f) {
            oldAplha = Theme.chat_radialProgressPaint.getAlpha();
            Theme.chat_radialProgressPaint.setAlpha((int) (roundProgressAlpha * oldAplha));
        }

        if (videoForwardDrawable != null && videoForwardDrawable.isAnimating()) {
            videoForwardDrawable.setBounds((int) photoImage.getImageX(), (int) photoImage.getImageY(), (int) (photoImage.getImageX() + photoImage.getImageWidth()), (int) (photoImage.getImageY() + photoImage.getImageHeight()));
            videoForwardDrawable.draw(canvas);
        }

        int paintAlpha = Theme.chat_radialProgressPaint.getAlpha();
        float paintWidth = Theme.chat_radialProgressPaint.getStrokeWidth();
        float audioProgress = roundProgressAlpha == 1f ? currentMessageObject.audioProgress : lastDrawingAudioProgress;
        if (pauseProgress > 0) {
            float radius = rect.width() / 2f;
            Theme.chat_radialProgressPaint.setStrokeWidth(paintWidth + paintWidth * 0.5f * roundToPauseProgress);
            Theme.chat_radialProgressPaint.setAlpha((int) (paintAlpha * roundToPauseProgress * 0.3f));
            canvas.drawCircle(rect.centerX(), rect.centerY(), radius, Theme.chat_radialProgressPaint);
            Theme.chat_radialProgressPaint.setAlpha(paintAlpha);

            seekbarRoundX = (float) (rect.centerX() + Math.sin(Math.toRadians(-360 * audioProgress + 180)) * radius);
            seekbarRoundY = (float) (rect.centerY() + Math.cos(Math.toRadians(-360 * audioProgress + 180)) * radius);
            Theme.chat_radialProgressPausedSeekbarPaint.setColor(Color.WHITE);
            Theme.chat_radialProgressPausedSeekbarPaint.setAlpha((int) (255 * Math.min(1f, pauseProgress)));
            canvas.drawCircle(seekbarRoundX, seekbarRoundY, AndroidUtilities.dp(3) + AndroidUtilities.dp(5) * pauseProgress + AndroidUtilities.dp(3) * roundToPauseProgress2, Theme.chat_radialProgressPausedSeekbarPaint);
        }
        if (roundSeekbarOutAlpha != 0f) {
            roundSeekbarOutAlpha -= 16f / 150f;
            if (roundSeekbarOutAlpha < 0) {
                roundSeekbarOutAlpha = 0f;
            } else {
                invalidate();
            }
        }
        if (roundSeekbarOutAlpha != 0f) {
            if (oldAplha == -1) {
                oldAplha = Theme.chat_radialProgressPaint.getAlpha();
            }
            Theme.chat_radialProgressPaint.setAlpha((int) (paintAlpha * (1f - roundSeekbarOutAlpha)));
            canvas.drawArc(rect, -90, 360 * audioProgress, false, Theme.chat_radialProgressPaint);
            Theme.chat_radialProgressPaint.setAlpha((int) (paintAlpha * roundSeekbarOutAlpha));
            canvas.drawArc(rect, -90, 360 * roundSeekbarOutProgress, false, Theme.chat_radialProgressPaint);
        } else {
            canvas.drawArc(rect, -90, 360 * audioProgress, false, Theme.chat_radialProgressPaint);
        }
        if (oldAplha != -1) {
            Theme.chat_radialProgressPaint.setAlpha(oldAplha);
        }
        Theme.chat_radialProgressPaint.setStrokeWidth(paintWidth);
    }

    private void updatePollAnimations(long dt) {
        if (pollVoteInProgress) {
            voteRadOffset += 360 * dt / 2000.0f;
            int count = (int) (voteRadOffset / 360);
            voteRadOffset -= count * 360;

            voteCurrentProgressTime += dt;
            if (voteCurrentProgressTime >= 500.0f) {
                voteCurrentProgressTime = 500.0f;
            }
            if (voteRisingCircleLength) {
                voteCurrentCircleLength = 4 + 266 * AndroidUtilities.accelerateInterpolator.getInterpolation(voteCurrentProgressTime / 500.0f);
            } else {
                voteCurrentCircleLength = 4 - (firstCircleLength ? 360 : 270) * (1.0f - AndroidUtilities.decelerateInterpolator.getInterpolation(voteCurrentProgressTime / 500.0f));
            }
            if (voteCurrentProgressTime == 500.0f) {
                if (voteRisingCircleLength) {
                    voteRadOffset += 270;
                    voteCurrentCircleLength = -266;
                }
                voteRisingCircleLength = !voteRisingCircleLength;
                if (firstCircleLength) {
                    firstCircleLength = false;
                }
                voteCurrentProgressTime = 0;
            }
            invalidate();
        }
        if (hintButtonVisible && hintButtonProgress < 1.0f) {
            hintButtonProgress += dt / 180.0f;
            if (hintButtonProgress > 1.0f) {
                hintButtonProgress = 1.0f;
            }
            invalidate();
        } else if (!hintButtonVisible && hintButtonProgress > 0.0f) {
            hintButtonProgress -= dt / 180.0f;
            if (hintButtonProgress < 0.0f) {
                hintButtonProgress = 0.0f;
            }
            invalidate();
        }
        if (animatePollAnswer) {
            pollAnimationProgressTime += dt;
            if (pollAnimationProgressTime >= 300.0f) {
                pollAnimationProgressTime = 300.0f;
            }
            pollAnimationProgress = AndroidUtilities.decelerateInterpolator.getInterpolation(pollAnimationProgressTime / 300.0f);
            if (pollAnimationProgress >= 1.0f) {
                pollAnimationProgress = 1.0f;
                animatePollAnswer = false;
                animatePollAnswerAlpha = false;
                pollVoteInProgress = false;
                if (pollUnvoteInProgress && animatePollAvatars) {
                    for (int a = 0; a < pollAvatarImages.length; a++) {
                        pollAvatarImages[a].setImageBitmap((Drawable) null);
                        pollAvatarImagesVisible[a] = false;
                    }
                }
                pollUnvoteInProgress = false;
                for (int a = 0, N = pollButtons.size(); a < N; a++) {
                    PollButton button = pollButtons.get(a);
                    button.prevChosen = false;
                }
            }
            invalidate();
        }
    }

    private void drawContent(Canvas canvas) {
        if (needNewVisiblePart && currentMessageObject.type == 0) {
            getLocalVisibleRect(scrollRect);
            setVisiblePart(scrollRect.top, scrollRect.bottom - scrollRect.top, parentHeight, parentViewTopOffset, viewTop, parentWidth, backgroundHeight);
            needNewVisiblePart = false;
        }

        float buttonX = this.buttonX;
        float buttonY = this.buttonY;
        if (transitionParams.animateButton) {
            buttonX = transitionParams.animateFromButtonX * (1f - transitionParams.animateChangeProgress) + this.buttonX * (transitionParams.animateChangeProgress);
            buttonY = (transitionParams.animateFromButtonY * (1f - transitionParams.animateChangeProgress) + this.buttonY * (transitionParams.animateChangeProgress));
            radialProgress.setProgressRect((int) buttonX, (int) buttonY, (int) buttonX + AndroidUtilities.dp(44), (int) buttonY + AndroidUtilities.dp(44));
        }
        if (transitionParams.animateBackgroundBoundsInner && documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            int backgroundWidth = (int) (this.backgroundWidth - transitionParams.deltaLeft + transitionParams.deltaRight);
            seekBarWaveform.setSize(backgroundWidth - AndroidUtilities.dp(92 + (hasLinkPreview ? 10 : 0)), AndroidUtilities.dp(30));
            seekBar.setSize(backgroundWidth - AndroidUtilities.dp(72 + (hasLinkPreview ? 10 : 0)), AndroidUtilities.dp(30));
        }
        forceNotDrawTime = currentMessagesGroup != null;
        photoImage.setPressed((isHighlightedAnimated || isHighlighted) && currentPosition != null ? 2 : 0);
        photoImage.setVisible(!PhotoViewer.isShowingImage(currentMessageObject) && !SecretMediaViewer.getInstance().isShowingImage(currentMessageObject), false);
        if (!photoImage.getVisible()) {
            mediaWasInvisible = true;
            timeWasInvisible = true;
            if (animatingNoSound == 1) {
                animatingNoSoundProgress = 0.0f;
                animatingNoSound = 0;
            } else if (animatingNoSound == 2) {
                animatingNoSoundProgress = 1.0f;
                animatingNoSound = 0;
            }
        } else if (groupPhotoInvisible) {
            timeWasInvisible = true;
        } else if (mediaWasInvisible || timeWasInvisible) {
            if (mediaWasInvisible) {
                controlsAlpha = 0.0f;
                mediaWasInvisible = false;
            }
            if (timeWasInvisible) {
                timeAlpha = 0.0f;
                timeWasInvisible = false;
            }
            lastControlsAlphaChangeTime = System.currentTimeMillis();
            totalChangeTime = 0;
        }
        radialProgress.setProgressColor(getThemedColor(Theme.key_chat_mediaProgress));
        videoRadialProgress.setProgressColor(getThemedColor(Theme.key_chat_mediaProgress));

        imageDrawn = false;
        radialProgress.setCircleCrossfadeColor(null, 0.0f, 1.0f);
        if (currentMessageObject.type == 0) {
            if (currentMessageObject.isOutOwner()) {
                textX = getCurrentBackgroundLeft() + AndroidUtilities.dp(11) + getExtraTextX();
            } else {
                textX = getCurrentBackgroundLeft() + AndroidUtilities.dp(!mediaBackground && drawPinnedBottom ? 11 : 17) + getExtraTextX();
            }
            if (hasGamePreview) {
                textX += AndroidUtilities.dp(11);
                textY = AndroidUtilities.dp(14) + namesOffset;
                if (siteNameLayout != null) {
                    textY += siteNameLayout.getLineBottom(siteNameLayout.getLineCount() - 1);
                }
            } else if (hasInvoicePreview) {
                textY = AndroidUtilities.dp(14) + namesOffset;
                if (siteNameLayout != null) {
                    textY += siteNameLayout.getLineBottom(siteNameLayout.getLineCount() - 1);
                }
            } else {
                textY = AndroidUtilities.dp(10) + namesOffset;
            }
            unmovedTextX = textX;
            if (currentMessageObject.textXOffset != 0 && replyNameLayout != null) {
                int diff = backgroundWidth - AndroidUtilities.dp(31) - currentMessageObject.textWidth;
                if (!hasNewLineForTime) {
                    diff -= timeWidth + AndroidUtilities.dp(4 + (currentMessageObject.isOutOwner() ? 20 : 0));
                }
                if (diff > 0) {
                    textX += diff - getExtraTimeX();
                }
            }
            if (!enterTransitionInPorgress && currentMessageObject != null && !currentMessageObject.preview) {
                if (transitionParams.animateChangeProgress != 1.0f && transitionParams.animateMessageText) {
                    canvas.save();
                    if (currentBackgroundDrawable != null) {
                        Rect r = currentBackgroundDrawable.getBounds();
                        if (currentMessageObject.isOutOwner() && !mediaBackground && !pinnedBottom) {
                            canvas.clipRect(
                                    r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                                    r.right - AndroidUtilities.dp(10), r.bottom - AndroidUtilities.dp(4)
                            );
                        } else {
                            canvas.clipRect(
                                    r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                                    r.right - AndroidUtilities.dp(4), r.bottom - AndroidUtilities.dp(4)
                            );
                        }
                    }
                    drawMessageText(canvas, transitionParams.animateOutTextBlocks, false, (1.0f - transitionParams.animateChangeProgress), false);
                    drawMessageText(canvas, currentMessageObject.textLayoutBlocks, true, transitionParams.animateChangeProgress, false);
                    canvas.restore();
                } else {
                    drawMessageText(canvas, currentMessageObject.textLayoutBlocks, true, 1.0f, false);
                }
            }

            if (!(enterTransitionInPorgress && !currentMessageObject.isVoice())) {
                drawLinkPreview(canvas, 1f);
            }
            drawTime = true;
        } else if (drawPhotoImage) {
            if (isRoundVideo && MediaController.getInstance().isPlayingMessage(currentMessageObject) && MediaController.getInstance().isVideoDrawingReady() && canvas.isHardwareAccelerated()) {
                imageDrawn = true;
                drawTime = true;
            } else {
                if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO && Theme.chat_roundVideoShadow != null) {
                    float x = photoImage.getImageX() - AndroidUtilities.dp(3);
                    float y = photoImage.getImageY() - AndroidUtilities.dp(2);
                    Theme.chat_roundVideoShadow.setAlpha(255/*(int) (photoImage.getCurrentAlpha() * 255)*/);
                    Theme.chat_roundVideoShadow.setBounds((int) x, (int) y, (int) (x + photoImage.getImageWidth() + AndroidUtilities.dp(6)), (int) (y + photoImage.getImageHeight() + AndroidUtilities.dp(6)));
                    Theme.chat_roundVideoShadow.draw(canvas);

                    if (!photoImage.hasBitmapImage() || photoImage.getCurrentAlpha() != 1) {
                        Theme.chat_docBackPaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outBubble : Theme.key_chat_inBubble));
                        canvas.drawCircle(photoImage.getCenterX(), photoImage.getCenterY(), photoImage.getImageWidth() / 2, Theme.chat_docBackPaint);
                    }
                } else if (currentMessageObject.type == 4) {
                    rect.set(photoImage.getImageX(), photoImage.getImageY(), photoImage.getImageX2(), photoImage.getImageY2());
                    Theme.chat_docBackPaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outLocationBackground : Theme.key_chat_inLocationBackground));
                    int[] rad = photoImage.getRoundRadius();
                    rectPath.reset();
                    for (int a = 0; a < rad.length; a++) {
                        radii[a * 2] = radii[a * 2 + 1] = rad[a];
                    }
                    rectPath.addRoundRect(rect, radii, Path.Direction.CW);
                    rectPath.close();
                    canvas.drawPath(rectPath, Theme.chat_docBackPaint);

                    Drawable iconDrawable = Theme.chat_locationDrawable[currentMessageObject.isOutOwner() ? 1 : 0];
                    setDrawableBounds(iconDrawable, rect.centerX() - iconDrawable.getIntrinsicWidth() / 2, rect.centerY() - iconDrawable.getIntrinsicHeight() / 2);
                    iconDrawable.draw(canvas);
                }
                drawMediaCheckBox = mediaCheckBox != null && (checkBoxVisible || mediaCheckBox.getProgress() != 0 || checkBoxAnimationInProgress) && currentMessagesGroup != null;
                if (drawMediaCheckBox && (mediaCheckBox.isChecked() || mediaCheckBox.getProgress() != 0 || checkBoxAnimationInProgress) && (!textIsSelectionMode())) {
                    if (!currentMessagesGroup.isDocuments) {
                        Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outBubbleSelected : Theme.key_chat_inBubbleSelected));
                        rect.set(photoImage.getImageX(), photoImage.getImageY(), photoImage.getImageX2(), photoImage.getImageY2());
                        int[] rad = photoImage.getRoundRadius();
                        rectPath.reset();
                        for (int a = 0; a < rad.length; a++) {
                            radii[a * 2] = radii[a * 2 + 1] = rad[a];
                        }
                        rectPath.addRoundRect(rect, radii, Path.Direction.CW);
                        rectPath.close();
                        canvas.drawPath(rectPath, Theme.chat_replyLinePaint);
                    }
                    photoImage.setSideClip(AndroidUtilities.dp(14) * mediaCheckBox.getProgress());
                    if (checkBoxAnimationInProgress) {
                        mediaCheckBox.setBackgroundAlpha(checkBoxAnimationProgress);
                    } else {
                        mediaCheckBox.setBackgroundAlpha(checkBoxVisible ? 1.0f : mediaCheckBox.getProgress());
                    }
                } else {
                    photoImage.setSideClip(0);
                }
                if (delegate == null || delegate.getPinchToZoomHelper() == null || !delegate.getPinchToZoomHelper().isInOverlayModeFor(this)) {
                    imageDrawn = photoImage.draw(canvas);
                }
                boolean drawTimeOld = drawTime;
                drawTime = photoImage.getVisible();
                if (currentPosition != null && drawTimeOld != drawTime) {
                    ViewGroup viewGroup = (ViewGroup) getParent();
                    if (viewGroup != null) {
                        if (!currentPosition.last) {
                            int count = viewGroup.getChildCount();
                            for (int a = 0; a < count; a++) {
                                View child = viewGroup.getChildAt(a);
                                if (child == this || !(child instanceof ChatMessageCell)) {
                                    continue;
                                }
                                ChatMessageCell cell = (ChatMessageCell) child;

                                if (cell.getCurrentMessagesGroup() == currentMessagesGroup) {
                                    MessageObject.GroupedMessagePosition position = cell.getCurrentPosition();
                                    if (position.last && position.maxY == currentPosition.maxY && cell.timeX - AndroidUtilities.dp(4) + cell.getLeft() < getRight()) {
                                        cell.groupPhotoInvisible = !drawTime;
                                        cell.invalidate();
                                        viewGroup.invalidate();
                                    }
                                }
                            }
                        } else {
                            viewGroup.invalidate();
                        }
                    }
                }
            }
        } else {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                drawMediaCheckBox = mediaCheckBox != null && (checkBoxVisible || mediaCheckBox.getProgress() != 0 || checkBoxAnimationInProgress) && currentMessagesGroup != null;
                if (drawMediaCheckBox) {
                    radialProgress.setCircleCrossfadeColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outTimeText : Theme.key_chat_inTimeText, checkBoxAnimationProgress, 1.0f - mediaCheckBox.getProgress());
                }
                if (drawMediaCheckBox && !textIsSelectionMode() && (mediaCheckBox.isChecked() || mediaCheckBox.getProgress() != 0 || checkBoxAnimationInProgress)) {
                    if (checkBoxAnimationInProgress) {
                        mediaCheckBox.setBackgroundAlpha(checkBoxAnimationProgress);
                        if (radialProgress.getMiniIcon() == MediaActionDrawable.ICON_NONE) {
                            radialProgress.setMiniIconScale(checkBoxAnimationProgress);
                        }
                    } else {
                        mediaCheckBox.setBackgroundAlpha(checkBoxVisible ? 1.0f : mediaCheckBox.getProgress());
                    }
                } else if (mediaCheckBox != null) {
                    mediaCheckBox.setBackgroundAlpha(1.0f);
                }
            }
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
            if (photoImage.getVisible() && !hasGamePreview && !currentMessageObject.needDrawBluredPreview()) {
                int oldAlpha = ((BitmapDrawable) Theme.chat_msgMediaMenuDrawable).getPaint().getAlpha();
                Theme.chat_msgMediaMenuDrawable.setAlpha((int) (oldAlpha * controlsAlpha));
                setDrawableBounds(Theme.chat_msgMediaMenuDrawable, otherX = (int) (photoImage.getImageX() + photoImage.getImageWidth() - AndroidUtilities.dp(14)), otherY = (int) (photoImage.getImageY() + AndroidUtilities.dp(8.1f)));
                Theme.chat_msgMediaMenuDrawable.draw(canvas);
                Theme.chat_msgMediaMenuDrawable.setAlpha(oldAlpha);
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
            if (durationLayout != null) {
                boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
                if (playing || roundProgressAlpha != 0) {
                    if (playing) {
                        roundProgressAlpha = 1f;
                    } else {
                        roundProgressAlpha -= 16 / 150f;
                        if (roundProgressAlpha < 0) {
                            roundProgressAlpha = 0;
                        } else {
                            invalidate();
                        }
                    }
                    drawRoundProgress(canvas);
                }
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.isOutOwner()) {
                Theme.chat_audioTitlePaint.setColor(getThemedColor(Theme.key_chat_outAudioTitleText));
                Theme.chat_audioPerformerPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outAudioPerformerSelectedText : Theme.key_chat_outAudioPerformerText));
                Theme.chat_audioTimePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outAudioDurationSelectedText : Theme.key_chat_outAudioDurationText));
                radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || buttonPressed != 0 ? Theme.key_chat_outAudioSelectedProgress : Theme.key_chat_outAudioProgress));
            } else {
                Theme.chat_audioTitlePaint.setColor(getThemedColor(Theme.key_chat_inAudioTitleText));
                Theme.chat_audioPerformerPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inAudioPerformerSelectedText : Theme.key_chat_inAudioPerformerText));
                Theme.chat_audioTimePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inAudioDurationSelectedText : Theme.key_chat_inAudioDurationText));
                radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || buttonPressed != 0 ? Theme.key_chat_inAudioSelectedProgress : Theme.key_chat_inAudioProgress));
            }

            radialProgress.setBackgroundDrawable(isDrawSelectionBackground() ? currentBackgroundSelectedDrawable : currentBackgroundDrawable);
            radialProgress.draw(canvas);

            canvas.save();
            canvas.translate(timeAudioX + songX, AndroidUtilities.dp(13) + namesOffset + mediaOffsetY);
            songLayout.draw(canvas);
            canvas.restore();
            
            boolean showSeekbar = MediaController.getInstance().isPlayingMessage(currentMessageObject);
            if (showSeekbar && toSeekBarProgress != 1f) {
                toSeekBarProgress += 16f / 100f;
                if (toSeekBarProgress > 1f) {
                    toSeekBarProgress = 1f;
                }
                invalidate();
            } else if (!showSeekbar && toSeekBarProgress != 0){
                toSeekBarProgress -= 16f / 100f;
                if (toSeekBarProgress < 0) {
                    toSeekBarProgress = 0;
                }
                invalidate();
            }
            if (toSeekBarProgress > 0) {
                if (toSeekBarProgress != 1f) {
                    canvas.saveLayerAlpha(seekBarX, seekBarY, seekBarX + seekBar.getWidth() + AndroidUtilities.dp(24), seekBarY + AndroidUtilities.dp(24), (int) (255 * (toSeekBarProgress)), Canvas.ALL_SAVE_FLAG);
                } else {
                    canvas.save();
                }
                canvas.translate(seekBarX, seekBarY);
                seekBar.draw(canvas);
                canvas.restore();
            }
            if (toSeekBarProgress < 1f) {
                float x = timeAudioX + performerX;
                float y = AndroidUtilities.dp(35) + namesOffset + mediaOffsetY;
                if (toSeekBarProgress != 0) {
                    canvas.saveLayerAlpha(x, y, x + performerLayout.getWidth(), y + performerLayout.getHeight(), (int) (255 * (1f - toSeekBarProgress)), Canvas.ALL_SAVE_FLAG);
                } else {
                    canvas.save();
                }
                if (toSeekBarProgress != 0) {
                    float s = 0.7f + 0.3f * (1f - toSeekBarProgress);
                    canvas.scale(s, s, x, y + performerLayout.getHeight() / 2f);
                }
                canvas.translate(x, y);
                performerLayout.draw(canvas);
                canvas.restore();
            }

            canvas.save();
            canvas.translate(timeAudioX, AndroidUtilities.dp(57) + namesOffset + mediaOffsetY);
            durationLayout.draw(canvas);
            canvas.restore();

            if (shouldDrawMenuDrawable()) {
                Drawable menuDrawable;
                if (currentMessageObject.isOutOwner()) {
                    menuDrawable = getThemedDrawable(isDrawSelectionBackground() ? Theme.key_drawable_msgOutMenuSelected : Theme.key_drawable_msgOutMenu);
                } else {
                    menuDrawable = isDrawSelectionBackground() ? Theme.chat_msgInMenuSelectedDrawable : Theme.chat_msgInMenuDrawable;
                }
                setDrawableBounds(menuDrawable, otherX = (int) buttonX + backgroundWidth - AndroidUtilities.dp(currentMessageObject.type == 0 ? 58 : 48), otherY = (int) buttonY - AndroidUtilities.dp(2));
                if (transitionParams.animateChangeProgress != 1f && transitionParams.animateShouldDrawMenuDrawable) {
                    menuDrawable.setAlpha((int) (255 * transitionParams.animateChangeProgress));
                }
                menuDrawable.draw(canvas);
                if (transitionParams.animateChangeProgress != 1f && transitionParams.animateShouldDrawMenuDrawable) {
                    menuDrawable.setAlpha(255);
                }
            }
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
            if (currentMessageObject.isOutOwner()) {
                Theme.chat_audioTimePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outAudioDurationSelectedText : Theme.key_chat_outAudioDurationText));
                radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || buttonPressed != 0 ? Theme.key_chat_outAudioSelectedProgress : Theme.key_chat_outAudioProgress));
            } else {
                Theme.chat_audioTimePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inAudioDurationSelectedText : Theme.key_chat_inAudioDurationText));
                radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || buttonPressed != 0 ? Theme.key_chat_inAudioSelectedProgress : Theme.key_chat_inAudioProgress));
            }
            AudioVisualizerDrawable audioVisualizerDrawable;
            if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                audioVisualizerDrawable = Theme.getCurrentAudiVisualizerDrawable();
            } else {
                audioVisualizerDrawable = Theme.getAnimatedOutAudioVisualizerDrawable(currentMessageObject);
            }

            if (audioVisualizerDrawable != null) {
                audioVisualizerDrawable.setParentView(this);
                audioVisualizerDrawable.draw(canvas, buttonX + AndroidUtilities.dp(22), buttonY + AndroidUtilities.dp(22), currentMessageObject.isOutOwner(), resourcesProvider);
            }

            if (!enterTransitionInPorgress) {
                radialProgress.setBackgroundDrawable(isDrawSelectionBackground() ? currentBackgroundSelectedDrawable : currentBackgroundDrawable);
                radialProgress.draw(canvas);
            }

            int seekBarX = this.seekBarX;
            int timeAudioX = this.timeAudioX;
            if (transitionParams.animateButton) {
                int offset = this.buttonX - (int) (transitionParams.animateFromButtonX * (1f - transitionParams.animateChangeProgress) + this.buttonX * (transitionParams.animateChangeProgress));
                seekBarX -= offset;
                timeAudioX -= offset;
            }
            canvas.save();
            if (useSeekBarWaweform) {
                canvas.translate(seekBarX + AndroidUtilities.dp(13), seekBarY);
                seekBarWaveform.draw(canvas, this);
            } else {
                canvas.translate(seekBarX, seekBarY);
                seekBar.draw(canvas);
            }
            canvas.restore();

            canvas.save();
            canvas.translate(timeAudioX, AndroidUtilities.dp(44) + namesOffset + mediaOffsetY);
            durationLayout.draw(canvas);
            canvas.restore();

            if (currentMessageObject.type != 0 && currentMessageObject.isContentUnread()) {
                Theme.chat_docBackPaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outVoiceSeekbarFill : Theme.key_chat_inVoiceSeekbarFill));
                canvas.drawCircle(timeAudioX + timeWidthAudio + AndroidUtilities.dp(6), AndroidUtilities.dp(51) + namesOffset + mediaOffsetY, AndroidUtilities.dp(3), Theme.chat_docBackPaint);
            }
        }

        if (captionLayout != null) {
            updateCaptionLayout();
        }
        if (!currentMessageObject.preview && (currentPosition == null || currentMessagesGroup != null && currentMessagesGroup.isDocuments) && !transitionParams.transformGroupToSingleMessage && !(enterTransitionInPorgress && currentMessageObject.isVoice())) {
            drawCaptionLayout(canvas, false, 1f);
        }

        if (hasOldCaptionPreview) {
            int linkX;
            if (currentMessageObject.type == MessageObject.TYPE_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || currentMessageObject.type == 8) {
                linkX = (int) (photoImage.getImageX() + AndroidUtilities.dp(5));
            } else {
                linkX = backgroundDrawableLeft + AndroidUtilities.dp(currentMessageObject.isOutOwner() ? 11 : 17);
            }
            int startY = totalHeight - AndroidUtilities.dp(drawPinnedTop ? 9 : 10) - linkPreviewHeight - AndroidUtilities.dp(8);
            int linkPreviewY = startY;

            Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outPreviewLine : Theme.key_chat_inPreviewLine));
            canvas.drawRect(linkX, linkPreviewY - AndroidUtilities.dp(3), linkX + AndroidUtilities.dp(2), linkPreviewY + linkPreviewHeight, Theme.chat_replyLinePaint);

            if (siteNameLayout != null) {
                Theme.chat_replyNamePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outSiteNameText : Theme.key_chat_inSiteNameText));
                canvas.save();
                int x;
                if (siteNameRtl) {
                    x = backgroundWidth - siteNameWidth - AndroidUtilities.dp(32);
                } else {
                    x = (hasInvoicePreview ? 0 : AndroidUtilities.dp(10));
                }
                canvas.translate(linkX + x, linkPreviewY - AndroidUtilities.dp(3));
                siteNameLayout.draw(canvas);
                canvas.restore();
                linkPreviewY += siteNameLayout.getLineBottom(siteNameLayout.getLineCount() - 1);
            }

            if (currentMessageObject.isOutOwner()) {
                Theme.chat_replyTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
            } else {
                Theme.chat_replyTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            }

            if (descriptionLayout != null) {
                if (linkPreviewY != startY) {
                    linkPreviewY += AndroidUtilities.dp(2);
                }
                descriptionY = linkPreviewY - AndroidUtilities.dp(3);
                canvas.save();
                canvas.translate(linkX + AndroidUtilities.dp(10) + descriptionX, descriptionY);
                descriptionLayout.draw(canvas);
                canvas.restore();
            }
            drawTime = true;
        }

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
            Drawable menuDrawable;
            if (currentMessageObject.isOutOwner()) {
                Theme.chat_docNamePaint.setColor(getThemedColor(Theme.key_chat_outFileNameText));
                Theme.chat_infoPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outFileInfoSelectedText : Theme.key_chat_outFileInfoText));
                Theme.chat_docBackPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outFileBackgroundSelected : Theme.key_chat_outFileBackground));
                menuDrawable = getThemedDrawable(isDrawSelectionBackground() ? Theme.key_drawable_msgOutMenuSelected : Theme.key_drawable_msgOutMenu);
            } else {
                Theme.chat_docNamePaint.setColor(getThemedColor(Theme.key_chat_inFileNameText));
                Theme.chat_infoPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inFileInfoSelectedText : Theme.key_chat_inFileInfoText));
                Theme.chat_docBackPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inFileBackgroundSelected : Theme.key_chat_inFileBackground));
                menuDrawable = isDrawSelectionBackground() ? Theme.chat_msgInMenuSelectedDrawable : Theme.chat_msgInMenuDrawable;
            }

            float x;
            int titleY;
            int subtitleY;
            if (drawPhotoImage) {
                if (currentMessageObject.type == 0) {
                    setDrawableBounds(menuDrawable, otherX = (int) (photoImage.getImageX() + backgroundWidth - AndroidUtilities.dp(56)), otherY = (int) (photoImage.getImageY() + AndroidUtilities.dp(4)));
                } else {
                    setDrawableBounds(menuDrawable, otherX = (int) (photoImage.getImageX() + backgroundWidth - AndroidUtilities.dp(40)), otherY = (int) (photoImage.getImageY() + AndroidUtilities.dp(4)));
                }

                x = (int) (photoImage.getImageX() + photoImage.getImageWidth() + AndroidUtilities.dp(10));
                titleY = (int) (photoImage.getImageY() + AndroidUtilities.dp(8));
                subtitleY = (int) (photoImage.getImageY() + (docTitleLayout != null ? docTitleLayout.getLineBottom(docTitleLayout.getLineCount() - 1) + AndroidUtilities.dp(13) : AndroidUtilities.dp(8)));
                if (!imageDrawn) {
                    if (currentMessageObject.isOutOwner()) {
                        radialProgress.setColors(Theme.key_chat_outLoader, Theme.key_chat_outLoaderSelected, Theme.key_chat_outMediaIcon, Theme.key_chat_outMediaIconSelected);
                        radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outFileProgressSelected : Theme.key_chat_outFileProgress));
                        videoRadialProgress.setColors(Theme.key_chat_outLoader, Theme.key_chat_outLoaderSelected, Theme.key_chat_outMediaIcon, Theme.key_chat_outMediaIconSelected);
                        videoRadialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outFileProgressSelected : Theme.key_chat_outFileProgress));
                    } else {
                        radialProgress.setColors(Theme.key_chat_inLoader, Theme.key_chat_inLoaderSelected, Theme.key_chat_inMediaIcon, Theme.key_chat_inMediaIconSelected);
                        radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inFileProgressSelected : Theme.key_chat_inFileProgress));
                        videoRadialProgress.setColors(Theme.key_chat_inLoader, Theme.key_chat_inLoaderSelected, Theme.key_chat_inMediaIcon, Theme.key_chat_inMediaIconSelected);
                        videoRadialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inFileProgressSelected : Theme.key_chat_inFileProgress));
                    }

                    rect.set(photoImage.getImageX(), photoImage.getImageY(), photoImage.getImageX() + photoImage.getImageWidth(), photoImage.getImageY() + photoImage.getImageHeight());

                    int[] rad = photoImage.getRoundRadius();
                    rectPath.reset();
                    for (int a = 0; a < rad.length; a++) {
                        radii[a * 2] = rad[a];
                        radii[a * 2 + 1] = rad[a];
                    }
                    rectPath.addRoundRect(rect, radii, Path.Direction.CW);
                    rectPath.close();
                    canvas.drawPath(rectPath, Theme.chat_docBackPaint);
                } else {
                    radialProgress.setColors(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
                    radialProgress.setProgressColor(getThemedColor(Theme.key_chat_mediaProgress));
                    videoRadialProgress.setColors(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
                    videoRadialProgress.setProgressColor(getThemedColor(Theme.key_chat_mediaProgress));

                    if (buttonState == -1 && radialProgress.getIcon() != MediaActionDrawable.ICON_NONE) {
                        radialProgress.setIcon(MediaActionDrawable.ICON_NONE, true, true);
                    }
                }
            } else {
                setDrawableBounds(menuDrawable, otherX = (int) buttonX + backgroundWidth - AndroidUtilities.dp(currentMessageObject.type == 0 ? 58 : 48), otherY = (int) buttonY - AndroidUtilities.dp(2));
                x = buttonX + AndroidUtilities.dp(53);
                titleY = (int) buttonY + AndroidUtilities.dp(4);
                subtitleY = (int) buttonY + AndroidUtilities.dp(27);
                if (docTitleLayout != null && docTitleLayout.getLineCount() > 1) {
                    subtitleY += (docTitleLayout.getLineCount() - 1) * AndroidUtilities.dp(16) + AndroidUtilities.dp(2);
                }
                if (currentMessageObject.isOutOwner()) {
                    radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || buttonPressed != 0 ? Theme.key_chat_outAudioSelectedProgress : Theme.key_chat_outAudioProgress));
                    videoRadialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || videoButtonPressed != 0 ? Theme.key_chat_outAudioSelectedProgress : Theme.key_chat_outAudioProgress));
                } else {
                    radialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || buttonPressed != 0 ? Theme.key_chat_inAudioSelectedProgress : Theme.key_chat_inAudioProgress));
                    videoRadialProgress.setProgressColor(getThemedColor(isDrawSelectionBackground() || videoButtonPressed != 0 ? Theme.key_chat_inAudioSelectedProgress : Theme.key_chat_inAudioProgress));
                }
            }
            if (shouldDrawMenuDrawable()) {
                if (transitionParams.animateChangeProgress != 1f && transitionParams.animateShouldDrawMenuDrawable) {
                    menuDrawable.setAlpha((int) (255 * transitionParams.animateChangeProgress));
                }
                menuDrawable.draw(canvas);
                if (transitionParams.animateChangeProgress != 1f && transitionParams.animateShouldDrawMenuDrawable) {
                    menuDrawable.setAlpha(255);
                }
            }

            try {
                if (docTitleLayout != null) {
                    canvas.save();
                    canvas.translate(x + docTitleOffsetX, titleY);
                    docTitleLayout.draw(canvas);
                    canvas.restore();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }

            try {
                if (infoLayout != null) {
                    canvas.save();
                    canvas.translate(x, subtitleY);
                    if (buttonState == 1 && loadingProgressLayout != null) {
                        loadingProgressLayout.draw(canvas);
                    } else {
                        infoLayout.draw(canvas);
                    }
                    canvas.restore();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (currentMessageObject.type == 4 && !(currentMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGeoLive) && currentMapProvider == 2 && photoImage.hasNotThumb()) {
            int w = (int) (Theme.chat_redLocationIcon.getIntrinsicWidth() * 0.8f);
            int h = (int) (Theme.chat_redLocationIcon.getIntrinsicHeight() * 0.8f);
            int x = (int) (photoImage.getImageX() + (photoImage.getImageWidth() - w) / 2);
            int y = (int) (photoImage.getImageY() + (photoImage.getImageHeight() / 2 - h));
            Theme.chat_redLocationIcon.setAlpha((int) (255 * photoImage.getCurrentAlpha()));
            Theme.chat_redLocationIcon.setBounds(x, y, x + w, y + h);
            Theme.chat_redLocationIcon.draw(canvas);
        }
        transitionParams.recordDrawingState();
    }

    public void drawLinkPreview(Canvas canvas, float alpha) {
        if (!currentMessageObject.isSponsored() && !hasLinkPreview && !hasGamePreview && !hasInvoicePreview) {
            return;
        }
        int startY;
        int linkX;
        if (hasGamePreview) {
            startY = AndroidUtilities.dp(14) + namesOffset;
            linkX = unmovedTextX - AndroidUtilities.dp(10);
        } else if (hasInvoicePreview) {
            startY = AndroidUtilities.dp(14) + namesOffset;
            linkX = unmovedTextX + AndroidUtilities.dp(1);
        } else if (currentMessageObject.isSponsored()) {
            startY = textY + currentMessageObject.textHeight - AndroidUtilities.dp(2);
            if (hasNewLineForTime) {
                startY += AndroidUtilities.dp(16);
            }
            linkX = unmovedTextX + AndroidUtilities.dp(1);
        } else {
            startY = textY + currentMessageObject.textHeight + AndroidUtilities.dp(8);
            linkX = unmovedTextX + AndroidUtilities.dp(1);
        }
        int linkPreviewY = startY;
        int smallImageStartY = 0;

        if (!hasInvoicePreview && !currentMessageObject.isSponsored()) {
            Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outPreviewLine : Theme.key_chat_inPreviewLine));
            if (alpha != 1f) {
                Theme.chat_replyLinePaint.setAlpha((int) (alpha * Theme.chat_replyLinePaint.getAlpha()));
            }
            canvas.drawRect(linkX, linkPreviewY - AndroidUtilities.dp(3), linkX + AndroidUtilities.dp(2), linkPreviewY + linkPreviewHeight + AndroidUtilities.dp(3), Theme.chat_replyLinePaint);
        }

        if (siteNameLayout != null) {
            smallImageStartY = linkPreviewY - AndroidUtilities.dp(1);
            Theme.chat_replyNamePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outSiteNameText : Theme.key_chat_inSiteNameText));
            if (alpha != 1f) {
                Theme.chat_replyNamePaint.setAlpha((int) (alpha * Theme.chat_replyLinePaint.getAlpha()));
            }
            canvas.save();
            int x;
            if (siteNameRtl) {
                x = backgroundWidth - siteNameWidth - AndroidUtilities.dp(32);
                if (isSmallImage) {
                    x -= AndroidUtilities.dp(48 + 6);
                }
            } else {
                x = (hasInvoicePreview ? 0 : AndroidUtilities.dp(10));
            }
            canvas.translate(linkX + x, linkPreviewY - AndroidUtilities.dp(3));
            siteNameLayout.draw(canvas);
            canvas.restore();
            linkPreviewY += siteNameLayout.getLineBottom(siteNameLayout.getLineCount() - 1);
        }
        if ((hasGamePreview || hasInvoicePreview) && currentMessageObject.textHeight != 0) {
            startY += currentMessageObject.textHeight + AndroidUtilities.dp(4);
            linkPreviewY += currentMessageObject.textHeight + AndroidUtilities.dp(4);
        }

        if (drawPhotoImage && drawInstantView && drawInstantViewType != 9 || drawInstantViewType == 6 && imageBackgroundColor != 0) {
            if (linkPreviewY != startY) {
                linkPreviewY += AndroidUtilities.dp(2);
            }
            if (imageBackgroundSideColor != 0) {
                int x = linkX + AndroidUtilities.dp(10);
                photoImage.setImageCoords(x + (imageBackgroundSideWidth - photoImage.getImageWidth()) / 2, linkPreviewY, photoImage.getImageWidth(), photoImage.getImageHeight());
                rect.set(x, photoImage.getImageY(), x + imageBackgroundSideWidth, photoImage.getImageY2());
                Theme.chat_instantViewPaint.setColor(ColorUtils.setAlphaComponent(imageBackgroundSideColor, (int) (255 * alpha)));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), Theme.chat_instantViewPaint);
            } else {
                photoImage.setImageCoords(linkX + AndroidUtilities.dp(10), linkPreviewY, photoImage.getImageWidth(), photoImage.getImageHeight());
            }
            if (imageBackgroundColor != 0) {
                rect.set(photoImage.getImageX(), photoImage.getImageY(), photoImage.getImageX2(), photoImage.getImageY2());
                if (imageBackgroundGradientColor1 != 0) {
                    if (imageBackgroundGradientColor2 != 0) {
                        if (motionBackgroundDrawable == null) {
                            motionBackgroundDrawable = new MotionBackgroundDrawable(imageBackgroundColor, imageBackgroundGradientColor1, imageBackgroundGradientColor2, imageBackgroundGradientColor3, true);
                            if (imageBackgroundIntensity < 0) {
                                photoImage.setGradientBitmap(motionBackgroundDrawable.getBitmap());
                            }
                            if (!photoImage.hasImageSet()) {
                                motionBackgroundDrawable.setRoundRadius(AndroidUtilities.dp(4));
                            }
                        }
                    } else {
                        if (gradientShader == null) {
                            Rect r = BackgroundGradientDrawable.getGradientPoints(AndroidUtilities.getWallpaperRotation(imageBackgroundGradientRotation, false), (int) rect.width(), (int) rect.height());
                            gradientShader = new LinearGradient(r.left, r.top, r.right, r.bottom, new int[]{imageBackgroundColor, imageBackgroundGradientColor1}, null, Shader.TileMode.CLAMP);
                        }
                        Theme.chat_instantViewPaint.setShader(gradientShader);
                        if (alpha != 1f) {
                            Theme.chat_instantViewPaint.setAlpha((int) (255 * alpha));
                        }
                    }
                } else {
                    Theme.chat_instantViewPaint.setShader(null);
                    Theme.chat_instantViewPaint.setColor(imageBackgroundColor);
                    if (alpha != 1f) {
                        Theme.chat_instantViewPaint.setAlpha((int) (255 * alpha));
                    }
                }
                if (motionBackgroundDrawable != null) {
                    motionBackgroundDrawable.setBounds((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
                    motionBackgroundDrawable.draw(canvas);
                } else if (imageBackgroundSideColor != 0) {
                    canvas.drawRect(photoImage.getImageX(), photoImage.getImageY(), photoImage.getImageX2(), photoImage.getImageY2(), Theme.chat_instantViewPaint);
                } else {
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), Theme.chat_instantViewPaint);
                }
                Theme.chat_instantViewPaint.setShader(null);
                Theme.chat_instantViewPaint.setAlpha(255);
            }
            if (drawPhotoImage && drawInstantView && drawInstantViewType != 9) {
                if (drawImageButton) {
                    int size = AndroidUtilities.dp(48);
                    buttonX = this.buttonX = (int) (photoImage.getImageX() + (photoImage.getImageWidth() - size) / 2.0f);
                    buttonY = this.buttonY = (int) (photoImage.getImageY() + (photoImage.getImageHeight() - size) / 2.0f);
                    radialProgress.setProgressRect((int) buttonX, (int ) buttonY, (int) buttonX + size, (int) buttonY + size);
                }
                if (delegate == null || delegate.getPinchToZoomHelper() == null || !delegate.getPinchToZoomHelper().isInOverlayModeFor(this)) {
                    if (alpha != 1f) {
                        photoImage.setAlpha(alpha);
                        imageDrawn = photoImage.draw(canvas);
                        photoImage.setAlpha(1f);
                    } else {
                        imageDrawn = photoImage.draw(canvas);
                    }
                }
            }
            linkPreviewY += photoImage.getImageHeight() + AndroidUtilities.dp(6);
        }

        if (currentMessageObject.isOutOwner()) {
            Theme.chat_replyNamePaint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messageTextOut), (int) (255 * alpha)));
            Theme.chat_replyTextPaint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messageTextOut), (int) (255 * alpha)));
        } else {
            Theme.chat_replyNamePaint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messageTextIn), (int) (255 * alpha)));
            Theme.chat_replyTextPaint.setColor(ColorUtils.setAlphaComponent(getThemedColor(Theme.key_chat_messageTextIn), (int) (255 * alpha)));
        }
        if (titleLayout != null) {
            if (linkPreviewY != startY) {
                linkPreviewY += AndroidUtilities.dp(2);
            }
            if (smallImageStartY == 0) {
                smallImageStartY = linkPreviewY - AndroidUtilities.dp(1);
            }
            canvas.save();
            canvas.translate(linkX + AndroidUtilities.dp(10) + titleX, linkPreviewY - AndroidUtilities.dp(3));
            titleLayout.draw(canvas);
            canvas.restore();
            linkPreviewY += titleLayout.getLineBottom(titleLayout.getLineCount() - 1);
        }

        if (authorLayout != null) {
            if (linkPreviewY != startY) {
                linkPreviewY += AndroidUtilities.dp(2);
            }
            if (smallImageStartY == 0) {
                smallImageStartY = linkPreviewY - AndroidUtilities.dp(1);
            }
            canvas.save();
            canvas.translate(linkX + AndroidUtilities.dp(10) + authorX, linkPreviewY - AndroidUtilities.dp(3));
            authorLayout.draw(canvas);
            canvas.restore();
            linkPreviewY += authorLayout.getLineBottom(authorLayout.getLineCount() - 1);
        }

        if (descriptionLayout != null) {
            if (linkPreviewY != startY) {
                linkPreviewY += AndroidUtilities.dp(2);
            }
            if (smallImageStartY == 0) {
                smallImageStartY = linkPreviewY - AndroidUtilities.dp(1);
            }
            descriptionY = linkPreviewY - AndroidUtilities.dp(3);
            canvas.save();
            canvas.translate(linkX + (hasInvoicePreview ? 0 : AndroidUtilities.dp(10)) + descriptionX, descriptionY);
            if (pressedLink != null && linkBlockNum == -10) {
                for (int b = 0; b < urlPath.size(); b++) {
                    canvas.drawPath(urlPath.get(b), Theme.chat_urlPaint);
                }
            }
            if (delegate.getTextSelectionHelper() != null && getDelegate().getTextSelectionHelper().isSelected(currentMessageObject)) {
                delegate.getTextSelectionHelper().drawDescription(currentMessageObject.isOutOwner(), descriptionLayout, canvas);
            }
            descriptionLayout.draw(canvas);
            canvas.restore();
            linkPreviewY += descriptionLayout.getLineBottom(descriptionLayout.getLineCount() - 1);
        }

        if (drawPhotoImage && (!drawInstantView || drawInstantViewType == 9 || drawInstantViewType == 11)) {
            if (linkPreviewY != startY) {
                linkPreviewY += AndroidUtilities.dp(2);
            }

            if (isSmallImage) {
                photoImage.setImageCoords(linkX + backgroundWidth - AndroidUtilities.dp(81), smallImageStartY, photoImage.getImageWidth(), photoImage.getImageHeight());
            } else {
                photoImage.setImageCoords(linkX + (hasInvoicePreview ? -AndroidUtilities.dp(6.3f) : AndroidUtilities.dp(10)), linkPreviewY, photoImage.getImageWidth(), photoImage.getImageHeight());
                if (drawImageButton) {
                    int size = AndroidUtilities.dp(48);
                    buttonX = this.buttonX = (int) (photoImage.getImageX() + (photoImage.getImageWidth() - size) / 2.0f);
                    buttonY = this.buttonY = (int) (photoImage.getImageY() + (photoImage.getImageHeight() - size) / 2.0f);
                    radialProgress.setProgressRect((int) buttonX, (int) buttonY, (int) buttonX + size, (int) buttonY + size);
                }
            }
            if (isRoundVideo && MediaController.getInstance().isPlayingMessage(currentMessageObject) && MediaController.getInstance().isVideoDrawingReady() && canvas.isHardwareAccelerated()) {
                imageDrawn = true;
                drawTime = true;
            } else {
                if (delegate == null || delegate.getPinchToZoomHelper() == null || !delegate.getPinchToZoomHelper().isInOverlayModeFor(this)) {
                    if (alpha != 1f) {
                        photoImage.setAlpha(alpha);
                        imageDrawn = photoImage.draw(canvas);
                        photoImage.setAlpha(1f);
                    } else {
                        imageDrawn = photoImage.draw(canvas);
                    }
                }
            }
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
            videoButtonX = (int) (photoImage.getImageX() + AndroidUtilities.dp(8));
            videoButtonY = (int) (photoImage.getImageY() + AndroidUtilities.dp(8));
            videoRadialProgress.setProgressRect(videoButtonX, videoButtonY, videoButtonX + AndroidUtilities.dp(24), videoButtonY + AndroidUtilities.dp(24));
        }
        Paint timeBackgroundPaint = getThemedPaint(Theme.key_paint_chatTimeBackground);
        if (photosCountLayout != null && photoImage.getVisible()) {
            int x = (int) (photoImage.getImageX() + photoImage.getImageWidth() - AndroidUtilities.dp(8) - photosCountWidth);
            int y = (int) (photoImage.getImageY() + photoImage.getImageHeight() - AndroidUtilities.dp(19));
            rect.set(x - AndroidUtilities.dp(4), y - AndroidUtilities.dp(1.5f), x + photosCountWidth + AndroidUtilities.dp(4), y + AndroidUtilities.dp(14.5f));
            int oldAlpha = timeBackgroundPaint.getAlpha();
            timeBackgroundPaint.setAlpha((int) (oldAlpha * controlsAlpha));
            Theme.chat_durationPaint.setAlpha((int) (255 * controlsAlpha));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), timeBackgroundPaint);
            timeBackgroundPaint.setAlpha(oldAlpha);
            canvas.save();
            canvas.translate(x, y);
            photosCountLayout.draw(canvas);
            canvas.restore();
            Theme.chat_durationPaint.setAlpha(255);
        }
        if (videoInfoLayout != null && (!drawPhotoImage || photoImage.getVisible()) && imageBackgroundSideColor == 0) {
            int x;
            int y;
            if (hasGamePreview || hasInvoicePreview || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER) {
                if (drawPhotoImage) {
                    x = (int) (photoImage.getImageX() + AndroidUtilities.dp(8.5f));
                    y = (int) (photoImage.getImageY() + AndroidUtilities.dp(6));
                    int height = AndroidUtilities.dp(documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER ? 14.5f : 16.5f);
                    rect.set(x - AndroidUtilities.dp(4), y - AndroidUtilities.dp(1.5f), x + durationWidth + AndroidUtilities.dp(4), y + height);
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), timeBackgroundPaint);
                } else {
                    x = linkX;
                    y = linkPreviewY;
                }
            } else {
                x = (int) (photoImage.getImageX() + photoImage.getImageWidth() - AndroidUtilities.dp(8) - durationWidth);
                y = (int) (photoImage.getImageY() + photoImage.getImageHeight() - AndroidUtilities.dp(19));
                rect.set(x - AndroidUtilities.dp(4), y - AndroidUtilities.dp(1.5f), x + durationWidth + AndroidUtilities.dp(4), y + AndroidUtilities.dp(14.5f));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(4), AndroidUtilities.dp(4), getThemedPaint(Theme.key_paint_chatTimeBackground));
            }

            canvas.save();
            canvas.translate(x, y);
            if (hasInvoicePreview) {
                if (drawPhotoImage) {
                    Theme.chat_shipmentPaint.setColor(getThemedColor(Theme.key_chat_previewGameText));
                } else {
                    if (currentMessageObject.isOutOwner()) {
                        Theme.chat_shipmentPaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
                    } else {
                        Theme.chat_shipmentPaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
                    }
                }
            }
            videoInfoLayout.draw(canvas);
            canvas.restore();
        }

        if (drawInstantView) {
            Drawable instantDrawable;
            int instantY = startY + linkPreviewHeight + AndroidUtilities.dp(10);
            Paint backPaint = Theme.chat_instantViewRectPaint;
            if (currentMessageObject.isOutOwner()) {
                instantDrawable = getThemedDrawable(Theme.key_drawable_msgOutInstant);
                Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_outPreviewInstantText));
                backPaint.setColor(getThemedColor(Theme.key_chat_outPreviewInstantText));
            } else {
                instantDrawable = Theme.chat_msgInInstantDrawable;
                Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
                backPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
            }

            instantButtonRect.set(linkX, instantY, linkX + instantWidth, instantY + AndroidUtilities.dp(36));
            if (Build.VERSION.SDK_INT >= 21) {
                selectorDrawableMaskType[0] = 0;
                selectorDrawable[0].setBounds(linkX, instantY, linkX + instantWidth, instantY + AndroidUtilities.dp(36));
                selectorDrawable[0].draw(canvas);
            }
            canvas.drawRoundRect(instantButtonRect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), backPaint);
            if (drawInstantViewType == 0) {
                setDrawableBounds(instantDrawable, instantTextLeftX + instantTextX + linkX - AndroidUtilities.dp(15), instantY + AndroidUtilities.dp(11.5f), AndroidUtilities.dp(9), AndroidUtilities.dp(13));
                instantDrawable.draw(canvas);
            }
            if (instantViewLayout != null) {
                canvas.save();
                canvas.translate(linkX + instantTextX, instantY + AndroidUtilities.dp(10.5f));
                instantViewLayout.draw(canvas);
                canvas.restore();
            }
        }
    }

    private boolean shouldDrawMenuDrawable() {
        return currentMessagesGroup == null || (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) != 0;
    }

    private void drawBotButtons(Canvas canvas, ArrayList<BotButton> botButtons, float alpha) {
        int addX;
        if (currentMessageObject.isOutOwner()) {
            addX = getMeasuredWidth() - widthForButtons - AndroidUtilities.dp(10);
        } else {
            addX = backgroundDrawableLeft + AndroidUtilities.dp(mediaBackground || drawPinnedBottom ? 1 : 7);
        }
        float top = layoutHeight - AndroidUtilities.dp(2) + transitionParams.deltaBottom;
        float height = 0;
        for (int a = 0; a < botButtons.size(); a++) {
            BotButton button = botButtons.get(a);
            int bottom = button.y + button.height;
            if (bottom > height) {
                height = bottom;
            }
        }
        rect.set(0, top, getMeasuredWidth(), top + height);
        if (alpha != 1f) {
            canvas.saveLayerAlpha(rect, (int) (255 * alpha), Canvas.ALL_SAVE_FLAG);
        } else {
            canvas.save();
        }

        for (int a = 0; a < botButtons.size(); a++) {
            BotButton button = botButtons.get(a);
            float y = button.y + layoutHeight - AndroidUtilities.dp(2) + transitionParams.deltaBottom;

            rect.set(button.x + addX, y, button.x + addX + button.width, y + button.height);
            applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
            canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), getThemedPaint(a == pressedBotButton ? Theme.key_paint_chatActionBackgroundSelected : Theme.key_paint_chatActionBackground));
            if (hasGradientService()) {
                canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Theme.chat_actionBackgroundGradientDarkenPaint);
            }

            canvas.save();
            canvas.translate(button.x + addX + AndroidUtilities.dp(5), y + (AndroidUtilities.dp(44) - button.title.getLineBottom(button.title.getLineCount() - 1)) / 2);
            button.title.draw(canvas);
            canvas.restore();
            if (button.button instanceof TLRPC.TL_keyboardButtonUrl) {
                Drawable drawable = getThemedDrawable(Theme.key_drawable_botLink);
                int x = button.x + button.width - AndroidUtilities.dp(3) - drawable.getIntrinsicWidth() + addX;
                setDrawableBounds(drawable, x, y + AndroidUtilities.dp(3));
                drawable.draw(canvas);
            } else if (button.button instanceof TLRPC.TL_keyboardButtonSwitchInline) {
                Drawable drawable = getThemedDrawable(Theme.key_drawable_botInline);
                int x = button.x + button.width - AndroidUtilities.dp(3) - drawable.getIntrinsicWidth() + addX;
                setDrawableBounds(drawable, x, y + AndroidUtilities.dp(3));
                drawable.draw(canvas);
            } else if (button.button instanceof TLRPC.TL_keyboardButtonCallback || button.button instanceof TLRPC.TL_keyboardButtonRequestGeoLocation || button.button instanceof TLRPC.TL_keyboardButtonGame || button.button instanceof TLRPC.TL_keyboardButtonBuy || button.button instanceof TLRPC.TL_keyboardButtonUrlAuth) {
                if (button.button instanceof TLRPC.TL_keyboardButtonBuy) {
                    int x = button.x + button.width - AndroidUtilities.dp(5) - Theme.chat_botCardDrawalbe.getIntrinsicWidth() + addX;
                    setDrawableBounds(Theme.chat_botCardDrawalbe, x, y + AndroidUtilities.dp(4));
                    Theme.chat_botCardDrawalbe.draw(canvas);
                }
                boolean drawProgress = (button.button instanceof TLRPC.TL_keyboardButtonCallback || button.button instanceof TLRPC.TL_keyboardButtonGame || button.button instanceof TLRPC.TL_keyboardButtonBuy || button.button instanceof TLRPC.TL_keyboardButtonUrlAuth) && SendMessagesHelper.getInstance(currentAccount).isSendingCallback(currentMessageObject, button.button) ||
                        button.button instanceof TLRPC.TL_keyboardButtonRequestGeoLocation && SendMessagesHelper.getInstance(currentAccount).isSendingCurrentLocation(currentMessageObject, button.button);
                if (drawProgress || button.progressAlpha != 0) {
                    Theme.chat_botProgressPaint.setAlpha(Math.min(255, (int) (button.progressAlpha * 255)));
                    int x = button.x + button.width - AndroidUtilities.dp(9 + 3) + addX;
                    if (button.button instanceof TLRPC.TL_keyboardButtonBuy) {
                        y += AndroidUtilities.dp(26);
                    }
                    rect.set(x, y + AndroidUtilities.dp(4), x + AndroidUtilities.dp(8), y + AndroidUtilities.dp(8 + 4));
                    canvas.drawArc(rect, button.angle, 220, false, Theme.chat_botProgressPaint);
                    invalidate();
                    long newTime = System.currentTimeMillis();
                    if (Math.abs(button.lastUpdateTime - System.currentTimeMillis()) < 1000) {
                        long delta = (newTime - button.lastUpdateTime);
                        float dt = 360 * delta / 2000.0f;
                        button.angle += dt;
                        button.angle -= 360 * (button.angle / 360);
                        if (drawProgress) {
                            if (button.progressAlpha < 1.0f) {
                                button.progressAlpha += delta / 200.0f;
                                if (button.progressAlpha > 1.0f) {
                                    button.progressAlpha = 1.0f;
                                }
                            }
                        } else {
                            if (button.progressAlpha > 0.0f) {
                                button.progressAlpha -= delta / 200.0f;
                                if (button.progressAlpha < 0.0f) {
                                    button.progressAlpha = 0.0f;
                                }
                            }
                        }
                    }
                    button.lastUpdateTime = newTime;
                }
            }
        }
        canvas.restore();
    }

    @SuppressLint("Range")
    public void drawMessageText(Canvas canvas, ArrayList<MessageObject.TextLayoutBlock> textLayoutBlocks, boolean origin, float alpha, boolean drawOnlyText) {
        if (textLayoutBlocks == null || textLayoutBlocks.isEmpty() || alpha == 0) {
            return;
        }
        int firstVisibleBlockNum;
        int lastVisibleBlockNum;
        if (origin) {
            if (fullyDraw) {
                this.firstVisibleBlockNum = 0;
                this.lastVisibleBlockNum = textLayoutBlocks.size();
            }
            firstVisibleBlockNum = this.firstVisibleBlockNum;
            lastVisibleBlockNum = this.lastVisibleBlockNum;
        } else {
            firstVisibleBlockNum = 0;
            lastVisibleBlockNum = textLayoutBlocks.size();
        }

        float textY = this.textY;
        if (transitionParams.animateText) {
            textY = transitionParams.animateFromTextY * (1f - transitionParams.animateChangeProgress) + this.textY * transitionParams.animateChangeProgress;
        }
        if (firstVisibleBlockNum >= 0) {
            int restore = Integer.MIN_VALUE;
            int oldAlpha = 0;
            int oldLinkAlpha = 0;
            boolean needRestoreColor = false;
            if (alpha != 1.0f) {
                if (drawOnlyText) {
                    needRestoreColor = true;
                    oldAlpha = Theme.chat_msgTextPaint.getAlpha();
                    oldLinkAlpha = Color.alpha(Theme.chat_msgTextPaint.linkColor);
                    Theme.chat_msgTextPaint.setAlpha((int) (oldAlpha * alpha));
                    Theme.chat_msgTextPaint.linkColor = ColorUtils.setAlphaComponent(Theme.chat_msgTextPaint.linkColor, (int) (oldLinkAlpha * alpha));
                } else {
                    if (currentBackgroundDrawable != null) {
                        int top = currentBackgroundDrawable.getBounds().top;
                        int bottom = currentBackgroundDrawable.getBounds().bottom;

                        if (getY() < 0) {
                            top = (int) -getY();
                        }
                        if (getY() + getMeasuredHeight() > parentHeight) {
                            bottom = (int) (parentHeight - getY());
                        }
                        rect.set(
                                getCurrentBackgroundLeft(), top,
                                currentBackgroundDrawable.getBounds().right, bottom
                        );
                    } else {
                        rect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    }
                    restore = canvas.saveLayerAlpha(rect, (int) (alpha * 255), Canvas.ALL_SAVE_FLAG);
                }
            }
            for (int a = firstVisibleBlockNum; a <= lastVisibleBlockNum; a++) {
                if (a >= textLayoutBlocks.size()) {
                    break;
                }
                MessageObject.TextLayoutBlock block = textLayoutBlocks.get(a);
                canvas.save();
                canvas.translate(textX - (block.isRtl() ? (int) Math.ceil(currentMessageObject.textXOffset) : 0), textY + block.textYOffset + transitionYOffsetForDrawables);
                if (pressedLink != null && a == linkBlockNum && !drawOnlyText) {
                    for (int b = 0; b < urlPath.size(); b++) {
                        canvas.drawPath(urlPath.get(b), Theme.chat_urlPaint);
                    }
                }
                if (a == linkSelectionBlockNum && !urlPathSelection.isEmpty() && !drawOnlyText) {
                    for (int b = 0; b < urlPathSelection.size(); b++) {
                        canvas.drawPath(urlPathSelection.get(b), Theme.chat_textSearchSelectionPaint);
                    }
                }

                if (delegate.getTextSelectionHelper() != null && transitionParams.animateChangeProgress == 1f && !drawOnlyText) {
                    delegate.getTextSelectionHelper().draw(currentMessageObject, block, canvas);
                }
                try {
                    Emoji.emojiDrawingYOffset = -transitionYOffsetForDrawables;
                    block.textLayout.draw(canvas);
                    Emoji.emojiDrawingYOffset = 0;
                } catch (Exception e) {
                    FileLog.e(e);
                }
                canvas.restore();
            }
            if (needRestoreColor) {
                Theme.chat_msgTextPaint.setAlpha(oldAlpha);
                Theme.chat_msgTextPaint.linkColor = ColorUtils.setAlphaComponent(Theme.chat_msgTextPaint.linkColor, oldLinkAlpha);
            }

            if (restore != Integer.MIN_VALUE) {
                canvas.restoreToCount(restore);
            }
        }
    }

    public void updateCaptionLayout() {
        if (currentMessageObject.type == MessageObject.TYPE_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || currentMessageObject.type == 8) {
            float x, y, h;
            if (transitionParams.imageChangeBoundsTransition) {
                x = transitionParams.animateToImageX;
                y = transitionParams.animateToImageY;
                h = transitionParams.animateToImageH;
            } else {
                x = photoImage.getImageX();
                y = photoImage.getImageY();
                h = photoImage.getImageHeight();
            }
            captionX = x + AndroidUtilities.dp(5) + captionOffsetX;
            captionY = y + h + AndroidUtilities.dp(6);
        } else if (hasOldCaptionPreview) {
            captionX = backgroundDrawableLeft + AndroidUtilities.dp(currentMessageObject.isOutOwner() ? 11 : 17) + captionOffsetX;
            captionY = totalHeight - captionHeight - AndroidUtilities.dp(drawPinnedTop ? 9 : 10) - linkPreviewHeight - AndroidUtilities.dp(17);
            if (drawCommentButton && drawSideButton != 3) {
                captionY -= AndroidUtilities.dp(shouldDrawTimeOnMedia() ? 41.3f : 43);
            }
        } else {
            captionX = backgroundDrawableLeft + AndroidUtilities.dp(currentMessageObject.isOutOwner() || mediaBackground || drawPinnedBottom ? 11 : 17) + captionOffsetX;
            captionY = totalHeight - captionHeight - AndroidUtilities.dp(drawPinnedTop ? 9 : 10);
            if (drawCommentButton && drawSideButton != 3) {
                captionY -= AndroidUtilities.dp(shouldDrawTimeOnMedia() ? 41.3f : 43);
            }
        }
        captionX += getExtraTextX();
    }

    private boolean textIsSelectionMode() {
        if (getCurrentMessagesGroup() != null) {
            return false;
        }
        return delegate.getTextSelectionHelper() != null && delegate.getTextSelectionHelper().isSelected(currentMessageObject);
    }

    private int getMiniIconForCurrentState() {
        if (miniButtonState < 0) {
            return MediaActionDrawable.ICON_NONE;
        }
        if (miniButtonState == 0) {
            return MediaActionDrawable.ICON_DOWNLOAD;
        } else {
            return MediaActionDrawable.ICON_CANCEL;
        }
    }

    private int getIconForCurrentState() {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.isOutOwner()) {
                radialProgress.setColors(Theme.key_chat_outLoader, Theme.key_chat_outLoaderSelected, Theme.key_chat_outMediaIcon, Theme.key_chat_outMediaIconSelected);
            } else {
                radialProgress.setColors(Theme.key_chat_inLoader, Theme.key_chat_inLoaderSelected, Theme.key_chat_inMediaIcon, Theme.key_chat_inMediaIconSelected);
            }
            if (buttonState == 1) {
                return MediaActionDrawable.ICON_PAUSE;
            } else if (buttonState == 2) {
                return MediaActionDrawable.ICON_DOWNLOAD;
            } else if (buttonState == 4) {
                return MediaActionDrawable.ICON_CANCEL;
            }
            return MediaActionDrawable.ICON_PLAY;
        } else {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT && !drawPhotoImage) {
                if (currentMessageObject.isOutOwner()) {
                    radialProgress.setColors(Theme.key_chat_outLoader, Theme.key_chat_outLoaderSelected, Theme.key_chat_outMediaIcon, Theme.key_chat_outMediaIconSelected);
                } else {
                    radialProgress.setColors(Theme.key_chat_inLoader, Theme.key_chat_inLoaderSelected, Theme.key_chat_inMediaIcon, Theme.key_chat_inMediaIconSelected);
                }
                if (buttonState == -1) {
                    return MediaActionDrawable.ICON_FILE;
                } else if (buttonState == 0) {
                    return MediaActionDrawable.ICON_DOWNLOAD;
                } else if (buttonState == 1) {
                    return MediaActionDrawable.ICON_CANCEL;
                }
            } else {
                radialProgress.setColors(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
                videoRadialProgress.setColors(Theme.key_chat_mediaLoaderPhoto, Theme.key_chat_mediaLoaderPhotoSelected, Theme.key_chat_mediaLoaderPhotoIcon, Theme.key_chat_mediaLoaderPhotoIconSelected);
                if (buttonState >= 0 && buttonState < 4) {
                    if (buttonState == 0) {
                        return MediaActionDrawable.ICON_DOWNLOAD;
                    } else if (buttonState == 1) {
                        return MediaActionDrawable.ICON_CANCEL;
                    } else if (buttonState == 2) {
                        return MediaActionDrawable.ICON_PLAY;
                    } else {
                        return autoPlayingMedia ? MediaActionDrawable.ICON_NONE : MediaActionDrawable.ICON_PLAY;
                    }
                } else if (buttonState == -1) {
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        return (drawPhotoImage && (currentPhotoObject != null || currentPhotoObjectThumb != null) && (photoImage.hasBitmapImage() || currentMessageObject.mediaExists || currentMessageObject.attachPathExists)) ? MediaActionDrawable.ICON_NONE : MediaActionDrawable.ICON_FILE;
                    } else if (currentMessageObject.needDrawBluredPreview()) {
                        if (currentMessageObject.messageOwner.destroyTime != 0) {
                            if (currentMessageObject.isOutOwner()) {
                                return MediaActionDrawable.ICON_SECRETCHECK;
                            } else {
                                return MediaActionDrawable.ICON_EMPTY_NOPROGRESS;
                            }
                        } else {
                            return MediaActionDrawable.ICON_FIRE;
                        }
                    } else if (hasEmbed) {
                        return MediaActionDrawable.ICON_PLAY;
                    }
                }
            }
        }
        return MediaActionDrawable.ICON_NONE;
    }

    private int getMaxNameWidth() {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER || currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
            int maxWidth;
            if (AndroidUtilities.isTablet()) {
                if (isChat && !isThreadPost && !currentMessageObject.isOutOwner() && currentMessageObject.needDrawAvatar()) {
                    maxWidth = AndroidUtilities.getMinTabletSide() - AndroidUtilities.dp(42);
                } else {
                    maxWidth = AndroidUtilities.getMinTabletSide();
                }
            } else {
                if (isChat && !isThreadPost && !currentMessageObject.isOutOwner() && currentMessageObject.needDrawAvatar()) {
                    maxWidth = Math.min(getParentWidth(), AndroidUtilities.displaySize.y) - AndroidUtilities.dp(42);
                } else {
                    maxWidth = Math.min(getParentWidth(), AndroidUtilities.displaySize.y);
                }
            }
            if (isPlayingRound) {
                int backgroundWidthLocal = backgroundWidth - (AndroidUtilities.roundPlayingMessageSize - AndroidUtilities.roundMessageSize);
                return maxWidth - backgroundWidthLocal - AndroidUtilities.dp(57);
            }
            return maxWidth - backgroundWidth - AndroidUtilities.dp(57);
        }
        if (currentMessagesGroup != null && !currentMessagesGroup.isDocuments) {
            int dWidth;
            if (AndroidUtilities.isTablet()) {
                dWidth = AndroidUtilities.getMinTabletSide();
            } else {
                dWidth = getParentWidth();
            }
            int firstLineWidth = 0;
            for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                if (position.minY == 0) {
                    firstLineWidth += Math.ceil((position.pw + position.leftSpanOffset) / 1000.0f * dWidth);
                } else {
                    break;
                }
            }
            return firstLineWidth - AndroidUtilities.dp(31 + (isAvatarVisible ? 48 : 0));
        } else {
            return backgroundWidth - AndroidUtilities.dp(mediaBackground ? 22 : 31);
        }
    }

    public void updateButtonState(boolean ifSame, boolean animated, boolean fromSet) {
        if (currentMessageObject == null) {
            return;
        }
        if (animated && (PhotoViewer.isShowingImage(currentMessageObject) || !attachedToWindow)) {
            animated = false;
        }
        drawRadialCheckBackground = false;
        String fileName = null;
        boolean fileExists = false;
        if (currentMessageObject.type == MessageObject.TYPE_PHOTO) {
            if (currentPhotoObject == null) {
                radialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, animated);
                return;
            }
            fileName = FileLoader.getAttachFileName(currentPhotoObject);
            fileExists = currentMessageObject.mediaExists;
        } else if (currentMessageObject.type == 8 || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER || currentMessageObject.type == 9 || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.useCustomPhoto) {
                buttonState = 1;
                radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                return;
            }
            if (currentMessageObject.attachPathExists && !TextUtils.isEmpty(currentMessageObject.messageOwner.attachPath)) {
                fileName = currentMessageObject.messageOwner.attachPath;
                fileExists = true;
            } else if (!currentMessageObject.isSendError() || documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                fileName = currentMessageObject.getFileName();
                fileExists = currentMessageObject.mediaExists;
            }
        } else if (documentAttachType != DOCUMENT_ATTACH_TYPE_NONE) {
            fileName = FileLoader.getAttachFileName(documentAttach);
            fileExists = currentMessageObject.mediaExists;
        } else if (currentPhotoObject != null) {
            fileName = FileLoader.getAttachFileName(currentPhotoObject);
            fileExists = currentMessageObject.mediaExists;
        }

        boolean autoDownload;
        if (documentAttach != null && documentAttach.dc_id == Integer.MIN_VALUE) {
            autoDownload = false;
        } else {
            autoDownload = DownloadController.getInstance(currentAccount).canDownloadMedia(currentMessageObject);
        }
        canStreamVideo = (currentMessageObject.isSent() || currentMessageObject.isForwarded()) && (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && autoDownload) && currentMessageObject.canStreamVideo() && !currentMessageObject.needDrawBluredPreview();
        if (SharedConfig.streamMedia && (int) currentMessageObject.getDialogId() != 0 && !currentMessageObject.isSecretMedia() &&
                (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC ||
                        canStreamVideo && currentPosition != null && ((currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0 || (currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) == 0))) {
            hasMiniProgress = fileExists ? 1 : 2;
            fileExists = true;
        }
        if (currentMessageObject.isSendError() || TextUtils.isEmpty(fileName) && (currentMessageObject.isAnyKindOfSticker() || !currentMessageObject.isSending() && !currentMessageObject.isEditing())) {
            radialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
            radialProgress.setMiniIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
            videoRadialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
            videoRadialProgress.setMiniIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
            return;
        }
        boolean fromBot = currentMessageObject.messageOwner.params != null && currentMessageObject.messageOwner.params.containsKey("query_id");

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (currentMessageObject.isOut() && (currentMessageObject.isSending() && !currentMessageObject.isForwarded() || currentMessageObject.isEditing() && currentMessageObject.isEditingMedia()) || currentMessageObject.isSendError() && fromBot) {
                if (!TextUtils.isEmpty(currentMessageObject.messageOwner.attachPath)) {
                    DownloadController.getInstance(currentAccount).addLoadingFileObserver(currentMessageObject.messageOwner.attachPath, currentMessageObject, this);
                    wasSending = true;
                    buttonState = 4;
                    boolean sending = SendMessagesHelper.getInstance(currentAccount).isSendingMessage(currentMessageObject.getId());
                    if (currentPosition != null && sending && buttonState == 4) {
                        drawRadialCheckBackground = true;
                        getIconForCurrentState();
                        radialProgress.setIcon(MediaActionDrawable.ICON_CHECK, ifSame, animated);
                    } else {
                        radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                    }
                    radialProgress.setMiniIcon(MediaActionDrawable.ICON_NONE, ifSame, animated);
                    if (!fromBot) {
                        long[] progress = ImageLoader.getInstance().getFileProgressSizes(currentMessageObject.messageOwner.attachPath);
                        float loadingProgress = 0;
                        if (progress == null && sending) {
                            loadingProgress = 1.0f;
                        } else if (progress != null) {
                            loadingProgress = DownloadController.getProgress(progress);
                        }
                        radialProgress.setProgress(loadingProgress, false);
                    } else {
                        radialProgress.setProgress(0, false);
                    }
                } else {
                    buttonState = -1;
                    getIconForCurrentState();
                    radialProgress.setIcon(MediaActionDrawable.ICON_CANCEL_NOPROFRESS, ifSame, false);
                    radialProgress.setProgress(0, false);
                    radialProgress.setMiniIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
                }
            } else {
                if (hasMiniProgress != 0) {
                    radialProgress.setMiniProgressBackgroundColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outLoader : Theme.key_chat_inLoader));
                    boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
                    if (!playing || MediaController.getInstance().isMessagePaused()) {
                        buttonState = 0;
                    } else {
                        buttonState = 1;
                    }
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                    if (hasMiniProgress == 1) {
                        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                        miniButtonState = -1;
                    } else {
                        DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                        if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                            createLoadingProgressLayout(documentAttach);
                            miniButtonState = 0;
                        } else {
                            miniButtonState = 1;
                            long[] progress = ImageLoader.getInstance().getFileProgressSizes(fileName);
                            if (progress != null) {
                                radialProgress.setProgress(DownloadController.getProgress(progress), animated);
                                createLoadingProgressLayout(progress[0], progress[1]);
                            } else {
                                radialProgress.setProgress(0, animated);
                                createLoadingProgressLayout(0, currentMessageObject.getSize());
                            }
                        }
                    }
                    radialProgress.setMiniIcon(getMiniIconForCurrentState(), ifSame, animated);
                } else if (fileExists) {
                    DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                    boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
                    if (!playing || MediaController.getInstance().isMessagePaused()) {
                        buttonState = 0;
                    } else {
                        buttonState = 1;
                    }
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                } else {
                    DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                    if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                        buttonState = 2;
                    } else {
                        buttonState = 4;
                        long[] progress = ImageLoader.getInstance().getFileProgressSizes(fileName);
                        if (progress != null) {
                            radialProgress.setProgress(DownloadController.getProgress(progress), animated);
                            createLoadingProgressLayout(progress[0], progress[1]);
                        } else {
                            createLoadingProgressLayout(documentAttach);
                            radialProgress.setProgress(0, animated);
                        }
                    }
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                }
            }
            updatePlayingMessageProgress();
        } else if (currentMessageObject.type == 0 &&
                documentAttachType != DOCUMENT_ATTACH_TYPE_DOCUMENT &&
                documentAttachType != DOCUMENT_ATTACH_TYPE_GIF &&
                documentAttachType != DOCUMENT_ATTACH_TYPE_ROUND &&
                documentAttachType != DOCUMENT_ATTACH_TYPE_VIDEO &&
                documentAttachType != DOCUMENT_ATTACH_TYPE_WALLPAPER &&
                documentAttachType != DOCUMENT_ATTACH_TYPE_THEME) {
            if (currentPhotoObject == null || !drawImageButton) {
                return;
            }
            if (!fileExists) {
                DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                float setProgress = 0;
                if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                    if (!currentMessageObject.loadingCancelled && (documentAttachType == 0 && autoDownload || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && MessageObject.isGifDocument(documentAttach, currentMessageObject.hasValidGroupId()) && autoDownload)) {
                        buttonState = 1;
                    } else {
                        buttonState = 0;
                    }
                } else {
                    buttonState = 1;
                    long[] progress = ImageLoader.getInstance().getFileProgressSizes(fileName);
                    setProgress = progress != null ? DownloadController.getProgress(progress) : 0;
                    if (progress != null && progress[0] == progress[1]) {
                        createLoadingProgressLayout(progress[0], progress[1]);
                    } else {
                        if (currentMessageObject.getDocument() != null) {
                            createLoadingProgressLayout(currentMessageObject.loadedFileSize, currentMessageObject.getSize());
                        }
                    }
                }
                radialProgress.setProgress(setProgress, false);
            } else {
                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && !photoImage.isAllowStartAnimation()) {
                    buttonState = 2;
                } else {
                    buttonState = -1;
                }
            }
            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
            invalidate();
        } else {
            if (currentMessageObject.isOut() && (currentMessageObject.isSending() && !currentMessageObject.isForwarded() || currentMessageObject.isEditing() && currentMessageObject.isEditingMedia())) {
                if (!TextUtils.isEmpty(currentMessageObject.messageOwner.attachPath)) {
                    DownloadController.getInstance(currentAccount).addLoadingFileObserver(currentMessageObject.messageOwner.attachPath, currentMessageObject, this);
                    wasSending = true;
                    boolean needProgress = currentMessageObject.messageOwner.attachPath == null || !currentMessageObject.messageOwner.attachPath.startsWith("http");
                    HashMap<String, String> params = currentMessageObject.messageOwner.params;
                    if (currentMessageObject.messageOwner.message != null && params != null && (params.containsKey("url") || params.containsKey("bot"))) {
                        needProgress = false;
                        buttonState = -1;
                    } else {
                        buttonState = 1;
                    }
                    boolean sending = SendMessagesHelper.getInstance(currentAccount).isSendingMessage(currentMessageObject.getId());
                    if (currentPosition != null && sending && buttonState == 1) {
                        drawRadialCheckBackground = true;
                        getIconForCurrentState();
                        radialProgress.setIcon(MediaActionDrawable.ICON_CHECK, ifSame, animated);
                    } else {
                        radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                    }
                    if (needProgress) {
                        long[] progress = ImageLoader.getInstance().getFileProgressSizes(currentMessageObject.messageOwner.attachPath);
                        float loadingProgress = 0;
                        if (progress == null && sending) {
                            loadingProgress = 1.0f;
                        } else if (progress != null) {
                            loadingProgress = DownloadController.getProgress(progress);
                            createLoadingProgressLayout(progress[0], progress[1]);
                        }
                        radialProgress.setProgress(loadingProgress, false);
                    } else {
                        radialProgress.setProgress(0, false);
                    }
                    invalidate();
                } else {
                    getIconForCurrentState();
                    if (currentMessageObject.isSticker() || currentMessageObject.isAnimatedSticker() || currentMessageObject.isLocation() || currentMessageObject.isGif()) {
                        buttonState = -1;
                        radialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
                    } else {
                        buttonState = 1;
                        radialProgress.setIcon(MediaActionDrawable.ICON_CANCEL_NOPROFRESS, ifSame, false);
                    }
                    radialProgress.setProgress(0, false);
                }
                videoRadialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
            } else {
                if (wasSending && !TextUtils.isEmpty(currentMessageObject.messageOwner.attachPath)) {
                    DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                }
                boolean isLoadingVideo = false;
                if ((documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) && autoPlayingMedia) {
                    isLoadingVideo = FileLoader.getInstance(currentAccount).isLoadingVideo(documentAttach, MediaController.getInstance().isPlayingMessage(currentMessageObject));
                    AnimatedFileDrawable animation = photoImage.getAnimation();
                    if (animation != null) {
                        if (currentMessageObject.hadAnimationNotReadyLoading) {
                            if (animation.hasBitmap()) {
                                currentMessageObject.hadAnimationNotReadyLoading = false;
                            }
                        } else {
                            currentMessageObject.hadAnimationNotReadyLoading = isLoadingVideo && !animation.hasBitmap();
                        }
                    } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && !fileExists) {
                        currentMessageObject.hadAnimationNotReadyLoading = true;
                    }
                }
                if (hasMiniProgress != 0) {
                    radialProgress.setMiniProgressBackgroundColor(getThemedColor(Theme.key_chat_inLoaderPhoto));
                    buttonState = 3;
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                    if (hasMiniProgress == 1) {
                        DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                        miniButtonState = -1;
                    } else {
                        DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                        if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                            miniButtonState = 0;
                        } else {
                            miniButtonState = 1;
                            long[] progress = ImageLoader.getInstance().getFileProgressSizes(fileName);
                            if (progress != null) {
                                createLoadingProgressLayout(progress[0], progress[1]);
                                radialProgress.setProgress(DownloadController.getProgress(progress), animated);
                            } else {
                                createLoadingProgressLayout(documentAttach);
                                radialProgress.setProgress(0, animated);
                            }
                        }
                    }
                    radialProgress.setMiniIcon(getMiniIconForCurrentState(), ifSame, animated);
                } else if (fileExists || (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) && autoPlayingMedia && !currentMessageObject.hadAnimationNotReadyLoading && !isLoadingVideo) {
                    DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                    if (drawVideoImageButton && animated) {
                        if (animatingDrawVideoImageButton != 1 && animatingDrawVideoImageButtonProgress > 0) {
                            if (animatingDrawVideoImageButton == 0) {
                                animatingDrawVideoImageButtonProgress = 1.0f;
                            }
                            animatingDrawVideoImageButton = 1;
                        }
                    } else if (animatingDrawVideoImageButton == 0) {
                        animatingDrawVideoImageButton = 1;
                    }
                    drawVideoImageButton = false;
                    drawVideoSize = false;
                    if (currentMessageObject.needDrawBluredPreview()) {
                        buttonState = -1;
                    } else {
                        if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && currentMessageObject.gifState == 1) {
                            if (photoImage.isAnimationRunning()) {
                                currentMessageObject.gifState = 0;
                                buttonState = -1;
                            } else {
                                buttonState = 2;
                            }
                        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO && !hasEmbed) {
                            buttonState = 3;
                        } else {
                            buttonState = -1;
                        }
                    }
                    videoRadialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, animatingDrawVideoImageButton != 0);
                    radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                    if (!fromSet && photoNotSet) {
                        setMessageObject(currentMessageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
                    }
                    invalidate();
                } else {
                    drawVideoSize = documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF;
                    if ((documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) && canStreamVideo && !drawVideoImageButton && animated) {
                        if (animatingDrawVideoImageButton != 2 && animatingDrawVideoImageButtonProgress < 1.0f) {
                            if (animatingDrawVideoImageButton == 0) {
                                animatingDrawVideoImageButtonProgress = 0.0f;
                            }
                            animatingDrawVideoImageButton = 2;
                        }
                    } else if (animatingDrawVideoImageButton == 0) {
                        animatingDrawVideoImageButtonProgress = 1.0f;
                    }
                    DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                    boolean progressVisible = false;
                    if (!FileLoader.getInstance(currentAccount).isLoadingFile(fileName)) {
                        if (!currentMessageObject.loadingCancelled && autoDownload) {
                            buttonState = 1;
                        } else {
                            buttonState = 0;
                        }
                        boolean hasDocLayout = currentMessageObject.type == MessageObject.TYPE_VIDEO || currentMessageObject.type == 8 || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO;
                        boolean fullWidth = true;
                        if (currentPosition != null) {
                            int mask = MessageObject.POSITION_FLAG_LEFT | MessageObject.POSITION_FLAG_RIGHT;
                            fullWidth = (currentPosition.flags & mask) == mask;
                        }
                        if ((documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && autoDownload) && canStreamVideo && hasDocLayout && fullWidth) {
                            drawVideoImageButton = true;
                            getIconForCurrentState();
                            radialProgress.setIcon(autoPlayingMedia ? MediaActionDrawable.ICON_NONE : MediaActionDrawable.ICON_PLAY, ifSame, animated);
                            videoRadialProgress.setIcon(MediaActionDrawable.ICON_DOWNLOAD, ifSame, animated);
                        } else {
                            drawVideoImageButton = false;
                            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                            videoRadialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
                            if (!drawVideoSize && animatingDrawVideoImageButton == 0) {
                                animatingDrawVideoImageButtonProgress = 0.0f;
                            }
                        }
                    } else {
                        buttonState = 1;
                        long[] progress = ImageLoader.getInstance().getFileProgressSizes(fileName);
                        if (progress != null) {
                            createLoadingProgressLayout(progress[0], progress[1]);
                        } else {
                            createLoadingProgressLayout(documentAttach);
                        }
                        boolean hasDocLayout = currentMessageObject.type == MessageObject.TYPE_VIDEO || currentMessageObject.type == 8 || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO;
                        boolean fullWidth = true;
                        if (currentPosition != null) {
                            int mask = MessageObject.POSITION_FLAG_LEFT | MessageObject.POSITION_FLAG_RIGHT;
                            fullWidth = (currentPosition.flags & mask) == mask;
                        }
                        if ((documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || MessageObject.isGifDocument(documentAttach, currentMessageObject.hasValidGroupId()) && autoDownload) && canStreamVideo && hasDocLayout && fullWidth) {
                            drawVideoImageButton = true;
                            getIconForCurrentState();
                            radialProgress.setIcon(autoPlayingMedia || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF ? MediaActionDrawable.ICON_NONE : MediaActionDrawable.ICON_PLAY, ifSame, animated);
                            videoRadialProgress.setProgress(progress != null ? DownloadController.getProgress(progress) : 0, animated);
                            videoRadialProgress.setIcon(MediaActionDrawable.ICON_CANCEL_FILL, ifSame, animated);
                        } else {
                            drawVideoImageButton = false;
                            radialProgress.setProgress(progress != null ? DownloadController.getProgress(progress) : 0, animated);
                            radialProgress.setIcon(getIconForCurrentState(), ifSame, animated);
                            videoRadialProgress.setIcon(MediaActionDrawable.ICON_NONE, ifSame, false);
                            if (!drawVideoSize && animatingDrawVideoImageButton == 0) {
                                animatingDrawVideoImageButtonProgress = 0.0f;
                            }
                        }
                    }
                    invalidate();
                }
            }
        }
        if (hasMiniProgress == 0) {
            radialProgress.setMiniIcon(MediaActionDrawable.ICON_NONE, false, animated);
        }
    }

    private void didPressMiniButton(boolean animated) {
        if (miniButtonState == 0) {
            miniButtonState = 1;
            radialProgress.setProgress(0, false);
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, 0);
                currentMessageObject.loadingCancelled = false;
            } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                createLoadingProgressLayout(documentAttach);
                FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                currentMessageObject.loadingCancelled = false;
            }
            radialProgress.setMiniIcon(getMiniIconForCurrentState(), false, true);
            invalidate();
        } else if (miniButtonState == 1) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                    MediaController.getInstance().cleanupPlayer(true, true);
                }
            }
            miniButtonState = 0;
            currentMessageObject.loadingCancelled = true;
            FileLoader.getInstance(currentAccount).cancelLoadFile(documentAttach);
            radialProgress.setMiniIcon(getMiniIconForCurrentState(), false, true);
            invalidate();
        }
    }

    private void didPressButton(boolean animated, boolean video) {
        if (buttonState == 0 && (!drawVideoImageButton || video)) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (miniButtonState == 0) {
                    FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, 0);
                    currentMessageObject.loadingCancelled = false;
                }
                if (delegate.needPlayMessage(currentMessageObject)) {
                    if (hasMiniProgress == 2 && miniButtonState != 1) {
                        miniButtonState = 1;
                        radialProgress.setProgress(0, false);
                        radialProgress.setMiniIcon(getMiniIconForCurrentState(), false, true);
                    }
                    updatePlayingMessageProgress();
                    buttonState = 1;
                    radialProgress.setIcon(getIconForCurrentState(), false, true);
                    invalidate();
                }
            } else {
                if (video) {
                    videoRadialProgress.setProgress(0, false);
                } else {
                    radialProgress.setProgress(0, false);
                }
                TLRPC.PhotoSize thumb;
                String thumbFilter;
                if (currentPhotoObject != null && (photoImage.hasNotThumb() || currentPhotoObjectThumb == null)) {
                    thumb = currentPhotoObject;
                    thumbFilter = thumb instanceof TLRPC.TL_photoStrippedSize || "s".equals(thumb.type) ? currentPhotoFilterThumb : currentPhotoFilter;
                } else {
                    thumb = currentPhotoObjectThumb;
                    thumbFilter = currentPhotoFilterThumb;
                }
                if (currentMessageObject.type == MessageObject.TYPE_PHOTO) {
                    photoImage.setForceLoading(true);
                    photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, currentPhotoObject.size, null, currentMessageObject, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                } else if (currentMessageObject.type == 8) {
                    FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, 0);
                    if (currentMessageObject.loadedFileSize > 0) {
                        createLoadingProgressLayout(documentAttach);
                    }
                } else if (isRoundVideo) {
                    if (currentMessageObject.isSecretMedia()) {
                        FileLoader.getInstance(currentAccount).loadFile(currentMessageObject.getDocument(), currentMessageObject, 1, 1);
                    } else {
                        currentMessageObject.gifState = 2;
                        TLRPC.Document document = currentMessageObject.getDocument();
                        photoImage.setForceLoading(true);
                        photoImage.setImage(ImageLocation.getForDocument(document), null, ImageLocation.getForObject(thumb, document), thumbFilter, document.size, null, currentMessageObject, 0);
                    }
                } else if (currentMessageObject.type == 9) {
                    FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, 0);
                    if (currentMessageObject.loadedFileSize > 0) {
                        createLoadingProgressLayout(documentAttach);
                    }
                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                    FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, currentMessageObject.shouldEncryptPhotoOrVideo() ? 2 : 0);
                    if (currentMessageObject.loadedFileSize > 0) {
                        createLoadingProgressLayout(currentMessageObject.getDocument());
                    }
                } else if (currentMessageObject.type == 0 && documentAttachType != DOCUMENT_ATTACH_TYPE_NONE) {
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                        photoImage.setForceLoading(true);
                        photoImage.setImage(ImageLocation.getForDocument(documentAttach), null, ImageLocation.getForDocument(currentPhotoObject, documentAttach), currentPhotoFilterThumb, documentAttach.size, null, currentMessageObject, 0);
                        currentMessageObject.gifState = 2;
                        if (currentMessageObject.loadedFileSize > 0) {
                            createLoadingProgressLayout(currentMessageObject.getDocument());
                        }
                    } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 0, 0);
                    } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER) {
                        photoImage.setImage(ImageLocation.getForDocument(documentAttach), currentPhotoFilter, ImageLocation.getForDocument(currentPhotoObject, documentAttach), "b1", 0, "jpg", currentMessageObject, 1);
                    }
                } else {
                    photoImage.setForceLoading(true);
                    photoImage.setImage(ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, 0, null, currentMessageObject, 0);
                }
                currentMessageObject.loadingCancelled = false;
                buttonState = 1;
                if (video) {
                    videoRadialProgress.setIcon(MediaActionDrawable.ICON_CANCEL_FILL, false, animated);
                } else {
                    radialProgress.setIcon(getIconForCurrentState(), false, animated);
                }
                invalidate();
            }
        } else if (buttonState == 1 && (!drawVideoImageButton || video)) {
            photoImage.setForceLoading(false);
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                boolean result = MediaController.getInstance().pauseMessage(currentMessageObject);
                if (result) {
                    buttonState = 0;
                    radialProgress.setIcon(getIconForCurrentState(), false, animated);
                    invalidate();
                }
            } else {
                if (currentMessageObject.isOut() && !drawVideoImageButton && (currentMessageObject.isSending() || currentMessageObject.isEditing())) {
                    if (radialProgress.getIcon() != MediaActionDrawable.ICON_CHECK) {
                        delegate.didPressCancelSendButton(this);
                    }
                } else {
                    currentMessageObject.loadingCancelled = true;
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT || documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER) {
                        FileLoader.getInstance(currentAccount).cancelLoadFile(documentAttach);
                    } else if (currentMessageObject.type == 0 || currentMessageObject.type == MessageObject.TYPE_PHOTO || currentMessageObject.type == 8 || currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                        ImageLoader.getInstance().cancelForceLoadingForImageReceiver(photoImage);
                        photoImage.cancelLoadImage();
                    } else if (currentMessageObject.type == 9) {
                        FileLoader.getInstance(currentAccount).cancelLoadFile(currentMessageObject.getDocument());
                    }
                    buttonState = 0;
                    if (video) {
                        videoRadialProgress.setIcon(MediaActionDrawable.ICON_DOWNLOAD, false, animated);
                    } else {
                        radialProgress.setIcon(getIconForCurrentState(), false, animated);
                    }
                    invalidate();
                }
            }
        } else if (buttonState == 2) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                radialProgress.setProgress(0, false);
                FileLoader.getInstance(currentAccount).loadFile(documentAttach, currentMessageObject, 1, 0);
                currentMessageObject.loadingCancelled = false;
                buttonState = 4;
                radialProgress.setIcon(getIconForCurrentState(), true, animated);
                invalidate();
            } else {
                if (isRoundVideo) {
                    MessageObject playingMessage = MediaController.getInstance().getPlayingMessageObject();
                    if (playingMessage == null || !playingMessage.isRoundVideo()) {
                        photoImage.setAllowStartAnimation(true);
                        photoImage.startAnimation();
                    }
                } else {
                    photoImage.setAllowStartAnimation(true);
                    photoImage.startAnimation();
                }
                currentMessageObject.gifState = 0;
                buttonState = -1;
                radialProgress.setIcon(getIconForCurrentState(), false, animated);
            }
        } else if (buttonState == 3 || buttonState == 0) {
            if (hasMiniProgress == 2 && miniButtonState != 1) {
                miniButtonState = 1;
                radialProgress.setProgress(0, false);
                radialProgress.setMiniIcon(getMiniIconForCurrentState(), false, animated);
            }
            delegate.didPressImage(this, 0, 0);
        } else if (buttonState == 4) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (currentMessageObject.isOut() && (currentMessageObject.isSending() || currentMessageObject.isEditing()) || currentMessageObject.isSendError()) {
                    if (delegate != null && radialProgress.getIcon() != MediaActionDrawable.ICON_CHECK) {
                        delegate.didPressCancelSendButton(this);
                    }
                } else {
                    currentMessageObject.loadingCancelled = true;
                    FileLoader.getInstance(currentAccount).cancelLoadFile(documentAttach);
                    buttonState = 2;
                    radialProgress.setIcon(getIconForCurrentState(), false, animated);
                    invalidate();
                }
            }
        }
    }

    @Override
    public void onFailedDownload(String fileName, boolean canceled) {
        updateButtonState(true, documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC, false);
    }

    @Override
    public void onSuccessDownload(String fileName) {
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER && currentMessageObject.isDice()) {
            DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
            setCurrentDiceValue(true);
        } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            updateButtonState(false, true, false);
            updateWaveform();
        } else {
            if (drawVideoImageButton) {
                videoRadialProgress.setProgress(1, true);
            } else {
                radialProgress.setProgress(1, true);
            }
            if (!currentMessageObject.needDrawBluredPreview() && !autoPlayingMedia && documentAttach != null) {
                if (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
                    photoImage.setImage(ImageLocation.getForDocument(documentAttach), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoObject instanceof TLRPC.TL_photoStrippedSize || currentPhotoObject != null && "s".equals(currentPhotoObject.type) ? currentPhotoFilterThumb : currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, null, documentAttach.size, null, currentMessageObject, 0);
                    photoImage.setAllowStartAnimation(true);
                    photoImage.startAnimation();
                    autoPlayingMedia = true;
                } else if (SharedConfig.autoplayVideo && documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0 && (currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0)) {
                    animatingNoSound = 2;
                    photoImage.setImage(ImageLocation.getForDocument(documentAttach), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoObject instanceof TLRPC.TL_photoStrippedSize || currentPhotoObject != null && "s".equals(currentPhotoObject.type) ? currentPhotoFilterThumb : currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, null, documentAttach.size, null, currentMessageObject, 0);
                    if (!PhotoViewer.isPlayingMessage(currentMessageObject)) {
                        photoImage.setAllowStartAnimation(true);
                        photoImage.startAnimation();
                    } else {
                        photoImage.setAllowStartAnimation(false);
                    }
                    autoPlayingMedia = true;
                } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
                    photoImage.setImage(ImageLocation.getForDocument(documentAttach), ImageLoader.AUTOPLAY_FILTER, ImageLocation.getForObject(currentPhotoObject, photoParentObject), currentPhotoObject instanceof TLRPC.TL_photoStrippedSize || currentPhotoObject != null && "s".equals(currentPhotoObject.type) ? currentPhotoFilterThumb : currentPhotoFilter, ImageLocation.getForObject(currentPhotoObjectThumb, photoParentObject), currentPhotoFilterThumb, null, documentAttach.size, null, currentMessageObject, 0);
                    if (SharedConfig.autoplayGifs) {
                        photoImage.setAllowStartAnimation(true);
                        photoImage.startAnimation();
                    } else {
                        photoImage.setAllowStartAnimation(false);
                        photoImage.stopAnimation();
                    }
                    autoPlayingMedia = true;
                }
            }
            if (currentMessageObject.type == 0) {
                if (!autoPlayingMedia && documentAttachType == DOCUMENT_ATTACH_TYPE_GIF && currentMessageObject.gifState != 1) {
                    buttonState = 2;
                    didPressButton(true, false);
                } else if (!photoNotSet) {
                    updateButtonState(false, true, false);
                } else {
                    setMessageObject(currentMessageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
                }
            } else {
                if (!photoNotSet) {
                    updateButtonState(false, true, false);
                }
                if (photoNotSet) {
                    setMessageObject(currentMessageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
                }
            }
        }
    }

    @Override
    public void didSetImage(ImageReceiver imageReceiver, boolean set, boolean thumb, boolean memCache) {
        if (currentMessageObject != null && set) {
            if (setCurrentDiceValue(!memCache && !currentMessageObject.wasUnread)) {
                return;
            }
            if (!thumb && !currentMessageObject.mediaExists && !currentMessageObject.attachPathExists && (currentMessageObject.type == 0 && (documentAttachType == DOCUMENT_ATTACH_TYPE_WALLPAPER || documentAttachType == DOCUMENT_ATTACH_TYPE_NONE || documentAttachType == DOCUMENT_ATTACH_TYPE_STICKER) || currentMessageObject.type == MessageObject.TYPE_PHOTO)){
                currentMessageObject.mediaExists = true;
                updateButtonState(false, true, false);
            }
        }
    }

    public boolean setCurrentDiceValue(boolean instant) {
        if (currentMessageObject.isDice()) {
            Drawable drawable = photoImage.getDrawable();
            if (drawable instanceof RLottieDrawable) {
                RLottieDrawable lottieDrawable = (RLottieDrawable) drawable;
                String emoji = currentMessageObject.getDiceEmoji();
                TLRPC.TL_messages_stickerSet stickerSet = MediaDataController.getInstance(currentAccount).getStickerSetByEmojiOrName(emoji);
                if (stickerSet != null) {
                    int value = currentMessageObject.getDiceValue();
                    if ("\uD83C\uDFB0".equals(currentMessageObject.getDiceEmoji())) {
                        if (value >= 0 && value <= 64) {
                            ((SlotsDrawable) lottieDrawable).setDiceNumber(this, value, stickerSet, instant);
                            if (currentMessageObject.isOut()) {
                                lottieDrawable.setOnFinishCallback(diceFinishCallback, Integer.MAX_VALUE);
                            }
                            currentMessageObject.wasUnread = false;
                        }
                        if (!lottieDrawable.hasBaseDice() && stickerSet.documents.size() > 0) {
                            ((SlotsDrawable) lottieDrawable).setBaseDice(this, stickerSet);
                        }
                    } else {
                        if (!lottieDrawable.hasBaseDice() && stickerSet.documents.size() > 0) {
                            TLRPC.Document document = stickerSet.documents.get(0);
                            File path = FileLoader.getPathToAttach(document, true);
                            if (lottieDrawable.setBaseDice(path)) {
                                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                            } else {
                                String fileName = FileLoader.getAttachFileName(document);
                                DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                                FileLoader.getInstance(currentAccount).loadFile(document, stickerSet, 1, 1);
                            }
                        }
                        if (value >= 0 && value < stickerSet.documents.size()) {
                            if (!instant && currentMessageObject.isOut()) {
                                MessagesController.DiceFrameSuccess frameSuccess = MessagesController.getInstance(currentAccount).diceSuccess.get(emoji);
                                if (frameSuccess != null && frameSuccess.num == value) {
                                    lottieDrawable.setOnFinishCallback(diceFinishCallback, frameSuccess.frame);
                                }
                            }
                            TLRPC.Document document = stickerSet.documents.get(Math.max(value, 0));
                            File path = FileLoader.getPathToAttach(document, true);
                            if (lottieDrawable.setDiceNumber(path, instant)) {
                                DownloadController.getInstance(currentAccount).removeLoadingFileObserver(this);
                            } else {
                                String fileName = FileLoader.getAttachFileName(document);
                                DownloadController.getInstance(currentAccount).addLoadingFileObserver(fileName, currentMessageObject, this);
                                FileLoader.getInstance(currentAccount).loadFile(document, stickerSet, 1, 1);
                            }
                            currentMessageObject.wasUnread = false;
                        }
                    }
                } else {
                    MediaDataController.getInstance(currentAccount).loadStickersByEmojiOrName(emoji, true, true);
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public void onAnimationReady(ImageReceiver imageReceiver) {
        if (currentMessageObject != null && imageReceiver == photoImage && currentMessageObject.isAnimatedSticker()) {
            delegate.setShouldNotRepeatSticker(currentMessageObject);
        }
    }

    @Override
    public void onProgressDownload(String fileName, long downloadedSize, long totalSize) {
        float progress = totalSize == 0 ? 0 : Math.min(1f, downloadedSize / (float) totalSize);
        currentMessageObject.loadedFileSize = downloadedSize;
        createLoadingProgressLayout(downloadedSize, totalSize);
        if (drawVideoImageButton) {
            videoRadialProgress.setProgress(progress, true);
        } else {
            radialProgress.setProgress(progress, true);
        }
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
            if (hasMiniProgress != 0) {
                if (miniButtonState != 1) {
                    updateButtonState(false, false, false);
                }
            } else {
                if (buttonState != 4) {
                    updateButtonState(false, false, false);
                }
            }
        } else {
            if (hasMiniProgress != 0) {
                if (miniButtonState != 1) {
                    updateButtonState(false, false, false);
                }
            } else {
                if (buttonState != 1) {
                    updateButtonState(false, false, false);
                }
            }
        }
    }

    @Override
    public void onProgressUpload(String fileName, long uploadedSize, long totalSize, boolean isEncrypted) {
        float progress = totalSize == 0 ? 0 : Math.min(1f, uploadedSize / (float) totalSize);
        currentMessageObject.loadedFileSize = uploadedSize;
        radialProgress.setProgress(progress, true);
        if (uploadedSize == totalSize && currentPosition != null) {
            boolean sending = SendMessagesHelper.getInstance(currentAccount).isSendingMessage(currentMessageObject.getId());
            if (sending && (buttonState == 1 || buttonState == 4 && documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC)) {
                drawRadialCheckBackground = true;
                getIconForCurrentState();
                radialProgress.setIcon(MediaActionDrawable.ICON_CHECK, false, true);
            }
        }
        createLoadingProgressLayout(uploadedSize, totalSize);
    }

    private void createLoadingProgressLayout(TLRPC.Document document) {
        if (document == null) {
            return;
        }
        long[] progresses = ImageLoader.getInstance().getFileProgressSizes(FileLoader.getDocumentFileName(document));
        if (progresses != null) {
            createLoadingProgressLayout(progresses[0], progresses[1]);
        } else {
            createLoadingProgressLayout(currentMessageObject.loadedFileSize, document.size);
        }
    }

    private void createLoadingProgressLayout(long loadedSize, long totalSize) {
        if (totalSize <= 0 || documentAttach == null) {
            loadingProgressLayout = null;
            return;
        }

        if (lastLoadingSizeTotal == 0) {
            lastLoadingSizeTotal = totalSize;
        } else {
            totalSize = lastLoadingSizeTotal;
            if (loadedSize > lastLoadingSizeTotal) {
                loadedSize = lastLoadingSizeTotal;
            }
        }

        String totalStr = AndroidUtilities.formatFileSize(totalSize);
        String maxAvailableString = String.format("000.0 mm / %s", totalStr);
        String str;
        int w;
        w = (int) Math.ceil(Theme.chat_infoPaint.measureText(maxAvailableString));
        boolean fullWidth = true;
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
            int max = Math.max(this.infoWidth, docTitleWidth);
            if (w <= max) {
                str = String.format("%s / %s", AndroidUtilities.formatFileSize(loadedSize), totalStr);
            } else {
                str = AndroidUtilities.formatFileSize(loadedSize);
            }
        } else {
            if (currentPosition != null) {
                int mask = MessageObject.POSITION_FLAG_LEFT | MessageObject.POSITION_FLAG_RIGHT;
                fullWidth = (currentPosition.flags & mask) == mask;
            }
            if (!fullWidth) {
                int percent = (int) (Math.min(1f, loadedSize / (float) totalSize) * 100);
                if (percent >= 100) {
                    str = "100%";
                } else {
                    str = String.format(Locale.US, "%2d%%", percent);
                }
            } else {
                str = String.format("%s / %s", AndroidUtilities.formatFileSize(loadedSize), totalStr);
            }
        }
        w = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
        if (fullWidth && w > backgroundWidth - AndroidUtilities.dp(48)) {
            int percent = (int) (Math.min(1f, loadedSize / (float) totalSize) * 100);
            if (percent >= 100) {
                str = "100%";
            } else {
                str = String.format(Locale.US, "%2d%%", percent);
            }
            w = (int) Math.ceil(Theme.chat_infoPaint.measureText(str));
        }
        loadingProgressLayout = new StaticLayout(str, Theme.chat_infoPaint, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
    }


    @Override
    public void onProvideStructure(ViewStructure structure) {
        super.onProvideStructure(structure);
        if (allowAssistant && Build.VERSION.SDK_INT >= 23) {
            if (currentMessageObject.messageText != null && currentMessageObject.messageText.length() > 0) {
                structure.setText(currentMessageObject.messageText);
            } else if (currentMessageObject.caption != null && currentMessageObject.caption.length() > 0) {
                structure.setText(currentMessageObject.caption);
            }
        }
    }

    public void setDelegate(ChatMessageCellDelegate chatMessageCellDelegate) {
        delegate = chatMessageCellDelegate;
    }

    public ChatMessageCellDelegate getDelegate() {
        return delegate;
    }

    public void setAllowAssistant(boolean value) {
        allowAssistant = value;
    }

    private void measureTime(MessageObject messageObject) {
        CharSequence signString;
        long fromId = messageObject.getFromChatId();
        if (messageObject.scheduled) {
            signString = null;
        } else if (messageObject.messageOwner.post_author != null) {
            if (isMegagroup && messageObject.getFromChatId() == messageObject.getDialogId()) {
                signString = null;
            } else {
                signString = messageObject.messageOwner.post_author.replace("\n", "");
            }
        } else if (messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.post_author != null) {
            signString = messageObject.messageOwner.fwd_from.post_author.replace("\n", "");
        } else if (messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.imported) {
            if (messageObject.messageOwner.fwd_from.date == messageObject.messageOwner.date) {
                signString = LocaleController.getString("ImportedMessage", R.string.ImportedMessage);
            } else {
                signString = LocaleController.formatImportedDate(messageObject.messageOwner.fwd_from.date) + " " + LocaleController.getString("ImportedMessage", R.string.ImportedMessage);
            }
        } else if (!messageObject.isOutOwner() && fromId > 0 && messageObject.messageOwner.post) {
            TLRPC.User signUser = MessagesController.getInstance(currentAccount).getUser(fromId);
            if (signUser != null) {
                signString = ContactsController.formatName(signUser.first_name, signUser.last_name).replace('\n', ' ');
            } else {
                signString = null;
            }
        } else {
            signString = null;
        }
        String timeString;
        TLRPC.User author = null;
        if (currentMessageObject.isFromUser()) {
            author = MessagesController.getInstance(currentAccount).getUser(fromId);
        }
        boolean hasReplies = messageObject.hasReplies();
        if (messageObject.scheduled || messageObject.isLiveLocation() || messageObject.messageOwner.edit_hide || messageObject.getDialogId() == 777000 || messageObject.messageOwner.via_bot_id != 0 || messageObject.messageOwner.via_bot_name != null || author != null && author.bot) {
            edited = false;
        } else if (currentPosition == null || currentMessagesGroup == null || currentMessagesGroup.messages.isEmpty()) {
            edited = (messageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_EDITED) != 0 || messageObject.isEditing();
        } else {
            edited = false;
            hasReplies = currentMessagesGroup.messages.get(0).hasReplies();
            for (int a = 0, size = currentMessagesGroup.messages.size(); a < size; a++) {
                MessageObject object = currentMessagesGroup.messages.get(a);
                if ((object.messageOwner.flags & TLRPC.MESSAGE_FLAG_EDITED) != 0 || object.isEditing()) {
                    edited = true;
                    break;
                }
            }
        }
        if (currentMessageObject.isSponsored()) {
            timeString = LocaleController.getString("SponsoredMessage", R.string.SponsoredMessage);
        } else if (currentMessageObject.scheduled && currentMessageObject.messageOwner.date == 0x7FFFFFFE) {
            timeString = "";
        } else if (edited) {
            timeString = LocaleController.getString("EditedMessage", R.string.EditedMessage) + " " + LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000);
        } else {
            timeString = LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000);
        }
        if (signString != null) {
            if (messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.imported) {
                currentTimeString = " " + timeString;
            } else {
                currentTimeString = ", " + timeString;
            }
        } else {
            currentTimeString = timeString;
        }
        timeTextWidth = timeWidth = (int) Math.ceil(Theme.chat_timePaint.measureText(currentTimeString));
        if (currentMessageObject.scheduled && currentMessageObject.messageOwner.date == 0x7FFFFFFE) {
            timeWidth -= AndroidUtilities.dp(8);
        }
        if ((messageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
            currentViewsString = String.format("%s", LocaleController.formatShortNumber(Math.max(1, messageObject.messageOwner.views), null));
            viewsTextWidth = (int) Math.ceil(Theme.chat_timePaint.measureText(currentViewsString));
            timeWidth += viewsTextWidth + Theme.chat_msgInViewsDrawable.getIntrinsicWidth() + AndroidUtilities.dp(10);
        }
        if (isChat && isMegagroup && !isThreadChat && hasReplies) {
            currentRepliesString = String.format("%s", LocaleController.formatShortNumber(getRepliesCount(), null));
            repliesTextWidth = (int) Math.ceil(Theme.chat_timePaint.measureText(currentRepliesString));
            timeWidth += repliesTextWidth + Theme.chat_msgInRepliesDrawable.getIntrinsicWidth() + AndroidUtilities.dp(10);
        } else {
            currentRepliesString = null;
        }
        if (isPinned) {
            timeWidth += Theme.chat_msgInPinnedDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3);
        }
        if (messageObject.scheduled) {
            if (messageObject.isSendError()) {
                timeWidth += AndroidUtilities.dp(18);
            } else if (messageObject.isSending() && messageObject.messageOwner.peer_id.channel_id != 0 && !messageObject.isSupergroup()) {
                timeWidth += AndroidUtilities.dp(18);
            }
        }
        if (signString != null) {
            if (availableTimeWidth == 0) {
                availableTimeWidth = AndroidUtilities.dp(1000);
            }
            int widthForSign = availableTimeWidth - timeWidth;
            if (messageObject.isOutOwner()) {
                if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                    widthForSign -= AndroidUtilities.dp(20);
                } else {
                    widthForSign -= AndroidUtilities.dp(96);
                }
            }
            int width = (int) Math.ceil(Theme.chat_timePaint.measureText(signString, 0, signString.length()));
            if (width > widthForSign) {
                if (widthForSign <= 0) {
                    signString = "";
                    width = 0;
                } else {
                    signString = TextUtils.ellipsize(signString, Theme.chat_timePaint, widthForSign, TextUtils.TruncateAt.END);
                    width = widthForSign;
                }
            }
            currentTimeString = signString + currentTimeString;
            timeTextWidth += width;
            timeWidth += width;
        }
    }

    private boolean isDrawSelectionBackground() {
        return (isPressed() && isCheckPressed || !isCheckPressed && isPressed || isHighlighted) && !textIsSelectionMode();
    }

    private boolean isOpenChatByShare(MessageObject messageObject) {
        return messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.saved_from_peer != null;
    }

    private boolean checkNeedDrawShareButton(MessageObject messageObject) {
        if (currentMessageObject.deleted || currentMessageObject.isSponsored()) {
            return false;
        }
        if (currentPosition != null) {
            if (!currentMessagesGroup.isDocuments && !currentPosition.last) {
                return false;
            }
        }
        return messageObject.needDrawShareButton();
    }

    public boolean isInsideBackground(float x, float y) {
        return currentBackgroundDrawable != null && x >= backgroundDrawableLeft && x <= backgroundDrawableLeft + backgroundDrawableRight;
    }

    public void updateShareButton(){
        currentMessageObject.forceUpdate=true;
        setMessageContent(currentMessageObject, currentMessagesGroup, pinnedBottom, pinnedTop);
    }

    private void updateCurrentUserAndChat() {
        MessagesController messagesController = MessagesController.getInstance(currentAccount);
        TLRPC.MessageFwdHeader fwd_from = currentMessageObject.messageOwner.fwd_from;
        long currentUserId = UserConfig.getInstance(currentAccount).getClientUserId();
        if (fwd_from != null && fwd_from.from_id instanceof TLRPC.TL_peerChannel && currentMessageObject.getDialogId() == currentUserId) {
            currentChat = MessagesController.getInstance(currentAccount).getChat(fwd_from.from_id.channel_id);
        } else if (fwd_from != null && fwd_from.saved_from_peer != null) {
            if (fwd_from.saved_from_peer.user_id != 0) {
                if (fwd_from.from_id instanceof TLRPC.TL_peerUser) {
                    currentUser = messagesController.getUser(fwd_from.from_id.user_id);
                } else {
                    currentUser = messagesController.getUser(fwd_from.saved_from_peer.user_id);
                }
            } else if (fwd_from.saved_from_peer.channel_id != 0) {
                if (currentMessageObject.isSavedFromMegagroup() && fwd_from.from_id instanceof TLRPC.TL_peerUser) {
                    currentUser = messagesController.getUser(fwd_from.from_id.user_id);
                } else {
                    currentChat = messagesController.getChat(fwd_from.saved_from_peer.channel_id);
                }
            } else if (fwd_from.saved_from_peer.chat_id != 0) {
                if (fwd_from.from_id instanceof TLRPC.TL_peerUser) {
                    currentUser = messagesController.getUser(fwd_from.from_id.user_id);
                } else {
                    currentChat = messagesController.getChat(fwd_from.saved_from_peer.chat_id);
                }
            }
        } else if (fwd_from != null && fwd_from.from_id instanceof TLRPC.TL_peerUser && (fwd_from.imported || currentMessageObject.getDialogId() == currentUserId)) {
            currentUser = messagesController.getUser(fwd_from.from_id.user_id);
        } else if (fwd_from != null && !TextUtils.isEmpty(fwd_from.from_name) && (fwd_from.imported || currentMessageObject.getDialogId() == currentUserId)) {
            currentUser = new TLRPC.TL_user();
            currentUser.first_name = fwd_from.from_name;
        } else {
            long fromId = currentMessageObject.getFromChatId();
            if (DialogObject.isUserDialog(fromId) && !currentMessageObject.messageOwner.post) {
                currentUser = messagesController.getUser(fromId);
            } else if (DialogObject.isChatDialog(fromId)) {
                currentChat = messagesController.getChat(-fromId);
            } else if (currentMessageObject.messageOwner.post) {
                currentChat = messagesController.getChat(currentMessageObject.messageOwner.peer_id.channel_id);
            }
        }
    }

    private void setMessageObjectInternal(MessageObject messageObject) {
        if (((messageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0 || messageObject.messageOwner.replies != null) && !currentMessageObject.scheduled && !currentMessageObject.isSponsored()) {
            if (!currentMessageObject.viewsReloaded) {
                MessagesController.getInstance(currentAccount).addToViewsQueue(currentMessageObject);
                currentMessageObject.viewsReloaded = true;
            }
        }

        updateCurrentUserAndChat();

        if (isAvatarVisible) {
            if (currentUser != null) {
                if (currentUser.photo != null) {
                    currentPhoto = currentUser.photo.photo_small;
                } else {
                    currentPhoto = null;
                }
                avatarDrawable.setInfo(currentUser);
                avatarImage.setForUserOrChat(currentUser, avatarDrawable);
            } else if (currentChat != null) {
                if (currentChat.photo != null) {
                    currentPhoto = currentChat.photo.photo_small;
                } else {
                    currentPhoto = null;
                }
                avatarDrawable.setInfo(currentChat);
                avatarImage.setForUserOrChat(currentChat, avatarDrawable);
            } else {
                currentPhoto = null;
                avatarDrawable.setInfo(messageObject.getFromChatId(), null, null);
                avatarImage.setImage(null, null, avatarDrawable, null, null, 0);
            }
        } else {
            currentPhoto = null;
        }


        measureTime(messageObject);

        namesOffset = 0;

        String viaUsername = null;
        CharSequence viaString = null;
        if (messageObject.messageOwner.via_bot_id != 0) {
            TLRPC.User botUser = MessagesController.getInstance(currentAccount).getUser(messageObject.messageOwner.via_bot_id);
            if (botUser != null && !TextUtils.isEmpty(botUser.username)) {
                viaUsername = "@" + botUser.username;
                viaString = AndroidUtilities.replaceTags(String.format(" %s <b>%s</b>", LocaleController.getString("ViaBot", R.string.ViaBot), viaUsername));
                viaWidth = (int) Math.ceil(Theme.chat_replyNamePaint.measureText(viaString, 0, viaString.length()));
                currentViaBotUser = botUser;
            }
        } else if (!TextUtils.isEmpty(messageObject.messageOwner.via_bot_name)) {
            viaUsername = "@" + messageObject.messageOwner.via_bot_name;
            viaString = AndroidUtilities.replaceTags(String.format(" %s <b>%s</b>", LocaleController.getString("ViaBot", R.string.ViaBot), viaUsername));
            viaWidth = (int) Math.ceil(Theme.chat_replyNamePaint.measureText(viaString, 0, viaString.length()));
        }

        boolean needAuthorName = isNeedAuthorName();
        boolean viaBot = (messageObject.messageOwner.fwd_from == null || messageObject.type == 14) && viaUsername != null;
        if (!hasPsaHint && (needAuthorName || viaBot)) {
            drawNameLayout = true;
            nameWidth = getMaxNameWidth();
            if (nameWidth < 0) {
                nameWidth = AndroidUtilities.dp(100);
            }
            int adminWidth;
            String adminString;
            String adminLabel;
            if (isMegagroup && currentChat != null && messageObject.messageOwner.post_author != null && currentChat.id == -currentMessageObject.getFromChatId()) {
                adminString = messageObject.messageOwner.post_author.replace("\n", "");
                adminWidth = (int) Math.ceil(Theme.chat_adminPaint.measureText(adminString));
                nameWidth -= adminWidth;
            } else if (isMegagroup && currentChat != null && currentMessageObject.isForwardedChannelPost()) {
                adminString = LocaleController.getString("DiscussChannel", R.string.DiscussChannel);
                adminWidth = (int) Math.ceil(Theme.chat_adminPaint.measureText(adminString));
                nameWidth -= adminWidth; //TODO
            } else if (currentUser != null && !currentMessageObject.isOutOwner() && !currentMessageObject.isAnyKindOfSticker() && currentMessageObject.type != 5 && delegate != null && (adminLabel = delegate.getAdminRank(currentUser.id)) != null) {
                if (adminLabel.length() == 0) {
                    adminLabel = LocaleController.getString("ChatAdmin", R.string.ChatAdmin);
                }
                adminString = adminLabel;
                adminWidth = (int) Math.ceil(Theme.chat_adminPaint.measureText(adminString));
                nameWidth -= adminWidth;
            } else {
                adminString = null;
                adminWidth = 0;
            }

            if (needAuthorName) {
                currentNameString = getAuthorName();
            } else {
                currentNameString = "";
            }
            CharSequence nameStringFinal = TextUtils.ellipsize(currentNameString.replace('\n', ' ').replace('\u200F', ' '), Theme.chat_namePaint, nameWidth - (viaBot ? viaWidth : 0), TextUtils.TruncateAt.END);
            if (viaBot) {
                viaNameWidth = (int) Math.ceil(Theme.chat_namePaint.measureText(nameStringFinal, 0, nameStringFinal.length()));
                if (viaNameWidth != 0) {
                    viaNameWidth += AndroidUtilities.dp(4);
                }
                int color;
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    color = getThemedColor(Theme.key_chat_stickerViaBotNameText);
                } else {
                    color = getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outViaBotNameText : Theme.key_chat_inViaBotNameText);
                }
                String viaBotString = LocaleController.getString("ViaBot", R.string.ViaBot);
                if (currentNameString.length() > 0) {
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder(String.format("%s %s %s", nameStringFinal, viaBotString, viaUsername));
                    stringBuilder.setSpan(viaSpan1 = new TypefaceSpan(Typeface.DEFAULT, 0, color), nameStringFinal.length() + 1, nameStringFinal.length() + 1 + viaBotString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    stringBuilder.setSpan(viaSpan2 = new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), 0, color), nameStringFinal.length() + 2 + viaBotString.length(), stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    nameStringFinal = stringBuilder;
                } else {
                    SpannableStringBuilder stringBuilder = new SpannableStringBuilder(String.format("%s %s", viaBotString, viaUsername));
                    stringBuilder.setSpan(viaSpan1 = new TypefaceSpan(Typeface.DEFAULT, 0, color), 0, viaBotString.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    stringBuilder.setSpan(viaSpan2 = new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf"), 0, color), 1 + viaBotString.length(), stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    nameStringFinal = stringBuilder;
                }
                nameStringFinal = TextUtils.ellipsize(nameStringFinal, Theme.chat_namePaint, nameWidth, TextUtils.TruncateAt.END);
            }
            try {
                nameLayout = new StaticLayout(nameStringFinal, Theme.chat_namePaint, nameWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (nameLayout.getLineCount() > 0) {
                    nameWidth = (int) Math.ceil(nameLayout.getLineWidth(0));
                    if (!messageObject.isAnyKindOfSticker()) {
                        namesOffset += AndroidUtilities.dp(19);
                    }
                    nameOffsetX = nameLayout.getLineLeft(0);
                } else {
                    nameWidth = 0;
                }
                if (adminString != null) {
                    adminLayout = new StaticLayout(adminString, Theme.chat_adminPaint, adminWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    nameWidth += adminLayout.getLineWidth(0) + AndroidUtilities.dp(8);
                } else {
                    adminLayout = null;
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (currentNameString.length() == 0) {
                currentNameString = null;
            }
        } else {
            currentNameString = null;
            nameLayout = null;
            nameWidth = 0;
        }

        currentForwardUser = null;
        currentForwardNameString = null;
        currentForwardChannel = null;
        currentForwardName = null;
        forwardedNameLayout[0] = null;
        forwardedNameLayout[1] = null;
        replyPanelIsForward = false;
        forwardedNameWidth = 0;
        if (messageObject.isForwarded()) {
            if (messageObject.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChannel) {
                currentForwardChannel = MessagesController.getInstance(currentAccount).getChat(messageObject.messageOwner.fwd_from.from_id.channel_id);
            } else if (messageObject.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChat) {
                currentForwardChannel = MessagesController.getInstance(currentAccount).getChat(messageObject.messageOwner.fwd_from.from_id.chat_id);
            } else if (messageObject.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerUser) {
                currentForwardUser = MessagesController.getInstance(currentAccount).getUser(messageObject.messageOwner.fwd_from.from_id.user_id);
            }
        }
        if (drawForwardedName && messageObject.needDrawForwarded() && (currentPosition == null || currentPosition.minY == 0)) {
            if (messageObject.messageOwner.fwd_from.from_name != null) {
                currentForwardName = messageObject.messageOwner.fwd_from.from_name;
            }

            if (currentForwardUser != null || currentForwardChannel != null || currentForwardName != null) {
                if (currentForwardChannel != null) {
                    if (currentForwardUser != null) {
                        currentForwardNameString = String.format("%s (%s)", currentForwardChannel.title, UserObject.getUserName(currentForwardUser));
                    } else if (!TextUtils.isEmpty(messageObject.messageOwner.fwd_from.post_author)) {
                        currentForwardNameString = String.format("%s (%s)", currentForwardChannel.title, messageObject.messageOwner.fwd_from.post_author);
                    } else {
                        currentForwardNameString = currentForwardChannel.title;
                    }
                } else if (currentForwardUser != null) {
                    currentForwardNameString = UserObject.getUserName(currentForwardUser);
                } else {
                    currentForwardNameString = currentForwardName;
                }

                forwardedNameWidth = getMaxNameWidth();
                String forwardedString = getForwardedMessageText(messageObject);
                if (hasPsaHint) {
                    forwardedNameWidth -= AndroidUtilities.dp(36);
                }
                String from = LocaleController.getString("From", R.string.From);
                String fromFormattedString = LocaleController.getString("FromFormatted", R.string.FromFormatted);
                int idx = fromFormattedString.indexOf("%1$s");
                int fromWidth = (int) Math.ceil(Theme.chat_forwardNamePaint.measureText(from + " "));
                CharSequence name = TextUtils.ellipsize(currentForwardNameString.replace('\n', ' '), Theme.chat_replyNamePaint, forwardedNameWidth - fromWidth - viaWidth, TextUtils.TruncateAt.END);
                String fromString;
                try {
                    fromString = String.format(fromFormattedString, name);
                } catch (Exception e) {
                    fromString = name.toString();
                }
                CharSequence lastLine;
                SpannableStringBuilder stringBuilder;
                if (viaString != null) {
                    stringBuilder = new SpannableStringBuilder(String.format("%s %s %s", fromString, LocaleController.getString("ViaBot", R.string.ViaBot), viaUsername));
                    viaNameWidth = (int) Math.ceil(Theme.chat_forwardNamePaint.measureText(fromString));
                    stringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf")), stringBuilder.length() - viaUsername.length() - 1, stringBuilder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    stringBuilder = new SpannableStringBuilder(String.format(fromFormattedString, name));
                }
                forwardNameCenterX = fromWidth + (int) Math.ceil(Theme.chat_forwardNamePaint.measureText(name, 0, name.length())) / 2;
                if (idx >= 0 && (currentForwardName == null || messageObject.messageOwner.fwd_from.from_id != null)) {
                    stringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf")), idx, idx + name.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                lastLine = stringBuilder;
                lastLine = TextUtils.ellipsize(lastLine, Theme.chat_forwardNamePaint, forwardedNameWidth, TextUtils.TruncateAt.END);
                try {
                    forwardedNameLayout[1] = new StaticLayout(lastLine, Theme.chat_forwardNamePaint, forwardedNameWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    lastLine = TextUtils.ellipsize(AndroidUtilities.replaceTags(forwardedString), Theme.chat_forwardNamePaint, forwardedNameWidth, TextUtils.TruncateAt.END);
                    forwardedNameLayout[0] = new StaticLayout(lastLine, Theme.chat_forwardNamePaint, forwardedNameWidth + AndroidUtilities.dp(2), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    forwardedNameWidth = Math.max((int) Math.ceil(forwardedNameLayout[0].getLineWidth(0)), (int) Math.ceil(forwardedNameLayout[1].getLineWidth(0)));
                    if (hasPsaHint) {
                        forwardedNameWidth += AndroidUtilities.dp(36);
                    }
                    forwardNameOffsetX[0] = forwardedNameLayout[0].getLineLeft(0);
                    forwardNameOffsetX[1] = forwardedNameLayout[1].getLineLeft(0);
                    if (messageObject.type != 5 && !messageObject.isAnyKindOfSticker()) {
                        namesOffset += AndroidUtilities.dp(36);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        }

        if ((!isThreadChat || messageObject.getReplyTopMsgId() != 0) && messageObject.hasValidReplyMessageObject() || messageObject.messageOwner.fwd_from != null && messageObject.isDice()) {
            if (currentPosition == null || currentPosition.minY == 0) {
                if (!messageObject.isAnyKindOfSticker() && messageObject.type != 5) {
                    namesOffset += AndroidUtilities.dp(42);
                    if (messageObject.type != 0) {
                        namesOffset += AndroidUtilities.dp(5);
                    }
                }

                int maxWidth = getMaxNameWidth();
                if (!messageObject.shouldDrawWithoutBackground()) {
                    maxWidth -= AndroidUtilities.dp(10);
                } else if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                    maxWidth += AndroidUtilities.dp(13);
                }

                CharSequence stringFinalText = null;

                String name = null;
                if ((!isThreadChat || messageObject.getReplyTopMsgId() != 0) && messageObject.hasValidReplyMessageObject()) {
                    lastReplyMessage = messageObject.replyMessageObject.messageOwner;
                    int cacheType = 1;
                    int size = 0;
                    TLObject photoObject;
                    TLRPC.PhotoSize photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.replyMessageObject.photoThumbs2, 320);
                    TLRPC.PhotoSize thumbPhotoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.replyMessageObject.photoThumbs2, 40);
                    photoObject = messageObject.replyMessageObject.photoThumbsObject2;
                    if (photoSize == null) {
                        if (messageObject.replyMessageObject.mediaExists) {
                            photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.replyMessageObject.photoThumbs, AndroidUtilities.getPhotoSize());
                            if (photoSize != null) {
                                size = photoSize.size;
                            }
                            cacheType = 0;
                        } else {
                            photoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.replyMessageObject.photoThumbs, 320);
                        }
                        thumbPhotoSize = FileLoader.getClosestPhotoSizeWithSize(messageObject.replyMessageObject.photoThumbs, 40);
                        photoObject = messageObject.replyMessageObject.photoThumbsObject;
                    }
                    if (thumbPhotoSize == photoSize) {
                        thumbPhotoSize = null;
                    }
                    if (photoSize == null || messageObject.replyMessageObject.isAnyKindOfSticker() || messageObject.isAnyKindOfSticker() && !AndroidUtilities.isTablet() || messageObject.replyMessageObject.isSecretMedia() || messageObject.replyMessageObject.isWebpageDocument()) {
                        replyImageReceiver.setImageBitmap((Drawable) null);
                        needReplyImage = false;
                    } else {
                        if (messageObject.replyMessageObject.isRoundVideo()) {
                            replyImageReceiver.setRoundRadius(AndroidUtilities.dp(22));
                        } else {
                            replyImageReceiver.setRoundRadius(AndroidUtilities.dp(2));
                        }
                        currentReplyPhoto = photoSize;
                        replyImageReceiver.setImage(ImageLocation.getForObject(photoSize, photoObject), "50_50", ImageLocation.getForObject(thumbPhotoSize, photoObject), "50_50_b", size, null, messageObject.replyMessageObject, cacheType);
                        needReplyImage = true;
                        maxWidth -= AndroidUtilities.dp(44);
                    }

                    if (messageObject.customReplyName != null) {
                        name = messageObject.customReplyName;
                    } else {
                        long fromId = messageObject.replyMessageObject.getFromChatId();
                        if (fromId > 0) {
                            TLRPC.User user = MessagesController.getInstance(currentAccount).getUser(fromId);
                            if (user != null) {
                                name = UserObject.getUserName(user);
                            }
                        } else if (fromId < 0) {
                            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-fromId);
                            if (chat != null) {
                                name = chat.title;
                            }
                        } else {
                            TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(messageObject.replyMessageObject.messageOwner.peer_id.channel_id);
                            if (chat != null) {
                                name = chat.title;
                            }
                        }
                    }

                    if (name == null) {
                        name = LocaleController.getString("Loading", R.string.Loading);
                    }
                    if (messageObject.replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame) {
                        stringFinalText = Emoji.replaceEmoji(messageObject.replyMessageObject.messageOwner.media.game.title, Theme.chat_replyTextPaint.getFontMetricsInt(), AndroidUtilities.dp(14), false);
                        stringFinalText = TextUtils.ellipsize(stringFinalText, Theme.chat_replyTextPaint, maxWidth, TextUtils.TruncateAt.END);
                    } else if (messageObject.replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice) {
                        stringFinalText = Emoji.replaceEmoji(messageObject.replyMessageObject.messageOwner.media.title, Theme.chat_replyTextPaint.getFontMetricsInt(), AndroidUtilities.dp(14), false);
                        stringFinalText = TextUtils.ellipsize(stringFinalText, Theme.chat_replyTextPaint, maxWidth, TextUtils.TruncateAt.END);
                    } else if (!TextUtils.isEmpty(messageObject.replyMessageObject.caption)) {
                        String mess = messageObject.replyMessageObject.caption.toString();
                        if (mess.length() > 150) {
                            mess = mess.substring(0, 150);
                        }
                        mess = mess.replace('\n', ' ');
                        stringFinalText = Emoji.replaceEmoji(mess, Theme.chat_replyTextPaint.getFontMetricsInt(), AndroidUtilities.dp(14), false);
                        stringFinalText = TextUtils.ellipsize(stringFinalText, Theme.chat_replyTextPaint, maxWidth, TextUtils.TruncateAt.END);
                    } else if (messageObject.replyMessageObject.messageText != null && messageObject.replyMessageObject.messageText.length() > 0) {
                        String mess = messageObject.replyMessageObject.messageText.toString();
                        if (mess.length() > 150) {
                            mess = mess.substring(0, 150);
                        }
                        mess = mess.replace('\n', ' ');
                        stringFinalText = Emoji.replaceEmoji(mess, Theme.chat_replyTextPaint.getFontMetricsInt(), AndroidUtilities.dp(14), false);
                        stringFinalText = TextUtils.ellipsize(stringFinalText, Theme.chat_replyTextPaint, maxWidth, TextUtils.TruncateAt.END);
                    }
                } else {
                    replyImageReceiver.setImageBitmap((Drawable) null);
                    needReplyImage = false;
                    replyPanelIsForward = true;
                    if (messageObject.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChannel) {
                        currentForwardChannel = MessagesController.getInstance(currentAccount).getChat(messageObject.messageOwner.fwd_from.from_id.channel_id);
                    } else if (messageObject.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerChat) {
                        currentForwardChannel = MessagesController.getInstance(currentAccount).getChat(messageObject.messageOwner.fwd_from.from_id.chat_id);
                    } else if (messageObject.messageOwner.fwd_from.from_id instanceof TLRPC.TL_peerUser) {
                        currentForwardUser = MessagesController.getInstance(currentAccount).getUser(messageObject.messageOwner.fwd_from.from_id.user_id);
                    }
                    if (messageObject.messageOwner.fwd_from.from_name != null) {
                        currentForwardName = messageObject.messageOwner.fwd_from.from_name;
                    }

                    if (currentForwardUser != null || currentForwardChannel != null || currentForwardName != null) {
                        if (currentForwardChannel != null) {
                            if (currentForwardUser != null) {
                                currentForwardNameString = String.format("%s (%s)", currentForwardChannel.title, UserObject.getUserName(currentForwardUser));
                            } else {
                                currentForwardNameString = currentForwardChannel.title;
                            }
                        } else if (currentForwardUser != null) {
                            currentForwardNameString = UserObject.getUserName(currentForwardUser);
                        } else {
                            currentForwardNameString = currentForwardName;
                        }
                        name = getForwardedMessageText(messageObject);
                        String from = LocaleController.getString("From", R.string.From);
                        String fromFormattedString = LocaleController.getString("FromFormatted", R.string.FromFormatted);
                        int idx = fromFormattedString.indexOf("%1$s");
                        int fromWidth = (int) Math.ceil(Theme.chat_replyNamePaint.measureText(from + " "));
                        CharSequence n = TextUtils.ellipsize(currentForwardNameString == null ? "" : currentForwardNameString.replace('\n', ' '), Theme.chat_replyNamePaint, maxWidth - fromWidth, TextUtils.TruncateAt.END);
                        SpannableStringBuilder stringBuilder = new SpannableStringBuilder(String.format(fromFormattedString, n));
                        if (idx >= 0 && (currentForwardName == null || messageObject.messageOwner.fwd_from.from_id != null)) {
                            stringBuilder.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/rmedium.ttf")), idx, idx + n.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        stringFinalText = TextUtils.ellipsize(stringBuilder, Theme.chat_replyTextPaint, maxWidth, TextUtils.TruncateAt.END);
                        forwardNameCenterX = fromWidth + (int) Math.ceil(Theme.chat_replyNamePaint.measureText(n, 0, n.length())) / 2;
                    }
                }
                CharSequence stringFinalName = name == null ? "" : TextUtils.ellipsize(name.replace('\n', ' '), Theme.chat_replyNamePaint, maxWidth, TextUtils.TruncateAt.END);

                try {
                    replyNameWidth = AndroidUtilities.dp(4 + (needReplyImage ? 44 : 0));
                    if (stringFinalName != null) {
                        replyNameLayout = new StaticLayout(stringFinalName, Theme.chat_replyNamePaint, maxWidth + AndroidUtilities.dp(6), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                        if (replyNameLayout.getLineCount() > 0) {
                            replyNameWidth += (int) Math.ceil(replyNameLayout.getLineWidth(0)) + AndroidUtilities.dp(8);
                            replyNameOffset = (int) replyNameLayout.getLineLeft(0);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
                try {
                    replyTextWidth = AndroidUtilities.dp(4 + (needReplyImage ? 44 : 0));
                    if (stringFinalText != null) {
                        replyTextLayout = new StaticLayout(stringFinalText, Theme.chat_replyTextPaint, maxWidth + AndroidUtilities.dp(10), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                        if (replyTextLayout.getLineCount() > 0) {
                            replyTextWidth += (int) Math.ceil(replyTextLayout.getLineWidth(0)) + AndroidUtilities.dp(8);
                            replyTextOffset = (int) replyTextLayout.getLineLeft(0);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } else if (!isThreadChat && messageObject.getReplyMsgId() != 0) {
            if (!(messageObject.replyMessageObject != null && messageObject.replyMessageObject.messageOwner instanceof TLRPC.TL_messageEmpty)) {
                if (!messageObject.isAnyKindOfSticker() && messageObject.type != 5) {
                    namesOffset += AndroidUtilities.dp(42);
                    if (messageObject.type != 0) {
                        namesOffset += AndroidUtilities.dp(5);
                    }
                }
                needReplyImage = false;

                int maxWidth = getMaxNameWidth();
                if (!messageObject.shouldDrawWithoutBackground()) {
                    maxWidth -= AndroidUtilities.dp(10);
                } else if (messageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                    maxWidth += AndroidUtilities.dp(13);
                }

                replyNameLayout = new StaticLayout(LocaleController.getString("Loading", R.string.Loading), Theme.chat_replyNamePaint, maxWidth + AndroidUtilities.dp(6), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                if (replyNameLayout.getLineCount() > 0) {
                    replyNameWidth += (int) Math.ceil(replyNameLayout.getLineWidth(0)) + AndroidUtilities.dp(8);
                    replyNameOffset = (int) replyNameLayout.getLineLeft(0);
                }
            }
        }

        requestLayout();
    }

    private boolean isNeedAuthorName() {
        return isPinnedChat && currentMessageObject.type == 0 || (!pinnedTop || ChatObject.isChannel(currentChat) && !currentChat.megagroup) && drawName && isChat && (!currentMessageObject.isOutOwner() || currentMessageObject.isSupergroup() && currentMessageObject.isFromGroup()) || currentMessageObject.isImportedForward() && currentMessageObject.messageOwner.fwd_from.from_id == null;
    }

    private String getAuthorName() {
        if (currentUser != null) {
            return UserObject.getUserName(currentUser);
        } else if (currentChat != null) {
            return currentChat.title;
        } else {
            return "DELETED";
        }
    }

    private String getForwardedMessageText(MessageObject messageObject) {
        if (hasPsaHint) {
            String forwardedString = LocaleController.getString("PsaMessage_" + messageObject.messageOwner.fwd_from.psa_type);
            if (forwardedString == null) {
                forwardedString = LocaleController.getString("PsaMessageDefault", R.string.PsaMessageDefault);
            }
            return forwardedString;
        } else {
            return LocaleController.getString("ForwardedMessage", R.string.ForwardedMessage);
        }
    }

    public int getExtraInsetHeight() {
        int h = addedCaptionHeight;
        if (drawCommentButton) {
            h += AndroidUtilities.dp(shouldDrawTimeOnMedia() ? 41.3f : 43);
        }
        return h;
    }

    public ImageReceiver getAvatarImage() {
        return isAvatarVisible ? avatarImage : null;
    }

    public float getCheckBoxTranslation() {
        return checkBoxTranslation;
    }

    public boolean shouldDrawAlphaLayer() {
        return (currentMessagesGroup == null || !currentMessagesGroup.transitionParams.backgroundChangeBounds) && getAlpha() != 1f;
    }

    public float getCaptionX() {
        return captionX;
    }

    public boolean isDrawPinnedBottom() {
        return mediaBackground || drawPinnedBottom;
    }

    public void drawCheckBox(Canvas canvas) {
        if (currentMessageObject != null && !currentMessageObject.isSending() && !currentMessageObject.isSendError() && checkBox != null && (checkBoxVisible || checkBoxAnimationInProgress) && (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0 && (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0)) {
            canvas.save();
            float y = getY();
            if (currentMessagesGroup != null && currentMessagesGroup.messages.size() > 1) {
                y = getTop() + currentMessagesGroup.transitionParams.offsetTop - getTranslationY();
            } else {
                y += transitionParams.deltaTop;
            }
            canvas.translate(0, y + transitionYOffsetForDrawables);
            checkBox.draw(canvas);
            canvas.restore();
        }
    }

    public void setBackgroundTopY(boolean fromParent) {
        for (int a = 0; a < 2; a++) {
            if (a == 1 && !fromParent) {
                return;
            }
            Theme.MessageDrawable drawable = a == 0 ? currentBackgroundDrawable : currentBackgroundSelectedDrawable;
            if (drawable == null) {
                continue;
            }
            int w = parentWidth;
            int h = parentHeight;
            if (h == 0) {
                w = getParentWidth();
                h = AndroidUtilities.displaySize.y;
                if (getParent() instanceof View) {
                    View view = (View) getParent();
                    w = view.getMeasuredWidth();
                    h = view.getMeasuredHeight();
                }
            }
            drawable.setTop((int) ((fromParent ? getY() : getTop()) + parentViewTopOffset), w, h, (int) parentViewTopOffset, pinnedTop, pinnedBottom || transitionParams.changePinnedBottomProgress != 1);
        }
    }

    public void setBackgroundTopY(int offset) {
        Theme.MessageDrawable drawable = currentBackgroundDrawable;
        int w = parentWidth;
        int h = parentHeight;
        if (h == 0) {
            w = getParentWidth();
            h = AndroidUtilities.displaySize.y;
            if (getParent() instanceof View) {
                View view = (View) getParent();
                w = view.getMeasuredWidth();
                h = view.getMeasuredHeight();
            }
        }
        drawable.setTop((int) (getTop() + parentViewTopOffset + offset), w, h, (int) parentViewTopOffset, pinnedTop, pinnedBottom || transitionParams.changePinnedBottomProgress != 1);
    }

    float transitionYOffsetForDrawables;
    public void setDrawableBoundsInner(Drawable drawable, int x, int y, int w, int h) {
        if (drawable != null) {
            transitionYOffsetForDrawables = (y + h + transitionParams.deltaBottom) - ((int) (y + h + transitionParams.deltaBottom));
            drawable.setBounds((int) (x + transitionParams.deltaLeft), (int) (y + transitionParams.deltaTop), (int) (x + w + transitionParams.deltaRight), (int) (y + h + transitionParams.deltaBottom));
        }
    }

    @SuppressLint("WrongCall")
    @Override
    protected void onDraw(Canvas canvas) {
        if (currentMessageObject == null) {
            return;
        }
        if (!wasLayout && !animationRunning) {
            forceLayout();
            return;
        }
        if (!wasLayout) {
            onLayout(false, getLeft(), getTop(), getRight(), getBottom());
        }

        if (currentMessageObject.isOutOwner()) {
            Theme.chat_msgTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
            Theme.chat_msgGameTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
            Theme.chat_msgGameTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkOut);
            Theme.chat_replyTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkOut);
            Theme.chat_msgTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkOut);
        } else {
            Theme.chat_msgTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            Theme.chat_msgGameTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            Theme.chat_msgGameTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkIn);
            Theme.chat_replyTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkIn);
            Theme.chat_msgTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkIn);
        }

        if (documentAttach != null) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO) {
                if (currentMessageObject.isOutOwner()) {
                    seekBarWaveform.setColors(getThemedColor(Theme.key_chat_outVoiceSeekbar), getThemedColor(Theme.key_chat_outVoiceSeekbarFill), getThemedColor(Theme.key_chat_outVoiceSeekbarSelected));
                    seekBar.setColors(getThemedColor(Theme.key_chat_outAudioSeekbar), getThemedColor(Theme.key_chat_outAudioCacheSeekbar), getThemedColor(Theme.key_chat_outAudioSeekbarFill), getThemedColor(Theme.key_chat_outAudioSeekbarFill), getThemedColor(Theme.key_chat_outAudioSeekbarSelected));
                } else {
                    seekBarWaveform.setColors(getThemedColor(Theme.key_chat_inVoiceSeekbar), getThemedColor(Theme.key_chat_inVoiceSeekbarFill), getThemedColor(Theme.key_chat_inVoiceSeekbarSelected));
                    seekBar.setColors(getThemedColor(Theme.key_chat_inAudioSeekbar), getThemedColor(Theme.key_chat_inAudioCacheSeekbar), getThemedColor(Theme.key_chat_inAudioSeekbarFill), getThemedColor(Theme.key_chat_inAudioSeekbarFill), getThemedColor(Theme.key_chat_inAudioSeekbarSelected));
                }
            } else if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC) {
                if (currentMessageObject.isOutOwner()) {
                    seekBar.setColors(getThemedColor(Theme.key_chat_outAudioSeekbar), getThemedColor(Theme.key_chat_outAudioCacheSeekbar), getThemedColor(Theme.key_chat_outAudioSeekbarFill), getThemedColor(Theme.key_chat_outAudioSeekbarFill), getThemedColor(Theme.key_chat_outAudioSeekbarSelected));
                } else {
                    seekBar.setColors(getThemedColor(Theme.key_chat_inAudioSeekbar), getThemedColor(Theme.key_chat_inAudioCacheSeekbar), getThemedColor(Theme.key_chat_inAudioSeekbarFill), getThemedColor(Theme.key_chat_inAudioSeekbarFill), getThemedColor(Theme.key_chat_inAudioSeekbarSelected));
                }
            }
        }
        if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
            Theme.chat_timePaint.setColor(getThemedColor(Theme.key_chat_serviceText));
        } else {
            if (mediaBackground) {
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    Theme.chat_timePaint.setColor(getThemedColor(Theme.key_chat_serviceText));
                } else {
                    Theme.chat_timePaint.setColor(getThemedColor(Theme.key_chat_mediaTimeText));
                }
            } else {
                if (currentMessageObject.isOutOwner()) {
                    Theme.chat_timePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outTimeSelectedText : Theme.key_chat_outTimeText));
                } else {
                    Theme.chat_timePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inTimeSelectedText : Theme.key_chat_inTimeText));
                }
            }
        }

        drawBackgroundInternal(canvas, false);
        if (isHighlightedAnimated) {
            long newTime = System.currentTimeMillis();
            long dt = Math.abs(newTime - lastHighlightProgressTime);
            if (dt > 17) {
                dt = 17;
            }
            highlightProgress -= dt;
            lastHighlightProgressTime = newTime;
            if (highlightProgress <= 0) {
                highlightProgress = 0;
                isHighlightedAnimated = false;
            }
            invalidate();
            if (getParent() != null) {
                ((View) getParent()).invalidate();
            }
        }

        int restore = Integer.MIN_VALUE;
        if (alphaInternal != 1.0f) {
            int top = 0;
            int left = 0;
            int bottom = getMeasuredHeight();
            int right = getMeasuredWidth();

            if (currentBackgroundDrawable != null) {
                top = currentBackgroundDrawable.getBounds().top;
                bottom = currentBackgroundDrawable.getBounds().bottom;
                left = currentBackgroundDrawable.getBounds().left;
                right = currentBackgroundDrawable.getBounds().right;
            }

            if (drawSideButton != 0) {
                if (currentMessageObject.isOutOwner()) {
                    left -= AndroidUtilities.dp(8 + 32);
                } else {
                    right += AndroidUtilities.dp(8 + 32);
                }
            }
            if (getY() < 0) {
                top = (int) -getY();
            }
            if (getY() + getMeasuredHeight() > parentHeight) {
                bottom = (int) (parentHeight - getY());
            }
            rect.set(left, top, right, bottom);
            restore = canvas.saveLayerAlpha(rect, (int) (255 * alphaInternal), Canvas.ALL_SAVE_FLAG);
        }
        boolean clipContent = false;
        if (transitionParams.animateBackgroundBoundsInner && currentBackgroundDrawable != null && !isRoundVideo) {
            Rect r = currentBackgroundDrawable.getBounds();
            canvas.save();
            canvas.clipRect(
                    r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                    r.right - AndroidUtilities.dp(4), r.bottom - AndroidUtilities.dp(4)
            );
            clipContent = true;
        }
        drawContent(canvas);
        if (clipContent) {
            canvas.restore();
        }

        if (!transitionParams.animateBackgroundBoundsInner) {
            if (!transitionParams.transitionBotButtons.isEmpty()) {
                drawBotButtons(canvas, transitionParams.transitionBotButtons, 1f - transitionParams.animateChangeProgress);
            }
            if (!botButtons.isEmpty()) {
                drawBotButtons(canvas, botButtons, transitionParams.animateChangeProgress);
            }
        }

        if (drawSideButton != 0) {
            if (currentMessageObject.isOutOwner()) {
                sideStartX = getCurrentBackgroundLeft() - AndroidUtilities.dp(8 + 32);
                if (currentMessagesGroup != null) {
                    sideStartX += currentMessagesGroup.transitionParams.offsetLeft - animationOffsetX;
                }
            } else {
                sideStartX = currentBackgroundDrawable.getBounds().right + AndroidUtilities.dp(8);
                if (currentMessagesGroup != null) {
                    sideStartX += currentMessagesGroup.transitionParams.offsetRight - animationOffsetX;
                }
            }
            sideStartY = layoutHeight - AndroidUtilities.dp(41) + transitionParams.deltaBottom;
            if (currentMessagesGroup != null) {
                sideStartY += currentMessagesGroup.transitionParams.offsetBottom;
                if (currentMessagesGroup.transitionParams.backgroundChangeBounds) {
                    sideStartY -= getTranslationY();
                }
            }
            if (!currentMessageObject.isOutOwner() && isRoundVideo && isAvatarVisible) {
                float offsetSize = (AndroidUtilities.roundPlayingMessageSize - AndroidUtilities.roundMessageSize) * 0.7f;
                float offsetX = isPlayingRound ? offsetSize : 0;
                if (transitionParams.animatePlayingRound) {
                    offsetX = (isPlayingRound ? transitionParams.animateChangeProgress : (1f - transitionParams.animateChangeProgress)) * offsetSize;
                }
                sideStartX -= offsetX;
            }
            if (drawSideButton == 3) {
                if (!(enterTransitionInPorgress && !currentMessageObject.isVoice())) {
                    drawCommentButton(canvas, 1f);
                }
            } else {
                rect.set(sideStartX, sideStartY, sideStartX + AndroidUtilities.dp(32), sideStartY + AndroidUtilities.dp(32));
                applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
                canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), getThemedPaint(sideButtonPressed ? Theme.key_paint_chatActionBackgroundSelected : Theme.key_paint_chatActionBackground));
                if (hasGradientService()) {
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), Theme.chat_actionBackgroundGradientDarkenPaint);
                }

                if (drawSideButton == 2) {
                    Drawable goIconDrawable = getThemedDrawable(Theme.key_drawable_goIcon);
                    if (currentMessageObject.isOutOwner()) {
                        setDrawableBounds(goIconDrawable, sideStartX + AndroidUtilities.dp(10), sideStartY + AndroidUtilities.dp(9));
                        canvas.save();
                        canvas.scale(-1, 1, goIconDrawable.getBounds().centerX(), goIconDrawable.getBounds().centerY());
                    } else {
                        setDrawableBounds(goIconDrawable, sideStartX + AndroidUtilities.dp(12), sideStartY + AndroidUtilities.dp(9));
                    }
                    goIconDrawable.draw(canvas);
                    if (currentMessageObject.isOutOwner()) {
                        canvas.restore();
                    }
                } else {
                    Drawable drawable = getThemedDrawable(Theme.key_drawable_shareIcon);
                    setDrawableBounds(drawable, sideStartX + AndroidUtilities.dp(8), sideStartY + AndroidUtilities.dp(9));
                    drawable.draw(canvas);
                }
            }
        }

        if (replyNameLayout != null) {
            if (currentMessageObject.shouldDrawWithoutBackground()) {
                if (currentMessageObject.isOutOwner()) {
                    replyStartX = AndroidUtilities.dp(23);
                    if (isPlayingRound) {
                        replyStartX -= (AndroidUtilities.roundPlayingMessageSize - AndroidUtilities.roundMessageSize);
                    }
                } else if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                    replyStartX = backgroundDrawableLeft + backgroundDrawableRight + AndroidUtilities.dp(4);
                } else {
                    replyStartX = backgroundDrawableLeft + backgroundDrawableRight + AndroidUtilities.dp(17);
                }
                replyStartY = AndroidUtilities.dp(12);
            } else {
                if (currentMessageObject.isOutOwner()) {
                    replyStartX = backgroundDrawableLeft + AndroidUtilities.dp(12) + getExtraTextX();
                } else {
                    if (mediaBackground) {
                        replyStartX = backgroundDrawableLeft + AndroidUtilities.dp(12) + getExtraTextX();
                    } else {
                        replyStartX = backgroundDrawableLeft + AndroidUtilities.dp(drawPinnedBottom ? 12 : 18) + getExtraTextX();
                    }
                }
                replyStartY = AndroidUtilities.dp(12 + (drawForwardedName && forwardedNameLayout[0] != null ? 36 : 0) + (drawNameLayout && nameLayout != null ? 20 : 0));
            }
        }
        if (currentPosition == null && !transitionParams.animateBackgroundBoundsInner && !(enterTransitionInPorgress && !currentMessageObject.isVoice())) {
            drawNamesLayout(canvas, 1f);
        }

        if ((!autoPlayingMedia || !MediaController.getInstance().isPlayingMessageAndReadyToDraw(currentMessageObject) || isRoundVideo) && !transitionParams.animateBackgroundBoundsInner) {
            drawOverlays(canvas);
        }
        if ((drawTime || !mediaBackground) && !forceNotDrawTime && !transitionParams.animateBackgroundBoundsInner && !(enterTransitionInPorgress && !currentMessageObject.isVoice())) {
            drawTime(canvas, 1f, false);
        }

        if ((controlsAlpha != 1.0f || timeAlpha != 1.0f) && currentMessageObject.type != 5) {
            long newTime = System.currentTimeMillis();
            long dt = Math.abs(lastControlsAlphaChangeTime - newTime);
            if (dt > 17) {
                dt = 17;
            }
            totalChangeTime += dt;
            if (totalChangeTime > 100) {
                totalChangeTime = 100;
            }
            lastControlsAlphaChangeTime = newTime;
            if (controlsAlpha != 1.0f) {
                controlsAlpha = AndroidUtilities.decelerateInterpolator.getInterpolation(totalChangeTime / 100.0f);
            }
            if (timeAlpha != 1.0f) {
                timeAlpha = AndroidUtilities.decelerateInterpolator.getInterpolation(totalChangeTime / 100.0f);
            }
            invalidate();
            if (forceNotDrawTime && currentPosition != null && currentPosition.last && getParent() != null) {
                View parent = (View) getParent();
                parent.invalidate();
            }
        }
        if (restore != Integer.MIN_VALUE) {
            canvas.restoreToCount(restore);
        }
        updateSelectionTextPosition();
    }

    @SuppressLint("WrongCall")
    public void drawBackgroundInternal(Canvas canvas, boolean fromParent) {
        if (currentMessageObject == null) {
            return;
        }
        if (!wasLayout && !animationRunning) {
            forceLayout();
            return;
        }
        if (!wasLayout) {
            onLayout(false, getLeft(), getTop(), getRight(), getBottom());
        }
        Drawable currentBackgroundShadowDrawable;
        int additionalTop = 0;
        int additionalBottom = 0;
        boolean forceMediaByGroup = currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0 && currentMessagesGroup.isDocuments && !drawPinnedBottom;
        if (currentMessageObject.isOutOwner()) {
            if (transitionParams.changePinnedBottomProgress >= 1 && !mediaBackground && !drawPinnedBottom && !forceMediaByGroup) {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOut);
                currentBackgroundSelectedDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOutSelected);
                transitionParams.drawPinnedBottomBackground = false;
            } else {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOutMedia);
                currentBackgroundSelectedDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOutMediaSelected);
                transitionParams.drawPinnedBottomBackground = true;
            }
            setBackgroundTopY(true);
            if (isDrawSelectionBackground() && (currentPosition == null || getBackground() != null)) {
                currentBackgroundShadowDrawable = currentBackgroundSelectedDrawable.getShadowDrawable();
            } else {
                currentBackgroundShadowDrawable = currentBackgroundDrawable.getShadowDrawable();
            }
            backgroundDrawableLeft = layoutWidth - backgroundWidth - (!mediaBackground ? 0 : AndroidUtilities.dp(9));
            backgroundDrawableRight = backgroundWidth - (mediaBackground ? 0 : AndroidUtilities.dp(3));
            if (currentMessagesGroup != null && !currentMessagesGroup.isDocuments) {
                if (!currentPosition.edge) {
                    backgroundDrawableRight += AndroidUtilities.dp(10);
                }
            }
            int backgroundLeft = backgroundDrawableLeft;
            if (!forceMediaByGroup && transitionParams.changePinnedBottomProgress != 1) {
                if (!mediaBackground) {
                    backgroundDrawableRight -= AndroidUtilities.dp(6);
                }
            } else if (!mediaBackground && drawPinnedBottom) {
                backgroundDrawableRight -= AndroidUtilities.dp(6);
            }

            if (currentPosition != null) {
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) == 0) {
                    backgroundDrawableRight += AndroidUtilities.dp(SharedConfig.bubbleRadius + 2);
                }
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                    backgroundLeft -= AndroidUtilities.dp(SharedConfig.bubbleRadius + 2);
                    backgroundDrawableRight += AndroidUtilities.dp(SharedConfig.bubbleRadius + 2);
                }
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                    additionalTop -= AndroidUtilities.dp(SharedConfig.bubbleRadius + 3);
                    additionalBottom += AndroidUtilities.dp(SharedConfig.bubbleRadius + 3);
                }
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                    additionalBottom += AndroidUtilities.dp(SharedConfig.bubbleRadius + 3);
                }
            }
            int offsetBottom;
            if (drawPinnedBottom && drawPinnedTop) {
                offsetBottom = 0;
            } else if (drawPinnedBottom) {
                offsetBottom = AndroidUtilities.dp(1);
            } else {
                offsetBottom = AndroidUtilities.dp(2);
            }
            backgroundDrawableTop = additionalTop + (drawPinnedTop ? 0 : AndroidUtilities.dp(1));
            int backgroundHeight = layoutHeight - offsetBottom + additionalBottom;
            backgroundDrawableBottom = backgroundDrawableTop + backgroundHeight;
            if (forceMediaByGroup) {
                setDrawableBoundsInner(currentBackgroundDrawable, backgroundLeft, backgroundDrawableTop - additionalTop, backgroundDrawableRight, backgroundHeight - additionalBottom + 10);
                setDrawableBoundsInner(currentBackgroundSelectedDrawable, backgroundDrawableLeft, backgroundDrawableTop, backgroundDrawableRight - AndroidUtilities.dp(6), backgroundHeight);
            } else {
                setDrawableBoundsInner(currentBackgroundDrawable, backgroundLeft, backgroundDrawableTop, backgroundDrawableRight, backgroundHeight);
                setDrawableBoundsInner(currentBackgroundSelectedDrawable, backgroundLeft, backgroundDrawableTop, backgroundDrawableRight, backgroundHeight);
            }
            setDrawableBoundsInner(currentBackgroundShadowDrawable, backgroundLeft, backgroundDrawableTop, backgroundDrawableRight, backgroundHeight);
        } else {
            if (transitionParams.changePinnedBottomProgress >= 1 && !mediaBackground && !drawPinnedBottom && !forceMediaByGroup) {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgIn);
                currentBackgroundSelectedDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgInSelected);
                transitionParams.drawPinnedBottomBackground = false;
            } else {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgInMedia);
                currentBackgroundSelectedDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgInMediaSelected);
                transitionParams.drawPinnedBottomBackground = true;
            }
            setBackgroundTopY(true);
            if (isDrawSelectionBackground() && (currentPosition == null || getBackground() != null)) {
                currentBackgroundShadowDrawable = currentBackgroundSelectedDrawable.getShadowDrawable();
            } else {
                currentBackgroundShadowDrawable = currentBackgroundDrawable.getShadowDrawable();
            }

            backgroundDrawableLeft = AndroidUtilities.dp((isChat && isAvatarVisible ? 48 : 0) + (!mediaBackground ? 3 : 9));
            backgroundDrawableRight = backgroundWidth - (mediaBackground ? 0 : AndroidUtilities.dp(3));
            if (currentMessagesGroup != null && !currentMessagesGroup.isDocuments) {
                if (!currentPosition.edge) {
                    backgroundDrawableLeft -= AndroidUtilities.dp(10);
                    backgroundDrawableRight += AndroidUtilities.dp(10);
                }
                if (currentPosition.leftSpanOffset != 0) {
                    backgroundDrawableLeft += (int) Math.ceil(currentPosition.leftSpanOffset / 1000.0f * getGroupPhotosWidth());
                }
            }
            if ((!mediaBackground && drawPinnedBottom) || !forceMediaByGroup && transitionParams.changePinnedBottomProgress != 1) {
                if (!(!drawPinnedBottom && mediaBackground)) {
                    backgroundDrawableRight -= AndroidUtilities.dp(6);
                }
                if (!mediaBackground) {
                    backgroundDrawableLeft += AndroidUtilities.dp(6);
                }

            }
            if (currentPosition != null) {
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) == 0) {
                    backgroundDrawableRight += AndroidUtilities.dp(SharedConfig.bubbleRadius + 2);
                }
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                    backgroundDrawableLeft -= AndroidUtilities.dp(SharedConfig.bubbleRadius + 2);
                    backgroundDrawableRight += AndroidUtilities.dp(SharedConfig.bubbleRadius + 2);
                }
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                    additionalTop -= AndroidUtilities.dp(SharedConfig.bubbleRadius + 3);
                    additionalBottom += AndroidUtilities.dp(SharedConfig.bubbleRadius + 3);
                }
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                    additionalBottom += AndroidUtilities.dp(SharedConfig.bubbleRadius + 4);
                }
            }
            int offsetBottom;
            if (drawPinnedBottom && drawPinnedTop) {
                offsetBottom = 0;
            } else if (drawPinnedBottom) {
                offsetBottom = AndroidUtilities.dp(1);
            } else {
                offsetBottom = AndroidUtilities.dp(2);
            }
            backgroundDrawableTop = additionalTop + (drawPinnedTop ? 0 : AndroidUtilities.dp(1));
            int backgroundHeight = layoutHeight - offsetBottom + additionalBottom;
            backgroundDrawableBottom = backgroundDrawableTop + backgroundHeight;
            setDrawableBoundsInner(currentBackgroundDrawable, backgroundDrawableLeft, backgroundDrawableTop, backgroundDrawableRight, backgroundHeight);
            if (forceMediaByGroup) {
                setDrawableBoundsInner(currentBackgroundSelectedDrawable, backgroundDrawableLeft + AndroidUtilities.dp(6), backgroundDrawableTop, backgroundDrawableRight - AndroidUtilities.dp(6), backgroundHeight);
            } else {
                setDrawableBoundsInner(currentBackgroundSelectedDrawable, backgroundDrawableLeft, backgroundDrawableTop, backgroundDrawableRight, backgroundHeight);
            }
            setDrawableBoundsInner(currentBackgroundShadowDrawable, backgroundDrawableLeft, backgroundDrawableTop, backgroundDrawableRight, backgroundHeight);
        }

        if (!currentMessageObject.isOutOwner() && transitionParams.changePinnedBottomProgress != 1 && !mediaBackground && !drawPinnedBottom) {
            backgroundDrawableLeft -= AndroidUtilities.dp(6);
            backgroundDrawableRight += AndroidUtilities.dp(6);
        }

        if (hasPsaHint) {
            int x;
            if (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_RIGHT) != 0) {
                x = currentBackgroundDrawable.getBounds().right;
            } else {
                x = 0;
                int dWidth = getGroupPhotosWidth();
                for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                    MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                    if (position.minY == 0) {
                        x += Math.ceil((position.pw + position.leftSpanOffset) / 1000.0f * dWidth);
                    } else {
                        break;
                    }
                }
            }
            Drawable drawable = Theme.chat_psaHelpDrawable[currentMessageObject.isOutOwner() ? 1 : 0];

            int y;
            if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                y = AndroidUtilities.dp(12);
            } else {
                y = AndroidUtilities.dp(10 + (drawNameLayout ? 19 : 0));
            }

            psaHelpX = x - drawable.getIntrinsicWidth() - AndroidUtilities.dp(currentMessageObject.isOutOwner() ? 20 : 14);
            psaHelpY = y + AndroidUtilities.dp(4);
        }

        if (checkBoxVisible || checkBoxAnimationInProgress) {
            if (checkBoxVisible && checkBoxAnimationProgress == 1.0f || !checkBoxVisible && checkBoxAnimationProgress == 0.0f) {
                checkBoxAnimationInProgress = false;
            }
            Interpolator interpolator = checkBoxVisible ? CubicBezierInterpolator.EASE_OUT : CubicBezierInterpolator.EASE_IN;
            checkBoxTranslation = (int) Math.ceil(interpolator.getInterpolation(checkBoxAnimationProgress) * AndroidUtilities.dp(35));
            if (!currentMessageObject.isOutOwner()) {
                updateTranslation();
            }

            int size = AndroidUtilities.dp(21);
            checkBox.setBounds(AndroidUtilities.dp(8 - 35) + checkBoxTranslation, currentBackgroundDrawable.getBounds().bottom - AndroidUtilities.dp(8) - size, size, size);

            if (checkBoxAnimationInProgress) {
                long newTime = SystemClock.elapsedRealtime();
                long dt = newTime - lastCheckBoxAnimationTime;
                lastCheckBoxAnimationTime = newTime;

                if (checkBoxVisible) {
                    checkBoxAnimationProgress += dt / 200.0f;
                    if (checkBoxAnimationProgress > 1.0f) {
                        checkBoxAnimationProgress = 1.0f;
                    }
                } else {
                    checkBoxAnimationProgress -= dt / 200.0f;
                    if (checkBoxAnimationProgress <= 0.0f) {
                        checkBoxAnimationProgress = 0.0f;
                    }
                }
                invalidate();
                ((View) getParent()).invalidate();
            }
        }

        if (!fromParent && drawBackgroundInParent()) {
            return;
        }

        boolean needRestore = false;
        if (transitionYOffsetForDrawables != 0) {
            needRestore = true;
            canvas.save();
            canvas.translate(0, transitionYOffsetForDrawables);
        }

        if (drawBackground && currentBackgroundDrawable != null && (currentPosition == null || isDrawSelectionBackground() && (currentMessageObject.isMusic() || currentMessageObject.isDocument())) && !(enterTransitionInPorgress && !currentMessageObject.isVoice())) {
            float alphaInternal = this.alphaInternal;
            if (fromParent) {
                alphaInternal *= getAlpha();
            }
            if (isHighlightedAnimated) {
                currentBackgroundDrawable.setAlpha((int) (255 * alphaInternal));
                currentBackgroundDrawable.drawCached(canvas, backgroundCacheParams);
                float alpha = highlightProgress >= 300 ? 1.0f : highlightProgress / 300.0f;
                currentSelectedBackgroundAlpha = alpha;
                if (currentPosition == null) {
                    currentBackgroundSelectedDrawable.setAlpha((int) (alphaInternal * alpha * 255));
                    currentBackgroundSelectedDrawable.drawCached(canvas, backgroundCacheParams);
                }
            } else if (selectedBackgroundProgress != 0 && !(currentMessagesGroup != null && currentMessagesGroup.isDocuments)) {
                currentBackgroundDrawable.drawCached(canvas, backgroundCacheParams);
                currentSelectedBackgroundAlpha = selectedBackgroundProgress;
                currentBackgroundSelectedDrawable.setAlpha((int) (selectedBackgroundProgress * alphaInternal * 255));
                currentBackgroundSelectedDrawable.drawCached(canvas, backgroundCacheParams);
                if (currentBackgroundDrawable.getGradientShader() == null) {
                    currentBackgroundShadowDrawable = null;
                }
            } else {
                if (isDrawSelectionBackground() && (currentPosition == null || currentMessageObject.isMusic() || currentMessageObject.isDocument() || getBackground() != null)) {
                    if (currentPosition != null) {
                        canvas.save();
                        canvas.clipRect(0, 0, getMeasuredWidth(), getMeasuredHeight());
                    }
                    currentSelectedBackgroundAlpha = 1f;
                    currentBackgroundSelectedDrawable.setAlpha((int) (255 * alphaInternal));
                    currentBackgroundSelectedDrawable.drawCached(canvas, backgroundCacheParams);
                    if (currentPosition != null) {
                        canvas.restore();
                    }
                } else {
                    currentSelectedBackgroundAlpha = 0;
                    currentBackgroundDrawable.setAlpha((int) (255 * alphaInternal));
                    currentBackgroundDrawable.drawCached(canvas, backgroundCacheParams);
                }
            }
            if (currentBackgroundShadowDrawable != null && currentPosition == null) {
                currentBackgroundShadowDrawable.setAlpha((int) (255 * alphaInternal));
                currentBackgroundShadowDrawable.draw(canvas);
            }

            if (transitionParams.changePinnedBottomProgress != 1f && currentPosition == null) {
                if (currentMessageObject.isOutOwner()) {
                    Theme.MessageDrawable drawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOut);

                    Rect rect = currentBackgroundDrawable.getBounds();
                    drawable.setBounds(rect.left, rect.top, rect.right + AndroidUtilities.dp(6), rect.bottom);
                    canvas.save();
                    canvas.clipRect(rect.right - AndroidUtilities.dp(12), rect.bottom - AndroidUtilities.dp(16), rect.right + AndroidUtilities.dp(12), rect.bottom);
                    int w = parentWidth;
                    int h = parentHeight;
                    if (h == 0) {
                        w = getParentWidth();
                        h = AndroidUtilities.displaySize.y;
                        if (getParent() instanceof View) {
                            View view = (View) getParent();
                            w = view.getMeasuredWidth();
                            h = view.getMeasuredHeight();
                        }
                    }
                    drawable.setTop((int) (getY() + parentViewTopOffset), w, h, (int) parentViewTopOffset, pinnedTop, pinnedBottom);
                    float alpha = !mediaBackground && !pinnedBottom ? transitionParams.changePinnedBottomProgress : (1f - transitionParams.changePinnedBottomProgress);
                    drawable.setAlpha((int) (255 * alpha));
                    drawable.draw(canvas);
                    drawable.setAlpha(255);
                    canvas.restore();
                } else {
                    Theme.MessageDrawable drawable;
                    if (transitionParams.drawPinnedBottomBackground) {
                        drawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgIn);
                    } else {
                        drawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgInMedia);
                    }
                    float alpha = !mediaBackground && !pinnedBottom ? transitionParams.changePinnedBottomProgress : (1f - transitionParams.changePinnedBottomProgress);
                    drawable.setAlpha((int) (255 * alpha));
                    Rect rect = currentBackgroundDrawable.getBounds();
                    drawable.setBounds(rect.left - AndroidUtilities.dp(6), rect.top, rect.right, rect.bottom);
                    canvas.save();
                    canvas.clipRect(rect.left - AndroidUtilities.dp(6), rect.bottom - AndroidUtilities.dp(16), rect.left + AndroidUtilities.dp(6), rect.bottom);
                    drawable.draw(canvas);
                    drawable.setAlpha(255);
                    canvas.restore();
                }
            }
        }
        if (needRestore) {
            canvas.restore();
        }
    }

    public boolean drawBackgroundInParent() {
        if (canDrawBackgroundInParent && currentMessageObject != null && currentMessageObject.isOutOwner()) {
            if (resourcesProvider != null) {
                return resourcesProvider.getCurrentColor(Theme.key_chat_outBubbleGradient1) != null;
            } else {
                return Theme.getColorOrNull(Theme.key_chat_outBubbleGradient1) != null;
            }
        }
        return false;
    }

    public void drawCommentButton(Canvas canvas, float alpha) {
        if (drawSideButton != 3) {
            return;
        }
        int height = AndroidUtilities.dp(32);
        if (commentLayout != null) {
            sideStartY -= AndroidUtilities.dp(18);
            height += AndroidUtilities.dp(18);
        }

        rect.set(sideStartX, sideStartY, sideStartX + AndroidUtilities.dp(32), sideStartY + height);
        applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
        if (alpha != 1f) {
            int oldAlpha = getThemedPaint(Theme.key_paint_chatActionBackground).getAlpha();
            getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha((int) (alpha * oldAlpha));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), getThemedPaint(Theme.key_paint_chatActionBackground));
            getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha(oldAlpha);
        } else {
            canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), getThemedPaint(sideButtonPressed ? Theme.key_paint_chatActionBackgroundSelected : Theme.key_paint_chatActionBackground));
        }
        if (hasGradientService()) {
            if (alpha != 1f) {
                int oldAlpha = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (alpha * oldAlpha));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), Theme.chat_actionBackgroundGradientDarkenPaint);
                Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(oldAlpha);
            } else {
                canvas.drawRoundRect(rect, AndroidUtilities.dp(16), AndroidUtilities.dp(16), Theme.chat_actionBackgroundGradientDarkenPaint);
            }
        }

        Drawable commentStickerDrawable = Theme.getThemeDrawable(Theme.key_drawable_commentSticker);
        setDrawableBounds(commentStickerDrawable, sideStartX + AndroidUtilities.dp(4), sideStartY + AndroidUtilities.dp(4));
        if (alpha != 1f) {
            commentStickerDrawable.setAlpha((int) (255 * alpha));
            commentStickerDrawable.draw(canvas);
            commentStickerDrawable.setAlpha(255);
        } else {
            commentStickerDrawable.draw(canvas);
        }

        if (commentLayout != null) {
            Theme.chat_stickerCommentCountPaint.setColor(getThemedColor(Theme.key_chat_stickerReplyNameText));
            Theme.chat_stickerCommentCountPaint.setAlpha((int) (255 * alpha));
            if (transitionParams.animateComments) {
                if (transitionParams.animateCommentsLayout != null) {
                    canvas.save();
                    Theme.chat_stickerCommentCountPaint.setAlpha((int) (255 * (1.0 - transitionParams.animateChangeProgress) * alpha));
                    canvas.translate(sideStartX + (AndroidUtilities.dp(32) - transitionParams.animateTotalCommentWidth) / 2, sideStartY + AndroidUtilities.dp(30));
                    transitionParams.animateCommentsLayout.draw(canvas);
                    canvas.restore();
                }
                Theme.chat_stickerCommentCountPaint.setAlpha((int) (255 * transitionParams.animateChangeProgress));
            }
            canvas.save();
            canvas.translate(sideStartX + (AndroidUtilities.dp(32) - totalCommentWidth) / 2, sideStartY + AndroidUtilities.dp(30));
            commentLayout.draw(canvas);
            canvas.restore();
        }
    }

    private void applyServiceShaderMatrix(int measuredWidth, int backgroundHeight, float x, float viewTop) {
        if (resourcesProvider != null) {
            resourcesProvider.applyServiceShaderMatrix(measuredWidth, backgroundHeight, x, viewTop);
        } else {
            Theme.applyServiceShaderMatrix(measuredWidth, backgroundHeight, x, viewTop);
        }
    }

    public void drawOutboundsContent(Canvas canvas) {
        if (transitionParams.animateBackgroundBoundsInner) {
            if (!transitionParams.transitionBotButtons.isEmpty()) {
                drawBotButtons(canvas, transitionParams.transitionBotButtons, 1f - transitionParams.animateChangeProgress);
            }
            if (!botButtons.isEmpty()) {
                drawBotButtons(canvas, botButtons, transitionParams.animateChangeProgress);
            }
        }
    }

    public void setTimeAlpha(float value) {
        timeAlpha = value;
    }

    public float getTimeAlpha() {
        return timeAlpha;
    }

    public int getBackgroundDrawableLeft() {
        if (currentMessageObject.isOutOwner()) {
            return layoutWidth - backgroundWidth - (!mediaBackground ? 0 : AndroidUtilities.dp(9));
        } else {
            int r = AndroidUtilities.dp((isChat && isAvatarVisible ? 48 : 0) + (!mediaBackground ? 3 : 9));
            if (currentMessagesGroup != null && !currentMessagesGroup.isDocuments) {
                if (currentPosition.leftSpanOffset != 0) {
                    r += (int) Math.ceil(currentPosition.leftSpanOffset / 1000.0f * getGroupPhotosWidth());
                }
            }
            if ((!mediaBackground && drawPinnedBottom)) {
                r += AndroidUtilities.dp(6);
            }
            return r;
        }
    }

    public int getBackgroundDrawableRight() {
        int right = backgroundWidth - (mediaBackground ? 0 : AndroidUtilities.dp(3));
        if (!mediaBackground && drawPinnedBottom && currentMessageObject.isOutOwner()) {
            right -= AndroidUtilities.dp(6);
        }
        if (!mediaBackground && drawPinnedBottom && !currentMessageObject.isOutOwner()) {
            right -= AndroidUtilities.dp(6);
        }
        return getBackgroundDrawableLeft() + right;
    }

    public int getBackgroundDrawableTop() {
        int additionalTop = 0;
        if (currentPosition != null) {
            if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                additionalTop -= AndroidUtilities.dp( 3);
            }
        }
        return additionalTop + (drawPinnedTop ? 0 : AndroidUtilities.dp(1));
    }

    public int getBackgroundDrawableBottom() {
        int additionalBottom = 0;
        if (currentPosition != null) {
            if ((currentPosition.flags & MessageObject.POSITION_FLAG_TOP) == 0) {
                additionalBottom += AndroidUtilities.dp(3);
            }
            if ((currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0) {
                additionalBottom += AndroidUtilities.dp((currentMessageObject.isOutOwner() ? 3 : 4));
            }
        }

        int offsetBottom;
        if (drawPinnedBottom && drawPinnedTop) {
            offsetBottom = 0;
        } else if (drawPinnedBottom) {
            offsetBottom = AndroidUtilities.dp(1);
        } else {
            offsetBottom = AndroidUtilities.dp(2);
        }
        return getBackgroundDrawableTop() + layoutHeight - offsetBottom + additionalBottom;
    }

    public void drawBackground(Canvas canvas, int left, int top, int right, int bottom, boolean pinnedTop, boolean pinnedBottom, boolean selected, int keyboardHeight) {
        if (currentMessageObject.isOutOwner()) {
            if (!mediaBackground && !pinnedBottom) {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(selected ? Theme.key_drawable_msgOutSelected : Theme.key_drawable_msgOut);
            } else {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(selected ? Theme.key_drawable_msgOutMediaSelected : Theme.key_drawable_msgOutMedia);
            }
        } else {
            if (!mediaBackground && !pinnedBottom) {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(selected ? Theme.key_drawable_msgInSelected : Theme.key_drawable_msgIn);
            } else {
                currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(selected ? Theme.key_drawable_msgInMediaSelected : Theme.key_drawable_msgInMedia);
            }
        }

        int w = parentWidth;
        int h = parentHeight;
        if (h == 0) {
            w = getParentWidth();
            h = AndroidUtilities.displaySize.y;
            if (getParent() instanceof View) {
                View view = (View) getParent();
                w = view.getMeasuredWidth();
                h = view.getMeasuredHeight();
            }
        }

        if (currentBackgroundDrawable != null) {
            currentBackgroundDrawable.setTop(keyboardHeight, w, h, (int) parentViewTopOffset, pinnedTop, pinnedBottom);
            Drawable currentBackgroundShadowDrawable = currentBackgroundDrawable.getShadowDrawable();
            if (currentBackgroundShadowDrawable != null) {
                currentBackgroundShadowDrawable.setAlpha((int) (getAlpha() * 255));
                currentBackgroundShadowDrawable.setBounds(left, top, right, bottom);
                currentBackgroundShadowDrawable.draw(canvas);
                currentBackgroundShadowDrawable.setAlpha(255);
            }
            currentBackgroundDrawable.setAlpha((int) (getAlpha() * 255));
            currentBackgroundDrawable.setBounds(left, top, right, bottom);
            currentBackgroundDrawable.drawCached(canvas, backgroundCacheParams);
            currentBackgroundDrawable.setAlpha(255);
        }
    }

    public boolean hasNameLayout() {
        return drawNameLayout && nameLayout != null ||
                drawForwardedName && forwardedNameLayout[0] != null && forwardedNameLayout[1] != null && (currentPosition == null || currentPosition.minY == 0 && currentPosition.minX == 0) ||
                replyNameLayout != null;
    }

    public boolean isDrawNameLayout() {
        return drawNameLayout && nameLayout != null;
    }

    public boolean isAdminLayoutChanged() {
        return !TextUtils.equals(lastPostAuthor, currentMessageObject.messageOwner.post_author);
    }

    public void drawNamesLayout(Canvas canvas, float alpha) {
        long newAnimationTime = SystemClock.elapsedRealtime();
        long dt = newAnimationTime - lastNamesAnimationTime;
        if (dt > 17) {
            dt = 17;
        }
        lastNamesAnimationTime = newAnimationTime;

        if (currentMessageObject.deleted && currentMessagesGroup != null && currentMessagesGroup.messages.size() >= 1) {
            return;
        }

        int restore = Integer.MIN_VALUE;
        if (alpha != 1f) {
            rect.set(0, 0, getMaxNameWidth(), getMeasuredHeight());
            restore = canvas.saveLayerAlpha(rect, (int) (255 * alpha), Canvas.ALL_SAVE_FLAG);
        }

        if (drawNameLayout && nameLayout != null) {
            canvas.save();

            int oldAlpha;

            if (currentMessageObject.shouldDrawWithoutBackground()) {
                Theme.chat_namePaint.setColor(getThemedColor(Theme.key_chat_stickerNameText));
                if (currentMessageObject.isOutOwner()) {
                    nameX = AndroidUtilities.dp(28);
                } else {
                    nameX = backgroundDrawableLeft + transitionParams.deltaLeft + backgroundDrawableRight + AndroidUtilities.dp(22);
                }
                nameY = layoutHeight - AndroidUtilities.dp(38);
                float alphaProgress = currentMessageObject.isOut() && (checkBoxVisible || checkBoxAnimationInProgress) ? (1.0f - checkBoxAnimationProgress) : 1.0f;

                rect.set((int) nameX - AndroidUtilities.dp(12), (int) nameY - AndroidUtilities.dp(5), (int) nameX + AndroidUtilities.dp(12) + nameWidth, (int) nameY + AndroidUtilities.dp(22));
                oldAlpha = getThemedPaint(Theme.key_paint_chatActionBackground).getAlpha();
                getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha((int) (alphaProgress * oldAlpha));
                applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
                canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), getThemedPaint(Theme.key_paint_chatActionBackground));
                if (hasGradientService()) {
                    int oldAlpha2 = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (oldAlpha2 * timeAlpha));
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Theme.chat_actionBackgroundGradientDarkenPaint);
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(oldAlpha2);
                }

                if (viaSpan1 != null || viaSpan2 != null) {
                    int color = getThemedColor(Theme.key_chat_stickerViaBotNameText);
                    color = (getThemedColor(Theme.key_chat_stickerViaBotNameText) & 0x00ffffff) | ((int) (Color.alpha(color) * alphaProgress) << 24);
                    if (viaSpan1 != null) {
                        viaSpan1.setColor(color);
                    }
                    if (viaSpan2 != null) {
                        viaSpan2.setColor(color);
                    }
                }
                nameX -= nameOffsetX;
                getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha(oldAlpha);
            } else {
                if (mediaBackground || currentMessageObject.isOutOwner()) {
                    nameX = backgroundDrawableLeft + transitionParams.deltaLeft + AndroidUtilities.dp(11) - nameOffsetX + getExtraTextX();
                } else {
                    nameX = backgroundDrawableLeft + transitionParams.deltaLeft + AndroidUtilities.dp(!mediaBackground && drawPinnedBottom ? 11 : 17) - nameOffsetX + getExtraTextX();
                }
                if (currentUser != null) {
                    if (currentBackgroundDrawable != null && currentBackgroundDrawable.hasGradient()) {
                        Theme.chat_namePaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
                    } else {
                        Theme.chat_namePaint.setColor(getThemedColor(AvatarDrawable.getNameColorNameForId(currentUser.id)));
                    }
                } else if (currentChat != null) {
                    if (ChatObject.isChannel(currentChat) && !currentChat.megagroup) {
                        Theme.chat_namePaint.setColor(Theme.changeColorAccent(getThemedColor(AvatarDrawable.getNameColorNameForId(5))));
                    } else if (currentMessageObject.isOutOwner()) {
                        Theme.chat_namePaint.setColor(getThemedColor(Theme.key_chat_outForwardedNameText));
                    } else {
                        Theme.chat_namePaint.setColor(getThemedColor(AvatarDrawable.getNameColorNameForId(currentChat.id)));
                    }
                } else {
                    Theme.chat_namePaint.setColor(getThemedColor(AvatarDrawable.getNameColorNameForId(0)));
                }
                nameY = AndroidUtilities.dp(drawPinnedTop ? 9 : 10);
                if (viaSpan1 != null || viaSpan2 != null) {
                    int color = getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outViaBotNameText : Theme.key_chat_inViaBotNameText);
                    if (viaSpan1 != null) {
                        viaSpan1.setColor(color);
                    }
                    if (viaSpan2 != null) {
                        viaSpan2.setColor(color);
                    }
                }
            }
            if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
                nameX += currentMessagesGroup.transitionParams.offsetLeft;
                nameY += currentMessagesGroup.transitionParams.offsetTop - getTranslationY();
            }
            nameX += animationOffsetX;
            nameY += transitionParams.deltaTop;
            float nx;
            if (transitionParams.animateSign) {
                nx = transitionParams.animateNameX + (nameX - transitionParams.animateNameX) * transitionParams.animateChangeProgress;
            } else {
                nx = nameX;
            }
            canvas.translate(nx, nameY);
            nameLayout.draw(canvas);
            canvas.restore();
            if (adminLayout != null) {
                int color;
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    color = getThemedColor(Theme.key_chat_stickerReplyNameText);
                } else if (currentMessageObject.isOutOwner()) {
                    color = getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outAdminSelectedText : Theme.key_chat_outAdminText);
                } else {
                    color = getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inAdminSelectedText : Theme.key_chat_inAdminText);
                }
                Theme.chat_adminPaint.setColor(color);
                canvas.save();
                float ax;
                if (currentMessagesGroup != null && !currentMessagesGroup.isDocuments) {
                    int dWidth = getGroupPhotosWidth();
                    int firstLineWidth = 0;
                    for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                        MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                        if (position.minY == 0) {
                            firstLineWidth += Math.ceil((position.pw + position.leftSpanOffset) / 1000.0f * dWidth);
                        } else {
                            break;
                        }
                    }
                    if (!mediaBackground && currentMessageObject.isOutOwner()) {
                        ax = backgroundDrawableLeft + firstLineWidth - AndroidUtilities.dp(17) - adminLayout.getLineWidth(0);
                    } else {
                        ax = backgroundDrawableLeft + firstLineWidth - AndroidUtilities.dp(11) - adminLayout.getLineWidth(0);
                    }
                    ax -= getExtraTextX() + AndroidUtilities.dp(8);
                    if (!currentMessageObject.isOutOwner()) {
                        ax -= AndroidUtilities.dp(48);
                    }
                } else {
                    if (currentMessageObject.shouldDrawWithoutBackground()) {
                        if (currentMessageObject.isOutOwner()) {
                            ax = AndroidUtilities.dp(28) + nameWidth - adminLayout.getLineWidth(0);
                        } else {
                            ax = backgroundDrawableLeft + transitionParams.deltaLeft + backgroundDrawableRight + AndroidUtilities.dp(22) + nameWidth - adminLayout.getLineWidth(0);
                        }

                    } else if (!mediaBackground && currentMessageObject.isOutOwner()) {
                        ax = backgroundDrawableLeft + backgroundDrawableRight - AndroidUtilities.dp(17) - adminLayout.getLineWidth(0);
                    } else {
                        ax = backgroundDrawableLeft + backgroundDrawableRight - AndroidUtilities.dp(11) - adminLayout.getLineWidth(0);
                    }
                }
                canvas.translate(ax, nameY + AndroidUtilities.dp(0.5f));
                if (transitionParams.animateSign) {
                    Theme.chat_adminPaint.setAlpha((int) (Color.alpha(color) * transitionParams.animateChangeProgress));
                }
                adminLayout.draw(canvas);
                canvas.restore();
            }
        }

        boolean drawForwardedNameLocal = drawForwardedName;
        StaticLayout[] forwardedNameLayoutLocal = forwardedNameLayout;
        float animatingAlpha = 1f;
        int forwardedNameWidthLocal = forwardedNameWidth;
        if (transitionParams.animateForwardedLayout) {
            if (!currentMessageObject.needDrawForwarded()) {
                drawForwardedNameLocal = true;
                forwardedNameLayoutLocal = transitionParams.animatingForwardedNameLayout;
                animatingAlpha = 1f - transitionParams.animateChangeProgress;
                forwardedNameWidthLocal = transitionParams.animateForwardNameWidth;
            } else {
                animatingAlpha = transitionParams.animateChangeProgress;
            }
        }

        float forwardNameXLocal;
        if (drawForwardedNameLocal && forwardedNameLayoutLocal[0] != null && forwardedNameLayoutLocal[1] != null && (currentPosition == null || currentPosition.minY == 0 && currentPosition.minX == 0)) {
            if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO || currentMessageObject.isAnyKindOfSticker()) {
                Theme.chat_forwardNamePaint.setColor(getThemedColor(Theme.key_chat_stickerReplyNameText));
                if (currentMessageObject.needDrawForwarded()) {
                    if (currentMessageObject.isOutOwner()) {
                        forwardNameXLocal = forwardNameX = AndroidUtilities.dp(23);
                    } else {
                        forwardNameXLocal = forwardNameX = backgroundDrawableLeft + backgroundDrawableRight + AndroidUtilities.dp(17);
                    }
                } else {
                    forwardNameXLocal = transitionParams.animateForwardNameX;
                }
                if (currentMessageObject.isOutOwner() && currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO && transitionParams.animatePlayingRound || isPlayingRound) {
                    forwardNameXLocal -= AndroidUtilities.dp (78) * (isPlayingRound ? transitionParams.animateChangeProgress : (1f - transitionParams.animateChangeProgress));
                }
                forwardNameY = AndroidUtilities.dp(12);

                int backWidth = forwardedNameWidthLocal + AndroidUtilities.dp(14);

                rect.set((int) forwardNameXLocal - AndroidUtilities.dp(7), forwardNameY - AndroidUtilities.dp(6), (int) forwardNameXLocal - AndroidUtilities.dp(7) + backWidth, forwardNameY + AndroidUtilities.dp(38));
                applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
                int oldAlpha1 = -1, oldAlpha2 = -1;
                if (animatingAlpha != 1f) {
                    oldAlpha1 = getThemedPaint(Theme.key_paint_chatActionBackground).getAlpha();
                    getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha((int) (oldAlpha1 * animatingAlpha));
                }
                canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), getThemedPaint(Theme.key_paint_chatActionBackground));
                if (hasGradientService()) {
                    if (animatingAlpha != 1f) {
                        oldAlpha2 = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                        Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (oldAlpha2 * animatingAlpha));
                    }
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Theme.chat_actionBackgroundGradientDarkenPaint);
                }
                if (oldAlpha1 >= 0) {
                    getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha(oldAlpha1);
                }
                if (oldAlpha2 >= 0) {
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(oldAlpha2);
                }
            } else {
                forwardNameY = AndroidUtilities.dp(10 + (drawNameLayout ? 19 : 0));
                if (currentMessageObject.isOutOwner()) {
                    if (hasPsaHint) {
                        Theme.chat_forwardNamePaint.setColor(getThemedColor(Theme.key_chat_outPsaNameText));
                    } else {
                        Theme.chat_forwardNamePaint.setColor(getThemedColor(Theme.key_chat_outForwardedNameText));
                    }
                    if (currentMessageObject.needDrawForwarded()) {
                        forwardNameXLocal = forwardNameX = backgroundDrawableLeft + AndroidUtilities.dp(11) + getExtraTextX();
                    } else {
                        forwardNameXLocal = transitionParams.animateForwardNameX;
                    }
                } else {
                    if (hasPsaHint) {
                        Theme.chat_forwardNamePaint.setColor(getThemedColor(Theme.key_chat_inPsaNameText));
                    } else {
                        Theme.chat_forwardNamePaint.setColor(getThemedColor(Theme.key_chat_inForwardedNameText));
                    }
                    if (currentMessageObject.needDrawForwarded()) {
                        if (mediaBackground) {
                            forwardNameXLocal = forwardNameX = backgroundDrawableLeft + AndroidUtilities.dp(11) + getExtraTextX();
                        } else {
                            forwardNameXLocal = forwardNameX = backgroundDrawableLeft + AndroidUtilities.dp(drawPinnedBottom ? 11 : 17) + getExtraTextX();
                        }
                    } else {
                        forwardNameXLocal = transitionParams.animateForwardNameX;
                    }
                }
            }
            boolean clipContent = false;
            if (transitionParams.animateForwardedLayout) {
                if (currentBackgroundDrawable != null && currentMessagesGroup == null && currentMessageObject.type != MessageObject.TYPE_ROUND_VIDEO && !currentMessageObject.isAnyKindOfSticker()) {
                    Rect r = currentBackgroundDrawable.getBounds();
                    canvas.save();
                    if (currentMessageObject.isOutOwner() && !mediaBackground && !pinnedBottom) {
                        canvas.clipRect(
                                r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                                r.right - AndroidUtilities.dp(10), r.bottom - AndroidUtilities.dp(4)
                        );
                    } else {
                        canvas.clipRect(
                                r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                                r.right - AndroidUtilities.dp(4), r.bottom - AndroidUtilities.dp(4)
                        );
                    }
                    clipContent = true;
                }
            }

            for (int a = 0; a < 2; a++) {
                canvas.save();
                canvas.translate(forwardNameXLocal - forwardNameOffsetX[a], forwardNameY + AndroidUtilities.dp(16) * a);
                if (animatingAlpha != 1f) {
                    int oldAlpha = forwardedNameLayoutLocal[a].getPaint().getAlpha();
                    forwardedNameLayoutLocal[a].getPaint().setAlpha((int) (oldAlpha * animatingAlpha));
                    forwardedNameLayoutLocal[a].draw(canvas);
                    forwardedNameLayoutLocal[a].getPaint().setAlpha(oldAlpha);
                } else {
                    forwardedNameLayoutLocal[a].draw(canvas);
                }
                canvas.restore();
            }
            if (clipContent) {
                canvas.restore();
            }

            if (hasPsaHint) {
                if (psaButtonVisible || psaButtonProgress > 0) {
                    Drawable drawable = Theme.chat_psaHelpDrawable[currentMessageObject.isOutOwner() ? 1 : 0];
                    int cx = psaHelpX + drawable.getIntrinsicWidth() / 2;
                    int cy = psaHelpY + drawable.getIntrinsicHeight() / 2;
                    float scale = psaButtonVisible && psaButtonProgress < 1 ? AnimationProperties.overshootInterpolator.getInterpolation(psaButtonProgress) : psaButtonProgress;
                    int w = (int) (drawable.getIntrinsicWidth() * scale);
                    int h = (int) (drawable.getIntrinsicHeight() * scale);
                    drawable.setBounds(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
                    drawable.draw(canvas);

                    if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null && selectorDrawableMaskType[0] == 3) {
                        canvas.save();
                        canvas.scale(psaButtonProgress, psaButtonProgress, selectorDrawable[0].getBounds().centerX(), selectorDrawable[0].getBounds().centerY());
                        selectorDrawable[0].draw(canvas);
                        canvas.restore();
                    }
                }
                if (psaButtonVisible && psaButtonProgress < 1.0f) {
                    psaButtonProgress += dt / 180.0f;
                    invalidate();
                    if (psaButtonProgress > 1.0f) {
                        psaButtonProgress = 1.0f;
                        setInvalidatesParent(false);
                    }
                } else if (!psaButtonVisible && psaButtonProgress > 0.0f) {
                    psaButtonProgress -= dt / 180.0f;
                    invalidate();
                    if (psaButtonProgress < 0.0f) {
                        psaButtonProgress = 0.0f;
                        setInvalidatesParent(false);
                    }
                }
            }
        }

        if (replyNameLayout != null) {
            float replyStartX = this.replyStartX;
            if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
                replyStartX += currentMessagesGroup.transitionParams.offsetLeft;
            }
            if (transitionParams.animateBackgroundBoundsInner) {
                if (isRoundVideo) {
                    replyStartX += transitionParams.deltaLeft + transitionParams.deltaRight;
                } else {
                    replyStartX += transitionParams.deltaLeft;
                }
            }
            if (currentMessageObject.shouldDrawWithoutBackground()) {
                Theme.chat_replyLinePaint.setColor(getThemedColor(Theme.key_chat_stickerReplyLine));
                int oldAlpha = Theme.chat_replyLinePaint.getAlpha();
                Theme.chat_replyLinePaint.setAlpha((int) (oldAlpha * timeAlpha));
                Theme.chat_replyNamePaint.setColor(getThemedColor(Theme.key_chat_stickerReplyNameText));
                oldAlpha = Theme.chat_replyNamePaint.getAlpha();
                Theme.chat_replyNamePaint.setAlpha((int) (oldAlpha * timeAlpha));
                Theme.chat_replyTextPaint.setColor(getThemedColor(Theme.key_chat_stickerReplyMessageText));
                oldAlpha = Theme.chat_replyTextPaint.getAlpha();
                Theme.chat_replyTextPaint.setAlpha((int) (oldAlpha * timeAlpha));
                int backWidth = Math.max(replyNameWidth, replyTextWidth) + AndroidUtilities.dp(14);

                rect.set((int) replyStartX - AndroidUtilities.dp(7), replyStartY - AndroidUtilities.dp(6), (int) replyStartX - AndroidUtilities.dp(7) + backWidth, replyStartY + AndroidUtilities.dp(41));
                applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
                oldAlpha = getThemedPaint(Theme.key_paint_chatActionBackground).getAlpha();
                getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha((int) (oldAlpha * timeAlpha));
                canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), getThemedPaint(Theme.key_paint_chatActionBackground));
                getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha(oldAlpha);
                if (hasGradientService()) {
                    oldAlpha = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (oldAlpha * timeAlpha));
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Theme.chat_actionBackgroundGradientDarkenPaint);
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(oldAlpha);
                }
            } else {
                if (currentMessageObject.isOutOwner()) {
                    Theme.chat_replyLinePaint.setColor(getThemedColor(Theme.key_chat_outReplyLine));
                    Theme.chat_replyNamePaint.setColor(getThemedColor(Theme.key_chat_outReplyNameText));
                    if (currentMessageObject.hasValidReplyMessageObject() && (currentMessageObject.replyMessageObject.type == 0 || !TextUtils.isEmpty(currentMessageObject.replyMessageObject.caption)) && !(currentMessageObject.replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame || currentMessageObject.replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice)) {
                        Theme.chat_replyTextPaint.setColor(getThemedColor(Theme.key_chat_outReplyMessageText));
                    } else {
                        Theme.chat_replyTextPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outReplyMediaMessageSelectedText : Theme.key_chat_outReplyMediaMessageText));
                    }
                } else {
                    Theme.chat_replyLinePaint.setColor(getThemedColor(Theme.key_chat_inReplyLine));
                    Theme.chat_replyNamePaint.setColor(getThemedColor(Theme.key_chat_inReplyNameText));
                    if (currentMessageObject.hasValidReplyMessageObject() && (currentMessageObject.replyMessageObject.type == 0 || !TextUtils.isEmpty(currentMessageObject.replyMessageObject.caption)) && !(currentMessageObject.replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame || currentMessageObject.replyMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice)) {
                        Theme.chat_replyTextPaint.setColor(getThemedColor(Theme.key_chat_inReplyMessageText));
                    } else {
                        Theme.chat_replyTextPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inReplyMediaMessageSelectedText : Theme.key_chat_inReplyMediaMessageText));
                    }
                }
            }
            forwardNameX = replyStartX - replyTextOffset + AndroidUtilities.dp(10 + (needReplyImage ? 44 : 0));
            if ((currentPosition == null || currentPosition.minY == 0 && currentPosition.minX == 0) && !(enterTransitionInPorgress && !currentMessageObject.isVoice())) {
                canvas.drawRect(replyStartX, replyStartY, replyStartX + AndroidUtilities.dp(2), replyStartY + AndroidUtilities.dp(35), Theme.chat_replyLinePaint);
                if (needReplyImage) {
                    replyImageReceiver.setImageCoords(replyStartX + AndroidUtilities.dp(10), replyStartY, AndroidUtilities.dp(35), AndroidUtilities.dp(35));
                    replyImageReceiver.draw(canvas);
                }

                if (replyNameLayout != null) {
                    canvas.save();
                    canvas.translate(replyStartX - replyNameOffset + AndroidUtilities.dp(10 + (needReplyImage ? 44 : 0)), replyStartY);
                    replyNameLayout.draw(canvas);
                    canvas.restore();
                }
                if (replyTextLayout != null) {
                    canvas.save();
                    canvas.translate(forwardNameX, replyStartY + AndroidUtilities.dp(19));
                    replyTextLayout.draw(canvas);
                    canvas.restore();
                }
            }
        }

        if (restore != Integer.MIN_VALUE) {
            canvas.restoreToCount(restore);
        }
    }

    public boolean hasCaptionLayout() {
        return captionLayout != null;
    }

    public boolean hasCommentLayout() {
        return drawCommentButton;
    }

    public StaticLayout getCaptionLayout() {
        return captionLayout;
    }

    public void setDrawSelectionBackground(boolean value) {
        if (drawSelectionBackground != value) {
            drawSelectionBackground = value;
            invalidate();
        }
    }

    public boolean isDrawingSelectionBackground() {
        return drawSelectionBackground || isHighlightedAnimated || isHighlighted;
    }

    public float getHightlightAlpha() {
        if (!drawSelectionBackground && isHighlightedAnimated) {
            return highlightProgress >= 300 ? 1.0f : highlightProgress / 300.0f;
        }
        return 1.0f;
    }

    public void setCheckBoxVisible(boolean visible, boolean animated) {
        if (visible && checkBox == null) {
            checkBox = new CheckBoxBase(this, 21, resourcesProvider);
            if (attachedToWindow) {
                checkBox.onAttachedToWindow();
            }
        }
        if (visible && mediaCheckBox == null && ((currentMessagesGroup != null && currentMessagesGroup.messages.size() > 1) || (groupedMessagesToSet != null && groupedMessagesToSet.messages.size() > 1))) {
            mediaCheckBox = new CheckBoxBase(this, 21, resourcesProvider);
            mediaCheckBox.setUseDefaultCheck(true);
            if (attachedToWindow) {
                mediaCheckBox.onAttachedToWindow();
            }
        }
        if (checkBoxVisible == visible) {
            if (animated != checkBoxAnimationInProgress && !animated) {
                checkBoxAnimationProgress = visible ? 1.0f : 0.0f;
                invalidate();
            }
            return;
        }
        checkBoxAnimationInProgress = animated;
        checkBoxVisible = visible;
        if (animated) {
            lastCheckBoxAnimationTime = SystemClock.elapsedRealtime();
        } else {
            checkBoxAnimationProgress = visible ? 1.0f : 0.0f;
        }
        invalidate();
    }

    public void setChecked(boolean checked, boolean allChecked, boolean animated) {
        if (checkBox != null) {
            checkBox.setChecked(allChecked, animated);
        }
        if (mediaCheckBox != null) {
            mediaCheckBox.setChecked(checked, animated);
        }
        backgroundDrawable.setSelected(allChecked, animated);
    }

    public void setLastTouchCoords(float x, float y) {
        lastTouchX = x;
        lastTouchY = y;
        backgroundDrawable.setTouchCoords(lastTouchX + getTranslationX(), lastTouchY);
    }

    public MessageBackgroundDrawable getBackgroundDrawable() {
        return backgroundDrawable;
    }

    public Theme.MessageDrawable getCurrentBackgroundDrawable(boolean update) {
        if (update) {
            boolean forceMediaByGroup = currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) == 0 && currentMessagesGroup.isDocuments && !drawPinnedBottom;
            if (currentMessageObject.isOutOwner()) {
                if (!mediaBackground && !drawPinnedBottom && !forceMediaByGroup) {
                    currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOut);
                } else {
                    currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgOutMedia);
                }
            } else {
                if (!mediaBackground && !drawPinnedBottom && !forceMediaByGroup) {
                    currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgIn);
                } else {
                    currentBackgroundDrawable = (Theme.MessageDrawable) getThemedDrawable(Theme.key_drawable_msgInMedia);
                }
            }
        }
        currentBackgroundDrawable.getBackgroundDrawable();
        return currentBackgroundDrawable;
    }

    public void drawCaptionLayout(Canvas canvas, boolean selectionOnly, float alpha) {
        if (transitionParams.animateReplaceCaptionLayout && transitionParams.animateChangeProgress != 1f) {
            drawCaptionLayout(canvas, transitionParams.animateOutCaptionLayout, selectionOnly, alpha * (1f - transitionParams.animateChangeProgress));
            drawCaptionLayout(canvas, captionLayout, selectionOnly, alpha * transitionParams.animateChangeProgress);
        } else {
            drawCaptionLayout(canvas, captionLayout, selectionOnly, alpha);
        }
    }

    private void drawCaptionLayout(Canvas canvas, StaticLayout captionLayout, boolean selectionOnly, float alpha) {
        if (currentBackgroundDrawable != null && drawCommentButton && timeLayout != null) {
            int x;
            float y = layoutHeight - AndroidUtilities.dp(18) - timeLayout.getHeight();
            if (mediaBackground) {
                x = backgroundDrawableLeft + AndroidUtilities.dp(12) + getExtraTextX();
            } else {
                x = backgroundDrawableLeft + AndroidUtilities.dp(drawPinnedBottom ? 12 : 18) + getExtraTextX();
            }
            int endX = x - getExtraTextX();
            if (currentMessagesGroup != null && !currentMessageObject.isMusic() && !currentMessageObject.isDocument()) {
                int dWidth = getGroupPhotosWidth();
                if ((currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0) {
                    endX += Math.ceil(currentPosition.pw / 1000.0f * dWidth);
                } else {
                    int firstLineWidth = 0;
                    for (int a = 0; a < currentMessagesGroup.posArray.size(); a++) {
                        MessageObject.GroupedMessagePosition position = currentMessagesGroup.posArray.get(a);
                        if (position.minY == 0) {
                            firstLineWidth += Math.ceil((position.pw + position.leftSpanOffset) / 1000.0f * dWidth);
                        } else {
                            break;
                        }
                    }
                    endX += firstLineWidth - AndroidUtilities.dp(9);
                }
            } else {
                endX += backgroundWidth - (mediaBackground ? 0 : AndroidUtilities.dp(9));
            }

            int h;
            int h2;
            if (pinnedBottom) {
                h = 2;
                h2 = 3;
            } else if (pinnedTop) {
                h = 4;
                h2 = 1;
            } else {
                h = 3;
                h2 = 0;
            }

            int buttonX = getCurrentBackgroundLeft() + AndroidUtilities.dp(currentMessageObject.isOutOwner() || mediaBackground || drawPinnedBottom ? 2 : 8);
            float buttonY = layoutHeight - AndroidUtilities.dp(45.1f - h2);
            if (currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) == 0 && !currentMessagesGroup.hasSibling) {
                endX += AndroidUtilities.dp(14);
                buttonX -= AndroidUtilities.dp(10);
            }
            commentButtonRect.set(buttonX, (int) buttonY, endX - AndroidUtilities.dp(14), layoutHeight - AndroidUtilities.dp(h));
            if (selectorDrawable[1] != null && selectorDrawableMaskType[1] == 2) {
                selectorDrawable[1].setBounds(commentButtonRect);
                selectorDrawable[1].draw(canvas);
            }
            if (currentPosition == null || (currentPosition.flags & MessageObject.POSITION_FLAG_LEFT) != 0 && currentPosition.minX == 0 && currentPosition.maxX == 0) {
                Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
                boolean drawnAvatars = false;
                int avatarsOffset = 2;
                if (commentAvatarImages != null) {
                    int toAdd = AndroidUtilities.dp(17);
                    int ax = x + getExtraTextX();
                    for (int a = commentAvatarImages.length - 1; a >= 0; a--) {
                        if (!commentAvatarImagesVisible[a] || !commentAvatarImages[a].hasImageSet()) {
                            continue;
                        }
                        commentAvatarImages[a].setImageX(ax + toAdd * a);
                        commentAvatarImages[a].setImageY(y - AndroidUtilities.dp(4) + (pinnedBottom ? AndroidUtilities.dp(2) : 0));
                        if (a != commentAvatarImages.length - 1) {
                            canvas.drawCircle(commentAvatarImages[a].getCenterX(), commentAvatarImages[a].getCenterY(), AndroidUtilities.dp(13), currentBackgroundDrawable.getPaint());
                        }
                        commentAvatarImages[a].draw(canvas);
                        drawnAvatars = true;
                        if (a != 0) {
                            avatarsOffset += 17;
                        }
                    }
                }
                if (!mediaBackground || captionLayout != null) {
                    if (isDrawSelectionBackground()) {
                        Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outVoiceSeekbarSelected : Theme.key_chat_inVoiceSeekbarSelected));
                    } else {
                        Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outVoiceSeekbar : Theme.key_chat_inVoiceSeekbar));
                    }
                    float ly = layoutHeight - AndroidUtilities.dp(45.1f - h2);
                    canvas.drawLine(x, ly, endX - AndroidUtilities.dp(14), ly, Theme.chat_replyLinePaint);
                }
                if (commentLayout != null && drawSideButton != 3) {
                    Theme.chat_replyNamePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outPreviewInstantText : Theme.key_chat_inPreviewInstantText));
                    commentX = x + AndroidUtilities.dp(33 + avatarsOffset);
                    if (drawCommentNumber) {
                        commentX += commentNumberWidth + AndroidUtilities.dp(4);
                    }
                    int prevAlpha = Theme.chat_replyNamePaint.getAlpha();
                    if (transitionParams.animateComments) {
                        if (transitionParams.animateCommentsLayout != null) {
                            canvas.save();
                            Theme.chat_replyNamePaint.setAlpha((int) (prevAlpha * (1.0 - transitionParams.animateChangeProgress)));
                            float cx = transitionParams.animateCommentX + (commentX - transitionParams.animateCommentX) * transitionParams.animateChangeProgress;
                            canvas.translate(cx, y - AndroidUtilities.dp(0.1f) + (pinnedBottom ? AndroidUtilities.dp(2) : 0));
                            transitionParams.animateCommentsLayout.draw(canvas);
                            canvas.restore();
                        }
                    }
                    canvas.save();
                    canvas.translate(x + AndroidUtilities.dp(33 + avatarsOffset), y - AndroidUtilities.dp(0.1f) + (pinnedBottom ? AndroidUtilities.dp(2) : 0));
                    if (!currentMessageObject.isSent()) {
                        Theme.chat_replyNamePaint.setAlpha(127);
                        Theme.chat_commentArrowDrawable.setAlpha(127);
                        Theme.chat_commentDrawable.setAlpha(127);
                    } else {
                        Theme.chat_commentArrowDrawable.setAlpha(255);
                        Theme.chat_commentDrawable.setAlpha(255);
                    }
                    if (drawCommentNumber || transitionParams.animateComments && transitionParams.animateDrawCommentNumber) {
                        if (drawCommentNumber && transitionParams.animateComments) {
                            if (transitionParams.animateDrawCommentNumber) {
                                Theme.chat_replyNamePaint.setAlpha(prevAlpha);
                            } else {
                                Theme.chat_replyNamePaint.setAlpha((int) (prevAlpha * transitionParams.animateChangeProgress));
                            }
                        }
                        commentNumberLayout.draw(canvas);
                        if (drawCommentNumber) {
                            canvas.translate(commentNumberWidth + AndroidUtilities.dp(4), 0);
                        }
                    }
                    if (transitionParams.animateComments && transitionParams.animateCommentsLayout != null) {
                        Theme.chat_replyNamePaint.setAlpha((int) (prevAlpha * transitionParams.animateChangeProgress));
                    } else {
                        Theme.chat_replyNamePaint.setAlpha((int) (prevAlpha * alpha));
                    }
                    commentLayout.draw(canvas);
                    canvas.restore();
                    commentUnreadX = x + commentWidth + AndroidUtilities.dp(33 + avatarsOffset) + AndroidUtilities.dp(9);
                    if (drawCommentNumber) {
                        commentUnreadX += commentNumberWidth + AndroidUtilities.dp(4);
                    }
                    TLRPC.MessageReplies replies = null;
                    if (currentMessagesGroup != null && !currentMessagesGroup.messages.isEmpty()) {
                        MessageObject messageObject = currentMessagesGroup.messages.get(0);
                        if (messageObject.hasReplies()) {
                            replies = messageObject.messageOwner.replies;
                        }
                    } else {
                        if (currentMessageObject.hasReplies()) {
                            replies = currentMessageObject.messageOwner.replies;
                        }
                    }
                    if (commentDrawUnread = (replies != null && replies.read_max_id != 0 && replies.read_max_id < replies.max_id)) {
                        int color = getThemedColor(Theme.key_chat_inInstant);
                        Theme.chat_docBackPaint.setColor(color);
                        int unreadX;
                        if (transitionParams.animateComments) {
                            if (!transitionParams.animateCommentDrawUnread) {
                                Theme.chat_docBackPaint.setAlpha((int) (Color.alpha(color) * transitionParams.animateChangeProgress));
                            }
                            unreadX = (int) (transitionParams.animateCommentUnreadX + (commentUnreadX - transitionParams.animateCommentUnreadX) * transitionParams.animateChangeProgress);
                        } else {
                            unreadX = commentUnreadX;
                        }
                        canvas.drawCircle(unreadX, y + AndroidUtilities.dp(8) + (pinnedBottom ? AndroidUtilities.dp(2) : 0), AndroidUtilities.dp(2.5f), Theme.chat_docBackPaint);
                    }
                }
                if (!drawnAvatars) {
                    setDrawableBounds(Theme.chat_commentDrawable, x, y - AndroidUtilities.dp(4) + (pinnedBottom ? AndroidUtilities.dp(2) : 0));
                    if (alpha != 1f) {
                        Theme.chat_commentDrawable.setAlpha((int) (255 * alpha));
                        Theme.chat_commentDrawable.draw(canvas);
                        Theme.chat_commentDrawable.setAlpha(255);
                    } else {
                        Theme.chat_commentDrawable.draw(canvas);
                    }
                }

                commentArrowX = endX - AndroidUtilities.dp(44);
                int commentX;
                if (transitionParams.animateComments) {
                    commentX = (int) (transitionParams.animateCommentArrowX + (commentArrowX - transitionParams.animateCommentArrowX) * transitionParams.animateChangeProgress);
                } else {
                    commentX = commentArrowX;
                }
                float commentY = y - AndroidUtilities.dp(4) + (pinnedBottom ? AndroidUtilities.dp(2) : 0);
                boolean drawProgress = delegate.shouldDrawThreadProgress(this);
                long newTime = SystemClock.elapsedRealtime();
                long dt = (newTime - commentProgressLastUpadteTime);
                commentProgressLastUpadteTime = newTime;
                if (dt > 17) {
                    dt = 17;
                }
                if (drawProgress) {
                    if (commentProgressAlpha < 1.0f) {
                        commentProgressAlpha += dt / 180.0f;
                        if (commentProgressAlpha > 1.0f) {
                            commentProgressAlpha = 1.0f;
                        }
                    }
                } else {
                    if (commentProgressAlpha > 0.0f) {
                        commentProgressAlpha -= dt / 180.0f;
                        if (commentProgressAlpha < 0.0f) {
                            commentProgressAlpha = 0.0f;
                        }
                    }
                }
                if ((drawProgress || commentProgressAlpha > 0.0f) && commentProgress != null) {
                    commentProgress.setColor(getThemedColor(Theme.key_chat_inInstant));
                    commentProgress.setAlpha(commentProgressAlpha);
                    commentProgress.draw(canvas, commentX + AndroidUtilities.dp(11), commentY + AndroidUtilities.dp(12), commentProgressAlpha);
                    invalidate();
                }
                if (!drawProgress || commentProgressAlpha < 1.0f) {
                    int aw = Theme.chat_commentArrowDrawable.getIntrinsicWidth();
                    int ah = Theme.chat_commentArrowDrawable.getIntrinsicHeight();
                    float acx = commentX + aw / 2;
                    float acy = commentY + ah / 2;
                    Theme.chat_commentArrowDrawable.setBounds((int) (acx - aw / 2 * (1.0f - commentProgressAlpha)), (int) (acy - ah / 2 * (1.0f - commentProgressAlpha)), (int) (acx + aw / 2 * (1.0f - commentProgressAlpha)), (int) (acy + ah / 2 * (1.0f - commentProgressAlpha)));
                    Theme.chat_commentArrowDrawable.setAlpha((int) (255 * (1.0f - commentProgressAlpha) * alpha));
                    Theme.chat_commentArrowDrawable.draw(canvas);
                }
            }
        }

        if (captionLayout == null || selectionOnly && pressedLink == null || (currentMessageObject.deleted && currentPosition != null) || alpha == 0) {
            return;
        }
        if (currentMessageObject.isOutOwner()) {
            Theme.chat_msgTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
            Theme.chat_msgTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkOut);
        } else {
            Theme.chat_msgTextPaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
            Theme.chat_msgTextPaint.linkColor = getThemedColor(Theme.key_chat_messageLinkIn);
        }
        canvas.save();
        float renderingAlpha = alpha;
        if (currentMessagesGroup != null) {
            renderingAlpha = currentMessagesGroup.transitionParams.captionEnterProgress * alpha;
        }
        if (renderingAlpha == 0) {
            return;
        }

        float captionY = this.captionY;
        float captionX = this.captionX;
        if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
            if (!transitionParams.animateReplaceCaptionLayout) {
                captionY -= getTranslationY();
            }
            captionX += currentMessagesGroup.transitionParams.offsetLeft;
        } else {
            if (transitionParams.animateBackgroundBoundsInner) {
                if (transitionParams.transformGroupToSingleMessage) {
                    captionY -= getTranslationY();
                    captionX += transitionParams.deltaLeft;
                } else if (transitionParams.moveCaption) {
                    captionX = this.captionX * transitionParams.animateChangeProgress + transitionParams.captionFromX * (1f - transitionParams.animateChangeProgress);
                    captionY = this.captionY * transitionParams.animateChangeProgress + transitionParams.captionFromY * (1f - transitionParams.animateChangeProgress);
                } else {
                    captionX += transitionParams.deltaLeft;
                }
            }
        }

        int restore = Integer.MIN_VALUE;
        if (renderingAlpha != 1.0f) {
            rect.set(captionX, captionY, captionX + captionLayout.getWidth(),  captionY + captionLayout.getHeight());
            restore = canvas.saveLayerAlpha(rect, (int) (255 * renderingAlpha), Canvas.ALL_SAVE_FLAG);
        }
        if (transitionParams.animateBackgroundBoundsInner && currentBackgroundDrawable != null && currentMessagesGroup == null) {
            Rect r = currentBackgroundDrawable.getBounds();
            if (currentMessageObject.isOutOwner() && !mediaBackground && !pinnedBottom) {
                canvas.clipRect(
                        r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                        r.right - AndroidUtilities.dp(10), r.bottom - AndroidUtilities.dp(4)
                );
            } else {
                canvas.clipRect(
                        r.left + AndroidUtilities.dp(4), r.top + AndroidUtilities.dp(4),
                        r.right - AndroidUtilities.dp(4), r.bottom - AndroidUtilities.dp(4)
                );
            }
        }
        canvas.translate(captionX, captionY);

        if (pressedLink != null) {
            for (int b = 0; b < urlPath.size(); b++) {
                canvas.drawPath(urlPath.get(b), Theme.chat_urlPaint);
            }
        }
        if (!urlPathSelection.isEmpty()) {
            for (int b = 0; b < urlPathSelection.size(); b++) {
                canvas.drawPath(urlPathSelection.get(b), Theme.chat_textSearchSelectionPaint);
            }
        }
        if (!selectionOnly) {
            try {
                if (getDelegate().getTextSelectionHelper() != null && getDelegate().getTextSelectionHelper().isSelected(currentMessageObject)) {
                    getDelegate().getTextSelectionHelper().drawCaption(currentMessageObject.isOutOwner(), captionLayout, canvas);
                }
                Emoji.emojiDrawingYOffset = -transitionYOffsetForDrawables;
                captionLayout.draw(canvas);
                Emoji.emojiDrawingYOffset = 0;
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        if (restore != Integer.MIN_VALUE) {
            canvas.restoreToCount(restore);
        }
        canvas.restore();
    }

    public boolean needDrawTime() {
        return !forceNotDrawTime;
    }

    public boolean shouldDrawTimeOnMedia() {
        if (overideShouldDrawTimeOnMedia != 0) {
            return overideShouldDrawTimeOnMedia == 1;
        }
        return mediaBackground && captionLayout == null/* || isMedia && drawCommentButton && !isRepliesChat*/;
    }

    public void drawTime(Canvas canvas, float alpha, boolean fromParent) {
        if (!drawFromPinchToZoom && delegate != null && delegate.getPinchToZoomHelper() != null && delegate.getPinchToZoomHelper().isInOverlayModeFor(this) && shouldDrawTimeOnMedia()) {
            return;
        }
        for (int i = 0; i < 2; i++) {
            float curentAplha = alpha;
            if (i == 0 && isDrawSelectionBackground() && currentSelectedBackgroundAlpha == 1f && !shouldDrawTimeOnMedia()) {
                continue;
            } else if (i == 1 && ((!isDrawSelectionBackground() && currentSelectedBackgroundAlpha == 0) || shouldDrawTimeOnMedia())) {
                break;
            }
            boolean drawSelectionBackground = i == 1;
            if (i == 1) {
                curentAplha *= currentSelectedBackgroundAlpha;
            } else if (!shouldDrawTimeOnMedia()){
                curentAplha *= (1f - currentSelectedBackgroundAlpha);
            }
            if (transitionParams.animateShouldDrawTimeOnMedia && transitionParams.animateChangeProgress != 1f) {
                if (shouldDrawTimeOnMedia()) {
                    overideShouldDrawTimeOnMedia = 1;
                    drawTimeInternal(canvas, curentAplha * transitionParams.animateChangeProgress, fromParent, this.timeX, timeLayout, timeWidth, drawSelectionBackground);
                    overideShouldDrawTimeOnMedia = 2;
                    drawTimeInternal(canvas, curentAplha * (1f - transitionParams.animateChangeProgress), fromParent, transitionParams.animateFromTimeX, transitionParams.animateTimeLayout, transitionParams.animateTimeWidth, drawSelectionBackground);
                } else {
                    overideShouldDrawTimeOnMedia = 2;
                    drawTimeInternal(canvas, curentAplha * transitionParams.animateChangeProgress, fromParent, this.timeX, timeLayout, timeWidth, drawSelectionBackground);
                    overideShouldDrawTimeOnMedia = 1;
                    drawTimeInternal(canvas, curentAplha * (1f - transitionParams.animateChangeProgress), fromParent, transitionParams.animateFromTimeX, transitionParams.animateTimeLayout, transitionParams.animateTimeWidth, drawSelectionBackground);
                }
                overideShouldDrawTimeOnMedia = 0;
            } else {
                float timeX;
                float timeWidth;
                if (transitionParams.shouldAnimateTimeX) {
                    timeX = this.timeX * transitionParams.animateChangeProgress + transitionParams.animateFromTimeX * (1f - transitionParams.animateChangeProgress);
                    timeWidth = this.timeWidth * transitionParams.animateChangeProgress + transitionParams.animateTimeWidth * (1f - transitionParams.animateChangeProgress);
                } else {
                    timeX = this.timeX + transitionParams.deltaRight;
                    timeWidth = this.timeWidth;
                }
                drawTimeInternal(canvas, curentAplha, fromParent, timeX, timeLayout, timeWidth, drawSelectionBackground);
            }
        }

        if (transitionParams.animateBackgroundBoundsInner) {
            drawOverlays(canvas);
        }
    }

    private void drawTimeInternal(Canvas canvas, float alpha, boolean fromParent, float timeX, StaticLayout timeLayout, float timeWidth, boolean drawSelectionBackground) {
        if ((!drawTime || groupPhotoInvisible) && shouldDrawTimeOnMedia() || timeLayout == null || (currentMessageObject.deleted && currentPosition != null) || currentMessageObject.type == 16) {
            return;
        }
        if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
            Theme.chat_timePaint.setColor(getThemedColor(Theme.key_chat_serviceText));
        } else {
            if (shouldDrawTimeOnMedia()) {
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    Theme.chat_timePaint.setColor(getThemedColor(Theme.key_chat_serviceText));
                } else {
                    Theme.chat_timePaint.setColor(getThemedColor(Theme.key_chat_mediaTimeText));
                }
            } else {
                if (currentMessageObject.isOutOwner()) {
                    Theme.chat_timePaint.setColor(getThemedColor(drawSelectionBackground ? Theme.key_chat_outTimeSelectedText : Theme.key_chat_outTimeText));
                } else {
                    Theme.chat_timePaint.setColor(getThemedColor(drawSelectionBackground ? Theme.key_chat_inTimeSelectedText : Theme.key_chat_inTimeText));
                }
            }
        }
        if (getTransitionParams().animateDrawingTimeAlpha) {
            alpha *= getTransitionParams().animateChangeProgress;
        }
        if (alpha != 1f) {
            Theme.chat_timePaint.setAlpha((int) (Theme.chat_timePaint.getAlpha() * alpha));
        }
        canvas.save();
        if (drawPinnedBottom && !shouldDrawTimeOnMedia()) {
            canvas.translate(0, AndroidUtilities.dp(2));
        }
        boolean bigRadius = false;
        float layoutHeight = this.layoutHeight + transitionParams.deltaBottom;
        float timeTitleTimeX = timeX;
        if (transitionParams.shouldAnimateTimeX) {
            timeTitleTimeX = transitionParams.animateFromTimeX * (1f - transitionParams.animateChangeProgress) + this.timeX * transitionParams.animateChangeProgress;
        }
        if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
            layoutHeight -= getTranslationY();
            timeX += currentMessagesGroup.transitionParams.offsetRight;
            timeTitleTimeX += currentMessagesGroup.transitionParams.offsetRight;
        }
        if (drawPinnedBottom && shouldDrawTimeOnMedia()) {
            layoutHeight += AndroidUtilities.dp(1);
        }
        if (transitionParams.animateBackgroundBoundsInner) {
            timeX += animationOffsetX;
            timeTitleTimeX += animationOffsetX;
        }

        int timeYOffset;
        if (shouldDrawTimeOnMedia()) {
            timeYOffset = -(drawCommentButton ? AndroidUtilities.dp(41.3f) : 0);
            Paint paint;
            if (currentMessageObject.shouldDrawWithoutBackground()) {
                paint = getThemedPaint(Theme.key_paint_chatActionBackground);
            } else {
                paint = getThemedPaint(Theme.key_paint_chatTimeBackground);
            }
            int oldAlpha = paint.getAlpha();
            paint.setAlpha((int) (oldAlpha * timeAlpha * alpha));
            Theme.chat_timePaint.setAlpha((int) (255 * timeAlpha * alpha));

            int r;
            if (documentAttachType != DOCUMENT_ATTACH_TYPE_ROUND && documentAttachType != DOCUMENT_ATTACH_TYPE_STICKER) {
                int[] rad = photoImage.getRoundRadius();
                r = Math.min(AndroidUtilities.dp(8), Math.max(rad[2], rad[3]));
                bigRadius = SharedConfig.bubbleRadius >= 10;
            } else {
                r = AndroidUtilities.dp(4);
            }
            float x1 = timeX - AndroidUtilities.dp(bigRadius ? 6 : 4);
            float timeY = photoImage.getImageY2() + additionalTimeOffsetY;
            float y1 = timeY - AndroidUtilities.dp(23);
            rect.set(x1, y1, x1 + timeWidth + AndroidUtilities.dp((bigRadius ? 12 : 8) + (currentMessageObject.isOutOwner() ? 20 : 0)), y1 + AndroidUtilities.dp(17));

            applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
            canvas.drawRoundRect(rect, r, r, paint);
            if (paint == getThemedPaint(Theme.key_paint_chatActionBackground) && hasGradientService()) {
                int oldAlpha2 = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (oldAlpha2 * timeAlpha * alpha));
                canvas.drawRoundRect(rect, r, r, Theme.chat_actionBackgroundGradientDarkenPaint);
                Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(oldAlpha2);
            }
            paint.setAlpha(oldAlpha);

            float additionalX = -timeLayout.getLineLeft(0);
            if (ChatObject.isChannel(currentChat) && !currentChat.megagroup || (currentMessageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0 || repliesLayout != null || isPinned) {
                additionalX += this.timeWidth - timeLayout.getLineWidth(0);

                int currentStatus = transitionParams.createStatusDrawableParams();
                if (transitionParams.lastStatusDrawableParams >= 0 && transitionParams.lastStatusDrawableParams != currentStatus && !statusDrawableAnimationInProgress) {
                    createStatusDrawableAnimator(transitionParams.lastStatusDrawableParams, currentStatus, fromParent);
                }
                if (statusDrawableAnimationInProgress) {
                    currentStatus = animateToStatusDrawableParams;
                }

                boolean drawClock = (currentStatus & 4) != 0;
                boolean drawError = (currentStatus & 8) != 0;

                if (statusDrawableAnimationInProgress) {
                    boolean outDrawClock = (animateFromStatusDrawableParams & 4) != 0;
                    boolean outDrawError = (animateFromStatusDrawableParams & 8) != 0;
                    drawClockOrErrorLayout(canvas, outDrawClock, outDrawError, layoutHeight, alpha, timeYOffset, timeX, 1f - statusDrawableProgress, drawSelectionBackground);
                    drawClockOrErrorLayout(canvas, drawClock, drawError, layoutHeight, alpha, timeYOffset, timeX, statusDrawableProgress, drawSelectionBackground);

                    if (!currentMessageObject.isOutOwner()) {
                        if (!outDrawClock && !outDrawError) {
                            drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, 1f - statusDrawableProgress, drawSelectionBackground);
                        }
                        if (!drawClock && !drawError) {
                            drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, statusDrawableProgress, drawSelectionBackground);
                        }
                    }
                } else {
                    if (!currentMessageObject.isOutOwner()) {
                        if (!drawClock && !drawError) {
                            drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, 1f, drawSelectionBackground);
                        }
                    }
                    drawClockOrErrorLayout(canvas, drawClock, drawError, layoutHeight, alpha, timeYOffset, timeX, 1f, drawSelectionBackground);
                }

                if (currentMessageObject.isOutOwner()) {
                    drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, 1f, drawSelectionBackground);
                }

                transitionParams.lastStatusDrawableParams = transitionParams.createStatusDrawableParams();

                if (drawClock && fromParent && getParent() != null) {
                    ((View)getParent()).invalidate();
                }
            }

            canvas.save();
            canvas.translate(drawTimeX = timeTitleTimeX + additionalX, drawTimeY = timeY - AndroidUtilities.dp(7.3f) - timeLayout.getHeight());
            timeLayout.draw(canvas);
            canvas.restore();
            Theme.chat_timePaint.setAlpha(255);
        } else {
            if (currentMessageObject.isSponsored()) {
                timeYOffset = -AndroidUtilities.dp(48);
                if (hasNewLineForTime) {
                    timeYOffset -= AndroidUtilities.dp(16);
                }
            } else {
                timeYOffset = -(drawCommentButton ? AndroidUtilities.dp(43) : 0);
            }
            float additionalX = -timeLayout.getLineLeft(0);
            if (ChatObject.isChannel(currentChat) && !currentChat.megagroup || (currentMessageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0 || (repliesLayout != null || transitionParams.animateReplies) || (isPinned || transitionParams.animatePinned)) {
                additionalX += this.timeWidth - timeLayout.getLineWidth(0);

                int currentStatus = transitionParams.createStatusDrawableParams();
                if (transitionParams.lastStatusDrawableParams >= 0 && transitionParams.lastStatusDrawableParams != currentStatus && !statusDrawableAnimationInProgress) {
                    createStatusDrawableAnimator(transitionParams.lastStatusDrawableParams, currentStatus, fromParent);
                }
                if (statusDrawableAnimationInProgress) {
                    currentStatus = animateToStatusDrawableParams;
                }

                boolean drawClock = (currentStatus & 4) != 0;
                boolean drawError = (currentStatus & 8) != 0;

                if (statusDrawableAnimationInProgress) {
                    boolean outDrawClock = (animateFromStatusDrawableParams & 4) != 0;
                    boolean outDrawError = (animateFromStatusDrawableParams & 8) != 0;
                    drawClockOrErrorLayout(canvas, outDrawClock, outDrawError, layoutHeight, alpha, timeYOffset, timeX, 1f - statusDrawableProgress, drawSelectionBackground);
                    drawClockOrErrorLayout(canvas, drawClock, drawError, layoutHeight, alpha, timeYOffset, timeX, statusDrawableProgress, drawSelectionBackground);

                    if (!currentMessageObject.isOutOwner()) {
                        if (!outDrawClock && !outDrawError) {
                            drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, 1f - statusDrawableProgress, drawSelectionBackground);
                        }
                        if (!drawClock && !drawError) {
                            drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, statusDrawableProgress, drawSelectionBackground);
                        }
                    }
                } else {
                    if (!currentMessageObject.isOutOwner()) {
                        if (!drawClock && !drawError) {
                            drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, 1f, drawSelectionBackground);
                        }
                    }
                    drawClockOrErrorLayout(canvas, drawClock, drawError, layoutHeight, alpha, timeYOffset, timeX, 1f, drawSelectionBackground);
                }

                if (currentMessageObject.isOutOwner()) {
                    drawViewsAndRepliesLayout(canvas, layoutHeight, alpha, timeYOffset, timeX, 1f, drawSelectionBackground);
                }
                transitionParams.lastStatusDrawableParams = transitionParams.createStatusDrawableParams();

                if (drawClock && fromParent && getParent() != null) {
                    ((View)getParent()).invalidate();
                }
            }
            canvas.save();
            if (transitionParams.animateEditedEnter && transitionParams.animateChangeProgress != 1f) {
                if (transitionParams.animateEditedLayout != null) {
                    canvas.translate(timeTitleTimeX + additionalX, layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 7.5f : 6.5f) - timeLayout.getHeight() + timeYOffset);
                    int oldAlpha = Theme.chat_timePaint.getAlpha();
                    Theme.chat_timePaint.setAlpha((int) (oldAlpha * transitionParams.animateChangeProgress));
                    transitionParams.animateEditedLayout.draw(canvas);
                    Theme.chat_timePaint.setAlpha(oldAlpha);
                    transitionParams.animateTimeLayout.draw(canvas);
                } else {
                    int oldAlpha = Theme.chat_timePaint.getAlpha();
                    canvas.save();
                    canvas.translate(transitionParams.animateFromTimeX + additionalX, layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 7.5f : 6.5f) - timeLayout.getHeight() + timeYOffset);
                    Theme.chat_timePaint.setAlpha((int) (oldAlpha * (1f - transitionParams.animateChangeProgress)));
                    transitionParams.animateTimeLayout.draw(canvas);
                    canvas.restore();

                    canvas.translate(timeTitleTimeX + additionalX, layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 7.5f : 6.5f) - timeLayout.getHeight() + timeYOffset);
                    Theme.chat_timePaint.setAlpha((int) (oldAlpha * (transitionParams.animateChangeProgress)));
                    timeLayout.draw(canvas);
                    Theme.chat_timePaint.setAlpha(oldAlpha);
                }
            } else {
                canvas.translate(drawTimeX = timeTitleTimeX + additionalX, drawTimeY = layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 7.5f : 6.5f) - timeLayout.getHeight() + timeYOffset);
                timeLayout.draw(canvas);
            }
            canvas.restore();
        }

        if (currentMessageObject.isOutOwner()) {
            int currentStatus = transitionParams.createStatusDrawableParams();
            if (transitionParams.lastStatusDrawableParams >= 0 && transitionParams.lastStatusDrawableParams != currentStatus && !statusDrawableAnimationInProgress) {
                createStatusDrawableAnimator(transitionParams.lastStatusDrawableParams, currentStatus, fromParent);
            }
            if (statusDrawableAnimationInProgress) {
                currentStatus = animateToStatusDrawableParams;
            }
            boolean drawCheck1 = (currentStatus & 1) != 0;
            boolean drawCheck2 = (currentStatus & 2) != 0;
            boolean drawClock = (currentStatus & 4) != 0;
            boolean drawError = (currentStatus & 8) != 0;
            boolean isBroadcast = (currentStatus & 16) != 0;
            boolean needRestore = false;
            if (transitionYOffsetForDrawables != 0) {
                needRestore = true;
                canvas.save();
                canvas.translate(0, transitionYOffsetForDrawables);
            }
            if (statusDrawableAnimationInProgress) {
                boolean outDrawCheck1 = (animateFromStatusDrawableParams & 1) != 0;
                boolean outDrawCheck2 = (animateFromStatusDrawableParams & 2) != 0;
                boolean outDrawClock = (animateFromStatusDrawableParams & 4) != 0;
                boolean outDrawError = (animateFromStatusDrawableParams & 8) != 0;
                boolean outIsBroadcast = (animateFromStatusDrawableParams & 16) != 0;
                if (!outDrawClock && !isBroadcast && !outIsBroadcast && outDrawCheck2 && drawCheck2 && !outDrawCheck1 && drawCheck1) {
                    drawStatusDrawable(canvas, drawCheck1, drawCheck2, drawClock, drawError, isBroadcast, alpha, bigRadius, timeYOffset, layoutHeight, statusDrawableProgress, true, drawSelectionBackground);
                } else {
                    drawStatusDrawable(canvas, outDrawCheck1, outDrawCheck2, outDrawClock, outDrawError, outIsBroadcast, alpha, bigRadius, timeYOffset, layoutHeight,1f - statusDrawableProgress, false, drawSelectionBackground);
                    drawStatusDrawable(canvas, drawCheck1, drawCheck2, drawClock, drawError, isBroadcast, alpha, bigRadius, timeYOffset, layoutHeight, statusDrawableProgress, false, drawSelectionBackground);
                }
            } else {
                drawStatusDrawable(canvas, drawCheck1, drawCheck2, drawClock, drawError, isBroadcast, alpha, bigRadius, timeYOffset, layoutHeight, 1, false, drawSelectionBackground);
            }
            if (needRestore) {
                canvas.restore();
            }
            transitionParams.lastStatusDrawableParams = transitionParams.createStatusDrawableParams();
            if (fromParent && drawClock && getParent() != null) {
                ((View) getParent()).invalidate();
            }
        }
        canvas.restore();
    }

    private void createStatusDrawableAnimator(int lastStatusDrawableParams, int currentStatus, boolean fromParent) {
        boolean drawCheck1 = (currentStatus & 1) != 0;
        boolean drawCheck2 = (currentStatus & 2) != 0;
        boolean isBroadcast = (currentStatus & 16) != 0;

        boolean outDrawCheck1 = (lastStatusDrawableParams & 1) != 0;
        boolean outDrawCheck2 = (lastStatusDrawableParams & 2) != 0;
        boolean outDrawClock = (lastStatusDrawableParams & 4) != 0;
        boolean outIsBroadcast = (lastStatusDrawableParams & 16) != 0;

        boolean moveCheckTransition = !outDrawClock && !isBroadcast && !outIsBroadcast && outDrawCheck2 && drawCheck2 && !outDrawCheck1 && drawCheck1;

        if (transitionParams.messageEntering && !moveCheckTransition) {
            return;
        }
        statusDrawableProgress = 0f;
        statusDrawableAnimator = ValueAnimator.ofFloat(0,1f);
        if (moveCheckTransition) {
            statusDrawableAnimator.setDuration(220);
        } else {
            statusDrawableAnimator.setDuration(150);
        }
        statusDrawableAnimator.setInterpolator(CubicBezierInterpolator.DEFAULT);
        animateFromStatusDrawableParams = lastStatusDrawableParams;
        animateToStatusDrawableParams = currentStatus;
        statusDrawableAnimator.addUpdateListener(valueAnimator -> {
            statusDrawableProgress = (float) valueAnimator.getAnimatedValue();
            invalidate();
            if (fromParent && getParent() != null) {
                ((View) getParent()).invalidate();
            }
        });
        statusDrawableAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                int currentStatus = transitionParams.createStatusDrawableParams();
                if (animateToStatusDrawableParams != currentStatus) {
                    createStatusDrawableAnimator(animateToStatusDrawableParams, currentStatus, fromParent);
                } else {
                    statusDrawableAnimationInProgress = false;
                    transitionParams.lastStatusDrawableParams = animateToStatusDrawableParams;
                }
            }
        });
        statusDrawableAnimationInProgress = true;
        statusDrawableAnimator.start();

    }

    private void drawClockOrErrorLayout(Canvas canvas, boolean drawTime, boolean drawError, float layoutHeight, float alpha, float timeYOffset, float timeX, float progress, boolean drawSelectionBackground) {
        boolean useScale = progress != 1f;
        float scale = 0.5f + 0.5f * progress;
        alpha *= progress;
        if (drawTime) {
            if (!currentMessageObject.isOutOwner()) {
                MsgClockDrawable clockDrawable = Theme.chat_msgClockDrawable;
                int clockColor;
                if (shouldDrawTimeOnMedia()) {
                    clockColor = getThemedColor(Theme.key_chat_mediaSentClock);
                } else {
                    clockColor = getThemedColor(drawSelectionBackground ? Theme.key_chat_outSentClockSelected : Theme.key_chat_mediaSentClock);
                }
                clockDrawable.setColor(clockColor);
                float timeY = shouldDrawTimeOnMedia() ? photoImage.getImageY2() + additionalTimeOffsetY - AndroidUtilities.dp(9.0f) : layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 9.5f : 8.5f) + timeYOffset;
                setDrawableBounds(clockDrawable, timeX + (currentMessageObject.scheduled ? 0 : AndroidUtilities.dp(11)),  timeY - clockDrawable.getIntrinsicHeight());
                clockDrawable.setAlpha((int) (255 * alpha));
                if (useScale) {
                    canvas.save();
                    canvas.scale(scale, scale, clockDrawable.getBounds().centerX(), clockDrawable.getBounds().centerY());
                }
                clockDrawable.draw(canvas);
                clockDrawable.setAlpha(255);
                invalidate();
                if (useScale) {
                    canvas.restore();
                }
            }
        } else if (drawError) {
            if (!currentMessageObject.isOutOwner()) {
                float x = timeX + (currentMessageObject.scheduled ? 0 : AndroidUtilities.dp(11));
                float y = shouldDrawTimeOnMedia() ? photoImage.getImageY2() + additionalTimeOffsetY - AndroidUtilities.dp(21.5f) : layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 21.5f : 20.5f) + timeYOffset;
                rect.set(x, y, x + AndroidUtilities.dp(14), y + AndroidUtilities.dp(14));
                int oldAlpha = Theme.chat_msgErrorPaint.getAlpha();
                Theme.chat_msgErrorPaint.setAlpha((int) (255 * alpha));
                if (useScale) {
                    canvas.save();
                    canvas.scale(scale, scale, rect.centerX(), rect.centerY());
                }
                canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), Theme.chat_msgErrorPaint);
                Theme.chat_msgErrorPaint.setAlpha(oldAlpha);
                Drawable errorDrawable = getThemedDrawable(Theme.key_drawable_msgError);
                setDrawableBounds(errorDrawable, x + AndroidUtilities.dp(6), y + AndroidUtilities.dp(2));
                errorDrawable.setAlpha((int) (255 * alpha));
                errorDrawable.draw(canvas);
                errorDrawable.setAlpha(255);
                if (useScale) {
                    canvas.restore();
                }
            }
        }
    }

    private void drawViewsAndRepliesLayout(Canvas canvas, float layoutHeight, float alpha, float timeYOffset, float timeX, float progress, boolean drawSelectionBackground) {
        boolean useScale = progress != 1f;
        float scale = 0.5f + 0.5f * progress;
        alpha *= progress;

        float offsetX = 0;
        int timeAlpha = Theme.chat_timePaint.getAlpha();
        float timeY = shouldDrawTimeOnMedia() ? photoImage.getImageY2() + additionalTimeOffsetY - AndroidUtilities.dp(7.3f) - timeLayout.getHeight() : layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 7.5f : 6.5f) - timeLayout.getHeight() + timeYOffset;
        if (repliesLayout != null || transitionParams.animateReplies) {
            float repliesX = (transitionParams.shouldAnimateTimeX ? this.timeX : timeX) + offsetX;
            boolean inAnimation = transitionParams.animateReplies && transitionParams.animateRepliesLayout == null && repliesLayout != null;
            boolean outAnimation = transitionParams.animateReplies && transitionParams.animateRepliesLayout != null && repliesLayout == null;
            boolean replaceAnimation = transitionParams.animateReplies && transitionParams.animateRepliesLayout != null && repliesLayout != null;
            if (transitionParams.shouldAnimateTimeX && !inAnimation) {
                if (outAnimation) {
                    repliesX = transitionParams.animateFromTimeXReplies;
                } else {
                    repliesX = transitionParams.animateFromTimeXReplies * (1f - transitionParams.animateChangeProgress) + repliesX * transitionParams.animateChangeProgress;
                }
            } else {
                repliesX += transitionParams.deltaRight;
            }
            if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
                repliesX += currentMessagesGroup.transitionParams.offsetRight;
            }
            if (transitionParams.animateBackgroundBoundsInner) {
                repliesX += animationOffsetX;
            }
            Drawable repliesDrawable;
            if (shouldDrawTimeOnMedia()) {
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    repliesDrawable = getThemedDrawable(Theme.key_drawable_msgStickerReplies);
                } else {
                    repliesDrawable = Theme.chat_msgMediaRepliesDrawable;
                }
            } else {
                if (!currentMessageObject.isOutOwner()) {
                    repliesDrawable = drawSelectionBackground ? Theme.chat_msgInRepliesSelectedDrawable : Theme.chat_msgInRepliesDrawable;
                } else {
                    repliesDrawable = getThemedDrawable(drawSelectionBackground ? Theme.key_drawable_msgOutRepliesSelected : Theme.key_drawable_msgOutReplies);
                }
            }
            setDrawableBounds(repliesDrawable, repliesX, timeY);
            float repliesAlpha = alpha;
            if (inAnimation) {
                repliesAlpha *= transitionParams.animateChangeProgress;
            } else if (outAnimation) {
                repliesAlpha *= (1f - transitionParams.animateChangeProgress);
            }
            repliesDrawable.setAlpha((int) (255 * repliesAlpha));
            if (useScale) {
                canvas.save();
                float cx = repliesX + (repliesDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3) + repliesTextWidth) / 2f;
                canvas.scale(scale, scale, cx, repliesDrawable.getBounds().centerY());
            }
            repliesDrawable.draw(canvas);
            repliesDrawable.setAlpha(255);

            if (transitionParams.animateReplies) {
                if (replaceAnimation) {
                    canvas.save();
                    Theme.chat_timePaint.setAlpha((int) (timeAlpha * (1.0 - transitionParams.animateChangeProgress)));
                    canvas.translate(repliesX + repliesDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3), timeY);
                    transitionParams.animateRepliesLayout.draw(canvas);
                    canvas.restore();
                }
                Theme.chat_timePaint.setAlpha((int) (timeAlpha * repliesAlpha));
            }
            canvas.save();
            canvas.translate(repliesX + repliesDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3), timeY);
            if (repliesLayout != null) {
                repliesLayout.draw(canvas);
            } else if (transitionParams.animateRepliesLayout != null) {
                transitionParams.animateRepliesLayout.draw(canvas);
            }
            canvas.restore();
            if (repliesLayout != null) {
                offsetX += repliesDrawable.getIntrinsicWidth() + repliesTextWidth + AndroidUtilities.dp(10);
            }

            if (useScale) {
                canvas.restore();
            }

            if (transitionParams.animateReplies) {
                Theme.chat_timePaint.setAlpha(timeAlpha);
            }
            transitionParams.lastTimeXReplies = repliesX;
        }
        if (viewsLayout != null) {
            float viewsX = (transitionParams.shouldAnimateTimeX ? this.timeX : timeX) + offsetX;
            if (transitionParams.shouldAnimateTimeX) {
                viewsX = transitionParams.animateFromTimeXViews * (1f - transitionParams.animateChangeProgress) + viewsX * transitionParams.animateChangeProgress;
            } else {
                viewsX += transitionParams.deltaRight;
            }
            if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
                viewsX += currentMessagesGroup.transitionParams.offsetRight;
            }
            if (transitionParams.animateBackgroundBoundsInner) {
                viewsX += animationOffsetX;
            }
            Drawable viewsDrawable;
            if (shouldDrawTimeOnMedia()) {
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    viewsDrawable = getThemedDrawable(Theme.key_drawable_msgStickerViews);
                } else {
                    viewsDrawable = Theme.chat_msgMediaViewsDrawable;
                }
            } else {
                if (!currentMessageObject.isOutOwner()) {
                    viewsDrawable = drawSelectionBackground ? Theme.chat_msgInViewsSelectedDrawable : Theme.chat_msgInViewsDrawable;
                } else {
                    viewsDrawable = getThemedDrawable(drawSelectionBackground ? Theme.key_drawable_msgOutViewsSelected : Theme.key_drawable_msgOutViews);
                }
            }
            float y = shouldDrawTimeOnMedia() ? photoImage.getImageY2() + additionalTimeOffsetY - AndroidUtilities.dp(5.5f) - timeLayout.getHeight() : layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 5.5f : 4.5f) - timeLayout.getHeight() + timeYOffset;
            setDrawableBounds(viewsDrawable, viewsX, y);
            if (useScale) {
                canvas.save();
                float cx = viewsX + (viewsDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3) + viewsTextWidth) / 2f;
                canvas.scale(scale, scale, cx, viewsDrawable.getBounds().centerY());
            }
            viewsDrawable.setAlpha((int) (255 * alpha));
            viewsDrawable.draw(canvas);
            viewsDrawable.setAlpha(255);

            if (transitionParams.animateViewsLayout != null) {
                canvas.save();
                Theme.chat_timePaint.setAlpha((int) (timeAlpha * (1.0 - transitionParams.animateChangeProgress)));
                canvas.translate(viewsX + viewsDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3), timeY);
                transitionParams.animateViewsLayout.draw(canvas);
                canvas.restore();
                Theme.chat_timePaint.setAlpha((int) (timeAlpha * transitionParams.animateChangeProgress));
            }

            canvas.save();
            canvas.translate(viewsX + viewsDrawable.getIntrinsicWidth() + AndroidUtilities.dp(3), timeY);
            viewsLayout.draw(canvas);
            canvas.restore();
            if (useScale) {
                canvas.restore();
            }

            offsetX += viewsTextWidth + Theme.chat_msgInViewsDrawable.getIntrinsicWidth() + AndroidUtilities.dp(10);

            if (transitionParams.animateViewsLayout != null) {
                Theme.chat_timePaint.setAlpha(timeAlpha);
            }
            transitionParams.lastTimeXViews = viewsX;
        }
        if (isPinned || transitionParams.animatePinned) {
            float pinnedX = (transitionParams.shouldAnimateTimeX ? this.timeX : timeX) + offsetX;
            boolean inAnimation = transitionParams.animatePinned && isPinned;
            boolean outAnimation = transitionParams.animatePinned && !isPinned;
            if (transitionParams.shouldAnimateTimeX && !inAnimation) {
                if (outAnimation) {
                    pinnedX = transitionParams.animateFromTimeXPinned;
                } else {
                    pinnedX = transitionParams.animateFromTimeXPinned * (1f - transitionParams.animateChangeProgress) + pinnedX * transitionParams.animateChangeProgress;
                }
            } else {
                pinnedX += transitionParams.deltaRight;
            }
            if (currentMessagesGroup != null && currentMessagesGroup.transitionParams.backgroundChangeBounds) {
                pinnedX += currentMessagesGroup.transitionParams.offsetRight;
            }
            if (transitionParams.animateBackgroundBoundsInner) {
                pinnedX += animationOffsetX;
            }

            Drawable pinnedDrawable;
            if (shouldDrawTimeOnMedia()) {
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    pinnedDrawable = getThemedDrawable(Theme.key_drawable_msgStickerPinned);
                } else {
                    pinnedDrawable = Theme.chat_msgMediaPinnedDrawable;
                }
            } else {
                if (!currentMessageObject.isOutOwner()) {
                    pinnedDrawable = drawSelectionBackground ? Theme.chat_msgInPinnedSelectedDrawable : Theme.chat_msgInPinnedDrawable;
                } else {
                    pinnedDrawable = getThemedDrawable(drawSelectionBackground ? Theme.key_drawable_msgOutPinnedSelected : Theme.key_drawable_msgOutPinned);
                }
            }
            if (transitionParams.animatePinned) {
                if (isPinned) {
                    pinnedDrawable.setAlpha((int) (255 * alpha * transitionParams.animateChangeProgress));
                    setDrawableBounds(pinnedDrawable, pinnedX, timeY);
                } else {
                    pinnedDrawable.setAlpha((int) (255 * alpha * (1.0f - transitionParams.animateChangeProgress)));
                    setDrawableBounds(pinnedDrawable, pinnedX, timeY);
                }
            } else {
                pinnedDrawable.setAlpha((int) (255 * alpha));
                setDrawableBounds(pinnedDrawable, pinnedX, timeY);
            }
            if (useScale) {
                canvas.save();
                float cx = pinnedX + pinnedDrawable.getIntrinsicWidth() / 2f;
                canvas.scale(scale, scale, cx, pinnedDrawable.getBounds().centerY());
            }
            pinnedDrawable.draw(canvas);
            pinnedDrawable.setAlpha(255);

            if (useScale) {
                canvas.restore();
            }
            transitionParams.lastTimeXPinned = pinnedX;
        }
    }

    private void drawStatusDrawable(Canvas canvas, boolean drawCheck1, boolean drawCheck2, boolean drawClock, boolean drawError, boolean isBroadcast, float alpha, boolean bigRadius, float timeYOffset, float layoutHeight, float progress, boolean moveCheck, boolean drawSelectionBackground) {
        final boolean useScale = progress != 1f && !moveCheck;
        float scale = 0.5f + 0.5f * progress;
        if (useScale) {
            alpha = alpha * progress;
        }
        float timeY = photoImage.getImageY2() + additionalTimeOffsetY - AndroidUtilities.dp(8.5f);
        if (drawClock) {
            MsgClockDrawable drawable = Theme.chat_msgClockDrawable;
            int color;
            if (shouldDrawTimeOnMedia()) {
                if (currentMessageObject.shouldDrawWithoutBackground()) {
                    color = getThemedColor(Theme.key_chat_serviceText);
                    setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 24 : 22) - drawable.getIntrinsicWidth(), timeY - drawable.getIntrinsicHeight() + timeYOffset);
                    drawable.setAlpha((int) (255 * timeAlpha * alpha));
                } else {
                    color = getThemedColor(Theme.key_chat_mediaSentClock);
                    setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 24 : 22) - drawable.getIntrinsicWidth(), timeY - drawable.getIntrinsicHeight() + timeYOffset);
                    drawable.setAlpha((int) (255 * alpha));
                }
            } else {
                color = getThemedColor(Theme.key_chat_outSentClock);
                setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(18.5f) - drawable.getIntrinsicWidth(), layoutHeight - AndroidUtilities.dp(8.5f) - drawable.getIntrinsicHeight() + timeYOffset);
                drawable.setAlpha((int) (255 * alpha));
            }
            drawable.setColor(color);

            if (useScale) {
                canvas.save();
                canvas.scale(scale, scale, drawable.getBounds().centerX(), drawable.getBounds().centerY());
            }
            drawable.draw(canvas);
            drawable.setAlpha(255);
            if (useScale) {
                canvas.restore();
            }
            invalidate();
        }
        if (isBroadcast) {
            if (drawCheck1 || drawCheck2) {
                Drawable drawable;
                if (shouldDrawTimeOnMedia()) {
                    setDrawableBounds(Theme.chat_msgBroadcastMediaDrawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 26 : 24) - Theme.chat_msgBroadcastMediaDrawable.getIntrinsicWidth(), layoutHeight - AndroidUtilities.dp(14.0f) - Theme.chat_msgBroadcastMediaDrawable.getIntrinsicHeight() + timeYOffset);
                    Theme.chat_msgBroadcastMediaDrawable.setAlpha((int) (255 * alpha));
                    drawable = Theme.chat_msgBroadcastMediaDrawable;
                } else {
                    setDrawableBounds(Theme.chat_msgBroadcastDrawable, layoutWidth - AndroidUtilities.dp(20.5f) - Theme.chat_msgBroadcastDrawable.getIntrinsicWidth(), layoutHeight - AndroidUtilities.dp(8.0f) - Theme.chat_msgBroadcastDrawable.getIntrinsicHeight() + timeYOffset);
                    Theme.chat_msgBroadcastDrawable.setAlpha((int) (255 * alpha));
                    drawable = Theme.chat_msgBroadcastDrawable;
                }
                if (useScale) {
                    canvas.save();
                    canvas.scale(scale, scale, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                }
                drawable.draw(canvas);
                if (useScale) {
                    canvas.restore();
                }
                drawable.setAlpha(255);
            }
        } else {
            if (drawCheck2) {
                if (shouldDrawTimeOnMedia()) {
                    Drawable drawable;
                    if (moveCheck) {
                        canvas.save();
                    }
                    if (currentMessageObject.shouldDrawWithoutBackground()) {
                        drawable = getThemedDrawable(Theme.key_drawable_msgStickerCheck);
                        if (drawCheck1) {
                            if (moveCheck) {
                                canvas.translate(AndroidUtilities.dp(4.8f) * (1f - progress), 0);
                            }
                            setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 28.3f : 26.3f) - drawable.getIntrinsicWidth(), timeY - drawable.getIntrinsicHeight() + timeYOffset);
                        } else {
                            setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 23.5f : 21.5f) - drawable.getIntrinsicWidth(), timeY - drawable.getIntrinsicHeight() + timeYOffset);
                        }
                        drawable.setAlpha((int) (255 * timeAlpha * alpha));
                    } else {
                        if (drawCheck1) {
                            if (moveCheck) {
                                canvas.translate(AndroidUtilities.dp(4.8f) * (1f - progress), 0);
                            }
                            setDrawableBounds(Theme.chat_msgMediaCheckDrawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 28.3f : 26.3f) - Theme.chat_msgMediaCheckDrawable.getIntrinsicWidth(), timeY - Theme.chat_msgMediaCheckDrawable.getIntrinsicHeight() + timeYOffset);
                        } else {
                            setDrawableBounds(Theme.chat_msgMediaCheckDrawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 23.5f : 21.5f) - Theme.chat_msgMediaCheckDrawable.getIntrinsicWidth(), timeY - Theme.chat_msgMediaCheckDrawable.getIntrinsicHeight() + timeYOffset);
                        }
                        Theme.chat_msgMediaCheckDrawable.setAlpha((int) (255 * timeAlpha * alpha));
                        drawable = Theme.chat_msgMediaCheckDrawable;
                    }
                    if (useScale) {
                        canvas.save();
                        canvas.scale(scale, scale, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                    }
                    drawable.draw(canvas);
                    if (useScale) {
                        canvas.restore();
                    }
                    if (moveCheck) {
                        canvas.restore();
                    }
                    drawable.setAlpha(255);
                } else {
                    Drawable drawable;
                    if (moveCheck) {
                        canvas.save();
                    }
                    if (drawCheck1) {
                        if (moveCheck) {
                            canvas.translate(AndroidUtilities.dp(4) * (1f - progress), 0);
                        }
                        drawable = getThemedDrawable(drawSelectionBackground ? Theme.key_drawable_msgOutCheckReadSelected : Theme.key_drawable_msgOutCheckRead);
                        setDrawableBounds(drawable, layoutWidth -  AndroidUtilities.dp(22.5f) - drawable.getIntrinsicWidth(), layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 9 : 8) - drawable.getIntrinsicHeight() + timeYOffset);
                    } else {
                        drawable = getThemedDrawable(drawSelectionBackground ? Theme.key_drawable_msgOutCheckSelected : Theme.key_drawable_msgOutCheck);
                        setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(18.5f) - drawable.getIntrinsicWidth(), layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 9 : 8) - drawable.getIntrinsicHeight() + timeYOffset);
                    }
                    drawable.setAlpha((int) (255 * alpha));
                    if (useScale) {
                        canvas.save();
                        canvas.scale(scale, scale, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                    }
                    drawable.draw(canvas);
                    if (useScale) {
                        canvas.restore();
                    }
                    if (moveCheck) {
                        canvas.restore();
                    }
                    drawable.setAlpha(255);
                }
            }
            if (drawCheck1) {
                if (shouldDrawTimeOnMedia()) {
                    Drawable drawable;
                    if (currentMessageObject.shouldDrawWithoutBackground()) {
                        drawable = getThemedDrawable(Theme.key_drawable_msgStickerHalfCheck);
                        setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 23.5f : 21.5f) - drawable.getIntrinsicWidth(), timeY - drawable.getIntrinsicHeight() + timeYOffset);
                        drawable.setAlpha((int) (255 * timeAlpha * alpha));
                    } else {
                        drawable = Theme.chat_msgMediaHalfCheckDrawable;
                        setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(bigRadius ? 23.5f : 21.5f) - drawable.getIntrinsicWidth(), timeY - drawable.getIntrinsicHeight() + timeYOffset);
                        drawable.setAlpha((int) (255 * timeAlpha * alpha));
                    }
                    if (useScale || moveCheck) {
                        canvas.save();
                        canvas.scale(scale, scale, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                    }
                    drawable.draw(canvas);
                    if (useScale || moveCheck) {
                        canvas.restore();
                    }
                    drawable.setAlpha(255);
                } else {
                    Drawable drawable = getThemedDrawable(drawSelectionBackground ? Theme.key_drawable_msgOutHalfCheckSelected : Theme.key_drawable_msgOutHalfCheck);
                    setDrawableBounds(drawable, layoutWidth - AndroidUtilities.dp(18) - drawable.getIntrinsicWidth(), layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 9 : 8) - drawable.getIntrinsicHeight() + timeYOffset);
                    drawable.setAlpha((int) (255 * alpha));
                    if (useScale || moveCheck) {
                        canvas.save();
                        canvas.scale(scale, scale, drawable.getBounds().centerX(), drawable.getBounds().centerY());
                    }
                    drawable.draw(canvas);
                    if (useScale || moveCheck) {
                        canvas.restore();
                    }
                    drawable.setAlpha(255);
                }
            }
        }
        if (drawError) {
            int x;
            float y;
            if (shouldDrawTimeOnMedia()) {
                x = layoutWidth - AndroidUtilities.dp(34.5f);
                y = layoutHeight - AndroidUtilities.dp(26.5f) + timeYOffset;
            } else {
                x = layoutWidth - AndroidUtilities.dp(32);
                y = layoutHeight - AndroidUtilities.dp(pinnedBottom || pinnedTop ? 22 : 21) + timeYOffset;
            }
            rect.set(x, y, x + AndroidUtilities.dp(14), y + AndroidUtilities.dp(14));
            int oldAlpha = Theme.chat_msgErrorPaint.getAlpha();
            Theme.chat_msgErrorPaint.setAlpha((int) (oldAlpha * alpha));
            canvas.drawRoundRect(rect, AndroidUtilities.dp(1), AndroidUtilities.dp(1), Theme.chat_msgErrorPaint);
            Theme.chat_msgErrorPaint.setAlpha(oldAlpha);
            setDrawableBounds(Theme.chat_msgErrorDrawable, x + AndroidUtilities.dp(6), y + AndroidUtilities.dp(2));
            Theme.chat_msgErrorDrawable.setAlpha((int) (255 * alpha));
            if (useScale) {
                canvas.save();
                canvas.scale(scale, scale, Theme.chat_msgErrorDrawable.getBounds().centerX(), Theme.chat_msgErrorDrawable.getBounds().centerY());
            }
            Theme.chat_msgErrorDrawable.draw(canvas);
            Theme.chat_msgErrorDrawable.setAlpha(255);
            if (useScale) {
                canvas.restore();
            }
        }
    }

    public void drawOverlays(Canvas canvas) {
        if (!drawFromPinchToZoom && delegate != null && delegate.getPinchToZoomHelper() != null && delegate.getPinchToZoomHelper().isInOverlayModeFor(this)) {
            return;
        }
        long newAnimationTime = SystemClock.elapsedRealtime();
        long animationDt = newAnimationTime - lastAnimationTime;
        if (animationDt > 17) {
            animationDt = 17;
        }
        lastAnimationTime = newAnimationTime;

        if (currentMessageObject.hadAnimationNotReadyLoading && photoImage.getVisible() && !currentMessageObject.needDrawBluredPreview() && (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF)) {
            AnimatedFileDrawable animation = photoImage.getAnimation();
            if (animation != null && animation.hasBitmap()) {
                currentMessageObject.hadAnimationNotReadyLoading = false;
                updateButtonState(false, true, false);
            }
        }

        if (hasGamePreview) {

        } else if (currentMessageObject.type == MessageObject.TYPE_VIDEO || currentMessageObject.type == MessageObject.TYPE_PHOTO || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
            if (photoImage.getVisible()) {
                if (!currentMessageObject.needDrawBluredPreview()) {
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                        int oldAlpha = ((BitmapDrawable) Theme.chat_msgMediaMenuDrawable).getPaint().getAlpha();
                        if (drawMediaCheckBox) {
                            Theme.chat_msgMediaMenuDrawable.setAlpha((int) (oldAlpha * controlsAlpha * (1.0f - checkBoxAnimationProgress)));
                        } else {
                            Theme.chat_msgMediaMenuDrawable.setAlpha((int) (oldAlpha * controlsAlpha));
                        }
                        setDrawableBounds(Theme.chat_msgMediaMenuDrawable, otherX = (int) (photoImage.getImageX() + photoImage.getImageWidth() - AndroidUtilities.dp(14)), otherY = (int) (photoImage.getImageY() + AndroidUtilities.dp(8.1f)));
                        Theme.chat_msgMediaMenuDrawable.draw(canvas);
                        Theme.chat_msgMediaMenuDrawable.setAlpha(oldAlpha);
                    }
                }

                boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
                if (animatingNoSoundPlaying != playing) {
                    animatingNoSoundPlaying = playing;
                    animatingNoSound = playing ? 1 : 2;
                    animatingNoSoundProgress = playing ? 1.0f : 0.0f;
                }

                boolean fullWidth = true;
                if (currentPosition != null) {
                    int mask = MessageObject.POSITION_FLAG_LEFT | MessageObject.POSITION_FLAG_RIGHT;
                    fullWidth = (currentPosition.flags & mask) == mask;
                }

                if ((documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) && (buttonState == 1 || buttonState == 2 || buttonState == 0 || buttonState == 3 || buttonState == -1 || currentMessageObject.needDrawBluredPreview())) {
                    if (autoPlayingMedia) {
                        updatePlayingMessageProgress();
                    }

                    if ((infoLayout != null || loadingProgressLayout != null) && (!forceNotDrawTime || autoPlayingMedia || drawVideoImageButton || animatingLoadingProgressProgress != 0 || (fullWidth && docTitleLayout != null) || (loadingProgressLayout != null && currentPosition != null && (buttonState == 1 || (buttonState == 3 && miniButtonState == 1))))) {
                        boolean drawLoadingProgress;
                        float alpha = 0;
                        boolean drawDocTitleLayout;
                        float loadingProgressAlpha = 1f;
                        if (!fullWidth) {
                            drawLoadingProgress = true;
                            drawDocTitleLayout = false;
                            loadingProgressAlpha = animatingLoadingProgressProgress;
                        } else {
                            drawLoadingProgress = (buttonState == 1 || miniButtonState == 1 || animatingLoadingProgressProgress != 0) && !currentMessageObject.isSecretMedia() &&
                                    (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT);
                            if (currentMessageObject.type == MessageObject.TYPE_VIDEO || currentMessageObject.type == 8 || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                                alpha = currentMessageObject.needDrawBluredPreview() && docTitleLayout == null ? 0 : animatingDrawVideoImageButtonProgress;
                            }
                            drawDocTitleLayout = alpha > 0 && docTitleLayout != null;
                            if (!drawDocTitleLayout && (drawLoadingProgress || infoLayout == null)) {
                                loadingProgressAlpha = animatingLoadingProgressProgress;
                            }
                        }

                        Theme.chat_infoPaint.setColor(getThemedColor(Theme.key_chat_mediaInfoText));
                        int x1 = (int) (photoImage.getImageX() + AndroidUtilities.dp(4));
                        int y1 = (int) (photoImage.getImageY() + AndroidUtilities.dp(4));

                        int imageW;
                        int infoW;
                        if (autoPlayingMedia && (!playing || animatingNoSound != 0)) {
                            imageW = (int) ((Theme.chat_msgNoSoundDrawable.getIntrinsicWidth() + AndroidUtilities.dp(4)) * animatingNoSoundProgress);
                        } else {
                            imageW = 0;
                        }
                        if (drawLoadingProgress && loadingProgressLayout != null){
                            imageW = 0;
                            infoW = (int) loadingProgressLayout.getLineWidth(0);
                        } else {
                            infoW = infoWidth;
                        }

                        int w = (int) Math.ceil(infoW + AndroidUtilities.dp(8) + imageW + (Math.max(infoW + (infoWidth == infoW ? imageW : 0), docTitleWidth) + (canStreamVideo ? AndroidUtilities.dp(32) : 0) - infoW - imageW) * alpha);

                        if (alpha != 0 && docTitleLayout == null) {
                            alpha = 0;
                        }

                        canvas.save();
                        canvas.scale(loadingProgressAlpha, loadingProgressAlpha, x1, y1);
                        int oldAlpha = getThemedPaint(Theme.key_paint_chatTimeBackground).getAlpha();
                        getThemedPaint(Theme.key_paint_chatTimeBackground).setAlpha((int) (oldAlpha * controlsAlpha * loadingProgressAlpha));
                        if (drawDocTitleLayout || (drawLoadingProgress && loadingProgressLayout != null) || (!drawLoadingProgress && infoLayout != null)) {
                            rect.set(x1, y1, x1 + w, y1 + AndroidUtilities.dp(16.5f + 15.5f * alpha));
                            int[] rad = photoImage.getRoundRadius();
                            int r = Math.min(AndroidUtilities.dp(8), Math.max(rad[0], rad[1]));
                            canvas.drawRoundRect(rect, r, r, getThemedPaint(Theme.key_paint_chatTimeBackground));
                        }

                        Theme.chat_infoPaint.setAlpha((int) (255 * controlsAlpha * loadingProgressAlpha));

                        canvas.translate(noSoundCenterX = (int) (photoImage.getImageX() + AndroidUtilities.dp(8 + (canStreamVideo ? 30 * alpha : 0))), photoImage.getImageY() + AndroidUtilities.dp(5.5f + 0.2f * alpha));
                        if (infoLayout != null && (!drawLoadingProgress || drawDocTitleLayout)) {
                            infoLayout.draw(canvas);
                        }
                        if (imageW != 0 && (!drawLoadingProgress || drawDocTitleLayout)) {
                            canvas.save();
                            Theme.chat_msgNoSoundDrawable.setAlpha((int) (255 * animatingNoSoundProgress * animatingNoSoundProgress * controlsAlpha));
                            int size = AndroidUtilities.dp(14 * animatingNoSoundProgress);
                            int y = (AndroidUtilities.dp(14) - size) / 2;
                            int offset = infoWidth + AndroidUtilities.dp(4);
                            canvas.translate(offset, 0);
                            Theme.chat_msgNoSoundDrawable.setBounds(0, y, size, y + size);
                            Theme.chat_msgNoSoundDrawable.draw(canvas);
                            noSoundCenterX += offset + size / 2;
                            canvas.restore();
                        }
                        if (drawLoadingProgress && loadingProgressLayout != null) {
                            canvas.save();
                            if (drawDocTitleLayout) {
                                Theme.chat_infoPaint.setAlpha((int) (255 * controlsAlpha * alpha));
                                canvas.translate(0, AndroidUtilities.dp(14.3f * alpha));
                            }
                            loadingProgressLayout.draw(canvas);
                            canvas.restore();
                        } else if (drawDocTitleLayout) {
                            Theme.chat_infoPaint.setAlpha((int) (255 * controlsAlpha * alpha));
                            canvas.translate(0, AndroidUtilities.dp(14.3f * alpha));
                            docTitleLayout.draw(canvas);
                        }
                        canvas.restore();
                        Theme.chat_infoPaint.setAlpha(255);
                        getThemedPaint(Theme.key_paint_chatTimeBackground).setAlpha(oldAlpha);
                    }
                }
                if (animatingDrawVideoImageButton == 1) {
                    animatingDrawVideoImageButtonProgress -= animationDt / 160.0f;
                    if (animatingDrawVideoImageButtonProgress <= 0) {
                        animatingDrawVideoImageButtonProgress = 0;
                        animatingDrawVideoImageButton = 0;
                    }
                    invalidate();
                } else if (animatingDrawVideoImageButton == 2) {
                    animatingDrawVideoImageButtonProgress += animationDt / 160.0f;
                    if (animatingDrawVideoImageButtonProgress >= 1) {
                        animatingDrawVideoImageButtonProgress = 1;
                        animatingDrawVideoImageButton = 0;
                    }
                    invalidate();
                }
                if (animatingNoSound == 1) {
                    animatingNoSoundProgress -= animationDt / 180.0f;
                    if (animatingNoSoundProgress <= 0.0f) {
                        animatingNoSoundProgress = 0.0f;
                        animatingNoSound = 0;
                    }
                    invalidate();
                } else if (animatingNoSound == 2) {
                    animatingNoSoundProgress += animationDt / 180.0f;
                    if (animatingNoSoundProgress >= 1.0f) {
                        animatingNoSoundProgress = 1.0f;
                        animatingNoSound = 0;
                    }
                    invalidate();
                }

                float animatingToLoadingProgress = (buttonState == 1 || miniButtonState == 1) && loadingProgressLayout != null ? 1f : 0f;
                if (animatingToLoadingProgress == 0f && infoLayout != null && fullWidth) {
                    animatingLoadingProgressProgress = 0f;
                }
                if (animatingLoadingProgressProgress < animatingToLoadingProgress) {
                    animatingLoadingProgressProgress += animationDt / 160.0f;
                    if (animatingLoadingProgressProgress > animatingToLoadingProgress) {
                        animatingLoadingProgressProgress = animatingToLoadingProgress;
                    }
                    invalidate();
                } else if (animatingLoadingProgressProgress != animatingToLoadingProgress) {
                    animatingLoadingProgressProgress -= animationDt / 160.0f;
                    if (animatingLoadingProgressProgress < animatingToLoadingProgress) {
                        animatingLoadingProgressProgress = animatingToLoadingProgress;
                    }
                    invalidate();
                }
            }
        } else if (currentMessageObject.type == 4) {
            if (docTitleLayout != null) {
                if (currentMessageObject.isOutOwner()) {
                    Theme.chat_locationTitlePaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
                    Theme.chat_locationAddressPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outVenueInfoSelectedText : Theme.key_chat_outVenueInfoText));
                } else {
                    Theme.chat_locationTitlePaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
                    Theme.chat_locationAddressPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inVenueInfoSelectedText : Theme.key_chat_inVenueInfoText));
                }

                if (currentMessageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGeoLive) {
                    int cy = (int) (photoImage.getImageY2() + AndroidUtilities.dp(30));
                    if (!locationExpired || transitionParams.animateLocationIsExpired) {
                        forceNotDrawTime = true;
                        float progress;
                        String text;
                        StaticLayout docTitleLayout = this.docTitleLayout;
                        StaticLayout infoLayout = this.infoLayout;
                        float alpha = 1f;
                        if (transitionParams.animateLocationIsExpired){
                            progress = transitionParams.lastDrawLocationExpireProgress;
                            text = transitionParams.lastDrawLocationExpireText;
                            docTitleLayout = transitionParams.lastDrawDocTitleLayout;
                            infoLayout = transitionParams.lastDrawInfoLayout;
                            alpha = 1f - transitionParams.animateChangeProgress;
                        } else {
                            progress = 1.0f - Math.abs(ConnectionsManager.getInstance(currentAccount).getCurrentTime() - currentMessageObject.messageOwner.date) / (float) currentMessageObject.messageOwner.media.period;
                            text = LocaleController.formatLocationLeftTime(Math.abs(currentMessageObject.messageOwner.media.period - (ConnectionsManager.getInstance(currentAccount).getCurrentTime() - currentMessageObject.messageOwner.date)));
                        }

                        rect.set(photoImage.getImageX2() - AndroidUtilities.dp(43), cy - AndroidUtilities.dp(15), photoImage.getImageX2() - AndroidUtilities.dp(13), cy + AndroidUtilities.dp(15));
                        if (currentMessageObject.isOutOwner()) {
                            Theme.chat_radialProgress2Paint.setColor(getThemedColor(Theme.key_chat_outInstant));
                            Theme.chat_livePaint.setColor(getThemedColor(Theme.key_chat_outInstant));
                        } else {
                            Theme.chat_radialProgress2Paint.setColor(getThemedColor(Theme.key_chat_inInstant));
                            Theme.chat_livePaint.setColor(getThemedColor(Theme.key_chat_inInstant));
                        }

                        int docTitleAlpha = Theme.chat_locationTitlePaint.getAlpha();
                        int infoAlpha = Theme.chat_locationAddressPaint.getAlpha();
                        int liveAplha = Theme.chat_livePaint.getAlpha();
                        if (alpha != 1f) {
                            Theme.chat_locationTitlePaint.setAlpha((int) (docTitleAlpha * alpha));
                            Theme.chat_locationAddressPaint.setAlpha((int) (infoAlpha * alpha));
                            Theme.chat_livePaint.setAlpha((int) (liveAplha * alpha));
                            canvas.save();
                            canvas.translate(0, -AndroidUtilities.dp(50) * transitionParams.animateChangeProgress);
                        }

                        Theme.chat_radialProgress2Paint.setAlpha((int) (50 * alpha));
                        canvas.drawCircle(rect.centerX(), rect.centerY(), AndroidUtilities.dp(15), Theme.chat_radialProgress2Paint);
                        Theme.chat_radialProgress2Paint.setAlpha((int) (255 * alpha));
                        canvas.drawArc(rect, -90, -360 * progress, false, Theme.chat_radialProgress2Paint);

                        float w = Theme.chat_livePaint.measureText(text);
                        canvas.drawText(text, rect.centerX() - w / 2, cy + AndroidUtilities.dp(4), Theme.chat_livePaint);

                        if (docTitleLayout != null && infoLayout != null) {
                            canvas.save();
                            canvas.translate(photoImage.getImageX() + AndroidUtilities.dp(10), photoImage.getImageY2() + AndroidUtilities.dp(10));
                            docTitleLayout.draw(canvas);
                            canvas.translate(0, AndroidUtilities.dp(23));
                            infoLayout.draw(canvas);
                            canvas.restore();
                        }

                        if (alpha != 1f) {
                            Theme.chat_locationTitlePaint.setAlpha(docTitleAlpha);
                            Theme.chat_locationAddressPaint.setAlpha(infoAlpha);
                            Theme.chat_livePaint.setAlpha(liveAplha);
                            canvas.restore();
                        }

                        transitionParams.lastDrawLocationExpireProgress = progress;
                        transitionParams.lastDrawLocationExpireText = text;
                        transitionParams.lastDrawDocTitleLayout = docTitleLayout;
                        transitionParams.lastDrawInfoLayout = infoLayout;
                    } else {
                        transitionParams.lastDrawLocationExpireText = null;
                        transitionParams.lastDrawDocTitleLayout = null;
                        transitionParams.lastDrawInfoLayout = null;
                    }

                    int cx = (int) (photoImage.getImageX() + photoImage.getImageWidth() / 2 - AndroidUtilities.dp(31));
                    cy = (int) (photoImage.getImageY() + photoImage.getImageHeight() / 2 - AndroidUtilities.dp(38));
                    setDrawableBounds(Theme.chat_msgAvatarLiveLocationDrawable, cx, cy);
                    Theme.chat_msgAvatarLiveLocationDrawable.draw(canvas);

                    locationImageReceiver.setImageCoords(cx + AndroidUtilities.dp(5.0f), cy + AndroidUtilities.dp(5.0f), AndroidUtilities.dp(52), AndroidUtilities.dp(52));
                    locationImageReceiver.draw(canvas);
                } else {
                    canvas.save();
                    canvas.translate(photoImage.getImageX() + AndroidUtilities.dp(6), photoImage.getImageY2() + AndroidUtilities.dp(8));
                    docTitleLayout.draw(canvas);
                    if (infoLayout != null) {
                        canvas.translate(0, AndroidUtilities.dp(21));
                        infoLayout.draw(canvas);
                    }
                    canvas.restore();
                }
            }
        } else if (currentMessageObject.type == 16) {
            if (currentMessageObject.isOutOwner()) {
                Theme.chat_audioTitlePaint.setColor(getThemedColor(Theme.key_chat_messageTextOut));
                Theme.chat_contactPhonePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outTimeSelectedText : Theme.key_chat_outTimeText));
            } else {
                Theme.chat_audioTitlePaint.setColor(getThemedColor(Theme.key_chat_messageTextIn));
                Theme.chat_contactPhonePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inTimeSelectedText : Theme.key_chat_inTimeText));
            }
            forceNotDrawTime = true;
            int x;
            if (currentMessageObject.isOutOwner()) {
                x = layoutWidth - backgroundWidth + AndroidUtilities.dp(16);
            } else {
                if (isChat && !isThreadPost && currentMessageObject.needDrawAvatar()) {
                    x = AndroidUtilities.dp(74);
                } else {
                    x = AndroidUtilities.dp(25);
                }
            }
            otherX = x;
            if (titleLayout != null) {
                canvas.save();
                canvas.translate(x, AndroidUtilities.dp(12) + namesOffset);
                titleLayout.draw(canvas);
                canvas.restore();
            }
            if (docTitleLayout != null) {
                canvas.save();
                canvas.translate(x + AndroidUtilities.dp(19), AndroidUtilities.dp(37) + namesOffset);
                docTitleLayout.draw(canvas);
                canvas.restore();
            }
            Drawable icon;
            Drawable phone;
            int idx = currentMessageObject.isVideoCall() ? 1 : 0;
            if (currentMessageObject.isOutOwner()) {
                icon = Theme.chat_msgCallUpGreenDrawable;
                if (currentMessageObject.isVideoCall()) {
                    phone = getThemedDrawable(isDrawSelectionBackground() ? Theme.key_drawable_msgOutCallVideoSelected : Theme.key_drawable_msgOutCallVideo);
                } else {
                    phone = getThemedDrawable(isDrawSelectionBackground() ? Theme.key_drawable_msgOutCallAudioSelected : Theme.key_drawable_msgOutCallAudio);
                }
            } else {
                TLRPC.PhoneCallDiscardReason reason = currentMessageObject.messageOwner.action.reason;
                if (reason instanceof TLRPC.TL_phoneCallDiscardReasonMissed || reason instanceof TLRPC.TL_phoneCallDiscardReasonBusy) {
                    icon = Theme.chat_msgCallDownRedDrawable;
                } else {
                    icon = Theme.chat_msgCallDownGreenDrawable;
                }
                phone = isDrawSelectionBackground() ? Theme.chat_msgInCallSelectedDrawable[idx] : Theme.chat_msgInCallDrawable[idx];
            }
            setDrawableBounds(icon, x - AndroidUtilities.dp(1), AndroidUtilities.dp(37) + namesOffset);
            icon.draw(canvas);

            if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null && selectorDrawableMaskType[0] == 4) {
                selectorDrawable[0].draw(canvas);
            }

            if (!pinnedBottom && !pinnedTop) {
                otherY = AndroidUtilities.dp(18.5f);
            } else if (pinnedBottom && pinnedTop) {
                otherY = AndroidUtilities.dp(18);
            } else if (!pinnedBottom) {
                otherY = AndroidUtilities.dp(17);
            } else {
                otherY = AndroidUtilities.dp(19);
            }
            setDrawableBounds(phone, x + AndroidUtilities.dp(idx == 0 ? 201 : 200), otherY);
            phone.draw(canvas);
        } else if (currentMessageObject.type == MessageObject.TYPE_POLL) {
            long newTime = System.currentTimeMillis();
            long dt = newTime - voteLastUpdateTime;
            if (dt > 17) {
                dt = 17;
            }
            voteLastUpdateTime = newTime;

            int color1;
            int color2;
            if (currentMessageObject.isOutOwner()) {
                color1 = getThemedColor(Theme.key_chat_messageTextOut);
                color2 = getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outTimeSelectedText : Theme.key_chat_outTimeText);
            } else {
                color1 = getThemedColor(Theme.key_chat_messageTextIn);
                color2 = getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inTimeSelectedText : Theme.key_chat_inTimeText);
            }
            Theme.chat_audioTitlePaint.setColor(color1);
            Theme.chat_audioPerformerPaint.setColor(color1);
            Theme.chat_instantViewPaint.setColor(color1);
            Theme.chat_timePaint.setColor(color2);
            Theme.chat_livePaint.setColor(color2);
            Theme.chat_locationAddressPaint.setColor(color2);

            canvas.save();
            if (transitionParams.animateForwardedLayout) {
                float y = namesOffset * transitionParams.animateChangeProgress + transitionParams.animateForwardedNamesOffset * (1f - transitionParams.animateChangeProgress);
                if (currentMessageObject.needDrawForwarded()) {
                    y -= namesOffset;
                }
                canvas.translate(0, y);
            }
            int x;
            if (currentMessageObject.isOutOwner()) {
                x = layoutWidth - backgroundWidth + AndroidUtilities.dp(11);
            } else {
                if (isChat && !isThreadPost && currentMessageObject.needDrawAvatar()) {
                    x = AndroidUtilities.dp(68);
                } else {
                    x = AndroidUtilities.dp(20);
                }
            }
            if (titleLayout != null) {
                canvas.save();
                canvas.translate(x + getExtraTextX(), AndroidUtilities.dp(15) + namesOffset);
                titleLayout.draw(canvas);
                canvas.restore();
            }
            int y = (titleLayout != null ? titleLayout.getHeight() : 0) + AndroidUtilities.dp(20) + namesOffset;
            if (docTitleLayout != null) {
                canvas.save();
                canvas.translate(x + docTitleOffsetX + getExtraTextX(), y);
                docTitleLayout.draw(canvas);
                canvas.restore();

                TLRPC.TL_messageMediaPoll media = (TLRPC.TL_messageMediaPoll) currentMessageObject.messageOwner.media;
                if (lastPoll.quiz && (pollVoted || pollClosed) && !TextUtils.isEmpty(media.results.solution)) {
                    Drawable drawable = getThemedDrawable(currentMessageObject.isOutOwner() ? Theme.key_drawable_chat_pollHintDrawableOut : Theme.key_drawable_chat_pollHintDrawableIn);
                    if (pollVoteInProgress) {
                        drawable.setAlpha((int) (255 * pollAnimationProgress));
                    } else {
                        drawable.setAlpha(255);
                    }
                    if (docTitleOffsetX < 0 || docTitleOffsetX == 0 && docTitleLayout.getLineLeft(0) == 0) {
                        pollHintX = currentBackgroundDrawable.getBounds().right - drawable.getIntrinsicWidth() - AndroidUtilities.dp(currentMessageObject.isOutOwner() ? 17 : 11);
                    } else {
                        pollHintX = getCurrentBackgroundLeft() + AndroidUtilities.dp(11);
                    }
                    pollHintY = y - AndroidUtilities.dp(6);
                    int cx = pollHintX + drawable.getIntrinsicWidth() / 2;
                    int cy = pollHintY + drawable.getIntrinsicHeight() / 2;
                    float scale = hintButtonVisible && hintButtonProgress < 1 ? AnimationProperties.overshootInterpolator.getInterpolation(hintButtonProgress) : hintButtonProgress;
                    int w = (int) (drawable.getIntrinsicWidth() * scale);
                    int h = (int) (drawable.getIntrinsicHeight() * scale);
                    drawable.setBounds(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
                    drawable.draw(canvas);
                } else {
                    pollHintX = -1;
                }

                if (pollAvatarImages != null && !isBot) {
                    int toAdd;
                    int ax;
                    int lineLeft = (int) Math.ceil(docTitleLayout.getLineLeft(0));
                    if (docTitleOffsetX != 0 || lineLeft != 0) {
                        toAdd = -AndroidUtilities.dp(13);
                        if (docTitleOffsetX != 0) {
                            ax = x + docTitleOffsetX - AndroidUtilities.dp(7 + 16) - getExtraTextX();
                        } else {
                            ax = x + lineLeft - AndroidUtilities.dp(7 + 16) - getExtraTextX();
                        }
                    } else {
                        toAdd = AndroidUtilities.dp(13);
                        ax = x + (int) Math.ceil(docTitleLayout.getLineWidth(0)) + AndroidUtilities.dp(7) + getExtraTextX();
                    }
                    for (int a = pollAvatarImages.length - 1; a >= 0; a--) {
                        if (!pollAvatarImagesVisible[a] || !pollAvatarImages[a].hasImageSet()) {
                            continue;
                        }
                        pollAvatarImages[a].setImageX(ax + toAdd * a);
                        pollAvatarImages[a].setImageY(y - AndroidUtilities.dp(1));
                        if (a != pollAvatarImages.length - 1) {
                            canvas.drawCircle(pollAvatarImages[a].getCenterX(), pollAvatarImages[a].getCenterY(), AndroidUtilities.dp(9), currentBackgroundDrawable.getPaint());
                        }
                        if (animatePollAvatars && animatePollAnswerAlpha) {
                            float alpha = Math.min(pollUnvoteInProgress ? (1.0f - pollAnimationProgress) / 0.3f : pollAnimationProgress, 1.0f);
                            pollAvatarImages[a].setAlpha(alpha);
                        }
                        pollAvatarImages[a].draw(canvas);
                    }
                }
            }
            if ((!pollClosed && !pollVoted || pollVoteInProgress) && lastPoll.quiz && lastPoll.close_period != 0) {
                long currentTime = ConnectionsManager.getInstance(currentAccount).getCurrentTimeMillis();
                long time = Math.max(0, ((long) lastPoll.close_date) * 1000 - currentTime);
                if (closeTimeText == null || lastPollCloseTime != time) {
                    closeTimeText = AndroidUtilities.formatDurationNoHours((int) Math.ceil(time / 1000.0f), false);
                    closeTimeWidth = (int) Math.ceil(Theme.chat_timePaint.measureText(closeTimeText));
                    lastPollCloseTime = time;
                }
                if (time <= 0 && !pollClosed) {
                    if (currentMessageObject.pollLastCheckTime + 1000 < SystemClock.elapsedRealtime()) {
                        currentMessageObject.pollLastCheckTime = 0;
                    }
                    delegate.needReloadPolls();
                }
                int tx = currentBackgroundDrawable.getBounds().right - closeTimeWidth - AndroidUtilities.dp(currentMessageObject.isOutOwner() ? 40 : 34);
                if (time <= 5000) {
                    Theme.chat_timePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outPollWrongAnswer : Theme.key_chat_inPollWrongAnswer));
                }
                if (animatePollAnswer) {
                    Theme.chat_timePaint.setAlpha((int) (255 * (1.0f - pollAnimationProgress)));
                }
                canvas.drawText(closeTimeText, tx, y + AndroidUtilities.dp(11), Theme.chat_timePaint);
                Theme.chat_pollTimerPaint.setColor(Theme.chat_timePaint.getColor());
                tx += closeTimeWidth + AndroidUtilities.dp(13);
                int rad = AndroidUtilities.dp(5.1f);
                int ty = y + AndroidUtilities.dp(6);
                if (time <= 60000) {
                    rect.set(tx - rad, ty - rad, tx + rad, ty + rad);
                    float radProgress = -360 * (time / (Math.min(60, lastPoll.close_period) * 1000.0f));
                    canvas.drawArc(rect, -90, radProgress, false, Theme.chat_pollTimerPaint);
                    timerParticles.draw(canvas, Theme.chat_pollTimerPaint, rect, radProgress, pollVoteInProgress ? (1.0f - pollAnimationProgress) : 1.0f);
                } else {
                    canvas.drawCircle(tx, ty, rad, Theme.chat_pollTimerPaint);
                }
                if (time > 60000 || timerTransitionProgress != 0.0f) {
                    Theme.chat_pollTimerPaint.setAlpha((int) (255 * timerTransitionProgress));
                    canvas.drawLine(tx - AndroidUtilities.dp(2.1f) * timerTransitionProgress, ty - AndroidUtilities.dp(7.5f), tx + AndroidUtilities.dp(2.1f) * timerTransitionProgress, ty - AndroidUtilities.dp(7.5f), Theme.chat_pollTimerPaint);
                    canvas.drawLine(tx, ty - AndroidUtilities.dp(3) * timerTransitionProgress, tx, ty, Theme.chat_pollTimerPaint);
                    if (time <= 60000) {
                        timerTransitionProgress -= dt / 180.0f;
                        if (timerTransitionProgress < 0) {
                            timerTransitionProgress = 0;
                        }
                    }
                }
                invalidate();
            }
            if (Build.VERSION.SDK_INT >= 21 && selectorDrawable[0] != null && (selectorDrawableMaskType[0] == 1 || selectorDrawableMaskType[0] == 3)) {
                if (selectorDrawableMaskType[0] == 3) {
                    canvas.save();
                    canvas.scale(hintButtonProgress, hintButtonProgress, selectorDrawable[0].getBounds().centerX(), selectorDrawable[0].getBounds().centerY());
                }
                selectorDrawable[0].draw(canvas);
                if (selectorDrawableMaskType[0] == 3) {
                    canvas.restore();
                }
            }
            int lastVoteY = 0;
            for (int a = 0, N = pollButtons.size(); a < N; a++) {
                PollButton button = pollButtons.get(a);
                button.x = x;
                canvas.save();
                canvas.translate(x + AndroidUtilities.dp(35), button.y + namesOffset);
                button.title.draw(canvas);
                int alpha = (int) (animatePollAnswerAlpha ? 255 * Math.min((pollUnvoteInProgress ? 1.0f - pollAnimationProgress : pollAnimationProgress) / 0.3f, 1.0f) : 255);
                if (pollVoted || pollClosed || animatePollAnswerAlpha) {
                    if (lastPoll.quiz && pollVoted && button.chosen) {
                        String key;
                        if (button.correct) {
                            key = currentMessageObject.isOutOwner() ? Theme.key_chat_outPollCorrectAnswer : Theme.key_chat_inPollCorrectAnswer;
                        } else {
                            key = currentMessageObject.isOutOwner() ? Theme.key_chat_outPollWrongAnswer : Theme.key_chat_inPollWrongAnswer;
                        }
                        if (!currentBackgroundDrawable.hasGradient() || Theme.hasThemeKey(key)) {
                            Theme.chat_docBackPaint.setColor(getThemedColor(key));
                        } else {
                            Theme.chat_docBackPaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outAudioSeekbarFill : Theme.key_chat_inAudioSeekbarFill));
                        }
                    } else {
                        Theme.chat_docBackPaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outAudioSeekbarFill : Theme.key_chat_inAudioSeekbarFill));
                    }
                    if (animatePollAnswerAlpha) {
                        float oldAlpha = Theme.chat_instantViewPaint.getAlpha() / 255.0f;
                        Theme.chat_instantViewPaint.setAlpha((int) (alpha * oldAlpha));
                        oldAlpha = Theme.chat_docBackPaint.getAlpha() / 255.0f;
                        Theme.chat_docBackPaint.setAlpha((int) (alpha * oldAlpha));
                    }

                    int currentPercent = (int) Math.ceil(button.prevPercent + (button.percent - button.prevPercent) * pollAnimationProgress);
                    String text = String.format("%d%%", currentPercent);
                    int width = (int) Math.ceil(Theme.chat_instantViewPaint.measureText(text));
                    canvas.drawText(text, -AndroidUtilities.dp(6.5f) - width, AndroidUtilities.dp(14), Theme.chat_instantViewPaint);

                    width = backgroundWidth - AndroidUtilities.dp(76);
                    float currentPercentProgress = button.prevPercentProgress + (button.percentProgress - button.prevPercentProgress) * pollAnimationProgress;
                    rect.set(0, button.height + AndroidUtilities.dp(6), width * currentPercentProgress, button.height + AndroidUtilities.dp(11));
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(2), AndroidUtilities.dp(2), Theme.chat_docBackPaint);

                    if (button.chosen || button.prevChosen || lastPoll.quiz && button.correct && (pollVoted || pollClosed)) {
                        float cx = rect.left - AndroidUtilities.dp(13.5f);
                        float cy = rect.centerY();
                        canvas.drawCircle(cx, cy, AndroidUtilities.dp(7), Theme.chat_docBackPaint);
                        Drawable drawable;
                        if (lastPoll.quiz && button.chosen && !button.correct) {
                            drawable = Theme.chat_pollCrossDrawable[currentMessageObject.isOutOwner() ? 1 : 0];
                        } else {
                            drawable = Theme.chat_pollCheckDrawable[currentMessageObject.isOutOwner() ? 1 : 0];
                        }
                        drawable.setAlpha(alpha);
                        setDrawableBounds(drawable, cx - drawable.getIntrinsicWidth() / 2, cy - drawable.getIntrinsicHeight() / 2);
                        drawable.draw(canvas);
                    }
                }

                if (!pollVoted && !pollClosed || animatePollAnswerAlpha) {
                    if (isDrawSelectionBackground()) {
                        Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outVoiceSeekbarSelected : Theme.key_chat_inVoiceSeekbarSelected));
                    } else {
                        Theme.chat_replyLinePaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outVoiceSeekbar : Theme.key_chat_inVoiceSeekbar));
                    }
                    if (animatePollAnswerAlpha) {
                        float oldAlpha = Theme.chat_replyLinePaint.getAlpha() / 255.0f;
                        Theme.chat_replyLinePaint.setAlpha((int) ((255 - alpha) * oldAlpha));
                    }
                    canvas.drawLine(-AndroidUtilities.dp(2), button.height + AndroidUtilities.dp(13), backgroundWidth - AndroidUtilities.dp(58), button.height + AndroidUtilities.dp(13), Theme.chat_replyLinePaint);
                    if (pollVoteInProgress && a == pollVoteInProgressNum) {
                        Theme.chat_instantViewRectPaint.setColor(getThemedColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outAudioSeekbarFill : Theme.key_chat_inAudioSeekbarFill));
                        if (animatePollAnswerAlpha) {
                            float oldAlpha = Theme.chat_instantViewRectPaint.getAlpha() / 255.0f;
                            Theme.chat_instantViewRectPaint.setAlpha((int) ((255 - alpha) * oldAlpha));
                        }
                        rect.set(-AndroidUtilities.dp(22) - AndroidUtilities.dp(8.5f), AndroidUtilities.dp(9) - AndroidUtilities.dp(8.5f), -AndroidUtilities.dp(23) + AndroidUtilities.dp(8.5f), AndroidUtilities.dp(9) + AndroidUtilities.dp(8.5f));
                        canvas.drawArc(rect, voteRadOffset, voteCurrentCircleLength, false, Theme.chat_instantViewRectPaint);
                    } else {
                        if (currentMessageObject.isOutOwner()) {
                            Theme.chat_instantViewRectPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outMenuSelected : Theme.key_chat_outMenu));
                        } else {
                            Theme.chat_instantViewRectPaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inMenuSelected : Theme.key_chat_inMenu));
                        }
                        if (animatePollAnswerAlpha) {
                            float oldAlpha = Theme.chat_instantViewRectPaint.getAlpha() / 255.0f;
                            Theme.chat_instantViewRectPaint.setAlpha((int) ((255 - alpha) * oldAlpha));
                        }
                        canvas.drawCircle(-AndroidUtilities.dp(22), AndroidUtilities.dp(9), AndroidUtilities.dp(8.5f), Theme.chat_instantViewRectPaint);
                        if (lastPoll.multiple_choice) {
                            int size = AndroidUtilities.dp(8.5f);
                            String key = Theme.key_checkboxCheck;
                            if (currentMessageObject.isOutOwner()) {
                                if (getThemedColor(key) == 0xffffffff) {
                                    key = Theme.key_chat_outBubble;
                                }
                            }
                            pollCheckBox[a].setColor(null, currentMessageObject.isOutOwner() ? Theme.key_chat_outAudioSeekbarFill : Theme.key_chat_inAudioSeekbarFill, key);
                            pollCheckBox[a].setBounds(-AndroidUtilities.dp(22) - size / 2, AndroidUtilities.dp(9) - size / 2, size, size);
                            pollCheckBox[a].draw(canvas);
                        }
                    }
                }
                canvas.restore();
                if (a == N - 1) {
                    lastVoteY = button.y + namesOffset + button.height;
                }
            }
            if (drawInstantView) {
                int textX = getCurrentBackgroundLeft() + AndroidUtilities.dp(currentMessageObject.isOutOwner() || mediaBackground || drawPinnedBottom ? 2 : 8);
                int instantY = lastVoteY + AndroidUtilities.dp(13);
                if (currentMessageObject.isOutOwner()) {
                    Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_outPreviewInstantText));
                } else {
                    Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
                }

                instantButtonRect.set(textX, instantY, textX + instantWidth, instantY + AndroidUtilities.dp(44));
                if (selectorDrawable[0] != null && selectorDrawableMaskType[0] == 2) {
                    selectorDrawable[0].setBounds(textX, instantY, textX + instantWidth, instantY + AndroidUtilities.dp(44));
                    selectorDrawable[0].draw(canvas);
                }
                if (instantViewLayout != null) {
                    canvas.save();
                    canvas.translate(textX + instantTextX, instantY + AndroidUtilities.dp(14.5f));
                    instantViewLayout.draw(canvas);
                    canvas.restore();
                }
            } else if (infoLayout != null) {
                if (lastPoll.public_voters || lastPoll.multiple_choice) {
                    lastVoteY += AndroidUtilities.dp(6);
                }
                canvas.save();
                canvas.translate(x + infoX, lastVoteY + AndroidUtilities.dp(22));
                infoLayout.draw(canvas);
                canvas.restore();
            }
            updatePollAnimations(dt);
            canvas.restore();
        } else if (currentMessageObject.type == 12) {
            if (currentMessageObject.isOutOwner()) {
                Theme.chat_contactNamePaint.setColor(getThemedColor(Theme.key_chat_outContactNameText));
                Theme.chat_contactPhonePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_outContactPhoneSelectedText : Theme.key_chat_outContactPhoneText));
            } else {
                Theme.chat_contactNamePaint.setColor(getThemedColor(Theme.key_chat_inContactNameText));
                Theme.chat_contactPhonePaint.setColor(getThemedColor(isDrawSelectionBackground() ? Theme.key_chat_inContactPhoneSelectedText : Theme.key_chat_inContactPhoneText));
            }
            if (titleLayout != null) {
                canvas.save();
                canvas.translate(photoImage.getImageX() + photoImage.getImageWidth() + AndroidUtilities.dp(9), AndroidUtilities.dp(16) + namesOffset);
                titleLayout.draw(canvas);
                canvas.restore();
            }
            if (docTitleLayout != null) {
                canvas.save();
                canvas.translate(photoImage.getImageX() + photoImage.getImageWidth() + AndroidUtilities.dp(9), AndroidUtilities.dp(39) + namesOffset);
                docTitleLayout.draw(canvas);
                canvas.restore();
            }

            Drawable menuDrawable;
            if (currentMessageObject.isOutOwner()) {
                menuDrawable = getThemedDrawable(isDrawSelectionBackground() ? Theme.key_drawable_msgOutMenuSelected : Theme.key_drawable_msgOutMenu);
            } else {
                menuDrawable = isDrawSelectionBackground() ? Theme.chat_msgInMenuSelectedDrawable : Theme.chat_msgInMenuDrawable;
            }
            setDrawableBounds(menuDrawable, otherX = (int) (photoImage.getImageX() + backgroundWidth - AndroidUtilities.dp(48)), otherY = (int) (photoImage.getImageY() - AndroidUtilities.dp(2)));
            menuDrawable.draw(canvas);

            if (drawInstantView) {
                int textX = (int) (photoImage.getImageX() - AndroidUtilities.dp(2));
                int instantY = currentBackgroundDrawable.getBounds().bottom - AndroidUtilities.dp(36 + 28);
                Paint backPaint = Theme.chat_instantViewRectPaint;
                if (currentMessageObject.isOutOwner()) {
                    Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_outPreviewInstantText));
                    backPaint.setColor(getThemedColor(Theme.key_chat_outPreviewInstantText));
                } else {
                    Theme.chat_instantViewPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
                    backPaint.setColor(getThemedColor(Theme.key_chat_inPreviewInstantText));
                }

                instantButtonRect.set(textX, instantY, textX + instantWidth, instantY + AndroidUtilities.dp(36));
                if (Build.VERSION.SDK_INT >= 21) {
                    selectorDrawableMaskType[0] = 0;
                    selectorDrawable[0].setBounds(textX, instantY, textX + instantWidth, instantY + AndroidUtilities.dp(36));
                    selectorDrawable[0].draw(canvas);
                }
                canvas.drawRoundRect(instantButtonRect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), backPaint);
                if (instantViewLayout != null) {
                    canvas.save();
                    canvas.translate(textX + instantTextX, instantY + AndroidUtilities.dp(10.5f));
                    instantViewLayout.draw(canvas);
                    canvas.restore();
                }
            }
        }

        if (drawImageButton && photoImage.getVisible()) {
            if (controlsAlpha != 1.0f) {
                radialProgress.setOverrideAlpha(controlsAlpha);
            }
            if (photoImage.hasImageSet()) {
                radialProgress.setBackgroundDrawable(null);
            } else {
                radialProgress.setBackgroundDrawable(isDrawSelectionBackground() ? currentBackgroundSelectedDrawable : currentBackgroundDrawable);
            }
            if (!currentMessageObject.needDrawBluredPreview() || !MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                radialProgress.draw(canvas);
            }
        }
        if (buttonState == -1 && currentMessageObject.needDrawBluredPreview() && !MediaController.getInstance().isPlayingMessage(currentMessageObject) && photoImage.getVisible() && currentMessageObject.messageOwner.destroyTime != 0) {
            if (!currentMessageObject.isOutOwner()) {
                long msTime = System.currentTimeMillis() + ConnectionsManager.getInstance(currentAccount).getTimeDifference() * 1000;
                float progress = Math.max(0, (long) currentMessageObject.messageOwner.destroyTime * 1000 - msTime) / (currentMessageObject.messageOwner.ttl * 1000.0f);
                Theme.chat_deleteProgressPaint.setAlpha((int) (255 * controlsAlpha));
                canvas.drawArc(deleteProgressRect, -90, -360 * progress, true, Theme.chat_deleteProgressPaint);
                if (progress != 0) {
                    int offset = AndroidUtilities.dp(2);
                    invalidate((int) deleteProgressRect.left - offset, (int) deleteProgressRect.top - offset, (int) deleteProgressRect.right + offset * 2, (int) deleteProgressRect.bottom + offset * 2);
                }
            }
            updateSecretTimeText(currentMessageObject);
        }
        if ((drawVideoImageButton || animatingDrawVideoImageButton != 0) && photoImage.getVisible()) {
            if (controlsAlpha != 1.0f) {
                videoRadialProgress.setOverrideAlpha(controlsAlpha);
            }
            videoRadialProgress.draw(canvas);
        }
        if (drawMediaCheckBox) {
            if (documentAttachType == DOCUMENT_ATTACH_TYPE_MUSIC || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT && !drawPhotoImage) {
                int size = AndroidUtilities.dp(20);
                mediaCheckBox.setBackgroundType(radialProgress.getMiniIcon() != MediaActionDrawable.ICON_NONE ? 12 : 13);
                mediaCheckBox.setBounds(buttonX + AndroidUtilities.dp(28), buttonY + AndroidUtilities.dp(28), size, size);
                mediaCheckBox.setColor(currentMessageObject.isOutOwner() ? Theme.key_chat_outTimeText : Theme.key_chat_inTimeText, currentMessageObject.isOutOwner() ? Theme.key_chat_outLoader : Theme.key_chat_inLoader, currentMessageObject.isOutOwner() ? Theme.key_chat_outBubble : Theme.key_chat_inBubble);
                mediaCheckBox.setBackgroundDrawable(isDrawSelectionBackground() ? currentBackgroundSelectedDrawable : currentBackgroundDrawable);
            } else {
                int size = AndroidUtilities.dp(21);
                mediaCheckBox.setBackgroundType(0);
                mediaCheckBox.setBounds((int) photoImage.getImageX2() - AndroidUtilities.dp(21 + 4), (int) photoImage.getImageY() + AndroidUtilities.dp(4), size, size);
                mediaCheckBox.setColor(null, null, currentMessageObject.isOutOwner() ? Theme.key_chat_outBubbleSelected : Theme.key_chat_inBubbleSelected);
                mediaCheckBox.setBackgroundDrawable(null);
            }
            mediaCheckBox.draw(canvas);
        }

        if (documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND) {
            float x1, y1;
            boolean playing = MediaController.getInstance().isPlayingMessage(currentMessageObject);
            if (currentMessageObject.type == MessageObject.TYPE_ROUND_VIDEO) {
                float offsetX = 0f;
                if (currentMessageObject.isOutOwner()) {
                    float offsetSize = (AndroidUtilities.roundPlayingMessageSize - AndroidUtilities.roundMessageSize) * 0.2f;
                    offsetX = isPlayingRound ? offsetSize : 0;
                    if (transitionParams.animatePlayingRound) {
                        offsetX = (isPlayingRound ? transitionParams.animateChangeProgress : (1f - transitionParams.animateChangeProgress)) * offsetSize;
                    }
                }

                x1 = backgroundDrawableLeft + transitionParams.deltaLeft + AndroidUtilities.dp(8) + roundPlayingDrawableProgress + offsetX;
                y1 = layoutHeight + transitionParams.deltaBottom - AndroidUtilities.dp(28 - (drawPinnedBottom ? 2 : 0));
                rect.set(x1, y1, x1 + timeWidthAudio + AndroidUtilities.dp(8 + 12 + 2), y1 + AndroidUtilities.dp(17));

                int oldAlpha = getThemedPaint(Theme.key_paint_chatActionBackground).getAlpha();
                getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha((int) (oldAlpha * timeAlpha));
                applyServiceShaderMatrix(getMeasuredWidth(), backgroundHeight, getX(), viewTop);
                canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), getThemedPaint(Theme.key_paint_chatActionBackground));
                if (hasGradientService()) {
                    int oldAlpha2 = Theme.chat_actionBackgroundGradientDarkenPaint.getAlpha();
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha((int) (oldAlpha2 * timeAlpha));
                    canvas.drawRoundRect(rect, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Theme.chat_actionBackgroundGradientDarkenPaint);
                    Theme.chat_actionBackgroundGradientDarkenPaint.setAlpha(oldAlpha2);
                }
                getThemedPaint(Theme.key_paint_chatActionBackground).setAlpha(oldAlpha);

                boolean showPlayingDrawable = playing || !currentMessageObject.isContentUnread();

                if (showPlayingDrawable && roundPlayingDrawableProgress != 1f) {
                    roundPlayingDrawableProgress += 16f / 150f;
                    if (roundPlayingDrawableProgress > 1f) {
                        roundPlayingDrawableProgress = 1f;
                    } else {
                        invalidate();
                    }
                } else if (!showPlayingDrawable && roundPlayingDrawableProgress != 0) {
                    roundPlayingDrawableProgress -= 16f / 150f;
                    if (roundPlayingDrawableProgress < 0f) {
                        roundPlayingDrawableProgress = 0f;
                    } else {
                        invalidate();
                    }
                }
                if (showPlayingDrawable) {
                    if (playing && !MediaController.getInstance().isMessagePaused()) {
                        roundVideoPlayingDrawable.start();
                    } else {
                        roundVideoPlayingDrawable.stop();
                    }
                }
                if (roundPlayingDrawableProgress < 1f) {
                    float cx = x1 + timeWidthAudio + AndroidUtilities.dp(12);
                    float cy = y1 + AndroidUtilities.dp(8.3f);
                    canvas.save();
                    canvas.scale((1f - roundPlayingDrawableProgress), (1f - roundPlayingDrawableProgress), cx, cy);
                    Theme.chat_docBackPaint.setColor(getThemedColor(Theme.key_chat_serviceText));
                    Theme.chat_docBackPaint.setAlpha((int) (255 * timeAlpha * (1f - roundPlayingDrawableProgress)));
                    canvas.drawCircle(cx, cy, AndroidUtilities.dp(3), Theme.chat_docBackPaint);
                    canvas.restore();
                }
                if (roundPlayingDrawableProgress > 0f) {
                    setDrawableBounds(roundVideoPlayingDrawable, x1 + timeWidthAudio + AndroidUtilities.dp(6), y1 + AndroidUtilities.dp(2.3f));
                    canvas.save();
                    canvas.scale(roundPlayingDrawableProgress, roundPlayingDrawableProgress, roundVideoPlayingDrawable.getBounds().centerX(), roundVideoPlayingDrawable.getBounds().centerY());
                    roundVideoPlayingDrawable.setAlpha((int) (255 * roundPlayingDrawableProgress));
                    roundVideoPlayingDrawable.draw(canvas);
                    canvas.restore();
                }
                x1 += AndroidUtilities.dp(4);
                y1 += AndroidUtilities.dp(1.7f);
            } else {
                x1 = backgroundDrawableLeft + AndroidUtilities.dp(currentMessageObject.isOutOwner() || drawPinnedBottom ? 12 : 18);
                y1 = layoutHeight - AndroidUtilities.dp(6.3f - (drawPinnedBottom ? 2 : 0)) - timeLayout.getHeight();
            }

            if (durationLayout != null) {
                Theme.chat_timePaint.setAlpha((int) (255 * timeAlpha));
                canvas.save();
                canvas.translate(x1, y1);
                durationLayout.draw(canvas);
                canvas.restore();
                Theme.chat_timePaint.setAlpha(255);
            }
        }
    }

    @Override
    public int getObserverTag() {
        return TAG;
    }

    public MessageObject getMessageObject() {
        return messageObjectToSet != null ? messageObjectToSet : currentMessageObject;
    }

    public TLRPC.Document getStreamingMedia() {
        return documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_ROUND || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF ? documentAttach : null;
    }

    public boolean drawPinnedBottom() {
        if (currentMessagesGroup != null && currentMessagesGroup.isDocuments) {
            if (currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_BOTTOM) != 0) {
                return pinnedBottom;
            }
            return true;
        }
        return pinnedBottom;
    }

    public boolean drawPinnedTop() {
        if (currentMessagesGroup != null && currentMessagesGroup.isDocuments) {
            if (currentPosition != null && (currentPosition.flags & MessageObject.POSITION_FLAG_TOP) != 0) {
                return pinnedTop;
            }
            return true;
        }
        return pinnedTop;
    }

    public boolean isPinnedBottom() {
        return pinnedBottom;
    }

    public boolean isPinnedTop() {
        return pinnedTop;
    }

    public MessageObject.GroupedMessages getCurrentMessagesGroup() {
        return currentMessagesGroup;
    }

    public MessageObject.GroupedMessagePosition getCurrentPosition() {
        return currentPosition;
    }

    public int getLayoutHeight() {
        return layoutHeight;
    }

    @Override
    public boolean performAccessibilityAction(int action, Bundle arguments) {
        if (action == AccessibilityNodeInfo.ACTION_CLICK) {
            int icon = getIconForCurrentState();
            if (icon != MediaActionDrawable.ICON_NONE && icon != MediaActionDrawable.ICON_FILE) {
                didPressButton(true, false);
            } else if (currentMessageObject.type == 16) {
                delegate.didPressOther(this, otherX, otherY);
            } else {
                didClickedImage();
            }
            return true;
        } else if (action == R.id.acc_action_small_button) {
            didPressMiniButton(true);
        } else if (action == R.id.acc_action_msg_options) {
            if (delegate != null) {
                if (currentMessageObject.type == 16) {
                    delegate.didLongPress(this, 0, 0);
                } else {
                    delegate.didPressOther(this, otherX, otherY);
                }
            }
        }
        if (currentMessageObject.isVoice() || currentMessageObject.isMusic() && MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
            if (seekBarAccessibilityDelegate.performAccessibilityActionInternal(action, arguments)) {
                return true;
            }
        }
        return super.performAccessibilityAction(action, arguments);
    }

    public void setAnimationRunning(boolean animationRunning, boolean willRemoved) {
        this.animationRunning = animationRunning;
        if (animationRunning) {
            this.willRemoved = willRemoved;
        } else {
            this.willRemoved = false;
        }
        if (getParent() == null && attachedToWindow) {
            onDetachedFromWindow();
        }
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();
        if (event.getAction() == MotionEvent.ACTION_HOVER_ENTER || event.getAction() == MotionEvent.ACTION_HOVER_MOVE) {
            for (int i = 0; i < accessibilityVirtualViewBounds.size(); i++) {
                Rect rect = accessibilityVirtualViewBounds.valueAt(i);
                if (rect.contains(x, y)) {
                    int id = accessibilityVirtualViewBounds.keyAt(i);
                    if (id != currentFocusedVirtualView) {
                        currentFocusedVirtualView = id;
                        sendAccessibilityEventForVirtualView(id, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                    }
                    return true;
                }
            }
        } else if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            currentFocusedVirtualView = 0;
        }
        return super.onHoverEvent(event);
    }

    @Override
    public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
    }

    @Override
    public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return new MessageAccessibilityNodeProvider();
    }

    private void sendAccessibilityEventForVirtualView(int viewId, int eventType) {
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am.isTouchExplorationEnabled()) {
            AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
            event.setPackageName(getContext().getPackageName());
            event.setSource(ChatMessageCell.this, viewId);
            if (getParent() != null) {
                getParent().requestSendAccessibilityEvent(ChatMessageCell.this, event);
            }
        }
    }

    public static Point getMessageSize(int imageW, int imageH) {
        return getMessageSize(imageW, imageH, 0, 0);
    }

    private static Point getMessageSize(int imageW, int imageH, int photoWidth, int photoHeight) {
        if (photoHeight == 0 || photoWidth == 0) {
            if (AndroidUtilities.isTablet()) {
                photoWidth = (int) (AndroidUtilities.getMinTabletSide() * 0.7f);
            } else {
                if (imageW >= imageH) {
                    photoWidth = Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) - AndroidUtilities.dp(64);
                } else {
                    photoWidth = (int) (Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) * 0.7f);
                }
            }

            photoHeight = photoWidth + AndroidUtilities.dp(100);

            if (photoWidth > AndroidUtilities.getPhotoSize()) {
                photoWidth = AndroidUtilities.getPhotoSize();
            }
            if (photoHeight > AndroidUtilities.getPhotoSize()) {
                photoHeight = AndroidUtilities.getPhotoSize();
            }
        }

        float scale = (float) imageW / (float) photoWidth;
        int w = (int) (imageW / scale);
        int h = (int) (imageH / scale);
        if (w == 0) {
            w = AndroidUtilities.dp(150);
        }
        if (h == 0) {
            h = AndroidUtilities.dp(150);
        }
        if (h > photoHeight) {
            float scale2 = h;
            h = photoHeight;
            scale2 /= h;
            w = (int) (w / scale2);
        } else if (h < AndroidUtilities.dp(120)) {
            h = AndroidUtilities.dp(120);
            float hScale = (float) imageH / h;
            if (imageW / hScale < photoWidth) {
                w = (int) (imageW / hScale);
            }
        }
        return new Point(w, h);
    }

    public StaticLayout getDescriptionlayout() {
        return descriptionLayout;
    }

    public void setSelectedBackgroundProgress(float value) {
        selectedBackgroundProgress = value;
        invalidate();
    }

    public int computeHeight(MessageObject object, MessageObject.GroupedMessages groupedMessages) {
        /*if (object.type == 2 || object.type == 12 || object.type == 9 ||
                object.type == 4 || object.type == 14 || object.type == 10 || object.type == 11 ||
                object.type == MessageObject.TYPE_ROUND_VIDEO) {
            return object.getApproximateHeight();
        }*/
        photoImage.setIgnoreImageSet(true);
        avatarImage.setIgnoreImageSet(true);
        replyImageReceiver.setIgnoreImageSet(true);
        locationImageReceiver.setIgnoreImageSet(true);

        if (groupedMessages != null && groupedMessages.messages.size() != 1) {
            int h = 0;
            for (int i = 0; i < groupedMessages.messages.size(); i++) {
                MessageObject o = groupedMessages.messages.get(i);
                MessageObject.GroupedMessagePosition position = groupedMessages.positions.get(o);
                if (position != null && (position.flags & MessageObject.POSITION_FLAG_LEFT) != 0) {
                    setMessageContent(o, groupedMessages, false, false);
                    h += totalHeight + keyboardHeight;
                }
            }
            return h;
        }

        setMessageContent(object, groupedMessages, false, false);
        photoImage.setIgnoreImageSet(false);
        avatarImage.setIgnoreImageSet(false);
        replyImageReceiver.setIgnoreImageSet(false);
        locationImageReceiver.setIgnoreImageSet(false);
        return totalHeight + keyboardHeight;
    }

    public void shakeView() {
        Keyframe kf0 = Keyframe.ofFloat(0f, 0);
        Keyframe kf1 = Keyframe.ofFloat(0.2f, 3);
        Keyframe kf2 = Keyframe.ofFloat(0.4f, -3);
        Keyframe kf3 = Keyframe.ofFloat(0.6f, 3);
        Keyframe kf4 = Keyframe.ofFloat(0.8f, -3);
        Keyframe kf5 = Keyframe.ofFloat(1f, 0);
        PropertyValuesHolder pvhRotation = PropertyValuesHolder.ofKeyframe(View.ROTATION, kf0, kf1, kf2, kf3, kf4, kf5);

        Keyframe kfs0 = Keyframe.ofFloat(0f, 1.0f);
        Keyframe kfs1 = Keyframe.ofFloat(0.5f, 0.97f);
        Keyframe kfs2 = Keyframe.ofFloat(1.0f, 1.0f);
        PropertyValuesHolder pvhScaleX = PropertyValuesHolder.ofKeyframe(View.SCALE_X, kfs0, kfs1, kfs2);
        PropertyValuesHolder pvhScaleY = PropertyValuesHolder.ofKeyframe(View.SCALE_Y, kfs0, kfs1, kfs2);

        shakeAnimation = new AnimatorSet();
        shakeAnimation.playTogether(
                ObjectAnimator.ofPropertyValuesHolder(this, pvhRotation),
                ObjectAnimator.ofPropertyValuesHolder(this, pvhScaleX),
                ObjectAnimator.ofPropertyValuesHolder(this, pvhScaleY));
        shakeAnimation.setDuration(500);
        shakeAnimation.start();
    }

    private void cancelShakeAnimation() {
        if (shakeAnimation != null) {
            shakeAnimation.cancel();
            shakeAnimation = null;

            setScaleX(1.0f);
            setScaleY(1.0f);
            setRotation(0);
        }
    }

    private float slidingOffsetX;
    private float animationOffsetX;

    public Property<ChatMessageCell, Float> ANIMATION_OFFSET_X = new Property<ChatMessageCell, Float>(Float.class, "animationOffsetX") {
        @Override
        public Float get(ChatMessageCell object) {
            return object.animationOffsetX;
        }

        @Override
        public void set(ChatMessageCell object, Float value) {
            object.setAnimationOffsetX(value);
        }
    };

    public void setSlidingOffset(float offsetX) {
        if (slidingOffsetX != offsetX) {
            slidingOffsetX = offsetX;
            updateTranslation();
        }
    }

    public void setAnimationOffsetX(float offsetX) {
        if (animationOffsetX != offsetX) {
            animationOffsetX = offsetX;
            updateTranslation();
        }
    }

    private void updateTranslation() {
        if (currentMessageObject == null) {
            return;
        }
        int checkBoxOffset = !currentMessageObject.isOutOwner() ? checkBoxTranslation : 0;
        setTranslationX(slidingOffsetX + animationOffsetX + checkBoxOffset);
    }

    public float getNonAnimationTranslationX(boolean update) {
        if (currentMessageObject != null && !currentMessageObject.isOutOwner()) {
            if (update && (checkBoxVisible || checkBoxAnimationInProgress)) {
                Interpolator interpolator = checkBoxVisible ? CubicBezierInterpolator.EASE_OUT : CubicBezierInterpolator.EASE_IN;
                checkBoxTranslation = (int) Math.ceil(interpolator.getInterpolation(checkBoxAnimationProgress) * AndroidUtilities.dp(35));
            }
            return slidingOffsetX + checkBoxTranslation;
        } else {
            return slidingOffsetX;
        }
    }

    public float getSlidingOffsetX() {
        return slidingOffsetX;
    }

    public boolean willRemovedAfterAnimation() {
        return willRemoved;
    }

    public float getAnimationOffsetX() {
        return animationOffsetX;
    }

    @Override
    public void setTranslationX(float translationX) {
        super.setTranslationX(translationX);
    }

    public SeekBar getSeekBar() {
        return seekBar;
    }

    private class MessageAccessibilityNodeProvider extends AccessibilityNodeProvider {

        private final int LINK_IDS_START = 2000;
        private final int LINK_CAPTION_IDS_START = 3000;
        private final int BOT_BUTTONS_START = 1000;
        private final int POLL_BUTTONS_START = 500;
        private final int INSTANT_VIEW = 499;
        private final int SHARE = 498;
        private final int REPLY = 497;
        private final int COMMENT = 496;
        private final int POLL_HINT = 495;
        private final int FORWARD = 494;
        private Path linkPath = new Path();
        private RectF rectF = new RectF();
        private Rect rect = new Rect();

        @Override
        public AccessibilityNodeInfo createAccessibilityNodeInfo(int virtualViewId) {
            int[] pos = {0, 0};
            getLocationOnScreen(pos);
            if (virtualViewId == HOST_VIEW_ID) {
                AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain(ChatMessageCell.this);
                onInitializeAccessibilityNodeInfo(info);
                StringBuilder sb = new StringBuilder();
                if (isChat && currentUser != null && !currentMessageObject.isOut()) {
                    sb.append(UserObject.getUserName(currentUser));
                    sb.append('\n');
                }
                if (drawForwardedName) {
                    for (int a = 0; a < 2; a++) {
                        if (forwardedNameLayout[a] != null) {
                            sb.append(forwardedNameLayout[a].getText());
                            sb.append(a == 0 ? " " : "\n");
                        }
                    }
                }
                if (!TextUtils.isEmpty(currentMessageObject.messageText)) {
                    sb.append(currentMessageObject.messageText);
                }
                if (documentAttach != null && (documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF || documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO)) {
                    if (buttonState == 1 && loadingProgressLayout != null) {
                        sb.append("\n");
                        final boolean sending = currentMessageObject.isSending();
                        final String key = sending ? "AccDescrUploadProgress" : "AccDescrDownloadProgress";
                        final int resId = sending ? R.string.AccDescrUploadProgress : R.string.AccDescrDownloadProgress;
                        sb.append(LocaleController.formatString(key, resId, AndroidUtilities.formatFileSize(currentMessageObject.loadedFileSize), AndroidUtilities.formatFileSize(lastLoadingSizeTotal)));
                    } else if (buttonState == 0 || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                        sb.append(", ");
                        sb.append(AndroidUtilities.formatFileSize(documentAttach.size));
                    }
                    if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO) {
                        sb.append(", ");
                        sb.append(LocaleController.formatDuration(currentMessageObject.getDuration()));
                    }
                }
                if (currentMessageObject.isMusic()) {
                    sb.append("\n");
                    sb.append(LocaleController.formatString("AccDescrMusicInfo", R.string.AccDescrMusicInfo, currentMessageObject.getMusicAuthor(), currentMessageObject.getMusicTitle()));
                    sb.append(", ");
                    sb.append(LocaleController.formatDuration(currentMessageObject.getDuration()));
                } else if (currentMessageObject.isVoice() || isRoundVideo) {
                    sb.append(", ");
                    sb.append(LocaleController.formatDuration(currentMessageObject.getDuration()));
                    if (currentMessageObject.isContentUnread()) {
                        sb.append(", ");
                        sb.append(LocaleController.getString("AccDescrMsgNotPlayed", R.string.AccDescrMsgNotPlayed));
                    }
                }
                if (lastPoll != null) {
                    sb.append(", ");
                    sb.append(lastPoll.question);
                    sb.append(", ");
                    String title;
                    if (pollClosed) {
                        title = LocaleController.getString("FinalResults", R.string.FinalResults);
                    } else {
                        if (lastPoll.quiz) {
                            if (lastPoll.public_voters) {
                                title = LocaleController.getString("QuizPoll", R.string.QuizPoll);
                            } else {
                                title = LocaleController.getString("AnonymousQuizPoll", R.string.AnonymousQuizPoll);
                            }
                        } else if (lastPoll.public_voters) {
                            title = LocaleController.getString("PublicPoll", R.string.PublicPoll);
                        } else {
                            title = LocaleController.getString("AnonymousPoll", R.string.AnonymousPoll);
                        }
                    }
                    sb.append(title);
                }
                if (currentMessageObject.messageOwner.media != null && !TextUtils.isEmpty(currentMessageObject.caption)) {
                    sb.append("\n");
                    sb.append(currentMessageObject.caption);
                }
                if (currentMessageObject.isOut()) {
                    if (currentMessageObject.isSent()) {
                        sb.append("\n");
                        if (currentMessageObject.scheduled) {
                            sb.append(LocaleController.formatString("AccDescrScheduledDate", R.string.AccDescrScheduledDate, currentTimeString));
                        } else {
                            sb.append(LocaleController.formatString("AccDescrSentDate", R.string.AccDescrSentDate, LocaleController.getString("TodayAt", R.string.TodayAt) + " " + currentTimeString));
                            sb.append(", ");
                            sb.append(currentMessageObject.isUnread() ? LocaleController.getString("AccDescrMsgUnread", R.string.AccDescrMsgUnread) : LocaleController.getString("AccDescrMsgRead", R.string.AccDescrMsgRead));
                        }
                    } else if (currentMessageObject.isSending()) {
                        sb.append("\n");
                        sb.append(LocaleController.getString("AccDescrMsgSending", R.string.AccDescrMsgSending));
                        final float sendingProgress = radialProgress.getProgress();
                        if (sendingProgress > 0f) {
                            sb.append(", ").append(Math.round(sendingProgress * 100)).append("%");
                        }
                    } else if (currentMessageObject.isSendError()) {
                        sb.append("\n");
                        sb.append(LocaleController.getString("AccDescrMsgSendingError", R.string.AccDescrMsgSendingError));
                    }
                } else {
                    sb.append("\n");
                    sb.append(LocaleController.formatString("AccDescrReceivedDate", R.string.AccDescrReceivedDate, LocaleController.getString("TodayAt", R.string.TodayAt) + " " + currentTimeString));
                }
                if ((currentMessageObject.messageOwner.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                    sb.append("\n");
                    sb.append(LocaleController.formatPluralString("AccDescrNumberOfViews", currentMessageObject.messageOwner.views));
                }
                sb.append("\n");
                info.setContentDescription(sb.toString());
                info.setEnabled(true);
                if (Build.VERSION.SDK_INT >= 19) {
                    AccessibilityNodeInfo.CollectionItemInfo itemInfo = info.getCollectionItemInfo();
                    if (itemInfo != null) {
                        info.setCollectionItemInfo(AccessibilityNodeInfo.CollectionItemInfo.obtain(itemInfo.getRowIndex(), 1, 0, 1, false));
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.acc_action_msg_options, LocaleController.getString("AccActionMessageOptions", R.string.AccActionMessageOptions)));
                    int icon = getIconForCurrentState();
                    CharSequence actionLabel = null;
                    switch (icon) {
                        case MediaActionDrawable.ICON_PLAY:
                            actionLabel = LocaleController.getString("AccActionPlay", R.string.AccActionPlay);
                            break;
                        case MediaActionDrawable.ICON_PAUSE:
                            actionLabel = LocaleController.getString("AccActionPause", R.string.AccActionPause);
                            break;
                        case MediaActionDrawable.ICON_FILE:
                            actionLabel = LocaleController.getString("AccActionOpenFile", R.string.AccActionOpenFile);
                            break;
                        case MediaActionDrawable.ICON_DOWNLOAD:
                            actionLabel = LocaleController.getString("AccActionDownload", R.string.AccActionDownload);
                            break;
                        case MediaActionDrawable.ICON_CANCEL:
                            actionLabel = LocaleController.getString("AccActionCancelDownload", R.string.AccActionCancelDownload);
                            break;
                        default:
                            if (currentMessageObject.type == 16) {
                                actionLabel = LocaleController.getString("CallAgain", R.string.CallAgain);
                            }
                    }
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_CLICK, actionLabel));
                    info.addAction(new AccessibilityNodeInfo.AccessibilityAction(AccessibilityNodeInfo.ACTION_LONG_CLICK, LocaleController.getString("AccActionEnterSelectionMode", R.string.AccActionEnterSelectionMode)));
                    int smallIcon = getMiniIconForCurrentState();
                    if (smallIcon == MediaActionDrawable.ICON_DOWNLOAD) {
                        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(R.id.acc_action_small_button, LocaleController.getString("AccActionDownload", R.string.AccActionDownload)));
                    }
                } else {
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                }

                if ((currentMessageObject.isVoice() || currentMessageObject.isMusic()) && MediaController.getInstance().isPlayingMessage(currentMessageObject)) {
                    seekBarAccessibilityDelegate.onInitializeAccessibilityNodeInfoInternal(info);
                }

                int i;
                if (currentMessageObject.messageText instanceof Spannable) {
                    Spannable buffer = (Spannable) currentMessageObject.messageText;
                    CharacterStyle[] links = buffer.getSpans(0, buffer.length(), ClickableSpan.class);
                    i = 0;
                    for (CharacterStyle link : links) {
                        info.addChild(ChatMessageCell.this, LINK_IDS_START + i);
                        i++;
                    }
                }
                if (currentMessageObject.caption instanceof Spannable && captionLayout != null) {
                    Spannable buffer = (Spannable) currentMessageObject.caption;
                    CharacterStyle[] links = buffer.getSpans(0, buffer.length(), ClickableSpan.class);
                    i = 0;
                    for (CharacterStyle link : links) {
                        info.addChild(ChatMessageCell.this, LINK_CAPTION_IDS_START + i);
                        i++;
                    }
                }
                i = 0;
                for (BotButton button : botButtons) {
                    info.addChild(ChatMessageCell.this, BOT_BUTTONS_START + i);
                    i++;
                }
                if (hintButtonVisible && pollHintX != -1 && currentMessageObject.isPoll()) {
                    info.addChild(ChatMessageCell.this, POLL_HINT);
                }
                i = 0;
                for (PollButton button : pollButtons) {
                    info.addChild(ChatMessageCell.this, POLL_BUTTONS_START + i);
                    i++;
                }
                if (drawInstantView && !instantButtonRect.isEmpty()) {
                    info.addChild(ChatMessageCell.this, INSTANT_VIEW);
                }
                if (commentLayout != null) {
                    info.addChild(ChatMessageCell.this, COMMENT);
                }
                if (drawSideButton == 1) {
                    info.addChild(ChatMessageCell.this, SHARE);
                }
                if (replyNameLayout != null) {
                    info.addChild(ChatMessageCell.this, REPLY);
                }
                if (forwardedNameLayout[0] != null && forwardedNameLayout[1] != null) {
                    info.addChild(ChatMessageCell.this, FORWARD);
                }
                if (drawSelectionBackground || getBackground() != null) {
                    info.setSelected(true);
                }
                return info;
            } else {
                AccessibilityNodeInfo info = AccessibilityNodeInfo.obtain();
                info.setSource(ChatMessageCell.this, virtualViewId);
                info.setParent(ChatMessageCell.this);
                info.setPackageName(getContext().getPackageName());
                if (virtualViewId >= LINK_CAPTION_IDS_START) {
                    if (!(currentMessageObject.caption instanceof Spannable) || captionLayout == null) {
                        return null;
                    }
                    Spannable buffer = (Spannable) currentMessageObject.caption;
                    ClickableSpan link = getLinkById(virtualViewId, true);
                    if (link == null) {
                        return null;
                    }
                    int[] linkPos = getRealSpanStartAndEnd(buffer, link);
                    String content = buffer.subSequence(linkPos[0], linkPos[1]).toString();
                    info.setText(content);
                    int length = captionLayout.getText().length();

                    captionLayout.getSelectionPath(linkPos[0], linkPos[1], linkPath);
                    linkPath.computeBounds(rectF, true);
                    rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
                    rect.offset((int) captionX, (int) captionY);
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);

                    info.setClassName("android.widget.TextView");
                    info.setEnabled(true);
                    info.setClickable(true);
                    info.setLongClickable(true);
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                } else if (virtualViewId >= LINK_IDS_START) {
                    if (!(currentMessageObject.messageText instanceof Spannable)) {
                        return null;
                    }
                    Spannable buffer = (Spannable) currentMessageObject.messageText;
                    ClickableSpan link = getLinkById(virtualViewId, false);
                    if (link == null) {
                        return null;
                    }
                    int[] linkPos = getRealSpanStartAndEnd(buffer, link);
                    String content = buffer.subSequence(linkPos[0], linkPos[1]).toString();
                    info.setText(content);
                    for (MessageObject.TextLayoutBlock block : currentMessageObject.textLayoutBlocks) {
                        int length = block.textLayout.getText().length();
                        if (block.charactersOffset <= linkPos[0] && block.charactersOffset + length >= linkPos[1]) {
                            block.textLayout.getSelectionPath(linkPos[0] - block.charactersOffset, linkPos[1] - block.charactersOffset, linkPath);
                            linkPath.computeBounds(rectF, true);
                            rect.set((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom);
                            rect.offset(0, (int) block.textYOffset);
                            rect.offset(textX, textY);
                            info.setBoundsInParent(rect);
                            if (accessibilityVirtualViewBounds.get(virtualViewId) == null) {
                                accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                            }
                            rect.offset(pos[0], pos[1]);
                            info.setBoundsInScreen(rect);
                            break;
                        }
                    }

                    info.setClassName("android.widget.TextView");
                    info.setEnabled(true);
                    info.setClickable(true);
                    info.setLongClickable(true);
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    info.addAction(AccessibilityNodeInfo.ACTION_LONG_CLICK);
                } else if (virtualViewId >= BOT_BUTTONS_START) {
                    int buttonIndex = virtualViewId - BOT_BUTTONS_START;
                    if (buttonIndex >= botButtons.size()) {
                        return null;
                    }
                    BotButton button = botButtons.get(buttonIndex);
                    info.setText(button.title.getText());
                    info.setClassName("android.widget.Button");
                    info.setEnabled(true);
                    info.setClickable(true);
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);

                    rect.set(button.x, button.y, button.x + button.width, button.y + button.height);
                    int addX;
                    if (currentMessageObject.isOutOwner()) {
                        addX = getMeasuredWidth() - widthForButtons - AndroidUtilities.dp(10);
                    } else {
                        addX = backgroundDrawableLeft + AndroidUtilities.dp(mediaBackground ? 1 : 7);
                    }
                    rect.offset(addX, layoutHeight);
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                } else if (virtualViewId >= POLL_BUTTONS_START) {
                    int buttonIndex = virtualViewId - POLL_BUTTONS_START;
                    if (buttonIndex >= pollButtons.size()) {
                        return null;
                    }
                    PollButton button = pollButtons.get(buttonIndex);
                    StringBuilder sb = new StringBuilder(button.title.getText());
                    if (!pollVoted) {
                        info.setClassName("android.widget.Button");
                    } else {
                        info.setSelected(button.chosen);
                        sb.append(", ").append(button.percent).append("%");
                        if (lastPoll != null && lastPoll.quiz && button.correct) {
                            sb.append(", ").append(LocaleController.getString("AccDescrQuizCorrectAnswer", R.string.AccDescrQuizCorrectAnswer));
                        }
                    }
                    info.setText(sb);
                    info.setEnabled(true);
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);

                    final int y = button.y + namesOffset;
                    final int w = backgroundWidth - AndroidUtilities.dp(76);
                    rect.set(button.x, y, button.x + w, y + button.height);
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);

                    info.setClickable(true);
                } else if (virtualViewId == POLL_HINT) {
                    info.setClassName("android.widget.Button");
                    info.setEnabled(true);
                    info.setText(LocaleController.getString("AccDescrQuizExplanation", R.string.AccDescrQuizExplanation));
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    rect.set(pollHintX - AndroidUtilities.dp(8), pollHintY - AndroidUtilities.dp(8), pollHintX + AndroidUtilities.dp(32), pollHintY + AndroidUtilities.dp(32));
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null || !accessibilityVirtualViewBounds.get(virtualViewId).equals(rect)) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                    info.setClickable(true);
                } else if (virtualViewId == INSTANT_VIEW) {
                    info.setClassName("android.widget.Button");
                    info.setEnabled(true);
                    if (instantViewLayout != null) {
                        info.setText(instantViewLayout.getText());
                    }
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    instantButtonRect.round(rect);
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null || !accessibilityVirtualViewBounds.get(virtualViewId).equals(rect)) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                    info.setClickable(true);
                } else if (virtualViewId == SHARE) {
                    info.setClassName("android.widget.ImageButton");
                    info.setEnabled(true);
                    if (isOpenChatByShare(currentMessageObject)) {
                        info.setContentDescription(LocaleController.getString("AccDescrOpenChat", R.string.AccDescrOpenChat));
                    } else {
                        info.setContentDescription(LocaleController.getString("ShareFile", R.string.ShareFile));
                    }
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    rect.set((int) sideStartX, (int) sideStartY, (int) sideStartX + AndroidUtilities.dp(40), (int) sideStartY + AndroidUtilities.dp(32));
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null || !accessibilityVirtualViewBounds.get(virtualViewId).equals(rect)) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                    info.setClickable(true);
                } else if (virtualViewId == REPLY) {
                    info.setEnabled(true);
                    StringBuilder sb = new StringBuilder();
                    sb.append(LocaleController.getString("Reply", R.string.Reply));
                    sb.append(", ");
                    if (replyNameLayout != null) {
                        sb.append(replyNameLayout.getText());
                        sb.append(", ");
                    }
                    if (replyTextLayout != null) {
                        sb.append(replyTextLayout.getText());
                    }
                    info.setContentDescription(sb.toString());
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);

                    rect.set(replyStartX, replyStartY, replyStartX + Math.max(replyNameWidth, replyTextWidth), replyStartY + AndroidUtilities.dp(35));
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null || !accessibilityVirtualViewBounds.get(virtualViewId).equals(rect)) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                    info.setClickable(true);
                } else if (virtualViewId == FORWARD) {
                    info.setEnabled(true);
                    StringBuilder sb = new StringBuilder();
                    if (forwardedNameLayout[0] != null && forwardedNameLayout[1] != null) {
                        for (int a = 0; a < 2; a++) {
                            sb.append(forwardedNameLayout[a].getText());
                            sb.append(a == 0 ? " " : "\n");
                        }
                    }
                    info.setContentDescription(sb.toString());
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);

                    int x = (int) Math.min(forwardNameX - forwardNameOffsetX[0], forwardNameX - forwardNameOffsetX[1]);
                    rect.set(x, forwardNameY, x + forwardedNameWidth, forwardNameY + AndroidUtilities.dp(32));
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null || !accessibilityVirtualViewBounds.get(virtualViewId).equals(rect)) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                    info.setClickable(true);
                } else if (virtualViewId == COMMENT) {
                    info.setClassName("android.widget.Button");
                    info.setEnabled(true);
                    if (commentLayout != null) {
                        info.setText(commentLayout.getText());
                    }
                    info.addAction(AccessibilityNodeInfo.ACTION_CLICK);
                    rect.set(commentButtonRect);
                    info.setBoundsInParent(rect);
                    if (accessibilityVirtualViewBounds.get(virtualViewId) == null || !accessibilityVirtualViewBounds.get(virtualViewId).equals(rect)) {
                        accessibilityVirtualViewBounds.put(virtualViewId, new Rect(rect));
                    }
                    rect.offset(pos[0], pos[1]);
                    info.setBoundsInScreen(rect);
                    info.setClickable(true);
                }
                info.setFocusable(true);
                info.setVisibleToUser(true);
                return info;
            }
        }

        @Override
        public boolean performAction(int virtualViewId, int action, Bundle arguments) {
            if (virtualViewId == HOST_VIEW_ID) {
                performAccessibilityAction(action, arguments);
            } else {
                if (action == AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS) {
                    sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED);
                } else if (action == AccessibilityNodeInfo.ACTION_CLICK) {
                     if (virtualViewId >= LINK_CAPTION_IDS_START) {
                        ClickableSpan link = getLinkById(virtualViewId, true);
                        if (link != null) {
                            delegate.didPressUrl(ChatMessageCell.this, link, false);
                            sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                        }
                    } else if (virtualViewId >= LINK_IDS_START) {
                        ClickableSpan link = getLinkById(virtualViewId, false);
                        if (link != null) {
                            delegate.didPressUrl(ChatMessageCell.this, link, false);
                            sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                        }
                    } else if (virtualViewId >= BOT_BUTTONS_START) {
                        int buttonIndex = virtualViewId - BOT_BUTTONS_START;
                        if (buttonIndex >= botButtons.size()) {
                            return false;
                        }
                        BotButton button = botButtons.get(buttonIndex);
                        if (delegate != null) {
                            if (button.button != null) {
                                delegate.didPressBotButton(ChatMessageCell.this, button.button);
                            } else if (button.reaction != null) {
                                delegate.didPressReaction(ChatMessageCell.this, button.reaction);
                            }
                        }
                        sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                    } else if (virtualViewId >= POLL_BUTTONS_START) {
                        int buttonIndex = virtualViewId - POLL_BUTTONS_START;
                        if (buttonIndex >= pollButtons.size()) {
                            return false;
                        }
                        PollButton button = pollButtons.get(buttonIndex);
                        if (delegate != null) {
                            ArrayList<TLRPC.TL_pollAnswer> answers = new ArrayList<>();
                            answers.add(button.answer);
                            delegate.didPressVoteButtons(ChatMessageCell.this, answers, -1, 0, 0);
                        }
                        sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED);
                    } else if (virtualViewId == POLL_HINT) {
                        if (delegate != null) {
                            delegate.didPressHint(ChatMessageCell.this, 0);
                        }
                    } else if (virtualViewId == INSTANT_VIEW) {
                        if (delegate != null) {
                            delegate.didPressInstantButton(ChatMessageCell.this, drawInstantViewType);
                        }
                    } else if (virtualViewId == SHARE) {
                        if (delegate != null) {
                            delegate.didPressSideButton(ChatMessageCell.this);
                        }
                    } else if (virtualViewId == REPLY) {
                        if (delegate != null && (!isThreadChat || currentMessageObject.getReplyTopMsgId() != 0) && currentMessageObject.hasValidReplyMessageObject()) {
                            delegate.didPressReplyMessage(ChatMessageCell.this, currentMessageObject.getReplyMsgId());
                        }
                    } else if (virtualViewId == FORWARD) {
                         if (delegate != null) {
                             if (currentForwardChannel != null) {
                                 delegate.didPressChannelAvatar(ChatMessageCell.this, currentForwardChannel, currentMessageObject.messageOwner.fwd_from.channel_post, lastTouchX, lastTouchY);
                             } else if (currentForwardUser != null) {
                                 delegate.didPressUserAvatar(ChatMessageCell.this, currentForwardUser, lastTouchX, lastTouchY);
                             } else if (currentForwardName != null) {
                                 delegate.didPressHiddenForward(ChatMessageCell.this);
                             }
                         }
                     } else if (virtualViewId == COMMENT) {
                        if (delegate != null) {
                            if (isRepliesChat) {
                                delegate.didPressSideButton(ChatMessageCell.this);
                            } else {
                                delegate.didPressCommentButton(ChatMessageCell.this);
                            }
                        }
                    }
                } else if (action == AccessibilityNodeInfo.ACTION_LONG_CLICK) {
                    ClickableSpan link = getLinkById(virtualViewId, virtualViewId >= LINK_CAPTION_IDS_START);
                    if (link != null) {
                        delegate.didPressUrl(ChatMessageCell.this, link, true);
                        sendAccessibilityEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
                    }
                }
            }
            return true;
        }

        private ClickableSpan getLinkById(int id, boolean caption) {
            if (caption) {
                id -= LINK_CAPTION_IDS_START;
                if (!(currentMessageObject.caption instanceof Spannable) || id < 0) {
                    return null;
                }
                Spannable buffer = (Spannable) currentMessageObject.caption;
                ClickableSpan[] links = buffer.getSpans(0, buffer.length(), ClickableSpan.class);
                if (links.length <= id) {
                    return null;
                }
                return links[id];
            } else {
                id -= LINK_IDS_START;
                if (!(currentMessageObject.messageText instanceof Spannable) || id < 0) {
                    return null;
                }
                Spannable buffer = (Spannable) currentMessageObject.messageText;
                ClickableSpan[] links = buffer.getSpans(0, buffer.length(), ClickableSpan.class);
                if (links.length <= id) {
                    return null;
                }
                return links[id];
            }
        }
    }

    public void setImageCoords(float x, float y, float w, float h) {
        photoImage.setImageCoords(x, y, w, h);
        if (documentAttachType == DOCUMENT_ATTACH_TYPE_VIDEO || documentAttachType == DOCUMENT_ATTACH_TYPE_GIF) {
            videoButtonX = (int) (photoImage.getImageX() + AndroidUtilities.dp(8));
            videoButtonY = (int) (photoImage.getImageY() + AndroidUtilities.dp(8));
            videoRadialProgress.setProgressRect(videoButtonX, videoButtonY, videoButtonX + AndroidUtilities.dp(24), videoButtonY + AndroidUtilities.dp(24));

            buttonX = (int) (x + (photoImage.getImageWidth() - AndroidUtilities.dp(48)) / 2.0f);
            buttonY = (int) (photoImage.getImageY() + (photoImage.getImageHeight() - AndroidUtilities.dp(48)) / 2);
            radialProgress.setProgressRect(buttonX, buttonY, buttonX + AndroidUtilities.dp(48), buttonY + AndroidUtilities.dp(48));
        }
    }

    @Override
    public float getAlpha() {
        if (ALPHA_PROPERTY_WORKAROUND) {
            return alphaInternal;
        }
        return super.getAlpha();
    }

    @Override
    public void setAlpha(float alpha) {
        if (ALPHA_PROPERTY_WORKAROUND) {
            alphaInternal = alpha;
            invalidate();
        } else {
            super.setAlpha(alpha);
        }
    }

    public int getCurrentBackgroundLeft() {
        int left = currentBackgroundDrawable.getBounds().left;
        if (!currentMessageObject.isOutOwner() && transitionParams.changePinnedBottomProgress != 1 && (!mediaBackground && !drawPinnedBottom)) {
            left -= AndroidUtilities.dp(6);
        }
        return left;
    }

    public TransitionParams getTransitionParams() {
        return transitionParams;
    }

    public int getTopMediaOffset() {
        if (currentMessageObject != null && currentMessageObject.type == 14) {
            return mediaOffsetY + namesOffset;
        }
        return 0;
    }

    public int getTextX() {
        return textX;
    }

    public int getTextY() {
        return textY;
    }

    public boolean isPlayingRound() {
        return isRoundVideo && isPlayingRound;
    }

    public int getParentWidth() {
        MessageObject object = currentMessageObject == null ? messageObjectToSet : currentMessageObject;
        if (object != null && object.preview && parentWidth > 0) {
            return parentWidth;
        }
        return AndroidUtilities.displaySize.x;
    }

    public class TransitionParams {

        public float lastDrawingImageX, lastDrawingImageY, lastDrawingImageW, lastDrawingImageH;
        public float lastDrawingCaptionX, lastDrawingCaptionY;
        public boolean animateChange;
        public int animateFromRepliesTextWidth;
        public boolean messageEntering;
        public boolean animateLocationIsExpired;
        public boolean lastLocatinIsExpired;
        public String lastDrawLocationExpireText;
        public float lastDrawLocationExpireProgress;
        public StaticLayout lastDrawDocTitleLayout;
        public StaticLayout lastDrawInfoLayout;
        public boolean updatePhotoImageX;

        private boolean lastIsPinned;
        private boolean animatePinned;
        public float lastTimeXPinned;
        public float animateFromTimeXPinned;

        private int lastRepliesCount;
        private boolean animateReplies;
        private StaticLayout lastRepliesLayout;
        private StaticLayout animateRepliesLayout;
        private float animateFromTimeXReplies;
        private float lastTimeXReplies;

        private float animateFromTimeXViews;
        private float lastTimeXViews;

        private int lastCommentsCount;
        private int lastTotalCommentWidth;
        private int lastCommentArrowX;
        private int lastCommentUnreadX;
        private boolean lastCommentDrawUnread;
        private float lastCommentX;
        private boolean lastDrawCommentNumber;
        private StaticLayout lastCommentLayout;
        private boolean animateComments;
        private StaticLayout animateCommentsLayout;
        private float animateCommentX;
        private int animateTotalCommentWidth;
        private int animateCommentArrowX;
        private int animateCommentUnreadX;
        private boolean animateCommentDrawUnread;
        private boolean animateDrawCommentNumber;

        private boolean animateSign;
        private float animateNameX;
        private String lastSignMessage;

        public boolean imageChangeBoundsTransition;
        public float deltaLeft;
        public float deltaRight;
        public float deltaBottom;
        public float deltaTop;

        public float animateToImageX, animateToImageY, animateToImageW, animateToImageH;
        public float captionFromX, captionFromY;
        private boolean moveCaption;

        public int[] imageRoundRadius = new int[4];
        public float captionEnterProgress = 1f;

        public boolean wasDraw;
        public boolean animateBackgroundBoundsInner;
        public boolean ignoreAlpha;
        public boolean drawPinnedBottomBackground;
        public float changePinnedBottomProgress = 1f;
        public int[] animateToRadius;
        public boolean animateRadius;
        public boolean transformGroupToSingleMessage;
        public Rect lastDrawingBackgroundRect = new Rect();

        boolean animateMessageText;
        private ArrayList<MessageObject.TextLayoutBlock> animateOutTextBlocks;
        private ArrayList<MessageObject.TextLayoutBlock> lastDrawingTextBlocks;

        private boolean animateEditedEnter;
        private StaticLayout animateEditedLayout;
        private StaticLayout animateTimeLayout;
        private int animateTimeWidth;
        private int lastTimeWidth;
        private boolean lastDrawingEdited;

        boolean animateReplaceCaptionLayout;
        private StaticLayout animateOutCaptionLayout;
        private StaticLayout lastDrawingCaptionLayout;
        public boolean lastDrawTime;
        public int lastTimeX;
        public int animateFromTimeX;
        public boolean shouldAnimateTimeX;

        public boolean animateDrawingTimeAlpha;

        public float animateChangeProgress = 1f;
        private ArrayList<BotButton> lastDrawBotButtons = new ArrayList<>();
        private ArrayList<BotButton> transitionBotButtons = new ArrayList<>();

        private float lastButtonX;
        private float lastButtonY;
        private float animateFromButtonX;
        private float animateFromButtonY;
        private boolean animateButton;

        public int lastStatusDrawableParams = -1;

        private int lastViewsCount;
        private StaticLayout lastViewsLayout;
        private StaticLayout animateViewsLayout;

        private boolean lastShouldDrawTimeOnMedia;
        private boolean animateShouldDrawTimeOnMedia;
        private boolean lastShouldDrawMenuDrawable;
        private boolean animateShouldDrawMenuDrawable;
        private StaticLayout lastTimeLayout;
        private boolean lastIsPlayingRound;
        public boolean animatePlayingRound;
        public boolean animateText;

        public float lastDrawingTextY;
        public float lastDrawingTextX;

        public float animateFromTextY;

        public int lastTopOffset;
        public boolean animateForwardedLayout;
        public int animateForwardedNamesOffset;
        public int lastForwardedNamesOffset;
        public boolean lastDrawnForwardedName;
        public StaticLayout[] lastDrawnForwardedNameLayout = new StaticLayout[2];
        public StaticLayout[] animatingForwardedNameLayout = new StaticLayout[2];
        float animateForwardNameX;
        float lastForwardNameX;
        int animateForwardNameWidth;
        int lastForwardNameWidth;

        public void recordDrawingState() {
            wasDraw = true;
            lastDrawingImageX = photoImage.getImageX();
            lastDrawingImageY = photoImage.getImageY();
            lastDrawingImageW = photoImage.getImageWidth();
            lastDrawingImageH = photoImage.getImageHeight();
            int[] r = photoImage.getRoundRadius();
            System.arraycopy(r, 0, imageRoundRadius, 0, 4);
            if (currentBackgroundDrawable != null) {
                lastDrawingBackgroundRect.set(currentBackgroundDrawable.getBounds());
            }
            lastDrawingTextBlocks = currentMessageObject.textLayoutBlocks;
            lastDrawingEdited = edited;

            lastDrawingCaptionX = captionX;
            lastDrawingCaptionY = captionY;

            lastDrawingCaptionLayout = captionLayout;
            if (!botButtons.isEmpty()) {
                lastDrawBotButtons.clear();
                lastDrawBotButtons.addAll(botButtons);
            }

            if (commentLayout != null) {
                lastCommentsCount = getRepliesCount();
                lastTotalCommentWidth = totalCommentWidth;
                lastCommentLayout = commentLayout;
                lastCommentArrowX = commentArrowX;
                lastCommentUnreadX = commentUnreadX;
                lastCommentDrawUnread = commentDrawUnread;
                lastCommentX = commentX;
                lastDrawCommentNumber = drawCommentNumber;
            }

            lastRepliesCount = getRepliesCount();
            this.lastViewsCount = getMessageObject().messageOwner.views;
            lastRepliesLayout = repliesLayout;
            lastViewsLayout = viewsLayout;

            lastIsPinned = isPinned;

            lastSignMessage = lastPostAuthor;

            lastButtonX = buttonX;
            lastButtonY = buttonY;

            lastDrawTime = !forceNotDrawTime;
            lastTimeX = timeX;
            lastTimeLayout = timeLayout;
            lastTimeWidth = timeWidth;

            lastShouldDrawTimeOnMedia = shouldDrawTimeOnMedia();
            lastTopOffset = getTopMediaOffset();
            lastShouldDrawMenuDrawable = shouldDrawMenuDrawable();

            lastLocatinIsExpired = locationExpired;
            lastIsPlayingRound = isPlayingRound;

            lastDrawingTextY = textY;
            lastDrawingTextX = textX;

            lastDrawnForwardedNameLayout[0] = forwardedNameLayout[0];
            lastDrawnForwardedNameLayout[1] = forwardedNameLayout[1];
            lastDrawnForwardedName = currentMessageObject.needDrawForwarded();
            lastForwardNameX = forwardNameX;
            lastForwardedNamesOffset = namesOffset;
            lastForwardNameWidth = forwardedNameWidth;
        }

        public void recordDrawingStatePreview() {
            lastDrawnForwardedNameLayout[0] = forwardedNameLayout[0];
            lastDrawnForwardedNameLayout[1] = forwardedNameLayout[1];
            lastDrawnForwardedName = currentMessageObject.needDrawForwarded();
            lastForwardNameX = forwardNameX;
            lastForwardedNamesOffset = namesOffset;
            lastForwardNameWidth = forwardedNameWidth;
        }
        public boolean animateChange() {
            if (!wasDraw) {
                return false;
            }
            boolean changed = false;

            animateMessageText = false;
            if (currentMessageObject.textLayoutBlocks != lastDrawingTextBlocks) {
                boolean sameText = true;
                if (currentMessageObject.textLayoutBlocks != null && lastDrawingTextBlocks != null && currentMessageObject.textLayoutBlocks.size() == lastDrawingTextBlocks.size()) {
                    for (int i = 0; i < lastDrawingTextBlocks.size(); i++) {
                        String newText = currentMessageObject.textLayoutBlocks.get(i).textLayout == null ? null : currentMessageObject.textLayoutBlocks.get(i).textLayout.getText().toString();
                        String oldText = lastDrawingTextBlocks.get(i).textLayout == null ? null : lastDrawingTextBlocks.get(i).textLayout.getText().toString();
                        if ((newText == null && oldText != null) || (newText != null && oldText == null) || !newText.equals(oldText)) {
                            sameText = false;
                            break;
                        }
                    }
                } else {
                    sameText = false;
                }
                if (!sameText) {
                    animateMessageText = true;
                    animateOutTextBlocks = lastDrawingTextBlocks;
                    changed = true;
                }
            }
            if (edited && !lastDrawingEdited && timeLayout != null) {
                String editedStr = LocaleController.getString("EditedMessage", R.string.EditedMessage);
                String text = timeLayout.getText().toString();
                int i = text.indexOf(editedStr);
                if (i >= 0) {
                    if (i == 0) {
                        animateEditedLayout = new StaticLayout(editedStr, Theme.chat_timePaint, timeTextWidth + AndroidUtilities.dp(100), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                        spannableStringBuilder.append(editedStr);
                        spannableStringBuilder.append(text.substring(editedStr.length()));
                        spannableStringBuilder.setSpan(new EmptyStubSpan(), 0, editedStr.length(), 0);
                        animateTimeLayout = new StaticLayout(spannableStringBuilder, Theme.chat_timePaint, timeTextWidth + AndroidUtilities.dp(100), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                    } else {
                        animateEditedLayout = null;
                        animateTimeLayout = lastTimeLayout;
                    }
                    animateEditedEnter = true;
                    animateTimeWidth = lastTimeWidth;
                    animateFromTimeX = lastTimeX;
                    changed = true;
                }
            }

            if (captionLayout != lastDrawingCaptionLayout) {
                String oldCaption = lastDrawingCaptionLayout == null ? null : lastDrawingCaptionLayout.getText().toString();
                String currentCaption = captionLayout == null ? null : captionLayout.getText().toString();
                if (currentCaption != null && (oldCaption == null || !oldCaption.equals(currentCaption))) {
                    animateReplaceCaptionLayout = true;
                    animateOutCaptionLayout = lastDrawingCaptionLayout;
                    changed = true;
                } else {
                    updateCaptionLayout();
                    if (lastDrawingCaptionX != captionX || lastDrawingCaptionY != captionY) {
                        moveCaption = true;
                        captionFromX = lastDrawingCaptionX;
                        captionFromY = lastDrawingCaptionY;
                        changed = true;
                    }
                }
            } else if (captionLayout != null && lastDrawingCaptionLayout != null) {
                updateCaptionLayout();
                if (lastDrawingCaptionX != captionX || lastDrawingCaptionY != captionY) {
                    moveCaption = true;
                    captionFromX = lastDrawingCaptionX;
                    captionFromY = lastDrawingCaptionY;
                    changed = true;
                }
            }
            if (!lastDrawBotButtons.isEmpty() || !botButtons.isEmpty()) {
                transitionBotButtons.addAll(lastDrawBotButtons);
            }

            if (documentAttachType == DOCUMENT_ATTACH_TYPE_AUDIO || documentAttachType == DOCUMENT_ATTACH_TYPE_DOCUMENT) {
                if (buttonX != lastButtonX || buttonY != lastButtonY) {
                    animateFromButtonX = lastButtonX;
                    animateFromButtonY = lastButtonY;
                    animateButton = true;
                    changed = true;
                }
            }

            boolean timeDrawablesIsChanged = false;

            if (lastIsPinned != isPinned) {
                animatePinned = true;
                changed = true;
                timeDrawablesIsChanged = true;
            }

            if ((lastRepliesLayout != null || repliesLayout != null) && lastRepliesCount != getRepliesCount()) {
                animateRepliesLayout = lastRepliesLayout;
                animateReplies = true;
                changed = true;
                timeDrawablesIsChanged = true;
            }

            if (lastViewsLayout != null && this.lastViewsCount != getMessageObject().messageOwner.views) {
                animateViewsLayout = lastViewsLayout;
                changed = true;
                timeDrawablesIsChanged = true;
            }

            if (commentLayout != null && lastCommentsCount != getRepliesCount()) {
                if (lastCommentLayout != null && !TextUtils.equals(lastCommentLayout.getText(), commentLayout.getText())) {
                    animateCommentsLayout = lastCommentLayout;
                } else {
                    animateCommentsLayout = null;
                }
                animateTotalCommentWidth = lastTotalCommentWidth;
                animateCommentX = lastCommentX;
                animateCommentArrowX = lastCommentArrowX;
                animateCommentUnreadX = lastCommentUnreadX;
                animateCommentDrawUnread = lastCommentDrawUnread;
                animateDrawCommentNumber = lastDrawCommentNumber;
                animateComments = true;
                changed = true;
            }

            if (!TextUtils.equals(lastSignMessage, lastPostAuthor)) {
                animateSign = true;
                animateNameX = nameX;
                changed = true;
            }

            if (lastDrawTime == forceNotDrawTime) {
                animateDrawingTimeAlpha = true;
                animateViewsLayout = null;
                changed = true;
            } else if (lastShouldDrawTimeOnMedia != shouldDrawTimeOnMedia()) {
                animateEditedEnter = false;
                animateShouldDrawTimeOnMedia = true;
                animateFromTimeX = lastTimeX;
                animateTimeLayout = lastTimeLayout;
                animateTimeWidth = lastTimeWidth;
                changed = true;
            } else if (timeDrawablesIsChanged || timeX != lastTimeX) {
                shouldAnimateTimeX = true;
                animateTimeWidth = lastTimeWidth;
                animateFromTimeX = lastTimeX;
                animateFromTimeXViews = lastTimeXViews;
                animateFromTimeXReplies = lastTimeXReplies;
                animateFromTimeXPinned = lastTimeXPinned;
            }

            if (lastShouldDrawMenuDrawable != shouldDrawMenuDrawable()) {
                animateShouldDrawMenuDrawable = true;
            }

            if (lastLocatinIsExpired != locationExpired) {
                animateLocationIsExpired = true;
            }

            if (lastIsPlayingRound != isPlayingRound) {
                animatePlayingRound = true;
                changed = true;
            }

            if (lastDrawingTextY != textY) {
                animateText = true;
                animateFromTextY = lastDrawingTextY;
                changed = true;
            }

            if (currentMessageObject != null) {
                if (lastDrawnForwardedName != currentMessageObject.needDrawForwarded()) {
                    animateForwardedLayout = true;
                    animatingForwardedNameLayout[0] = lastDrawnForwardedNameLayout[0];
                    animatingForwardedNameLayout[1] = lastDrawnForwardedNameLayout[1];
                    animateForwardNameX = lastForwardNameX;
                    animateForwardedNamesOffset = lastForwardedNamesOffset;
                    animateForwardNameWidth = lastForwardNameWidth;
                    changed = true;
                }
            }
            return changed;
        }

        public void onDetach() {
            wasDraw = false;
        }

        public void resetAnimation() {
            animateChange = false;
            animatePinned = false;
            animateBackgroundBoundsInner = false;
            deltaLeft = 0;
            deltaRight = 0;
            deltaBottom = 0;
            deltaTop = 0;
            if (imageChangeBoundsTransition && animateToImageW != 0 && animateToImageH != 0) {
                photoImage.setImageCoords(animateToImageX, animateToImageY, animateToImageW, animateToImageH);
            }
            if (animateRadius) {
                photoImage.setRoundRadius(animateToRadius);
            }
            animateToImageX = 0;
            animateToImageY = 0;
            animateToImageW = 0;
            animateToImageH = 0;
            imageChangeBoundsTransition = false;
            changePinnedBottomProgress = 1f;
            captionEnterProgress = 1f;
            animateRadius = false;
            animateChangeProgress = 1f;
            animateMessageText = false;
            animateOutTextBlocks = null;
            animateEditedLayout = null;
            animateTimeLayout = null;
            animateEditedEnter = false;
            animateReplaceCaptionLayout = false;
            transformGroupToSingleMessage = false;
            animateOutCaptionLayout = null;
            moveCaption = false;
            animateDrawingTimeAlpha = false;
            transitionBotButtons.clear();
            animateButton = false;

            animateReplies = false;
            animateRepliesLayout = null;

            animateComments = false;
            animateCommentsLayout = null;
            animateViewsLayout = null;
            animateShouldDrawTimeOnMedia = false;
            animateShouldDrawMenuDrawable = false;
            shouldAnimateTimeX = false;
            animateSign = false;
            animateDrawingTimeAlpha = false;
            animateLocationIsExpired = false;
            animatePlayingRound = false;
            animateText = false;
            animateForwardedLayout = false;
            animatingForwardedNameLayout[0] = null;
            animatingForwardedNameLayout[1] = null;
        }

        public boolean supportChangeAnimation() {
            return true;
        }

        public int createStatusDrawableParams() {
            if (currentMessageObject.isOutOwner()) {
                boolean drawCheck1 = false;
                boolean drawCheck2 = false;
                boolean drawClock = false;
                boolean drawError = false;

                if (currentMessageObject.isSending() || currentMessageObject.isEditing()) {
                    drawCheck2 = false;
                    drawClock = true;
                    drawError = false;
                } else if (currentMessageObject.isSendError()) {
                    drawCheck2 = false;
                    drawClock = false;
                    drawError = true;
                } else if (currentMessageObject.isSent()) {
                    if (!currentMessageObject.scheduled && !currentMessageObject.isUnread()) {
                        drawCheck1 = true;
                    } else {
                        drawCheck1 = false;
                    }
                    drawCheck2 = true;
                    drawClock = false;
                    drawError = false;
                }
                return (drawCheck1 ? 1 : 0) | (drawCheck2 ? 2 : 0) | (drawClock ? 4 : 0) | (drawError ? 8 : 0);
            } else {
                boolean drawClock = currentMessageObject.isSending() || currentMessageObject.isEditing();
                boolean drawError = currentMessageObject.isSendError();

                return (drawClock ? 4 : 0) | (drawError ? 8 : 0);
            }
        }
    }

    private int getThemedColor(String key) {
        Integer color = resourcesProvider != null ? resourcesProvider.getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }

    private Drawable getThemedDrawable(String key) {
        Drawable drawable = resourcesProvider != null ? resourcesProvider.getDrawable(key) : null;
        return drawable != null ? drawable : Theme.getThemeDrawable(key);
    }

    private Paint getThemedPaint(String paintKey) {
        Paint paint = resourcesProvider != null ? resourcesProvider.getPaint(paintKey) : null;
        return paint != null ? paint : Theme.getThemePaint(paintKey);
    }
    
    private boolean hasGradientService() {
        return resourcesProvider != null ? resourcesProvider.hasGradientService() : Theme.hasGradientService();
    }
}

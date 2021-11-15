package org.telegram.ui;

import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextPaint;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.core.math.MathUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.SimpleTextView;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.CalendarBottomButton;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SharedMediaLayout;
import org.telegram.ui.Components.TitleNumberTextView;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Calendar;

public class MediaCalendarActivity extends BaseFragment {

    FrameLayout contentView;

    RecyclerListView listView;
    LinearLayoutManager layoutManager;
    TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    TextPaint activeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    TextPaint textPaint2 = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    Paint blackoutPaint = new Paint();
    Paint selectDayPaint = new Paint();

    private long dialogId;
    private boolean loading;
    private boolean checkEnterItems;
    private boolean isFromChat;
    private boolean isSelectRangeMode;
    private CalendarBottomButton bottomOverlayText;
    private FrameLayout bottomOverlay;
    private Paint bottomButtonBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private PreviewDialog previewDialog;
    private View blurView;

    int startFromYear;
    int startFromMonth;
    int monthCount;

    CalendarAdapter adapter;
    Callback callback;

    SparseArray<SparseArray<PeriodDay>> messagesByYearMounth = new SparseArray<>();
    boolean endReached;
    int startOffset = 0;
    int lastId;
    int minMontYear;
    private int photosVideosTypeFilter;
    private boolean isOpened;
    int selectedYear;
    int selectedMonth;
    private TitleNumberTextView selectedMessagesCountTextView;
    private int startDate = 0;
    private int endDate = 0;
    private int selectedDay = 0;
    private int selectedDays = 0;
    private boolean isJumpToDaySelected;

    public MediaCalendarActivity(Bundle args, int photosVideosTypeFilter, int selectedDate, boolean isFromChat) {
        super(args);
        this.photosVideosTypeFilter = photosVideosTypeFilter;

        if (selectedDate != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(selectedDate * 1000L);
            selectedYear = calendar.get(Calendar.YEAR);
            selectedMonth = calendar.get(Calendar.MONTH);
        }
        this.isFromChat = isFromChat;
    }

    public MediaCalendarActivity(Bundle args, int photosVideosTypeFilter, int selectedDate) {
        this(args, photosVideosTypeFilter, selectedDate, false);
    }

    @Override
    public boolean onFragmentCreate() {
        dialogId = getArguments().getLong("dialog_id");
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        textPaint.setTextSize(AndroidUtilities.dp(16));
        textPaint.setTextAlign(Paint.Align.CENTER);

        textPaint2.setTextSize(AndroidUtilities.dp(11));
        textPaint2.setTextAlign(Paint.Align.CENTER);
        textPaint2.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));

        activeTextPaint.setTextSize(AndroidUtilities.dp(16));
        activeTextPaint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        activeTextPaint.setTextAlign(Paint.Align.CENTER);

        selectDayPaint.setStrokeWidth(8);

        FrameLayout mainContent = new FrameLayout(context);
        contentView = new FrameLayout(context);
        LinearLayout actionBarLayout = new LinearLayout(context);
        actionBarLayout.setOrientation(LinearLayout.VERTICAL);
        mainContent.addView(actionBarLayout);
        createActionBar(context);
        actionBarLayout.addView(actionBar);
        actionBarLayout.addView(contentView);
        actionBar.setTitle(LocaleController.getString("Calendar", R.string.Calendar));
        actionBar.setCastShadows(false);
        actionBar.setAddToContainer(false);

        listView = new RecyclerListView(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                super.dispatchDraw(canvas);
                checkEnterItems = false;
            }
        };
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context));
        layoutManager.setReverseLayout(true);
        listView.setAdapter(adapter = new CalendarAdapter());
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                checkLoadNext();
            }
        });

        final ActionBarMenu actionMode = actionBar.createActionMode();
        if (isFromChat) {
            selectedMessagesCountTextView = new TitleNumberTextView(actionMode.getContext(),
                    LocaleController.getString("CalendarSelectDaysTitle", R.string.CalendarSelectDaysTitle));
            selectedMessagesCountTextView.setTextSize(18);
            selectedMessagesCountTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            selectedMessagesCountTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            selectedMessagesCountTextView.setNumber(0, LocaleController.formatPluralString("CalendarDaysCount", 0), true);
            actionMode.addView(selectedMessagesCountTextView, LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1.0f, 65, 0, 0, 0));
            bottomButtonBorderPaint.setColor(0xFFECECEC);
            bottomButtonBorderPaint.setStrokeWidth(4);
            bottomButtonBorderPaint.setStyle(Paint.Style.STROKE);
            bottomOverlay = new FrameLayout(context) {
                @Override
                public void onDraw(Canvas canvas) {
                    canvas.drawRect(AndroidUtilities.dp(8), AndroidUtilities.dp(4), getMeasuredWidth() - AndroidUtilities.dp(8),
                            getMeasuredHeight() - AndroidUtilities.dp(4), bottomButtonBorderPaint);
                }
            };
            bottomOverlay.setWillNotDraw(false);
            bottomOverlay.setVisibility(View.VISIBLE);
            bottomOverlay.setFocusable(true);
            bottomOverlay.setFocusableInTouchMode(true);
            bottomOverlay.setClickable(true);
            contentView.addView(bottomOverlay, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 56, Gravity.BOTTOM));

            bottomOverlayText = new CalendarBottomButton(getParentActivity(), getResourceProvider());
            bottomOverlayText.setText(LocaleController.getString("CalendarSelectDaysButton", R.string.CalendarSelectDaysButton),
                    Theme.key_chat_fieldOverlayText);
            bottomOverlayText.setOnClickListener(v -> {
                if (!isSelectRangeMode) {
                    setSelectRangeMode();
                }
                if (isSelectRangeMode && bottomOverlayText.isEnabled()) {
                    createDeleteHistoryConfirmationDialog(false);
                }
            });
            bottomOverlay.addView(bottomOverlayText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM));
        }
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, 0, 0, 36, 0,  isFromChat ? 56 : 0));

        final String[] daysOfWeek = new String[]{
                LocaleController.getString("CalendarWeekNameShortMonday", R.string.CalendarWeekNameShortMonday),
                LocaleController.getString("CalendarWeekNameShortTuesday", R.string.CalendarWeekNameShortTuesday),
                LocaleController.getString("CalendarWeekNameShortWednesday", R.string.CalendarWeekNameShortWednesday),
                LocaleController.getString("CalendarWeekNameShortThursday", R.string.CalendarWeekNameShortThursday),
                LocaleController.getString("CalendarWeekNameShortFriday", R.string.CalendarWeekNameShortFriday),
                LocaleController.getString("CalendarWeekNameShortSaturday", R.string.CalendarWeekNameShortSaturday),
                LocaleController.getString("CalendarWeekNameShortSunday", R.string.CalendarWeekNameShortSunday),
        };

        Drawable headerShadowDrawable = ContextCompat.getDrawable(context, R.drawable.header_shadow).mutate();

        View calendarSignatureView = new View(context) {

            @Override
            protected void onDraw(Canvas canvas) {
                super.onDraw(canvas);
                float xStep = getMeasuredWidth() / 7f;
                for (int i = 0; i < 7; i++) {
                    float cx = xStep * i + xStep / 2f;
                    float cy = (getMeasuredHeight() - AndroidUtilities.dp(2)) / 2f;
                    canvas.drawText(daysOfWeek[i], cx, cy + AndroidUtilities.dp(5), textPaint2);
                }
                headerShadowDrawable.setBounds(0, getMeasuredHeight() - AndroidUtilities.dp(3), getMeasuredWidth(), getMeasuredHeight());
                headerShadowDrawable.draw(canvas);
            }
        };

        contentView.addView(calendarSignatureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, 0, 0, 0, 0, 0));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    if (actionBar.isActionModeShowed()) {
                        setOriginMode();
                    } else {
                        finishFragment(true);
                    }
                }
            }
        });

        fragmentView = mainContent;

        Calendar calendar = Calendar.getInstance();
        startFromYear = calendar.get(Calendar.YEAR);
        startFromMonth = calendar.get(Calendar.MONTH);

        if (selectedYear != 0) {
            monthCount = (startFromYear - selectedYear) * 12 + startFromMonth - selectedMonth + 1;
            layoutManager.scrollToPositionWithOffset(monthCount - 1, AndroidUtilities.dp(120));
        }
        if (monthCount < 3) {
            monthCount = 3;
        }

        if (isFromChat) {
            callback = new MediaCalendarActivity.Callback() {
                @Override
                public void onDateSelected(MessageObject messageObject, int startOffset) {}

                @Override
                public void onDateClicked(int timestamp) {
                    selectedDay = timestamp;
                    MediaCalendarActivity.this.onDateClicked(timestamp);
                }
            };
        }

        loadNext();
        updateColors();
        activeTextPaint.setColor(Color.WHITE);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setBackButtonDrawable((new BackDrawable(false)));

        blurView = new View(context) {
            @Override
            public void setAlpha(float alpha) {
                super.setAlpha(alpha);
                if (fragmentView != null) {
                    fragmentView.invalidate();
                }
            }
        };
        blurView.setVisibility(View.GONE);
        mainContent.addView(blurView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private void createDeleteHistoryConfirmationDialog(boolean isSingleDay) {
        final boolean[] deleteForAll = new boolean[1];
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        FrameLayout frameLayout = new FrameLayout(getParentActivity());
        builder.setTitle(
                LocaleController.getString("AreYouSureDeleteMessagesCalendarTitle", R.string.AreYouSureDeleteMessagesCalendarTitle));
        final String areYouSureDeleteMessages = LocaleController
                .getString("AreYouSureDeleteMessagesCalendar", R.string.AreYouSureDeleteMessagesCalendar);
        final String selectedDaysCount = LocaleController.formatPluralString("SelectedDaysCount", selectedDays);
        builder.setMessage(String.format("%s %s", areYouSureDeleteMessages, selectedDaysCount));
        CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1, getResourceProvider());
        cell.setBackground(Theme.getSelectorDrawable(false));
        TLRPC.User user = getMessagesController().getUser(dialogId);
        cell.setText(
                LocaleController
                        .formatString("DeleteMessagesOptionAlso", R.string.DeleteMessagesOptionAlso, UserObject.getFirstName(user)),
                "", false, false);
        cell.setOnClickListener(c -> {
            CheckBoxCell cell12 = (CheckBoxCell) c;
            deleteForAll[0] = !deleteForAll[0];
            cell12.setChecked(deleteForAll[0], true);
        });
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0,
                LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
        frameLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.LEFT, 0, 0, 0, 0));
        builder.setPositiveButton(LocaleController.getString("Delete", R.string.Delete), (dialogInterface, i) -> {
            if (isSingleDay) {
                MessagesController.getInstance(currentAccount).deleteMessageHistoryForDaysRange(dialogId, selectedDay, selectedDay + 86400, deleteForAll[0]);
            } else {
                MessagesController.getInstance(currentAccount).deleteMessageHistoryForDaysRange(dialogId, startDate, endDate, deleteForAll[0]);
            }
            setOriginMode();
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.setView(frameLayout);
        builder.setCustomViewOffset(9);
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
        }
    }

    private void onDateClicked(int timestamp) {
        if (isSelectRangeMode) {
            if (startDate == 0) {
                startDate = timestamp;
                endDate = timestamp;
            } else {
                final int tempDate = startDate;
                startDate = Math.min(startDate, timestamp);
                if (startDate == timestamp) {
                    endDate = tempDate;
                } else {
                    endDate = timestamp;
                }
            }
            selectedDays = Math.round((endDate - startDate) / (24f * 60 * 60) + 1);
            selectedMessagesCountTextView.setNumber(selectedDays, LocaleController.formatPluralString("CalendarDaysCount", selectedDays), true);
            bottomOverlayText.setEnabled(selectedDays != 0);
            for (int i = 0; i < listView.getChildCount(); i++) {
                View child = listView.getChildAt(i);
                if (child instanceof MonthView) {
                    child.invalidate();
                }
            }
        } else {
            jumpToDate(timestamp);
        }
    }

    private void setSelectRangeMode() {
        isSelectRangeMode = true;
        bottomOverlayText.setText(LocaleController.getString("CalendarClearHistoryButton", R.string.CalendarClearHistoryButton), true, Theme.key_chat_fieldOverlayTextWarning);
        bottomOverlayText.setEnabled(false);
        selectedMessagesCountTextView.setNumber(0, LocaleController.formatPluralString("CalendarDaysCount" ,0), true);
        actionBar.showActionMode(true);
    }

    private void setOriginMode() {
        isSelectRangeMode = false;
        selectedDays = 0;
        startDate = 0;
        endDate = 0;
        bottomOverlayText.setText(LocaleController.getString("CalendarSelectDaysButton", R.string.CalendarSelectDaysButton), false, Theme.key_chat_fieldOverlayText);
        bottomOverlayText.setEnabled(true);
        actionBar.hideActionMode();
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof MonthView) {
                child.invalidate();
            }
        }
    }

    private void updateColors() {
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        activeTextPaint.setColor(Color.WHITE);
        textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textPaint2.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), true);
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_listSelector), false);
    }

    private void loadNext() {
        if (loading || endReached) {
            return;
        }
        loading = true;
        TLRPC.TL_messages_getSearchResultsCalendar req = new TLRPC.TL_messages_getSearchResultsCalendar();
        if (photosVideosTypeFilter == SharedMediaLayout.FILTER_PHOTOS_ONLY) {
            req.filter = new TLRPC.TL_inputMessagesFilterPhotos();
        } else if (photosVideosTypeFilter == SharedMediaLayout.FILTER_VIDEOS_ONLY) {
            req.filter = new TLRPC.TL_inputMessagesFilterVideo();
        } else {
            req.filter = new TLRPC.TL_inputMessagesFilterPhotoVideo();
        }

        req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
        req.offset_id = lastId;

        Calendar calendar = Calendar.getInstance();
        listView.setItemAnimator(null);
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_messages_searchResultsCalendar res = (TLRPC.TL_messages_searchResultsCalendar) response;

                for (int i = 0; i < res.periods.size(); i++) {
                    TLRPC.TL_searchResultsCalendarPeriod period = res.periods.get(i);
                    calendar.setTimeInMillis(period.date * 1000L);
                    int month = calendar.get(Calendar.YEAR) * 100 + calendar.get(Calendar.MONTH);
                    SparseArray<PeriodDay> messagesByDays = messagesByYearMounth.get(month);
                    if (messagesByDays == null) {
                        messagesByDays = new SparseArray<>();
                        messagesByYearMounth.put(month, messagesByDays);
                    }
                    PeriodDay periodDay = new PeriodDay();
                    MessageObject messageObject = new MessageObject(currentAccount, res.messages.get(i), false, false);
                    periodDay.messageObject = messageObject;
                    startOffset += res.periods.get(i).count;
                    periodDay.startOffset = startOffset;
                    int index = calendar.get(Calendar.DAY_OF_MONTH) - 1;
                    if (messagesByDays.get(index, null) == null) {
                        messagesByDays.put(index, periodDay);
                    }
                    if (month < minMontYear || minMontYear == 0) {
                        minMontYear = month;
                    }

                }

                loading = false;
                if (!res.messages.isEmpty()) {
                    lastId = res.messages.get(res.messages.size() - 1).id;
                    endReached = false;
                    checkLoadNext();
                } else {
                    endReached = true;
                }
                if (isOpened) {
                    checkEnterItems = true;
                }
                listView.invalidate();
                int newMonthCount = (int) (((calendar.getTimeInMillis() / 1000) - res.min_date) / 2629800) + 1;
                adapter.notifyItemRangeChanged(0, monthCount);
                if (newMonthCount > monthCount) {
                    adapter.notifyItemRangeInserted(monthCount + 1, newMonthCount);
                    monthCount = newMonthCount;
                }
                if (endReached) {
                    resumeDelayedFragmentAnimation();
                }
            }
        }));
    }

    private void checkLoadNext() {
        if (loading || endReached) {
            return;
        }
        int listMinMonth = Integer.MAX_VALUE;
        for (int i = 0; i < listView.getChildCount(); i++) {
            View child = listView.getChildAt(i);
            if (child instanceof MonthView) {
                int currentMonth = ((MonthView) child).currentYear * 100 + ((MonthView) child).currentMonthInYear;
                if (currentMonth < listMinMonth) {
                    listMinMonth = currentMonth;
                }
            }
        };
        int min1 = (minMontYear / 100 * 12) + minMontYear % 100;
        int min2 = (listMinMonth / 100 * 12) + listMinMonth % 100;
        if (min1 + 3 >= min2) {
            loadNext();
        }
    }

    private class CalendarAdapter extends RecyclerView.Adapter {

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new RecyclerListView.Holder(new MonthView(parent.getContext()));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            MonthView monthView = (MonthView) holder.itemView;

            int year = startFromYear - position / 12;
            int month = startFromMonth - position % 12;
            if (month < 0) {
                month += 12;
                year--;
            }
            boolean animated = monthView.currentYear == year && monthView.currentMonthInYear == month;
            monthView.setDate(year, month, messagesByYearMounth.get(year * 100 + month), animated);
        }

        @Override
        public long getItemId(int position) {
            int year = startFromYear - position / 12;
            int month = startFromMonth - position % 12;
            return year * 100L + month;
        }

        @Override
        public int getItemCount() {
            return monthCount;
        }
    }

    private class MonthView extends FrameLayout {

        SimpleTextView titleView;
        int currentYear;
        int currentMonthInYear;
        int daysInMonth;
        int startDayOfWeek;
        int cellCount;
        int startMonthTime;
        boolean needToDrawSelectedCircle;

        SparseArray<PeriodDay> messagesByDays = new SparseArray<>();
        SparseArray<ImageReceiver> imagesByDays = new SparseArray<>();
        SparseArray<ImageReceiver> allDays = new SparseArray<>();
        SparseIntArray timeStamps = new SparseIntArray();

        SparseArray<PeriodDay> animatedFromMessagesByDays = new SparseArray<>();
        SparseArray<ImageReceiver> animatedFromImagesByDays = new SparseArray<>();
        private final GestureDetector gestureDetector;

        boolean attached;
        float animationProgress = 1f;
        boolean pressed;
        float pressedX;
        float pressedY;

        public MonthView(Context context) {
            super(context);
            setWillNotDraw(false);
            titleView = new SimpleTextView(context);
            titleView.setTextSize(15);
            titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            titleView.setGravity(Gravity.CENTER);
            titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 28, 0, 0, 12, 0, 4));

            gestureDetector = new GestureDetector(context, new GestureDetector.OnGestureListener() {

                @Override
                public boolean onDown(MotionEvent e) {
                    //somehow always onLongClick
                    return true;
                }

                @Override
                public void onShowPress(MotionEvent e) { }

                @Override
                public boolean onSingleTapUp(MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        pressed = true;
                        pressedX = event.getX();
                        pressedY = event.getY();
                        if (pressed) {
                            if (isFromChat) {
                                for (int i = 0, n = allDays.size(); i < n; i++) {
                                    if (allDays.valueAt(i).getDrawRegion().contains(pressedX, pressedY)) {
                                        callback.onDateClicked(timeStamps.get(i));
                                        break;
                                    }
                                }
                            } else {
                                for (int i = 0; i < imagesByDays.size(); i++) {
                                    if (imagesByDays.valueAt(i).getDrawRegion().contains(pressedX, pressedY)) {
                                        if (callback != null) {
                                            PeriodDay periodDay = messagesByDays.valueAt(i);
                                            callback.onDateSelected(periodDay.messageObject, periodDay.startOffset);
                                            finishFragment();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        pressed = false;
                    } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                        pressed = false;
                    }
                    return pressed;
                }

                @Override
                public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                    return false;
                }

                @Override
                public void onLongPress(MotionEvent event) {
                    pressed = true;
                    pressedX = event.getX();
                    pressedY = event.getY();
                    if (isFromChat) {
                        for (int i = 0, n = allDays.size(); i < n; i++) {
                            if (allDays.valueAt(i).getDrawRegion().contains(pressedX, pressedY)) {
                                selectedDay = timeStamps.get(i);
                                previewDialog = new PreviewDialog(getParentActivity(), getResourceProvider());
                                previewDialog.setOnDismissListener(dialog -> previewDialog = null);
                                previewDialog.show();
                                break;
                            }
                        }
                    }
                }

                @Override
                public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                    return false;
                }
            });
        }

        public void setDate(int year, int monthInYear, SparseArray<PeriodDay> messagesByDays, boolean animated) {
            boolean dateChanged = year != currentYear && monthInYear != currentMonthInYear;
            currentYear = year;
            currentMonthInYear = monthInYear;
            this.messagesByDays = messagesByDays;

            if (dateChanged) {
                if (imagesByDays != null) {
                    for (int i = 0; i < imagesByDays.size(); i++) {
                        imagesByDays.valueAt(i).onDetachedFromWindow();
                        imagesByDays.valueAt(i).setParentView(null);
                    }
                    imagesByDays = null;
                }
            }
            if (messagesByDays != null) {
                if (imagesByDays == null) {
                    imagesByDays = new SparseArray<>();
                }

                for (int i = 0; i < messagesByDays.size(); i++) {
                    int key = messagesByDays.keyAt(i);
                    if (imagesByDays.get(key, null) != null) {
                        continue;
                    }
                    ImageReceiver receiver = new ImageReceiver();
                    receiver.setParentView(this);
                    PeriodDay periodDay = messagesByDays.get(key);
                    MessageObject messageObject = periodDay.messageObject;
                    if (messageObject != null) {
                        if (messageObject.isVideo()) {
                            TLRPC.Document document = messageObject.getDocument();
                            TLRPC.PhotoSize thumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 50);
                            TLRPC.PhotoSize qualityThumb = FileLoader.getClosestPhotoSizeWithSize(document.thumbs, 320);
                            if (thumb == qualityThumb) {
                                qualityThumb = null;
                            }
                            if (thumb != null) {
                                if (messageObject.strippedThumb != null) {
                                    receiver.setImage(ImageLocation.getForDocument(qualityThumb, document), "44_44", messageObject.strippedThumb, null, messageObject, 0);
                                } else {
                                    receiver.setImage(ImageLocation.getForDocument(qualityThumb, document), "44_44", ImageLocation.getForDocument(thumb, document), "b", (String) null, messageObject, 0);
                                }
                            }
                        } else if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaPhoto && messageObject.messageOwner.media.photo != null && !messageObject.photoThumbs.isEmpty()) {
                            TLRPC.PhotoSize currentPhotoObjectThumb = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 50);
                            TLRPC.PhotoSize currentPhotoObject = FileLoader.getClosestPhotoSizeWithSize(messageObject.photoThumbs, 320, false, currentPhotoObjectThumb, false);
                            if (messageObject.mediaExists || DownloadController.getInstance(currentAccount).canDownloadMedia(messageObject)) {
                                if (currentPhotoObject == currentPhotoObjectThumb) {
                                    currentPhotoObjectThumb = null;
                                }
                                if (messageObject.strippedThumb != null) {
                                    receiver.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "44_44", null, null, messageObject.strippedThumb, currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                                } else {
                                    receiver.setImage(ImageLocation.getForObject(currentPhotoObject, messageObject.photoThumbsObject), "44_44", ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", currentPhotoObject != null ? currentPhotoObject.size : 0, null, messageObject, messageObject.shouldEncryptPhotoOrVideo() ? 2 : 1);
                                }
                            } else {
                                if (messageObject.strippedThumb != null) {
                                    receiver.setImage(null, null, messageObject.strippedThumb, null, messageObject, 0);
                                } else {
                                    receiver.setImage(null, null, ImageLocation.getForObject(currentPhotoObjectThumb, messageObject.photoThumbsObject), "b", (String) null, messageObject, 0);
                                }
                            }
                        }
                        receiver.setRoundRadius(AndroidUtilities.dp(22));
                        imagesByDays.put(key, receiver);
                    }
                }
            }

            YearMonth yearMonthObject = YearMonth.of(year, monthInYear + 1);
            daysInMonth = yearMonthObject.lengthOfMonth();

            Calendar calendar = Calendar.getInstance();
            calendar.set(year, monthInYear, 0);
            startDayOfWeek = (calendar.get(Calendar.DAY_OF_WEEK) + 6) % 7;
            startMonthTime = (int) (calendar.getTimeInMillis() / 1000L);

            int totalColumns = daysInMonth + startDayOfWeek;
            cellCount = (int) (totalColumns / 7f) + (totalColumns % 7 == 0 ? 0 : 1);
            calendar.set(year, monthInYear + 1, 0);
            titleView.setText(LocaleController.formatYearMont(calendar.getTimeInMillis() / 1000, true));

            for (int i = 0; i < daysInMonth; i++) {
                ImageReceiver receiver = new ImageReceiver();
                receiver.setImageBitmap(new ColorDrawable(Color.TRANSPARENT));
                receiver.setParentView(this);
                receiver.setRoundRadius(AndroidUtilities.dp(22));
                calendar.set(year, monthInYear, i + 1, 0, 0, 0);
                allDays.put(i, receiver);
                timeStamps.put(i, (int) (calendar.getTimeInMillis() / 1000));
            }
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(cellCount * (44 + 8) + 44), MeasureSpec.EXACTLY));
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            return gestureDetector.onTouchEvent(event);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            int currentCell = 0;
            int currentColumn = startDayOfWeek;

            float xStep = getMeasuredWidth() / 7f;
            float yStep = AndroidUtilities.dp(44 + 8);
            for (int i = 0; i < daysInMonth; i++) {
                float cx = xStep * currentColumn + xStep / 2f;
                float cy = yStep * currentCell + yStep / 2f + AndroidUtilities.dp(44);
                int nowTime = (int) (System.currentTimeMillis() / 1000L);
                long timestamp = timeStamps.get(i);
                needToDrawSelectedCircle = timestamp >= startDate && timestamp <= endDate;
                if (nowTime < startMonthTime + (i + 1) * 86400) {
                    int oldAlpha = textPaint.getAlpha();
                    textPaint.setAlpha((int) (oldAlpha * 0.3f));
                    drawSelectedCircle(canvas, i, cx, cy, currentColumn, false,  i == 0, i == daysInMonth - 1);
                    textPaint.setAlpha(oldAlpha);
                } else if (messagesByDays != null && messagesByDays.get(i, null) != null) {
                    float alpha = 1f;
                    if (imagesByDays.get(i) != null) {
                        if (checkEnterItems && !messagesByDays.get(i).wasDrawn) {
                            messagesByDays.get(i).enterAlpha = 0f;
                            messagesByDays.get(i).startEnterDelay = (cy + getY()) / listView.getMeasuredHeight() * 150;
                        }
                        if (messagesByDays.get(i).startEnterDelay > 0) {
                            messagesByDays.get(i).startEnterDelay -= 16;
                            if (messagesByDays.get(i).startEnterDelay < 0) {
                                messagesByDays.get(i).startEnterDelay = 0;
                            } else {
                                invalidate();
                            }
                        }
                        if (messagesByDays.get(i).startEnterDelay == 0 && messagesByDays.get(i).enterAlpha != 1f) {
                            messagesByDays.get(i).enterAlpha += 16 / 220f;
                            if (messagesByDays.get(i).enterAlpha > 1f) {
                                messagesByDays.get(i).enterAlpha = 1f;
                            } else {
                                invalidate();
                            }
                        }
                        alpha = messagesByDays.get(i).enterAlpha;
                        if (alpha != 1f) {
                            canvas.save();
                            float s =  0.8f + 0.2f * alpha;
                            canvas.scale(s, s,cx, cy);
                        }
                        drawSelectedCircle(canvas, i, cx, cy, currentColumn, true,  i == 0, i == daysInMonth - 1);
                        imagesByDays.get(i).setAlpha(messagesByDays.get(i).enterAlpha);
                        if (needToDrawSelectedCircle) {
                            final int size = AndroidUtilities.dp(44 * 0.8f);
                            imagesByDays.get(i).setImageCoords(cx - size / 2f, cy - size / 2f, size, size);
                            imagesByDays.get(i).draw(canvas);
                            blackoutPaint.setColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (messagesByDays.get(i).enterAlpha * 80)));
                            canvas.drawCircle(cx, cy, AndroidUtilities.dp(44 * 0.8f) / 2f, blackoutPaint);
                        } else {
                            imagesByDays.get(i).setImageCoords(cx - AndroidUtilities.dp(44) / 2f, cy - AndroidUtilities.dp(44) / 2f, AndroidUtilities.dp(44), AndroidUtilities.dp(44));
                            imagesByDays.get(i).draw(canvas);
                            blackoutPaint.setColor(ColorUtils.setAlphaComponent(Color.BLACK, (int) (messagesByDays.get(i).enterAlpha * 80)));
                            canvas.drawCircle(cx, cy, AndroidUtilities.dp(44) / 2f, blackoutPaint);
                        }
                        messagesByDays.get(i).wasDrawn = true;
                        if (alpha != 1f) {
                            canvas.restore();
                        }
                    }
                    if (alpha != 1f) {
                        int oldAlpha = textPaint.getAlpha();
                        textPaint.setAlpha((int) (oldAlpha * (1f - alpha)));
                        canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), textPaint);
                        textPaint.setAlpha(oldAlpha);

                        oldAlpha = textPaint.getAlpha();
                        activeTextPaint.setAlpha((int) (oldAlpha * alpha));
                        canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), activeTextPaint);
                        activeTextPaint.setAlpha(oldAlpha);
                    } else {
                        canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), activeTextPaint);
                    }

                } else {
                    drawSelectedCircle(canvas, i, cx, cy, currentColumn, false,  i == 0, i == daysInMonth - 1);
                }
                final ImageReceiver imageReceiver = allDays.get(i);
                imageReceiver.setImageCoords(cx - AndroidUtilities.dp(44) / 2f, cy - AndroidUtilities.dp(44) / 2f, AndroidUtilities.dp(44), AndroidUtilities.dp(44));
                imageReceiver.draw(canvas);
                currentColumn++;
                if (currentColumn >= 7) {
                    currentColumn = 0;
                    currentCell++;
                }
            }
        }

        private void drawSelectedCircle(Canvas canvas, int i, float cx, float cy, int currentColumn, boolean hasImage, boolean isFirstDay, boolean isLastDay) {
            if (needToDrawSelectedCircle) {
                long timestamp = timeStamps.get(i);
                float xStep = getMeasuredWidth() / 7f;
                final float radius = AndroidUtilities.dp(44) / 2f;
                selectDayPaint.setColor(getThemedColor(Theme.key_calendar_selected_day_edge));
                if (timestamp == startDate || timestamp == endDate) {
                    if (timestamp != endDate && !isLastDay) {
                        selectDayPaint.setAlpha((int) (255 * 0.3f));
                        canvas.drawRect(cx, cy - radius, cx + xStep, cy + radius, selectDayPaint);
                    }
                    selectDayPaint.setAlpha(255);
                    selectDayPaint.setStyle(Paint.Style.FILL);
                    selectDayPaint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                    canvas.drawCircle(cx, cy, radius, selectDayPaint);
                    selectDayPaint.setStyle(Paint.Style.STROKE);
                    selectDayPaint.setColor(getThemedColor(Theme.key_calendar_selected_day_edge));
                    canvas.drawCircle(cx, cy, radius, selectDayPaint);
                    selectDayPaint.setStyle(Paint.Style.FILL);
                    if (!hasImage) {
                        canvas.drawCircle(cx, cy, radius - AndroidUtilities.dp(4), selectDayPaint);
                    }
                    canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), activeTextPaint);
                } else {
                    selectDayPaint.setStyle(Paint.Style.FILL);
                    selectDayPaint.setAlpha((int) (255 * 0.3f));
                    if (currentColumn != 6 && !isLastDay) {
                        canvas.drawRect(cx, cy - radius, cx + xStep, cy + radius, selectDayPaint);
                    }
                    if (currentColumn == 6 || currentColumn == 0 || isFirstDay || isLastDay) {
                        RectF rectF = new RectF();
                        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius);
                        if (currentColumn == 0 || isFirstDay) {
                            canvas.drawArc(rectF, 90, 180, true, selectDayPaint);
                        }
                        if (currentColumn == 6 || isLastDay) {
                            canvas.drawArc(rectF, 270, 180, true, selectDayPaint);
                        }
                    }
                    canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), textPaint);
                }
            } else {
                canvas.drawText(Integer.toString(i + 1), cx, cy + AndroidUtilities.dp(5), textPaint);
            }
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            attached = true;
            if (imagesByDays != null) {
                for (int i = 0; i < imagesByDays.size(); i++) {
                    imagesByDays.valueAt(i).onAttachedToWindow();
                }
            }
            if (allDays != null) {
                for(int i = 0, n = allDays.size(); i < n; i++) {
                    allDays.valueAt(i).onAttachedToWindow();
                }
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            attached = false;
            if (imagesByDays != null) {
                for (int i = 0; i < imagesByDays.size(); i++) {
                    imagesByDays.valueAt(i).onDetachedFromWindow();
                }
            }
            if (allDays != null) {
                for(int i = 0, n = allDays.size(); i < n; i++) {
                    allDays.valueAt(i).onDetachedFromWindow();
                }
            }
        }
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public interface Callback {
        void onDateSelected(MessageObject messageObject, int startOffset);
        void onDateClicked(int timestamp);
    }

    private class PeriodDay {
        MessageObject messageObject;
        int startOffset;
        float enterAlpha = 1f;
        float startEnterDelay = 1f;
        boolean wasDrawn;
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {

        ThemeDescription.ThemeDescriptionDelegate descriptionDelegate = new ThemeDescription.ThemeDescriptionDelegate() {
            @Override
            public void didSetColor() {
                updateColors();
            }
        };
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        new ThemeDescription(null, 0, null, null, null, descriptionDelegate, Theme.key_windowBackgroundWhite);
        new ThemeDescription(null, 0, null, null, null, descriptionDelegate, Theme.key_windowBackgroundWhiteBlackText);
        new ThemeDescription(null, 0, null, null, null, descriptionDelegate, Theme.key_listSelector);


        return super.getThemeDescriptions();
    }

    @Override
    public boolean needDelayOpenAnimation() {
        return true;
    }

    @Override
    protected void onTransitionAnimationStart(boolean isOpen, boolean backward) {
        super.onTransitionAnimationStart(isOpen, backward);
        isOpened = true;
    }

    @Override
    public int getThemedColor(String key) {
        Integer color = getResourceProvider() != null ? getResourceProvider().getColor(key) : null;
        return color != null ? color : Theme.getColor(key);
    }

    public void jumpToDate(int date) {
        hidePreview();
    }

    public void clearHistory() {
        createDeleteHistoryConfirmationDialog(true);
    }

    public void selectCurrentDay(int date) {
        setSelectRangeMode();
        selectedDay = date;
        onDateClicked(date);
        hidePreview();
    }

    private class PreviewDialog extends Dialog {

        private final int shadowPaddingTop;
        private final int shadowPaddingLeft;
        private final Drawable pagerShadowDrawable = getContext().getResources().getDrawable(R.drawable.popup_fixed_alert2).mutate();
        private final TextView dateText = new TextView(getContext());
        private final TextView messagesCountText = new TextView(getContext());
        private final LinearLayout headerBackground;
        private final ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout;

        private ValueAnimator animator;
        private float animationProgress;
        private final FrameLayout mFrameLayout;
        private final FrameLayout.LayoutParams mLp;
        private final View chatActivityView;

        public PreviewDialog(@NonNull Context context, @NonNull Theme.ResourcesProvider resourcesProvider) {
            super(context, R.style.TransparentDialog2);
            setCancelable(true);
            getCountOfMessages(selectedDay);
            contentView.setVisibility(INVISIBLE);

            int backgroundColor = Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, getResourceProvider());
            pagerShadowDrawable.setColorFilter(new PorterDuffColorFilter(Color.TRANSPARENT, PorterDuff.Mode.MULTIPLY));
            pagerShadowDrawable.setCallback(contentView);
            android.graphics.Rect paddingRect = new android.graphics.Rect();
            pagerShadowDrawable.getPadding(paddingRect);
            shadowPaddingTop = paddingRect.top;
            shadowPaddingLeft = paddingRect.left;

            headerBackground = new LinearLayout(getParentActivity()) {

                private final Path path = new Path();

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY));
                }

                @Override
                public void draw(Canvas canvas) {
                    int count = canvas.save();
                    path.addRoundRect(new RectF(0, AndroidUtilities.dp(6), getWidth(), getHeight() + AndroidUtilities.dp(6)), AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                    canvas.clipPath(path);
                    super.draw(canvas);
                    canvas.restoreToCount(count);
                }
            };
            headerBackground.setOrientation(LinearLayout.VERTICAL);
            headerBackground.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault, getResourceProvider()));
            dateText.setMaxLines(1);
            dateText.setTextColor(Theme.getColor(Theme.key_actionBarDefaultTitle, getResourceProvider()));
            dateText.setTextSize(16);
            dateText.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            dateText.setText(LocaleController.formatDateChat(selectedDay, true));
            headerBackground.addView(dateText);
            LinearLayout.LayoutParams dateLP = (LinearLayout.LayoutParams) dateText.getLayoutParams();
            dateLP.topMargin = AndroidUtilities.dp(12);
            dateLP.leftMargin = AndroidUtilities.dp(12);

            messagesCountText.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubtitle, getResourceProvider()));
            messagesCountText.setTextSize(14);
            headerBackground.addView(messagesCountText);
            LinearLayout.LayoutParams messagesCountLP = (LinearLayout.LayoutParams) messagesCountText.getLayoutParams();
            messagesCountLP.topMargin = AndroidUtilities.dp(4);
            messagesCountLP.leftMargin = AndroidUtilities.dp(12);

            popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, resourcesProvider);
            popupLayout.setBackgroundColor(backgroundColor);

            ActionBarMenuSubItem jumpToDateCell = new ActionBarMenuSubItem(context, true, false);
            jumpToDateCell.setColors(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider), Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon, resourcesProvider));
            jumpToDateCell.setSelectorColor(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider));
            jumpToDateCell.setTextAndIcon(LocaleController.getString("JumpToDate", R.string.JumpToDate), R.drawable.msg_message);
            jumpToDateCell.setOnClickListener((v) -> {
                isJumpToDaySelected = true;
                jumpToDate(selectedDay);
            });
            popupLayout.addView(jumpToDateCell);

            ActionBarMenuSubItem selectDayCell = new ActionBarMenuSubItem(context, false, false);
            selectDayCell.setColors(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider), Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon, resourcesProvider));
            selectDayCell.setSelectorColor(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider));
            selectDayCell.setTextAndIcon("Select this day", R.drawable.msg_select);
            selectDayCell.setOnClickListener((v) -> {
                selectCurrentDay(selectedDay);
            });
            popupLayout.addView(selectDayCell);

            ActionBarMenuSubItem clearHistory = new ActionBarMenuSubItem(context, false, true);
            clearHistory.setColors(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider), Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon, resourcesProvider));
            clearHistory.setSelectorColor(Theme.getColor(Theme.key_dialogButtonSelector, resourcesProvider));
            clearHistory.setTextAndIcon("Clear history", R.drawable.msg_delete);
            clearHistory.setOnClickListener((v) -> {
                clearHistory();
                hidePreview();
            });
            popupLayout.addView(clearHistory);

            mFrameLayout = new FrameLayout(getParentActivity()) {

                private final Path path = new Path();

                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY));
                }

                @Override
                public void draw(Canvas canvas) {
                    int count = canvas.save();
                    path.addRoundRect(new RectF(0, -AndroidUtilities.dp(6), getWidth(), getHeight()), AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                    canvas.clipPath(path);
                    super.draw(canvas);
                    canvas.restoreToCount(count);
                }
            };

            Bundle args = new Bundle();
            args.putLong("user_id", dialogId);
            args.putInt("jump_to_date", selectedDay);
            ChatActivity chatActivity = new ChatActivity(args);
            presentFragmentAsPreview(chatActivity);
            chatActivityView = chatActivity.getFragmentView();
            mLp = (FrameLayout.LayoutParams) chatActivityView.getLayoutParams();
            parentLayout.drawPreviewFromCalendar(false);
            contentView.addView(popupLayout);
            parentLayout.addView(mFrameLayout);
            contentView.addView(headerBackground);
            mFrameLayout.setAlpha(0);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getWindow().setWindowAnimations(R.style.DialogNoAnimation);
            setContentView(contentView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.dimAmount = 0;
            params.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
            params.gravity = Gravity.TOP | Gravity.LEFT;
            if (Build.VERSION.SDK_INT >= 21) {
                params.flags |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR |
                        WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS;
            }
            if (Build.VERSION.SDK_INT >= 28) {
                params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            }
            getWindow().setAttributes(params);
        }

        @Override
        public void show() {
            super.show();
            AndroidUtilities.runOnUIThread(() -> {
                prepareBlurBitmap();
                runAnimation(true);
            }, 80);
        }

        @Override
        public void dismiss() {
            finishPreviewFragment();
            runAnimation(false);
        }

        private void runAnimation(boolean show) {
            if (animator != null) {
                animator.cancel();
            }

            int[] location = new int[2];
            mFrameLayout.getLocationOnScreen(location);
            final float fromScale = mFrameLayout.getWidth() * 1f / getContentWidth();
            final float xFrom = location[0] - (mFrameLayout.getLeft() + (int)((getContentWidth() * (1f - fromScale) / 2f)));
            final float yFrom = location[1] - (mFrameLayout.getTop() + (int)((getContentHeight() * (1f - fromScale) / 2f)));

            int popupLayoutTranslation = -popupLayout.getTop() / 2;
            animator = ValueAnimator.ofFloat(show ? 0f : 1f, show ? 1f : 0f);
            animator.addUpdateListener(animation -> {
                animationProgress = (float) animation.getAnimatedValue();
                float scale = fromScale + (1f - fromScale) * animationProgress;
                contentView.setScaleX(scale);
                contentView.setScaleY(scale);
                contentView.setTranslationX(xFrom * (1f - animationProgress));
                contentView.setTranslationY(yFrom * (1f - animationProgress));

                float alpha = MathUtils.clamp(2 * animationProgress - 1f, 0f, 1f);
                pagerShadowDrawable.setAlpha((int)(255 * alpha));
                dateText.setAlpha(alpha);
                messagesCountText.setAlpha(alpha);
                chatActivityView.setAlpha((int)(255 * alpha));
                headerBackground.setAlpha(alpha);
                blurView.setAlpha((int)(255 * alpha));
                popupLayout.setTranslationY(popupLayoutTranslation * (1f - animationProgress));
                popupLayout.setAlpha(alpha);
            });
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    contentView.setVisibility(VISIBLE);
                    if (show) {
                        parentLayout.drawPreviewFromCalendar(true);
                        parentLayout.removeView(mFrameLayout);
                        blurView.setVisibility(VISIBLE);
                        contentView.setScaleX(fromScale);
                        contentView.setScaleY(fromScale);
                    }
                }
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    if (!show) {
                        blurView.setVisibility(View.GONE);
                        blurView.setBackground(null);
                        PreviewDialog.super.dismiss();
                        if (isJumpToDaySelected) {
                            Bundle args = new Bundle();
                            args.putInt("jump_to_date", selectedDay);
                            args.putLong("user_id", dialogId);
                            ChatActivity chatActivity = new ChatActivity(args);
                            presentFragment(chatActivity, true);
                        }
                    }
                }
            });
            animator.setDuration(320);
            animator.setInterpolator(CubicBezierInterpolator.DEFAULT);
            animator.start();
        }

        private void prepareBlurBitmap() {
            if (blurView == null) {
                return;
            }
            int w = (int) (fragmentView.getMeasuredWidth() / 6.0f);
            int h = (int) (fragmentView.getMeasuredHeight() / 6.0f);
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            canvas.scale(1.0f / 6.0f, 1.0f / 6.0f);
            fragmentView.draw(canvas);
            Utilities.stackBlurBitmap(bitmap, Math.max(7, Math.max(w, h) / 180));
            blurView.setBackground(new BitmapDrawable(bitmap));
            blurView.setVisibility(View.VISIBLE);
        }

        private int getContentHeight() {
            int height = mFrameLayout.getMeasuredHeight();
            height += AndroidUtilities.dp(12) + dateText.getMeasuredHeight();
            height += AndroidUtilities.dp(4) + messagesCountText.getMeasuredHeight();
            height += AndroidUtilities.dp(12) + popupLayout.getMeasuredHeight();
            return height;
        }

        private int getContentWidth() {
            return mFrameLayout.getMeasuredWidth();
        }

        private void getCountOfMessages(int date) {
            final int[] offsets = new int[2];
            TLRPC.TL_messages_getHistory req = new TLRPC.TL_messages_getHistory();
            req.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
            req.offset_date = date;
            req.add_offset = -1;
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    offsets[0] = ((TLRPC.TL_messages_messagesSlice)response).offset_id_offset;
                    TLRPC.TL_messages_getHistory req1 = new TLRPC.TL_messages_getHistory();
                    req1.peer = MessagesController.getInstance(currentAccount).getInputPeer(dialogId);
                    req1.offset_date = date + 86400;
                    req1.limit = 1;
                    getConnectionsManager().sendRequest(req1, (response1, error1) -> {
                        if (error1 == null) {
                            offsets[1] = ((TLRPC.TL_messages_messagesSlice)response1).offset_id_offset;
                            AndroidUtilities.runOnUIThread(() -> {
                                messagesCountText.setText(LocaleController.formatPluralString("CalendarMessagesCount", Math.abs(offsets[0] - offsets[1])));
                            });
                        }
                    });
                }
            });
        }

        private final ViewGroup contentView = new ViewGroup(getContext()) {

            private final GestureDetector gestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onDown(MotionEvent e) {
                    return true;
                }
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    boolean isTouchInsideContent = pagerShadowDrawable.getBounds().contains((int) e.getX(), (int) e.getY()) ||
                            popupLayout.getLeft() < e.getX() && e.getX() < popupLayout.getRight() &&
                                    popupLayout.getTop() < e.getY() && e.getY() < popupLayout.getBottom();
                    if (!isTouchInsideContent) {
                        dismiss();
                    }
                    return super.onSingleTapUp(e);
                }
            });
            private final Path clipPath = new Path();
            private final RectF rectF = new RectF();
            private boolean firstSizeChange = true;

            @SuppressLint("ClickableViewAccessibility")
            @Override
            public boolean onTouchEvent(MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }

            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                setWillNotDraw(false);
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                int minSize = Math.min(getMeasuredWidth(), getMeasuredHeight());
                int sizeHeight = (int) (getMeasuredHeight() * 0.55f - AndroidUtilities.dp(12));
                int specHeight = MeasureSpec.makeMeasureSpec(sizeHeight, MeasureSpec.AT_MOST);
                int sizeWidth = Math.min(minSize, (int)(getMeasuredHeight() * 0.66)) - AndroidUtilities.dp(12) * 2;
                int specWidth = MeasureSpec.makeMeasureSpec(sizeWidth, MeasureSpec.AT_MOST);
                mFrameLayout.measure(specWidth, specHeight);
                headerBackground.measure(specWidth, specHeight);
                int textWidthSpec = MeasureSpec.makeMeasureSpec(sizeWidth - AndroidUtilities.dp(16) * 2, MeasureSpec.EXACTLY);
                //dateText.measure(textWidthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                //messagesCountText.measure(textWidthSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                popupLayout.measure(View.MeasureSpec.makeMeasureSpec(mFrameLayout.getMeasuredWidth() + shadowPaddingLeft * 2, MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            }

            @Override
            protected void onLayout(boolean changed, int l, int t, int r, int b) {
                int top = (getHeight() - getContentHeight()) / 2;
                int left = (getWidth() - mFrameLayout.getMeasuredWidth()) / 2;
                //dateText.layout(left + AndroidUtilities.dp(12), top, left + dateText.getMeasuredWidth(), top + dateText.getMeasuredHeight());
                headerBackground.layout(left, top - AndroidUtilities.dp(16), left + headerBackground.getMeasuredWidth(), top + dateText.getMeasuredHeight() + messagesCountText.getMeasuredHeight() + AndroidUtilities.dp(12));
                top += dateText.getMeasuredHeight();
                //messagesCountText.layout(dateText.getLeft(), top, dateText.getRight() - AndroidUtilities.dp(16), top + messagesCountText.getMeasuredHeight());
                top += messagesCountText.getMeasuredHeight();
                top += AndroidUtilities.dp(12);
                mFrameLayout.layout(left, top, left + mFrameLayout.getMeasuredWidth(), top + mFrameLayout.getMeasuredHeight());
                top += mFrameLayout.getMeasuredHeight();
                pagerShadowDrawable.setBounds(
                        mFrameLayout.getLeft() - shadowPaddingLeft,
                        dateText.getTop() - shadowPaddingTop - AndroidUtilities.dp(8),
                        mFrameLayout.getRight() + shadowPaddingLeft,
                        top + shadowPaddingTop
                );

                popupLayout.layout(left - shadowPaddingLeft, top, popupLayout.getMeasuredWidth() + shadowPaddingLeft, top + popupLayout.getMeasuredHeight());
                popupLayout.setVisibility(popupLayout.getBottom() < b ? VISIBLE : GONE);

                int radius = AndroidUtilities.dp(6);
                rectF.set(mFrameLayout.getLeft(), pagerShadowDrawable.getBounds().top, mFrameLayout.getRight(), pagerShadowDrawable.getBounds().top + radius * 2);
                clipPath.reset();
                clipPath.addRoundRect(rectF, radius, radius, Path.Direction.CW);
                rectF.set(l, pagerShadowDrawable.getBounds().top + radius, r, b);
                clipPath.addRect(rectF, Path.Direction.CW);

                mLp.topMargin = dateText.getMeasuredHeight() + messagesCountText.getMeasuredHeight();
                mLp.width = mFrameLayout.getWidth();
                mLp.leftMargin = mFrameLayout.getLeft();
                mLp.height = top - dateText.getMeasuredHeight() - messagesCountText.getMeasuredHeight();
            }

            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {
                super.onSizeChanged(w, h, oldw, oldh);
                boolean isLandscape = AndroidUtilities.displaySize.x > AndroidUtilities.displaySize.y;
                if (isLandscape) {
                    PreviewDialog.super.dismiss();
                }
                if (w != oldw && h != oldh) {
                    if (!firstSizeChange) {
                        prepareBlurBitmap();
                    }
                    firstSizeChange = false;
                }
            }

            @Override
            protected void dispatchDraw(Canvas canvas) {
                canvas.save();
                canvas.clipPath(clipPath);
                super.dispatchDraw(canvas);
                canvas.restore();
            }

            @Override
            protected void onDraw(Canvas canvas) {
                pagerShadowDrawable.draw(canvas);
                super.onDraw(canvas);
            }

            @Override
            protected boolean verifyDrawable(@NonNull Drawable who) {
                return who == pagerShadowDrawable || super.verifyDrawable(who);
            }
        };
    }

    private void hidePreview() {
        previewDialog.dismiss();
    }
}

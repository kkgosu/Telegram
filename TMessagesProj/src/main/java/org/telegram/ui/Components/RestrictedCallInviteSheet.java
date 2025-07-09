package org.telegram.ui.Components;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_phone;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;

public class RestrictedCallInviteSheet extends BottomSheet {

    public RestrictedCallInviteSheet(BaseFragment parentFragment, TLRPC.User contact) {
        super(parentFragment.getParentActivity(), false);
        final Context ctx = parentFragment.getParentActivity();

        LinearLayout rootContainer = createMainContainer(ctx);
        FrameLayout headerSection = createHeaderSection(ctx);
        rootContainer.addView(headerSection, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(112)));
        LinearLayout textSection = new LinearLayout(ctx);
        textSection.setOrientation(LinearLayout.VERTICAL);
        TextView heading = createHeading(ctx);
        TextView details = createDetails(ctx, contact);
        textSection.addView(heading, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 32, 16, 32, 0));
        textSection.addView(details, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 32, 12, 32, 16));
        rootContainer.addView(textSection);
        FrameLayout buttonArea = createButtonArea(ctx, parentFragment, contact);
        rootContainer.addView(buttonArea, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, AndroidUtilities.dp(68), Gravity.BOTTOM));
        ScrollView scrollContainer = new ScrollView(ctx);
        scrollContainer.addView(rootContainer);
        setCustomView(scrollContainer);
    }

    private LinearLayout createMainContainer(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        return container;
    }

    private FrameLayout createHeaderSection(Context ctx) {
        FrameLayout header = new FrameLayout(ctx);
        ImageView closeBtn = new ImageView(ctx);
        closeBtn.setImageResource(R.drawable.msg_close);
        closeBtn.setColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.SRC_IN);
        closeBtn.setBackground(Theme.AdaptiveRipple.filledCircle());
        int padding = AndroidUtilities.dp(6);
        closeBtn.setPadding(padding, padding, padding, padding);
        closeBtn.setOnClickListener(v -> dismiss());
        RLottieImageView animationView = new RLottieImageView(ctx);
        animationView.setAnimation(R.raw.shared_link_enter, 90, 90);
        animationView.playAnimation();
        FrameLayout iconBackground = new FrameLayout(ctx);
        iconBackground.setBackground(Theme.createCircleDrawable(AndroidUtilities.dp(90), Theme.getColor(Theme.key_featuredStickers_addButton)));
        iconBackground.addView(animationView, LayoutHelper.createFrame(90, 90, Gravity.CENTER));
        header.addView(closeBtn, LayoutHelper.createFrame(40, 40, Gravity.END | Gravity.TOP, 0, 0, 6, 0));
        header.addView(iconBackground, LayoutHelper.createFrame(90, 90, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM));
        return header;
    }

    private TextView createHeading(Context ctx) {
        TextView heading = new TextView(ctx);
        heading.setGravity(Gravity.CENTER);
        heading.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        heading.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        heading.setTypeface(AndroidUtilities.bold());
        heading.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.CallInviteViaLinkTitle)));
        return heading;
    }

    private TextView createDetails(Context ctx, TLRPC.User contact) {
        TextView details = new TextView(ctx);
        details.setGravity(Gravity.CENTER);
        details.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        details.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        String userName = ContactsController.formatName(contact);
        details.setText(AndroidUtilities.replaceTags(LocaleController.formatString(R.string.CallingRestricted, userName)));
        return details;
    }

    private FrameLayout createButtonArea(Context ctx, BaseFragment fragment, TLRPC.User contact) {
        FrameLayout buttonContainer = new FrameLayout(ctx);
        buttonContainer.setBackgroundColor(getThemedColor(Theme.key_dialogBackground));
        TextView inviteButton = new TextView(ctx);
        inviteButton.setPadding(AndroidUtilities.dp(32), 0, AndroidUtilities.dp(32), 0);
        inviteButton.setGravity(Gravity.CENTER);
        inviteButton.setTextColor(Theme.getColor(Theme.key_featuredStickers_buttonText));
        inviteButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        inviteButton.setTypeface(AndroidUtilities.bold());
        inviteButton.setBackground(Theme.AdaptiveRipple.filledRectByKey(Theme.key_featuredStickers_addButton, 8));
        inviteButton.setText(LocaleController.getString(R.string.SendInviteLink));
        inviteButton.setOnClickListener(v -> handleInviteAction(fragment, contact));
        buttonContainer.addView(inviteButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.CENTER_VERTICAL, 16, 0, 16, 0));
        return buttonContainer;
    }

    private void handleInviteAction(BaseFragment fragment, TLRPC.User contact) {
        AlertDialog loadingDialog = new AlertDialog(getContext(), AlertDialog.ALERT_TYPE_SPINNER);
        loadingDialog.showDelayed(300);
        TL_phone.createConferenceCall request = new TL_phone.createConferenceCall();
        request.random_id = Utilities.random.nextInt();
        fragment.getConnectionsManager().sendRequest(request, (response, error) -> {
            if (response instanceof TLRPC.Updates) {
                processCallResponse((TLRPC.Updates) response, fragment, contact, loadingDialog);
            } else {
                handleErrorResponse(error, loadingDialog);
            }
        });
    }

    private void processCallResponse(TLRPC.Updates updates, BaseFragment fragment, TLRPC.User contact, AlertDialog dialog) {
        MessagesController controller = MessagesController.getInstance(currentAccount);
        controller.putUsers(updates.users, false);
        controller.putChats(updates.chats, false);
        TLRPC.GroupCall callData = null;
        for (TLRPC.Update update : updates.updates) {
            if (update instanceof TLRPC.TL_updateGroupCall) {
                callData = ((TLRPC.TL_updateGroupCall) update).call;
                break;
            }
        }
        if (callData == null) {
            AndroidUtilities.runOnUIThread(dialog::dismiss);
            return;
        }
        String inviteLink = callData.invite_link;
        AndroidUtilities.runOnUIThread(() -> {
            sendInviteLink(fragment, contact, inviteLink);
            navigateToChat(fragment, contact);
            dialog.dismiss();
            dismiss();
        });
    }

    private void sendInviteLink(BaseFragment fragment, TLRPC.User contact, String link) {
        SendMessagesHelper.SendMessageParams params = new SendMessagesHelper.SendMessageParams();
        params.peer = contact.id;
        params.message = link;
        fragment.getSendMessagesHelper().sendMessage(params);
    }

    private void navigateToChat(BaseFragment fragment, TLRPC.User contact) {
        if (!(fragment instanceof ChatActivity)) {
            Bundle args = new Bundle();
            args.putLong("user_id", contact.id);
            fragment.presentFragment(new INavigationLayout.NavigationParams(new ChatActivity(args)).setRemoveLast(true));
        }
    }

    private void handleErrorResponse(TLRPC.TL_error error, AlertDialog dialog) {
        NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.needShowAlert, 6, error.text);
        AndroidUtilities.runOnUIThread(dialog::dismiss);
    }
}
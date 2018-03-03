/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package tk.wasdennnoch.androidn_ify.extracted.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import de.robv.android.xposed.XposedHelpers;
import tk.wasdennnoch.androidn_ify.R;
import tk.wasdennnoch.androidn_ify.XposedHook;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.NotificationsStuff;
import tk.wasdennnoch.androidn_ify.systemui.notifications.stack.NotificationStackScrollLayoutHooks;
import tk.wasdennnoch.androidn_ify.systemui.notifications.views.RemoteInputHelper;
import tk.wasdennnoch.androidn_ify.utils.Classes;
import tk.wasdennnoch.androidn_ify.utils.Fields;
import tk.wasdennnoch.androidn_ify.utils.ReflectionUtils;
import tk.wasdennnoch.androidn_ify.utils.ResourceUtils;
import tk.wasdennnoch.androidn_ify.utils.ViewUtils;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.callStaticMethod;

/**
 * Host for the remote input.
 */
public class RemoteInputView extends LinearLayout implements View.OnClickListener, TextWatcher {

    private static final String TAG = "RemoteInput";

    // A marker object that let's us easily find views of this class.
    public static final Object VIEW_TAG = new Object();

    public final Object mToken = new Object();

    private RemoteEditText mEditText;
    private ImageButton mSendButton;
    private ProgressBar mProgressBar;
    private PendingIntent mPendingIntent;
    private RemoteInput[] mRemoteInputs;
    private RemoteInput mRemoteInput;

    private RemoteInputController mController;

    private Object mEntry;

    private Object mScrollContainer;
    private View mScrollContainerChild;
    private boolean mRemoved;

    private int mRevealCx;
    private int mRevealCy;
    private int mRevealR;

    private boolean mResetting;

    private Object headsUpEntry;

    public RemoteInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mProgressBar = findViewById(R.id.remote_input_progress);

        mSendButton = findViewById(R.id.remote_input_send);
        mSendButton.setOnClickListener(this);

        mEditText = (RemoteEditText) getChildAt(0);
        mEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                final boolean isSoftImeEvent = event == null
                        && (actionId == EditorInfo.IME_ACTION_DONE
                        || actionId == EditorInfo.IME_ACTION_NEXT
                        || actionId == EditorInfo.IME_ACTION_SEND);
                final boolean isKeyboardEnterKey = event != null
                        && (boolean) callStaticMethod(KeyEvent.class, "isConfirmKey", event.getKeyCode())
                        && event.getAction() == KeyEvent.ACTION_DOWN;
                if (isSoftImeEvent || isKeyboardEnterKey) {
                    if (mEditText.length() > 0) {
                        sendRemoteInput();
                    }
                    // Consume action to prevent IME from closing.
                    return true;
                }
                return false;
            }
        });
        mEditText.addTextChangedListener(this);
        mEditText.setInnerFocusable(false);
        mEditText.mRemoteInputView = this;
    }

    private void sendRemoteInput() {
        Bundle results = new Bundle();
        results.putString(mRemoteInput.getResultKey(), mEditText.getText().toString());
        Intent fillInIntent = new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        RemoteInput.addResultsToIntent(mRemoteInputs, fillInIntent,
                results);

        mEditText.setEnabled(false);
        mSendButton.setVisibility(INVISIBLE);
        mProgressBar.setVisibility(VISIBLE);
        XposedHelpers.setAdditionalInstanceField(mEntry, "remoteInputText", mEditText.getText());
        mController.addSpinning((String) ReflectionUtils.get(Fields.SystemUI.NotificationDataEntry.key, mEntry), mToken);
        mController.removeRemoteInput(mEntry, mToken);
        mEditText.mShowImeOnInputConnection = false;
        mController.remoteInputSent(mEntry);
        try {
            mPendingIntent.send(getContext(), 0, fillInIntent);
        } catch (PendingIntent.CanceledException e) {
            XposedHook.logE(TAG, "Unable to send remote input result", e);
        }
    }

    public static RemoteInputView inflate(Context context, ViewGroup root,
                                          Object entry,
                                          RemoteInputController controller) {
        LayoutInflater inflater = LayoutInflater.from(context).cloneInContext(ResourceUtils.createOwnContext(context));
        inflater.setFactory2(new LayoutInflater.Factory2() {
            @Override
            public View onCreateView(String name, Context context, AttributeSet attrs) {
                if (name.equals(RemoteInputView.class.getCanonicalName())) {
                    return new RemoteInputView(context, attrs);
                } else if (name.equals(RemoteEditText.class.getCanonicalName())) {
                    return new RemoteEditText(context, attrs);
                } else return null;
            }

            @Override
            public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
                return onCreateView(name, context, attrs);
            }
        });
        RemoteInputView v = (RemoteInputView) inflater.inflate(ResourceUtils.getInstance(context).getLayout(R.layout.remote_input), root, false);
        v.mController = controller;
        v.mEntry = entry;
        v.setTag(VIEW_TAG);

        return v;
    }

    @Override
    public void onClick(View v) {
        if (v == mSendButton) {
            sendRemoteInput();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);

        // We never want for a touch to escape to an outer view or one we covered.
        return true;
    }

    public void onDefocus(boolean animate) {
        mController.removeRemoteInput(mEntry, mToken);
        XposedHelpers.setAdditionalInstanceField(mEntry, "remoteInputText", mEditText.getText());
        /*if (headsUpEntry != null)
            callMethod(headsUpEntry, "removeAsSoonAsPossible");
        RemoteInputHelper.setWindowManagerFocus(false);*/

        // During removal, we get reattached and lose focus. Not hiding in that
        // case to prevent flicker.
        if (!mRemoved) {
            if (animate && mRevealR > 0) {
                Animator reveal = ViewAnimationUtils.createCircularReveal(
                        this, mRevealCx, mRevealCy, mRevealR, 0);
                reveal.setInterpolator(Interpolators.FAST_OUT_LINEAR_IN);
                reveal.setDuration(StackStateAnimator.ANIMATION_DURATION_CLOSE_REMOTE_INPUT);
                reveal.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        setVisibility(INVISIBLE);
                    }
                });
                reveal.start();
            } else {
                setVisibility(INVISIBLE);
            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (NotificationsStuff.isChangingPosition(ReflectionUtils.get(Fields.SystemUI.NotificationDataEntry.row, mEntry))) {
            if (getVisibility() == VISIBLE && mEditText.isFocusable()) {
                mEditText.requestFocus();
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (NotificationsStuff.isChangingPosition(ReflectionUtils.get(Fields.SystemUI.NotificationDataEntry.row, mEntry)) || ViewUtils.isTemporarilyDetached(this)) {
            return;
        }
        mController.removeRemoteInput(mEntry, mToken);
        mController.removeSpinning((String) ReflectionUtils.get(Fields.SystemUI.NotificationDataEntry.key, mEntry), mToken);
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        mPendingIntent = pendingIntent;
    }

    public void setRemoteInput(RemoteInput[] remoteInputs, RemoteInput remoteInput) {
        mRemoteInputs = remoteInputs;
        mRemoteInput = remoteInput;
        mEditText.setHint(mRemoteInput.getLabel());
    }

    public void focusAnimated() {
        if (getVisibility() != VISIBLE) {
            Animator animator = ViewAnimationUtils.createCircularReveal(
                    this, mRevealCx, mRevealCy, 0, mRevealR);
            animator.setDuration(StackStateAnimator.ANIMATION_DURATION_STANDARD);
            animator.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
            animator.start();
        }
        focus();
    }

    public void focus() {
        setVisibility(VISIBLE);
        mController.addRemoteInput(mEntry, mToken);
        mEditText.setInnerFocusable(true);
        mEditText.mShowImeOnInputConnection = true;
        mEditText.setText((Spannable) XposedHelpers.getAdditionalInstanceField(mEntry, "remoteInputText"));
        mEditText.setSelection(mEditText.getText().length());
        mEditText.requestFocus();
        updateSendButton();
    }

    public void onNotificationUpdateOrReset() {
        boolean sending = mProgressBar.getVisibility() == VISIBLE;

        if (sending) {
            // Update came in after we sent the reply, time to reset.
            reset();
        }
    }

    private void reset() {
        mResetting = true;

        mEditText.getText().clear();
        mEditText.setEnabled(true);
        mSendButton.setVisibility(VISIBLE);
        mProgressBar.setVisibility(INVISIBLE);
        mController.removeSpinning((String) ReflectionUtils.get(Fields.SystemUI.NotificationDataEntry.key, mEntry), mToken);
        updateSendButton();
        onDefocus(false /*animate*/);

        mResetting = false;
    }

    @Override
    public boolean onRequestSendAccessibilityEvent(View child, AccessibilityEvent event) {
        if (mResetting && child == mEditText) {
            // Suppress text events if it happens during resetting. Ideally this would be
            // suppressed by the text view not being shown, but that doesn't work here because it
            // needs to stay visible for the animation.
            return false;
        }
        return super.onRequestSendAccessibilityEvent(child, event);
    }

    private void updateSendButton() {
        mSendButton.setEnabled(mEditText.getText().length() != 0);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
        updateSendButton();
    }

    public void close() {
        mEditText.defocusIfNeeded(false /* animated */);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            findScrollContainer();
            if (mScrollContainer != null) {
                callMethod(mScrollContainer, "removeLongPressCallback");
                NotificationStackScrollLayoutHooks stackScrollLayoutHooks = NotificationHooks.mStackScrollLayoutHooks;
                if (stackScrollLayoutHooks != null)
                    stackScrollLayoutHooks.requestDisallowDismiss();
            }
        }
        return super.onInterceptTouchEvent(ev);
    }

    public boolean requestScrollTo() {
        findScrollContainer();
        NotificationStackScrollLayoutHooks stackScrollLayoutHooks = NotificationHooks.mStackScrollLayoutHooks;
        if (stackScrollLayoutHooks != null) {
            stackScrollLayoutHooks.lockScrollTo(mScrollContainerChild);
        }
        return true;
    }

    private void findScrollContainer() {
        if (mScrollContainer == null) {
            mScrollContainerChild = null;
            ViewParent p = this;
            while (p != null) {
                if (mScrollContainerChild == null && Classes.SystemUI.ExpandableView.isInstance(p)) {
                    mScrollContainerChild = (View) p;
                }
                if (Classes.SystemUI.NotificationStackScrollLayout.isInstance(p.getParent())) {
                    mScrollContainer = p.getParent();
                    if (mScrollContainerChild == null) {
                        mScrollContainerChild = (View) p;
                    }
                    break;
                }
                p = p.getParent();
            }
        }
    }

    public boolean isActive() {
        return mEditText.isFocused() && mEditText.isEnabled();
    }

    public void stealFocusFrom(RemoteInputView other) {
        other.close();
        setPendingIntent(other.mPendingIntent);
        setRemoteInput(other.mRemoteInputs, other.mRemoteInput);
        setRevealParameters(other.mRevealCx, other.mRevealCy, other.mRevealR);
        focus();
    }

    /**
     * Tries to find an action in {@param actions} that matches the current pending intent
     * of this view and updates its state to that of the found action
     *
     * @return true if a matching action was found, false otherwise
     */
    public boolean updatePendingIntentFromActions(Notification.Action[] actions) {
        if (mPendingIntent == null || actions == null) {
            return false;
        }
        Intent current = (Intent) callMethod(mPendingIntent, "getIntent");
        if (current == null) {
            return false;
        }

        for (Notification.Action a : actions) {
            RemoteInput[] inputs = a.getRemoteInputs();
            if (a.actionIntent == null || inputs == null) {
                continue;
            }
            Intent candidate = (Intent) callMethod(a.actionIntent, "getIntent");
            if (!current.filterEquals(candidate)) {
                continue;
            }

            RemoteInput input = null;
            for (RemoteInput i : inputs) {
                if (i.getAllowFreeFormInput()) {
                    input = i;
                }
            }
            if (input == null) {
                continue;
            }
            setPendingIntent(a.actionIntent);
            setRemoteInput(inputs, input);
            return true;
        }
        return false;
    }

    public PendingIntent getPendingIntent() {
        return mPendingIntent;
    }

    public void setRemoved() {
        mRemoved = true;
    }

    public void setHeadsUpEntry(Object headsUpEntry) {
        this.headsUpEntry = headsUpEntry;
    }

    public void setRevealParameters(int cx, int cy, int r) {
        mRevealCx = cx;
        mRevealCy = cy;
        mRevealR = r;
    }

    @Override
    public void dispatchStartTemporaryDetach() {
        super.dispatchStartTemporaryDetach();
        // Detach the EditText temporarily such that it doesn't get onDetachedFromWindow and
        // won't lose IME focus.
        XposedHook.logI(TAG, "dispatchStartTemporaryDetach called!");
        detachViewFromParent(mEditText);
    }

    @Override
    public void dispatchFinishTemporaryDetach() {
        if (isAttachedToWindow()) {
            attachViewToParent(mEditText, 0, mEditText.getLayoutParams());
        } else {
            removeDetachedView(mEditText, false /* animate */);
        }
        XposedHook.logI(TAG, "dispatchFinishTemporaryDetach called!");
        super.dispatchFinishTemporaryDetach();
    }

    /**
     * An EditText that changes appearance based on whether it's focusable and becomes
     * un-focusable whenever the user navigates away from it or it becomes invisible.
     */
    public static class RemoteEditText extends EditText {
        private final Drawable mBackground;
        boolean mShowImeOnInputConnection;
        private RemoteInputView mRemoteInputView;

        public RemoteEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            mBackground = getBackground();
        }

        private void defocusIfNeeded(boolean animate) {
            if (mRemoteInputView != null && NotificationsStuff.isChangingPosition(ReflectionUtils.get(Fields.SystemUI.NotificationDataEntry.row, mRemoteInputView.mEntry))
                    || ViewUtils.isTemporarilyDetached(this)) {
                if (ViewUtils.isTemporarilyDetached(this)) {
                    // We might get reattached but then the other one of HUN / expanded might steal
                    // our focus, so we'll need to save our text here.
                    if (mRemoteInputView != null) {
                        XposedHelpers.setAdditionalInstanceField(mRemoteInputView.mEntry, "remoteInputText", getText());
                    }
                }
                return;
            }
            if (isFocusable() && isEnabled()) {
                setInnerFocusable(false);
                if (mRemoteInputView != null) {
                    mRemoteInputView.onDefocus(animate);
                }
                mShowImeOnInputConnection = false;
            }
        }

        @Override
        protected void onVisibilityChanged(View changedView, int visibility) {
            super.onVisibilityChanged(changedView, visibility);
            if (!isShown()) {
                defocusIfNeeded(false /* animate */);
            }
        }

        @Override
        protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            if (!focused) {
                defocusIfNeeded(true /* animate */);
            }
        }

        @Override
        public void getFocusedRect(Rect r) {
            super.getFocusedRect(r);
            r.top = getScrollY();
            r.bottom = getScrollY() + (getBottom() - getTop());
        }

        @Override
        public boolean requestRectangleOnScreen(Rect rectangle) {
            return mRemoteInputView.requestScrollTo();
        }

        @Override
        public boolean onKeyDown(int keyCode, KeyEvent event) {
            XposedHook.logI(TAG, "onKeyDown!");
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // Eat the DOWN event here to prevent any default behavior.
                return true;
            }
            return super.onKeyDown(keyCode, event);
        }

        @Override
        public boolean onKeyUp(int keyCode, KeyEvent event) {
            XposedHook.logI(TAG, "onKeyUp!");
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                defocusIfNeeded(true  /*animate*/ );
                return true;
            }
            return super.onKeyUp(keyCode, event);
        }

//        @Override
//        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
//            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
//                return true;
//            }
//            if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP) {
//                defocusIfNeeded(true  /*animate*/ );
//                final InputMethodManager imm = (InputMethodManager) callStaticMethod(InputMethodManager.class, "getInstance");
//                imm.hideSoftInputFromWindow(getWindowToken(), 0);
//                return true;
//            }
//            return super.onKeyPreIme(keyCode, event);
//        }

        @Override
        public boolean onCheckIsTextEditor() {
            // Stop being editable while we're being removed. During removal, we get reattached,
            // and editable views get their spellchecking state re-evaluated which is too costly
            // during the removal animation.
            boolean flyingOut = mRemoteInputView != null && mRemoteInputView.mRemoved;
            return !flyingOut && super.onCheckIsTextEditor();
        }

        @Override
        public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
            final InputConnection inputConnection = super.onCreateInputConnection(outAttrs);

            if (mShowImeOnInputConnection && inputConnection != null) {
                final InputMethodManager imm = (InputMethodManager) callStaticMethod(InputMethodManager.class, "getInstance");
                if (imm != null) {
                    // onCreateInputConnection is called by InputMethodManager in the middle of
                    // setting up the connection to the IME; wait with requesting the IME until that
                    // work has completed.
                    post(new Runnable() {
                        @Override
                        public void run() {
                            imm.viewClicked(RemoteEditText.this);
                            imm.showSoftInput(RemoteEditText.this, 0);
                        }
                    });
                }
            }

            return inputConnection;
        }

        @Override
        public void onCommitCompletion(CompletionInfo text) {
            clearComposingText();
            setText(text.getText());
            setSelection(getText().length());
        }

        void setInnerFocusable(boolean focusable) {
            setFocusableInTouchMode(focusable);
            setFocusable(focusable);
            setCursorVisible(focusable);

            if (focusable) {
                requestFocus();
                setBackground(mBackground);
            } else {
                setBackground(null);
            }
        }
    }
}
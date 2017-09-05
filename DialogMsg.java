package com.gogowan.petrochina.custom;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.gogowan.petrochina.R;

public class DialogMsg extends Dialog {

    public DialogMsg(Context context, int theme) {
        super(context, theme);
    }

    public DialogMsg(Context context) {
        super(context);
    }

    /**
     * Helper class for creating a custom dialog
     */
    public static class Builder {

        private Context context;
        private CharSequence message;
        private String positiveTVStr;
        private String negativeTVStr;
        private String titleTVStr;
        private int titleIVid = -1;
        private View contentView;
        private int msgColor = 0xff000000;

        private OnClickListener positiveTVClickListener,
                negativeTVClickListener;

        public Builder(Context context) {
            this.context = context;
        }

        /**
         * Set the Dialog message from String
         *
         * @param message
         * @return
         */
        public Builder setMessage(CharSequence message) {
            this.message = message;
            return this;
        }

        /**
         * Set the Dialog message from resource
         *
         * @param message
         * @return
         */
        public Builder setMessage(int message) {
            this.message = (CharSequence) context.getText(message);
            return this;
        }

        /**
         * Set a custom content view for the Dialog. If a message is set, the
         * contentView is not added to the Dialog...
         *
         * @param v
         * @return
         */
        public Builder setContentView(View v) {
            this.contentView = v;
            return this;
        }

        /**
         * Set the titleIV resource
         *
         * @param titleIVid
         * @return
         */
        public Builder setTitleIVResource(int titleIVid) {
            this.titleIVid = titleIVid;
            return this;
        }

        /**
         * Set the titleTV resource
         *
         * @param titleTVStr
         * @return
         */
        public Builder setTitleTV(int titleTVStr) {
            this.titleTVStr = (String) context
                    .getText(titleTVStr);
            return this;
        }

        /**
         * Set the titleTV resource
         *
         * @param titleTVStr
         * @return
         */
        public Builder setTitleTV(String titleTVStr) {
            this.titleTVStr = titleTVStr;
            return this;
        }

        /**
         * Set the positive button resource and it's listener
         *
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setPositiveButton(int positiveButtonText,
                                         OnClickListener listener) {
            this.positiveTVStr = (String) context
                    .getText(positiveButtonText);
            this.positiveTVClickListener = listener;
            return this;
        }

        /**
         * Set the positive button text and it's listener
         *
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setPositiveButton(String positiveButtonText,
                                         OnClickListener listener) {
            this.positiveTVStr = positiveButtonText;
            this.positiveTVClickListener = listener;
            return this;
        }

        /**
         * Set the negative button resource and it's listener
         *
         * @param negativeButtonText
         * @param listener
         * @return
         */
        public Builder setNegativeButton(int negativeButtonText,
                                         OnClickListener listener) {
            this.negativeTVStr = (String) context
                    .getText(negativeButtonText);
            this.negativeTVClickListener = listener;
            return this;
        }

        /**
         * Set the negative button text and it's listener
         *
         * @param negativeButtonText
         * @param listener
         * @return
         */
        public Builder setNegativeButton(String negativeButtonText,
                                         OnClickListener listener) {
            this.negativeTVStr = negativeButtonText;
            this.negativeTVClickListener = listener;
            return this;
        }

        /**
         * Set msg color
         *
         * @param color
         * @return
         */
        public Builder setMsgColor(int color) {
            this.msgColor = color;
            return this;
        }

        /**
         * Create the custom dialog
         */
        public DialogMsg create() {

            final DialogMsg dialog = new DialogMsg(context, R.style.resDialogStyle);
            dialog.addContentView(contentView, new LayoutParams(
                    LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager m = dialog.getWindow().getWindowManager();
            Display d = m.getDefaultDisplay();
            WindowManager.LayoutParams params = dialog.getWindow()
                    .getAttributes();
            params.width = (int) (d.getWidth() * 0.9);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
            // set the title
            if (!TextUtils.isEmpty(titleTVStr)) {
                ((TextView) contentView
                        .findViewById(R.id.dialog_msg_titleTV))
                        .setText(titleTVStr);
                if (titleIVid == -1) {
                    ((ImageView) contentView
                            .findViewById(R.id.dialog_msg_titleIV))
                            .setVisibility(View.GONE);
                } else {
                    // if no titleIVid just set the visibility to GONE
                    ((ImageView) contentView
                            .findViewById(R.id.dialog_msg_titleIV))
                            .setVisibility(View.VISIBLE);
                    contentView.findViewById(R.id.dialog_msg_titlelineV)
                            .setVisibility(View.GONE);
                    ((ImageView) contentView
                            .findViewById(R.id.dialog_msg_titleIV))
                            .setImageResource(titleIVid);
                }
            } else {
                // if no title just set the visibility to GONE
                contentView.findViewById(R.id.dialog_msg_titleLinear)
                        .setVisibility(View.GONE);
                contentView.findViewById(R.id.dialog_msg_titlelineV)
                        .setVisibility(View.GONE);
            }
            // set the positive button
            if (positiveTVStr != null) {
                ((TextView) contentView
                        .findViewById(R.id.dialog_msg_positiveTV))
                        .setText(positiveTVStr);
                if (positiveTVClickListener != null) {
                    ((TextView) contentView
                            .findViewById(R.id.dialog_msg_positiveTV))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {

                                    positiveTVClickListener.onClick(dialog,
                                            DialogInterface.BUTTON_POSITIVE);
                                    dialog.dismiss();
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                contentView.findViewById(R.id.dialog_msg_positiveTV)
                        .setVisibility(View.GONE);
                contentView.findViewById(R.id.dialog_msg_divlineV)
                        .setVisibility(View.GONE);
            }
            // set the cancel button
            if (negativeTVStr != null) {
                ((TextView) contentView
                        .findViewById(R.id.dialog_msg_negativeTV))
                        .setText(negativeTVStr);
                if (negativeTVClickListener != null) {
                    ((TextView) contentView
                            .findViewById(R.id.dialog_msg_negativeTV))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {

                                    negativeTVClickListener.onClick(dialog,
                                            DialogInterface.BUTTON_NEGATIVE);
                                    dialog.dismiss();
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                contentView.findViewById(R.id.dialog_msg_negativeTV)
                        .setVisibility(View.GONE);
                contentView.findViewById(R.id.dialog_msg_divlineV)
                        .setVisibility(View.GONE);
            }
            // set the title message
            if (message != null) {
                ((TextView) contentView
                        .findViewById(R.id.dialog_msg_contentTV))
                        .setText(message);
                ((TextView) contentView
                        .findViewById(R.id.dialog_msg_contentTV))
                        .setTextColor(msgColor);
            }
            dialog.setContentView(contentView);
            return dialog;
        }
    }
}

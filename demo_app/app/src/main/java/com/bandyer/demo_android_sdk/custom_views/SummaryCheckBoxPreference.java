package com.bandyer.demo_android_sdk.custom_views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.preference.CheckBoxPreference;
import androidx.preference.PreferenceViewHolder;

import com.bandyer.demo_android_sdk.R;
import com.bandyer.demo_android_sdk.utils.Utils;

public class SummaryCheckBoxPreference extends CheckBoxPreference implements SummaryPreference {

    private TextView secondarySummaryTextView;
    private String secondarySummaryText;

    public SummaryCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SummaryCheckBoxPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public SummaryCheckBoxPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SummaryCheckBoxPreference(Context context) {
        super(context);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        TextView title = (TextView) holder.findViewById(android.R.id.title);
        if (secondarySummaryTextView != null) return;
        View secondarySummary = LayoutInflater.from(getContext()).inflate(R.layout.preference_summary_layout, null);
        secondarySummaryTextView = secondarySummary.findViewById(android.R.id.summary);
        secondarySummaryTextView.setText(secondarySummaryText);
        RelativeLayout summaryParent = (RelativeLayout) ((LinearLayout) holder.itemView).getChildAt(1);
        RelativeLayout.LayoutParams secondarySummaryLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        secondarySummaryLayoutParams.addRule(RelativeLayout.BELOW, android.R.id.title);
        secondarySummaryLayoutParams.bottomMargin = Utils.dpToPx(getContext(), 16);
        secondarySummaryLayoutParams.topMargin = Utils.dpToPx(getContext(), 16);
        summaryParent.addView(secondarySummary, secondarySummaryLayoutParams);

        LinearLayout checkBoxLayout = (LinearLayout) ((LinearLayout) holder.itemView).getChildAt(2);
        checkBoxLayout.setPadding(0, Utils.dpToPx(getContext(), 11), 0, 0);
        checkBoxLayout.setGravity(Gravity.CENTER_HORIZONTAL);
    }

    @Override
    public void setSecondarySummmary(String secondarySummmary) {
        this.secondarySummaryText = secondarySummmary;
        notifyChanged();
    }
}
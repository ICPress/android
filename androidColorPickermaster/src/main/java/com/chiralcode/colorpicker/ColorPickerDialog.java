package com.chiralcode.colorpicker;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Arrays;

public class ColorPickerDialog extends AlertDialog {

    private ColorPicker colorPickerView;
    private final OnColorSelectedListener onColorSelectedListener;
    private LinearLayout noColorLinearLayout;
    private float[] defaultColor = new float[] { 0f, 0f, 1f };
    private Context context;

    // Color resources from main project
    private int primaryColor;
    private int secondaryColor;
    private int secondaryTextColor;
    private int primaryTextColor;
    private int surfaceColor;
    private int secondaryColor20;
    private int borderColor;

    public ColorPickerDialog(Context context, int initialColor,
                             OnColorSelectedListener onColorSelectedListener,
                             ColorPickerTheme theme) {
        super(context);
        this.context = context;
        this.onColorSelectedListener = onColorSelectedListener;

        // Store theme colors
        this.primaryColor = theme.primaryColor;
        this.secondaryColor = theme.secondaryColor;
        this.secondaryTextColor = theme.secondaryTextColor;
        this.primaryTextColor = theme.primaryTextColor;
        this.surfaceColor = theme.surfaceColor;
        this.secondaryColor20 = theme.secondaryColor20;
        this.borderColor = theme.borderColor;

        buildDialog(initialColor);

        // IMPORTANT: Add the buttons here
        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), onClickListener);
        setButton(BUTTON_NEGATIVE, context.getString(android.R.string.cancel), onClickListener);
    }

    private void buildDialog(int initialColor) {
        RelativeLayout relativeLayout = new RelativeLayout(context);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);

        Integer colorpickerId = 2359329;
        colorPickerView = new ColorPicker(context, onColorClickSelected);
        if (initialColor != 0) colorPickerView.setColor(initialColor);
        colorPickerView.setId(colorpickerId);

        relativeLayout.addView(colorPickerView, layoutParams);

        // No color option with theme colors
        noColorLinearLayout = new LinearLayout(context);
        LinearLayout basicView = new LinearLayout(context);
        LinearLayout.LayoutParams basicLayoutParams = new LinearLayout.LayoutParams(50, 50);
        basicView.setLayoutParams(basicLayoutParams);
        basicView.setBackgroundDrawable(context.getResources().getDrawable(R.drawable.border_outline));

        ImageView img = new ImageView(context);
        LinearLayout.LayoutParams imgLayoutParams = new LinearLayout.LayoutParams(80, 80);
        ViewGroup.MarginLayoutParams imgLayoutParamsMargin = new ViewGroup.MarginLayoutParams(imgLayoutParams);
        imgLayoutParamsMargin.topMargin = -15;
        imgLayoutParamsMargin.leftMargin = -15;
        img.setLayoutParams(imgLayoutParamsMargin);
        img.setImageDrawable(context.getResources().getDrawable(R.drawable.non_color));
        basicView.addView(img);

        noColorLinearLayout.addView(basicView);

        TextView noColorTv = new TextView(context);
        noColorTv.setText("No color");
        noColorTv.setTextColor(primaryTextColor);
        noColorTv.setTextSize(14);

        LinearLayout.LayoutParams tvlLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ViewGroup.MarginLayoutParams tvLayoutParams = new ViewGroup.MarginLayoutParams(tvlLayoutParams);
        tvLayoutParams.leftMargin = 20;
        tvLayoutParams.topMargin = 8;
        tvLayoutParams.bottomMargin = 8;
        noColorTv.setLayoutParams(tvLayoutParams);
        noColorLinearLayout.addView(noColorTv);

        noColorLinearLayout.setOrientation(LinearLayout.HORIZONTAL);

        RelativeLayout.LayoutParams linearLayoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        linearLayoutParams.addRule(RelativeLayout.BELOW, colorpickerId);
        linearLayoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, colorpickerId);
        linearLayoutParams.topMargin = 30;
        noColorLinearLayout.setLayoutParams(linearLayoutParams);
        noColorLinearLayout.setOnClickListener(onNoColorClickSelected);
        noColorLinearLayout.setPadding(16, 12, 16, 12);
        relativeLayout.addView(noColorLinearLayout);

        setView(relativeLayout);
    }

    @Override
    public void show() {
        super.show();

        // Style buttons with theme colors
        Button positiveButton = getButton(BUTTON_POSITIVE);
        Button negativeButton = getButton(BUTTON_NEGATIVE);

        if (positiveButton != null) {
            positiveButton.setTextColor(secondaryColor);
            positiveButton.setTextSize(14);
            positiveButton.setAllCaps(false);
            positiveButton.setTypeface(positiveButton.getTypeface(), android.graphics.Typeface.BOLD);
            // Make sure button is visible
            positiveButton.setVisibility(View.VISIBLE);
        }

        if (negativeButton != null) {
            negativeButton.setTextColor(secondaryTextColor);
            negativeButton.setTextSize(14);
            negativeButton.setAllCaps(false);
            // Make sure button is visible
            negativeButton.setVisibility(View.VISIBLE);
        }

        // Style dialog title
        TextView titleView = (TextView) findViewById(android.R.id.title);
        if (titleView != null) {
            titleView.setTextColor(primaryColor);
            titleView.setTextSize(18);
            titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);
            titleView.setPadding(24, 24, 24, 8);
        }
    }

    private View.OnClickListener onColorClickSelected = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            colorPickerView.resetArrowPointerSize();
            colorPickerView.invalidate();

            // Reset no color background
            noColorLinearLayout.setBackgroundDrawable(null);

            // Reset no color text color
            for (int i = 0; i < noColorLinearLayout.getChildCount(); i++) {
                View child = noColorLinearLayout.getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(primaryTextColor);
                }
            }
        }
    };

    private View.OnClickListener onNoColorClickSelected = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            colorPickerView.arrowPointerSize = 0;
            colorPickerView.colorHSV = defaultColor.clone();
            colorPickerView.invalidate();

            // Create a drawable for selected state programmatically
            android.graphics.drawable.GradientDrawable selectedDrawable = new android.graphics.drawable.GradientDrawable();
            selectedDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            selectedDrawable.setColor(secondaryColor20);
            selectedDrawable.setStroke(2, secondaryColor);
            selectedDrawable.setCornerRadius(8);
            noColorLinearLayout.setBackgroundDrawable(selectedDrawable);

            // Update text color for selected state
            for (int i = 0; i < ((LinearLayout) view).getChildCount(); i++) {
                View child = ((LinearLayout) view).getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setTextColor(secondaryColor);
                }
            }
        }
    };

    private OnClickListener onClickListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case BUTTON_POSITIVE:
                    if (Arrays.equals(colorPickerView.colorHSV, defaultColor)) {
                        if (onColorSelectedListener != null) {
                            onColorSelectedListener.onNoColorSelected();
                        }
                    } else {
                        int selectedColor = colorPickerView.getColor();
                        if (onColorSelectedListener != null) {
                            onColorSelectedListener.onColorSelected(selectedColor);
                        }
                    }
                    dismiss();
                    break;
                case BUTTON_NEGATIVE:
                    dismiss();
                    break;
            }
        }
    };

    public interface OnColorSelectedListener {
        void onNoColorSelected();
        void onColorSelected(int color);
    }

    // Theme container class
    public static class ColorPickerTheme {
        public final int primaryColor;
        public final int secondaryColor;
        public final int secondaryTextColor;
        public final int primaryTextColor;
        public final int surfaceColor;
        public final int secondaryColor20;
        public final int borderColor;

        public ColorPickerTheme(int primaryColor, int secondaryColor, int secondaryTextColor,
                                int primaryTextColor, int surfaceColor, int secondaryColor20,
                                int borderColor) {
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.secondaryTextColor = secondaryTextColor;
            this.primaryTextColor = primaryTextColor;
            this.surfaceColor = surfaceColor;
            this.secondaryColor20 = secondaryColor20;
            this.borderColor = borderColor;
        }
    }
}
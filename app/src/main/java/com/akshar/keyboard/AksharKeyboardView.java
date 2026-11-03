package com.akshar.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.Locale;

final class AksharKeyboardView extends LinearLayout {
    private enum KeyboardPage {
        LETTERS,
        NUMBERS,
        MATHS
    }

    interface KeyAction {
        void run(String value);
    }

    private static final int GREEN = Color.rgb(49, 92, 72);
    private static final int INK = Color.rgb(32, 37, 31);
    private static final int MUTED = Color.rgb(119, 123, 112);
    private static final int KEY_BACKGROUND = Color.rgb(255, 254, 250);

    private final Context context;
    private final KeyAction typeKey;
    private final KeyAction controlShortcut;
    private final Runnable backspace;
    private final Runnable enter;
    private boolean shifted;
    private boolean control;
    private boolean superscript;
    private boolean subscript;
    private KeyboardPage page = KeyboardPage.LETTERS;

    AksharKeyboardView(
            Context context, KeyAction typeKey, KeyAction controlShortcut,
            Runnable backspace, Runnable enter) {
        super(context);
        this.context = context;
        this.typeKey = typeKey;
        this.controlShortcut = controlShortcut;
        this.backspace = backspace;
        this.enter = enter;
        setOrientation(VERTICAL);
        setPadding(dp(7), 0, dp(7), 0);
        setBackgroundColor(Color.rgb(244, 241, 233));
        buildKeyboard();
    }

    private void buildKeyboard() {
        if (page == KeyboardPage.NUMBERS) {
            LinearLayout symbols = row();
            String[] symbolsList = AksharImeService.numberSymbols();
            addCharacterKeys(symbols, symbolsList, 0, symbolsList.length);
            for (String symbol : new String[] {"\u20B9", "%"}) {
                addKey(symbols, symbol, null, 1f, () -> typeCharacter(symbol));
            }
            addView(symbols, rowParams(42));

            LinearLayout numbers = row();
            for (String number : new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"}) {
                addKey(numbers, number, null, 1f, () -> typeCharacter(number));
            }
            addView(numbers, rowParams(42));
            LinearLayout punctuation = row();
            String[] punctuationKeys = AksharImeService.numberPagePunctuation();
            addCharacterKeys(punctuation, punctuationKeys, 0, punctuationKeys.length);
            addView(punctuation, rowParams(42));
        } else if (page == KeyboardPage.MATHS) {
            String[] mathSymbols = AksharImeService.mathSymbols();
            LinearLayout firstMathRow = row();
            addCharacterKeys(firstMathRow, mathSymbols, 0, 9);
            addView(firstMathRow, rowParams(42));
            LinearLayout secondMathRow = row();
            addCharacterKeys(secondMathRow, mathSymbols, 9, 17);
            addView(secondMathRow, rowParams(42));
            LinearLayout thirdMathRow = row();
            addCharacterKeys(thirdMathRow, mathSymbols, 17, mathSymbols.length);
            addModeKeys(thirdMathRow);
            addView(thirdMathRow, rowParams(42));
        } else {
            String[] keys = AksharImeService.letterKeyboardKeys();
            int[][] rowRanges = {
                    {0, 9},
                    {9, 17},
                    {17, keys.length}
            };
            for (int rowIndex = 0; rowIndex < rowRanges.length; rowIndex++) {
                int[] range = rowRanges[rowIndex];
                LinearLayout row = row();
                if (rowIndex == rowRanges.length - 1) {
                    addShiftKey(row);
                }
                addCharacterKeys(row, keys, range[0], range[1]);
                if (rowIndex == rowRanges.length - 1) {
                    addKey(row, ",", null, 1f, () -> typeCharacter(","));
                }
                addView(row, rowParams(42));
            }
        }

        LinearLayout controls = row();
        KeyboardPage shortcutPage = pageShortcutDestination();
        Button pageButton = addControlKey(controls, pageShortcutLabel(), null, 1f, () -> {
            page = shortcutPage;
            rebuildKeyboard();
        });
        pageButton.setContentDescription("Show " + pageDescription(shortcutPage) + " keyboard");
        if (page == KeyboardPage.LETTERS) {
            Button controlButton = addControlKey(controls, "Ctrl", null, 1f, () -> {
                control = !control;
                rebuildKeyboard();
            });
            controlButton.setContentDescription(control ? "Control, on" : "Control, off");
            if (control) {
                controlButton.setBackground(activeBackground());
                controlButton.setTextColor(Color.WHITE);
            }
            addControlKey(controls, ".", null, 1f, () -> typeCharacter("."));
        }
        if (page == KeyboardPage.NUMBERS) {
            Button mathsPageButton = addControlKey(controls, "∑", null, 1f, () -> {
                page = KeyboardPage.MATHS;
                rebuildKeyboard();
            });
            mathsPageButton.setContentDescription("Show maths keyboard");
        }
        if (page == KeyboardPage.MATHS) {
            Button numberPageButton = addControlKey(controls, "123", null, 1f, () -> {
                page = KeyboardPage.NUMBERS;
                rebuildKeyboard();
            });
            numberPageButton.setContentDescription("Show number and symbol keyboard");
        }
        addControlKey(controls, "space", null, 2f, () -> typeKey.run(" "));
        addControlKey(controls, "⌫", null, 1f, backspace);
        addControlKey(controls, "↵", null, 1f, enter);
        addView(controls, controlRowParams(42));
    }

    private void addShiftKey(LinearLayout row) {
        Button shift = addKey(row, "⇧", null, 1f, () -> {
            shifted = !shifted;
            rebuildKeyboard();
        });
        shift.setContentDescription(shifted ? "Shift, on" : "Shift, off");
        if (shifted) {
            shift.setBackground(activeBackground());
            shift.setTextColor(Color.WHITE);
        }
    }

    private void addEmptyRow() {
        addView(row(), rowParams(42));
    }

    private void addCharacterKeys(LinearLayout row, String[] keys, int start, int end) {
        for (int index = start; index < end; index++) {
            String roman = keys[index];
            String key = shifted ? roman.toUpperCase(Locale.ROOT) : roman;
            addKey(row, key, null, 1f, () -> typeCharacter(key));
        }
    }

    private void addModeKeys(LinearLayout row) {
        addModeKey(row, "xⁿ", "Superscript", superscript, () -> {
            superscript = !superscript;
            subscript = false;
            rebuildKeyboard();
        });
        addModeKey(row, "xₙ", "Subscript", subscript, () -> {
            subscript = !subscript;
            superscript = false;
            rebuildKeyboard();
        });
    }

    private String pageShortcutLabel() {
        switch (page) {
            case NUMBERS:
            case MATHS: return "ABC";
            default: return "123";
        }
    }

    private KeyboardPage pageShortcutDestination() {
        return page == KeyboardPage.LETTERS ? KeyboardPage.NUMBERS : KeyboardPage.LETTERS;
    }

    private String pageDescription(KeyboardPage destination) {
        switch (destination) {
            case LETTERS: return "letter";
            case NUMBERS: return "number and symbol";
            default: return "maths";
        }
    }

    private void typeCharacter(String key) {
        if (control) {
            control = false;
            String shortcut = key.toLowerCase(Locale.ROOT);
            if ("pcybui".contains(shortcut)) {
                controlShortcut.run(shortcut);
            } else {
                typeKey.run(AksharImeService.convertCharacter(key, superscript, subscript));
            }
            rebuildKeyboard();
            return;
        }
        typeKey.run(AksharImeService.convertCharacter(key, superscript, subscript));
    }

    private void addModeKey(
            LinearLayout row, String label, String description, boolean active, Runnable action) {
        Button button = addKey(row, label, null, 1f, action);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
        button.setContentDescription(description + (active ? ", on" : ", off"));
        if (active) {
            button.setBackground(activeBackground());
            button.setTextColor(Color.WHITE);
        }
    }

    private void rebuildKeyboard() {
        removeAllViews();
        buildKeyboard();
    }

    private LinearLayout row() {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        return row;
    }

    private Button addKey(
            LinearLayout row, String label, String secondary, float weight, Runnable action) {
        return addKey(row, label, secondary, Math.round(keyWidthDp() * weight), 0f, action);
        }

        private Button addControlKey(
            LinearLayout row, String label, String secondary, float weight, Runnable action) {
        return addKey(row, label, secondary, 0, weight, action);
        }

        private Button addKey(
            LinearLayout row, String label, String secondary, int widthDp, float weight,
            Runnable action) {
        Button button = new Button(context);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(1), 0, dp(1), 0);
        button.setMinWidth(0);
        button.setMinHeight(0);
        button.setTextColor(INK);
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, secondary == null ? 15 : 13);
        button.setBackground(background());
        button.setStateListAnimator(null);
        button.setContentDescription(secondary == null ? label : label + ", " + secondary);
        if (secondary != null) {
            SpannableString text = new SpannableString(label + "\n" + secondary);
            text.setSpan(new RelativeSizeSpan(0.78f), label.length() + 1, text.length(), 0);
            text.setSpan(new android.text.style.ForegroundColorSpan(MUTED),
                    label.length() + 1, text.length(), 0);
            button.setText(text);
        } else {
            button.setText(label);
        }
        button.setOnClickListener(view -> action.run());
        LayoutParams params = new LayoutParams(dp(widthDp), dp(40), weight);
        params.setMargins(dp(2), dp(1), dp(2), dp(1));
        row.addView(button, params);
        return button;
    }

    private LayoutParams controlRowParams(int height) {
        int firstRowKeyCount = page == KeyboardPage.NUMBERS ? 10 : 9;
        int firstRowWidthDp = firstRowKeyCount * (keyWidthDp() + 4);
        LayoutParams params = new LayoutParams(dp(firstRowWidthDp), dp(height));
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private int keyWidthDp() {
        float density = getResources().getDisplayMetrics().density;
        int screenWidthDp = (int) (getResources().getDisplayMetrics().widthPixels / density);
        int maximumKeysPerRow = 10;
        int keyMarginsPerKeyDp = 4;
        int availableKeyWidthDp = screenWidthDp - 14
                - keyMarginsPerKeyDp * maximumKeysPerRow;
        int equalKeyWidthDp = availableKeyWidthDp / maximumKeysPerRow;
        return Math.max(1, Math.min(28, equalKeyWidthDp));
    }

    private GradientDrawable background() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(KEY_BACKGROUND);
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(1), Color.rgb(230, 226, 216));
        return drawable;
    }

    private GradientDrawable activeBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(GREEN);
        drawable.setCornerRadius(dp(7));
        drawable.setStroke(dp(1), GREEN);
        return drawable;
    }

    private LayoutParams rowParams(int height) {
        return new LayoutParams(LayoutParams.MATCH_PARENT, dp(height));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}

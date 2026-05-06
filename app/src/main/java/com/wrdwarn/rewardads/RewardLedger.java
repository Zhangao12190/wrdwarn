package com.wrdwarn.rewardads;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class RewardLedger {
    private static final String PREFS_NAME = "reward_ledger";
    private static final String KEY_BALANCE = "coin_balance";
    private static final String KEY_HISTORY = "coin_history";
    private static final int MAX_HISTORY_ITEMS = 20;

    private final SharedPreferences preferences;

    RewardLedger(Context context) {
        preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    int getBalance() {
        return preferences.getInt(KEY_BALANCE, 0);
    }

    int addReward(String source, int amount) {
        int balance = getBalance() + amount;
        preferences.edit()
                .putInt(KEY_BALANCE, balance)
                .putString(KEY_HISTORY, prependHistory(source, amount, balance))
                .apply();
        return balance;
    }

    List<String> getHistory() {
        String rawHistory = preferences.getString(KEY_HISTORY, "");
        List<String> items = new ArrayList<>();
        if (rawHistory == null || rawHistory.isEmpty()) {
            return items;
        }

        String[] lines = rawHistory.split("\\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                items.add(line);
            }
        }
        return items;
    }

    String getHistoryText() {
        List<String> items = getHistory();
        if (items.isEmpty()) {
            return "还没有金币流水。完成一次激励广告后会显示记录。";
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < items.size(); index++) {
            if (index > 0) {
                builder.append('\n');
            }
            builder.append(items.get(index));
        }
        return builder.toString();
    }

    private String prependHistory(String source, int amount, int balance) {
        List<String> existing = getHistory();
        DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.getDefault());
        String newLine = "+" + amount + " 金币 - " + source + " - 余额 " + balance + " - " + format.format(new Date());

        StringBuilder builder = new StringBuilder(newLine);
        int limit = Math.min(existing.size(), MAX_HISTORY_ITEMS - 1);
        for (int index = 0; index < limit; index++) {
            builder.append('\n').append(existing.get(index));
        }
        return builder.toString();
    }
}

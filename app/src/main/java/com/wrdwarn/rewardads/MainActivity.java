package com.wrdwarn.rewardads;

import android.app.AlertDialog;
import android.app.Activity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.text.NumberFormat;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final int COINS_PER_COMPLETED_AD = 10;
    private static final int MIN_WITHDRAW_COINS = 1000;
    private static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private RewardLedger ledger;
    private RewardedAd rewardedAd;
    private TextView balanceText;
    private TextView statusText;
    private TextView historyText;
    private ProgressBar adLoadingProgress;
    private Button watchAdButton;
    private Button withdrawButton;
    private boolean isLoadingAd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ledger = new RewardLedger(this);
        buildLayout();
        refreshUi("正在初始化广告 SDK...");

        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        int padding = dp(20);
        root.setPadding(padding, padding, padding, padding);
        root.setBackgroundColor(getColor(R.color.screen_background));

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextAppearance(android.R.style.TextAppearance_Material_Large);
        title.setTextColor(getColor(R.color.primary_text));
        title.setGravity(Gravity.CENTER);
        root.addView(title, fullWidthWrapContent());

        TextView policyNotice = new TextView(this);
        policyNotice.setText(R.string.policy_notice);
        policyNotice.setTextColor(getColor(R.color.secondary_text));
        policyNotice.setPadding(0, dp(12), 0, dp(12));
        root.addView(policyNotice, fullWidthWrapContent());

        balanceText = new TextView(this);
        balanceText.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        balanceText.setTextColor(getColor(R.color.primary_text));
        balanceText.setGravity(Gravity.CENTER);
        root.addView(balanceText, fullWidthWrapContent());

        adLoadingProgress = new ProgressBar(this);
        adLoadingProgress.setIndeterminate(true);
        root.addView(adLoadingProgress, wrapContentCentered());

        statusText = new TextView(this);
        statusText.setTextColor(getColor(R.color.secondary_text));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusText, fullWidthWrapContent());

        watchAdButton = new Button(this);
        watchAdButton.setText(R.string.watch_rewarded_ad);
        watchAdButton.setOnClickListener(view -> showRewardedAd());
        root.addView(watchAdButton, fullWidthWrapContent());

        withdrawButton = new Button(this);
        withdrawButton.setText(R.string.request_withdrawal);
        withdrawButton.setOnClickListener(view -> showWithdrawalDialog());
        root.addView(withdrawButton, fullWidthWrapContent());

        Button complianceButton = new Button(this);
        complianceButton.setText(R.string.compliance_title);
        complianceButton.setOnClickListener(view -> showComplianceDialog());
        root.addView(complianceButton, fullWidthWrapContent());

        TextView historyTitle = new TextView(this);
        historyTitle.setText(R.string.ledger_title);
        historyTitle.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        historyTitle.setTextColor(getColor(R.color.primary_text));
        historyTitle.setPadding(0, dp(16), 0, dp(8));
        root.addView(historyTitle, fullWidthWrapContent());

        historyText = new TextView(this);
        historyText.setTextColor(getColor(R.color.secondary_text));
        historyText.setMovementMethod(new ScrollingMovementMethod());
        root.addView(historyText, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        setContentView(root);
    }

    private void loadRewardedAd() {
        if (isLoadingAd || rewardedAd != null) {
            return;
        }

        isLoadingAd = true;
        setAdLoadingState(true, "正在加载激励广告...");
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, TEST_REWARDED_AD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                isLoadingAd = false;
                rewardedAd.setFullScreenContentCallback(createFullScreenContentCallback());
                setAdLoadingState(false, "广告已准备好，完整观看后可获得金币。");
            }

            @Override
            public void onAdFailedToLoad(LoadAdError loadAdError) {
                rewardedAd = null;
                isLoadingAd = false;
                setAdLoadingState(false, "广告加载失败：" + loadAdError.getMessage());
            }
        });
    }

    private FullScreenContentCallback createFullScreenContentCallback() {
        return new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                rewardedAd = null;
                refreshUi("广告已关闭，正在准备下一条广告。");
                loadRewardedAd();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                rewardedAd = null;
                refreshUi("广告展示失败：" + adError.getMessage());
                loadRewardedAd();
            }

            @Override
            public void onAdShowedFullScreenContent() {
                statusText.setText(R.string.ad_showing);
            }
        };
    }

    private void showRewardedAd() {
        if (rewardedAd == null) {
            Toast.makeText(this, R.string.ad_not_ready, Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            return;
        }

        OnUserEarnedRewardListener rewardListener = rewardItem -> grantReward(rewardItem);
        rewardedAd.show(this, rewardListener);
    }

    private void grantReward(RewardItem rewardItem) {
        int amount = COINS_PER_COMPLETED_AD;
        String type = rewardItem.getType();
        if (rewardItem.getAmount() > 0) {
            type = type + " x" + rewardItem.getAmount();
        }

        ledger.addReward("完成激励广告：" + type, amount);
        refreshUi("已获得 " + amount + " 金币。");
    }

    private void showWithdrawalDialog() {
        int balance = ledger.getBalance();
        String message;
        if (balance < MIN_WITHDRAW_COINS) {
            message = getString(R.string.withdrawal_not_enough, MIN_WITHDRAW_COINS, balance);
        } else {
            message = getString(R.string.withdrawal_ready, balance);
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.request_withdrawal)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showComplianceDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.compliance_title)
                .setMessage(R.string.compliance_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void setAdLoadingState(boolean loading, String status) {
        adLoadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        watchAdButton.setEnabled(!loading && rewardedAd != null);
        refreshUi(status);
    }

    private void refreshUi(String status) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.CHINA);
        balanceText.setText(getString(R.string.balance_format, numberFormat.format(ledger.getBalance())));
        historyText.setText(ledger.getHistoryText());
        withdrawButton.setEnabled(ledger.getBalance() >= MIN_WITHDRAW_COINS);
        statusText.setText(status);
        if (!isLoadingAd) {
            watchAdButton.setEnabled(rewardedAd != null);
            adLoadingProgress.setVisibility(View.GONE);
        }
    }

    private LinearLayout.LayoutParams fullWidthWrapContent() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, dp(6), 0, dp(6));
        return params;
    }

    private LinearLayout.LayoutParams wrapContentCentered() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL;
        return params;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

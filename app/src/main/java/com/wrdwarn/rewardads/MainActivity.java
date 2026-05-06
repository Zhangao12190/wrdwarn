package com.wrdwarn.rewardads;

import android.app.AlertDialog;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
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
    private static final int SPIKY_SNAKE_BASE_REWARD = 8;
    private static final int SPIKY_SNAKE_REWARD_PER_COIN = 6;
    private static final int MIN_WITHDRAW_COINS = 1000;
    private static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private RewardLedger ledger;
    private RewardedAd rewardedAd;
    private TextView balanceText;
    private TextView pendingRewardText;
    private TextView statusText;
    private TextView accountText;
    private ProgressBar adLoadingProgress;
    private Button claimRewardButton;
    private boolean isLoadingAd;
    private String latestStatus = "正在初始化广告 SDK...";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ledger = new RewardLedger(this);
        buildSimpleGameLayout();
        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());
    }

    private void buildSimpleGameLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(screenBackground());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(22));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout hero = createCard();
        hero.setBackground(heroBackground());
        TextView tag = createSmallCaps("CAMPUS PLAY · 简化版");
        tag.setTextColor(Color.rgb(255, 221, 107));
        hero.addView(tag, fullWidthWrapContent());

        TextView title = createTitle("点开游戏\n赢金币奖励");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        hero.addView(title, fullWidthWrapContent());

        TextView subtitle = createBodyText("只保留核心流程：玩尖刺蛇，游戏结束后点击观看广告领取奖励。");
        subtitle.setTextColor(Color.rgb(224, 241, 236));
        subtitle.setGravity(Gravity.CENTER);
        hero.addView(subtitle, fullWidthWrapContent());
        root.addView(hero, cardLayoutParams());

        LinearLayout balanceCard = createCard();
        balanceCard.addView(createSectionTitle("我的奖励"), fullWidthWrapContent());
        LinearLayout stats = createHorizontalRow();
        balanceText = createMetricText("");
        pendingRewardText = createMetricText("");
        pendingRewardText.setTextColor(getColor(R.color.coin_gold));
        stats.addView(createMetricBox("账户金币", balanceText), weightedParams(1f, 0));
        stats.addView(createMetricBox("待领取", pendingRewardText), weightedParams(1f, dp(10)));
        balanceCard.addView(stats, fullWidthWrapContent());
        root.addView(balanceCard, cardLayoutParams());

        LinearLayout gameCard = createCard();
        gameCard.addView(createSectionTitle("尖刺蛇"), fullWidthWrapContent());
        gameCard.addView(createBodyText("滑动控制蛇吃金币。撞墙、撞尖刺或撞到自己后，分数会变成待领取金币。"), fullWidthWrapContent());

        Button startGameButton = new Button(this);
        startGameButton.setText("开始游戏");
        stylePrimaryButton(startGameButton);
        startGameButton.setOnClickListener(view -> showSpikySnakeGame());
        gameCard.addView(startGameButton, fullWidthWrapContent());

        adLoadingProgress = new ProgressBar(this);
        adLoadingProgress.setIndeterminate(true);
        gameCard.addView(adLoadingProgress, wrapContentCentered());

        statusText = createCaption("");
        statusText.setGravity(Gravity.CENTER);
        gameCard.addView(statusText, fullWidthWrapContent());

        claimRewardButton = new Button(this);
        claimRewardButton.setText("观看广告领取奖励");
        stylePrimaryButton(claimRewardButton);
        claimRewardButton.setOnClickListener(view -> showRewardedAd());
        gameCard.addView(claimRewardButton, fullWidthWrapContent());
        root.addView(gameCard, cardLayoutParams());

        LinearLayout footerCard = createCard();
        footerCard.addView(createSectionTitle("账户"), fullWidthWrapContent());
        accountText = createCaption("");
        footerCard.addView(accountText, fullWidthWrapContent());

        Button withdrawButton = new Button(this);
        withdrawButton.setText(R.string.request_withdrawal);
        styleSecondaryButton(withdrawButton);
        withdrawButton.setOnClickListener(view -> showWithdrawalDialog());
        footerCard.addView(withdrawButton, fullWidthWrapContent());

        TextView note = createCaption("提示：广告不会自动弹出。游戏结束后需要你主动点击确认，才会展示激励广告并领取奖励。");
        note.setGravity(Gravity.CENTER);
        footerCard.addView(note, fullWidthWrapContent());
        root.addView(footerCard, cardLayoutParams());

        setContentView(scrollView);
        refreshUi(latestStatus);
    }

    private void loadRewardedAd() {
        if (isLoadingAd || rewardedAd != null) {
            return;
        }

        isLoadingAd = true;
        setAdLoadingState(true, "正在加载广告...");
        RewardedAd.load(this, TEST_REWARDED_AD_UNIT_ID, new AdRequest.Builder().build(), new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(RewardedAd ad) {
                rewardedAd = ad;
                isLoadingAd = false;
                rewardedAd.setFullScreenContentCallback(createFullScreenContentCallback());
                setAdLoadingState(false, "广告已准备好。先玩游戏，再领取奖励。");
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
        if (ledger.getPendingReward() <= 0) {
            Toast.makeText(this, R.string.no_pending_reward, Toast.LENGTH_SHORT).show();
            return;
        }
        if (rewardedAd == null) {
            Toast.makeText(this, R.string.ad_not_ready, Toast.LENGTH_SHORT).show();
            loadRewardedAd();
            return;
        }

        OnUserEarnedRewardListener rewardListener = rewardItem -> grantReward(rewardItem);
        rewardedAd.show(this, rewardListener);
    }

    private void grantReward(RewardItem rewardItem) {
        int amount = ledger.getPendingReward();
        if (amount <= 0) {
            refreshUi("广告已完成，但当前没有待领取奖励。");
            return;
        }

        String source = ledger.getPendingSource();
        ledger.addReward(source + "，广告确认：" + rewardItem.getType(), amount);
        ledger.clearPendingReward();
        refreshUi("奖励已入账：" + amount + " 金币。");
    }

    private void showSpikySnakeGame() {
        if (ledger.getPendingReward() > 0) {
            Toast.makeText(this, R.string.pending_reward_exists, Toast.LENGTH_SHORT).show();
            return;
        }

        SpikySnakeGameView gameView = new SpikySnakeGameView(this);
        AlertDialog gameDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.spiky_snake_title)
                .setView(gameView)
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> gameView.stop())
                .create();
        gameView.setGameListener(new SpikySnakeGameView.GameListener() {
            @Override
            public void onScoreChanged(int score) {
                gameDialog.setTitle(getString(R.string.spiky_snake_score_title, score));
            }

            @Override
            public void onGameOver(int score) {
                gameDialog.dismiss();
                finishSpikySnakeGame(score);
            }
        });
        gameDialog.setOnShowListener(dialog -> gameView.start());
        gameDialog.setOnDismissListener(dialog -> gameView.stop());
        gameDialog.show();
    }

    private void finishSpikySnakeGame(int score) {
        int reward = SPIKY_SNAKE_BASE_REWARD + (score * SPIKY_SNAKE_REWARD_PER_COIN);
        ledger.setPendingReward("小游戏：尖刺蛇，得分 " + score, reward);
        refreshUi("游戏结束，获得 " + reward + " 待领取金币。");
        showAdPromptAfterGame(score, reward);
    }

    private void showAdPromptAfterGame(int score, int reward) {
        new AlertDialog.Builder(this)
                .setTitle("游戏结束")
                .setMessage("本局得分 " + score + "，获得 " + reward + " 待领取金币。\n\n是否现在观看广告领取？")
                .setPositiveButton("观看广告领取", (dialog, which) -> showRewardedAd())
                .setNegativeButton("稍后领取", null)
                .show();
    }

    private void showWithdrawalDialog() {
        int balance = ledger.getBalance();
        if (balance < MIN_WITHDRAW_COINS) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.request_withdrawal)
                    .setMessage(getString(R.string.withdrawal_not_enough, MIN_WITHDRAW_COINS, balance))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(8), dp(4), dp(8), 0);

        TextView warning = createCaption("测试版只创建“审核中”的提现申请，不会从客户端自动打款。");
        form.addView(warning, fullWidthWrapContent());

        EditText nameInput = new EditText(this);
        nameInput.setHint("支付宝实名姓名");
        nameInput.setSingleLine(true);
        form.addView(nameInput, fullWidthWrapContent());

        EditText accountInput = new EditText(this);
        accountInput.setHint("支付宝账号");
        accountInput.setSingleLine(true);
        accountInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(accountInput, fullWidthWrapContent());

        EditText amountInput = new EditText(this);
        amountInput.setHint("提现金币数量");
        amountInput.setSingleLine(true);
        amountInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        amountInput.setText(String.valueOf(MIN_WITHDRAW_COINS));
        form.addView(amountInput, fullWidthWrapContent());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.request_withdrawal)
                .setView(form)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("提交申请", null)
                .create();
        dialog.setOnShowListener(dialogInterface -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String realName = nameInput.getText().toString().trim();
            String alipayAccount = accountInput.getText().toString().trim();
            String amountText = amountInput.getText().toString().trim();
            if (realName.isEmpty() || alipayAccount.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(this, "请填写姓名、支付宝账号和提现金币数量。", Toast.LENGTH_SHORT).show();
                return;
            }

            int amount;
            try {
                amount = Integer.parseInt(amountText);
            } catch (NumberFormatException exception) {
                Toast.makeText(this, "提现金币数量格式不正确。", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount < MIN_WITHDRAW_COINS) {
                Toast.makeText(this, "最低提现 " + MIN_WITHDRAW_COINS + " 金币。", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amount > ledger.getBalance()) {
                Toast.makeText(this, "账户金币不足，无法提交提现申请。", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ledger.submitWithdrawal(realName, alipayAccount, amount)) {
                refreshUi("支付宝提现申请已提交。");
                Toast.makeText(this, "提现申请已提交，等待人工/后端审核。", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            }
        }));
        dialog.show();
    }

    private void setAdLoadingState(boolean loading, String status) {
        isLoadingAd = loading;
        latestStatus = status;
        if (adLoadingProgress == null || claimRewardButton == null) {
            return;
        }

        adLoadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        claimRewardButton.setEnabled(!loading && rewardedAd != null && ledger.getPendingReward() > 0);
        refreshUi(status);
    }

    private void refreshUi(String status) {
        latestStatus = status;
        if (balanceText == null || pendingRewardText == null || statusText == null) {
            return;
        }

        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.CHINA);
        balanceText.setText(numberFormat.format(ledger.getBalance()));
        pendingRewardText.setText(numberFormat.format(ledger.getPendingReward()) + " 金币");
        accountText.setText("体验账户：" + ledger.getAccountId());
        statusText.setText(status);
        if (!isLoadingAd) {
            claimRewardButton.setEnabled(rewardedAd != null && ledger.getPendingReward() > 0);
            adLoadingProgress.setVisibility(View.GONE);
        }
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(cardBackground());
        return card;
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private LinearLayout createMetricBox(String label, TextView metricText) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(10), dp(12), dp(10), dp(12));
        box.setBackground(insetBackground(Color.WHITE, Color.rgb(221, 234, 229), dp(18)));

        TextView labelView = createCaption(label);
        labelView.setGravity(Gravity.CENTER);
        box.addView(labelView, fullWidthWrapContent());
        box.addView(metricText, fullWidthWrapContent());
        return box;
    }

    private TextView createSmallCaps(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.12f);
        return view;
    }

    private TextView createTitle(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(24);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(getColor(R.color.primary_text));
        return view;
    }

    private TextView createSectionTitle(String text) {
        TextView view = createTitle(text);
        view.setTextSize(20);
        return view;
    }

    private TextView createMetricText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(22);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(getColor(R.color.primary_text));
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(6), 0, dp(6));
        return view;
    }

    private TextView createBodyText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setLineSpacing(0, 1.15f);
        view.setTextColor(getColor(R.color.secondary_text));
        return view;
    }

    private TextView createCaption(String text) {
        TextView view = createBodyText(text);
        view.setTextSize(13);
        return view;
    }

    private void stylePrimaryButton(Button button) {
        button.setTextColor(Color.WHITE);
        button.setTextSize(16);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{getColor(R.color.brand_green), getColor(R.color.brand_green_dark)}
        );
        background.setCornerRadius(dp(18));
        button.setBackground(background);
        button.setAllCaps(false);
        button.setMinHeight(dp(50));
    }

    private void styleSecondaryButton(Button button) {
        button.setTextColor(getColor(R.color.brand_green_dark));
        button.setTextSize(15);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(232, 245, 241));
        background.setStroke(dp(1), Color.rgb(181, 219, 209));
        background.setCornerRadius(dp(18));
        button.setBackground(background);
        button.setAllCaps(false);
        button.setMinHeight(dp(46));
    }

    private GradientDrawable screenBackground() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.rgb(232, 246, 241), Color.rgb(248, 251, 249)}
        );
    }

    private GradientDrawable heroBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(6, 49, 61), Color.rgb(14, 124, 102)}
        );
        drawable.setCornerRadius(dp(24));
        return drawable;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(getColor(R.color.surface));
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), Color.rgb(221, 234, 229));
        return drawable;
    }

    private GradientDrawable insetBackground(int fillColor, int strokeColor, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(radius);
        drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private LinearLayout.LayoutParams weightedParams(float weight, int leftMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                weight
        );
        params.setMargins(leftMargin, dp(6), 0, dp(6));
        return params;
    }

    private LinearLayout.LayoutParams cardLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(14));
        return params;
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

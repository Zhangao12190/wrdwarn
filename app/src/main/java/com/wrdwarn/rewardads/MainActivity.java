package com.wrdwarn.rewardads;

import android.app.AlertDialog;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
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
import java.util.Random;

public class MainActivity extends Activity {
    private static final int GUESS_GAME_WIN_REWARD = 30;
    private static final int GUESS_GAME_PLAY_REWARD = 5;
    private static final int SPIKY_SNAKE_BASE_REWARD = 8;
    private static final int SPIKY_SNAKE_REWARD_PER_COIN = 6;
    private static final int MIN_WITHDRAW_COINS = 1000;
    private static final String TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private RewardLedger ledger;
    private RewardedAd rewardedAd;
    private TextView accountText;
    private TextView balanceText;
    private TextView pendingRewardText;
    private TextView gameStatusText;
    private TextView statusText;
    private TextView historyText;
    private ProgressBar adLoadingProgress;
    private Button claimRewardButton;
    private Button withdrawButton;
    private boolean isLoadingAd;
    private String latestStatus = "正在初始化广告 SDK...";
    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ledger = new RewardLedger(this);
        buildLaunchLayout();
        MobileAds.initialize(this, initializationStatus -> loadRewardedAd());
    }

    private void buildLaunchLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);
        root.setBackground(startupBackground());

        TextView label = new TextView(this);
        label.setText("CAMPUS PLAY HUB");
        label.setTextColor(Color.rgb(255, 221, 107));
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setGravity(Gravity.CENTER);
        root.addView(label, fullWidthWrapContent());

        TextView title = new TextView(this);
        title.setText("课余小游戏\n外快灵感站");
        title.setTextColor(Color.WHITE);
        title.setTextSize(34);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLineSpacing(0, 1.05f);
        root.addView(title, fullWidthWrapContent());

        TextView slogan = new TextView(this);
        slogan.setText("给大学生和在家兼职的人准备：先玩轻量小游戏，完成挑战后观看激励广告领取金币。");
        slogan.setTextColor(Color.rgb(224, 241, 236));
        slogan.setTextSize(16);
        slogan.setGravity(Gravity.CENTER);
        slogan.setPadding(0, dp(16), 0, dp(12));
        root.addView(slogan, fullWidthWrapContent());

        TextView note = new TextView(this);
        note.setText("当前是商业模型测试版：金币和提现为演示流程，正式上线前需要后端审核、风控和广告平台合规。");
        note.setTextColor(Color.rgb(178, 211, 202));
        note.setTextSize(13);
        note.setGravity(Gravity.CENTER);
        root.addView(note, fullWidthWrapContent());

        Button enterButton = new Button(this);
        enterButton.setText("进入游戏中心");
        stylePrimaryButton(enterButton);
        enterButton.setOnClickListener(view -> {
            buildGameCenterLayout();
            refreshUi(latestStatus);
        });
        root.addView(enterButton, fullWidthWrapContent());

        setContentView(root);
    }

    private void buildGameCenterLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(screenBackground());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(18));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout heroCard = createCard();
        heroCard.setBackground(heroBackground());
        TextView eyebrow = createSmallCaps("STUDENT SIDE QUESTS");
        eyebrow.setTextColor(Color.rgb(255, 221, 107));
        heroCard.addView(eyebrow, fullWidthWrapContent());

        TextView heroTitle = createTitle("课余游戏中心");
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setTextSize(28);
        heroCard.addView(heroTitle, fullWidthWrapContent());

        TextView heroSubtitle = createBodyText("在宿舍、通勤或家里，用碎片时间玩小游戏、攒金币、验证你的兼职副业模型。");
        heroSubtitle.setTextColor(Color.rgb(224, 241, 236));
        heroCard.addView(heroSubtitle, fullWidthWrapContent());
        content.addView(heroCard, cardLayoutParams());

        LinearLayout accountCard = createCard();
        accountCard.addView(createSectionTitle("个人中心"), fullWidthWrapContent());
        accountText = createBodyText("");
        balanceText = createMetricText("");
        pendingRewardText = createMetricText("");
        pendingRewardText.setTextColor(getColor(R.color.coin_gold));
        accountCard.addView(accountText, fullWidthWrapContent());
        accountCard.addView(balanceText, fullWidthWrapContent());
        accountCard.addView(pendingRewardText, fullWidthWrapContent());
        TextView accountTip = createCaption("适合大学生/居家兼职人群的体验账户。真实版本会接入手机号登录、实名审核和提现风控。");
        accountCard.addView(accountTip, fullWidthWrapContent());
        content.addView(accountCard, cardLayoutParams());

        LinearLayout gameCard = createCard();
        gameCard.addView(createSectionTitle("热门小游戏"), fullWidthWrapContent());
        TextView gameName = createTitle("尖刺蛇挑战");
        gameName.setTextSize(22);
        gameCard.addView(gameName, fullWidthWrapContent());
        gameStatusText = createBodyText(getString(R.string.spiky_snake_intro));
        gameCard.addView(gameStatusText, fullWidthWrapContent());

        Button spikySnakeButton = new Button(this);
        spikySnakeButton.setText(R.string.play_spiky_snake);
        stylePrimaryButton(spikySnakeButton);
        spikySnakeButton.setOnClickListener(view -> showSpikySnakeGame());
        gameCard.addView(spikySnakeButton, fullWidthWrapContent());

        Button guessGameButton = new Button(this);
        guessGameButton.setText(R.string.play_guess_game);
        styleSecondaryButton(guessGameButton);
        guessGameButton.setOnClickListener(view -> showGuessGameDialog());
        gameCard.addView(guessGameButton, fullWidthWrapContent());

        TextView moreGamesText = createCaption("后续扩展：转盘、刮刮卡、答题、拼图、排行榜、每日任务。");
        gameCard.addView(moreGamesText, fullWidthWrapContent());
        content.addView(gameCard, cardLayoutParams());

        LinearLayout rewardCard = createCard();
        rewardCard.addView(createSectionTitle("奖励领取"), fullWidthWrapContent());
        TextView rewardCopy = createBodyText("完成小游戏后，奖励会先进入“待领取”。你需要主动观看一次激励广告，广告 SDK 确认后才会入账。");
        rewardCard.addView(rewardCopy, fullWidthWrapContent());
        adLoadingProgress = new ProgressBar(this);
        adLoadingProgress.setIndeterminate(true);
        rewardCard.addView(adLoadingProgress, wrapContentCentered());
        statusText = createCaption("");
        statusText.setGravity(Gravity.CENTER);
        rewardCard.addView(statusText, fullWidthWrapContent());
        claimRewardButton = new Button(this);
        claimRewardButton.setText(R.string.claim_game_reward);
        stylePrimaryButton(claimRewardButton);
        claimRewardButton.setOnClickListener(view -> showRewardedAd());
        rewardCard.addView(claimRewardButton, fullWidthWrapContent());
        content.addView(rewardCard, cardLayoutParams());

        LinearLayout profileCard = createCard();
        profileCard.addView(createSectionTitle("我的账户"), fullWidthWrapContent());
        withdrawButton = new Button(this);
        withdrawButton.setText(R.string.request_withdrawal);
        styleSecondaryButton(withdrawButton);
        withdrawButton.setOnClickListener(view -> showWithdrawalDialog());
        profileCard.addView(withdrawButton, fullWidthWrapContent());

        Button complianceButton = new Button(this);
        complianceButton.setText(R.string.compliance_title);
        styleSecondaryButton(complianceButton);
        complianceButton.setOnClickListener(view -> showComplianceDialog());
        profileCard.addView(complianceButton, fullWidthWrapContent());

        TextView historyTitle = createSectionTitle("金币流水");
        historyTitle.setTextSize(18);
        profileCard.addView(historyTitle, fullWidthWrapContent());
        historyText = createCaption("");
        historyText.setMovementMethod(new ScrollingMovementMethod());
        historyText.setMinLines(4);
        historyText.setMaxLines(8);
        profileCard.addView(historyText, fullWidthWrapContent());
        content.addView(profileCard, cardLayoutParams());

        setContentView(scrollView);
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
                setAdLoadingState(false, "广告已准备好，完成小游戏后可观看广告领取奖励。");
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
                if (statusText != null) {
                    statusText.setText(R.string.ad_showing);
                }
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
            refreshUi("广告已完成，但当前没有待领取的游戏奖励。");
            return;
        }

        String source = ledger.getPendingSource();
        ledger.addReward(source + "，广告确认：" + rewardItem.getType(), amount);
        ledger.clearPendingReward();
        refreshUi("已把小游戏奖励 " + amount + " 金币计入账户。");
    }

    private void showGuessGameDialog() {
        if (ledger.getPendingReward() > 0) {
            Toast.makeText(this, R.string.pending_reward_exists, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] choices = {"1", "2", "3"};
        int target = random.nextInt(3) + 1;
        new AlertDialog.Builder(this)
                .setTitle(R.string.guess_game_title)
                .setMessage(R.string.guess_game_prompt)
                .setItems(choices, (dialog, which) -> finishGuessGame(which + 1, target))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void finishGuessGame(int guess, int target) {
        int reward;
        String message;
        if (guess == target) {
            reward = GUESS_GAME_WIN_REWARD;
            message = getString(R.string.guess_game_win, target, reward);
        } else {
            reward = GUESS_GAME_PLAY_REWARD;
            message = getString(R.string.guess_game_miss, target, reward);
        }

        ledger.setPendingReward("小游戏：幸运猜数字", reward);
        if (gameStatusText != null) {
            gameStatusText.setText(message);
        }
        refreshUi("小游戏已完成，请观看激励广告领取 " + reward + " 金币。");
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
        String message = getString(R.string.spiky_snake_finished, score, reward);
        if (gameStatusText != null) {
            gameStatusText.setText(message);
        }
        refreshUi("尖刺蛇已结束，请观看激励广告领取 " + reward + " 金币。");
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
        if (balanceText == null || pendingRewardText == null || historyText == null) {
            return;
        }

        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.CHINA);
        accountText.setText("大学生体验账户\n" + ledger.getAccountId());
        balanceText.setText("账户金币\n" + numberFormat.format(ledger.getBalance()));
        pendingRewardText.setText("待领取任务奖励\n" + numberFormat.format(ledger.getPendingReward()) + " 金币");
        historyText.setText(ledger.getHistoryText());
        withdrawButton.setEnabled(ledger.getBalance() >= MIN_WITHDRAW_COINS);
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
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(20);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(getColor(R.color.primary_text));
        return view;
    }

    private TextView createMetricText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(20);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(getColor(R.color.primary_text));
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(8), 0, dp(8));
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
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{getColor(R.color.brand_green), getColor(R.color.brand_green_dark)}
        );
        background.setCornerRadius(dp(18));
        button.setBackground(background);
        button.setAllCaps(false);
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
    }

    private GradientDrawable startupBackground() {
        return new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(4, 40, 48), Color.rgb(10, 99, 89), Color.rgb(7, 91, 74)}
        );
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

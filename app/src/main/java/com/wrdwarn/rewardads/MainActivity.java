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
    private final Random random = new Random();

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

        accountText = new TextView(this);
        accountText.setTextColor(getColor(R.color.secondary_text));
        accountText.setGravity(Gravity.CENTER);
        root.addView(accountText, fullWidthWrapContent());

        balanceText = new TextView(this);
        balanceText.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        balanceText.setTextColor(getColor(R.color.primary_text));
        balanceText.setGravity(Gravity.CENTER);
        root.addView(balanceText, fullWidthWrapContent());

        pendingRewardText = new TextView(this);
        pendingRewardText.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        pendingRewardText.setTextColor(getColor(R.color.coin_gold));
        pendingRewardText.setGravity(Gravity.CENTER);
        root.addView(pendingRewardText, fullWidthWrapContent());

        TextView gamesTitle = new TextView(this);
        gamesTitle.setText(R.string.game_center_title);
        gamesTitle.setTextAppearance(android.R.style.TextAppearance_Material_Medium);
        gamesTitle.setTextColor(getColor(R.color.primary_text));
        gamesTitle.setPadding(0, dp(12), 0, dp(4));
        root.addView(gamesTitle, fullWidthWrapContent());

        gameStatusText = new TextView(this);
        gameStatusText.setText(R.string.spiky_snake_intro);
        gameStatusText.setTextColor(getColor(R.color.secondary_text));
        gameStatusText.setGravity(Gravity.CENTER);
        root.addView(gameStatusText, fullWidthWrapContent());

        Button spikySnakeButton = new Button(this);
        spikySnakeButton.setText(R.string.play_spiky_snake);
        spikySnakeButton.setOnClickListener(view -> showSpikySnakeGame());
        root.addView(spikySnakeButton, fullWidthWrapContent());

        Button guessGameButton = new Button(this);
        guessGameButton.setText(R.string.play_guess_game);
        guessGameButton.setOnClickListener(view -> showGuessGameDialog());
        root.addView(guessGameButton, fullWidthWrapContent());

        TextView moreGamesText = new TextView(this);
        moreGamesText.setText(R.string.more_games_coming);
        moreGamesText.setTextColor(getColor(R.color.secondary_text));
        moreGamesText.setGravity(Gravity.CENTER);
        root.addView(moreGamesText, fullWidthWrapContent());

        adLoadingProgress = new ProgressBar(this);
        adLoadingProgress.setIndeterminate(true);
        root.addView(adLoadingProgress, wrapContentCentered());

        statusText = new TextView(this);
        statusText.setTextColor(getColor(R.color.secondary_text));
        statusText.setGravity(Gravity.CENTER);
        statusText.setPadding(0, dp(8), 0, dp(8));
        root.addView(statusText, fullWidthWrapContent());

        claimRewardButton = new Button(this);
        claimRewardButton.setText(R.string.claim_game_reward);
        claimRewardButton.setOnClickListener(view -> showRewardedAd());
        root.addView(claimRewardButton, fullWidthWrapContent());

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
        gameStatusText.setText(message);
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
        gameStatusText.setText(message);
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
        adLoadingProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        claimRewardButton.setEnabled(!loading && rewardedAd != null && ledger.getPendingReward() > 0);
        refreshUi(status);
    }

    private void refreshUi(String status) {
        NumberFormat numberFormat = NumberFormat.getIntegerInstance(Locale.CHINA);
        accountText.setText(getString(R.string.account_format, ledger.getAccountId()));
        balanceText.setText(getString(R.string.balance_format, numberFormat.format(ledger.getBalance())));
        pendingRewardText.setText(getString(
                R.string.pending_reward_format,
                numberFormat.format(ledger.getPendingReward())
        ));
        historyText.setText(ledger.getHistoryText());
        withdrawButton.setEnabled(ledger.getBalance() >= MIN_WITHDRAW_COINS);
        statusText.setText(status);
        if (!isLoadingAd) {
            claimRewardButton.setEnabled(rewardedAd != null && ledger.getPendingReward() > 0);
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

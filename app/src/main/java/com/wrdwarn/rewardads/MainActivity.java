package com.wrdwarn.rewardads;

import android.app.AlertDialog;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.method.ScrollingMovementMethod;
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
    private TextView withdrawalText;
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
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(startupBackground());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        int padding = dp(24);
        root.setPadding(padding, padding, padding, padding);
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        TextView label = createPill("CAMPUS PLAY HUB · BETA");
        label.setGravity(Gravity.CENTER);
        root.addView(label, wrapContentCentered());

        TextView title = new TextView(this);
        title.setText("课余玩一局\n把碎片时间变金币");
        title.setTextColor(Color.WHITE);
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setLineSpacing(0, 1.05f);
        root.addView(title, fullWidthWrapContent());

        TextView slogan = new TextView(this);
        slogan.setText("面向大学生、居家兼职和想尝试外快副业的人：先玩轻量小游戏，再主动观看激励广告领取奖励。");
        slogan.setTextColor(Color.rgb(224, 241, 236));
        slogan.setTextSize(16);
        slogan.setGravity(Gravity.CENTER);
        slogan.setLineSpacing(0, 1.18f);
        slogan.setPadding(0, dp(14), 0, dp(12));
        root.addView(slogan, fullWidthWrapContent());

        LinearLayout launchChips = createHorizontalRow();
        launchChips.addView(createLaunchChip("小游戏驱动"), weightedParams(1f, 0));
        launchChips.addView(createLaunchChip("广告确认入账"), weightedParams(1f, dp(8)));
        root.addView(launchChips, fullWidthWrapContent());

        LinearLayout routeCard = createGlassCard();
        routeCard.addView(createLaunchStep("01", "玩尖刺蛇", "滑动控制蛇吃金币，避开尖刺和墙壁。"), fullWidthWrapContent());
        routeCard.addView(createLaunchStep("02", "生成待领取金币", "小游戏结束后，分数会换成待领取奖励。"), fullWidthWrapContent());
        routeCard.addView(createLaunchStep("03", "看激励广告入账", "广告 SDK 确认后，金币进入体验账户。"), fullWidthWrapContent());
        root.addView(routeCard, cardLayoutParams());

        TextView note = new TextView(this);
        note.setText("当前是商业模型测试版：金币和提现为演示流程，不承诺真实收益。正式上线前需要后端审核、风控和广告平台合规。");
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

        setContentView(scrollView);
    }

    private void buildGameCenterLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackground(screenBackground());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(18), dp(16), dp(22));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        LinearLayout topBar = createHorizontalRow();
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = createLogoBadge();
        topBar.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(48)));
        LinearLayout topCopy = new LinearLayout(this);
        topCopy.setOrientation(LinearLayout.VERTICAL);
        topCopy.setPadding(dp(12), 0, 0, 0);
        TextView appTitle = createTitle("Campus Play");
        appTitle.setTextSize(22);
        topCopy.addView(appTitle, fullWidthWrapContent());
        topCopy.addView(createCaption("小游戏任务中心 · 测试版"), fullWidthWrapContent());
        topBar.addView(topCopy, weightedParams(1f, 0));
        TextView levelBadge = createStatusChip("LV.1 体验账户", Color.rgb(255, 250, 232), Color.rgb(245, 222, 142));
        topBar.addView(levelBadge, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        content.addView(topBar, cardLayoutParams());

        LinearLayout heroCard = createCard();
        heroCard.setBackground(heroBackground());
        TextView eyebrow = createSmallCaps("STUDENT SIDE QUESTS");
        eyebrow.setTextColor(Color.rgb(255, 221, 107));
        heroCard.addView(eyebrow, fullWidthWrapContent());

        TextView heroTitle = createTitle("校园轻兼职游戏中心");
        heroTitle.setTextColor(Color.WHITE);
        heroTitle.setTextSize(27);
        heroCard.addView(heroTitle, fullWidthWrapContent());

        TextView heroSubtitle = createBodyText("在宿舍、通勤或家里，用碎片时间玩小游戏、攒金币，验证“游戏 + 激励广告”的副业模型。");
        heroSubtitle.setTextColor(Color.rgb(224, 241, 236));
        heroCard.addView(heroSubtitle, fullWidthWrapContent());

        LinearLayout heroStats = createHorizontalRow();
        heroStats.addView(createHeroStat("2", "已接入游戏"), weightedParams(1f, 0));
        heroStats.addView(createHeroStat("广告", "领取奖励"), weightedParams(1f, dp(8)));
        heroStats.addView(createHeroStat("Beta", "模型测试"), weightedParams(1f, dp(8)));
        heroCard.addView(heroStats, fullWidthWrapContent());
        content.addView(heroCard, cardLayoutParams());

        LinearLayout missionCard = createCard();
        missionCard.addView(createSectionHeader("今日任务路线", "完成后再领取奖励"), fullWidthWrapContent());
        missionCard.addView(createTaskRow("1", "玩一局尖刺蛇", "先获得待领取金币"), fullWidthWrapContent());
        missionCard.addView(createTaskRow("2", "观看一次激励广告", "SDK 确认后入账"), fullWidthWrapContent());
        missionCard.addView(createTaskRow("3", "查看个人中心流水", "确认金币记录"), fullWidthWrapContent());
        content.addView(missionCard, cardLayoutParams());

        LinearLayout rewardCard = createCard();
        rewardCard.setBackground(accentCardBackground());
        rewardCard.addView(createSectionHeader("今日奖励面板", "先玩后领，状态更清楚"), fullWidthWrapContent());
        TextView rewardCopy = createBodyText("完成小游戏后，奖励会先进入“待领取”。观看激励广告成功后，金币才会进入账户。");
        rewardCard.addView(rewardCopy, fullWidthWrapContent());
        LinearLayout rewardStats = createHorizontalRow();
        balanceText = createMetricText("");
        pendingRewardText = createMetricText("");
        pendingRewardText.setTextColor(getColor(R.color.coin_gold));
        rewardStats.addView(createMetricBox("账户金币", balanceText), weightedParams(1f, 0));
        rewardStats.addView(createMetricBox("待领取", pendingRewardText), weightedParams(1f, dp(10)));
        rewardCard.addView(rewardStats, fullWidthWrapContent());
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

        LinearLayout gameCard = createCard();
        gameCard.addView(createSectionHeader("热门小游戏", "主推尖刺蛇，后续可扩展更多游戏"), fullWidthWrapContent());
        gameStatusText = createBodyText(getString(R.string.spiky_snake_intro));
        gameCard.addView(gameStatusText, fullWidthWrapContent());

        LinearLayout snakePanel = createInsetPanel(Color.rgb(246, 251, 249), Color.rgb(206, 231, 224));
        LinearLayout snakeHeader = createHorizontalRow();
        snakeHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView snakeBadge = createStatusChip("主推 · 手速挑战", Color.rgb(230, 247, 241), Color.rgb(177, 221, 208));
        snakeHeader.addView(snakeBadge, weightedParams(1f, 0));
        TextView snakeReward = createStatusChip("基础 8 金币", Color.rgb(255, 250, 232), Color.rgb(245, 222, 142));
        snakeHeader.addView(snakeReward, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        snakePanel.addView(snakeHeader, fullWidthWrapContent());
        TextView snakeTitle = createTitle("尖刺蛇 Spiky Snake");
        snakeTitle.setTextSize(22);
        snakePanel.addView(snakeTitle, fullWidthWrapContent());
        snakePanel.addView(createBodyText("滑动控制方向，吃金币冲分，避开尖刺、墙壁和自己的身体。"), fullWidthWrapContent());
        snakePanel.addView(createSnakePreview(), fullWidthWrapContent());
        Button spikySnakeButton = new Button(this);
        spikySnakeButton.setText(R.string.play_spiky_snake);
        stylePrimaryButton(spikySnakeButton);
        spikySnakeButton.setOnClickListener(view -> showSpikySnakeGame());
        snakePanel.addView(spikySnakeButton, fullWidthWrapContent());
        gameCard.addView(snakePanel, fullWidthWrapContent());

        LinearLayout guessPanel = createInsetPanel(Color.rgb(255, 250, 232), Color.rgb(245, 222, 142));
        guessPanel.addView(createSmallCaps("快速任务"), fullWidthWrapContent());
        TextView guessTitle = createTitle("幸运猜数");
        guessTitle.setTextSize(20);
        guessPanel.addView(guessTitle, fullWidthWrapContent());
        guessPanel.addView(createBodyText("30 秒内完成一次轻量小游戏，适合快速测试奖励领取流程。"), fullWidthWrapContent());
        Button guessGameButton = new Button(this);
        guessGameButton.setText(R.string.play_guess_game);
        styleSecondaryButton(guessGameButton);
        guessGameButton.setOnClickListener(view -> showGuessGameDialog());
        guessPanel.addView(guessGameButton, fullWidthWrapContent());
        gameCard.addView(guessPanel, fullWidthWrapContent());

        TextView moreGamesText = createCaption("规划中：转盘、刮刮卡、答题、拼图、排行榜、每日任务。");
        gameCard.addView(moreGamesText, fullWidthWrapContent());
        content.addView(gameCard, cardLayoutParams());

        LinearLayout profileCard = createCard();
        profileCard.addView(createSectionHeader("个人中心", "账户、成长路线和金币流水"), fullWidthWrapContent());
        accountText = createBodyText("");
        profileCard.addView(accountText, fullWidthWrapContent());
        LinearLayout profilePlan = createInsetPanel(Color.rgb(247, 248, 255), Color.rgb(222, 224, 247));
        profilePlan.addView(createSmallCaps("成长路线"), fullWidthWrapContent());
        profilePlan.addView(createBodyText("小游戏金币 · 每日任务 · 邀请活动 · 提现审核 · 反作弊风控"), fullWidthWrapContent());
        profileCard.addView(profilePlan, fullWidthWrapContent());
        LinearLayout alipayPlan = createInsetPanel(Color.rgb(246, 251, 249), Color.rgb(206, 231, 224));
        alipayPlan.addView(createSmallCaps("支付宝提现"), fullWidthWrapContent());
        alipayPlan.addView(createBodyText("当前版本先提交申请并冻结金币，正式版需要后端审核后调用支付宝企业转账接口。"), fullWidthWrapContent());
        profileCard.addView(alipayPlan, fullWidthWrapContent());
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
        TextView withdrawalTitle = createSectionTitle("提现申请记录");
        withdrawalTitle.setTextSize(18);
        profileCard.addView(withdrawalTitle, fullWidthWrapContent());
        withdrawalText = createCaption("");
        withdrawalText.setMinLines(2);
        withdrawalText.setMaxLines(6);
        withdrawalText.setMovementMethod(new ScrollingMovementMethod());
        profileCard.addView(withdrawalText, fullWidthWrapContent());
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

        TextView warning = createCaption("测试版只创建“审核中”的提现申请，不会从客户端自动打款。真实自动打款必须由后端审核后调用支付宝企业转账接口。");
        form.addView(warning, fullWidthWrapContent());

        EditText nameInput = new EditText(this);
        nameInput.setHint("支付宝实名姓名");
        nameInput.setSingleLine(true);
        form.addView(nameInput, fullWidthWrapContent());

        EditText accountInput = new EditText(this);
        accountInput.setHint("支付宝账号（手机号或邮箱）");
        accountInput.setSingleLine(true);
        accountInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        form.addView(accountInput, fullWidthWrapContent());

        EditText amountInput = new EditText(this);
        amountInput.setHint("提现金币数量，最低 " + MIN_WITHDRAW_COINS);
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
                refreshUi("支付宝提现申请已提交，金币已进入审核冻结状态。");
                Toast.makeText(this, "提现申请已提交，等待人工/后端审核。", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                Toast.makeText(this, "提交失败，请检查余额后重试。", Toast.LENGTH_SHORT).show();
            }
        }));
        dialog.show();
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
        balanceText.setText(numberFormat.format(ledger.getBalance()));
        pendingRewardText.setText(numberFormat.format(ledger.getPendingReward()) + " 金币");
        historyText.setText(ledger.getHistoryText());
        if (withdrawalText != null) {
            withdrawalText.setText(ledger.getWithdrawalHistoryText());
        }
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

    private LinearLayout createGlassCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(36, 255, 255, 255));
        background.setStroke(dp(1), Color.argb(80, 255, 255, 255));
        background.setCornerRadius(dp(22));
        card.setBackground(background);
        return card;
    }

    private LinearLayout createLaunchStep(String number, String title, String description) {
        LinearLayout row = createHorizontalRow();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(this);
        badge.setText(number);
        badge.setTextColor(Color.rgb(255, 221, 107));
        badge.setTextSize(16);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable badgeBackground = new GradientDrawable();
        badgeBackground.setColor(Color.argb(42, 255, 255, 255));
        badgeBackground.setCornerRadius(dp(18));
        badge.setBackground(badgeBackground);
        row.addView(badge, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextColor(Color.WHITE);
        titleView.setTextSize(15);
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(titleView, fullWidthWrapContent());
        TextView descView = new TextView(this);
        descView.setText(description);
        descView.setTextColor(Color.rgb(204, 231, 224));
        descView.setTextSize(13);
        copy.addView(descView, fullWidthWrapContent());
        row.addView(copy, weightedParams(1f, 0));
        return row;
    }

    private LinearLayout createHeroStat(String value, String label) {
        LinearLayout stat = new LinearLayout(this);
        stat.setOrientation(LinearLayout.VERTICAL);
        stat.setGravity(Gravity.CENTER);
        stat.setPadding(dp(8), dp(10), dp(8), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(32, 255, 255, 255));
        background.setCornerRadius(dp(16));
        stat.setBackground(background);

        TextView valueView = new TextView(this);
        valueView.setText(value);
        valueView.setTextColor(Color.WHITE);
        valueView.setTextSize(18);
        valueView.setTypeface(Typeface.DEFAULT_BOLD);
        valueView.setGravity(Gravity.CENTER);
        stat.addView(valueView, fullWidthWrapContent());

        TextView labelView = new TextView(this);
        labelView.setText(label);
        labelView.setTextColor(Color.rgb(204, 231, 224));
        labelView.setTextSize(12);
        labelView.setGravity(Gravity.CENTER);
        stat.addView(labelView, fullWidthWrapContent());
        return stat;
    }

    private LinearLayout createMetricBox(String label, TextView metricText) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(10), dp(12), dp(10), dp(12));
        box.setBackground(insetBackground(Color.rgb(255, 255, 255), Color.rgb(230, 237, 234), dp(18)));

        TextView labelView = createCaption(label);
        labelView.setGravity(Gravity.CENTER);
        box.addView(labelView, fullWidthWrapContent());
        box.addView(metricText, fullWidthWrapContent());
        return box;
    }

    private LinearLayout createInsetPanel(int fillColor, int strokeColor) {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(14), dp(14), dp(14));
        panel.setBackground(insetBackground(fillColor, strokeColor, dp(20)));
        return panel;
    }

    private LinearLayout createHorizontalRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        return row;
    }

    private TextView createLogoBadge() {
        TextView view = new TextView(this);
        view.setText("CP");
        view.setTextSize(18);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.WHITE);
        view.setGravity(Gravity.CENTER);
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(14, 124, 102), Color.rgb(91, 75, 219)}
        );
        background.setCornerRadius(dp(16));
        view.setBackground(background);
        return view;
    }

    private TextView createLaunchChip(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(Color.rgb(235, 249, 245));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(28, 255, 255, 255));
        background.setStroke(dp(1), Color.argb(72, 255, 255, 255));
        background.setCornerRadius(dp(16));
        view.setBackground(background);
        return view;
    }

    private TextView createStatusChip(String text, int fillColor, int strokeColor) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setTextColor(getColor(R.color.primary_text));
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(10), dp(7), dp(10), dp(7));
        view.setBackground(insetBackground(fillColor, strokeColor, dp(999)));
        return view;
    }

    private LinearLayout createSectionHeader(String title, String subtitle) {
        LinearLayout header = createHorizontalRow();
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        TextView titleView = createSectionTitle(title);
        copy.addView(titleView, fullWidthWrapContent());
        TextView subtitleView = createCaption(subtitle);
        copy.addView(subtitleView, fullWidthWrapContent());
        header.addView(copy, weightedParams(1f, 0));

        TextView mark = createStatusChip("NEW", Color.rgb(230, 247, 241), Color.rgb(177, 221, 208));
        header.addView(mark, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return header;
    }

    private LinearLayout createTaskRow(String number, String title, String description) {
        LinearLayout row = createHorizontalRow();
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView numberView = new TextView(this);
        numberView.setText(number);
        numberView.setTextSize(14);
        numberView.setTypeface(Typeface.DEFAULT_BOLD);
        numberView.setTextColor(Color.WHITE);
        numberView.setGravity(Gravity.CENTER);
        GradientDrawable numberBackground = new GradientDrawable();
        numberBackground.setColor(getColor(R.color.brand_green));
        numberBackground.setCornerRadius(dp(999));
        numberView.setBackground(numberBackground);
        row.addView(numberView, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPadding(dp(12), 0, 0, 0);
        TextView titleView = createBodyText(title);
        titleView.setTextColor(getColor(R.color.primary_text));
        titleView.setTypeface(Typeface.DEFAULT_BOLD);
        copy.addView(titleView, fullWidthWrapContent());
        copy.addView(createCaption(description), fullWidthWrapContent());
        row.addView(copy, weightedParams(1f, 0));
        return row;
    }

    private LinearLayout createSnakePreview() {
        LinearLayout board = new LinearLayout(this);
        board.setOrientation(LinearLayout.VERTICAL);
        board.setPadding(dp(10), dp(10), dp(10), dp(10));
        board.setBackground(insetBackground(Color.rgb(238, 248, 244), Color.rgb(206, 231, 224), dp(18)));
        int[][] pattern = {
                {0, 0, 3, 0, 0, 2},
                {0, 0, 3, 3, 3, 0},
                {2, 0, 0, 0, 3, 0},
                {0, 1, 0, 0, 3, 0}
        };
        for (int rowIndex = 0; rowIndex < pattern.length; rowIndex++) {
            LinearLayout row = createHorizontalRow();
            for (int column = 0; column < pattern[rowIndex].length; column++) {
                row.addView(createBoardSquare(pattern[rowIndex][column]), weightedParams(1f, column == 0 ? 0 : dp(4)));
            }
            board.addView(row, fullWidthWrapContent());
        }
        return board;
    }

    private TextView createBoardSquare(int type) {
        TextView square = new TextView(this);
        square.setGravity(Gravity.CENTER);
        square.setTextSize(10);
        GradientDrawable background = new GradientDrawable();
        if (type == 1) {
            square.setText("G");
            square.setTextColor(Color.WHITE);
            background.setColor(getColor(R.color.coin_gold));
        } else if (type == 2) {
            square.setText("X");
            square.setTextColor(Color.WHITE);
            background.setColor(getColor(R.color.danger_red));
        } else if (type == 3) {
            background.setColor(getColor(R.color.brand_green));
        } else {
            background.setColor(Color.WHITE);
        }
        background.setCornerRadius(dp(8));
        background.setStroke(dp(1), Color.rgb(221, 234, 229));
        square.setBackground(background);
        square.setMinHeight(dp(26));
        return square;
    }

    private TextView createSmallCaps(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(12);
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setLetterSpacing(0.12f);
        return view;
    }

    private TextView createPill(String text) {
        TextView view = createSmallCaps(text);
        view.setTextColor(Color.rgb(255, 221, 107));
        view.setPadding(dp(14), dp(8), dp(14), dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.argb(42, 255, 255, 255));
        background.setStroke(dp(1), Color.argb(84, 255, 255, 255));
        background.setCornerRadius(dp(999));
        view.setBackground(background);
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
        button.setMinHeight(dp(48));
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
        button.setMinHeight(dp(48));
    }

    private GradientDrawable accentCardBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{Color.rgb(255, 253, 244), Color.rgb(232, 246, 241)}
        );
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), Color.rgb(221, 234, 229));
        return drawable;
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

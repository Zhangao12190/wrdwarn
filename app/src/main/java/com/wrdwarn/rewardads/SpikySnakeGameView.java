package com.wrdwarn.rewardads;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

final class SpikySnakeGameView extends View {
    static final int DIRECTION_UP = 0;
    static final int DIRECTION_RIGHT = 1;
    static final int DIRECTION_DOWN = 2;
    static final int DIRECTION_LEFT = 3;

    interface GameListener {
        void onScoreChanged(int score);

        void onGameOver(int score);
    }

    private static final int GRID_SIZE = 12;
    private static final int SPIKE_COUNT = 14;
    private static final long TICK_MS = 420L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final ArrayDeque<Point> snake = new ArrayDeque<>();
    private final List<Point> spikes = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF cellRect = new RectF();
    private GameListener listener;

    private Point food = new Point();
    private float touchStartX;
    private float touchStartY;
    private int direction = DIRECTION_RIGHT;
    private int nextDirection = DIRECTION_RIGHT;
    private int score;
    private boolean running;
    private boolean gameOver;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            tick();
        }
    };

    SpikySnakeGameView(Context context) {
        super(context);
        setFocusable(true);
    }

    void setGameListener(GameListener listener) {
        this.listener = listener;
    }

    void start() {
        handler.removeCallbacks(tickRunnable);
        snake.clear();
        spikes.clear();
        direction = DIRECTION_RIGHT;
        nextDirection = DIRECTION_RIGHT;
        score = 0;
        running = true;
        gameOver = false;

        int center = GRID_SIZE / 2;
        snake.addFirst(new Point(center, center));
        snake.addLast(new Point(center - 1, center));
        snake.addLast(new Point(center - 2, center));

        placeSpikes();
        placeFood();
        notifyScoreChanged();
        invalidate();
        handler.postDelayed(tickRunnable, TICK_MS);
    }

    void stop() {
        running = false;
        handler.removeCallbacks(tickRunnable);
    }

    void setDirection(int requestedDirection) {
        if (isOpposite(direction, requestedDirection)) {
            return;
        }
        nextDirection = requestedDirection;
    }

    @Override
    protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int availableWidth = MeasureSpec.getSize(widthMeasureSpec);
        int desiredSize = Math.min(availableWidth, dp(320));
        if (desiredSize <= 0) {
            desiredSize = dp(320);
        }
        setMeasuredDimension(desiredSize, desiredSize);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            touchStartX = event.getX();
            touchStartY = event.getY();
            return true;
        }

        if (event.getAction() == MotionEvent.ACTION_UP) {
            float deltaX = event.getX() - touchStartX;
            float deltaY = event.getY() - touchStartY;
            if (Math.max(Math.abs(deltaX), Math.abs(deltaY)) < dp(24)) {
                return true;
            }

            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                setDirection(deltaX > 0 ? DIRECTION_RIGHT : DIRECTION_LEFT);
            } else {
                setDirection(deltaY > 0 ? DIRECTION_DOWN : DIRECTION_UP);
            }
            return true;
        }
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        float cell = size / GRID_SIZE;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(248, 250, 252));
        canvas.drawRoundRect(left, top, left + size, top + size, cell / 2f, cell / 2f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1f);
        paint.setColor(Color.rgb(226, 232, 240));
        for (int index = 0; index <= GRID_SIZE; index++) {
            float line = left + index * cell;
            canvas.drawLine(line, top, line, top + size, paint);
            float row = top + index * cell;
            canvas.drawLine(left, row, left + size, row, paint);
        }

        drawFood(canvas, left, top, cell);
        drawSpikes(canvas, left, top, cell);
        drawSnake(canvas, left, top, cell);

        if (gameOver) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(150, 0, 0, 0));
            canvas.drawRoundRect(left, top, left + size, top + size, cell / 2f, cell / 2f, paint);

            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(cell * 0.9f);
            canvas.drawText("Game Over", left + size / 2f, top + size / 2f, paint);
        }
    }

    private void tick() {
        if (!running) {
            return;
        }

        direction = nextDirection;
        Point head = snake.peekFirst();
        Point nextHead = new Point(head);
        if (direction == DIRECTION_UP) {
            nextHead.y--;
        } else if (direction == DIRECTION_RIGHT) {
            nextHead.x++;
        } else if (direction == DIRECTION_DOWN) {
            nextHead.y++;
        } else if (direction == DIRECTION_LEFT) {
            nextHead.x--;
        }

        if (isWall(nextHead) || containsPoint(spikes, nextHead) || containsSnake(nextHead, true)) {
            finishGame();
            return;
        }

        snake.addFirst(nextHead);
        if (sameCell(nextHead, food)) {
            score++;
            notifyScoreChanged();
            placeFood();
        } else {
            snake.removeLast();
        }

        invalidate();
        handler.postDelayed(tickRunnable, TICK_MS);
    }

    private void finishGame() {
        running = false;
        gameOver = true;
        handler.removeCallbacks(tickRunnable);
        invalidate();
        if (listener != null) {
            listener.onGameOver(score);
        }
    }

    private void notifyScoreChanged() {
        if (listener != null) {
            listener.onScoreChanged(score);
        }
    }

    private void placeSpikes() {
        while (spikes.size() < SPIKE_COUNT) {
            Point point = randomCell();
            if (!containsSnake(point, false) && !containsPoint(spikes, point)) {
                spikes.add(point);
            }
        }
    }

    private void placeFood() {
        do {
            food = randomCell();
        } while (containsSnake(food, false) || containsPoint(spikes, food));
    }

    private Point randomCell() {
        return new Point(random.nextInt(GRID_SIZE), random.nextInt(GRID_SIZE));
    }

    private void drawFood(Canvas canvas, float left, float top, float cell) {
        float cx = left + food.x * cell + cell / 2f;
        float cy = top + food.y * cell + cell / 2f;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(244, 180, 0));
        canvas.drawCircle(cx, cy, cell * 0.32f, paint);
    }

    private void drawSpikes(Canvas canvas, float left, float top, float cell) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.rgb(190, 38, 51));
        for (Point spike : spikes) {
            float x = left + spike.x * cell;
            float y = top + spike.y * cell;
            Path path = new Path();
            path.moveTo(x + cell / 2f, y + cell * 0.18f);
            path.lineTo(x + cell * 0.82f, y + cell * 0.82f);
            path.lineTo(x + cell * 0.18f, y + cell * 0.82f);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    private void drawSnake(Canvas canvas, float left, float top, float cell) {
        Iterator<Point> iterator = snake.iterator();
        boolean isHead = true;
        while (iterator.hasNext()) {
            Point part = iterator.next();
            float inset = cell * 0.12f;
            cellRect.set(
                    left + part.x * cell + inset,
                    top + part.y * cell + inset,
                    left + (part.x + 1) * cell - inset,
                    top + (part.y + 1) * cell - inset
            );
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(isHead ? Color.rgb(29, 78, 216) : Color.rgb(37, 99, 235));
            canvas.drawRoundRect(cellRect, cell * 0.18f, cell * 0.18f, paint);
            isHead = false;
        }
    }

    private boolean isWall(Point point) {
        return point.x < 0 || point.y < 0 || point.x >= GRID_SIZE || point.y >= GRID_SIZE;
    }

    private boolean containsSnake(Point point, boolean ignoreTail) {
        int index = 0;
        int last = snake.size() - 1;
        for (Point part : snake) {
            if (ignoreTail && index == last) {
                return false;
            }
            if (sameCell(part, point)) {
                return true;
            }
            index++;
        }
        return false;
    }

    private boolean containsPoint(List<Point> points, Point point) {
        for (Point item : points) {
            if (sameCell(item, point)) {
                return true;
            }
        }
        return false;
    }

    private boolean sameCell(Point first, Point second) {
        return first.x == second.x && first.y == second.y;
    }

    private boolean isOpposite(int currentDirection, int requestedDirection) {
        return (currentDirection == DIRECTION_UP && requestedDirection == DIRECTION_DOWN)
                || (currentDirection == DIRECTION_DOWN && requestedDirection == DIRECTION_UP)
                || (currentDirection == DIRECTION_LEFT && requestedDirection == DIRECTION_RIGHT)
                || (currentDirection == DIRECTION_RIGHT && requestedDirection == DIRECTION_LEFT);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}

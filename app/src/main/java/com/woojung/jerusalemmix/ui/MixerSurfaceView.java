package com.woojung.jerusalemmix.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import com.woojung.jerusalemmix.model.ChannelState;
import com.woojung.jerusalemmix.model.MixerState;

import java.util.Locale;

@SuppressLint("ViewConstructor")
public final class MixerSurfaceView extends View {
    public interface Listener {
        void onChannelSelected(int index);
        void onMuteChanged(ChannelState channel, boolean channelOn);
        void onFaderChanged(ChannelState channel, int hundredthDb, boolean finished);
        void onSendOnChanged(ChannelState channel, int mix, boolean on);
        void onBankRequested(int bank);
    }

    private final MixerState state;
    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int activeStrip = -1;
    private boolean draggingFader;

    public MixerSurfaceView(Context context, MixerState state, Listener listener) {
        super(context);
        this.state = state;
        this.listener = listener;
        paint.setTypeface(android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL));
        setBackgroundColor(Color.rgb(19, 22, 28));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float top = dp(42);
        float bottom = h - dp(58);
        float stripW = w / 8f;
        int start = state.bank * 8;

        drawMeterBridge(canvas, stripW, start);
        for (int slot = 0; slot < 8; slot++) {
            int index = start + slot;
            if (index >= state.channels().size()) break;
            drawStrip(canvas, state.channel(index), slot, stripW, top, bottom, index == state.selectedChannel);
        }
        drawBankBar(canvas, w, h);
    }

    private void drawMeterBridge(Canvas c, float stripW, int start) {
        paint.setColor(Color.rgb(10, 12, 16));
        c.drawRect(0, 0, getWidth(), dp(40), paint);
        for (int slot = 0; slot < 8; slot++) {
            int index = start + slot;
            if (index >= state.channels().size()) break;
            ChannelState ch = state.channel(index);
            float x = slot * stripW;
            paint.setColor(colorFor(ch.colorName));
            c.drawRect(x + dp(2), 0, x + stripW - dp(2), dp(5), paint);
            paint.setColor(Color.rgb(32, 37, 44));
            c.drawRect(x + dp(7), dp(10), x + stripW - dp(7), dp(34), paint);
            float meter = Math.max(0f, Math.min(1f, (ch.faderHundredthDb + 6000f) / 7000f));
            paint.setColor(meter > .85f ? Color.rgb(255, 164, 40) : Color.rgb(54, 214, 105));
            c.drawRect(x + dp(8), dp(33) - dp(22) * meter, x + stripW - dp(8), dp(33), paint);
        }
    }

    private void drawStrip(Canvas c, ChannelState ch, int slot, float stripW, float top, float bottom, boolean selected) {
        float left = slot * stripW;
        float right = left + stripW;
        paint.setColor(selected ? Color.rgb(47, 55, 67) : Color.rgb(29, 33, 40));
        c.drawRect(left + dp(1), top, right - dp(1), bottom, paint);

        paint.setColor(colorFor(ch.colorName));
        rounded(c, left + dp(5), top + dp(5), right - dp(5), top + dp(37), dp(4), paint);
        paint.setColor(contrastText(colorFor(ch.colorName)));
        text(c, ch.name, (left + right) / 2f, top + dp(26), dp(12), Paint.Align.CENTER, true);

        paint.setColor(Color.LTGRAY);
        String number = ch.stereo ? "ST " + (ch.logicalIndex - 71) : "CH " + (ch.logicalIndex + 1);
        text(c, number, (left + right) / 2f, top + dp(53), dp(10), Paint.Align.CENTER, false);

        int value = state.sendsOnFader ? ch.mixSendHundredthDb[state.selectedMix] : ch.faderHundredthDb;
        float fTop = top + dp(79);
        float fBottom = bottom - dp(83);
        float faderY = levelToY(value, fTop, fBottom);
        paint.setColor(Color.rgb(9, 11, 14));
        rounded(c, (left + right) / 2f - dp(4), fTop, (left + right) / 2f + dp(4), fBottom, dp(4), paint);
        paint.setColor(Color.rgb(98, 107, 118));
        for (int db : new int[]{10, 0, -10, -20, -40, -60}) {
            float y = levelToY(db * 100, fTop, fBottom);
            c.drawRect(left + dp(12), y, right - dp(12), y + dp(1), paint);
            paint.setColor(Color.rgb(176, 182, 190));
            text(c, db == 10 ? "+10" : String.valueOf(db), left + dp(8), y + dp(3), dp(7), Paint.Align.LEFT, false);
            paint.setColor(Color.rgb(98, 107, 118));
        }
        paint.setColor(state.sendsOnFader ? Color.rgb(218, 155, 43) : Color.rgb(203, 209, 217));
        rounded(c, left + dp(11), faderY - dp(13), right - dp(11), faderY + dp(13), dp(5), paint);
        paint.setColor(Color.rgb(54, 59, 66));
        c.drawRect(left + dp(16), faderY - dp(2), right - dp(16), faderY + dp(2), paint);

        paint.setColor(Color.rgb(213, 220, 228));
        text(c, formatDb(value), (left + right) / 2f, bottom - dp(61), dp(11), Paint.Align.CENTER, true);

        boolean on = state.sendsOnFader ? ch.mixSendOn[state.selectedMix] : ch.channelOn;
        boolean warning = !on;
        paint.setColor(warning ? Color.rgb(210, 43, 50) : Color.rgb(47, 54, 64));
        rounded(c, left + dp(8), bottom - dp(50), right - dp(8), bottom - dp(9), dp(5), paint);
        paint.setColor(Color.WHITE);
        text(c, state.sendsOnFader ? (on ? "SEND ON" : "SEND OFF") : (on ? "MUTE" : "MUTED"),
                (left + right) / 2f, bottom - dp(25), dp(11), Paint.Align.CENTER, true);
    }

    private void drawBankBar(Canvas c, float w, float h) {
        float y = h - dp(56);
        paint.setColor(Color.rgb(12, 14, 18));
        c.drawRect(0, y, w, h, paint);
        float bw = w / 10f;
        for (int i = 0; i < 10; i++) {
            paint.setColor(i == state.bank ? Color.rgb(46, 139, 201) : Color.rgb(40, 45, 53));
            rounded(c, i * bw + dp(3), y + dp(7), (i + 1) * bw - dp(3), h - dp(7), dp(4), paint);
            paint.setColor(Color.WHITE);
            String label = i == 9 ? "ST 1-8" : (i * 8 + 1) + "-" + (i * 8 + 8);
            text(c, label, (i + .5f) * bw, y + dp(34), dp(10), Paint.Align.CENTER, i == state.bank);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int slot = Math.max(0, Math.min(7, (int) (x / (getWidth() / 8f))));
        int channelIndex = state.bank * 8 + slot;
        if (channelIndex >= state.channels().size()) return true;
        ChannelState ch = state.channel(channelIndex);
        float top = dp(42);
        float bottom = getHeight() - dp(58);
        float fTop = top + dp(79);
        float fBottom = bottom - dp(83);

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (y >= getHeight() - dp(56)) {
                int bank = Math.max(0, Math.min(9, (int) (x / (getWidth() / 10f))));
                listener.onBankRequested(bank);
                return true;
            }
            activeStrip = slot;
            if (y >= top + dp(5) && y <= top + dp(65)) {
                listener.onChannelSelected(channelIndex);
                return true;
            }
            if (y >= bottom - dp(55)) {
                if (state.sendsOnFader) {
                    boolean next = !ch.mixSendOn[state.selectedMix];
                    ch.mixSendOn[state.selectedMix] = next;
                    listener.onSendOnChanged(ch, state.selectedMix, next);
                } else {
                    ch.channelOn = !ch.channelOn;
                    listener.onMuteChanged(ch, ch.channelOn);
                }
                invalidate();
                return true;
            }
            if (y >= fTop - dp(20) && y <= fBottom + dp(20)) {
                draggingFader = true;
                updateFader(ch, y, fTop, fBottom, false);
                return true;
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && draggingFader && activeStrip == slot) {
            updateFader(ch, y, fTop, fBottom, false);
            return true;
        } else if ((event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) && draggingFader) {
            updateFader(ch, y, fTop, fBottom, true);
            draggingFader = false;
            activeStrip = -1;
            if (event.getAction() == MotionEvent.ACTION_UP) performClick();
            return true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            performClick();
        }
        return true;
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    private void updateFader(ChannelState ch, float y, float top, float bottom, boolean finished) {
        int value = yToLevel(y, top, bottom);
        if (state.sendsOnFader) {
            ch.mixSendHundredthDb[state.selectedMix] = value;
        } else {
            ch.faderHundredthDb = value;
        }
        listener.onFaderChanged(ch, value, finished);
        invalidate();
    }

    private float levelToY(int value, float top, float bottom) {
        float db = value <= -32000 ? -60f : value / 100f;
        float normalized = (Math.max(-60f, Math.min(10f, db)) + 60f) / 70f;
        return bottom - normalized * (bottom - top);
    }

    private int yToLevel(float y, float top, float bottom) {
        float normalized = 1f - Math.max(0f, Math.min(1f, (y - top) / (bottom - top)));
        float db = normalized * 70f - 60f;
        return db <= -59.7f ? -32768 : Math.round(db * 100f);
    }

    private String formatDb(int value) {
        if (value <= -32000) return "-∞";
        return String.format(Locale.US, "%+.1f", value / 100f);
    }

    private int colorFor(String name) {
        return switch (name == null ? "" : name.toLowerCase(Locale.US)) {
            case "red" -> Color.rgb(205, 48, 48);
            case "yellow" -> Color.rgb(224, 183, 41);
            case "green" -> Color.rgb(57, 174, 83);
            case "skyblue", "cyan" -> Color.rgb(48, 176, 207);
            case "purple" -> Color.rgb(134, 80, 195);
            case "pink", "magenta" -> Color.rgb(213, 83, 155);
            case "orange" -> Color.rgb(224, 118, 40);
            case "off" -> Color.rgb(94, 101, 111);
            default -> Color.rgb(48, 105, 195);
        };
    }

    private int contrastText(int background) {
        double luminance = 0.2126 * Color.red(background) + 0.7152 * Color.green(background) + 0.0722 * Color.blue(background);
        return luminance > 145 ? Color.BLACK : Color.WHITE;
    }

    private void text(Canvas c, String value, float x, float y, float size, Paint.Align align, boolean bold) {
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setTypeface(android.graphics.Typeface.create("sans", bold ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL));
        c.drawText(value == null ? "" : value, x, y, paint);
    }

    private void rounded(Canvas c, float left, float top, float right, float bottom, float radius, Paint p) {
        rect.set(left, top, right, bottom);
        c.drawRoundRect(rect, radius, radius, p);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}

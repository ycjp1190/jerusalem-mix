package com.woojung.jerusalemmix;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.window.OnBackInvokedDispatcher;

import com.woojung.jerusalemmix.model.ChannelState;
import com.woojung.jerusalemmix.model.EqBand;
import com.woojung.jerusalemmix.model.MixerState;
import com.woojung.jerusalemmix.protocol.ClConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity implements ClConnection.Listener {
    private static final String LOCAL_APP = "file:///android_asset/index.html";
    private static final String RELEASE_API = "https://api.github.com/repos/ycjp1190/jerusalem-mix/releases/latest";
    private MixerState state;
    private ClConnection connection;
    private WebView webView;
    private long lastFaderSendMs;
    private final ExecutorService background = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideSystemBars();
        state = new MixerState();
        connection = new ClConnection(state, this);
        setContentView(buildWebView());
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    () -> webView.evaluateJavascript("window.JerusalemMix&&window.JerusalemMix.closeModal()", null));
        }
        background.execute(this::checkForUpdate);
    }

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    private View buildWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(9, 12, 17));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowContentAccess(false);
        settings.setAllowFileAccess(true);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setMediaPlaybackRequiresUserGesture(true);
        webView.addJavascriptInterface(new MixerBridge(), "JerusalemNative");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if ("file".equals(uri.getScheme()) && uri.toString().startsWith("file:///android_asset/")) return false;
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        });
        webView.loadUrl(LOCAL_APP);
        return webView;
    }

    private final class MixerBridge {
        @JavascriptInterface public String getState() { return stateJson().toString(); }
        @JavascriptInterface public String getPlatform() { return "android"; }
        @JavascriptInterface public String getVersion() { return BuildConfig.VERSION_NAME; }

        @JavascriptInterface public void setBank(int bank) {
            state.bank = clamp(bank, 0, 9);
            connection.pollVisibleBank();
            publishState();
        }

        @JavascriptInterface public void selectChannel(int index) {
            state.selectedChannel = clamp(index, 0, state.channels().size() - 1);
            connection.pollSelectedDetails(state.channel(state.selectedChannel));
            publishState();
        }

        @JavascriptInterface public void setSendsOnFader(boolean enabled) {
            state.sendsOnFader = enabled;
            connection.pollVisibleBank();
            publishState();
        }

        @JavascriptInterface public void setMix(int mix) {
            state.selectedMix = clamp(mix, 0, 23);
            connection.pollVisibleBank();
            publishState();
        }

        @JavascriptInterface public void setFader(int index, int value, boolean finished) {
            ChannelState channel = state.channel(index);
            int level = clamp(value, -32768, 1000);
            if (state.sendsOnFader) channel.mixSendHundredthDb[state.selectedMix] = level;
            else channel.faderHundredthDb = level;
            long now = System.currentTimeMillis();
            if (finished || now - lastFaderSendMs >= 55) {
                lastFaderSendMs = now;
                if (state.sendsOnFader) connection.setSendLevel(channel, state.selectedMix, level);
                else connection.setFader(channel, level);
            }
        }

        @JavascriptInterface public void setChannelOn(int index, boolean on) {
            ChannelState channel = state.channel(index);
            channel.channelOn = on;
            connection.setChannelOn(channel, on);
            publishState();
        }

        @JavascriptInterface public void setSendOn(int index, int mix, boolean on) {
            ChannelState channel = state.channel(index);
            int safeMix = clamp(mix, 0, 23);
            channel.mixSendOn[safeMix] = on;
            connection.setSendOn(channel, safeMix, on);
            publishState();
        }

        @JavascriptInterface public void setGain(int index, int value) {
            ChannelState channel = state.channel(index);
            channel.gainHundredthDb = clamp(value, -600, 6600);
            connection.setExperimental(channel, "Port/HA/Gain", 0, channel.gainHundredthDb);
        }

        @JavascriptInterface public void setPan(int index, int value) {
            ChannelState channel = state.channel(index);
            channel.pan = clamp(value, -63, 63);
            connection.setExperimental(channel, "ToSt/Pan", 0, channel.pan);
        }

        @JavascriptInterface public void setPeqOn(int index, boolean on) {
            ChannelState channel = state.channel(index);
            channel.peqOn = on;
            connection.setExperimental(channel, "PEQ/On", 0, on ? 1 : 0);
        }

        @JavascriptInterface public void setEq(int index, int band, String parameter, int value) {
            ChannelState channel = state.channel(index);
            int safeBand = clamp(band, 0, 3);
            EqBand eq = channel.eqBands[safeBand];
            switch (parameter) {
                case "Freq" -> eq.frequencyTenthHz = clamp(value, 200, 200000);
                case "Gain" -> eq.gainHundredthDb = clamp(value, -1800, 1800);
                case "Q" -> eq.qThousandths = clamp(value, 100, 16000);
                default -> { return; }
            }
            connection.setExperimental(channel, "PEQ/Band/" + parameter, safeBand, value);
        }

        @JavascriptInterface public void requestPhantom(int index, boolean enabled) {
            connection.setPhantomBlocked(state.channel(index), enabled);
        }

        @JavascriptInterface public void openSetup() { runOnUiThread(MainActivity.this::showSetup); }
        @JavascriptInterface public void checkUpdate() { background.execute(MainActivity.this::checkForUpdate); }
    }

    private JSONObject stateJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("platform", "android");
            root.put("connection", state.connection.name());
            root.put("connectionDetail", state.connectionDetail);
            root.put("controlEnabled", connection.isControlEnabled());
            root.put("experimentalEnabled", connection.isExperimentalEnabled());
            root.put("bank", state.bank);
            root.put("selectedChannel", state.selectedChannel);
            root.put("selectedMix", state.selectedMix);
            root.put("sendsOnFader", state.sendsOnFader);
            JSONArray channels = new JSONArray();
            for (ChannelState channel : state.channels()) channels.put(channelJson(channel));
            root.put("channels", channels);
        } catch (JSONException ignored) { }
        return root;
    }

    private JSONObject channelJson(ChannelState channel) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("index", channel.logicalIndex);
        json.put("stereo", channel.stereo);
        json.put("name", channel.name);
        json.put("color", channel.colorName);
        json.put("fader", channel.faderHundredthDb);
        json.put("on", channel.channelOn);
        json.put("gain", channel.gainHundredthDb);
        json.put("phantom", channel.phantom48);
        json.put("pan", channel.pan);
        json.put("peqOn", channel.peqOn);
        json.put("mixLevels", new JSONArray(channel.mixSendHundredthDb));
        JSONArray mixOn = new JSONArray();
        for (boolean on : channel.mixSendOn) mixOn.put(on);
        json.put("mixOn", mixOn);
        JSONArray bands = new JSONArray();
        for (EqBand band : channel.eqBands) {
            bands.put(new JSONObject().put("freq", band.frequencyTenthHz)
                    .put("gain", band.gainHundredthDb).put("q", band.qThousandths));
        }
        json.put("eq", bands);
        return json;
    }

    private void publishState() {
        if (webView == null) return;
        String encoded = JSONObject.quote(stateJson().toString());
        runOnUiThread(() -> webView.evaluateJavascript("window.JerusalemMix&&window.JerusalemMix.receive(" + encoded + ")", null));
    }

    private void showSetup() {
        SharedPreferences prefs = getSharedPreferences("cl5", MODE_PRIVATE);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(22);
        form.setPadding(padding, dp(8), padding, dp(8));
        EditText ip = input(prefs.getString("ip", "192.168.0.128"));
        EditText port = input(String.valueOf(prefs.getInt("port", 49280)));
        port.setInputType(InputType.TYPE_CLASS_NUMBER);
        CheckBox control = new CheckBox(this);
        control.setText(R.string.allow_verified_control);
        CheckBox experimental = new CheckBox(this);
        experimental.setText(R.string.allow_experimental_control);
        form.addView(field("CL5 IP 주소", ip));
        form.addView(field("TCP 포트 (기본값 49280)", port));
        form.addView(control);
        form.addView(experimental);
        TextView warning = new TextView(this);
        warning.setText(R.string.control_safety_warning);
        warning.setTextColor(Color.rgb(185, 45, 52));
        form.addView(warning);

        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("CL5 연결 설정").setView(form)
                .setNegativeButton("취소", null).setNeutralButton("연결 해제", (d, w) -> connection.disconnect())
                .setPositiveButton("연결", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String host = ip.getText().toString().trim();
            int portNumber;
            try { portNumber = Integer.parseInt(port.getText().toString().trim()); }
            catch (NumberFormatException error) { toast("포트 번호를 확인해 주세요."); return; }
            if (host.isEmpty() || portNumber < 1 || portNumber > 65535) { toast("IP 주소와 포트를 확인해 주세요."); return; }
            if (experimental.isChecked() && !control.isChecked()) { toast("실험 기능은 검증 기능 제어를 먼저 허용해야 합니다."); return; }
            prefs.edit().putString("ip", host).putInt("port", portNumber).apply();
            connection.connect(host, portNumber, control.isChecked(), experimental.isChecked());
            dialog.dismiss();
        }));
        dialog.show();
    }

    private LinearLayout field(String label, View input) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        TextView caption = new TextView(this);
        caption.setText(label);
        box.addView(caption);
        box.addView(input, new LinearLayout.LayoutParams(-1, dp(48)));
        return box;
    }

    private EditText input(String value) {
        EditText input = new EditText(this);
        input.setText(value);
        input.setSingleLine(true);
        return input;
    }

    private void checkForUpdate() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(RELEASE_API).openConnection();
            connection.setConnectTimeout(3500);
            connection.setReadTimeout(3500);
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Jerusalem-Mix-Android");
            if (connection.getResponseCode() != 200) return;
            StringBuilder body = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line; while ((line = reader.readLine()) != null) body.append(line);
            }
            JSONObject release = new JSONObject(body.toString());
            String tag = release.optString("tag_name", "").replaceFirst("^[vV]", "");
            String page = release.optString("html_url", "");
            if (!tag.isEmpty() && newerThan(tag, BuildConfig.VERSION_NAME) && !page.isEmpty()) {
                runOnUiThread(() -> new AlertDialog.Builder(this).setTitle("새 Jerusalem Mix 버전")
                        .setMessage("버전 " + tag + "을 다운로드할 수 있습니다. 예배 중에는 업데이트하지 마세요.")
                        .setNegativeButton("나중에", null)
                        .setPositiveButton("다운로드 페이지", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(page))))
                        .show());
            }
        } catch (Exception ignored) {
            // Update checks never affect offline mixer operation.
        } finally { if (connection != null) connection.disconnect(); }
    }

    private static boolean newerThan(String remote, String local) {
        int[] a = versionParts(remote); int[] b = versionParts(local);
        for (int i = 0; i < 3; i++) { if (a[i] != b[i]) return a[i] > b[i]; }
        return false;
    }

    private static int[] versionParts(String value) {
        int[] result = new int[3];
        String[] parts = value.split("[-+]")[0].split("\\.");
        for (int i = 0; i < Math.min(3, parts.length); i++) try { result[i] = Integer.parseInt(parts[i]); } catch (NumberFormatException ignored) { }
        return result;
    }

    @Override public void onConnectionChanged(MixerState.Connection next, String detail) { publishState(); }
    @Override public void onStateChanged() { publishState(); }
    @Override public void onProtocolWarning(String message) { runOnUiThread(() -> toast(message)); }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private void hideSystemBars() {
        getWindow().setStatusBarColor(Color.rgb(9, 12, 17));
        getWindow().setNavigationBarColor(Color.rgb(9, 12, 17));
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) controller.hide(WindowInsets.Type.statusBars());
        }
    }

    @Override protected void onDestroy() {
        connection.close();
        background.shutdownNow();
        if (webView != null) webView.destroy();
        super.onDestroy();
    }

}

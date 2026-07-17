package com.woojung.jerusalemmix.protocol;

import android.os.Handler;
import android.os.Looper;

import com.woojung.jerusalemmix.model.ChannelState;
import com.woojung.jerusalemmix.model.MixerState;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ClConnection implements AutoCloseable {
    public interface Listener {
        void onConnectionChanged(MixerState.Connection state, String detail);
        void onStateChanged();
        void onProtocolWarning(String message);
    }

    private final MixerState state;
    private final ClStateReducer reducer;
    private final Listener listener;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final ScheduledExecutorService polling = Executors.newSingleThreadScheduledExecutor();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final Object writerLock = new Object();
    private volatile boolean controlEnabled;
    private volatile boolean experimentalEnabled;
    private Socket socket;
    private BufferedWriter writer;

    public ClConnection(MixerState state, Listener listener) {
        this.state = state;
        this.reducer = new ClStateReducer(state);
        this.listener = listener;
        polling.scheduleWithFixedDelay(this::pollVisibleBank, 4, 4, TimeUnit.SECONDS);
    }

    public void connect(String ip, int port, boolean allowControl, boolean allowExperimental) {
        disconnect();
        controlEnabled = allowControl;
        experimentalEnabled = allowExperimental;
        publishConnection(MixerState.Connection.CONNECTING, ip + ":" + port + " 연결 중");
        io.execute(() -> runConnection(ip, port));
    }

    private void runConnection(String ip, int port) {
        try {
            Socket newSocket = new Socket();
            newSocket.connect(new InetSocketAddress(ip, port), 3500);
            newSocket.setKeepAlive(true);
            newSocket.setTcpNoDelay(true);
            socket = newSocket;
            writer = new BufferedWriter(new OutputStreamWriter(newSocket.getOutputStream(), StandardCharsets.US_ASCII));
            connected.set(true);
            publishConnection(controlEnabled ? MixerState.Connection.CONTROL : MixerState.Connection.READ_ONLY,
                    controlEnabled ? "CL5 CONTROL" : "CL5 READ ONLY");

            sendRaw("devinfo protocolver");
            sendRaw("devinfo paramsetver");
            sendRaw("devinfo productname");
            sendRaw("devinfo version");
            sendRaw("devstatus runmode");
            pollVisibleBank();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(newSocket.getInputStream(), StandardCharsets.US_ASCII))) {
                String line;
                while (connected.get() && (line = reader.readLine()) != null) {
                    ProtocolMessage message = YamahaClProtocol.parse(line);
                    if (message.kind() == ProtocolMessage.Kind.ERROR) publishWarning(message.raw());
                    if (reducer.apply(message)) main.post(listener::onStateChanged);
                }
            }
            if (connected.get()) throw new IOException("콘솔이 연결을 종료했습니다.");
        } catch (IOException error) {
            if (connected.get() || state.connection == MixerState.Connection.CONNECTING) {
                publishConnection(MixerState.Connection.ERROR, error.getMessage() == null ? "연결 실패" : error.getMessage());
            }
        } finally {
            connected.set(false);
            closeSocket();
        }
    }

    public void disconnect() {
        connected.set(false);
        closeSocket();
        if (state.connection != MixerState.Connection.DEMO) {
            publishConnection(MixerState.Connection.DEMO, "OFFLINE DEMO");
        }
    }

    public boolean isControlEnabled() {
        return connected.get() && controlEnabled;
    }

    public boolean isExperimentalEnabled() {
        return isControlEnabled() && experimentalEnabled;
    }

    public void setChannelOn(ChannelState channel, boolean on) {
        sendVerified(YamahaClProtocol.channelSet(channel, "Fader/On", on ? 1 : 0));
    }

    public void setFader(ChannelState channel, int hundredthDb) {
        sendVerified(YamahaClProtocol.channelSet(channel, "Fader/Level", YamahaClProtocol.clampLevel(hundredthDb)));
    }

    public void setSendOn(ChannelState channel, int mix, boolean on) {
        sendVerified(YamahaClProtocol.sendSet(channel, mix, "On", on ? 1 : 0));
    }

    public void setSendLevel(ChannelState channel, int mix, int hundredthDb) {
        sendVerified(YamahaClProtocol.sendSet(channel, mix, "Level", YamahaClProtocol.clampLevel(hundredthDb)));
    }

    public void setExperimental(ChannelState channel, String suffix, int y, int value) {
        if (!isExperimentalEnabled()) {
            publishWarning("실험 기능 안전 잠금이 켜져 있습니다.");
            return;
        }
        sendRaw(YamahaClProtocol.set(channel.protocolRoot() + "/" + suffix, channel.wireIndex, y, value));
    }

    /** Phantom power writes stay blocked until the CL5 path is verified on site. */
    public void setPhantomBlocked(ChannelState channel, boolean enabled) {
        publishWarning("+48V 제어는 현장 프로토콜 검증 전까지 안전 잠금 상태입니다.");
    }

    public void pollVisibleBank() {
        if (!connected.get()) return;
        int start = state.bank * 8;
        int end = Math.min(start + 8, state.channels().size());
        for (int i = start; i < end; i++) {
            ChannelState ch = state.channel(i);
            sendRaw(YamahaClProtocol.channelGet(ch, "Fader/On"));
            sendRaw(YamahaClProtocol.channelGet(ch, "Fader/Level"));
            if (state.sendsOnFader) {
                sendRaw(YamahaClProtocol.sendGet(ch, state.selectedMix, "On"));
                sendRaw(YamahaClProtocol.sendGet(ch, state.selectedMix, "Level"));
            }
            if (experimentalEnabled) {
                sendRaw(YamahaClProtocol.channelGet(ch, "Label/Name"));
                sendRaw(YamahaClProtocol.channelGet(ch, "Label/Color"));
            }
        }
    }

    public void pollSelectedDetails(ChannelState ch) {
        if (!connected.get() || !experimentalEnabled) return;
        sendRaw(YamahaClProtocol.channelGet(ch, "Port/HA/Gain"));
        // Reading the unverified phantom path is intentionally disabled as well.
        sendRaw(YamahaClProtocol.channelGet(ch, "ToSt/Pan"));
        sendRaw(YamahaClProtocol.channelGet(ch, "PEQ/On"));
        for (int band = 0; band < 4; band++) {
            sendRaw(YamahaClProtocol.get(ch.protocolRoot() + "/PEQ/Band/Freq", ch.wireIndex, band));
            sendRaw(YamahaClProtocol.get(ch.protocolRoot() + "/PEQ/Band/Gain", ch.wireIndex, band));
            sendRaw(YamahaClProtocol.get(ch.protocolRoot() + "/PEQ/Band/Q", ch.wireIndex, band));
        }
    }

    private void sendVerified(String command) {
        if (!isControlEnabled()) {
            publishWarning("읽기 전용입니다. 설정에서 '검증 기능 제어'를 켜세요.");
            return;
        }
        sendRaw(command);
    }

    private void sendRaw(String command) {
        if (!connected.get() || writer == null) return;
        io.execute(() -> {
            synchronized (writerLock) {
                try {
                    writer.write(command);
                    writer.write("\n");
                    writer.flush();
                } catch (IOException error) {
                    publishConnection(MixerState.Connection.ERROR, "명령 전송 실패: " + error.getMessage());
                    connected.set(false);
                    closeSocket();
                }
            }
        });
    }

    private void publishConnection(MixerState.Connection next, String detail) {
        state.connection = next;
        state.connectionDetail = detail;
        main.post(() -> listener.onConnectionChanged(next, detail));
    }

    private void publishWarning(String message) {
        main.post(() -> listener.onProtocolWarning(message));
    }

    private void closeSocket() {
        Socket current = socket;
        socket = null;
        writer = null;
        if (current != null) {
            try { current.close(); } catch (IOException ignored) {}
        }
    }

    @Override
    public void close() {
        disconnect();
        io.shutdownNow();
        polling.shutdownNow();
    }
}

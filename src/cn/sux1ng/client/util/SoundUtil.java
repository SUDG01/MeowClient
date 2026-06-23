package cn.sux1ng.client.util;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SoundUtil {

    // 🧠 内存缓存：把读取过的声音存起来，下次直接用！
    private static final Map<String, byte[]> soundCache = new HashMap<>();

    public static void playsound(String filename, float volume, float pitch) {
        new Thread(() -> {
            try {
                byte[] audioData;

                // 1. 检查缓存：如果之前读过，直接拿来用
                if (soundCache.containsKey(filename)) {
                    audioData = soundCache.get(filename);
                } else {
                    // 2. 没读过：去读取文件并存入缓存
                    String path = "/assets/meowclient/sounds/" + filename;
                    InputStream is = SoundUtil.class.getResourceAsStream(path);
                    if (is == null) return;

                    // 把流转成 byte 数组
                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                    int nRead;
                    byte[] data = new byte[16384];
                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                        buffer.write(data, 0, nRead);
                    }
                    audioData = buffer.toByteArray();
                    soundCache.put(filename, audioData); // 存入缓存
                }

                // 3. 准备播放
                InputStream byteStream = new ByteArrayInputStream(audioData);
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(byteStream);

                // 获取音频格式
                AudioFormat baseFormat = audioInputStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, baseFormat);
                Clip clip = (Clip) AudioSystem.getLine(info);
                clip.open(audioInputStream);

                // --- 🎛️ 调节音量 ---
                if (volume > 0f && volume != 1.0f) {
                    FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                    float dB = (float) (Math.log(volume) / Math.log(10.0) * 20.0);
                    gainControl.setValue(dB);
                }

                if (pitch != 1.0f) {
                    if (clip.isControlSupported(FloatControl.Type.SAMPLE_RATE)) {
                        FloatControl rateControl = (FloatControl) clip.getControl(FloatControl.Type.SAMPLE_RATE);
                        rateControl.setValue(baseFormat.getSampleRate() * pitch);
                    }
                }

                clip.start();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
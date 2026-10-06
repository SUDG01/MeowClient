package cn.sux1ng.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.client.resources.IResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** One reusable download per cape, with bounded workers and a quiet negative cache. */
final class CapeTexture extends ThreadDownloadImageData {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger THREADS = new AtomicInteger();
    private static final ThreadPoolExecutor DOWNLOADS = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<Runnable>(64), runnable -> {
                Thread thread = new Thread(runnable, "Meow Cape #" + THREADS.incrementAndGet());
                thread.setDaemon(true); return thread;
            }, new ThreadPoolExecutor.AbortPolicy());
    private static final long RETRY_DELAY = TimeUnit.MINUTES.toNanos(5);
    private final String username;
    private final String url;
    private final File cache;
    private final AtomicBoolean loading = new AtomicBoolean();
    private volatile boolean ready;
    private volatile long retryAfter;

    CapeTexture(String username, File cache) {
        super(null, CapeManager.getOptiFineCapeUrl(username), null, null);
        this.username = username; this.url = CapeManager.getOptiFineCapeUrl(username); this.cache = cache;
    }
    boolean isReady() { return ready; }
    boolean shouldRetry() { return !loading.get() && retryAfter != 0 && System.nanoTime() - retryAfter >= 0; }

    @Override public void loadTexture(IResourceManager resources) { if (!ready) loadTextureFromServer(); }
    @Override protected void loadTextureFromServer() {
        if (ready || retryAfter != 0 && !shouldRetry() || !loading.compareAndSet(false, true)) return;
        try { DOWNLOADS.execute(this::download); }
        catch (RejectedExecutionException full) {
            retryAfter = System.nanoTime() + TimeUnit.SECONDS.toNanos(30); loading.set(false);
        }
    }
    @Override public void setBufferedImage(BufferedImage image) {
        BufferedImage normalized = CapeManager.normalizeCape(image);
        if (normalized == null) return;
        super.setBufferedImage(normalized);
        // The volatile publication also makes the superclass image visible to the render thread.
        ready = true; retryAfter = 0;
    }
    private void download() {
        HttpURLConnection connection = null;
        try {
            if (cache != null && cache.isFile() && System.currentTimeMillis() - cache.lastModified() < TimeUnit.DAYS.toMillis(1)) {
                try { setBufferedImage(ImageIO.read(cache)); } catch (IOException | IllegalArgumentException ignored) {}
                if (ready) return;
            }
            Minecraft mc = Minecraft.getMinecraft();
            Proxy proxy = mc == null || mc.getProxy() == null ? Proxy.NO_PROXY : mc.getProxy();
            connection = (HttpURLConnection)new URL(url).openConnection(proxy);
            connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
            connection.setUseCaches(false); connection.setRequestProperty("User-Agent", "MeowClient");
            int status = connection.getResponseCode();
            // A missing cape is normal and remains cached for five minutes.
            if (status == 404 || status == 204) return;
            if (status / 100 != 2) throw new IOException("HTTP " + status);
            BufferedImage image;
            try (InputStream input = connection.getInputStream()) { image = ImageIO.read(input); }
            if (image == null) throw new IOException("not an image");
            setBufferedImage(image);
            if (!ready) throw new IOException("unsupported cape dimensions");
            if (cache != null) {
                try {
                    File parent = cache.getParentFile();
                    if (parent.isDirectory() || parent.mkdirs()) ImageIO.write(CapeManager.normalizeCape(image), "png", cache);
                } catch (IOException failure) { LOGGER.debug("Cape disk cache unavailable: {}", failure.getMessage()); }
            }
        } catch (IOException | IllegalArgumentException failure) {
            LOGGER.warn("OptiFine cape for {} unavailable: {}", username, failure.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
            if (!ready) retryAfter = System.nanoTime() + RETRY_DELAY;
            loading.set(false);
        }
    }
}

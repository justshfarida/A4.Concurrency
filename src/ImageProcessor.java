import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.logging.Logger;

/** Pixelates an image by replacing each square block with its average color. */
public class ImageProcessor {
    private static final Logger LOGGER = Logger.getLogger(ImageProcessor.class.getName());

    /** Approximate length of the single-threaded animation (visualization only). */
    private static final long ANIMATION_DURATION_NANOS = 15_000_000_000L;
    private static final long NANOS_PER_MILLI = 1_000_000L;

    private final BufferedImage image;
    private final int squareSize;
    private final Runnable onProgress;
    private final long nanosPerSquare;

    /**
     * @param image      the original, full-size image; it is modified in place
     * @param squareSize side of the averaging square in pixels
     * @param onProgress called after each square, used to refresh the display
     */
    public ImageProcessor(BufferedImage image, int squareSize, Runnable onProgress) {
        this.image = image;
        this.squareSize = squareSize;
        this.onProgress = onProgress;

        long squaresAcross = Math.ceilDiv(image.getWidth(), squareSize);
        long squaresDown = Math.ceilDiv(image.getHeight(), squareSize);
        this.nanosPerSquare = ANIMATION_DURATION_NANOS / (squaresAcross * squaresDown);
    }

    /** Processes the whole image in the calling thread, left to right, top to bottom. */
    public void processSingleThreaded() {
        long start = System.nanoTime();
        processRows(0, image.getHeight());
        logDone(start);
    }

    /** Processes all squares whose top edge lies in [startY, endY). */
    private void processRows(int startY, int endY) {
        long start = System.nanoTime();
        long squaresDone = 0;
        for (int y = startY; y < endY; y += squareSize) {
            for (int x = 0; x < image.getWidth(); x += squareSize) {
                averageSquare(x, y);
                squaresDone++;
                onProgress.run();
                waitForSchedule(start, squaresDone);
            }
        }
    }

    /** Replaces the square at (x, y) with its average color. Edge squares may be smaller. */
    private void averageSquare(int x, int y) {
        int width = Math.min(squareSize, image.getWidth() - x);
        int height = Math.min(squareSize, image.getHeight() - y);
        int[] pixels = image.getRGB(x, y, width, height, null, 0, width);

        long red = 0;
        long green = 0;
        long blue = 0;
        for (int pixel : pixels) {
            red += (pixel >> 16) & 0xFF;
            green += (pixel >> 8) & 0xFF;
            blue += pixel & 0xFF;
        }

        int count = pixels.length;
        int average = 0xFF000000
                | (int) (red / count) << 16
                | (int) (green / count) << 8
                | (int) (blue / count);

        Arrays.fill(pixels, average);
        image.setRGB(x, y, width, height, pixels, 0, width);
    }

    /**
     * Slows processing down so the progress is visible. Sleeps only when this
     * worker is ahead of its schedule, so tiny per-square delays add up correctly.
     */
    private void waitForSchedule(long startNanos, long squaresDone) {
        long aheadNanos = startNanos + squaresDone * nanosPerSquare - System.nanoTime();
        if (aheadNanos >= NANOS_PER_MILLI) {
            try {
                Thread.sleep(aheadNanos / NANOS_PER_MILLI);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logDone(long startNanos) {
        double seconds = (System.nanoTime() - startNanos) / 1e9;
        LOGGER.info(String.format("Done in %.2f s", seconds));
    }
}
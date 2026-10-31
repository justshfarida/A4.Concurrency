import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

public class Main {
    private static final String RESULT_FILE = "result.jpg";

    // Space reserved for the window title bar and borders.
    private static final int FRAME_MARGIN_WIDTH = 20;
    private static final int FRAME_MARGIN_HEIGHT = 60;

    public static void main(String[] args) {
        if (args.length != 3) {
            exitWithError("Usage: java Main <file.jpg> <square size> <S|M>");
        }

        String fileName = args[0];
        int squareSize = parseSquareSize(args[1]);
        String mode = args[2].toUpperCase();
        if (!mode.equals("S") && !mode.equals("M")) {
            exitWithError("Processing mode must be S (single) or M (multi).");
        }

        BufferedImage image = loadImage(fileName);
        ImagePanel panel = createWindow(image);

        // Processing runs on the main thread, never on Swing's UI thread,
        // so the window stays responsive while squares are being averaged.
        ImageProcessor processor = new ImageProcessor(image, squareSize, panel::repaint);
        processor.processSingleThreaded(); // M mode is added in the next step

        panel.repaint();
        saveImage(image, RESULT_FILE);
    }

    private static int parseSquareSize(String value) {
        try {
            int size = Integer.parseInt(value);
            if (size <= 0) {
                exitWithError("Square size must be a positive integer.");
            }
            return size;
        } catch (NumberFormatException e) {
            exitWithError("Square size must be a positive integer.");
            return -1; // unreachable
        }
    }

    private static BufferedImage loadImage(String fileName) {
        try {
            BufferedImage image = ImageIO.read(new File(fileName));
            if (image == null) {
                exitWithError("Unsupported or unreadable image: " + fileName);
            }
            return image;
        } catch (IOException e) {
            exitWithError("Could not read file: " + fileName);
            return null; // unreachable
        }
    }

    private static void saveImage(BufferedImage image, String fileName) {
        try {
            ImageIO.write(image, "jpg", new File(fileName));
            System.out.println("Result saved to " + fileName);
        } catch (IOException e) {
            exitWithError("Could not save result: " + e.getMessage());
        }
    }

    /** Builds and shows the window on Swing's UI thread and waits until it is ready. */
    private static ImagePanel createWindow(BufferedImage image) {
        ImagePanel[] panel = new ImagePanel[1];
        try {
            SwingUtilities.invokeAndWait(() -> panel[0] = showWindow(image));
        } catch (InterruptedException | InvocationTargetException e) {
            exitWithError("Could not open the window: " + e.getMessage());
        }
        return panel[0];
    }

    private static ImagePanel showWindow(BufferedImage image) {
        ImagePanel panel = new ImagePanel(image, computeDisplaySize(image));
        JFrame frame = new JFrame("Image Averaging");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
        return panel;
    }

    /** Fits the image inside the usable screen area, never enlarging it. */
    private static Dimension computeDisplaySize(BufferedImage image) {
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getMaximumWindowBounds();
        double maxWidth = screen.width - FRAME_MARGIN_WIDTH;
        double maxHeight = screen.height - FRAME_MARGIN_HEIGHT;
        double scale = Math.min(1.0, Math.min(maxWidth / image.getWidth(),
                maxHeight / image.getHeight()));
        return new Dimension((int) (image.getWidth() * scale),
                (int) (image.getHeight() * scale));
    }

    private static void exitWithError(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
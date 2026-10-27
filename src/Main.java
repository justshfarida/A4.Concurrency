import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {
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
        SwingUtilities.invokeLater(() -> showWindow(image));
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

    private static void showWindow(BufferedImage image) {
        JFrame frame = new JFrame("Image Averaging");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(new ImagePanel(image, computeDisplaySize(image)));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);
        frame.setVisible(true);
    }

    private static void exitWithError(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
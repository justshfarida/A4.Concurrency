import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/** Panel that draws an image scaled to a fixed display size. */
public class ImagePanel extends JPanel {
    private final BufferedImage image;
    private final Dimension displaySize;

    public ImagePanel(BufferedImage image, Dimension displaySize) {
        this.image = image;
        this.displaySize = displaySize;
        setPreferredSize(displaySize);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(image, 0, 0, displaySize.width, displaySize.height, null);
    }
}
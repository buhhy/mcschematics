package schematic;

import schematic.models.images.RedstoneImageProvider;
import schematic.views.MainFrame;

import javax.swing.*;

/**
 * Launcher for the Schematic-to-blueprint application
 * @author klaue
 */
public class Schematic2Blueprint {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
//                try {
//                    UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                new MainFrame();
            }
        });
    }
}

package core;
public class App {
    public static void main(String[] args) throws Exception {
        // Swing components must be created on the event dispatch thread.
        javax.swing.SwingUtilities.invokeLater(GameWindow::new);
    }
}

package ghost;

import java.awt.EventQueue;
import javax.swing.JFrame;

public class Ghost implements Runnable {

    Game game; // model

    public static void main(String[] args) {
        EventQueue.invokeLater(new Ghost());
    }

    public Ghost() {
        Trie trie = new Trie("WORD.LST.txt");
        game = new Game(trie);
    }

    @Override
    public void run() {
        JFrame ui = new GhostUI(game);
    }
}

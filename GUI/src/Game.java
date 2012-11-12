package ghost;

import java.util.Observable;
import java.util.Random;

public class Game extends Observable {

    Trie.Node triePtr;
    int winner = -1;
    int move = 1;
    String currMover = "User";
    String currWord = "";
    int winStatus = -1;

    public Game(Trie trie) {
        triePtr = trie.root;
    }

    protected void move(char c) {
        if (!triePtr.children.containsKey(c)) {
            winStatus = 0;
            winner = (currMover.equals("User")) ? 1 : 0;
        } else if (triePtr.children.get(c).maxDepth < 4) {
            winStatus = 1;
            winner = (currMover.equals("User")) ? 1 : 0;
        } else {
            currWord += c;

            triePtr = triePtr.children.get(c);
            if (triePtr.terminal) {
                winStatus = 2;
                winner = (currMover.equals("User")) ? 1 : 0;
            } else {
                currMover = (currMover.equals("User")) ? "Computer" : "User";
                move++;
            }
        }

        setChanged();
        notifyObservers(this);
    }

    // the computer first tries to choose a computer goal node. 
    // If there aren't any, choose among paths that force maximal game length.
    protected void compMove() {
        Random rand = new Random();

        char c;

        int numGoals = triePtr.compGoals.size();
        if (numGoals > 0) {
            c = triePtr.compGoals.get(rand.nextInt(numGoals)).myChar;
        } else {
            int numMaxLosingPaths = triePtr.maxLosingPaths.size();
            c = triePtr.maxLosingPaths.get(rand.nextInt(numMaxLosingPaths)).myChar;
        }
        move(c);
    }
}
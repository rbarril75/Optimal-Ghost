package ghost;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Trie {

    Node root;

    public Trie(String filename) {
        root = new Node(null, 0);
        build(filename);
        solve();
    }

    private void build(String filename) {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(filename));
            String line;
            while ((line = buf.readLine()) != null) {
                insertWord(line);
            }
        } catch (IOException x) {
            System.out.println("Invalid pathname.");
        }

    }

    private void insertWord(String word) {
        root.insertWord(word);
    }

    private void solve() {
        solveTrie(root);
    }

    protected class Node {

        Map<Character, Node> children = new HashMap<Character, Node>();
        boolean terminal = false; // indicates if this node represents a char ending a word (of length >= 4), i.e. a leaf
        Character myChar;
        int depth; // distance from root
        int maxDepth; // depth of lowest leaf in tree rooted at this node
        boolean compGoal;
        int depthOfUserGoal = -1; // depth of lowest user goal in sub-tree rooted at this node
        int depthOfCompGoal = -1; // depth of lowest user goal in sub-tree rooted at this node
        List<Node> compGoals = new ArrayList<Node>(); // list of children designated computer (P2) goals
        List<Node> userGoals = new ArrayList<Node>(); // list of children designated user (P1) goals
        List<Node> maxLosingPaths = new ArrayList<Node>(); // holds children node(s) that could be taken by losing player to force maximal game length

        private Node(Character myChar, int depth) {
            this.depth = depth;
            maxDepth = depth;
            compGoal = (depth % 2 == 0);
            this.myChar = myChar;
        }

        void insertWord(String word) {
            if (terminal) {
                return;
            }
            if (word.equals("")) {
                if (depth >= 4) {
                    terminal = true;
                }
                return;
            }
            if (!children.containsKey(word.charAt(0))) {
                Node newNode = new Node(word.charAt(0), depth + 1);
                addChild(word.charAt(0), newNode);
                newNode.insertWord(word.substring(1));
            } else {
                children.get(word.charAt(0)).insertWord(word.substring(1));
            }

            maxDepth = (maxDepth > depth + word.length()) ? maxDepth : depth + word.length();
        }

        void addChild(Character c, Node node) {
            children.put(c, node);
        }
    }

    private void solveTrie(Node node) {
        boolean isEvenNode = (node.depth % 2 == 0);

        if (node.children.isEmpty()) {
            if (node.depth >= 4) // only count 4+ length strings as goals
            {
                node.compGoal = !isEvenNode;
            }

            if (isEvenNode) {
                node.depthOfUserGoal = node.depth;
            } else {
                node.depthOfCompGoal = node.depth;
            }

            return;
        }

        // node.compGoal = isEvenNode; // non-leaf initialization (done in constructor instead)

        for (Node n : node.children.values()) {
            solveTrie(n);
            if (isEvenNode) { // it is the user's turn
                if (!n.compGoal) {
                    // if there is a user goal to be chosen, this node is a user goal as well
                    // if no user goals can be chosen, this node is a computer goal
                    node.compGoal = false;
                    node.depthOfUserGoal = (node.depthOfUserGoal > n.depthOfUserGoal)
                            ? node.depthOfUserGoal : n.depthOfUserGoal;
                    node.userGoals.add(n);
                } else {
                    node.compGoals.add(n);

                    // if the user cannot choose a user goal, he chooses the (computer-winning) path(s) that forces the maximal game length
                    // node.maxLosingPaths holds these path(s) and is updated here
                    updateMaxLosingPaths(node, n, "compGoals");
                }
            } else { // it is the computer's turn
                if (n.compGoal) {
                    // if there is a computer goal to be chosen, this node is a computer goal as well
                    // if no computer goals can be chosen, this node is a user goal
                    node.compGoal = true;
                    node.depthOfCompGoal = (node.depthOfCompGoal > n.depthOfCompGoal)
                            ? node.depthOfCompGoal : n.depthOfCompGoal;
                    node.compGoals.add(n);
                } else {
                    node.userGoals.add(n);

                    // if the computer cannot choose a computer goal, he chooses the (user-winning) path(s) that forces the maximal game length
                    // node.maxLosingPaths holds these path(s) and is updated here
                    updateMaxLosingPaths(node, n, "userGoals");
                }
            }
        }
    }

    // update paths a losing player could take at node to force maximal game length
    private void updateMaxLosingPaths(Node node, Node n, String type) {
        boolean typeComp = type.equals("compGoals");
        List<Node> goals = (typeComp) ? n.compGoals : n.userGoals;

        if (node.maxLosingPaths.isEmpty()) {
            node.maxLosingPaths.add(n);
            updateDepthOfGoal(node, n, type);
        } else if (!goals.isEmpty()) {
            int numGoalsOfChild = (typeComp)
                    ? node.maxLosingPaths.get(0).compGoals.size() : node.maxLosingPaths.get(0).userGoals.size();

            int nodeDepthOfGoal = (typeComp) ? node.depthOfCompGoal : node.depthOfUserGoal;
            int nDepthOfGoal = (typeComp) ? n.depthOfCompGoal : n.depthOfUserGoal;

            if (goals.size() == numGoalsOfChild) {
                // enter this branch if another node has an equal number of computer-winning paths
                // if depth of other node is bigger, replace old node. if equal, join with old node. if smaller, ignore.
                if (nDepthOfGoal > nodeDepthOfGoal) {
                    node.maxLosingPaths.clear();
                    node.maxLosingPaths.add(n);
                    updateDepthOfGoal(node, n, type);
                } else if (nDepthOfGoal == nodeDepthOfGoal) {
                    node.maxLosingPaths.add(n);
                }
            } else if (goals.size() > numGoalsOfChild) {
                // n has a larger number of computer-winning paths. replace old paths if n's paths are all longer than old paths.
                if (allLongerPaths(n, node.maxLosingPaths, type)) {
                    node.maxLosingPaths.clear();
                    node.maxLosingPaths.add(n);
                    updateDepthOfGoal(node, n, type);
                }
            } else {
                // another node has a smaller number of computer-winning paths. 
                // replace old node if at least one of other node's paths is longer than one in old node
                if (!allLongerPaths(node.maxLosingPaths, n, type)) {
                    node.maxLosingPaths.clear();
                    node.maxLosingPaths.add(n);
                    updateDepthOfGoal(node, n, type);
                }
            }
        }
    }

    private void updateDepthOfGoal(Node node, Node n, String type) {
        if (type.equals("compGoals")) {
            node.depthOfCompGoal = n.depthOfCompGoal;
        } else {
            node.depthOfUserGoal = n.depthOfUserGoal;
        }
    }

    // returns true if the goals of n have a greater than or equal depthOfGoal then the goals of every node in list
    private boolean allLongerPaths(Node n, List<Node> list, String type) {
        boolean typeComp = type.equals("compGoals");

        for (Node k : list) {
            List<Node> kGoalList = (typeComp) ? k.compGoals : k.userGoals;
            List<Node> nGoalList = (typeComp) ? n.compGoals : n.userGoals;
            for (Node kGoal : kGoalList) {
                for (Node nGoal : nGoalList) {
                    int kGoalDepthOfGoal = (typeComp) ? kGoal.depthOfCompGoal : kGoal.depthOfUserGoal;
                    int nGoalDepthOfGoal = (typeComp) ? nGoal.depthOfCompGoal : nGoal.depthOfUserGoal;
                    if (kGoalDepthOfGoal > nGoalDepthOfGoal) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // returns true if the goals of every node in list have a greater than or equal depthOfGoal than the goals of n
    private boolean allLongerPaths(List<Node> list, Node n, String type) {
        boolean typeComp = type.equals("compGoals");
        for (Node k : list) {
            List<Node> kGoalList = (typeComp) ? k.compGoals : k.userGoals;
            List<Node> nGoalList = (typeComp) ? n.compGoals : n.userGoals;
            for (Node kGoal : kGoalList) {
                for (Node nGoal : nGoalList) {
                    int nGoalDepthOfGoal = (typeComp) ? nGoal.depthOfCompGoal : nGoal.depthOfUserGoal;
                    int kGoalDepthOfGoal = (typeComp) ? kGoal.depthOfCompGoal : kGoal.depthOfUserGoal;
                    if (nGoalDepthOfGoal > kGoalDepthOfGoal) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}

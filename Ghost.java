/*
//  Ghost.java
//  Created by Ryan Barril on 10/26/12.
*/

import java.io.*;
import java.util.*;

public class Ghost {
	
	static class Trie {
		Node root;
		int numWords;
		
		Trie() {
			root = new Node(null, 0);
		}
		
		void insertWord(String word) {
			if (!root.children.containsKey(word.charAt(0))) {
				Node newNode = new Node(word.charAt(0), 1);
				root.addChild(word.charAt(0), newNode);
				newNode.insertWord(word.substring(1));
			}
			else {
				root.children.get(word.charAt(0)).insertWord(word.substring(1));
			}
		}
	}
	
	static class Node {
		Map<Character, Node> children;
		boolean terminal; // indicates if this node represents a char ending a word (of length >= 4). This node is a leaf.
		int depth; // distance from root
		int maxDepth; // distance of lowest leaf in tree rooted at this node from trie root
		boolean compGoal;
		int maxDepthToGoal;
		List<Node> compGoals;
		List<Node> userGoals;
		List<Node> maxLosingPaths;
		Character myChar;
		
		Node(Character myChar, int depth) {
			children = new HashMap<Character, Node>();
			terminal = false;
			this.depth = depth;
			maxDepth = depth;
			compGoals = new ArrayList<Node>();
			userGoals = new ArrayList<Node>();
			maxLosingPaths = new ArrayList<Node>();
			this.myChar = myChar;
		}
		
		void insertWord(String word) {
			if (terminal)
				return;
			if (word.equals("")) {
				if (depth >= 4)
					terminal = true;
				return;
			}
			if (!children.containsKey(word.charAt(0))) {
				Node newNode = new Node(word.charAt(0), depth + 1);
				addChild(word.charAt(0), newNode);
				newNode.insertWord(word.substring(1));
				maxDepth = (maxDepth > depth + word.length()) ? maxDepth : depth + word.length();
			}
			else {
				children.get(word.charAt(0)).insertWord(word.substring(1));
			}
		}
		
		void addChild(Character c, Node node) {
			children.put(c, node);
		}
	}
	
	static class Game {
		User user;
		Comp comp;
		Comp2 comp2;
		Node triePtr;
		int winner;
		int move;
		String currMover;
		String currWord;
		boolean human;
		
		Game(Trie trie) {
			user = new User();
			comp = new Comp();
			comp2 = new Comp2();
			triePtr = trie.root;
			winner = -1;
			move = 1;
			currMover = "User";
			currWord = "";
			human = true;
		}
		
		void begin() {
			while (winner == -1) {
				char c;
				if (move % 2 == 1)
					c = user.move();
				else
					c = comp.move(triePtr);
				
				System.out.println(currMover + " moves " + c);
				
				Map<Character, Node> children = triePtr.children;
				if (!children.containsKey(c)) {
					winner = (currMover.equals("User")) ? 1 : 0;
					if (winner == 1) 
						System.out.println("Invalid word. Computer wins.");
					else 
						System.out.println("Invalid word. User wins.");
					continue;
				}
				
				if (children.get(c).maxDepth < 4) {
					winner = (currMover.equals("User")) ? 1 : 0;
					if (winner == 1) 
						System.out.println("No valid word longer than 3 letters can be extended from this word. Computer wins.");
					else 
						System.out.println("No valid word longer than 3 letters can be extended from this word. User wins.");
					continue;				
				}
				
				currWord += c;
				System.out.println("Word so far is " + currWord);
				triePtr = children.get(c);
				if (triePtr.terminal) {
					winner = (currMover.equals("User")) ? 1 : 0;
					if (winner == 1) 
						System.out.println("Word completed. Computer wins.");
					else 
						System.out.println("Word completed. User wins.");
				}
				currMover = (currMover.equals("User")) ? "Computer" : "User";
				move++;
			}
		} 
	}
	
	static class User {
		Scanner in = new Scanner(System.in);
		
		char move() {
			String input;
			
			while (true) {
				System.out.print("Enter a character: ");
				input = in.nextLine();
				if (input.length() != 1) {
					System.out.println("Invalid move. User, move again.");
					continue;
				}
				break;
			}
			return input.charAt(0);
		}
	}
	
	/* The computer first tries to choose a computer goal node. If there aren't any, choose among paths that force maximal game length. */
	static class Comp {
		char move(Node node) {			
			Random rand = new Random();
			
			int numGoals = node.compGoals.size();
			if (numGoals > 0) {
				System.out.println("Comp1 choosing from " + numGoals + " goals.");
				return node.compGoals.get(rand.nextInt(numGoals)).myChar;
			}
			
			int numMaxLosingPaths = node.maxLosingPaths.size();
			
			return node.maxLosingPaths.get(rand.nextInt(numMaxLosingPaths)).myChar;
		}
	}
	
	static void solveTrie(Trie trie) {
		solveTrie(trie.root);
	}
	
	// Sets whether this node is a computer (P2) or User (P1) goal. Returns depth of furthest goal leaf.
	static int solveTrie(Node node) {
		boolean isEvenNode = (node.depth % 2 == 0);
		
		if (node.children.isEmpty()) {
			if (node.depth >= 4) // only count 4+ length strings as goals
				node.compGoal = !isEvenNode;
			return node.depth;
		}
		
		node.compGoal = isEvenNode; // non-leaf initialization
		
		for (Node n : node.children.values()) {
			int depth = solveTrie(n);
			if (isEvenNode) { // it is the user's turn
				if (!n.compGoal) {
					// if there is a user goal to be chosen, this node is a user goal as well
					// if no user goals can be chosen, this node is a computer goal
					node.compGoal = false; 
					node.userGoals.add(n);
				}
				else {
					node.compGoals.add(n);
					
					// if the user cannot choose a user goal, he chooses the (computer-winning) path(s) that forces the maximal game length
					// node.maxLosingPaths holds these path(s)
					if (node.maxLosingPaths.size() == 0) {
						node.maxLosingPaths.add(n);
						node.maxDepthToGoal = depth;
					}
					else if (n.compGoals.size() != 0 && n.compGoals.size() <= node.maxLosingPaths.get(0).compGoals.size()) {
						if (n.compGoals.size() == node.maxLosingPaths.get(0).compGoals.size()) {
							// enter this branch if another node has an equal number of computer-winning paths
							// if depth of other node is bigger, replace old node. if equal, join with old node. if smaller, ignore.
							if (depth > node.maxDepthToGoal) {
								node.maxLosingPaths.clear();
								node.maxLosingPaths.add(n);
								node.maxDepthToGoal = depth;
							}
							else if (depth == node.maxDepthToGoal)
								node.maxLosingPaths.add(n);
						}
						else {
							// another node has a smaller number of computer-winning paths. replace old node.
							node.maxLosingPaths.clear();
							node.maxLosingPaths.add(n);
							node.maxDepthToGoal = depth;
						}
					}
				}
			}
			else { // it is the computer's turn
				if (n.compGoal) {
					// if there is a computer goal to be chosen, this node is a computer goal as well
					// if no computer goals can be chosen, this node is a user goal
					node.compGoal = true;
					node.compGoals.add(n);
				}
				else {
					node.userGoals.add(n);
					
					// if the computer cannot choose a computer goal, he chooses the (user-winning) path(s) that forces the maximal game length
					// node.maxLosingPaths holds these path(s)
					if (node.maxLosingPaths.size() == 0) {
						node.maxLosingPaths.add(n);
						node.maxDepthToGoal = depth;
					}
					else if (n.userGoals.size() != 0 && n.userGoals.size() <= node.maxLosingPaths.get(0).userGoals.size()) {
						if (n.userGoals.size() == node.maxLosingPaths.get(0).userGoals.size()) {
							// enter this branch if another node has an equal number of computer-winning paths
							// if depth of other node is bigger, replace old node. if equal, join with old node. if smaller, ignore.
							if (depth > node.maxDepthToGoal) {
								node.maxLosingPaths.clear();
								node.maxLosingPaths.add(n);
								node.maxDepthToGoal = depth;
							}
							else if (depth == node.maxDepthToGoal) {
								node.maxLosingPaths.add(n);
							}
						}
						else {
							// another node has a smaller number of computer-winning paths. replace old node.
							node.maxLosingPaths.clear();
							node.maxLosingPaths.add(n);
							node.maxDepthToGoal = depth;
						}
					}
				}
			}
		}
		
		return node.maxDepthToGoal; // return depth of (losing) goal leaf
	}
	
	/* Insert words from dictionary into a trie. */
	static Trie createTrie(String filename) {
		Trie trie = new Trie();
		
		try {
			BufferedReader buf = new BufferedReader(new FileReader(filename));
			String line = null;
			while ((line = buf.readLine()) != null) {
				trie.insertWord(line);
			}
		} catch (IOException x) {
			System.out.println("Invalid pathname.");
		}
		
		return trie;
	}
	
	public static void main(String[] args) {
		Trie trie = createTrie("WORD.LST.txt");
		solveTrie(trie);
		Game game = new Game(trie);
		game.begin();
	}
}

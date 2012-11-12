//
//  Ghost.java
//  
//
//  Created by Ryan Barril on 10/26/12.
//  Copyright 2012 __MyCompanyName__. All rights reserved.
//

import java.io.*;
import java.util.*;

public class Ghost {
	
	static class Trie {
		Node root;
		
		Trie() {
			root = new Node(null, 0);
		}
		
		void insertWord(String word) {
			root.insertWord(word);
		}
	}
	
	static class Node {
		Map<Character, Node> children;
		boolean terminal; // indicates if this node represents a char ending a word (of length >= 4), i.e. a leaf
		int depth; // distance from root
		int maxDepth; // depth of lowest leaf in tree rooted at this node
		boolean compGoal;
		int depthOfUserGoal; // depth of lowest user goal in sub-tree rooted at this node
		int depthOfCompGoal; // depth of lowest user goal in sub-tree rooted at this node
		List<Node> compGoals; // list of children designated computer (P2) goals
		List<Node> userGoals; // list of children designated user (P1) goals
		List<Node> maxLosingPaths; // holds children node(s) that could be taken by losing player to force maximal game length
		Character myChar;
		
		Node(Character myChar, int depth) {
			children = new HashMap<Character, Node>();
			terminal = false;
			this.depth = depth;
			maxDepth = depth;
			compGoal = (depth % 2 == 0);
			int depthOfUserGoal = -1;
			int depthOfCompGoal = -1;
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
			}
			else {
				children.get(word.charAt(0)).insertWord(word.substring(1));
			}
			
			maxDepth = (maxDepth > depth + word.length()) ? maxDepth : depth + word.length();
		}
		
		void addChild(Character c, Node node) {
			children.put(c, node);
		}
	}
	
	static class Game {
		User user;
		Comp comp;
		Node triePtr;
		int winner;
		int move;
		String currMover;
		String currWord;
		boolean human;
		
		Game(Trie trie) {
			user = new User();
			comp = new Comp();
			triePtr = trie.root;
			winner = -1;
			move = 1;
			currMover = "User";
			currWord = "";
			human = false;
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
	
	// the computer first tries to choose a computer goal node. If there aren't any, choose among paths that force maximal game length.
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
	
	// sets whether this node is a computer (P2) or user (P1) goal
	static void solveTrie(Node node) {
		boolean isEvenNode = (node.depth % 2 == 0);
		
		if (node.children.isEmpty()) {
			if (node.depth >= 4) // only count 4+ length strings as goals
				node.compGoal = !isEvenNode;
			
			if (isEvenNode) node.depthOfUserGoal = node.depth;
			else node.depthOfCompGoal = node.depth;
			
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
					node.depthOfUserGoal = (node.depthOfUserGoal > n.depthOfUserGoal) ? 
					node.depthOfUserGoal : n.depthOfUserGoal;
					node.userGoals.add(n);
				}
				else {
					node.compGoals.add(n);
					
					// if the user cannot choose a user goal, he chooses the (computer-winning) path(s) that forces the maximal game length
					// node.maxLosingPaths holds these path(s) and is updated here
					updateMaxLosingPaths(node, n, "compGoals");
				}
			}
			else { // it is the computer's turn
				if (n.compGoal) {
					// if there is a computer goal to be chosen, this node is a computer goal as well
					// if no computer goals can be chosen, this node is a user goal
					node.compGoal = true;
					node.depthOfCompGoal = (node.depthOfCompGoal > n.depthOfCompGoal) ?
					node.depthOfCompGoal : n.depthOfCompGoal;
					node.compGoals.add(n);
				}
				else {
					node.userGoals.add(n);
					
					// if the computer cannot choose a computer goal, he chooses the (user-winning) path(s) that forces the maximal game length
					// node.maxLosingPaths holds these path(s) and is updated here
					updateMaxLosingPaths(node, n, "userGoals");
				}
			}
		}		
	}
	
	// update paths a losing player could take at node to force maximal game length
	static void updateMaxLosingPaths(Node node, Node n, String type) {
		boolean typeComp = type.equals("compGoals");
		List<Node> goals = (typeComp) ? n.compGoals : n.userGoals;
		
		if (node.maxLosingPaths.size() == 0) {
			node.maxLosingPaths.add(n);
			updateDepthOfGoal(node, n, type);
		}
		else if (goals.size() != 0) {
			int numGoalsOfChild = (typeComp) ?
			node.maxLosingPaths.get(0).compGoals.size() : node.maxLosingPaths.get(0).userGoals.size();
			
			int nodeDepthOfGoal = (typeComp) ? node.depthOfCompGoal : node.depthOfUserGoal;
			int nDepthOfGoal = (typeComp) ? n.depthOfCompGoal : n.depthOfUserGoal;
			
			if (goals.size() == numGoalsOfChild) {
				// enter this branch if another node has an equal number of computer-winning paths
				// if depth of other node is bigger, replace old node. if equal, join with old node. if smaller, ignore.
				if (nDepthOfGoal > nodeDepthOfGoal) {
					node.maxLosingPaths.clear();
					node.maxLosingPaths.add(n);
					updateDepthOfGoal(node, n, type);
				}
				else if (nDepthOfGoal == nodeDepthOfGoal)
					node.maxLosingPaths.add(n);
			}
			else if (goals.size() > numGoalsOfChild) {
				// n has a larger number of computer-winning paths. replace old paths if n's paths are all longer than old paths.
				if (allLongerPaths(n, node.maxLosingPaths, type)) {
					node.maxLosingPaths.clear();
					node.maxLosingPaths.add(n);
					updateDepthOfGoal(node, n, type);			
				}
			}
			else {
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
	
	static void updateDepthOfGoal(Node node, Node n, String type) {
		if (type.equals("compGoals")) node.depthOfCompGoal = n.depthOfCompGoal;
		else node.depthOfUserGoal = n.depthOfUserGoal;
	}
	
	// returns true if the goals of n have a greater than or equal depthOfGoal then the goals of every node in list
	static boolean allLongerPaths(Node n, List<Node> list, String type) {
		boolean typeComp = type.equals("compGoals");
		
		for (Node k : list) {
			List<Node> kGoalList = (typeComp) ? k.compGoals : k.userGoals;
			List<Node> nGoalList = (typeComp) ? n.compGoals : n.userGoals;
			for (Node kGoal : kGoalList)
				for (Node nGoal : nGoalList) {
					int kGoalDepthOfGoal = (typeComp) ? kGoal.depthOfCompGoal : kGoal.depthOfUserGoal;
					int nGoalDepthOfGoal = (typeComp) ? nGoal.depthOfCompGoal : nGoal.depthOfUserGoal;
					if (kGoalDepthOfGoal > nGoalDepthOfGoal)
						return false;
				}
		}
		return true;
	}
	
	// returns true if the goals of every node in list have a greater than or equal depthOfGoal than the goals of n
	static boolean allLongerPaths(List<Node> list, Node n, String type) {	
		boolean typeComp = type.equals("compGoals");
		for (Node k : list) {
			List<Node> kGoalList = (typeComp) ? k.compGoals : k.userGoals;
			List<Node> nGoalList = (typeComp) ? n.compGoals : n.userGoals;
			for (Node kGoal : kGoalList)
				for (Node nGoal : nGoalList) {
					int nGoalDepthOfGoal = (typeComp) ? nGoal.depthOfCompGoal : nGoal.depthOfUserGoal;
					int kGoalDepthOfGoal = (typeComp) ? kGoal.depthOfCompGoal : kGoal.depthOfUserGoal;
					if (nGoalDepthOfGoal > kGoalDepthOfGoal)
						return false;
				}
		}
		return true;
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

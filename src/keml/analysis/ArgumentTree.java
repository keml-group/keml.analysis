package keml.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import keml.Literal;

/**
 * Tree-like data structure to facilitate the construction of Argument Trees. 
 * mainly used for undercuts between {@link LogicArgument}s.
 */
public class ArgumentTree {
	/** root {@link LogicArgument} of the current tree*/
	LogicArgument root; 
	/** List of children of the root, that themselves are other trees*/
	List<ArgumentTree> children; 
	
	/**
	 * Constructor for {@link ArgumentTree}
	 * @param root {@link LogicArgument}
	 */
	public ArgumentTree(LogicArgument root) {
		this.root = root;
		this.children = new ArrayList<>();
	}
	
	
	/**
	 * adds a child to this tree
	 * @param at {@link ArgumentTree}
	 */
	public void addChild (ArgumentTree at) {
		this.children.add(at);
	}

	
	/**
	 * getter for the root of this tree
	 * @return {@link ArgumentTree#root}
	 */
	public LogicArgument getRoot() {
		return root;
	}

	
	/**
	 * getter for the children of this root
	 * @return {@link ArgumentTree#children}
	 */
	public List<ArgumentTree> getChildren() {
		return children;
	}
	
	
	/**
	 * a <b>static</b> method that creates a String representation of a given {@link ArgumentTree}
	 * @param node {@link ArgumentTree} to be represented
	 * @param prefix String prefix to facilitate recursive call. Should be empty ("") during first call
	 * @param literals2String Map of literals and their associated String symbol. See {@link LAFConversationAnalyser#literals2String}
	 * @return String representation of a given tree
	 */
	public static String printTree(ArgumentTree node, String prefix, Map<Literal, String> literals2String) {
	    StringBuilder tree = new StringBuilder();
	    printTreeHelper(node, prefix, literals2String, tree);
	    return tree.toString();
	}
	
	
	/**
	 * Helper method for {@link ArgumentTree#printTree} to facilitate recursive calls.
	 * @param  node {@link ArgumentTree} to be represented
	 * @param prefix String prefix to facilitate recursive call.
	 * @param literals2String Map of literals and their associated String symbol. See {@link LAFConversationAnalyser#literals2String}
	 * @param tree {@link StringBuilder} of the tree constructed so far.
	 */
	private static void printTreeHelper(ArgumentTree node, String prefix, Map<Literal, String> literals2String, StringBuilder tree) {
	    tree.append(prefix).append("└── ").append(LogicArgument.asString(node.getRoot(), literals2String)).append("\n");
	    
	    for (int i = 0; i < node.getChildren().size(); i++) {
	        printTreeHelper(node.getChildren().get(i), prefix + "    ", literals2String, tree);
	    }
	}

}

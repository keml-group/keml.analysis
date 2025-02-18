package keml.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import keml.Literal;

public class ArgumentTree {
	
	LogicArgument root;
	List<ArgumentTree> children;
	
	public ArgumentTree(LogicArgument root) {
		this.root = root;
		this.children = new ArrayList<>();
	}
	
	public void addChild (ArgumentTree at) {
		this.children.add(at);
	}

	public LogicArgument getRoot() {
		return root;
	}

	public List<ArgumentTree> getChildren() {
		return children;
	}
//	
//    public static String printTree(ArgumentTree node, String prefix, Map<Literal, String> literals2String, String tree) {
//    	tree += prefix + "└── " + LogicArgument.asString(node.root, literals2String);
//        for (int i = 0; i < node.children.size(); i++) {
//            tree += printTree(node.children.get(i), prefix + "    ", literals2String, tree);
//        }
//        
//        return tree;
//    }

	public static String printTree(ArgumentTree node, String prefix, Map<Literal, String> literals2String) {
	    StringBuilder tree = new StringBuilder();
	    printTreeHelper(node, prefix, literals2String, tree);
	    return tree.toString();
	}

	private static void printTreeHelper(ArgumentTree node, String prefix, Map<Literal, String> literals2String, StringBuilder tree) {
	    tree.append(prefix).append("└── ").append(LogicArgument.asString(node.getRoot(), literals2String)).append("\n");
	    
	    for (int i = 0; i < node.getChildren().size(); i++) {
	        printTreeHelper(node.getChildren().get(i), prefix + "    ", literals2String, tree);
	    }
	}

}

package keml.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import keml.Information;
import keml.Junction;
import keml.Literal;
import keml.LogicExpression;
import keml.PreKnowledge;
/**
 * Utility class containing a myriad of <b>static</b> methods that help the analysis of LAF keml models
 */
public class LogicUtilities {

	/**
	 * <b>static</b> method that creates a map of rebuttals of each claim {@link Literal} for a list of {@link LogicArgument}s 
	 * @param logicArguments target map of literals and their list of arguments
	 * @return Map of literals and logic arguments that rebut them
	 */
	public static Map<Literal, List<LogicArgument>> createRebuttalsMap(Map<Literal, List<LogicArgument>> logicArguments) {
		Map<Literal, List<LogicArgument>> rebuttals = new HashMap<>();
		for (Literal l : logicArguments.keySet()) {
			Literal counterClaim = findCounterLiteral(l); //negated the claim
			rebuttals.put(l, logicArguments.get(counterClaim)); //find the arguments for the negated claim
		}
		return rebuttals;
	}
	
	
	/**
	 * <b>static</b> method that creates a map of undercuts of each {@link LogicArgument} for a list of arguments 
	 * @param logicArguments target map of {@link Literal}s and their list of arguments
	 * @return Map of logic arguments and their {@link ArgumentTree}, where children undercut their parent node 
	 */
	public static Map<LogicArgument, ArgumentTree> createUndercutTreesMap(Map<Literal, List<LogicArgument>> logicArguments) {
		Map<LogicArgument, ArgumentTree> undercutTrees = new HashMap<>();
		//create empty trees for each argument of each literal
		for (List<LogicArgument> args : logicArguments.values()) {
			for (LogicArgument arg : args) {
				ArgumentTree at = new ArgumentTree(arg);
				undercutTrees.put(arg, at);
			}
		}
		// connect the created trees whenever there exists an undercut relation between them
		for (LogicArgument arg : undercutTrees.keySet())
			undercutTrees = findUndercuts(arg, undercutTrees, logicArguments);
		
		return undercutTrees;
	}
	
	
	/**
	 * <b>static</b> method that finds all undercuts for a given {@link LogicArgument} by connecting undercut trees
	 * @param la target argument
	 * @param undercutTrees Map of logic arguments and their corresponding {@link ArgumentTree}
	 * @param logicArguments Map of {@link Literal} and their logic arguments
	 * @return undercutTrees after connecting trees that have undercut relations
	 */
	private static Map<LogicArgument, ArgumentTree> findUndercuts(LogicArgument la, Map<LogicArgument, ArgumentTree> undercutTrees, Map<Literal, List<LogicArgument>> logicArguments) {
		ArgumentTree at = undercutTrees.get(la); // root node
		List<LogicExpression> premises = la.getPremises(); // premises of root
		
		Literal counterLiteral; //negated root
		
		for (LogicExpression premise : premises) {
			if (premise instanceof Literal) {
				counterLiteral = findCounterLiteral((Literal) premise);
				if (logicArguments.containsKey(counterLiteral))  {// if counter literal has argument(s) then it undercuts the root argument!
					for (LogicArgument undercutter : logicArguments.get(counterLiteral)) {
						if (!undercutter.getClaim().equals(la.getClaim()))
						at.addChild(undercutTrees.get(undercutter));
					}
				}
				
			} else { // if premises contain junctions
				for (LogicExpression junctionContent : ((Junction) premise).getContent()) {
					// TODO this currently doesn't handle nested junctions
					counterLiteral = findCounterLiteral((Literal) junctionContent); 
					if (logicArguments.containsKey(counterLiteral)) // if counter literal has argument(s) then it undercuts the root argument!
						for (LogicArgument undercutter : logicArguments.get(counterLiteral)) {
							at.addChild(undercutTrees.get(undercutter));
						}
				}
			}
		}
		return undercutTrees;
	}
	
	
	/**
	 * <b>static</b> method that finds the negated version of a given {@link Literal}
	 * @param l target literal
	 * @return negated version of target literal
	 */
	private static Literal findCounterLiteral(Literal l) {
		Information sourceInfo = l.getSource();
		
		if (l.isNegated())
			return sourceInfo.getAsLiterals().get(0); //non-negated are stored first during parsing
		else 
			return sourceInfo.getAsLiterals().get(1); //negated are stored last during parsing
	}
	
	
	/**
	 * <b>static</b> method that computes the hCategorizer value for a given {@link ArgumentTree}.
	 * @param at target argument tree
	 * @return float categorisation value
	 */
	public static float hCategorizer(ArgumentTree at) {

	    List<ArgumentTree> incomingEdges = at.getChildren();
	    if (incomingEdges.isEmpty()) //if no children
	        return 1.0f; 

	    float sum = 0.0f; 
	    for (ArgumentTree edge : incomingEdges) {
	        sum += hCategorizer(edge); // recursively find hCat value for children
	    }

	    return 1.0f / (1.0f + sum); 
	}
	
	
	/**
	 * <b>static</b> method that computes the hCategorizer value for a flat tree.
	 * <p> this method is mainly used to compute values of rebuttal trees </p>
	 * @param numberOfChildren int number of children the root of this flat tree has
	 * @return float categorisation value
	 */
	public static float flatTreeHCategorizer(int numberOfChildren) {
		
		return 1.0f/(1.0f + (float) numberOfChildren);
	}
	
	
	/**
	 * b>static</b> method that computes the logAccumulator value given a list of hCategorisations for
	 * a given argument and one against the argument.
	 * @param forCategorizations List of categorisation values for a given argument
	 * @param againstCategorizations List of categorisation values against a given argument
	 * @return float log-accumulated value
	 */
	public static float logAccumulator(List<Float> forCategorizations, List<Float>againstCategorizations) {
		// if the claim has no arguments for it or against
	    if (forCategorizations.isEmpty() && againstCategorizations.isEmpty())
	        return 0.0f;  

	    float forSum = 1.0f; 
	    float againstSum = 1.0f; 

	    for (float value : forCategorizations) {
	        forSum += Math.abs(value); 
	    }

	    for (float value : againstCategorizations) {
	        againstSum += Math.abs(value);
	    }

	    double logResult = Math.log(forSum) - Math.log(againstSum);

	    return (float) logResult; 
	}
}

package keml.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import keml.Information;
import keml.Junction;
import keml.Literal;
import keml.LogicExpression;

public class LogicUtilities {

	
	public static Map<Literal, List<LogicArgument>> createRebuttalsMap(Map<Literal, List<LogicArgument>> logicArguments) {
		Map<Literal, List<LogicArgument>> rebuttals = new HashMap<>();
		for (Literal l : logicArguments.keySet()) {
			Literal counterClaim = findCounterLiteral(l);
			rebuttals.put(l, logicArguments.get(counterClaim));
		}
		
		return rebuttals;

	}
	
	public static Map<LogicArgument, ArgumentTree> createUndercutTreesMap(Map<Literal, List<LogicArgument>> logicArguments) {
		Map<LogicArgument, ArgumentTree> undercutTrees = new HashMap<>();
		for (Map.Entry<Literal, List<LogicArgument>> entry : logicArguments.entrySet()) {
			Literal l = entry.getKey();
			List<LogicArgument> args = entry.getValue();
			
			for (LogicArgument arg : args) {
				ArgumentTree at = new ArgumentTree(arg);
				undercutTrees.put(arg, at);
			}
		}
		
		for (LogicArgument arg : undercutTrees.keySet())
			undercutTrees = findUndercuts(arg, undercutTrees, logicArguments);
		
		return undercutTrees;
		
	}
	
	private static Map<LogicArgument, ArgumentTree> findUndercuts(LogicArgument la, Map<LogicArgument, ArgumentTree> undercutTrees, Map<Literal, List<LogicArgument>> logicArguments) {
		ArgumentTree at = undercutTrees.get(la);
		
		List<LogicExpression> premises = la.getPremises(); // premises of the argument
		Literal counterLiteral;
		
		for (LogicExpression premise : premises) {
			if (premise instanceof Literal) {
				counterLiteral = findCounterLiteral((Literal) premise);
				if (logicArguments.containsKey(counterLiteral))  {// if counter literal has argument(s)
					for (LogicArgument undercutter : logicArguments.get(counterLiteral)) {
						if (!undercutter.getClaim().equals(la.getClaim()))
						at.addChild(undercutTrees.get(undercutter));
					}
				}
				
			} else {
				for (LogicExpression junctionContent : ((Junction) premise).getContent()) {
					// TODO what if junction within junction?
					counterLiteral = findCounterLiteral((Literal) junctionContent); 
					if (logicArguments.containsKey(counterLiteral)) // if counter literal has argument(s)
						for (LogicArgument undercutter : logicArguments.get(counterLiteral)) {
							at.addChild(undercutTrees.get(undercutter));
						}
				}
			}
		}
		
		return undercutTrees;
	}
	
	private static Literal findCounterLiteral(Literal l) {
		//return the negation of this literal
		Information sourceInfo = l.getSource();
		
		if (l.isNegated())
			return sourceInfo.getAsLiteral().get(0);
		else 
			return sourceInfo.getAsLiteral().get(1);
	}
	
	public static double hCategorizer(ArgumentTree at) {

		List<ArgumentTree> incomingEdges = at.getChildren();
		if (incomingEdges.isEmpty())
			return 1.0;
		double sum = 1.0;
		for (ArgumentTree edge : incomingEdges) {
            sum += hCategorizer(edge);
        }
	
		return 1/(sum);
	}
	
	public static double flatTreeHCategorizer(double numberOfChildren) {
		
		return 1/(1 + numberOfChildren);
	}
	
	public static double logAccumulator(List<Double> forCategorizations, List<Double>againstCategorizations) {
		if (forCategorizations.isEmpty() && againstCategorizations.isEmpty())
			return 0.0;
		else if (againstCategorizations.isEmpty())
			return 1.0;
		else if (forCategorizations.isEmpty())
			return -1.0;
		
		double forSum = 1;
		double againstSum = 1;
		double result = 0;
		for (double value : forCategorizations) {
			forSum += value;
		}
		
		for (double value : againstCategorizations) {
			againstSum += value;
		}
		result += Math.log(forSum);
		result -= Math.log(againstSum);
		
		return result;
		
	}
}

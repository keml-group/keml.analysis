package keml.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import keml.Conversation;
import keml.Information;
import keml.Literal;
import keml.LogicExpression;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;




public class ArgumentationStructurer {

	
	
	// TODO we don't handle cycles in the model yet (e.g., a => not b, b => not a), which breaks analysis
	// TODO analysis output: All arguments, undercut trees, rebuttals, Cat and Accu values
	// TODO validate SNegated_Implication with use case
	// 
	
	
	Conversation conv;
	List<String> partners;
	List<ReceiveMessage> receives;  
	List<NewInformation> newInfos;
	List<PreKnowledge> preKnowledge;
	HashSet<Information> allInfo;
	
//	Map<Literal, List<LogicExpression>> logicArguments = new HashMap<>();
	Map<LogicArgument, ArgumentTree> undercutTrees = new HashMap<>();
	Map<Literal, List<LogicArgument>> rebuttals = new HashMap<>();
	
	Map<Literal, List<LogicArgument>> logicArguments = new HashMap<>();
	Map<Literal, String> literals2String = new LinkedHashMap<>();

	
	
	public ArgumentationStructurer(Conversation conv) {
		this.conv = conv;
		this.partners = ConversationAnalyser.getPartnerNames(conv); 
		this.receives = ConversationAnalyser.getReceives(conv);
		this.newInfos = ConversationAnalyser.getNewInfos(receives);
		this.preKnowledge = conv.getAuthor().getPreknowledge();
		this.allInfo  = Stream.concat(preKnowledge.stream(), newInfos.stream()).collect(Collectors.toCollection(HashSet::new));
		
		mapLiteralsToStrings();
		buildLogicArguments();
		undercutTrees = LogicUtilities.createUndercutTreesMap(logicArguments);
		rebuttals = LogicUtilities.createRebuttalsMap(logicArguments);
		keml2Logic();
	}
	

//	
	public void buildLogicArguments() {

			
		for (Information i : allInfo) {
			for (Literal l : i.getAsLiteral()) {
				List<LogicArgument> args = new ArrayList<>(); 
				
//				// Preknowledge asserts itself as axiom
//				if (i instanceof PreKnowledge && !l.isNegated()) {
//					LogicArgument newArg = new LogicArgument(l);
//					newArg.addPremise(l);
//					args.add(newArg);
//				}
				
				for (LogicExpression le : l.getPremises()) {
					LogicArgument newArg = new LogicArgument(l);
					newArg.addPremise(le);
					args.add(newArg);
				}
	
				logicArguments.put(l, args);
			}
		}
		
	}
	

	public void mapLiteralsToStrings() {
		String symbol = "L";
		int i = 0;
		for (Information inf : allInfo) {
			for (Literal l : inf.getAsLiteral()) 
				literals2String.put(l, (l.isNegated() ? "!" : "") + symbol + i);
			i++;
		}
			
	}
	
	public void keml2Logic() {

		
		debugInfos();
		debugArguments();
//		printUndercuts();
//		printRebuttals();
		testAccumulator();
//		testCat();
//		testAccu();
		
		
//		debugCatNAccu();

	}
	

	public void debugInfos() {

		for (Map.Entry<Literal, String> entry : literals2String.entrySet()) {
			if (!entry.getKey().isNegated())
				System.out.println(entry.getValue() + ": " + entry.getKey().getSource().getMessage());
		}
	}
	

	public void debugArguments() {
		for (List<LogicArgument> args : logicArguments.values()) {
			for (LogicArgument arg : args) {
				System.out.println(LogicArgument.asString(arg, literals2String));
			}
		}
	}
	
	public void printUndercuts() {
		for (Map.Entry<LogicArgument, ArgumentTree> entry : undercutTrees.entrySet()) {
			LogicArgument arg = entry.getKey();
			ArgumentTree at = entry.getValue();
			System.out.println("Undercuts for " + LogicArgument.asString(arg, literals2String) + " [hCategorizer value : " +  LogicUtilities.hCategorizer(at) + "]");
			System.out.println(ArgumentTree.printTree(at, "", literals2String));

		}
	}
	
	public void printRebuttals() {
		for (Map.Entry<Literal, List<LogicArgument>> entry : rebuttals.entrySet()) {
			Literal l = entry.getKey();
			List<LogicArgument> rebuttals = entry.getValue();
			System.out.println("Rebuttals for " + literals2String.get(l) + " [hCategorizer value : " + LogicUtilities.flatTreeHCategorizer(rebuttals.size()) + "]");
			for (LogicArgument rebutter : rebuttals) 
				System.out.println(LogicArgument.asString(rebutter, literals2String));

		}
	}
	
	public void testAccumulator() {
		for (Information i : allInfo) {
			Literal l = i.getAsLiteral().getFirst();
			Literal negated = i.getAsLiteral().getLast();
			List<Double> forCat = new ArrayList<>();
			List<Double> againstCat = new ArrayList<>();
			
			// rebuttals
			if (i instanceof PreKnowledge) {
				for (LogicArgument arg : rebuttals.get(negated))
					System.out.println(LogicArgument.asString(arg, literals2String));
			}
			forCat.add(LogicUtilities.flatTreeHCategorizer(rebuttals.get(l).size()));
			againstCat.add(LogicUtilities.flatTreeHCategorizer(rebuttals.get(negated).size()));
			System.out.println("REBUTTALS hCat for: " + i.getMessage() + " [+: " + forCat + " | -: " + againstCat + "]");

			System.out.println("Acc result: " + LogicUtilities.logAccumulator(forCat, againstCat));
			
			// undercuts
			forCat.clear();
			againstCat.clear();
			for (LogicArgument arg : logicArguments.get(l)) {
				forCat.add(LogicUtilities.hCategorizer(undercutTrees.get(arg)));
			}
			for (LogicArgument arg : logicArguments.get(negated)) {
				againstCat.add(LogicUtilities.hCategorizer(undercutTrees.get(arg)));
			}
			System.out.println("UNDERCUTS hCats for: " + i.getMessage() + " [+: " + forCat + " | -: " + againstCat + "]");
			System.out.println("Acc result: " + LogicUtilities.logAccumulator(forCat, againstCat));
		}
	}

}
package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import keml.Conversation;
import keml.Information;
import keml.Literal;
import keml.LogicExpression;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;




public class ArgumentationStructurer {

	
	
	// TODO we don't handle cycles in the model yet (e.g., a => not b, b => not a), which breaks analysis
	// TODO Analysis Output for the Trees and Cats
	// TODO improve readability of analysis output
	// TODO validate SNegated_Implication with use case
	// 
	
	
	Conversation conv;
	List<String> partners;
	List<ReceiveMessage> receives;  
	List<NewInformation> newInfos;
	List<PreKnowledge> preKnowledge;
	HashSet<Information> allInfo;
	
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
	

	
	public void buildLogicArguments() {

			
		for (Information i : allInfo) {
			for (Literal l : i.getAsLiteral()) {
				List<LogicArgument> args = new ArrayList<>(); 
				
				// Preknowledge asserts itself as axiom
				if (i instanceof PreKnowledge && !l.isNegated()) {
					LogicArgument newArg = new LogicArgument(l);
					newArg.addPremise(l);
					args.add(newArg);
				}
				
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
				literals2String.put(l, (l.isNegated() ? "¬" : "") + symbol + i);
			i++;
		}
			
	}
	
	public void keml2Logic() {

		
		debugInfos();
		debugArguments();
//		printUndercuts();
//		printRebuttals();

		

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
	
	public HashMap<Information, Float> undercutsAccumulator() {
		HashMap<Information, Float> result = new HashMap<>();
		for (Information i : allInfo) {
			Literal l = i.getAsLiteral().getFirst();
			Literal negated = i.getAsLiteral().getLast();
			List<Float> forCat = new ArrayList<>();
			List<Float> againstCat = new ArrayList<>();


			for (LogicArgument arg : logicArguments.get(l)) 
				forCat.add(LogicUtilities.hCategorizer(undercutTrees.get(arg)));
		

			for (LogicArgument arg : logicArguments.get(negated)) 
				againstCat.add(LogicUtilities.hCategorizer(undercutTrees.get(arg)));

			result.put(i, LogicUtilities.logAccumulator(forCat, againstCat));
		}
		return result;
	}
	
	public HashMap<Information, Float> rebuttalsAccumulator() {
		HashMap<Information, Float> result = new HashMap<>();
		List<Float> forCat = new ArrayList<>();
		List<Float> againstCat = new ArrayList<>();
		for (Information i : allInfo) {
			Literal l = i.getAsLiteral().getFirst();
			Literal negated = i.getAsLiteral().getLast();

			forCat = Arrays.asList(LogicUtilities.flatTreeHCategorizer(rebuttals.get(l).size()));
			againstCat = Arrays.asList(LogicUtilities.flatTreeHCategorizer(rebuttals.get(negated).size()));


			result.put(i, LogicUtilities.logAccumulator(forCat, againstCat));
		}
		return result;
	}
	
	public void scoresMatrix(String path) throws IOException {
		LAFWorkbookController wbc = new LAFWorkbookController();
		wbc.initializeForLAF(newInfos, preKnowledge, literals2String, logicArguments);
		wbc.addTrustsForLAF(undercutsAccumulator(), "Undercuts");
		wbc.addTrustsForLAF(rebuttalsAccumulator(), "Rebuttals");
		wbc.write(path);

	}
	
	
	public void writeLogicArgumentationCSV(String path) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			writeInformationConnections(csvPrinter);
			csvPrinter.flush();
		}
        System.out.println("Wrote logic arguments to " + path);
	}
	
	private List<List<String>> findLiteralMessagePairs(String partner, boolean instructions) {
		List<List<String>> literalMessagePairs = new ArrayList<>();
		
		for (Information i : allInfo) {
			List<String> partnerInfo = new ArrayList<>();
			if (partner == null && i instanceof PreKnowledge) {
				
				partnerInfo.add(literals2String.get(i.getAsLiteral().getFirst()));
				partnerInfo.add(i.getMessage());
			} 
			if (partner != null 
					&& i instanceof NewInformation 
					&& ((NewInformation) i).getSource().getCounterPart().getName().equals(partner)) {
				
				partnerInfo.add(literals2String.get(i.getAsLiteral().getFirst()));
				partnerInfo.add(i.getMessage());
				
			}
			if (!partnerInfo.isEmpty() && instructions)
				if (i.isIsInstruction())
					literalMessagePairs.add(partnerInfo);
			else
				literalMessagePairs.add(partnerInfo);
		}
		
		return literalMessagePairs;
		
	}
	
	private void writeInformationConnections(CSVPrinter csvPrinter) throws IOException {
		String[] columns = new String[] {"Literal", "Message"};
		List<List<String>> facts = new ArrayList<>();
		List<List<String>> instructions = new ArrayList<>();
		for (String partner : partners) {
			facts = findLiteralMessagePairs(partner, false);
			instructions = findLiteralMessagePairs(partner, true);
			
			if (!facts.isEmpty()) {
				csvPrinter.printRecord(partner + " Facts");
				csvPrinter.printRecord((Object[]) columns);
				for (List<String> literalFactPair : facts) {
					csvPrinter.printRecord(literalFactPair);
				}
				facts.clear();
				csvPrinter.printRecord();
			}

			if (!instructions.isEmpty()) {
				csvPrinter.printRecord(partner + " Instructions");
				csvPrinter.printRecord((Object[]) columns);
				for (List<String> literalFactPair : instructions) {
					csvPrinter.printRecord(literalFactPair);
				}
				instructions.clear();
				//
				csvPrinter.printRecord();
			}


		}
		
		facts = findLiteralMessagePairs(null, false);
		instructions = findLiteralMessagePairs(null, true);
		
		if (!facts.isEmpty()) {
			csvPrinter.printRecord("Author PreKnowledge Facts");
			csvPrinter.printRecord((Object[]) columns);
			for (List<String> literalFactPair : facts) {
				csvPrinter.printRecord(literalFactPair);
			}
			csvPrinter.printRecord();
		}
		
		if (!instructions.isEmpty()) {
			csvPrinter.printRecord("Author PreKnowledge Instructions");
			csvPrinter.printRecord((Object[]) columns);
			for (List<String> literalFactPair : instructions) {
				csvPrinter.printRecord(literalFactPair);
			}
			//
			csvPrinter.printRecord();
		}
		
		csvPrinter.printRecord("Logic Arguments (<{premises} claim>)");
		csvPrinter.printRecord((Object[]) new String[] {"Claim", "Argument"});
		for (Map.Entry<Literal, List<LogicArgument>> entry : logicArguments.entrySet()) {
			Literal claim = entry.getKey();
			List<LogicArgument> args = entry.getValue();
			for (LogicArgument arg : args) {
				csvPrinter.printRecord((Object[]) new String[]{
						literals2String.get(claim),
						LogicArgument.asString(arg, literals2String)
				});
			}
			
		}

	}

}
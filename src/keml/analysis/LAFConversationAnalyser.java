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

/**
 * This class acts as the main facilitator for LAF-related analysis.
 * <p>This class supports the following functionalities:</p>
 * <ul>
 *     <li>Mapping unique String symbols to each information piece</li>
 *     <li>Deriving logic arguments (<{premises}, claim> pairs) based on the parsed knowledge base in {@link Conversation}</li>
 *     <li>Construction of Undercut trees through {@link ArgumentTree}</li>
 *     <li>Construction of Rebuttal relations between logic argument instances</li>
 *     <li>Computation of hCategorisation and logAccumulation values for Undercut or Rebuttal trees</li>
 *     <li>Serialization of analysis results in suitable csv or xlsx files</li>
 * </ul>
 */
public class LAFConversationAnalyser {
	/** the target {@link Conversation} instance*/
	Conversation conv;
	/** List of conversation partners in {@link LAFConversationAnalyser#conv}*/
	List<String> partners;
	/** List of messages received in {@link LAFConversationAnalyser#conv}*/
	List<ReceiveMessage> receives;  
	/** List of {@link NewInformation} instances in {@link LAFConversationAnalyser#conv}*/
	List<NewInformation> newInfos;
	/** List of {@link PreKnowledge} instances in {@link LAFConversationAnalyser#conv}*/
	List<PreKnowledge> preKnowledge;
	/** Set of all  {@link Information} instances in {@link LAFConversationAnalyser#conv}*/
	HashSet<Information> allInfo;
	
	/** Undercuts Map of {@link LogicArgument} to {@link ArgumentTree}*/
	Map<LogicArgument, ArgumentTree> undercutTrees = new HashMap<>();
	/** Rebuttal Map of {@link Literal} to list of {@link LogicArgument}*/
	Map<Literal, List<LogicArgument>> rebuttals = new HashMap<>();
	/** Logic argument Map of {@link Literal} to list of related {@link LogicArgument}*/
	Map<Literal, List<LogicArgument>> logicArguments = new HashMap<>();
	/** Map of {@link Literal} instances and their associated String symbol*/
	Map<Literal, String> literals2String = new LinkedHashMap<>();

	
	/**
	 * Constructor for {@link LAFConversationAnalyser}
	 * @param conv
	 */
	public LAFConversationAnalyser(Conversation conv) {
		this.conv = conv;
		this.partners = ConversationAnalyser.getPartnerNames(conv); 
		this.receives = ConversationAnalyser.getReceives(conv);
		this.newInfos = ConversationAnalyser.getNewInfos(receives);
		this.preKnowledge = conv.getAuthor().getPreknowledge();
		this.allInfo  = Stream.concat(preKnowledge.stream(), newInfos.stream()).collect(Collectors.toCollection(HashSet::new));
		
		mapLiteralsToStrings(); //map the literals to a unique string symbol
		buildLogicArguments(); // derive logic arguments from parsed associations in the keml KB
		undercutTrees = LogicUtilities.createUndercutTreesMap(logicArguments); // create undercut trees for each logic argument
		rebuttals = LogicUtilities.createRebuttalsMap(logicArguments); // find rebuttals of each claim

	}
	

	/**
	 * This method creates instances of {@link LogicArgument} based on the literals
	 * of {@link Information} pieces and whether they have premises. It populates {@link LAFConversationAnalyser#logicArguments}.
	 * <p>This method additionally treats instances of {@link PreKnowledge} as axioms
	 * (i.e., there is an argument for each pre-knowledge, where the claim and the premise are the pre-knowledge instance).</p>
	 */
	private void buildLogicArguments() {
		//iterate all information pieces
		for (Information i : allInfo) {
			//for each literal representation
			for (Literal l : i.getAsLiterals()) {
				//create a list of logic arguments for the literal
				List<LogicArgument> args = new ArrayList<>(); 
				
				// Preknowledge asserts itself as axiom
				if (i instanceof PreKnowledge && !l.isNegated()) {
					LogicArgument newArg = new LogicArgument(l);
					newArg.addPremise(l);
					args.add(newArg);
				}
				
				// if the literal has premises, create logic arguments for each one
				for (LogicExpression le : l.getPremises()) {
					LogicArgument newArg = new LogicArgument(l);
					newArg.addPremise(le);
					args.add(newArg);
				}

				logicArguments.put(l, args);
			}
		}
		
	}
	
	
	/**
	 * This method assigns a unique String symbol to each {@link Information} piece
	 * to facilitate an easier handling of logic arguments. It populates {@link LAFConversationAnalyser#literals2String}.
	 */
	private void mapLiteralsToStrings() {
		String symbol = "L";
		int i = 0;
		for (Information inf : allInfo) {
			for (Literal l : inf.getAsLiterals()) 
				literals2String.put(l, (l.isNegated() ? "¬" : "") + symbol + i);
			i++;
		}
			
	}
	
	
	/**
	 * Computes the hCategorizer values for all {@link Information} instances of this 
	 * class's {@link LAFConversationAnalyser#conv}, differentiating negated or non-negated {@link Literal} 
	 * and whether undercuts or rebuttals are considered.
	 * @param undercuts Boolean true for a computation with undercut trees | false for a computation with rebuttals
	 * @param minus Boolean true for a computation of a negated claim | false  for a computation of a non-negated claim
	 * @return HashMap of information pieces and a list of hCategorizer values for its respective {@link Literal}
	 */
	public HashMap<Information, List<Float>> hCatComputer(boolean undercuts, boolean minus) {
		HashMap<Information, List<Float>> result = new HashMap<>();
		for (Information i : allInfo) {
			Literal l = minus ? i.getAsLiterals().getLast() : i.getAsLiterals().getFirst();
			List<Float> cats = new ArrayList<>();
			
			if (undercuts) {
				for (LogicArgument arg : logicArguments.get(l)) 
					cats.add(LogicUtilities.hCategorizer(undercutTrees.get(arg)));
			} else {
				cats = Arrays.asList(LogicUtilities.flatTreeHCategorizer(rebuttals.get(l).size()));
			}
			result.put(i, cats);
		}
		return result;
	}
	
	
	/**
	 * Computes the logAccumulator values for all {@link Information} instances of this 
	 * class's {@link LAFConversationAnalyser#conv}.
	 * @param hCatPlus List of hCategorization values for the information piece's non-negated literal
	 * @param hCatMinus List of hCategorization values for this information piece's negated literal
	 * @return HashMap of information pieces and float logAccumulator result
	 */
	public HashMap<Information, Float> accumulatorComputer(HashMap<Information, List<Float>> hCatPlus, HashMap<Information, List<Float>> hCatMinus) {
		HashMap<Information, Float> result = new HashMap<>();
		hCatPlus.forEach((i, v) -> {
			result.put(i, LogicUtilities.logAccumulator(v, hCatMinus.get(i)));
		});
		
		return result;
	}
	
	
	/**
	 * Creates the -scores-trust.xlsx matrix for a given path.
	 * <p>This method populates an xlsx matrix with meta-data, hCateogirzation,
	 * and logAccumuator values for each {@link Information} piece of {@link LAFConversationAnalyser#conv}.</p>
	 * @param path String path of the matrix
	 * @throws IOException 
	 */
	public void scoresMatrix(String path) throws IOException {
		LAFWorkbookController wbc = new LAFWorkbookController();
		wbc.initializeForLAF(newInfos, preKnowledge, literals2String, logicArguments);
		HashMap<Information, List<Float>> undercutHCatPlus = hCatComputer(true, false);
		HashMap<Information, List<Float>> undercutHCatMinus = hCatComputer(true, true);
		HashMap<Information, List<Float>> rebuttalHCatPlus = hCatComputer(false, false);
		HashMap<Information, List<Float>> rebuttalHCatMinus = hCatComputer(false, true);
		wbc.addTrustsForLAF(undercutHCatPlus, undercutHCatMinus, accumulatorComputer(undercutHCatPlus, undercutHCatMinus), "Undercuts");
		wbc.addTrustsForLAF(rebuttalHCatPlus, rebuttalHCatMinus, accumulatorComputer(rebuttalHCatPlus, rebuttalHCatMinus), "Rebuttals");
		wbc.write(path);
		System.out.println("Wrote trust analysis to " + path);

	}
	
	
	/**
	 * Creates the x-arguments.csv file.
	 * <p>This method creates a csv file that lists all {@link Information}s, derived {@link LogicArgument}s,
	 * constructed {@link LAFConversationAnalyser#undercutTrees}, and {@link LAFConversationAnalyser#rebuttals}. </p>
	 * @param path String path of the csv file
	 * @throws IOException
	 */
	public void writeLogicArgumentationCSV(String path) throws IOException {
		BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
		try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			writeInformationConnections(csvPrinter);
			csvPrinter.flush();
		}
        System.out.println("Wrote logic arguments to " + path);
	}
	
	
	/**
	 * Utility method that locates all {@link Information} pieces of a specific conversation
	 * partner that are of a specific type (Instruction | Fact), and adds them to lists alongside the literal-symbol
	 * of the respective information piece.
	 * @param partner String name of the target conversation partner | null for Author
	 * @param instructions Boolean true to return instruction-type information pieces | false for facts
	 * @return List containing lists of (String Symbol, information piece) pairs.  
	 */
	private List<List<String>> findLiteralMessagePairs(String partner, boolean instructions) {
		List<List<String>> literalMessagePairs = new ArrayList<>();
		
		for (Information i : allInfo) {
			List<String> partnerInfo = new ArrayList<>();
			if (instructions && !i.isIsInstruction())
				continue;
			else if (!instructions && i.isIsInstruction())
				continue;
			
			if (i instanceof PreKnowledge && partner == null) {
				// pre-knowledge of author
				partnerInfo.add(literals2String.get(i.getAsLiterals().getFirst()));
				partnerInfo.add(i.getMessage());
			} 
			else if (i instanceof NewInformation 
					&& ((NewInformation) i).getSource().getCounterPart().getName().equals(partner)) {
				
				partnerInfo.add(literals2String.get(i.getAsLiterals().getFirst()));
				partnerInfo.add(i.getMessage());
				
			}
			if (!partnerInfo.isEmpty()) 
				literalMessagePairs.add(partnerInfo);
		}
		return literalMessagePairs;
	}
	
	
	/**
	 * helper method for the creation of x-arguments.csv files (see {@link LAFConversationAnalyser#writeLogicArgumentationCSV)
	 * @param csvPrinter target csv file to be modified
	 * @throws IOException
	 */
	private void writeInformationConnections(CSVPrinter csvPrinter) throws IOException {
		//----------------- List messages and their unique symbols --------------------
		String[] columns = new String[] {"Literal", "Message"}; // columns
		List<List<String>> facts = new ArrayList<>();
		List<List<String>> instructions = new ArrayList<>();
		
		for (String partner : partners) {
			//for each partner find all facts and instructions
			facts = findLiteralMessagePairs(partner, false);
			instructions = findLiteralMessagePairs(partner, true);
			
			if (!facts.isEmpty()) {
				//write the facts and their literal symbol
				csvPrinter.printRecord(partner + " Facts"); // header
				csvPrinter.printRecord((Object[]) columns); // columns
				for (List<String> literalFactPair : facts) {
					csvPrinter.printRecord(literalFactPair);
				}
				
				csvPrinter.printRecord();
			}

			if (!instructions.isEmpty()) {
				//write the instructions and their literal symbol
				csvPrinter.printRecord(partner + " Instructions"); // header
				csvPrinter.printRecord((Object[]) columns); // columns
				for (List<String> literalFactPair : instructions) {
					csvPrinter.printRecord(literalFactPair);
				}
		
				csvPrinter.printRecord();
			}
		}
		//-------- List messages and their unique symbols of AUTHOR ----------
		facts = findLiteralMessagePairs(null, false);
		instructions = findLiteralMessagePairs(null, true);
		
		if (!facts.isEmpty()) {
			//write the facts and their literal symbol
			csvPrinter.printRecord("Author PreKnowledge Facts"); // header
			csvPrinter.printRecord((Object[]) columns); // columns
			for (List<String> literalFactPair : facts) { 
				csvPrinter.printRecord(literalFactPair);
			}
			csvPrinter.printRecord();
		}
		//write the instructions and their literal symbol
		if (!instructions.isEmpty()) {
			csvPrinter.printRecord("Author PreKnowledge Instructions"); // header
			csvPrinter.printRecord((Object[]) columns); // columns
			for (List<String> literalFactPair : instructions) {
				csvPrinter.printRecord(literalFactPair);
			}
			//
			csvPrinter.printRecord();
		}
		
		//--------------- Write all logic arguments derived ------------
		csvPrinter.printRecord("Logic Arguments (<{premises} claim>)"); // header
		csvPrinter.printRecord((Object[]) new String[] {"Claim", "Argument"}); // columns
		// for each literal/claim, list all logic arguments for it
		for (Map.Entry<Literal, List<LogicArgument>> entry : logicArguments.entrySet()) {
			Literal claim = entry.getKey();
			List<LogicArgument> args = entry.getValue();
			if (args.isEmpty())
				continue;
			csvPrinter.printRecord((Object[]) new String[]{
					literals2String.get(claim),
					args.stream().map(n -> LogicArgument.asString(n, literals2String)).collect(Collectors.toList()).toString()
			});
			
		}
		csvPrinter.printRecord();
		
		//-------- Write all undercut trees found for the arguments -------------
		csvPrinter.printRecord("Undercut Trees"); //header
		for (Map.Entry<LogicArgument, ArgumentTree> entry : undercutTrees.entrySet()) {
			LogicArgument arg = entry.getKey();
			ArgumentTree at = entry.getValue();
			if (at.children.isEmpty()) // if an argument doesn't have undercuts, skip
				continue;
			
			csvPrinter.printRecord((Object[]) new String[]{
					ArgumentTree.printTree(undercutTrees.get(arg), "", literals2String, new StringBuilder()).strip()
					});

		}
		csvPrinter.printRecord();
		
		//------ Write all rebuttals for the arguments -------------
		csvPrinter.printRecord("Rebuttals"); // header
		csvPrinter.printRecord((Object[]) new String[] {"Claim", "rebutted by"}); // column
		for (Map.Entry<Literal, List<LogicArgument>> entry : rebuttals.entrySet()) {
			Literal claim = entry.getKey();
			List<LogicArgument> args = entry.getValue();
			if (args.isEmpty())
				continue;
			csvPrinter.printRecord((Object[]) new String[]{
					literals2String.get(claim),
					args.stream().map(n -> LogicArgument.asString(n, literals2String)).collect(Collectors.toList()).toString()
			});			
		}
	}

}
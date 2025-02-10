package keml.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import keml.Conversation;
import keml.Information;
import keml.InformationLink;
import keml.InformationLinkType;
import keml.Junction;
import keml.Literal;
import keml.LogicExpression;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;

public class ArgumentationStructurer {
	
	

	// TODO redo the diagnosis conversation with a more conservative, logical use of junctions
	
	Conversation conv;
	List<String> partners;
	List<ReceiveMessage> receives;  
	List<NewInformation> newInfos;
	List<PreKnowledge> preKnowledge;
	HashSet<Information> allInfo;
	
	List<Information> infoList;
	Map<Information, List<Literal>> info2Literal = new HashMap<>();
	Map<Literal, List<LogicExpression>> logicArguments = new HashMap<>();

	
	
	public ArgumentationStructurer(Conversation conv) {
		this.conv = conv;
		this.partners = ConversationAnalyser.getPartnerNames(conv); 
		this.receives = ConversationAnalyser.getReceives(conv);
		this.newInfos = ConversationAnalyser.getNewInfos(receives);
		this.preKnowledge = conv.getAuthor().getPreknowledge();
		this.allInfo  = Stream.concat(preKnowledge.stream(), newInfos.stream()).collect(Collectors.toCollection(HashSet::new));
		
		infoList = new ArrayList<>(allInfo);
		keml2Logic();
	}
	
	

	
	public void findLogicExpressions() {
		//find information literals
		for (Information i : allInfo) {
			for (Literal l : i.getAsLiteral()) {
				List<LogicExpression> premises = new ArrayList<>();
				for (LogicExpression le : l.getPremises()) 
					premises.add(le);
				
				logicArguments.put(l, premises);
			}
		}
		
	}
	
	public void keml2Logic() {
		findLogicExpressions();
		debugInfos();
//		debugArguments();
		debugAttacks();
//		debugCatNAccu();
	}
	
	

	
	public List<Literal> findUndercuts(Literal l) {
		List<Literal> result = new ArrayList<>();
		List<LogicExpression> premises = logicArguments.get(l);
		Literal counterLiteral;
		
		for (LogicExpression premise : premises) {
			if (premise instanceof Literal) {
				counterLiteral = findCounterLiteral((Literal) premise);
				if (!logicArguments.get(counterLiteral).isEmpty()) // if counter literal has premises
					result.add(counterLiteral);
				
			} else {
				for (LogicExpression junctionContent : ((Junction) premise).getContent()) {
					counterLiteral = findCounterLiteral((Literal) junctionContent); 
					if (!logicArguments.get(counterLiteral).isEmpty()) // if counter literal has premises
						result.add(counterLiteral);
				}
			}
		}
		
		return result;
	}
	
	public double hCategorizer(Literal claim) {
		List<Literal> undercuts = findUndercuts(claim);
		if (undercuts.isEmpty())
			return 1.0;
		double sum = 1.0;
		for (Literal l : undercuts) {
			sum += hCategorizer(l);
		}
		
		return 1/(sum);
	}
	
	public double logAccumulator(List<Double> forCategorizations, List<Double>againstCategorizations) {
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
	
	public Literal findCounterLiteral(Literal l) {
		//return the negation of this literal, if it exists
		Information sourceInfo = l.getSource();
		
		if (l.isNegated())
			return sourceInfo.getAsLiteral().get(0);
		else 
			return sourceInfo.getAsLiteral().get(1);
	}
	
	public List<Literal> findRebuttals(Literal l) {
		List<Literal> result = new ArrayList<>();

		Literal counterLiteral = findCounterLiteral(l);
		if (!logicArguments.get(counterLiteral).isEmpty())
			result.add(counterLiteral);
		
		return result;
		
		
	}
	
	
	public void debugInfos() {
		for (int i = 0; i < infoList.size(); i++) { 
			System.out.println(i + " : " + infoList.get(i).getMessage());
			
		}
	}

	
	public void debugAttacks() {
		for (Literal l : logicArguments.keySet()) {
//			System.out.println(debugSingleLiteral(l));
			System.out.println("Finding undercuts for " + debugSingleLiteral(l));
			List<Literal> undercuts = findUndercuts(l);

			for (Literal undercut : undercuts) {
				System.out.println(debugSingleLiteral(undercut));
			}
			System.out.println("Finding rebuttals for " + debugSingleLiteral(l));
			List<Literal> rebuttals = findRebuttals(l);
			for (Literal rebuttal : rebuttals) {
				System.out.println(debugSingleLiteral(rebuttal));
			}

		}
	}
	
	public void debugCatNAccu() {
		for (Information info : allInfo) {
			if (logicArguments.get(info.getAsLiteral().getFirst()).isEmpty() && logicArguments.get(info.getAsLiteral().getLast()).isEmpty())
				continue;
			
			Literal nonNegated = info.getAsLiteral().getFirst();
			Literal negated = info.getAsLiteral().getLast();
			
			List<Double> forCategorizations = new ArrayList<>();
			forCategorizations.add(hCategorizer(nonNegated));
			
			List<Double> againstCategorizations = new ArrayList<>();
			againstCategorizations.add(hCategorizer(negated));
			System.out.println("Log-Accumulator for: " + debugSingleLiteral(nonNegated) + " and " + debugSingleLiteral(negated) + " = " + logAccumulator(forCategorizations, againstCategorizations));
		}
		
	}
	
	
	public String debugSingleLiteral(Literal claim) {
		List<LogicExpression> premises = logicArguments.get(claim);
		String premisesString = "";
		for (int j = 0; j < premises.size(); j++) {
			LogicExpression premise = premises.get(j);
			if (premise instanceof Junction) {
				List<LogicExpression> junctionContent = ((Junction) premise).getContent();
				premisesString += "(";
				
				for (int i = 0; i < junctionContent.size(); i++) {
					
					premisesString += (((Literal) junctionContent.get(i)).isNegated() ? "!" : "") +  infoList.indexOf(((Literal) junctionContent.get(i)).getSource()) ;
					
					if (i != junctionContent.size() - 1) 
						premisesString += " & ";
					
				}
				premisesString += ") => " +  (claim.isNegated() ? "!" : "") + infoList.indexOf(claim.getSource());

			}
			
			else if (premise instanceof Literal) {
				premisesString += (((Literal) premise).isNegated() ? "!" : "") +  infoList.indexOf(((Literal) premise).getSource())  
					+ " => " + (claim.isNegated() ? "!" : "") + infoList.indexOf(claim.getSource());
				
			}
			if (j != premises.size() -1 )
				premisesString += ", ";
		}

		return "<[" + premisesString + "], " + (claim.isNegated() ? " !" : "") + infoList.indexOf(claim.getSource()) + " >";
		
	}

	public void debugArguments() {
		for (Literal l : logicArguments.keySet()) {
			System.out.println(debugSingleLiteral(l));
		}
	}


	
	

	

	
	
	
	

}

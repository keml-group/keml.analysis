package keml.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import keml.Junction;
import keml.Literal;
import keml.LogicExpression;

/**
 * Object representation of a logical argument
 */
public class LogicArgument {

	/** {@link Literal} claim of this argument */
	Literal claim;
	/** {@link LogicExpression} premises of this argument*/
	List<LogicExpression> premises;
	
	/**
	 * Constructor for {@link LogicArgument}
	 * @param claim {@link Literal} claim of the to-be-created instance
	 */
	public LogicArgument(Literal claim) {
		this.claim = claim;
		this.premises = new ArrayList<>();
	}
	
	/**
	 * adds a premise to the list of {@link LogicArgument#premises} for this instance.
	 * @param le {@link LogicExpression}
	 */
	public void addPremise(LogicExpression le) {
		premises.add(le);
	}

	/**
	 * getter for this instance's {@link LogicArgument#claim}.
	 * @return claim {@link Literal}
	 */
	public Literal getClaim() {
		return claim;
	}

	/**
	 * getter for this instance's {@link LogicArgument#premises}.
	 * @return premises as List of {@link LogicExpression}s
	 */
	public List<LogicExpression> getPremises() {
		return premises;
	}


	/**
	 * Utility <b>static</b> method that creates a String representation of a {@link Junction} using
	 * recursion and a map of {@link Literal}s to their unique String symbol
	 * @param j target junction
	 * @param content String content so far (facilitates recursion)
	 * @param literals2String Map of literals to their String symbol
	 * @return String representation of the target junction
	 */
	private static String junctionAsString(Junction j, String content, Map<Literal, String> literals2String) {
		
		for (int i = 0; i < j.getContent().size(); i++) {
			LogicExpression le = j.getContent().get(i);
			if (le instanceof Literal) { 
				content += literals2String.get((Literal) le);
			} else { // if junction contains junctions
				content += junctionAsString((Junction) le, content, literals2String);
			} // conjunct or disjunct the content so far
			if (i < j.getContent().size() - 1)
				content += j.isDisjunction() ? " || " : " && ";
			
		}
		
		return content + ")";
	}
	
	
	/**
 	 * Utility <b>static</b> method that creates a String representation of the premises of a {@link LogicArgument}.
	 * @param la target logic argument
	 * @param literals2String Map of {@link Literal}s to their String symbol
	 * @return String representation of the target argument's premises
	 */
	private static String premisesAsString(LogicArgument la, Map<Literal, String> literals2String) {
		String premises = "";
		for (int i = 0; i < la.getPremises().size(); i++) {
			LogicExpression le = la.getPremises().get(i);
			if (le instanceof Literal) {
				premises += literals2String.get((Literal) le);
				if (!((Literal) le).equals(la.getClaim())) 
					premises += " => " + literals2String.get(la.getClaim());
	
			} else { //if premises contain junctions
				premises += junctionAsString((Junction) le, "(", literals2String) + " => " + literals2String.get(la.getClaim());
			}
			// separate premises so far
			if (i < la.getPremises().size() - 1) {
				premises += ", ";
			}
			
		}
		
		return premises;
	}
	
	
	/**
	 * Utility <b>static</b> method that creates a String representation of a {@link LogicArgument}.
	 * @param la target argument
	 * @param literals2String Map of {@link Literal}s to their String symbol
	 * @return String representation of the target argument
	 */
	public static String asString(LogicArgument la, Map<Literal, String> literals2String) {			
		return "<{" +  premisesAsString(la, literals2String) + "}, " + literals2String.get(la.getClaim()) + ">";
	}
	
	
}

package keml.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import keml.Junction;
import keml.Literal;
import keml.LogicExpression;

public class LogicArgument {

	Literal claim;
	List<LogicExpression> premises;
	
	
	public LogicArgument(Literal claim) {
		this.claim = claim;
		this.premises = new ArrayList<>();
	}
	
	public void addPremise(LogicExpression le) {
		premises.add(le);
	}


	public Literal getClaim() {
		return claim;
	}


	public List<LogicExpression> getPremises() {
		return premises;
	}


	public void setClaim(Literal claim) {
		this.claim = claim;
	}


	public void setPremises(List<LogicExpression> premises) {
		this.premises = premises;
	}
	
	private static String junctionAsString(Junction j, String content, Map<Literal, String> literals2String) {
		
		
		for (int i = 0; i < j.getContent().size(); i++) {
			LogicExpression le = j.getContent().get(i);
			if (le instanceof Literal) {
				content += literals2String.get((Literal) le);
			} else {
				content += junctionAsString((Junction) le, content, literals2String);
			}
			if (i < j.getContent().size() - 1)
				content += j.isIsDisjunction() ? " || " : " && ";
			
		}
		
		return content + ")";
	}
	
	public static String asString(LogicArgument la, Map<Literal, String> literals2String) {
		String result = "<{";
		String premises = "";
		for (int i = 0; i < la.getPremises().size(); i++) {
			LogicExpression le = la.getPremises().get(i);
			if (le instanceof Literal) {
				premises += literals2String.get((Literal) le) + " => " + literals2String.get(la.getClaim());
			} else {
				premises += junctionAsString((Junction) le, "(", literals2String) + " => " + literals2String.get(la.getClaim());
			}
			
			if (i < la.getPremises().size() - 1) {
				premises += ", ";
			}
			
		}
		
		return result + premises + "}, " + literals2String.get(la.getClaim()) + ">";
	}
	
	
}

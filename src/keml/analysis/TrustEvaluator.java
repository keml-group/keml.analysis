package keml.analysis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.javatuples.Pair;

import keml.Conversation;
import keml.Information;
import keml.InformationLink;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;

// takes a conversation and prepares it for trust analysis, each analysis modifies initial and current trust scores
public class TrustEvaluator {
	// +1 is absolute trust, -1 is absolute distrust, 0 is ignore - no knowledge about that node

	Conversation conv;
	List<String> partners;
	List<ReceiveMessage> receives;  //starting point for work on knowledge part
	List<NewInformation> newInfos;
	List<PreKnowledge> preKnowledge;
	
	int weight;
	
	
	public TrustEvaluator(Conversation conv, int weight) {
		this.conv = conv;
		this.partners = ConversationAnalyser.getPartnerNames(conv); //works as header row
		this.receives = ConversationAnalyser.getReceives(conv);
		this.newInfos = ConversationAnalyser.getNewInfos(receives);
		this.preKnowledge = conv.getAuthor().getPreknowledge();
		
		this.weight = weight;
	}
	
	public static List<Map<String, Float>> standardTrustConfigurations() {
		ArrayList<Map<String, Float>> res = new ArrayList<>();
		
		Map<String, Float> valuesPerPartner10 = new HashMap<String, Float>();
		valuesPerPartner10.put("LLM", 1.0F);
		valuesPerPartner10.put("Browser", 1.0F);
		res.add(valuesPerPartner10);
		
		Map<String, Float> valuesPerPartner1005 = new HashMap<String, Float>();
		valuesPerPartner1005.put("LLM", 1.0F);
		valuesPerPartner1005.put("Browser", 0.5F);
		res.add(valuesPerPartner1005);
		
		Map<String, Float> valuesPerPartner0510 = new HashMap<String, Float>();
		valuesPerPartner0510.put("LLM", 0.5F);
		valuesPerPartner0510.put("Browser", 1.0F);
		res.add(valuesPerPartner0510);
		
		Map<String, Float> valuesPerPartner05 = new HashMap<String, Float>();
		valuesPerPartner05.put("LLM", 0.5F);
		valuesPerPartner05.put("Browser", 0.5F);
		res.add(valuesPerPartner05);
		
		return res;
	}
	
	public void writeRowAnalysis(String path, List<Map<String, Float>> trustInPartner, Float authorValue) throws IOException {
		
		List<HashMap<Information, Pair<Float, Float>>> analysedTrustConfigs = new ArrayList<>();
		
		// we split the first case and do it last so that we can fall back to the standard procedure with just one score - will fail if none exist, though
		Map<String, Float> finalPass = trustInPartner.removeFirst();
		trustInPartner.forEach(trusts -> { //others in row,
			analysedTrustConfigs.add(analyse(trusts, authorValue));
		});
		analyse(finalPass, authorValue);
		
		// now create wb from whole list
		WorkbookController wbc = new WorkbookController();
		wbc.putData(newInfos, preKnowledge);
		// add all other analysis results to wb
		analysedTrustConfigs.forEach(c -> wbc.addTrusts(c));
		wbc.write(path);
	}
	
	public void writeSingleAnalysis(String path, Map<String, Float> trustInPartner, Float authorValue) throws IOException {
		
		analyse(trustInPartner, authorValue);		
		WorkbookController wbc = new WorkbookController();
		wbc.putData(newInfos, preKnowledge);
		wbc.write(path);
	}
	
	public HashMap<Information, Pair<Float, Float>> analyseWithDefault() {
		Float authorValue = 1.0F;
		Map<String, Float> valuesPerPartner = new HashMap<String, Float>();
		partners.forEach(p -> {
			valuesPerPartner.put(p, 1.0F);
		});
		// modify LLM credibility:
		valuesPerPartner.put("LLM", 0.5F);
		return analyse(valuesPerPartner, authorValue);
	}
	
	public HashMap<Information, Pair<Float, Float>> analyse(Map<String, Float> valuePerPartner, Float authorValue) {
		assignInitialTrust(valuePerPartner, authorValue);
		evaluate();
		
		HashMap<Information, Pair<Float, Float>> res = new HashMap<Information, Pair<Float, Float>> ();
		
		preKnowledge.forEach(pre -> res.put(pre, new Pair<Float, Float>(pre.getInitialTrust(), pre.getCurrentTrust())));
		newInfos.forEach(info -> res.put(info, new Pair<Float, Float>(info.getInitialTrust(), info.getCurrentTrust())));
		
		return res;
	}
	
	public void assignInitialTrust(Map<String, Float> valuePerPartner, Float authorValue) {
		preKnowledge.forEach(p -> {
			p.setInitialTrust(authorValue);
			p.setCurrentTrust(authorValue);
		});
		newInfos.forEach(info -> {
			Float v = valuePerPartner.get(info.getSourceConversationPartner().getName());
			info.setInitialTrust(v);
			info.setCurrentTrust(v);
		});
	}
	
	public void evaluate() {
		HashSet<Information> toVisit = Stream.concat(preKnowledge.stream(), newInfos.stream()).collect(Collectors.toCollection(HashSet::new));
		//List<Information> toVisit = Stream.concat(preKnowledge.stream(), newInfos.stream()).toList().reversed();// start with last time stamp
		
		while (toVisit.size()>0) {
			int remaining = toVisit.size();
			for (Iterator<Information> i = toVisit.iterator(); i.hasNext(); ) {
				 Information info = i.next();
				 if (ready(info, toVisit)) {
					 info.setCurrentTrust(currentNodeTrust(info));
					 i.remove();
				 }
			}
			if (toVisit.size() == remaining) {
				System.err.println(toVisit.toString());
				for (Information i: toVisit) {
					for(InformationLink l : i.getTargetedBy()) {
						System.err.println(l.getSource().getMessage() + " -> "+i.getMessage());
					}
				}
				throw new IllegalArgumentException("Endless loop of "+ toVisit.size() +" nodes - please check the argumentation graph");
			}
		}
	}
	
	private boolean ready(Information info, HashSet<Information> toVisit) {
		for (InformationLink informationLink : info.getTargetedBy()) {
			if (toVisit.contains(informationLink.getSource()))
				return false;
		}
		return true;
	}
	
	private Float currentNodeTrust(Information info) {
		return limitTo1(info.getInitialTrust()
			+ getRepetitionScore(info)
			+ weight*argumentationScore(info));
	}
	
	// relies on current trust 
	private float argumentationScore(Information info) {
		return (float) info.getTargetedBy().stream()
			.mapToDouble(link -> score(link))
			.sum();
	}
	
	private float score(InformationLink link) {
		float edgeWeight;
		switch(link.getType()) {
		case SUPPORT:
			edgeWeight = 0.5f;
			break;
		case STRONG_SUPPORT:
			edgeWeight = 1.0f;
			break;
		case ATTACK:
			edgeWeight = -0.5f;
			break;
		case STRONG_ATTACK:
			edgeWeight = -1.0f;
			break;
		case SUPPLEMENT:
		default:
			edgeWeight = 0.0f;
			break;	
		}
		// TODO should we ignore nodes that have negative trust or let them work in opposite direction? Currently opposite:
		return edgeWeight*link.getSource().getCurrentTrust();
	}
	
	private float getRepetitionScore(Information info) {
		return (float) info.getRepeatedBy().size()/receives.size();
	}

	private static Float limitTo1(Float f) {
		if (f<-1) return -1F;
		if (f>1) return 1F;
		return f;
	}
	
	public void writeToExcel(String file) throws IOException {
		
		WorkbookController wbc = new WorkbookController();
		wbc.putData(newInfos, preKnowledge);
		wbc.write(file);

	}
	
}

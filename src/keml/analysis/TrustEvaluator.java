package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.Information;
import keml.InformationLink;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;

public class TrustEvaluator {
	// +1 is absolute trust, -1 is absolute distrust, 0 is ignore - no knowledge about that node

	Conversation conv;
	List<String> partners;
	List<ReceiveMessage> receives;  //starting point for work on knowledge part
	List<NewInformation> newInfos;
	List<PreKnowledge> preKnowledge;
	
	
	public TrustEvaluator(Conversation conv) {
		this.conv = conv;
		this.partners = ConversationAnalyser.getPartnerNames(conv); //works as header row
		this.receives = ConversationAnalyser.getReceives(conv);
		this.newInfos = ConversationAnalyser.getNewInfos(receives);
		this.preKnowledge = conv.getAuthor().getPreknowledge();
	}
	
	public void analyse(String path) {
		Float authorValue = 1.0F;
		Map<String, Float> valuesPerPartner = new HashMap<String, Float>();
		partners.forEach(p -> {
			valuesPerPartner.put(p, 1.0F);
		});
		// modify LLM credibility:
		valuesPerPartner.put("LLM", 0.5F);
		analyse(path, valuesPerPartner, authorValue);
	}
	
	public void analyse(String path, Map<String, Float> valuePerPartner, Float authorValue) {
		assignInitialTrust(valuePerPartner, authorValue);
		evaluate();
		try {
			writeToExcel(path);
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			+ 2*argumentationScore(info));
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
	
	private static final DecimalFormat df = new DecimalFormat("#.##");
	
	private static String roundTwoDigits(Float f) {
		return df.format(f);
	}
	
	// write current values
	public void write(String file) throws IOException {
		String path = FilenameUtils.removeExtension(file) + "-trust.csv";
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
	        try (CSVPrinter csvPrinter = new CSVPrinter(writer, 
	        		CSVFormat.TDF.builder().setHeader("TimeStamp", "Message", "InitialTrust", "CurrentTrust").build())
	        ) {
	        	preKnowledge.forEach(info -> {
					try {
						csvPrinter.printRecord("-1", info.getMessage(), roundTwoDigits(info.getInitialTrust()), roundTwoDigits(info.getCurrentTrust()));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	        		
	        	});
	        	newInfos.forEach(info -> {
					try {
						csvPrinter.printRecord(info.getTiming(), info.getMessage(), roundTwoDigits(info.getInitialTrust()), roundTwoDigits(info.getCurrentTrust()));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	        		
	        	});
				csvPrinter.flush();
			}
		}
	    System.out.println("Wrote trust analysis to " + path);		
	}
	
	public void writeToExcel(String file) throws IOException {
		
		WorkbookController wbc = new WorkbookController();
		wbc.putData(newInfos, preKnowledge);
		wbc.write(file);

	}
	
}

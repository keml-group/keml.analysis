package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;
import keml.analysis.ConversationAnalyser.InformationType;

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
			write(path);
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
		// TODO
	}
	
	// write current values
	public void write(String file) throws IOException {
		String path = FilenameUtils.removeExtension(file) + "-trust.csv";
		try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(path))) {
	        try (CSVPrinter csvPrinter = new CSVPrinter(writer, 
	        		CSVFormat.DEFAULT.builder().setHeader("TimeStamp", "Message", "CurrentTrust", "InitialTrust").build())
	        ) {
	        	newInfos.forEach(info -> {
					try {
						csvPrinter.printRecord(info.getTiming(), info.getMessage(), info.getCurrentTrust(), info.getInitialTrust());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}	        		
	        	});
	        	preKnowledge.forEach(info -> {
					try {
						csvPrinter.printRecord("-1", info.getMessage(), info.getCurrentTrust(), info.getInitialTrust());
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

}

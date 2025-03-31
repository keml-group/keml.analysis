package keml.analysis;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVPrinter;

import keml.ITargetable;
import keml.Information;
import keml.InformationLink;
import keml.InformationLinkType;
import keml.NewInformation;
import keml.PreKnowledge;
import keml.ReceiveMessage;

public class InformationPartAnalyser {
	
	List<String> partners; //works as headers
	int dimension; // for analysis of message connections
	List<PreKnowledge> preKnowledge;
	List<ReceiveMessage> receives;  //starting point for work on knowledge part
	List<NewInformation> newInfos;


	public InformationPartAnalyser(List<String> partners, List<ReceiveMessage> receives, List<PreKnowledge> preKnowledge) {
		this.partners = partners;
		dimension = (partners.size()+1)*2;
		this.receives = receives;
		this.newInfos = ConversationAnalyser.getNewInfos(receives);
		this.preKnowledge = preKnowledge;
	}
	
	public void writeInformationConnections(CSVPrinter csvPrinter) throws IOException {
		//matrix holds fact and instruction entry for the author (for pre-knowledge) and each partner
		int[][] countAttacks = new int[dimension][dimension];
		int[][] countSupports = new int[dimension][dimension];
		int[][] countRecAttacks = new int[dimension][dimension];
		int[][] countRecSupports = new int[dimension][dimension];
		
		Stream.concat(newInfos.stream(), preKnowledge.stream())
			.forEach(info -> {
				int index = getIndexOfInfo(info);
				info.getCauses().forEach(link -> {
					if (link.getTarget() instanceof Information) { //NEW: because of new meta model 
						Information i = (Information) link.getTarget();
						int partnerIndex = getIndexOfInfo(i);
						if (link.getType() == InformationLinkType.SUPPORT || link.getType() == InformationLinkType.STRONG_SUPPORT) {
							countSupports[index][partnerIndex] +=1;
						}
						if (link.getType() == InformationLinkType.ATTACK || link.getType() == InformationLinkType.STRONG_ATTACK) {
							countAttacks[index][partnerIndex] +=1;
						}
					} else {
						// NEW: counts recursive attacks and supports
						if (link.getTarget() instanceof InformationLink) {
							InformationLink i = (InformationLink) link.getTarget();
							int partnerIndex = getIndexOfInfoLink(i);
							if (link.getType() == InformationLinkType.SUPPORT || link.getType() == InformationLinkType.STRONG_SUPPORT) {
								countRecSupports[index][partnerIndex] +=1;
							}
							if (link.getType() == InformationLinkType.ATTACK || link.getType() == InformationLinkType.STRONG_ATTACK) {
								countRecAttacks[index][partnerIndex] +=1;
							}
						}
					}
					
				});
			});
		
		String[][] text = combineMatrices(countAttacks, countSupports,countRecAttacks, countRecSupports);
		writeMatrix(text, csvPrinter);	
	}


	// writes both matrices a, b into one having entries "a/b" (NEW: now there are 4 because recursive attacks and supports are added)
	// assumption is that a and b are square matrices and have the same length dimensions
	private String[][] combineMatrices(int[][] a, int[][]b,int[][] c, int[][]d) {
		String[][] result = new String[dimension][dimension];
		for (int i = 0; i < dimension; i++) {
			for (int j=0; j < dimension; j++) {
				result[i][j] = a[i][j]+"/"+b[i][j]+"/"+c[i][j]+"/"+d[i][j];
			}
		}
		return result;
	}
	
	String[] headers() {
		String[] headers = new String[dimension];
		for (int i = 0; i< partners.size(); i++) {
			headers[2*i] = partners.get(i) + " F";
			headers[2*i+1] = partners.get(i) + " I";
		}
		headers[dimension-2]= "Author F";
		headers[dimension-1]= "Author I";
		return headers;
	}

	// writes both matrices A, B into one having entries "a/b"
	private void writeMatrix(String[][] m, CSVPrinter csvPrinter) throws IOException {
		String[] headers = headers();
		csvPrinter.print("Attacks/Supports/RecAttacks/RecSupports");
		csvPrinter.printRecord(headers);
		for (int i = 0; i < dimension; i++) {
			csvPrinter.print(headers[i]);
			csvPrinter.printRecord(m[i]);
		}
	}
	
	private int getIndexOfInfo(Information info) {
		int offset;
		if (info.isIsInstruction()) offset = 1; else offset = 0;
		int partnerIndex;
		if (info instanceof NewInformation) { //NEW: because of new meta model
			partnerIndex = partners.indexOf(((NewInformation) info).getSourceConversationPartner().getName());
		} else {
			partnerIndex = partners.size();
		}
		return 2*partnerIndex + offset;
	}
	
	// NEW: the target node of an recursive edge is the target node of the "normal" edge to which it (recursively) points
	private int getIndexOfInfoLink(InformationLink link) {
		ITargetable target = link.getTarget();
		if (target instanceof NewInformation) {
			int offset;
			Information info = (Information) link.getTarget(); 
			if (info.isIsInstruction()) offset = 1; else offset = 0;
			int partnerIndex;
			if (info instanceof NewInformation) {
				partnerIndex = partners.indexOf(((NewInformation) info).getSourceConversationPartner().getName());
			} else {
				partnerIndex = partners.size();
			}
			return 2*partnerIndex + offset;
		} else {
			link = (InformationLink) target;
			return getIndexOfInfoLink(link);
		}
	}
	
}

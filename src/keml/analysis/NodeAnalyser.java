package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import keml.Conversation;
import keml.Information;
import keml.InformationLinkType;
import keml.ReceiveMessage;
import keml.SendMessage;
import keml.MessageExecution;
import keml.NewInformation;

public class NodeAnalyser {
	
	Conversation conv;
	List<String> partners; //works as headers
	int dimension; // for analysis of message connections
	List<ReceiveMessage> receives;  //starting point for work on knowledge part
	List<NewInformation> newInfos;
	
	public NodeAnalyser(Conversation conv) {
		super();
		this.conv = conv;
		this.partners = conv.getConversationPartners().stream().map(s -> s.getName()).toList(); //works as header row
		dimension = (partners.size()+1)*2;
		this.receives = conv.getAuthor().getMessageExecutions().stream()
				.filter(m -> m instanceof ReceiveMessage).map(ms -> {
					return (ReceiveMessage) ms;
				}).toList();
		this.newInfos = receives.stream().map(m -> m.getGenerates()).flatMap(List::stream).toList();
	}

	public void createCSV(String path) throws IOException {
		
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			csvPrinter.printRecord("MessagePart");
			printPartnerHeaderRow(csvPrinter);
			writeMessageCounts(csvPrinter);		
			csvPrinter.printRecord();		
			csvPrinter.printRecord("KnowledgePart");
			csvPrinter.printRecord("Preknowledge", conv.getAuthor().getPreknowledge().size());
			csvPrinter.print("New Information");
			csvPrinter.printRecord(partners);
			HashMap<InformationType, Map<String, Long>> newInfos = countNewInformationByPartner();
			//Long allNew = newInfos.get(InformationType.OVERALL).values().stream().collect(Collectors.reducing(0L, (x, y) -> Long.sum(x, y)));
			//csvPrinter.printRecord("NewInformation", allNew);
			//printPartnerHeaderRow(csvPrinter);			
			//writeForPartners(newInfos.get(InformationType.OVERALL), "all", csvPrinter, 0L);
			writeForPartners(newInfos.get(InformationType.FACT), "Facts", csvPrinter, 0L);
			writeForPartners(newInfos.get(InformationType.INSTRUCTION), "Instructions", csvPrinter, 0L);
			csvPrinter.printRecord("Trust:");
			csvPrinter.printRecord("Repetitions", countRepetitions());
			writeInformationConnections(csvPrinter);
			csvPrinter.flush();
		}
        System.out.println("Wrote analysis to " + path);
	}
		
	private void writeInformationConnections(CSVPrinter csvPrinter) throws IOException {
		//matrix holds fact and instruction entry for the author (for pre-knowledge) and each partner
		int[][] countAttacks = new int[dimension][dimension];
		int[][] countSupports = new int[dimension][dimension];
		newInfos.forEach(info -> {
			int index = getIndexOfInfo(info);
			info.getCauses().forEach(link -> {
				int partnerIndex = getIndexOfInfo(link.getTarget());
				if (link.getType() == InformationLinkType.SUPPORT || link.getType() == InformationLinkType.ACCEPT) {
					countSupports[index][partnerIndex] +=1;
				}
				if (link.getType() == InformationLinkType.CHALLENGE || link.getType() == InformationLinkType.REJECT) {
					countAttacks[index][partnerIndex] +=1;
				}
				// todo analyse supplements?
			});
		});
		
		String[][] text = combineMatrices(countAttacks, countSupports);
		writeMatrix(text, csvPrinter);	
	}
	
	// writes both matrices a, b into one having entries "a/b"
	// assumption is that a and b are square matrices and have the same length dimensions
	private String[][] combineMatrices(int[][] a, int[][]b) {
		String[][] result = new String[dimension][dimension];
		for (int i = 0; i < dimension; i++) {
			for (int j=0; j < dimension; j++) {
				result[i][j] = a[i][j]+"/"+b[i][j];
			}
		}
		return result;
	}
	
	String[] headers() {
		String[] headers = new String[dimension];
		for (int i = 0; i< partners.size(); i++) {
			headers[2*i] = partners.get(i) + " Facts";
			headers[2*i+1] = partners.get(i) + " Instructions";
		}
		headers[dimension-2]= "Author Facts";
		headers[dimension-1]= "Author Instructions";
		return headers;
	}

	
	// writes both matrices A, B into one having entries "a/b"
	private void writeMatrix(String[][] m, CSVPrinter csvPrinter) throws IOException {
		String[] headers = headers();
		csvPrinter.print("Attacks/Supports");
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
		if (info instanceof NewInformation) {
			partnerIndex = partners.indexOf(((NewInformation) info).getSourceConversationPartner().getName());
		} else {
			partnerIndex = partners.size();
		}
		return 2*partnerIndex + offset;
	}
	
	private void printPartnerHeaderRow(CSVPrinter csvPrinter) throws IOException {
		csvPrinter.print("");
		csvPrinter.printRecord(partners);
	}
	
	enum InformationType {
		INSTRUCTION, FACT, OVERALL;
	}
	
	private HashMap<InformationType, Map<String, Long>> countNewInformationByPartner() {
		List<String> instructions = new ArrayList<String>();
		List<String> facts = new ArrayList<String>();
		List<String> overall = new ArrayList<String>(); //could be computed from the other two if we ever ran into memory issues
		
		receives.forEach(msg -> {
			String partner = msg.getCounterPart().getName();
			msg.getGenerates().forEach(info -> {
				overall.add(partner);
				if (info.isIsInstruction())
					instructions.add(partner);
				else
					facts.add(partner);
			});
		});	
		HashMap<InformationType, Map<String, Long>> res = new HashMap<InformationType, Map<String, Long>>();
		res.put(InformationType.INSTRUCTION, countFromStringStream(instructions.stream()));
		res.put(InformationType.FACT, countFromStringStream(facts.stream()));
		res.put(InformationType.OVERALL, countFromStringStream(overall.stream()));
		return res;	
	}
	
	private Integer countRepetitions() {
		return receives.stream().mapToInt(s -> s.getRepeats().size()).sum();
	}
	
	private void writeMessageCounts(CSVPrinter csvPrinter) throws IOException {
		Map<Boolean, List<MessageExecution>> isSend = conv.getAuthor().getMessageExecutions().stream()
				.collect(Collectors.partitioningBy( m -> m instanceof SendMessage));	
		var sent = countByName(isSend.get(true));
		var receive = countByName(isSend.get(false));
		var interrupted = countByName(
				isSend.get(false).stream()
				.filter(msg -> {
					ReceiveMessage m = (ReceiveMessage) msg;
					return m.isIsInterrupted();
				})
				.toList()
			);
		writeForPartners(sent, "SendMsg", csvPrinter, 0L);
		writeForPartners(receive, "ReceiveMsg", csvPrinter, 0L);
		writeForPartners(interrupted, "Interrupted", csvPrinter, 0L);
	}
	
	private static Map<String, Long> countByName(List<MessageExecution> msgs) {
		return msgs.stream()
		  .collect(Collectors.groupingBy(s -> s.getCounterPart().getName(), Collectors.counting()));
	}
	
	private static Map<String, Long> countFromStringStream(Stream<String> stream) {
		return stream.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}
	
	// write according to header line partners into line
	private <T> void writeForPartners(Map<String, T> content, String firstColumn, CSVPrinter csvPrinter, T defaultValue) throws IOException {
		csvPrinter.print(firstColumn );
		partners.forEach(p -> {
			try {
				csvPrinter.print(content.getOrDefault(p, defaultValue));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}		
		});
		csvPrinter.printRecord();
	}

}

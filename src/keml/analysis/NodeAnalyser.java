package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import keml.Conversation;
import keml.ReceiveMessage;
import keml.SendMessage;
import keml.MessageExecution;

public class NodeAnalyser {
	
	Conversation conv;
	List<String> partners;
	
	
	public NodeAnalyser(Conversation conv) {
		super();
		this.conv = conv;
		this.partners = listConversationPartnersWithTrailer(); //header row for small tables
	}

	public void createCSV(String path) throws IOException {
		
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			csvPrinter.printRecord("MessagePart");
			csvPrinter.printRecord(partners);
			writeMessageCounts(conv);
			
			csvPrinter.flush();
		}
	}
	
	public List<String> listConversationPartnersWithTrailer() {
		List<String> partners = conv.getConversationPartners().stream().map(s -> s.getName()).toList();
		ArrayList<String> partnersWithTrailer = new ArrayList<String>();
		partnersWithTrailer.add("");
		partnersWithTrailer.addAll(partners);
		return partners;
	}
	
	public static void writeMessageCounts(Conversation conv) {
		Map<Boolean, List<MessageExecution>> isSend = conv.getAuthor().getMessageExecutions().stream()
				.collect(Collectors.partitioningBy( m -> m instanceof SendMessage));	
		var sent = countByName(isSend.get(true));
		var receive = countByName(isSend.get(false));
		// TODO write according to headers into line
		System.out.println(sent);
		System.out.println(receive);
	}
	
	public static Map<String, Long> countByName(List<MessageExecution> msgs) {
		return msgs.stream()
		.map(s -> s.getCounterPart().getName())
		  .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

}

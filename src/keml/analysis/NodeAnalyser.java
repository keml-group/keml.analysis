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
		this.partners = conv.getConversationPartners().stream().map(s -> s.getName()).toList(); //works as header row
	}

	public void createCSV(String path) throws IOException {
		
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			csvPrinter.printRecord("MessagePart");
			csvPrinter.print("");
			csvPrinter.printRecord(partners);
			writeMessageCounts(csvPrinter);
			
			csvPrinter.flush();
		}
	}
	
	public void writeMessageCounts(CSVPrinter csvPrinter) throws IOException {
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
	
	public static Map<String, Long> countByName(List<MessageExecution> msgs) {
		return msgs.stream()
		.map(s -> s.getCounterPart().getName())
		  .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}
	
	public <T> void writeForPartners(Map<String, T> content, String firstColumn, CSVPrinter csvPrinter, T defaultValue) throws IOException {
		// TODO write according to headers into line
		System.out.println(content);
		//List<String> r = 
		csvPrinter.printRecord(firstColumn );		
	}

}

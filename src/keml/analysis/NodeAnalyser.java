package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import keml.Conversation;

public class NodeAnalyser {
	
	// Conversation conv;
	
	public static void createCSV(Conversation conv, String path) throws IOException {
		
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
        try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
			csvPrinter.printRecord("MessagePart");
			List<String> partners = listConversationPartnersWithTrailer(conv);
			csvPrinter.printRecord(partners);
			listMessageCounts(conv);
			
			csvPrinter.flush();
		}
	}
	
	public static List<String> listConversationPartnersWithTrailer(Conversation conv) throws IOException {
		List<String> partners = conv.getConversationPartners().stream().map(s -> s.getName()).toList();
		ArrayList<String> partnersWithTrailer = new ArrayList<String>();
		partnersWithTrailer.add("");
		partnersWithTrailer.addAll(partners);
		return partners;
	}
	
	public static void listMessageCounts(Conversation conv) {
		var r = conv.getAuthor().getMessageExecutions().stream()
		.map(s -> s.getCounterPart().getName())
		.collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
		System.out.println(r);
	}

}

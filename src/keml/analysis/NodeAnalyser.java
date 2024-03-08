package keml.analysis;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import keml.Conversation;

public class NodeAnalyser {
	
	// Conversation conv;
	
	public static void createCSV(Conversation conv, String path) throws IOException {
		
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(path));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
        csvPrinter.printRecord("MessagePart");
        printConversationPartners(conv, csvPrinter);
        
		csvPrinter.flush();
	}
	
	public static void printConversationPartners(Conversation conv, CSVPrinter csvPrinter) throws IOException {
		List<String> partners = conv.getConversationPartners().stream().map(s -> s.getName()).toList();
		ArrayList<String> partnersWithTrailer = new ArrayList();
		partnersWithTrailer.add("");
		partnersWithTrailer.addAll(partners);
		csvPrinter.printRecord(partners);
	}

}

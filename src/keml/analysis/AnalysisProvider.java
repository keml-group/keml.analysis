package keml.analysis;

import java.io.IOException;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;

import keml.io.KemlFileHandler;

public class AnalysisProvider {

	public static void main(String[] args) throws Exception {
		String source = "../../graphs/objective3-2-2v4.keml";
		String target = FilenameUtils.removeExtension(source) + ".csv";

		Conversation conv = new KemlFileHandler().loadKeml(source);
		new NodeAnalyser().createCSV(conv, target);
	}

}

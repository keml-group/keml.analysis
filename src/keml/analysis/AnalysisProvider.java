package keml.analysis;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;

import keml.io.KemlFileHandler;

public class AnalysisProvider {

	public static void main(String[] args) {
		String source = "../../graphs/objective3-2-2v4.keml";
		String target = FilenameUtils.removeExtension(source) + ".csv";

		Conversation conv = new KemlFileHandler().loadKeml(source);
		NodeAnalyser.Companion.createCSV(conv, target);
	}

}

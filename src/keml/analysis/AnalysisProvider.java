package keml.analysis;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;

import keml.io.KemlFileHandler;

public class AnalysisProvider {

	public static void main(String[] args) throws Exception {
		String source = "../../graphs/objective3-2-2v5.keml";
		String target = FilenameUtils.removeExtension(source) + ".csv";
		System.out.println(target);

		Conversation conv = new KemlFileHandler().loadKeml(source);
		new NodeAnalyser(conv).createCSV(target);
	}

}

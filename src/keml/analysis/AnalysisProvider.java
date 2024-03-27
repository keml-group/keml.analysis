package keml.analysis;

import java.io.File;
import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.io.KemlFileHandler;


public class AnalysisProvider {

	public static void main(String[] args) throws Exception {
		
		String folder = "../../graphs/";		
		File[] files = new File(folder).listFiles((dir, name) -> name.toLowerCase().endsWith(".keml"));
		
		for (File file: files) {
			try {
				String source = file.getAbsolutePath();
				String basePath = FilenameUtils.removeExtension(source);
				Conversation conv = new KemlFileHandler().loadKeml(source);
				new ConversationAnalyser(conv).createCSVs(basePath);
				new TrustEvaluator(conv).analyse(basePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

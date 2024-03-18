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
				String target = FilenameUtils.removeExtension(source) + ".csv";
				Conversation conv = new KemlFileHandler().loadKeml(source);
				new NodeAnalyser(conv).createCSV(target);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

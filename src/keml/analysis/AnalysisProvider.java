package keml.analysis;

import java.io.File;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.io.KemlFileHandler;

import org.apache.poi.util.LocaleUtil;

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
				LocaleUtil.setUserLocale(Locale.US);
				
				for(int i = 2; i<= 5; i++) {
					TrustEvaluator trusty = new TrustEvaluator(conv, i);
					trusty.writeRowAnalysis(basePath+"-w"+i+"-", TrustEvaluator.standardTrustConfigurations(), 1.0F);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

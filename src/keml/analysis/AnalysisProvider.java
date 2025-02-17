package keml.analysis;

import java.io.File;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.util.LocaleUtil;

import keml.Conversation;
import keml.io.KemlFileHandler;

public class AnalysisProvider {

	public static void main(String[] args) throws Exception {

		String folder = "../../OrgPäd/Dialoge";
		File parentFolder = new File(folder);
		
		File[] srcFolders = parentFolder.listFiles((dir, name) -> dir.isDirectory());
		for (File src :  srcFolders) {
			System.out.println(src.getName());
			try {
				AnalysisProvider.analyseOneFolder(src);				
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}

	}
	
	public static void analyseOneFolder(File sourceFolder) {
		String folder = sourceFolder.getPath();
		//File sourceFolder = new File(folder);
		File targetFolder = new File(folder + "/analysis/");

		// if directory contains .keml but no ../analysis/
		if (sourceFolder.exists() && !targetFolder.exists()) {
			targetFolder.mkdirs();
		}

		System.out.println("You started the KEML analysis.\n I will read KEML files from " + folder
				+ ".\n I will write the resulting files into " + targetFolder);

		File[] files = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith("keml.json"));
		
		for (File file : files) {
			try {
				String source = file.getAbsolutePath();
				Conversation conv;
				
				conv = new KemlFileHandler().loadKemlJSON(source);
				
				String basePath = targetFolder + "/" + FilenameUtils.removeExtension(file.getName());

				new ConversationAnalyser(conv).createCSVs(basePath);
				LocaleUtil.setUserLocale(Locale.US);

				for (int i = 2; i <= 10; i++) {
					TrustEvaluator trusty = new TrustEvaluator(conv, i);
					trusty.writeRowAnalysis(basePath + "-w" + i + "-",
							TrustEvaluator.standardTrustConfigurations(conv.getConversationPartners()), 1.0F);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

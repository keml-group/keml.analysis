package keml.analysis;

import java.io.File;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;

import keml.Conversation;
import keml.io.KemlFileHandler;

import org.apache.poi.util.LocaleUtil;

 

public class AnalysisProvider {

	public static void main(String[] args) throws Exception {
		
		String folder;
		if (args.length == 0) {
			folder = "../keml.sample/introductoryExamples";
		} else {
			folder = args[0];			
		}
		
		File sourceFolder = new File(folder + "/keml/");
		File targetFolder = new File(folder+ "/analysis/");

		// if directory contains .keml but no ../analysis/
		if (sourceFolder.exists() && !targetFolder.exists())
			targetFolder.mkdirs();

		System.out.println("You started the KEML analysis.\n I will read KEML files from " + folder 
				+ ".\n I will write the resulting files into " + targetFolder);

		
		File[] files = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".keml"));
		
		for (File file: files) {
			try {
				String source = file.getAbsolutePath();
				Conversation conv = new KemlFileHandler().loadKeml(source);
				
				String basePath = targetFolder +"/" + FilenameUtils.removeExtension(file.getName());

				ArgumentationStructurer as = new ArgumentationStructurer(conv);
				new ConversationAnalyser(conv).writeGeneralCSV(basePath + "-general.csv");
				as.writeLogicArgumentationCSV(basePath + "-arguments.csv");
				as.scoresMatrix(basePath + "-scores.xlsx");
				
//				new ConversationAnalyser(conv).createCSVs(basePath);
//				LocaleUtil.setUserLocale(Locale.US);
//				
//				for(int i = 2; i<= 10; i++) {
//					TrustEvaluator trusty = new TrustEvaluator(conv, i);
//					trusty.writeRowAnalysis(
//							basePath+"-w"+i+"-",
//							TrustEvaluator.standardTrustConfigurations(conv.getConversationPartners()),
//							1.0F);
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}

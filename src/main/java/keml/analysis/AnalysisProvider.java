package keml.analysis;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.util.LocaleUtil;

import keml.Conversation;
import keml.analysis_server.utils.ExecutionMode;
import keml.io.KemlFileHandler;

public class AnalysisProvider {

	public static String runAnalysis(Path json, boolean runFurtherAnalysis, String path, ExecutionMode executionMode) throws IOException {
		Path source = json.toAbsolutePath();
		Conversation conv = new KemlFileHandler().loadKemlJSON(source.toString());
		String fileName = FilenameUtils.removeExtension(source.getFileName().toString());
		String basePath = path + "/analysis/" + fileName + "/";
		Path dir = Paths.get(basePath);
		Files.createDirectories(dir);
		String basePathFile = basePath + fileName;
		new ConversationAnalyser(conv).createCSVs(basePathFile);
		LocaleUtil.setUserLocale(Locale.US);
		for (int i = 2; i <= 10; i++) {
			TrustEvaluator trusty = new TrustEvaluator(conv, i);
			trusty.writeRowAnalysis(basePathFile + "-w" + i + "-",
					TrustEvaluator.standardTrustConfigurations(conv.getConversationPartners()), 1.0F);
		}
		if (runFurtherAnalysis) {
			boolean success = PythonExecutor.runPythonScript(fileName, path, executionMode);
			if (!success) {
				throw new IOException("Failed to execute python script");
			}
		}
		return basePath;
	}
	
	public static void main(String[] args) throws IOException {
		String folder;
		File file;
		File sourceFolder;
		boolean runFurtherAnalysis = false;
		if (args.length == 0) {
			folder = "../keml.sample/introductoryExamples";
			sourceFolder = new File(folder + "/keml/");
			file = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"))[0];
		} else if (args.length == 1){
			runFurtherAnalysis = Boolean.getBoolean(args[0]);
			folder = "../keml.sample/introductoryExamples";
			sourceFolder = new File(folder + "/keml/");
			file = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"))[0];
		} else if (args.length == 2){
			runFurtherAnalysis = Boolean.getBoolean(args[0]);
			folder = args[1];
			sourceFolder = new File(folder + "/keml/");
			file = sourceFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"))[0];
		} else {
			runFurtherAnalysis = Boolean.getBoolean(args[0]);
			folder = args[1];
			sourceFolder = new File(folder + "/keml/");
			file = new File(sourceFolder.getName() + args[2]);
		}
		runAnalysis(file.toPath(), runFurtherAnalysis, folder, ExecutionMode.STANDARD);
	}
}

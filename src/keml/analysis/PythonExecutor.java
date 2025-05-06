package keml.analysis;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class PythonExecutor {

	public static boolean runPythonScript(String dirName, String path) {
		try {
			List<String> commands = new ArrayList<>();
			commands.add("python3");
			commands.add("/app/python-scripts/main.py");
//			commands.add("../keml.analysis/src/keml/analysis/py/main.py");
			commands.add(dirName);
			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println(line);
			}
			int exitCode = process.waitFor();
			return exitCode == 0;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}

	}

}

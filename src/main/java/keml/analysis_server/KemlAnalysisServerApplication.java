package keml.analysis_server;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import keml.analysis_server.utils.ExecutionMode;

@SpringBootApplication
public class KemlAnalysisServerApplication {
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(KemlAnalysisServerApplication.class, args);
		if (args.length > 0) {
			try {
				ExecutionMode.valueOf(args[0]);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				System.err.println("Invalid ExecutionMode: " + args[0] + ". Valid values are: " + Arrays.toString(ExecutionMode.values()));
				int exitCode = SpringApplication.exit(context, () -> 42);
				System.exit(exitCode);
			}
		}
	}
}

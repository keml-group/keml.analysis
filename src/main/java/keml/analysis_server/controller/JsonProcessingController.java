package keml.analysis_server.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import keml.analysis_server.services.JsonProcessorService;
import keml.analysis_server.utils.ExecutionMode;

@RestController
@RequestMapping("/api")
public class JsonProcessingController {

	private final JsonProcessorService jsonProcessorService;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final List<String> args;
	private final ExecutionMode executionMode;

	public JsonProcessingController(JsonProcessorService jsonProcessorService, ApplicationArguments args) {
		this.jsonProcessorService = jsonProcessorService;
		this.args = args.getNonOptionArgs();
		if (this.args.size() > 0) {
			this.executionMode = ExecutionMode.valueOf(this.args.get(0));
		} else {
			this.executionMode = ExecutionMode.STANDARD;
		}
	}

	private final String APPLICATION_ZIP_VALUE = "application/zip";

	@PostMapping(path = "/process-json", consumes = MediaType.APPLICATION_JSON_VALUE, produces = APPLICATION_ZIP_VALUE)
	public ResponseEntity<?> processAndRespond(@RequestParam boolean runFurtherAnalysis, @RequestBody JsonNode jsonNode) {
		try {
			String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
			String basePath = this.args.size() > 1 ? this.args.get(1) : null;			
			byte[] zipBytes = this.jsonProcessorService.processJsonAndReturn(jsonNode, timestamp, runFurtherAnalysis, basePath, this.executionMode);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentDisposition(
					ContentDisposition.attachment().filename("input_" + timestamp + ".zip").build());
			headers.setContentType(MediaType.valueOf(APPLICATION_ZIP_VALUE));
			return new ResponseEntity<>(zipBytes, headers, HttpStatus.OK);
		} catch (IOException e) {
			return ResponseEntity.internalServerError()
					.body(objectMapper.createObjectNode().put("error", e.getMessage()));
		}
	}
}

package keml.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Server {

	private static File zipAnalysisFiles(String analysisFiles) throws IOException {
		String target = "../keml.sample/introductoryExamples/zipped/test.zip";
		File sourceFolder = new File(analysisFiles);
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(target))) {
			File[] files = sourceFolder.listFiles();
			for (File file : files) {
				String zipEntryName = sourceFolder.toPath().relativize(file.toPath()).toString();
				try {
					zipOut.putNextEntry(new ZipEntry(zipEntryName));
					Files.copy(file.toPath(), zipOut);
					zipOut.closeEntry();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return new File(target);
	}

	private static File createFile(char[] buf) throws IOException {
		File json = new File("../keml.sample/introductoryExamples/keml/test.json");
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(json))) {
			json.createNewFile();
			bw.write(buf);
			return json;
		} catch (IOException e) {
			e.printStackTrace();
			throw new IOException("JSON file could not be created");
		}
	}

	private static class ClientConnection implements Runnable {

		private Socket clientSocket;

		private ClientConnection(Socket clientSocket) {
			this.clientSocket = clientSocket;
		}

		@Override
		public void run() {
			try (BufferedReader in = new BufferedReader(
					new InputStreamReader(clientSocket.getInputStream(), Charset.forName(UTF8_CHARSET)));
					OutputStream out = clientSocket.getOutputStream()) {
				String line = in.readLine();
//				if (!line.startsWith("POST")) {
//					System.out.println("Request is not of type POST");
//					clientSocket.close();
//					throw new SocketException();
//				}
				int contentLength = 0;
				while ((line = in.readLine()) != null) {
					if (line.isEmpty()) {
						break;
					}
					if (line.startsWith("Content-Length")) {
						contentLength = Integer.parseInt(line.split(":")[1].trim());
					}
				}
				char[] inputBuffer = new char[contentLength];
				in.read(inputBuffer);
				File json = createFile(inputBuffer);
				try {
					AnalysisProvider.runAnalysis(json);
				} catch (Exception e) {
					e.printStackTrace();
				}
				File zipFile = zipAnalysisFiles("../keml.sample/introductoryExamples/analysis/test/");
				String header = "HTTP/1.1 200 OK\r\n" + "Content-Type: application/zip\r\n" + "Content-Length: "
						+ zipFile.length() + "\r\n" + "Content-Disposition: attachment; filename=\"" + zipFile.getName()
						+ "\"\r\n" + "Connection: close\r\n\r\n";
				out.write(header.getBytes());
				try (FileInputStream fileInput = new FileInputStream(zipFile)) {
					byte[] outputBuffer = new byte[4096];
					int bytesRead;
					while ((bytesRead = fileInput.read(outputBuffer)) != -1) {
						out.write(outputBuffer, 0, bytesRead);
					}
				}
				out.flush();
//				terminate();
			} catch (SocketException e) {
				System.out.println("ClientSocket closed");
			} catch (IOException e) {
				try {
					clientSocket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				e.printStackTrace();
			}
			clientSockets.remove(clientSocket);
		}

	}

	private static final int PORT = 12345;
	private static final String UTF8_CHARSET = "UTF-8";
	private static ServerSocket serverSocket;
	private static List<Socket> clientSockets = new LinkedList<>();

	private static void terminate() {
		try {
			serverSocket.close();
			for (Socket socket : clientSockets) {
				socket.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			Server.serverSocket = serverSocket;
			System.out.println("Server listening on port " + PORT);
			while (!serverSocket.isClosed()) {
				Socket clientSocket = serverSocket.accept();
				clientSockets.add(clientSocket);
				System.out.println("  connected to " + clientSocket.getInetAddress().getHostName() + " ("
						+ clientSocket.getInetAddress().getHostAddress() + ")");
				System.out.println("  local port: " + clientSocket.getLocalPort());
				System.out.println("  remote port: " + clientSocket.getPort());
				ClientConnection connection = new ClientConnection(clientSocket);
				Thread thread = new Thread(connection);
				thread.start();
			}
		} catch (SocketException s) {
			System.out.println("ServerSocket closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private List<String> validPaths;

    public Server(){}

    public Server(List<String> validPaths) {
        this.validPaths = validPaths;
    }

    public void setValidPaths(List<String> validPaths) {
        this.validPaths = validPaths;
    }

    public void listen(int port) throws IOException {
        final ExecutorService threadPool = Executors.newFixedThreadPool(64);
        try (final ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, validPaths);
                threadPool.execute(clientHandler);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private BufferedOutputStream out;
        private BufferedReader in;
        private final List<String> validPaths;

        public ClientHandler(Socket socket, List<String> validPaths) {
            this.validPaths = validPaths;

            try {
                out = new BufferedOutputStream(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                final String requestLine = in.readLine();
                final String[] parts = requestLine.split(" ");

                if (parts.length != 3) {
                    badRequest(out);
                    return;
                }

                final String path = parts[1];
                if (!path.startsWith("/")) {
                    badRequest(out);
                    return;
                }

                if (!validPaths.contains(path)) {
                    notFound(out);
                    return;
                }

                final Path filePath = Path.of(".", "public", path);
                final String mimeType = Files.probeContentType(filePath);

                if (path.equals("/classic.html")) {
                    final String template = Files.readString(filePath);
                    final byte[] content = template.replace(
                            "{time}",
                            LocalDateTime.now().toString()
                    ).getBytes();
                    out.write((
                            "HTTP/1.1 200 OK\r\n" +
                                    "Content-Type: " + mimeType + "\r\n" +
                                    "Content-Length: " + content.length + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"
                    ).getBytes());
                    out.write(content);
                    out.flush();
                    return;
                }

                final long length = Files.size(filePath);
                OK(out, mimeType, length, filePath);

            } catch (IOException e) {
                e.printStackTrace();

            }
        }

        private static void badRequest(BufferedOutputStream out) throws IOException {
            out.write((
                    "HTTP/1.1 400 Bad Request\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        private static void notFound(BufferedOutputStream out) throws IOException {
            out.write((
                    "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Length: 0\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            out.flush();
        }

        private static void OK(BufferedOutputStream out, String mimeType, long length, Path filePath) throws IOException {
            out.write((
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + mimeType + "\r\n" +
                            "Content-Length: " + length + "\r\n" +
                            "Connection: close\r\n" +
                            "\r\n"
            ).getBytes());
            if (filePath != null)
                Files.copy(filePath, out);
            out.flush();
        }
    }
}

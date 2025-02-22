import org.apache.commons.codec.Charsets;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    final static List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE");
    private ConcurrentHashMap<String, Handler> handlers = new ConcurrentHashMap<>();
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
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                threadPool.execute(clientHandler);
            }
        }
    }

    public void addHandler(String method, String path, Handler handler){
        handlers.put(method + path, handler);
    }

    private static class ClientHandler implements Runnable {
        private BufferedOutputStream out;
        private BufferedInputStream in;
        private final Server server;

        public ClientHandler(Socket socket, Server server){
            this.server = server;

            try {
                out = new BufferedOutputStream(socket.getOutputStream());
                in = new BufferedInputStream(socket.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // лимит на request line + заголовки
                final int limit = 4096;

                in.mark(limit);
                final byte[] buffer = new byte[limit];
                final int read = in.read(buffer);

                // ищем request line
                final byte[] requestLineDelimiter = new byte[]{'\r', '\n'};
                final int requestLineEnd = indexOf(buffer, requestLineDelimiter, 0, read);
                if (requestLineEnd == -1) {
                    badRequest(out);
                    return;
                }

                // читаем request line
                final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
                if (requestLine.length != 3) {
                    badRequest(out);
                    return;
                }

                final String method = requestLine[0];
                if (!allowedMethods.contains(method)) {
                    badRequest(out);
                    return;
                }
                System.out.println(method);

                String path = requestLine[1];
                if (!path.startsWith("/")) {
                    badRequest(out);
                    return;
                }
                System.out.println(path);

                List<NameValuePair> params = URLEncodedUtils.parse(new URI(path), Charset.forName("UTF-8"));
                Map<String, String> mapParams = new HashMap<>();

                for (NameValuePair param : params) {
                    System.out.println(param.getName() + " : " + param.getValue());
                    mapParams.put(param.getName(), param.getValue());
                }

                path = path.split("\\?")[0];

                // ищем заголовки
                final byte[] headersDelimiter = new byte[]{'\r', '\n', '\r', '\n'};
                final int headersStart = requestLineEnd + requestLineDelimiter.length;
                final int headersEnd = indexOf(buffer, headersDelimiter, headersStart, read);
                if (headersEnd == -1) {
                    badRequest(out);
                    return;
                }

                // отматываем на начало буфера
                in.reset();
                // пропускаем requestLine
                in.skip(headersStart);

                final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
                final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
                System.out.println(headers);

                // для GET тела нет
                String body = "";
                if (!method.equals("GET")) {
                    in.skip(headersDelimiter.length);
                    // вычитываем Content-Length, чтобы прочитать body
                    final Optional<String> contentLength = extractHeader(headers, "Content-Length");
                    if (contentLength.isPresent()) {
                        final int length = Integer.parseInt(contentLength.get());
                        final byte[] bodyBytes = in.readNBytes(length);

                        body = new String(bodyBytes, StandardCharsets.UTF_8);
                        System.out.println(body);
                    }
                }

                final Optional<String> contentType = extractHeader(headers, "Content-Type");
                System.out.println(contentType.get());
                if (contentType.get().equals("application/x-www-form-urlencoded") && !body.isEmpty()) {
                    List<NameValuePair> postParams = new ArrayList<>();
                    String[] bodyParams = body.split("&");
                    for (String param : bodyParams) {
                        String[] keyValue = param.split("=");
                        postParams.add(new BasicNameValuePair(keyValue[0], keyValue[1]));
                    }
                    for (NameValuePair param : postParams) {
                        System.out.println(param.getName() + " : " + param.getValue());
                    }
                }


//                String handlerKey = method + path;
//                if (!server.validPaths.contains(path) && !server.handlers.containsKey(handlerKey)) {
//                    notFound(out);
//                    return;
//                }
//
//                if (server.validPaths.contains(path)){
//                    final Path filePath = Path.of(".", "public", path);
//                    final String mimeType = Files.probeContentType(filePath);
//
//                    if (path.equals("/classic.html")) {
//                        final String template = Files.readString(filePath);
//                        final byte[] content = template.replace(
//                                "{time}",
//                                LocalDateTime.now().toString()
//                        ).getBytes();
//                        out.write((
//                                "HTTP/1.1 200 OK\r\n" +
//                                        "Content-Type: " + mimeType + "\r\n" +
//                                        "Content-Length: " + content.length + "\r\n" +
//                                        "Connection: close\r\n" +
//                                        "\r\n"
//                        ).getBytes());
//                        out.write(content);
//                        out.flush();
//                        return;
//                    }
//
//                    final long length = Files.size(filePath);
//                    OK(out, mimeType, length, filePath);
//                }
//
//                Request request = new Request(method, path, mapParams, headers, body);
//                server.handlers.get(handlerKey).handle(request, out);


            } catch (URISyntaxException | IOException e) {
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

        private static int indexOf(byte[] array, byte[] target, int start, int max) {
            outer:
            for (int i = start; i < max - target.length + 1; i++) {
                for (int j = 0; j < target.length; j++) {
                    if (array[i + j] != target[j]) {
                        continue outer;
                    }
                }
                return i;
            }
            return -1;
        }

        private static Optional<String> extractHeader(List<String> headers, String header) {
            return headers.stream()
                    .filter(o -> o.startsWith(header))
                    .map(o -> o.substring(o.indexOf(" ")))
                    .map(String::trim)
                    .findFirst();
        }
    }
}

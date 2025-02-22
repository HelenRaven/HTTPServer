import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        final Server server = getServer();

        // добавление хендлеров (обработчиков)
        server.addHandler("GET", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                byte[] content = "GET Message for u".getBytes();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html \r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();
            }
        });
        server.addHandler("POST", "/messages", new Handler() {
            public void handle(Request request, BufferedOutputStream responseStream) throws IOException {
                byte[] content = "POST Message for u".getBytes();
                responseStream.write((
                        "HTTP/1.1 200 OK\r\n" +
                                "Content-Type: text/html \r\n" +
                                "Content-Length: " + content.length + "\r\n" +
                                "Connection: close\r\n" +
                                "\r\n"
                ).getBytes());
                responseStream.write(content);
                responseStream.flush();
            }
        });

        server.listen(9999);
    }

    private static Server getServer() {
        final var validPaths = List.of("/index.html",
                                                 "/spring.svg",
                                                 "/spring.png",
                                                 "/resources.html",
                                                 "/styles.css",
                                                 "/app.js",
                                                 "/links.html",
                                                 "/forms.html",
                                                 "/classic.html",
                                                 "/events.html",
                                                 "/events.js");
        return new Server(validPaths);
    }
}
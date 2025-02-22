import java.io.IOException;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        final var validPaths = List.of("/index.html", "/spring.svg", "/spring.png", "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html", "/classic.html", "/events.html", "/events.js");
        final Server server = new Server(validPaths);
        server.listen(9999);
    }
}
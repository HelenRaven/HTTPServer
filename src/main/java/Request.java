import java.util.ArrayList;
import java.util.List;

public class Request {
    private String method;
    private String path;
    private List<String> headers = new ArrayList<>();
   // private String body;

    public Request(String method, String path, List<String> headers) {
        this.method = method;
        this.path = path;
        this.headers = headers;
       // this.body = body;
    }

    public Request(){}

    public void setMethod(String method) {
        this.method = method;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

  //  public void setBody(String body) {
  //      this.body = body;
 //   }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public List<String> getHeaders() {
        return headers;
    }

  //  public String getBody() {
  //      return body;
  //  }
}

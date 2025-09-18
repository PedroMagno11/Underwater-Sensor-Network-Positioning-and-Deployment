package ipqm.lafiaa.algoritmogenetico.utils;

import java.net.http.HttpResponse;

public class Response {
    private Object body;
    private int statusCode;

    public Response(HttpResponse response) {
        body = response.body();
        statusCode = response.statusCode();
    }

    public Object getBody() {
        return body;
    }

    public int getStatusCode() {
        return statusCode;
    }
}

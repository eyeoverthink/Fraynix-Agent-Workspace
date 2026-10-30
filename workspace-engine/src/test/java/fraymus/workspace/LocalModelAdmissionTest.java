package fraymus.workspace;
import com.sun.net.httpserver.*;
import org.json.*;
import org.junit.Test;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

public class LocalModelAdmissionTest {
    @Test public void freshDigestMetadataPassesWhileProxyChangesFail() throws Exception {
        var tag=new JSONObject().put("name","fixture:local").put("digest","a".repeat(64)).put("size",1000);var show=new JSONObject().put("modelfile","FROM /local/weights");
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/",e->{byte[] body=(e.getRequestURI().getPath().endsWith("tags")?new JSONObject().put("models",new JSONArray().put(tag)):show).toString().getBytes(StandardCharsets.UTF_8);e.sendResponseHeaders(200,body.length);try(var out=e.getResponseBody()){out.write(body);}});server.start();
        try {String base="http://127.0.0.1:"+server.getAddress().getPort();assertEquals("a".repeat(64),LocalModelAdmission.verify(base,"fixture:local").getString("digest"));
            show.put("details",new JSONObject().put("remote_model","hidden-proxy"));assertThrows(IOException.class,()->LocalModelAdmission.verify(base,"fixture:local"));show.remove("details");
            tag.put("remote_host","https://remote.invalid");assertThrows(IOException.class,()->LocalModelAdmission.verify(base,"fixture:local"));tag.remove("remote_host");
            tag.put("digest","");assertThrows(IOException.class,()->LocalModelAdmission.verify(base,"fixture:local"));
        }finally{server.stop(0);}
    }
    @Test public void redirectsAndNonlocalEndpointsAreRejectedWithoutFollowing() throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/api/",e->{e.getResponseHeaders().set("Location","https://example.com/");e.sendResponseHeaders(302,-1);e.close();});server.start();
        try {assertThrows(IOException.class,()->LocalModelAdmission.verify("http://127.0.0.1:"+server.getAddress().getPort(),"fixture:local"));
            assertThrows(IOException.class,()->LocalModelAdmission.verify("https://example.com","fixture:local"));
            assertThrows(IOException.class,()->LocalModelAdmission.verify("http://127.0.0.1","fixture:cloud"));
        }finally{server.stop(0);}
    }
}

import com.sun.net.httpserver.*;
import java.net.*;
import java.nio.file.*;
import java.sql.*;

public class App {

    static String URL  = System.getenv("DB_URL");
    static String USER = System.getenv("DB_USER");
    static String PASS = System.getenv("DB_PASS");

    public static void main(String[] args) throws Exception {

        Connection c=null;
        while(c==null){
            try{
                c=DriverManager.getConnection(URL,USER,PASS);
            }catch(Exception e){
                Thread.sleep(2000);
            }
        }

        Statement s=c.createStatement();
        s.execute("""
          create table if not exists scores(
            name text primary key,
            clicks int not null
          )
        """);
        s.close();
        c.close();

        HttpServer server=HttpServer.create(new InetSocketAddress(8000),0);

        server.createContext("/",e->{
            try{
                byte[] html=Files.readAllBytes(Path.of("index.html"));
                e.getResponseHeaders().add("Content-Type","text/html");
                e.sendResponseHeaders(200,html.length);
                e.getResponseBody().write(html);
            }catch(Exception ex){
                e.sendResponseHeaders(500,0);
            }
            e.close();
        });

        server.createContext("/score",e->{
            try(Connection cc=DriverManager.getConnection(URL,USER,PASS)){
                String[] p=new String(e.getRequestBody().readAllBytes()).split(",");
                String name=p[0];
                int clicks=Integer.parseInt(p[1]);

                PreparedStatement ps=cc.prepareStatement("""
                  insert into scores(name,clicks)
                  values(?,?)
                  on conflict(name)
                  do update set clicks=least(scores.clicks,excluded.clicks)
                """);

                ps.setString(1,name);
                ps.setInt(2,clicks);
                ps.executeUpdate();

                e.sendResponseHeaders(200,0);
            }catch(Exception ex){
                e.sendResponseHeaders(500,0);
            }
            e.close();
        });

        server.createContext("/scores",e->{
            try(Connection cc=DriverManager.getConnection(URL,USER,PASS);
                Statement ss=cc.createStatement()){

                ResultSet r=ss.executeQuery(
                  "select name,clicks from scores order by clicks asc limit 10"
                );

                StringBuilder json=new StringBuilder("[");
                while(r.next()){
                    json.append("{\"name\":\"")
                        .append(r.getString(1))
                        .append("\",\"clicks\":")
                        .append(r.getInt(2))
                        .append("},");
                }
                if(json.charAt(json.length()-1)==',')
                    json.deleteCharAt(json.length()-1);
                json.append("]");

                byte[] out=json.toString().getBytes();
                e.getResponseHeaders().add("Content-Type","application/json");
                e.sendResponseHeaders(200,out.length);
                e.getResponseBody().write(out);
            }catch(Exception ex){
                e.sendResponseHeaders(500,0);
            }
            e.close();
        });

        server.start();
        System.out.println("Server started on port 8000");
    }
}
package com.game1000;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import java.sql.*;
import java.util.*;

@SpringBootApplication
@RestController
public class App implements CommandLineRunner {

    static String URL  = System.getenv("DB_URL");
    static String USER = System.getenv("DB_USER");
    static String PASS = System.getenv("DB_PASS");

    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
        System.out.println("Game1000 Spring Boot started");
    }

    @Override
    public void run(String... args) throws Exception {
        Connection c = null;
        while (c == null) {
            try {
                System.out.println("⏳ Waiting for database...");
                c = DriverManager.getConnection(URL, USER, PASS);
            } catch (Exception e) {
                Thread.sleep(2000);
            }
        }

        try (Statement s = c.createStatement()) {
            s.execute("""
              create table if not exists scores(
                name text primary key,
                clicks int not null
              )
            """);
            System.out.println("✅ scores table ready");
        }

        c.close();
    }

    @PostMapping("/score")
    public void saveScore(@RequestBody String body) throws Exception {
        String[] p = body.split(",");
        String name = p[0];
        int clicks = Integer.parseInt(p[1]);

        try (Connection c = DriverManager.getConnection(URL, USER, PASS)) {
            PreparedStatement ps = c.prepareStatement("""
              insert into scores(name, clicks)
              values (?, ?)
              on conflict(name)
              do update set clicks = least(scores.clicks, excluded.clicks)
            """);
            ps.setString(1, name);
            ps.setInt(2, clicks);
            ps.executeUpdate();
        }
    }

    @GetMapping("/scores")
    public List<Map<String,Object>> scores() throws Exception {
        List<Map<String,Object>> list = new ArrayList<>();

        try (Connection c = DriverManager.getConnection(URL, USER, PASS);
             Statement s = c.createStatement()) {

            ResultSet r = s.executeQuery(
              "select name, clicks from scores order by clicks asc limit 10"
            );

            while (r.next()) {
                Map<String,Object> m = new HashMap<>();
                m.put("name", r.getString("name"));
                m.put("clicks", r.getInt("clicks"));
                list.add(m);
            }
        }
        return list;
    }
}
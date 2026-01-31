
package com.example.learnacademy;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;



import com.example.learnacademy.model.User;
import com.example.learnacademy.repository.UserRepository;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class learnacademyApplication {

	@Autowired
	UserRepository repo;
	@Value("${spring.datasource.url:NOT_FOUND}")
	private String dbUrl;
	@Autowired
	private JdbcTemplate jdbcTemplate;


	@PostConstruct
	public void checkDbUrl() {
		System.out.println("DB URL FROM SPRING = " + dbUrl);
	}
	@PostConstruct
	public void printTables() {
		try {
			List<String> tables = jdbcTemplate.queryForList(
					"SELECT table_name FROM information_schema.tables WHERE table_schema='public'",
					String.class
			);

			System.out.println("✅ TABLES IN CONNECTED DATABASE:");
			tables.forEach(t -> System.out.println("👉 " + t));

		} catch (Exception e) {
			System.out.println("❌ Failed to read tables:");
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		String environment = System.getenv("ENVIRONMENT");

		System.out.println("ENVIRONMENT = " + environment);

		if (environment == null || !"production".equals(environment)) {

			Dotenv dotenv = Dotenv.configure()
					.ignoreIfMissing()
					.load();

			dotenv.entries().forEach(e ->
					System.setProperty(e.getKey(), e.getValue())
			);
		}
		SpringApplication.run(learnacademyApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello() {
		return "Hello from your API";
	}

}
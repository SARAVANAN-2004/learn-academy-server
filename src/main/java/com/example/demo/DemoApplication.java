
package com.example.demo;

import java.util.List;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;


import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class DemoApplication {

	@Autowired
	UserRepository repo;
	@Value("${spring.datasource.url:NOT_FOUND}")
	private String dbUrl;

	@PostConstruct
	public void checkDbUrl() {
		System.out.println("DB URL FROM SPRING = " + dbUrl);
	}

	public static void main(String[] args) {
		String dbUrl = System.getenv("DB_URL");
		String dbUser = System.getenv("DB_USERNAME");
		String dbPass = System.getenv("DB_PASSWORD");

		System.out.println("DB_URL = " + dbUrl);
		System.out.println("DB_USERNAME = " + dbUser);
		System.out.println("DB_PASSWORD = " + (dbPass != null ? "***" : "null"));

//		Dotenv dotenv = Dotenv.configure()
//				.ignoreIfMissing()
//				.load();
//
//		dotenv.entries().forEach(e ->
//				System.setProperty(e.getKey(), e.getValue())
//		);
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/hello")
	public String hello() {
		return "Hello from your API";
	}

	// Get all users
	@GetMapping("/users")
	public List<User> getUsers() {
		return repo.findAll();
	}

	// Create user
	@PostMapping("/users")
	public User createUser(@RequestBody User user) {
		return repo.save(user);
	}
}
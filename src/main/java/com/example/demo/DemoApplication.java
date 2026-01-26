
package com.example.demo;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import io.github.cdimascio.dotenv.Dotenv;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class DemoApplication {

	@Autowired
	UserRepository repo;

	public static void main(String[] args) {

//		Dotenv dotenv = Dotenv.configure()
//				.ignoreIfMissing()
//				.load();
//
//		dotenv.entries().forEach(e ->
//				System.setProperty(e.getKey(), e.getValue())
//		);

		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/api/hello")
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
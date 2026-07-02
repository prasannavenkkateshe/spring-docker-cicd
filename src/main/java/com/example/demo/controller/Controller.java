package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.User;
import com.example.demo.repos.UserRepository;

@RestController
@CrossOrigin
public class Controller {
	
	@GetMapping("/sayHello")
	public String sayHi() {
		return "Hello from Springboot app....";
	}
	
	@Autowired
	private UserRepository userRepository;

	@PostMapping("/users")
	public User createUser(@RequestParam String name) {
	    User user = new User();
	    user.setName(name);
	    return userRepository.save(user);
	}

	@GetMapping("/users")
	public List<User> getUsers() {
	    return userRepository.findAll();
	}

}

package com.firas.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main entry point for the Spring Boot Generator application.
 * 
 * This application provides a REST API for generating Spring Boot projects
 * with customizable dependencies, CRUD operations from SQL schemas, and
 * various project templates.
 * 
 * @author Firas Baklouti
 * @version 1.0
 * @since 2025-12-01
 */
@SpringBootApplication
public class BackendApplication {

	/**
	 * Main method to start the Spring Boot application.
	 * 
	 * @param args Command line arguments passed to the application
	 */
	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}

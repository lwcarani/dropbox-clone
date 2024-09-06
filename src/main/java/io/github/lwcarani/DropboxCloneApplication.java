package io.github.lwcarani;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import io.github.lwcarani.cli.DropboxCliRunner;

// Main Spring Boot application class for the Dropbox Clone application
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
// Exclude DataSourceAutoConfiguration as we're not using a database in this application

// Specify the base packages to scan for Spring components
@ComponentScan(basePackages = { "io.github.lwcarani", "io.github.lwcarani.config" })
public class DropboxCloneApplication {

	public static void main(String[] args) {
		// Start the Spring application and get the application context
		ConfigurableApplicationContext context = SpringApplication.run(DropboxCloneApplication.class, args);

		// Retrieve the DropboxCliRunner bean from the application context
		DropboxCliRunner cliRunner = context.getBean(DropboxCliRunner.class);

		// Run the CLI application
		cliRunner.run(args);

		// Add a shutdown hook
		context.registerShutdownHook();
	}
}
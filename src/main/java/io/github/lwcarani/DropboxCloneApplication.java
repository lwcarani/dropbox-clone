package io.github.lwcarani;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import io.github.lwcarani.cli.DropboxCliRunner;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@ComponentScan(basePackages = { "io.github.lwcarani", "io.github.lwcarani.config" })
public class DropboxCloneApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(DropboxCloneApplication.class, args);

		DropboxCliRunner cliRunner = context.getBean(DropboxCliRunner.class);
		cliRunner.run(args);
	}
}
package com.smartdocfinder.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableJpaRepositories(basePackages = "com.smartdocfinder.core.repository")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SmartDocFinderBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartDocFinderBackendApplication.class, args);
	}
}

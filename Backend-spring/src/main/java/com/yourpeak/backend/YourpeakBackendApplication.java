package com.yourpeak.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class YourpeakBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(YourpeakBackendApplication.class, args);
	}

}

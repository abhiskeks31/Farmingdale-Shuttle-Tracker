package com.farmingdale.shuttle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShuttleBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShuttleBackendApplication.class, args);
	}

}

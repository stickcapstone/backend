package com.veriweb.veriweb_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VeriwebBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(VeriwebBackendApplication.class, args);
	}

}

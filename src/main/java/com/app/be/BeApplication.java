package com.app.be;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;

@SpringBootApplication
//@SpringBootApplication(exclude = {SessionAutoConfiguration.class})
//@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, SessionAutoConfiguration.class})
public class BeApplication {

	public static void main(String[] args) {
		SpringApplication.run(BeApplication.class, args);
	}

}

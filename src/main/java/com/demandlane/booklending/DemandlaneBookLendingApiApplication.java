package com.demandlane.booklending;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class DemandlaneBookLendingApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemandlaneBookLendingApiApplication.class, args);
	}

}

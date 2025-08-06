package br.gov.ma.idox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class IdoxApplication {

	public static void main(String[] args) {
		SpringApplication.run(IdoxApplication.class, args);
	}

}

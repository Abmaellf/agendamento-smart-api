package com.agendamento.smart;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AgendaSmartApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgendaSmartApplication.class, args);
	}
}

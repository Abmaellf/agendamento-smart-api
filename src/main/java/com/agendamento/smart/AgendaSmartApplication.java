package com.agendamento.smart;



import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
public class AgendaSmartApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgendaSmartApplication.class, args);
	}
	
//	@RequestMapping("/list") ou
//	@GetMapping("/hello")
//	public String ListMenssage() {
//		return "Ola Mundo";
//	}
}

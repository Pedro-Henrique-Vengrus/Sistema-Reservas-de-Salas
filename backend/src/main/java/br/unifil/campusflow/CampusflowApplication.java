package br.unifil.campusflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync   // envio de e-mail fora da thread da requisicao
public class CampusflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusflowApplication.class, args);
    }
}

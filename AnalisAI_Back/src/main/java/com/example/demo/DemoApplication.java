package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableScheduling // ✅ Ativa o agendador de tarefas automáticas (scheduler)
public class DemoApplication {

    public static void main(String[] args) {
        // Tenta carregar o arquivo .env da raiz do projeto
        Dotenv dotenv = Dotenv.configure()
                              .ignoreIfMissing()   // não quebra se o arquivo não existir
                              .load();

        // Adiciona as variáveis do .env ao System properties (Spring consegue ler)
        dotenv.entries().forEach(entry ->
            System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(DemoApplication.class, args);
    }
}
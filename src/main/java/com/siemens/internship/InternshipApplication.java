package com.siemens.internship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync //needed for @Async
public class InternshipApplication {

	public static void main(String[] args) {

		SpringApplication.run(InternshipApplication.class, args);
	}

	//Bean for ThreadPoolTaskExecutor
	@Bean(name = "itemTaskExecutor")
	public Executor itemTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(20);
		executor.setThreadNamePrefix("ItemThread-");
		executor.initialize();
		return executor;
	}

}

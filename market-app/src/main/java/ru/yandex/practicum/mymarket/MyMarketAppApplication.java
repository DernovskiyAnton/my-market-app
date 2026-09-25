package ru.yandex.practicum.mymarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MyMarketAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyMarketAppApplication.class, args);
	}

}

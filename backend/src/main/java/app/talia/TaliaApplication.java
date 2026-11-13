package app.talia;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TaliaApplication {

	public static void main(String[] args) {
		SpringApplication.run(TaliaApplication.class, args);
	}

}

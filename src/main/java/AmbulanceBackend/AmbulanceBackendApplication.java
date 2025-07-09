package AmbulanceBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class})
public class AmbulanceBackendApplication {
    public static void main(String[] args) {
        SpringApplication.run(AmbulanceBackendApplication.class, args);
    }
}

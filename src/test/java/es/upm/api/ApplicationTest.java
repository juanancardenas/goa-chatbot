package es.upm.api;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ApplicationTest {

    @Test
    void mainShouldDelegateToSpringApplicationRun() {
        String[] args = {"--spring.profiles.active=test"};

        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            Application.main(args);

            springApplication.verify(() -> SpringApplication.run(Application.class, args));
        }
    }
}

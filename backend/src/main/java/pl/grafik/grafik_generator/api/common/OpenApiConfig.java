package pl.grafik.grafik_generator.api.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI grafikGeneratorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Grafik Generator API")
                        .version("v1")
                        .description("REST API aplikacji Grafik Generator — zarządzanie sekcjami, pracownikami, "
                                + "konfiguracjami grafiku oraz uruchamianie generacji algorytmem genetycznym.")
                        .contact(new Contact().name("Grafik Generator"))
                        .license(new License().name("Internal")));
    }
}

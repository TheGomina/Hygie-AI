package com.hygie.patientservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de la documentation OpenAPI pour le Patient Service.
 *
 * Cette classe configure la documentation OpenAPI (anciennement Swagger) pour décrire
 * les API REST du service patient, facilitant ainsi l'intégration et l'utilisation
 * par d'autres services et applications.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * Crée et configure l'objet OpenAPI principal.
     *
     * @return Une instance configurée de OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Assertion #1: Vérification que le nom de l'application est défini
        assert applicationName != null && !applicationName.isBlank() :
            "Le nom de l'application doit être défini";

        final String securitySchemeName = "bearerAuth";

        final OpenAPI api = new OpenAPI()
            .info(new Info()
                .title("API " + applicationName)
                .version("1.0.0")
                .description("API REST pour la gestion des patients dans le système Hygie-AI")
                .contact(new Contact()
                    .name("Hygie-AI Team")
                    .email("contact@hygie-ai.com")
                    .url("https://hygie-ai.com"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://hygie-ai.com/license")))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));

        // Assertion #2: Vérification que l'objet OpenAPI a été correctement construit
        assert api.getInfo() != null : "Les informations de l'API doivent être définies";
        assert api.getComponents() != null : "Les composants de l'API doivent être définis";

        return api;
    }
}

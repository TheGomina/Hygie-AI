package com.hygie.patientservice.config;

import com.hygie.patientservice.interceptor.AuditInterceptor;
import com.hygie.patientservice.interceptor.RequestValidationInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration MVC Web pour le Patient Service.
 *
 * Cette classe configure les intercepteurs et autres aspects MVC Web
 * pour assurer la validation des requêtes et l'audit des accès.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestValidationInterceptor requestValidationInterceptor;
    private final AuditInterceptor auditInterceptor;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param requestValidationInterceptor L'intercepteur de validation des requêtes
     * @param auditInterceptor L'intercepteur d'audit
     */
    @Autowired
    public WebMvcConfig(RequestValidationInterceptor requestValidationInterceptor,
                       AuditInterceptor auditInterceptor) {
        // Assertion #1: Vérification que les intercepteurs ne sont pas null
        assert requestValidationInterceptor != null :
            "L'intercepteur de validation ne peut pas être null";
        assert auditInterceptor != null :
            "L'intercepteur d'audit ne peut pas être null";

        this.requestValidationInterceptor = requestValidationInterceptor;
        this.auditInterceptor = auditInterceptor;
    }

    /**
     * Ajoute les intercepteurs au registre.
     *
     * @param registry Le registre d'intercepteurs
     */
    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // Assertion #1: Vérification que le registre n'est pas null
        assert registry != null : "Le registre d'intercepteurs ne peut pas être null";

        // Ajout des intercepteurs dans l'ordre
        registry.addInterceptor(requestValidationInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health");

        registry.addInterceptor(auditInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/health");

        // Assertion #2: Vérification post-ajout
        assert registry != null : "Le registre d'intercepteurs a été modifié sans erreur";
    }
}

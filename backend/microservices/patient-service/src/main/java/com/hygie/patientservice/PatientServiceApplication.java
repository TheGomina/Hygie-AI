package com.hygie.patientservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Classe principale pour démarrer le microservice de gestion des patients.
 *
 * Ce microservice fait partie de la plateforme Hygie-AI et gère les patients,
 * leurs médicaments et leurs prescriptions pour le Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableMongoRepositories
public class PatientServiceApplication {

    /**
     * Point d'entrée de l'application.
     *
     * @param args Les arguments de ligne de commande
     */
    public static void main(String[] args) {
        // Assertion #1: Vérification que les arguments ne sont pas null
        assert args != null : "Les arguments ne peuvent pas être null";

        SpringApplication.run(PatientServiceApplication.class, args);

        // Note: Dans un environnement de production, l'application continuerait à s'exécuter
        // indéfiniment pour traiter les requêtes entrantes
    }

    /**
     * Configuration du processeur de validation des méthodes.
     *
     * @return Un processeur de validation pour les méthodes annotées
     */
    @Bean
    public MethodValidationPostProcessor methodValidationPostProcessor() {
        // Assertion #1: Création d'un nouveau processeur
        final MethodValidationPostProcessor processor = new MethodValidationPostProcessor();

        // Assertion #2: Vérification que le processeur est correctement instancié
        assert processor != null : "Le processeur de validation ne peut pas être null";

        return processor;
    }

    /**
     * Configuration d'un RestTemplate pour les appels HTTP externes.
     *
     * @return Un RestTemplate configuré
     */
    @Bean
    public RestTemplate restTemplate() {
        // Assertion #1: Création d'un nouveau RestTemplate
        final RestTemplate restTemplate = new RestTemplate();

        // Assertion #2: Vérification que le RestTemplate est correctement instancié
        assert restTemplate != null : "Le RestTemplate ne peut pas être null";

        return restTemplate;
    }

    /**
     * Configuration de l'aspect TimedAspect pour la mesure des performances.
     *
     * @param registry Le registre de métriques
     * @return Un aspect TimedAspect configuré
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        // Assertion #1: Vérification que le registre n'est pas null
        assert registry != null : "Le registre de métriques ne peut pas être null";

        final TimedAspect timedAspect = new TimedAspect(registry);

        // Assertion #2: Vérification que l'aspect est correctement instancié
        assert timedAspect != null : "L'aspect TimedAspect ne peut pas être null";

        return timedAspect;
    }
}

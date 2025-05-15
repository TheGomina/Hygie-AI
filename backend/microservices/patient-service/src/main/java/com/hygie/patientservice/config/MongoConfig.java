package com.hygie.patientservice.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.NonNull;

/**
 * Configuration MongoDB pour le Patient Service.
 *
 * Cette classe configure la connexion à MongoDB et les validateurs
 * pour garantir l'intégrité des données persistées.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Configuration
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    /**
     * Fournit le nom de la base de données.
     *
     * @return Le nom de la base de données
     */
    @Override
    @NonNull
    protected String getDatabaseName() {
        // Assertion #1: Vérification du nom de la base de données
        assert databaseName != null && !databaseName.isBlank() :
            "Le nom de la base de données doit être défini";

        return databaseName;
    }

    /**
     * Configure et crée le client MongoDB.
     *
     * @return Le client MongoDB configuré
     */
    @Override
    @NonNull
    public MongoClient mongoClient() {
        // Assertion #1: Vérification de l'URI MongoDB
        assert mongoUri != null && !mongoUri.isBlank() :
            "L'URI MongoDB doit être défini";

        final ConnectionString connectionString = new ConnectionString(mongoUri);

        final MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .applyToConnectionPoolSettings(builder ->
                builder.maxConnectionIdleTime(30000, TimeUnit.MILLISECONDS))
            .applyToSocketSettings(builder ->
                builder.connectTimeout(2000, TimeUnit.MILLISECONDS))
            .build();

        // Assertion #2: Vérification des paramètres de connexion
        assert mongoClientSettings != null :
            "Les paramètres de connexion MongoDB ne peuvent pas être null";

        return MongoClients.create(mongoClientSettings);
    }

    /**
     * Fournit le template MongoDB pour les opérations de base de données.
     *
     * @return Le template MongoDB configuré
     */
    @Bean
    public MongoTemplate mongoTemplate() {
        // Assertion #1: Vérification du client MongoDB
        assert mongoClient() != null :
            "Le client MongoDB ne peut pas être null";

        final MongoTemplate mongoTemplate = new MongoTemplate(mongoClient(), getDatabaseName());

        // Assertion #2: Vérification du template MongoDB
        assert mongoTemplate != null :
            "Le template MongoDB ne peut pas être null";

        return mongoTemplate;
    }

    /**
     * Configure un écouteur d'événements de validation pour MongoDB.
     *
     * @param validator Le bean factory de validation
     * @return L'écouteur d'événements de validation MongoDB
     */
    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener(LocalValidatorFactoryBean validator) {
        // Assertion #1: Vérification du validateur
        assert validator != null :
            "Le validateur ne peut pas être null";

        final ValidatingMongoEventListener listener = new ValidatingMongoEventListener(validator);

        // Assertion #2: Vérification de l'écouteur
        assert listener != null :
            "L'écouteur d'événements de validation ne peut pas être null";

        return listener;
    }

    /**
     * Fournit la factory de validation locale pour les annotations de validation.
     *
     * @return La factory de validation configurée
     */
    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    /**
     * Configure les collections à mapper.
     *
     * @return Les noms des collections à mapper
     */
    @Override
    @NonNull
    protected Collection<String> getMappingBasePackages() {
        return Collections.singleton("com.hygie.patientservice.model");
    }
}

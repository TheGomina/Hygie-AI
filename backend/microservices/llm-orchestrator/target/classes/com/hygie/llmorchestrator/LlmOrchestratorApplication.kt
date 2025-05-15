package com.hygie.llmorchestrator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.config.EnableWebFlux
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import javax.annotation.PostConstruct
import java.lang.management.ManagementFactory
import java.lang.management.MemoryMXBean
import org.slf4j.LoggerFactory

/**
 * Classe principale de l'application d'orchestration des LLMs médicaux.
 *
 * Cette application coordonne l'utilisation de plusieurs modèles de langage (LLMs)
 * spécialisés dans le domaine médical pour fournir des analyses et recommandations
 * pharmaceutiques pertinentes.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableWebFlux
class LlmOrchestratorApplication {

    private val logger = LoggerFactory.getLogger(LlmOrchestratorApplication::class.java)

    /**
     * Vérifie les conditions préalables au démarrage de l'application.
     *
     * @param env L'environnement Spring contenant les propriétés de configuration
     * @throws IllegalStateException Si une condition critique n'est pas remplie
     */
    @PostConstruct
    fun validateEnvironment(env: Environment) {
        // Validation des propriétés essentielles
        val requiredProperties = listOf(
            "spring.data.mongodb.uri",
            "spring.redis.host",
            "llm.service.timeout",
            "llm.models.endpoints"
        )

        // Assertion #1: Vérification de la présence des propriétés requises
        for (prop in requiredProperties) {
            require(env.getProperty(prop) != null) {
                "Propriété requise manquante: $prop"
            }
        }

        // Validation des ressources système
        val memoryBean: MemoryMXBean = ManagementFactory.getMemoryMXBean()
        val heapMemoryUsage = memoryBean.heapMemoryUsage
        val availableMemory = heapMemoryUsage.max - heapMemoryUsage.used

        // Assertion #2: Vérification des ressources mémoire minimales
        require(availableMemory > 1_073_741_824L) { // 1 GB minimum
            "Mémoire insuffisante pour exécuter le service d'orchestration LLM"
        }

        logger.info("Validation de l'environnement réussie avec ${availableMemory / 1_048_576L} MB disponibles")
    }

    /**
     * Configure les métriques système pour la surveillance de l'application.
     *
     * @param registry Le registre de métriques Micrometer
     * @return Les métriques enregistrées
     */
    @Bean
    fun jvmMetrics(registry: MeterRegistry): JvmMemoryMetrics {
        val metrics = JvmMemoryMetrics()
        metrics.bindTo(registry)
        return metrics
    }

    /**
     * Configure les métriques du processeur pour la surveillance de l'application.
     *
     * @param registry Le registre de métriques Micrometer
     * @return Les métriques enregistrées
     */
    @Bean
    fun processorMetrics(registry: MeterRegistry): ProcessorMetrics {
        val metrics = ProcessorMetrics()
        metrics.bindTo(registry)
        return metrics
    }
}

/**
 * Fonction principale qui démarre l'application Spring Boot.
 *
 * @param args Arguments de ligne de commande (non utilisés)
 */
fun main(args: Array<String>) {
    runApplication<LlmOrchestratorApplication>(*args)
}

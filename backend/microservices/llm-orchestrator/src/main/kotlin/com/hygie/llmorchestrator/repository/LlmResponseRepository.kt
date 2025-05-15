package com.hygie.llmorchestrator.repository

import com.hygie.llmorchestrator.model.AnalysisCategory
import com.hygie.llmorchestrator.model.LlmModelType
import com.hygie.llmorchestrator.model.LlmResponse
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

/**
 * Interface de repository pour les réponses générées par les LLMs.
 *
 * Fournit des méthodes pour persister et récupérer les réponses d'analyse
 * médicamenteuse générées par les modèles LLM.
 *
 * @author Hygie-AI Team
 */
@Repository
interface LlmResponseRepository : ReactiveMongoRepository<LlmResponse, String> {

    /**
     * Trouve une réponse par son ID de requête.
     *
     * @param requestId L'identifiant de la requête
     * @return Un Mono contenant la réponse LLM si trouvée
     */
    fun findByRequestId(requestId: String): Mono<LlmResponse>

    /**
     * Trouve toutes les réponses contenant des recommandations dans une catégorie spécifique.
     *
     * @param category La catégorie d'analyse pharmaceutique
     * @return Un Flux de réponses LLM correspondant à la catégorie
     */
    fun findByRecommendationsCategoryOrderByTimestampDesc(category: AnalysisCategory): Flux<LlmResponse>

    /**
     * Trouve les réponses générées par un type de modèle spécifique dans une période donnée.
     *
     * @param modelType Le type de modèle LLM utilisé
     * @param startTime Le début de la période
     * @param endTime La fin de la période
     * @return Un Flux de réponses LLM correspondant aux critères
     */
    fun findByModelInfoModelTypeContainingAndTimestampBetween(
        modelType: LlmModelType,
        startTime: Instant,
        endTime: Instant
    ): Flux<LlmResponse>

    /**
     * Trouve les réponses dont les recommandations concernent un médicament spécifique.
     *
     * @param medication Le nom du médicament
     * @return Un Flux de réponses LLM concernant ce médicament
     */
    fun findByRecommendationsMedicationsContaining(medication: String): Flux<LlmResponse>

    /**
     * Compte le nombre de réponses avec un niveau de confiance supérieur à un seuil.
     *
     * @param confidenceThreshold Le seuil de confiance minimum
     * @return Un Mono contenant le nombre de réponses
     */
    fun countByRecommendationsConfidenceLevelScoreGreaterThan(confidenceThreshold: Double): Mono<Long>

    /**
     * Supprime les réponses antérieures à une date donnée.
     *
     * @param threshold La date seuil
     * @return Un Mono contenant le nombre de documents supprimés
     */
    fun deleteByTimestampBefore(threshold: Instant): Mono<Long>
}

package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.exception.LlmServiceException
import com.hygie.llmorchestrator.model.*
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Service d'orchestration ensemble utilisant plusieurs modèles LLM médicaux.
 *
 * Ce service coordonne l'utilisation de plusieurs modèles en parallèle
 * pour obtenir des analyses plus complètes et complémentaires.
 *
 * @property bioMistralService Service pour le modèle BioMistral
 * @property hippoMistralService Service pour le modèle HippoMistral
 * @property medFoundService Service pour le modèle MedFound
 * @property fallbackStrategy Stratégie de repli en cas d'échec d'un modèle
 * @property loadBalancingStrategy Stratégie de répartition de charge
 * @property meterRegistry Registre pour les métriques
 * @author Hygie-AI Team
 */
@Service
class EnsembleLlmService(
    private val bioMistralService: BioMistralService,
    @Qualifier("hippoMistralService") private val hippoMistralService: LlmProviderService,
    @Qualifier("medFoundService") private val medFoundService: LlmProviderService,
    @Value("\${llm.models.fallback-strategy:sequential}") private val fallbackStrategy: String,
    @Value("\${llm.models.load-balancing:round-robin}") private val loadBalancingStrategy: String,
    private val meterRegistry: MeterRegistry
) : LlmProviderService {
    private val logger = LoggerFactory.getLogger(EnsembleLlmService::class.java)
    private lateinit var requestTimer: Timer
    private var currentModelIndex = 0
    private val modelWeights = ConcurrentHashMap<LlmModelType, Int>()

    @PostConstruct
    fun initialize() {
        logger.info("Initialisation du service d'ensemble LLM avec stratégie de fallback: {}, load balancing: {}",
            fallbackStrategy, loadBalancingStrategy)

        requestTimer = Timer.builder("llm.ensemble.request.duration")
            .description("Durée des requêtes au service d'ensemble LLM")
            .register(meterRegistry)

        // Configuration des poids initiaux pour chaque modèle
        modelWeights[LlmModelType.BIOMISTRAL] = 40    // 40% des requêtes
        modelWeights[LlmModelType.HIPPOMISTRAL] = 30  // 30% des requêtes
        modelWeights[LlmModelType.MEDFOUND] = 30      // 30% des requêtes

        // Assertions de vérification
        // Assertion #1: Vérifier que les poids somment à 100%
        val totalWeight = modelWeights.values.sum()
        require(totalWeight == 100) {
            "La somme des poids des modèles doit être égale à 100 (actuel: $totalWeight)"
        }

        // Assertion #2: Vérifier que les stratégies configurées sont valides
        require(fallbackStrategy in listOf("sequential", "parallel", "vote")) {
            "Stratégie de fallback invalide: $fallbackStrategy (valeurs acceptées: sequential, parallel, vote)"
        }

        require(loadBalancingStrategy in listOf("round-robin", "weighted", "adaptive")) {
            "Stratégie de load balancing invalide: $loadBalancingStrategy (valeurs acceptées: round-robin, weighted, adaptive)"
        }
    }

    /**
     * Retourne le type de modèle LLM.
     *
     * @return LlmModelType.ENSEMBLE
     */
    override fun getModelType(): LlmModelType = LlmModelType.ENSEMBLE

    /**
     * Vérifie si le service d'ensemble est opérationnel.
     *
     * @return true si au moins un des modèles est disponible
     */
    override fun isOperational(): Boolean {
        return bioMistralService.isOperational() ||
               hippoMistralService.isOperational() ||
               medFoundService.isOperational()
    }

    /**
     * Appelle le service d'ensemble LLM avec un prompt optimisé.
     *
     * @param prompt Le prompt optimisé
     * @param originalRequest La requête originale
     * @return Un Mono contenant la réponse combinée des modèles LLM
     */
    @CircuitBreaker(name = "ensembleLlmService")
    @Retry(name = "ensembleLlmService")
    override fun callLlmService(
        prompt: String,
        originalRequest: LlmRequest
    ): Mono<LlmResponse> {
        logger.debug("Appel au service d'ensemble LLM pour la requête: {}", originalRequest.requestId)

        val startTime = Instant.now()

        // Sélection de la stratégie de traitement
        return when (originalRequest.analysisCategories.size) {
            // Pour une seule catégorie, utiliser le modèle le plus approprié
            1 -> callSingleModelForCategory(prompt, originalRequest, originalRequest.analysisCategories.first())

            // Pour 2-3 catégories, distribuer les catégories aux modèles appropriés
            in 2..3 -> distributeCategoriesToModels(prompt, originalRequest)

            // Pour plus de catégories, utiliser la stratégie d'ensemble complète
            else -> callEnsembleStrategy(prompt, originalRequest)
        }.map { response ->
            val endTime = Instant.now()
            val duration = Duration.between(startTime, endTime)

            // Enregistrement de la durée
            requestTimer.record(duration)

            // Mise à jour du temps de traitement
            response.copy(processingTimeMs = duration.toMillis())
        }.doOnSuccess { response ->
            logger.info("Réponse ensemble générée pour la requête {} avec {} recommandations en {} ms",
                originalRequest.requestId, response.recommendations.size, response.processingTimeMs)
        }
    }

    /**
     * Appelle un seul modèle spécialisé pour une catégorie spécifique.
     *
     * @param prompt Le prompt optimisé
     * @param originalRequest La requête originale
     * @param category La catégorie d'analyse
     * @return Un Mono contenant la réponse du modèle spécialisé
     */
    private fun callSingleModelForCategory(
        prompt: String,
        originalRequest: LlmRequest,
        category: AnalysisCategory
    ): Mono<LlmResponse> {
        // Sélection du modèle le plus approprié pour la catégorie
        val selectedService = when (category) {
            AnalysisCategory.DRUG_INTERACTION,
            AnalysisCategory.CONTRAINDICATION -> bioMistralService

            AnalysisCategory.ELDERLY_APPROPRIATENESS,
            AnalysisCategory.SIDE_EFFECT_RISK -> hippoMistralService

            AnalysisCategory.DOSAGE_ADJUSTMENT,
            AnalysisCategory.THERAPEUTIC_REDUNDANCY,
            AnalysisCategory.COST_OPTIMIZATION -> medFoundService

            else -> selectModelByLoadBalancing()
        }

        // Assertion #1: Vérifier que le service sélectionné est opérationnel
        if (!selectedService.isOperational()) {
            logger.warn("Le service {} n'est pas disponible, sélection d'un service alternatif",
                selectedService.getModelType())
            return callAlternativeModel(prompt, originalRequest, selectedService)
        }

        // Assertion #2: Vérifier que le modèle est adapté à la requête
        logger.info("Utilisation du modèle {} pour l'analyse de catégorie {}",
            selectedService.getModelType(), category)

        return selectedService.callLlmService(prompt, originalRequest)
    }

    /**
     * Distribue différentes catégories d'analyse aux modèles spécialisés.
     *
     * @param prompt Le prompt optimisé
     * @param originalRequest La requête originale
     * @return Un Mono contenant la réponse combinée des modèles
     */
    private fun distributeCategoriesToModels(
        prompt: String,
        originalRequest: LlmRequest
    ): Mono<LlmResponse> {
        logger.debug("Distribution des catégories aux modèles spécialisés")

        // Création de sous-requêtes pour chaque catégorie
        val categoryRequests = originalRequest.analysisCategories.map { category ->
            val subRequest = originalRequest.copy(
                requestId = "${originalRequest.requestId}-${category.name}",
                analysisCategories = setOf(category)
            )
            category to subRequest
        }

        // Regroupement des catégories par modèle spécialisé
        val modelAssignments = mutableMapOf<LlmProviderService, MutableList<Pair<AnalysisCategory, LlmRequest>>>()

        for ((category, request) in categoryRequests) {
            val service = when (category) {
                AnalysisCategory.DRUG_INTERACTION,
                AnalysisCategory.CONTRAINDICATION -> bioMistralService

                AnalysisCategory.ELDERLY_APPROPRIATENESS,
                AnalysisCategory.SIDE_EFFECT_RISK -> hippoMistralService

                else -> medFoundService
            }

            modelAssignments.getOrPut(service) { mutableListOf() }.add(category to request)
        }

        // Traitement par chaque modèle de ses catégories assignées
        val modelResponses = modelAssignments.map { (service, assignments) ->
            if (!service.isOperational()) {
                // Si le service n'est pas disponible, rediriger vers un autre modèle
                Flux.fromIterable(assignments)
                    .flatMap { (category, request) ->
                        callAlternativeModel(prompt, request, service)
                    }
                    .collectList()
            } else {
                // Traitement groupé des catégories par le modèle assigné
                Flux.fromIterable(assignments)
                    .flatMap { (category, request) ->
                        service.callLlmService(prompt, request)
                    }
                    .collectList()
            }
        }

        // Combinaison des résultats de tous les modèles
        return Flux.concat(modelResponses)
            .flatMap { Flux.fromIterable(it) }
            .collectList()
            .map { responses ->
                combineResponses(responses, originalRequest)
            }
    }

    /**
     * Appelle un modèle alternatif en cas d'indisponibilité du modèle principal.
     *
     * @param prompt Le prompt optimisé
     * @param originalRequest La requête originale
     * @param unavailableService Le service indisponible
     * @return Un Mono contenant la réponse du modèle alternatif
     */
    private fun callAlternativeModel(
        prompt: String,
        originalRequest: LlmRequest,
        unavailableService: LlmProviderService
    ): Mono<LlmResponse> {
        // Liste des services disponibles excluant le service indisponible
        val availableServices = listOf(
            bioMistralService,
            hippoMistralService,
            medFoundService
        ).filter { it != unavailableService && it.isOperational() }

        if (availableServices.isEmpty()) {
            return Mono.error(LlmServiceException(
                "Aucun modèle LLM n'est disponible actuellement"))
        }

        // Sélection du premier service disponible
        val alternativeService = availableServices.first()

        logger.info("Utilisation du modèle alternatif {} en remplacement de {}",
            alternativeService.getModelType(), unavailableService.getModelType())

        return alternativeService.callLlmService(prompt, originalRequest)
    }

    /**
     * Appelle tous les modèles disponibles selon la stratégie d'ensemble.
     *
     * @param prompt Le prompt optimisé
     * @param originalRequest La requête originale
     * @return Un Mono contenant la réponse combinée de tous les modèles
     */
    private fun callEnsembleStrategy(
        prompt: String,
        originalRequest: LlmRequest
    ): Mono<LlmResponse> {
        logger.debug("Utilisation de la stratégie d'ensemble complète: {}", fallbackStrategy)

        // Liste des services disponibles
        val availableServices = listOf(
            bioMistralService,
            hippoMistralService,
            medFoundService
        ).filter { it.isOperational() }

        if (availableServices.isEmpty()) {
            return Mono.error(LlmServiceException(
                "Aucun modèle LLM n'est disponible actuellement"))
        }

        // Application de la stratégie d'ensemble
        return when (fallbackStrategy) {
            "parallel" -> {
                // Appel parallèle à tous les modèles disponibles
                Flux.fromIterable(availableServices)
                    .flatMap { service ->
                        service.callLlmService(prompt, originalRequest)
                            .onErrorResume { error ->
                                logger.warn("Erreur avec le modèle {}: {}",
                                    service.getModelType(), error.message)
                                Mono.empty()
                            }
                    }
                    .collectList()
                    .flatMap { responses ->
                        if (responses.isEmpty()) {
                            Mono.error(LlmServiceException(
                                "Tous les modèles ont échoué à traiter la requête"))
                        } else {
                            Mono.just(combineResponses(responses, originalRequest))
                        }
                    }
            }
            "vote" -> {
                // Appel à tous les modèles et combinaison par vote
                Flux.fromIterable(availableServices)
                    .flatMap { service ->
                        service.callLlmService(prompt, originalRequest)
                            .onErrorResume { error ->
                                logger.warn("Erreur avec le modèle {}: {}",
                                    service.getModelType(), error.message)
                                Mono.empty()
                            }
                    }
                    .collectList()
                    .flatMap { responses ->
                        if (responses.isEmpty()) {
                            Mono.error(LlmServiceException(
                                "Tous les modèles ont échoué à traiter la requête"))
                        } else {
                            Mono.just(combineResponsesByVoting(responses, originalRequest))
                        }
                    }
            }
            else -> {
                // Stratégie séquentielle (par défaut)
                callSequentialModels(prompt, originalRequest, availableServices)
            }
        }
    }

    /**
     * Appelle les modèles de manière séquentielle jusqu'à obtenir un résultat.
     *
     * @param prompt Le prompt optimisé
     * @param originalRequest La requête originale
     * @param availableServices Liste des services disponibles
     * @return Un Mono contenant la première réponse valide
     */
    private fun callSequentialModels(
        prompt: String,
        originalRequest: LlmRequest,
        availableServices: List<LlmProviderService>
    ): Mono<LlmResponse> {
        if (availableServices.isEmpty()) {
            return Mono.error(LlmServiceException(
                "Aucun modèle LLM n'est disponible actuellement"))
        }

        // Appel au premier service
        val service = availableServices.first()

        return service.callLlmService(prompt, originalRequest)
            .onErrorResume { error ->
                logger.warn("Erreur avec le modèle {}: {}, essai avec le modèle suivant",
                    service.getModelType(), error.message)

                // En cas d'erreur, essayer avec les services restants
                callSequentialModels(prompt, originalRequest, availableServices.drop(1))
            }
    }

    /**
     * Combine plusieurs réponses en une seule réponse cohérente.
     *
     * @param responses Liste des réponses à combiner
     * @param originalRequest Requête d'origine
     * @return Réponse combinée
     */
    private fun combineResponses(
        responses: List<LlmResponse>,
        originalRequest: LlmRequest
    ): LlmResponse {
        logger.debug("Combinaison de {} réponses pour la requête {}",
            responses.size, originalRequest.requestId)

        if (responses.isEmpty()) {
            throw LlmServiceException("Aucune réponse à combiner")
        }

        if (responses.size == 1) {
            return responses.first()
        }

        // Collecte de toutes les recommandations
        val allRecommendations = responses.flatMap { it.recommendations }

        // Élimination des doublons par ID et regroupement par description similaire
        val uniqueRecommendations = removeRecommendationDuplicates(allRecommendations)

        // Calcul du temps de traitement moyen
        val avgProcessingTime = responses.map { it.processingTimeMs }.average().toLong()

        // Extraction des types de modèles utilisés
        val modelTypes = responses.flatMap { it.modelInfo.modelType }.toSet()

        // Fusion des résumés
        val combinedSummary = if (responses.size > 1) {
            "Analyse combinée de ${responses.size} modèles LLM médicaux:\n\n" +
            responses.mapIndexed { index, response ->
                val modelName = response.modelInfo.modelType.firstOrNull()?.name ?: "Modèle inconnu"
                "[$modelName]: ${response.summary.take(100)}${if (response.summary.length > 100) "..." else ""}"
            }.joinToString("\n\n")
        } else {
            responses.first().summary
        }

        return LlmResponse(
            requestId = originalRequest.requestId,
            recommendations = uniqueRecommendations,
            summary = combinedSummary,
            timestamp = Instant.now(),
            modelInfo = LlmModelInfo(
                modelType = modelTypes,
                modelVersion = "ensemble-1.0",
                tokenCount = responses.sumOf { it.modelInfo.tokenCount }
            ),
            processingTimeMs = avgProcessingTime
        )
    }

    /**
     * Élimine les recommandations en double.
     *
     * @param recommendations Liste des recommandations
     * @return Liste des recommandations uniques
     */
    private fun removeRecommendationDuplicates(
        recommendations: List<LlmRecommendation>
    ): List<LlmRecommendation> {
        // Map pour stocker les recommandations uniques par signature
        val uniqueRecommendations = mutableMapOf<String, LlmRecommendation>()

        for (recommendation in recommendations) {
            // Création d'une signature basée sur la catégorie et les médicaments
            val signature = "${recommendation.category}_${recommendation.medications.sorted().joinToString(",")}"

            // Si la signature existe déjà, garder celle avec le plus haut niveau de confiance
            val existing = uniqueRecommendations[signature]
            if (existing == null ||
                recommendation.confidenceLevel.score > existing.confidenceLevel.score) {
                uniqueRecommendations[signature] = recommendation
            }
        }

        return uniqueRecommendations.values.toList()
    }

    /**
     * Combine les réponses par un mécanisme de vote.
     *
     * @param responses Liste des réponses à combiner
     * @param originalRequest Requête d'origine
     * @return Réponse combinée avec les recommandations validées par vote
     */
    private fun combineResponsesByVoting(
        responses: List<LlmResponse>,
        originalRequest: LlmRequest
    ): LlmResponse {
        logger.debug("Combinaison de {} réponses par vote pour la requête {}",
            responses.size, originalRequest.requestId)

        if (responses.isEmpty()) {
            throw LlmServiceException("Aucune réponse à combiner par vote")
        }

        if (responses.size == 1) {
            return responses.first()
        }

        // Collecte de toutes les recommandations pour le vote
        val allRecommendations = responses.flatMap { response ->
            response.recommendations.map { rec ->
                // Signature de la recommandation pour comparaison
                val signature = "${rec.category}_${rec.medications.sorted().joinToString(",")}"
                signature to rec
            }
        }

        // Comptage des votes par signature
        val voteCounts = allRecommendations
            .groupBy { it.first }
            .mapValues { (_, recs) -> recs.size }

        // Sélection des recommandations ayant reçu au moins 2 votes ou un vote avec confiance élevée
        val validatedRecommendations = allRecommendations
            .groupBy { it.first }
            .filter { (signature, recs) ->
                voteCounts[signature]!! >= 2 || // Au moins 2 modèles sont d'accord
                recs.any { it.second.confidenceLevel.score >= 0.9 } // Ou confiance très élevée
            }
            .map { (_, recs) ->
                // Sélection de la recommandation avec le niveau de confiance le plus élevé
                recs.maxByOrNull { it.second.confidenceLevel.score }?.second
                    ?: recs.first().second
            }

        // Extraction des types de modèles utilisés
        val modelTypes = responses.flatMap { it.modelInfo.modelType }.toSet()

        // Construction de la réponse combinée
        return LlmResponse(
            requestId = originalRequest.requestId,
            recommendations = validatedRecommendations,
            summary = "Analyse par vote majoritaire de ${responses.size} modèles LLM médicaux, " +
                      "générant ${validatedRecommendations.size} recommandations validées.",
            timestamp = Instant.now(),
            modelInfo = LlmModelInfo(
                modelType = modelTypes,
                modelVersion = "ensemble-voting-1.0",
                tokenCount = responses.sumOf { it.modelInfo.tokenCount }
            ),
            processingTimeMs = responses.sumOf { it.processingTimeMs }
        )
    }

    /**
     * Sélectionne un modèle selon la stratégie de répartition de charge.
     *
     * @return Le service LLM sélectionné
     */
    private fun selectModelByLoadBalancing(): LlmProviderService {
        return when (loadBalancingStrategy) {
            "weighted" -> {
                // Sélection pondérée selon les poids configurés
                selectWeightedModel()
            }
            "adaptive" -> {
                // Sélection adaptative selon les performances récentes
                selectAdaptiveModel()
            }
            else -> {
                // Round-robin (par défaut)
                selectRoundRobinModel()
            }
        }
    }

    /**
     * Sélectionne un modèle selon la stratégie round-robin.
     *
     * @return Le service LLM sélectionné
     */
    private fun selectRoundRobinModel(): LlmProviderService {
        val services = listOf(
            bioMistralService,
            hippoMistralService,
            medFoundService
        ).filter { it.isOperational() }

        if (services.isEmpty()) {
            throw LlmServiceException("Aucun modèle LLM n'est disponible actuellement")
        }

        // Incrémentation circulaire de l'index
        currentModelIndex = (currentModelIndex + 1) % services.size
        return services[currentModelIndex]
    }

    /**
     * Sélectionne un modèle selon la stratégie pondérée.
     *
     * @return Le service LLM sélectionné
     */
    private fun selectWeightedModel(): LlmProviderService {
        val operationalServices = mapOf(
            LlmModelType.BIOMISTRAL to bioMistralService,
            LlmModelType.HIPPOMISTRAL to hippoMistralService,
            LlmModelType.MEDFOUND to medFoundService
        ).filter { (_, service) -> service.isOperational() }

        if (operationalServices.isEmpty()) {
            throw LlmServiceException("Aucun modèle LLM n'est disponible actuellement")
        }

        // Calcul des poids cumulatifs pour les services opérationnels
        var cumulativeWeight = 0
        val weightMap = operationalServices.map { (type, service) ->
            cumulativeWeight += (modelWeights[type] ?: 0)
            Triple(type, service, cumulativeWeight)
        }

        // Génération d'un nombre aléatoire entre 1 et le poids total
        val randomWeight = (1..cumulativeWeight).random()

        // Sélection du service correspondant au poids aléatoire
        return weightMap.first { (_, _, cumWeight) -> randomWeight <= cumWeight }.second
    }

    /**
     * Sélectionne un modèle selon une stratégie adaptative.
     * Cette implémentation est simplifiée et utiliserait normalement
     * des métriques de performance en temps réel.
     *
     * @return Le service LLM sélectionné
     */
    private fun selectAdaptiveModel(): LlmProviderService {
        // Dans une implémentation réelle, cette méthode utiliserait des métriques
        // comme le temps de réponse, le taux d'erreur et la qualité des réponses
        // pour ajuster dynamiquement les poids des modèles

        // Pour cette démonstration, nous utilisons une version simplifiée
        return selectWeightedModel()
    }

    /**
     * Obtient des métriques sur les performances des modèles ensembles.
     *
     * @return Un Mono contenant une map de métriques
     */
    override fun getModelMetrics(): Mono<Map<String, Any>> {
        // Récupération des métriques de tous les services
        val metricsList = listOf(
            bioMistralService.getModelMetrics().map { it to LlmModelType.BIOMISTRAL },
            hippoMistralService.getModelMetrics().map { it to LlmModelType.HIPPOMISTRAL },
            medFoundService.getModelMetrics().map { it to LlmModelType.MEDFOUND }
        )

        return Flux.concat(metricsList)
            .collectList()
            .map { metrics ->
                val result = mutableMapOf<String, Any>()

                // Métriques générales du service d'ensemble
                result["service"] = "ensemble"
                result["operational"] = isOperational()
                result["models_available"] = metrics.count { (m, _) -> m["available"] == true }
                result["fallback_strategy"] = fallbackStrategy
                result["load_balancing"] = loadBalancingStrategy

                // Métriques spécifiques à chaque modèle
                result["models"] = metrics.associate { (modelMetrics, modelType) ->
                    modelType.name to modelMetrics
                }

                result
            }
            .onErrorReturn(mapOf(
                "service" to "ensemble",
                "operational" to isOperational(),
                "error" to "Impossible de récupérer les métriques complètes"
            ))
    }
}

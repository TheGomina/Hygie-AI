package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.exception.LlmServiceException
import com.hygie.llmorchestrator.model.*
import com.hygie.llmorchestrator.repository.LlmResponseRepository
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeoutException
import javax.annotation.PostConstruct

/**
 * Service d'orchestration des modèles LLM médicaux.
 *
 * Ce service coordonne l'utilisation de différents modèles de langage spécialisés
 * en médecine pour générer des analyses et recommandations pharmaceutiques
 * pertinentes et fondées sur des preuves.
 *
 * @property llmProviders Map des fournisseurs de modèles LLM par type
 * @property llmResponseRepository Repository pour persister les réponses
 * @property redisTemplate Template pour la gestion du cache Redis
 * @property promptEngineeringService Service de génération de prompts optimisés
 * @property meterRegistry Registre pour les métriques d'observabilité
 * @property cacheTtl Durée de vie du cache en secondes
 * @property cacheEnabled Flag indiquant si le cache est activé
 *
 * @author Hygie-AI Team
 */
@Service
class LlmOrchestratorService(
    private val llmProviders: Map<LlmModelType, LlmProviderService>,
    private val llmResponseRepository: LlmResponseRepository,
    private val redisTemplate: ReactiveRedisTemplate<String, LlmResponse>,
    private val promptEngineeringService: PromptEngineeringService,
    private val responseValidationService: ResponseValidationService,
    private val meterRegistry: MeterRegistry,
    @Value("\${llm.cache.ttl:3600}") private val cacheTtl: Long,
    @Value("\${llm.cache.enabled:true}") private val cacheEnabled: Boolean
) {
    private val logger = LoggerFactory.getLogger(LlmOrchestratorService::class.java)
    private lateinit var requestTimer: Timer
    private lateinit var processingTimer: Timer
    private val responseCache = redisTemplate.opsForValue()

    /**
     * Initialise les compteurs et timers pour les métriques.
     */
    @PostConstruct
    fun initialize() {
        requestTimer = Timer.builder("llm.request.duration")
            .description("Durée de traitement des requêtes LLM")
            .register(meterRegistry)

        processingTimer = Timer.builder("llm.processing.duration")
            .description("Durée de traitement interne des requêtes LLM")
            .register(meterRegistry)

        logger.info("Service d'orchestration LLM initialisé avec cache {}",
            if (cacheEnabled) "activé (TTL: ${cacheTtl}s)" else "désactivé")
    }

    /**
     * Traite une requête d'analyse pharmaceutique.
     *
     * Orchestre l'utilisation des modèles LLM appropriés selon le contexte
     * et le type d'analyse demandé, puis valide et persiste la réponse générée.
     *
     * @param request La requête d'analyse
     * @return Un Mono contenant la réponse générée
     * @throws LlmServiceException Si une erreur survient pendant le traitement
     */
    @CircuitBreaker(name = "llmService", fallbackMethod = "fallbackProcessRequest")
    @Retry(name = "llmService")
    @TimeLimiter(name = "llmService")
    fun processRequest(request: LlmRequest): Mono<LlmResponse> {
        // Assertion #1: Validation des préconditions
        request.validate()

        val requestId = request.requestId
        val startTime = Instant.now()

        logger.info("Traitement de la requête LLM [{}] pour {} médicaments et {} catégories d'analyse",
            requestId, request.medications.size, request.analysisCategories.size)

        // Vérification du cache si activé
        return if (cacheEnabled) {
            checkCache(requestId)
                .switchIfEmpty(processRequestInternal(request, startTime))
        } else {
            processRequestInternal(request, startTime)
        }
    }

    /**
     * Traitement interne d'une requête LLM.
     *
     * @param request La requête à traiter
     * @param startTime L'heure de début du traitement
     * @return Un Mono contenant la réponse LLM
     */
    private fun processRequestInternal(
        request: LlmRequest,
        startTime: Instant
    ): Mono<LlmResponse> {
        val processingStart = Instant.now()

        // Sélection du modèle approprié selon le type demandé et le contenu
        return selectModel(request)
            .flatMap { selectedService ->
                // Génération d'un prompt optimisé pour le modèle et le contexte
                promptEngineeringService.generatePrompt(request)
                    .flatMap { optimizedPrompt ->
                        // Appel au service LLM
                        selectedService.callLlmService(optimizedPrompt, request)
                    }
            }
            .flatMap { rawResponse ->
                // Validation de la réponse
                responseValidationService.validateResponse(rawResponse, request)
                    .map { validatedResponse ->
                        // Construction de la réponse finale avec métriques
                        val endTime = Instant.now()
                        val processingTimeMs = Duration.between(processingStart, endTime).toMillis()

                        // Assertion #2: Vérification du temps de traitement
                        require(processingTimeMs >= 0) {
                            "Temps de traitement négatif détecté: $processingTimeMs ms"
                        }

                        validatedResponse.copy(processingTimeMs = processingTimeMs)
                    }
            }
            .flatMap { response ->
                // Persistance de la réponse
                llmResponseRepository.save(response)
                    .doOnSuccess { saved ->
                        if (cacheEnabled) {
                            // Mise en cache de la réponse
                            responseCache.set(request.requestId, saved, Duration.ofSeconds(cacheTtl))
                                .subscribe()
                        }

                        // Enregistrement des métriques
                        val totalDuration = Duration.between(startTime, Instant.now())
                        requestTimer.record(totalDuration)
                        processingTimer.record(Duration.ofMillis(saved.processingTimeMs))

                        logger.info("Requête LLM [{}] traitée en {}ms avec {} recommandations",
                            request.requestId, saved.processingTimeMs, saved.recommendations.size)
                    }
            }
            .onErrorMap { error ->
                logger.error("Erreur lors du traitement de la requête LLM [{}]: {}",
                    request.requestId, error.message, error)

                when (error) {
                    is TimeoutException -> LlmServiceException(
                        "Délai d'exécution dépassé pour la requête: ${request.requestId}", error)
                    is IllegalArgumentException -> LlmServiceException(
                        "Validation échouée: ${error.message}", error)
                    else -> LlmServiceException(
                        "Erreur lors du traitement de la requête: ${error.message}", error)
                }
            }
    }

    /**
     * Vérifie si une réponse existe déjà en cache.
     *
     * @param requestId L'identifiant de la requête
     * @return Un Mono contenant la réponse si elle existe en cache
     */
    private fun checkCache(requestId: String): Mono<LlmResponse> {
        return responseCache.get(requestId)
            .doOnSuccess { cachedResponse ->
                if (cachedResponse != null) {
                    logger.debug("Réponse trouvée en cache pour la requête [{}]", requestId)
                }
            }
    }

    /**
     * Sélectionne le modèle LLM le plus approprié pour traiter la requête.
     *
     * @param request La requête d'analyse
     * @return Un Mono contenant le service LLM sélectionné
     */
    private fun selectModel(request: LlmRequest): Mono<LlmProviderService> {
        return Mono.defer {
            val selectedProvider = when (request.modelType) {
                LlmModelType.ENSEMBLE -> {
                    // Pour le type ensemble, la stratégie dépend des catégories d'analyse
                    selectEnsembleStrategy(request)
                }
                else -> {
                    // Utilisation directe du modèle spécifié
                    llmProviders[request.modelType] ?: llmProviders[LlmModelType.BIOMISTRAL]
                }
            }

            // Assertion #1: Vérification que le provider n'est pas null
            requireNotNull(selectedProvider) {
                "Aucun fournisseur LLM disponible pour le type: ${request.modelType}"
            }

            // Assertion #2: Vérification que le provider est opérationnel
            require(selectedProvider.isOperational()) {
                "Le fournisseur LLM sélectionné n'est pas opérationnel: ${request.modelType}"
            }

            Mono.just(selectedProvider)
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Sélectionne une stratégie d'ensemble pour combiner plusieurs modèles LLM.
     *
     * @param request La requête d'analyse
     * @return Le service de fournisseur LLM pour l'ensemble
     */
    private fun selectEnsembleStrategy(request: LlmRequest): LlmProviderService {
        // Détermination des modèles pertinents selon les catégories d'analyse
        val categories = request.analysisCategories

        return when {
            // Si la catégorie concerne les interactions médicamenteuses, privilégier BioMistral
            categories.contains(AnalysisCategory.DRUG_INTERACTION) ->
                llmProviders[LlmModelType.BIOMISTRAL]

            // Si la catégorie concerne l'appropriation pour les personnes âgées, privilégier HippoMistral
            categories.contains(AnalysisCategory.ELDERLY_APPROPRIATENESS) ->
                llmProviders[LlmModelType.HIPPOMISTRAL]

            // Pour les analyses nécessitant des preuves scientifiques solides
            categories.contains(AnalysisCategory.CONTRAINDICATION) ->
                llmProviders[LlmModelType.MEDFOUND]

            // Par défaut, utiliser l'orchestrateur d'ensemble complet
            else -> llmProviders[LlmModelType.ENSEMBLE] ?: llmProviders.values.first()
        } ?: llmProviders.values.first()
    }

    /**
     * Méthode de repli en cas d'échec du circuit breaker.
     *
     * @param request La requête originale
     * @param error L'erreur survenue
     * @return Un Mono contenant une réponse dégradée
     */
    private fun fallbackProcessRequest(request: LlmRequest, error: Throwable): Mono<LlmResponse> {
        logger.warn("Circuit breaker activé pour la requête [{}]: {}",
            request.requestId, error.message, error)

        // Création d'une réponse dégradée
        val fallbackResponse = LlmResponse(
            requestId = request.requestId,
            recommendations = emptyList(),
            summary = "Service temporairement indisponible. Veuillez réessayer ultérieurement.",
            modelInfo = LlmModelInfo(
                modelType = setOf(LlmModelType.ENSEMBLE),
                modelVersion = "fallback-1.0",
                tokenCount = 0
            ),
            processingTimeMs = 0
        )

        return Mono.just(fallbackResponse)
    }
}

package com.hygie.llmorchestrator.controller

import com.hygie.llmorchestrator.exception.LlmServiceException
import com.hygie.llmorchestrator.model.AnalysisCategory
import com.hygie.llmorchestrator.model.LlmModelType
import com.hygie.llmorchestrator.model.LlmRequest
import com.hygie.llmorchestrator.model.LlmResponse
import com.hygie.llmorchestrator.service.LlmOrchestratorService
import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.validation.Valid
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

/**
 * Contrôleur REST pour le service d'orchestration des LLMs.
 *
 * Expose les endpoints permettant d'interagir avec les modèles de langage
 * pour l'analyse pharmaceutique.
 *
 * @property orchestratorService Service d'orchestration des LLMs
 * @author Hygie-AI Team
 */
@RestController
@RequestMapping("/api/llm")
@Validated
class LlmOrchestratorController(
    private val orchestratorService: LlmOrchestratorService
) {
    private val logger = LoggerFactory.getLogger(LlmOrchestratorController::class.java)

    /**
     * Endpoint pour soumettre une requête d'analyse pharmaceutique.
     *
     * @param request La requête contenant les données d'analyse
     * @return Une réponse contenant les recommandations générées
     */
    @PostMapping("/analyze", produces = [MediaType.APPLICATION_JSON_VALUE])
    @Timed(value = "api.llm.analyze", description = "Durée de traitement des requêtes d'analyse")
    fun analyzeRequest(@RequestBody @Valid request: LlmRequest): Mono<ResponseEntity<LlmResponse>> {
        logger.info("Réception d'une requête d'analyse pour {} médicaments", request.medications.size)

        // Assertion #1: Vérifier que la requête contient au moins un médicament
        require(request.medications.isNotEmpty()) {
            "La requête doit contenir au moins un médicament"
        }

        // Assertion #2: Vérifier que la requête spécifie au moins une catégorie d'analyse
        require(request.analysisCategories.isNotEmpty()) {
            "La requête doit spécifier au moins une catégorie d'analyse"
        }

        return orchestratorService.processRequest(request)
            .map { response ->
                ResponseEntity.ok(response)
            }
            .onErrorResume { error ->
                logger.error("Erreur lors du traitement de la requête: {}", error.message, error)

                val errorResponse = when (error) {
                    is IllegalArgumentException -> {
                        Mono.just(ResponseEntity
                            .status(HttpStatus.BAD_REQUEST)
                            .body<LlmResponse>(null))
                    }
                    is LlmServiceException -> {
                        Mono.just(ResponseEntity
                            .status(HttpStatus.SERVICE_UNAVAILABLE)
                            .body<LlmResponse>(null))
                    }
                    else -> {
                        Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body<LlmResponse>(null))
                    }
                }

                errorResponse
            }
    }

    /**
     * Endpoint pour récupérer une réponse par son ID de requête.
     *
     * @param requestId L'identifiant de la requête
     * @return La réponse correspondante
     */
    @GetMapping("/responses/{requestId}")
    fun getResponseByRequestId(
        @PathVariable @NotBlank requestId: String
    ): Mono<ResponseEntity<LlmResponse>> {
        logger.debug("Récupération de la réponse pour la requête: {}", requestId)

        return orchestratorService.getResponseByRequestId(requestId)
            .map { response -> ResponseEntity.ok(response) }
            .switchIfEmpty(Mono.just(ResponseEntity.notFound().build()))
            .onErrorResume { error ->
                logger.error("Erreur lors de la récupération de la réponse: {}", error.message, error)
                Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build())
            }
    }

    /**
     * Endpoint pour rechercher des réponses par catégorie d'analyse.
     *
     * @param category La catégorie d'analyse
     * @return Un flux de réponses correspondant à la catégorie
     */
    @GetMapping("/responses/by-category/{category}")
    fun getResponsesByCategory(
        @PathVariable @NotNull category: AnalysisCategory
    ): Flux<LlmResponse> {
        logger.debug("Recherche de réponses par catégorie: {}", category)
        return orchestratorService.findResponsesByCategory(category)
    }

    /**
     * Endpoint pour rechercher des réponses concernant un médicament spécifique.
     *
     * @param medication Le nom du médicament
     * @return Un flux de réponses contenant des recommandations pour ce médicament
     */
    @GetMapping("/responses/by-medication")
    fun getResponsesByMedication(
        @RequestParam @NotBlank medication: String
    ): Flux<LlmResponse> {
        logger.debug("Recherche de réponses pour le médicament: {}", medication)
        return orchestratorService.findResponsesByMedication(medication)
    }

    /**
     * Endpoint pour récupérer les statistiques d'utilisation des modèles.
     *
     * @param modelType Le type de modèle (optionnel)
     * @param days Le nombre de jours à considérer (par défaut 30)
     * @return Les statistiques d'utilisation
     */
    @GetMapping("/stats/usage")
    fun getModelUsageStats(
        @RequestParam(required = false) modelType: LlmModelType?,
        @RequestParam(defaultValue = "30") days: Int
    ): Mono<Map<String, Any>> {
        logger.debug("Récupération des statistiques d'utilisation des modèles sur {} jours", days)

        // Assertion #1: Validation du nombre de jours
        require(days in 1..365) {
            "Le nombre de jours doit être compris entre 1 et 365"
        }

        val startDate = Instant.now().minus(days.toLong(), ChronoUnit.DAYS)

        // Assertion #2: Vérification que la date de début n'est pas dans le futur
        require(!startDate.isAfter(Instant.now())) {
            "La date de début ne peut pas être dans le futur"
        }

        return orchestratorService.getModelUsageStats(modelType, startDate)
    }

    /**
     * Endpoint pour vérifier la santé du service.
     *
     * @return Le statut de santé du service
     */
    @GetMapping("/health")
    fun healthCheck(): Mono<Map<String, Any>> {
        return orchestratorService.healthCheck()
            .map { status ->
                mapOf(
                    "status" to status.status,
                    "uptime" to status.uptime,
                    "timestamp" to Instant.now().toString(),
                    "version" to status.version,
                    "modelsAvailable" to status.modelsAvailable
                )
            }
    }
}

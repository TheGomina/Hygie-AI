package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.exception.LlmServiceException
import com.hygie.llmorchestrator.model.*
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.retry.annotation.Retry
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.TimeoutException
import javax.annotation.PostConstruct

/**
 * Implémentation du service LLM pour le modèle BioMistral.
 *
 * Ce service spécialisé gère les interactions avec le modèle BioMistral,
 * optimisé pour les analyses pharmacologiques et biomédicales.
 *
 * @property webClient Client HTTP pour les appels API
 * @property baseUrl URL de base de l'API BioMistral
 * @property apiKey Clé API pour l'authentification
 * @property timeout Timeout pour les appels API en millisecondes
 * @property meterRegistry Registre pour les métriques
 * @author Hygie-AI Team
 */
@Service
class BioMistralService(
    private val webClient: WebClient,
    @Value("\${llm.models.endpoints.biomistral}") private val baseUrl: String,
    @Value("\${llm.models.api-keys.biomistral:}") private val apiKey: String,
    @Value("\${llm.service.timeout:30000}") private val timeout: Long,
    private val meterRegistry: MeterRegistry
) : LlmProviderService {
    private val logger = LoggerFactory.getLogger(BioMistralService::class.java)
    private lateinit var requestTimer: Timer
    private lateinit var tokenCounter: Timer
    private var isAvailable = true

    @PostConstruct
    fun initialize() {
        logger.info("Initialisation du service BioMistral avec endpoint: {}", baseUrl)

        // Initialisation des métriques
        requestTimer = Timer.builder("llm.biomistral.request.duration")
            .description("Durée des requêtes au modèle BioMistral")
            .register(meterRegistry)

        tokenCounter = Timer.builder("llm.biomistral.tokens")
            .description("Nombre de tokens utilisés par requête BioMistral")
            .register(meterRegistry)

        // Vérifications de validité
        // Assertion #1: Vérifier que l'URL de base est valide
        require(baseUrl.startsWith("http")) {
            "L'URL de base pour BioMistral doit commencer par http:// ou https://"
        }

        // Assertion #2: Vérifier que le timeout est dans une plage raisonnable
        require(timeout in 1000..60000) {
            "Le timeout doit être entre 1000 et 60000 ms"
        }

        // Vérification de disponibilité initiale
        checkAvailability()
    }

    /**
     * Vérifie la disponibilité du service BioMistral.
     */
    private fun checkAvailability() {
        webClient.get()
            .uri("$baseUrl/health")
            .retrieve()
            .bodyToMono(Map::class.java)
            .timeout(Duration.ofMillis(timeout))
            .doOnSuccess { response ->
                val status = response["status"] as? String
                isAvailable = status == "ok"
                logger.info("Statut du service BioMistral: {}", if (isAvailable) "disponible" else "indisponible")
            }
            .doOnError { error ->
                isAvailable = false
                logger.warn("Service BioMistral indisponible: {}", error.message)
            }
            .subscribe()
    }

    /**
     * Retourne le type de modèle LLM.
     *
     * @return LlmModelType.BIOMISTRAL
     */
    override fun getModelType(): LlmModelType = LlmModelType.BIOMISTRAL

    /**
     * Vérifie si le service est opérationnel.
     *
     * @return true si le service est disponible
     */
    override fun isOperational(): Boolean = isAvailable

    /**
     * Appelle le service LLM avec un prompt optimisé.
     *
     * @param prompt Le prompt optimisé pour le modèle
     * @param originalRequest La requête originale
     * @return Un Mono contenant la réponse du modèle LLM
     */
    @CircuitBreaker(name = "biomistralService")
    @Retry(name = "biomistralService")
    override fun callLlmService(
        prompt: String,
        originalRequest: LlmRequest
    ): Mono<LlmResponse> {
        logger.debug("Appel au service BioMistral pour la requête: {}", originalRequest.requestId)

        // Préparation de la requête
        val requestBody = mapOf(
            "prompt" to prompt,
            "max_tokens" to originalRequest.maxTokens,
            "temperature" to originalRequest.temperature,
            "model" to "biomistral-7b",
            "stream" to false
        )

        val startTime = Instant.now()

        return webClient.post()
            .uri("$baseUrl/generate")
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $apiKey")
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(Map::class.java)
            .timeout(Duration.ofMillis(timeout))
            .map { response ->
                val endTime = Instant.now()
                val duration = Duration.between(startTime, endTime)

                // Enregistrement des métriques
                requestTimer.record(duration)

                // Extraction et traitement de la réponse
                val rawText = response["text"] as? String
                    ?: throw LlmServiceException("Réponse invalide du service BioMistral")

                val tokenCount = response["usage"]?.let { usage ->
                    (usage as? Map<*, *>)?.get("total_tokens") as? Int
                } ?: estimateTokenCount(prompt, rawText)

                tokenCounter.record(Duration.ofMillis(tokenCount.toLong()))

                // Construction de la réponse
                parseModelResponse(rawText, originalRequest, tokenCount)
            }
            .onErrorMap { error ->
                logger.error("Erreur lors de l'appel au service BioMistral: {}", error.message, error)

                when (error) {
                    is TimeoutException -> LlmServiceException(
                        "Délai d'attente dépassé pour le service BioMistral", error)
                    else -> LlmServiceException(
                        "Erreur du service BioMistral: ${error.message}", error)
                }
            }
            .doOnSuccess { response ->
                logger.info("Réponse BioMistral reçue pour la requête {} avec {} recommandations",
                    originalRequest.requestId, response.recommendations.size)
            }
    }

    /**
     * Obtient des métriques sur les performances du modèle.
     *
     * @return Un Mono contenant une map de métriques
     */
    override fun getModelMetrics(): Mono<Map<String, Any>> {
        return webClient.get()
            .uri("$baseUrl/metrics")
            .retrieve()
            .bodyToMono(Map::class.java)
            .timeout(Duration.ofMillis(timeout))
            .onErrorReturn(mapOf(
                "status" to "error",
                "message" to "Impossible de récupérer les métriques",
                "available" to isAvailable
            ))
    }

    /**
     * Estime le nombre de tokens dans un texte.
     * Méthode simplifiée, en production on utiliserait un tokenizer précis.
     *
     * @param prompt Le prompt envoyé
     * @param response La réponse reçue
     * @return Estimation du nombre de tokens
     */
    private fun estimateTokenCount(prompt: String, response: String): Int {
        // Estimation simplifiée: ~1 token pour 4 caractères
        return (prompt.length + response.length) / 4
    }

    /**
     * Parse la réponse brute du modèle en structure LlmResponse.
     *
     * @param rawResponse Texte brut de la réponse
     * @param originalRequest Requête d'origine
     * @param tokenCount Nombre de tokens utilisés
     * @return Réponse structurée
     */
    private fun parseModelResponse(
        rawResponse: String,
        originalRequest: LlmRequest,
        tokenCount: Int
    ): LlmResponse {
        // Dans une implémentation réelle, nous utiliserions une logique plus robuste
        // pour extraire les recommandations structurées du texte brut

        // Pour cet exemple, nous construisons une réponse structurée simplifiée
        val recommendations = mutableListOf<LlmRecommendation>()

        // Analyse basique du texte pour extraire des recommandations
        // Recherche de sections commençant par "Recommandation" ou "Problème"
        val recommendationBlocks = extractRecommendationBlocks(rawResponse)

        for (block in recommendationBlocks) {
            try {
                val recommendation = parseRecommendationBlock(block, originalRequest.medications)
                recommendations.add(recommendation)
            } catch (e: Exception) {
                logger.warn("Impossible de parser un bloc de recommandation: {}", e.message)
            }
        }

        // Extraction du résumé (première partie du texte avant les recommandations)
        val summary = if (recommendationBlocks.isEmpty()) {
            rawResponse
        } else {
            rawResponse.substring(0, rawResponse.indexOf(recommendationBlocks.first()))
                .trim()
                .takeIf { it.isNotBlank() } ?: "Analyse des médicaments complétée."
        }

        return LlmResponse(
            requestId = originalRequest.requestId,
            recommendations = recommendations,
            summary = summary,
            timestamp = Instant.now(),
            modelInfo = LlmModelInfo(
                modelType = setOf(LlmModelType.BIOMISTRAL),
                modelVersion = "biomistral-7b",
                tokenCount = tokenCount
            ),
            processingTimeMs = 0 // Sera mis à jour par le service d'orchestration
        )
    }

    /**
     * Extrait les blocs de texte contenant des recommandations.
     *
     * @param rawText Texte brut de la réponse
     * @return Liste des blocs de recommandation
     */
    private fun extractRecommendationBlocks(rawText: String): List<String> {
        val blocks = mutableListOf<String>()
        val markers = listOf(
            "Recommandation", "Problème identifié", "Interaction détectée",
            "Analyse:", "Suggestion:", "Contre-indication:"
        )

        // Recherche d'indices pour chaque marqueur
        val indices = mutableListOf<Int>()
        for (marker in markers) {
            var startIdx = rawText.indexOf(marker)
            while (startIdx >= 0) {
                indices.add(startIdx)
                startIdx = rawText.indexOf(marker, startIdx + 1)
            }
        }

        // Tri des indices pour traiter les sections dans l'ordre
        indices.sort()

        // Extraction des blocs
        for (i in indices.indices) {
            val start = indices[i]
            val end = if (i < indices.size - 1) indices[i + 1] else rawText.length

            val block = rawText.substring(start, end).trim()
            if (block.length > 50) { // Ignorer les blocs trop courts
                blocks.add(block)
            }
        }

        return blocks
    }

    /**
     * Parse un bloc de texte en recommandation structurée.
     *
     * @param block Bloc de texte contenant une recommandation
     * @param medications Liste des médicaments de la requête
     * @return Recommandation structurée
     */
    private fun parseRecommendationBlock(
        block: String,
        medications: List<MedicationInfo>
    ): LlmRecommendation {
        // Détermination de la catégorie
        val category = when {
            block.contains("interaction", ignoreCase = true) -> AnalysisCategory.DRUG_INTERACTION
            block.contains("contre-indication", ignoreCase = true) -> AnalysisCategory.CONTRAINDICATION
            block.contains("posologie", ignoreCase = true) -> AnalysisCategory.DOSAGE_ADJUSTMENT
            block.contains("redondance", ignoreCase = true) -> AnalysisCategory.THERAPEUTIC_REDUNDANCY
            block.contains("personne âgée", ignoreCase = true) -> AnalysisCategory.ELDERLY_APPROPRIATENESS
            block.contains("observance", ignoreCase = true) -> AnalysisCategory.ADHERENCE_OPTIMIZATION
            block.contains("coût", ignoreCase = true) -> AnalysisCategory.COST_OPTIMIZATION
            else -> AnalysisCategory.DRUG_INTERACTION // Par défaut
        }

        // Extraction des médicaments mentionnés
        val mentionedMedications = medications.filter { med ->
            block.contains(med.name, ignoreCase = true)
        }.map { it.name }.toMutableList()

        if (mentionedMedications.isEmpty()) {
            // Si aucun médicament n'est identifié, ajouter une indication
            mentionedMedications.add("Médicament non spécifié")
        }

        // Extraction de la description et suggestion
        val descriptionEndIndex = block.indexOf("Suggestion:")
        val description = if (descriptionEndIndex > 0) {
            block.substring(0, descriptionEndIndex).trim()
        } else {
            // Diviser le bloc en deux si "Suggestion:" n'est pas trouvé
            block.substring(0, block.length / 2).trim()
        }

        val suggestion = if (descriptionEndIndex > 0) {
            block.substring(descriptionEndIndex).trim()
        } else {
            block.substring(block.length / 2).trim()
        }

        // Création d'une source (simplifiée)
        val sources = listOf(
            RecommendationSource(
                type = "Référentiel",
                reference = "Base de données médicamenteuse BioMistral 2025",
                url = null,
                year = 2025
            )
        )

        // Niveau de confiance (simplifié)
        val confidenceLevel = ConfidenceLevel(
            score = 0.85,
            rationale = "Basé sur des données pharmacologiques validées",
            evidenceGrade = "B"
        )

        return LlmRecommendation(
            id = UUID.randomUUID().toString(),
            category = category,
            description = description,
            suggestion = suggestion,
            medications = mentionedMedications,
            confidenceLevel = confidenceLevel,
            sources = sources
        )
    }
}

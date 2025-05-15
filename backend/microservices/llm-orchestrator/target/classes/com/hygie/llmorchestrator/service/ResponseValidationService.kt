package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.model.*
import com.hygie.llmorchestrator.exception.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.time.Instant
import java.util.UUID
import javax.annotation.PostConstruct

/**
 * Service de validation des réponses générées par les LLMs.
 *
 * Implémente l'approche ACUTE pour garantir la qualité des recommandations:
 * - Accuracy: vérification de l'exactitude des informations médicales
 * - Consistency: détection des contradictions internes
 * - semantically Unaltered outputs: préservation du sens médical précis
 * - Traceability: citation et vérification des sources
 * - Ethical considerations: aspects éthiques et bénéfice/risque
 *
 * @property confidenceThreshold Seuil minimal de confiance pour les recommandations
 * @property sourceValidationEnabled Flag activant la validation des sources
 * @property medicalTermsService Service de validation des termes médicaux
 * @property medicalSourcesService Service de validation des sources médicales
 * @author Hygie-AI Team
 */
@Service
class ResponseValidationService(
    @Value("\${llm.validation.confidence-threshold:0.75}") private val confidenceThreshold: Double,
    @Value("\${llm.validation.source-validation:true}") private val sourceValidationEnabled: Boolean,
    private val medicalTermsService: MedicalTermsService,
    private val medicalSourcesService: MedicalSourcesService
) {
    private val logger = LoggerFactory.getLogger(ResponseValidationService::class.java)

    @PostConstruct
    fun initialize() {
        logger.info("Service de validation initialisé avec seuil de confiance: {}, validation de sources: {}",
            confidenceThreshold, sourceValidationEnabled)

        // Assertion #1: Vérification de la validité du seuil de confiance
        require(confidenceThreshold in 0.0..1.0) {
            "Le seuil de confiance doit être compris entre 0.0 et 1.0"
        }
    }

    /**
     * Valide une réponse LLM selon les critères ACUTE.
     *
     * @param rawResponse La réponse brute à valider
     * @param originalRequest La requête d'origine
     * @return Un Mono contenant la réponse validée et potentiellement enrichie
     * @throws ValidationException Si la validation échoue et ne peut être corrigée
     */
    fun validateResponse(
        rawResponse: LlmResponse,
        originalRequest: LlmRequest
    ): Mono<LlmResponse> {
        logger.debug("Validation de la réponse pour la requête: {}", originalRequest.requestId)

        return Mono.fromCallable {
            // Assertion #1: Vérification que la requête correspond à la réponse
            require(rawResponse.requestId == originalRequest.requestId) {
                "L'ID de requête de la réponse ne correspond pas à la requête d'origine"
            }

            // Ensemble des validations à effectuer
            val validatedResponse = validateAccuracy(rawResponse)
                .let { validateConsistency(it, originalRequest) }
                .let { validateSemanticPreservation(it) }
                .let { validateTraceability(it) }
                .let { validateEthicalConsiderations(it) }

            // Toujours effectuer une validation globale de sécurité
            validateSafety(validatedResponse)

            logger.info("Validation réussie pour la réponse à la requête: {}", originalRequest.requestId)
            validatedResponse
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Valide l'exactitude des informations médicales.
     *
     * @param response La réponse à valider
     * @return La réponse validée, potentiellement modifiée
     */
    private fun validateAccuracy(response: LlmResponse): LlmResponse {
        logger.debug("Validation de l'exactitude pour {} recommandations", response.recommendations.size)

        // Filtrer les recommandations avec une confiance suffisante
        val validatedRecommendations = response.recommendations.filter { recommendation ->
            val isConfidenceAcceptable = recommendation.confidenceLevel.score >= confidenceThreshold

            // Enregistrer les recommandations rejetées pour analyse
            if (!isConfidenceAcceptable) {
                logger.warn("Recommandation rejetée (confiance insuffisante): {}",
                    recommendation.id)
            }

            isConfidenceAcceptable
        }.map { recommendation ->
            // Validation des termes médicaux
            val validatedDescription = medicalTermsService.validateAndNormalizeMedicalTerms(
                recommendation.description)
            val validatedSuggestion = medicalTermsService.validateAndNormalizeMedicalTerms(
                recommendation.suggestion)

            // Assertion #2: Vérification de la présence des informations essentielles
            require(validatedDescription.isNotBlank()) {
                "Description vide dans la recommandation: ${recommendation.id}"
            }
            require(validatedSuggestion.isNotBlank()) {
                "Suggestion vide dans la recommandation: ${recommendation.id}"
            }

            // Retourner la recommandation avec les termes validés
            recommendation.copy(
                description = validatedDescription,
                suggestion = validatedSuggestion
            )
        }

        return response.copy(recommendations = validatedRecommendations)
    }

    /**
     * Valide la cohérence interne des recommandations.
     *
     * @param response La réponse à valider
     * @param originalRequest La requête d'origine
     * @return La réponse validée, potentiellement modifiée
     */
    private fun validateConsistency(
        response: LlmResponse,
        originalRequest: LlmRequest
    ): LlmResponse {
        logger.debug("Validation de la cohérence pour {} recommandations", response.recommendations.size)

        // Vérification de la présence des médicaments mentionnés dans la requête
        val requestMedications = originalRequest.medications.map { it.name.toLowerCase() }.toSet()

        val validatedRecommendations = response.recommendations.filter { recommendation ->
            // Vérifier que les médicaments mentionnés existent dans la requête d'origine
            val allMedicationsExist = recommendation.medications.all { medication ->
                val medicationExists = requestMedications.any { reqMed ->
                    medication.toLowerCase().contains(reqMed) || reqMed.contains(medication.toLowerCase())
                }

                if (!medicationExists) {
                    logger.warn("Médicament non trouvé dans la requête: {} (recommandation: {})",
                        medication, recommendation.id)
                }

                medicationExists
            }

            // Vérifier la cohérence entre la catégorie et le contenu
            val isCategoryConsistent = validateCategoryConsistency(
                recommendation.category, recommendation.description, recommendation.suggestion)

            allMedicationsExist && isCategoryConsistent
        }

        // Détection de contradictions entre recommandations
        val contradictions = detectContradictions(validatedRecommendations)
        if (contradictions.isNotEmpty()) {
            logger.warn("Contradictions détectées entre recommandations: {}", contradictions)

            // Filtrer les recommandations contradictoires de moindre confiance
            val nonContradictoryRecommendations = resolveContradictions(validatedRecommendations, contradictions)
            return response.copy(recommendations = nonContradictoryRecommendations)
        }

        return response.copy(recommendations = validatedRecommendations)
    }

    /**
     * Valide la cohérence entre la catégorie d'analyse et le contenu de la recommandation.
     *
     * @param category La catégorie d'analyse
     * @param description La description du problème
     * @param suggestion La suggestion d'intervention
     * @return true si la catégorie est cohérente avec le contenu
     */
    private fun validateCategoryConsistency(
        category: AnalysisCategory,
        description: String,
        suggestion: String
    ): Boolean {
        // Ensemble de mots-clés attendus pour chaque catégorie
        val categoryKeywords = mapOf(
            AnalysisCategory.DRUG_INTERACTION to setOf(
                "interaction", "interagit", "antagonisme", "synergie", "effet potentialisé"),
            AnalysisCategory.CONTRAINDICATION to setOf(
                "contre-indiqué", "contre-indication", "contre indication", "inapproprié", "éviter"),
            AnalysisCategory.DOSAGE_ADJUSTMENT to setOf(
                "posologie", "dose", "ajustement", "adaptation", "réduction", "augmentation", "clearance"),
            AnalysisCategory.THERAPEUTIC_REDUNDANCY to setOf(
                "redondance", "doublon", "chevauchement", "même classe", "équivalent", "similaire"),
            AnalysisCategory.ELDERLY_APPROPRIATENESS to setOf(
                "personne âgée", "sujet âgé", "gériatrie", "beers", "stopp", "start", "inapproprié", "fragile"),
            AnalysisCategory.ADHERENCE_OPTIMIZATION to setOf(
                "observance", "adhérence", "compliance", "prise", "oubli", "simplification", "plan de prise"),
            AnalysisCategory.COST_OPTIMIZATION to setOf(
                "coût", "économique", "générique", "biosimilaire", "remboursement", "tarif", "prix")
        )

        // Vérifier la présence d'au moins un mot-clé de la catégorie
        val keywords = categoryKeywords[category] ?: return true
        val combinedText = (description + " " + suggestion).toLowerCase()

        return keywords.any { keyword -> combinedText.contains(keyword.toLowerCase()) }
    }

    /**
     * Détecte les contradictions entre recommandations.
     *
     * @param recommendations Liste des recommandations à analyser
     * @return Liste des paires de recommandations contradictoires
     */
    private fun detectContradictions(
        recommendations: List<LlmRecommendation>
    ): List<Pair<LlmRecommendation, LlmRecommendation>> {
        val contradictions = mutableListOf<Pair<LlmRecommendation, LlmRecommendation>>()

        // Analyser chaque paire de recommandations
        for (i in recommendations.indices) {
            for (j in i + 1 until recommendations.size) {
                val rec1 = recommendations[i]
                val rec2 = recommendations[j]

                // Vérifier si les recommandations concernent les mêmes médicaments
                val commonMedications = rec1.medications.intersect(rec2.medications.toSet())
                if (commonMedications.isEmpty()) continue

                // Vérifier les contradictions selon les catégories
                if (areContradictory(rec1, rec2)) {
                    contradictions.add(rec1 to rec2)
                }
            }
        }

        return contradictions
    }

    /**
     * Détermine si deux recommandations sont contradictoires.
     *
     * @param rec1 Première recommandation
     * @param rec2 Seconde recommandation
     * @return true si les recommandations sont contradictoires
     */
    private fun areContradictory(rec1: LlmRecommendation, rec2: LlmRecommendation): Boolean {
        // Catégories intrinsèquement contradictoires
        val contradictoryPairs = setOf(
            setOf(AnalysisCategory.DOSAGE_ADJUSTMENT, AnalysisCategory.ADHERENCE_OPTIMIZATION),
            setOf(AnalysisCategory.CONTRAINDICATION, AnalysisCategory.COST_OPTIMIZATION)
        )

        if (contradictoryPairs.contains(setOf(rec1.category, rec2.category))) {
            // Analyse plus fine du contenu
            val combinedText1 = (rec1.description + " " + rec1.suggestion).toLowerCase()
            val combinedText2 = (rec2.description + " " + rec2.suggestion).toLowerCase()

            // Recherche d'oppositions directes (augmenter/diminuer, arrêter/continuer)
            val oppositionPatterns = listOf(
                Pair("augmenter", "diminuer"),
                Pair("majorer", "réduire"),
                Pair("arrêter", "continuer"),
                Pair("interrompre", "poursuivre"),
                Pair("contre-indiqué", "recommandé")
            )

            for ((term1, term2) in oppositionPatterns) {
                if ((combinedText1.contains(term1) && combinedText2.contains(term2)) ||
                    (combinedText1.contains(term2) && combinedText2.contains(term1))) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Résout les contradictions en conservant les recommandations les plus pertinentes.
     *
     * @param recommendations Liste complète des recommandations
     * @param contradictions Paires de recommandations contradictoires
     * @return Liste des recommandations non contradictoires
     */
    private fun resolveContradictions(
        recommendations: List<LlmRecommendation>,
        contradictions: List<Pair<LlmRecommendation, LlmRecommendation>>
    ): List<LlmRecommendation> {
        // Ensemble des recommandations à exclure
        val excludedRecommendations = mutableSetOf<String>()

        // Pour chaque contradiction, conserver la recommandation avec le niveau de confiance le plus élevé
        for ((rec1, rec2) in contradictions) {
            if (rec1.confidenceLevel.score >= rec2.confidenceLevel.score) {
                excludedRecommendations.add(rec2.id)
            } else {
                excludedRecommendations.add(rec1.id)
            }
        }

        return recommendations.filterNot { rec -> excludedRecommendations.contains(rec.id) }
    }

    /**
     * Valide la préservation du sens médical dans les reformulations.
     *
     * @param response La réponse à valider
     * @return La réponse validée, potentiellement modifiée
     */
    private fun validateSemanticPreservation(response: LlmResponse): LlmResponse {
        logger.debug("Validation de la préservation sémantique")

        // Cette validation est complexe et nécessiterait idéalement une comparaison
        // avec les sources originales. Pour cette implémentation, nous nous limitons
        // à la vérification de la terminologie médicale correcte.

        val validatedRecommendations = response.recommendations.map { recommendation ->
            // Vérification de la terminologie médicale précise
            val normalizedDescription = medicalTermsService.validateAndNormalizeMedicalTerms(
                recommendation.description, requireStrictTerminology = true)
            val normalizedSuggestion = medicalTermsService.validateAndNormalizeMedicalTerms(
                recommendation.suggestion, requireStrictTerminology = true)

            recommendation.copy(
                description = normalizedDescription,
                suggestion = normalizedSuggestion
            )
        }

        return response.copy(recommendations = validatedRecommendations)
    }

    /**
     * Valide la traçabilité des recommandations (citation des sources).
     *
     * @param response La réponse à valider
     * @return La réponse validée, potentiellement modifiée
     */
    private fun validateTraceability(response: LlmResponse): LlmResponse {
        logger.debug("Validation de la traçabilité des sources")

        if (!sourceValidationEnabled) {
            logger.debug("Validation des sources désactivée, traçabilité non vérifiée")
            return response
        }

        val validatedRecommendations = response.recommendations.map { recommendation ->
            // Vérification de la présence d'au moins une source
            if (recommendation.sources.isEmpty()) {
                logger.warn("Recommandation sans source: {}", recommendation.id)

                // Ajouter une note sur l'absence de source
                val updatedDescription = recommendation.description +
                    "\n[Note: Cette recommandation n'est pas accompagnée de sources scientifiques validées.]"

                return@map recommendation.copy(
                    description = updatedDescription,
                    confidenceLevel = recommendation.confidenceLevel.copy(
                        score = recommendation.confidenceLevel.score * 0.8 // Réduction du score de confiance
                    )
                )
            }

            // Vérification de la validité des sources
            val validatedSources = recommendation.sources.filter { source ->
                medicalSourcesService.validateSource(source).let { validation ->
                    if (!validation.isValid) {
                        logger.warn("Source invalide: {} - {}", source.reference, validation.reason)
                    }
                    validation.isValid
                }
            }

            // Si aucune source n'est valide, ajuster la recommandation
            if (validatedSources.isEmpty() && recommendation.sources.isNotEmpty()) {
                logger.warn("Toutes les sources de la recommandation {} sont invalides", recommendation.id)

                val updatedDescription = recommendation.description +
                    "\n[Note: Les sources citées n'ont pas pu être validées.]"

                recommendation.copy(
                    description = updatedDescription,
                    sources = recommendation.sources, // Conserver les sources mais avec avertissement
                    confidenceLevel = recommendation.confidenceLevel.copy(
                        score = recommendation.confidenceLevel.score * 0.7 // Réduction significative du score
                    )
                )
            } else {
                // Réajuster le score de confiance en fonction du ratio de sources valides
                val validRatio = validatedSources.size.toDouble() / recommendation.sources.size
                val adjustedConfidenceScore = recommendation.confidenceLevel.score *
                    (0.3 + 0.7 * validRatio) // 30% de base + 70% proportionnel aux sources valides

                recommendation.copy(
                    sources = validatedSources,
                    confidenceLevel = recommendation.confidenceLevel.copy(
                        score = adjustedConfidenceScore
                    )
                )
            }
        }

        return response.copy(recommendations = validatedRecommendations)
    }

    /**
     * Valide les considérations éthiques des recommandations.
     *
     * @param response La réponse à valider
     * @return La réponse validée, potentiellement modifiée
     */
    private fun validateEthicalConsiderations(response: LlmResponse): LlmResponse {
        logger.debug("Validation des considérations éthiques")

        val ethicalIssues = mutableListOf<String>()
        val validatedRecommendations = response.recommendations.map { recommendation ->
            // Vérification des problèmes éthiques potentiels
            val ethicalFlags = checkEthicalFlags(recommendation)

            if (ethicalFlags.isNotEmpty()) {
                ethicalIssues.addAll(ethicalFlags)

                // Ajout d'une note sur les considérations éthiques
                val ethicalNote = "\n[Note: Cette recommandation soulève des considérations éthiques: " +
                    "${ethicalFlags.joinToString(", ")}. Une évaluation personnalisée par le pharmacien " +
                    "est fortement recommandée.]"

                recommendation.copy(
                    suggestion = recommendation.suggestion + ethicalNote
                )
            } else {
                recommendation
            }
        }

        // Si des problèmes éthiques ont été identifiés, les mentionner dans le résumé
        val updatedSummary = if (ethicalIssues.isNotEmpty()) {
            val ethicalSummary = "\n\nConsidérations éthiques à prendre en compte: " +
                "${ethicalIssues.distinct().joinToString(". ")}. " +
                "Ces aspects nécessitent une attention particulière lors de l'analyse pharmaceutique."

            response.summary + ethicalSummary
        } else {
            response.summary
        }

        return response.copy(
            recommendations = validatedRecommendations,
            summary = updatedSummary
        )
    }

    /**
     * Vérifie la présence de problèmes éthiques dans une recommandation.
     *
     * @param recommendation La recommandation à vérifier
     * @return Liste des problèmes éthiques identifiés
     */
    private fun checkEthicalFlags(recommendation: LlmRecommendation): List<String> {
        val ethicalFlags = mutableListOf<String>()
        val combinedText = (recommendation.description + " " + recommendation.suggestion).toLowerCase()

        // Mots-clés associés à des considérations éthiques particulières
        val ethicalKeywords = mapOf(
            "rapport bénéfice/risque incertain" to
                setOf("bénéfice/risque incertain", "bénéfice-risque défavorable", "balance incertaine"),
            "absence de consensus scientifique" to
                setOf("consensus partiel", "absence de consensus", "avis divergent", "controverse"),
            "impact potentiel sur la qualité de vie" to
                setOf("qualité de vie", "confort du patient", "impact quotidien", "autonomie"),
            "considérations économiques significatives" to
                setOf("coût élevé", "problème de remboursement", "reste à charge", "accessibilité financière"),
            "risque d'effets indésirables graves" to
                setOf("effet indésirable grave", "risque vital", "toxicité importante", "hospitalisation")
        )

        // Vérifier la présence de chaque type de considération éthique
        for ((flag, keywords) in ethicalKeywords) {
            if (keywords.any { keyword -> combinedText.contains(keyword) }) {
                ethicalFlags.add(flag)
            }
        }

        return ethicalFlags
    }

    /**
     * Effectue une validation finale de sécurité.
     *
     * @param response La réponse à valider
     * @throws ValidationException Si la validation de sécurité échoue
     */
    private fun validateSafety(response: LlmResponse) {
        // Assertion #1: Vérifier que la réponse contient des recommandations ou une explication
        require(response.recommendations.isNotEmpty() || response.summary.length > 100) {
            "La réponse ne contient ni recommandations ni explication suffisante"
        }

        // Assertion #2: Vérifier l'absence de recommandations à risque vital non sourcées
        response.recommendations.forEach { recommendation ->
            val isHighRisk = recommendation.description.toLowerCase().contains("risque vital") ||
                recommendation.suggestion.toLowerCase().contains("risque vital") ||
                recommendation.description.toLowerCase().contains("urgence médicale") ||
                recommendation.suggestion.toLowerCase().contains("urgence médicale")

            if (isHighRisk && (recommendation.sources.isEmpty() ||
                recommendation.confidenceLevel.score < 0.9)) {
                throw ValidationException(
                    "Recommandation à risque vital détectée sans sources suffisantes ou " +
                    "avec un niveau de confiance insuffisant: ${recommendation.id}"
                )
            }
        }
    }
}

package com.hygie.llmorchestrator.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import java.util.UUID
import com.fasterxml.jackson.annotation.JsonFormat
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size
import kotlin.jvm.Transient

/**
 * Énumération des différents modèles LLM disponibles dans le système.
 * Chaque modèle est spécialisé dans un aspect particulier de l'analyse médicale.
 */
enum class LlmModelType {
    BIOMISTRAL,    // Spécialisé dans les interactions biomédicales et la pharmacologie
    HIPPOMISTRAL,  // Spécialisé dans le diagnostic et les recommandations cliniques
    MEDFOUND,      // Spécialisé dans la recherche de littérature médicale et les preuves
    ENSEMBLE       // Utilisation combinée de plusieurs modèles
}

/**
 * Énumération des catégories d'analyse pharmaceutique.
 * Définit les différents types d'analyses pouvant être effectuées.
 */
enum class AnalysisCategory {
    DRUG_INTERACTION,        // Analyse des interactions médicamenteuses
    CONTRAINDICATION,        // Analyse des contre-indications
    DOSAGE_ADJUSTMENT,       // Ajustement posologique
    THERAPEUTIC_REDUNDANCY,  // Redondance thérapeutique
    SIDE_EFFECT_RISK,        // Risque d'effets indésirables
    ELDERLY_APPROPRIATENESS, // Pertinence pour les personnes âgées (critères STOPP/START)
    ADHERENCE_OPTIMIZATION,  // Optimisation de l'observance
    COST_OPTIMIZATION        // Optimisation du coût
}

/**
 * Représente une requête d'analyse envoyée aux modèles LLM.
 *
 * @property requestId Identifiant unique de la requête
 * @property patientContext Contexte patient complet pseudonymisé (âge, sexe, conditions médicales, etc.)
 * @property medications Liste des médicaments avec leurs posologies et durées
 * @property analysisCategories Catégories d'analyse demandées
 * @property additionalContext Informations supplémentaires pertinentes pour l'analyse
 * @property modelType Type de modèle LLM à utiliser
 * @property maxTokens Nombre maximum de tokens dans la réponse
 * @property temperature Paramètre de température pour contrôler la créativité du modèle
 */
data class LlmRequest(
    val requestId: String = UUID.randomUUID().toString(),
    @field:NotBlank @field:Size(max = 20000) val patientContext: String,
    @field:NotNull @field:Size(min = 1, max = 100) val medications: List<MedicationInfo>,
    @field:NotNull @field:Size(min = 1) val analysisCategories: Set<AnalysisCategory>,
    val additionalContext: String? = null,
    val modelType: LlmModelType = LlmModelType.ENSEMBLE,
    val maxTokens: Int = 4096,
    val temperature: Double = 0.3
) {
    @Transient
    private val maxMedications = 100

    /**
     * Validation des préconditions de la requête.
     *
     * @throws IllegalArgumentException si les validations échouent
     */
    fun validate() {
        // Assertion #1: Vérification de la taille de la liste des médicaments
        require(medications.size <= maxMedications) {
            "Le nombre de médicaments ne peut pas dépasser $maxMedications"
        }

        // Assertion #2: Vérification que la température est dans une plage valide
        require(temperature in 0.0..1.0) {
            "La température doit être comprise entre 0.0 et 1.0, valeur fournie: $temperature"
        }
    }
}

/**
 * Information sur un médicament pour l'analyse.
 *
 * @property name Nom du médicament (DCI ou commercial)
 * @property dosage Posologie complète
 * @property duration Durée du traitement
 * @property route Voie d'administration
 * @property startDate Date de début du traitement
 * @property isActive Indique si le traitement est actif actuellement
 */
data class MedicationInfo(
    @field:NotBlank val name: String,
    @field:NotBlank val dosage: String,
    val duration: String? = null,
    val route: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd") val startDate: Instant? = null,
    val isActive: Boolean = true
)

/**
 * Source d'une recommandation ou information médicale.
 *
 * @property type Type de source (article scientifique, référentiel, guide de pratique, etc.)
 * @property reference Référence bibliographique complète
 * @property url URL vers la source (si disponible)
 * @property year Année de publication
 */
data class RecommendationSource(
    val type: String,
    val reference: String,
    val url: String? = null,
    val year: Int? = null
)

/**
 * Niveau de confiance associé à une recommandation.
 *
 * @property score Score numérique entre 0 et 1
 * @property rationale Justification du score de confiance
 * @property evidenceGrade Grade de preuve (selon les standards médicaux)
 */
data class ConfidenceLevel(
    val score: Double,
    val rationale: String,
    val evidenceGrade: String? = null
) {
    init {
        // Assertion #1: Vérification que le score est dans une plage valide
        require(score in 0.0..1.0) {
            "Le score de confiance doit être compris entre 0.0 et 1.0"
        }

        // Assertion #2: Vérification que la justification n'est pas vide
        require(rationale.isNotBlank()) {
            "La justification du niveau de confiance ne peut pas être vide"
        }
    }
}

/**
 * Recommandation pharmaceutique générée par un modèle LLM.
 *
 * @property id Identifiant unique de la recommandation
 * @property category Catégorie d'analyse
 * @property description Description détaillée du problème identifié
 * @property suggestion Suggestion d'intervention pharmaceutique
 * @property medications Médicaments concernés par la recommandation
 * @property confidenceLevel Niveau de confiance de la recommandation
 * @property sources Sources scientifiques supportant la recommandation
 */
data class LlmRecommendation(
    val id: String = UUID.randomUUID().toString(),
    @field:NotNull val category: AnalysisCategory,
    @field:NotBlank val description: String,
    @field:NotBlank val suggestion: String,
    @field:NotNull @field:Size(min = 1) val medications: List<String>,
    @field:NotNull val confidenceLevel: ConfidenceLevel,
    @field:NotNull @field:Size(min = 1) val sources: List<RecommendationSource>
)

/**
 * Réponse globale retournée par le service d'orchestration LLM.
 *
 * @property requestId Identifiant de la requête correspondante
 * @property recommendations Liste des recommandations pharmaceutiques
 * @property summary Résumé de l'analyse complète
 * @property timestamp Horodatage de la génération de la réponse
 * @property modelInfo Informations sur le(s) modèle(s) utilisé(s)
 * @property processingTimeMs Temps de traitement en millisecondes
 */
@Document(collection = "llm_responses")
data class LlmResponse(
    @Id val id: String = UUID.randomUUID().toString(),
    val requestId: String,
    @field:NotNull @field:Size(min = 0) val recommendations: List<LlmRecommendation>,
    val summary: String,
    val timestamp: Instant = Instant.now(),
    val modelInfo: LlmModelInfo,
    val processingTimeMs: Long
) {
    /**
     * Validation des données de la réponse.
     *
     * @throws IllegalArgumentException si les validations échouent
     */
    fun validate() {
        // Assertion #1: Vérification que le temps de traitement est positif
        require(processingTimeMs > 0) {
            "Le temps de traitement doit être positif"
        }

        // Assertion #2: Vérification de la cohérence du timestamp
        require(!timestamp.isAfter(Instant.now())) {
            "L'horodatage ne peut pas être dans le futur"
        }
    }
}

/**
 * Informations sur le modèle LLM utilisé pour générer une réponse.
 *
 * @property modelType Type(s) de modèle(s) utilisé(s)
 * @property modelVersion Version du modèle
 * @property tokenCount Nombre de tokens utilisés
 */
data class LlmModelInfo(
    val modelType: Set<LlmModelType>,
    val modelVersion: String,
    val tokenCount: Int
)

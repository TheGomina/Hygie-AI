package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.model.AnalysisCategory
import com.hygie.llmorchestrator.model.LlmModelType
import com.hygie.llmorchestrator.model.LlmRequest
import com.hygie.llmorchestrator.model.MedicationInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.PostConstruct

/**
 * Service d'ingénierie de prompts pour optimiser les interactions avec les LLMs.
 *
 * Ce service génère des prompts optimisés pour chaque modèle LLM et contexte d'analyse
 * pharmaceutique, maximisant ainsi la pertinence et la qualité des réponses obtenues.
 *
 * @property resourceLoader Chargeur de ressources Spring
 * @property templatesPath Chemin vers les templates de prompts
 * @property contextWindowSize Taille maximale de la fenêtre de contexte pour les modèles
 * @author Hygie-AI Team
 */
@Service
class PromptEngineeringService(
    private val resourceLoader: ResourceLoader,
    @Value("\${llm.prompt-engineering.templates-path:classpath:prompts/}")
    private val templatesPath: String,
    @Value("\${llm.prompt-engineering.context-window-size:16384}")
    private val contextWindowSize: Int
) {
    private val logger = LoggerFactory.getLogger(PromptEngineeringService::class.java)
    private val promptTemplates = ConcurrentHashMap<String, String>()

    /**
     * Charge les templates de prompts au démarrage du service.
     */
    @PostConstruct
    fun loadPromptTemplates() {
        logger.info("Chargement des templates de prompts depuis: {}", templatesPath)

        val templateTypes = listOf(
            "general", "drug_interaction", "contraindication",
            "dosage_adjustment", "elderly", "adherence"
        )

        val modelTypes = LlmModelType.values()

        // Pour chaque combinaison de modèle et type de template
        for (modelType in modelTypes) {
            for (templateType in templateTypes) {
                val templatePath = "$templatesPath${modelType.name.lowercase()}_${templateType}.txt"
                try {
                    val resource = resourceLoader.getResource(templatePath)
                    if (resource.exists()) {
                        val templateContent = resource.inputStream.readAllBytes()
                            .toString(StandardCharsets.UTF_8)
                        promptTemplates["${modelType}_${templateType}"] = templateContent
                        logger.debug("Template chargé: {}", templatePath)
                    } else {
                        // Utiliser le template général pour ce modèle comme fallback
                        val fallbackPath = "$templatesPath${modelType.name.lowercase()}_general.txt"
                        val fallbackResource = resourceLoader.getResource(fallbackPath)
                        if (fallbackResource.exists()) {
                            val fallbackContent = fallbackResource.inputStream.readAllBytes()
                                .toString(StandardCharsets.UTF_8)
                            promptTemplates["${modelType}_${templateType}"] = fallbackContent
                            logger.debug("Fallback template utilisé pour {}: {}",
                                templatePath, fallbackPath)
                        } else {
                            logger.warn("Aucun template trouvé pour: {}", templatePath)
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Erreur lors du chargement du template {}: {}",
                        templatePath, e.message, e)
                }
            }
        }

        // Assertion #1: Vérifier qu'au moins un template a été chargé
        require(promptTemplates.isNotEmpty()) {
            "Aucun template de prompt n'a pu être chargé, vérifiez le chemin: $templatesPath"
        }

        // Assertion #2: Vérifier que chaque modèle a au moins un template général
        for (modelType in modelTypes) {
            require(promptTemplates.containsKey("${modelType}_general")) {
                "Template général manquant pour le modèle: $modelType"
            }
        }

        logger.info("{} templates de prompts chargés avec succès", promptTemplates.size)
    }

    /**
     * Génère un prompt optimisé pour une requête LLM spécifique.
     *
     * @param request La requête LLM contenant le contexte patient et les médicaments
     * @return Un Mono contenant le prompt optimisé
     */
    fun generatePrompt(request: LlmRequest): Mono<String> {
        return Mono.fromCallable {
            logger.debug("Génération de prompt pour la requête: {}", request.requestId)

            // Sélection du template le plus approprié selon la catégorie d'analyse principale
            val templateKey = selectBestTemplate(request.modelType, request.analysisCategories)
            val template = promptTemplates[templateKey] ?: promptTemplates["${request.modelType}_general"]
            ?: throw IllegalStateException("Aucun template disponible pour: $templateKey")

            // Génération des sections du prompt
            val patientContextSection = formatPatientContext(request.patientContext)
            val medicationsSection = formatMedications(request.medications)
            val analysisInstructionsSection = formatAnalysisInstructions(request.analysisCategories)
            val additionalContextSection = formatAdditionalContext(request.additionalContext)

            // Substitution des variables dans le template
            var finalPrompt = template
                .replace("{PATIENT_CONTEXT}", patientContextSection)
                .replace("{MEDICATIONS}", medicationsSection)
                .replace("{ANALYSIS_INSTRUCTIONS}", analysisInstructionsSection)
                .replace("{ADDITIONAL_CONTEXT}", additionalContextSection)
                .replace("{REQUEST_ID}", request.requestId)

            // Troncature si nécessaire pour respecter la taille maximale de contexte
            // Assertion #1: Vérifier que la taille du prompt est conforme à la taille de contexte
            val promptLength = finalPrompt.length
            if (promptLength > contextWindowSize) {
                logger.warn("Prompt trop long ({} caractères), troncature à {} caractères",
                    promptLength, contextWindowSize)
                finalPrompt = truncatePrompt(finalPrompt, contextWindowSize)
            }

            // Assertion #2: Vérifier que le prompt final n'est pas vide
            require(finalPrompt.isNotBlank()) {
                "Le prompt généré est vide pour la requête: ${request.requestId}"
            }

            logger.debug("Prompt généré pour la requête {} ({} caractères)",
                request.requestId, finalPrompt.length)

            finalPrompt
        }.subscribeOn(Schedulers.boundedElastic())
    }

    /**
     * Sélectionne le meilleur template selon le modèle et les catégories d'analyse.
     *
     * @param modelType Le type de modèle LLM
     * @param categories Les catégories d'analyse demandées
     * @return La clé du template sélectionné
     */
    private fun selectBestTemplate(
        modelType: LlmModelType,
        categories: Set<AnalysisCategory>
    ): String {
        // Correspondance entre catégories d'analyse et types de templates
        val categoryToTemplateMap = mapOf(
            AnalysisCategory.DRUG_INTERACTION to "drug_interaction",
            AnalysisCategory.CONTRAINDICATION to "contraindication",
            AnalysisCategory.DOSAGE_ADJUSTMENT to "dosage_adjustment",
            AnalysisCategory.ELDERLY_APPROPRIATENESS to "elderly",
            AnalysisCategory.ADHERENCE_OPTIMIZATION to "adherence"
        )

        // Si une seule catégorie est demandée et qu'elle a un template spécifique, l'utiliser
        if (categories.size == 1) {
            val category = categories.first()
            val templateType = categoryToTemplateMap[category]
            if (templateType != null) {
                val key = "${modelType}_$templateType"
                if (promptTemplates.containsKey(key)) {
                    return key
                }
            }
        }

        // Pour plusieurs catégories, utiliser une heuristique de priorité
        // Prioriser les catégories avec des risques plus élevés
        val priorityOrder = listOf(
            AnalysisCategory.CONTRAINDICATION,
            AnalysisCategory.DRUG_INTERACTION,
            AnalysisCategory.DOSAGE_ADJUSTMENT,
            AnalysisCategory.ELDERLY_APPROPRIATENESS
        )

        for (priorityCategory in priorityOrder) {
            if (categories.contains(priorityCategory)) {
                val templateType = categoryToTemplateMap[priorityCategory]
                if (templateType != null) {
                    val key = "${modelType}_$templateType"
                    if (promptTemplates.containsKey(key)) {
                        return key
                    }
                }
            }
        }

        // Par défaut, utiliser le template général
        return "${modelType}_general"
    }

    /**
     * Formate le contexte patient pour le prompt.
     *
     * @param patientContext Le contexte patient brut
     * @return Le contexte patient formaté
     */
    private fun formatPatientContext(patientContext: String): String {
        // Vérification de la taille du contexte patient
        if (patientContext.length > 10000) {
            return "CONTEXTE PATIENT:\n" + patientContext.substring(0, 10000) +
                "\n[Contexte tronqué en raison de sa taille...]"
        }
        return "CONTEXTE PATIENT:\n$patientContext"
    }

    /**
     * Formate la liste des médicaments pour le prompt.
     *
     * @param medications La liste des médicaments à analyser
     * @return La section du prompt concernant les médicaments
     */
    private fun formatMedications(medications: List<MedicationInfo>): String {
        val sb = StringBuilder("MÉDICAMENTS À ANALYSER:\n")

        for ((index, med) in medications.withIndex()) {
            sb.append("${index + 1}. ${med.name} - ${med.dosage}")
            med.route?.let { sb.append(", Voie: $it") }
            med.duration?.let { sb.append(", Durée: $it") }
            sb.append(", Statut: ${if (med.isActive) "Actif" else "Inactif"}")
            sb.append("\n")
        }

        return sb.toString()
    }

    /**
     * Formate les instructions d'analyse selon les catégories demandées.
     *
     * @param categories Les catégories d'analyse demandées
     * @return La section du prompt avec les instructions d'analyse
     */
    private fun formatAnalysisInstructions(categories: Set<AnalysisCategory>): String {
        val sb = StringBuilder("INSTRUCTIONS D'ANALYSE:\n")

        // Map des descriptions spécifiques pour chaque catégorie
        val categoryDescriptions = mapOf(
            AnalysisCategory.DRUG_INTERACTION to
                "Identifiez les interactions médicamenteuses significatives et graves.",
            AnalysisCategory.CONTRAINDICATION to
                "Détectez les contre-indications absolues ou relatives pour le patient.",
            AnalysisCategory.DOSAGE_ADJUSTMENT to
                "Évaluez la nécessité d'ajuster les posologies selon les caractéristiques du patient.",
            AnalysisCategory.THERAPEUTIC_REDUNDANCY to
                "Identifiez les redondances thérapeutiques potentielles.",
            AnalysisCategory.SIDE_EFFECT_RISK to
                "Évaluez les risques d'effets indésirables pertinents.",
            AnalysisCategory.ELDERLY_APPROPRIATENESS to
                "Analysez l'adéquation des traitements selon les critères STOPP/START et Beers.",
            AnalysisCategory.ADHERENCE_OPTIMIZATION to
                "Suggérez des optimisations pour améliorer l'observance.",
            AnalysisCategory.COST_OPTIMIZATION to
                "Proposez des alternatives pour optimiser le coût du traitement."
        )

        for (category in categories) {
            sb.append("- ${categoryDescriptions[category] ?: "Analysez selon la catégorie: $category"}\n")
        }

        sb.append("\nPour chaque problème identifié, fournissez:\n")
        sb.append("1. Une description claire du problème\n")
        sb.append("2. Une proposition d'intervention pharmaceutique précise\n")
        sb.append("3. Les médicaments concernés\n")
        sb.append("4. Un niveau de confiance justifié\n")
        sb.append("5. Des sources scientifiques pertinentes\n")

        return sb.toString()
    }

    /**
     * Formate le contexte additionnel pour le prompt.
     *
     * @param additionalContext Le contexte additionnel fourni
     * @return La section formattée du contexte additionnel
     */
    private fun formatAdditionalContext(additionalContext: String?): String {
        if (additionalContext.isNullOrBlank()) {
            return ""
        }
        return "\nCONTEXTE ADDITIONNEL:\n$additionalContext"
    }

    /**
     * Tronque intelligemment un prompt pour respecter la taille de contexte maximale.
     *
     * @param prompt Le prompt complet
     * @param maxSize La taille maximale autorisée
     * @return Le prompt tronqué
     */
    private fun truncatePrompt(prompt: String, maxSize: Int): String {
        // Si le prompt est déjà assez court, le retourner tel quel
        if (prompt.length <= maxSize) {
            return prompt
        }

        // Identifier les sections du prompt
        val patientContextIndex = prompt.indexOf("CONTEXTE PATIENT:")
        val medicationsIndex = prompt.indexOf("MÉDICAMENTS À ANALYSER:")
        val instructionsIndex = prompt.indexOf("INSTRUCTIONS D'ANALYSE:")
        val additionalContextIndex = prompt.indexOf("CONTEXTE ADDITIONNEL:")

        // Calculer la taille de chaque section
        val sections = mutableListOf<Pair<String, IntRange>>()

        if (patientContextIndex >= 0) {
            val nextSectionStart = minOf(
                medicationsIndex.takeIf { it >= 0 } ?: Int.MAX_VALUE,
                instructionsIndex.takeIf { it >= 0 } ?: Int.MAX_VALUE,
                additionalContextIndex.takeIf { it >= 0 } ?: Int.MAX_VALUE
            )
            if (nextSectionStart != Int.MAX_VALUE) {
                sections.add("CONTEXTE PATIENT" to IntRange(patientContextIndex, nextSectionStart - 1))
            }
        }

        if (medicationsIndex >= 0) {
            val nextSectionStart = minOf(
                instructionsIndex.takeIf { it >= 0 } ?: Int.MAX_VALUE,
                additionalContextIndex.takeIf { it >= 0 } ?: Int.MAX_VALUE
            )
            if (nextSectionStart != Int.MAX_VALUE) {
                sections.add("MÉDICAMENTS" to IntRange(medicationsIndex, nextSectionStart - 1))
            }
        }

        if (instructionsIndex >= 0) {
            val nextSectionStart = additionalContextIndex.takeIf { it >= 0 } ?: Int.MAX_VALUE
            if (nextSectionStart != Int.MAX_VALUE) {
                sections.add("INSTRUCTIONS" to IntRange(instructionsIndex, nextSectionStart - 1))
            } else {
                sections.add("INSTRUCTIONS" to IntRange(instructionsIndex, prompt.length))
            }
        }

        if (additionalContextIndex >= 0) {
            sections.add("ADDITIONNEL" to IntRange(additionalContextIndex, prompt.length))
        }

        // Stratégie de troncature: réduire d'abord le contexte patient, puis le contexte additionnel
        var truncatedPrompt = prompt

        // Réduire le contexte patient si nécessaire
        val patientSection = sections.find { it.first == "CONTEXTE PATIENT" }
        if (patientSection != null && truncatedPrompt.length > maxSize) {
            val patientContext = truncatedPrompt.substring(patientSection.second)
            val maxPatientContextSize = maxSize - (truncatedPrompt.length - patientContext.length) - 40 // 40 pour le message de troncature
            if (maxPatientContextSize > 0) {
                val truncatedContext = patientContext.substring(0, minOf(maxPatientContextSize, patientContext.length)) +
                    "\n[Contexte patient tronqué en raison de la taille de la requête...]"
                truncatedPrompt = truncatedPrompt.replace(patientContext, truncatedContext)
            }
        }

        // Réduire le contexte additionnel si nécessaire
        val additionalSection = sections.find { it.first == "ADDITIONNEL" }
        if (additionalSection != null && truncatedPrompt.length > maxSize) {
            val additionalContext = truncatedPrompt.substring(additionalSection.second)
            val maxAdditionalContextSize = maxSize - (truncatedPrompt.length - additionalContext.length) - 40
            if (maxAdditionalContextSize > 0) {
                val truncatedContext = additionalContext.substring(0, minOf(maxAdditionalContextSize, additionalContext.length)) +
                    "\n[Contexte additionnel tronqué en raison de la taille de la requête...]"
                truncatedPrompt = truncatedPrompt.replace(additionalContext, truncatedContext)
            }
        }

        // Si toujours trop long, tronquer brutalement
        if (truncatedPrompt.length > maxSize) {
            truncatedPrompt = truncatedPrompt.substring(0, maxSize - 40) +
                "\n[Prompt tronqué en raison de sa taille...]"
        }

        return truncatedPrompt
    }
}

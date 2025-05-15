package com.hygie.llmorchestrator.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ResourceLoader
import java.nio.charset.StandardCharsets
import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap

/**
 * Service de validation et de normalisation des termes médicaux.
 *
 * Ce service garantit l'exactitude et la cohérence de la terminologie médicale
 * utilisée dans les recommandations pharmaceutiques.
 *
 * @property resourceLoader Chargeur de ressources Spring
 * @property medicalTermsPath Chemin vers le fichier de terminologie médicale
 * @property strictValidation Active la validation stricte de la terminologie
 * @author Hygie-AI Team
 */
@Service
class MedicalTermsService(
    private val resourceLoader: ResourceLoader,
    @Value("\${llm.validation.medical-terms-path:classpath:data/medical_terms.csv}")
    private val medicalTermsPath: String,
    @Value("\${llm.validation.strict-terminology:true}")
    private val strictValidation: Boolean
) {
    private val logger = LoggerFactory.getLogger(MedicalTermsService::class.java)

    // Map des termes médicaux normalisés (terme courant -> terme standard)
    private val standardTerms = ConcurrentHashMap<String, String>()

    // Map des abréviations médicales (abréviation -> forme complète)
    private val medicalAbbreviations = ConcurrentHashMap<String, String>()

    /**
     * Charge les dictionnaires de termes médicaux et d'abréviations.
     */
    @PostConstruct
    fun initialize() {
        try {
            logger.info("Chargement du dictionnaire de termes médicaux depuis: {}", medicalTermsPath)

            val resource = resourceLoader.getResource(medicalTermsPath)
            if (!resource.exists()) {
                logger.warn("Dictionnaire de termes médicaux non trouvé: {}", medicalTermsPath)
                return
            }

            val content = resource.inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
            val lines = content.lines().filter { it.isNotBlank() }

            for (line in lines) {
                val parts = line.split(";")
                if (parts.size >= 2) {
                    val commonTerm = parts[0].trim().toLowerCase()
                    val standardTerm = parts[1].trim()

                    standardTerms[commonTerm] = standardTerm

                    // Enregistrer également les éventuelles abréviations
                    if (parts.size >= 3 && parts[2].trim().isNotBlank()) {
                        val abbreviation = parts[2].trim().toLowerCase()
                        medicalAbbreviations[abbreviation] = standardTerm
                    }
                }
            }

            // Assertion #1: Vérifier que le dictionnaire contient des entrées
            require(standardTerms.isNotEmpty()) {
                "Le dictionnaire de termes médicaux est vide"
            }

            // Assertion #2: Vérifier qu'il y a des abréviations chargées
            require(medicalAbbreviations.isNotEmpty()) {
                "Aucune abréviation médicale chargée"
            }

            logger.info("{} termes médicaux et {} abréviations chargés avec succès",
                standardTerms.size, medicalAbbreviations.size)

        } catch (e: Exception) {
            logger.error("Erreur lors du chargement du dictionnaire de termes médicaux: {}",
                e.message, e)
        }
    }

    /**
     * Valide et normalise les termes médicaux dans un texte.
     *
     * @param text Le texte contenant des termes médicaux
     * @param requireStrictTerminology Indique si une terminologie stricte est requise
     * @return Le texte avec termes médicaux normalisés
     */
    fun validateAndNormalizeMedicalTerms(
        text: String,
        requireStrictTerminology: Boolean = false
    ): String {
        // Si le dictionnaire est vide ou si la validation n'est pas activée, retourner le texte tel quel
        if (standardTerms.isEmpty() || (!strictValidation && !requireStrictTerminology)) {
            return text
        }

        var normalizedText = text

        // Remplacer les abréviations par leur forme complète
        for ((abbreviation, fullForm) in medicalAbbreviations) {
            val regex = "\\b$abbreviation\\b".toRegex(RegexOption.IGNORE_CASE)
            normalizedText = normalizedText.replace(regex, fullForm)
        }

        // Normaliser les termes médicaux courants vers leur forme standard
        for ((commonTerm, standardTerm) in standardTerms) {
            val regex = "\\b$commonTerm\\b".toRegex(RegexOption.IGNORE_CASE)
            normalizedText = normalizedText.replace(regex, standardTerm)
        }

        return normalizedText
    }

    /**
     * Vérifie si un terme médical spécifique est valide selon la terminologie standard.
     *
     * @param term Le terme médical à vérifier
     * @return true si le terme est valide ou a un équivalent standard
     */
    fun isValidMedicalTerm(term: String): Boolean {
        if (standardTerms.isEmpty()) {
            // Sans dictionnaire, on ne peut pas valider, donc on considère le terme comme valide
            return true
        }

        val normalizedTerm = term.trim().toLowerCase()

        // Le terme est valide s'il existe dans le dictionnaire ou est lui-même un terme standard
        return standardTerms.containsKey(normalizedTerm) ||
               standardTerms.values.any { it.equals(term, ignoreCase = true) } ||
               medicalAbbreviations.containsKey(normalizedTerm)
    }

    /**
     * Extrait et valide les termes médicaux présents dans un texte.
     *
     * @param text Le texte à analyser
     * @return Une map des termes médicaux identifiés avec leur forme normalisée
     */
    fun extractMedicalTerms(text: String): Map<String, String> {
        val result = mutableMapOf<String, String>()

        if (standardTerms.isEmpty()) {
            return result
        }

        // Pour chaque terme médical connu, vérifier s'il apparaît dans le texte
        for ((commonTerm, standardTerm) in standardTerms) {
            val regex = "\\b$commonTerm\\b".toRegex(RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                result[commonTerm] = standardTerm
            }
        }

        // Vérifier également les abréviations
        for ((abbreviation, fullForm) in medicalAbbreviations) {
            val regex = "\\b$abbreviation\\b".toRegex(RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(text)) {
                result[abbreviation] = fullForm
            }
        }

        return result
    }
}

package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.model.RecommendationSource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.Year
import javax.annotation.PostConstruct
import java.util.concurrent.ConcurrentHashMap

/**
 * Résultat de la validation d'une source médicale.
 *
 * @property isValid Indique si la source est valide
 * @property reason Raison de l'invalidation (si applicable)
 * @property validatedReference Référence validée et normalisée (si applicable)
 */
data class SourceValidationResult(
    val isValid: Boolean,
    val reason: String? = null,
    val validatedReference: String? = null
)

/**
 * Service de validation des sources médicales.
 *
 * Ce service vérifie l'existence et la fiabilité des sources scientifiques
 * citées dans les recommandations pharmaceutiques.
 *
 * @property webClient Client HTTP pour les vérifications externes
 * @property pubmedApiKey Clé API pour PubMed
 * @property validationEnabled Flag indiquant si la validation externe est activée
 * @property timeoutMs Délai maximum pour les requêtes de validation
 * @author Hygie-AI Team
 */
@Service
class MedicalSourcesService(
    private val webClient: WebClient,
    @Value("\${llm.validation.pubmed-api-key:}") private val pubmedApiKey: String,
    @Value("\${llm.validation.source-validation-enabled:true}") private val validationEnabled: Boolean,
    @Value("\${llm.validation.source-validation-timeout-ms:5000}") private val timeoutMs: Long
) {
    private val logger = LoggerFactory.getLogger(MedicalSourcesService::class.java)

    // Cache des résultats de validation (référence -> résultat)
    private val validationCache = ConcurrentHashMap<String, SourceValidationResult>()

    // Liste des journaux médicaux reconnus
    private val recognizedJournals = ConcurrentHashMap<String, Int>()

    @PostConstruct
    fun initialize() {
        logger.info("Initialisation du service de validation des sources médicales (validation externe: {})",
            validationEnabled)

        // Charger la liste des journaux médicaux reconnus
        loadRecognizedJournals()

        // Assertions pour la configuration
        // Assertion #1: Vérifier que le timeout est raisonnable
        require(timeoutMs in 1000..30000) {
            "Timeout de validation des sources invalide: $timeoutMs ms (doit être entre 1000 et 30000 ms)"
        }

        // Assertion #2: Vérifier que la clé API est présente si la validation est activée
        if (validationEnabled && pubmedApiKey.isNotBlank()) {
            require(pubmedApiKey.length >= 10) {
                "Clé API PubMed de longueur insuffisante: ${pubmedApiKey.length} caractères"
            }
        } else if (validationEnabled) {
            logger.warn("Validation des sources activée mais aucune clé API PubMed fournie")
        }
    }

    /**
     * Charge la liste des journaux médicaux reconnus.
     */
    private fun loadRecognizedJournals() {
        // Cette liste serait idéalement chargée depuis une base de données ou un fichier
        // Pour cette implémentation, nous ajoutons directement quelques journaux reconnus
        // avec leur facteur d'impact (approximatif)
        val journals = mapOf(
            "NEJM" to 91,
            "New England Journal of Medicine" to 91,
            "N Engl J Med" to 91,
            "Lancet" to 79,
            "The Lancet" to 79,
            "JAMA" to 56,
            "Journal of the American Medical Association" to 56,
            "BMJ" to 39,
            "British Medical Journal" to 39,
            "JAMA Internal Medicine" to 21,
            "Nature Medicine" to 53,
            "Nat Med" to 53,
            "Annals of Internal Medicine" to 25,
            "Ann Intern Med" to 25,
            "JACC" to 24,
            "Journal of the American College of Cardiology" to 24,
            "Circulation" to 29,
            "JAMA Psychiatry" to 18,
            "European Heart Journal" to 30,
            "Eur Heart J" to 30,
            "PLoS Medicine" to 11,
            "PLOS Medicine" to 11,
            "JAMA Oncology" to 31,
            "Cochrane Database of Systematic Reviews" to 9,
            "Cochrane Database Syst Rev" to 9,
            "Journal of Clinical Oncology" to 33,
            "J Clin Oncol" to 33,
            "Blood" to 22,
            "Diabetes Care" to 19,
            "Journal of Hepatology" to 25,
            "J Hepatol" to 25,
            "Gut" to 23,
            "Health Affairs" to 9,
            "American Journal of Respiratory and Critical Care Medicine" to 21,
            "Am J Respir Crit Care Med" to 21,
            "MMWR" to 15,
            "Morbidity and Mortality Weekly Report" to 15,
            "Journal of the American Pharmacists Association" to 3,
            "J Am Pharm Assoc" to 3,
            "Pharmacotherapy" to 4,
            "Annals of Pharmacotherapy" to 3,
            "Ann Pharmacother" to 3,
            "International Journal of Clinical Pharmacy" to 2,
            "Int J Clin Pharm" to 2,
            "British Journal of Clinical Pharmacology" to 5,
            "Br J Clin Pharmacol" to 5,
            "Clinical Pharmacology & Therapeutics" to 7,
            "Clin Pharmacol Ther" to 7
        )

        recognizedJournals.putAll(journals)
        logger.info("{} journaux médicaux reconnus chargés", recognizedJournals.size)
    }

    /**
     * Valide une source médicale.
     *
     * @param source La source à valider
     * @return Un résultat de validation
     */
    fun validateSource(source: RecommendationSource): SourceValidationResult {
        // Vérifier d'abord le cache pour éviter des validations redondantes
        val cacheKey = source.reference.trim()
        validationCache[cacheKey]?.let { cachedResult ->
            logger.debug("Résultat de validation trouvé en cache pour: {}", cacheKey)
            return cachedResult
        }

        // Validation de base (locale)
        val basicValidation = performBasicValidation(source)
        if (!basicValidation.isValid) {
            cacheValidationResult(cacheKey, basicValidation)
            return basicValidation
        }

        // Si la validation externe est désactivée, s'arrêter ici
        if (!validationEnabled) {
            val result = SourceValidationResult(true, null, source.reference)
            cacheValidationResult(cacheKey, result)
            return result
        }

        // Effectuer une validation externe (asynchrone)
        return performExternalValidation(source)
            .doOnSuccess { result -> cacheValidationResult(cacheKey, result) }
            .doOnError { error ->
                logger.error("Erreur lors de la validation externe de la source: {}", error.message, error)
                val fallbackResult = SourceValidationResult(
                    isValid = true, // Considérer valide en cas d'erreur technique
                    reason = "Validation externe indisponible: ${error.message}",
                    validatedReference = source.reference
                )
                cacheValidationResult(cacheKey, fallbackResult)
            }
            .onErrorReturn(
                SourceValidationResult(
                    isValid = true, // Considérer valide en cas d'erreur technique
                    reason = "Validation externe indisponible",
                    validatedReference = source.reference
                )
            )
            .block(Duration.ofMillis(timeoutMs))
            ?: SourceValidationResult(
                isValid = true, // Considérer valide en cas de timeout
                reason = "Timeout lors de la validation externe",
                validatedReference = source.reference
            )
    }

    /**
     * Effectue une validation de base (locale) de la source.
     *
     * @param source La source à valider
     * @return Un résultat de validation
     */
    private fun performBasicValidation(source: RecommendationSource): SourceValidationResult {
        // Assertion #1: Vérifier que la référence n'est pas vide
        if (source.reference.isBlank()) {
            return SourceValidationResult(
                isValid = false,
                reason = "Référence vide"
            )
        }

        // Assertion #2: Vérifier que l'année est valide si présente
        if (source.year != null) {
            val currentYear = Year.now().value
            if (source.year < 1800 || source.year > currentYear) {
                return SourceValidationResult(
                    isValid = false,
                    reason = "Année invalide: ${source.year} (doit être comprise entre 1800 et $currentYear)"
                )
            }
        }

        // Vérifier si la source provient d'un journal reconnu
        val journalName = extractJournalName(source.reference)
        if (journalName != null) {
            val impactFactor = recognizedJournals[journalName]
            if (impactFactor != null) {
                logger.debug("Journal reconnu trouvé dans la référence: {} (impact factor: {})",
                    journalName, impactFactor)
                return SourceValidationResult(
                    isValid = true,
                    validatedReference = source.reference
                )
            }
        }

        // Vérifier si la référence contient un DOI ou PMID
        if (containsIdentifier(source.reference)) {
            return SourceValidationResult(
                isValid = true,
                validatedReference = source.reference
            )
        }

        // Si la source a une URL, elle est considérée comme potentiellement valide
        // mais nécessite une validation externe
        if (!source.url.isNullOrBlank()) {
            return SourceValidationResult(
                isValid = true,
                validatedReference = source.reference
            )
        }

        // Par défaut, considérer la source comme valide mais nécessitant une validation externe
        return SourceValidationResult(
            isValid = true,
            validatedReference = source.reference
        )
    }

    /**
     * Extrait le nom du journal d'une référence bibliographique.
     *
     * @param reference La référence bibliographique
     * @return Le nom du journal identifié, ou null si non trouvé
     */
    private fun extractJournalName(reference: String): String? {
        // Recherche parmi les journaux reconnus
        for (journal in recognizedJournals.keys) {
            if (reference.contains(journal, ignoreCase = true)) {
                return journal
            }
        }

        // Tentative d'extraction selon des patterns courants
        val patterns = listOf(
            """\b([A-Z][A-Za-z\s&]+)\.\s\d{4}""".toRegex(), // Format: Journal Name. 2022
            """\bin\s([A-Z][A-Za-z\s&]+),\s\d{4}""".toRegex(), // Format: in Journal Name, 2022
            """\b([A-Z][A-Za-z\s&]+),\svol\.""".toRegex() // Format: Journal Name, vol.
        )

        for (pattern in patterns) {
            val match = pattern.find(reference)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }
        }

        return null
    }

    /**
     * Vérifie si une référence contient un identifiant standard (DOI, PMID).
     *
     * @param reference La référence à vérifier
     * @return true si un identifiant est détecté
     */
    private fun containsIdentifier(reference: String): Boolean {
        // DOI pattern: 10.xxxx/yyyy
        val doiPattern = """(10\.\d{4,}(?:\.\d+)*/(?:(?!["&\'<>])\S)+)""".toRegex()
        if (doiPattern.containsMatchIn(reference)) {
            return true
        }

        // PMID pattern: PMID: xxxxxxxx
        val pmidPattern = """PMID:\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        if (pmidPattern.containsMatchIn(reference)) {
            return true
        }

        // PubMed Central ID pattern: PMC xxxxxxx
        val pmcPattern = """PMC\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        return pmcPattern.containsMatchIn(reference)
    }

    /**
     * Effectue une validation externe de la source via des API.
     *
     * @param source La source à valider
     * @return Un Mono contenant le résultat de validation
     */
    private fun performExternalValidation(source: RecommendationSource): Mono<SourceValidationResult> {
        // Si une URL est fournie, tenter de la valider
        if (!source.url.isNullOrBlank()) {
            return validateUrl(source.url)
                .map { isValid ->
                    if (isValid) {
                        SourceValidationResult(
                            isValid = true,
                            validatedReference = source.reference
                        )
                    } else {
                        SourceValidationResult(
                            isValid = false,
                            reason = "URL invalide ou inaccessible",
                            validatedReference = source.reference
                        )
                    }
                }
        }

        // Extraire le DOI ou PMID pour validation
        val doi = extractDoi(source.reference)
        if (doi != null) {
            return validateDoi(doi)
                .map { isValid ->
                    if (isValid) {
                        SourceValidationResult(
                            isValid = true,
                            validatedReference = source.reference
                        )
                    } else {
                        SourceValidationResult(
                            isValid = false,
                            reason = "DOI non trouvé dans les bases de données scientifiques",
                            validatedReference = source.reference
                        )
                    }
                }
        }

        val pmid = extractPmid(source.reference)
        if (pmid != null) {
            return validatePmid(pmid)
                .map { isValid ->
                    if (isValid) {
                        SourceValidationResult(
                            isValid = true,
                            validatedReference = source.reference
                        )
                    } else {
                        SourceValidationResult(
                            isValid = false,
                            reason = "PMID non trouvé dans PubMed",
                            validatedReference = source.reference
                        )
                    }
                }
        }

        // Si aucune validation externe n'est possible, s'appuyer sur la validation de base
        return Mono.just(
            SourceValidationResult(
                isValid = true,
                reason = "Aucune validation externe possible",
                validatedReference = source.reference
            )
        )
    }

    /**
     * Extrait un DOI d'une référence.
     *
     * @param reference La référence contenant potentiellement un DOI
     * @return Le DOI extrait, ou null si non trouvé
     */
    private fun extractDoi(reference: String): String? {
        val doiPattern = """(10\.\d{4,}(?:\.\d+)*/(?:(?!["&\'<>])\S)+)""".toRegex()
        val match = doiPattern.find(reference)
        return match?.groupValues?.get(1)
    }

    /**
     * Extrait un PMID d'une référence.
     *
     * @param reference La référence contenant potentiellement un PMID
     * @return Le PMID extrait, ou null si non trouvé
     */
    private fun extractPmid(reference: String): String? {
        val pmidPattern = """PMID:\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        val match = pmidPattern.find(reference)
        return match?.groupValues?.get(1)
    }

    /**
     * Valide un DOI via l'API CrossRef.
     *
     * @param doi Le DOI à valider
     * @return Un Mono indiquant si le DOI est valide
     */
    private fun validateDoi(doi: String): Mono<Boolean> {
        return webClient.get()
            .uri("https://api.crossref.org/works/$doi")
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { response ->
                val status = response["status"] as? String
                status == "ok"
            }
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(false)
    }

    /**
     * Valide un PMID via l'API PubMed.
     *
     * @param pmid Le PMID à valider
     * @return Un Mono indiquant si le PMID est valide
     */
    private fun validatePmid(pmid: String): Mono<Boolean> {
        val baseUrl = "https://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi"
        val apiKeyParam = if (pubmedApiKey.isNotBlank()) "&api_key=$pubmedApiKey" else ""

        return webClient.get()
            .uri("$baseUrl?db=pubmed&id=$pmid&retmode=json$apiKeyParam")
            .retrieve()
            .bodyToMono(Map::class.java)
            .map { response ->
                val result = response["result"] as? Map<*, *>
                result?.containsKey(pmid) == true
            }
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(false)
    }

    /**
     * Valide une URL en vérifiant son accessibilité.
     *
     * @param url L'URL à valider
     * @return Un Mono indiquant si l'URL est accessible
     */
    private fun validateUrl(url: String): Mono<Boolean> {
        return webClient.head()
            .uri(url)
            .retrieve()
            .toBodilessEntity()
            .map { response ->
                response.statusCode.is2xxSuccessful
            }
            .timeout(Duration.ofMillis(timeoutMs))
            .onErrorReturn(false)
    }

    /**
     * Met en cache un résultat de validation.
     *
     * @param key La clé du cache (référence)
     * @param result Le résultat de validation
     */
    private fun cacheValidationResult(key: String, result: SourceValidationResult) {
        validationCache[key] = result
        // Limiter la taille du cache à 1000 entrées
        if (validationCache.size > 1000) {
            val keysToRemove = validationCache.keys.take(100)
            keysToRemove.forEach { validationCache.remove(it) }
        }
    }
}

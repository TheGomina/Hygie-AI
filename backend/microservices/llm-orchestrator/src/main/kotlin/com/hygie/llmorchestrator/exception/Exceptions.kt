package com.hygie.llmorchestrator.exception

import org.springframework.http.HttpStatus

/**
 * Exception de base pour les erreurs du service Hygie-AI.
 *
 * Cette classe sert de parent commun à toutes les exceptions spécifiques
 * du service, facilitant leur gestion cohérente.
 *
 * @property message Message décrivant l'erreur
 * @property cause Cause sous-jacente de l'erreur (optionnelle)
 * @property httpStatus Code HTTP associé à l'erreur
 * @property errorCode Code d'erreur interne pour le suivi et la journalisation
 * @author Hygie-AI Team
 */
abstract class HygieServiceException(
    override val message: String,
    override val cause: Throwable? = null,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    val errorCode: String = "HYGIE-INTERNAL-ERROR"
) : RuntimeException(message, cause) {

    /**
     * Retourne un message d'erreur formaté incluant le code d'erreur.
     *
     * @return Le message d'erreur formaté
     */
    fun getFormattedErrorMessage(): String {
        // Assertion #1: Vérification que le message n'est pas vide
        require(message.isNotBlank()) { "Le message d'erreur ne peut pas être vide" }

        // Assertion #2: Vérification que le code d'erreur est valide
        require(errorCode.matches(Regex("^[A-Z]+-[A-Z0-9-]+$"))) {
            "Format de code d'erreur invalide: $errorCode"
        }

        return "[$errorCode] $message"
    }
}

/**
 * Exception levée pour les erreurs liées aux services LLM.
 *
 * Utilisée lorsqu'une erreur survient pendant l'interaction avec un modèle LLM
 * ou pendant le traitement d'une requête/réponse.
 *
 * @property message Message décrivant l'erreur
 * @property cause Cause sous-jacente de l'erreur (optionnelle)
 */
class LlmServiceException(
    override val message: String,
    override val cause: Throwable? = null
) : HygieServiceException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    errorCode = "HYGIE-LLM-SERVICE-ERROR"
)

/**
 * Exception levée pour les erreurs de validation.
 *
 * Utilisée lorsqu'une donnée ne respecte pas les critères de validation,
 * notamment lors de la validation des réponses des LLMs.
 *
 * @property message Message décrivant l'erreur de validation
 * @property cause Cause sous-jacente de l'erreur (optionnelle)
 */
class ValidationException(
    override val message: String,
    override val cause: Throwable? = null
) : HygieServiceException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.BAD_REQUEST,
    errorCode = "HYGIE-VALIDATION-ERROR"
)

/**
 * Exception levée pour les erreurs d'accès aux ressources.
 *
 * Utilisée lorsqu'une ressource (base de données, fichier, API externe)
 * n'est pas accessible ou renvoie une erreur.
 *
 * @property message Message décrivant l'erreur d'accès
 * @property cause Cause sous-jacente de l'erreur (optionnelle)
 * @property resourceType Type de ressource concernée
 */
class ResourceAccessException(
    override val message: String,
    override val cause: Throwable? = null,
    val resourceType: String
) : HygieServiceException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.FAILED_DEPENDENCY,
    errorCode = "HYGIE-RESOURCE-ACCESS-ERROR"
) {
    init {
        // Assertion #1: Vérification que le type de ressource est spécifié
        require(resourceType.isNotBlank()) { "Le type de ressource doit être spécifié" }

        // Assertion #2: Vérification que le message contient des informations sur la ressource
        require(message.contains(resourceType)) {
            "Le message doit mentionner le type de ressource ($resourceType)"
        }
    }
}

/**
 * Exception levée pour les erreurs de sécurité.
 *
 * Utilisée lorsqu'une opération échoue en raison de problèmes de sécurité,
 * comme des erreurs d'authentification ou d'autorisation.
 *
 * @property message Message décrivant l'erreur de sécurité
 * @property cause Cause sous-jacente de l'erreur (optionnelle)
 */
class SecurityException(
    override val message: String,
    override val cause: Throwable? = null
) : HygieServiceException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.FORBIDDEN,
    errorCode = "HYGIE-SECURITY-ERROR"
)

/**
 * Exception levée pour les erreurs de configuration.
 *
 * Utilisée lorsqu'une configuration incorrecte ou manquante
 * empêche le bon fonctionnement d'un service.
 *
 * @property message Message décrivant l'erreur de configuration
 * @property cause Cause sous-jacente de l'erreur (optionnelle)
 * @property configKey Clé de configuration concernée (optionnelle)
 */
class ConfigurationException(
    override val message: String,
    override val cause: Throwable? = null,
    val configKey: String? = null
) : HygieServiceException(
    message = message,
    cause = cause,
    httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
    errorCode = "HYGIE-CONFIG-ERROR"
) {
    /**
     * Construit un message détaillé incluant la clé de configuration si disponible.
     *
     * @return Le message détaillé
     */
    override fun toString(): String {
        return if (configKey != null) {
            "ConfigurationException: $message (clé: $configKey)"
        } else {
            "ConfigurationException: $message"
        }
    }
}

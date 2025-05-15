package com.hygie.llmorchestrator.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import javax.validation.ConstraintViolationException

/**
 * Représentation standardisée d'une erreur pour les réponses API.
 *
 * @property timestamp Horodatage de l'erreur
 * @property status Code HTTP de l'erreur
 * @property error Type d'erreur
 * @property message Message descriptif de l'erreur
 * @property path Chemin de la requête ayant provoqué l'erreur
 * @property errorCode Code d'erreur interne pour le suivi et la journalisation
 * @property details Détails supplémentaires sur l'erreur (facultatif)
 */
data class ApiError(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val error: String,
    val message: String,
    val path: String,
    val errorCode: String? = null,
    val details: List<String>? = null
)

/**
 * Gestionnaire global des exceptions pour les contrôleurs REST.
 *
 * Intercepte les exceptions non gérées et les convertit en réponses HTTP
 * formatées de manière cohérente.
 *
 * @author Hygie-AI Team
 */
@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Gère les exceptions spécifiques au service Hygie.
     *
     * @param ex L'exception levée
     * @param exchange L'échange web en cours
     * @return Une réponse formatée contenant les détails de l'erreur
     */
    @ExceptionHandler(HygieServiceException::class)
    fun handleHygieServiceException(
        ex: HygieServiceException,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiError> {
        // Assertion #1: Vérification que le message d'erreur n'est pas vide
        val errorMessage = ex.message.takeIf { it.isNotBlank() } ?: "Erreur interne du service"

        // Journalisation de l'erreur
        logger.error("Erreur de service: {} (code: {})", errorMessage, ex.errorCode, ex)

        // Construction de la réponse d'erreur
        val apiError = ApiError(
            status = ex.httpStatus.value(),
            error = ex.httpStatus.reasonPhrase,
            message = errorMessage,
            path = exchange.request.path.value(),
            errorCode = ex.errorCode,
            details = ex.cause?.message?.let { listOf(it) }
        )

        // Assertion #2: Vérification que le statut HTTP est cohérent
        require(ex.httpStatus.value() >= 400) { "Le statut HTTP doit être un code d'erreur (>= 400)" }

        return ResponseEntity
            .status(ex.httpStatus)
            .body(apiError)
    }

    /**
     * Gère les exceptions de validation des contraintes.
     *
     * @param ex L'exception de validation
     * @param exchange L'échange web en cours
     * @return Une réponse formatée contenant les détails des violations
     */
    @ExceptionHandler(ConstraintViolationException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleConstraintViolationException(
        ex: ConstraintViolationException,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiError> {
        logger.warn("Erreur de validation des contraintes: {}", ex.message, ex)

        // Extraction des détails des violations
        val details = ex.constraintViolations.map { violation ->
            "${violation.propertyPath}: ${violation.message}"
        }

        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Erreur de validation des données",
            path = exchange.request.path.value(),
            errorCode = "HYGIE-VALIDATION-ERROR",
            details = details
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(apiError)
    }

    /**
     * Gère les exceptions de liaison des échanges web.
     *
     * @param ex L'exception de liaison
     * @param exchange L'échange web en cours
     * @return Une réponse formatée contenant les détails des erreurs de liaison
     */
    @ExceptionHandler(WebExchangeBindException::class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    fun handleWebExchangeBindException(
        ex: WebExchangeBindException,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiError> {
        logger.warn("Erreur de liaison des données: {}", ex.message, ex)

        // Extraction des détails des erreurs de champ
        val details = ex.bindingResult.fieldErrors.map { fieldError ->
            "${fieldError.field}: ${fieldError.defaultMessage}"
        }

        val apiError = ApiError(
            status = HttpStatus.BAD_REQUEST.value(),
            error = HttpStatus.BAD_REQUEST.reasonPhrase,
            message = "Erreur de validation des données de requête",
            path = exchange.request.path.value(),
            errorCode = "HYGIE-REQUEST-VALIDATION-ERROR",
            details = details
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(apiError)
    }

    /**
     * Gère toutes les autres exceptions non spécifiquement traitées.
     *
     * @param ex L'exception non gérée
     * @param exchange L'échange web en cours
     * @return Une réponse formatée générique
     */
    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleGenericException(
        ex: Exception,
        exchange: ServerWebExchange
    ): ResponseEntity<ApiError> {
        logger.error("Exception non gérée: {}", ex.message, ex)

        val apiError = ApiError(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            error = HttpStatus.INTERNAL_SERVER_ERROR.reasonPhrase,
            message = "Une erreur interne est survenue",
            path = exchange.request.path.value(),
            errorCode = "HYGIE-INTERNAL-ERROR"
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(apiError)
    }
}

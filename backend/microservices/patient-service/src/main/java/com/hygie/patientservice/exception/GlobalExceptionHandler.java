package com.hygie.patientservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global d'exceptions pour le service patient.
 *
 * Centralise la gestion des exceptions et fournit des réponses d'erreur cohérentes
 * pour toutes les APIs du service, conformément aux règles de développement Hygie-AI.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Gère les exceptions liées aux patients non trouvés.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur 404 avec des détails sur l'erreur
     */
    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePatientNotFoundException(
            PatientNotFoundException ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.warn("Patient non trouvé: {}", ex.getMessage());

        final ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getDescription(false));

        // Assertion #2: Vérification de la réponse
        assert errorResponse != null : "La réponse d'erreur ne peut pas être null";
        assert errorResponse.getStatus() == HttpStatus.NOT_FOUND.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère les exceptions liées aux prescriptions non trouvées.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur 404 avec des détails sur l'erreur
     */
    @ExceptionHandler(PrescriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePrescriptionNotFoundException(
            PrescriptionNotFoundException ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.warn("Prescription non trouvée: {}", ex.getMessage());

        final ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getDescription(false));

        // Assertion #2: Vérification de la réponse
        assert errorResponse != null : "La réponse d'erreur ne peut pas être null";
        assert errorResponse.getStatus() == HttpStatus.NOT_FOUND.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère les exceptions liées aux médicaments non trouvés.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur 404 avec des détails sur l'erreur
     */
    @ExceptionHandler(MedicationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMedicationNotFoundException(
            MedicationNotFoundException ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.warn("Médicament non trouvé: {}", ex.getMessage());

        final ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                request.getDescription(false));

        // Assertion #2: Vérification de la réponse
        assert errorResponse != null : "La réponse d'erreur ne peut pas être null";
        assert errorResponse.getStatus() == HttpStatus.NOT_FOUND.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Gère les exceptions liées aux violations de contraintes de validation.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur 400 avec des détails sur les violations
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.warn("Violation de contrainte: {}", ex.getMessage());

        final Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String fieldName = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            errors.put(fieldName, errorMessage);
        });

        final ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur de validation",
                LocalDateTime.now(),
                request.getDescription(false),
                errors);

        // Assertion #2: Vérification de la réponse
        assert validationErrorResponse != null : "La réponse d'erreur de validation ne peut pas être null";
        assert validationErrorResponse.getStatus() == HttpStatus.BAD_REQUEST.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";
        assert !validationErrorResponse.getErrors().isEmpty() :
            "La liste des erreurs de validation ne peut pas être vide";

        return new ResponseEntity<>(validationErrorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère les exceptions liées aux arguments invalides dans les méthodes.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur 400 avec des détails sur les erreurs de validation
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.warn("Argument de méthode invalide: {}", ex.getMessage());

        final Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        final ValidationErrorResponse validationErrorResponse = new ValidationErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Erreur de validation",
                LocalDateTime.now(),
                request.getDescription(false),
                errors);

        // Assertion #2: Vérification de la réponse
        assert validationErrorResponse != null : "La réponse d'erreur de validation ne peut pas être null";
        assert validationErrorResponse.getStatus() == HttpStatus.BAD_REQUEST.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";
        assert !validationErrorResponse.getErrors().isEmpty() :
            "La liste des erreurs de validation ne peut pas être vide";

        return new ResponseEntity<>(validationErrorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Gère les exceptions liées aux services Hygie.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur avec des détails sur l'erreur de service
     */
    @ExceptionHandler(HygieServiceException.class)
    public ResponseEntity<ErrorResponse> handleHygieServiceException(
            HygieServiceException ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.error("Erreur de service Hygie: {}", ex.getMessage(), ex);

        final HttpStatus status = ex.getStatus() != null ? ex.getStatus() : HttpStatus.INTERNAL_SERVER_ERROR;

        final ErrorResponse errorResponse = createErrorResponse(
                status,
                ex.getMessage(),
                request.getDescription(false));

        // Assertion #2: Vérification de la réponse
        assert errorResponse != null : "La réponse d'erreur ne peut pas être null";
        assert errorResponse.getStatus() == status.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";

        return new ResponseEntity<>(errorResponse, status);
    }

    /**
     * Gère toutes les autres exceptions non spécifiquement traitées.
     *
     * @param ex L'exception levée
     * @param request La requête web
     * @return Une réponse d'erreur 500 avec des détails sur l'erreur
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex, WebRequest request) {
        // Assertion #1: Vérification que l'exception n'est pas null
        assert ex != null : "L'exception ne peut pas être null";

        // Log de l'erreur
        logger.error("Erreur non gérée: {}", ex.getMessage(), ex);

        final ErrorResponse errorResponse = createErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Une erreur inattendue s'est produite. Veuillez contacter le support.",
                request.getDescription(false));

        // Assertion #2: Vérification de la réponse
        assert errorResponse != null : "La réponse d'erreur ne peut pas être null";
        assert errorResponse.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Crée une réponse d'erreur standard.
     *
     * @param status Le statut HTTP
     * @param message Le message d'erreur
     * @param details Les détails de l'erreur
     * @return Une réponse d'erreur formatée
     */
    private ErrorResponse createErrorResponse(HttpStatus status, String message, String details) {
        // Assertion #1: Vérification des paramètres
        assert status != null : "Le statut HTTP ne peut pas être null";
        assert message != null && !message.isBlank() : "Le message d'erreur ne peut pas être null ou vide";

        final ErrorResponse errorResponse = new ErrorResponse(
                status.value(),
                message,
                LocalDateTime.now(),
                details);

        // Assertion #2: Vérification du résultat
        assert errorResponse != null : "La réponse d'erreur ne peut pas être null";
        assert errorResponse.getStatus() == status.value() :
            "Le statut HTTP de la réponse d'erreur est incorrect";

        return errorResponse;
    }
}

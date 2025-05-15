package com.hygie.patientservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception de base pour les erreurs de service dans le système Hygie-AI.
 *
 * Cette exception générique peut être utilisée pour toutes les erreurs
 * spécifiques aux services métier, avec un statut HTTP configurable.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class HygieServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final HttpStatus status;

    /**
     * Constructeur avec message et statut HTTP.
     *
     * @param message Le message d'erreur
     * @param status Le statut HTTP associé
     */
    public HygieServiceException(String message, HttpStatus status) {
        super(message);

        // Assertion #1: Vérification que le message n'est pas null ou vide
        assert message != null && !message.isBlank() :
            "Le message d'erreur ne peut pas être null ou vide";

        // Assertion #2: Vérification que le statut n'est pas null
        assert status != null : "Le statut HTTP ne peut pas être null";

        this.status = status;
    }

    /**
     * Constructeur avec message, cause et statut HTTP.
     *
     * @param message Le message d'erreur
     * @param cause La cause de l'exception
     * @param status Le statut HTTP associé
     */
    public HygieServiceException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);

        // Assertion #1: Vérification que le message n'est pas null ou vide
        assert message != null && !message.isBlank() :
            "Le message d'erreur ne peut pas être null ou vide";

        // Assertion #2: Vérification que la cause et le statut ne sont pas null
        assert cause != null : "La cause ne peut pas être null";
        assert status != null : "Le statut HTTP ne peut pas être null";

        this.status = status;
    }

    /**
     * Constructeur avec message seulement, utilisant par défaut le statut 500.
     *
     * @param message Le message d'erreur
     */
    public HygieServiceException(String message) {
        this(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Constructeur avec message et cause, utilisant par défaut le statut 500.
     *
     * @param message Le message d'erreur
     * @param cause La cause de l'exception
     */
    public HygieServiceException(String message, Throwable cause) {
        this(message, cause, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Récupère le statut HTTP associé à cette exception.
     *
     * @return Le statut HTTP
     */
    public HttpStatus getStatus() {
        return status;
    }
}

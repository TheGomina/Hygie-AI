package com.hygie.patientservice.exception;

import java.time.LocalDateTime;

/**
 * Classe de réponse standard pour les erreurs API dans le système Hygie-AI.
 *
 * Fournit une structure cohérente pour toutes les réponses d'erreur,
 * incluant le statut HTTP, un message, un horodatage et des détails.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class ErrorResponse {

    private final int status;
    private final String message;
    private final LocalDateTime timestamp;
    private final String details;

    /**
     * Constructeur pour une réponse d'erreur.
     *
     * @param status Le code de statut HTTP
     * @param message Le message d'erreur
     * @param timestamp L'horodatage de l'erreur
     * @param details Les détails supplémentaires
     */
    public ErrorResponse(int status, String message, LocalDateTime timestamp, String details) {
        // Assertion #1: Vérification que le message n'est pas null ou vide
        assert message != null && !message.isBlank() : "Le message d'erreur ne peut pas être null ou vide";

        // Assertion #2: Vérification que l'horodatage n'est pas null
        assert timestamp != null : "L'horodatage ne peut pas être null";

        this.status = status;
        this.message = message;
        this.timestamp = timestamp;
        this.details = details;
    }

    // Getters - Pas de setters pour garantir l'immutabilité

    public int getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}

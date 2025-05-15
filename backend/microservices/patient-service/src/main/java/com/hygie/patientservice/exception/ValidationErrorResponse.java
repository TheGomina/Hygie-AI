package com.hygie.patientservice.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Classe de réponse spécifique pour les erreurs de validation dans le système Hygie-AI.
 *
 * Étend la réponse d'erreur standard en ajoutant une carte des erreurs de validation,
 * permettant de retourner les détails précis de chaque violation de contrainte.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class ValidationErrorResponse extends ErrorResponse {

    private final Map<String, String> errors;

    /**
     * Constructeur pour une réponse d'erreur de validation.
     *
     * @param status Le code de statut HTTP
     * @param message Le message d'erreur
     * @param timestamp L'horodatage de l'erreur
     * @param details Les détails supplémentaires
     * @param errors Une carte des erreurs de validation (champ -> message)
     */
    public ValidationErrorResponse(int status, String message, LocalDateTime timestamp,
                                  String details, Map<String, String> errors) {
        super(status, message, timestamp, details);

        // Assertion #1: Vérification que la carte d'erreurs n'est pas null
        assert errors != null : "La carte des erreurs ne peut pas être null";

        // Assertion #2: Vérification que la carte d'erreurs n'est pas vide
        assert !errors.isEmpty() : "La carte des erreurs ne peut pas être vide";

        this.errors = errors;
    }

    /**
     * Récupère la carte des erreurs de validation.
     *
     * @return Une carte des erreurs de validation, où les clés sont les noms des champs
     *         et les valeurs sont les messages d'erreur
     */
    public Map<String, String> getErrors() {
        return errors;
    }
}

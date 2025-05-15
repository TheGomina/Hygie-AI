package com.hygie.patientservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception spécifique pour les cas où un patient n'est pas trouvé.
 *
 * Cette exception est levée lorsqu'une opération tente d'accéder à un
 * patient qui n'existe pas dans le système.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PatientNotFoundException extends HygieServiceException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructeur avec ID du patient.
     *
     * @param id L'identifiant du patient non trouvé
     */
    public PatientNotFoundException(String id) {
        super("Patient non trouvé avec l'ID: " + id, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec numéro de sécurité sociale.
     *
     * @param socialSecurityNumber Le numéro de sécurité sociale du patient non trouvé
     * @param isSsn Indicateur que le paramètre est un numéro de sécurité sociale
     */
    public PatientNotFoundException(String socialSecurityNumber, boolean isSsn) {
        super("Patient non trouvé avec le numéro de sécurité sociale: " + socialSecurityNumber,
              HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que le NSS n'est pas null ou vide
        assert socialSecurityNumber != null && !socialSecurityNumber.isBlank() :
            "Le numéro de sécurité sociale ne peut pas être null ou vide";

        // Assertion #2: Vérification du flag isSsn
        assert isSsn : "Le flag isSsn doit être true pour ce constructeur";
    }

    /**
     * Constructeur avec message personnalisé et cause.
     *
     * @param message Le message d'erreur personnalisé
     * @param cause La cause de l'exception
     */
    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que le message n'est pas null ou vide
        assert message != null && !message.isBlank() :
            "Le message d'erreur ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec critère de recherche personnalisé.
     *
     * @param searchValue La valeur recherchée
     * @param searchCriteria Le critère de recherche (ex: "nom", "prénom")
     */
    public PatientNotFoundException(String searchValue, String searchCriteria) {
        super("Patient non trouvé avec le " + searchCriteria + ": " + searchValue, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que la valeur n'est pas null ou vide
        assert searchValue != null && !searchValue.isBlank() :
            "La valeur de recherche ne peut pas être null ou vide";

        // Assertion #2: Vérification que le critère n'est pas null ou vide
        assert searchCriteria != null && !searchCriteria.isBlank() :
            "Le critère de recherche ne peut pas être null ou vide";
    }
}

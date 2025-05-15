package com.hygie.patientservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception spécifique pour les cas où une prescription n'est pas trouvée.
 *
 * Cette exception est levée lorsqu'une opération tente d'accéder à une
 * prescription qui n'existe pas dans le système.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PrescriptionNotFoundException extends HygieServiceException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructeur avec ID de la prescription.
     *
     * @param id L'identifiant de la prescription non trouvée
     */
    public PrescriptionNotFoundException(String id) {
        super("Prescription non trouvée avec l'ID: " + id, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec ID du patient et ID de prescription.
     *
     * @param patientId L'identifiant du patient
     * @param prescriptionId L'identifiant de la prescription non trouvée
     */
    public PrescriptionNotFoundException(String patientId, String prescriptionId) {
        super("Prescription non trouvée avec l'ID: " + prescriptionId +
              " pour le patient avec l'ID: " + patientId, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        // Assertion #2: Vérification que l'ID de la prescription n'est pas null ou vide
        assert prescriptionId != null && !prescriptionId.isBlank() :
            "L'ID de la prescription ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec message personnalisé et cause.
     *
     * @param message Le message d'erreur personnalisé
     * @param cause La cause de l'exception
     */
    public PrescriptionNotFoundException(String message, Throwable cause) {
        super(message, cause, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que le message n'est pas null ou vide
        assert message != null && !message.isBlank() :
            "Le message d'erreur ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec critère de recherche.
     *
     * @param searchValue La valeur recherchée
     * @param searchField Le champ sur lequel la recherche est effectuée
     */
    public PrescriptionNotFoundException(String searchValue, String searchField, boolean isSearchField) {
        super("Prescription non trouvée avec le " + searchField + ": " + searchValue, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que la valeur de recherche n'est pas null ou vide
        assert searchValue != null && !searchValue.isBlank() :
            "La valeur de recherche ne peut pas être null ou vide";

        // Assertion #2: Vérification que le champ de recherche n'est pas null ou vide
        assert searchField != null && !searchField.isBlank() :
            "Le champ de recherche ne peut pas être null ou vide";

        // Assertion #3: Vérification du flag de champ de recherche
        assert isSearchField : "Le flag isSearchField doit être true pour ce constructeur";
    }
}

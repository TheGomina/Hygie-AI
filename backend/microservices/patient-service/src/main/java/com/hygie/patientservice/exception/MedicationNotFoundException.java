package com.hygie.patientservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception spécifique pour les cas où un médicament n'est pas trouvé.
 *
 * Cette exception est levée lorsqu'une opération tente d'accéder à un
 * médicament qui n'existe pas dans le système.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class MedicationNotFoundException extends HygieServiceException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructeur avec ID du médicament.
     *
     * @param id L'identifiant du médicament non trouvé
     */
    public MedicationNotFoundException(String id) {
        super("Médicament non trouvé avec l'ID: " + id, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec code CIS.
     *
     * @param cisCode Le code CIS du médicament non trouvé
     * @param isCisCode Indicateur que le paramètre est un code CIS
     */
    public MedicationNotFoundException(String cisCode, boolean isCisCode) {
        super("Médicament non trouvé avec le code CIS: " + cisCode,
              HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que le code CIS n'est pas null ou vide
        assert cisCode != null && !cisCode.isBlank() :
            "Le code CIS ne peut pas être null ou vide";

        // Assertion #2: Vérification du flag isCisCode
        assert isCisCode : "Le flag isCisCode doit être true pour ce constructeur";
    }

    /**
     * Constructeur avec message personnalisé et cause.
     *
     * @param message Le message d'erreur personnalisé
     * @param cause La cause de l'exception
     */
    public MedicationNotFoundException(String message, Throwable cause) {
        super(message, cause, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que le message n'est pas null ou vide
        assert message != null && !message.isBlank() :
            "Le message d'erreur ne peut pas être null ou vide";
    }

    /**
     * Constructeur avec message personnalisé pour recherche par nom.
     *
     * @param name Le nom du médicament recherché
     * @param isSearchByName Flag indiquant une recherche par nom
     */
    public MedicationNotFoundException(String name, String searchType) {
        super("Médicament non trouvé avec le " + searchType + ": " + name, HttpStatus.NOT_FOUND);

        // Assertion #1: Vérification que le nom n'est pas null ou vide
        assert name != null && !name.isBlank() :
            "Le nom du médicament ne peut pas être null ou vide";

        // Assertion #2: Vérification que le type de recherche n'est pas null
        assert searchType != null && !searchType.isBlank() :
            "Le type de recherche ne peut pas être null ou vide";
    }
}

package com.hygie.patientservice.model;

import java.util.UUID;

/**
 * Classe Builder pour la création d'éléments de prescription immutables.
 *
 * Permet de construire des instances de PrescriptionItem de manière fluide
 * tout en respectant les contraintes d'immutabilité.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PrescriptionItemBuilder {

    private String id;
    private String medicationId;
    private String medicationName;
    private String dosage;
    private String route;
    private String frequency;
    private int durationDays;
    private String instructions;
    private boolean asNeeded;
    private int quantityPrescribed;
    private String unit;
    private boolean substitutionAllowed;
    private String reasonForPrescription;

    /**
     * Constructeur par défaut.
     */
    public PrescriptionItemBuilder() {
        this.id = UUID.randomUUID().toString();
        this.route = "oral"; // Valeur par défaut courante
        this.substitutionAllowed = true; // Valeur par défaut
    }

    /**
     * Constructeur avec un élément de prescription existant.
     *
     * @param item L'élément de prescription à partir duquel construire
     */
    public PrescriptionItemBuilder(PrescriptionItem item) {
        // Assertion #1: Vérification que l'item n'est pas nul
        assert item != null : "L'élément de prescription ne peut pas être nul";

        // Assertion #2: Vérification des données minimales
        assert item.getMedicationId() != null && !item.getMedicationId().isBlank() :
            "L'ID du médicament est obligatoire";

        this.id = item.getId();
        this.medicationId = item.getMedicationId();
        this.medicationName = item.getMedicationName();
        this.dosage = item.getDosage();
        this.route = item.getRoute();
        this.frequency = item.getFrequency();
        this.durationDays = item.getDurationDays();
        this.instructions = item.getInstructions();
        this.asNeeded = item.isAsNeeded();
        this.quantityPrescribed = item.getQuantityPrescribed();
        this.unit = item.getUnit();
        this.substitutionAllowed = item.isSubstitutionAllowed();
        this.reasonForPrescription = item.getReasonForPrescription();
    }

    /**
     * Définit l'ID.
     *
     * @param id L'ID de l'élément de prescription
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setId(String id) {
        // Assertion #1: Vérification que l'ID n'est pas vide
        assert id != null && !id.isBlank() : "L'ID ne peut pas être null ou vide";

        this.id = id;
        return this;
    }

    /**
     * Définit l'ID du médicament.
     *
     * @param medicationId L'ID du médicament
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setMedicationId(String medicationId) {
        // Assertion #1: Vérification que l'ID du médicament n'est pas vide
        assert medicationId != null && !medicationId.isBlank() :
            "L'ID du médicament ne peut pas être null ou vide";

        this.medicationId = medicationId;
        return this;
    }

    /**
     * Définit le nom du médicament.
     *
     * @param medicationName Le nom du médicament
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setMedicationName(String medicationName) {
        // Assertion #1: Vérification que le nom du médicament n'est pas vide
        assert medicationName != null && !medicationName.isBlank() :
            "Le nom du médicament ne peut pas être null ou vide";

        this.medicationName = medicationName;
        return this;
    }

    /**
     * Définit la posologie.
     *
     * @param dosage La posologie (ex: "500 mg")
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setDosage(String dosage) {
        // Assertion #1: Vérification que la posologie n'est pas vide
        assert dosage != null && !dosage.isBlank() :
            "La posologie ne peut pas être null ou vide";

        this.dosage = dosage;
        return this;
    }

    /**
     * Définit la voie d'administration.
     *
     * @param route La voie d'administration
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setRoute(String route) {
        this.route = route;
        return this;
    }

    /**
     * Définit la fréquence d'administration.
     *
     * @param frequency La fréquence d'administration (ex: "3 fois par jour")
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setFrequency(String frequency) {
        // Assertion #1: Vérification que la fréquence n'est pas vide
        assert frequency != null && !frequency.isBlank() :
            "La fréquence d'administration ne peut pas être null ou vide";

        this.frequency = frequency;
        return this;
    }

    /**
     * Définit la durée du traitement en jours.
     *
     * @param durationDays La durée du traitement en jours
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setDuration(int durationDays) {
        // Assertion #1: Vérification que la durée n'est pas négative
        assert durationDays >= 0 : "La durée du traitement ne peut pas être négative";

        this.durationDays = durationDays;
        return this;
    }

    /**
     * Définit les instructions spéciales pour la prise.
     *
     * @param instructions Les instructions spéciales
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setInstructions(String instructions) {
        this.instructions = instructions;
        return this;
    }

    /**
     * Définit si le médicament doit être pris selon les besoins.
     *
     * @param asNeeded Indique si le médicament est à prendre selon les besoins
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setAsNeeded(boolean asNeeded) {
        this.asNeeded = asNeeded;
        return this;
    }

    /**
     * Définit la quantité prescrite.
     *
     * @param quantityPrescribed La quantité prescrite
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setQuantityPrescribed(int quantityPrescribed) {
        // Assertion #1: Vérification que la quantité n'est pas négative
        assert quantityPrescribed >= 0 : "La quantité prescrite ne peut pas être négative";

        this.quantityPrescribed = quantityPrescribed;
        return this;
    }

    /**
     * Définit l'unité de la quantité.
     *
     * @param unit L'unité (ex: "comprimés")
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setUnit(String unit) {
        this.unit = unit;
        return this;
    }

    /**
     * Définit si la substitution par un générique est autorisée.
     *
     * @param substitutionAllowed Indique si la substitution est autorisée
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setSubstitutionAllowed(boolean substitutionAllowed) {
        this.substitutionAllowed = substitutionAllowed;
        return this;
    }

    /**
     * Définit la raison de la prescription.
     *
     * @param reasonForPrescription La raison médicale de la prescription
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setReasonForPrescription(String reasonForPrescription) {
        this.reasonForPrescription = reasonForPrescription;
        return this;
    }

    /**
     * Définit les instructions spéciales.
     *
     * @param specialInstructions Les instructions spéciales
     * @return Une référence à ce builder
     */
    public PrescriptionItemBuilder setSpecialInstructions(String specialInstructions) {
        // Alias pour setInstructions pour compatibilité avec le code existant
        return setInstructions(specialInstructions);
    }

    /**
     * Construit une instance de PrescriptionItem.
     *
     * @return Une nouvelle instance de PrescriptionItem
     */
    public PrescriptionItem build() {
        // Assertion #1: Vérification des champs obligatoires
        assert medicationId != null && !medicationId.isBlank() :
            "L'ID du médicament est obligatoire";
        assert medicationName != null && !medicationName.isBlank() :
            "Le nom du médicament est obligatoire";
        assert dosage != null && !dosage.isBlank() :
            "La posologie est obligatoire";
        assert frequency != null && !frequency.isBlank() :
            "La fréquence d'administration est obligatoire";

        // On utilise le constructeur de PrescriptionItem
        PrescriptionItem item = new PrescriptionItem(
            medicationId,
            medicationName,
            dosage,
            route,
            frequency,
            durationDays,
            instructions,
            asNeeded,
            quantityPrescribed,
            unit,
            substitutionAllowed
        );

        // On utilise la réflexion pour setter les autres champs non-accessibles par le constructeur
        try {
            java.lang.reflect.Field idField = PrescriptionItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item, id);

            if (reasonForPrescription != null) {
                java.lang.reflect.Field reasonField = PrescriptionItem.class.getDeclaredField("reasonForPrescription");
                reasonField.setAccessible(true);
                reasonField.set(item, reasonForPrescription);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la construction de l'item de prescription: " + e.getMessage(), e);
        }

        return item;
    }
}

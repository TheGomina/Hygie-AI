package com.hygie.patientservice.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe Builder pour la création de prescriptions immutables.
 *
 * Permet de construire des instances de Prescription de manière fluide
 * tout en respectant les contraintes d'immutabilité.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PrescriptionBuilder {

    private String id;
    private String patientId;
    private String prescriberId;
    private String prescriberSpecialty;
    private LocalDate prescriptionDate;
    private LocalDate expirationDate;
    private List<PrescriptionItem> prescriptionItems;
    private boolean isRenewal;
    private int renewalNumber;
    private int validityPeriodMonths;
    private String pharmacyId;
    private LocalDate dispensingDate;
    private Prescription.PrescriptionStatus status;
    private List<String> notes;

    /**
     * Constructeur par défaut.
     */
    public PrescriptionBuilder() {
        this.id = UUID.randomUUID().toString();
        this.prescriptionItems = new ArrayList<>();
        this.notes = new ArrayList<>();
        this.validityPeriodMonths = 3; // Valeur par défaut
        this.prescriptionDate = LocalDate.now();
        this.status = Prescription.PrescriptionStatus.ACTIVE;
    }

    /**
     * Constructeur avec une prescription existante.
     *
     * @param prescription La prescription à partir de laquelle construire
     */
    public PrescriptionBuilder(Prescription prescription) {
        // Assertion #1: Vérification que la prescription n'est pas nulle
        assert prescription != null : "La prescription ne peut pas être nulle";

        // Assertion #2: Vérification des données minimales
        assert prescription.getPatientId() != null && !prescription.getPatientId().isBlank() :
            "L'ID du patient est obligatoire";

        this.id = prescription.getId();
        this.patientId = prescription.getPatientId();
        this.prescriberId = prescription.getPrescriberId();
        this.prescriberSpecialty = prescription.getPrescriberSpecialty();
        this.prescriptionDate = prescription.getPrescriptionDate();
        this.expirationDate = prescription.getExpirationDate();
        this.prescriptionItems = new ArrayList<>(prescription.getPrescriptionItems());
        this.isRenewal = prescription.isRenewal();
        this.renewalNumber = prescription.getRenewalNumber();
        this.validityPeriodMonths = prescription.getValidityPeriodMonths();
        this.pharmacyId = prescription.getPharmacyId();
        this.dispensingDate = prescription.getDispensingDate();
        this.status = prescription.getStatus();
        this.notes = new ArrayList<>(prescription.getNotes());
    }

    /**
     * Définit l'ID.
     *
     * @param id L'ID de la prescription
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setId(String id) {
        // Assertion #1: Vérification que l'ID n'est pas vide
        assert id != null && !id.isBlank() : "L'ID ne peut pas être null ou vide";

        this.id = id;
        return this;
    }

    /**
     * Définit l'ID du patient.
     *
     * @param patientId L'ID du patient
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setPatientId(String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        this.patientId = patientId;
        return this;
    }

    /**
     * Définit l'ID du prescripteur.
     *
     * @param prescriberId L'ID du prescripteur
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setPrescriberId(String prescriberId) {
        // Assertion #1: Vérification que l'ID du prescripteur n'est pas vide
        assert prescriberId != null && !prescriberId.isBlank() :
            "L'ID du prescripteur ne peut pas être null ou vide";

        this.prescriberId = prescriberId;
        return this;
    }

    /**
     * Définit la spécialité du prescripteur.
     *
     * @param prescriberSpecialty La spécialité du prescripteur
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setPrescriberSpecialty(String prescriberSpecialty) {
        this.prescriberSpecialty = prescriberSpecialty;
        return this;
    }

    /**
     * Définit la date de prescription.
     *
     * @param prescriptionDate La date de prescription
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setPrescriptionDate(LocalDate prescriptionDate) {
        // Assertion #1: Vérification que la date n'est pas nulle et pas dans le futur
        assert prescriptionDate != null && !prescriptionDate.isAfter(LocalDate.now()) :
            "La date de prescription doit être valide et ne peut pas être dans le futur";

        this.prescriptionDate = prescriptionDate;
        return this;
    }

    /**
     * Définit la date d'expiration.
     *
     * @param expirationDate La date d'expiration
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
        return this;
    }

    /**
     * Définit les items de prescription.
     *
     * @param items La liste des items de prescription
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setItems(List<PrescriptionItem> items) {
        // Assertion #1: Vérification que la liste n'est pas nulle
        assert items != null : "La liste des items ne peut pas être nulle";

        this.prescriptionItems = new ArrayList<>(items);
        return this;
    }

    /**
     * Ajoute un item à la prescription.
     *
     * @param item L'item à ajouter
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder addItem(PrescriptionItem item) {
        // Assertion #1: Vérification que l'item n'est pas nul
        assert item != null : "L'item ne peut pas être nul";

        this.prescriptionItems.add(item);
        return this;
    }

    /**
     * Définit si c'est un renouvellement.
     *
     * @param isRenewal Indique si c'est un renouvellement
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setRenewal(boolean isRenewal) {
        this.isRenewal = isRenewal;
        return this;
    }

    /**
     * Définit le numéro de renouvellement.
     *
     * @param renewalNumber Le numéro de renouvellement
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setRenewalNumber(int renewalNumber) {
        // Assertion #1: Vérification que le numéro n'est pas négatif
        assert renewalNumber >= 0 : "Le numéro de renouvellement ne peut pas être négatif";

        this.renewalNumber = renewalNumber;
        return this;
    }

    /**
     * Définit la période de validité en mois.
     *
     * @param validityPeriodMonths La période de validité en mois
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setValidityPeriodMonths(int validityPeriodMonths) {
        // Assertion #1: Vérification que la période est dans une plage raisonnable
        assert validityPeriodMonths > 0 && validityPeriodMonths <= 12 :
            "La période de validité doit être entre 1 et 12 mois";

        this.validityPeriodMonths = validityPeriodMonths;
        return this;
    }

    /**
     * Définit l'ID de la pharmacie.
     *
     * @param pharmacyId L'ID de la pharmacie
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
        return this;
    }

    /**
     * Définit la date de délivrance.
     *
     * @param dispensingDate La date de délivrance
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setDispensingDate(LocalDate dispensingDate) {
        this.dispensingDate = dispensingDate;
        return this;
    }

    /**
     * Définit le statut.
     *
     * @param status Le statut de la prescription
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setStatus(Prescription.PrescriptionStatus status) {
        // Assertion #1: Vérification que le statut n'est pas nul
        assert status != null : "Le statut ne peut pas être nul";

        this.status = status;
        return this;
    }

    /**
     * Définit les notes.
     *
     * @param notes La liste des notes
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder setNotes(List<String> notes) {
        // Assertion #1: Vérification que la liste n'est pas nulle
        assert notes != null : "La liste des notes ne peut pas être nulle";

        this.notes = new ArrayList<>(notes);
        return this;
    }

    /**
     * Ajoute une note.
     *
     * @param note La note à ajouter
     * @return Une référence à ce builder
     */
    public PrescriptionBuilder addNote(String note) {
        // Assertion #1: Vérification que la note n'est pas vide
        assert note != null && !note.isBlank() : "La note ne peut pas être null ou vide";

        this.notes.add(note);
        return this;
    }

    /**
     * Construit une instance de Prescription.
     *
     * @return Une nouvelle instance de Prescription
     */
    public Prescription build() {
        // Assertion #1: Vérification des champs obligatoires
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient est obligatoire";
        assert prescriberId != null && !prescriberId.isBlank() :
            "L'ID du prescripteur est obligatoire";
        assert prescriptionDate != null :
            "La date de prescription est obligatoire";

        // Assertion #2: Vérification que la liste des items n'est pas vide
        assert !prescriptionItems.isEmpty() :
            "La prescription doit contenir au moins un item";

        // On utilise le constructeur le plus complet de Prescription disponible
        Prescription prescription = new Prescription(
            patientId,
            prescriberId,
            prescriberSpecialty,
            prescriptionDate,
            validityPeriodMonths,
            isRenewal,
            renewalNumber
        );

        // On utilise la réflexion pour setter les autres champs non-accessibles par le constructeur
        // Note: Cette approche est nécessaire uniquement si la réflexion est disponible et autorisée
        // dans le contexte du projet. Sinon, le pattern Builder complet nécessiterait une refonte
        // plus profonde de la classe Prescription.
        try {
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(prescription, id);

            java.lang.reflect.Field expirationDateField = Prescription.class.getDeclaredField("expirationDate");
            expirationDateField.setAccessible(true);
            expirationDateField.set(prescription, expirationDate);

            java.lang.reflect.Field prescriptionItemsField = Prescription.class.getDeclaredField("prescriptionItems");
            prescriptionItemsField.setAccessible(true);
            prescriptionItemsField.set(prescription, prescriptionItems);

            java.lang.reflect.Field pharmacyIdField = Prescription.class.getDeclaredField("pharmacyId");
            pharmacyIdField.setAccessible(true);
            pharmacyIdField.set(prescription, pharmacyId);

            java.lang.reflect.Field dispensingDateField = Prescription.class.getDeclaredField("dispensingDate");
            dispensingDateField.setAccessible(true);
            dispensingDateField.set(prescription, dispensingDate);

            java.lang.reflect.Field statusField = Prescription.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(prescription, status);

            java.lang.reflect.Field notesField = Prescription.class.getDeclaredField("notes");
            notesField.setAccessible(true);
            notesField.set(prescription, notes);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la construction de la prescription: " + e.getMessage(), e);
        }

        return prescription;
    }
}

package com.hygie.patientservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Modèle représentant une prescription médicamenteuse dans le système Hygie-AI.
 *
 * Implémente le modèle FHIR MedicationRequest avec les extensions nécessaires
 * pour le contexte pharmaceutique du Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Document(collection = "prescriptions")
public class Prescription {

    @Id
    private final String id;

    @NotBlank(message = "L'identifiant du patient est obligatoire")
    private final String patientId;

    @NotBlank(message = "L'identifiant du prescripteur est obligatoire")
    private final String prescriberId;

    private final String prescriberSpecialty;

    @NotNull(message = "La date de prescription est obligatoire")
    @PastOrPresent(message = "La date de prescription ne peut pas être dans le futur")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate prescriptionDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate expirationDate;

    @NotNull(message = "La liste des médicaments prescrits est obligatoire")
    @Size(min = 1, message = "Une prescription doit contenir au moins un médicament")
    private final List<PrescriptionItem> prescriptionItems;

    private final boolean isRenewal;

    private final int renewalNumber;

    private final int validityPeriodMonths;

    private final String pharmacyId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate dispensingDate;

    private PrescriptionStatus status;

    private final List<String> notes;

    /**
     * Énumération des statuts possibles d'une prescription.
     */
    public enum PrescriptionStatus {
        ACTIVE,
        COMPLETED,
        CANCELLED,
        EXPIRED,
        PARTIALLY_DISPENSED
    }

    /**
     * Constructeur pour une prescription.
     *
     * @param patientId L'identifiant du patient
     * @param prescriberId L'identifiant du prescripteur
     * @param prescriberSpecialty La spécialité du prescripteur
     * @param prescriptionDate La date de prescription
     * @param validityPeriodMonths La période de validité en mois
     * @param isRenewal Indique s'il s'agit d'un renouvellement
     * @param renewalNumber Le numéro du renouvellement
     */
    public Prescription(String patientId, String prescriberId, String prescriberSpecialty,
                       LocalDate prescriptionDate, int validityPeriodMonths,
                       boolean isRenewal, int renewalNumber) {

        // Assertion #1: Vérification des paramètres obligatoires
        assert patientId != null && !patientId.isBlank() :
            "L'identifiant du patient ne peut pas être null ou vide";
        assert prescriberId != null && !prescriberId.isBlank() :
            "L'identifiant du prescripteur ne peut pas être null ou vide";
        assert prescriptionDate != null && !prescriptionDate.isAfter(LocalDate.now()) :
            "La date de prescription doit être valide et ne peut pas être dans le futur";

        // Assertion #2: Vérification des valeurs numériques
        assert validityPeriodMonths > 0 && validityPeriodMonths <= 12 :
            "La période de validité doit être entre 1 et 12 mois";
        assert renewalNumber >= 0 : "Le numéro de renouvellement ne peut pas être négatif";

        this.id = UUID.randomUUID().toString();
        this.patientId = patientId;
        this.prescriberId = prescriberId;
        this.prescriberSpecialty = prescriberSpecialty;
        this.prescriptionDate = prescriptionDate;
        this.validityPeriodMonths = validityPeriodMonths;
        this.expirationDate = prescriptionDate.plusMonths(validityPeriodMonths);
        this.isRenewal = isRenewal;
        this.renewalNumber = renewalNumber;
        this.prescriptionItems = new ArrayList<>();
        this.status = PrescriptionStatus.ACTIVE;
        this.notes = new ArrayList<>();
        this.pharmacyId = null;
        this.dispensingDate = null;
    }

    /**
     * Ajoute un item à la prescription.
     *
     * @param item L'item de prescription à ajouter
     */
    public void addPrescriptionItem(PrescriptionItem item) {
        // Assertion #1: Vérification que l'item n'est pas null
        assert item != null : "L'item de prescription ne peut pas être null";

        // Assertion #2: Vérification que l'item n'est pas déjà présent
        assert !prescriptionItems.contains(item) : "Cet item est déjà présent dans la prescription";

        prescriptionItems.add(item);
    }

    /**
     * Vérifie si la prescription est expirée.
     *
     * @return true si la prescription est expirée
     */
    public boolean isExpired() {
        // Assertion #1: Vérification que la date d'expiration est définie
        assert expirationDate != null : "La date d'expiration n'est pas définie";

        // Assertion #2: Vérification de cohérence avec la date de prescription
        assert !expirationDate.isBefore(prescriptionDate) :
            "La date d'expiration ne peut pas être antérieure à la date de prescription";

        return LocalDate.now().isAfter(expirationDate);
    }

    /**
     * Calcule le nombre de jours restants avant l'expiration de la prescription.
     *
     * @return Le nombre de jours restants, ou 0 si la prescription est expirée
     */
    public int getDaysUntilExpiration() {
        // Assertion #1: Vérification que la date d'expiration est définie
        assert expirationDate != null : "La date d'expiration n'est pas définie";

        // Assertion #2: Vérification de la cohérence temporelle
        assert !expirationDate.isBefore(LocalDate.now().minusYears(1)) :
            "La date d'expiration est trop ancienne";

        if (isExpired()) {
            return 0;
        }

        return Period.between(LocalDate.now(), expirationDate).getDays();
    }

    /**
     * Calcule la durée totale de traitement prévue par la prescription.
     * Basée sur l'item ayant la plus longue durée.
     *
     * @return La durée totale en jours
     */
    public int getTotalTreatmentDuration() {
        // Assertion #1: Vérification que la liste d'items n'est pas vide
        assert prescriptionItems != null && !prescriptionItems.isEmpty() :
            "La prescription doit contenir au moins un item";

        // Assertion #2: Vérification de la validité des durées
        for (PrescriptionItem item : prescriptionItems) {
            assert item.getDurationDays() >= 0 :
                "La durée du traitement ne peut pas être négative";
        }

        return prescriptionItems.stream()
                .mapToInt(PrescriptionItem::getDurationDays)
                .max()
                .orElse(0);
    }

    /**
     * Met à jour le statut de la prescription.
     *
     * @param newStatus Le nouveau statut
     */
    public void updateStatus(PrescriptionStatus newStatus) {
        // Assertion #1: Vérification que le statut n'est pas null
        assert newStatus != null : "Le nouveau statut ne peut pas être null";

        // Assertion #2: Vérification de la cohérence du changement de statut
        assert isValidStatusTransition(this.status, newStatus) :
            "Transition de statut invalide: " + this.status + " -> " + newStatus;

        this.status = newStatus;
    }

    /**
     * Vérifie si une transition de statut est valide.
     *
     * @param currentStatus Le statut actuel
     * @param newStatus Le nouveau statut
     * @return true si la transition est valide
     */
    private boolean isValidStatusTransition(PrescriptionStatus currentStatus, PrescriptionStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;
        }

        switch (currentStatus) {
            case ACTIVE:
                return newStatus == PrescriptionStatus.COMPLETED ||
                       newStatus == PrescriptionStatus.CANCELLED ||
                       newStatus == PrescriptionStatus.EXPIRED ||
                       newStatus == PrescriptionStatus.PARTIALLY_DISPENSED;
            case PARTIALLY_DISPENSED:
                return newStatus == PrescriptionStatus.COMPLETED ||
                       newStatus == PrescriptionStatus.EXPIRED;
            case COMPLETED:
            case CANCELLED:
            case EXPIRED:
                return false; // États terminaux
            default:
                return false;
        }
    }

    /**
     * Ajoute une note à la prescription.
     *
     * @param note La note à ajouter
     */
    public void addNote(String note) {
        // Assertion #1: Vérification que la note n'est pas vide
        assert note != null && !note.isBlank() : "La note ne peut pas être null ou vide";

        // Assertion #2: Vérification que la liste des notes est initialisée
        assert notes != null : "La liste des notes n'est pas initialisée";

        notes.add(note);
    }

    /**
     * Vérifie si la prescription contient un médicament spécifique.
     *
     * @param medicationId L'identifiant du médicament à rechercher
     * @return true si le médicament est présent dans la prescription
     */
    public boolean containsMedication(String medicationId) {
        // Assertion #1: Vérification que l'ID du médicament n'est pas vide
        assert medicationId != null && !medicationId.isBlank() :
            "L'identifiant du médicament ne peut pas être null ou vide";

        // Assertion #2: Vérification que la liste d'items est initialisée
        assert prescriptionItems != null : "La liste des items n'est pas initialisée";

        return prescriptionItems.stream()
                .anyMatch(item -> medicationId.equals(item.getMedicationId()));
    }

    /**
     * Génère un résumé formaté de la prescription.
     *
     * @return Un résumé de la prescription
     */
    public String generateSummary() {
        // Assertion #1: Vérification des données minimales
        assert prescriptionDate != null && prescriptionItems != null :
            "Les données minimales sont requises pour générer un résumé";

        // Assertion #2: Vérification de la validité des items
        assert !prescriptionItems.isEmpty() : "La prescription doit contenir au moins un item";

        final StringBuilder summary = new StringBuilder(100);
        summary.append("Prescription du ").append(prescriptionDate);

        if (isRenewal) {
            summary.append(" (renouvellement #").append(renewalNumber).append(")");
        }

        summary.append("\nStatut: ").append(status);

        if (expirationDate != null) {
            if (isExpired()) {
                summary.append(" - EXPIRÉE depuis le ").append(expirationDate);
            } else {
                summary.append(" - valable jusqu'au ").append(expirationDate);
            }
        }

        summary.append("\nMédicaments (").append(prescriptionItems.size()).append("):");

        for (PrescriptionItem item : prescriptionItems) {
            summary.append("\n- ").append(item.generateSummary());
        }

        return summary.toString();
    }

    // Getters - Pas de setters pour garantir l'immutabilité maximale

    public String getId() {
        return id;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getPrescriberId() {
        return prescriberId;
    }

    public String getPrescriberSpecialty() {
        return prescriberSpecialty;
    }

    public LocalDate getPrescriptionDate() {
        return prescriptionDate;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public List<PrescriptionItem> getPrescriptionItems() {
        return Collections.unmodifiableList(prescriptionItems);
    }

    public boolean isRenewal() {
        return isRenewal;
    }

    public int getRenewalNumber() {
        return renewalNumber;
    }

    public int getValidityPeriodMonths() {
        return validityPeriodMonths;
    }

    public String getPharmacyId() {
        return pharmacyId;
    }

    public LocalDate getDispensingDate() {
        return dispensingDate;
    }

    public PrescriptionStatus getStatus() {
        return status;
    }

    public List<String> getNotes() {
        return Collections.unmodifiableList(notes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prescription that = (Prescription) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

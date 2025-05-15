package com.hygie.patientservice.model;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Objects;
import java.util.UUID;

/**
 * Modèle représentant un élément de prescription médicamenteuse dans le système Hygie-AI.
 *
 * Cette classe définit les détails de prescription pour un médicament spécifique:
 * posologie, durée, instructions, etc.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PrescriptionItem {

    private final String id;

    @NotBlank(message = "L'identifiant du médicament est obligatoire")
    private final String medicationId;

    @NotBlank(message = "Le nom du médicament est obligatoire")
    private final String medicationName;

    @NotBlank(message = "La posologie est obligatoire")
    private final String dosage;

    @Pattern(regexp = "^(oral|parentéral|cutané|oculaire|inhalation|rectal|vaginal|sublingual|auriculaire|nasal|autre)$",
             message = "La voie d'administration doit être une valeur valide")
    private final String route;

    @NotNull(message = "La fréquence d'administration est obligatoire")
    private final String frequency;

    @Min(value = 0, message = "La durée du traitement ne peut pas être négative")
    private final int durationDays;

    private final String instructions;

    private final boolean asNeeded;

    @Min(value = 0, message = "La quantité prescrite ne peut pas être négative")
    private final int quantityPrescribed;

    private final String unit;

    private final boolean substitutionAllowed;

    private String reasonForPrescription;

    /**
     * Constructeur complet pour un élément de prescription.
     *
     * @param medicationId L'identifiant du médicament
     * @param medicationName Le nom du médicament
     * @param dosage La posologie (ex: "500 mg")
     * @param route La voie d'administration
     * @param frequency La fréquence d'administration (ex: "3 fois par jour")
     * @param durationDays La durée du traitement en jours
     * @param instructions Instructions spéciales pour la prise
     * @param asNeeded Si le médicament doit être pris selon les besoins
     * @param quantityPrescribed La quantité prescrite
     * @param unit L'unité de la quantité (ex: "comprimés")
     * @param substitutionAllowed Si la substitution par un générique est autorisée
     */
    public PrescriptionItem(String medicationId, String medicationName, String dosage,
                           String route, String frequency, int durationDays,
                           String instructions, boolean asNeeded, int quantityPrescribed,
                           String unit, boolean substitutionAllowed) {

        // Assertion #1: Vérification des paramètres obligatoires
        assert medicationId != null && !medicationId.isBlank() :
            "L'identifiant du médicament ne peut pas être null ou vide";
        assert medicationName != null && !medicationName.isBlank() :
            "Le nom du médicament ne peut pas être null ou vide";
        assert dosage != null && !dosage.isBlank() :
            "La posologie ne peut pas être null ou vide";
        assert frequency != null && !frequency.isBlank() :
            "La fréquence d'administration ne peut pas être null ou vide";

        // Assertion #2: Vérification des valeurs numériques et des contraintes
        assert durationDays >= 0 : "La durée du traitement ne peut pas être négative";
        assert quantityPrescribed >= 0 : "La quantité prescrite ne peut pas être négative";

        this.id = UUID.randomUUID().toString();
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.dosage = dosage;
        this.route = route;
        this.frequency = frequency;
        this.durationDays = durationDays;
        this.instructions = instructions;
        this.asNeeded = asNeeded;
        this.quantityPrescribed = quantityPrescribed;
        this.unit = unit;
        this.substitutionAllowed = substitutionAllowed;
    }

    /**
     * Constructeur simplifié pour un élément de prescription.
     *
     * @param medicationId L'identifiant du médicament
     * @param medicationName Le nom du médicament
     * @param dosage La posologie
     * @param frequency La fréquence d'administration
     * @param durationDays La durée du traitement en jours
     */
    public PrescriptionItem(String medicationId, String medicationName,
                          String dosage, String frequency, int durationDays) {
        this(medicationId, medicationName, dosage, null, frequency, durationDays,
             null, false, 0, null, true);
    }

    /**
     * Calcule la dose journalière en fonction de la posologie et de la fréquence.
     *
     * @return La dose journalière estimée, ou -1 si elle ne peut pas être calculée
     */
    public double calculateDailyDose() {
        // Assertion #1: Vérification des données nécessaires
        assert dosage != null && frequency != null :
            "La posologie et la fréquence sont requises pour calculer la dose journalière";

        // Assertion #2: Vérification du format attendu pour la posologie
        assert dosage.matches(".*\\d+.*") :
            "La posologie doit contenir au moins un nombre";

        try {
            // Extraction de la valeur numérique de la posologie (ex: "500 mg" -> 500)
            String numericPart = dosage.replaceAll("[^0-9.]", "");
            double doseValue = Double.parseDouble(numericPart);

            // Calcul du nombre de prises par jour
            double dosesPerDay = extractDosesPerDay(frequency);

            return doseValue * dosesPerDay;
        } catch (Exception e) {
            return -1; // Impossible de calculer
        }
    }

    /**
     * Extrait le nombre de prises par jour à partir de la fréquence.
     *
     * @param frequency La fréquence d'administration
     * @return Le nombre de prises par jour estimé
     */
    private double extractDosesPerDay(String frequency) {
        // La fréquence est normalisée en minuscules pour faciliter l'analyse
        String normalizedFreq = frequency.toLowerCase();

        // Traitement des cas courants
        if (normalizedFreq.contains("fois par jour") || normalizedFreq.contains("x/jour") ||
            normalizedFreq.contains("prises par jour")) {
            // Ex: "3 fois par jour" -> 3
            String numericPart = normalizedFreq.replaceAll("[^0-9]", "");
            return numericPart.isEmpty() ? 1 : Double.parseDouble(numericPart);
        } else if (normalizedFreq.contains("toutes les") && normalizedFreq.contains("heures")) {
            // Ex: "toutes les 8 heures" -> 24/8 = 3
            String numericPart = normalizedFreq.replaceAll("[^0-9]", "");
            double hours = Double.parseDouble(numericPart);
            return 24.0 / hours;
        } else if (normalizedFreq.contains("matin") && normalizedFreq.contains("soir")) {
            // "matin et soir" -> 2
            return 2.0;
        } else if (normalizedFreq.contains("jour") || normalizedFreq.contains("quotidien")) {
            // "une fois par jour" ou "quotidien" -> 1
            return 1.0;
        } else if (normalizedFreq.contains("semaine")) {
            // "une fois par semaine" -> 1/7
            String numericPart = normalizedFreq.replaceAll("[^0-9]", "");
            double timesPerWeek = numericPart.isEmpty() ? 1 : Double.parseDouble(numericPart);
            return timesPerWeek / 7.0;
        }

        // Valeur par défaut
        return 1.0;
    }

    /**
     * Vérifie si la prescription indique une prise avant les repas.
     *
     * @return true si la prise est indiquée avant les repas
     */
    public boolean isBeforeMeals() {
        // Assertion #1: Vérification des instructions
        if (instructions == null) {
            return false;
        }

        // Assertion #2: Vérification de la présence des termes spécifiques
        assert instructions != null : "Les instructions ne peuvent pas être null";

        String lowerInstructions = instructions.toLowerCase();
        return lowerInstructions.contains("avant") &&
               (lowerInstructions.contains("repas") || lowerInstructions.contains("manger"));
    }

    /**
     * Vérifie si la prescription indique une prise après les repas.
     *
     * @return true si la prise est indiquée après les repas
     */
    public boolean isAfterMeals() {
        // Assertion #1: Vérification des instructions
        if (instructions == null) {
            return false;
        }

        // Assertion #2: Vérification de la présence des termes spécifiques
        assert instructions != null : "Les instructions ne peuvent pas être null";

        String lowerInstructions = instructions.toLowerCase();
        return lowerInstructions.contains("après") &&
               (lowerInstructions.contains("repas") || lowerInstructions.contains("manger"));
    }

    /**
     * Définit la raison de la prescription.
     *
     * @param reason La raison médicale de la prescription
     */
    public void setReasonForPrescription(String reason) {
        // Assertion #1: Vérification que la raison n'est pas vide
        assert reason != null && !reason.isBlank() :
            "La raison de prescription ne peut pas être null ou vide";

        // Assertion #2: Vérification de la longueur minimale
        assert reason.length() >= 3 :
            "La raison de prescription doit comporter au moins 3 caractères";

        this.reasonForPrescription = reason;
    }

    /**
     * Génère une représentation textuelle de la prescription d'un médicament.
     *
     * @return Un résumé formaté de l'élément de prescription
     */
    public String generateSummary() {
        // Assertion #1: Vérification des données obligatoires
        assert medicationName != null && dosage != null && frequency != null :
            "Les données de base sont requises pour générer un résumé";

        // Assertion #2: Vérification de la cohérence de la durée
        if (durationDays > 0) {
            assert durationDays <= 365 * 2 :
                "La durée du traitement semble excessive (> 2 ans)";
        }

        StringBuilder summary = new StringBuilder(100);
        summary.append(medicationName).append(" ").append(dosage)
               .append(", ").append(frequency);

        if (durationDays > 0) {
            summary.append(", pendant ").append(durationDays).append(" jours");
        }

        if (route != null && !route.isBlank()) {
            summary.append(", voie ").append(route);
        }

        if (asNeeded) {
            summary.append(" (si besoin)");
        }

        if (quantityPrescribed > 0 && unit != null) {
            summary.append(", ").append(quantityPrescribed).append(" ").append(unit);
        }

        if (instructions != null && !instructions.isBlank()) {
            summary.append(" - ").append(instructions);
        }

        if (!substitutionAllowed) {
            summary.append(" [Non substituable]");
        }

        return summary.toString();
    }

    // Getters - Pas de setters pour garantir l'immutabilité maximale

    public String getId() {
        return id;
    }

    public String getMedicationId() {
        return medicationId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public String getDosage() {
        return dosage;
    }

    public String getRoute() {
        return route;
    }

    public String getFrequency() {
        return frequency;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public String getInstructions() {
        return instructions;
    }

    public boolean isAsNeeded() {
        return asNeeded;
    }

    public int getQuantityPrescribed() {
        return quantityPrescribed;
    }

    public String getUnit() {
        return unit;
    }

    public boolean isSubstitutionAllowed() {
        return substitutionAllowed;
    }

    public String getReasonForPrescription() {
        return reasonForPrescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrescriptionItem that = (PrescriptionItem) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

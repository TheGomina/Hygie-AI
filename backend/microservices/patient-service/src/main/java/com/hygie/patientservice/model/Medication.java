package com.hygie.patientservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Modèle représentant un médicament dans le système Hygie-AI.
 *
 * Implémente le modèle FHIR Medication avec les extensions nécessaires
 * pour le contexte pharmaceutique.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Document(collection = "medications")
public class Medication {

    @Id
    private final String id;

    @NotBlank(message = "Le code CIS est obligatoire")
    @Indexed(unique = true)
    @Pattern(regexp = "^\\d{8}$", message = "Le code CIS doit être composé de 8 chiffres")
    private final String cisCode;

    @NotBlank(message = "Le nom du médicament est obligatoire")
    private final String name;

    @NotBlank(message = "La DCI est obligatoire")
    private final String activeSubstance;

    private final String atcCode;

    private final String pharmaceuticalForm;

    private final String strength;

    private final String route;

    private final boolean prescriptionRequired;

    private final boolean reimbursed;

    private final float reimbursementRate;

    private final List<String> interactions;

    private final List<String> contraindications;

    private final List<String> pregnancyRecommendations;

    private final List<String> renalAdjustments;

    private final List<String> hepaticAdjustments;

    private final List<String> warnings;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate lastUpdateDate;

    /**
     * Constructeur pour un médicament.
     *
     * @param cisCode Le code CIS unique du médicament
     * @param name Le nom commercial du médicament
     * @param activeSubstance La substance active (DCI)
     * @param atcCode Le code ATC
     * @param pharmaceuticalForm La forme pharmaceutique
     * @param strength Le dosage
     * @param route La voie d'administration
     * @param prescriptionRequired Nécessite-t-il une prescription
     * @param reimbursed Est-il remboursé
     * @param reimbursementRate Taux de remboursement
     */
    public Medication(String cisCode, String name, String activeSubstance,
                     String atcCode, String pharmaceuticalForm, String strength,
                     String route, boolean prescriptionRequired,
                     boolean reimbursed, float reimbursementRate) {

        // Assertion #1: Vérification des paramètres obligatoires
        assert cisCode != null && cisCode.matches("^\\d{8}$") :
            "Le code CIS doit être composé de 8 chiffres";
        assert name != null && !name.isBlank() :
            "Le nom du médicament ne peut pas être null ou vide";
        assert activeSubstance != null && !activeSubstance.isBlank() :
            "La DCI ne peut pas être null ou vide";

        // Assertion #2: Vérification des paramètres numériques
        assert reimbursementRate >= 0 && reimbursementRate <= 100 :
            "Le taux de remboursement doit être entre 0 et 100%";

        this.id = UUID.randomUUID().toString();
        this.cisCode = cisCode;
        this.name = name;
        this.activeSubstance = activeSubstance;
        this.atcCode = atcCode;
        this.pharmaceuticalForm = pharmaceuticalForm;
        this.strength = strength;
        this.route = route;
        this.prescriptionRequired = prescriptionRequired;
        this.reimbursed = reimbursed;
        this.reimbursementRate = reimbursementRate;
        this.interactions = new ArrayList<>();
        this.contraindications = new ArrayList<>();
        this.pregnancyRecommendations = new ArrayList<>();
        this.renalAdjustments = new ArrayList<>();
        this.hepaticAdjustments = new ArrayList<>();
        this.warnings = new ArrayList<>();
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Constructeur minimal pour un médicament.
     *
     * @param cisCode Le code CIS unique du médicament
     * @param name Le nom commercial du médicament
     * @param activeSubstance La substance active (DCI)
     */
    public Medication(String cisCode, String name, String activeSubstance) {
        this(cisCode, name, activeSubstance, null, null, null, null, false, false, 0);
    }

    /**
     * Vérifie si le médicament nécessite un ajustement posologique pour insuffisance rénale.
     *
     * @return true si un ajustement pour insuffisance rénale est nécessaire
     */
    public boolean requiresRenalAdjustment() {
        // Assertion #1: Vérification que la liste est initialisée
        assert renalAdjustments != null : "La liste des ajustements rénaux n'est pas initialisée";

        // Assertion #2: Vérification de cohérence si des ajustements sont présents
        if (!renalAdjustments.isEmpty()) {
            assert renalAdjustments.stream().noneMatch(String::isBlank) :
                "Les ajustements rénaux ne peuvent pas être vides";
        }

        return !renalAdjustments.isEmpty();
    }

    /**
     * Vérifie si le médicament nécessite un ajustement posologique pour insuffisance hépatique.
     *
     * @return true si un ajustement pour insuffisance hépatique est nécessaire
     */
    public boolean requiresHepaticAdjustment() {
        // Assertion #1: Vérification que la liste est initialisée
        assert hepaticAdjustments != null : "La liste des ajustements hépatiques n'est pas initialisée";

        // Assertion #2: Vérification de cohérence si des ajustements sont présents
        if (!hepaticAdjustments.isEmpty()) {
            assert hepaticAdjustments.stream().noneMatch(String::isBlank) :
                "Les ajustements hépatiques ne peuvent pas être vides";
        }

        return !hepaticAdjustments.isEmpty();
    }

    /**
     * Vérifie si le médicament est contre-indiqué dans une condition médicale spécifique.
     *
     * @param condition La condition médicale à vérifier
     * @return true si le médicament est contre-indiqué pour cette condition
     */
    public boolean isContraindicatedFor(String condition) {
        // Assertion #1: Vérification que la condition n'est pas vide
        assert condition != null && !condition.isBlank() :
            "La condition médicale ne peut pas être null ou vide";

        // Assertion #2: Vérification que la liste des contre-indications est initialisée
        assert contraindications != null : "La liste des contre-indications n'est pas initialisée";

        if (contraindications.isEmpty()) {
            return false;
        }

        return contraindications.stream()
                .anyMatch(ci -> ci.toLowerCase().contains(condition.toLowerCase()));
    }

    /**
     * Vérifie si le médicament est susceptible d'interagir avec un autre médicament.
     *
     * @param otherMedication L'autre médicament à vérifier
     * @return true si une interaction est possible
     */
    public boolean interactsWith(Medication otherMedication) {
        // Assertion #1: Vérification que l'autre médicament n'est pas null
        assert otherMedication != null : "L'autre médicament ne peut pas être null";

        // Assertion #2: Vérification des listes d'interactions
        assert interactions != null : "La liste des interactions n'est pas initialisée";
        assert otherMedication.getActiveSubstance() != null :
            "La substance active de l'autre médicament doit être définie";

        if (interactions.isEmpty()) {
            return false;
        }

        return interactions.stream()
                .anyMatch(interaction -> interaction.toLowerCase()
                        .contains(otherMedication.getActiveSubstance().toLowerCase()));
    }

    /**
     * Vérifie si le médicament est un médicament à risque pour les personnes âgées.
     * Basé sur les critères de Beers ou STOPP/START.
     *
     * @return true si le médicament présente un risque accru chez les personnes âgées
     */
    public boolean isRiskyForElderly() {
        // Assertion #1: Vérification que les listes sont initialisées
        assert warnings != null : "La liste des avertissements n'est pas initialisée";

        // Assertion #2: Vérification de cohérence
        if (isContraindicatedFor("personne âgée") || isContraindicatedFor("sujet âgé")) {
            assert warnings.stream().anyMatch(w -> w.toLowerCase().contains("âgé")) :
                "Incohérence: contre-indication chez la personne âgée sans avertissement";
        }

        return isContraindicatedFor("personne âgée") ||
               isContraindicatedFor("sujet âgé") ||
               warnings.stream().anyMatch(w -> w.toLowerCase().contains("beers") ||
                                            w.toLowerCase().contains("stopp"));
    }

    /**
     * Génère un résumé des informations essentielles du médicament.
     *
     * @return Un résumé formaté du médicament
     */
    public String generateSummary() {
        // Assertion #1: Vérification des données obligatoires
        assert name != null && activeSubstance != null :
            "Le nom et la substance active sont requis pour générer un résumé";

        // Assertion #2: Vérification du format du code CIS
        assert cisCode != null && cisCode.matches("^\\d{8}$") :
            "Format de code CIS invalide";

        final StringBuilder summary = new StringBuilder(100);
        summary.append(name);

        if (strength != null && !strength.isBlank()) {
            summary.append(" ").append(strength);
        }

        if (pharmaceuticalForm != null && !pharmaceuticalForm.isBlank()) {
            summary.append(" ").append(pharmaceuticalForm);
        }

        summary.append(" (").append(activeSubstance).append(")");

        if (prescriptionRequired) {
            summary.append(" - Liste I");
        }

        if (reimbursed) {
            summary.append(" - Remboursé à ").append(reimbursementRate).append("%");
        }

        return summary.toString();
    }

    // Getters - Pas de setters pour garantir l'immutabilité

    public String getId() {
        return id;
    }

    public String getCisCode() {
        return cisCode;
    }

    public String getName() {
        return name;
    }

    public String getActiveSubstance() {
        return activeSubstance;
    }

    public String getAtcCode() {
        return atcCode;
    }

    public String getPharmaceuticalForm() {
        return pharmaceuticalForm;
    }

    public String getStrength() {
        return strength;
    }

    public String getRoute() {
        return route;
    }

    public boolean isPrescriptionRequired() {
        return prescriptionRequired;
    }

    public boolean isReimbursed() {
        return reimbursed;
    }

    public float getReimbursementRate() {
        return reimbursementRate;
    }

    public List<String> getInteractions() {
        return new ArrayList<>(interactions);
    }

    public List<String> getContraindications() {
        return new ArrayList<>(contraindications);
    }

    public List<String> getPregnancyRecommendations() {
        return new ArrayList<>(pregnancyRecommendations);
    }

    public List<String> getRenalAdjustments() {
        return new ArrayList<>(renalAdjustments);
    }

    public List<String> getHepaticAdjustments() {
        return new ArrayList<>(hepaticAdjustments);
    }

    public List<String> getWarnings() {
        return new ArrayList<>(warnings);
    }

    public LocalDate getLastUpdateDate() {
        return lastUpdateDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Medication that = (Medication) o;
        return Objects.equals(id, that.id) || Objects.equals(cisCode, that.cisCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, cisCode);
    }
}

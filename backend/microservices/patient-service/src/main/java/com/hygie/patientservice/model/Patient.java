package com.hygie.patientservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Modèle représentant un patient dans le système Hygie-AI.
 *
 * Implémente le modèle FHIR Patient avec les extensions nécessaires
 * pour le contexte pharmaceutique.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Document(collection = "patients")
public class Patient {

    @Id
    private final String id;

    @NotBlank(message = "L'identifiant national de santé (INS) est obligatoire")
    @Pattern(regexp = "^[0-9]{13,15}$", message = "Format d'INS invalide")
    @Indexed(unique = true)
    private String nationalId;

    @NotBlank(message = "Le nom de famille est obligatoire")
    private String lastName;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotNull(message = "La date de naissance est obligatoire")
    @Past(message = "La date de naissance doit être dans le passé")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @Pattern(regexp = "^[MF]$", message = "Le genre doit être 'M' ou 'F'")
    private String gender;

    @Pattern(regexp = "^\\d{5}$", message = "Format de code postal invalide")
    private String postalCode;

    private Double height; // en cm

    private Double weight; // en kg

    private Double creatinineClearance; // en mL/min

    private String renalFunction; // Normal, Insuffisance légère, modérée, sévère, terminale

    private String hepaticFunction; // Normal, Child-Pugh A, B ou C

    private final List<String> allergies;

    private final List<String> activeConditions;

    private final List<MedicalHistory> medicalHistory;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate lastUpdateDate;

    /**
     * Constructeur par défaut.
     */
    public Patient() {
        this.id = UUID.randomUUID().toString();
        this.allergies = new ArrayList<>();
        this.activeConditions = new ArrayList<>();
        this.medicalHistory = new ArrayList<>();
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Constructeur avec les informations de base du patient.
     *
     * @param nationalId L'identifiant national de santé
     * @param lastName Le nom de famille
     * @param firstName Le prénom
     * @param birthDate La date de naissance
     * @param gender Le genre (M/F)
     */
    public Patient(String nationalId, String lastName, String firstName,
                   LocalDate birthDate, String gender) {
        // Assertion #1: Vérification des paramètres obligatoires
        assert nationalId != null && !nationalId.isBlank() : "L'identifiant national ne peut pas être null ou vide";
        assert lastName != null && !lastName.isBlank() : "Le nom de famille ne peut pas être null ou vide";
        assert firstName != null && !firstName.isBlank() : "Le prénom ne peut pas être null ou vide";
        assert birthDate != null && birthDate.isBefore(LocalDate.now()) : "La date de naissance doit être valide et dans le passé";
        assert gender != null && (gender.equals("M") || gender.equals("F")) : "Le genre doit être 'M' ou 'F'";

        // Assertion #2: Vérification de la validité de l'identifiant national
        assert nationalId.matches("^[0-9]{13,15}$") : "Format d'INS invalide";

        this.id = UUID.randomUUID().toString();
        this.nationalId = nationalId;
        this.lastName = lastName;
        this.firstName = firstName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.allergies = new ArrayList<>();
        this.activeConditions = new ArrayList<>();
        this.medicalHistory = new ArrayList<>();
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Calcule l'âge du patient en années.
     *
     * @return L'âge du patient en années
     */
    public int getAge() {
        // Assertion #1: Vérification que la date de naissance est définie
        assert birthDate != null : "La date de naissance est requise pour calculer l'âge";

        // Assertion #2: Vérification que la date de naissance est dans le passé
        assert birthDate.isBefore(LocalDate.now()) : "La date de naissance doit être dans le passé";

        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    /**
     * Calcule l'indice de masse corporelle (IMC) du patient.
     *
     * @return L'IMC du patient, ou null si les données sont manquantes
     */
    public Double getBmi() {
        if (height == null || weight == null || height <= 0 || weight <= 0) {
            return null;
        }

        // Assertion #1: Vérification que les valeurs sont positives
        assert height > 0 : "La taille doit être positive";
        assert weight > 0 : "Le poids doit être positif";

        // Assertion #2: Vérification que les valeurs sont dans des plages réalistes
        assert height >= 30 && height <= 250 : "La taille doit être entre 30 et 250 cm";
        assert weight >= 2 && weight <= 300 : "Le poids doit être entre 2 et 300 kg";

        // Conversion de la hauteur en mètres
        final double heightInMeters = height / 100.0;
        return Math.round((weight / (heightInMeters * heightInMeters)) * 10.0) / 10.0;
    }

    /**
     * Vérifie si le patient est considéré comme âgé (65 ans ou plus).
     *
     * @return true si le patient est âgé de 65 ans ou plus
     */
    public boolean isElderly() {
        // Assertion #1: Vérification que l'âge peut être calculé
        assert birthDate != null : "La date de naissance est requise";

        final int age = getAge();

        // Assertion #2: Vérification que l'âge est dans une plage réaliste
        assert age >= 0 && age <= 120 : "L'âge doit être dans une plage réaliste (0-120)";

        return age >= 65;
    }

    /**
     * Vérifie si le patient a une insuffisance rénale.
     *
     * @return true si le patient a une insuffisance rénale (clearance < 60 mL/min)
     */
    public boolean hasRenalImpairment() {
        if (creatinineClearance == null) {
            return false;
        }

        // Assertion #1: Vérification que la valeur est positive
        assert creatinineClearance >= 0 : "La clairance de la créatinine doit être positive";

        // Assertion #2: Vérification que la valeur est dans une plage réaliste
        assert creatinineClearance <= 200 : "La clairance de la créatinine doit être ≤ 200 mL/min";

        return creatinineClearance < 60.0;
    }

    /**
     * Vérifie si le patient a une insuffisance hépatique.
     *
     * @return true si le patient a une insuffisance hépatique
     */
    public boolean hasHepaticImpairment() {
        if (hepaticFunction == null) {
            return false;
        }

        // Assertion #1: Vérification que la valeur est valide
        assert hepaticFunction.matches("Normal|Child-Pugh A|Child-Pugh B|Child-Pugh C") :
            "La fonction hépatique doit avoir une valeur valide";

        // Assertion #2: Vérification de cohérence
        if ("Normal".equals(hepaticFunction)) {
            assert !hasActiveCondition("Cirrhose") && !hasActiveCondition("Hépatite") :
                "Incohérence entre fonction hépatique et conditions actives";
        }

        return !hepaticFunction.equals("Normal");
    }

    /**
     * Ajoute une allergie à la liste des allergies du patient.
     *
     * @param allergy L'allergie à ajouter
     */
    public void addAllergy(final String allergy) {
        // Assertion #1: Vérification que l'allergie n'est pas vide
        assert allergy != null && !allergy.isBlank() : "L'allergie ne peut pas être null ou vide";

        // Assertion #2: Vérification que l'allergie n'est pas déjà présente
        assert !allergies.contains(allergy) : "Cette allergie est déjà enregistrée";

        allergies.add(allergy);
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Vérifie si le patient a une condition médicale spécifique active.
     *
     * @param condition La condition à vérifier
     * @return true si la condition est active pour ce patient
     */
    public boolean hasActiveCondition(final String condition) {
        // Assertion #1: Vérification que la condition n'est pas vide
        assert condition != null && !condition.isBlank() : "La condition ne peut pas être null ou vide";

        // Assertion #2: Vérification que la liste des conditions est initialisée
        assert activeConditions != null : "La liste des conditions actives n'est pas initialisée";

        return activeConditions.contains(condition);
    }

    /**
     * Ajoute une condition médicale active à la liste des conditions du patient.
     *
     * @param condition La condition médicale à ajouter
     */
    public void addActiveCondition(final String condition) {
        // Assertion #1: Vérification que la condition n'est pas vide
        assert condition != null && !condition.isBlank() : "La condition ne peut pas être null ou vide";

        // Assertion #2: Vérification que la condition n'est pas déjà présente
        assert !activeConditions.contains(condition) : "Cette condition est déjà enregistrée";

        activeConditions.add(condition);
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Ajoute un élément à l'historique médical du patient.
     *
     * @param history L'élément d'historique médical à ajouter
     */
    public void addMedicalHistory(final MedicalHistory history) {
        // Assertion #1: Vérification que l'historique n'est pas null
        assert history != null : "L'historique médical ne peut pas être null";

        // Assertion #2: Vérification de la date de l'événement
        assert history.getEventDate() != null && history.getEventDate().isBefore(LocalDate.now()) :
            "La date de l'événement doit être dans le passé";

        medicalHistory.add(history);
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Génère un résumé clinique du patient.
     *
     * @return Un résumé clinique du patient
     */
    public String generateClinicalSummary() {
        // Assertion #1: Vérification des données de base du patient
        assert lastName != null && firstName != null && birthDate != null && gender != null :
            "Les données de base du patient sont requises pour générer un résumé";

        // Assertion #2: Vérification de la cohérence des données
        if (height != null && weight != null) {
            assert height > 0 && weight > 0 : "La taille et le poids doivent être positifs";
        }

        final StringBuilder summary = new StringBuilder(100);
        summary.append(String.format("Patient: %s %s, %d ans, %s\n",
                    firstName, lastName, getAge(), gender.equals("M") ? "Homme" : "Femme"));

        if (height != null && weight != null) {
            summary.append(String.format("Morphologie: %.1f cm, %.1f kg (IMC: %.1f)\n",
                    height, weight, getBmi()));
        }

        if (creatinineClearance != null) {
            summary.append(String.format("Fonction rénale: Clairance créatinine %.1f mL/min (%s)\n",
                    creatinineClearance, renalFunction != null ? renalFunction : "Non précisée"));
        }

        if (hepaticFunction != null) {
            summary.append(String.format("Fonction hépatique: %s\n", hepaticFunction));
        }

        if (!allergies.isEmpty()) {
            summary.append("Allergies: ").append(String.join(", ", allergies)).append("\n");
        }

        if (!activeConditions.isEmpty()) {
            summary.append("Conditions médicales actives: ").append(String.join(", ", activeConditions)).append("\n");
        }

        return summary.toString();
    }

    /**
     * Met à jour la date de dernière modification.
     */
    public void updateLastUpdateDate() {
        this.lastUpdateDate = LocalDate.now();
    }

    // Getters et Setters

    public String getId() {
        return id;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(final String nationalId) {
        this.nationalId = nationalId;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(final String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(final String firstName) {
        this.firstName = firstName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(final LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(final String gender) {
        this.gender = gender;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(final String postalCode) {
        this.postalCode = postalCode;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(final Double height) {
        this.height = height;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(final Double weight) {
        this.weight = weight;
    }

    public Double getCreatinineClearance() {
        return creatinineClearance;
    }

    public void setCreatinineClearance(final Double creatinineClearance) {
        this.creatinineClearance = creatinineClearance;
    }

    public String getRenalFunction() {
        return renalFunction;
    }

    public void setRenalFunction(final String renalFunction) {
        this.renalFunction = renalFunction;
    }

    public String getHepaticFunction() {
        return hepaticFunction;
    }

    public void setHepaticFunction(final String hepaticFunction) {
        this.hepaticFunction = hepaticFunction;
    }

    public List<String> getAllergies() {
        return new ArrayList<>(allergies);
    }

    public List<String> getActiveConditions() {
        return new ArrayList<>(activeConditions);
    }

    public List<MedicalHistory> getMedicalHistory() {
        return new ArrayList<>(medicalHistory);
    }

    public LocalDate getLastUpdateDate() {
        return lastUpdateDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Patient patient = (Patient) o;
        return Objects.equals(id, patient.id) ||
               Objects.equals(nationalId, patient.nationalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nationalId);
    }
}

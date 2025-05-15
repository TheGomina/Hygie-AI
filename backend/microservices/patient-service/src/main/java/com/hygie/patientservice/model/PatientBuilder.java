package com.hygie.patientservice.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Classe Builder pour la création de patients immutables.
 *
 * Permet de construire des instances de Patient de manière fluide
 * tout en respectant les contraintes d'immutabilité.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PatientBuilder {

    private String id;
    private String nationalId;
    @SuppressWarnings("unused") // Alias pour nationalId, utilisé dans setSocialSecurityNumber
    private String socialSecurityNumber;
    private String lastName;
    private String firstName;
    private LocalDate birthDate;
    private String gender;
    private String postalCode;
    private Double height;
    private Double weight;
    private Double creatinineClearance;
    private String renalFunction;
    private String hepaticFunction;
    private List<String> allergies;
    private List<String> activeConditions;
    @SuppressWarnings("unused") // Alias pour activeConditions, utilisé dans setConditions
    private List<String> conditions;
    private List<MedicalHistory> medicalHistory;
    private LocalDate lastUpdateDate;

    /**
     * Constructeur par défaut.
     */
    public PatientBuilder() {
        this.id = UUID.randomUUID().toString();
        this.allergies = new ArrayList<>();
        this.activeConditions = new ArrayList<>();
        this.medicalHistory = new ArrayList<>();
        this.lastUpdateDate = LocalDate.now();
    }

    /**
     * Constructeur avec un patient existant.
     *
     * @param patient Le patient à partir duquel construire
     */
    public PatientBuilder(Patient patient) {
        // Assertion #1: Vérification que le patient n'est pas nul
        assert patient != null : "Le patient ne peut pas être nul";

        this.id = patient.getId();
        this.nationalId = patient.getNationalId();
        this.lastName = patient.getLastName();
        this.firstName = patient.getFirstName();
        this.birthDate = patient.getBirthDate();
        this.gender = patient.getGender();
        this.postalCode = patient.getPostalCode();
        this.height = patient.getHeight();
        this.weight = patient.getWeight();
        this.creatinineClearance = patient.getCreatinineClearance();
        this.renalFunction = patient.getRenalFunction();
        this.hepaticFunction = patient.getHepaticFunction();
        this.allergies = new ArrayList<>(patient.getAllergies());
        this.activeConditions = new ArrayList<>(patient.getActiveConditions());
        this.medicalHistory = new ArrayList<>(patient.getMedicalHistory());
        this.lastUpdateDate = patient.getLastUpdateDate();
    }

    /**
     * Crée un builder à partir d'un patient existant.
     *
     * @param patient Le patient à partir duquel construire le builder
     * @return Un nouveau builder initialisé avec les données du patient
     */
    public static PatientBuilder fromPatient(Patient patient) {
        // Assertion: Vérification que le patient n'est pas nul
        assert patient != null : "Le patient ne peut pas être nul";

        return new PatientBuilder(patient);
    }

    /**
     * Définit l'ID.
     *
     * @param id L'ID du patient
     * @return Une référence à ce builder
     */
    public PatientBuilder setId(String id) {
        // Assertion #1: Vérification que l'ID n'est pas vide
        assert id != null && !id.isBlank() : "L'ID ne peut pas être null ou vide";

        this.id = id;
        return this;
    }

    /**
     * Définit l'identifiant national.
     *
     * @param nationalId L'identifiant national
     * @return Une référence à ce builder
     */
    public PatientBuilder setNationalId(String nationalId) {
        // Assertion #1: Vérification que l'identifiant n'est pas vide
        assert nationalId != null && !nationalId.isBlank() :
            "L'identifiant national ne peut pas être null ou vide";

        this.nationalId = nationalId;
        return this;
    }

    /**
     * Définit le numéro de sécurité sociale (alias pour nationalId).
     *
     * @param ssn Le numéro de sécurité sociale
     * @return Une référence à ce builder
     */
    public PatientBuilder setSocialSecurityNumber(String ssn) {
        // Assertion #1: Vérification que le numéro n'est pas vide
        assert ssn != null && !ssn.isBlank() :
            "Le numéro de sécurité sociale ne peut pas être null ou vide";

        this.nationalId = ssn;
        this.socialSecurityNumber = ssn;
        return this;
    }

    /**
     * Définit le nom de famille.
     *
     * @param lastName Le nom de famille
     * @return Une référence à ce builder
     */
    public PatientBuilder setLastName(String lastName) {
        // Assertion #1: Vérification que le nom n'est pas vide
        assert lastName != null && !lastName.isBlank() :
            "Le nom de famille ne peut pas être null ou vide";

        this.lastName = lastName;
        return this;
    }

    /**
     * Définit le prénom.
     *
     * @param firstName Le prénom
     * @return Une référence à ce builder
     */
    public PatientBuilder setFirstName(String firstName) {
        // Assertion #1: Vérification que le prénom n'est pas vide
        assert firstName != null && !firstName.isBlank() :
            "Le prénom ne peut pas être null ou vide";

        this.firstName = firstName;
        return this;
    }

    /**
     * Définit la date de naissance.
     *
     * @param birthDate La date de naissance
     * @return Une référence à ce builder
     */
    public PatientBuilder setBirthDate(LocalDate birthDate) {
        // Assertion #1: Vérification que la date n'est pas nulle et dans le passé
        assert birthDate != null && birthDate.isBefore(LocalDate.now()) :
            "La date de naissance doit être valide et dans le passé";

        this.birthDate = birthDate;
        return this;
    }

    /**
     * Définit le genre.
     *
     * @param gender Le genre (M/F)
     * @return Une référence à ce builder
     */
    public PatientBuilder setGender(String gender) {
        // Assertion #1: Vérification que le genre est valide
        assert gender != null && (gender.equals("M") || gender.equals("F")) :
            "Le genre doit être 'M' ou 'F'";

        this.gender = gender;
        return this;
    }

    /**
     * Définit le code postal.
     *
     * @param postalCode Le code postal
     * @return Une référence à ce builder
     */
    public PatientBuilder setPostalCode(String postalCode) {
        // Assertion #1: Vérification du format si non null
        assert postalCode == null || postalCode.matches("^\\d{5}$") :
            "Format de code postal invalide";

        this.postalCode = postalCode;
        return this;
    }

    /**
     * Définit la taille.
     *
     * @param height La taille en cm
     * @return Une référence à ce builder
     */
    public PatientBuilder setHeight(Double height) {
        // Assertion #1: Vérification que la taille est dans une plage raisonnable
        assert height == null || (height > 30 && height < 250) :
            "La taille doit être dans une plage raisonnable (30-250 cm)";

        this.height = height;
        return this;
    }

    /**
     * Définit le poids.
     *
     * @param weight Le poids en kg
     * @return Une référence à ce builder
     */
    public PatientBuilder setWeight(Double weight) {
        // Assertion #1: Vérification que le poids est dans une plage raisonnable
        assert weight == null || (weight > 0 && weight < 500) :
            "Le poids doit être dans une plage raisonnable (0-500 kg)";

        this.weight = weight;
        return this;
    }

    /**
     * Définit la clairance de la créatinine.
     *
     * @param creatinineClearance La clairance en mL/min
     * @return Une référence à ce builder
     */
    public PatientBuilder setCreatinineClearance(Double creatinineClearance) {
        // Assertion #1: Vérification que la valeur est dans une plage raisonnable
        assert creatinineClearance == null || (creatinineClearance >= 0 && creatinineClearance <= 200) :
            "La clairance de créatinine doit être dans une plage raisonnable (0-200 mL/min)";

        this.creatinineClearance = creatinineClearance;
        return this;
    }

    /**
     * Définit la fonction rénale.
     *
     * @param renalFunction La description de la fonction rénale
     * @return Une référence à ce builder
     */
    public PatientBuilder setRenalFunction(String renalFunction) {
        this.renalFunction = renalFunction;
        return this;
    }

    /**
     * Définit la fonction hépatique.
     *
     * @param hepaticFunction La description de la fonction hépatique
     * @return Une référence à ce builder
     */
    public PatientBuilder setHepaticFunction(String hepaticFunction) {
        this.hepaticFunction = hepaticFunction;
        return this;
    }

    /**
     * Définit les allergies.
     *
     * @param allergies La liste des allergies
     * @return Une référence à ce builder
     */
    public PatientBuilder setAllergies(List<String> allergies) {
        // Assertion #1: Vérification que la liste n'est pas nulle
        assert allergies != null : "La liste des allergies ne peut pas être nulle";

        this.allergies = new ArrayList<>(allergies);
        return this;
    }

    /**
     * Ajoute une allergie.
     *
     * @param allergy L'allergie à ajouter
     * @return Une référence à ce builder
     */
    public PatientBuilder addAllergy(String allergy) {
        // Assertion #1: Vérification que l'allergie n'est pas vide
        assert allergy != null && !allergy.isBlank() :
            "L'allergie ne peut pas être null ou vide";

        this.allergies.add(allergy);
        return this;
    }

    /**
     * Définit les conditions médicales actives.
     *
     * @param activeConditions La liste des conditions médicales actives
     * @return Une référence à ce builder
     */
    public PatientBuilder setActiveConditions(List<String> activeConditions) {
        // Assertion #1: Vérification que la liste n'est pas nulle
        assert activeConditions != null :
            "La liste des conditions médicales ne peut pas être nulle";

        this.activeConditions = new ArrayList<>(activeConditions);
        return this;
    }

    /**
     * Définit les conditions médicales (alias pour activeConditions).
     *
     * @param conditions La liste des conditions médicales
     * @return Une référence à ce builder
     */
    public PatientBuilder setConditions(List<String> conditions) {
        // Assertion #1: Vérification que la liste n'est pas nulle
        assert conditions != null :
            "La liste des conditions médicales ne peut pas être nulle";

        this.activeConditions = new ArrayList<>(conditions);
        this.conditions = conditions;
        return this;
    }

    /**
     * Ajoute une condition médicale active.
     *
     * @param condition La condition médicale à ajouter
     * @return Une référence à ce builder
     */
    public PatientBuilder addActiveCondition(String condition) {
        // Assertion #1: Vérification que la condition n'est pas vide
        assert condition != null && !condition.isBlank() :
            "La condition médicale ne peut pas être null ou vide";

        this.activeConditions.add(condition);
        return this;
    }

    /**
     * Définit l'historique médical.
     *
     * @param medicalHistory La liste des éléments d'historique médical
     * @return Une référence à ce builder
     */
    public PatientBuilder setMedicalHistory(List<MedicalHistory> medicalHistory) {
        // Assertion #1: Vérification que la liste n'est pas nulle
        assert medicalHistory != null :
            "La liste de l'historique médical ne peut pas être nulle";

        this.medicalHistory = new ArrayList<>(medicalHistory);
        return this;
    }

    /**
     * Ajoute un élément à l'historique médical.
     *
     * @param historyItem L'élément d'historique à ajouter
     * @return Une référence à ce builder
     */
    public PatientBuilder addMedicalHistoryItem(MedicalHistory historyItem) {
        // Assertion #1: Vérification que l'élément n'est pas nul
        assert historyItem != null :
            "L'élément d'historique médical ne peut pas être nul";

        this.medicalHistory.add(historyItem);
        return this;
    }

    /**
     * Définit la date de dernière mise à jour.
     *
     * @param lastUpdateDate La date de dernière mise à jour
     * @return Une référence à ce builder
     */
    public PatientBuilder setLastUpdateDate(LocalDate lastUpdateDate) {
        // Assertion #1: Vérification que la date n'est pas nulle
        assert lastUpdateDate != null :
            "La date de mise à jour ne peut pas être nulle";

        this.lastUpdateDate = lastUpdateDate;
        return this;
    }

    /**
     * Construit une instance de Patient.
     *
     * @return Une nouvelle instance de Patient
     */
    public Patient build() {
        // Assertion #1: Vérification des champs obligatoires
        assert nationalId != null && !nationalId.isBlank() :
            "L'identifiant national est obligatoire";
        assert lastName != null && !lastName.isBlank() :
            "Le nom de famille est obligatoire";
        assert firstName != null && !firstName.isBlank() :
            "Le prénom est obligatoire";
        assert birthDate != null :
            "La date de naissance est obligatoire";
        assert gender != null && (gender.equals("M") || gender.equals("F")) :
            "Le genre doit être 'M' ou 'F'";

        // On utilise le constructeur avec informations de base
        Patient patient = new Patient(
            nationalId,
            lastName,
            firstName,
            birthDate,
            gender
        );

        // On utilise la réflexion pour setter les autres champs non-accessibles par le constructeur
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(patient, id);

            // On utilise les setters existants pour les autres champs
            if (postalCode != null) {
                patient.setPostalCode(postalCode);
            }

            if (height != null) {
                patient.setHeight(height);
            }

            if (weight != null) {
                patient.setWeight(weight);
            }

            if (creatinineClearance != null) {
                patient.setCreatinineClearance(creatinineClearance);
            }

            if (renalFunction != null) {
                patient.setRenalFunction(renalFunction);
            }

            if (hepaticFunction != null) {
                patient.setHepaticFunction(hepaticFunction);
            }

            // Les collections sont finales, donc on doit utiliser la réflexion
            if (!allergies.isEmpty()) {
                java.lang.reflect.Field allergiesField = Patient.class.getDeclaredField("allergies");
                allergiesField.setAccessible(true);

                // Suppression de l'avertissement de conversion non vérifiée car nous savons que le champ contient une List<String>
                @SuppressWarnings("unchecked")
                List<String> currentAllergies = (List<String>) allergiesField.get(patient);
                currentAllergies.addAll(allergies);
            }

            if (!activeConditions.isEmpty()) {
                java.lang.reflect.Field conditionsField = Patient.class.getDeclaredField("activeConditions");
                conditionsField.setAccessible(true);

                // Suppression de l'avertissement de conversion non vérifiée car nous savons que le champ contient une List<String>
                @SuppressWarnings("unchecked")
                List<String> currentConditions = (List<String>) conditionsField.get(patient);
                currentConditions.addAll(activeConditions);
            }

            if (!medicalHistory.isEmpty()) {
                java.lang.reflect.Field historyField = Patient.class.getDeclaredField("medicalHistory");
                historyField.setAccessible(true);

                // Suppression de l'avertissement de conversion non vérifiée car nous savons que le champ contient une List<MedicalHistory>
                @SuppressWarnings("unchecked")
                List<MedicalHistory> currentHistory = (List<MedicalHistory>) historyField.get(patient);
                currentHistory.addAll(medicalHistory);
            }

            if (lastUpdateDate != null) {
                java.lang.reflect.Field dateField = Patient.class.getDeclaredField("lastUpdateDate");
                dateField.setAccessible(true);
                dateField.set(patient, lastUpdateDate);
            }

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la construction du patient: " + e.getMessage(), e);
        }

        return patient;
    }
}

package com.hygie.patientservice.service;

import com.hygie.patientservice.model.Patient;
import com.hygie.patientservice.model.PatientBuilder;
import com.hygie.patientservice.model.MedicalHistory;
import com.hygie.patientservice.repository.PatientRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des patients dans le système Hygie-AI.
 *
 * Ce service implémente les opérations métier liées aux patients
 * et communique avec le repository pour les opérations de persistance.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Service
@Transactional
public class PatientService {

    private final PatientRepository patientRepository;
    private final PrescriptionService prescriptionService;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param patientRepository Le repository pour accéder aux données des patients
     * @param prescriptionService Le service pour accéder aux prescriptions
     */
    @Autowired
    public PatientService(PatientRepository patientRepository,
                         PrescriptionService prescriptionService) {
        // Assertion #1: Vérification que le repository n'est pas null
        assert patientRepository != null : "Le repository de patients ne peut pas être null";

        // Assertion #2: Vérification que le service de prescriptions n'est pas null
        assert prescriptionService != null : "Le service de prescriptions ne peut pas être null";

        this.patientRepository = patientRepository;
        this.prescriptionService = prescriptionService;
    }

    /**
     * Récupère tous les patients.
     *
     * @return Une liste de tous les patients
     */
    public List<Patient> getAllPatients() {
        final List<Patient> patients = patientRepository.findAll();

        // Assertion #1: Vérification que la liste n'est pas null
        assert patients != null : "La liste des patients ne peut pas être null";

        // Assertion #2: Postcondition optionnelle sur la taille
        // Cette assertion est utile pour les tests mais peut être omise en production
        // assert !patients.isEmpty() : "Aucun patient trouvé dans la base de données";

        return patients;
    }

    /**
     * Récupère un patient par son ID.
     *
     * @param id L'ID du patient
     * @return Un Optional contenant le patient ou vide s'il n'existe pas
     */
    public Optional<Patient> getPatientById(String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";

        final Optional<Patient> patient = patientRepository.findById(id);

        // Assertion #2: Postcondition
        if (patient.isPresent()) {
            assert patient.get().getId().equals(id) :
                "L'ID du patient récupéré ne correspond pas à celui demandé";
        }

        return patient;
    }

    /**
     * Recherche des patients par nom ou prénom.
     *
     * @param query Le terme de recherche
     * @return Une liste de patients correspondant à la recherche
     */
    public List<Patient> searchPatients(String query) {
        // Assertion #1: Vérification que la requête n'est pas null ou vide
        assert query != null && !query.isBlank() :
            "Le terme de recherche ne peut pas être null ou vide";

        // Assertion #2: Vérification de la longueur minimale
        assert query.length() >= 2 :
            "Le terme de recherche doit comporter au moins 2 caractères";

        final List<Patient> byFirstName = patientRepository.findByFirstNameContainingIgnoreCase(query);
        final List<Patient> byLastName = patientRepository.findByLastNameContainingIgnoreCase(query);

        // Fusionner les résultats en éliminant les doublons
        final List<Patient> results = new ArrayList<>(byFirstName);
        final List<String> existingIds = byFirstName.stream()
                .map(Patient::getId)
                .collect(Collectors.toList());

        for (Patient pat : byLastName) {
            if (!existingIds.contains(pat.getId())) {
                results.add(pat);
            }
        }

        return results;
    }

    /**
     * Sauvegarde un patient dans la base de données.
     *
     * @param patient Le patient à sauvegarder
     * @return Le patient sauvegardé avec son ID généré
     */
    public Patient savePatient(Patient patient) {
        // Assertion #1: Vérification que le patient n'est pas null
        assert patient != null : "Le patient à sauvegarder ne peut pas être null";

        // Assertion #2: Vérification des données minimales
        assert patient.getFirstName() != null && !patient.getFirstName().isBlank() :
            "Le prénom du patient ne peut pas être null ou vide";
        assert patient.getLastName() != null && !patient.getLastName().isBlank() :
            "Le nom du patient ne peut pas être null ou vide";

        final Patient savedPatient = patientRepository.save(patient);

        // Postcondition: Vérification que l'enregistrement a fonctionné
        assert savedPatient != null && savedPatient.getId() != null :
            "Échec de la sauvegarde du patient";

        return savedPatient;
    }

    /**
     * Met à jour un patient existant.
     *
     * @param id L'ID du patient à mettre à jour
     * @param patientDetails Les nouvelles données du patient
     * @return Le patient mis à jour, ou null s'il n'existe pas
     */
    public Patient updatePatient(String id, Patient patientDetails) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";
        assert patientDetails != null : "Les détails du patient ne peuvent pas être null";

        final Optional<Patient> existingPatient = patientRepository.findById(id);

        if (existingPatient.isEmpty()) {
            return null;
        }

        // Créer une version mise à jour du patient avec PatientBuilder
        // puisque la classe Patient est immuable
        final Patient currentPatient = existingPatient.get();

        // Utiliser PatientBuilder pour construire un nouveau patient avec les données mises à jour
        // tout en conservant l'ID original et l'historique médical si non fourni
        PatientBuilder builder = PatientBuilder.fromPatient(currentPatient)
            .setNationalId(patientDetails.getNationalId())
            .setLastName(patientDetails.getLastName())
            .setFirstName(patientDetails.getFirstName())
            .setBirthDate(patientDetails.getBirthDate())
            .setGender(patientDetails.getGender())
            .setPostalCode(patientDetails.getPostalCode())
            .setHeight(patientDetails.getHeight())
            .setWeight(patientDetails.getWeight())
            .setCreatinineClearance(patientDetails.getCreatinineClearance())
            .setRenalFunction(patientDetails.getRenalFunction())
            .setHepaticFunction(patientDetails.getHepaticFunction());

        // Conserver l'historique médical existant si le nouveau ne le remplace pas
        if (patientDetails.getMedicalHistory() != null && !patientDetails.getMedicalHistory().isEmpty()) {
            builder.setMedicalHistory(patientDetails.getMedicalHistory());
        }

        // Conserver les conditions actives existantes si les nouvelles ne les remplacent pas
        if (patientDetails.getActiveConditions() != null && !patientDetails.getActiveConditions().isEmpty()) {
            builder.setActiveConditions(patientDetails.getActiveConditions());
        }

        Patient updatedPatient = builder.build();
        updatedPatient = patientRepository.save(updatedPatient);

        // Assertion #2: Vérification que la mise à jour a bien fonctionné
        assert updatedPatient != null : "La mise à jour du patient a échoué";
        assert updatedPatient.getId().equals(id) :
            "L'ID du patient mis à jour ne correspond pas";

        return updatedPatient;
    }

    /**
     * Supprime un patient par son ID.
     *
     * @param id L'ID du patient à supprimer
     * @return true si le patient a été supprimé, false s'il n'existait pas
     */
    public boolean deletePatient(String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";

        final boolean exists = patientRepository.existsById(id);

        if (!exists) {
            return false;
        }

        patientRepository.deleteById(id);

        // Assertion #2: Vérification que la suppression a fonctionné
        final boolean stillExists = patientRepository.existsById(id);
        assert !stillExists : "Le patient n'a pas été correctement supprimé";

        return true;
    }

    /**
     * Ajoute un élément à l'historique médical d'un patient.
     *
     * @param patientId L'ID du patient
     * @param medicalHistoryItem L'élément d'historique médical à ajouter
     * @return Le patient mis à jour avec son historique médical
     */
    public Patient addMedicalHistoryItem(String patientId, MedicalHistory medicalHistoryItem) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert medicalHistoryItem != null : "L'élément d'historique médical ne peut pas être null";

        final Optional<Patient> existingPatient = patientRepository.findById(patientId);

        if (existingPatient.isEmpty()) {
            return null;
        }

        final Patient patient = existingPatient.get();

        // Créer une nouvelle liste contenant l'historique existant plus le nouvel élément
        List<MedicalHistory> updatedHistory = new ArrayList<>(patient.getMedicalHistory());
        updatedHistory.add(medicalHistoryItem);

        // Utiliser PatientBuilder pour créer un nouveau patient avec l'historique médical mis à jour
        final Patient updatedPatient = PatientBuilder.fromPatient(patient)
            .setMedicalHistory(updatedHistory)
            .setLastUpdateDate(LocalDate.now())
            .build();

        final Patient savedPatient = patientRepository.save(updatedPatient);

        // Assertion #2: Vérification que la mise à jour a fonctionné
        assert savedPatient != null : "La mise à jour du patient a échoué";
        assert savedPatient.getMedicalHistory().contains(medicalHistoryItem) :
            "L'élément d'historique médical n'a pas été correctement ajouté";

        return savedPatient;
    }

    /**
     * Recherche les patients avec une condition médicale spécifique.
     *
     * @param condition La condition médicale à rechercher
     * @return Une liste de patients ayant cette condition
     */
    public List<Patient> findPatientsByCondition(String condition) {
        // Assertion #1: Vérification que la condition n'est pas null ou vide
        assert condition != null && !condition.isBlank() :
            "La condition médicale ne peut pas être null ou vide";

        final List<Patient> patients = patientRepository.findByConditionsContainingIgnoreCase(condition);

        // Assertion #2: Vérification du résultat
        assert patients != null : "La liste des patients ne peut pas être null";

        return patients;
    }

    /**
     * Calcule l'âge d'un patient.
     *
     * @param patient Le patient
     * @return L'âge du patient en années, ou -1 si la date de naissance n'est pas définie
     */
    public int calculatePatientAge(Patient patient) {
        // Assertion #1: Vérification que le patient n'est pas null
        assert patient != null : "Le patient ne peut pas être null";

        if (patient.getBirthDate() == null) {
            return -1;
        }

        // Assertion #2: Vérification que la date de naissance est dans le passé
        assert patient.getBirthDate().isBefore(LocalDate.now()) :
            "La date de naissance doit être dans le passé";

        return Period.between(patient.getBirthDate(), LocalDate.now()).getYears();
    }

    /**
     * Recherche les patients éligibles pour un Bilan Partagé de Médication.
     *
     * @param minAge L'âge minimum pour qualifier un patient
     * @param minMedications Le nombre minimum de médicaments
     * @return Une liste de patients éligibles pour un BPM
     */
    public List<Patient> findPatientsEligibleForBPM(int minAge, int minMedications) {
        // Assertion #1: Vérification des paramètres
        assert minAge > 0 : "L'âge minimum doit être positif";
        assert minMedications > 0 : "Le nombre minimum de médicaments doit être positif";

        // Récupérer tous les patients qui ont l'âge minimum requis
        final List<Patient> allPatients = getAllPatients();
        final LocalDate maxBirthDate = LocalDate.now().minusYears(minAge);

        final List<Patient> eligiblePatients = new ArrayList<>();

        for (Patient patient : allPatients) {
            // Vérifier l'âge
            if (patient.getBirthDate() != null && patient.getBirthDate().isBefore(maxBirthDate)) {
                // Vérifier le nombre de médicaments actuels
                final int medicationCount = prescriptionService.getCurrentMedications(patient.getId()).size();

                if (medicationCount >= minMedications) {
                    eligiblePatients.add(patient);
                }
            }
        }

        // Assertion #2: Vérification du résultat
        assert eligiblePatients != null : "La liste des patients éligibles ne peut pas être null";

        return eligiblePatients;
    }

    /**
     * Analyse les risques pour un patient basés sur son historique médical et ses médicaments.
     *
     * @param patientId L'ID du patient
     * @return Une liste des risques identifiés pour le patient
     */
    public List<String> analyzePatientRisks(String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        final Optional<Patient> patientOpt = getPatientById(patientId);

        if (patientOpt.isEmpty()) {
            return new ArrayList<>();
        }

        final Patient patient = patientOpt.get();
        final List<String> risks = new ArrayList<>();

        // Vérifier l'âge
        final int age = calculatePatientAge(patient);
        if (age >= 65) {
            risks.add("Patient âgé (>= 65 ans): risque accru d'effets indésirables");
        }

        // Vérifier les interactions médicamenteuses
        final List<PrescriptionService.PrescriptionItemPair> interactions =
                prescriptionService.checkMedicationInteractions(patientId);

        if (!interactions.isEmpty()) {
            risks.add("Interactions médicamenteuses identifiées: " + interactions.size() + " interactions");
        }

        // Vérifier la présence de conditions spécifiques
        List<String> activeConditions = patient.getActiveConditions();
        if (activeConditions != null && !activeConditions.isEmpty()) {
            for (String condition : activeConditions) {
                // Conditions spécifiques à risque
                if (condition.toLowerCase().contains("rein") ||
                    condition.toLowerCase().contains("rénal")) {
                    risks.add("Insuffisance rénale: ajustement posologique potentiellement nécessaire");
                }
                if (condition.toLowerCase().contains("foie") ||
                    condition.toLowerCase().contains("hépatique")) {
                    risks.add("Insuffisance hépatique: ajustement posologique potentiellement nécessaire");
                }
            }
        }

        // Assertion #2: Vérification du résultat
        assert risks != null : "La liste des risques ne peut pas être null";

        return risks;
    }
}

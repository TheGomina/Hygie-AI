package com.hygie.patientservice.controller;

import com.hygie.patientservice.model.Patient;
import com.hygie.patientservice.model.MedicalHistory;
import com.hygie.patientservice.service.PatientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des patients dans le système Hygie-AI.
 *
 * Expose des endpoints permettant de consulter, ajouter, modifier et supprimer
 * des patients, ainsi que gérer leur historique médical pour les besoins
 * du Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/patients")
@Validated
public class PatientController {

    private static final Logger logger = LoggerFactory.getLogger(PatientController.class);

    private final PatientService patientService;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param patientService Le service de gestion des patients
     */
    @Autowired
    public PatientController(PatientService patientService) {
        // Assertion #1: Vérification que le service n'est pas null
        assert patientService != null : "Le service de patients ne peut pas être null";

        this.patientService = patientService;

        // Assertion #2: Vérification post-initialisation
        assert this.patientService != null : "Échec d'initialisation du service de patients";
    }

    /**
     * Récupère tous les patients.
     *
     * @return Une liste de tous les patients
     */
    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        // Assertion #1: Vérification de l'état du service
        assert patientService != null : "Le service de patients n'est pas initialisé";

        try {
            final List<Patient> patients = patientService.getAllPatients();

            // Assertion #2: Vérification du résultat
            assert patients != null : "La liste des patients ne peut pas être null";

            logger.info("Récupération de {} patients", patients.size());
            return ResponseEntity.ok(patients);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de tous les patients", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère un patient par son ID.
     *
     * @param id L'ID du patient
     * @return Le patient s'il existe, sinon 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";

        try {
            final Optional<Patient> patient = patientService.getPatientById(id);

            // Assertion #2: Vérification de la cohérence du résultat
            assert patient != null : "Le résultat de la recherche ne peut pas être null";

            if (patient.isPresent()) {
                logger.info("Patient trouvé avec l'ID: {}", id);
                return ResponseEntity.ok(patient.get());
            } else {
                logger.warn("Patient non trouvé avec l'ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du patient avec ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Recherche des patients par nom ou prénom.
     *
     * @param query Le terme de recherche
     * @return Une liste de patients correspondant à la recherche
     */
    @GetMapping("/search")
    public ResponseEntity<List<Patient>> searchPatients(
            @RequestParam @NotBlank(message = "Le terme de recherche ne peut pas être vide") String query) {
        // Assertion #1: Vérification que la requête n'est pas null ou vide
        assert query != null && !query.isBlank() :
            "Le terme de recherche ne peut pas être null ou vide";

        try {
            // Vérification de la longueur minimale
            if (query.length() < 2) {
                logger.warn("Terme de recherche trop court: {}", query);
                return ResponseEntity.badRequest().build();
            }

            final List<Patient> patients = patientService.searchPatients(query);

            // Assertion #2: Vérification du résultat
            assert patients != null : "La liste des patients ne peut pas être null";

            logger.info("Recherche de patients pour '{}': {} résultats", query, patients.size());
            return ResponseEntity.ok(patients);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de patients avec le terme: {}", query, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crée un nouveau patient.
     *
     * @param patient Le patient à créer
     * @return Le patient créé avec son ID généré
     */
    @PostMapping
    public ResponseEntity<Patient> createPatient(@Valid @RequestBody Patient patient) {
        // Assertion #1: Vérification que le patient n'est pas null
        assert patient != null : "Le patient à créer ne peut pas être null";

        try {
            final Patient savedPatient = patientService.savePatient(patient);

            // Assertion #2: Vérification que l'enregistrement a fonctionné
            assert savedPatient != null && savedPatient.getId() != null :
                "Échec de la création du patient";

            logger.info("Patient créé avec l'ID: {}", savedPatient.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPatient);
        } catch (Exception e) {
            logger.error("Erreur lors de la création du patient", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Met à jour un patient existant.
     *
     * @param id L'ID du patient à mettre à jour
     * @param patientDetails Les nouvelles données du patient
     * @return Le patient mis à jour, ou 404 s'il n'existe pas
     */
    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id,
            @Valid @RequestBody Patient patientDetails) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";
        assert patientDetails != null : "Les détails du patient ne peuvent pas être null";

        try {
            final Optional<Patient> existingPatient = patientService.getPatientById(id);

            if (existingPatient.isEmpty()) {
                logger.warn("Patient non trouvé pour mise à jour, ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            final Patient updatedPatient = patientService.updatePatient(id, patientDetails);

            // Assertion #2: Vérification que la mise à jour a fonctionné
            assert updatedPatient != null && updatedPatient.getId().equals(id) :
                "Échec de la mise à jour du patient";

            logger.info("Patient mis à jour, ID: {}", id);
            return ResponseEntity.ok(updatedPatient);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du patient avec ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime un patient.
     *
     * @param id L'ID du patient à supprimer
     * @return 204 si supprimé, 404 s'il n'existe pas
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";

        try {
            final boolean isDeleted = patientService.deletePatient(id);

            // Assertion #2: Vérification de l'opération
            assert patientService.getPatientById(id).isEmpty() :
                "Le patient n'a pas été correctement supprimé";

            if (isDeleted) {
                logger.info("Patient supprimé, ID: {}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Patient non trouvé pour suppression, ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du patient avec ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère l'historique médical d'un patient.
     *
     * @param id L'ID du patient
     * @return L'historique médical du patient
     */
    @GetMapping("/{id}/medical-history")
    public ResponseEntity<List<MedicalHistory>> getPatientMedicalHistory(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";

        try {
            final Optional<Patient> patient = patientService.getPatientById(id);

            if (patient.isEmpty()) {
                logger.warn("Patient non trouvé pour récupération de l'historique médical, ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            final List<MedicalHistory> medicalHistory = patient.get().getMedicalHistory();

            // Assertion #2: Vérification du résultat
            assert medicalHistory != null : "L'historique médical ne peut pas être null";

            logger.info("Récupération de {} entrées d'historique médical pour le patient ID: {}",
                        medicalHistory.size(), id);
            return ResponseEntity.ok(medicalHistory);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique médical du patient avec ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Ajoute un élément à l'historique médical d'un patient.
     *
     * @param id L'ID du patient
     * @param medicalHistoryItem L'élément d'historique médical à ajouter
     * @return Le patient mis à jour avec son historique médical
     */
    @PostMapping("/{id}/medical-history")
    public ResponseEntity<Patient> addMedicalHistoryItem(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id,
            @Valid @RequestBody MedicalHistory medicalHistoryItem) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID du patient ne peut pas être null ou vide";
        assert medicalHistoryItem != null : "L'élément d'historique médical ne peut pas être null";

        try {
            final Optional<Patient> existingPatient = patientService.getPatientById(id);

            if (existingPatient.isEmpty()) {
                logger.warn("Patient non trouvé pour ajout d'historique médical, ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            final Patient updatedPatient = patientService.addMedicalHistoryItem(id, medicalHistoryItem);

            // Assertion #2: Vérification que la mise à jour a fonctionné
            assert updatedPatient != null : "Échec de l'ajout d'historique médical";
            assert updatedPatient.getMedicalHistory().contains(medicalHistoryItem) :
                "L'élément d'historique médical n'a pas été correctement ajouté";

            logger.info("Élément d'historique médical ajouté pour le patient ID: {}", id);
            return ResponseEntity.ok(updatedPatient);
        } catch (Exception e) {
            logger.error("Erreur lors de l'ajout d'un élément d'historique médical pour le patient avec ID: {}",
                        id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les patients ayant des conditions médicales spécifiques.
     *
     * @param condition La condition médicale à rechercher
     * @return Une liste de patients avec cette condition
     */
    @GetMapping("/condition")
    public ResponseEntity<List<Patient>> getPatientsByCondition(
            @RequestParam @NotBlank(message = "La condition ne peut pas être vide") String condition) {
        // Assertion #1: Vérification que la condition n'est pas null ou vide
        assert condition != null && !condition.isBlank() :
            "La condition médicale ne peut pas être null ou vide";

        try {
            final List<Patient> patients = patientService.findPatientsByCondition(condition);

            // Assertion #2: Vérification du résultat
            assert patients != null : "La liste des patients ne peut pas être null";

            logger.info("Récupération de {} patients avec la condition médicale: {}",
                       patients.size(), condition);
            return ResponseEntity.ok(patients);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de patients avec la condition: {}", condition, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les patients nécessitant un Bilan Partagé de Médication.
     *
     * @param minAge L'âge minimum pour qualifier un patient (par défaut 65 ans)
     * @param minMedications Le nombre minimum de médicaments (par défaut 5)
     * @return Une liste de patients éligibles pour un BPM
     */
    @GetMapping("/eligible-for-bpm")
    public ResponseEntity<List<Patient>> getPatientsEligibleForBPM(
            @RequestParam(defaultValue = "65") int minAge,
            @RequestParam(defaultValue = "5") int minMedications) {
        // Assertion #1: Vérification des paramètres
        assert minAge > 0 : "L'âge minimum doit être positif";
        assert minMedications > 0 : "Le nombre minimum de médicaments doit être positif";

        try {
            final List<Patient> eligiblePatients =
                    patientService.findPatientsEligibleForBPM(minAge, minMedications);

            // Assertion #2: Vérification du résultat
            assert eligiblePatients != null : "La liste des patients éligibles ne peut pas être null";

            logger.info("Récupération de {} patients éligibles pour un BPM (âge >= {}, médicaments >= {})",
                       eligiblePatients.size(), minAge, minMedications);
            return ResponseEntity.ok(eligiblePatients);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche de patients éligibles pour un BPM", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

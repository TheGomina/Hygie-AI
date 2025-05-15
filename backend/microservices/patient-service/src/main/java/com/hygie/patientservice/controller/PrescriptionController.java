package com.hygie.patientservice.controller;

import com.hygie.patientservice.model.Prescription;
import com.hygie.patientservice.model.Prescription.PrescriptionStatus;
import com.hygie.patientservice.model.PrescriptionItem;
import com.hygie.patientservice.service.PrescriptionService;
import com.hygie.patientservice.service.PrescriptionService.PrescriptionItemPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des prescriptions médicamenteuses dans le système Hygie-AI.
 *
 * Expose des endpoints permettant de consulter, ajouter, modifier et supprimer
 * des prescriptions, ainsi que d'effectuer des analyses pharmaceutiques pour le
 * Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/prescriptions")
@Validated
public class PrescriptionController {

    private static final Logger logger = LoggerFactory.getLogger(PrescriptionController.class);

    private final PrescriptionService prescriptionService;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param prescriptionService Le service de gestion des prescriptions
     */
    @Autowired
    public PrescriptionController(PrescriptionService prescriptionService) {
        // Assertion #1: Vérification que le service n'est pas null
        assert prescriptionService != null : "Le service de prescriptions ne peut pas être null";

        this.prescriptionService = prescriptionService;

        // Assertion #2: Vérification post-initialisation
        assert this.prescriptionService != null : "Échec d'initialisation du service de prescriptions";
    }

    /**
     * Récupère une prescription par son ID.
     *
     * @param id L'ID de la prescription
     * @return La prescription si elle existe, sinon 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Prescription> getPrescriptionById(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";

        final Optional<Prescription> prescription = prescriptionService.getPrescriptionById(id);

        // Assertion #2: Vérification de la cohérence du résultat
        assert prescription != null : "Le résultat de la recherche ne peut pas être null";

        if (prescription.isPresent()) {
            logger.info("Prescription trouvée avec l'ID: {}", id);
            return ResponseEntity.ok(prescription.get());
        } else {
            logger.warn("Prescription non trouvée avec l'ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupère toutes les prescriptions d'un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste des prescriptions du patient
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<Prescription>> getPatientPrescriptions(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        try {
            final List<Prescription> prescriptions = prescriptionService.getPatientPrescriptions(patientId);

            // Assertion #2: Vérification du résultat
            assert prescriptions != null : "La liste des prescriptions ne peut pas être null";

            logger.info("Récupération de {} prescriptions pour le patient ID: {}",
                       prescriptions.size(), patientId);
            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des prescriptions du patient", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les prescriptions actives d'un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste des prescriptions actives
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<List<Prescription>> getActivePrescriptions(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        try {
            final List<Prescription> activePrescriptions = prescriptionService.getActivePrescriptions(patientId);

            // Assertion #2: Vérification du résultat
            assert activePrescriptions != null : "La liste des prescriptions actives ne peut pas être null";

            logger.info("Récupération de {} prescriptions actives pour le patient ID: {}",
                       activePrescriptions.size(), patientId);
            return ResponseEntity.ok(activePrescriptions);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des prescriptions actives", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Crée une nouvelle prescription.
     *
     * @param prescription La prescription à créer
     * @return La prescription créée avec son ID généré
     */
    @PostMapping
    public ResponseEntity<Prescription> createPrescription(@Valid @RequestBody Prescription prescription) {
        // Assertion #1: Vérification que la prescription n'est pas null
        assert prescription != null : "La prescription à créer ne peut pas être null";

        try {
            // Assertion #2: Vérification des données minimales
            assert prescription.getPatientId() != null && !prescription.getPatientId().isBlank() :
                "L'ID du patient est obligatoire";
            assert prescription.getPrescriberId() != null && !prescription.getPrescriberId().isBlank() :
                "L'ID du prescripteur est obligatoire";
            assert !prescription.getPrescriptionItems().isEmpty() :
                "Une prescription doit contenir au moins un médicament";

            final Prescription savedPrescription = prescriptionService.savePrescription(prescription);

            logger.info("Prescription créée avec l'ID: {}", savedPrescription.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedPrescription);
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la prescription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Met à jour le statut d'une prescription.
     *
     * @param id L'ID de la prescription
     * @param status Le nouveau statut
     * @return La prescription mise à jour, ou 404 si elle n'existe pas
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Prescription> updatePrescriptionStatus(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id,
            @RequestParam PrescriptionStatus status) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";
        assert status != null : "Le statut ne peut pas être null";

        try {
            final Prescription updatedPrescription = prescriptionService.updatePrescriptionStatus(id, status);

            if (updatedPrescription == null) {
                logger.warn("Prescription non trouvée pour mise à jour, ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Assertion #2: Vérification que la mise à jour a fonctionné
            assert updatedPrescription.getStatus() == status :
                "Le statut de la prescription n'a pas été correctement mis à jour";

            logger.info("Statut de la prescription mis à jour, ID: {}, nouveau statut: {}", id, status);
            return ResponseEntity.ok(updatedPrescription);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du statut de la prescription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime une prescription.
     *
     * @param id L'ID de la prescription à supprimer
     * @return 204 si supprimée, 404 si elle n'existe pas
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrescription(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";

        try {
            final boolean isDeleted = prescriptionService.deletePrescription(id);

            // Assertion #2: Vérification de l'opération
            assert prescriptionService.getPrescriptionById(id).isEmpty() :
                "La prescription n'a pas été correctement supprimée";

            if (isDeleted) {
                logger.info("Prescription supprimée, ID: {}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Prescription non trouvée pour suppression, ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression de la prescription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les prescriptions contenant un médicament spécifique pour un patient.
     *
     * @param patientId L'ID du patient
     * @param medicationId L'ID du médicament
     * @return Une liste des prescriptions contenant le médicament
     */
    @GetMapping("/patient/{patientId}/medication/{medicationId}")
    public ResponseEntity<List<Prescription>> getPrescriptionsWithMedication(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId,
            @PathVariable @NotBlank(message = "L'ID du médicament ne peut pas être vide") String medicationId) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert medicationId != null && !medicationId.isBlank() :
            "L'ID du médicament ne peut pas être null ou vide";

        try {
            final List<Prescription> prescriptions =
                    prescriptionService.findPrescriptionsWithMedication(patientId, medicationId);

            // Assertion #2: Vérification du résultat
            assert prescriptions != null : "La liste des prescriptions ne peut pas être null";

            logger.info("Récupération de {} prescriptions avec le médicament ID: {} pour le patient ID: {}",
                       prescriptions.size(), medicationId, patientId);
            return ResponseEntity.ok(prescriptions);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des prescriptions avec médicament", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les médicaments actuellement prescrits à un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste des items de prescription actifs
     */
    @GetMapping("/patient/{patientId}/current-medications")
    public ResponseEntity<List<PrescriptionItem>> getCurrentMedications(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        try {
            final List<PrescriptionItem> currentMedications = prescriptionService.getCurrentMedications(patientId);

            // Assertion #2: Vérification du résultat
            assert currentMedications != null : "La liste des médicaments actuels ne peut pas être null";

            logger.info("Récupération de {} médicaments actuellement prescrits pour le patient ID: {}",
                       currentMedications.size(), patientId);
            return ResponseEntity.ok(currentMedications);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des médicaments actuels", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les prescriptions qui expirent bientôt pour un patient.
     *
     * @param patientId L'ID du patient
     * @param daysThreshold Le nombre de jours avant expiration à considérer
     * @return Une liste des prescriptions qui expirent bientôt
     */
    @GetMapping("/patient/{patientId}/expiring")
    public ResponseEntity<List<Prescription>> getExpiringPrescriptions(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId,
            @RequestParam(defaultValue = "30") @Min(1) int daysThreshold) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert daysThreshold > 0 : "Le seuil de jours doit être positif";

        try {
            final List<Prescription> expiringPrescriptions =
                    prescriptionService.getExpiringPrescriptions(patientId, daysThreshold);

            // Assertion #2: Vérification du résultat
            assert expiringPrescriptions != null :
                "La liste des prescriptions expirant bientôt ne peut pas être null";

            logger.info("Récupération de {} prescriptions expirant dans les {} jours pour le patient ID: {}",
                       expiringPrescriptions.size(), daysThreshold, patientId);
            return ResponseEntity.ok(expiringPrescriptions);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des prescriptions expirant bientôt", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Vérifie les interactions médicamenteuses potentielles pour un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste de paires de médicaments qui interagissent
     */
    @GetMapping("/patient/{patientId}/interactions")
    public ResponseEntity<List<PrescriptionItemPair>> checkMedicationInteractions(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        try {
            final List<PrescriptionItemPair> interactions = prescriptionService.checkMedicationInteractions(patientId);

            // Assertion #2: Vérification du résultat
            assert interactions != null : "La liste des interactions ne peut pas être null";

            logger.info("Détection de {} interactions médicamenteuses pour le patient ID: {}",
                       interactions.size(), patientId);
            return ResponseEntity.ok(interactions);
        } catch (Exception e) {
            logger.error("Erreur lors de la vérification des interactions médicamenteuses", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les prescriptions récentes d'un patient.
     *
     * @param patientId L'ID du patient
     * @param limit Le nombre maximum de prescriptions à récupérer
     * @return Une liste des prescriptions récentes
     */
    @GetMapping("/patient/{patientId}/recent")
    public ResponseEntity<List<Prescription>> getRecentPrescriptions(
            @PathVariable @NotBlank(message = "L'ID du patient ne peut pas être vide") String patientId,
            @RequestParam(defaultValue = "5") @Min(1) int limit) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert limit > 0 : "La limite doit être positive";

        try {
            final List<Prescription> recentPrescriptions =
                    prescriptionService.getRecentPrescriptions(patientId, limit);

            // Assertion #2: Vérification du résultat
            assert recentPrescriptions != null :
                "La liste des prescriptions récentes ne peut pas être null";
            assert recentPrescriptions.size() <= limit :
                "Le nombre de prescriptions récupérées dépasse la limite spécifiée";

            logger.info("Récupération des {} prescriptions récentes pour le patient ID: {}",
                       recentPrescriptions.size(), patientId);
            return ResponseEntity.ok(recentPrescriptions);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération des prescriptions récentes", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Purge les prescriptions expirées antérieures à une date.
     * Endpoint administratif nécessitant une authentification spécifique.
     *
     * @param olderThan La date limite (prescriptions expirées avant cette date)
     * @return Le nombre de prescriptions supprimées
     */
    @DeleteMapping("/purge-expired")
    public ResponseEntity<Long> purgeExpiredPrescriptions(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate olderThan) {
        // Assertion #1: Vérification que la date n'est pas null
        assert olderThan != null : "La date limite ne peut pas être null";

        // Vérification que la date est dans le passé
        if (olderThan.isAfter(LocalDate.now())) {
            logger.warn("Date limite dans le futur: {}", olderThan);
            return ResponseEntity.badRequest().build();
        }

        try {
            final long purgedCount = prescriptionService.purgeExpiredPrescriptions(olderThan);

            // Assertion #2: Vérification du résultat
            assert purgedCount >= 0 : "Le nombre de prescriptions purgées ne peut pas être négatif";

            logger.info("{} prescriptions expirées supprimées (antérieures à {})", purgedCount, olderThan);
            return ResponseEntity.ok(purgedCount);
        } catch (Exception e) {
            logger.error("Erreur lors de la purge des prescriptions expirées", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

package com.hygie.patientservice.controller;

import com.hygie.patientservice.model.Medication;
import com.hygie.patientservice.service.MedicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.List;
import java.util.Optional;

/**
 * Contrôleur REST pour la gestion des médicaments dans le système Hygie-AI.
 *
 * Expose des endpoints permettant de consulter, ajouter, modifier et supprimer
 * des médicaments, ainsi que d'effectuer des recherches spécialisées.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/medications")
@Validated
public class MedicationController {

    private static final Logger logger = LoggerFactory.getLogger(MedicationController.class);

    private final MedicationService medicationService;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param medicationService Le service de gestion des médicaments
     */
    @Autowired
    public MedicationController(MedicationService medicationService) {
        // Assertion #1: Vérification que le service n'est pas null
        assert medicationService != null : "Le service de médicaments ne peut pas être null";

        this.medicationService = medicationService;

        // Assertion #2: Vérification post-initialisation
        assert this.medicationService != null : "Échec d'initialisation du service de médicaments";
    }

    /**
     * Récupère tous les médicaments.
     *
     * @return Une liste de tous les médicaments
     */
    @GetMapping
    public ResponseEntity<List<Medication>> getAllMedications() {
        // Assertion #1: Vérification de l'état du service
        assert medicationService != null : "Le service de médicaments n'est pas initialisé";

        final List<Medication> medications = medicationService.getAllMedications();

        // Assertion #2: Vérification du résultat
        assert medications != null : "La liste des médicaments ne peut pas être null";

        logger.info("Récupération de {} médicaments", medications.size());
        return ResponseEntity.ok(medications);
    }

    /**
     * Récupère un médicament par son ID.
     *
     * @param id L'ID du médicament
     * @return Le médicament s'il existe, sinon 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<Medication> getMedicationById(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";

        final Optional<Medication> medication = medicationService.getMedicationById(id);

        // Assertion #2: Vérification de la cohérence du résultat
        assert medication != null : "Le résultat de la recherche ne peut pas être null";

        if (medication.isPresent()) {
            logger.info("Médicament trouvé avec l'ID: {}", id);
            return ResponseEntity.ok(medication.get());
        } else {
            logger.warn("Médicament non trouvé avec l'ID: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Récupère un médicament par son code CIS.
     *
     * @param cisCode Le code CIS du médicament
     * @return Le médicament s'il existe, sinon 404
     */
    @GetMapping("/cis/{cisCode}")
    public ResponseEntity<Medication> getMedicationByCisCode(
            @PathVariable @Pattern(regexp = "^\\d{8}$",
                                  message = "Le code CIS doit être composé de 8 chiffres") String cisCode) {
        // Assertion #1: Vérification que le code CIS est au bon format
        assert cisCode != null && cisCode.matches("^\\d{8}$") :
            "Le code CIS doit être composé de 8 chiffres";

        final Optional<Medication> medication = medicationService.getMedicationByCisCode(cisCode);

        // Assertion #2: Vérification de la cohérence du résultat
        assert medication != null : "Le résultat de la recherche ne peut pas être null";

        if (medication.isPresent()) {
            logger.info("Médicament trouvé avec le code CIS: {}", cisCode);
            return ResponseEntity.ok(medication.get());
        } else {
            logger.warn("Médicament non trouvé avec le code CIS: {}", cisCode);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Recherche des médicaments par nom ou substance active.
     *
     * @param query Le terme de recherche
     * @return Une liste de médicaments correspondant à la recherche
     */
    @GetMapping("/search")
    public ResponseEntity<List<Medication>> searchMedications(
            @RequestParam @NotBlank(message = "Le terme de recherche ne peut pas être vide") String query) {
        // Assertion #1: Vérification que la requête n'est pas null ou vide
        assert query != null && !query.isBlank() :
            "Le terme de recherche ne peut pas être null ou vide";

        // Vérification de la longueur minimale
        if (query.length() < 3) {
            logger.warn("Terme de recherche trop court: {}", query);
            return ResponseEntity.badRequest().build();
        }

        final List<Medication> medications = medicationService.searchMedications(query);

        // Assertion #2: Vérification du résultat
        assert medications != null : "La liste des médicaments ne peut pas être null";

        logger.info("Recherche de médicaments pour '{}': {} résultats", query, medications.size());
        return ResponseEntity.ok(medications);
    }

    /**
     * Crée un nouveau médicament.
     *
     * @param medication Le médicament à créer
     * @return Le médicament créé avec son ID généré
     */
    @PostMapping
    public ResponseEntity<Medication> createMedication(@Valid @RequestBody Medication medication) {
        // Assertion #1: Vérification que le médicament n'est pas null
        assert medication != null : "Le médicament à créer ne peut pas être null";

        try {
            final Optional<Medication> existingMedication =
                    medicationService.getMedicationByCisCode(medication.getCisCode());

            if (existingMedication.isPresent()) {
                logger.warn("Un médicament avec le code CIS {} existe déjà", medication.getCisCode());
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }

            final Medication savedMedication = medicationService.saveMedication(medication);

            // Assertion #2: Vérification que l'enregistrement a fonctionné
            assert savedMedication != null && savedMedication.getId() != null :
                "Échec de la création du médicament";

            logger.info("Médicament créé avec l'ID: {}", savedMedication.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedMedication);
        } catch (Exception e) {
            logger.error("Erreur lors de la création du médicament", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Met à jour un médicament existant.
     *
     * @param id L'ID du médicament à mettre à jour
     * @param medicationDetails Les nouvelles données du médicament
     * @return Le médicament mis à jour, ou 404 s'il n'existe pas
     */
    @PutMapping("/{id}")
    public ResponseEntity<Medication> updateMedication(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id,
            @Valid @RequestBody Medication medicationDetails) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";
        assert medicationDetails != null : "Les détails du médicament ne peuvent pas être null";

        try {
            final Medication updatedMedication = medicationService.updateMedication(id, medicationDetails);

            if (updatedMedication == null) {
                logger.warn("Médicament non trouvé pour mise à jour, ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            // Assertion #2: Vérification que la mise à jour a fonctionné
            assert updatedMedication.getId() != null :
                "L'ID du médicament mis à jour ne peut pas être null";

            logger.info("Médicament mis à jour, ID: {}", id);
            return ResponseEntity.ok(updatedMedication);
        } catch (Exception e) {
            logger.error("Erreur lors de la mise à jour du médicament", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Supprime un médicament.
     *
     * @param id L'ID du médicament à supprimer
     * @return 204 si supprimé, 404 s'il n'existe pas
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedication(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";

        try {
            final boolean isDeleted = medicationService.deleteMedication(id);

            // Assertion #2: Vérification de l'opération
            assert medicationService.getMedicationById(id).isEmpty() :
                "Le médicament n'a pas été correctement supprimé";

            if (isDeleted) {
                logger.info("Médicament supprimé, ID: {}", id);
                return ResponseEntity.noContent().build();
            } else {
                logger.warn("Médicament non trouvé pour suppression, ID: {}", id);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Erreur lors de la suppression du médicament", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les médicaments qui interagissent avec un médicament spécifique.
     *
     * @param id L'ID du médicament
     * @return Une liste de médicaments qui interagissent
     */
    @GetMapping("/{id}/interactions")
    public ResponseEntity<List<Medication>> getInteractingMedications(
            @PathVariable @NotBlank(message = "L'ID ne peut pas être vide") String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";

        try {
            if (!medicationService.getMedicationById(id).isPresent()) {
                logger.warn("Médicament non trouvé, ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            final List<Medication> interactingMedications = medicationService.findInteractingMedications(id);

            // Assertion #2: Vérification du résultat
            assert interactingMedications != null : "La liste des médicaments ne peut pas être null";

            logger.info("Récupération de {} médicaments interagissant avec ID: {}",
                       interactingMedications.size(), id);
            return ResponseEntity.ok(interactingMedications);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des interactions", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les médicaments qui nécessitent un ajustement posologique pour insuffisance rénale.
     *
     * @return Une liste de médicaments nécessitant un ajustement rénal
     */
    @GetMapping("/renal-adjustment")
    public ResponseEntity<List<Medication>> getMedicationsRequiringRenalAdjustment() {
        // Assertion #1: Vérification de l'état du service
        assert medicationService != null : "Le service de médicaments n'est pas initialisé";

        try {
            final List<Medication> medications = medicationService.findMedicationsRequiringRenalAdjustment();

            // Assertion #2: Vérification du résultat
            assert medications != null : "La liste des médicaments ne peut pas être null";

            logger.info("Récupération de {} médicaments nécessitant un ajustement rénal",
                       medications.size());
            return ResponseEntity.ok(medications);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des médicaments avec ajustement rénal", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère les médicaments à risque pour les personnes âgées.
     *
     * @return Une liste de médicaments à risque pour les personnes âgées
     */
    @GetMapping("/elderly-risk")
    public ResponseEntity<List<Medication>> getRiskyMedicationsForElderly() {
        // Assertion #1: Vérification de l'état du service
        assert medicationService != null : "Le service de médicaments n'est pas initialisé";

        try {
            final List<Medication> medications = medicationService.findRiskyMedicationsForElderly();

            // Assertion #2: Vérification du résultat
            assert medications != null : "La liste des médicaments ne peut pas être null";

            logger.info("Récupération de {} médicaments à risque pour les personnes âgées",
                       medications.size());
            return ResponseEntity.ok(medications);
        } catch (Exception e) {
            logger.error("Erreur lors de la recherche des médicaments à risque pour les personnes âgées", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

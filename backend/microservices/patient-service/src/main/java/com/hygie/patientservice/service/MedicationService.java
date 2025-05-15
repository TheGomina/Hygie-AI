package com.hygie.patientservice.service;

import com.hygie.patientservice.model.Medication;
import com.hygie.patientservice.repository.MedicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des médicaments dans le système Hygie-AI.
 *
 * Ce service implémente les opérations métier liées aux médicaments
 * et communique avec le repository pour les opérations de persistance.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Service
@Transactional
public class MedicationService {

    private final MedicationRepository medicationRepository;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param medicationRepository Le repository pour accéder aux données des médicaments
     */
    @Autowired
    public MedicationService(MedicationRepository medicationRepository) {
        // Assertion #1: Vérification que le repository n'est pas null
        assert medicationRepository != null : "Le repository de médicaments ne peut pas être null";

        this.medicationRepository = medicationRepository;
    }

    /**
     * Sauvegarde un médicament dans la base de données.
     *
     * @param medication Le médicament à sauvegarder
     * @return Le médicament sauvegardé avec son ID généré
     */
    public Medication saveMedication(Medication medication) {
        // Assertion #1: Vérification que le médicament n'est pas null
        assert medication != null : "Le médicament à sauvegarder ne peut pas être null";

        // Assertion #2: Vérification que le code CIS est unique
        final Optional<Medication> existingMedication = medicationRepository.findByCisCode(medication.getCisCode());
        assert existingMedication.isEmpty() || existingMedication.get().getId().equals(medication.getId()) :
            "Un médicament avec ce code CIS existe déjà";

        final Medication savedMedication = medicationRepository.save(medication);

        // Postcondition: Vérification que l'enregistrement a fonctionné
        assert savedMedication != null && savedMedication.getId() != null :
            "Échec de la sauvegarde du médicament";

        return savedMedication;
    }

    /**
     * Récupère un médicament par son ID.
     *
     * @param id L'ID du médicament
     * @return Un Optional contenant le médicament ou vide s'il n'existe pas
     */
    public Optional<Medication> getMedicationById(String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";

        final Optional<Medication> medication = medicationRepository.findById(id);

        // Assertion #2: Postcondition
        if (medication.isPresent()) {
            assert medication.get().getId().equals(id) :
                "L'ID du médicament récupéré ne correspond pas à celui demandé";
        }

        return medication;
    }

    /**
     * Récupère un médicament par son code CIS.
     *
     * @param cisCode Le code CIS du médicament
     * @return Un Optional contenant le médicament ou vide s'il n'existe pas
     */
    public Optional<Medication> getMedicationByCisCode(String cisCode) {
        // Assertion #1: Vérification que le code CIS n'est pas null ou vide
        assert cisCode != null && !cisCode.isBlank() :
            "Le code CIS ne peut pas être null ou vide";

        // Assertion #2: Vérification du format du code CIS
        assert cisCode.matches("^\\d{8}$") :
            "Le code CIS doit être composé de 8 chiffres";

        return medicationRepository.findByCisCode(cisCode);
    }

    /**
     * Recherche des médicaments par nom ou substance active.
     *
     * @param query Le terme de recherche (nom ou substance active)
     * @return Une liste de médicaments correspondant à la recherche
     */
    public List<Medication> searchMedications(String query) {
        // Assertion #1: Vérification que la requête n'est pas null ou vide
        assert query != null && !query.isBlank() :
            "Le terme de recherche ne peut pas être null ou vide";

        // Assertion #2: Vérification de la longueur minimale
        assert query.length() >= 3 :
            "Le terme de recherche doit comporter au moins 3 caractères";

        final List<Medication> byName = medicationRepository.findByNameContainingIgnoreCase(query);
        final List<Medication> byActiveSubstance = medicationRepository.findByActiveSubstanceContainingIgnoreCase(query);

        // Fusionner les résultats en éliminant les doublons
        final List<Medication> results = new ArrayList<>(byName);
        final List<String> existingIds = byName.stream()
                .map(Medication::getId)
                .collect(Collectors.toList());

        for (Medication med : byActiveSubstance) {
            if (!existingIds.contains(med.getId())) {
                results.add(med);
            }
        }

        return results;
    }

    /**
     * Supprime un médicament par son ID.
     *
     * @param id L'ID du médicament à supprimer
     * @return true si le médicament a été supprimé, false s'il n'existait pas
     */
    public boolean deleteMedication(String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";

        final Optional<Medication> existingMedication = medicationRepository.findById(id);

        if (existingMedication.isEmpty()) {
            return false;
        }

        medicationRepository.deleteById(id);

        // Assertion #2: Vérification que la suppression a fonctionné
        final boolean stillExists = medicationRepository.existsById(id);
        assert !stillExists : "Le médicament n'a pas été correctement supprimé";

        return true;
    }

    /**
     * Récupère tous les médicaments qui peuvent interagir avec un médicament spécifique.
     *
     * @param medicationId L'ID du médicament
     * @return Une liste de médicaments qui interagissent
     */
    public List<Medication> findInteractingMedications(String medicationId) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert medicationId != null && !medicationId.isBlank() :
            "L'ID du médicament ne peut pas être null ou vide";

        final Optional<Medication> medication = medicationRepository.findById(medicationId);

        // Assertion #2: Vérification que le médicament existe
        assert medication.isPresent() : "Le médicament spécifié n'existe pas";

        final String activeSubstance = medication.get().getActiveSubstance();
        return medicationRepository.findByInteractionsWith(activeSubstance);
    }

    /**
     * Vérifie si deux médicaments ont une interaction connue.
     *
     * @param medicationId1 L'ID du premier médicament
     * @param medicationId2 L'ID du second médicament
     * @return true s'il existe une interaction entre les médicaments
     */
    public boolean checkInteraction(String medicationId1, String medicationId2) {
        // Assertion #1: Vérification que les IDs ne sont pas null ou vides
        assert medicationId1 != null && !medicationId1.isBlank() :
            "L'ID du premier médicament ne peut pas être null ou vide";
        assert medicationId2 != null && !medicationId2.isBlank() :
            "L'ID du second médicament ne peut pas être null ou vide";

        final Optional<Medication> med1 = medicationRepository.findById(medicationId1);
        final Optional<Medication> med2 = medicationRepository.findById(medicationId2);

        // Assertion #2: Vérification que les médicaments existent
        assert med1.isPresent() : "Le premier médicament spécifié n'existe pas";
        assert med2.isPresent() : "Le second médicament spécifié n'existe pas";

        return med1.get().interactsWith(med2.get()) || med2.get().interactsWith(med1.get());
    }

    /**
     * Récupère la liste de tous les médicaments.
     *
     * @return Une liste de tous les médicaments
     */
    public List<Medication> getAllMedications() {
        final List<Medication> medications = medicationRepository.findAll();

        // Assertion #1: Vérification que la liste n'est pas null
        assert medications != null : "La liste de médicaments ne peut pas être null";

        // Assertion #2: Postcondition optionnelle sur la taille
        // Cette assertion est utile pour les tests mais peut être omise en production
        // assert !medications.isEmpty() : "Aucun médicament trouvé dans la base de données";

        return medications;
    }

    /**
     * Recherche les médicaments à risque pour les personnes âgées.
     *
     * @return Une liste des médicaments à risque
     */
    public List<Medication> findRiskyMedicationsForElderly() {
        final List<Medication> riskyMedications = medicationRepository.findRiskyMedicationsForElderly();

        // Assertion #1: Vérification que la liste n'est pas null
        assert riskyMedications != null : "La liste de médicaments à risque ne peut pas être null";

        // Assertion #2: Vérification que chaque médicament est effectivement à risque
        assert riskyMedications.stream().allMatch(Medication::isRiskyForElderly) :
            "Un médicament retourné n'est pas identifié comme à risque pour les personnes âgées";

        return riskyMedications;
    }

    /**
     * Met à jour les données d'un médicament existant.
     *
     * @param id L'ID du médicament à mettre à jour
     * @param medicationDetails Les nouvelles données du médicament
     * @return Le médicament mis à jour, ou null si le médicament n'existe pas
     */
    public Medication updateMedication(String id, Medication medicationDetails) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID du médicament ne peut pas être null ou vide";
        assert medicationDetails != null : "Les détails du médicament ne peuvent pas être null";

        final Optional<Medication> existingMedication = medicationRepository.findById(id);

        if (existingMedication.isEmpty()) {
            return null;
        }

        // Création d'un nouveau médicament avec l'ID existant et les nouvelles données
        // (NB: Comme Medication est immutable, on doit créer une nouvelle instance)
        final Medication updatedMedication = new Medication(
                medicationDetails.getCisCode(),
                medicationDetails.getName(),
                medicationDetails.getActiveSubstance(),
                medicationDetails.getAtcCode(),
                medicationDetails.getPharmaceuticalForm(),
                medicationDetails.getStrength(),
                medicationDetails.getRoute(),
                medicationDetails.isPrescriptionRequired(),
                medicationDetails.isReimbursed(),
                medicationDetails.getReimbursementRate()
        );

        final Medication savedMedication = medicationRepository.save(updatedMedication);

        // Assertion #2: Vérification que la mise à jour a bien fonctionné
        assert savedMedication != null : "La mise à jour du médicament a échoué";
        assert savedMedication.getCisCode().equals(medicationDetails.getCisCode()) :
            "Le code CIS du médicament mis à jour ne correspond pas";

        return savedMedication;
    }

    /**
     * Récupère les médicaments qui nécessitent un ajustement posologique pour insuffisance rénale.
     *
     * @return Une liste des médicaments nécessitant un ajustement rénal
     */
    public List<Medication> findMedicationsRequiringRenalAdjustment() {
        final List<Medication> medications = medicationRepository.findAllRequiringRenalAdjustment();

        // Assertion #1: Vérification que la liste n'est pas null
        assert medications != null : "La liste de médicaments ne peut pas être null";

        // Assertion #2: Vérification que chaque médicament nécessite bien un ajustement rénal
        assert medications.stream().allMatch(Medication::requiresRenalAdjustment) :
            "Un médicament retourné ne nécessite pas d'ajustement rénal";

        return medications;
    }
}

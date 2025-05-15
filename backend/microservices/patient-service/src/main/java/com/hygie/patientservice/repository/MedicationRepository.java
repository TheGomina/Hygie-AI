package com.hygie.patientservice.repository;

import com.hygie.patientservice.model.Medication;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données des médicaments dans MongoDB.
 *
 * Fournit des méthodes pour interagir avec les documents Medication dans la collection
 * medications et implémente des requêtes spécifiques pour les besoins du Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Repository
public interface MedicationRepository extends MongoRepository<Medication, String> {

    /**
     * Recherche un médicament par son code CIS.
     *
     * @param cisCode Le code CIS du médicament
     * @return Un Optional contenant le médicament si trouvé
     */
    Optional<Medication> findByCisCode(String cisCode);

    /**
     * Recherche des médicaments par substance active (DCI).
     *
     * @param activeSubstance La substance active recherchée
     * @return Une liste de médicaments ayant cette substance active
     */
    List<Medication> findByActiveSubstanceContainingIgnoreCase(String activeSubstance);

    /**
     * Recherche des médicaments par nom commercial.
     *
     * @param name Le nom commercial recherché
     * @return Une liste de médicaments correspondant au nom
     */
    List<Medication> findByNameContainingIgnoreCase(String name);

    /**
     * Recherche des médicaments par code ATC.
     *
     * @param atcCode Le code ATC recherché
     * @return Une liste de médicaments ayant ce code ATC
     */
    List<Medication> findByAtcCode(String atcCode);

    /**
     * Recherche des médicaments qui nécessitent un ajustement posologique pour l'insuffisance rénale.
     *
     * @return Une liste de médicaments nécessitant un ajustement rénal
     */
    @Query("{ 'renalAdjustments': { $exists: true, $ne: [] } }")
    List<Medication> findAllRequiringRenalAdjustment();

    /**
     * Recherche des médicaments qui nécessitent un ajustement posologique pour l'insuffisance hépatique.
     *
     * @return Une liste de médicaments nécessitant un ajustement hépatique
     */
    @Query("{ 'hepaticAdjustments': { $exists: true, $ne: [] } }")
    List<Medication> findAllRequiringHepaticAdjustment();

    /**
     * Recherche des médicaments qui présentent des interactions médicamenteuses avec une substance spécifique.
     *
     * @param substanceName Le nom de la substance pour vérifier les interactions
     * @return Une liste de médicaments ayant des interactions avec cette substance
     */
    @Query("{ 'interactions': { $regex: ?0, $options: 'i' } }")
    List<Medication> findByInteractionsWith(String substanceName);

    /**
     * Recherche des médicaments par forme pharmaceutique.
     *
     * @param form La forme pharmaceutique (comprimé, solution, etc.)
     * @return Une liste de médicaments ayant cette forme pharmaceutique
     */
    List<Medication> findByPharmaceuticalFormContainingIgnoreCase(String form);

    /**
     * Recherche des médicaments contre-indiqués pour une condition médicale spécifique.
     *
     * @param condition La condition médicale
     * @return Une liste de médicaments contre-indiqués pour cette condition
     */
    @Query("{ 'contraindications': { $regex: ?0, $options: 'i' } }")
    List<Medication> findByContraindicationFor(String condition);

    /**
     * Recherche des médicaments ayant des précautions d'emploi pour la grossesse.
     *
     * @return Une liste de médicaments ayant des recommandations pour la grossesse
     */
    @Query("{ 'pregnancyRecommendations': { $exists: true, $ne: [] } }")
    List<Medication> findWithPregnancyPrecautions();

    /**
     * Recherche des médicaments à risque pour les personnes âgées.
     *
     * @return Une liste de médicaments potentiellement inappropriés chez les personnes âgées
     */
    @Query("{ $or: [ " +
           "{ 'contraindications': { $regex: 'personne âgée|sujet âgé', $options: 'i' } }, " +
           "{ 'warnings': { $regex: 'beers|stopp', $options: 'i' } } " +
           "] }")
    List<Medication> findRiskyMedicationsForElderly();

    /**
     * Compte le nombre de médicaments appartenant à une classe thérapeutique (code ATC débutant par).
     *
     * @param atcPrefix Le préfixe du code ATC définissant la classe
     * @return Le nombre de médicaments dans cette classe thérapeutique
     */
    long countByAtcCodeStartingWith(String atcPrefix);

    /**
     * Recherche des médicaments pour une voie d'administration spécifique.
     *
     * @param route La voie d'administration
     * @return Une liste de médicaments administrés par cette voie
     */
    List<Medication> findByRoute(String route);

    /**
     * Recherche des médicaments soumis à prescription médicale.
     *
     * @return Une liste de médicaments nécessitant une prescription
     */
    List<Medication> findByPrescriptionRequiredTrue();

    /**
     * Recherche des médicaments remboursés.
     *
     * @return Une liste de médicaments remboursés
     */
    List<Medication> findByReimbursedTrue();

    /**
     * Recherche des médicaments avec un taux de remboursement supérieur à la valeur spécifiée.
     *
     * @param rate Le taux de remboursement minimum
     * @return Une liste de médicaments remboursés au-dessus de ce taux
     */
    List<Medication> findByReimbursedTrueAndReimbursementRateGreaterThanEqual(float rate);
}

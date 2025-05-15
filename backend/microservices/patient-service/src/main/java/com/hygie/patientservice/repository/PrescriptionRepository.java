package com.hygie.patientservice.repository;

import com.hygie.patientservice.model.Prescription;
import com.hygie.patientservice.model.Prescription.PrescriptionStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Repository pour l'accès aux données des prescriptions dans MongoDB.
 *
 * Fournit des méthodes pour interagir avec les documents Prescription dans la collection
 * prescriptions et implémente des requêtes spécifiques pour les besoins du Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {

    /**
     * Recherche toutes les prescriptions pour un patient spécifique.
     *
     * @param patientId L'identifiant du patient
     * @return Une liste des prescriptions du patient
     */
    List<Prescription> findByPatientId(String patientId);

    /**
     * Recherche les prescriptions actives pour un patient spécifique.
     *
     * @param patientId L'identifiant du patient
     * @param status Le statut de prescription recherché
     * @return Une liste des prescriptions actives du patient
     */
    List<Prescription> findByPatientIdAndStatus(String patientId, PrescriptionStatus status);

    /**
     * Recherche les prescriptions pour un patient qui ne sont pas expirées.
     *
     * @param patientId L'identifiant du patient
     * @param currentDate La date actuelle servant de référence
     * @return Une liste des prescriptions non expirées
     */
    List<Prescription> findByPatientIdAndExpirationDateGreaterThanEqual(String patientId, LocalDate currentDate);

    /**
     * Recherche les prescriptions par prescripteur.
     *
     * @param prescriberId L'identifiant du prescripteur
     * @return Une liste des prescriptions émises par ce prescripteur
     */
    List<Prescription> findByPrescriberId(String prescriberId);

    /**
     * Recherche les prescriptions par spécialité de prescripteur.
     *
     * @param specialty La spécialité du prescripteur
     * @return Une liste des prescriptions émises par cette spécialité
     */
    List<Prescription> findByPrescriberSpecialty(String specialty);

    /**
     * Recherche les prescriptions contenant un médicament spécifique.
     *
     * @param patientId L'identifiant du patient
     * @param medicationId L'identifiant du médicament
     * @return Une liste des prescriptions contenant ce médicament
     */
    @Query("{ 'patientId': ?0, 'prescriptionItems.medicationId': ?1 }")
    List<Prescription> findByPatientIdAndMedicationId(String patientId, String medicationId);

    /**
     * Recherche les prescriptions émises dans un intervalle de dates.
     *
     * @param startDate La date de début de l'intervalle
     * @param endDate La date de fin de l'intervalle
     * @return Une liste des prescriptions dans cet intervalle
     */
    List<Prescription> findByPrescriptionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Recherche les prescriptions pour un patient avec un statut renouvelable.
     *
     * @param patientId L'identifiant du patient
     * @return Une liste des prescriptions renouvelables
     */
    List<Prescription> findByPatientIdAndRenewalTrue(String patientId);

    /**
     * Compte le nombre de prescriptions pour un patient.
     *
     * @param patientId L'identifiant du patient
     * @return Le nombre de prescriptions
     */
    long countByPatientId(String patientId);

    /**
     * Recherche les prescriptions qui expirent prochainement.
     *
     * @param startDate La date de début de la période à vérifier
     * @param endDate La date de fin de la période
     * @param status Le statut des prescriptions à vérifier
     * @return Une liste des prescriptions qui expirent dans cette période
     */
    List<Prescription> findByExpirationDateBetweenAndStatus(
            LocalDate startDate, LocalDate endDate, PrescriptionStatus status);

    /**
     * Recherche les dernières prescriptions d'un patient, triées par date de prescription décroissante.
     *
     * @param patientId L'identifiant du patient
     * @return Une liste des prescriptions, les plus récentes en premier
     */
    List<Prescription> findByPatientIdOrderByPrescriptionDateDesc(String patientId);

    /**
     * Recherche les prescriptions dispensées par une pharmacie spécifique.
     *
     * @param pharmacyId L'identifiant de la pharmacie
     * @return Une liste des prescriptions dispensées par cette pharmacie
     */
    List<Prescription> findByPharmacyId(String pharmacyId);

    /**
     * Recherche les prescriptions dispensées dans un intervalle de dates.
     *
     * @param startDate La date de début de l'intervalle
     * @param endDate La date de fin de l'intervalle
     * @return Une liste des prescriptions dispensées dans cet intervalle
     */
    List<Prescription> findByDispensingDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Supprime les prescriptions expirées antérieures à une date.
     *
     * @param date La date limite
     * @param status Le statut des prescriptions à supprimer
     * @return Le nombre de prescriptions supprimées
     */
    long deleteByExpirationDateBeforeAndStatus(LocalDate date, PrescriptionStatus status);
}

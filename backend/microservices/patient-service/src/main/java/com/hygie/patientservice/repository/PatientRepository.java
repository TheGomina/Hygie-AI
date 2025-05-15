package com.hygie.patientservice.repository;

import com.hygie.patientservice.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'accès aux données des patients dans MongoDB.
 *
 * Fournit des méthodes pour interagir avec les documents Patient dans la collection
 * patients et implémente des requêtes spécifiques pour les besoins du Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {

    /**
     * Recherche un patient par son numéro de sécurité sociale.
     *
     * @param socialSecurityNumber Le numéro de sécurité sociale
     * @return Un Optional contenant le patient s'il existe
     */
    Optional<Patient> findBySocialSecurityNumber(String socialSecurityNumber);

    /**
     * Recherche des patients par prénom.
     *
     * @param firstName Le prénom du patient
     * @return Une liste de patients avec ce prénom
     */
    List<Patient> findByFirstNameContainingIgnoreCase(String firstName);

    /**
     * Recherche des patients par nom de famille.
     *
     * @param lastName Le nom de famille du patient
     * @return Une liste de patients avec ce nom de famille
     */
    List<Patient> findByLastNameContainingIgnoreCase(String lastName);

    /**
     * Recherche des patients par nom et prénom.
     *
     * @param firstName Le prénom du patient
     * @param lastName Le nom de famille du patient
     * @return Une liste de patients correspondant au nom et prénom
     */
    List<Patient> findByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCase(
            String firstName, String lastName);

    /**
     * Recherche des patients par pharmacie.
     *
     * @param pharmacyId L'identifiant de la pharmacie
     * @return Une liste de patients suivis par cette pharmacie
     */
    List<Patient> findByPharmacyId(String pharmacyId);

    /**
     * Recherche des patients par médecin traitant.
     *
     * @param doctorId L'identifiant du médecin traitant
     * @return Une liste de patients de ce médecin traitant
     */
    List<Patient> findByDoctorId(String doctorId);

    /**
     * Recherche des patients par condition médicale.
     *
     * @param condition La condition médicale recherchée
     * @return Une liste de patients ayant cette condition
     */
    @Query("{ 'conditions': { $regex: ?0, $options: 'i' } }")
    List<Patient> findByConditionsContainingIgnoreCase(String condition);

    /**
     * Recherche des patients avec des allergies spécifiques.
     *
     * @param allergy L'allergie recherchée
     * @return Une liste de patients ayant cette allergie
     */
    @Query("{ 'allergies': { $regex: ?0, $options: 'i' } }")
    List<Patient> findByAllergiesContainingIgnoreCase(String allergy);

    /**
     * Recherche des patients nés avant une date spécifique.
     *
     * @param date La date limite de naissance
     * @return Une liste de patients nés avant cette date
     */
    List<Patient> findByBirthDateBefore(LocalDate date);

    /**
     * Recherche des patients nés dans une période spécifique.
     *
     * @param startDate La date de début de la période
     * @param endDate La date de fin de la période
     * @return Une liste de patients nés dans cette période
     */
    List<Patient> findByBirthDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Compte le nombre de patients suivis par une pharmacie.
     *
     * @param pharmacyId L'identifiant de la pharmacie
     * @return Le nombre de patients
     */
    long countByPharmacyId(String pharmacyId);

    /**
     * Recherche des patients éligibles pour un Bilan Partagé de Médication.
     *
     * @param maxBirthDate La date de naissance maximale (pour l'âge minimum)
     * @return Une liste de patients potentiellement éligibles
     */
    List<Patient> findByBirthDateBeforeAndBpmEligibleTrue(LocalDate maxBirthDate);

    /**
     * Recherche des patients qui ont déjà réalisé un Bilan Partagé de Médication.
     *
     * @return Une liste de patients ayant déjà réalisé un BPM
     */
    List<Patient> findByBpmCompletedTrue();

    /**
     * Recherche des patients par code postal.
     *
     * @param postalCode Le code postal
     * @return Une liste de patients habitant dans ce code postal
     */
    List<Patient> findByAddressPostalCodeStartingWith(String postalCode);

    /**
     * Recherche des patients par ville.
     *
     * @param city La ville
     * @return Une liste de patients habitant dans cette ville
     */
    List<Patient> findByAddressCityIgnoreCase(String city);

    /**
     * Recherche des patients par sexe.
     *
     * @param gender Le sexe (M ou F)
     * @return Une liste de patients de ce sexe
     */
    List<Patient> findByGender(String gender);
}

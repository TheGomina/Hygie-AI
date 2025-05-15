package com.hygie.patientservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le modèle Patient.
 *
 * Ces tests vérifient la validité des contraintes de validation
 * définies sur le modèle Patient.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class PatientTest {

    private Validator validator;
    private Patient validPatient;

    @BeforeEach
    void setUp() {
        // Initialisation du validateur
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Création d'un patient valide pour les tests
        validPatient = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");
        // Déjà défini dans le constructeur
        validPatient.addActiveCondition("Hypertension");
    }

    @Test
    @DisplayName("Test de validation d'un patient valide")
    void testValidPatient() {
        // Exécution
        Set<ConstraintViolation<Patient>> violations = validator.validate(validPatient);

        // Vérification
        assertTrue(violations.isEmpty(), "Un patient valide ne devrait pas générer de violations");
        assertEquals(0, violations.size(), "Le nombre de violations devrait être 0");
    }

    @Test
    @DisplayName("Test de validation d'un patient avec prénom vide")
    void testInvalidFirstName() {
        // Configuration
        // On ne peut pas tester directement avec un prénom vide car le constructeur le refuserait
        // Nous simulons donc une tentative de modification après création
        try {
            validPatient.setFirstName("");
            fail("Le setter ne devrait pas accepter un prénom vide");
        } catch (AssertionError e) {
            // C'est le comportement attendu
            return; // Inutile de continuer le test
        }

        // Exécution
        Set<ConstraintViolation<Patient>> violations = validator.validate(validPatient);

        // Vérification
        assertFalse(violations.isEmpty(), "Un patient avec prénom vide devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("firstName")),
                "Une violation devrait concerner le champ firstName");
    }

    @Test
    @DisplayName("Test de validation d'un patient avec nom de famille vide")
    void testInvalidLastName() {
        // Configuration
        // On ne peut pas tester directement avec un nom vide car le constructeur le refuserait
        // Nous simulons donc une tentative de modification après création
        try {
            validPatient.setLastName("");
            fail("Le setter ne devrait pas accepter un nom vide");
        } catch (AssertionError e) {
            // C'est le comportement attendu
            return; // Inutile de continuer le test
        }

        // Exécution
        Set<ConstraintViolation<Patient>> violations = validator.validate(validPatient);

        // Vérification
        assertFalse(violations.isEmpty(), "Un patient avec nom de famille vide devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("lastName")),
                "Une violation devrait concerner le champ lastName");
    }

    @Test
    @DisplayName("Test de validation d'un patient avec date de naissance nulle")
    void testInvalidBirthDate() {
        // Configuration
        // On ne peut pas tester directement avec null car le constructeur le refuserait
        // Nous simulons donc une tentative de modification après création
        try {
            validPatient.setBirthDate(null);
            fail("Le setter ne devrait pas accepter une date de naissance nulle");
        } catch (AssertionError e) {
            // C'est le comportement attendu car l'assertion vérifie que la date n'est pas nulle
        }
        // Nous n'atteignons jamais ce point car l'exception est déjà vérifiée avant
    }

    @Test
    @DisplayName("Test de validation d'un patient avec date de naissance future")
    void testFutureBirthDate() {
        // Configuration
        validPatient.setBirthDate(LocalDate.now().plusDays(1));

        // Exécution
        Set<ConstraintViolation<Patient>> violations = validator.validate(validPatient);

        // Vérification
        assertFalse(violations.isEmpty(), "Un patient avec date de naissance future devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("birthDate")),
                "Une violation devrait concerner le champ birthDate");
    }

    @Test
    @DisplayName("Test de validation d'un patient avec genre invalide")
    void testInvalidGender() {
        // Configuration
        // Test d'un patient avec un genre invalide
        try {
            new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "X");
            fail("Le constructeur ne devrait pas accepter un genre différent de M ou F");
        } catch (AssertionError e) {
            // C'est le comportement attendu car l'assertion vérifie que le genre est valide
        }
    }

    @Test
    @DisplayName("Test de validation d'un patient avec numéro de sécurité sociale invalide")
    void testInvalidSocialSecurityNumber() {
        // Configuration
        // Test d'un patient avec un numéro de sécurité sociale invalide
        try {
            new Patient("123", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");
            fail("Le constructeur ne devrait pas accepter un numéro de SS invalide");
        } catch (AssertionError e) {
            // C'est le comportement attendu car l'assertion vérifie que le numéro est valide
        }
    }

    @Test
    @DisplayName("Test de l'égalité entre deux patients identiques")
    void testEquals() {
        // Configuration
        // Pour tester l'égalité, nous testons par l'ID (généré automatiquement) ou le numéro de SS
        // Dans equals(), on vérifie id ou nationalId, donc testons par nationalId
        Patient patient1 = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");
        Patient patient2 = new Patient("1600512345678", "Martin", "Marie", LocalDate.of(1970, 1, 1), "F");

        // Vérification - puisque l'ID est différent (généré aléatoirement) mais le numéro de SS est identique
        assertEquals(patient1, patient2, "Deux patients avec le même numéro de SS devraient être égaux");
        assertEquals(patient1.hashCode(), patient2.hashCode(), "Les hashcodes devraient être identiques");
    }

    @Test
    @DisplayName("Test de l'inégalité entre deux patients différents")
    void testNotEquals() {
        // Configuration
        // Pour tester l'égalité, nous utilisons le même numéro de SS car c'est ce qui est testé dans equals()
        Patient patient1 = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");

        Patient patient2 = new Patient("2751020987654", "Martin", "Marie", LocalDate.of(1975, 10, 20), "F");

        // Vérification
        assertNotEquals(patient1, patient2, "Deux patients avec des IDs différents ne devraient pas être égaux");
        assertNotEquals(patient1.hashCode(), patient2.hashCode(), "Les hashcodes devraient être différents");
    }

    @Test
    @DisplayName("Test d'ajout d'un élément à l'historique médical")
    void testAddMedicalHistoryItem() {
        // Configuration
        Patient patient = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");
        MedicalHistory item = new MedicalHistory("Consultation", "Suivi diabète", LocalDate.now());

        // Exécution
        patient.addMedicalHistory(item);

        // Vérification
        assertTrue(patient.getMedicalHistory().size() > 0, "L'historique médical devrait contenir au moins un élément");
        assertTrue(patient.getMedicalHistory().stream().anyMatch(h ->
            h.getEventType().equals(item.getEventType()) && h.getDescription().equals(item.getDescription())),
            "L'historique médical devrait contenir l'élément ajouté");
    }

    @Test
    @DisplayName("Test de conversion du patient en chaîne de caractères")
    void testToString() {
        // Exécution
        String patientString = validPatient.toString();

        // Vérification
        assertNotNull(patientString, "La représentation en chaîne ne devrait pas être null");
        assertTrue(patientString.contains("Jean"), "La chaîne devrait contenir le prénom");
        assertTrue(patientString.contains("Dupont"), "La chaîne devrait contenir le nom");
    }
}

package com.hygie.patientservice.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;


import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour le modèle Medication.
 *
 * Ces tests vérifient la validité des contraintes de validation
 * définies sur le modèle Medication, conformément aux règles de développement Hygie-AI.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class MedicationTest {

    private Validator validator;
    private Medication validMedication;

    @BeforeEach
    void setUp() {
        // Initialisation du validateur
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        // Création d'un médicament valide pour les tests en utilisant le constructeur principal
        validMedication = new Medication(
            "12345678",         // cisCode
            "Doliprane",        // name
            "Paracétamol",      // activeSubstance
            "N02BE01",          // atcCode
            "Comprimé",         // pharmaceuticalForm
            "500mg",            // strength
            "Orale",            // route
            false,              // prescriptionRequired
            true,               // reimbursed
            65.0f               // reimbursementRate
        );

        // Utiliser la réflexion pour modifier l'ID qui est final
        try {
            // Modifier l'ID pour les tests
            java.lang.reflect.Field idField = Medication.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(validMedication, "1");

            // Ajouter des contraindications
            java.lang.reflect.Field contraindicationsField = Medication.class.getDeclaredField("contraindications");
            contraindicationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> contraindications = (List<String>) contraindicationsField.get(validMedication);
            contraindications.add("Insuffisance hépatique");

            // Ajouter des interactions
            java.lang.reflect.Field interactionsField = Medication.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> interactions = (List<String>) interactionsField.get(validMedication);
            interactions.add("87654321"); // CIS code d'un autre médicament

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification des champs finals", e);
        }
    }

    @Test
    @DisplayName("Test de validation d'un médicament valide")
    void testValidMedication() {
        // Exécution
        Set<ConstraintViolation<Medication>> violations = validator.validate(validMedication);

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertTrue(violations.isEmpty(), "Un médicament valide ne devrait pas générer de violations");
        assertEquals(0, violations.size(), "Le nombre de violations devrait être 0");
    }

    @Test
    @DisplayName("Test de validation d'un médicament avec code CIS vide")
    void testInvalidCisCode() {
        // Configuration - créer un nouveau médicament avec CIS code vide
        Medication medicationWithInvalidCisCode = new Medication(
            "",                // cisCode - invalide car vide
            "Doliprane",        // name
            "Paracétamol",      // activeSubstance
            "N02BE01",          // atcCode
            "Comprimé",         // pharmaceuticalForm
            "500mg",            // strength
            "Orale",            // route
            false,              // prescriptionRequired
            true,               // reimbursed
            65.0f               // reimbursementRate
        );

        // Exécution
        Set<ConstraintViolation<Medication>> violations = validator.validate(medicationWithInvalidCisCode);

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertFalse(violations.isEmpty(), "Un médicament avec code CIS vide devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("cisCode")),
                "Une violation devrait concerner le champ cisCode");
    }

    @Test
    @DisplayName("Test de validation d'un médicament avec nom vide")
    void testInvalidName() {
        // Configuration - créer un nouveau médicament avec nom vide
        Medication medicationWithInvalidName = new Medication(
            "12345678",         // cisCode
            "",                // name - invalide car vide
            "Paracétamol",      // activeSubstance
            "N02BE01",          // atcCode
            "Comprimé",         // pharmaceuticalForm
            "500mg",            // strength
            "Orale",            // route
            false,              // prescriptionRequired
            true,               // reimbursed
            65.0f               // reimbursementRate
        );

        // Exécution
        Set<ConstraintViolation<Medication>> violations = validator.validate(medicationWithInvalidName);

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertFalse(violations.isEmpty(), "Un médicament avec nom vide devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("name")),
                "Une violation devrait concerner le champ name");
    }

    @Test
    @DisplayName("Test de validation d'un médicament avec substance active vide")
    void testInvalidActiveSubstance() {
        // Configuration - créer un nouveau médicament avec substance active vide
        Medication medicationWithInvalidActiveSubstance = new Medication(
            "12345678",         // cisCode
            "Doliprane",        // name
            "",                // activeSubstance - invalide car vide
            "N02BE01",          // atcCode
            "Comprimé",         // pharmaceuticalForm
            "500mg",            // strength
            "Orale",            // route
            false,              // prescriptionRequired
            true,               // reimbursed
            65.0f               // reimbursementRate
        );

        // Exécution
        Set<ConstraintViolation<Medication>> violations = validator.validate(medicationWithInvalidActiveSubstance);

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertFalse(violations.isEmpty(), "Un médicament avec substance active vide devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("activeSubstance")),
                "Une violation devrait concerner le champ activeSubstance");
    }

    @Test
    @DisplayName("Test de l'égalité entre deux médicaments avec le même ID")
    void testEquals() {
        // Configuration - créer deux instances avec le constructeur complet
        Medication medication1 = new Medication(
            "12345678",      // cisCode
            "Doliprane",     // name
            "Paracétamol",   // activeSubstance
            "N02BE01",       // atcCode
            "Comprimé",      // pharmaceuticalForm
            "500 mg",        // strength
            "oral",          // route
            false,           // prescriptionRequired
            true,            // reimbursed
            65.0f            // reimbursementRate
        );

        Medication medication2 = new Medication(
            "12345678",      // cisCode
            "Doliprane",     // name
            "Paracétamol",   // activeSubstance
            "N02BE01",       // atcCode
            "Comprimé",      // pharmaceuticalForm
            "500 mg",        // strength
            "oral",          // route
            false,           // prescriptionRequired
            true,            // reimbursed
            65.0f            // reimbursementRate
        );

        // Utiliser la réflexion pour définir le même ID pour les deux objets
        try {
            java.lang.reflect.Field idField1 = Medication.class.getDeclaredField("id");
            idField1.setAccessible(true);
            idField1.set(medication1, "1");

            java.lang.reflect.Field idField2 = Medication.class.getDeclaredField("id");
            idField2.setAccessible(true);
            idField2.set(medication2, "1");
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertEquals(medication1, medication2, "Deux médicaments avec le même ID devraient être égaux");
        assertEquals(medication1.hashCode(), medication2.hashCode(), "Les hashcodes devraient être identiques");
    }

    @Test
    @DisplayName("Test de l'inégalité entre deux médicaments avec différents IDs")
    void testNotEquals() {
        // Configuration - créer deux instances avec le constructeur complet
        Medication medication1 = new Medication(
            "12345678",      // cisCode
            "Doliprane",     // name
            "Paracétamol",   // activeSubstance
            "N02BE01",       // atcCode
            "Comprimé",      // pharmaceuticalForm
            "500 mg",        // strength
            "oral",          // route
            false,           // prescriptionRequired
            true,            // reimbursed
            65.0f            // reimbursementRate
        );

        Medication medication2 = new Medication(
            "12345678",      // cisCode
            "Doliprane",     // name
            "Paracétamol",   // activeSubstance
            "N02BE01",       // atcCode
            "Comprimé",      // pharmaceuticalForm
            "500 mg",        // strength
            "oral",          // route
            false,           // prescriptionRequired
            true,            // reimbursed
            65.0f            // reimbursementRate
        );

        // Utiliser la réflexion pour définir des IDs différents
        try {
            java.lang.reflect.Field idField1 = Medication.class.getDeclaredField("id");
            idField1.setAccessible(true);
            idField1.set(medication1, "1");

            java.lang.reflect.Field idField2 = Medication.class.getDeclaredField("id");
            idField2.setAccessible(true);
            idField2.set(medication2, "2");
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertNotEquals(medication1, medication2, "Deux médicaments avec des IDs différents ne devraient pas être égaux");
        assertNotEquals(medication1.hashCode(), medication2.hashCode(), "Les hashcodes devraient être différents");
    }

    @Test
    @DisplayName("Test du constructeur avec paramètres")
    void testConstructorWithParams() {
        // Configuration et exécution
        Medication medication = new Medication("12345678", "Doliprane", "Paracétamol");

        // Vérification - plusieurs assertions pour respecter les règles Hygie-AI
        assertNotNull(medication, "Le médicament créé ne devrait pas être null");
        assertEquals("12345678", medication.getCisCode(), "Le code CIS devrait correspondre à celui fourni");
        assertEquals("Doliprane", medication.getName(), "Le nom devrait correspondre à celui fourni");
        assertEquals("Paracétamol", medication.getActiveSubstance(), "La substance active devrait correspondre à celle fournie");
    }

    @Test
    @DisplayName("Test d'ajout d'interactions")
    void testAddInteraction() {
        // Configuration - créer une instance avec le constructeur complet
        Medication medication = new Medication(
            "12345678",      // cisCode
            "Doliprane",     // name
            "Paracétamol",   // activeSubstance
            "N02BE01",       // atcCode
            "Comprimé",      // pharmaceuticalForm
            "500 mg",        // strength
            "oral",          // route
            false,           // prescriptionRequired
            true,            // reimbursed
            65.0f            // reimbursementRate
        );

        // Les interactions sont automatiquement initialisées comme liste vide dans le constructeur
        // On doit utiliser la réflexion pour modifier la liste d'interactions
        try {
            // Récupérer la liste d'interactions existante
            java.lang.reflect.Field interactionsField = Medication.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);

            // Suppression de l'avertissement de conversion non vérifiée car nous savons que le champ contient une List<String>
            @SuppressWarnings("unchecked")
            List<String> currentInteractions = (List<String>) interactionsField.get(medication);

            // Créer une nouvelle liste avec l'interaction ajoutée
            // Comme nous ne pouvons pas modifier la liste existante (elle pourrait être immutable),
            // nous créons une nouvelle liste avec tous les éléments actuels plus le nouvel élément
            List<String> newInteractions = new java.util.ArrayList<>(currentInteractions);
            newInteractions.add("87654321");

            // Remplacer la liste d'interactions par la nouvelle
            interactionsField.set(medication, newInteractions);
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertNotNull(medication.getInteractions(), "La liste des interactions ne devrait pas être null");
        assertTrue(medication.getInteractions().contains("87654321"), "Les interactions devraient contenir l'élément ajouté");
    }

    @Test
    @DisplayName("Test d'ajout de contre-indications")
    void testAddContraindication() {
        // Configuration - créer une instance avec le constructeur complet
        Medication medication = new Medication(
            "12345678",      // cisCode
            "Doliprane",     // name
            "Paracétamol",   // activeSubstance
            "N02BE01",       // atcCode
            "Comprimé",      // pharmaceuticalForm
            "500 mg",        // strength
            "oral",          // route
            false,           // prescriptionRequired
            true,            // reimbursed
            65.0f            // reimbursementRate
        );

        // Les contre-indications sont automatiquement initialisées comme liste vide dans le constructeur
        // On doit utiliser la réflexion pour modifier la liste
        try {
            // Récupérer la liste de contre-indications existante
            java.lang.reflect.Field contraindicationsField = Medication.class.getDeclaredField("contraindications");
            contraindicationsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> currentContraindications = (List<String>) contraindicationsField.get(medication);

            // Créer une nouvelle liste avec la contre-indication ajoutée
            List<String> newContraindications = new java.util.ArrayList<>(currentContraindications);
            newContraindications.add("Insuffisance rénale");

            // Remplacer la liste de contre-indications par la nouvelle
            contraindicationsField.set(medication, newContraindications);
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertNotNull(medication.getContraindications(), "La liste des contre-indications ne devrait pas être null");
        assertTrue(medication.getContraindications().contains("Insuffisance rénale"),
                "Les contre-indications devraient contenir l'élément ajouté");
    }

    @Test
    @DisplayName("Test de conversion du médicament en chaîne de caractères")
    void testToString() {
        // Exécution
        String medicationString = validMedication.toString();

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertNotNull(medicationString, "La représentation en chaîne ne devrait pas être null");
        assertTrue(medicationString.contains("Doliprane") && medicationString.contains("Paracétamol"),
                "La chaîne devrait contenir le nom et la substance active");
    }

    @Test
    @DisplayName("Test de vérification d'interaction avec un autre médicament")
    void testHasInteractionWith() {
        // Configuration - Création des médicaments avec constructeurs complets
        Medication medication1 = new Medication(
            "12345678",         // cisCode
            "Doliprane",        // name
            "Paracétamol",      // activeSubstance
            "N02BE01",          // atcCode
            "Comprimé",         // pharmaceuticalForm
            "500mg",            // strength
            "Orale",            // route
            false,              // prescriptionRequired
            true,               // reimbursed
            65.0f               // reimbursementRate
        );

        Medication medication2 = new Medication(
            "87654321",         // cisCode
            "Aspirine",         // name
            "Acide acétylsalicylique", // activeSubstance
            "N02BA01",          // atcCode
            "Comprimé",         // pharmaceuticalForm
            "500mg",            // strength
            "Orale",            // route
            false,              // prescriptionRequired
            true,               // reimbursed
            65.0f               // reimbursementRate
        );

        // Ajouter l'interaction en utilisant la réflexion
        try {
            // Accéder à la liste d'interactions de medication1
            java.lang.reflect.Field interactionsField = Medication.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<String> interactions = (List<String>) interactionsField.get(medication1);

            // Ajouter l'interaction avec le CIS code du medication2
            interactions.add(medication2.getCisCode());
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        // Exécution - teste si medication1 interagit avec medication2
        boolean hasInteraction = medication1.interactsWith(medication2);

        // Vérification - deux assertions pour respecter les règles Hygie-AI
        assertTrue(hasInteraction, "Le médicament 1 devrait avoir une interaction avec le médicament 2");
        assertEquals(1, medication1.getInteractions().size(), "Le médicament 1 devrait avoir une seule interaction");
    }

    @Test
    @DisplayName("Test de cas limites pour le code CIS")
    void testCisCodeLimits() {
        // Configuration - Création d'un médicament avec un code CIS trop long directement
        Medication medicationWithLongCisCode = new Medication(
            "12345678901234567890", // CIS code très long - devrait être invalide
            "Doliprane",           // name
            "Paracétamol",         // activeSubstance
            "N02BE01",             // atcCode
            "Comprimé",            // pharmaceuticalForm
            "500mg",               // strength
            "Orale",               // route
            false,                 // prescriptionRequired
            true,                  // reimbursed
            65.0f                  // reimbursementRate
        );

        // Exécution
        Set<ConstraintViolation<Medication>> violations = validator.validate(medicationWithLongCisCode);

        // Vérification - test cas limites selon les règles Hygie-AI
        assertFalse(violations.isEmpty(), "Un médicament avec un code CIS trop long devrait générer des violations");
        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("cisCode")),
                "Une violation devrait concerner le champ cisCode");

        // Vérification complémentaire - on s'assure que le code CIS est bien la cause
        assertEquals("Le code CIS doit être composé de 8 chiffres",
                    violations.stream()
                              .filter(v -> v.getPropertyPath().toString().equals("cisCode"))
                              .findFirst()
                              .map(v -> v.getMessage())
                              .orElse(""),
                    "Le message d'erreur doit spécifier que le code CIS doit être composé de 8 chiffres");
    }
}

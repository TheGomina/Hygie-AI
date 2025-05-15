package com.hygie.patientservice.service;

import com.hygie.patientservice.model.Prescription;
import com.hygie.patientservice.model.PrescriptionItem;
import com.hygie.patientservice.model.Prescription.PrescriptionStatus;
import com.hygie.patientservice.repository.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service de gestion des prescriptions.
 *
 * Ces tests vérifient le bon fonctionnement des méthodes du service
 * en simulant les interactions avec le repository et les services dépendants.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class PrescriptionServiceTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private MedicationService medicationService;

    @InjectMocks
    private PrescriptionService prescriptionService;

    private Prescription prescription1;
    private Prescription prescription2;
    private PrescriptionItem item1;
    private PrescriptionItem item2;

    @BeforeEach
    void setUp() {
        // Création des items de prescription de test
        item1 = new PrescriptionItem(
            "med1",                    // medicationId
            "Doliprane",              // medicationName
            "500 mg",                 // dosage
            "oral",                   // route
            "3 fois par jour",        // frequency
            10,                       // durationDays
            "Prendre pendant les repas", // instructions
            false,                    // asNeeded
            20,                       // quantityPrescribed
            "comprimés",              // unit
            true                      // substitutionAllowed
        );

        item2 = new PrescriptionItem(
            "med2",                    // medicationId
            "Ibuprofène",              // medicationName
            "200 mg",                 // dosage
            "oral",                   // route
            "2 fois par jour",        // frequency
            7,                        // durationDays
            "Prendre après les repas", // instructions
            false,                    // asNeeded
            14,                       // quantityPrescribed
            "comprimés",              // unit
            true                      // substitutionAllowed
        );

        // Simuler un ID spécifique pour les tests
        try {
            java.lang.reflect.Field idField1 = PrescriptionItem.class.getDeclaredField("id");
            idField1.setAccessible(true);
            idField1.set(item1, "item1");

            java.lang.reflect.Field idField2 = PrescriptionItem.class.getDeclaredField("id");
            idField2.setAccessible(true);
            idField2.set(item2, "item2");
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals des items: " + e.getMessage());
        }

        // Création des prescriptions de test avec le constructeur immutable
        prescription1 = new Prescription(
            "patient1",                // patientId
            "doctor1",                 // prescriberId
            "Médecin généraliste",     // prescriberSpecialty
            LocalDate.now(),           // prescriptionDate
            3,                         // validityPeriodMonths
            false,                     // isRenewal
            0                          // renewalNumber
        );

        prescription2 = new Prescription(
            "patient1",                // patientId
            "doctor2",                 // prescriberId
            "Cardiologue",             // prescriberSpecialty
            LocalDate.now().minusDays(30), // prescriptionDate
            6,                         // validityPeriodMonths
            true,                      // isRenewal
            1                          // renewalNumber
        );

        // Utiliser la réflexion pour définir les IDs et les items
        try {
            // Définir l'ID pour prescription1
            java.lang.reflect.Field idField1 = Prescription.class.getDeclaredField("id");
            idField1.setAccessible(true);
            idField1.set(prescription1, "prescription1");

            // Définir l'ID pour prescription2
            java.lang.reflect.Field idField2 = Prescription.class.getDeclaredField("id");
            idField2.setAccessible(true);
            idField2.set(prescription2, "prescription2");

            // Définir les items pour prescription1
            java.lang.reflect.Field itemsField1 = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField1.setAccessible(true);
            itemsField1.set(prescription1, new ArrayList<>(List.of(item1)));

            // Définir les items pour prescription2
            java.lang.reflect.Field itemsField2 = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField2.setAccessible(true);
            itemsField2.set(prescription2, new ArrayList<>(List.of(item2)));
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals des prescriptions: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("Test de récupération d'une prescription par ID")
    void testGetPrescriptionById() {
        // Configuration
        when(prescriptionRepository.findById("1")).thenReturn(Optional.of(prescription1));

        // Exécution
        Optional<Prescription> result = prescriptionService.getPrescriptionById("1");

        // Vérification
        assertTrue(result.isPresent(), "La prescription devrait être présente");
        assertEquals(prescription1, result.get(), "La prescription retournée devrait être prescription1");
        verify(prescriptionRepository, times(1)).findById("1");
    }

    @Test
    @DisplayName("Test de récupération d'une prescription inexistante par ID")
    void testGetPrescriptionByIdNotFound() {
        // Configuration
        when(prescriptionRepository.findById("999")).thenReturn(Optional.empty());

        // Exécution
        Optional<Prescription> result = prescriptionService.getPrescriptionById("999");

        // Vérification
        assertFalse(result.isPresent(), "La prescription ne devrait pas être trouvée");
        verify(prescriptionRepository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Test de sauvegarde d'une prescription")
    void testSavePrescription() {
        // Configuration
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription1);

        // Exécution
        Prescription savedPrescription = prescriptionService.savePrescription(prescription1);

        // Vérification
        assertNotNull(savedPrescription, "La prescription sauvegardée ne devrait pas être null");
        assertEquals("patient1", savedPrescription.getPatientId(), "L'ID du patient devrait être patient1");
        verify(prescriptionRepository, times(1)).save(prescription1);
    }

    @Test
    @DisplayName("Test de récupération des prescriptions d'un patient")
    void testGetPatientPrescriptions() {
        // Configuration
        List<Prescription> prescriptions = Arrays.asList(prescription1, prescription2);
        when(prescriptionRepository.findByPatientId("patient1")).thenReturn(prescriptions);

        // Exécution
        List<Prescription> results = prescriptionService.getPatientPrescriptions("patient1");

        // Vérification
        assertEquals(2, results.size(), "La liste devrait contenir deux prescriptions");
        assertTrue(results.contains(prescription1), "La liste devrait contenir prescription1");
        assertTrue(results.contains(prescription2), "La liste devrait contenir prescription2");
        verify(prescriptionRepository, times(1)).findByPatientId("patient1");
    }

    @Test
    @DisplayName("Test de récupération des prescriptions actives d'un patient")
    void testGetActivePrescriptions() {
        // Configuration
        List<Prescription> validPrescriptions = Arrays.asList(prescription1);
        when(prescriptionRepository.findByPatientIdAndExpirationDateGreaterThanEqual(
                eq("patient1"), any(LocalDate.class))).thenReturn(validPrescriptions);

        // Exécution
        List<Prescription> results = prescriptionService.getActivePrescriptions("patient1");

        // Vérification
        assertEquals(1, results.size(), "La liste devrait contenir une prescription");
        assertEquals(prescription1, results.get(0), "La prescription active devrait être prescription1");
    }

    @Test
    @DisplayName("Test de mise à jour du statut d'une prescription")
    void testUpdatePrescriptionStatus() {
        // Configuration
        when(prescriptionRepository.findById("1")).thenReturn(Optional.of(prescription1));

        // Définir le statut initial avec réflexion
        try {
            java.lang.reflect.Field statusField = Prescription.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(prescription1, PrescriptionStatus.ACTIVE);
        } catch (Exception e) {
            fail("Erreur lors de la définition du statut initial: " + e.getMessage());
        }

        // Créer une nouvelle prescription représentant la version mise à jour
        Prescription updatedPrescription = new Prescription(
            "patient1",                // patientId
            "doctor1",                 // prescriberId
            "Médecin généraliste",     // prescriberSpecialty
            LocalDate.now(),           // prescriptionDate
            3,                         // validityPeriodMonths
            false,                     // isRenewal
            0                          // renewalNumber
        );

        // Définir les propriétés de la prescription mise à jour avec réflexion
        try {
            // Définir l'ID
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPrescription, "1");

            // Définir les items
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            itemsField.set(updatedPrescription, new ArrayList<>(List.of(item1)));

            // Définir le statut mis à jour
            java.lang.reflect.Field statusField = Prescription.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(updatedPrescription, PrescriptionStatus.COMPLETED);

            // Définir la date d'expiration
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(updatedPrescription, LocalDate.now().plusMonths(3));
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(updatedPrescription);

        // Exécution
        Prescription result = prescriptionService.updatePrescriptionStatus("1", PrescriptionStatus.COMPLETED);

        // Vérification
        assertNotNull(result, "La prescription mise à jour ne devrait pas être null");
        assertEquals(PrescriptionStatus.COMPLETED, result.getStatus(),
                "Le statut devrait être mis à jour à COMPLETED");
        verify(prescriptionRepository, times(1)).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Test de mise à jour du statut d'une prescription inexistante")
    void testUpdatePrescriptionStatusNotFound() {
        // Configuration
        when(prescriptionRepository.findById("999")).thenReturn(Optional.empty());

        // Exécution
        Prescription result = prescriptionService.updatePrescriptionStatus("999", PrescriptionStatus.COMPLETED);

        // Vérification
        assertNull(result, "Le résultat devrait être null pour une prescription inexistante");
        verify(prescriptionRepository, never()).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Test de suppression d'une prescription")
    void testDeletePrescription() {
        // Configuration
        when(prescriptionRepository.existsById("1")).thenReturn(true).thenReturn(false);
        doNothing().when(prescriptionRepository).deleteById("1");

        // Exécution
        boolean result = prescriptionService.deletePrescription("1");

        // Vérification
        assertTrue(result, "La suppression devrait retourner true");
        verify(prescriptionRepository, times(1)).deleteById("1");
        verify(prescriptionRepository, times(2)).existsById("1");
    }

    @Test
    @DisplayName("Test de suppression d'une prescription inexistante")
    void testDeletePrescriptionNotFound() {
        // Configuration
        when(prescriptionRepository.existsById("999")).thenReturn(false);

        // Exécution
        boolean result = prescriptionService.deletePrescription("999");

        // Vérification
        assertFalse(result, "La suppression devrait retourner false pour une prescription inexistante");
        verify(prescriptionRepository, never()).deleteById("999");
    }

    @Test
    @DisplayName("Test de récupération des prescriptions qui expirent bientôt")
    void testGetExpiringPrescriptions() {
        // Configuration - Simuler une prescription qui expire bientôt
        LocalDate prescriptionDate = LocalDate.now().minusDays(20);
        LocalDate expirationDate = prescriptionDate.plusMonths(1); // Validité de 1 mois, donc expiration dans 10 jours

        Prescription expiringPrescription = new Prescription(
            "patient1",                // patientId
            "doctor1",                 // prescriberId
            "Médecin généraliste",     // prescriberSpecialty
            prescriptionDate,          // prescriptionDate
            1,                         // validityPeriodMonths
            false,                     // isRenewal
            0                          // renewalNumber
        );

        // Utiliser la réflexion pour définir les propriétés
        try {
            // Définir l'ID
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(expiringPrescription, "expiring1");

            // Définir les items
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            itemsField.set(expiringPrescription, new ArrayList<>(List.of(item1)));

            // Définir la date d'expiration
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(expiringPrescription, expirationDate);
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        List<Prescription> allPrescriptions = List.of(expiringPrescription);
        when(prescriptionRepository.findByPatientId("patient1")).thenReturn(allPrescriptions);

        // Exécution
        List<Prescription> results = prescriptionService.getExpiringPrescriptions("patient1", 15);

        // Vérification
        assertEquals(1, results.size(), "La liste devrait contenir une prescription");
        verify(prescriptionRepository, times(1)).findByPatientId("patient1");
    }

    @Test
    @DisplayName("Test de récupération des médicaments actuellement prescrits")
    void testGetCurrentMedications() {
        // Configuration
        List<Prescription> activePrescriptions = Arrays.asList(prescription1, prescription2);

        // Mock de la méthode getActivePrescriptions que nous venons de tester
        doReturn(activePrescriptions).when(prescriptionService).getActivePrescriptions("patient1");

        // Exécution
        List<PrescriptionItem> results = prescriptionService.getCurrentMedications("patient1");

        // Vérification
        assertEquals(2, results.size(), "La liste devrait contenir deux items de prescription");
        assertTrue(results.contains(item1), "La liste devrait contenir item1");
        assertTrue(results.contains(item2), "La liste devrait contenir item2");
    }

    @Test
    @DisplayName("Test d'ajout d'un item à une prescription existante")
    void testAddItemToPrescription() {
        // Configuration
        when(prescriptionRepository.findById("prescription1")).thenReturn(Optional.of(prescription1));

        // Créer un nouvel item à ajouter
        PrescriptionItem newItem = new PrescriptionItem(
            "med3",                    // medicationId
            "Aspirine",               // medicationName
            "100 mg",                 // dosage
            "oral",                   // route
            "1 fois par jour",        // frequency
            5,                        // durationDays
            "Prendre le matin",       // instructions
            false,                    // asNeeded
            5,                        // quantityPrescribed
            "comprimés",              // unit
            true                      // substitutionAllowed
        );

        // Simuler un ID spécifique pour le nouvel item
        try {
            java.lang.reflect.Field idField = PrescriptionItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(newItem, "item3");
        } catch (Exception e) {
            fail("Erreur lors de la définition de l'ID du nouvel item: " + e.getMessage());
        }

        // Créer la prescription attendue après l'ajout
        Prescription expectedUpdatedPrescription = new Prescription(
            "patient1",                // patientId
            "doctor1",                 // prescriberId
            "Médecin généraliste",     // prescriberSpecialty
            LocalDate.now(),           // prescriptionDate
            3,                         // validityPeriodMonths
            false,                     // isRenewal
            0                          // renewalNumber
        );

        // Préparer la prescription mise à jour attendue
        try {
            // Définir les mêmes propriétés que prescription1
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(expectedUpdatedPrescription, "prescription1");

            // Ajouter les items avec le nouvel item
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            List<PrescriptionItem> updatedItems = new ArrayList<>(List.of(item1, newItem));
            itemsField.set(expectedUpdatedPrescription, updatedItems);

            // Définir la date d'expiration
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(expectedUpdatedPrescription, LocalDate.now().plusMonths(3));
        } catch (Exception e) {
            fail("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(expectedUpdatedPrescription);

        // Exécution
        Prescription result = prescriptionService.addItemToPrescription("prescription1", newItem);

        // Vérification
        assertNotNull(result, "La prescription mise à jour ne devrait pas être null");
        assertEquals(2, result.getPrescriptionItems().size(), "La prescription devrait contenir 2 items après l'ajout");
        verify(prescriptionRepository).save(any(Prescription.class));
    }

    @Test
    @DisplayName("Test de suppression d'un item d'une prescription existante")
    void testRemoveItemFromPrescription() {
        // Configuration
        // Créer une prescription avec deux items pour le test
        Prescription multiItemPrescription = new Prescription(
            "patient1",                // patientId
            "doctor1",                 // prescriberId
            "Médecin généraliste",     // prescriberSpecialty
            LocalDate.now(),           // prescriptionDate
            3,                         // validityPeriodMonths
            false,                     // isRenewal
            0                          // renewalNumber
        );

        // Créer un deuxième item pour cette prescription
        PrescriptionItem extraItem = new PrescriptionItem(
            "med3",                    // medicationId
            "Aspirine",               // medicationName
            "100 mg",                 // dosage
            "oral",                   // route
            "1 fois par jour",        // frequency
            5,                        // durationDays
            "Prendre le matin",       // instructions
            false,                    // asNeeded
            5,                        // quantityPrescribed
            "comprimés",              // unit
            true                      // substitutionAllowed
        );

        // Définir les propriétés avec réflexion
        try {
            // Définir l'ID de la prescription
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(multiItemPrescription, "multi1");

            // Définir l'ID du deuxième item
            java.lang.reflect.Field itemIdField = PrescriptionItem.class.getDeclaredField("id");
            itemIdField.setAccessible(true);
            itemIdField.set(extraItem, "item3");

            // Ajouter les deux items à la prescription
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            List<PrescriptionItem> twoItems = new ArrayList<>(List.of(item1, extraItem));
            itemsField.set(multiItemPrescription, twoItems);

            // Définir la date d'expiration
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(multiItemPrescription, LocalDate.now().plusMonths(3));
        } catch (Exception e) {
            fail("Erreur lors de la configuration de la prescription multi-items: " + e.getMessage());
        }

        when(prescriptionRepository.findById("multi1")).thenReturn(Optional.of(multiItemPrescription));

        // Créer la prescription attendue après la suppression
        Prescription expectedUpdatedPrescription = new Prescription(
            "patient1",                // patientId
            "doctor1",                 // prescriberId
            "Médecin généraliste",     // prescriberSpecialty
            LocalDate.now(),           // prescriptionDate
            3,                         // validityPeriodMonths
            false,                     // isRenewal
            0                          // renewalNumber
        );

        // Préparer la prescription mise à jour attendue (sans l'item3)
        try {
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(expectedUpdatedPrescription, "multi1");

            // Ne garder que le premier item
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            itemsField.set(expectedUpdatedPrescription, new ArrayList<>(List.of(item1)));

            // Définir la date d'expiration
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(expectedUpdatedPrescription, LocalDate.now().plusMonths(3));
        } catch (Exception e) {
            fail("Erreur lors de la préparation de la prescription sans l'item supprimé: " + e.getMessage());
        }

        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(expectedUpdatedPrescription);

        // Exécution
        Prescription result = prescriptionService.removeItemFromPrescription("multi1", "item3");

        // Vérification
        assertNotNull(result, "La prescription mise à jour ne devrait pas être null");
        assertEquals(1, result.getPrescriptionItems().size(), "La prescription devrait contenir 1 item après la suppression");
        assertTrue(result.getPrescriptionItems().contains(item1), "L'item1 devrait toujours être présent");
        verify(prescriptionRepository).save(any(Prescription.class));
    }
}

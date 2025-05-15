package com.hygie.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hygie.patientservice.model.Prescription;
import com.hygie.patientservice.model.PrescriptionItem;
import com.hygie.patientservice.service.PrescriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour le contrôleur de gestion des prescriptions.
 *
 * Ces tests vérifient le bon fonctionnement des endpoints REST
 * en simulant les requêtes HTTP et les réponses du service.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@WebMvcTest(PrescriptionController.class)
public class PrescriptionControllerTest {

    /**
     * Méthode d'aide pour créer une prescription de test avec l'identifiant spécifié.
     * Utilise la réflexion pour contourner l'immutabilité des champs pour les tests.
     */
    private Prescription createTestPrescription(String id, String patientId, String doctorId,
                                             LocalDate prescriptionDate, List<PrescriptionItem> items) {
        // Créer une instance de prescription avec le constructeur minimal requis
        Prescription prescription = new Prescription(
            patientId,                 // patientId
            doctorId,                  // prescriberId
            "Généraliste",            // prescriberSpecialty
            prescriptionDate,          // prescriptionDate
            3,                        // validityPeriodMonths (3 mois)
            false,                    // isRenewal
            0                         // renewalNumber
        );

        try {
            // Accéder au champ id et le modifier
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(prescription, id);

            // Accéder au champ prescriptionItems et le modifier
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            itemsField.set(prescription, items);

        } catch (Exception e) {
            // Pour les tests, on peut ignorer cette exception
            System.err.println("Erreur lors de la modification par réflexion: " + e.getMessage());
        }

        return prescription;
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PrescriptionService prescriptionService;

    private Prescription prescription1;
    private Prescription prescription2;
    private PrescriptionItem item1;
    private PrescriptionItem item2;

    @BeforeEach
    void setUp() {
        // Création des items de prescription de test
        // Utilisation du constructeur complet pour PrescriptionItem
        item1 = new PrescriptionItem(
            "med1",               // medicationId
            "Paracétamol 500mg",  // medicationName
            "500 mg",            // dosage
            "oral",              // route
            "2 fois par jour",    // frequency
            10,                   // durationDays
            "Prendre avec un verre d'eau", // instructions
            false,                // asNeeded
            20,                   // quantityPrescribed
            "comprimés",          // unit
            true                  // substitutionAllowed
        );
        // Simuler un ID spécifique pour les tests
        try {
            java.lang.reflect.Field idField = PrescriptionItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item1, "i1");
        } catch (Exception e) {
            // Pour les tests, on peut ignorer cette exception
        }

        item2 = new PrescriptionItem(
            "med2",               // medicationId
            "Amoxicilline 1g",    // medicationName
            "1 g",               // dosage
            "oral",              // route
            "1 fois par jour",    // frequency
            7,                   // durationDays
            "Prendre le matin à jeun",  // instructions
            false,                // asNeeded
            7,                   // quantityPrescribed
            "comprimés",          // unit
            false                 // substitutionAllowed
        );
        try {
            java.lang.reflect.Field idField = PrescriptionItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(item2, "i2");
        } catch (Exception e) {
            // Pour les tests, on peut ignorer cette exception
        }

        // Création des prescriptions de test avec le constructeur approprié
        // Utiliser la réflexion pour accéder aux champs privés pour les tests
        prescription1 = createTestPrescription("1", "p1", "d1", LocalDate.of(2025, 5, 1), new ArrayList<>(List.of(item1)));
        prescription2 = createTestPrescription("2", "p2", "d2", LocalDate.of(2025, 5, 5), new ArrayList<>(List.of(item2)));
    }

    @Test
    @DisplayName("Test de récupération de toutes les prescriptions")
    void testGetAllPrescriptions() throws Exception {
        // Configuration
        List<Prescription> prescriptions = Arrays.asList(prescription1, prescription2);
        when(prescriptionService.getAllPrescriptions()).thenReturn(prescriptions);

        // Exécution et vérification
        mockMvc.perform(get("/api/prescriptions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].patientId", is("p1")))
                .andExpect(jsonPath("$[0].prescriptionItems", hasSize(1)));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).getAllPrescriptions();
    }

    @Test
    @DisplayName("Test de récupération d'une prescription par ID")
    void testGetPrescriptionById() throws Exception {
        // Configuration
        when(prescriptionService.getPrescriptionById("1")).thenReturn(Optional.of(prescription1));

        // Exécution et vérification
        mockMvc.perform(get("/api/prescriptions/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId", is("p1")))
                .andExpect(jsonPath("$.items[0].dosage", is("1 comprimé matin et soir")));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).getPrescriptionById("1");
    }

    @Test
    @DisplayName("Test de récupération d'une prescription inexistante par ID")
    void testGetPrescriptionByIdNotFound() throws Exception {
        // Configuration
        when(prescriptionService.getPrescriptionById("999")).thenReturn(Optional.empty());

        // Exécution et vérification
        mockMvc.perform(get("/api/prescriptions/999"))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(prescriptionService, times(1)).getPrescriptionById("999");
    }

    @Test
    @DisplayName("Test de récupération des prescriptions pour un patient")
    void testGetPrescriptionsByPatientId() throws Exception {
        // Configuration
        when(prescriptionService.getPrescriptionsByPatientId("p1")).thenReturn(List.of(prescription1));

        // Exécution et vérification
        mockMvc.perform(get("/api/prescriptions/patient/p1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].patientId", is("p1")));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).getPrescriptionsByPatientId("p1");
    }

    @Test
    @DisplayName("Test de création d'une prescription")
    void testCreatePrescription() throws Exception {
        // Configuration
        when(prescriptionService.savePrescription(any(Prescription.class))).thenReturn(prescription1);

        // Exécution et vérification
        mockMvc.perform(post("/api/prescriptions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prescription1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.patientId", is("p1")))
                .andExpect(jsonPath("$.items", hasSize(1)));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).savePrescription(any(Prescription.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'une prescription")
    void testUpdatePrescription() throws Exception {
        // Configuration
        // Créer une prescription avec le constructeur immutable
        Prescription updatedPrescription = new Prescription(
            "p1",                                  // patientId
            "d3",                                  // prescriberId (modifié)
            "Cardiologue",                         // prescriberSpecialty
            LocalDate.of(2025, 5, 1),              // prescriptionDate
            3,                                      // validityPeriodMonths
            false,                                  // isRenewal
            0                                       // renewalNumber
        );

        // Utiliser la réflexion pour définir l'ID qui est final
        try {
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPrescription, "1");

            // Ajouter les items à la prescription
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            List<PrescriptionItem> items = new ArrayList<>(List.of(item1, item2));
            itemsField.set(updatedPrescription, items);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification des champs finals", e);
        }

        when(prescriptionService.updatePrescription(eq("1"), any(Prescription.class))).thenReturn(updatedPrescription);

        // Exécution et vérification
        mockMvc.perform(put("/api/prescriptions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPrescription)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.doctorId", is("d3")))
                .andExpect(jsonPath("$.items", hasSize(2)));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).updatePrescription(eq("1"), any(Prescription.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'une prescription inexistante")
    void testUpdatePrescriptionNotFound() throws Exception {
        // Configuration
        when(prescriptionService.updatePrescription(eq("999"), any(Prescription.class))).thenReturn(null);

        // Exécution et vérification
        mockMvc.perform(put("/api/prescriptions/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prescription1)))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(prescriptionService, times(1)).updatePrescription(eq("999"), any(Prescription.class));
    }

    @Test
    @DisplayName("Test de suppression d'une prescription")
    void testDeletePrescription() throws Exception {
        // Configuration
        when(prescriptionService.deletePrescription("1")).thenReturn(true);

        // Exécution et vérification
        mockMvc.perform(delete("/api/prescriptions/1"))
                .andExpect(status().isNoContent());

        // Vérification des appels au service
        verify(prescriptionService, times(1)).deletePrescription("1");
    }

    @Test
    @DisplayName("Test de suppression d'une prescription inexistante")
    void testDeletePrescriptionNotFound() throws Exception {
        // Configuration
        when(prescriptionService.deletePrescription("999")).thenReturn(false);

        // Exécution et vérification
        mockMvc.perform(delete("/api/prescriptions/999"))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(prescriptionService, times(1)).deletePrescription("999");
    }

    @Test
    @DisplayName("Test d'ajout d'un item à une prescription")
    void testAddItemToPrescription() throws Exception {
        // Configuration
        PrescriptionItem newItem = new PrescriptionItem(
            "med3",               // medicationId
            "Omeprazole 20mg",    // medicationName
            "20 mg",             // dosage
            "oral",              // route
            "1 fois par jour",    // frequency
            5,                   // durationDays
            "Prendre le soir avant le coucher",  // instructions
            false,                // asNeeded
            5,                    // quantityPrescribed
            "gélules",            // unit
            true                  // substitutionAllowed
        );
        // Simuler un ID spécifique pour les tests
        try {
            java.lang.reflect.Field idField = PrescriptionItem.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(newItem, "i3");
        } catch (Exception e) {
            // Pour les tests, on peut ignorer cette exception
        }

        // Créer une prescription mise à jour avec les items
        ArrayList<PrescriptionItem> updatedItems = new ArrayList<>(List.of(item1, newItem));
        Prescription updatedPrescription = createTestPrescription("1", "p1", "d1", LocalDate.of(2025, 5, 1), updatedItems);

        when(prescriptionService.addItemToPrescription(eq("1"), any(PrescriptionItem.class))).thenReturn(updatedPrescription);

        // Exécution et vérification
        mockMvc.perform(post("/api/prescriptions/1/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newItem)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionItems", hasSize(2)))
                .andExpect(jsonPath("$.prescriptionItems[1].medicationId", is("med3")));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).addItemToPrescription(eq("1"), any(PrescriptionItem.class));
    }

    @Test
    @DisplayName("Test de suppression d'un item d'une prescription")
    void testRemoveItemFromPrescription() throws Exception {
        // Configuration
        // Créer une prescription mise à jour sans items
        Prescription updatedPrescription = createTestPrescription("1", "p1", "d1", LocalDate.of(2025, 5, 1), new ArrayList<>());

        when(prescriptionService.removeItemFromPrescription("1", "i1")).thenReturn(updatedPrescription);

        // Exécution et vérification
        mockMvc.perform(delete("/api/prescriptions/1/items/i1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prescriptionItems", hasSize(0)));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).removeItemFromPrescription("1", "i1");
    }

    @Test
    @DisplayName("Test de vérification des interactions médicamenteuses")
    void testCheckMedicationInteractions() throws Exception {
        // Configuration
        // Créer des items de prescription pour les interactions
        PrescriptionItem item1 = new PrescriptionItem(
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

        PrescriptionItem item2 = new PrescriptionItem(
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

        // Simuler des IDs spécifiques pour les items
        try {
            java.lang.reflect.Field idField1 = PrescriptionItem.class.getDeclaredField("id");
            idField1.setAccessible(true);
            idField1.set(item1, "item1");

            java.lang.reflect.Field idField2 = PrescriptionItem.class.getDeclaredField("id");
            idField2.setAccessible(true);
            idField2.set(item2, "item2");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification des champs finals: " + e.getMessage());
        }

        // Créer une paire d'interactions
        PrescriptionService.PrescriptionItemPair itemPair = new PrescriptionService.PrescriptionItemPair(item1, item2);

        // Créer une liste de paires pour le mock
        List<PrescriptionService.PrescriptionItemPair> interactionPairs = List.of(itemPair);

        // Mocker la méthode pour qu'elle retourne une liste de paires d'items
        when(prescriptionService.checkMedicationInteractions("1")).thenReturn(interactionPairs);

        // Exécution et vérification
        // Dans la réalité, le contrôleur formatera les paires en objets JSON
        mockMvc.perform(get("/api/prescriptions/1/interactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                // Vérifier les propriétés des items dans la réponse JSON
                .andExpect(jsonPath("$[0].item1.medicationName", is("Doliprane")))
                .andExpect(jsonPath("$[0].item2.medicationName", is("Ibuprofène")));

        // Vérification des appels au service
        verify(prescriptionService, times(1)).checkMedicationInteractions("1");
    }
}

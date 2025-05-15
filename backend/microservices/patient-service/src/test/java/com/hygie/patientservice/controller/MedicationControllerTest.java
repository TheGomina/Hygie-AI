package com.hygie.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hygie.patientservice.model.Medication;
import com.hygie.patientservice.service.MedicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour le contrôleur de gestion des médicaments.
 *
 * Ces tests vérifient le bon fonctionnement des endpoints REST
 * en simulant les requêtes HTTP et les réponses du service.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@WebMvcTest(MedicationController.class)
public class MedicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MedicationService medicationService;

    private Medication medication1;
    private Medication medication2;

    @BeforeEach
    void setUp() {
        // Création des médicaments de test
        medication1 = new Medication("12345678", "Doliprane", "Paracétamol");
        medication2 = new Medication("87654321", "Ibuprofène", "Ibuprofène");
    }

    @Test
    @DisplayName("Test de récupération de tous les médicaments")
    void testGetAllMedications() throws Exception {
        // Configuration
        List<Medication> medications = Arrays.asList(medication1, medication2);
        when(medicationService.getAllMedications()).thenReturn(medications);

        // Exécution et vérification
        mockMvc.perform(get("/api/medications"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Doliprane")))
                .andExpect(jsonPath("$[1].name", is("Ibuprofène")));

        // Vérification des appels au service
        verify(medicationService, times(1)).getAllMedications();
    }

    @Test
    @DisplayName("Test de récupération d'un médicament par ID")
    void testGetMedicationById() throws Exception {
        // Configuration
        when(medicationService.getMedicationById("1")).thenReturn(Optional.of(medication1));

        // Exécution et vérification
        mockMvc.perform(get("/api/medications/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Doliprane")))
                .andExpect(jsonPath("$.activeSubstance", is("Paracétamol")));

        // Vérification des appels au service
        verify(medicationService, times(1)).getMedicationById("1");
    }

    @Test
    @DisplayName("Test de récupération d'un médicament inexistant par ID")
    void testGetMedicationByIdNotFound() throws Exception {
        // Configuration
        when(medicationService.getMedicationById("999")).thenReturn(Optional.empty());

        // Exécution et vérification
        mockMvc.perform(get("/api/medications/999"))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(medicationService, times(1)).getMedicationById("999");
    }

    @Test
    @DisplayName("Test de récupération d'un médicament par code CIS")
    void testGetMedicationByCisCode() throws Exception {
        // Configuration
        when(medicationService.getMedicationByCisCode("12345678")).thenReturn(Optional.of(medication1));

        // Exécution et vérification
        mockMvc.perform(get("/api/medications/cis/12345678"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("Doliprane")))
                .andExpect(jsonPath("$.cisCode", is("12345678")));

        // Vérification des appels au service
        verify(medicationService, times(1)).getMedicationByCisCode("12345678");
    }

    @Test
    @DisplayName("Test de recherche de médicaments")
    void testSearchMedications() throws Exception {
        // Configuration
        when(medicationService.searchMedications("para")).thenReturn(List.of(medication1));

        // Exécution et vérification
        mockMvc.perform(get("/api/medications/search")
                .param("query", "para"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Doliprane")));

        // Vérification des appels au service
        verify(medicationService, times(1)).searchMedications("para");
    }

    @Test
    @DisplayName("Test de recherche avec un terme trop court")
    void testSearchMedicationsWithShortQuery() throws Exception {
        // Exécution et vérification
        mockMvc.perform(get("/api/medications/search")
                .param("query", "pa"))
                .andExpect(status().isBadRequest());

        // Vérification des appels au service
        verify(medicationService, never()).searchMedications(anyString());
    }

    @Test
    @DisplayName("Test de création d'un médicament")
    void testCreateMedication() throws Exception {
        // Configuration
        when(medicationService.getMedicationByCisCode(anyString())).thenReturn(Optional.empty());
        when(medicationService.saveMedication(any(Medication.class))).thenReturn(medication1);

        // Exécution et vérification
        mockMvc.perform(post("/api/medications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medication1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Doliprane")));

        // Vérification des appels au service
        verify(medicationService, times(1)).getMedicationByCisCode(anyString());
        verify(medicationService, times(1)).saveMedication(any(Medication.class));
    }

    @Test
    @DisplayName("Test de création d'un médicament déjà existant")
    void testCreateMedicationAlreadyExists() throws Exception {
        // Configuration
        when(medicationService.getMedicationByCisCode(anyString())).thenReturn(Optional.of(medication1));

        // Exécution et vérification
        mockMvc.perform(post("/api/medications")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medication1)))
                .andExpect(status().isConflict());

        // Vérification des appels au service
        verify(medicationService, times(1)).getMedicationByCisCode(anyString());
        verify(medicationService, never()).saveMedication(any(Medication.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'un médicament")
    void testUpdateMedication() throws Exception {
        // Configuration
        Medication updatedMedication = new Medication("12345678", "Doliprane Forte", "Paracétamol");
        when(medicationService.updateMedication(eq("1"), any(Medication.class))).thenReturn(updatedMedication);

        // Exécution et vérification
        mockMvc.perform(put("/api/medications/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedMedication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Doliprane Forte")));

        // Vérification des appels au service
        verify(medicationService, times(1)).updateMedication(eq("1"), any(Medication.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'un médicament inexistant")
    void testUpdateMedicationNotFound() throws Exception {
        // Configuration
        when(medicationService.updateMedication(eq("999"), any(Medication.class))).thenReturn(null);

        // Exécution et vérification
        mockMvc.perform(put("/api/medications/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medication1)))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(medicationService, times(1)).updateMedication(eq("999"), any(Medication.class));
    }

    @Test
    @DisplayName("Test de suppression d'un médicament")
    void testDeleteMedication() throws Exception {
        // Configuration
        when(medicationService.deleteMedication("1")).thenReturn(true);

        // Exécution et vérification
        mockMvc.perform(delete("/api/medications/1"))
                .andExpect(status().isNoContent());

        // Vérification des appels au service
        verify(medicationService, times(1)).deleteMedication("1");
    }

    @Test
    @DisplayName("Test de suppression d'un médicament inexistant")
    void testDeleteMedicationNotFound() throws Exception {
        // Configuration
        when(medicationService.deleteMedication("999")).thenReturn(false);

        // Exécution et vérification
        mockMvc.perform(delete("/api/medications/999"))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(medicationService, times(1)).deleteMedication("999");
    }

    @Test
    @DisplayName("Test de récupération des médicaments interagissant")
    void testGetInteractingMedications() throws Exception {
        // Configuration
        when(medicationService.getMedicationById("1")).thenReturn(Optional.of(medication1));
        when(medicationService.findInteractingMedications("1")).thenReturn(List.of(medication2));

        // Exécution et vérification
        mockMvc.perform(get("/api/medications/1/interactions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Ibuprofène")));

        // Vérification des appels au service
        verify(medicationService, times(1)).getMedicationById("1");
        verify(medicationService, times(1)).findInteractingMedications("1");
    }
}

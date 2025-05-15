package com.hygie.patientservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hygie.patientservice.model.Patient;
import com.hygie.patientservice.model.MedicalHistory;
import com.hygie.patientservice.service.PatientService;
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

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests unitaires pour le contrôleur de gestion des patients.
 *
 * Ces tests vérifient le bon fonctionnement des endpoints REST
 * en simulant les requêtes HTTP et les réponses du service.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@WebMvcTest(PatientController.class)
public class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PatientService patientService;

    private Patient patient1;
    private Patient patient2;
    private MedicalHistory medicalHistory1;

    @BeforeEach
    void setUp() {
        // Création des patients et des historiques médicaux de test
        patient1 = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");
        // Nous simulons un ID explicite pour les tests
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(patient1, "1");
        } catch (Exception e) {
            // En production, une erreur ici serait grave, mais pour les tests c'est acceptable
        }
        patient1.addActiveCondition("Hypertension");
        patient1.addActiveCondition("Diabète type 2");

        patient2 = new Patient("2751020987654", "Martin", "Marie", LocalDate.of(1975, 10, 20), "F");
        // Nous simulons un ID explicite pour les tests
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(patient2, "2");
        } catch (Exception e) {
            // En production, une erreur ici serait grave, mais pour les tests c'est acceptable
        }
        patient2.addActiveCondition("Asthme");

        medicalHistory1 = new MedicalHistory(
            "Hospitalisation",
            "Hospitalisation pour pneumonie",
            LocalDate.of(2023, 3, 10)
        );
    }

    @Test
    @DisplayName("Test de récupération de tous les patients")
    void testGetAllPatients() throws Exception {
        // Configuration
        List<Patient> patients = Arrays.asList(patient1, patient2);
        when(patientService.getAllPatients()).thenReturn(patients);

        // Exécution et vérification
        mockMvc.perform(get("/api/patients"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].firstName", is("Jean")))
                .andExpect(jsonPath("$[1].firstName", is("Marie")));

        // Vérification des appels au service
        verify(patientService, times(1)).getAllPatients();
    }

    @Test
    @DisplayName("Test de récupération d'un patient par ID")
    void testGetPatientById() throws Exception {
        // Configuration
        when(patientService.getPatientById("1")).thenReturn(Optional.of(patient1));

        // Exécution et vérification
        mockMvc.perform(get("/api/patients/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.firstName", is("Jean")))
                .andExpect(jsonPath("$.conditions", hasSize(2)));

        // Vérification des appels au service
        verify(patientService, times(1)).getPatientById("1");
    }

    @Test
    @DisplayName("Test de récupération d'un patient inexistant par ID")
    void testGetPatientByIdNotFound() throws Exception {
        // Configuration
        when(patientService.getPatientById("999")).thenReturn(Optional.empty());

        // Exécution et vérification
        mockMvc.perform(get("/api/patients/999"))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(patientService, times(1)).getPatientById("999");
    }

    @Test
    @DisplayName("Test de recherche de patients")
    void testSearchPatients() throws Exception {
        // Configuration
        when(patientService.searchPatients("Jean")).thenReturn(List.of(patient1));

        // Exécution et vérification
        mockMvc.perform(get("/api/patients/search")
                .param("query", "Jean"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName", is("Jean")));

        // Vérification des appels au service
        verify(patientService, times(1)).searchPatients("Jean");
    }

    @Test
    @DisplayName("Test de recherche avec un terme trop court")
    void testSearchPatientsWithShortQuery() throws Exception {
        // Exécution et vérification
        mockMvc.perform(get("/api/patients/search")
                .param("query", "Je"))
                .andExpect(status().isBadRequest());

        // Vérification des appels au service
        verify(patientService, never()).searchPatients(anyString());
    }

    @Test
    @DisplayName("Test de création d'un patient")
    void testCreatePatient() throws Exception {
        // Configuration
        when(patientService.savePatient(any(Patient.class))).thenReturn(patient1);

        // Exécution et vérification
        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName", is("Jean")))
                .andExpect(jsonPath("$.lastName", is("Dupont")));

        // Vérification des appels au service
        verify(patientService, times(1)).savePatient(any(Patient.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'un patient")
    void testUpdatePatient() throws Exception {
        // Configuration
        Patient updatedPatient = new Patient(
            "1600512345678",  // nationalId
            "Dupont",        // lastName
            "Jean-Pierre",    // firstName
            LocalDate.of(1960, 5, 15), // birthDate
            "M"              // gender
        );

        // Utiliser la réflexion pour modifier l'ID qui est final
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPatient, "1");
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification de l'ID", e);
        }

        when(patientService.updatePatient(eq("1"), any(Patient.class))).thenReturn(updatedPatient);

        // Exécution et vérification
        mockMvc.perform(put("/api/patients/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPatient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName", is("Jean-Pierre")));

        // Vérification des appels au service
        verify(patientService, times(1)).updatePatient(eq("1"), any(Patient.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'un patient inexistant")
    void testUpdatePatientNotFound() throws Exception {
        // Configuration
        when(patientService.updatePatient(eq("999"), any(Patient.class))).thenReturn(null);

        // Exécution et vérification
        mockMvc.perform(put("/api/patients/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patient1)))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(patientService, times(1)).updatePatient(eq("999"), any(Patient.class));
    }

    @Test
    @DisplayName("Test de suppression d'un patient")
    void testDeletePatient() throws Exception {
        // Configuration
        when(patientService.deletePatient("1")).thenReturn(true);

        // Exécution et vérification
        mockMvc.perform(delete("/api/patients/1"))
                .andExpect(status().isNoContent());

        // Vérification des appels au service
        verify(patientService, times(1)).deletePatient("1");
    }

    @Test
    @DisplayName("Test de suppression d'un patient inexistant")
    void testDeletePatientNotFound() throws Exception {
        // Configuration
        when(patientService.deletePatient("999")).thenReturn(false);

        // Exécution et vérification
        mockMvc.perform(delete("/api/patients/999"))
                .andExpect(status().isNotFound());

        // Vérification des appels au service
        verify(patientService, times(1)).deletePatient("999");
    }

    @Test
    @DisplayName("Test d'ajout d'un élément à l'historique médical")
    void testAddMedicalHistoryItem() throws Exception {
        // Configuration
        Patient updatedPatient = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");
        // Simuler un ID spécifique pour les tests
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPatient, "1");
        } catch (Exception e) {
            // Pour les tests, on peut ignorer cette exception
        }
        updatedPatient.addMedicalHistory(medicalHistory1);

        when(patientService.addMedicalHistoryItem(eq("1"), any(MedicalHistory.class))).thenReturn(updatedPatient);

        // Exécution et vérification
        mockMvc.perform(post("/api/patients/1/medical-history")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(medicalHistory1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.medicalHistory", hasSize(1)))
                .andExpect(jsonPath("$.medicalHistory[0].eventType", is("Hospitalisation")));

        // Vérification des appels au service
        verify(patientService, times(1)).addMedicalHistoryItem(eq("1"), any(MedicalHistory.class));
    }

    @Test
    @DisplayName("Test de recherche de patients par condition médicale")
    void testFindPatientsByCondition() throws Exception {
        // Configuration
        when(patientService.findPatientsByCondition("Diabète")).thenReturn(List.of(patient1));

        // Exécution et vérification
        mockMvc.perform(get("/api/patients/condition/Diabète"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].firstName", is("Jean")));

        // Vérification des appels au service
        verify(patientService, times(1)).findPatientsByCondition("Diabète");
    }

    @Test
    @DisplayName("Test d'analyse des risques pour un patient")
    void testAnalyzePatientRisks() throws Exception {
        // Configuration
        List<String> risks = Arrays.asList(
            "Patient âgé (65 ans): surveillance accrue requise",
            "Présence de multiples comorbidités: risque d'interactions"
        );
        when(patientService.analyzePatientRisks("1")).thenReturn(risks);

        // Exécution et vérification
        mockMvc.perform(get("/api/patients/1/risks"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0]", containsString("Patient âgé")));

        // Vérification des appels au service
        verify(patientService, times(1)).analyzePatientRisks("1");
    }

    @Test
    @DisplayName("Test de validation de données patient incorrectes")
    void testInvalidPatientData() throws Exception {
        // Création d'un objet Map représentant un patient avec des données invalides
        // Nous n'utilisons pas directement la classe Patient car son constructeur valide les données
        java.util.Map<String, Object> invalidPatient = new java.util.HashMap<>();
        invalidPatient.put("nationalId", "1234567890123"); // Valide
        invalidPatient.put("firstName", ""); // Prénom vide, non valide
        invalidPatient.put("lastName", "Dupont"); // Valide
        invalidPatient.put("birthDate", "1960-05-15"); // Valide
        invalidPatient.put("gender", "M"); // Valide

        // Exécution et vérification
        mockMvc.perform(post("/api/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPatient)))
                .andExpect(status().isBadRequest());

        // Vérification des appels au service
        verify(patientService, never()).savePatient(any(Patient.class));
    }
}

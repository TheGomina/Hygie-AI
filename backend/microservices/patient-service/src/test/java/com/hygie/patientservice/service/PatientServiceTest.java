package com.hygie.patientservice.service;

import com.hygie.patientservice.model.Patient;
import com.hygie.patientservice.model.MedicalHistory;
import com.hygie.patientservice.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service de gestion des patients.
 *
 * Ces tests vérifient le bon fonctionnement des méthodes du service
 * en simulant les interactions avec le repository et les services dépendants.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PrescriptionService prescriptionService;

    @InjectMocks
    private PatientService patientService;

    private Patient patient1;
    private Patient patient2;
    private MedicalHistory medicalHistory1;

    @BeforeEach
    void setUp() {
        // Création des patients et des historiques médicaux de test
        patient1 = new Patient("1600512345678", "Dupont", "Jean", LocalDate.of(1960, 5, 15), "M");

        // Ajout des conditions via la méthode addActiveCondition
        patient1.addActiveCondition("Hypertension");
        patient1.addActiveCondition("Diabète type 2");

        patient2 = new Patient("2751020987654", "Martin", "Marie", LocalDate.of(1975, 10, 20), "F");
        patient2.addActiveCondition("Asthme");

        medicalHistory1 = new MedicalHistory(
            "Hospitalisation",
            "Hospitalisation pour pneumonie",
            LocalDate.of(2023, 3, 10)
        );
    }

    @Test
    @DisplayName("Test de récupération de tous les patients")
    void testGetAllPatients() {
        // Configuration
        List<Patient> patients = Arrays.asList(patient1, patient2);
        when(patientRepository.findAll()).thenReturn(patients);

        // Exécution
        List<Patient> results = patientService.getAllPatients();

        // Vérification
        assertEquals(2, results.size(), "La liste devrait contenir deux patients");
        assertTrue(results.contains(patient1), "La liste devrait contenir le patient1");
        assertTrue(results.contains(patient2), "La liste devrait contenir le patient2");
        verify(patientRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Test de récupération d'un patient par ID")
    void testGetPatientById() {
        // Configuration
        when(patientRepository.findById("1")).thenReturn(Optional.of(patient1));

        // Exécution
        Optional<Patient> result = patientService.getPatientById("1");

        // Vérification
        assertTrue(result.isPresent(), "Le patient devrait être présent");
        assertEquals(patient1, result.get(), "Le patient retourné devrait être patient1");
        verify(patientRepository, times(1)).findById("1");
    }

    @Test
    @DisplayName("Test de récupération d'un patient inexistant par ID")
    void testGetPatientByIdNotFound() {
        // Configuration
        when(patientRepository.findById("999")).thenReturn(Optional.empty());

        // Exécution
        Optional<Patient> result = patientService.getPatientById("999");

        // Vérification
        assertFalse(result.isPresent(), "Le patient ne devrait pas être trouvé");
        verify(patientRepository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Test de recherche de patients")
    void testSearchPatients() {
        // Configuration
        when(patientRepository.findByFirstNameContainingIgnoreCase("Jea")).thenReturn(Collections.singletonList(patient1));
        when(patientRepository.findByLastNameContainingIgnoreCase("Jea")).thenReturn(Collections.emptyList());

        // Exécution
        List<Patient> results = patientService.searchPatients("Jea");

        // Vérification
        assertEquals(1, results.size(), "La liste devrait contenir un patient");
        assertEquals("Jean", results.get(0).getFirstName(), "Le prénom du patient trouvé devrait être Jean");
        verify(patientRepository, times(1)).findByFirstNameContainingIgnoreCase("Jea");
        verify(patientRepository, times(1)).findByLastNameContainingIgnoreCase("Jea");
    }

    @Test
    @DisplayName("Test de sauvegarde d'un patient")
    void testSavePatient() {
        // Configuration
        when(patientRepository.save(any(Patient.class))).thenReturn(patient1);

        // Exécution
        Patient savedPatient = patientService.savePatient(patient1);

        // Vérification
        assertNotNull(savedPatient, "Le patient sauvegardé ne devrait pas être null");
        assertEquals("Jean", savedPatient.getFirstName(), "Le prénom du patient sauvegardé devrait être Jean");
        verify(patientRepository, times(1)).save(patient1);
    }

    @Test
    @DisplayName("Test de mise à jour d'un patient")
    void testUpdatePatient() {
        // Configuration
        // Créer le patient avec ses attributs de base via constructeur
        Patient updatedPatient = new Patient(
            "123456789012345",           // nationalId
            "Dupont",                   // lastName
            "Jean-Pierre",              // firstName
            LocalDate.of(1960, 5, 15), // birthDate
            "M"                        // gender
        );

        // Utiliser la réflexion pour définir l'ID qui est final
        try {
            java.lang.reflect.Field idField = Patient.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPatient, "1");
        } catch (Exception e) {
            fail("Erreur lors de la modification de l'ID par réflexion: " + e.getMessage());
        }

        when(patientRepository.findById("1")).thenReturn(Optional.of(patient1));
        when(patientRepository.save(any(Patient.class))).thenReturn(updatedPatient);

        // Exécution
        Patient result = patientService.updatePatient("1", updatedPatient);

        // Vérification
        assertNotNull(result, "Le patient mis à jour ne devrait pas être null");
        assertEquals("Jean-Pierre", result.getFirstName(), "Le prénom mis à jour devrait être Jean-Pierre");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'un patient inexistant")
    void testUpdatePatientNotFound() {
        // Configuration
        when(patientRepository.findById("999")).thenReturn(Optional.empty());

        // Exécution
        Patient result = patientService.updatePatient("999", patient1);

        // Vérification
        assertNull(result, "Le résultat devrait être null pour un patient inexistant");
        verify(patientRepository, never()).save(any(Patient.class));
    }

    @Test
    @DisplayName("Test de suppression d'un patient")
    void testDeletePatient() {
        // Configuration
        when(patientRepository.existsById("1")).thenReturn(true).thenReturn(false);
        doNothing().when(patientRepository).deleteById("1");

        // Exécution
        boolean result = patientService.deletePatient("1");

        // Vérification
        assertTrue(result, "La suppression devrait retourner true");
        verify(patientRepository, times(1)).deleteById("1");
        verify(patientRepository, times(2)).existsById("1");
    }

    @Test
    @DisplayName("Test de suppression d'un patient inexistant")
    void testDeletePatientNotFound() {
        // Configuration
        when(patientRepository.existsById("999")).thenReturn(false);

        // Exécution
        boolean result = patientService.deletePatient("999");

        // Vérification
        assertFalse(result, "La suppression devrait retourner false pour un patient inexistant");
        verify(patientRepository, never()).deleteById("999");
    }

    @Test
    @DisplayName("Test d'ajout d'un élément à l'historique médical")
    void testAddMedicalHistoryItem() {
        // Configuration
        when(patientRepository.findById("1")).thenReturn(Optional.of(patient1));
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient savedPatient = invocation.getArgument(0);
            // Simuler l'ajout à l'historique médical
            savedPatient.addMedicalHistory(medicalHistory1);
            return savedPatient;
        });

        // Exécution
        Patient updatedPatient = patientService.addMedicalHistoryItem("1", medicalHistory1);

        // Vérification
        assertNotNull(updatedPatient, "Le patient mis à jour ne devrait pas être null");
        // Vérifier que l'historique médical contient un élément avec les bonnes propriétés
        assertTrue(updatedPatient.getMedicalHistory().stream()
                .anyMatch(mh -> mh.getEventType().equals(medicalHistory1.getEventType()) &&
                         mh.getDescription().equals(medicalHistory1.getDescription())),
                "L'historique médical du patient devrait contenir le nouvel élément");
        verify(patientRepository, times(1)).save(any(Patient.class));
    }

    @Test
    @DisplayName("Test de recherche de patients par condition médicale")
    void testFindPatientsByCondition() {
        // Configuration
        when(patientRepository.findByConditionsContainingIgnoreCase("Diabète"))
                .thenReturn(Collections.singletonList(patient1));

        // Exécution
        List<Patient> results = patientService.findPatientsByCondition("Diabète");

        // Vérification
        assertEquals(1, results.size(), "La liste devrait contenir un patient");
        assertEquals("Jean", results.get(0).getFirstName(), "Le prénom du patient trouvé devrait être Jean");
        verify(patientRepository, times(1)).findByConditionsContainingIgnoreCase("Diabète");
    }

    @Test
    @DisplayName("Test de calcul de l'âge d'un patient")
    void testCalculatePatientAge() {
        // Configuration - patient1 est né en 1960

        // Exécution
        int age = patient1.getAge();

        // Vérification
        assertTrue(age >= 63, "L'âge calculé devrait être au moins 63 ans (en 2023)");
    }

    @Test
    @DisplayName("Test d'analyse des risques pour un patient")
    void testAnalyzePatientRisks() {
        // Configuration
        when(patientRepository.findById("1")).thenReturn(Optional.of(patient1));
        when(prescriptionService.checkMedicationInteractions("1")).thenReturn(Collections.emptyList());

        // Exécution
        List<String> risks = patientService.analyzePatientRisks("1");

        // Vérification
        assertNotNull(risks, "La liste des risques ne devrait pas être null");
        assertTrue(risks.stream().anyMatch(r -> r.contains("Patient âgé")),
                "La liste devrait contenir un risque lié à l'âge");
        verify(patientRepository, times(1)).findById("1");
        verify(prescriptionService, times(1)).checkMedicationInteractions("1");
    }
}

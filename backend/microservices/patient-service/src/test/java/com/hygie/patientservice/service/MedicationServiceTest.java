package com.hygie.patientservice.service;

import com.hygie.patientservice.model.Medication;
import com.hygie.patientservice.repository.MedicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour le service de gestion des médicaments.
 *
 * Ces tests vérifient le bon fonctionnement des méthodes du service
 * en simulant les interactions avec le repository.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class MedicationServiceTest {

    @Mock
    private MedicationRepository medicationRepository;

    @InjectMocks
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
    @DisplayName("Test de récupération d'un médicament par ID")
    void testGetMedicationById() {
        // Configuration
        when(medicationRepository.findById("1")).thenReturn(Optional.of(medication1));

        // Exécution
        Optional<Medication> result = medicationService.getMedicationById("1");

        // Vérification
        assertTrue(result.isPresent(), "Le médicament devrait être présent");
        assertEquals(medication1, result.get(), "Le médicament retourné devrait être medication1");
        verify(medicationRepository, times(1)).findById("1");
    }

    @Test
    @DisplayName("Test de récupération d'un médicament inexistant par ID")
    void testGetMedicationByIdNotFound() {
        // Configuration
        when(medicationRepository.findById("999")).thenReturn(Optional.empty());

        // Exécution
        Optional<Medication> result = medicationService.getMedicationById("999");

        // Vérification
        assertFalse(result.isPresent(), "Le médicament ne devrait pas être trouvé");
        verify(medicationRepository, times(1)).findById("999");
    }

    @Test
    @DisplayName("Test de récupération d'un médicament par code CIS")
    void testGetMedicationByCisCode() {
        // Configuration
        when(medicationRepository.findByCisCode("12345678")).thenReturn(Optional.of(medication1));

        // Exécution
        Optional<Medication> result = medicationService.getMedicationByCisCode("12345678");

        // Vérification
        assertTrue(result.isPresent(), "Le médicament devrait être présent");
        assertEquals("Doliprane", result.get().getName(), "Le nom du médicament devrait être Doliprane");
        verify(medicationRepository, times(1)).findByCisCode("12345678");
    }

    @Test
    @DisplayName("Test de sauvegarde d'un médicament")
    void testSaveMedication() {
        // Configuration
        when(medicationRepository.findByCisCode(anyString())).thenReturn(Optional.empty());
        when(medicationRepository.save(any(Medication.class))).thenReturn(medication1);

        // Exécution
        Medication savedMedication = medicationService.saveMedication(medication1);

        // Vérification
        assertNotNull(savedMedication, "Le médicament sauvegardé ne devrait pas être null");
        assertEquals("Doliprane", savedMedication.getName(), "Le nom du médicament sauvegardé devrait être Doliprane");
        verify(medicationRepository, times(1)).save(medication1);
    }

    @Test
    @DisplayName("Test de recherche de médicaments")
    void testSearchMedications() {
        // Configuration
        when(medicationRepository.findByNameContainingIgnoreCase("para")).thenReturn(List.of(medication1));
        when(medicationRepository.findByActiveSubstanceContainingIgnoreCase("para")).thenReturn(List.of(medication1));

        // Exécution
        List<Medication> results = medicationService.searchMedications("para");

        // Vérification
        assertFalse(results.isEmpty(), "La liste de résultats ne devrait pas être vide");
        assertEquals(1, results.size(), "La liste devrait contenir un médicament");
        assertEquals("Doliprane", results.get(0).getName(), "Le médicament trouvé devrait être Doliprane");
    }

    @Test
    @DisplayName("Test de suppression d'un médicament")
    void testDeleteMedication() {
        // Configuration
        when(medicationRepository.existsById("1")).thenReturn(true).thenReturn(false);
        doNothing().when(medicationRepository).deleteById("1");

        // Exécution
        boolean result = medicationService.deleteMedication("1");

        // Vérification
        assertTrue(result, "La suppression devrait retourner true");
        verify(medicationRepository, times(1)).deleteById("1");
        verify(medicationRepository, times(2)).existsById("1");
    }

    @Test
    @DisplayName("Test de suppression d'un médicament inexistant")
    void testDeleteMedicationNotFound() {
        // Configuration
        when(medicationRepository.existsById("999")).thenReturn(false);

        // Exécution
        boolean result = medicationService.deleteMedication("999");

        // Vérification
        assertFalse(result, "La suppression devrait retourner false pour un médicament inexistant");
        verify(medicationRepository, never()).deleteById("999");
    }

    @Test
    @DisplayName("Test de récupération de tous les médicaments")
    void testGetAllMedications() {
        // Configuration
        List<Medication> medications = Arrays.asList(medication1, medication2);
        when(medicationRepository.findAll()).thenReturn(medications);

        // Exécution
        List<Medication> results = medicationService.getAllMedications();

        // Vérification
        assertEquals(2, results.size(), "La liste devrait contenir deux médicaments");
        assertTrue(results.contains(medication1), "La liste devrait contenir medication1");
        assertTrue(results.contains(medication2), "La liste devrait contenir medication2");
    }

    @Test
    @DisplayName("Test de vérification d'interaction entre médicaments")
    void testCheckInteraction() {
        // Configuration
        when(medicationRepository.findById("1")).thenReturn(Optional.of(medication1));
        when(medicationRepository.findById("2")).thenReturn(Optional.of(medication2));

        // On modifie la liste d'interactions pour le test en utilisant la réflexion
        try {
            // Créer la nouvelle liste d'interactions
            List<String> interactions = new ArrayList<>();
            interactions.add("Ibuprofène");

            // Accéder au champ interactions et le modifier via la réflexion
            java.lang.reflect.Field interactionsField = Medication.class.getDeclaredField("interactions");
            interactionsField.setAccessible(true);

            // Récupérer la liste existante
            @SuppressWarnings("unchecked")
            List<String> currentInteractions = (List<String>) interactionsField.get(medication1);

            // Effacer et ajouter le nouvel élément
            currentInteractions.clear();
            currentInteractions.add("Ibuprofène");
        } catch (Exception e) {
            fail("Erreur lors de la modification des interactions par réflexion: " + e.getMessage());
        }

        // Exécution
        boolean hasInteraction = medicationService.checkInteraction("1", "2");

        // Vérification
        assertTrue(hasInteraction, "Les médicaments devraient avoir une interaction");
        verify(medicationRepository, times(1)).findById("1");
        verify(medicationRepository, times(1)).findById("2");
    }

    @Test
    @DisplayName("Test de mise à jour d'un médicament")
    void testUpdateMedication() {
        // Configuration
        Medication updatedMedication = new Medication("12345678", "Doliprane Forte", "Paracétamol");
        when(medicationRepository.findById("1")).thenReturn(Optional.of(medication1));
        when(medicationRepository.save(any(Medication.class))).thenReturn(updatedMedication);

        // Exécution
        Medication result = medicationService.updateMedication("1", updatedMedication);

        // Vérification
        assertNotNull(result, "Le médicament mis à jour ne devrait pas être null");
        assertEquals("Doliprane Forte", result.getName(), "Le nom mis à jour devrait être Doliprane Forte");
        verify(medicationRepository, times(1)).save(any(Medication.class));
    }

    @Test
    @DisplayName("Test de mise à jour d'un médicament inexistant")
    void testUpdateMedicationNotFound() {
        // Configuration
        when(medicationRepository.findById("999")).thenReturn(Optional.empty());

        // Exécution
        Medication result = medicationService.updateMedication("999", medication1);

        // Vérification
        assertNull(result, "Le résultat devrait être null pour un médicament inexistant");
        verify(medicationRepository, never()).save(any(Medication.class));
    }
}

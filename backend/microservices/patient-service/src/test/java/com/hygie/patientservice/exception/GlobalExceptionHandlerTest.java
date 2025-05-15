package com.hygie.patientservice.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Tests unitaires pour le gestionnaire global d'exceptions.
 *
 * Ces tests vérifient le bon fonctionnement du gestionnaire global d'exceptions
 * qui transforme les exceptions en réponses HTTP adaptées.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Test
    @DisplayName("Test de gestion d'exception Patient Not Found")
    void testHandlePatientNotFoundException() {
        // Configuration
        PatientNotFoundException exception = new PatientNotFoundException("1");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/patients/1");

        // Exécution
        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePatientNotFoundException(exception, webRequest);

        // Vérification
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Le code HTTP devrait être 404 NOT FOUND");

        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "Le corps de la réponse ne devrait pas être null");
        assertEquals("Patient introuvable avec l'ID: 1", errorResponse.getMessage(),
                "Le message d'erreur devrait indiquer le patient manquant");
        assertNotNull(errorResponse.getTimestamp(), "Le timestamp devrait être présent");
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus(),
                "Le statut numérique devrait être 404");
    }

    @Test
    @DisplayName("Test de gestion d'une MedicationNotFoundException")
    void testHandleMedicationNotFoundException() {
        // Configuration
        MedicationNotFoundException exception = new MedicationNotFoundException("med123");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/medications/med123");

        // Exécution
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleMedicationNotFoundException(exception, webRequest);

        // Vérification
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Le code HTTP devrait être 404 NOT FOUND");

        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "Le corps de la réponse ne devrait pas être null");
        assertEquals("Médicament introuvable avec l'ID: med123", errorResponse.getMessage(),
                "Le message d'erreur devrait indiquer le médicament manquant");
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus(),
                "Le statut numérique devrait être 404");
    }

    @Test
    @DisplayName("Test de gestion d'une PrescriptionNotFoundException")
    void testHandlePrescriptionNotFoundException() {
        // Configuration
        PrescriptionNotFoundException exception = new PrescriptionNotFoundException("presc456");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/prescriptions/presc456");

        // Exécution
        ResponseEntity<ErrorResponse> response = exceptionHandler.handlePrescriptionNotFoundException(exception, webRequest);

        // Vérification
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(), "Le code HTTP devrait être 404 NOT FOUND");

        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "Le corps de la réponse ne devrait pas être null");
        assertEquals("Prescription introuvable avec l'ID: presc456", errorResponse.getMessage(),
                "Le message d'erreur devrait indiquer la prescription manquante");
        assertEquals(HttpStatus.NOT_FOUND.value(), errorResponse.getStatus(),
                "Le statut numérique devrait être 404");
    }

    @Test
    @DisplayName("Test de gestion d'une HygieServiceException")
    void testHandleHygieServiceException() {
        // Configuration
        HygieServiceException exception = new HygieServiceException("Erreur critique du service");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/service-operation");

        // Exécution
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleHygieServiceException(exception, webRequest);

        // Vérification
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
                "Le code HTTP devrait être 500 INTERNAL_SERVER_ERROR");

        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "Le corps de la réponse ne devrait pas être null");
        assertEquals("Erreur critique du service", errorResponse.getMessage(),
                "Le message d'erreur devrait être celui fourni à l'exception");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus(),
                "Le statut numérique devrait être 500");
    }

    @Test
    @DisplayName("Test de gestion d'une erreur de validation (MethodArgumentNotValidException)")
    void testHandleMethodArgumentNotValidException() throws Exception {
        // Configuration
        MethodArgumentNotValidException mockException = mock(MethodArgumentNotValidException.class);
        BindingResult mockBindingResult = mock(BindingResult.class);

        List<FieldError> fieldErrors = new ArrayList<>();
        fieldErrors.add(new FieldError("patient", "firstName", "Le prénom ne peut pas être vide"));
        fieldErrors.add(new FieldError("patient", "birthDate", "La date de naissance est obligatoire"));

        when(mockException.getBindingResult()).thenReturn(mockBindingResult);
        when(mockBindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // Ajouter un WebRequest mock
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/patients");

        // Exécution
        ResponseEntity<ValidationErrorResponse> response = exceptionHandler.handleMethodArgumentNotValidException(mockException, webRequest);

        // Vérification
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(),
                "Le code HTTP devrait être 400 BAD_REQUEST");

        ValidationErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "Le corps de la réponse ne devrait pas être null");
        assertEquals("Erreur de validation des données", errorResponse.getMessage(),
                "Le message d'erreur devrait indiquer une erreur de validation");
        assertEquals(HttpStatus.BAD_REQUEST.value(), errorResponse.getStatus(),
                "Le statut numérique devrait être 400");

        assertEquals(2, errorResponse.getErrors().size(),
                "La liste des erreurs devrait contenir 2 éléments");
        assertTrue(errorResponse.getErrors().containsKey("firstName"),
                "Les erreurs devraient contenir une entrée pour firstName");
        assertTrue(errorResponse.getErrors().containsKey("birthDate"),
                "Les erreurs devraient contenir une entrée pour birthDate");
        assertEquals("Le prénom ne peut pas être vide", errorResponse.getErrors().get("firstName"),
                "Le message d'erreur pour firstName devrait correspondre");
    }

    @Test
    @DisplayName("Test de gestion d'une exception générique non traitée")
    void testHandleGenericException() {
        // Configuration
        Exception exception = new RuntimeException("Une erreur inattendue s'est produite");
        WebRequest webRequest = mock(WebRequest.class);
        when(webRequest.getDescription(false)).thenReturn("uri=/api/operation");

        // Exécution
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGlobalException(exception, webRequest);

        // Vérification
        assertNotNull(response, "La réponse ne devrait pas être null");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode(),
                "Le code HTTP devrait être 500 INTERNAL_SERVER_ERROR");

        ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse, "Le corps de la réponse ne devrait pas être null");
        // Le message d'erreur est généralisé pour les exceptions globales
        assertEquals("Une erreur inattendue s'est produite. Veuillez contacter le support.", errorResponse.getMessage(),
                "Le message d'erreur devrait être générique pour les erreurs internes");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), errorResponse.getStatus(),
                "Le statut numérique devrait être 500");
    }

    // Nous ne testons pas la méthode handleConstraintViolationException pour le moment
    // car sa mise en œuvre est complexe et nécessiterait un environnement de test spécifique
    // pour la validation des contraintes.
    //
    // Dans un contexte de production, il faudrait implémenter ce test en utilisant
    // un environnement qui simule les mécanismes de validation de Jakarta.
}

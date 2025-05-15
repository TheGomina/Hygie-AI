package com.hygie.patientservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Past;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Modèle représentant un élément d'historique médical dans le système Hygie-AI.
 *
 * Cette classe permet de stocker les événements médicaux passés
 * du patient (hospitalisation, chirurgie, diagnostic, etc.).
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
public class MedicalHistory {

    private final String id;

    @NotBlank(message = "Le type d'événement médical est obligatoire")
    private final String eventType;

    @NotBlank(message = "La description de l'événement est obligatoire")
    private final String description;

    @NotNull(message = "La date de l'événement est obligatoire")
    @Past(message = "La date de l'événement doit être dans le passé")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private final LocalDate eventDate;

    private final String location;

    private final String healthcareProfessional;

    private final String result;

    /**
     * Constructeur complet pour un élément d'historique médical.
     *
     * @param eventType Le type d'événement (ex: Hospitalisation, Chirurgie, Diagnostic)
     * @param description La description détaillée de l'événement
     * @param eventDate La date de l'événement
     * @param location Le lieu de l'événement (hôpital, clinique, etc.)
     * @param healthcareProfessional Le professionnel de santé impliqué
     * @param result Le résultat ou l'issue de l'événement
     */
    public MedicalHistory(
            String eventType,
            String description,
            LocalDate eventDate,
            String location,
            String healthcareProfessional,
            String result) {

        // Assertion #1: Vérification des paramètres obligatoires
        assert eventType != null && !eventType.isBlank() : "Le type d'événement ne peut pas être null ou vide";
        assert description != null && !description.isBlank() : "La description ne peut pas être null ou vide";
        assert eventDate != null && eventDate.isBefore(LocalDate.now()) :
            "La date de l'événement doit être valide et dans le passé";

        // Assertion #2: Vérification de la validité des paramètres optionnels
        if (location != null) {
            assert !location.isBlank() : "Le lieu ne peut pas être vide s'il est spécifié";
        }
        if (healthcareProfessional != null) {
            assert !healthcareProfessional.isBlank() :
                "Le professionnel de santé ne peut pas être vide s'il est spécifié";
        }

        this.id = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.description = description;
        this.eventDate = eventDate;
        this.location = location;
        this.healthcareProfessional = healthcareProfessional;
        this.result = result;
    }

    /**
     * Constructeur minimal pour un élément d'historique médical.
     *
     * @param eventType Le type d'événement
     * @param description La description détaillée
     * @param eventDate La date de l'événement
     */
    public MedicalHistory(String eventType, String description, LocalDate eventDate) {
        this(eventType, description, eventDate, null, null, null);
    }

    /**
     * Détermine si cet événement médical est récent (moins de 1 an).
     *
     * @return true si l'événement est survenu il y a moins d'un an
     */
    public boolean isRecent() {
        // Assertion #1: Vérification que la date de l'événement est définie
        assert eventDate != null : "La date de l'événement est requise";

        // Assertion #2: Vérification que la date est dans le passé
        assert eventDate.isBefore(LocalDate.now()) : "La date de l'événement doit être dans le passé";

        final LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        return eventDate.isAfter(oneYearAgo);
    }

    /**
     * Génère un résumé formaté de l'événement médical.
     *
     * @return Une description formatée de l'événement médical
     */
    public String formatEvent() {
        // Assertion #1: Vérification que les données minimales sont présentes
        assert eventType != null && description != null && eventDate != null :
            "Les données minimales sont requises pour formater l'événement";

        // Assertion #2: Vérification que les données sont valides
        assert !eventType.isBlank() && !description.isBlank() :
            "Le type d'événement et la description ne peuvent pas être vides";

        final StringBuilder formatted = new StringBuilder(100);
        formatted.append(eventDate.toString())
                 .append(" - ")
                 .append(eventType)
                 .append(": ")
                 .append(description);

        if (location != null && !location.isBlank()) {
            formatted.append(" (").append(location).append(")");
        }

        if (result != null && !result.isBlank()) {
            formatted.append(" - Résultat: ").append(result);
        }

        return formatted.toString();
    }

    /**
     * Vérifie si cet événement est lié à un médicament spécifique.
     *
     * @param medicationName Le nom du médicament à rechercher
     * @return true si l'événement mentionne ce médicament
     */
    public boolean isMedicationRelated(String medicationName) {
        // Assertion #1: Vérification que le nom du médicament n'est pas vide
        assert medicationName != null && !medicationName.isBlank() :
            "Le nom du médicament ne peut pas être null ou vide";

        // Assertion #2: Vérification que la description existe
        assert description != null : "La description de l'événement est requise";

        return description.toLowerCase().contains(medicationName.toLowerCase());
    }

    // Getters - Pas de setters pour garantir l'immutabilité

    public String getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDescription() {
        return description;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getLocation() {
        return location;
    }

    public String getHealthcareProfessional() {
        return healthcareProfessional;
    }

    public String getResult() {
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalHistory that = (MedicalHistory) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

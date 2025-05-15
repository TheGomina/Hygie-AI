package com.hygie.patientservice.service;

import com.hygie.patientservice.model.Prescription;
import com.hygie.patientservice.model.PrescriptionItem;
import com.hygie.patientservice.model.Prescription.PrescriptionStatus;
import com.hygie.patientservice.repository.PrescriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service pour la gestion des prescriptions médicamenteuses dans le système Hygie-AI.
 *
 * Ce service implémente les opérations métier liées aux prescriptions et ordonnances,
 * notamment pour les besoins du Bilan Partagé de Médication.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Service
@Transactional
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final MedicationService medicationService;

    /**
     * Constructeur avec injection des dépendances.
     *
     * @param prescriptionRepository Le repository pour accéder aux données des prescriptions
     * @param medicationService Le service pour accéder aux données des médicaments
     */
    @Autowired
    public PrescriptionService(PrescriptionRepository prescriptionRepository,
                              MedicationService medicationService) {
        // Assertion #1: Vérification que le repository n'est pas null
        assert prescriptionRepository != null : "Le repository de prescriptions ne peut pas être null";

        // Assertion #2: Vérification que le service de médicaments n'est pas null
        assert medicationService != null : "Le service de médicaments ne peut pas être null";

        this.prescriptionRepository = prescriptionRepository;
        this.medicationService = medicationService;
    }

    /**
     * Sauvegarde une prescription dans la base de données.
     *
     * @param prescription La prescription à sauvegarder
     * @return La prescription sauvegardée avec son ID généré
     */
    public Prescription savePrescription(Prescription prescription) {
        // Assertion #1: Vérification que la prescription n'est pas null
        assert prescription != null : "La prescription à sauvegarder ne peut pas être null";

        // Assertion #2: Vérification des données minimales
        assert prescription.getPatientId() != null && !prescription.getPatientId().isBlank() :
            "L'ID du patient est obligatoire";
        assert prescription.getPrescriberId() != null && !prescription.getPrescriberId().isBlank() :
            "L'ID du prescripteur est obligatoire";
        assert !prescription.getPrescriptionItems().isEmpty() :
            "Une prescription doit contenir au moins un médicament";

        // Mise à jour automatique du statut si nécessaire
        if (prescription.isExpired() && prescription.getStatus() == PrescriptionStatus.ACTIVE) {
            prescription.updateStatus(PrescriptionStatus.EXPIRED);
        }

        final Prescription savedPrescription = prescriptionRepository.save(prescription);

        // Postcondition
        assert savedPrescription != null && savedPrescription.getId() != null :
            "Échec de la sauvegarde de la prescription";

        return savedPrescription;
    }

    /**
     * Récupère une prescription par son ID.
     *
     * @param id L'ID de la prescription
     * @return Un Optional contenant la prescription ou vide si elle n'existe pas
     */
    public Optional<Prescription> getPrescriptionById(String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";

        final Optional<Prescription> prescription = prescriptionRepository.findById(id);

        // Assertion #2: Postcondition
        if (prescription.isPresent()) {
            assert prescription.get().getId().equals(id) :
                "L'ID de la prescription récupérée ne correspond pas à celui demandé";
        }

        return prescription;
    }

    /**
     * Récupère toutes les prescriptions d'un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste des prescriptions du patient
     */
    public List<Prescription> getPatientPrescriptions(String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        final List<Prescription> prescriptions =
                prescriptionRepository.findByPatientId(patientId);

        // Assertion #2: Vérification du résultat
        assert prescriptions != null : "La liste des prescriptions ne peut pas être null";

        return prescriptions;
    }

    /**
     * Alias pour getPatientPrescriptions pour compatibilité avec les tests.
     *
     * @param patientId L'ID du patient
     * @return Une liste des prescriptions du patient
     */
    public List<Prescription> getPrescriptionsByPatientId(String patientId) {
        return getPatientPrescriptions(patientId);
    }

    /**
     * Récupère toutes les prescriptions.
     *
     * @return Une liste de toutes les prescriptions
     */
    public List<Prescription> getAllPrescriptions() {
        final List<Prescription> prescriptions = prescriptionRepository.findAll();

        // Assertion: Vérification du résultat
        assert prescriptions != null : "La liste des prescriptions ne peut pas être null";

        return prescriptions;
    }

    /**
     * Récupère les prescriptions actives d'un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste des prescriptions actives
     */
    public List<Prescription> getActivePrescriptions(String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        final LocalDate today = LocalDate.now();

        // Récupération des prescriptions non expirées
        final List<Prescription> validPrescriptions =
                prescriptionRepository.findByPatientIdAndExpirationDateGreaterThanEqual(patientId, today);

        // Filtrage pour ne garder que celles qui sont actives
        final List<Prescription> activePrescriptions = validPrescriptions.stream()
                .filter(p -> p.getStatus() == PrescriptionStatus.ACTIVE ||
                            p.getStatus() == PrescriptionStatus.PARTIALLY_DISPENSED)
                .collect(Collectors.toList());

        // Assertion #2: Vérification du résultat
        assert activePrescriptions != null : "La liste des prescriptions actives ne peut pas être null";
        assert activePrescriptions.stream().noneMatch(Prescription::isExpired) :
            "Une prescription expirée a été incluse dans les prescriptions actives";

        return activePrescriptions;
    }

    /**
     * Recherche les prescriptions contenant un médicament spécifique.
     *
     * @param patientId L'ID du patient
     * @param medicationId L'ID du médicament
     * @return Une liste des prescriptions contenant le médicament
     */
    public List<Prescription> findPrescriptionsWithMedication(String patientId, String medicationId) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert medicationId != null && !medicationId.isBlank() :
            "L'ID du médicament ne peut pas être null ou vide";

        // Vérification que le médicament existe
        final boolean medicationExists = medicationService.getMedicationById(medicationId).isPresent();

        // Assertion #2: Vérification que le médicament existe
        assert medicationExists : "Le médicament spécifié n'existe pas";

        return prescriptionRepository.findByPatientIdAndMedicationId(patientId, medicationId);
    }

    /**
     * Met à jour une prescription entière.
     *
     * @param id L'ID de la prescription à mettre à jour
     * @param prescriptionDetails La nouvelle prescription
     * @return La prescription mise à jour, ou null si elle n'existe pas
     */
    public Prescription updatePrescription(String id, Prescription prescriptionDetails) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";
        assert prescriptionDetails != null : "Les détails de la prescription ne peuvent pas être null";

        final Optional<Prescription> existingPrescription = prescriptionRepository.findById(id);

        if (existingPrescription.isEmpty()) {
            return null;
        }

        // Utiliser la réflexion pour modifier l'ID de la prescription (champ final) tout en respectant l'immutabilité
        try {
            // Accéder au champ ID qui est final
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            // Modifier l'ID avec la valeur existante
            idField.set(prescriptionDetails, id);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la modification de l'ID de la prescription", e);
        }

        final Prescription updatedPrescription = prescriptionRepository.save(prescriptionDetails);

        // Assertion #2: Vérification que la mise à jour a bien fonctionné
        assert updatedPrescription != null : "La mise à jour de la prescription a échoué";
        assert updatedPrescription.getId().equals(id) :
            "L'ID de la prescription mise à jour ne correspond pas";

        return updatedPrescription;
    }

    /**
     * Met à jour le statut d'une prescription.
     *
     * @param id L'ID de la prescription
     * @param newStatus Le nouveau statut
     * @return La prescription mise à jour, ou null si elle n'existe pas
     */
    public Prescription updatePrescriptionStatus(String id, PrescriptionStatus newStatus) {
        // Assertion #1: Vérification des paramètres
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";
        assert newStatus != null : "Le nouveau statut ne peut pas être null";

        final Optional<Prescription> existingPrescription = prescriptionRepository.findById(id);

        if (existingPrescription.isEmpty()) {
            return null;
        }

        final Prescription prescription = existingPrescription.get();
        prescription.updateStatus(newStatus);

        final Prescription updatedPrescription = prescriptionRepository.save(prescription);

        // Assertion #2: Vérification que la mise à jour a fonctionné
        assert updatedPrescription.getStatus() == newStatus :
            "Le statut de la prescription n'a pas été correctement mis à jour";

        return updatedPrescription;
    }

    /**
     * Supprime une prescription.
     *
     * @param id L'ID de la prescription à supprimer
     * @return true si la prescription a été supprimée, false si elle n'existait pas
     */
    public boolean deletePrescription(String id) {
        // Assertion #1: Vérification que l'ID n'est pas null ou vide
        assert id != null && !id.isBlank() : "L'ID de la prescription ne peut pas être null ou vide";

        final boolean exists = prescriptionRepository.existsById(id);

        if (!exists) {
            return false;
        }

        prescriptionRepository.deleteById(id);

        // Assertion #2: Vérification que la suppression a fonctionné
        final boolean stillExists = prescriptionRepository.existsById(id);
        assert !stillExists : "La prescription n'a pas été correctement supprimée";

        return true;
    }

    /**
     * Récupère les prescriptions qui expirent bientôt pour un patient.
     *
     * @param patientId L'ID du patient
     * @param daysThreshold Le nombre de jours avant expiration à considérer
     * @return Une liste des prescriptions qui expirent bientôt
     */
    public List<Prescription> getExpiringPrescriptions(String patientId, int daysThreshold) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert daysThreshold > 0 : "Le seuil de jours doit être positif";

        final LocalDate today = LocalDate.now();
        final LocalDate thresholdDate = today.plusDays(daysThreshold);

        final List<Prescription> allPrescriptions = prescriptionRepository.findByPatientId(patientId);

        final List<Prescription> expiringPrescriptions = allPrescriptions.stream()
                .filter(p -> p.getStatus() == PrescriptionStatus.ACTIVE ||
                            p.getStatus() == PrescriptionStatus.PARTIALLY_DISPENSED)
                .filter(p -> !p.isExpired())
                .filter(p -> p.getExpirationDate().isBefore(thresholdDate))
                .collect(Collectors.toList());

        // Assertion #2: Vérification du résultat
        assert expiringPrescriptions != null :
            "La liste des prescriptions expirant bientôt ne peut pas être null";
        assert expiringPrescriptions.stream().allMatch(p ->
                p.getExpirationDate().isAfter(today) &&
                p.getExpirationDate().isBefore(thresholdDate)) :
            "Une prescription en dehors de la période spécifiée a été incluse";

        return expiringPrescriptions;
    }

    /**
     * Génère un résumé des médicaments actuellement prescrits à un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste d'items de prescription actifs
     */
    public List<PrescriptionItem> getCurrentMedications(String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        final List<Prescription> activePrescriptions = getActivePrescriptions(patientId);

        // Extraction de tous les items de prescription des prescriptions actives
        final List<PrescriptionItem> allItems = new ArrayList<>();
        for (Prescription prescription : activePrescriptions) {
            allItems.addAll(prescription.getPrescriptionItems());
        }

        // Déduplication par ID de médicament (garde le plus récent)
        final List<String> processedMedicationIds = new ArrayList<>();
        final List<PrescriptionItem> currentMedications = new ArrayList<>();

        // On trie d'abord par date de prescription décroissante
        final List<Prescription> sortedPrescriptions = activePrescriptions.stream()
                .sorted((p1, p2) -> p2.getPrescriptionDate().compareTo(p1.getPrescriptionDate()))
                .collect(Collectors.toList());

        for (Prescription prescription : sortedPrescriptions) {
            for (PrescriptionItem item : prescription.getPrescriptionItems()) {
                if (!processedMedicationIds.contains(item.getMedicationId())) {
                    currentMedications.add(item);
                    processedMedicationIds.add(item.getMedicationId());
                }
            }
        }

        // Assertion #2: Vérification du résultat
        assert currentMedications != null : "La liste des médicaments actuels ne peut pas être null";

        return currentMedications;
    }

    /**
     * Vérifie les interactions médicamenteuses potentielles pour un patient.
     *
     * @param patientId L'ID du patient
     * @return Une liste de paires de médicaments qui interagissent
     */
    public List<PrescriptionItemPair> checkMedicationInteractions(String patientId) {
        // Assertion #1: Vérification que l'ID du patient n'est pas null ou vide
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";

        final List<PrescriptionItem> currentMedications = getCurrentMedications(patientId);
        final List<PrescriptionItemPair> interactions = new ArrayList<>();

        // Vérification des interactions entre tous les médicaments actifs
        for (int i = 0; i < currentMedications.size(); i++) {
            for (int j = i + 1; j < currentMedications.size(); j++) {
                final PrescriptionItem item1 = currentMedications.get(i);
                final PrescriptionItem item2 = currentMedications.get(j);

                final boolean hasInteraction = medicationService.checkInteraction(
                        item1.getMedicationId(), item2.getMedicationId());

                if (hasInteraction) {
                    interactions.add(new PrescriptionItemPair(item1, item2));
                }
            }
        }

        // Assertion #2: Vérification du résultat
        assert interactions != null : "La liste des interactions ne peut pas être null";

        return interactions;
    }

    /**
     * Purge les prescriptions expirées antérieures à une date.
     *
     * @param olderThan La date limite
     * @return Le nombre de prescriptions supprimées
     */
    public long purgeExpiredPrescriptions(LocalDate olderThan) {
        // Assertion #1: Vérification que la date n'est pas null
        assert olderThan != null : "La date limite ne peut pas être null";

        // Assertion #2: Vérification que la date est dans le passé
        assert !olderThan.isAfter(LocalDate.now()) :
            "La date limite ne peut pas être dans le futur";

        return prescriptionRepository.deleteByExpirationDateBeforeAndStatus(
                olderThan, PrescriptionStatus.EXPIRED);
    }

    /**
     * Récupère les prescriptions les plus récentes pour un patient.
     *
     * @param patientId L'ID du patient
     * @param limit Le nombre maximum de prescriptions à récupérer
     * @return Une liste des prescriptions les plus récentes
     */
    public List<Prescription> getRecentPrescriptions(String patientId, int limit) {
        // Assertion #1: Vérification des paramètres
        assert patientId != null && !patientId.isBlank() :
            "L'ID du patient ne peut pas être null ou vide";
        assert limit > 0 : "La limite doit être positive";

        final List<Prescription> allPrescriptions =
                prescriptionRepository.findByPatientIdOrderByPrescriptionDateDesc(patientId);

        // Limiter le nombre de résultats
        final int effectiveLimit = Math.min(limit, allPrescriptions.size());
        final List<Prescription> recentPrescriptions =
                allPrescriptions.subList(0, effectiveLimit);

        // Assertion #2: Vérification du résultat
        assert recentPrescriptions != null :
            "La liste des prescriptions récentes ne peut pas être null";
        assert recentPrescriptions.size() <= limit :
            "Le nombre de prescriptions récupérées dépasse la limite spécifiée";

        return recentPrescriptions;
    }

    /**
     * Ajoute un item de prescription à une prescription existante.
     * Cette méthode préserve l'immutabilité en créant une nouvelle instance.
     *
     * @param prescriptionId L'ID de la prescription
     * @param newItem L'item à ajouter
     * @return La prescription mise à jour avec le nouvel item, ou null si elle n'existe pas
     */
    public Prescription addItemToPrescription(String prescriptionId, PrescriptionItem newItem) {
        // Assertion #1: Vérification des paramètres
        assert prescriptionId != null && !prescriptionId.isBlank() :
            "L'ID de la prescription ne peut pas être null ou vide";
        assert newItem != null : "L'item à ajouter ne peut pas être null";

        final Optional<Prescription> existingPrescriptionOpt = getPrescriptionById(prescriptionId);

        if (existingPrescriptionOpt.isEmpty()) {
            return null;
        }

        final Prescription existingPrescription = existingPrescriptionOpt.get();

        // Créer une nouvelle liste avec tous les items existants plus le nouveau
        final List<PrescriptionItem> updatedItems = new ArrayList<>(existingPrescription.getPrescriptionItems());
        updatedItems.add(newItem);

        // Cloner la prescription avec la nouvelle liste (immutabilité)
        try {
            // Créer une nouvelle instance avec les mêmes propriétés
            Prescription updatedPrescription = new Prescription(
                existingPrescription.getPatientId(),
                existingPrescription.getPrescriberId(),
                existingPrescription.getPrescriberSpecialty(),
                existingPrescription.getPrescriptionDate(),
                existingPrescription.getValidityPeriodMonths(),
                existingPrescription.isRenewal(),
                existingPrescription.getRenewalNumber()
            );

            // Utiliser la réflexion pour définir l'ID et les autres champs finals
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPrescription, existingPrescription.getId());

            // Définir la liste d'items
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            itemsField.set(updatedPrescription, updatedItems);

            // Mettre à jour les autres champs finals si nécessaire
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(updatedPrescription, existingPrescription.getExpirationDate());

            java.lang.reflect.Field statusField = Prescription.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(updatedPrescription, existingPrescription.getStatus());

            // Assertion #2: Vérification du résultat
            assert updatedPrescription.getPrescriptionItems().size() == existingPrescription.getPrescriptionItems().size() + 1 :
                "L'item n'a pas été correctement ajouté à la prescription";

            return prescriptionRepository.save(updatedPrescription);
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la prescription", e);
        }
    }

    /**
     * Supprime un item de prescription d'une prescription existante.
     * Cette méthode préserve l'immutabilité en créant une nouvelle instance.
     *
     * @param prescriptionId L'ID de la prescription
     * @param itemId L'ID de l'item à supprimer
     * @return La prescription mise à jour sans l'item, ou null si elle n'existe pas
     */
    public Prescription removeItemFromPrescription(String prescriptionId, String itemId) {
        // Assertion #1: Vérification des paramètres
        assert prescriptionId != null && !prescriptionId.isBlank() :
            "L'ID de la prescription ne peut pas être null ou vide";
        assert itemId != null && !itemId.isBlank() :
            "L'ID de l'item à supprimer ne peut pas être null ou vide";

        final Optional<Prescription> existingPrescriptionOpt = getPrescriptionById(prescriptionId);

        if (existingPrescriptionOpt.isEmpty()) {
            return null;
        }

        final Prescription existingPrescription = existingPrescriptionOpt.get();
        final List<PrescriptionItem> currentItems = existingPrescription.getPrescriptionItems();

        // Vérifier que l'item existe
        boolean itemExists = currentItems.stream()
                .anyMatch(item -> itemId.equals(item.getId()));

        if (!itemExists) {
            return existingPrescription; // Retourner la prescription inchangée
        }

        // Créer une nouvelle liste sans l'item à supprimer
        final List<PrescriptionItem> updatedItems = currentItems.stream()
                .filter(item -> !itemId.equals(item.getId()))
                .collect(Collectors.toList());

        // Empêcher la suppression du dernier item (une prescription doit avoir au moins un item)
        if (updatedItems.isEmpty()) {
            throw new IllegalStateException("Une prescription doit contenir au moins un médicament");
        }

        // Cloner la prescription avec la nouvelle liste (immutabilité)
        try {
            // Créer une nouvelle instance avec les mêmes propriétés
            Prescription updatedPrescription = new Prescription(
                existingPrescription.getPatientId(),
                existingPrescription.getPrescriberId(),
                existingPrescription.getPrescriberSpecialty(),
                existingPrescription.getPrescriptionDate(),
                existingPrescription.getValidityPeriodMonths(),
                existingPrescription.isRenewal(),
                existingPrescription.getRenewalNumber()
            );

            // Utiliser la réflexion pour définir l'ID et les autres champs finals
            java.lang.reflect.Field idField = Prescription.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(updatedPrescription, existingPrescription.getId());

            // Définir la liste d'items
            java.lang.reflect.Field itemsField = Prescription.class.getDeclaredField("prescriptionItems");
            itemsField.setAccessible(true);
            itemsField.set(updatedPrescription, updatedItems);

            // Mettre à jour les autres champs finals si nécessaire
            java.lang.reflect.Field expirationField = Prescription.class.getDeclaredField("expirationDate");
            expirationField.setAccessible(true);
            expirationField.set(updatedPrescription, existingPrescription.getExpirationDate());

            java.lang.reflect.Field statusField = Prescription.class.getDeclaredField("status");
            statusField.setAccessible(true);
            statusField.set(updatedPrescription, existingPrescription.getStatus());

            // Assertion #2: Vérification du résultat
            assert updatedPrescription.getPrescriptionItems().size() == existingPrescription.getPrescriptionItems().size() - 1 :
                "L'item n'a pas été correctement supprimé de la prescription";

            return prescriptionRepository.save(updatedPrescription);
        } catch (IllegalStateException e) {
            throw e; // Propager l'exception concernant le dernier item
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la mise à jour de la prescription", e);
        }
    }

    /**
     * Classe interne pour représenter une paire de médicaments en interaction.
     */
    public static class PrescriptionItemPair {
        private final PrescriptionItem item1;
        private final PrescriptionItem item2;

        public PrescriptionItemPair(PrescriptionItem item1, PrescriptionItem item2) {
            this.item1 = item1;
            this.item2 = item2;
        }

        public PrescriptionItem getItem1() {
            return item1;
        }

        public PrescriptionItem getItem2() {
            return item2;
        }
    }
}

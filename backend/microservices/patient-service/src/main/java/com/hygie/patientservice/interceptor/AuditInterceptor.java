package com.hygie.patientservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Intercepteur pour l'audit des accès aux ressources sensibles.
 *
 * Cet intercepteur journalise les accès aux données sensibles pour assurer
 * la traçabilité et la conformité avec les réglementations comme le RGPD.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Component
public class AuditInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuditInterceptor.class);
    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    // Chemins sensibles nécessitant un audit détaillé
    private static final Set<String> SENSITIVE_PATHS = new HashSet<>(Arrays.asList(
        "/api/patients",
        "/api/prescriptions",
        "/api/medications"
    ));

    // Méthodes HTTP nécessitant un audit détaillé
    private static final Set<String> AUDITED_METHODS = new HashSet<>(Arrays.asList(
        "POST", "PUT", "DELETE", "PATCH"
    ));

    private final ThreadLocal<LocalDateTime> requestStartTime = new ThreadLocal<>();

    /**
     * Méthode exécutée avant le traitement de la requête par le contrôleur.
     * Enregistre le début de la requête et journalise les détails initiaux.
     *
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param handler Le handler qui traitera la requête
     * @return true pour continuer le traitement de la requête
     */
    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // Assertion #1: Vérification des paramètres
        assert request != null : "La requête ne peut pas être null";
        assert response != null : "La réponse ne peut pas être null";

        // Enregistrer le temps de début
        requestStartTime.set(LocalDateTime.now());

        // Journaliser l'accès si c'est une ressource sensible
        if (isSensitivePath(request.getRequestURI()) && isAuditedMethod(request.getMethod())) {
            final String username = getCurrentUsername();

            auditLogger.info(
                "AUDIT_ACCESS_ATTEMPT: Utilisateur [{}] tente d'accéder à [{}] via [{}] depuis [{}]",
                username,
                request.getRequestURI(),
                request.getMethod(),
                request.getRemoteAddr()
            );
        }

        // Assertion #2: Vérification post-traitement
        assert requestStartTime.get() != null :
            "Le temps de début de la requête doit être enregistré";

        return true;
    }

    /**
     * Méthode exécutée après le traitement de la requête, mais avant le rendu de la vue.
     * Journalise les détails de l'accès réussi.
     *
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param handler Le handler qui a traité la requête
     * @param modelAndView Le modèle et la vue (peut être null)
     */
    @Override
    public void postHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable ModelAndView modelAndView) {
        // Assertion #1: Vérification des paramètres
        assert request != null : "La requête ne peut pas être null";
        assert response != null : "La réponse ne peut pas être null";

        // Journaliser l'accès réussi si c'est une ressource sensible
        if (isSensitivePath(request.getRequestURI()) && isAuditedMethod(request.getMethod())) {
            final String username = getCurrentUsername();
            final int status = response.getStatus();

            if (status >= 200 && status < 300) {
                auditLogger.info(
                    "AUDIT_ACCESS_SUCCESS: Utilisateur [{}] a accédé à [{}] via [{}], statut [{}]",
                    username,
                    request.getRequestURI(),
                    request.getMethod(),
                    status
                );
            }
        }

        // Assertion #2: Vérification de cohérence
        assert requestStartTime.get() != null :
            "Le temps de début de la requête doit toujours être disponible";
    }

    /**
     * Méthode exécutée après la complétion de la requête.
     * Journalise les détails finals et nettoie les ressources.
     *
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param handler Le handler qui a traité la requête
     * @param ex Exception éventuelle levée durant le traitement
     */
    @Override
    public void afterCompletion(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler,
            @Nullable Exception ex) {
        // Assertion #1: Vérification des paramètres
        assert request != null : "La requête ne peut pas être null";
        assert response != null : "La réponse ne peut pas être null";

        try {
            final LocalDateTime startTime = requestStartTime.get();
            if (startTime != null) {
                final LocalDateTime endTime = LocalDateTime.now();
                final long durationMs = ChronoUnit.MILLIS.between(startTime, endTime);

                // Journaliser l'erreur éventuelle
                if (ex != null || response.getStatus() >= 400) {
                    final String username = getCurrentUsername();

                    auditLogger.warn(
                        "AUDIT_ACCESS_FAILURE: Utilisateur [{}] a eu une erreur en accédant à [{}] via [{}], statut [{}], durée [{}ms], erreur: [{}]",
                        username,
                        request.getRequestURI(),
                        request.getMethod(),
                        response.getStatus(),
                        durationMs,
                        ex != null ? ex.getMessage() : "Aucune exception"
                    );
                }

                // Journaliser les détails de performance si nécessaire
                if (durationMs > 1000) { // Plus d'une seconde
                    logger.warn("Requête lente: {} {} a pris {}ms",
                            request.getMethod(), request.getRequestURI(), durationMs);
                }
            }
        } finally {
            // Nettoyer la ThreadLocal pour éviter les fuites mémoire
            requestStartTime.remove();
        }

        // Assertion #2: Vérification post-nettoyage
        assert requestStartTime.get() == null :
            "La ThreadLocal doit être nettoyée après le traitement";
    }

    /**
     * Détermine si un chemin est considéré comme sensible et nécessite un audit.
     *
     * @param path Le chemin de la requête
     * @return true si le chemin est sensible, false sinon
     */
    private boolean isSensitivePath(String path) {
        // Assertion #1: Vérification du paramètre
        assert path != null : "Le chemin ne peut pas être null";

        for (String sensitivePath : SENSITIVE_PATHS) {
            if (path.startsWith(sensitivePath)) {
                return true;
            }
        }

        // Assertion #2: Vérification interne
        assert SENSITIVE_PATHS != null && !SENSITIVE_PATHS.isEmpty() :
            "L'ensemble des chemins sensibles doit être défini";

        return false;
    }

    /**
     * Détermine si une méthode HTTP nécessite un audit.
     *
     * @param method La méthode HTTP
     * @return true si la méthode nécessite un audit, false sinon
     */
    private boolean isAuditedMethod(String method) {
        // Assertion #1: Vérification du paramètre
        assert method != null : "La méthode ne peut pas être null";

        final boolean result = AUDITED_METHODS.contains(method.toUpperCase());

        // Assertion #2: Vérification interne
        assert AUDITED_METHODS != null && !AUDITED_METHODS.isEmpty() :
            "L'ensemble des méthodes auditées doit être défini";

        return result;
    }

    /**
     * Récupère le nom d'utilisateur courant depuis le contexte de sécurité.
     *
     * @return Le nom d'utilisateur ou "anonyme" si non authentifié
     */
    private String getCurrentUsername() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }

        return "anonyme";
    }
}

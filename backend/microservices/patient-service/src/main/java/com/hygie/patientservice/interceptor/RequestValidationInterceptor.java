package com.hygie.patientservice.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.UUID;

/**
 * Intercepteur pour la validation des requêtes entrantes.
 *
 * Cet intercepteur vérifie la présence et la validité des en-têtes obligatoires,
 * applique des limites de taille pour les requêtes et génère un ID de corrélation
 * unique pour le suivi des requêtes à travers le système.
 *
 * @author Hygie-AI Team
 * @version 1.0.0
 */
@Component
public class RequestValidationInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestValidationInterceptor.class);

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String REQUEST_ID_ATTRIBUTE = "requestId";
    private static final long MAX_CONTENT_LENGTH = 10 * 1024 * 1024; // 10 MB

    /**
     * Méthode exécutée avant le traitement de la requête par le contrôleur.
     * Vérifie et valide la requête entrante.
     *
     * @param request La requête HTTP
     * @param response La réponse HTTP
     * @param handler Le handler qui traitera la requête
     * @return true si la requête est valide et doit être traitée, false sinon
     */
    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        // Assertion #1: Vérification des paramètres
        assert request != null : "La requête ne peut pas être null";
        assert response != null : "La réponse ne peut pas être null";

        // Générer ou récupérer l'ID de corrélation
        final String correlationId = extractOrGenerateCorrelationId(request);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        logger.debug("Requête reçue: {} {} (CorrelationID: {})",
                request.getMethod(), request.getRequestURI(), correlationId);

        // Vérifier la taille du contenu
        final String contentLengthHeader = request.getHeader("Content-Length");
        if (contentLengthHeader != null) {
            try {
                final long contentLength = Long.parseLong(contentLengthHeader);
                if (contentLength > MAX_CONTENT_LENGTH) {
                    logger.warn("Contenu trop volumineux: {} octets (max: {} octets)",
                            contentLength, MAX_CONTENT_LENGTH);
                    response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
                    return false;
                }
            } catch (NumberFormatException e) {
                logger.warn("En-tête Content-Length invalide: {}", contentLengthHeader);
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return false;
            }
        }

        // Assertion #2: Vérification post-validation
        assert request.getAttribute(REQUEST_ID_ATTRIBUTE) != null :
            "L'ID de requête doit être défini après validation";

        return true;
    }

    /**
     * Extrait l'ID de corrélation de la requête ou en génère un nouveau si absent.
     *
     * @param request La requête HTTP
     * @return L'ID de corrélation
     */
    private String extractOrGenerateCorrelationId(HttpServletRequest request) {
        // Assertion #1: Vérification du paramètre
        assert request != null : "La requête ne peut pas être null";

        final String correlationId = request.getHeader(CORRELATION_ID_HEADER);

        if (correlationId != null && !correlationId.trim().isEmpty()) {
            // Vérifier la validité du format UUID
            try {
                UUID.fromString(correlationId);
                return correlationId;
            } catch (IllegalArgumentException e) {
                logger.warn("Format d'ID de corrélation invalide: {}", correlationId);
            }
        }

        // Générer un nouvel ID de corrélation
        final String newCorrelationId = UUID.randomUUID().toString();

        // Assertion #2: Vérification du résultat
        assert newCorrelationId != null && !newCorrelationId.isEmpty() :
            "L'ID de corrélation généré ne peut pas être null ou vide";

        return newCorrelationId;
    }
}

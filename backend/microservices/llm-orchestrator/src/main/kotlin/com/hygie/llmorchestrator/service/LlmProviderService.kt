package com.hygie.llmorchestrator.service

import com.hygie.llmorchestrator.model.LlmModelType
import com.hygie.llmorchestrator.model.LlmRequest
import com.hygie.llmorchestrator.model.LlmResponse
import reactor.core.publisher.Mono

/**
 * Interface pour les services fournisseurs de modèles LLM.
 *
 * Cette interface définit le contrat pour tous les services qui interagissent
 * avec des modèles de langage spécifiques. Elle permet une abstraction cohérente
 * des différents fournisseurs de LLM, facilitant leur orchestration.
 *
 * @author Hygie-AI Team
 */
interface LlmProviderService {

    /**
     * Obtient le type de modèle LLM fourni par ce service.
     *
     * @return Le type de modèle LLM
     */
    fun getModelType(): LlmModelType

    /**
     * Vérifie si le service est opérationnel.
     *
     * @return true si le service est disponible et fonctionnel, false sinon
     */
    fun isOperational(): Boolean

    /**
     * Appelle le service LLM avec un prompt optimisé.
     *
     * @param prompt Le prompt optimisé pour le modèle
     * @param originalRequest La requête originale
     * @return Un Mono contenant la réponse du modèle LLM
     */
    fun callLlmService(prompt: String, originalRequest: LlmRequest): Mono<LlmResponse>

    /**
     * Obtient des métriques sur les performances du modèle.
     *
     * @return Un Mono contenant une map de métriques
     */
    fun getModelMetrics(): Mono<Map<String, Any>>
}

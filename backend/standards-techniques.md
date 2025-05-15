# Standards Techniques Hygie-AI

Ce document définit les standards techniques communs à tous les microservices du projet Hygie-AI, conformément aux règles de développement établies.

## 1. Langages et frameworks

### 1.1 Backend
- **Langage** : Java 17
- **Framework** : Spring Boot 2.7.x
- **Gestion des dépendances** : Maven
- **API REST** : Spring Web MVC avec OpenAPI 3.0
- **Communication inter-services** :
  - REST/HTTP pour les API publiques
  - gRPC pour les communications critiques haute-performance

### 1.2 Base de données
- **Données médicales** : MongoDB 4.9+
- **Données relationnelles** : PostgreSQL 14+
- **Cache** : Redis 6+
- **Analyse temps réel** : ClickHouse

## 2. Standards de code

### 2.1 Principes généraux
- Toutes les classes de modèle doivent être immutables (champs `final`, pas de setters)
- Maximum 50 lignes par méthode
- Minimum 2 assertions par fonction (préconditions et postconditions)
- Pas de construction de flux complexes (pas de goto, récursivité limitée)
- Variables déclarées au plus près de leur utilisation
- Traiter toutes les valeurs de retour des fonctions non-void

### 2.2 Documentation
- JavaDoc complet pour chaque classe et méthode publique
- Format standardisé : description, @param, @return, @throws, @example
- Commentaires obligatoires pour la logique métier complexe

### 2.3 Tests
- Couverture de code minimale : 85%
- Tests unitaires : JUnit 5 + Mockito
- Tests d'intégration : Testcontainers
- Tests de performance : JMeter

### 2.4 Gestion d'erreurs
- Hiérarchie d'exceptions cohérente
- Exceptions métier spécifiques héritant de `HygieServiceException`
- Logging complet des exceptions avec contexte
- Utilisation de Resilience4j pour les circuit-breakers

## 3. Architecture des microservices

### 3.1 Structure standard
```
com.hygie.[service]
  ├── config/         # Configuration Spring
  ├── controller/     # Contrôleurs REST
  ├── exception/      # Exceptions spécifiques
  ├── interceptor/    # Intercepteurs (validation, audit)
  ├── model/          # Modèles de données immutables
  ├── repository/     # Accès aux données
  └── service/        # Logique métier
```

### 3.2 API REST
- Versionning dans l'URL (/api/v1/...)
- Réponses standardisées (success: true/false, data, error)
- Format de documentation OpenAPI pour tous les endpoints
- Validation des entrées avec Bean Validation

### 3.3 Sécurité
- Authentification par JWT
- CORS configuré pour les origines spécifiques
- En-têtes de sécurité HTTP (HSTS, CSP, etc.)
- Chiffrement des données sensibles en transit et au repos

## 4. Observabilité

### 4.1 Logging
- SLF4J + Logback
- Format JSON pour agrégation centralisée
- Niveau INFO par défaut, DEBUG pour packages spécifiques
- Logs d'audit séparés pour les opérations sensibles

### 4.2 Métriques
- Prometheus + Micrometer
- Métriques standards : latence, débit, erreurs, saturation
- Métriques spécifiques par service
- Alertes configurées sur seuils critiques

### 4.3 Tracing
- Propagation des en-têtes de traçage inter-services
- Corrélation des requêtes par ID unique

## 5. Déploiement

### 5.1 Conteneurisation
- Dockerfile multi-stage optimisé
- Image de base : Eclipse Temurin JDK 17 Alpine
- Configuration par variables d'environnement

### 5.2 Kubernetes
- Limites de ressources explicites
- Liveness et readiness probes
- NetworkPolicy restrictive
- Configuration des secrets via Vault
- Autoscaling horizontal basé sur métriques

## 6. Conformité

### 6.1 RGPD
- Pseudonymisation systématique des données personnelles
- Logs d'accès aux données sensibles
- Mécanismes d'effacement et de portabilité des données

### 6.2 HDS
- Isolation des données de santé
- Chiffrement des données sensibles
- Traçabilité des accès
- Sauvegardes chiffrées

# Architecture Backend Hygie-AI

## 1. Vue d'ensemble

L'architecture backend de Hygie-AI est conçue selon les principes de microservices, avec une stricte séparation des responsabilités et une forte emphase sur la sécurité, la traçabilité et la fiabilité des données médicales.

## 2. Principes architecturaux

- **Immutabilité** : Toutes les entités métier sont immutables pour éviter les effets secondaires.
- **Audit complet** : Toutes les opérations sont journalisées avec un niveau de détail adapté à leur sensibilité.
- **Sécurité en profondeur** : Multiples couches de sécurité à chaque niveau de l'architecture.
- **Validation stricte** : Validation des données en entrée/sortie avec assertions multiples.
- **Résilience** : Tolérance aux pannes avec circuit breakers et retry policies.
- **Observabilité** : Métriques, traces et logs unifiés pour une visibilité complète.

## 3. Services et responsabilités

### 3.1 API Gateway
- Point d'entrée unique pour les clients
- Routage des requêtes
- Rate limiting et protection contre les attaques
- Transformation des requêtes/réponses
- Monitoring centralisé

### 3.2 Service Registry
- Découverte dynamique des services
- Health checks
- Configuration centralisée

### 3.3 Identity Service
- Authentification OAuth 2.0/OpenID Connect
- Gestion des tokens JWT
- Gestion des utilisateurs et rôles
- Vérification des identités professionnelles
- Audit des connexions et tentatives

### 3.4 Patient Service
- Gestion des données patients
- Recherche et récupération des patients
- Analyse d'éligibilité pour les BPM
- Pseudonymisation des données sensibles
- Export/import FHIR

### 3.5 Medication Service
- Catalogue des médicaments
- Analyse des interactions médicamenteuses
- Intégration bases de données médicamenteuses
- Historique des modifications de statut

### 3.6 Prescription Service
- Gestion des prescriptions
- Validation des prescriptions
- Détection des redondances thérapeutiques
- Historique des modifications

### 3.7 LLM Orchestrator Service
- Interface gRPC haute performance
- Abstraction des modèles LLM
- Stratégies de routage contextuel
- Engineering des prompts dynamiques
- Validation ACUTE des réponses
- Traçabilité complète

### 3.8 Medical Analysis Service
- Règles STOPP/START pour personnes âgées
- Détection d'ajustements posologiques nécessaires
- Analyse des problèmes d'observance
- Génération de recommandations contextuelles
- Score de confiance pour recommandations

### 3.9 Document Generation Service
- Templates de documents
- Génération structurée
- Personnalisation
- Export multi-format (PDF, DOCX, HTML)
- Signature numérique

### 3.10 SESAM-Vitale Service
- Intégration cartes CPS/Vitale
- Vérification droits ADRi
- Génération FSE
- Facturation automatisée BPM
- Télétransmission

### 3.11 Notification Service
- Notifications temps réel
- Multiples canaux (in-app, email, SMS)
- Priorisation et agrégation
- Préférences personnalisables

### 3.12 Analytics Service
- Métriques cliniques et économiques
- Intégration ClickHouse
- Anonymisation des données
- Détection de tendances

### 3.13 Document Import Service
- OCR ordonnances
- Extraction structurée
- Normalisation terminologies
- Import multi-source

### 3.14 Billing Service
- Gestion de la facturation
- Suivi des paiements
- Intégration avec SESAM-Vitale
- Exports comptables

## 4. Communication inter-services

### 4.1 Synchrone
- REST (OpenAPI) pour les API publiques
- gRPC pour les communications critiques inter-services

### 4.2 Asynchrone
- Kafka pour les événements métier
- RabbitMQ pour les files de tâches et communications asynchrones

## 5. Persistance des données

### 5.1 Données transactionnelles
- MongoDB pour les données médicales (patients, prescriptions)
- PostgreSQL pour les données relationnelles (utilisateurs, facturation)

### 5.2 Caching
- Redis pour le caching distribué et sessions

### 5.3 Analytique
- ClickHouse pour l'analyse en temps réel

### 5.4 Secrets
- HashiCorp Vault pour la gestion des secrets

## 6. Sécurité

### 6.1 Authentification et Autorisation
- JWT pour l'authentification stateless
- RBAC pour le contrôle d'accès granulaire
- Scope-based authorization pour les API

### 6.2 Chiffrement
- TLS 1.3 pour toutes les communications
- AES-256 pour le chiffrement des données sensibles
- Chiffrement transparent pour les données au repos

### 6.3 Audit
- Journalisation de tous les accès aux données sensibles
- Signatures cryptographiques pour l'intégrité des logs
- Conservation longue durée des journaux d'audit

## 7. Infrastructure

### 7.1 Conteneurisation
- Images Docker optimisées
- Configuration via variables d'environnement
- Secrets injectés (non buildés dans les images)

### 7.2 Orchestration
- Kubernetes pour l'orchestration
- Helm charts pour le déploiement
- Istio pour le service mesh

### 7.3 Scalabilité
- Autoscaling horizontal basé sur les métriques
- Optimisation des ressources

### 7.4 Observabilité
- Prometheus pour les métriques
- Jaeger pour le tracing distribué
- ELK stack pour la centralisation des logs
- Grafana pour les dashboards

## 8. Résilience

### 8.1 Circuit Breakers
- Resilience4j pour la gestion des pannes

### 8.2 Retry Policies
- Backoff exponentiel avec jitter

### 8.3 Fallbacks
- Dégradation gracieuse des fonctionnalités
- Modes offline pour les fonctionnalités critiques

### 8.4 Disaster Recovery
- Sauvegardes chiffrées multirégions
- Tests de reprise réguliers

## 9. Conformité

### 9.1 RGPD
- Privacy by Design dans tous les services
- Minimisation des données
- Journalisation des consentements
- Mécanismes de purge automatique

### 9.2 HDS
- Hébergement conforme HDS
- Cloisonnement des données
- Chiffrement renforcé

### 9.3 Traçabilité pharmaceutique
- Audit trail complet des actions
- Conservation selon obligations légales

## 10. Orchestrateur LLM

- Chargement des modèles locaux (BioMistral, MedFound-LLaMA3, Hippomistral) via `transformers` (device_map="auto", torch.float16).
- Cache LRU (`functools.lru_cache(maxsize=3)`) évitant le rechargement fréquent.
- Stratégies de génération :
  - **ensemble_generate** : vote majoritaire entre tous les modèles disponibles.
  - **cascade_generate** : pipeline BioMistral → MedFound → Hippomistral.
  - **hybrid_generate_summary** : combinaison pipeline cascade + vote d’ensemble.
- Fallback vers l’API Hugging Face (`_hf_generate`) si modèle local absent ou erreur.
- Benchmarks de performance (CSV <50 ms, PDF <20 ms) et tests unitaires/intégration pour valider la cohérence et la latence.

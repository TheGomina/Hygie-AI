# Hygie-AI

[![CI](https://github.com/TheGomina/Hygie-AI/actions/workflows/ci.yml/badge.svg)](https://github.com/TheGomina/Hygie-AI/actions/workflows/ci.yml)

[![Coverage](https://codecov.io/gh/TheGomina/Hygie-AI/branch/main/graph/badge.svg?token=32faca0c-fb12-478f-b368-171bc78de989&threshold=85)](https://codecov.io/gh/TheGomina/Hygie-AI)

## Plateforme Révolutionnaire de Pharmacie Clinique Orchestrant des LLM Médicaux

Hygie-AI transforme la pharmacie clinique en orchestrant des LLMs médicaux spécialisés (BioMistral, Hippo-Mistral, MedFound) pour améliorer les Bilans Partagés de Médication (BPM) et créer un nouveau standard de pratique pharmaceutique.

## Caractéristiques principales

- Orchestration intelligente de LLMs médicaux spécialisés
- Automatisation et enrichissement des Bilans Partagés de Médication
- Détection avancée des problèmes complexes de médication
- Analyses contextuelles tenant compte du profil patient complet
- Recommandations pharmaceutiques personnalisées et sourcées
- Génération de comptes-rendus structurés pour les médecins
- Facturation automatisée SESAM-Vitale
- Interface utilisateur intuitive et accessible

## Architecture technique

- **Backend** : Microservices FHIR-natifs (Java/Kotlin avec Spring Boot, Python avec FastAPI)
- **Orchestrateur LLM** : Service central gérant les modèles d'IA avec routage intelligent
- **Persistance** : Architecture polyglot (PostgreSQL, MongoDB, Redis)
- **Frontend** : SPA avec React/TypeScript et Next.js ou Vue.js avec Nuxt
- **Sécurité** : Chiffrement AES-256, TLS 1.3, conformité RGPD, certification HDS

## Démarrage rapide

```bash
# Cloner le dépôt
git clone https://github.com/votre-organisation/hygie-ai.git
cd hygie-ai

# Installation des dépendances et configuration initiale
./setup.sh

# Démarrer l'environnement de développement
docker-compose up -d
```

## Licence

Ce projet est sous licence propriétaire. Tous droits réservés.

## Algorithmes pharmaceutiques et Base de Connaissances

Hygie-AI intègre les concepts clés du Chapitre 2 de *AlgorithmesEtBaseDeConnaissances.pdf* (ANAP) :  

- **Algorithme pharmaceutique (AP)** : modélisation d’une « situation à risque » puis définition d’une conduite à tenir pour l’intervention pharmaceutique.  
- **Base de Connaissances des AP (BDC-AP)** : ontologie standardisée de règles, classifications de gravité (EI, PLP, IP) et références bibliographiques.  

**Structure générique d’un AP :**  
1. Descriptif de la situation à risque  
2. Règle en langage logique  
3. Règle en langage informatique  
4. Conduite à tenir  
5. Références bibliographiques  
6. Référencement dans la BDC  
7. Modèle générique d’un AP  

Se référer à `backend/resources/AlgorithmesEtBaseDeConnaissances.pdf` pour plus de détails.  

## Conformité aux règles de développement

Ce projet adhère strictement aux 20 règles de développement Hygie-AI, incluant:

- Limitation des fonctions à 50 lignes maximum
- Minimum de 2 assertions par fonction
- Structures de données immutables privilégiées
- Tests avec couverture minimum de 85%
- Documentation exhaustive
- Validation technique et clinique des algorithmes médicaux

## Performance et Observabilité

Le projet intègre des outils de performance et d'observabilité pour garantir un suivi précis et une optimisation continue:

### Tests de performance

```bash
# Exécuter les benchmarks
pytest tests/test_benchmarks.py -v

# Lancer les profils détaillés
pytest tests/test_profiling.py -v
```

### Métriques Prometheus

L'application expose des métriques Prometheus sur l'endpoint `/metrics`, incluant:

- `bmp_requests_total` - Compteur de requêtes HTTP
- `bmp_request_processing_seconds` - Latence de traitement des requêtes
- `bmp_csv_load_seconds` - Temps de chargement des fichiers CSV
- `bmp_pdf_load_seconds` - Temps de traitement des fichiers PDF

### Déploiement avec Helm

```bash
# Installer le chart Helm
helm install bmp-service kubernetes/helm/bmp-service

# Vérifier le déploiement
kubectl get pods -l app.kubernetes.io/name=bmp-service

# Accéder au service (port-forward)
kubectl port-forward svc/bmp-service 8000:8000
```

Le chart Helm inclut:
- Déploiement du service BMP avec scaling automatique
- Configuration du ServiceMonitor pour Prometheus
- Dashboard Grafana préconfiguré pour visualiser les métriques

Pour importer le dashboard dans Grafana manuellement, utilisez le fichier JSON dans `docs/grafana/dashboard-bmp-service.json`.

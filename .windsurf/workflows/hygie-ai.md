---
description: Workflow de développement Hygie-AI: process standardisé pour assurer qualité, sécurité et efficacité dans le développement de notre plateforme de pharmacie clinique basée sur LLM.
---

# Workflow de Développement Hygie-AI

Ce workflow définit le processus standard pour développer, tester et déployer les fonctionnalités de Hygie-AI.

## Initialisation d'une Fonctionnalité

- Créer un ticket détaillé dans le système de suivi
- Créer une branche feature/* depuis develop
- Définir les tests avant implémentation (TDD)
- Pour les fonctionnalités cliniques: identifier les validateurs techniques ET cliniques

## Phase de Développement

- Écrire les tests unitaires avant le code
- Implémenter en respectant les 20 règles de développement
- Exécuter tests locaux après chaque modification significative
- Vérifier la minimisation des données et le chiffrement des données sensibles
- Documenter selon le standard établi incluant sources et niveau de confiance
- Assurer une couverture de tests >85%

## Revue de Code

- Créer une Pull Request (description, référence au ticket)
- Assigner au moins deux reviewers (développeur senior, expert domaine)
- Pour modules cliniques: double revue technique ET clinique obligatoire
- Vérifier: conformité aux règles, couverture de tests, documentation, sécurité
- Valider l'absence de biais dans les recommandations médicales
- Obtenir minimum 2 approbations avant de procéder

## Intégration et Tests

- Merger dans develop après approbation
- Vérifier les tests d'intégration automatiques incluant tests de biais
- Déployer en staging
- Tracer précisément les versions des modèles LLM et leurs paramètres
- Exécuter tests manuels complémentaires
- Si problèmes: créer ticket, fixer dans branche bugfix/

## Préparation des Releases

- Créer branche release/* depuis develop
- Semaine de stabilisation (uniquement corrections)
- Tests complets: automatisés, performance, sécurité, scénarios cliniques
- Documenter exhaustivement les versions des LLM utilisés et leurs paramètres
- Préparer documentation et notes de version
- Merger dans main et develop après validation

## Déploiement et Surveillance

- Déploiement progressif (canary/blue-green)
- Surveillance active des métriques, erreurs et niveaux de confiance
- Collecter feedback utilisateurs et pharmaciens référents
- Auditer les recommandations pour détecter d'éventuels biais
- Identifier améliorations pour prochaine itération

## Règles Spécifiques LLM

- Versionnage strict des modèles avec tests de non-régression
- Gouvernance des prompts avec revue clinique obligatoire
- Audit régulier des réponses et ajustement selon pertinence clinique
- Traçabilité complète permettant de relier chaque recommandation à ses sources

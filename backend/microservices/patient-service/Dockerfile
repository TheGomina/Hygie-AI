# Utilisation d'une image de base sécurisée et légère
FROM eclipse-temurin:17-jre-alpine AS builder

# Arguments de build pour la version de l'application
ARG JAR_FILE=target/*.jar
ARG APP_NAME=patient-service
ARG APP_VERSION=1.0.0

# Labels informatifs
LABEL maintainer="Hygie-AI Team <contact@hygie-ai.com>"
LABEL description="Patient Service pour la plateforme Hygie-AI - Gestion des patients, médicaments et prescriptions"
LABEL version="${APP_VERSION}"

# Création d'un utilisateur non-root pour l'exécution
RUN addgroup -S hygie && adduser -S hygie -G hygie

# Variables d'environnement
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JAVA_OPTS="-Xms256m -Xmx512m" \
    APP_NAME=${APP_NAME} \
    APP_VERSION=${APP_VERSION}

# Copie du JAR dans le conteneur
COPY ${JAR_FILE} /app/app.jar

# Définition du répertoire de travail
WORKDIR /app

# Vérification et optimisation du JAR
RUN sha256sum app.jar > app.jar.sha256 && \
    # Extraction du contenu du JAR pour optimiser les layers Docker
    mkdir -p extracted && \
    java -Djarmode=layertools -jar app.jar extract --destination extracted && \
    # Attribution des permissions
    chown -R hygie:hygie /app

# Construction de l'image finale en plusieurs étapes pour réduire la taille
FROM eclipse-temurin:17-jre-alpine

# Copie des utilisateurs de l'étape précédente
COPY --from=builder /etc/passwd /etc/passwd
COPY --from=builder /etc/group /etc/group

# Variables d'environnement
ENV SPRING_OUTPUT_ANSI_ENABLED=ALWAYS \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication" \
    TZ=Europe/Paris

# Installation des dépendances minimales
RUN apk add --no-cache tzdata curl && \
    # Création des répertoires nécessaires
    mkdir -p /app/logs /app/config && \
    # Attribution des permissions
    chown -R hygie:hygie /app

# Définition du répertoire de travail
WORKDIR /app

# Copie des composants extraits du JAR
COPY --from=builder --chown=hygie:hygie /app/extracted/dependencies/ ./
COPY --from=builder --chown=hygie:hygie /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=hygie:hygie /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=hygie:hygie /app/extracted/application/ ./

# Exposition du port
EXPOSE 8081

# Passage à l'utilisateur non-root
USER hygie

# Vérification de la santé
HEALTHCHECK --interval=30s --timeout=3s --retries=3 CMD curl -f http://localhost:8081/patient-service/actuator/health || exit 1

# Commande d'exécution
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]

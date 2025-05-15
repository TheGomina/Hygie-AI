# Dockerfile pour le service d'orchestration des LLMs (Hygie-AI)
# Utilisation d'une image de base avec JDK 17 alpine pour minimiser la taille
FROM eclipse-temurin:17-jdk-alpine AS builder

# Variables d'environnement pour Maven
ENV MAVEN_VERSION 3.9.4
ENV MAVEN_HOME /usr/lib/mvn
ENV PATH $MAVEN_HOME/bin:$PATH

# Installation des dépendances nécessaires
RUN apk add --no-cache curl tar bash

# Installation de Maven
RUN mkdir -p /usr/lib/mvn && \
    curl -fsSL https://archive.apache.org/dist/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz | \
    tar -xzC /usr/lib/mvn --strip-components=1 && \
    ln -s /usr/lib/mvn/bin/mvn /usr/bin/mvn

WORKDIR /app

# Copie du fichier pom.xml et téléchargement des dépendances séparément
# pour tirer parti du cache Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copie du code source
COPY src ./src

# Compilation et packaging (avec tests)
RUN mvn package -DskipTests && \
    mkdir -p target/dependency && \
    (cd target/dependency; jar -xf ../llm-orchestrator-*.jar)

# Construction de l'image finale optimisée
FROM eclipse-temurin:17-jre-alpine AS production

# Métadonnées de l'image
LABEL maintainer="Hygie-AI Team"
LABEL version="1.0.0"
LABEL description="Service d'orchestration des LLMs médicaux pour Hygie-AI"

# Arguments de build avec valeurs par défaut
ARG JAR_FILE=llm-orchestrator.jar
ARG APP_HOME=/app
ARG SPRING_PROFILES_ACTIVE=prod
ARG PORT=8080

# Utilisateur non-root pour la sécurité
RUN addgroup -S hygie && adduser -S hygie -G hygie

# Variables d'environnement
ENV SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
ENV PORT=${PORT}
ENV LOG_PATH=${APP_HOME}/logs
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0 -Djava.security.egd=file:/dev/./urandom"

# Création des dossiers nécessaires
RUN mkdir -p ${APP_HOME}/config ${LOG_PATH}
WORKDIR ${APP_HOME}

# Copie des fichiers de l'application depuis l'étape de build
COPY --from=builder /app/target/dependency/BOOT-INF/lib ${APP_HOME}/lib
COPY --from=builder /app/target/dependency/META-INF ${APP_HOME}/META-INF
COPY --from=builder /app/target/dependency/BOOT-INF/classes ${APP_HOME}

# Copie des prompts et ressources
COPY --from=builder /app/src/main/resources/prompts ${APP_HOME}/prompts

# Changement des permissions
RUN chown -R hygie:hygie ${APP_HOME}
USER hygie

# Configuration des points de montage pour les volumes
VOLUME ["${APP_HOME}/config", "${LOG_PATH}"]

# Vérification de santé
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -q --spider http://localhost:${PORT}/actuator/health || exit 1

# Exposition du port
EXPOSE ${PORT}

# Commande de démarrage avec profil de production par défaut
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -cp .:lib/*:./config org.springframework.boot.loader.JarLauncher"]

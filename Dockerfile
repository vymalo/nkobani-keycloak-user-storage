ARG TAG=22.0.1
ARG PLUGIN_VERSION=0.3.0

FROM curlimages/curl AS DOWNLOADER

WORKDIR /app

ENV NKOBANI_USER_STORAGE_PLUGIN_VERSION=$PLUGIN_VERSION

# TODO webhook ?
RUN curl -H "Accept: application/zip" https://github.com/vymalo/keycloak-webhook/releases/download/v${NKOBANI_USER_STORAGE_PLUGIN_VERSION}/keycloak-webhook-${NKOBANI_USER_STORAGE_PLUGIN_VERSION}-all.jar -o /app/keycloak-nkobani-user-${NKOBANI_USER_STORAGE_PLUGIN_VERSION}.jar -Li


FROM quay.io/keycloak/keycloak:${TAG}

ENV NKOBANI_USER_STORAGE_PLUGIN_VERSION=$PLUGIN_VERSION
ENV KEYCLOAK_DIR /opt/keycloak
ENV KC_PROXY edge

LABEL maintainer="Stephane, Segning Lambou <selastlambou@gmail.com>"

USER 0

RUN mkdir $JBOSS_HOME/providers

COPY --from=DOWNLOADER /app/keycloak-nkobani-user-storage-${NKOBANI_USER_STORAGE_PLUGIN_VERSION}.jar $JBOSS_HOME/providers/

RUN $KEYCLOAK_DIR/bin/kc.sh build

USER 1000
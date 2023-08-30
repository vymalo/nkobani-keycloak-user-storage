ARG TAG=22.0.1

FROM quay.io/keycloak/keycloak:${TAG}

ENV NKOBANI_USER_STORAGE_PLUGIN_VERSION 0.3.0

ENV KEYCLOAK_DIR /opt/keycloak
ENV KC_PROXY edge

LABEL maintainer="Stephane, Segning Lambou <selastlambou@gmail.com>"

USER 0

# TODO webhook ?
COPY build/libs/nkobani-keycloak-user-storage-${NKOBANI_USER_STORAGE_PLUGIN_VERSION}-all.jar $KEYCLOAK_DIR/providers/keycloak-nkobani-user-storage.jar

RUN $KEYCLOAK_DIR/bin/kc.sh build

USER 1000
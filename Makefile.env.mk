# Docker env
DOCKER_REGISTRY ?= docker.io
DOCKER_ORG      ?= enmasseproject
DOCKER          ?= docker
PROJECT_PREFIX  ?= enmasse
PROJECT_NAME    ?= $(shell basename $(CURDIR))
COMMIT 			?= $(shell git rev-parse HEAD)
VERSION         ?= $(shell grep "release.version" $(TOPDIR)/pom.properties| cut -d'=' -f2)
MAVEN_VERSION   ?= $(shell grep "maven.version" $(TOPDIR)/pom.properties| cut -d'=' -f2)
TAG             ?= latest

# Go settings
GOPATH          := $(abspath $(TOPDIR))/go
GOPRJ           := $(GOPATH)/src/github.com/enmasseproject/enmasse
export GOPATH

# Image settings
DOCKER_REGISTRY_PREFIX ?= $(DOCKER_REGISTRY)/
IMAGE_VERSION 		   ?= $(TAG)
ADDRESS_SPACE_CONTROLLER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/address-space-controller:$(IMAGE_VERSION)"
API_SERVER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/api-server:$(IMAGE_VERSION)"
STANDARD_CONTROLLER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/standard-controller:$(IMAGE_VERSION)"
ROUTER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/router:$(IMAGE_VERSION)"
ARTEMIS_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/artemis:$(IMAGE_VERSION)"
ARTEMIS_PLUGIN_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/broker-plugin:$(IMAGE_VERSION)"
TOPIC_FORWARDER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/topic-forwarder:$(IMAGE_VERSION)"
AGENT_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/agent:$(IMAGE_VERSION)"
MQTT_GATEWAY_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/mqtt-gateway:$(IMAGE_VERSION)"
MQTT_LWT_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/mqtt-lwt:$(IMAGE_VERSION)"
NONE_AUTHSERVICE_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/none-authservice:$(IMAGE_VERSION)"
KEYCLOAK_IMAGE ?= "jboss/keycloak-openshift:4.8.3.Final"
KEYCLOAK_PLUGIN_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/keycloak-plugin:$(IMAGE_VERSION)"
KEYCLOAK_CONTROLLER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/keycloak-controller:$(IMAGE_VERSION)"
SERVICE_BROKER_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/service-broker:$(IMAGE_VERSION)"
CONSOLE_IMAGE ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/console:$(IMAGE_VERSION)"
PROMETHEUS_IMAGE ?= "prom/prometheus:v2.4.3"
ALERTMANAGER_IMAGE ?= "prom/alertmanager:v0.15.2"
GRAFANA_IMAGE ?= "grafana/grafana:5.3.1"
KUBE_STATE_METRICS_IMAGE ?= "quay.io/coreos/kube-state-metrics:v1.4.0"
QDROUTERD_BASE_IMAGE ?= "enmasseproject/qdrouterd-base:1.6.0-rc1"
OAUTH_PROXY_IMAGE ?= "openshift/oauth-proxy:latest"

ENMASSE_CONTROLLER_MANAGER_IMAGE   ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/enmasse-controller-manager:$(IMAGE_VERSION)"

IOT_AUTH_SERVICE_IMAGE             ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-auth-service:$(IMAGE_VERSION)"
IOT_DEVICE_REGISTRY_IMAGE          ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-device-registry:$(IMAGE_VERSION)"
IOT_GC_IMAGE                       ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-gc:$(IMAGE_VERSION)"
IOT_HTTP_ADAPTER_IMAGE             ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-http-adapter:$(IMAGE_VERSION)"
IOT_MQTT_ADAPTER_IMAGE             ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-mqtt-adapter:$(IMAGE_VERSION)"
IOT_TENANT_SERVICE_IMAGE           ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/iot-tenant-service:$(IMAGE_VERSION)"
QDR_PROXY_CONFIGURATOR_IMAGE       ?= "$(DOCKER_REGISTRY_PREFIX)$(DOCKER_ORG)/qdr-proxy-configurator:$(IMAGE_VERSION)"


DEFAULT_PROJECT ?= "enmasse-infra"
HONO_VERSION ?= "0.9"
IMAGE_PULL_POLICY ?= "Always"

IMAGE_ENV=ADDRESS_SPACE_CONTROLLER_IMAGE=$(ADDRESS_SPACE_CONTROLLER_IMAGE) \
			API_SERVER_IMAGE=$(API_SERVER_IMAGE) \
			STANDARD_CONTROLLER_IMAGE=$(STANDARD_CONTROLLER_IMAGE) \
			ROUTER_IMAGE=$(ROUTER_IMAGE) \
			ARTEMIS_IMAGE=$(ARTEMIS_IMAGE) \
			ARTEMIS_PLUGIN_IMAGE=$(ARTEMIS_PLUGIN_IMAGE) \
			TOPIC_FORWARDER_IMAGE=$(TOPIC_FORWARDER_IMAGE) \
			SUBSERV_IMAGE=$(SUBSERV_IMAGE) \
			SERVICE_BROKER_IMAGE=$(SERVICE_BROKER_IMAGE) \
			NONE_AUTHSERVICE_IMAGE=$(NONE_AUTHSERVICE_IMAGE) \
			AGENT_IMAGE=$(AGENT_IMAGE) \
			KEYCLOAK_IMAGE=$(KEYCLOAK_IMAGE) \
			KEYCLOAK_PLUGIN_IMAGE=$(KEYCLOAK_PLUGIN_IMAGE) \
			KEYCLOAK_CONTROLLER_IMAGE=$(KEYCLOAK_CONTROLLER_IMAGE) \
			MQTT_GATEWAY_IMAGE=$(MQTT_GATEWAY_IMAGE) \
			MQTT_LWT_IMAGE=$(MQTT_LWT_IMAGE) \
			CONSOLE_IMAGE=$(CONSOLE_IMAGE) \
			PROMETHEUS_IMAGE=$(PROMETHEUS_IMAGE) \
			ALERTMANAGER_IMAGE=$(ALERTMANAGER_IMAGE) \
			GRAFANA_IMAGE=$(GRAFANA_IMAGE) \
			KUBE_STATE_METRICS_IMAGE=$(KUBE_STATE_METRICS_IMAGE) \
			OAUTH_PROXY_IMAGE=$(OAUTH_PROXY_IMAGE) \
			QDROUTERD_BASE_IMAGE=$(QDROUTERD_BASE_IMAGE) \
			ENMASSE_CONTROLLER_MANAGER_IMAGE=$(ENMASSE_CONTROLLER_MANAGER_IMAGE) \
			QDR_PROXY_CONFIGURATOR_IMAGE=$(QDR_PROXY_CONFIGURATOR_IMAGE) \
			IOT_AUTH_SERVICE_IMAGE=$(IOT_AUTH_SERVICE_IMAGE) \
			IOT_DEVICE_REGISTRY_IMAGE=$(IOT_DEVICE_REGISTRY_IMAGE) \
			IOT_GC_IMAGE=$(IOT_GC_IMAGE) \
			IOT_HTTP_ADAPTER_IMAGE=$(IOT_HTTP_ADAPTER_IMAGE) \
			IOT_MQTT_ADAPTER_IMAGE=$(IOT_MQTT_ADAPTER_IMAGE) \
			IOT_TENANT_SERVICE_IMAGE=$(IOT_TENANT_SERVICE_IMAGE) \
			IMAGE_PULL_POLICY=$(IMAGE_PULL_POLICY) \
			HONO_VERSION=${HONO_VERSION} \
			ENMASSE_VERSION=$(VERSION) \
			MAVEN_VERSION=$(MAVEN_VERSION)

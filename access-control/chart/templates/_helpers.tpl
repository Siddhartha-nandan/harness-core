{{/*
Expand the name of the chart.
*/}}
{{- define "access-control.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "access-control.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "access-control.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "access-control.labels" -}}
helm.sh/chart: {{ include "access-control.chart" . }}
{{ include "access-control.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "access-control.selectorLabels" -}}
app.kubernetes.io/name: {{ include "access-control.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "access-control.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "access-control.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Manage Access-Control Secrets
USAGE:
{{- "access-control.generateSecrets" (dict "ctx" $)}}
*/}}

{{- define "access-control.generateSecrets" }}
    {{- $ := .ctx }}
    {{- $hasAtleastOneSecret := false }}
    {{- $localESOSecretCtxIdentifier := (include "harnesscommon.secrets.localESOSecretCtxIdentifier" (dict "ctx" $ )) }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "IDENTITY_SERVICE_SECRET")) "true" }}
    {{- $hasAtleastOneSecret = true }}
IDENTITY_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.IDENTITY_SERVICE_SECRET | b64enc }}'
{{- if not (quote .ctx.Values.secrets.default.CF_CLIENT_API_KEY | empty) }}
CF_CLIENT_API_KEY: '{{ .ctx.Values.secrets.default.CF_CLIENT_API_KEY | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.ACCESS_CONTROL_CLIENT_SERVICE_SECRET | empty) }}
ACCESS_CONTROL_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.ACCESS_CONTROL_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.ACCESS_CONTROL_SERVICE_SECRET | empty) }}
ACCESS_CONTROL_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.ACCESS_CONTROL_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.ACCOUNT_SERVICE_SECRET | empty) }}
ACCOUNT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.ACCOUNT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY | empty) }}
APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY: '{{ .ctx.Values.secrets.default.APPDYNAMICS_AGENT_ACCOUNT_ACCESS_KEY | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.DEFAULT_SERVICE_SECRET | empty) }}
DEFAULT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.DEFAULT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.EVENTS_CONFIG_REDIS_PASSWORD | empty) }}
EVENTS_CONFIG_REDIS_PASSWORD: '{{ .ctx.Values.secrets.default.EVENTS_CONFIG_REDIS_PASSWORD | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.FEATURE_FLAG_CLIENT_SECRET | empty) }}
FEATURE_FLAG_CLIENT_SECRET: '{{ .ctx.Values.secrets.default.FEATURE_FLAG_CLIENT_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.JWT_AUTH_SECRET | empty) }}
JWT_AUTH_SECRET: '{{ .ctx.Values.secrets.default.JWT_AUTH_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.LOCK_CONFIG_REDIS_PASSWORD | empty) }}
LOCK_CONFIG_REDIS_PASSWORD: '{{ .ctx.Values.secrets.default.LOCK_CONFIG_REDIS_PASSWORD | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.ORGANIZATION_CLIENT_SERVICE_SECRET | empty) }}
ORGANIZATION_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.ORGANIZATION_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.PROJECT_CLIENT_SERVICE_SECRET | empty) }}
PROJECT_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.PROJECT_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.RESOURCE_GROUP_CLIENT_SERVICE_SECRET | empty) }}
RESOURCE_GROUP_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.RESOURCE_GROUP_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.SEGMENT_APIKEY | empty) }}
SEGMENT_APIKEY: '{{ .ctx.Values.secrets.default.SEGMENT_APIKEY | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.SERVICEACCOUNT_CLIENT_SERVICE_SECRET | empty) }}
SERVICEACCOUNT_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.SERVICEACCOUNT_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.USER_CLIENT_SERVICE_SECRET | empty) }}
USER_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.USER_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.USER_GROUP_CLIENT_SERVICE_SECRET | empty) }}
USER_GROUP_CLIENT_SERVICE_SECRET: '{{ .ctx.Values.secrets.default.USER_GROUP_CLIENT_SERVICE_SECRET | b64enc }}'
{{- end }}
{{- if not (quote .ctx.Values.secrets.default.EVENTS_CONFIG_REDIS_SSL_CA_TRUST_STORE_PASSWORD | empty) }}
EVENTS_CONFIG_REDIS_SSL_CA_TRUST_STORE_PASSWORD: '{{ .ctx.Values.secrets.default.EVENTS_CONFIG_REDIS_SSL_CA_TRUST_STORE_PASSWORD | b64enc }}'
{{- end }}
    {{- end }}
    {{- if not $hasAtleastOneSecret }}
{}
    {{- end }}
{{- end }}
{{/*
Helper function for pullSecrets at chart level or global level.
*/}}
{{- define "access-control.pullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.image .Values.waitForInitContainer.image) "global" .Values.global ) }}
{{- end -}}

{{/*
Generates comma separated list of Mongo Protocol
*/}}
{{- define "access-control.mongoProtocol" }}
    {{- $ := .ctx }}
    {{- $database := .database }}
    {{- if empty $database }}
        {{- fail "ERROR: missing input argument - database" }}
    {{- end }}
    {{- $instanceName := include "harnesscommon.dbv3.generateInstanceName" (dict "database" $database) }}
    {{- if empty $instanceName }}
        {{- fail "ERROR: invalid instanceName value" }}
    {{- end }}
    {{- $localDBCtx := get $.Values.database.mongo $instanceName }}
    {{- $globalDBCtx := $.Values.global.database.mongo }}
    {{- if and $ $localDBCtx $globalDBCtx }}
        {{- $localEnabled := dig "enabled" false $localDBCtx }}
        {{- $installed := dig "installed" true $globalDBCtx }}
        {{- $protocol := "" }}
        {{- if $localEnabled }}
            {{- $protocol = $localDBCtx.protocol }}
        {{- else if $installed }}
            {{- $protocol = "mongodb://" }}
        {{- else }}
            {{- $protocol = $globalDBCtx.protocol }}
        {{- end }}
{{- printf "%s://" $protocol | quote }}
    {{- else }}
        {{- fail (printf "ERROR: invalid contexts") }}
    {{- end }}
{{- end }}

{{/*
Generates comma separated list of Mongo Host names based off environment
*/}}
{{- define "access-control.mongohosts" }}
    {{- $ := .ctx }}
    {{- $database := .database }}
    {{- if empty $database }}
        {{- fail "ERROR: missing input argument - database" }}
    {{- end }}
    {{- $instanceName := include "harnesscommon.dbv3.generateInstanceName" (dict "database" $database) }}
    {{- if empty $instanceName }}
        {{- fail "ERROR: invalid instanceName value" }}
    {{- end }}
    {{- $localDBCtx := get $.Values.database.mongo $instanceName }}
    {{- $globalDBCtx := $.Values.global.database.mongo }}
    {{- if and $ $localDBCtx $globalDBCtx }}
        {{- $localEnabled := dig "enabled" false $localDBCtx }}
        {{- $installed := dig "installed" true $globalDBCtx }}
        {{- $mongoHosts := "" }}
        {{- if $localEnabled }}
            {{- $hosts := $.Values.mongoHosts}}
            {{- $mongoHosts = (join "," $hosts ) }}
        {{- else if $installed }}
            {{- $namespace := $.Release.Namespace }}
            {{- if $.Values.global.ha }}
                {{- $mongoHosts = printf "'mongodb-replicaset-chart-0.mongodb-replicaset-chart.%s.svc,mongodb-replicaset-chart-1.mongodb-replicaset-chart.%s.svc,mongodb-replicaset-chart-2.mongodb-replicaset-chart.%s.svc:27017'" $namespace $namespace $namespace }}
            {{- else }}
                {{- $mongoHosts = printf "'mongodb-replicaset-chart-0.mongodb-replicaset-chart.%s.svc'" $namespace }}
            {{- end }}
        {{- else }}
            {{- $hosts := $.Values.mongoHosts}}
            {{- $mongoHosts = (join "," $hosts ) }}
        {{- end }}
{{- printf "%s" $mongoHosts }}
    {{- else }}
        {{- fail (printf "ERROR: invalid contexts") }}
    {{- end }}
{{- end }}

{{/* Generates Mongo Connection string
{{ include "access-control.mongoConnectionUrl" (dict "database" "foo" "context" $) }}
*/}}
{{- define "access-control.mongoConnectionUrl" }}
    {{- $ := .ctx }}
    {{- $database := .database }}
    {{- if empty $database }}
        {{- fail "ERROR: missing input argument - database" }}
    {{- end }}
    {{- $instanceName := include "harnesscommon.dbv3.generateInstanceName" (dict "database" $database) }}
    {{- if empty $instanceName }}
        {{- fail "ERROR: invalid instanceName value" }}
    {{- end }}
    {{- $localDBCtx := get $.Values.database.mongo $instanceName }}
    {{- $globalDBCtx := $.Values.global.database.mongo }}
    {{- if and $ $localDBCtx $globalDBCtx }}
        {{- $localEnabled := dig "enabled" false $localDBCtx }}
        {{- $installed := dig "installed" true $globalDBCtx }}
        {{- if $localEnabled }}
            {{- $hosts := $localDBCtx.hosts }}
            {{- $extraArgs := $localDBCtx.extraArgs }}
            {{- $args := (printf "/%s?%s" $database $extraArgs ) }}
            {{- $finalhost := (index $hosts  0) }}
            {{- range $host := (rest $hosts ) }}
                {{- $finalhost = printf "%s,%s" $finalhost $host }}
            {{- end }}
            {{- $connectionString := (printf "%s%s" $finalhost $args) }}
            {{- printf "%s" $connectionString }}
        {{- else if $installed }}
            {{- $namespace := $.Release.Namespace }}
            {{- if $.Values.global.ha }}
            {{- printf "'mongodb-replicaset-chart-0.mongodb-replicaset-chart.%s.svc,mongodb-replicaset-chart-1.mongodb-replicaset-chart.%s.svc,mongodb-replicaset-chart-2.mongodb-replicaset-chart.%s.svc:27017/%s?replicaSet=rs0&authSource=admin'" $namespace $namespace $namespace .database }}
            {{- else }}
                {{- printf "'mongodb-replicaset-chart-0.mongodb-replicaset-chart.%s.svc/%s?authSource=admin'" $namespace .database }}
            {{- end }}
        {{- else }}
            {{- $hosts := $globalDBCtx.hosts }}
            {{- $extraArgs := $globalDBCtx.extraArgs }}
            {{- $args := (printf "/%s?%s" $database $extraArgs ) }}
            {{- $finalhost := (index $hosts  0) }}
            {{- range $host := (rest $hosts ) }}
                {{- $finalhost = printf "%s,%s" $finalhost $host }}
            {{- end }}
            {{- $connectionString := (printf "%s%s" $finalhost $args) }}
            {{- printf "%s" $connectionString }}
        {{- end }}
    {{- else }}
        {{- fail (printf "ERROR: invalid contexts") }}
    {{- end }}
{{- end }}

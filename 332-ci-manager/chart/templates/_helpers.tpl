{{/*
Expand the name of the chart.
*/}}
{{- define "ci-manager.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ci-manager.fullname" -}}
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
{{- define "ci-manager.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ci-manager.labels" -}}
helm.sh/chart: {{ include "ci-manager.chart" . }}
{{ include "ci-manager.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "ci-manager.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ci-manager.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ci-manager.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "ci-manager.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the name of the sentinet image to use
*/}}
{{- define "ci-manager.securityImage" -}}
{{ include "common.images.image" (dict "imageRoot" .Values.securityImage.image "global" .Values.global) }}
{{- end }}

{{- define "ci-manager.pullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.image .Values.waitForInitContainer.image .Values.securityImage.image .Values.ci_images.addon.image .Values.ci_images.lite_engine.image .Values.ci_images.git_clone.image .Values.ci_images.kaniko.image .Values.ci_images.kaniko_ecr.image .Values.ci_images.kaniko_gcr.image .Values.ci_images.gcs_upload.image .Values.ci_images.s3_upload.image .Values.ci_images.artifactory_upload.image .Values.ci_images.gcs_cache.image .Values.ci_images.s3_cache.image) "global" .Values.global ) }}
{{- end -}}

{{/*
Manage CI-Manager Secrets
USAGE:
{{- "ci-manager.generateSecrets" (dict "ctx" $)}}
*/}}
{{- define "ci-manager.generateSecrets" }}
    {{- $ := .ctx }}
    {{- $hasAtleastOneSecret := false }}
    {{- $localESOSecretCtxIdentifier := (include "harnesscommon.secrets.localESOSecretCtxIdentifier" (dict "ctx" $ )) }}
    {{- if eq (include "harnesscommon.secrets.isDefault" (dict "ctx" $ "variableName" "LOG_SERVICE_GLOBAL_TOKEN" "extKubernetesSecretCtxs" (list $.Values.secrets.kubernetesSecrets) "esoSecretCtxs" (list (dict $localESOSecretCtxIdentifier $.Values.secrets.secretManagement.externalSecretsOperator)))) "true" }}
    {{- $hasAtleastOneSecret = true }}
LOG_SERVICE_GLOBAL_TOKEN: {{ .ctx.Values.secrets.default.LOG_SERVICE_GLOBAL_TOKEN | b64enc }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefault" (dict "ctx" $ "variableName" "TI_SERVICE_GLOBAL_TOKEN" "extKubernetesSecretCtxs" (list $.Values.secrets.kubernetesSecrets) "esoSecretCtxs" (list (dict $localESOSecretCtxIdentifier $.Values.secrets.secretManagement.externalSecretsOperator)))) "true" }}
    {{- $hasAtleastOneSecret = true }}
TI_SERVICE_GLOBAL_TOKEN: {{ .ctx.Values.secrets.default.TI_SERVICE_GLOBAL_TOKEN | b64enc }}
    {{- end }}
    {{- if not $hasAtleastOneSecret }}
{}
    {{- end }}
{{- end }}

{{/*
Outputs the filepath prefix based on db type and db name

USAGE:
{{ include "harnesscommon.dbv3.filepathprefix" (dict "context" $ "dbType" "redis" "dbName" "") }}
*/}}
{{- define "harnesscommon.dbv3.filepathprefix" }}
  {{- $dbType := lower .dbType }}
  {{- $database := (default "" .dbName) }}
  {{- if eq $database "" }}
    {{- printf "%s" $dbType }}
  {{- else }}
    {{- printf "%s-%s" $dbType $database }}
  {{- end }}
{{- end }}

{{/*
Outputs env variables for SSL

USAGE:
{{ include "harnesscommon.dbv3.sslEnv" (dict "context" $ "dbType" "redis" "dbName" "" "variableNames" ( dict "sslEnabled" "REDIS_SSL_ENABLED" "sslCATrustStorePath" "REDIS_SSL_CA_TRUST_STORE_PATH" "sslCATrustStorePassword" "REDIS_SSL_CA_TRUST_STORE_PASSWORD" "sslCACertPath" "REDIS_SSL_CA_CERT_PATH")) | indent 12 }}
*/}}
{{- define "harnesscommon.dbv3.sslEnv" }}
  {{- $ := .context }}
  {{- $dbType := lower .dbType }}
  {{- $database := (default "" .dbName) }}
  {{- $globalCtx := (index $.Values.global.database $dbType) }}
  {{- $localCtx := default $globalCtx (index $.Values $dbType) }}
  {{- $localDbCtx := default $localCtx (index $localCtx $database) }}
  {{- $globalDbCtx := default $globalCtx (index $globalCtx $database) }}
  {{- $globalDbCtxCopy := deepCopy $globalDbCtx }}
  {{- $mergedCtx := deepCopy $localDbCtx | mergeOverwrite $globalDbCtxCopy }}
  {{- $installed := $mergedCtx.installed }}
  {{- if not $installed }}
    {{- $sslEnabled := $mergedCtx.ssl.enabled }}
    {{- if $sslEnabled }}
    {{- $filepathprefix := (include "harnesscommon.dbv3.filepathprefix" (dict "dbType" $dbType "dbName" $database)) }}
    {{- if .variableNames.sslEnabled }}
- name: {{ .variableNames.sslEnabled }}
  value: {{ printf "%v" $mergedCtx.ssl.enabled | quote }}
    {{- end }}
    {{- if and .variableNames.sslCATrustStorePath $mergedCtx.ssl.trustStoreKey }}
- name: {{ .variableNames.sslCATrustStorePath }}
  value: {{ printf "/opt/harness/svc/ssl/%s-ca-truststore" $filepathprefix | quote }}
    {{- end }}
    {{- if and .variableNames.sslCACertPath $mergedCtx.ssl.caFileKey }}
- name: {{ .variableNames.sslCACertPath }}
  value: {{ printf "/opt/harness/svc/ssl/%s-ca" $filepathprefix | quote }}
    {{- end }}
    {{- if and .variableNames.sslCATrustStorePassword $mergedCtx.ssl.trustStorePasswordKey }}
- name: {{ .variableNames.sslCATrustStorePassword }}
  valueFrom:
    secretKeyRef:
      name: {{ $mergedCtx.ssl.secret }}
      key: {{ $mergedCtx.ssl.trustStorePasswordKey }}
    {{- end }}
    {{- end }}
  {{- end }}
{{- end }}

{{/*
Outputs volumes

USAGE:
{{ include "harnesscommon.dbv3.sslVolume" (dict "context" $ "dbType" "redis" "dbName" "") | indent 12 }}
*/}}
{{- define "harnesscommon.dbv3.sslVolume" }}
  {{- $ := .context }}
  {{- $dbType := lower .dbType }}
  {{- $database := (default "" .dbName) }}
  {{- $globalCtx := (index $.Values.global.database $dbType) }}
  {{- $localCtx := default $globalCtx (index $.Values $dbType) }}
  {{- $localDbCtx := default $localCtx (index $localCtx $database) }}
  {{- $globalDbCtx := default $globalCtx (index $globalCtx $database) }}
  {{- $globalDbCtxCopy := deepCopy $globalDbCtx }}
  {{- $mergedCtx := deepCopy $localDbCtx | mergeOverwrite $globalDbCtxCopy }}
  {{- $installed := $mergedCtx.installed }}
  {{- if not $installed }}
    {{- $sslEnabled := $mergedCtx.ssl.enabled }}
    {{- if and $sslEnabled (or $mergedCtx.ssl.trustStoreKey $mergedCtx.ssl.caFileKey) $mergedCtx.ssl.secret}}
    {{- $filepathprefix := (include "harnesscommon.dbv3.filepathprefix" (dict "dbType" $dbType "dbName" $database)) }}
- name: {{ printf "%s-ssl" $filepathprefix }}
  secret:
    secretName: {{ $mergedCtx.ssl.secret }}
    items:
    {{- if $mergedCtx.ssl.trustStoreKey }}
      - key: {{ $mergedCtx.ssl.trustStoreKey }}
        path: {{ printf "%s-ca-truststore" $filepathprefix }}
    {{- end }}
    {{- if $mergedCtx.ssl.caFileKey }}
      - key: {{ $mergedCtx.ssl.caFileKey }}
        path: {{ printf "%s-ca" $filepathprefix -}}
    {{- end }}
    {{- end }}
  {{- end }}
{{- end }}


{{/*
Outputs volumes

USAGE:
{{ include "harnesscommon.dbv3.sslVolumeMount" (dict "context" $ "dbType" "redis" "dbName" "") | indent 12 }}
*/}}
{{- define "harnesscommon.dbv3.sslVolumeMount" }}
  {{- $ := .context }}
  {{- $dbType := lower .dbType }}
  {{- $database := (default "" .dbName) }}
  {{- $globalCtx := (index $.Values.global.database $dbType) }}
  {{- $localCtx := default $globalCtx (index $.Values $dbType) }}
  {{- $localDbCtx := default $localCtx (index $localCtx $database) }}
  {{- $globalDbCtx := default $globalCtx (index $globalCtx $database) }}
  {{- $globalDbCtxCopy := deepCopy $globalDbCtx }}
  {{- $mergedCtx := deepCopy $localDbCtx | mergeOverwrite $globalDbCtxCopy }}
  {{- $installed := $mergedCtx.installed }}
  {{- if not $installed }}
    {{- $sslEnabled := $mergedCtx.ssl.enabled }}
    {{- if and $sslEnabled (or $mergedCtx.ssl.trustStoreKey $mergedCtx.ssl.caFileKey) $mergedCtx.ssl.secret }}
    {{- $filepathprefix := (include "harnesscommon.dbv3.filepathprefix" (dict "dbType" $dbType "dbName" $database)) }}
- name: {{ printf "%s-ssl" $filepathprefix }}
  mountPath: "/opt/harness/svc/ssl"
  readOnly: true
    {{- end }}
  {{- end }}
{{- end }}
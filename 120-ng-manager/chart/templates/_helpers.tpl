{{/*
Expand the name of the chart.
*/}}
{{- define "ng-manager.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "ng-manager.fullname" -}}
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
{{- define "ng-manager.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "ng-manager.labels" -}}
helm.sh/chart: {{ include "ng-manager.chart" . }}
{{ include "ng-manager.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "ng-manager.selectorLabels" -}}
app.kubernetes.io/name: {{ include "ng-manager.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "ng-manager.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "ng-manager.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "ng-manager.pullSecrets" -}}
{{ include "common.images.pullSecrets" (dict "images" (list .Values.image .Values.waitForInitContainer.image .Values.initContainer.image) "global" .Values.global ) }}
{{- end -}}

{{/*
Manage NG Manager Secrets
USAGE:
{{- "ng-manager.generateSecrets" (dict "ctx" $)}}
default LOG_STREAMING_SERVICE_TOKEN was c76e567a-b341-404d-a8dd-d9738714eb82 and
*/}}
{{- define "ng-manager.generateSecrets" }}
    {{- $ := .ctx }}
    {{- $hasAtleastOneSecret := false }}
    {{- $localESOSecretCtxIdentifier := (include "harnesscommon.secrets.localESOSecretCtxIdentifier" (dict "ctx" $ )) }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "LOG_STREAMING_SERVICE_TOKEN")) "true" }}
    {{- $hasAtleastOneSecret = true }}
LOG_STREAMING_SERVICE_TOKEN: {{ .ctx.Values.secrets.default.LOG_STREAMING_SERVICE_TOKEN | b64enc }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "OPA_SERVER_SECRET")) "true" }}
    {{- $hasAtleastOneSecret = true }}
OPA_SERVER_SECRET: {{ .ctx.Values.secrets.default.OPA_SERVER_SECRET | b64enc }}
    {{- end }}
    {{- if eq (include "harnesscommon.secrets.isDefaultAppSecret" (dict "ctx" $ "variableName" "GITOPS_SERVICE_SECRET")) "true" }}
    {{- $hasAtleastOneSecret = true }}
GITOPS_SERVICE_SECRET: {{ .ctx.Values.secrets.default.GITOPS_SERVICE_SECRET | b64enc }}
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
  value: {{ printf "%v" $mergedCtx.ssl.enabled }}
    {{- end }}
    {{- if and .variableNames.sslCATrustStorePath $mergedCtx.ssl.trustStoreKey }}
- name: {{ .variableNames.sslCATrustStorePath }}
  value: {{ printf "/opt/harness/svc/ssl/%s-ca-truststore" $filepathprefix }}
    {{- end }}
    {{- if and .variableNames.sslCACertPath $mergedCtx.ssl.caFileKey }}
- name: {{ .variableNames.sslCACertPath }}
  value: {{ printf "/opt/harness/svc/ssl/%s-ca" $filepathprefix }}
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

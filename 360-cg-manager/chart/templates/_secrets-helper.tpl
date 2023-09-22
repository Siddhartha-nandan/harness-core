{{/*
Generates env object for Secret

Example:
{{ include "harnesscommon.secrets.isDefault" (dict "ctx" . "variableName" "MY_VARIABLE" "providedSecretValues" (list "values.secret1" "")  "extKubernetesSecretCtxs" (list .Values.secrets) "esoSecretCtxs" (list (dict "" .Values.secrets.secretManagement.externalSecretsOperator))) }}
*/}}
{{- define "harnesscommon.secrets.isDefault" }}
  {{- $ := .ctx }}
  {{- $variableName := .variableName }}
  {{- $defaultValue := .defaultValue }}
  {{- $isDefault := true }}
  {{- if eq (include "harnesscommon.secrets.hasESOSecret" (dict "variableName" .variableName "esoSecretCtxs" .esoSecretCtxs)) "true" }}
    {{- $isDefault = false }}
  {{- else if eq (include "harnesscommon.secrets.hasExtKubernetesSecret" (dict "variableName" .variableName "extKubernetesSecretCtxs" .extKubernetesSecretCtxs)) "true" }}
    {{- $isDefault = false }}
  {{- else if eq (include "harnesscommon.secrets.hasprovidedSecretValues" (dict "ctx" $ "providedSecretValues" .providedSecretValues)) "true" }}
    {{- $isDefault = false }}
  {{- end }}
  {{- printf "%v" $isDefault }}
{{- end }}

{{/*
Generates env object for Secret

Example:
{{ include "harnesscommon.secrets.manageEnv" (dict "ctx" . "variableName" "MY_VARIABLE" "defaultValue" "my-secret-value" "defaultKubernetesSecretName" "defaultSecretName" "defaultKubernetesSecretKey" "defaultSecretKey" "providedSecretValues" (list "values.secret1" "")  "extKubernetesSecretCtxs" (list .Values.secrets) "esoSecretCtxs" (list (dict "" .Values.secrets.secretManagement.externalSecretsOperator))) }}
*/}}
{{- define "harnesscommon.secrets.manageEnv" }}
{{- $ := .ctx }}
{{- $variableName := .variableName }}
{{- $defaultValue := .defaultValue }}
{{- if eq (include "harnesscommon.secrets.hasESOSecret" (dict "variableName" .variableName "esoSecretCtxs" .esoSecretCtxs)) "true" }}
  {{- include "harnesscommon.secrets.manageESOSecretEnv" (dict "ctx" $ "variableName" .variableName "esoSecretCtxs" .esoSecretCtxs) }}
{{- else if eq (include "harnesscommon.secrets.hasExtKubernetesSecret" (dict "variableName" .variableName "extKubernetesSecretCtxs" .extKubernetesSecretCtxs)) "true" }}
  {{- include "harnesscommon.secrets.manageExtKubernetesSecretEnv" (dict "variableName" .variableName "extKubernetesSecretCtxs" .extKubernetesSecretCtxs) }}
{{- else if eq (include "harnesscommon.secrets.hasprovidedSecretValues" (dict "ctx" $ "providedSecretValues" .providedSecretValues)) "true" }}
  {{- include "harnesscommon.secrets.manageProvidedSecretValuesEnv" (dict "ctx" $ "variableName" .variableName "providedSecretValues" .providedSecretValues) }}
{{- else }}
  {{- if not (empty $defaultValue) }}
- name: {{ print $variableName }}
  value: {{ print $defaultValue }}
  {{- else if and (not (empty .defaultKubernetesSecretName)) (not (empty .defaultKubernetesSecretKey)) }}
    {{- include "harnesscommon.secrets.manageDefaultKubernetesSecretEnv" (dict "variableName" .variableName "defaultKubernetesSecretName" .defaultKubernetesSecretName "defaultKubernetesSecretKey" .defaultKubernetesSecretKey) }}
  {{- end }}
{{- end }}
{{- end }}

{{/*
Generates env object for DB Secret

Example:
{{ include "harnesscommon.secrets.manageDBEnv" (dict "ctx" . "installed" .Values.global.database.mongo.installed  "variableName" "MY_VARIABLE" "defaultValue" "my-secret-value" "depKubernetesSecretName" "secretName" "depKubernetesSecretKey" "secretKey" "defaultKubernetesSecretName" "defaultSecretName" "defaultKubernetesSecretKey" "defaultSecretKey" "providedSecretValues" (list "values.secret1" "")  "extKubernetesSecretCtxs" (list .Values.secrets) "esoSecretCtxs" (list (dict "" .Values.secrets.secretManagement.externalSecretsOperator))) }}
*/}}
{{- define "harnesscommon.secrets.manageDBEnv" }}
  {{- $variableName := .variableName }}
  {{- $installed := .installed }}
  {{- $defaultValue := .defaultValue -}}
  {{- $ := .ctx }}
  {{- if eq (include "harnesscommon.secrets.hasESOSecret" (dict "variableName" .variableName "esoSecretCtxs" .esoSecretCtxs)) "true" }}
    {{- include "harnesscommon.secrets.manageESOSecretEnv" (dict "ctx" $ "variableName" .variableName "esoSecretCtxs" .esoSecretCtxs) }}
  {{- else if eq (include "harnesscommon.secrets.hasExtKubernetesSecret" (dict "variableName" .variableName "extKubernetesSecretCtxs" .extKubernetesSecretCtxs)) "true" }}
    {{- include "harnesscommon.secrets.manageExtKubernetesSecretEnv" (dict "variableName" .variableName "extKubernetesSecretCtxs" .extKubernetesSecretCtxs) }}
  {{- else if eq (include "harnesscommon.secrets.hasprovidedSecretValues" (dict "ctx" $ "providedSecretValues" .providedSecretValues)) "true" }}
    {{- include "harnesscommon.secrets.manageProvidedSecretValuesEnv" (dict "ctx" $ "variableName" .variableName "providedSecretValues" .providedSecretValues) }}
  {{- else if not $installed }}
    {{- include "harnesscommon.secrets.manageDefaultKubernetesSecretEnv" (dict "variableName" .variableName "defaultKubernetesSecretName" .depKubernetesSecretName "defaultKubernetesSecretKey" .depKubernetesSecretKey) }}
  {{- else }}
    {{- if not (empty $defaultValue) }}
- name: {{ print $variableName }}
  value: {{ print $defaultValue }}
    {{- else }}
      {{- include "harnesscommon.secrets.manageDefaultKubernetesSecretEnv" (dict "variableName" .variableName "defaultKubernetesSecretName" .defaultKubernetesSecretName "defaultKubernetesSecretKey" .defaultKubernetesSecretKey) }}
    {{- end }}
  {{- end }}
{{- end }}


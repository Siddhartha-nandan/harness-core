# audit-event-streaming

![Version: 0.11.0](https://img.shields.io/badge/Version-0.11.0-informational?style=flat-square) ![Type: application](https://img.shields.io/badge/Type-application-informational?style=flat-square) ![AppVersion: 1.0.79802](https://img.shields.io/badge/AppVersion-1.0.79802-informational?style=flat-square)

A Helm chart for Kubernetes

## Requirements

| Repository | Name | Version |
|------------|------|---------|
| https://harness.github.io/helm-common | harness-common | 1.x.x |

## Values

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| additionalConfigs | object | `{}` | additionalConfigs Additional configurations for the deployment |
| affinity | object | `{}` | affinity Affinity for pod assignment. Evaluated as a template. # ref: https://kubernetes.io/docs/concepts/configuration/assign-pod-node/#affinity-and-anti-affinity # Note: podAffinityPreset, podAntiAffinityPreset, and nodeAffinityPreset will be ignored when it's set # |
| autoscaling | object | `{"enabled":false,"maxReplicas":100,"minReplicas":1,"targetCPU":"","targetMemory":""}` | autoscaling.targetMemory Target Memory utilization percentage # |
| config | object | `{"AUDIT_CLIENT_BASEURL":"http://platform-service:9005/api/","BATCH_CURSOR_SIZE":"1000","BATCH_LIMIT":"10000","BATCH_MAX_RETRIES":"3","DEPLOY_MODE":"KUBERNETES","EVENT_COLLECTION_BATCH_JOB_CRON":"0 */30 * * * *","GRPC_MANAGER_AUTHORITY":"harness-manager:9879","GRPC_MANAGER_TARGET":"harness-manager:9879","JAVA_ADVANCED_FLAGS":"-XX:NativeMemoryTracking=summary -XX:-UseBiasedLocking -XX:+UseG1GC","LOGGING_LEVEL":"INFO","MEMORY":"768m","MONGO_MAX_DOCUMENT_LIMIT":"10000","MONGO_MAX_OPERATION_TIME_IN_MILLIS":"30000","NG_MANAGER_CLIENT_BASEURL":"http://ng-manager:7090/","REPLICA":"1"}` | Configurations for Harness application |
| containerSecurityContext.runAsNonRoot | bool | `true` |  |
| containerSecurityContext.runAsUser | int | `65534` |  |
| database.mongo.ngaudits.enabled | bool | `false` |  |
| database.mongo.ngaudits.extraArgs | string | `""` |  |
| database.mongo.ngaudits.hosts | list | `[]` |  |
| database.mongo.ngaudits.protocol | string | `""` |  |
| database.mongo.ngaudits.secrets.kubernetesSecrets[0].keys.MONGO_PASSWORD | string | `""` |  |
| database.mongo.ngaudits.secrets.kubernetesSecrets[0].keys.MONGO_USER | string | `""` |  |
| database.mongo.ngaudits.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| database.mongo.ngaudits.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_PASSWORD.name | string | `""` |  |
| database.mongo.ngaudits.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_PASSWORD.property | string | `""` |  |
| database.mongo.ngaudits.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_USER.name | string | `""` |  |
| database.mongo.ngaudits.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.MONGO_USER.property | string | `""` |  |
| database.mongo.ngaudits.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| database.mongo.ngaudits.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| extraEnvVars | list | `[]` | extraEnvVars Extra environment variables to be set on container # e.g: # extraEnvVars: #   - name: FOO #     value: "bar" # |
| extraEnvVarsCM | string | `""` | extraEnvVarsCM ConfigMap with extra environment variables # |
| extraEnvVarsSecret | string | `""` | extraEnvVarsSecret Secret with extra environment variables # |
| extraVolumeMounts | list | `[]` | extraVolumeMounts Optionally specify extra list of additional volumeMounts for ; container(s) # e.g. extraVolumeMounts: - name: service-account   mountPath: /opt/harness/svc - name: stackdriver   mountPath: /opt/harness/monitoring - name: dumps   mountPath: /opt/harness/dumps |
| extraVolumes | list | `[]` | extraVolumes Optionally specify extra list of additional volumes for ; pods # e.g. extraVolumes: - name: service-account   secret:     secretName: redis-ca     items:     - key: redis-labs-ca-truststore       path: redis_labs_ca_truststore - name: stackdriver   secret:     secretName: stackdriver-creds     items:     - key: stackdriver-key-file       path: stackdriver.json - name: dumps   hostPath:     path: /var/dumps     type: DirectoryOrCreate |
| fullnameOverride | string | `""` | fullnameOverride String to fully override common.names.fullname template # |
| global | object | `{"commonAnnotations":{},"commonLabels":{},"database":{"mongo":{"extraArgs":"","hosts":[],"installed":true,"passwordKey":"","protocol":"mongodb","secretName":"","secrets":{"kubernetesSecrets":[{"keys":{"MONGO_PASSWORD":"","MONGO_USER":""},"secretName":""}],"secretManagement":{"externalSecretsOperator":[{"remoteKeys":{"MONGO_PASSWORD":{"name":"","property":""},"MONGO_USER":{"name":"","property":""}},"secretStore":{"kind":"","name":""}}]}},"userKey":""},"postgres":{"extraArgs":"","hosts":["postgres:5432"],"installed":true,"passwordKey":"","protocol":"postgres","secretName":"","userKey":""},"redis":{"extraArgs":"","hosts":["redis:6379"],"installed":true,"passwordKey":"","protocol":"redis","secretName":"","secrets":{"kubernetesSecrets":[{"keys":{"REDIS_PASSWORD":"","REDIS_USERNAME":""},"secretName":""}],"secretManagement":{"externalSecretsOperator":[{"remoteKeys":{"REDIS_PASSWORD":{"name":"","property":""},"REDIS_USERNAME":{"name":"","property":""}},"secretStore":{"kind":"","name":""}}]}},"userKey":""},"timescaledb":{"extraArgs":"","hosts":["timescaledb-single-chart:5432"],"installed":true,"passwordKey":"","protocol":"jdbc:postgresql","secretName":"","userKey":""}},"ha":false,"imagePullSecrets":[],"ingress":{"className":"harness","enabled":false,"hosts":["my-host.example.org"],"objects":{"annotations":{}},"tls":{"enabled":true,"secretName":""}},"istio":{"enabled":false,"gateway":{"create":false},"virtualService":{"gateways":null,"hosts":null}},"kubeVersion":"","loadbalancerURL":"","stackDriverLoggingEnabled":false}` | global.storageClass Global StorageClass for Persistent Volume(s) |
| global.database.mongo.hosts | list | `[]` | provide default values if mongo.installed is set to false |
| global.database.redis.hosts | list | `["redis:6379"]` | provide default values if redis.installed is set to false |
| global.database.timescaledb.hosts | list | `["timescaledb-single-chart:5432"]` | provide default values if mongo.installed is set to false |
| global.ingress.className | string | `"harness"` | set ingress object classname |
| global.ingress.enabled | bool | `false` | create ingress objects |
| global.ingress.hosts | list | `["my-host.example.org"]` | set host of ingressObjects |
| global.ingress.objects | object | `{"annotations":{}}` | add annotations to ingress objects |
| global.ingress.tls | object | `{"enabled":true,"secretName":""}` | set tls for ingress objects |
| global.istio.enabled | bool | `false` | create virtualServices objects |
| global.istio.gateway | object | `{"create":false}` | create gateway and use in virtualservice |
| global.istio.virtualService | object | `{"gateways":null,"hosts":null}` | if gateway not created, use specified gateway and host |
| image | object | `{"digest":"","imagePullSecrets":[],"pullPolicy":"IfNotPresent","registry":"docker.io","repository":"harness/<placeholderService>","tag":"latest"}` | image.imagePullSecrets Specify docker-registry secret names as an array |
| ingress.annotations | object | `{}` |  |
| initContainers | list | `[]` | initContainers Add additional init containers to the ; pods # e.g: # initContainers: #   - name: your-image-name #     image: your-image #     imagePullPolicy: Always #     ports: #       - name: portname #         containerPort: 1234 # |
| java.memory | string | `"768m"` |  |
| java17flags | string | `""` |  |
| lifecycleHooks | object | `{}` |  |
| livenessProbe | object | `{"enabled":false,"failureThreshold":null,"initialDelaySeconds":60,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":10}` | livenessProbe.successThreshold Success threshold for livenessProbe # |
| mongoHosts | list | `[]` | mongoHosts List of mongo hosts |
| mongoSSL.enabled | bool | `false` |  |
| mongoSecrets.password.key | string | `"mongodb-root-password"` |  |
| mongoSecrets.password.name | string | `"mongodb-replicaset-chart"` |  |
| mongoSecrets.userName.key | string | `"mongodbUsername"` |  |
| mongoSecrets.userName.name | string | `"harness-secrets"` |  |
| nameOverride | string | `""` | nameOverride String to partially override common.names.fullname template (will maintain the release name) # |
| nodeSelector | object | `{}` | nodeSelector Node labels for pod assignment. Evaluated as a template. # ref: https://kubernetes.io/docs/user-guide/node-selection/ # |
| pdb.create | bool | `true` | pdb.create Enable/disable a Pod Disruption Budget creation # |
| pdb.maxUnavailable | string | `""` | pdb.maxUnavailable Maximum number/percentage of pods that may be made unavailable after the eviction # |
| pdb.minAvailable | string | `"50%"` | pdb.minAvailable Minimum number/percentage of pods that must still be available after the eviction # |
| podAnnotations | object | `{}` |  |
| podSecurityContext | object | `{}` |  |
| progressDeadlineSeconds | int | `720` | set progressDealineSeconds in seconds, number of seconds the Deployment controller waits before indicating failure # ref: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/ |
| readinessProbe | object | `{"enabled":false,"failureThreshold":10,"initialDelaySeconds":60,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":10}` | readinessProbe.successThreshold Success threshold for readinessProbe # |
| redis.extraArgs | string | `""` |  |
| redis.hosts | list | `[]` |  |
| redis.passwordKey | string | `""` |  |
| redis.protocol | string | `""` |  |
| redis.secretName | string | `""` |  |
| redis.secrets.kubernetesSecrets[0].keys.REDIS_PASSWORD | string | `""` |  |
| redis.secrets.kubernetesSecrets[0].keys.REDIS_USERNAME | string | `""` |  |
| redis.secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_PASSWORD.name | string | `""` |  |
| redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_PASSWORD.property | string | `""` |  |
| redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_USERNAME.name | string | `""` |  |
| redis.secrets.secretManagement.externalSecretsOperator[0].remoteKeys.REDIS_USERNAME.property | string | `""` |  |
| redis.secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| redis.secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| redis.userKey | string | `""` |  |
| replicaCount | int | `1` | replicaCount Number of pods # |
| resources | object | `{"limits":{"memory":"8192Mi"},"requests":{"cpu":1,"memory":"712Mi"}}` | resources.requests The requested resources for the containers # |
| schedulerName | string | `""` | schedulerName Specifies the schedulerName, if it's nil uses kube-scheduler # https://kubernetes.io/docs/tasks/administer-cluster/configure-multiple-schedulers/ # |
| secrets.default.IDENTITY_SERVICE_SECRET | string | `"HVSKUYqD4e5Rxu12hFDdCJKGM64sxgEynvdDhaOHaTHhwwn0K4Ttr0uoOxSsEVYNrUU="` |  |
| secrets.kubernetesSecrets[0].keys.IDENTITY_SERVICE_SECRET | string | `""` |  |
| secrets.kubernetesSecrets[0].secretName | string | `""` |  |
| secrets.secretManagement.externalSecretsOperator[0].remoteKeys.IDENTITY_SERVICE_SECRET.name | string | `""` |  |
| secrets.secretManagement.externalSecretsOperator[0].remoteKeys.IDENTITY_SERVICE_SECRET.property | string | `""` |  |
| secrets.secretManagement.externalSecretsOperator[0].secretStore.kind | string | `""` |  |
| secrets.secretManagement.externalSecretsOperator[0].secretStore.name | string | `""` |  |
| service.annotations | object | `{}` |  |
| service.port | int | `9006` |  |
| service.type | string | `"ClusterIP"` |  |
| serviceAccount.annotations | object | `{}` |  |
| serviceAccount.create | bool | `false` |  |
| serviceAccount.name | string | `"harness-default"` |  |
| sidecars | list | `[]` | sidecars Add additional sidecar containers to the ; pods # e.g: # sidecars: #   - name: your-image-name #     image: your-image #     imagePullPolicy: Always #     ports: #       - name: portname #         containerPort: 1234 # |
| startupProbe | object | `{"enabled":false,"failureThreshold":60,"initialDelaySeconds":0,"periodSeconds":10,"successThreshold":1,"timeoutSeconds":5}` | startupProbe.successThreshold Success threshold for startupProbe # |
| terminationGracePeriodSeconds | string | `"30"` | terminationGracePeriodSeconds In seconds, time the given to the pod needs to terminate gracefully # ref: https://kubernetes.io/docs/concepts/workloads/pods/pod/#termination-of-pods # |
| tolerations | list | `[]` | tolerations Tolerations for pod assignment. Evaluated as a template. # ref: https://kubernetes.io/docs/concepts/configuration/taint-and-toleration/ # |
| updateStrategy | object | `{"rollingUpdate":{"maxSurge":"25%","maxUnavailable":"25%"},"type":"RollingUpdate"}` | deployment.updateStrategy.type Deployment strategy type # ref: https://kubernetes.io/docs/concepts/workloads/controllers/deployment/#update-strategies # e.g: |
| virtualService.annotations | object | `{}` |  |
| waitForInitContainer | object | `{"containerSecurityContext":{"runAsNonRoot":true,"runAsUser":65534},"image":{"digest":"","imagePullSecrets":[],"pullPolicy":"IfNotPresent","registry":"docker.io","repository":"harness/helm-init-container","tag":"latest"},"resources":{"limits":{"memory":"128Mi"},"requests":{"cpu":"128m","memory":"128Mi"}}}` | Wait-For-App initContainers details |
| waitForInitContainer.image | object | `{"digest":"","imagePullSecrets":[],"pullPolicy":"IfNotPresent","registry":"docker.io","repository":"harness/helm-init-container","tag":"latest"}` | image.imagePullSecrets Specify docker-registry secret names as an array |

----------------------------------------------
Autogenerated from chart metadata using [helm-docs v1.11.0](https://github.com/norwoodj/helm-docs/releases/v1.11.0)

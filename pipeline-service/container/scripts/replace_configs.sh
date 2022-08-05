#!/usr/bin/env bash
# Copyright 2021 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Shield 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

CONFIG_FILE=/opt/harness/config.yml
REDISSON_CACHE_FILE=/opt/harness/redisson-jcache.yaml
ENTERPRISE_REDISSON_CACHE_FILE=/opt/harness/enterprise-redisson-jcache.yaml

replace_key_value () {
  CONFIG_KEY="$1";
  CONFIG_VALUE="$2";
  if [[ "" != "$CONFIG_VALUE" ]]; then
    yq -i '.$CONFIG_KEY="$CONFIG_VALUE"' $CONFIG_FILE
  fi
}

if [[ "" != "$SERVER_MAX_THREADS" ]]; then
  yq -i '.server.maxThreads="$SERVER_MAX_THREADS"' $CONFIG_FILE
fi

yq -i '.server.adminConnectors="[]"' $CONFIG_FILE

yq 'del(.'grpcServerConfig.connectors.(secure==true)')' $CONFIG_FILE
yq 'del(.'gitSdkConfiguration.gitSdkGrpcServerConfig.connectors.(secure==true)')' $CONFIG_FILE

if [[ "" != "$LOGGING_LEVEL" ]]; then
    yq -i '.logging.level="$LOGGING_LEVEL"' $CONFIG_FILE
fi

if [[ "" != "$LOGGERS" ]]; then
  IFS=',' read -ra LOGGER_ITEMS <<< "$LOGGERS"
  for ITEM in "${LOGGER_ITEMS[@]}"; do
    LOGGER=`echo $ITEM | awk -F= '{print $1}'`
    LOGGER_LEVEL=`echo $ITEM | awk -F= '{print $2}'`
    yq -i '.logging.loggers.[$LOGGER]="${LOGGER_LEVEL}"' $CONFIG_FILE
  done
fi

if [[ "" != "$MONGO_URI" ]]; then
  yq -i '.mongo.uri="${MONGO_URI//\\&/&}"' $CONFIG_FILE
fi

if [[ "" != "$MONGO_TRACE_MODE" ]]; then
  yq -i '.mongo.traceMode="$MONGO_TRACE_MODE"' $CONFIG_FILE
fi

if [[ "" != "$MONGO_CONNECT_TIMEOUT" ]]; then
  yq -i '.mongo.connectTimeout="$MONGO_CONNECT_TIMEOUT"' $CONFIG_FILE
fi

if [[ "" != "$MONGO_SERVER_SELECTION_TIMEOUT" ]]; then
  yq -i '.mongo.serverSelectionTimeout="$MONGO_SERVER_SELECTION_TIMEOUT"' $CONFIG_FILE
fi

if [[ "" != "$MAX_CONNECTION_IDLE_TIME" ]]; then
  yq -i '.mongo.maxConnectionIdleTime="$MAX_CONNECTION_IDLE_TIME"' $CONFIG_FILE
fi

if [[ "" != "$MONGO_CONNECTIONS_PER_HOST" ]]; then
  yq -i '.mongo.connectionsPerHost="$MONGO_CONNECTIONS_PER_HOST"' $CONFIG_FILE
fi

if [[ "" != "$MONGO_INDEX_MANAGER_MODE" ]]; then
  yq -i '.mongo.indexManagerMode="$MONGO_INDEX_MANAGER_MODE"' $CONFIG_FILE
fi

if [[ "" != "$MONGO_TRANSACTIONS_ALLOWED" ]]; then
  yq -i '.mongo.transactionsEnabled="$MONGO_TRANSACTIONS_ALLOWED"' $CONFIG_FILE
fi

if [[ "" != "$DISTRIBUTED_LOCK_IMPLEMENTATION" ]]; then
  yq -i '.distributedLockImplementation="$DISTRIBUTED_LOCK_IMPLEMENTATION"' $CONFIG_FILE
fi

if [[ "" != "$GRPC_SERVER_PORT" ]]; then
  yq -i '.grpcServerConfig.connectors[0].port="$GRPC_SERVER_PORT"' $CONFIG_FILE
fi

if [[ "" != "$MANAGER_TARGET" ]]; then
  yq -i '.managerTarget="$MANAGER_TARGET"' $CONFIG_FILE
fi

if [[ "" != "$MANAGER_AUTHORITY" ]]; then
  yq -i '.managerAuthority="$MANAGER_AUTHORITY"' $CONFIG_FILE
fi

if [[ "" != "$MANAGER_BASE_URL" ]]; then
  yq -i '.managerClientConfig.baseUrl="$MANAGER_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$MANAGER_SERVICE_SECRET" ]]; then
  yq -i '.managerServiceSecret="$MANAGER_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_BASE_URL" ]]; then
  yq -i '.ngManagerServiceHttpClientConfig.baseUrl="$NG_MANAGER_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_SERVICE_SECRET" ]]; then
  yq -i '.ngManagerServiceSecret="$NG_MANAGER_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$PIPELINE_SERVICE_ENDPOINT" ]]; then
  yq -i '.pipelineServiceClientConfig.baseUrl="$PIPELINE_SERVICE_ENDPOINT"' $CONFIG_FILE
fi

if [[ "" != "$PIPELINE_SERVICE_SECRET" ]]; then
  yq -i '.pipelineServiceSecret="$PIPELINE_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$TEMPLATE_SERVICE_ENDPOINT" ]]; then
  yq -i '.templateServiceClientConfig.baseUrl="$TEMPLATE_SERVICE_ENDPOINT"' $CONFIG_FILE
fi

if [[ "" != "$TEMPLATE_SERVICE_SECRET" ]]; then
  yq -i '.templateServiceSecret="$TEMPLATE_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$CI_MANAGER_BASE_URL" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.ci.serviceHttpClientConfig.baseUrl="$CI_MANAGER_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$CI_MANAGER_SERVICE_CONNECT_TIMEOUT_IN_SECONDS" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.ci.serviceHttpClientConfig.connectTimeOutSeconds="$CI_MANAGER_SERVICE_CONNECT_TIMEOUT_IN_SECONDS"' $CONFIG_FILE
fi

if [[ "" != "$CI_MANAGER_SERVICE_READ_TIMEOUT_IN_SECONDS" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.ci.serviceHttpClientConfig.readTimeOutSeconds="$CI_MANAGER_SERVICE_READ_TIMEOUT_IN_SECONDS"' $CONFIG_FILE
fi

if [[ "" != "$CI_MANAGER_SERVICE_SECRET" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.ci.secret="$CI_MANAGER_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_BASE_URL" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cd.serviceHttpClientConfig.baseUrl="$NG_MANAGER_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_SERVICE_CONNECT_TIMEOUT_IN_SECONDS" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cd.serviceHttpClientConfig.connectTimeOutSeconds="$NG_MANAGER_SERVICE_CONNECT_TIMEOUT_IN_SECONDS"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_SERVICE_READ_TIMEOUT_IN_SECONDS" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cd.serviceHttpClientConfig.readTimeOutSeconds="$NG_MANAGER_SERVICE_READ_TIMEOUT_IN_SECONDS"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_SERVICE_SECRET" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cd.secret="$NG_MANAGER_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$CV_MANAGER_BASE_URL" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cv.serviceHttpClientConfig.baseUrl="$CV_MANAGER_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$CV_MANAGER_SERVICE_CONNECT_TIMEOUT_IN_SECONDS" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cv.serviceHttpClientConfig.connectTimeOutSeconds="$CV_MANAGER_SERVICE_CONNECT_TIMEOUT_IN_SECONDS"' $CONFIG_FILE
fi

if [[ "" != "$CV_MANAGER_SERVICE_READ_TIMEOUT_IN_SECONDS" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cv.serviceHttpClientConfig.readTimeOutSeconds="$CV_MANAGER_SERVICE_READ_TIMEOUT_IN_SECONDS"' $CONFIG_FILE
fi

if [[ "" != "$CV_MANAGER_SERVICE_SECRET" ]]; then
  yq -i '.yamlSchemaClientConfig.yamlSchemaHttpClientMap.cv.secret="$CV_MANAGER_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_TARGET" ]]; then
  yq -i '.grpcClientConfigs.cd.target="$NG_MANAGER_TARGET"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_AUTHORITY" ]]; then
  yq -i '.grpcClientConfigs.cd.authority="$NG_MANAGER_AUTHORITY"' $CONFIG_FILE
fi

if [[ "" != "$CVNG_MANAGER_TARGET" ]]; then
  yq -i '.grpcClientConfigs.cv.target="$CVNG_MANAGER_TARGET"' $CONFIG_FILE
fi

if [[ "" != "$CVNG_MANAGER_AUTHORITY" ]]; then
  yq -i '.grpcClientConfigs.cv.authority="$CVNG_MANAGER_AUTHORITY"' $CONFIG_FILE
fi

if [[ "" != "$CI_MANAGER_TARGET" ]]; then
  yq -i '.grpcClientConfigs.ci.target="$CI_MANAGER_TARGET"' $CONFIG_FILE
fi

if [[ "" != "$CI_MANAGER_AUTHORITY" ]]; then
  yq -i '.grpcClientConfigs.ci.authority="$CI_MANAGER_AUTHORITY"' $CONFIG_FILE
fi

if [[ "" != "$STO_MANAGER_TARGET" ]]; then
  yq -i '.grpcClientConfigs.sto.target="$STO_MANAGER_TARGET"' $CONFIG_FILE
fi

if [[ "" != "$STO_MANAGER_AUTHORITY" ]]; then
  yq -i '.grpcClientConfigs.sto.authority="$STO_MANAGER_AUTHORITY"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_GITSYNC_TARGET" ]]; then
  yq -i '.gitSdkConfiguration.gitManagerGrpcClientConfig.target="$NG_MANAGER_GITSYNC_TARGET"' $CONFIG_FILE
fi

if [[ "" != "$NG_MANAGER_GITSYNC_AUTHORITY" ]]; then
  yq -i '.gitSdkConfiguration.gitManagerGrpcClientConfig.authority="$NG_MANAGER_GITSYNC_AUTHORITY"' $CONFIG_FILE
fi

if [[ "" != "$SCM_SERVICE_URI" ]]; then
  yq -i '.gitSdkConfiguration.scmConnectionConfig.url="$SCM_SERVICE_URI"' $CONFIG_FILE
fi

if [[ "" != "$PIPELINE_SERVICE_BASE_URL" ]]; then
  yq -i '.pipelineServiceBaseUrl="$PIPELINE_SERVICE_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$PMS_API_BASE_URL" ]]; then
  yq -i '.pmsApiBaseUrl="$PMS_API_BASE_URL"' $CONFIG_FILE
fi

if [[ "$STACK_DRIVER_LOGGING_ENABLED" == "true" ]]; then
  yq 'del(.'logging.appenders.(type==console)')' $CONFIG_FILE
  yq -i '.'logging.appenders.(type==gke-console).stackdriverLogEnabled'="true"' $CONFIG_FILE
else
  yq 'del(.'logging.appenders.(type==gke-console)')' $CONFIG_FILE
fi

if [[ "" != "$JWT_AUTH_SECRET" ]]; then
  yq -i '.jwtAuthSecret="$JWT_AUTH_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$JWT_IDENTITY_SERVICE_SECRET" ]]; then
  yq -i '.jwtIdentityServiceSecret="$JWT_IDENTITY_SERVICE_SECRET"' $CONFIG_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq -i '.eventsFramework.redis.sentinelUrls.[$INDEX]="${REDIS_SENTINEL_URL}"' $CONFIG_FILE
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$NOTIFICATION_BASE_URL" ]]; then
  yq -i '.notificationClient.httpClient.baseUrl="$NOTIFICATION_BASE_URL"' $CONFIG_FILE
fi

if [[ "" != "$NOTIFICATION_MONGO_URI" ]]; then
  yq -i '.notificationClient.messageBroker.uri="${NOTIFICATION_MONGO_URI//\\&/&}"' $CONFIG_FILE
fi

if [[ "" != "$MANAGER_CLIENT_BASEURL" ]]; then
  yq -i '.managerClientConfig.baseUrl="$MANAGER_CLIENT_BASEURL"' $CONFIG_FILE
fi

if [[ "" != "$TIMESCALE_PASSWORD" ]]; then
  yq -i '.timescaledb.timescaledbPassword="$TIMESCALE_PASSWORD"' $CONFIG_FILE
fi

if [[ "" != "$TIMESCALE_URI" ]]; then
  yq -i '.timescaledb.timescaledbUrl="$TIMESCALE_URI"' $CONFIG_FILE
fi

if [[ "" != "$TIMESCALEDB_USERNAME" ]]; then
  yq -i '.timescaledb.timescaledbUsername="$TIMESCALEDB_USERNAME"' $CONFIG_FILE
fi

if [[ "" != "$ENABLE_DASHBOARD_TIMESCALE" ]]; then
  yq -i '.enableDashboardTimescale="$ENABLE_DASHBOARD_TIMESCALE"' $CONFIG_FILE
fi

yq 'del(.codec)' $REDISSON_CACHE_FILE

if [[ "$REDIS_SCRIPT_CACHE" == "false" ]]; then
  yq -i '.useScriptCache="false"' $REDISSON_CACHE_FILE
fi


if [[ "" != "$CACHE_CONFIG_REDIS_URL" ]]; then
  yq -i '.singleServerConfig.address="$CACHE_CONFIG_REDIS_URL"' $REDISSON_CACHE_FILE
fi

if [[ "$CACHE_CONFIG_USE_SENTINEL" == "true" ]]; then
  yq 'del(.singleServerConfig)' $REDISSON_CACHE_FILE
fi

if [[ "" != "$CACHE_CONFIG_SENTINEL_MASTER_NAME" ]]; then
  yq -i '.sentinelServersConfig.masterName="$CACHE_CONFIG_SENTINEL_MASTER_NAME"' $REDISSON_CACHE_FILE
fi

if [[ "" != "$CACHE_CONFIG_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$CACHE_CONFIG_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq -i '.sentinelServersConfig.sentinelAddresses.[$INDEX]="${REDIS_SENTINEL_URL}"' $REDISSON_CACHE_FILE
    INDEX=$(expr $INDEX + 1)
  done
fi

if [[ "" != "$REDIS_NETTY_THREADS" ]]; then
  yq -i '.nettyThreads="$REDIS_NETTY_THREADS"' $REDISSON_CACHE_FILE
fi

yq 'del(.codec)' $ENTERPRISE_REDISSON_CACHE_FILE

if [[ "$REDIS_SCRIPT_CACHE" == "false" ]]; then
  yq -i '.useScriptCache="false"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_NETTY_THREADS" ]]; then
  yq -i '.nettyThreads="$EVENTS_FRAMEWORK_NETTY_THREADS"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_URL" ]]; then
  yq -i '.singleServerConfig.address="$EVENTS_FRAMEWORK_REDIS_URL"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_USERNAME" ]]; then
  yq -i '.singleServerConfig.username="$EVENTS_FRAMEWORK_REDIS_USERNAME"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_PASSWORD" ]]; then
  yq -i '.singleServerConfig.password="$EVENTS_FRAMEWORK_REDIS_PASSWORD"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH" ]]; then
  yq -i '.singleServerConfig.sslTruststore="file:$EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD" ]]; then
  yq -i '.singleServerConfig.sslTruststorePassword="$EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD"' $ENTERPRISE_REDISSON_CACHE_FILE
fi

if [[ "$EVENTS_FRAMEWORK_USE_SENTINEL" == "true" ]]; then
  yq 'del(.singleServerConfig)' $ENTERPRISE_REDISSON_CACHE_FILE

  if [[ "" != "$EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME" ]]; then
    yq -i '.sentinelServersConfig.masterName="$EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME"' $ENTERPRISE_REDISSON_CACHE_FILE
  fi

  if [[ "" != "$EVENTS_FRAMEWORK_REDIS_SENTINELS" ]]; then
    IFS=',' read -ra SENTINEL_URLS <<< "$EVENTS_FRAMEWORK_REDIS_SENTINELS"
    INDEX=0
    for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
      yq -i '.sentinelServersConfig.sentinelAddresses.[$INDEX]="${REDIS_SENTINEL_URL}"' $ENTERPRISE_REDISSON_CACHE_FILE
      INDEX=$(expr $INDEX + 1)
    done
  fi
fi

replace_key_value cacheConfig.cacheNamespace $CACHE_NAMESPACE
replace_key_value cacheConfig.cacheBackend $CACHE_BACKEND
replace_key_value cacheConfig.enterpriseCacheEnabled $ENTERPRISE_CACHE_ENABLED

replace_key_value eventsFramework.redis.sentinel $EVENTS_FRAMEWORK_USE_SENTINEL
replace_key_value eventsFramework.redis.envNamespace $EVENTS_FRAMEWORK_ENV_NAMESPACE
replace_key_value eventsFramework.redis.redisUrl $EVENTS_FRAMEWORK_REDIS_URL
replace_key_value eventsFramework.redis.masterName $EVENTS_FRAMEWORK_SENTINEL_MASTER_NAME
replace_key_value eventsFramework.redis.userName $EVENTS_FRAMEWORK_REDIS_USERNAME
replace_key_value eventsFramework.redis.password $EVENTS_FRAMEWORK_REDIS_PASSWORD
replace_key_value eventsFramework.redis.nettyThreads $EVENTS_FRAMEWORK_NETTY_THREADS
replace_key_value eventsFramework.redis.sslConfig.enabled $EVENTS_FRAMEWORK_REDIS_SSL_ENABLED
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePath $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PATH
replace_key_value eventsFramework.redis.sslConfig.CATrustStorePassword $EVENTS_FRAMEWORK_REDIS_SSL_CA_TRUST_STORE_PASSWORD

if [[ "" != "$LOCK_CONFIG_REDIS_SENTINELS" ]]; then
  IFS=',' read -ra SENTINEL_URLS <<< "$LOCK_CONFIG_REDIS_SENTINELS"
  INDEX=0
  for REDIS_SENTINEL_URL in "${SENTINEL_URLS[@]}"; do
    yq -i '.redisLockConfig.sentinelUrls.[$INDEX]="${REDIS_SENTINEL_URL}"' $CONFIG_FILE
    INDEX=$(expr $INDEX + 1)
  done
fi

replace_key_value redisLockConfig.redisUrl "$LOCK_CONFIG_REDIS_URL"
replace_key_value redisLockConfig.envNamespace "$LOCK_CONFIG_ENV_NAMESPACE"
replace_key_value redisLockConfig.sentinel "$LOCK_CONFIG_USE_SENTINEL"
replace_key_value redisLockConfig.masterName "$LOCK_CONFIG_SENTINEL_MASTER_NAME"
replace_key_value redisLockConfig.userName "$LOCK_CONFIG_REDIS_USERNAME"
replace_key_value redisLockConfig.password "$LOCK_CONFIG_REDIS_PASSWORD"
replace_key_value redisLockConfig.nettyThreads "$REDIS_NETTY_THREADS"

replace_key_value accessControlClient.enableAccessControl "$ACCESS_CONTROL_ENABLED"

replace_key_value accessControlClient.accessControlServiceConfig.baseUrl "$ACCESS_CONTROL_BASE_URL"

replace_key_value accessControlClient.accessControlServiceSecret "$ACCESS_CONTROL_SECRET"

replace_key_value logStreamingServiceConfig.baseUrl "$LOG_STREAMING_SERVICE_BASEURL"

replace_key_value logStreamingServiceConfig.serviceToken "$LOG_STREAMING_SERVICE_TOKEN"

replace_key_value iteratorsConfig.approvalInstanceIteratorConfig.enabled "$APPROVAL_INSTANCE_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.approvalInstanceIteratorConfig.targetIntervalInSeconds "$APPROVAL_INSTANCE_ITERATOR_INTERVAL_SEC"
replace_key_value orchestrationStepConfig.ffServerBaseUrl "$FF_SERVER_BASE_URL"
replace_key_value orchestrationStepConfig.ffServerApiKey "$FF_SERVER_API_KEY"

replace_key_value shouldDeployWithGitSync "$ENABLE_GIT_SYNC"

replace_key_value enableAudit "$ENABLE_AUDIT"
replace_key_value auditClientConfig.baseUrl "$AUDIT_SERVICE_BASE_URL"
replace_key_value notificationClient.secrets.notificationClientSecret "$NOTIFICATION_CLIENT_SECRET"

replace_key_value triggerConfig.webhookBaseUrl "$WEBHOOK_TRIGGER_BASEURL"
replace_key_value triggerConfig.customBaseUrl "$CUSTOM_TRIGGER_BASEURL"

replace_key_value opaServerConfig.baseUrl "$OPA_SERVER_BASEURL"
replace_key_value opaServerConfig.secret "$OPA_SERVER_SECRET"

replace_key_value delegatePollingConfig.syncDelay "$POLLING_SYNC_DELAY"
replace_key_value delegatePollingConfig.asyncDelay "$POLLING_ASYNC_DELAY"
replace_key_value delegatePollingConfig.progressDelay "$POLLING_PROGRESS_DELAY"

replace_key_value segmentConfiguration.enabled "$SEGMENT_ENABLED"
replace_key_value segmentConfiguration.apiKey "$SEGMENT_APIKEY"

#Iterators Configuration
replace_key_value iteratorsConfig.approvalInstance.enabled "$APPROVAL_INSTANCE_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.approvalInstance.threadPoolCount "$BARRIER_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.approvalInstance.targetIntervalInSeconds "$APPROVAL_INSTANCE_ITERATOR_INTERVAL_SEC"

replace_key_value iteratorsConfig.webhook.enabled "$WEBHOOK_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.webhook.threadPoolCount "$WEBHOOK_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.webhook.targetIntervalInSeconds "$WEBHOOK_ITERATOR_INTERVAL_SEC"

replace_key_value iteratorsConfig.scheduledTrigger.enabled "$SCHEDULED_TRIGGER_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.scheduledTrigger.threadPoolCount "$SCHEDULED_TRIGGER_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.scheduledTrigger.targetIntervalInSeconds "$SCHEDULED_TRIGGER_ITERATOR_INTERVAL_SEC"

replace_key_value iteratorsConfig.timeoutEngine.enabled "$TIME_OUT_ENGINE_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.timeoutEngine.threadPoolCount "$TIMEOUT_ENGINE_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.timeoutEngine.targetIntervalInSeconds "$TIME_OUT_ENGINE_ITERATOR_INTERVAL_SEC"

replace_key_value iteratorsConfig.barrier.enabled "$BARRIER_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.barrier.threadPoolCount "$BARRIER_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.barrier.targetIntervalInSeconds "$BARRIER_ITERATOR_INTERVAL_SEC"

replace_key_value iteratorsConfig.resourceRestraint.enabled "$RESOURCE_RESTRAINT_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.resourceRestraint.threadPoolCount "$RESOURCE_RESTRAINT_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.resourceRestraint.targetIntervalInSeconds "$RESOURCE_RESTRAINT_ITERATOR_INTERVAL_SEC"

replace_key_value iteratorsConfig.interruptMonitor.enabled "$INTERRUPT_MONITOR_ITERATOR_ENABLED"
replace_key_value iteratorsConfig.interruptMonitor.threadPoolCount "$INTERRUPT_MONITOR_ITERATOR_THREAD_POOL_SIZE"
replace_key_value iteratorsConfig.interruptMonitor.targetIntervalInSeconds "$INTERRUPT_MONITOR_ITERATOR_INTERVAL_SEC"

#consumers configuration
replace_key_value pipelineEventConsumersConfig.interrupt.threads "$INTERRUPT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.orchestrationEvent.threads "$ORCHESTRATION_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.facilitatorEvent.threads "$FACILITATE_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.nodeStart.threads "$NODE_START_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.progress.threads "$PROGRESS_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.advise.threads "$ADVISE_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.resume.threads "$RESUME_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.sdkResponse.threads "$SDK_RESPONSE_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.graphUpdate.threads "$GRAPH_UPDATE_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.partialPlanResponse.threads "$PARTIAL_PLAN_RESPONSE_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.createPlan.threads "$CREATE_PLAN_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.planNotify.threads "$PLAN_NOTIFY_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.pmsNotify.threads "$PMS_NOTIFY_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.webhookEvent.threads "$PMS_WEBHOOK_EVENT_CONSUMER_THREAD_COUNT"
replace_key_value pipelineEventConsumersConfig.initiateNode.threads "$INITIATE_NODE_EVENT_CONSUMER_THREAD_COUNT"

replace_key_value enforcementClientConfiguration.enforcementCheckEnabled "$ENFORCEMENT_CHECK_ENABLED"
replace_key_value segmentConfiguration.url "$SEGMENT_URL"

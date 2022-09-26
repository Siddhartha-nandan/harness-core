package io.harness.redis;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisClientConfig;
import org.redisson.client.codec.Codec;
import org.redisson.config.Config;
import org.redisson.config.ReadMode;
import org.redisson.config.SingleServerConfig;

@OwnedBy(PL)
@UtilityClass
@Slf4j
public class RedissonClientFactory {
  private static final int DEFAULT_MIN_CONNECTION_IDLE_SIZE = 5;
  private static final int DEFAULT_PING_CONNECTION_INTERVAL_IN_MILLIS = 30000;

  private static final Map<RedisConfig, RedissonClient> redisConfigRedissonClientMap = new HashMap<>();

  public RedissonClient getClient(RedisConfig redisConfig) {
    synchronized (redisConfigRedissonClientMap) {
      if (redisConfigRedissonClientMap.containsKey(redisConfig)) {
        return redisConfigRedissonClientMap.get(redisConfig);
      }

      Config config = new Config();
      if (!redisConfig.isSentinel()) {
        if (redisConfig.getRedisUrl().equals("dummyRedisUrl")) {
          return null;
        }
        SingleServerConfig serverConfig = config.useSingleServer().setAddress(redisConfig.getRedisUrl());
        String redisPassword = redisConfig.getPassword();
        String redisUserName = redisConfig.getUserName();

        if (isNotEmpty(redisUserName)) {
          serverConfig.setUsername(redisUserName);
        }
        if (isNotEmpty(redisPassword)) {
          serverConfig.setPassword(redisPassword);
        }

        if (redisConfig.getSubscriptionsPerConnection() != 0) {
          config.useSingleServer().setSubscriptionsPerConnection(redisConfig.getSubscriptionsPerConnection());
        }
        if (redisConfig.getSubscriptionConnectionPoolSize() != 0) {
          config.useSingleServer().setSubscriptionConnectionPoolSize(redisConfig.getSubscriptionConnectionPoolSize());
        }
        config.useSingleServer().setPingConnectionInterval(DEFAULT_PING_CONNECTION_INTERVAL_IN_MILLIS);

        serverConfig.setConnectionMinimumIdleSize(
            Math.max(DEFAULT_MIN_CONNECTION_IDLE_SIZE, redisConfig.getConnectionMinimumIdleSize()));

        RedisSSLConfig sslConfig = redisConfig.getSslConfig();
        if (sslConfig != null && sslConfig.isEnabled()) {
          try {
            serverConfig.setSslTruststore(new File(sslConfig.getCATrustStorePath()).toURI().toURL());
            serverConfig.setSslTruststorePassword(sslConfig.getCATrustStorePassword());
          } catch (MalformedURLException e) {
            log.error("Malformed URL provided for Redis SSL CA trustStore file", e);
            return null;
          }
        }
      } else {
        config.useSentinelServers().setMasterName(redisConfig.getMasterName());
        for (String sentinelUrl : redisConfig.getSentinelUrls()) {
          config.useSentinelServers().addSentinelAddress(sentinelUrl);
        }
        config.useSentinelServers().setReadMode(ReadMode.valueOf(redisConfig.getReadMode().name()));
        if (redisConfig.getSubscriptionsPerConnection() != 0) {
          config.useSentinelServers().setSubscriptionsPerConnection(redisConfig.getSubscriptionsPerConnection());
        }
        if (redisConfig.getSubscriptionConnectionPoolSize() != 0) {
          config.useSentinelServers().setSubscriptionConnectionPoolSize(
              redisConfig.getSubscriptionConnectionPoolSize());
        }
        config.useSentinelServers().setPingConnectionInterval(DEFAULT_PING_CONNECTION_INTERVAL_IN_MILLIS);
      }
      config.setNettyThreads(redisConfig.getNettyThreads());
      config.setUseScriptCache(redisConfig.isUseScriptCache());
      if (redisConfig.getCodec() != null) {
        config.setCodec(getCodec(redisConfig.getCodec()));
      }

      log.info("Creating Redis Client");
      redisConfigRedissonClientMap.put(redisConfig, Redisson.create(config));
      return redisConfigRedissonClientMap.get(redisConfig);
    }
  }

  private Codec getCodec(Class<? extends Codec> codec) {
    try {
      return codec.getConstructor().newInstance();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public RedisClient getLowLevelClient(RedisConfig redisConfig) {
    RedisClientConfig config = new RedisClientConfig();
    if (!redisConfig.isSentinel()) {
      config = config.setAddress(redisConfig.getRedisUrl());
      String redisPassword = redisConfig.getPassword();
      String redisUserName = redisConfig.getUserName();

      if (isNotEmpty(redisUserName)) {
        config.setUsername(redisUserName);
      }

      if (isNotEmpty(redisPassword)) {
        config.setPassword(redisPassword);
      }

      RedisSSLConfig sslConfig = redisConfig.getSslConfig();
      if (sslConfig != null && sslConfig.isEnabled()) {
        try {
          config.setSslTruststore(new File(sslConfig.getCATrustStorePath()).toURI().toURL());
          config.setSslTruststorePassword(sslConfig.getCATrustStorePassword());
        } catch (MalformedURLException e) {
          log.error("Malformed URL provided for Redis SSL CA trustStore file", e);
          return null;
        }
      }
    } else {
      throw new NotImplementedException("Sentinel support is not added for low level redis client");
    }
    log.info("Creating Redis Client");
    return RedisClient.create(config);
  }
}

package io.harness.ngsettings.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Sets;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.ngsettings.entities.SettingConfiguration;
import io.harness.ngsettings.entities.SettingsConfigurationState;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.ngsettings.services.SettingsService;
import io.harness.ngsettings.services.UserSettingsService;
import io.harness.repositories.ngsettings.custom.ConfigurationStateRepository;
import lombok.extern.slf4j.Slf4j;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

@Slf4j
@OwnedBy(PL)

public class UserSettingsCreationJob {
    private final UserSettingsConfig userSettingsConfig;
    private final UserSettingsService userSettingsService;
    private final ConfigurationStateRepository configurationStateRepository;
    private static final String USER_SETTINGS_YAML_PATH = "io/harness/ngsettings/userSettings.yml";
    private PersistentLocker persistentLocker;
    private static final Duration LOCK_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration WAIT_TIMEOUT = Duration.ofSeconds(20);


    @Inject
    public UserSettingsCreationJob ( UserSettingsService userSettingsService, PersistentLocker persistentLocker, ConfigurationStateRepository configurationStateRepository)
    {
        this.userSettingsService = userSettingsService;
        this.configurationStateRepository = configurationStateRepository;
        this.persistentLocker = persistentLocker;
        ObjectMapper om = new ObjectMapper(new YAMLFactory());
        URL url = getClass().getClassLoader().getResource(USER_SETTINGS_YAML_PATH);
        try {
            byte[] bytes = Resources.toByteArray(url);
            this.userSettingsConfig = om.readValue(bytes, UserSettingsConfig.class);
            validateConfig(userSettingsConfig);
        }
        catch (IOException ex)
        {
            throw new InvalidRequestException("User Settings file path is invalid or the syntax is incorrect", ex);
        }
    }

    public void validateConfig( UserSettingsConfig userSettingsConfig)
    {
        if(null == userSettingsConfig)
        {
            throw new InvalidRequestException("Missing user settings config. Check userSettings.yml file");
        }
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        final Set<ConstraintViolation<UserSettingsConfig>> violations = validator.validate(userSettingsConfig);
        StringBuilder builder = new StringBuilder("Validation violations:\n");
        violations.forEach(violation -> {
            builder.append(String.format("%s: %s%n", violation.getPropertyPath(), violation.getMessage()));
        });
        if (isNotEmpty(violations)) {
            throw new InvalidRequestException(builder.toString());
        }
        if (isNotEmpty(userSettingsConfig.getUserSettings())) {
            Set<String> identifiers = new HashSet<>();
            Set<String> duplicateIdentifiers = userSettingsConfig.getUserSettings()
                    .stream()
                    .map(SettingConfiguration::getIdentifier)
                    .filter(identifier -> !identifiers.add(identifier))
                    .collect(Collectors.toSet());
            if (isNotEmpty(duplicateIdentifiers)) {
                throw new InvalidRequestException(String.format(
                        "Identifiers must be uniques in %s. Duplicate identifiers: %s", USER_SETTINGS_YAML_PATH, duplicateIdentifiers));
            }
        }
    }


    public void run() {
        String lockName = String.format("%s_userSettingConfigurationsLock", UserSettingsCreationJob.class.getName());
        try (AcquiredLock<?> lock = persistentLocker.waitToAcquireLockOptional(lockName, LOCK_TIMEOUT, WAIT_TIMEOUT)) {
            if (lock == null) {
                log.warn("Count not acquire the lock- {}", lockName);
                return;
            }
            Optional<SettingsConfigurationState> optional =
                    configurationStateRepository.getByIdentifier(userSettingsConfig.getName());

            if (optional.isPresent() && optional.get().getConfigVersion() >= userSettingsConfig.getVersion()) {
                log.info("User Settings are already updated in the database");
                return;
            }

            log.info("Updating settings in the database");


            Set<UserSettingConfiguration> latestUserSettings = isNotEmpty(userSettingsConfig.getUserSettings()) ? userSettingsConfig.getUserSettings() : new HashSet<>();
            Set<UserSettingConfiguration> currentUserSettings = new HashSet<>(userSettingsService.listDefaultSettings());
            Set<String> latestIdentifiers =
                    latestUserSettings.stream().map(UserSettingConfiguration::getIdentifier).collect(Collectors.toSet());
            Set<String> currentIdentifiers =
                    currentUserSettings.stream().map(UserSettingConfiguration::getIdentifier).collect(Collectors.toSet());
            Set<String> removedIdentifiers = Sets.difference(currentIdentifiers, latestIdentifiers);
            Map<String, String> userSettingIdMap = new HashMap<>();
            Map<String, UserSettingConfiguration> currentUserSettingsMap = new HashMap<>();

            currentUserSettings.forEach(userSettingConfiguration -> {
                currentUserSettingsMap.put(userSettingConfiguration.getIdentifier(), userSettingConfiguration);
                userSettingIdMap.put(userSettingConfiguration.getIdentifier(), userSettingConfiguration.getId());
                userSettingConfiguration.setId(null);
            });
            Set<UserSettingConfiguration> toBeUpsertedSettings = new HashSet<>(latestUserSettings);
            toBeUpsertedSettings.removeAll(currentUserSettings);
            toBeUpsertedSettings.forEach(userSettings -> {
                userSettings.setId(userSettingIdMap.get(userSettings.getIdentifier()));
                try {
                    userSettingsService.upsertSettingConfiguration(userSettings);
                    log.info("Upserting user setting  - \n Current config- {} \n Updated config- {}",
                            currentUserSettingsMap.get(userSettings.getIdentifier()), userSettings);
                } catch (Exception exception) {
                    log.error(String.format("Error while updating user setting [%s]", userSettings.getIdentifier()), exception);
                    throw exception;
                }
            });
            removedIdentifiers.forEach(userSettingsService::removeSetting);

            SettingsConfigurationState configurationState =
                    optional.orElseGet(() -> SettingsConfigurationState.builder().identifier(userSettingsConfig.getName()).build());
            configurationState.setConfigVersion(userSettingsConfig.getVersion());
            configurationStateRepository.upsert(configurationState);
        }
    }
    }

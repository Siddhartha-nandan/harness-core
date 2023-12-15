package io.harness.repositories.ngsettings.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.entities.UserSettingConfiguration;
import io.harness.repositories.ngsettings.custom.UserSettingConfigurationRepositoryCustom;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@HarnessRepo
public interface UserSettingConfigurationRepository   extends PagingAndSortingRepository<UserSettingConfiguration, String>, UserSettingConfigurationRepositoryCustom {

    Optional<UserSettingConfiguration> findByIdentifier(String identifier);

}

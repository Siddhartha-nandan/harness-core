package io.harness.repositories.ngsettings.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ngsettings.SettingCategory;
import io.harness.ngsettings.entities.Setting;
import io.harness.ngsettings.entities.UserSetting;
import io.harness.repositories.ngsettings.custom.UserSettingRepositoryCustom;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

import static io.harness.annotations.dev.HarnessTeam.PL;

@OwnedBy(PL)
@HarnessRepo
public interface UserSettingRepository extends CrudRepository<UserSetting, String>, UserSettingRepositoryCustom {

}

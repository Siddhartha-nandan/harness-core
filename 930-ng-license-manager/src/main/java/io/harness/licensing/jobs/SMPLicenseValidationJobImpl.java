/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.licensing.jobs;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.entities.modules.ModuleLicense;
import io.harness.licensing.mappers.LicenseObjectConverter;
import io.harness.repositories.ModuleLicenseRepository;
import io.harness.smp.license.models.SMPLicense;
import io.harness.smp.license.v1.LicenseGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class SMPLicenseValidationJobImpl implements SMPLicenseValidationJob {

    private final LicenseGenerator licenseGenerator;
    private final LicenseObjectConverter licenseObjectConverter;
    private final ScheduledExecutorService executorService;
    private final ModuleLicenseRepository moduleLicenseRepository;
    private long lastValidTimeMs;

    @Inject
    public SMPLicenseValidationJobImpl(LicenseGenerator licenseGenerator,
                                       LicenseObjectConverter licenseObjectConverter,
                                       @Named("SMP_EXECUTOR_SERVICE") ScheduledExecutorService executorService,
                                       ModuleLicenseRepository moduleLicenseRepository) {
        this.licenseGenerator = licenseGenerator;
        this.licenseObjectConverter = licenseObjectConverter;
        this.executorService = executorService;
        this.moduleLicenseRepository = moduleLicenseRepository;
        this.lastValidTimeMs = 0;
    }

    @Override
    public void scheduleValidation(String accountIdentifier, String licenseSign, int frequencyInMinutes) {
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                List<ModuleLicense> moduleLicenses = moduleLicenseRepository.findByAccountIdentifier(accountIdentifier);
                String sign = sign(moduleLicenses);
                String systemLicenseSign = "";
                if (!StringUtils.isEmpty(licenseSign)) {
                    systemLicenseSign = licenseSign;
                }
                boolean licenseMatch = systemLicenseSign.equals(sign);
                if (licenseMatch) {
                    lastValidTimeMs = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - lastValidTimeMs > 259500000) {
                        log.error("License validation is failing for past 3 days. Either do helm upgrade" +
                                " with correct license file or contact harness for support");
                    }
                }
            }

            private String sign(List<ModuleLicense> moduleLicenses) {
                try {
                    SMPLicense smpLicense = SMPLicense.builder()
                            .moduleLicenses(moduleLicenses.stream()
                                    .map(licenseObjectConverter::toDTO)
                                    .map(m -> (ModuleLicenseDTO) m)
                                    .collect(Collectors.toList()))
                            .build();
                    return licenseGenerator.sign(smpLicense);
                } catch (Exception e) {
                    log.error("Unable to generate license during validation: {}", e.getMessage());
                    return "";
                }
            }
        }, frequencyInMinutes, frequencyInMinutes, TimeUnit.MINUTES);

    }
}

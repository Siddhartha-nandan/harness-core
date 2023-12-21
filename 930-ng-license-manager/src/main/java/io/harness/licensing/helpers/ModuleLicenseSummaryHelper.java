/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.licensing.helpers;

import io.harness.ModuleType;
import io.harness.licensing.beans.modules.CDModuleLicenseDTO;
import io.harness.licensing.beans.modules.CEModuleLicenseDTO;
import io.harness.licensing.beans.modules.CETModuleLicenseDTO;
import io.harness.licensing.beans.modules.CFModuleLicenseDTO;
import io.harness.licensing.beans.modules.CIModuleLicenseDTO;
import io.harness.licensing.beans.modules.ChaosModuleLicenseDTO;
import io.harness.licensing.beans.modules.CodeModuleLicenseDTO;
import io.harness.licensing.beans.modules.IACMModuleLicenseDTO;
import io.harness.licensing.beans.modules.IDPModuleLicenseDTO;
import io.harness.licensing.beans.modules.ModuleLicenseDTO;
import io.harness.licensing.beans.modules.SEIModuleLicenseDTO;
import io.harness.licensing.beans.modules.SRMModuleLicenseDTO;
import io.harness.licensing.beans.modules.STOModuleLicenseDTO;
import io.harness.licensing.beans.summary.CDLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CELicenseSummaryDTO;
import io.harness.licensing.beans.summary.CETLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CFLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CILicenseSummaryDTO;
import io.harness.licensing.beans.summary.CVLicenseSummaryDTO;
import io.harness.licensing.beans.summary.ChaosLicenseSummaryDTO;
import io.harness.licensing.beans.summary.CodeLicenseSummaryDTO;
import io.harness.licensing.beans.summary.IACMLicenseSummaryDTO;
import io.harness.licensing.beans.summary.IDPLicenseSummaryDTO;
import io.harness.licensing.beans.summary.LicensesWithSummaryDTO;
import io.harness.licensing.beans.summary.SEILicenseSummaryDTO;
import io.harness.licensing.beans.summary.STOLicenseSummaryDTO;
import io.harness.licensing.utils.ModuleLicenseUtils;

import com.google.inject.Singleton;
import java.time.Instant;
import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
@Singleton
public class ModuleLicenseSummaryHelper {
  public static LicensesWithSummaryDTO generateSummary(
      ModuleType moduleType, List<ModuleLicenseDTO> moduleLicenseDTOs) {
    long currentTime = Instant.now().toEpochMilli();

    SummaryHandler summaryHandler;
    LicensesWithSummaryDTO licensesWithSummaryDTO;
    switch (moduleType) {
      case CI:
        licensesWithSummaryDTO = CILicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CIModuleLicenseDTO temp = (CIModuleLicenseDTO) moduleLicenseDTO;
          CILicenseSummaryDTO ciLicenseSummaryDTO = (CILicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfCommitters() != null) {
              ciLicenseSummaryDTO.setTotalDevelopers(ModuleLicenseUtils.computeAdd(
                  ciLicenseSummaryDTO.getTotalDevelopers(), temp.getNumberOfCommitters()));
            }
            if (temp.getCacheAllowance() != null) {
              ciLicenseSummaryDTO.setCacheSizeAllowance(
                  ModuleLicenseUtils.computeAdd(ciLicenseSummaryDTO.getCacheSizeAllowance(), temp.getCacheAllowance()));
            }
          }
        };
        break;
      case CD:
        licensesWithSummaryDTO = CDLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CDModuleLicenseDTO temp = (CDModuleLicenseDTO) moduleLicenseDTO;
          CDLicenseSummaryDTO cdLicenseSummaryDTO = (CDLicenseSummaryDTO) summaryDTO;

          if (current < temp.getExpiryTime()) {
            if (temp.getWorkloads() != null) {
              cdLicenseSummaryDTO.setTotalWorkload(
                  ModuleLicenseUtils.computeAdd(cdLicenseSummaryDTO.getTotalWorkload(), temp.getWorkloads()));
            }
            if (temp.getServiceInstances() != null) {
              cdLicenseSummaryDTO.setTotalServiceInstances(ModuleLicenseUtils.computeAdd(
                  cdLicenseSummaryDTO.getTotalServiceInstances(), temp.getServiceInstances()));
            }
          }
        };
        break;
      case CV:
      case SRM:
        licensesWithSummaryDTO = CVLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          SRMModuleLicenseDTO temp = (SRMModuleLicenseDTO) moduleLicenseDTO;
          CVLicenseSummaryDTO cvLicenseSummaryDTO = (CVLicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfServices() != null) {
              cvLicenseSummaryDTO.setTotalServices(
                  ModuleLicenseUtils.computeAdd(cvLicenseSummaryDTO.getTotalServices(), temp.getNumberOfServices()));
            }
          }
        };
        break;
      case CF:
        licensesWithSummaryDTO = CFLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CFModuleLicenseDTO temp = (CFModuleLicenseDTO) moduleLicenseDTO;
          CFLicenseSummaryDTO cfLicenseSummaryDTO = (CFLicenseSummaryDTO) summaryDTO;

          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfClientMAUs() != null) {
              cfLicenseSummaryDTO.setTotalClientMAUs(ModuleLicenseUtils.computeAdd(
                  cfLicenseSummaryDTO.getTotalClientMAUs(), temp.getNumberOfClientMAUs()));
            }
            if (temp.getNumberOfUsers() != null) {
              cfLicenseSummaryDTO.setTotalFeatureFlagUnits(ModuleLicenseUtils.computeAdd(
                  cfLicenseSummaryDTO.getTotalFeatureFlagUnits(), temp.getNumberOfUsers()));
            }
          }
        };
        break;
      case CE:
        licensesWithSummaryDTO = CELicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CEModuleLicenseDTO temp = (CEModuleLicenseDTO) moduleLicenseDTO;
          CELicenseSummaryDTO ceLicenseSummaryDTO = (CELicenseSummaryDTO) summaryDTO;

          if (current < temp.getExpiryTime()) {
            if (temp.getSpendLimit() != null) {
              ceLicenseSummaryDTO.setTotalSpendLimit(
                  ModuleLicenseUtils.computeAdd(ceLicenseSummaryDTO.getTotalSpendLimit(), temp.getSpendLimit()));
            }
          }
        };
        break;
      case STO:
        licensesWithSummaryDTO = STOLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          STOModuleLicenseDTO temp = (STOModuleLicenseDTO) moduleLicenseDTO;
          STOLicenseSummaryDTO stoLicenseSummaryDTO = (STOLicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfDevelopers() != null) {
              stoLicenseSummaryDTO.setTotalDevelopers(ModuleLicenseUtils.computeAdd(
                  stoLicenseSummaryDTO.getTotalDevelopers(), temp.getNumberOfDevelopers()));
            }
          }
        };
        break;
      case CHAOS:
        licensesWithSummaryDTO = ChaosLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          ChaosModuleLicenseDTO temp = (ChaosModuleLicenseDTO) moduleLicenseDTO;
          ChaosLicenseSummaryDTO chaosLicenseSummaryDTO = (ChaosLicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getTotalChaosExperimentRuns() != null) {
              chaosLicenseSummaryDTO.setTotalChaosExperimentRuns(ModuleLicenseUtils.computeAdd(
                  chaosLicenseSummaryDTO.getTotalChaosExperimentRuns(), temp.getTotalChaosExperimentRuns()));
            }
            if (temp.getTotalChaosInfrastructures() != null) {
              chaosLicenseSummaryDTO.setTotalChaosInfrastructures(ModuleLicenseUtils.computeAdd(
                  chaosLicenseSummaryDTO.getTotalChaosExperimentRuns(), temp.getTotalChaosInfrastructures()));
            }
          }
        };
        break;
      case IACM:
        licensesWithSummaryDTO = IACMLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          IACMModuleLicenseDTO temp = (IACMModuleLicenseDTO) moduleLicenseDTO;
          IACMLicenseSummaryDTO iacmLicenseSummaryDTO = (IACMLicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfDevelopers() != null) {
              iacmLicenseSummaryDTO.setTotalDevelopers(ModuleLicenseUtils.computeAdd(
                  iacmLicenseSummaryDTO.getTotalDevelopers(), temp.getNumberOfDevelopers()));
            }
          }
        };
        break;
      case CET:
        licensesWithSummaryDTO = CETLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CETModuleLicenseDTO temp = (CETModuleLicenseDTO) moduleLicenseDTO;
          CETLicenseSummaryDTO cetLicenseSummaryDTO = (CETLicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime()) {
            if (temp.getNumberOfAgents() != null) {
              cetLicenseSummaryDTO.setNumberOfAgents(
                  ModuleLicenseUtils.computeAdd(cetLicenseSummaryDTO.getNumberOfAgents(), temp.getNumberOfAgents()));
            }
          }
        };
        break;
      case SEI:
        licensesWithSummaryDTO = SEILicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          SEIModuleLicenseDTO temp = (SEIModuleLicenseDTO) moduleLicenseDTO;
          SEILicenseSummaryDTO seiLicenseSummaryDTO = (SEILicenseSummaryDTO) summaryDTO;
          if (current < temp.getExpiryTime() && temp.getNumberOfContributors() != null) {
            seiLicenseSummaryDTO.setNumberOfContributors(ModuleLicenseUtils.computeAdd(
                seiLicenseSummaryDTO.getNumberOfContributors(), temp.getNumberOfContributors()));
          }
        };
        break;
      case IDP:
        licensesWithSummaryDTO = IDPLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          IDPModuleLicenseDTO idpModuleLicenseDTO = (IDPModuleLicenseDTO) moduleLicenseDTO;
          IDPLicenseSummaryDTO idpLicenseSummaryDTO = (IDPLicenseSummaryDTO) summaryDTO;
          if (current < idpModuleLicenseDTO.getExpiryTime() && idpModuleLicenseDTO.getNumberOfDevelopers() != null) {
            idpLicenseSummaryDTO.setNumberOfDevelopers(ModuleLicenseUtils.computeAdd(
                idpLicenseSummaryDTO.getNumberOfDevelopers(), idpModuleLicenseDTO.getNumberOfDevelopers()));
          }
        };
        break;
      case CODE:
        licensesWithSummaryDTO = CodeLicenseSummaryDTO.builder().build();
        summaryHandler = (moduleLicenseDTO, summaryDTO, current) -> {
          CodeModuleLicenseDTO codeModuleLicenseDTO = (CodeModuleLicenseDTO) moduleLicenseDTO;
          CodeLicenseSummaryDTO codeLicenseSummaryDTO = (CodeLicenseSummaryDTO) summaryDTO;
          if (current < codeModuleLicenseDTO.getExpiryTime() && codeModuleLicenseDTO.getNumberOfDevelopers() != null) {
            codeLicenseSummaryDTO.setNumberOfDevelopers(ModuleLicenseUtils.computeAdd(
                codeLicenseSummaryDTO.getNumberOfDevelopers(), codeModuleLicenseDTO.getNumberOfDevelopers()));
            codeLicenseSummaryDTO.setNumberOfRepositories(ModuleLicenseUtils.computeAdd(
                codeLicenseSummaryDTO.getNumberOfRepositories(), codeModuleLicenseDTO.getNumberOfRepositories()));
          }
        };
        break;

      default:
        throw new UnsupportedOperationException("Unsupported module type");
    }

    moduleLicenseDTOs.forEach(l -> {
      // calculate summary detail info via each moduleLicenseDTO
      summaryHandler.calculateModuleSummary(l, licensesWithSummaryDTO, currentTime);

      // Use the last expiring license info as the summary general info
      if (l.getExpiryTime() > licensesWithSummaryDTO.getMaxExpiryTime()) {
        licensesWithSummaryDTO.setMaxExpiryTime(l.getExpiryTime());
        licensesWithSummaryDTO.setEdition(l.getEdition());
        licensesWithSummaryDTO.setLicenseType(l.getLicenseType());
      }
    });
    licensesWithSummaryDTO.setModuleType(moduleType);
    return licensesWithSummaryDTO;
  }

  private interface SummaryHandler {
    void calculateModuleSummary(
        ModuleLicenseDTO moduleLicenseDTO, LicensesWithSummaryDTO licensesWithSummaryDTO, long currentTime);
  }
}

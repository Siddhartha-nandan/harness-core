/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.assessment.settings.services;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.assessment.settings.beans.dto.AssessmentInviteDTO;
import io.harness.assessment.settings.beans.entities.Assessment;
import io.harness.assessment.settings.beans.entities.Organization;
import io.harness.assessment.settings.beans.entities.User;
import io.harness.assessment.settings.beans.entities.UserInvitation;
import io.harness.assessment.settings.repositories.AssessmentRepository;
import io.harness.assessment.settings.repositories.OrganizationRepository;
import io.harness.assessment.settings.repositories.UserInvitationRepository;
import io.harness.assessment.settings.repositories.UserRepository;

import com.google.inject.Inject;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import javax.ws.rs.BadRequestException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(HarnessTeam.SEI)
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
public class InvitationServiceImpl implements InvitationService {
  private UserRepository userRepository;
  private OrganizationRepository organizationRepository;
  private AssessmentRepository assessmentRepository;

  private UserInvitationRepository userInvitationRepository;

  @Override
  public AssessmentInviteDTO sendAssessmentInvite(AssessmentInviteDTO assessmentInviteDTO) {
    // call crunchbase or some other API
    String assessmentId = assessmentInviteDTO.getAssessmentId();
    Optional<Assessment> assessmentOptional =
        assessmentRepository.findFirstByAssessmentIdOrderByVersionDesc(assessmentId);
    if (assessmentOptional.isEmpty()) {
      throw new BadRequestException("Assessment not found, for Id : " + assessmentId);
    }
    // add code to reject generic emails TODO
    String encoded;
    // check for repeat emails and reject those TODO
    for (String userEmail : assessmentInviteDTO.getEmails()) {
      try {
        String organizationId = StringUtils.substringAfter(userEmail, "@");
        Optional<Organization> organizationOptional = organizationRepository.findById(organizationId);
        if (organizationOptional.isEmpty()) {
          organizationRepository.save(
              Organization.builder().id(organizationId).organizationName(organizationId).build());
        }
        encoded = TokenGenerationUtil.generateInviteFromEmail(userEmail, assessmentId);
        String invitedBy = assessmentInviteDTO.getInvitedBy().orElse("Self");
        // check user is not already there TODO
        // tie to correct org.
        Optional<User> optionalUser = userRepository.findOneByUserId(userEmail);
        if (optionalUser.isEmpty()) {
          User user = User.builder().userId(userEmail).organizationId(organizationId).build();
          userRepository.save(user);
        }
        UserInvitation userInvitation = UserInvitation.builder()
                                            .userId(userEmail)
                                            .generatedCode(encoded)
                                            .assessmentId(assessmentId)
                                            .invitedBy(invitedBy)
                                            .build();
        userInvitationRepository.save(userInvitation);
      } catch (NoSuchAlgorithmException e) {
        //        throw new RuntimeException(e);
        log.error("Cannot invite : {} for assessment {}", userEmail, assessmentId);
      }
    }
    // TODO generate a unique code and send it to email.
    // Create the user in DB
    // call sending invite.
    return assessmentInviteDTO;
  }
}

/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.shell;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.BOJAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.artifactory.ArtifactoryConfigRequest;
import io.harness.artifactory.ArtifactoryNgService;
import io.harness.category.element.UnitTests;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthType;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryAuthenticationDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryConnectorDTO;
import io.harness.delegate.beans.connector.artifactoryconnector.ArtifactoryUsernamePasswordAuthDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsAuthType;
import io.harness.delegate.beans.connector.jenkins.JenkinsAuthenticationDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsConnectorDTO;
import io.harness.delegate.beans.connector.jenkins.JenkinsUserNamePasswordDTO;
import io.harness.delegate.task.artifactory.ArtifactoryRequestMapper;
import io.harness.delegate.task.ssh.artifact.ArtifactoryArtifactDelegateConfig;
import io.harness.delegate.task.ssh.artifact.JenkinsArtifactDelegateConfig;
import io.harness.delegate.task.winrm.ArtifactoryArtifactDownloadHandler;
import io.harness.delegate.task.winrm.JenkinsArtifactDownloadHandler;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.shell.ScriptType;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@OwnedBy(CDP)
@FieldDefaults(level = AccessLevel.PRIVATE)
@RunWith(MockitoJUnitRunner.Silent.class)
public class ArtifactDownloadHandlerTest extends CategoryTest {
  private static final String SECRET_IDENT = "secret_ident";
  private static final char[] DECRYPTED_PASSWORD_VALUE = new char[] {'t', 'e', 's', 't'};
  @InjectMocks private ArtifactoryArtifactDownloadHandler artifactoryArtifactDownloadHandler;
  @InjectMocks private JenkinsArtifactDownloadHandler jenkinsArtifactDownloadHandler;
  @Mock private ArtifactoryRequestMapper artifactoryRequestMapper;
  @Mock private SecretDecryptionService secretDecryptionService;
  @Mock private ArtifactoryNgService artifactoryNgService;
  private SecretRefData passwordRef;

  @Before
  public void setup() {
    passwordRef = SecretRefData.builder().identifier(SECRET_IDENT).decryptedValue(DECRYPTED_PASSWORD_VALUE).build();
    JenkinsUserNamePasswordDTO credentials =
        JenkinsUserNamePasswordDTO.builder().username("username").passwordRef(passwordRef).build();
    when(secretDecryptionService.decrypt(any(JenkinsUserNamePasswordDTO.class), any())).thenReturn(credentials);

    ArtifactoryUsernamePasswordAuthDTO artifactoryUsernamePasswordAuthDTO =
        ArtifactoryUsernamePasswordAuthDTO.builder().username("username").passwordRef(passwordRef).build();
    when(secretDecryptionService.decrypt(any(ArtifactoryUsernamePasswordAuthDTO.class), any()))
        .thenReturn(artifactoryUsernamePasswordAuthDTO);
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testArtifactoryGetCommandString_BASH() {
    when(artifactoryRequestMapper.toArtifactoryRequest(any())).thenReturn(getDefaultArtifactoryConfigRequest());
    ConnectorInfoDTO connectorDTO = getArtifactoryConnectorInfoDTO();
    ArtifactoryArtifactDelegateConfig artifactDelegateConfig = ArtifactoryArtifactDelegateConfig.builder()
                                                                   .repositoryName("repo_name")
                                                                   .artifactPath("artifact_path")
                                                                   .identifier("identifier")
                                                                   .artifactDirectory("testdir")
                                                                   .connectorDTO(connectorDTO)
                                                                   .build();
    String commandString =
        artifactoryArtifactDownloadHandler.getCommandString(artifactDelegateConfig, "testdestination", ScriptType.BASH);

    assertThat(commandString)
        .isEqualTo(
            "curl -L --fail -X GET \"http://hostname/repo_name/artifact_path\" -o \"testdestination/artifact_path\"");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testArtifactoryGetCommandStringWithAuth_BASH() {
    when(artifactoryRequestMapper.toArtifactoryRequest(any())).thenReturn(getArtifactoryConfigRequestWithPass());
    ConnectorInfoDTO connectorDTO = getArtifactoryConnectorInfoDTOWithSecret();
    ArtifactoryArtifactDelegateConfig artifactDelegateConfig = ArtifactoryArtifactDelegateConfig.builder()
                                                                   .repositoryName("repo_name")
                                                                   .artifactPath("artifact_path")
                                                                   .identifier("identifier")
                                                                   .artifactDirectory("testdir")
                                                                   .connectorDTO(connectorDTO)
                                                                   .build();
    String commandString =
        artifactoryArtifactDownloadHandler.getCommandString(artifactDelegateConfig, "testdestination", ScriptType.BASH);

    assertThat(commandString)
        .isEqualTo("curl -L --fail -H \"Authorization: Basic dXNlcm5hbWU6dGVzdA==\""
            + " -X GET \"http://hostname/repo_name/artifact_path\""
            + " -o \"testdestination/artifact_path\"");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testArtifactoryGetCommandString_POWERSHELL() {
    when(artifactoryRequestMapper.toArtifactoryRequest(any())).thenReturn(getDefaultArtifactoryConfigRequest());
    ArtifactoryArtifactDelegateConfig artifactDelegateConfig = ArtifactoryArtifactDelegateConfig.builder()
                                                                   .repositoryName("repo_name")
                                                                   .artifactPath("artifact_path")
                                                                   .identifier("identifier")
                                                                   .artifactDirectory("testdir")
                                                                   .connectorDTO(getArtifactoryConnectorInfoDTO())
                                                                   .build();
    String commandString = artifactoryArtifactDownloadHandler.getCommandString(
        artifactDelegateConfig, "testdestination", ScriptType.POWERSHELL);

    assertThat(commandString)
        .isEqualTo("[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n"
            + " $ProgressPreference = 'SilentlyContinue'\n"
            + "Invoke-WebRequest -Uri \"http://hostname/repo_name/artifact_path\" -OutFile \"testdestination\\artifact_path\"");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testArtifactoryGetCommandStringWithAuth_POWERSHELL() {
    when(artifactoryRequestMapper.toArtifactoryRequest(any())).thenReturn(getArtifactoryConfigRequestWithPass());

    ConnectorInfoDTO connectorDTO = getArtifactoryConnectorInfoDTOWithSecret();

    ArtifactoryArtifactDelegateConfig artifactDelegateConfig = ArtifactoryArtifactDelegateConfig.builder()
                                                                   .repositoryName("repo_name")
                                                                   .artifactPath("artifact_path")
                                                                   .identifier("identifier")
                                                                   .artifactDirectory("testdir")
                                                                   .connectorDTO(connectorDTO)
                                                                   .build();
    String commandString = artifactoryArtifactDownloadHandler.getCommandString(
        artifactDelegateConfig, "testdestination", ScriptType.POWERSHELL);

    assertThat(commandString)
        .isEqualTo("$Headers = @{\n"
            + "    Authorization = \"Basic dXNlcm5hbWU6dGVzdA==\"\n"
            + "}\n"
            + " [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12\n"
            + " $ProgressPreference = 'SilentlyContinue'\n"
            + " Invoke-WebRequest -Uri \"http://hostname/repo_name/artifact_path\" -Headers $Headers -OutFile \"testdestination\\artifact_path\"");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testJenkinsGetCommandString_BASH() {
    ConnectorInfoDTO connectorDTO = getJenkinsConnectorInfoDTO();
    JenkinsArtifactDelegateConfig artifactDelegateConfig = JenkinsArtifactDelegateConfig.builder()
                                                               .artifactPath("artifact_path")
                                                               .identifier("identifier")
                                                               .connectorDTO(connectorDTO)
                                                               .jobName("job_name")
                                                               .build("build_number54")
                                                               .build();
    String commandString =
        jenkinsArtifactDownloadHandler.getCommandString(artifactDelegateConfig, "testdestination", ScriptType.BASH);

    assertThat(commandString)
        .isEqualTo(
            "curl --fail -H \"Authorization: null\" -X GET \"http://hostname/job/job_name/build_number54/artifact/artifact_path\" -o \"testdestination/artifact_path\"");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testJenkinsGetCommandStringWithAuth_BASH() {
    ConnectorInfoDTO connectorDTO = getJenkinsConnectorInfoDTOWithSecret();
    JenkinsArtifactDelegateConfig artifactDelegateConfig = JenkinsArtifactDelegateConfig.builder()
                                                               .artifactPath("artifact_path")
                                                               .identifier("identifier")
                                                               .connectorDTO(connectorDTO)
                                                               .jobName("job_name")
                                                               .build("build_number54")
                                                               .build();
    String commandString =
        jenkinsArtifactDownloadHandler.getCommandString(artifactDelegateConfig, "testdestination", ScriptType.BASH);

    assertThat(commandString)
        .isEqualTo("curl --fail -H \"Authorization: Basic dXNlcm5hbWU6dGVzdA==\" "
            + "-X GET \"http://hostname/job/job_name/build_number54/artifact/artifact_path\""
            + " -o \"testdestination/artifact_path\"");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testJenkinsGetCommandString_POWERSHELL() {
    ConnectorInfoDTO connectorDTO = getJenkinsConnectorInfoDTO();

    JenkinsArtifactDelegateConfig artifactDelegateConfig = JenkinsArtifactDelegateConfig.builder()
                                                               .artifactPath("artifact_path")
                                                               .identifier("identifier")
                                                               .jobName("job_name")
                                                               .build("build_number54")
                                                               .connectorDTO(connectorDTO)
                                                               .build();
    String commandString = jenkinsArtifactDownloadHandler.getCommandString(
        artifactDelegateConfig, "testdestination", ScriptType.POWERSHELL);

    assertThat(commandString)
        .isEqualTo("$webClient = New-Object System.Net.WebClient \n"
            + "$webClient.Headers[[System.Net.HttpRequestHeader]::Authorization] = \"null\";\n"
            + "$url = \"http://hostname/job/job_name/build_number54/artifact/artifact_path\" \n"
            + "$localfilename = \"testdestination\\artifact_path\" \n"
            + "$webClient.DownloadFile($url, $localfilename)");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void testJenkinsGetCommandStringWithAuth_POWERSHELL() {
    ConnectorInfoDTO connectorDTO = getJenkinsConnectorInfoDTOWithSecret();
    JenkinsArtifactDelegateConfig artifactDelegateConfig = JenkinsArtifactDelegateConfig.builder()
                                                               .artifactPath("artifact_path")
                                                               .identifier("identifier")
                                                               .connectorDTO(connectorDTO)
                                                               .jobName("job_name")
                                                               .build("build_number54")
                                                               .build();
    String commandString = jenkinsArtifactDownloadHandler.getCommandString(
        artifactDelegateConfig, "testdestination", ScriptType.POWERSHELL);

    assertThat(commandString)
        .isEqualTo("$webClient = New-Object System.Net.WebClient \n"
            + "$webClient.Headers[[System.Net.HttpRequestHeader]::Authorization] = \"Basic dXNlcm5hbWU6dGVzdA==\";\n"
            + "$url = \"http://hostname/job/job_name/build_number54/artifact/artifact_path\" \n"
            + "$localfilename = \"testdestination\\artifact_path\" \n"
            + "$webClient.DownloadFile($url, $localfilename)");
  }

  private ConnectorInfoDTO getArtifactoryConnectorInfoDTOWithSecret() {
    SecretRefData passwordRef = SecretRefData.builder().identifier("secret_ident").build();
    ArtifactoryUsernamePasswordAuthDTO credentials =
        ArtifactoryUsernamePasswordAuthDTO.builder().username("username").passwordRef(passwordRef).build();
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO = ArtifactoryAuthenticationDTO.builder()
                                                                    .authType(ArtifactoryAuthType.USER_PASSWORD)
                                                                    .credentials(credentials)
                                                                    .build();
    ArtifactoryConnectorDTO connectorConfig = ArtifactoryConnectorDTO.builder()
                                                  .auth(artifactoryAuthenticationDTO)
                                                  .artifactoryServerUrl("http://hostname")
                                                  .build();
    return ConnectorInfoDTO.builder().connectorConfig(connectorConfig).build();
  }

  private ConnectorInfoDTO getJenkinsConnectorInfoDTOWithSecret() {
    SecretRefData passwordRef = SecretRefData.builder().identifier("secret_ident").build();
    JenkinsUserNamePasswordDTO credentials =
        JenkinsUserNamePasswordDTO.builder().username("username").passwordRef(passwordRef).build();
    JenkinsAuthenticationDTO jenkinsAuthenticationDTO =
        JenkinsAuthenticationDTO.builder().authType(JenkinsAuthType.USER_PASSWORD).credentials(credentials).build();
    JenkinsConnectorDTO connectorConfig =
        JenkinsConnectorDTO.builder().jenkinsUrl("http://hostname").auth(jenkinsAuthenticationDTO).build();
    return ConnectorInfoDTO.builder().connectorConfig(connectorConfig).build();
  }

  private ConnectorInfoDTO getJenkinsConnectorInfoDTO() {
    JenkinsAuthenticationDTO jenkinsAuthenticationDTO =
        JenkinsAuthenticationDTO.builder().authType(JenkinsAuthType.ANONYMOUS).build();
    JenkinsConnectorDTO connectorConfig =
        JenkinsConnectorDTO.builder().jenkinsUrl("http://hostname").auth(jenkinsAuthenticationDTO).build();
    return ConnectorInfoDTO.builder().connectorConfig(connectorConfig).build();
  }

  private ConnectorInfoDTO getArtifactoryConnectorInfoDTO() {
    ArtifactoryAuthenticationDTO artifactoryAuthenticationDTO =
        ArtifactoryAuthenticationDTO.builder().authType(ArtifactoryAuthType.ANONYMOUS).build();
    ArtifactoryConnectorDTO connectorConfig = ArtifactoryConnectorDTO.builder()
                                                  .auth(artifactoryAuthenticationDTO)
                                                  .artifactoryServerUrl("http://hostname")
                                                  .build();
    return ConnectorInfoDTO.builder().connectorConfig(connectorConfig).build();
  }

  private ArtifactoryConfigRequest getArtifactoryConfigRequestWithPass() {
    return ArtifactoryConfigRequest.builder()
        .artifactoryUrl("http://hostname")
        .artifactRepositoryUrl("")
        .hasCredentials(true)
        .username("username")
        .password(DECRYPTED_PASSWORD_VALUE)
        .build();
  }

  private ArtifactoryConfigRequest getDefaultArtifactoryConfigRequest() {
    return ArtifactoryConfigRequest.builder()
        .artifactoryUrl("http://hostname")
        .hasCredentials(false)
        .username("username")
        .build();
  }
}
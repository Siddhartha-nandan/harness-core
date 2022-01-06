package io.harness.secretmanagerclient.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static com.fasterxml.jackson.annotation.JsonSubTypes.Type;

import io.harness.annotations.dev.OwnedBy;
import io.harness.secretmanagerclient.dto.awskms.AwsKmsConfigDTO;
import io.harness.secretmanagerclient.dto.awssecretmanager.AwsSMConfigDTO;
import io.harness.secretmanagerclient.dto.azurekeyvault.AzureKeyVaultConfigDTO;
import io.harness.security.encryption.EncryptionType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@OwnedBy(PL)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "encryptionType",
    visible = true)
@JsonSubTypes({
  @Type(name = "LOCAL", value = LocalConfigDTO.class)
  , @Type(name = "VAULT", value = VaultConfigDTO.class), @Type(name = "GCP_KMS", value = GcpKmsConfigDTO.class),
      @Type(name = "KMS", value = AwsKmsConfigDTO.class),
      @Type(name = "AZURE_VAULT", value = AzureKeyVaultConfigDTO.class),
      @Type(name = "AWS_SECRETS_MANAGER", value = AwsSMConfigDTO.class)
})
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(
    name = "SecretManagerConfig", description = "This is the view of the SecretManagerConfig entity defined in Harness")
public abstract class SecretManagerConfigDTO {
  private String name;
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private Map<String, String> tags;
  private String identifier;
  private String description;

  private EncryptionType encryptionType;
  @JsonProperty("default") private boolean isDefault;
  private boolean harnessManaged;
}

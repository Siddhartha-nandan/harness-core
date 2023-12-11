package io.harness.ng.core.dto;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MoveProjectRequest {
  private String destinationOrgIdentifier;
}

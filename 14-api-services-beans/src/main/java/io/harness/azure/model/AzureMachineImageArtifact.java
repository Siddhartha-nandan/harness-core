package io.harness.azure.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureMachineImageArtifact {
  private ImageType imageType;
  private OSType osType;

  private MachineImageReference imageReference;

  public enum OSType { LINUX, WINDOWS }

  public enum ImageType {
    IMAGE_GALLERY,
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MachineImageReference {
    private String id;
    private String publisher;
    private String offer;
    private String sku;
    private String version;
    private OsState osState;

    public enum OsState {
      GENERALIZED("Generalized"),
      SPECIALIZED("Specialized");

      private final String value;

      OsState(String value) {
        this.value = value;
      }

      public static OsState fromString(String value) {
        OsState[] items = OsState.values();
        for (OsState item : items) {
          if (item.toString().equalsIgnoreCase(value)) {
            return item;
          }
        }
        return null;
      }

      @Override
      public String toString() {
        return this.value;
      }
    }
  }
}

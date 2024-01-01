package io.harness.ssca.utils;

import io.harness.ssca.entities.OperatorEntity;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.experimental.UtilityClass;
import org.apache.maven.artifact.versioning.ComparableVersion;

@UtilityClass
public class VersionUtils {
  public boolean compareVersions(
      @NotBlank String version1, @NotNull OperatorEntity operatorEntity, @NotBlank String version2) {
    ComparableVersion comparableVersion1 = new ComparableVersion(version1);
    ComparableVersion comparableVersion2 = new ComparableVersion(version2);
    switch (operatorEntity) {
      case EQUALS: {
        return comparableVersion1.compareTo(comparableVersion2) == 0;
      }
      case STARTSWITH: {
        return version1.startsWith(version2);
      }
      case NOTEQUALS: {
        return comparableVersion1.compareTo(comparableVersion2) != 0;
      }
      case GREATERTHAN: {
        return comparableVersion1.compareTo(comparableVersion2) > 0;
      }
      case GREATERTHANEQUALS: {
        return comparableVersion1.compareTo(comparableVersion2) >= 0;
      }
      case LESSTHAN: {
        return comparableVersion1.compareTo(comparableVersion2) < 0;
      }
      case LESSTHANEQUALS: {
        return comparableVersion1.compareTo(comparableVersion2) <= 0;
      }
      default:
        throw new IllegalArgumentException("Illegal comparison operator " + operatorEntity);
    }
  }
}

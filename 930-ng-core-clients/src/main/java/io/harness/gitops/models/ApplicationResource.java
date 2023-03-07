package io.harness.gitops.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationResource {
  @JsonProperty("agentIdentifier") public String agentIdentifier;
  @JsonProperty("name") public String name;
  @JsonProperty("stale") public Boolean stale;
  @JsonProperty("app") public App app;

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class App {
    @JsonProperty("metadata") public ApplicationMetadata metadata;
    @JsonProperty("spec") public ApplicationSpec spec;
    @JsonProperty("status") public ApplicationStatus status;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApplicationMetadata {
    @JsonProperty("name") public String name;
    @JsonProperty("namespace") public String namespace;
    @JsonProperty("ownerReferences") public List<OwnerReference> ownerReferences;

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class OwnerReference {
      @JsonProperty("apiVersion") public String apiVersion;
      @JsonProperty("kind") public String kind;
      @JsonProperty("name") public String name;
    }
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApplicationSpec {
    @JsonProperty("source") public Source source;
    @JsonProperty("destination") public Destination destination;
    @JsonProperty("project") public String project;
    @JsonProperty("syncPolicy") public SyncPolicy syncPolicy;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Source {
    @JsonProperty("repoURL") public String repoURL;
    @JsonProperty("path") public String path;
    @JsonProperty("targetRevision") public String targetRevision;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Destination {
    @JsonProperty("server") public String server;
    @JsonProperty("namespace") public String namespace;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SyncPolicy {
    @JsonProperty("automated") public Automated automated;

    @Data
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Automated {}
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class ApplicationStatus {
    @JsonProperty("resources") public List<Resource> resources;
    @JsonProperty("sync") public Sync sync;
    @JsonProperty("health") public Health health;
    @JsonProperty("operationState") public OperationState operationState;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Sync {
    @JsonProperty("status") public String status;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class OperationState {
    @JsonProperty("phase") public String phase;
    @JsonProperty("message") public String message;
    @JsonProperty("syncResult") public SyncResult syncResult;
    @JsonProperty("startedAt") public String startedAt;
    @JsonProperty("finishedAt") public String finishedAt;
    @JsonProperty("startedAtTs") public String startedAtTs;
    @JsonProperty("finishedAtTs") public String finishedAtTs;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Health {
    @JsonProperty("status") public String status;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Resource {
    @JsonProperty("group") public String group;
    @JsonProperty("version") public String version;
    @JsonProperty("kind") public String kind;
    @JsonProperty("namespace") public String namespace;
    @JsonProperty("name") public String name;
    @JsonProperty("status") public String status;
    @JsonProperty("health") public Health health;
  }

  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class SyncResult {
    @JsonProperty("resources") public List<Resource> resources;
    @JsonProperty("revision") public String revision;
    @JsonProperty("source") public Source source;
  }
}

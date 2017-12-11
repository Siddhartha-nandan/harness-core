package software.wings.beans.yaml;

/**
 * @author rktummala on 10/17/17
 */
public interface YamlConstants {
  String PATH_DELIMITER = "/";
  String ANY = ".[^/]*?";
  String YAML_EXPRESSION = ".[^/]*?\\.yaml";
  String YAML_FILE_NAME_PATTERN = ".*?" + PATH_DELIMITER + YAML_EXPRESSION;
  //  String SETUP_ENTITY_ID = "setup";
  String SETUP_FOLDER = "Setup";
  String APPLICATIONS_FOLDER = "Applications";
  String SERVICES_FOLDER = "Services";
  String COMMANDS_FOLDER = "Commands";
  String CONFIG_FILES_FOLDER = "Config Files";
  String ENVIRONMENTS_FOLDER = "Environments";
  String INFRA_MAPPING_FOLDER = "Service Infrastructure";
  String WORKFLOWS_FOLDER = "Workflows";
  String PIPELINES_FOLDER = "Pipelines";

  String CLOUD_PROVIDERS_FOLDER = "Cloud Providers";
  String ARTIFACT_SERVERS_FOLDER = "Artifact Servers";
  String COLLABORATION_PROVIDERS_FOLDER = "Collaboration Providers";
  String VERIFICATION_PROVIDERS_FOLDER = "Verification Providers";

  String ARTIFACT_SOURCES_FOLDER = "Artifact Servers";
  String DEPLOYMENT_SPECIFICATION_FOLDER = "Deployment Specifications";

  String LOAD_BALANCERS_FOLDER = "Load Balancers";

  String YAML_EXTENSION = ".yaml";
  String INDEX_YAML = "Index.yaml";
  String ARTIFACT_SERVER = "ARTIFACT_SERVER";
  String COLLABORATION_PROVIDER = "COLLABORATION_PROVIDER";
  String LOADBALANCER_PROVIDER = "LOADBALANCER_PROVIDER";
  String VERIFICATION_PROVIDER = "VERIFICATION_PROVIDER";

  String DEPLOYMENT_SPECIFICATION = "DEPLOYMENT_SPECIFICATION";

  String LAMBDA_SPEC_YAML_FILE_NAME = "Lambda";
  String ECS_CONTAINER_TASK_YAML_FILE_NAME = "Ecs";
  String KUBERNETES_CONTAINER_TASK_YAML_FILE_NAME = "Kubernetes";

  String LINK_PREFIX = "link";
  String NODE_PREFIX = "node";

  int DEFAULT_COORDINATE = 50;
  int COORDINATE_INCREMENT_BY = 150;

  String NODE_PROPERTY_REFERENCEID = "referenceId";
  String NODE_PROPERTY_COMMAND_STRING = "commandString";
  String NODE_PROPERTY_TAIL_FILES = "tailFiles";
  String NODE_PROPERTY_TAIL_PATTERNS = "tailPatterns";
  String NODE_PROPERTY_COMMAND_TYPE = "commandType";
  String NODE_PROPERTY_COMMAND_PATH = "commandPath";
  String NODE_PROPERTY_FILE_CATEGORY = "fileCategory";
  String NODE_PROPERTY_DESTINATION_DIR_PATH = "destinationDirectoryPath";
  String NODE_PROPERTY_DESTINATION_PARENT_PATH = "destinationParentPath";
}

package software.wings.beans.yaml;

/**
 * @author rktummala on 10/17/17
 */
public interface YamlConstants {
  String PATH_DELIMITER = "/";
  String ANY = ".[^/]*?";
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
  String TRIGGERS_FOLDER = "Triggers";

  String CLOUD_PROVIDERS_FOLDER = "Cloud Providers";
  String CONNECTORS_FOLDER = "Connectors";
  String ARTIFACT_SERVERS_FOLDER = "Artifact Servers";
  String COLLABORATION_PROVIDERS_FOLDER = "Collaboration Providers";
  String LOAD_BALANCER_PROVIDERS_FOLDER = "Load Balancer Providers";
  String VERIFICATION_PROVIDERS_FOLDER = "Verification Providers";
  String AWS_FOLDER = "Amazon Web Services";
  String GCP_FOLDER = "Google Cloud Platform";
  String PHYSICAL_DATA_CENTER_FOLDER = "Physical Data Centers";

  String ARTIFACT_SOURCES_FOLDER = "Artifact Sources";

  String SMTP_FOLDER = "SMTP";
  String SLACK_FOLDER = "Slack";

  String LOAD_BALANCERS_FOLDER = "Load Balancers";
  String ELB_FOLDER = "Elastic Classic Load Balancers";

  String JENKINS_FOLDER = "Jenkins";
  String APP_DYNAMICS_FOLDER = "AppDynamics";
  String SPLUNK_FOLDER = "Splunk";
  String ELK_FOLDER = "ELK";
  String LOGZ_FOLDER = "LOGZ";

  String YAML_EXPRESSION = ".[^/]*?\\.yaml";
  String YAML_EXTENSION = ".yaml";
  String ARTIFACT_SERVER = "ARTIFACT_SERVER";
  String COLLABORATION_PROVIDER = "COLLABORATION_PROVIDER";
  String LOADBALANCER_PROVIDER = "LOADBALANCER_PROVIDER";
  String VERIFICATION_PROVIDER = "VERIFICATION_PROVIDER";
}

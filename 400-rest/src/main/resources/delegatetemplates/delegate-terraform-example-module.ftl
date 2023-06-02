module "delegate" {
  source = "harness/harness-delegate/kubernetes"
  version = "0.1.6"

  account_id = "${account_id}"
  delegate_token = "${token}"
  delegate_name = "terraform-delegate"
  namespace = "harness-delegate-ng"
  manager_endpoint = "${manager_url}"
  delegate_image = "${image}"
  replicas = 1
  upgrader_enabled = false
}

provider "helm" {
  kubernetes {
    config_path = "~/.kube/config"
  }
}
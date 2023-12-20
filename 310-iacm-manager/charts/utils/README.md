# K8S Dry Run Script

The script in this project is designed to provide a diff of the local infrastructure definitions with a target environment.

Note that this is not a perfect diff, as there are number of changes made by during actual releases, but the reports should give you good insight into the changes.

## How to use

### Required tools

Below is a list of the required tools needed to run the script:
* bash
* yq
* kubectl
* helm

### Set your target

The script will target whatever cluster your local `kubectl` is configured to use. Therefore ensure you are pointing at the correct cluster.
Below is a list of commands used to connect to common cluster targets:

    PreQA
        $ gcloud container clusters get-credentials qa-stress --region us-west1 --project qa-setup
    QA
        $ gcloud container clusters get-credentials qa-private --region us-west1 --project qa-setup
    UAT
        $ gcloud container clusters get-credentials uat-private --zone us-central1-a --project uat-setup-261723
    PROD Primary
        $ gcloud container clusters get-credentials prod-private-uswest1-primary --region us-west1 --project prod-setup-205416
    PROD Failover
        $ gcloud container clusters get-credentials prod-private-uswest2-failover --region us-west2 --project prod-setup-205416

### Running the script

The script takes 3 optional params
* env
    * See list of current envs [here](https://github.com/wings-software/ng-prod-manifests/tree/master/environments)
* namespace
    * Target namespace that you wish to preform the diff against
* cluster_purpose
    * Either the `primary` or `failover` cluster

You can then run the script with:

    $ ./k8s_dry_run.sh prod harness-nextgen primary

All FF env scripts:

    UAT
    $ gcloud container clusters get-credentials qa-private --region us-west1 --project qa-setup
    $ ./k8s_dry_run.sh uat harness-nextgen primary

    Prod
    $ gcloud container clusters get-credentials prod-private-uswest1-primary --region us-west1 --project prod-setup-205416
    $ ./k8s_dry_run.sh prod harness-nextgen primary
    $ ./k8s_dry_run.sh free harness-nextgen-free primary
    $ ./k8s_dry_run.sh compliance harness-nextgen-compliance primary

## Results

After you run the script, it will generate a report for each module in the FF service.
Reivew these reports to see where the drifts are from your local to the remote

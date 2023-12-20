#!/bin/bash

# namespace
ns_suffix="${1}"

build_manager_chart_yaml () {
    helm -n harness-nextgen template ../iacm-manager \
        -f ../iacm-manager/values.yaml \
        > iacm-manager-test.yaml
}

diff_current_manager () {
    module_id="iacm-manager"
    ns_prefix="harness-nextgen"
    source_file="${module_id}-test.yaml"

    cm_revision=$(kubectl get deployment -n ${ns_prefix}${ns_suffix} $module_id -o jsonpath="{.spec.template.spec.containers[0].envFrom[0].configMapRef.name}")
    secret_revision=$(kubectl get deployment -n ${ns_prefix}${ns_suffix} $module_id -o jsonpath="{.spec.template.spec.containers[0].envFrom[1].secretRef.name}")

    for di in $(yq '. | ({"doc": document_index, "match": .})| to_json | @base64' $source_file); do
        di_obj=$(printf $di | base64 -D)
        index=$(echo $di_obj | jq ".doc" - )
        if [[ "$(yq 'select(document_index == '$index').kind' $source_file)" == "Secret" ]] && [[ "$(yq 'select(document_index == '$index').metadata.name' $source_file)" == "iacm-manager" ]]; then
             yq -i 'select(document_index == '$index').metadata.name = "'$secret_revision'"' $source_file
        elif [[ "$(yq 'select(document_index == '$index').kind' $source_file)" == "ConfigMap" ]]; then
             yq -i 'select(document_index == '$index').metadata.name = "'$cm_revision'"' $source_file
        elif [[ "$(yq 'select(document_index == '$index').kind' $source_file)" == "Deployment" ]]; then
             yq -i 'select(document_index == '$index').spec.template.spec.containers[0].envFrom[0].configMapRef.name = "'$cm_revision'"' $source_file
             yq -i 'select(document_index == '$index').spec.template.spec.containers[0].envFrom[1].secretRef.name = "'$secret_revision'"' $source_file
        fi
    done

    kubectl diff  -n ${ns_prefix}${ns_suffix} -f ${module_id}-test.yaml > "${module_id}-${ns_prefix}${ns_suffix}-report.yaml"
    echo "generated report at: ${module_id}-${ns_prefix}${ns_suffix}-report.yaml"
}

k8s_dry_run () {
    build_manager_chart_yaml
    diff_current_manager
}

k8s_dry_run

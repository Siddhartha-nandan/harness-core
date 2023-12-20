# Copyright 2023 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

.PHONY: help
help:			## list available cmds.
	@sed -ne '/@sed/!s/## //p' $(MAKEFILE_LIST)

lint:
	helm lint ../iacm-manager

.PHONY: debug
debug:   		## generate yaml from iacm-manager helm chart
	helm -n test template ../iacm-manager -f ../iacm-manager/values.yaml --debug > test.yaml

.PHONY: build
build:  		## package iacm-manager helm chart dependency charts
	cd ../iacm-manager && helm dependency update && helm dependency build

.PHONY: diff
diff:  			## compare local charts with remote
	./k8s_dry_run.sh

.PHONY: install
install:		## install chart into target namespace using default values (eg 'make NS=markmcc install')
	helm upgrade --install -n $(NS) iacm-manager ../iacm-manager -f global-values.yaml


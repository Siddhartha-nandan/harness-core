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

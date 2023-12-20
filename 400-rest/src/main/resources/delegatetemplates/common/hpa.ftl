<#macro scale>
apiVersion: autoscaling/v1
kind: HorizontalPodAutoscaler
metadata:
   name: ${delegateName}-hpa
   namespace: ${delegateNamespace}
   labels:
       harness.io/name: ${delegateName}
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: ${delegateName}
  minReplicas: 1
  maxReplicas: 1
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 80
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
</#macro>
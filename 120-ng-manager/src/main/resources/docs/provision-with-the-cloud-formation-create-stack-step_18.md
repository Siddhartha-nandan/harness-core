## Option: Continue Based on Stack Statuses

In **Continue Based on Stack Statuses**, you can add the stack states that allow provisioning.

Harness checks if the stack is in `ROLLBACK_COMPLETE` state before the deployment. If present, Harness deletes the stack and then triggers the deployment.

# Infnispan and Kubeping test

## Usage

log into the openshift console and be in an openshift project called "fabric8"

Add a service account for Kube Ping

    oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)

Run `mvn fabric8:deploy` to generate docker image and deploy it on openshift, it deploys automatically 2 pods

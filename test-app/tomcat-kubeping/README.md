# Tomcat and KubePing test

## Usage

Log into the OpenShift console and switch to this app's project
(by default: `tomcat-kubeping`, can be customized thorugh `fabric8.namespace` in `pom.xml`).

Add a service account for KubePing:

```sh
oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)
```

Run `mvn fabric8:deploy` to generate the Docker image and deploy it on OpenShift.
By default, 2 pods are deployed. This can be changed in `src/main/fabric8/deployment.yml`.

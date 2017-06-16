# Tomcat Session Replication in OpenShift

This project was realized for the *R&D Workshop* at the University of Neuchâtel
during the spring semester 2017.

The goal was to research ways to extend Tomcat's session replication mechanism
to cloud deployments such as OpenShift and implement a proof of concept. Our
implementation can be found in this repository.

A simple application (`TestServlet.java`) was also created. It responds to
requests with a JSON object containing the IP and hostname of the responding
node, as well as a counter that is incremented on each request, allowing to
verify if session data is correctly replicated.


## Usage

### Prepare OpenShift

Create a new OpenShift project or switch to an existing one:

```sh
oc project tomcat-in-the-cloud
```

Authorize pods to see their peers:

```sh
oc policy add-role-to-user view system:serviceaccount:$(oc project -q):default -n $(oc project -q)
```

### Deploy your application

```sh 
mvn fabric8:deploy
```

## Customization

### Project name

By default, the application is deployed to the project `tomcat-in-the-cloud`.
To change this, modify the value of `fabric8.namespace` in `pom.xml`.

### Server port

Tomcat is set to listen to port 8080, and fabric8 must expose this port in the
resulting containers.  This value is specified in two places: in the
application itself (see `Main.java`), and in fabric8's configuration (variable
`port` in `pom.xml`)

### Number of replicas

Upon deployment, the application is automatically scaled to 2 pods. This value
can be changed in `src/main/fabric8/deployment.yml`. When the default behavior
is desired, this file can safely be deleted.

### Sticky sessions

To make testing easier, sticky sessions have been disabled in
`src/main/fabric8/route.yml`.  To re-enable them, change
`haproxy.router.openshift.io/disable_cookies` to `true` or simply delete the
file.

When making changes to the route configuration, run `mvn fabric8:undeploy`
before deploying again to make sure that all changes are taken into account.

## Limitations and possible improvements

- Error handling is at a bare minimum and should be improved.
- `CertificateStreamProvider` (see
  [JGroups-Kubernetes](https://github.com/jgroups-extras/jgroups-kubernetes/blob/master/src/main/java/org/jgroups/protocols/kubernetes/stream/CertificateStreamProvider.java))
  isn't currently provided, as it wasn't needed in our test deployments and we
  wanted to avoid the additional `net.oauth` dependency.
- Although the class is named `KubernetesMemberProvider`, it might not work in
  non-OpenShift Kubernetes, since the environment variables used to set up API
  calls might be different.
- The environment variable name prefix `OPENSHIFT_KUBE_PING_` was taken as-is
  from
  [JGroups-Kubernetes](https://github.com/jgroups-extras/jgroups-kubernetes/)
  and should probably be changed to something more meaningful. It seems that
  these variables are meant to be set manually only to override those provided
  by Kubernetes.
- Multiple applications deployed to the same namespace/project will form a
  single cluster and share session data together, which adds some overhead (due
  to more data being shared) and can pose security risks.  A solution to this
  could be possible with *labels*: `KubernetesMemberProvider` already filters
  pods by label if the environment variable `OPENSHIFT_KUBE_PING_LABELS` is set
  (can be set in `pom.xml`, similarly to `OPENSHIFT_KUBE_PING_NAMESPACE`), and
  labels can be added to your deployment (see [fabric8-maven-plugin
  documentation](https://maven.fabric8.io/#resource-labels-annotations)).


## Credits

Classes in the package `org.example.tomcat.cloud.stream` are taken from the
[JGroups-Kubernetes](https://github.com/jgroups-extras/jgroups-kubernetes/)
project. This project also served as inspiration for our implementation.

/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */

package consoleservice

import (
	"fmt"
	"github.com/enmasseproject/enmasse/pkg/apis/admin/v1beta1"
	"github.com/enmasseproject/enmasse/pkg/controller/authenticationservice"
	"github.com/enmasseproject/enmasse/pkg/util"
	"github.com/enmasseproject/enmasse/pkg/util/install"
	oauthv1 "github.com/openshift/api/oauth/v1"
	routev1 "github.com/openshift/api/route/v1"
	"golang.org/x/net/context"
	appsv1 "k8s.io/api/apps/v1"
	corev1 "k8s.io/api/core/v1"
	k8errors "k8s.io/apimachinery/pkg/api/errors"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/apimachinery/pkg/runtime"
	"k8s.io/apimachinery/pkg/types"
	"k8s.io/apimachinery/pkg/util/intstr"
	"sigs.k8s.io/controller-runtime/pkg/client"
	"sigs.k8s.io/controller-runtime/pkg/controller"
	"sigs.k8s.io/controller-runtime/pkg/controller/controllerutil"
	"sigs.k8s.io/controller-runtime/pkg/handler"
	"sigs.k8s.io/controller-runtime/pkg/manager"
	"sigs.k8s.io/controller-runtime/pkg/reconcile"
	logf "sigs.k8s.io/controller-runtime/pkg/runtime/log"
	"sigs.k8s.io/controller-runtime/pkg/source"
	"strings"
)


/*
TODO kubernetes path
TODO refactor CRD to organise fields for the openshift/kubernetes in a more cohesive way
TODO Handle the case of many ingress points (Openshift)
TODO TLS for the HTTPD side car
TODO Tidy up (minimise) Apache HTTPD conf
TODO Expose console endpoint to the user (as a dynamic field in the CRD?)
TODO Tidy up this code - extract utility methods
TODO unit tests - having prblem with client when interacting with openshift API endpoints?

 */
var log = logf.Log.WithName("controller_consoleservice")

// Gets called by parent "init", adding as to the manager
func Add(mgr manager.Manager) error {
	return add(mgr, newReconciler(mgr))
}

func newReconciler(mgr manager.Manager) *ReconcileConsoleService {
	return &ReconcileConsoleService{client: mgr.GetClient(), scheme: mgr.GetScheme(), namespace: util.GetEnvOrDefault("NAMESPACE", "enmasse-infra")}
}

func add(mgr manager.Manager, r *ReconcileConsoleService) error {

	// Create a new controller
	c, err := controller.New("consoleservice-controller", mgr, controller.Options{Reconciler: r})
	if err != nil {
		return err
	}

	// Watch for changes to primary resource ConsoleService
	err = c.Watch(&source.Kind{Type: &v1beta1.ConsoleService{}}, &handler.EnqueueRequestForObject{})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &appsv1.Deployment{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &v1beta1.ConsoleService{},
	})
	if err != nil {
		return err
	}

	err = c.Watch(&source.Kind{Type: &corev1.Service{}}, &handler.EnqueueRequestForOwner{
		IsController: true,
		OwnerType:    &v1beta1.ConsoleService{},
	})
	if err != nil {
		return err
	}

	// TODO We need to watch routes, so we can adjust the redirects of the oauthclient
	// TODO We need to watch oauthclient, so we can adjust the secret cached in the secret.

	return nil
}

var _ reconcile.Reconciler = &ReconcileConsoleService{}

type ReconcileConsoleService struct {
	// This client, initialized using mgr.Client() above, is a split client
	// that reads objects from the cache and writes to the apiserver
	client    client.Client
	scheme    *runtime.Scheme
	namespace string
}

// Reconcile by reading the console service spec and making required changes
//
// returning an error will get the request re-queued
func (r *ReconcileConsoleService) Reconcile(request reconcile.Request) (reconcile.Result, error) {
	reqLogger := log.WithValues("Request.Namespace", request.Namespace, "Request.Name", request.Name)
	reqLogger.Info("Reconciling ConsoleService")

	ctx := context.TODO()
	consoleservice := &v1beta1.ConsoleService{}
	err := r.client.Get(ctx, request.NamespacedName, consoleservice)
	if err != nil {
		if k8errors.IsNotFound(err) {
			reqLogger.Info("ConsoleService resource not found. Ignoring since object must be deleted")
			return reconcile.Result{}, nil
		}
		// Error reading the object - requeue the request
		reqLogger.Error(err, "Failed to get ConsoleService")
		return reconcile.Result{}, err
	}

	err = applyConsoleServiceDefaults(ctx, r.client, r.scheme, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}

	// If kubernetes, check for configmap, if no exist, requeue

	// service
	result, err := r.reconcileService(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue := result.Requeue

	// route
	result, err = r.reconcileRoute(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	// oauthclient
	result, err = r.reconcileOauthClient(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	// deployment

	result, err = r.reconcileDeployment(ctx, consoleservice)
	if err != nil {
		return reconcile.Result{}, err
	}
	requeue = requeue || result.Requeue

	return reconcile.Result{}, nil
}

func applyConsoleServiceDefaults(ctx context.Context, client client.Client, scheme *runtime.Scheme, consoleservice *v1beta1.ConsoleService) error {
	if consoleservice.Spec.DeploymentName == nil {
		consoleservice.Spec.DeploymentName = &consoleservice.Name
	}
	if consoleservice.Spec.ServiceName == nil {
		consoleservice.Spec.ServiceName = &consoleservice.Name
	}
	if consoleservice.Spec.RouteName == nil {
		consoleservice.Spec.RouteName = &consoleservice.Name
	}
	if consoleservice.Spec.ServiceAccountName == nil {
		serviceaccount := "consoleservice"
		consoleservice.Spec.ServiceAccountName = &serviceaccount
	}
	if consoleservice.Spec.CertificateSecret == nil {
		secretName := consoleservice.Name + "-cert"
		consoleservice.Spec.CertificateSecret = &corev1.SecretReference{
			Name: secretName,
		}

		if !util.IsOpenshift() {
			err := authenticationservice.CreateAuthserviceSecret(ctx, client, scheme, consoleservice.Namespace, secretName, consoleservice, func(secret *corev1.Secret) error {
				install.ApplyDefaultLabels(&secret.ObjectMeta, "consoleservice", secretName)

				cn := util.ServiceToCommonName(consoleservice.Namespace, *consoleservice.Spec.ServiceName)
				return util.GenerateSelfSignedCertSecret(cn, nil, nil, secret)
			})
			if err != nil {
				return err
			}
		}
	}

	if util.IsOpenshift() {
		if consoleservice.Spec.OauthClientName == nil  {
			consoleservice.Spec.OauthClientName = &consoleservice.Name
		}

		if consoleservice.Spec.Scope == nil {
			scope := "user:full"
			consoleservice.Spec.Scope = &scope
		}

		if (consoleservice.Spec.OauthClientSecret == nil) {
			secretName := consoleservice.Name + "-oauth"
			consoleservice.Spec.OauthClientSecret = &corev1.SecretReference{Name: secretName}
		}
	} else {
		if consoleservice.Spec.Scope == nil {
			scope := "openid"
			consoleservice.Spec.Scope = &scope
		}
	}

	return nil
}

func (r *ReconcileConsoleService) reconcileService(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	service := &corev1.Service{
		ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: *consoleservice.Spec.ServiceName},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, service, func(existing runtime.Object) error {
		existingService := existing.(*corev1.Service)

		if err := controllerutil.SetControllerReference(consoleservice, existingService, r.scheme); err != nil {
			return err
		}

		return applyService(consoleservice, existingService)
	})

	if err != nil {
		log.Error(err, "Failed reconciling Service")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

func applyService(consoleService *v1beta1.ConsoleService, service *corev1.Service) error {

	install.ApplyServiceDefaults(service, "consoleservice", *consoleService.Spec.ServiceName)
	service.Spec.Selector = install.CreateDefaultLabels(nil, "consoleservice", consoleService.Name)

	if service.Annotations == nil {
		service.Annotations = make(map[string]string)
	}
	service.Annotations["service.alpha.openshift.io/serving-cert-secret-name"] = consoleService.Spec.CertificateSecret.Name
	service.Spec.Ports = []corev1.ServicePort{
		{
			Port:       8443,
			Protocol:   corev1.ProtocolTCP,
			TargetPort: intstr.FromString("https"),
			Name:       "https",
		},
	}
	return nil
}


func (r *ReconcileConsoleService) reconcileRoute(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	if util.IsOpenshift() {
		route := &routev1.Route{
			ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: *consoleservice.Spec.RouteName},
		}
		_, err := controllerutil.CreateOrUpdate(ctx, r.client, route, func(existing runtime.Object) error {
			existingRoute := existing.(*routev1.Route)

			secretName := types.NamespacedName{
				Name:      consoleservice.Spec.CertificateSecret.Name,
				Namespace: consoleservice.Namespace,
			}
			certsecret := &corev1.Secret{}
			err := r.client.Get(ctx, secretName, certsecret)
			if err != nil {
				return err
			}
			cert := certsecret.Data["tls.crt"]
			if err := controllerutil.SetControllerReference(consoleservice, existingRoute, r.scheme); err != nil {
				return err
			}
			return applyRoute(consoleservice, existingRoute, string(cert[:]))
		})

		if err != nil {
			log.Error(err, "Failed reconciling Route")
			return reconcile.Result{}, err
		}
	}
	return reconcile.Result{}, nil
}

func applyRoute(consoleservice *v1beta1.ConsoleService, route *routev1.Route, caCertificate string) error {

	install.ApplyDefaultLabels(&route.ObjectMeta, "consoleservice", consoleservice.Name)

	route.Spec = routev1.RouteSpec{
		To: routev1.RouteTargetReference{
			Kind: "Service",
			Name: consoleservice.Name,
		},
		TLS: &routev1.TLSConfig{
			Termination:   routev1.TLSTerminationReencrypt,
			CACertificate: caCertificate,
		},
		Port: &routev1.RoutePort{
			TargetPort: intstr.FromString("https"),
		},
	}
	return nil
}

func (r *ReconcileConsoleService) reconcileDeployment(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	deployment := &appsv1.Deployment{
		ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: *consoleservice.Spec.DeploymentName},
	}
	_, err := controllerutil.CreateOrUpdate(ctx, r.client, deployment, func(existing runtime.Object) error {
		existingDeployment := existing.(*appsv1.Deployment)

		if err := controllerutil.SetControllerReference(consoleservice, existingDeployment, r.scheme); err != nil {
			return err
		}

		return applyDeployment(consoleservice, existingDeployment)
	})

	if err != nil {
		log.Error(err, "Failed reconciling Deployment")
		return reconcile.Result{}, err
	}
	return reconcile.Result{}, nil
}

func applyDeployment(consoleservice *v1beta1.ConsoleService, deployment *appsv1.Deployment) error {

	install.ApplyDeploymentDefaults(deployment, "consoleservice", *consoleservice.Spec.DeploymentName)

	install.ApplyEmptyDirVolume(deployment, "apps")
	install.ApplySecretVolume(deployment, "console-tls", consoleservice.Spec.CertificateSecret.Name)

	install.ApplyInitContainer(deployment, "console-init", func(container *corev1.Container) {
		install.ApplyContainerImage(container, "console-init", nil)

		if consoleservice.Spec.Scope != nil {
			install.ApplyEnv(container, "OAUTH2_SCOPE", func(envvar *corev1.EnvVar) {
				envvar.Value = *consoleservice.Spec.Scope;
			})
		}

		if util.IsOpenshift() && consoleservice.Spec.ServiceAccountName != nil {
			install.ApplyEnv(container, "OPENSHIFT_SERVICE_ACCOUNT", func(envvar *corev1.EnvVar) {
				envvar.Value = *consoleservice.Spec.ServiceAccountName;
			})
		}

		if consoleservice.Spec.DiscoveryMetadataURL != nil {
			install.ApplyEnv(container, "DISCOVERY_METADATA_URL", func(envvar *corev1.EnvVar) {
				envvar.Value = *consoleservice.Spec.DiscoveryMetadataURL;
			})
		}

		container.Command = []string{"/oauth-proxy/bin/init.sh", "/apps/"}
		install.ApplyVolumeMountSimple(container, "apps", "/apps", false);
	})


	if util.IsOpenshift() {
		install.ApplyContainer(deployment, "console-proxy", func(container *corev1.Container) {
			install.ApplyContainerImage(container, "console-proxy-openshift", nil)

			container.Args = []string{"-config=/apps/cfg/oauth-proxy-openshift.cfg"}

			install.ApplyVolumeMountSimple(container, "apps", "/apps", false);
			install.ApplyVolumeMountSimple(container, "console-tls", "/etc/tls/private", true);

			if consoleservice.Spec.OauthClientSecret != nil {
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_ID", "client-id", consoleservice.Spec.OauthClientSecret.Name)
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_SECRET", "client-secret", consoleservice.Spec.OauthClientSecret.Name)
			}

			container.Ports = []corev1.ContainerPort{{
				ContainerPort: 8443,
				Name:          "https",
			}}

			container.ReadinessProbe = &corev1.Probe{
				InitialDelaySeconds: 60,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
			container.LivenessProbe = &corev1.Probe{
				InitialDelaySeconds: 120,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
		})

		install.ApplyContainer(deployment, "console-httpd", func(container *corev1.Container) {
			install.ApplyContainerImage(container, "console-httpd", nil)

			container.Ports = []corev1.ContainerPort{{
				ContainerPort: 8080,
				Name:          "http",
			}}

			// Can't define a probe as HTTPD bound to loopback.  Perhaps have oauth-proxy's probes reach through
			// HTTP and hit whoami?

		})
	} else {
		install.ApplyContainer(deployment, "console-proxy", func(container *corev1.Container) {
			install.ApplyContainerImage(container, "console-proxy-kubernetes", nil)

			container.Args = []string{"-config=/apps/cfg/oauth-proxy-console-proxy-kubernetes.cfg"}

			install.ApplyVolumeMountSimple(container, "apps", "/apps", false);
			install.ApplyVolumeMountSimple(container, "console-tls", "/etc/tls/private", true);

			if consoleservice.Spec.OauthClientSecret != nil {
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_ID", "client-id", consoleservice.Spec.OauthClientSecret.Name)
				install.ApplyEnvSecret(container, "OAUTH2_PROXY_CLIENT_SECRET", "client-secret", consoleservice.Spec.OauthClientSecret.Name)
			}

			if consoleservice.Spec.Scope != nil {
				install.ApplyEnv(container, "SSL_CERT_DIR", func(envvar *corev1.EnvVar) {
					envvar.Value = "/var/run/secrets/kubernetes.io/serviceaccount/";
				})
			}

			container.Ports = []corev1.ContainerPort{{
				ContainerPort: 8443,
				Name:          "https",
			}}

			container.ReadinessProbe = &corev1.Probe{
				InitialDelaySeconds: 60,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
			container.LivenessProbe = &corev1.Probe{
				InitialDelaySeconds: 120,
				Handler: corev1.Handler{
					HTTPGet: &corev1.HTTPGetAction{
						Port:   intstr.FromString("https"),
						Path:   "/oauth/healthz",
						Scheme: "HTTPS",
					},
				},
			}
		})
	}

	deployment.Spec.Template.Spec.ServiceAccountName = *consoleservice.Spec.ServiceAccountName

	deployment.Spec.Strategy = appsv1.DeploymentStrategy{
		Type: appsv1.RecreateDeploymentStrategyType,
	}
	return nil
}

func (r *ReconcileConsoleService) reconcileOauthClient(ctx context.Context, consoleservice *v1beta1.ConsoleService) (reconcile.Result, error) {
	if util.IsOpenshift() {

		key := client.ObjectKey{Namespace: consoleservice.Namespace, Name: *consoleservice.Spec.RouteName}
		route := &routev1.Route{}
		err := r.client.Get(ctx, key, route)
		if err != nil {
			return reconcile.Result{}, err
		}

		var openshiftUrl string
		openshiftUrl, err = util.OpenshiftUri()
		if err != nil {
			return reconcile.Result{}, err
		}
		if openshiftUrl == "" || strings.Contains(openshiftUrl, "https://localhost:8443") || strings.Contains(openshiftUrl, "https://127.0.0.1:8443") {
			openshiftUrl = fmt.Sprintf("https://%s:%s", util.GetEnvOrDefault("KUBERNETES_SERVICE_HOST", "172.30.0.1"), util.GetEnvOrDefault("KUBERNETES_SERVICE_PORT", "443"))
		}

		if len(route.Status.Ingress) == 0 {
			return reconcile.Result{}, nil
		}

		redirects := make([]string, len(route.Status.Ingress))

		scheme := "http"
		if route.Spec.TLS != nil {
			scheme = "https"
		}
		for i, element := range route.Status.Ingress {
			redirects[i] = fmt.Sprintf("%s://%s", scheme, element.Host )
		}

		var oauthname = *consoleservice.Spec.OauthClientName
		oauth := &oauthv1.OAuthClient{
			ObjectMeta: metav1.ObjectMeta{Name: oauthname},

		}

		_, err = controllerutil.CreateOrUpdate(ctx, r.client, oauth, func(existing runtime.Object) error {
			existingOauth := existing.(*oauthv1.OAuthClient)

			applyOauthClient(consoleservice, existingOauth, redirects)

			//if err := controllerutil.SetControllerReference(consoleservice, existingOauth, r.scheme); err != nil {
			//	return err
			//}

			return nil
		})

		if err != nil {
			log.Error(err, "Failed reconciling OAuth")
			return reconcile.Result{}, err
		}

		secretref := consoleservice.Spec.OauthClientSecret

		secret := &corev1.Secret{
			ObjectMeta: metav1.ObjectMeta{Namespace: consoleservice.Namespace, Name: secretref.Name},
		}
		_, err = controllerutil.CreateOrUpdate(ctx, r.client, secret, func(existing runtime.Object) error {
			existingSecret := existing.(*corev1.Secret)

			if err := controllerutil.SetControllerReference(consoleservice, existingSecret, r.scheme); err != nil {
				return err
			}

			return applyOauthSecret(consoleservice, existingSecret, oauth)
		})

		if err != nil {
			log.Error(err, "Failed reconciling OAuth Secret")
			return reconcile.Result{}, err
		}
		return reconcile.Result{}, nil


		//if consoleservice.Annotations == nil {
		//	consoleservice.Annotations = make(map[string]string)
		//}
		//
		//if redirectUrl != "" {
		//	consoleservice.Annotations["enmasse.io/oauth-url"] = redirectUrl
		//}
		//
		//if openshiftUrl != "" {
		//	consoleservice.Annotations["enmasse.io/identity-provider-url"] = openshiftUrl
		//	consoleservice.Annotations["enmasse.io/identity-provider-client-id"] = oauth.Name
		//	consoleservice.Annotations["enmasse.io/identity-provider-client-secret"] = oauth.Secret
		//}
	}
	return reconcile.Result{}, nil
}


func applyOauthClient(consoleservice *v1beta1.ConsoleService, oauth *oauthv1.OAuthClient, redirects []string) error {
	install.ApplyDefaultLabels(&oauth.ObjectMeta, "oauthclient", oauth.Name)
	if oauth.Secret == "" {
		password, err := util.GeneratePassword(32)
		if err != nil {
			return err
		}
		oauth.Secret = password
	}
	oauth.GrantMethod = oauthv1.GrantHandlerAuto
	oauth.RedirectURIs = redirects;
	return nil
}

func applyOauthSecret(consoleService *v1beta1.ConsoleService, secret *corev1.Secret, oauth *oauthv1.OAuthClient) error {

	secret.StringData = make(map[string]string)
	secret.StringData["client-id"] = oauth.Name
	secret.StringData["client-secret"] = oauth.Secret;

	return nil
}


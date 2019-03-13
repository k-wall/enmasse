/*
 * Copyright 2019, EnMasse authors.
 * License: Apache License 2.0 (see the file LICENSE or http://apache.org/licenses/LICENSE-2.0.html).
 */
package main

import (
	"crypto/tls"
	"github.com/gin-gonic/contrib/static"
	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/json"
	"k8s.io/client-go/rest"
	"log"
	"net/http"
	"os"
)

var version = "1.0.0"
var clientId = ""
var discoveryUri = ""
var config *rest.Config


func main() {

	log.Printf("KWDEBUG starting")

	discoveryUri = os.Getenv("OPENID_CONNECT_DISCOVERY_URI");
	if discoveryUri == "" {
		discoveryUri = "https://openshift.default.svc/.well-known/oauth-authorization-server"
	}

	clientId = os.Getenv("OAUTH_CLIENT_ID")
	if clientId == "" {
		log.Panic("OAUTH_CLIENT_ID not specified in process's environment")
	}

	config = getConfig()

	router := gin.Default()

	v1 := router.Group("/api/v1/console-discovery")
	v1.GET("/", consoleDisoveryEndpointResponse)

	router.Use(
		static.Serve(
			"/",
			static.LocalFile("./ui/dist", true),
		),
	)

	err := router.Run(":8080")
	if err != nil {
		log.Panicf("Error starting server: %v", err)
	}
}

func consoleDisoveryEndpointResponse(c *gin.Context) {
	// TODO Need client trust
	// Could cache and reuse
	http.DefaultTransport.(*http.Transport).TLSClientConfig = &tls.Config{InsecureSkipVerify: true}
	resp, err := http.Get(discoveryUri)
	if err != nil {
		log.Fatalf("Failed to GET %s :  %v", discoveryUri, err)
		c.JSON(http.StatusInternalServerError, gin.H{"status": http.StatusInternalServerError})
		return
	}
	defer resp.Body.Close()


	var openidConnectDiscovery10 map[string]interface{}

	err = json.NewDecoder(resp.Body).Decode(&openidConnectDiscovery10)
	if err != nil {
		log.Fatalf("Error unmarshalling response from %s : %v", discoveryUri, err)
		c.JSON(http.StatusInternalServerError, gin.H{"status": http.StatusInternalServerError})
		return
	}

	response := gin.H{"openid-connect-discovery-1_0": openidConnectDiscovery10,
		              "client_id": clientId,
					   "api-uri": config.Host}
	// TODO need to expose cert to client side too??
	c.JSON(http.StatusOK, response)
}

func getConfig() (*rest.Config) {
	config, e := rest.InClusterConfig()
	if e != nil {
		log.Panic("Failed to retreive cluster config", e);
	}
	// TODO try local .kube/config to facilitate testing outwith container

	return config
}

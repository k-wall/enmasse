package main

import (
	"github.com/gin-gonic/contrib/static"
	"github.com/gin-gonic/gin"
	"log"
)

var version = "1.0.0"

func main() {

	router := gin.Default()

	router.Use(
		static.Serve(
			"/",
			static.LocalFile("./ui/dist", true),
		),
	)

	err := router.Run(":8080")
	if err != nil {
		log.Fatalf("Error running router: %v", err)
	}
}
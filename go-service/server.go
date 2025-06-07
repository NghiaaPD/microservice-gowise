package main

import (
	"bytes"
	"log"
	"net"
	"net/http"
	"os"
	"text/template"
	"time"

	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	"github.com/prometheus/client_golang/prometheus/promhttp"
)

var instanceID string

func registerWithEureka() {
	hostName, _ := os.Hostname()
	instanceID = hostName + ":" + os.Getenv("SERVICE_NAME") + ":" + os.Getenv("SERVICE_PORT")
	eurekaURL := os.Getenv("EUREKA_SERVER") + "apps/" + os.Getenv("SERVICE_NAME")

	xmlTemplate := `
		<instance>
		<hostName>{{.HostName}}</hostName>
		<app>{{.ServiceName}}</app>
		<ipAddr>{{.HostName}}</ipAddr>
		<vipAddress>{{.ServiceName}}</vipAddress>
		<status>UP</status>
		<port enabled="true">{{.ServicePort}}</port>
		<instanceId>{{.InstanceID}}</instanceId>
		<dataCenterInfo class="com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo">
			<name>MyOwn</name>
		</dataCenterInfo>
		</instance>
	`
	data := struct {
		HostName    string
		ServiceName string
		ServicePort string
		InstanceID  string
	}{
		HostName:    hostName, // Dùng hostname cho cả hostName và ipAddr
		ServiceName: os.Getenv("SERVICE_NAME"),
		ServicePort: os.Getenv("SERVICE_PORT"),
		InstanceID:  instanceID,
	}

	var body bytes.Buffer
	tmpl, _ := template.New("eureka").Parse(xmlTemplate)
	tmpl.Execute(&body, data)

	for {
		req, _ := http.NewRequest("POST", eurekaURL, &body)
		req.Header.Set("Content-Type", "application/xml")
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			log.Println("Eureka registration error:", err)
			time.Sleep(5 * time.Second)
			continue
		}
		log.Println("Eureka registration status:", resp.Status)
		resp.Body.Close()
		break
	}
}

func renewWithEureka() {
	eurekaRenewURL := os.Getenv("EUREKA_SERVER") + "apps/" + os.Getenv("SERVICE_NAME") + "/" + instanceID
	for {
		req, _ := http.NewRequest("PUT", eurekaRenewURL, nil)
		resp, err := http.DefaultClient.Do(req)
		if err != nil {
			log.Println("Eureka renew error:", err)
		} else {
			log.Println("Eureka renew status:", resp.Status)
			resp.Body.Close()
		}
		time.Sleep(30 * time.Second)
	}
}

func getLocalIP() string {
	addrs, err := net.InterfaceAddrs()
	if err != nil {
		log.Fatal(err)
	}

	for _, address := range addrs {
		if ipNet, ok := address.(*net.IPNet); ok && !ipNet.IP.IsLoopback() && ipNet.IP.To4() != nil {
			return ipNet.IP.String()
		}
	}

	return ""
}

func main() {
	err := godotenv.Load()
	if err != nil {
		log.Println("No .env file found")
	}

	r := gin.Default()
	r.GET("/v1/ping", func(c *gin.Context) {
		c.JSON(http.StatusOK, gin.H{"message": "hello"})
	})
	r.GET("/metrics", gin.WrapH(promhttp.Handler()))
	r.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "UP"})
	})
	go registerWithEureka()
	go renewWithEureka()
	r.Run(":" + os.Getenv("SERVICE_PORT"))
}

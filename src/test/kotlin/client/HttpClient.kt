package client

import jdk.incubator.http.HttpClient
import jdk.incubator.http.HttpRequest
import jdk.incubator.http.HttpResponse
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.ProxySelector
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths


val LOCAL_SERVER_URI = "https://localhost:80/"

fun syncHttpClient() {
    val client = HttpClient.newHttpClient()

    val request = HttpRequest.newBuilder()
            .uri(URI(LOCAL_SERVER_URI))
            .build()

    val response = client.send(request, HttpResponse.BodyHandler.asString())

    println(response.statusCode())
    println(response.body())
}

fun syncHttpClientToFile() {
    val client = HttpClient.newHttpClient()

    val request = HttpRequest.newBuilder()
            .uri(URI(LOCAL_SERVER_URI))
            .build()

    val tempFile = Files.createTempFile("consol-labs-home", ".html")
    val response = client.send(request, HttpResponse.BodyHandler.asFile(tempFile))
    println(response.statusCode())
    println(response.body())
}

fun syncHttpClientPostFile() {
    val client = HttpClient.newHttpClient()

    val request = HttpRequest.newBuilder()
            .uri(URI(LOCAL_SERVER_URI + "upload/"))
            .POST(HttpRequest.BodyProcessor.fromFile(Paths.get("/tmp/file-to-upload.txt")))
            .build()

    val response = client.send(request, HttpResponse.BodyHandler.discard<String>(null))
    println(response.statusCode())
}

fun asyncHttpClient() {
    val client = HttpClient.newHttpClient()

    val request = HttpRequest.newBuilder()
            .uri(URI(LOCAL_SERVER_URI))
            .GET()
            .build()

    val response = client.sendAsync(request, HttpResponse.BodyHandler.asString())

    Thread.sleep(5000)
    if (response.isDone()) {
        println(response.get().statusCode())
        println(response.get().body())
    } else {
        response.cancel(true)
        println("Request took more than 5 seconds... cancelling.")
    }
}

fun syncHttpClientWithProxy() {
    val client = HttpClient.newBuilder()
            .proxy(ProxySelector.getDefault())
            .build()

    val request = HttpRequest.newBuilder()
            .uri(URI(LOCAL_SERVER_URI))
            .build()

    val response = client.send(request, HttpResponse.BodyHandler.asString())

    println(response.statusCode())
    println(response.body())
}

fun syncHttpClientWithBasicAuth() {
    val client = HttpClient.newBuilder()
            .authenticator(object : Authenticator() {
                override fun getPasswordAuthentication() =
                        PasswordAuthentication("username", "password".toCharArray())
            })
            .build()

    val request = HttpRequest.newBuilder()
            .uri(URI(LOCAL_SERVER_URI))
            .build()

    val response = client.send(request, HttpResponse.BodyHandler.asString())

    println(response.statusCode())
    println(response.body())
}
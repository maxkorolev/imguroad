# imguroad

Imgur image uploading service that exposes a REST API.

Clients of this service:

1. Submit image upload jobs
2. Each upload job is an array of image URLs. The service takes each URL,
downloads the content, and uploads it to Imgur.

## Usage dev

```scala
sbt
> ~reStart
```

## Usage docker

```scala
sbt
> docker:publishLocal
> exit
docker run --rm -p 8080:80 -e IMGUR_BEARER_TOKEN=MY_ACCESS_TOKEN imguroad:0.0.1-SNAPSHOT
```

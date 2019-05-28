# imguroad

Imgur image uploading service that exposes a REST API.

Clients of this service:
1. Submit image upload jobs
2. Each upload job is an array of image URLs. The service takes each URL,
downloads the content, and uploads it to Imgur.

Requirements
* The service needs to download the
image at the given URL and re-upload to Imgur.
* OAuth client ID and secret can be embedded into the service config or injected at
runtime.
* Only in-memory data structures.
* POST /v1/images/upload
  * Should return immediately. The uploading happens asynchronously.
  * Should log the reason for failed image uploads.

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
docker run --rm -p 80:8080 -e IMGUR_BEARER_TOKEN=MY_ACCESS_TOKEN imguroad:0.0.1-SNAPSHOT
```

```bash
curl -X POST \
  http://localhost/v1/images/upload \
  -H 'content-type: application/json' \
  -d '{ "urls": [
              "https://farm3.staticflickr.com/2879/11234651086_681b3c2c00_b_d.jpg",
              "https://farm4.staticflickr.com/3790/11244125445_3c2f32cd83_k_d.jpg"
              ]}'
```

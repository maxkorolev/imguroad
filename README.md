# imguroad

Imgur image uploading service that exposes a REST API.

Clients of this service:

1. Submit image upload jobs
2. Each upload job is an array of image URLs. The service takes each URL,
downloads the content, and uploads it to Imgur.

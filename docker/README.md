# Docker-compose scenarios

Available scenarios:

- nginx+java: 3 java images (use an app.jar with embedded Tomcat) behind an nginx reverse proxy
- nginx+java+redis: same as the previous one but with an additional Redis server, meant to be use with the spring session test app

## Usage

Copy your .jar to `server/app.jar` and run `docker-compose up`. Your app will
be available on `localhost:8080`

# Docker-compose scenarios

Available scenarios:

- nginx+java: 3 java images (use an app.jar with embedded Tomcat) behind an nginx reverse proxy

## Usage

Copy your .jar to `server/app.jar` and run `docker-compose up`. Your app will
be available on `localhost:8080`

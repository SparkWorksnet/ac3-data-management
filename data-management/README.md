# Run the broker

Run the following only once:

````shell
docker network create ac3
````

Run the docker stack every time:

````shell
docker compose -f docker-compose.yml up --build
````

## Accessing the broker

Access the broker using the following url link [http://localhost:15672](http://localhost:15672/#/queues)


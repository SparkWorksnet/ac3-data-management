# Run the broker

````shell
docker network create ac3
docker compose -f docker-compose.yml up --build
````

## Accessing the broker
Access the broker using the following url link [http://localhost:15672](http://localhost:15672/#/queues)


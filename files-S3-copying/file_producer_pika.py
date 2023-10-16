import pika
import json

# RabbitMQ setup
credentials = pika.PlainCredentials("user", "password")
connection_parameters = pika.ConnectionParameters(
    host='127.0.0.1', port=5672, virtual_host="/", credentials=credentials, heartbeat=30)
connection = pika.BlockingConnection(connection_parameters)
channel = connection.channel()

# Message
message_data = {
    'source_bucket': 'ac3-filestore-1',
    'source_file': 'final_cube_LR-V.fits',
    'destination_bucket': 'ac3-filestore-2',
    'destination_file': 'cube.fits'
}
message_body = json.dumps(message_data)

routing_key = '#'
channel.basic_publish(exchange='uc3-data', routing_key=routing_key, body=message_body)

print(" [x] Message data sent: " + message_body)

connection.close()

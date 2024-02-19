import pika
import json
import boto3
import sys
import os


def main():
    # RabbitMQ setup
    credentials = pika.PlainCredentials(os.getenv('BROKER_USERNAME'), os.getenv('BROKER_PASSWORD'))
    connection_parameters = pika.ConnectionParameters(host=os.getenv('BROKER_HOST'), port=int(os.getenv('BROKER_PORT')),
                                                      virtual_host=os.getenv('BROKER_VHOST', '/'),
                                                      credentials=credentials)
    connection = pika.BlockingConnection(connection_parameters)
    channel = connection.channel()
    queue_name = os.getenv('BROKER_QUEUE')
    channel.basic_consume(queue=queue_name, on_message_callback=callback, auto_ack=True)
    print(' [*] Waiting for messages.')
    channel.start_consuming()


def callback(ch, method, properties, body):
    # Get message
    message_data = json.loads(body.decode('utf-8'))
    source_bucket = message_data['source_bucket']
    source_file = message_data['source_file']
    destination_bucket = message_data['destination_bucket']
    destination_file = message_data['destination_file']

    try:
        # Copy the file from source bucket to destination bucket
        s3 = boto3.client('s3')
        s3.copy_object(
            CopySource={'Bucket': source_bucket, 'Key': source_file},
            Bucket=destination_bucket,
            Key=destination_file
        )
        print(f" [x] Copied {source_bucket}/{source_file} to {destination_bucket}/{destination_file}")
    except Exception as e:
        print(f" [x] Error copying file: {e}")


if __name__ == '__main__':
    try:
        main()
        print('started main!')
    except KeyboardInterrupt:
        print('Interrupted')
        try:
            sys.exit(0)
        except SystemExit:
            os._exit(0)

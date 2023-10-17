# Run the file manager

Run the `file_manager.py` file.

The program starts a connection to the edge/cloud broker and waits for commands on what files are needed for processing
by the Data Management PaaS.
On each message the file is copied from the source bucket to the destination bucket as needed.

## Command Message

````json
{
  "source_bucket": "bucket_a",
  "source_file": "some/path/to/file/somefile.fits",
  "destination_bucket": "bucket_b",
  "destination_file": "some/new/path/to/file/filename.fits"
}
````

## Send a test request

To send a test request run the `test_file_request.py` file.

The program opens a connection to the rabbitmq edge/cloud broker and sends a message to the message exchange requesting
a file to be transfered from one bucket to the other.

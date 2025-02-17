import consumer_helper as consumer

asset_name = "files-asset-2"
files_path = "/data"

PROVIDER_DOMAIN = "http://192.168.1.215"
CONSUMER_DOMAIN = "http://192.168.1.215"

consumer_helper = consumer.ConsumerHelper(consumer_base_url=CONSUMER_DOMAIN, provider_base_url=PROVIDER_DOMAIN)

# DATA TRANSFER

contract_agreement_id = "80f43552-0427-4aa2-8397-840ed1ff7b01"

response = consumer_helper.start_transfer(contract_agreement_id,
                                          asset_name=asset_name,
                                          destination_url="forwarder.ac303:0BlYMKMIcH@ionos-s1.sparkworks.net:5672",
                                          provider_id="provider")
print(f"Transfer started with id {response['@id']}")

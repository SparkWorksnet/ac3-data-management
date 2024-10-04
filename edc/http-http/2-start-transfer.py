import time

import consumer_helper as consumer

asset_name = "files-asset"
files_path = "/data"

PROVIDER_DOMAIN = "http://192.168.1.10"
CONSUMER_DOMAIN = "http://192.168.1.10"

consumer_helper = consumer.ConsumerHelper(consumer_base_url=CONSUMER_DOMAIN, provider_base_url=PROVIDER_DOMAIN)

# DATA TRANSFER

contract_agreement_id = "389469c9-dcfb-4e8b-a026-06114c3bed2a"

response = consumer_helper.start_transfer(contract_agreement_id,
                                          asset_name=asset_name,
                                          destination_url="http://192.168.1.10:4000",
                                          provider_id="provider")
print(f"Transfer started with id {response['@id']}")

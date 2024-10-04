import time

import consumer_helper as consumer

asset_name = "files-asset-2"

PROVIDER_DOMAIN = "http://192.168.1.10"
CONSUMER_DOMAIN = "http://192.168.1.10"

consumer_helper = consumer.ConsumerHelper(consumer_base_url=CONSUMER_DOMAIN, provider_base_url=PROVIDER_DOMAIN)

# CONTRACT NEGOTIATION

response = consumer_helper.request_asset(asset_name)
print(f"response: {response}")
policy_id = response["odrl:hasPolicy"]["@id"]
print(f"policy_id: {policy_id}")

time.sleep(5)

response = consumer_helper.negotiate_contract(policy_id, asset_name, provider_id="provider")
print(f"response: {response}")
negotiation_id = response["@id"]
print(f"negotiation_id: {negotiation_id}")

contract_negotiation_status = consumer_helper.get_negotiation_status(negotiation_id)
while contract_negotiation_status.get("state", "") != "FINALIZED":
    contract_negotiation_status = consumer_helper.get_negotiation_status(negotiation_id)
    print("waiting for contract negotiation to complete...")
    time.sleep(1)

contract_agreement_id = contract_negotiation_status["contractAgreementId"]
print(f"contract_agreement_id: {contract_agreement_id}")

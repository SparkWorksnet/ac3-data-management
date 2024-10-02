import time

import provider_helper as provider
import consumer_helper as consumer

asset_name = "files-asset-5"
files_path = "myassets\\"

# ASSET CREATION

a = provider.create_asset(asset_name, files_path)
print(f"response: {a}")

a = provider.define_policy()
print(f"response: {a}")

a = provider.create_contract_definitions()
print(f"response: {a}")

# CONTRACT NEGOTIATION

policy_id = consumer.request_asset(asset_name, provider_url=provider.PROVIDER_URL_2)["odrl:hasPolicy"]["@id"]
print(f"policy_id: {policy_id}")

negotiation_id = consumer.negotiate_contract(policy_id, asset_name, "provider")["@id"]
print(f"negotiation_id: {negotiation_id}")

contract_negotiation_status = consumer.get_negotiation_status(negotiation_id)
while contract_negotiation_status.get("state", "") != "FINALIZED":
    contract_negotiation_status = consumer.get_negotiation_status(negotiation_id)
    print("waiting for contract negotiation to complete...")
    time.sleep(1)

contract_agreement_id = contract_negotiation_status["contractAgreementId"]
print(f"contract_agreement_id: {contract_agreement_id}")

# DATA TRANSFER

response = consumer.start_transfer(contract_agreement_id,
                                   asset_name=asset_name,
                                   destination_url="http://localhost:4000",
                                   provider_url=provider.PROVIDER_URL_2,
                                   provider_id="provider")
print(f"Transfer started with id {response['@id']}")

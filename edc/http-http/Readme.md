## What we need to know

+ provider name
+ provider url
+ asset name

## Preparing the Provider

### Launching the provider

````shell
export EDC_FS_CONFIG=provider.properties
java -jar connector.jar
````

### Creating an asset

````shell
asset_name = "my-asset"
files_path = "myassetdir"

a = provider.create_asset(asset_name, files_path)
print(f"response: {a}")

a = provider.define_policy()
print(f"response: {a}")

a = provider.create_contract_definitions()
print(f"response: {a}")
````

## Preparing the Consumer

### Launching the Consumer

````shell
export EDC_FS_CONFIG=consumer.properties
java -jar connector.jar
````

### Contract Negotiation

````shell
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
````

### Launching the logger

````shell
export HTTP_SERVER_PORT=4000
java -jar http-request-logger.jar
````

### Initiating the Transfer

````shell
response = consumer.start_transfer(contract_agreement_id,
                                   asset_name=asset_name,
                                   destination_url="http://localhost:4000",
                                   provider_url=provider.PROVIDER_URL_2,
                                   provider_id="provider")
print(f"Transfer started with id {response['@id']}")

````

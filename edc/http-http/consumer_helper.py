import json
import logging
from requests import post, get

CONSUMER_DOMAIN = "localhost"
CONSUMER_URL = f"http://{CONSUMER_DOMAIN}:28181"

headers = {'Content-Type': 'application/json'}


# curl -H 'Content-Type: application/json' -d @get-dataset.json -X POST "http://localhost:28181/management/v3/catalog/dataset/request" -s | jq
def request_asset(asset_name, provider_url):
    response = post(f"{CONSUMER_URL}/management/v3/catalog/dataset/request",
                    headers=headers,
                    data=json.dumps({
                        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                        "@type": "DatasetRequest",
                        "@id": asset_name,
                        "counterPartyAddress": f"{provider_url}/protocol",
                        "protocol": "dataspace-protocol-http"
                    }))
    logging.debug(response)
    logging.debug(response.text)
    return response.json()


# curl -H 'Content-Type: application/json' -d @my-negotiate-contract.json  -X POST "http://localhost:28181/management/v3/contractnegotiations" -s | jq
def negotiate_contract(policy_id, asset_name, provider_id="provider"):
    response = post(f"{CONSUMER_URL}/management/v3/contractnegotiations",
                    headers=headers,
                    data=json.dumps({
                        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                                     "odrl": "http://www.w3.org/ns/odrl/2/"},
                        "@type": "ContractRequest",
                        "counterPartyAddress": "http://localhost:18182/protocol",
                        "providerId": provider_id,
                        "protocol": "dataspace-protocol-http",
                        "policy": {
                            "@id": policy_id,
                            "@type": "http://www.w3.org/ns/odrl/2/Offer",
                            "odrl:permission": [],
                            "odrl:prohibition": [],
                            "odrl:obligation": [],
                            "odrl:target": {
                                "@id": asset_name
                            },
                            "odrl:assigner": {
                                "@id": provider_id
                            }
                        }
                    }))
    logging.debug(response)
    logging.debug(response.text)
    return response.json()


# curl "http://localhost:28181/management/v3/contractnegotiations/b531bd46-3b60-48b9-8df2-22c79b999dd7" -s
def get_negotiation_status(negotiation_id):
    response = get(f"{CONSUMER_URL}/management/v3/contractnegotiations/{negotiation_id}")
    logging.debug(response)
    logging.debug(response.text)
    return response.json()


# curl -H 'Content-Type: application/json' -d @my-transfer.json -X POST "http://localhost:28181/management/v3/transferprocesses" -s
def start_transfer(contract_id, asset_name, destination_url, provider_url, provider_id="provider",
                   transfer_type="HttpData-PUSH"):
    response = post(f"{CONSUMER_URL}/management/v3/transferprocesses",
                    headers=headers,
                    data=json.dumps({
                        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                        "@type": "TransferRequest",
                        "dataDestination": {
                            "type": "HttpData",
                            "baseUrl": destination_url
                        },
                        "protocol": "dataspace-protocol-http",
                        "assetId": asset_name,
                        "contractId": contract_id,
                        "connectorId": provider_id,
                        "counterPartyAddress": f"{provider_url}/protocol",
                        "transferType": transfer_type
                    }))
    logging.debug(response)
    logging.debug(response.text)
    return response.json()

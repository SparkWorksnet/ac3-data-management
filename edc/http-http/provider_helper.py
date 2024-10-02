import json
import logging

from requests import post

PROVIDER_DOMAIN = "localhost"
PROVIDER_URL = f"http://{PROVIDER_DOMAIN}:18181"
PROVIDER_URL_2 = f"http://{PROVIDER_DOMAIN}:18182"

headers = {'Content-Type': 'application/json'}


# curl -H 'Content-Type: application/json' -d @my-asset.json -X POST "http://localhost:18181"
def create_asset(asset_name, file_path):
    response = post(f"{PROVIDER_URL}/management/v3/assets",
                    headers=headers,
                    data=json.dumps({
                        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                        "@id": asset_name,
                        "properties": {},
                        "dataAddress": {"type": "HttpStreaming", "sourceFolder": file_path}
                    }))
    logging.debug(response)
    logging.debug(response.text)
    return response.json()


# curl -H 'Content-Type: application/json' -d @policy-definition.json -X POST "http://localhost:18181/management/v3/policydefinitions"
def define_policy():
    response = post(f"{PROVIDER_URL}/management/v3/policydefinitions",
                    headers=headers,
                    data=json.dumps({
                        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                        "@id": "no-constraint-policy",
                        "policy": {"@context": "http://www.w3.org/ns/odrl.jsonld", "@type": "Set"}
                    }))
    logging.debug(response)
    logging.debug(response.text)
    return response.json()


# curl -H 'Content-Type: application/json' -d @contract-definition.json -X POST "http://localhost:18181/management/v3/contractdefinitions"
def create_contract_definitions():
    response = post(f"{PROVIDER_URL}/management/v3/contractdefinitions",
                    headers=headers,
                    data=json.dumps({
                        "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                        "@id": "contract-definition",
                        "accessPolicyId": "no-constraint-policy",
                        "contractPolicyId": "no-constraint-policy",
                        "assetsSelector": []
                    }))
    logging.debug(response)
    logging.debug(response.text)
    return response.json()

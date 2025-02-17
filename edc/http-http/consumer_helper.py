import json
import logging
from requests import post, get


class ConsumerHelper:
    def __init__(self,
                 consumer_base_url="http://localhost", consumer_http_port=28180, consumer_http_management_port=28181,
                 consumer_http_protocol_port=28182, consumer_http_control_port=28183,
                 provider_base_url="http://localhost", provider_http_port=18180, provider_http_management_port=18181,
                 provider_http_protocol_port=18182, provider_http_control_port=18183):
        self.cons_domain = consumer_base_url
        self.consumer_http_port = consumer_http_port
        self.consumer_url = f"{self.cons_domain}:{self.consumer_http_port}"
        self.consumer_http_management_port = consumer_http_management_port
        self.consumer_url_management = f"{self.cons_domain}:{self.consumer_http_management_port}"
        self.consumer_http_protocol_port = consumer_http_protocol_port
        self.consumer_url_protocol = f"{self.cons_domain}:{self.consumer_http_protocol_port}"
        self.consumer_http_control_port = consumer_http_control_port
        self.consumer_url_control = f"{self.cons_domain}:{self.consumer_http_control_port}"
        self.provider_domain = provider_base_url
        self.provider_http_port = provider_http_port
        self.provider_url = f"{self.provider_domain}:{self.provider_http_port}"
        self.provider_http_management_port = provider_http_management_port
        self.provider_url_management = f"{self.provider_domain}:{self.provider_http_management_port}"
        self.provider_http_protocol_port = provider_http_protocol_port
        self.provider_url_protocol = f"{self.provider_domain}:{self.provider_http_protocol_port}"
        self.provider_http_control_port = provider_http_control_port
        self.provider_url_control = f"{self.provider_domain}:{self.provider_http_control_port}"
        self.headers = {'Content-Type': 'application/json'}

    # curl -H 'Content-Type: application/json' -d @get-dataset.json -X POST "http://localhost:28181/management/v3/catalog/dataset/request" -s | jq
    def request_asset(self, asset_name):
        response = post(f"{self.consumer_url_management}/management/v3/catalog/dataset/request",
                        headers=self.headers,
                        data=json.dumps({
                            "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                            "@type": "DatasetRequest",
                            "@id": asset_name,
                            "counterPartyAddress": f"{self.provider_url_protocol}/protocol",
                            "protocol": "dataspace-protocol-http"
                        }))
        logging.debug(response)
        logging.debug(response.text)
        return response.json()

    # curl -H 'Content-Type: application/json' -d @my-negotiate-contract.json  -X POST "http://localhost:28181/management/v3/contractnegotiations" -s | jq
    def negotiate_contract(self, policy_id, asset_name, provider_id="provider"):
        response = post(f"{self.consumer_url_management}/management/v3/contractnegotiations",
                        headers=self.headers,
                        data=json.dumps({
                            "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                                         "odrl": "http://www.w3.org/ns/odrl/2/"},
                            "@type": "ContractRequest",
                            "counterPartyAddress": f"{self.provider_url_protocol}/protocol",
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
    def get_negotiation_status(self, negotiation_id):
        response = get(f"{self.consumer_url_management}/management/v3/contractnegotiations/{negotiation_id}")
        print(response)
        print(response.text)
        return response.json()

    # curl -H 'Content-Type: application/json' -d @my-transfer.json -X POST "http://localhost:28181/management/v3/transferprocesses" -s
    def start_transfer(self, contract_id, asset_name, destination_url, provider_id="provider",
                       transfer_type="HttpData-PUSH"):
        response = post(f"{self.consumer_url_management}/management/v3/transferprocesses",
                        headers=self.headers,
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
                            "counterPartyAddress": f"{self.provider_url_protocol}/protocol",
                            "transferType": transfer_type
                        }))
        logging.debug(response)
        logging.debug(response.text)
        return response.json()

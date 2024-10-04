import json
import logging

from requests import post


class ProviderHelper:
    def __init__(self, provider_base_url="http://localhost", provider_http_port=18180,
                 provider_http_management_port=18181, provider_http_protocol_port=18182,
                 provider_http_control_port=18183):
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

    # curl -H 'Content-Type: application/json' -d @my-asset.json -X POST "http://localhost:18181"
    def create_asset(self, asset_name, file_path):
        response = post(f"{self.provider_url_management}/management/v3/assets",
                        headers=self.headers,
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
    def define_policy(self):
        response = post(f"{self.provider_url_management}/management/v3/policydefinitions",
                        headers=self.headers,
                        data=json.dumps({
                            "@context": {"@vocab": "https://w3id.org/edc/v0.0.1/ns/"},
                            "@id": "no-constraint-policy",
                            "policy": {"@context": "http://www.w3.org/ns/odrl.jsonld", "@type": "Set"}
                        }))
        logging.debug(response)
        logging.debug(response.text)
        return response.json()

    # curl -H 'Content-Type: application/json' -d @contract-definition.json -X POST "http://localhost:18181/management/v3/contractdefinitions"
    def create_contract_definitions(self):
        response = post(f"{self.provider_url_management}/management/v3/contractdefinitions",
                        headers=self.headers,
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

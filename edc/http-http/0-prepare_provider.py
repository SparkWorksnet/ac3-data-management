import provider_helper as provider

asset_name = "files-asset-1"
files_path = "/usr/src/app/data"

PROVIDER_DOMAIN = "http://192.168.1.215"

provider_helper = provider.ProviderHelper(provider_base_url=PROVIDER_DOMAIN)

# ASSET CREATION

a = provider_helper.create_asset(asset_name, files_path)
print(f"response: {a}")

a = provider_helper.define_policy()
print(f"response: {a}")

a = provider_helper.create_contract_definitions()
print(f"response: {a}")

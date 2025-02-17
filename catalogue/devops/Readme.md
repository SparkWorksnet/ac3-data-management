# Installation

## Keycloak

Run the docker file with

````shell
docker compose -f docker-compose-keycloak.yml up -d
````

+ Login to keycloak using the administrator credentials
+ Create a new Realm with name `piveau`
+ Go to Realm settings, `Keys` > `Providers`, Delete the `rsa-enc-generated` provider
+ Go to Realm role and create a role called `operator`
+ Create a client with the name `piveau-hub-ui`
    + Go to `Settings` tab
        + Set `Root URL`, `Web origins` and `Admin URL` to `http://localhost:8080`
        + Set `Home URL` to `/`
        + Set `Valid redirect URIs` to `http://localhost:8080/*`
        + Set `Valid post logout redirect URIs` to `+`
+ Create client `piveau-hub-repo`
    + Enable authentication and authorization for the client under `Settings` > `Capability config`
    + Go to `Authorization` tab of `piveau-hub-repo` client,
        + Go to `Settings` tab
            + Set `Decision Strategy` to `Affirmative`
        + Go to `Scopes` tab
            + Create scopes: `dataset:create`, `dataset:update` and `dataset:delete`
            + Create scopes: `catalogue:create`, `catalogue:update` and `catalogue:delete`
        + Go to `Policies` tab
            + Click on `Create Policy`, and select the `Role` policy type
                + Name it `Operator Policy`
                + Add the `operator` role to the policy
        + Go to `Permissions` tab
            + Create an `operator` permissions
                + Click on `Create scope-based permission`
                    + Add all available scopes
                    + Add `operator` policy
        + Go to `Resource` tab
            + Delete `Default Resource`
            + Create a resource `Catalogue Resource`
            + Set display name to `Catalogue Resource`
            + Add URI: `urn:piveau-hub-repo:resource:catalogue`
            + Add all catalogue scopes
            + Enable User-Managed access
    + Go to `Service accounts roles`
        + Click in `Assign role`
        + Filter by clients
        + Search for `manage-users` and assign
    + Go to `Advanced` Tab
        + Set `refresh tokens` ON
        + Set` Refresh tokens for client credentials grant` ON
+ Go to `Realm Settings`
    + Go to `Security Defences`
        + Set `Content-Security-Policy` to `frame-src 'self' *; frame-ancestors 'self' *; object-src 'none';`

## hub-repo configuration

+ Get from `keycloak` -> `clients` -> `credentials` -> `client secret` the client's secret key.
    + Set it to `PIVEAU_HUB_AUTHORIZATION_PROCESS_DATA.clientSecret` under `piveau-hub-repo` service
      in `docker-compose-piveau.yml`
+ Set the rest of the keycloak's data to `PIVEAU_HUB_AUTHORIZATION_PROCESS_DATA.*`

## hub-ui configuration

+ Set the keycloak's data to `VUE_APP_AUTHENTICATION_KEYCLOAK_*` settings under `piveau-hub-ui` service
  in `docker-compose-piveau.yml`

## create a single user

+ Go to `keycloak` -> `users` -> `Add User`
    + Set `Username` and any other settings needed.
    + Go to `Credentials`, click `Set Password` and set the user's password.

## start piveau

Start the piveau services using

````shell
docker compose -f docker-compose-piveau.yml up -d
````


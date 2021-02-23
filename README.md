# SBchain Verifier Agent

The SBchain Verifier Agent has been built as a Keycloak plugin. Keycloak is an open source software product that allows single sign-on with Identity and Access Management. Specifically Keycloak supports both OIDC and SAML and is capable of acting as a fully functional industrial ready Identity Management Server (IdMs). Keycloak supports the creation and installment of various plugins that intervene in the normal authentication flow of a user. Specifically, through the use of these plugins (referred to as Service Provider Interfaces or SPIs) various means of user authentications mechanisms can be deployed.

In detail, the SBchain Verifier Agent plugin implements authentication flows that interrupt the normal OIDC flow to request form the user the presentation of (some of) the following Verifiable Credentials, issued by the SBchain VC Issuer service (D4.3):
* TAXIS ID
* Civil Registry ID
* Electricity Bill ID
* Personal Financial Status
* Contact Me
* KEA - Personal Declaration

Once the user discloses the credential the SPI verifies it and then continues with the OIDC authentication (as was originally requested) by inserting in the OIDC authorization token then claims aforementioned VC attributes. 

package com.maxime.help.msauth.domain.port.out;

/** Outbound port for exchanging a Google authorization code for a verified identity. */
public interface GoogleIdentityProvider {

    /**
     * Exchanges a JS-SDK ("postmessage" flow) authorization code for a verified Google identity.
     *
     * @throws GoogleAuthenticationException on any exchange or verification failure
     */
    GoogleIdentity exchangeAuthorizationCode(String authorizationCode);
}

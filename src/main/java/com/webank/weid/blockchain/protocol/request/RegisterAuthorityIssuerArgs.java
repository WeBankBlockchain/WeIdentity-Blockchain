

package com.webank.weid.blockchain.protocol.request;

import com.webank.weid.blockchain.protocol.base.WeIdPrivateKey;
import lombok.Data;

import com.webank.weid.blockchain.protocol.base.AuthorityIssuer;

/**
 * The Arguments for SDK RegisterAuthorityIssuer.
 *
 * @author chaoxinhu 2018.10
 */
@Data
public class RegisterAuthorityIssuerArgs {

    /**
     * Required: The authority issuer information.
     */
    private AuthorityIssuer authorityIssuer;

    /**
     * Required: The WeIdentity DID private key for sending transaction.
     */
    private WeIdPrivateKey weIdPrivateKey;
}

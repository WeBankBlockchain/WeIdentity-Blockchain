

package com.webank.weid.blockchain.protocol.response;

import com.webank.weid.blockchain.protocol.base.WeIdPrivateKey;
import lombok.Data;

import com.webank.weid.blockchain.protocol.base.WeIdPublicKey;

/**
 * The response result for creating WeIdentity DID.
 *
 * @author tonychen 2018.10.9
 */
@Data
public class CreateWeIdDataResult {

    /**
     * The WeIdentity DID String.
     */
    private String weId;

    /**
     * The WeIdentity DID Public Key wrapper.
     */
    private WeIdPublicKey userWeIdPublicKey;

    /**
     * The WeIdentity DID Private Key wrapper.
     */
    private WeIdPrivateKey userWeIdPrivateKey;
}



package com.webank.weid.blockchain.protocol.response;

import lombok.Data;

import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.Uint8;


/**
 * The internal base RSV signature data class.
 *
 * @author lingfenghe
 */
@Data
public class RsvSignature {

    /**
     * The v value.
     */
    private Uint8 v;

    /**
     * The r value.
     */
    private Bytes32 r;

    /**
     * The s value.
     */
    private Bytes32 s;

    /**
     * todo support sm2
     */
//    private Bytes32 pub;
}

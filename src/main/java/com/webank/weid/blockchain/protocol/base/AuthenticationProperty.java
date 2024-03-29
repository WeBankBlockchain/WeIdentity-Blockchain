

package com.webank.weid.blockchain.protocol.base;

import com.webank.weid.blockchain.util.DataToolUtils;
import com.webank.weid.blockchain.util.Multibase.Multibase;
import com.webank.weid.blockchain.util.Multicodec.DecodedData;
import com.webank.weid.blockchain.util.Multicodec.MulticodecEncoder;
import com.webank.weid.blockchain.util.PropertyUtils;
import lombok.Data;
import org.fisco.bcos.sdk.model.CryptoType;

/**
 * The base data structure for AuthenticationProperty.
 *
 * @author tonychen 2018.10.8
 */
@Data
public class AuthenticationProperty {

    /**
     * Required: The verification method id.
     */
    private String id;

    /**
     * Required: The verification method type.
     */
    private String type = Integer.parseInt(PropertyUtils.getProperty("sdk.sm-crypto", "0")) == CryptoType.ECDSA_TYPE? "Ed25519VerificationKey2020":"SM2VerificationKey";

    /**
     * Required: The verification method controller.
     */
    private String controller;

    /**
     * Required: The verification method material.
     */
    private String publicKeyMultibase;

    public String toString() {
        return this.id + ',' + this.type + ',' + this.controller + ',' + this.publicKeyMultibase;
    }

    public static AuthenticationProperty fromString(String authString) {
        String[] result = authString.split(",");
        AuthenticationProperty authenticationProperty = new AuthenticationProperty();
        authenticationProperty.setId(result[0]);
        authenticationProperty.setController(result[2]);
        authenticationProperty.setPublicKeyMultibase(result[3]);
        return authenticationProperty;
    }

    public String getPublicKey() {
        byte[] publicKeyEncode = Multibase.decode(this.publicKeyMultibase);
        DecodedData decodedData = MulticodecEncoder.decode(publicKeyEncode);
        return new String(decodedData.getData());
    }
}

package com.webank.weid.blockchain.service.fisco;

import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.protocol.response.RsvSignature;
import com.webank.weid.blockchain.constant.WeIdConstant;
import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.CryptoSuite;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.crypto.signature.ECDSASignatureResult;
import org.fisco.bcos.sdk.crypto.signature.SM2SignatureResult;
import org.fisco.bcos.sdk.crypto.signature.SignatureResult;
import org.fisco.bcos.sdk.model.CryptoType;
import org.fisco.bcos.sdk.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class CryptoFisco {
    private static final Logger logger = LoggerFactory.getLogger(CryptoFisco.class);
    /**
     * The Fisco Config bundle.
     */
    public static final FiscoConfig fiscoConfig;
    /**
     * use this to create key pair of v2 or v3
     * WARN: create keyPair must use BigInteger of privateKey or decimal String of privateKey
     */
    public static final CryptoSuite cryptoSuite;

    static {
        fiscoConfig = new FiscoConfig();
        if (!fiscoConfig.load()) {
            logger.error("[BaseService] Failed to load Fisco-BCOS blockchain node information.");
            System.exit(1);
        }
    }

    static {
        if (fiscoConfig.getVersion().startsWith(WeIdConstant.FISCO_BCOS_1_X_VERSION_PREFIX)) {
            logger.error("fisco version not support v1");
            System.exit(1);
            cryptoSuite = null;
        } else if (fiscoConfig.getVersion().startsWith(WeIdConstant.FISCO_BCOS_2_X_VERSION_PREFIX)) {
            cryptoSuite = new CryptoSuite(((Client) BaseServiceFisco.getClient()).getCryptoType());
        } else {
            cryptoSuite = new CryptoSuite(((org.fisco.bcos.sdk.v3.client.Client) BaseServiceFisco.getClient()).getCryptoType());
        }
    }

    //generate hex privateKey
    public static String generatePrivateKey() {
        return cryptoSuite.getKeyPairFactory().generateKeyPair().getHexPrivateKey();
    }

    //derive hex publicKey from privateKey
    public static CryptoKeyPair keypairFromPrivate(BigInteger privateKey) {
        return cryptoSuite.getKeyPairFactory().createKeyPair(privateKey);
    }

    //derive address from publicKey
    public static String addressFromPublicKey(BigInteger publicKey) {
        return Numeric.toHexString(cryptoSuite.getKeyPairFactory().getAddress(publicKey));
    }

    /**
     * Sha 3.
     *
     * @param input the input
     * @return the byte[]
     */
    public static byte[] hash(byte[] input) {
        return cryptoSuite.hash(input);
    }

    /**
     * Sha 3.
     *
     * @param input the input
     * @return the string
     */
    public static String hash(String input) {
        return cryptoSuite.hash(input);
    }

    /**
     * Secp256k1 or SM3 sign to Signature.
     *
     * @param messageHash hash of original raw data
     * @param privateKey decimal
     * @return SignatureData for signature value
     */
    public static RsvSignature sign(String messageHash, String privateKey) {
        CryptoKeyPair cryptoKeyPair =  cryptoSuite.getKeyPairFactory().createKeyPair(new BigInteger(privateKey));
        RsvSignature rsvSignature = new RsvSignature();
        SignatureResult signatureResult = cryptoSuite.sign(messageHash, cryptoKeyPair);
        Bytes32 R = new Bytes32(signatureResult.getR());
        rsvSignature.setR(R);
        Bytes32 S = new Bytes32(signatureResult.getS());
        rsvSignature.setS(S);
        if(cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE){
            ECDSASignatureResult ecdsaSignatureResult = new ECDSASignatureResult(signatureResult.convertToString());
            rsvSignature.setV(new Uint8(BigInteger.valueOf(ecdsaSignatureResult.getV())));
        } else {
            rsvSignature.setV(new Uint8(0));
        }
        return rsvSignature;
    }

    /**
     * Verify secp256k1 signature.
     *
     * @param hexPublicKey publicKey in hex string
     * @param messageHash hash of original raw data
     * @param rsvSignature signature value
     * @return return boolean result, true is success and false is fail
     */
    public static boolean verifySignature(
            String hexPublicKey,
            String messageHash,
            RsvSignature rsvSignature
    ) {
        if(cryptoSuite.getCryptoTypeConfig() == CryptoType.ECDSA_TYPE) {
            ECDSASignatureResult signatureResult = new ECDSASignatureResult(
                    rsvSignature.getV().getValue().byteValueExact(),
                    rsvSignature.getR().getValue(),
                    rsvSignature.getS().getValue());
            return cryptoSuite.verify(hexPublicKey, messageHash, signatureResult.convertToString());
        } else {
//                byte[] sigBytes = new byte[64];
//                System.arraycopy(rsvSignature.getR(), 0, sigBytes, 0, 32);
//                System.arraycopy(rsvSignature.getS(), 0, sigBytes, 32, 32);
            SM2SignatureResult signatureResult = new SM2SignatureResult(
                    Numeric.hexStringToByteArray(hexPublicKey), //todo pub of sm2 sig
                    rsvSignature.getR().getValue(),
                    rsvSignature.getS().getValue());
            return cryptoSuite.verify(hexPublicKey, messageHash, signatureResult.convertToString());
        }
    }
}

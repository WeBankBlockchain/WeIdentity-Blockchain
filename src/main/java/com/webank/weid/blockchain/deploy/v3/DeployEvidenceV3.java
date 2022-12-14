

package com.webank.weid.blockchain.deploy.v3;

import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.protocol.base.WeIdPrivateKey;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.blockchain.constant.CnsType;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.deploy.AddressProcess;
import com.webank.weid.contract.v3.EvidenceContract;

import java.math.BigInteger;

import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.v3.client.Client;
import org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.v3.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeployEvidenceV3 extends AddressProcess {
    
    /**
     * log4j.
     */
    private static final Logger logger = LoggerFactory.getLogger(
        DeployEvidenceV3.class);

    /**
     * The cryptoKeyPair.
     */
    private static CryptoKeyPair cryptoKeyPair;
    
    /**
     * Inits the cryptoKeyPair.
     *
     * @return true, if successful
     */
    private static String initCryptoKeyPair(String inputPrivateKey) {
        if (StringUtils.isNotBlank(inputPrivateKey)) {
            logger.info("[DeployEvidenceV2] begin to init credentials by privateKey..");
            cryptoKeyPair = ((Client) BaseServiceFisco.getClient()).getCryptoSuite()
                .getKeyPairFactory().createKeyPair(new BigInteger(inputPrivateKey));
        } else {
            // 此分支逻辑实际情况不会执行，因为通过build-tool进来是先给创建私钥
            logger.info("[DeployEvidenceV2] begin to init credentials..");
            /*credentials = GenCredential.create();
            String privateKey = credentials.getEcKeyPair().getPrivateKey().toString();
            String publicKey = credentials.getEcKeyPair().getPublicKey().toString();*/
            cryptoKeyPair = ((Client) BaseServiceFisco.getClient()).getCryptoSuite()
                .getKeyPairFactory().generateKeyPair();
            byte[] priBytes = Numeric.hexStringToByteArray(cryptoKeyPair.getHexPrivateKey());
            byte[] pubBytes = Numeric.hexStringToByteArray(cryptoKeyPair.getHexPublicKey());
            String privateKey = new BigInteger(1, priBytes).toString(10);
            String publicKey = new BigInteger(1, pubBytes).toString(10);
            writeAddressToFile(publicKey, "public_key");
            writeAddressToFile(privateKey, "private_key");
        }

        //if (credentials == null) {
        if (cryptoKeyPair == null) {
            logger.error("[DeployEvidenceV2] credentials init failed. ");
            return StringUtils.EMPTY;
        }
        //return credentials.getEcKeyPair().getPrivateKey().toString();
        byte[] priBytes = Numeric.hexStringToByteArray(cryptoKeyPair.getHexPrivateKey());
        return new BigInteger(1, priBytes).toString(10);
    }

    protected static Client getClient(String groupId) {
        return (Client) BaseServiceFisco.getClient(groupId);
    }
    
    public static String deployContract(
        FiscoConfig fiscoConfig,
        String inputPrivateKey,
        String groupId,
        boolean instantEnable
    ) {
        //String privateKey = initCredentials(inputPrivateKey);
        String privateKey = initCryptoKeyPair(inputPrivateKey);
        
        String evidenceAddress = deployEvidenceContractsNew(groupId);
        // 将地址注册到cns中
        CnsType cnsType = CnsType.SHARE;
        RegisterAddressV3.registerAllCns(privateKey);
        // 根据群组和evidence Address获取hash
        String hash = getHashForShare(groupId, evidenceAddress);
        // 将evidence地址注册到cns中
        RegisterAddressV3.registerAddress(
            cnsType, 
            hash, 
            evidenceAddress, 
            WeIdConstant.CNS_EVIDENCE_ADDRESS,
                privateKey
        );
        // 将群组编号注册到cns中
        RegisterAddressV3.registerAddress(
            cnsType, 
            hash, 
            groupId.toString(), 
            WeIdConstant.CNS_GROUP_ID,
                privateKey
        );
        
        if (instantEnable) {
            //将evidence hash配置到机构配置cns中
            RegisterAddressV3.registerHashToOrgConfig(
                fiscoConfig.getCurrentOrgId(), 
                WeIdConstant.CNS_EVIDENCE_HASH + groupId.toString(), 
                hash,
                    privateKey
            );
            //将evidence地址配置到机构配置cns中
            RegisterAddressV3.registerHashToOrgConfig(
                fiscoConfig.getCurrentOrgId(), 
                WeIdConstant.CNS_EVIDENCE_ADDRESS + groupId.toString(), 
                evidenceAddress,
                    privateKey
            );
            // 合约上也启用hash
            RegisterAddressV3.enableHash(cnsType, hash, privateKey);
        }
        return hash;
    }
    
    private static String deployEvidenceContractsNew(String groupId) {
        try {
            EvidenceContract evidenceContract =
                EvidenceContract.deploy(
                    getClient(groupId),
                    cryptoKeyPair
                );
            String evidenceContractAddress = evidenceContract.getContractAddress();
            return evidenceContractAddress;
        } catch (Exception e) {
            logger.error("EvidenceFactory deploy exception", e);
        }
        return StringUtils.EMPTY;
    }
}

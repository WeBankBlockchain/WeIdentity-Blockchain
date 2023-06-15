

package com.webank.weid.blockchain.service.fisco.engine.fiscov3;

import com.webank.weid.blockchain.service.fisco.engine.BaseEngineFisco;
import com.webank.weid.blockchain.service.fisco.engine.EvidenceServiceEngineFisco;
import com.webank.weid.blockchain.constant.CnsType;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.contract.v3.EvidenceContract;
import com.webank.weid.contract.v3.EvidenceContract.EvidenceAttributeChangedEventResponse;
import com.webank.weid.contract.v3.EvidenceContract.EvidenceExtraAttributeChangedEventResponse;
import com.webank.weid.blockchain.exception.WeIdBaseException;
import com.webank.weid.blockchain.protocol.base.EvidenceInfo;
import com.webank.weid.blockchain.protocol.base.EvidenceSignInfo;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.protocol.response.TransactionInfo;
import com.webank.weid.blockchain.util.DataToolUtils;
import com.webank.weid.blockchain.util.WeIdUtils;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.v3.codec.datatypes.generated.tuples.generated.Tuple5;
import org.fisco.bcos.sdk.v3.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EvidenceServiceEngine calls evidence contract which runs on FISCO BCOS 2.0.
 *
 * @author yanggang, chaoxinhu
 */
public class EvidenceServiceFiscoEngineFiscoV3 extends BaseEngineFisco implements EvidenceServiceEngineFisco {

    private static final Logger logger = LoggerFactory.getLogger(
        EvidenceServiceFiscoEngineFiscoV3.class);

    private EvidenceContract evidenceContract;

    private String evidenceAddress;

    private String groupId;

    /**
     * 构造函数.
     *
     * @param groupId 群组编号
     */
    public EvidenceServiceFiscoEngineFiscoV3(String groupId) {
        super(groupId);
        this.groupId = groupId;
        initEvidenceAddress();
        evidenceContract = getContractService(this.evidenceAddress, EvidenceContract.class);
    }

    private void initEvidenceAddress() {
        if (groupId == null || masterGroupId.equals(groupId)) {
            logger.info("[initEvidenceAddress] the groupId is master.");
            this.evidenceAddress = fiscoConfig.getEvidenceAddress();
            return;
        }
        this.evidenceAddress = super.getBucket(CnsType.ORG_CONFING).get(
            fiscoConfig.getCurrentOrgId(), WeIdConstant.CNS_EVIDENCE_ADDRESS + groupId).getResult();
        if (StringUtils.isBlank(evidenceAddress)) {
            throw new WeIdBaseException("can not found the evidence address from chain, you may "
                + "not activate the evidence contract on WeID Build Tools.");
        }
        logger.info(
            "[initEvidenceAddress] get the address from cns. address = {}",
            evidenceAddress
        );
    }

    @Override
    public ResponseData<String> createEvidence(
        String hashValue,
        String signature,
        String extra,
        Long timestamp,
        String privateKey
    ) {
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            if (!DataToolUtils.isValidHash(hashValue)) {
                return new ResponseData<>(StringUtils.EMPTY, ErrorCode.ILLEGAL_INPUT, null);
            }
            hashByteList.add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValue));
            /*String address = WeIdUtils
                .convertWeIdToAddress(DataToolUtils.convertPrivateKeyToDefaultWeId(privateKey));*/

            String address = DataToolUtils.addressFromPrivate(new BigInteger(privateKey));
            List<String> signerList = new ArrayList<>();
            signerList.add(address);
            List<String> sigList = new ArrayList<>();
            sigList.add(signature);
            List<String> logList = new ArrayList<>();
            logList.add(extra);
            List<BigInteger> timestampList = new ArrayList<>();
            timestampList.add(new BigInteger(String.valueOf(timestamp), 10));
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            TransactionReceipt receipt =
                evidenceContractWriter.createEvidence(
                    hashByteList,
                    signerList,
                    sigList,
                    logList,
                    timestampList
                );

            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceContract.CreateEvidenceEventResponse> eventList =
                evidenceContract.getCreateEvidenceEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(StringUtils.EMPTY,
                    ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(StringUtils.EMPTY,
                    ErrorCode.CREDENTIAL_EVIDENCE_ALREADY_EXISTS, info);
            } else {
                for (EvidenceContract.CreateEvidenceEventResponse event : eventList) {
                    if (event.sig.equalsIgnoreCase(signature)
                            && event.signer.equalsIgnoreCase(address)) {
                        return new ResponseData<>(hashValue, ErrorCode.SUCCESS, info);
                    }
                }
            }
            return new ResponseData<>(StringUtils.EMPTY,
                ErrorCode.CREDENTIAL_EVIDENCE_CONTRACT_FAILURE_ILLEAGAL_INPUT);
        } catch (Exception e) {
            logger.error("create evidence failed due to system error. ", e);
            return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<List<Boolean>> batchCreateEvidence(
        List<String> hashValues,
        List<String> signatures,
        List<String> logs,
        List<Long> timestamp,
        List<String> signers,
        String privateKey
    ) {
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < hashValues.size(); i++) {
            result.add(false);
        }
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            List<String> signerList = new ArrayList<>();
            List<BigInteger> timestampList = new ArrayList<>();
            List<String> logList = new ArrayList<>();
            List<String> sigList = new ArrayList<>();
            for (int i = 0; i < hashValues.size(); i++) {
                if (hashValues.get(i) == null) {
                    hashValues.set(i, StringUtils.EMPTY);
                }
                if (!DataToolUtils.isValidHash(hashValues.get(i))) {
                    continue;
                }
                hashByteList
                    .add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValues.get(i)));
                signerList.add(WeIdUtils.convertWeIdToAddress(signers.get(i)));
                timestampList.add(new BigInteger(String.valueOf(timestamp.get(i)), 10));
                logList.add(logs.get(i));
                sigList.add(signatures.get(i));
            }
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            TransactionReceipt receipt =
                evidenceContractWriter.createEvidence(
                    hashByteList,
                    signerList,
                    sigList,
                    logList,
                    timestampList
                );

            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceContract.CreateEvidenceEventResponse> eventList =
                evidenceContractWriter.getCreateEvidenceEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(result, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(result, ErrorCode.CREDENTIAL_EVIDENCE_ALREADY_EXISTS,
                    info);
            } else {
                List<String> returnedHashs = new ArrayList<>();
                for (EvidenceContract.CreateEvidenceEventResponse event : eventList) {
                    //Object[] hashArray = event.hash.toArray();
                    returnedHashs.add(DataToolUtils.convertHashByte32ArrayIntoHashStr(
                            (new Bytes32(event.hash)).getValue()));
                }
                return new ResponseData<>(
                    DataToolUtils.strictCheckExistence(hashValues, returnedHashs),
                    ErrorCode.SUCCESS, info);
            }
        } catch (Exception e) {
            logger.error("create evidence failed due to system error. ", e);
            return new ResponseData<>(result, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<List<Boolean>> batchCreateEvidenceWithCustomKey(
        List<String> hashValues,
        List<String> signatures,
        List<String> logs,
        List<Long> timestamp,
        List<String> signers,
        List<String> customKeys,
        String privateKey
    ) {
        List<Boolean> result = new ArrayList<>();
        for (int i = 0; i < hashValues.size(); i++) {
            result.add(false);
        }
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            List<String> signerList = new ArrayList<>();
            List<BigInteger> timestampList = new ArrayList<>();
            List<String> customKeyList = new ArrayList<>();
            List<String> logList = new ArrayList<>();
            List<String> sigList = new ArrayList<>();
            for (int i = 0; i < hashValues.size(); i++) {
                if (hashValues.get(i) == null) {
                    hashValues.set(i, StringUtils.EMPTY);
                }
                if (!DataToolUtils.isValidHash(hashValues.get(i))) {
                    continue;
                }
                hashByteList
                    .add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValues.get(i)));
                signerList.add(WeIdUtils.convertWeIdToAddress(signers.get(i)));
                timestampList.add(new BigInteger(String.valueOf(timestamp.get(i)), 10));
                customKeyList.add(customKeys.get(i));
                logList.add(logs.get(i));
                sigList.add(signatures.get(i));
            }
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            TransactionReceipt receipt =
                evidenceContractWriter.createEvidenceWithExtraKey(
                    hashByteList,
                    signerList,
                    sigList,
                    logList,
                    timestampList,
                    customKeyList
                );

            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceContract.CreateEvidenceEventResponse> eventList =
                evidenceContractWriter.getCreateEvidenceEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(result,
                    ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(result, ErrorCode.CREDENTIAL_EVIDENCE_ALREADY_EXISTS,
                    info);
            } else {
                List<String> returnedHashs = new ArrayList<>();
                for (EvidenceContract.CreateEvidenceEventResponse event : eventList) {
                    //Object[] hashArray = event.hash.toArray();
                    returnedHashs.add(DataToolUtils.convertHashByte32ArrayIntoHashStr(
                            (new Bytes32(event.hash)).getValue()));
                }
                return new ResponseData<>(
                    DataToolUtils.strictCheckExistence(hashValues, returnedHashs),
                    ErrorCode.SUCCESS, info);
            }
        } catch (Exception e) {
            logger.error("create evidence failed due to system error. ", e);
            return new ResponseData<>(result, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<Boolean> addLog(
        String hashValue,
        String sig,
        String log,
        Long timestamp,
        String privateKey
    ) {
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            if (!DataToolUtils.isValidHash(hashValue)) {
                return new ResponseData<>(false, ErrorCode.ILLEGAL_INPUT, null);
            }
            hashByteList.add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValue));
            List<String> sigList = new ArrayList<>();
            sigList.add(sig);
            List<String> logList = new ArrayList<>();
            logList.add(log);
            List<BigInteger> timestampList = new ArrayList<>();
            timestampList.add(new BigInteger(String.valueOf(timestamp), 10));
            /*String address = WeIdUtils
                .convertWeIdToAddress(DataToolUtils.convertPrivateKeyToDefaultWeId(privateKey));*/

            String address = DataToolUtils.addressFromPrivate(new BigInteger(privateKey));
            List<String> signerList = new ArrayList<>();
            signerList.add(address);
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            TransactionReceipt receipt =
                evidenceContractWriter.addSignatureAndLogs(
                    hashByteList,
                    signerList,
                    sigList,
                    logList,
                    timestampList
                );
            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceAttributeChangedEventResponse> eventList =
                evidenceContractWriter.getEvidenceAttributeChangedEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST, info);
            } else {
                for (EvidenceAttributeChangedEventResponse event : eventList) {
                    if (event.signer.equalsIgnoreCase(address)) {
                        return new ResponseData<>(true, ErrorCode.SUCCESS, info);
                    }
                }
            }
            return new ResponseData<>(false,
                ErrorCode.CREDENTIAL_EVIDENCE_CONTRACT_FAILURE_ILLEAGAL_INPUT);
        } catch (Exception e) {
            logger.error("add log failed due to system error. ", e);
            return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<Boolean> addLogByCustomKey(
        String hashValue,
        String sig,
        String log,
        Long timestamp,
        String customKey,
        String privateKey
    ) {
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            if (!DataToolUtils.isValidHash(hashValue)) {
                return new ResponseData<>(false, ErrorCode.ILLEGAL_INPUT, null);
            }
            hashByteList.add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValue));
            List<String> sigList = new ArrayList<>();
            sigList.add(sig);
            List<String> logList = new ArrayList<>();
            logList.add(log);
            List<BigInteger> timestampList = new ArrayList<>();
            timestampList.add(new BigInteger(String.valueOf(timestamp), 10));
            /*String address = WeIdUtils
                .convertWeIdToAddress(DataToolUtils.convertPrivateKeyToDefaultWeId(privateKey));*/

            String address = DataToolUtils.addressFromPrivate(new BigInteger(privateKey));
            List<String> signerList = new ArrayList<>();
            signerList.add(address);
            List<String> customKeyList = new ArrayList<>();
            customKeyList.add(customKey);
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            signerList.add(address);
            TransactionReceipt receipt =
                evidenceContractWriter.addSignatureAndLogsWithExtraKey(
                    hashByteList,
                    signerList,
                    sigList,
                    logList,
                    timestampList,
                    customKeyList
                );
            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceAttributeChangedEventResponse> eventList =
                evidenceContractWriter.getEvidenceAttributeChangedEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST, info);
            } else {
                for (EvidenceAttributeChangedEventResponse event : eventList) {
                    if (event.signer.equalsIgnoreCase(address)) {
                        return new ResponseData<>(true, ErrorCode.SUCCESS, info);
                    }
                }
            }
            return new ResponseData<>(false,
                ErrorCode.CREDENTIAL_EVIDENCE_CONTRACT_FAILURE_ILLEAGAL_INPUT);
        } catch (Exception e) {
            logger.error("add log failed due to system error. ", e);
            return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<String> getHashByCustomKey(String customKey) {
        try {
            String hash = DataToolUtils.convertHashByte32ArrayIntoHashStr(
                evidenceContract.getHashByExtraKey(customKey));
            if (!StringUtils.isEmpty(hash)) {
                return new ResponseData<>(hash, ErrorCode.SUCCESS);
            }
        } catch (Exception e) {
            logger.error("get hash failed.", e);
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST);
    }

    /**
     * Get an evidence full info.
     *
     * @param hash evidence hash
     * @return evidence info
     */
    @Override
    public ResponseData<EvidenceInfo> getInfo(String hash) {
        EvidenceInfo evidenceInfo = new EvidenceInfo();
        evidenceInfo.setCredentialHash(hash);
        byte[] hashByte = DataToolUtils.convertHashStrIntoHashByte32Array(hash);
        try {
            Tuple5<List<String>, List<String>, List<String>, List<BigInteger>, List<Boolean>> result = evidenceContract.getEvidence(hashByte);
            if (result == null) {
                return new ResponseData<>(null, ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST);
            }
            Map<String, EvidenceSignInfo> signInfoMap = new HashMap<>();
            for(int i=0; i<result.getValue1().size(); i++){
                //如果signer已经存在（通过addSignatureAndLogs添加的签名和log），则覆盖签名，把log添加到已有的logs列表
                if(signInfoMap.containsKey(result.getValue1().get(i))){
                    if(!StringUtils.isEmpty(result.getValue2().get(i))){
                        signInfoMap.get(result.getValue1().get(i)).setSignature(result.getValue2().get(i));
                    }
                    if(!StringUtils.isEmpty(result.getValue3().get(i))){
                        signInfoMap.get(result.getValue1().get(i)).getLogs().add(result.getValue3().get(i));
                    }
                    signInfoMap.get(result.getValue1().get(i)).setTimestamp(result.getValue4().get(i).toString());
                }else{
                    EvidenceSignInfo evidenceSignInfo = new EvidenceSignInfo();
                    evidenceSignInfo.setSignature(result.getValue2().get(i));
                    if(!StringUtils.isEmpty(result.getValue3().get(i))){
                        evidenceSignInfo.getLogs().add(result.getValue3().get(i));
                    }
                    evidenceSignInfo.setTimestamp(result.getValue4().get(i).toString());
                    evidenceSignInfo.setRevoked(result.getValue5().get(i));
                    signInfoMap.put(result.getValue1().get(i), evidenceSignInfo);
                }
            }
            evidenceInfo.setSignInfo(signInfoMap);
            // Reverse the order of the list
            /*for (String signer : evidenceInfo.getSigners()) {
                List<String> extraList = evidenceInfo.getSignInfo().get(signer).getLogs();
                if (extraList != null && !extraList.isEmpty()) {
                    Collections.reverse(evidenceInfo.getSignInfo().get(signer).getLogs());
                }
            }*/
            return new ResponseData<>(evidenceInfo, ErrorCode.SUCCESS);
        } catch (Exception e) {
            logger.error("get evidence failed.", e);
            return new ResponseData<>(null, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.service.fisco.engine.EvidenceServiceEngine#createEvidence(
     * java.lang.String, java.lang.String, java.lang.String, java.lang.Long, java.lang.String,
     * java.lang.String)
     */
    @Override
    public ResponseData<String> createEvidenceWithCustomKey(
        String hashValue,
        String signature,
        String extra,
        Long timestamp,
        String extraKey,
        String privateKey) {
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            if (!DataToolUtils.isValidHash(hashValue)) {
                return new ResponseData<>(StringUtils.EMPTY, ErrorCode.ILLEGAL_INPUT, null);
            }
            hashByteList.add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValue));
            /*String address = WeIdUtils
                .convertWeIdToAddress(DataToolUtils.convertPrivateKeyToDefaultWeId(privateKey));*/

            String address = DataToolUtils.addressFromPrivate(new BigInteger(privateKey));
            List<String> signerList = new ArrayList<>();
            signerList.add(address);
            List<String> sigList = new ArrayList<>();
            sigList.add(signature);
            List<String> logList = new ArrayList<>();
            logList.add(extra);
            List<BigInteger> timestampList = new ArrayList<>();
            timestampList.add(new BigInteger(String.valueOf(timestamp), 10));
            List<String> extraKeyList = new ArrayList<>();
            extraKeyList.add(extraKey);
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            TransactionReceipt receipt =
                evidenceContractWriter.createEvidenceWithExtraKey(
                    hashByteList,
                    signerList,
                    sigList,
                    logList,
                    timestampList,
                    extraKeyList
                );

            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceContract.CreateEvidenceEventResponse> eventList =
                evidenceContractWriter.getCreateEvidenceEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(StringUtils.EMPTY,
                    ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(StringUtils.EMPTY,
                    ErrorCode.CREDENTIAL_EVIDENCE_ALREADY_EXISTS, info);
            } else {
                for (EvidenceContract.CreateEvidenceEventResponse event : eventList) {
                    if (event.sig.equalsIgnoreCase(signature)
                            && event.signer.equalsIgnoreCase(address)) {
                        return new ResponseData<>(hashValue, ErrorCode.SUCCESS, info);
                    }
                }
            }
            return new ResponseData<>(StringUtils.EMPTY,
                ErrorCode.CREDENTIAL_EVIDENCE_CONTRACT_FAILURE_ILLEAGAL_INPUT);
        } catch (Exception e) {
            logger.error("create evidence failed due to system error. ", e);
            return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.service.fisco.engine.EvidenceServiceEngine#getInfoByCustomKey(
     * java.lang.String)
     */
    @Override
    public ResponseData<EvidenceInfo> getInfoByCustomKey(String extraKey) {

        if (StringUtils.isBlank(extraKey) || !DataToolUtils.isUtf8String(extraKey)) {
            logger.error("[getInfoByCustomKey] extraKey illegal. ");
            return new ResponseData<EvidenceInfo>(null, ErrorCode.ILLEGAL_INPUT);
        }
        try {
            String hash = DataToolUtils.convertHashByte32ArrayIntoHashStr(
                evidenceContract.getHashByExtraKey(extraKey));
            if (StringUtils.isBlank(hash)) {
                logger.error("[getInfoByCustomKey] extraKey dose not match any hash. ");
                return new ResponseData<EvidenceInfo>(null,
                    ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST);
            }
            return this.getInfo(hash);
        } catch (Exception e) {
            logger.error("[getInfoByCustomKey] get evidence info failed. ", e);
            return new ResponseData<EvidenceInfo>(null, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<Boolean> setAttribute(
        String hashValue,
        String key,
        String value,
        Long timestamp,
        String privateKey
    ) {
        try {
            List<byte[]> hashByteList = new ArrayList<>();
            if (!DataToolUtils.isValidHash(hashValue)) {
                return new ResponseData<>(false, ErrorCode.ILLEGAL_INPUT, null);
            }
            hashByteList.add(DataToolUtils.convertHashStrIntoHashByte32Array(hashValue));
            List<String> keyList = new ArrayList<>();
            keyList.add(key);
            List<String> valueList = new ArrayList<>();
            valueList.add(value);
            List<BigInteger> timestampList = new ArrayList<>();
            timestampList.add(new BigInteger(String.valueOf(timestamp), 10));
            /*String address = WeIdUtils
                .convertWeIdToAddress(DataToolUtils.convertPrivateKeyToDefaultWeId(privateKey));*/

            String address = DataToolUtils.addressFromPrivate(new BigInteger(privateKey));
            List<String> signerList = new ArrayList<>();
            signerList.add(address);
            EvidenceContract evidenceContractWriter =
                reloadContract(
                    this.evidenceAddress,
                    privateKey,
                    EvidenceContract.class
                );
            TransactionReceipt receipt =
                evidenceContractWriter.setAttribute(
                    hashByteList,
                    signerList,
                    keyList,
                    valueList,
                    timestampList
                );
            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceExtraAttributeChangedEventResponse> eventList =
                evidenceContractWriter.getEvidenceExtraAttributeChangedEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST, info);
            } else {
                for (EvidenceExtraAttributeChangedEventResponse event : eventList) {
                    if (event.signer.equalsIgnoreCase(address)) {
                        return new ResponseData<>(true, ErrorCode.SUCCESS, info);
                    }
                }
            }
            return new ResponseData<>(false,
                ErrorCode.CREDENTIAL_EVIDENCE_CONTRACT_FAILURE_ILLEAGAL_INPUT);
        } catch (Exception e) {
            logger.error("add log failed due to system error. ", e);
            return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }

    @Override
    public ResponseData<Boolean> revoke(
            String hashValue,
            Boolean revokeStage,
            Long timestamp,
            String privateKey
    ) {
        try {
            String address = DataToolUtils.addressFromPrivate(new BigInteger(privateKey));
            EvidenceContract evidenceContractWriter =
                    reloadContract(
                            this.evidenceAddress,
                            privateKey,
                            EvidenceContract.class
                    );
            TransactionReceipt receipt =
                    evidenceContractWriter.revoke(
                            DataToolUtils.convertHashStrIntoHashByte32Array(hashValue),
                            address,
                            revokeStage
                    );
            TransactionInfo info = new TransactionInfo(receipt);
            List<EvidenceContract.RevokeEventResponse> eventList =
                    evidenceContractWriter.getRevokeEvents(receipt);
            if (eventList == null) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR, info);
            } else if (eventList.isEmpty()) {
                return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_NOT_EXIST, info);
            } else {
                for (EvidenceContract.RevokeEventResponse event : eventList) {
                    if (event.signer.equalsIgnoreCase(address)) {
                        return new ResponseData<>(true, ErrorCode.SUCCESS, info);
                    }
                }
            }
            return new ResponseData<>(false,
                    ErrorCode.CREDENTIAL_EVIDENCE_CONTRACT_FAILURE_ILLEAGAL_INPUT);
        } catch (Exception e) {
            logger.error("add log failed due to system error. ", e);
            return new ResponseData<>(false, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
        }
    }
}



package com.webank.weid.blockchain.service.impl;

import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.protocol.base.EvidenceInfo;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.rpc.EvidenceService;
import com.webank.weid.blockchain.rpc.WeIdService;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.blockchain.service.fisco.engine.EngineFactoryFisco;
import com.webank.weid.blockchain.service.fisco.engine.EvidenceServiceEngineFisco;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service implementations for operations on Evidence.
 *
 * @author afeexian 2022.10
 */
public class EvidenceServiceImpl extends AbstractService implements EvidenceService {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceServiceImpl.class);

    private WeIdService weIdService = new WeIdServiceImpl();

    //private ProcessingMode processingMode = ProcessingMode.IMMEDIATE;

    private EvidenceServiceEngineFisco evidenceServiceEngineFisco;

    //FISCO BCOS区块链同时支持不同群组，其他区块链可以不使用这个变量或者赋予其他涵义
    public String groupId;

    public EvidenceServiceImpl() {
        super();
        initEvidenceServiceEngine(BaseServiceFisco.masterGroupId);
    }

    /**
     * 传入processingMode来决定上链模式.
     *
     * @param groupId 群组编号
     */
    public EvidenceServiceImpl(String groupId) {
        super();
        //super(groupId);
        //this.processingMode = processingMode;
        initEvidenceServiceEngine(groupId);
    }

    private void initEvidenceServiceEngine(String groupId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            evidenceServiceEngineFisco = EngineFactoryFisco.createEvidenceServiceEngine(groupId);
            this.groupId = groupId;
        }
    }

    @Override
    public String getGroupId(){
        return this.groupId;
    }

    @Override
    public ResponseData<String> createEvidence(
            String hashValue,
            String signature,
            String log,
            Long timestamp,
            String privateKey
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return evidenceServiceEngineFisco.createEvidence(
                    hashValue,
                    signature,
                    log,
                    timestamp,
                    privateKey
            );
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Boolean> createEvidenceWithCustomKey(
        String hashValue,
        String signature,
        String log,
        Long timestamp,
        String extraKey,
        String privateKey
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            ResponseData<String> hashResp = evidenceServiceEngineFisco.createEvidenceWithCustomKey(
                    hashValue,
                    signature,
                    log,
                    timestamp,
                    extraKey,
                    privateKey
            );
            if (hashResp.getResult().equalsIgnoreCase(hashValue)) {
                return new ResponseData<>(true, ErrorCode.SUCCESS);
            } else {
                return new ResponseData<>(false, hashResp.getErrorCode(), hashResp.getErrorMessage());
            }
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<List<Boolean>> batchCreateEvidence(
            List<String> hashValues,
            List<String> signatures,
            List<String> logs,
            List<Long> timestamps,
            List<String> signers,
            String privateKey
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            ResponseData<List<Boolean>> resp = evidenceServiceEngineFisco.batchCreateEvidence(
                    hashValues, signatures, logs, timestamps, signers, privateKey);
            return new ResponseData<>(resp.getResult(), resp.getErrorCode(),
                    resp.getErrorMessage());
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<List<Boolean>> batchCreateEvidenceWithCustomKey(
            List<String> hashValues,
            List<String> signatures,
            List<String> logs,
            List<Long> timestamps,
            List<String> signers,
            List<String> extraKeys,
            String privateKey
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            ResponseData<List<Boolean>> resp = evidenceServiceEngineFisco.batchCreateEvidenceWithCustomKey(
                    hashValues, signatures, logs, timestamps, signers, extraKeys, privateKey);
            return new ResponseData<>(resp.getResult(), resp.getErrorCode(),
                    resp.getErrorMessage());
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Boolean> addLog(
            String hashValue,
            String signature,
            String log,
            Long timestamp,
            String privateKey
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return evidenceServiceEngineFisco.addLog(
                    hashValue,
                    signature,
                    log,
                    timestamp,
                    privateKey
            );
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get the hash info from blockchain using custom key.
     *
     * @param customKey the customKey on chain
     * @return The EvidenceInfo
     */
    @Override
    public ResponseData<String> getHashByCustomKey(String customKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return evidenceServiceEngineFisco.getHashByCustomKey(customKey);
            } catch (Exception e) {
                logger.error("Failed to find the hash value from custom key.", e);
                return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
            }
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Boolean> addLogByCustomKey(
            String hashValue,
            String signature,
            String log,
            Long timestamp,
            String customKey,
            String privateKey
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return evidenceServiceEngineFisco.addLogByCustomKey(
                    hashValue,
                    signature,
                    log,
                    timestamp,
                    customKey,
                    privateKey
            );
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get the evidence from blockchain.
     *
     * @param hashValue the evidence hash on chain
     * @return The EvidenceInfo
     */
    @Override
    public ResponseData<EvidenceInfo> getInfo(String hashValue) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return evidenceServiceEngineFisco.getInfo(hashValue);
            } catch (Exception e) {
                logger.error("Failed to find the hash value from custom key.", e);
                return new ResponseData<>(null, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get the evidence from blockchain.
     *
     * @param customKey the evidence hash on chain
     * @return The EvidenceInfo
     */
    @Override
    public ResponseData<EvidenceInfo> getInfoByCustomKey(String customKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return evidenceServiceEngineFisco.getInfoByCustomKey(customKey);
            } catch (Exception e) {
                logger.error("Failed to find the hash value from custom key.", e);
                return new ResponseData<>(null, ErrorCode.CREDENTIAL_EVIDENCE_BASE_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Revoke an evidence - which can be un-revoked.
     *
     * @param hash the hash
     * @param revokeStage the revokeStage
     * @param timestamp the timestamp
     * @param privateKey the weid privateKey
     * @return true if yes, false otherwise, with error codes
     */
    @Override
    public ResponseData<Boolean> revoke(String hash, Boolean revokeStage, Long timestamp, String privateKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return evidenceServiceEngineFisco.revoke(
                    hash,
                    revokeStage,
                    timestamp,
                    privateKey
            );
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }
}



package com.webank.weid.blockchain.service.fisco.engine;

import java.util.List;

import com.webank.weid.blockchain.protocol.base.EvidenceInfo;
import com.webank.weid.blockchain.protocol.response.ResponseData;

public interface EvidenceServiceEngineFisco {

    ResponseData<String> createEvidence(
        String hashValue,
        String signature,
        String log,
        Long timestamp,
        String privateKey
    );

    ResponseData<List<Boolean>> batchCreateEvidence(
        List<String> hashValues,
        List<String> signatures,
        List<String> logs,
        List<Long> timestamp,
        List<String> signers,
        String privateKey
    );

    ResponseData<Boolean> addLog(
        String hashValue,
        String sig,
        String log,
        Long timestamp,
        String privateKey
    );

    ResponseData<Boolean> addLogByCustomKey(
        String hashValue,
        String signature,
        String log,
        Long timestamp,
        String customKey,
        String privateKey
    );

    ResponseData<String> getHashByCustomKey(String customKey);

    ResponseData<String> createEvidenceWithCustomKey(
        String hashValue,
        String signature,
        String log,
        Long timestamp,
        String customKey,
        String privateKey
    );

    ResponseData<List<Boolean>> batchCreateEvidenceWithCustomKey(
        List<String> hashValues,
        List<String> signatures,
        List<String> logs,
        List<Long> timestamps,
        List<String> signers,
        List<String> customKeys,
        String privateKey
    );

    ResponseData<EvidenceInfo> getInfo(String evidenceAddress);

    ResponseData<EvidenceInfo> getInfoByCustomKey(String extraKey);

    ResponseData<Boolean> setAttribute(
        String hashValue,
        String key,
        String value,
        Long timestamp,
        String privateKey
    );

    ResponseData<Boolean> revoke(
            String hashValue,
            Boolean revokeStage,
            Long timestamp,
            String privateKey
    );

}

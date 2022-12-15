

package com.webank.weid.blockchain.rpc;

import com.webank.weid.blockchain.protocol.base.EvidenceInfo;
import com.webank.weid.blockchain.protocol.response.ResponseData;

import java.util.List;

/**
 * Service inf for operations on Evidence for Credentials.
 *
 * @author chaoxinhu 2019.1
 */
public interface EvidenceService {

    /**
     * Revoke an evidence - which can be un-revoked.
     *
     * @param hash the hash
     * @param revokeStage the revokeStage
     * @param timestamp the timestamp
     * @param privateKey the weid privateKey
     * @return true if yes, false otherwise, with error codes
     */
    ResponseData<Boolean> revoke(String hash, Boolean revokeStage, Long timestamp, String privateKey);

    /**
     * Get the evidence info from blockchain using hash as key.
     *
     * @param hashValue the hash, on chain
     * @return The EvidenceInfo
     */
    ResponseData<EvidenceInfo> getInfo(String hashValue);

    /**
     * Get the evidence info from blockchain using custom key.
     *
     * @param customKey the custom key, on chain
     * @return The EvidenceInfo
     */
    ResponseData<EvidenceInfo> getInfoByCustomKey(String customKey);

    /**
     * A direct pass-thru method to create raw evidence where all inputs can be customized.
     *
     * @param hashValue the hash value
     * @param signature the signature value
     * @param extra the extra value
     * @param timestamp the timestamp
     * @param privateKey the private key
     * @return true if yes, false otherwise
     */
    ResponseData<String> createEvidence(
            String hashValue,
            String signature,
            String extra,
            Long timestamp,
            String privateKey
    );

    /**
     * A direct pass-thru method to create raw evidence where all inputs can be customized.
     *
     * @param hashValue the hash value
     * @param signature the signature value
     * @param log the log
     * @param timestamp the timestamp
     * @param extraKey the extra data
     * @param privateKey the private key
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> createEvidenceWithCustomKey(
        String hashValue,
        String signature,
        String log,
        Long timestamp,
        String extraKey,
        String privateKey
    );

    /**
     * A direct pass-thru method to create raw evidence where all inputs can be customized.
     *
     * @param hashValues a list of hash value
     * @param signatures a list of signature value
     * @param logs a list of log
     * @param timestamps a list of timestamp
     * @param signers a list of extra signer
     * @param privateKey the private key
     * @return true if yes, false otherwise
     */
    ResponseData<List<Boolean>> batchCreateEvidence(
            List<String> hashValues,
            List<String> signatures,
            List<String> logs,
            List<Long> timestamps,
            List<String> signers,
            String privateKey
    );

    /**
     * A direct pass-thru method to create raw evidence where all inputs can be customized.
     *
     * @param hashValues a list of hash value
     * @param signatures a list of signature value
     * @param logs a list of log
     * @param timestamps a list of timestamp
     * @param signers a list of extra signer
     * @param extraKeys a list of extra extraKey
     * @param privateKey the private key
     * @return true if yes, false otherwise
     */

    ResponseData<List<Boolean>> batchCreateEvidenceWithCustomKey(
            List<String> hashValues,
            List<String> signatures,
            List<String> logs,
            List<Long> timestamps,
            List<String> signers,
            List<String> extraKeys,
            String privateKey
    );

    /**
     * A direct pass-thru method to create raw evidence where all inputs can be customized.
     *
     * @param hashValue the hash value
     * @param signature the signature value
     * @param log the log
     * @param timestamp the timestamp
     * @param privateKey the private key
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> addLog(
            String hashValue,
            String signature,
            String log,
            Long timestamp,
            String privateKey
    );

    /**
     * Get the hash info from blockchain using custom key.
     *
     * @param customKey the custom key, on chain
     * @return The EvidenceInfo
     */
    ResponseData<String> getHashByCustomKey(String customKey);

    /**
     * A direct pass-thru method to create raw evidence where all inputs can be customized.
     *
     * @param hashValue the hash value
     * @param signature the signature value
     * @param log the log
     * @param timestamp the timestamp
     * @param customKey the customKey
     * @param privateKey the private key
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> addLogByCustomKey(
            String hashValue,
            String signature,
            String log,
            Long timestamp,
            String customKey,
            String privateKey
    );
}

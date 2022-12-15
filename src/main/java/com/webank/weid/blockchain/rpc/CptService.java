

package com.webank.weid.blockchain.rpc;

import java.util.List;

import com.webank.weid.blockchain.protocol.base.Cpt;
import com.webank.weid.blockchain.protocol.base.CptBaseInfo;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.protocol.response.RsvSignature;
import com.webank.wedpr.selectivedisclosure.CredentialTemplateEntity;

/**
 * Service inf for operation on CPT (Claim protocol Type).
 *
 * @author lingfenghe
 */
public interface CptService {

    /**
     * Register a new CPT to the blockchain.
     *
     * @param address the address of creator
     * @param cptJsonSchemaNew the new cptJsonSchema
     * @param rsvSignature the rsvSignature of cptJsonSchema
     * @param privateKey the decimal privateKey of creator
     * @return The registered CPT info
     */
    ResponseData<CptBaseInfo> registerCpt(String address,
                                          String cptJsonSchemaNew,
                                          RsvSignature rsvSignature,
                                          String privateKey);

    /**
     * Register a new CPT with a pre-set CPT ID, to the blockchain.
     *
     * @param address the address of creator
     * @param cptJsonSchemaNew the new cptJsonSchema
     * @param rsvSignature the rsvSignature of cptJsonSchema
     * @param privateKey the decimal privateKey of creator
     * @param cptId the CPT ID
     * @return The registered CPT info
     */
    ResponseData<CptBaseInfo> registerCpt(String address,
                                          String cptJsonSchemaNew,
                                          RsvSignature rsvSignature,
                                          String privateKey,
                                          Integer cptId);

    /**
     * Query the latest CPT version.
     *
     * @param cptId the cpt id
     * @return The registered CPT info
     */
    ResponseData<Cpt> queryCpt(Integer cptId);

    /**
     * Query the latest CPT version.
     *
     * @param cptId the cpt id
     * @param credentialPublicKey the publicKey of credential
     * @param credentialKeyCorrectnessProof the proof of publicKey
     * @return Whether the CredentialTemplate has been put into blockchain
     */
    ResponseData<Boolean> putCredentialTemplate(Integer cptId, String credentialPublicKey, String credentialKeyCorrectnessProof);

    /**
     * Update the data fields of a registered CPT.
     *
     * @param address the address of creator
     * @param cptJsonSchemaNew the new cptJsonSchema
     * @param rsvSignature the rsvSignature of cptJsonSchema
     * @param privateKey the decimal privateKey of creator
     * @param cptId the CPT ID
     * @return The updated CPT info
     */
    ResponseData<CptBaseInfo> updateCpt(String address,
                                        String cptJsonSchemaNew,
                                        RsvSignature rsvSignature,
                                        String privateKey,
                                        Integer cptId);

    /**
     * Update the data fields of a registered CPT.
     *
     * @param cptId the cpt id
     * @return The updated CPT info
     */
    ResponseData<CredentialTemplateEntity> queryCredentialTemplate(Integer cptId);
    
    /**
     * Get CPTIDS from chain.
     *
     * @param startPos start position
     * @param num batch number
     * @return CPTID list
     */
    ResponseData<List<Integer>> getCptIdList(Integer startPos, Integer num);
    
    /**
     * Get CPT count.
     *
     * @return the cpt count
     */
    ResponseData<Integer> getCptCount();
}

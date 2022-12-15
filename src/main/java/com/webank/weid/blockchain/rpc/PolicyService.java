

package com.webank.weid.blockchain.rpc;

import com.webank.weid.blockchain.protocol.base.ClaimPolicy;
import com.webank.weid.blockchain.protocol.base.PresentationPolicyE;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.protocol.response.RsvSignature;

import java.util.List;

/**
 * Service inf for operation on Policy on blockchain (Claim protocol Type).
 *
 * @author junqizhang 2020.8
 */
public interface PolicyService {

    /**
     * Put Claim Policy List on blockchain under a CPT ID.
     *
     * @param cptId CPT ID
     * @param policies Policy list
     * @param privateKey privateKey of cpt issuer
     * @return claimPolicyId the Claim policy ID on-chain
     */
    ResponseData<Integer> putPolicyIntoCpt(Integer cptId, List<Integer> policies,
                                           String privateKey);

    /**
     * Register Claim Policy on blockchain.
     *
     * @param address address of issuer
     * @param cptJsonSchemaNew cptJsonSchema
     * @param rsvSignature signature of issuer
     * @param privateKey privateKey of issuer
     * @return claimPolicyId the Claim policy ID on-chain
     */
    public ResponseData<Integer> registerPolicyData(
            String address,
            String cptJsonSchemaNew,
            RsvSignature rsvSignature,
            String privateKey);

    /**
     * Get Claim Policy Json from blockchain given a policy ID.
     *
     * @param policyId the Claim Policy ID on-chain
     * @return the claim Json
     */
    ResponseData<String> getClaimPolicy(Integer policyId);

    /**
     * Get all claim policies from this CPT ID.
     *
     * @param cptId cpt id
     * @return claim policies list
     */
    ResponseData<List<Integer>> getClaimPoliciesFromCpt(Integer cptId);

    /**
     * Register Presentation Policy which contains a number of claim policies.
     *
     * @param claimPolicyIdList claim policies list
     * @param privateKey privateKey of weid
     * @return the presentation policy id
     */
    ResponseData<Integer> registerPresentationPolicy(List<Integer> claimPolicyIdList,
        String privateKey);

    /**
     * Get Presentation policies under this id from chain.
     *
     * @param presentationPolicyId presentation policy id
     * @return the full presentation policy
     */
    ResponseData<PresentationPolicyE> getPresentationPolicy(Integer presentationPolicyId);

    /**
     * Get all claim policies from chain.
     *
     * @param startPos start position
     * @param num batch number
     * @return claim policy list
     */
    ResponseData<List<Integer>> getAllClaimPolicies(Integer startPos, Integer num);
    
    /**
     * Get Policy count.
     *
     * @return the Policy count
     */
    ResponseData<Integer> getPolicyCount();
}

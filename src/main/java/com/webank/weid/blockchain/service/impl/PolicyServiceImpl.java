package com.webank.weid.blockchain.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.protocol.base.Cpt;
import com.webank.weid.blockchain.protocol.base.CptBaseInfo;
import com.webank.weid.blockchain.protocol.base.PresentationPolicyE;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.protocol.response.RsvSignature;
import com.webank.weid.blockchain.util.DataToolUtils;
import com.webank.weid.blockchain.rpc.PolicyService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.constant.WeIdConstant;
import org.springframework.stereotype.Component;

/**
 * Service implementations for operations on Evidence.
 *
 * @author afeexian 2022.8
 */
@Component("blockchain")
public class PolicyServiceImpl extends AbstractService implements PolicyService {

    private static final Logger logger = LoggerFactory.getLogger(PolicyServiceImpl.class);

    /**
     * Put Claim Policy List on blockchain under a CPT ID.
     *
     * @param cptId CPT ID
     * @param policies Policy list
     * @param privateKey privateKey of cpt issuer
     * @return claimPolicyId the Claim policy ID on-chain
     */
    @Override
    public ResponseData<Integer> putPolicyIntoCpt(Integer cptId, List<Integer> policies,
                                                  String privateKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            //CptBaseInfo cptBaseInfo;
            ResponseData<Integer> addResp = cptServiceEngineFisco
                    .putPolicyIntoCpt(cptId, policies, privateKey);
            if (addResp.getErrorCode() != ErrorCode.SUCCESS.getCode()) {
                logger.error("Failed to add this policy ID {} into existing CPT ID's list: {}",
                        policies, cptId);
                return addResp;
            }
            return new ResponseData<>(addResp.getResult(), ErrorCode.SUCCESS);
        }
        return new ResponseData<>(-1, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Register Claim Policy on blockchain.
     *
     * @param address address of issuer
     * @param cptJsonSchemaNew cptJsonSchema
     * @param rsvSignature signature of issuer
     * @param privateKey privateKey of issuer
     * @return claimPolicyId the Claim policy ID on-chain
     */
    @Override
    public ResponseData<Integer> registerPolicyData(
            String address,
            String cptJsonSchemaNew,
            RsvSignature rsvSignature,
            String privateKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            CptBaseInfo cptBaseInfo;
            try {
                cptBaseInfo = cptServiceEngineFisco.registerCpt(address, cptJsonSchemaNew, rsvSignature,
                        privateKey, WeIdConstant.POLICY_DATA_INDEX).getResult();
            } catch (Exception e) {
                logger.error("[register policy] register failed due to unknown error. ", e);
                return new ResponseData<>(-1, ErrorCode.UNKNOW_ERROR.getCode(),
                        ErrorCode.UNKNOW_ERROR.getCodeDesc() + e.getMessage());
            }
            if (cptBaseInfo != null && cptBaseInfo.getCptId() > 0) {
                return new ResponseData<>(cptBaseInfo.getCptId(), ErrorCode.SUCCESS);
            } else {
                return new ResponseData<>(-1, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(-1, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get Claim Policy Json from blockchain given a policy ID.
     *
     * @param policyId the Claim Policy ID on-chain
     * @return the claim Json
     */
    @Override
    public ResponseData<String> getClaimPolicy(Integer policyId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            ResponseData<Cpt> policyResp = cptServiceEngineFisco
                    .queryCpt(policyId, WeIdConstant.POLICY_DATA_INDEX);
            if (policyResp.getResult() == null) {
                return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CPT_NOT_EXISTS.getCode(),
                        ErrorCode.CPT_NOT_EXISTS.getCodeDesc() + policyResp.getErrorMessage());
            }
            String claimPolicy = DataToolUtils.serialize(policyResp.getResult().getCptJsonSchema());
            /*claimPolicy.setFieldsToBeDisclosed(
                    DataToolUtils.serialize(policyResp.getResult().getCptJsonSchema()));*/
            return new ResponseData<>(claimPolicy, ErrorCode.SUCCESS);
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get all claim policies from this CPT ID.
     *
     * @param cptId cpt id
     * @return claim policies list
     */
    @Override
    public ResponseData<List<Integer>> getClaimPoliciesFromCpt(Integer cptId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.getPolicyFromCpt(cptId);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Register Presentation Policy which contains a number of claim policies.
     *
     * @param claimPolicyIdList claim policies list
     * @param privateKey privateKey of weid
     * @return the presentation policy id
     */
    @Override
    public ResponseData<Integer> registerPresentationPolicy(List<Integer> claimPolicyIdList,
        String privateKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco
                    .putPolicyIntoPresentation(claimPolicyIdList, privateKey);
        }
        return new ResponseData<>(-1, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get Presentation policies under this id from chain.
     *
     * @param presentationPolicyId presentation policy id
     * @return the full presentation policy
     */
    @Override
    public ResponseData<PresentationPolicyE> getPresentationPolicy(Integer presentationPolicyId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            PresentationPolicyE presentationPolicy = cptServiceEngineFisco
                    .getPolicyFromPresentation(presentationPolicyId)
                    .getResult();
            if (presentationPolicy == null) {
                return new ResponseData<>(null, ErrorCode.CREDENTIAL_CLAIM_POLICY_NOT_EXIST);
            }
            Map<Integer, String> policyMap = new HashMap<>();
            for (Map.Entry<Integer, String> entry : presentationPolicy.getPolicy().entrySet()) {
                policyMap.put(entry.getKey(), getClaimPolicy(entry.getKey()).getResult());
            }
            presentationPolicy.setPolicy(policyMap);
            return new ResponseData<>(presentationPolicy, ErrorCode.SUCCESS);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }


    /**
     * Get all claim policies from chain.
     *
     * @param startPos start position
     * @param num batch number
     * @return claim policy list
     */
    @Override
    public ResponseData<List<Integer>> getAllClaimPolicies(Integer startPos, Integer num) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.getCptIdList(startPos, num, WeIdConstant.POLICY_DATA_INDEX);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getPolicyCount() {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.getCptCount(WeIdConstant.POLICY_DATA_INDEX);
        }
        return new ResponseData<>(-1, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }
}

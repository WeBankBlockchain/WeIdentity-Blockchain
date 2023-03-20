

package com.webank.weid.blockchain.service.impl;

import java.util.List;

import com.webank.weid.blockchain.protocol.base.Cpt;
import com.webank.weid.blockchain.protocol.base.CptBaseInfo;
import com.webank.wedpr.selectivedisclosure.CredentialTemplateEntity;
import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.protocol.response.RsvSignature;
import com.webank.weid.blockchain.rpc.CptService;
import org.springframework.stereotype.Component;

/**
 * Service implementation for operation on CPT (Claim Protocol Type).
 *
 * @author afeexian
 */
@Component("blockchain")
public class CptServiceImpl extends AbstractService implements CptService {

    private static final Logger logger = LoggerFactory.getLogger(CptServiceImpl.class);

    /**
     * Register a new CPT with a pre-set CPT ID, to the blockchain.
     *
     * @param address the address of creator
     * @param cptJsonSchemaNew the new cptJsonSchema
     * @param rsvSignature the rsvSignature of cptJsonSchema
     * @param privateKey the decimal privateKey of creator
     * @param cptId the CPT ID
     * @return response data
     */
    public ResponseData<CptBaseInfo> registerCpt(
            String address,
            String cptJsonSchemaNew,
            RsvSignature rsvSignature,
            String privateKey,
            Integer cptId) {
        if (StringUtils.isEmpty(address) || StringUtils.isEmpty(cptJsonSchemaNew) || StringUtils.isEmpty(privateKey)) {
            logger.error("[registerCpt] input argument is illegal");
            return new ResponseData<>(null, ErrorCode.ILLEGAL_INPUT);
        }
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return cptServiceEngineFisco.registerCpt(cptId, address, cptJsonSchemaNew, rsvSignature,
                        privateKey, WeIdConstant.CPT_DATA_INDEX);
            } catch (Exception e) {
                logger.error("[registerCpt] register cpt failed due to unknown error. ", e);
                return new ResponseData<>(null, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * This is used to register a new CPT to the blockchain.
     *
     * @param address the address of creator
     * @param cptJsonSchemaNew the new cptJsonSchema
     * @param rsvSignature the rsvSignature of cptJsonSchema
     * @param privateKey the decimal privateKey of creator
     * @return the response data
     */
    public ResponseData<CptBaseInfo> registerCpt(String address,
                                                 String cptJsonSchemaNew,
                                                 RsvSignature rsvSignature,
                                                 String privateKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return cptServiceEngineFisco.registerCpt(address, cptJsonSchemaNew, rsvSignature,
                        privateKey, WeIdConstant.CPT_DATA_INDEX);
            } catch (Exception e) {
                logger.error("[registerCpt] register cpt failed due to unknown error. ", e);
                return new ResponseData<>(null, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * this is used to query cpt with the latest version which has been registered.
     *
     * @param cptId the cpt id
     * @return the response data
     */
    public ResponseData<Cpt> queryCpt(Integer cptId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return cptServiceEngineFisco.queryCpt(cptId, WeIdConstant.CPT_DATA_INDEX);
            } catch (Exception e) {
                logger.error("[updateCpt] query cpt failed due to unknown error. ", e);
                return new ResponseData<>(null, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * This is used to update a CPT data which has been register.
     *
     * @param address the address of creator
     * @param cptJsonSchemaNew the new cptJsonSchema
     * @param rsvSignature the rsvSignature of cptJsonSchema
     * @param privateKey the decimal privateKey of creator
     * @param cptId the CPT ID
     * @return the response data
     */
    public ResponseData<CptBaseInfo> updateCpt(String address,
                                               String cptJsonSchemaNew,
                                               RsvSignature rsvSignature,
                                               String privateKey,
                                               Integer cptId) {
        if (StringUtils.isEmpty(address) || StringUtils.isEmpty(cptJsonSchemaNew) || StringUtils.isEmpty(privateKey)) {
            logger.error("[registerCpt] input argument is illegal");
            return new ResponseData<>(null, ErrorCode.ILLEGAL_INPUT);
        }
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return cptServiceEngineFisco.updateCpt(cptId, address, cptJsonSchemaNew, rsvSignature,
                        privateKey, WeIdConstant.CPT_DATA_INDEX);
            } catch (Exception e) {
                logger.error("[updateCpt] update cpt failed due to unkown error. ", e);
                return new ResponseData<>(null, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }


    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.rpc.CptService#queryCredentialTemplate(java.lang.Integer)
     */
    @Override
    public ResponseData<CredentialTemplateEntity> queryCredentialTemplate(Integer cptId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.queryCredentialTemplate(cptId);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.rpc.CptService#putCredentialTemplate(java.lang.Integer, java.lang.String, java.lang.String)
     */
    @Override
    public ResponseData<Boolean> putCredentialTemplate(Integer cptId, String credentialPublicKey, String credentialKeyCorrectnessProof) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.putCredentialTemplate(cptId, credentialPublicKey, credentialKeyCorrectnessProof);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<List<Integer>> getCptIdList(Integer startPos, Integer num) {
        if (startPos < 0 || num < 1) {
            return new ResponseData<>(null, ErrorCode.ILLEGAL_INPUT);
        }
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.getCptIdList(startPos, num, WeIdConstant.CPT_DATA_INDEX);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getCptCount() {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return cptServiceEngineFisco.getCptCount(WeIdConstant.CPT_DATA_INDEX);
        }
        return new ResponseData<>(-1, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }
}

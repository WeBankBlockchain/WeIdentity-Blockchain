

package com.webank.weid.blockchain.service.impl;

import com.webank.weid.blockchain.exception.LoadContractException;
import com.webank.weid.blockchain.exception.PrivateKeyIllegalException;
import com.webank.weid.blockchain.protocol.base.WeIdDocument;
import com.webank.weid.blockchain.protocol.base.WeIdDocumentMetadata;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.rpc.WeIdService;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service implementations for operations on WeIdentity DID.
 *
 * @author afeexian 2022.08
 */
@Component("blockchain")
public class WeIdServiceImpl extends AbstractService implements WeIdService {

    /**
     * log4j object, for recording log.
     */
    private static final Logger logger = LoggerFactory.getLogger(WeIdServiceImpl.class);

    public ResponseData<Boolean> createWeId(
            String address,
            List<String> authList,
            List<String> serviceList,
            String privateKey) {

        try {
            if(authList.size()==0 || serviceList.size()==0){
                return new ResponseData<>(false, ErrorCode.ILLEGAL_INPUT);
            }
            //V2和V3都统一从这里进入
            if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
                return weIdServiceEngineFisco.createWeId(address, authList, serviceList, privateKey);
            }
            return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
        } catch (PrivateKeyIllegalException e) {
            logger.error("[createWeId] create weid failed because privateKey is illegal. ",
                    e);
            return new ResponseData<>(false, e.getErrorCode());
        } catch (LoadContractException e) {
            logger.error("[createWeId] create weid failed because Load Contract with "
                            + "exception. ",
                    e);
            return new ResponseData<>(false, e.getErrorCode());
        } catch (Exception e) {
            logger.error("[createWeId] create weid failed with exception. ", e);
            return new ResponseData<>(false, ErrorCode.UNKNOW_ERROR);
        }
    }

    /**
     * Get a WeIdentity DID Document.
     *
     * @param weId the WeIdentity DID
     * @return the WeIdentity DID document
     */
    @Override
    public ResponseData<WeIdDocument> getWeIdDocument(String weId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            ResponseData<WeIdDocument> weIdDocResp = weIdServiceEngineFisco.getWeIdDocument(weId);
            return weIdDocResp;
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get a WeIdentity DID Document Metadata.
     *
     * @param weId the WeIdentity DID
     * @return the WeIdentity DID document
     */
    @Override
    public ResponseData<WeIdDocumentMetadata> getWeIdDocumentMetadata(String weId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            ResponseData<WeIdDocumentMetadata> weIdDocResp = weIdServiceEngineFisco.getWeIdDocumentMetadata(weId);
            return weIdDocResp;
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Check if WeIdentity DID exists on Chain.
     *
     * @param weId the WeIdentity DID
     * @return true if exists, false otherwise
     */
    @Override
    public ResponseData<Boolean> isWeIdExist(String weId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return weIdServiceEngineFisco.isWeIdExist(weId);
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Check if WeIdentity DID is deactivated on Chain.
     *
     * @param weId the WeIdentity DID
     * @return true if is deactivated, false otherwise
     */
    @Override
    public ResponseData<Boolean> isDeactivated(String weId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return weIdServiceEngineFisco.isDeactivated(weId);
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * call weid contract to update the weid document.
     *
     * @param weIdDocument weIdDocument on blockchain
     * @param address address of the identity
     * @param privateKey privateKey identity's private key
     * @return result
     */
    @Override
    public ResponseData<Boolean> updateWeId(
            WeIdDocument weIdDocument,
            String privateKey,
            String address) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                return weIdServiceEngineFisco
                        .updateWeId(weIdDocument,
                                address,
                                privateKey);
            } catch (PrivateKeyIllegalException e) {
                logger.error("[updateWeId] updateWeId failed because privateKey is illegal. ",
                                e);
                return new ResponseData<>(false, e.getErrorCode());
            } catch (Exception e) {
                logger.error("[updateWeId] updateWeId failed. Error message :{}", e);
                return new ResponseData<>(false, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<List<String>> getWeIdList(
            Integer first,
            Integer last
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            try {
                logger.info("[getWeIdList] begin get weIdList, first index = {}, last index = {}",
                        first,
                        last
                );
                return weIdServiceEngineFisco.getWeIdList(first, last);
            } catch (Exception e) {
                logger.error("[getWeIdList] get weIdList failed with exception. ", e);
                return new ResponseData<>(null, ErrorCode.UNKNOW_ERROR);
            }
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getWeIdCount() {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return weIdServiceEngineFisco.getWeIdCount();
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

}

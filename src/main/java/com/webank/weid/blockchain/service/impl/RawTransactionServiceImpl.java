

package com.webank.weid.blockchain.service.impl;

import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.rpc.RawTransactionService;
import org.springframework.stereotype.Component;

/**
 * Service interface for operations on direct transactions on blockchain.
 *
 * @author afeexian 2022.8
 */
@Component("blockchain")
public class RawTransactionServiceImpl extends AbstractService implements RawTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(RawTransactionServiceImpl.class);

    /**
     * Create a WeIdentity DID from the provided public key, with preset transaction hex value.
     *
     * @param transactionHex the transaction hex value
     * @return Error message if any
     */
    @Override
    public ResponseData<String> createWeId(String transactionHex) {

        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return rawEngineFisco.createWeId(transactionHex);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Register a new Authority Issuer on Chain with preset transaction hex value. The inputParam is
     * a Json String, with two keys: WeIdentity DID and Name. Parameters will be ordered as
     * mentioned after validity check; then transactionHex will be sent to blockchain.
     *
     * @param transactionHex the transaction hex value
     * @return true if succeeds, false otherwise
     */
    @Override
    public ResponseData<String> registerAuthorityIssuer(String transactionHex) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return rawEngineFisco.registerAuthorityIssuer(transactionHex);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }


    /**
     * Register a new CPT to the blockchain with preset transaction hex value.
     *
     * @param transactionHex the transaction hex value
     * @return The registered CPT info
     */
    public ResponseData<String> registerCpt(String transactionHex) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return rawEngineFisco.registerCpt(transactionHex);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }
}
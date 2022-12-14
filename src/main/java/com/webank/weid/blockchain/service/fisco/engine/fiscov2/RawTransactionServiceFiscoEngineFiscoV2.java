

package com.webank.weid.blockchain.service.fisco.engine.fiscov2;

import java.util.List;
import java.util.Optional;

import com.webank.weid.blockchain.exception.WeIdBaseException;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.protocol.response.TransactionInfo;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.blockchain.service.fisco.engine.BaseEngineFisco;
import com.webank.weid.blockchain.service.fisco.engine.RawTransactionServiceEngineFisco;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.client.protocol.response.BcosTransactionReceipt;
import org.fisco.bcos.sdk.client.protocol.response.SendTransaction;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.contract.v2.AuthorityIssuerController;
import com.webank.weid.contract.v2.AuthorityIssuerController.AuthorityIssuerRetLogEventResponse;
import com.webank.weid.contract.v2.CptController;
import com.webank.weid.contract.v2.CptController.RegisterCptRetLogEventResponse;
import com.webank.weid.contract.v2.WeIdContract;
import com.webank.weid.blockchain.protocol.base.CptBaseInfo;
import com.webank.weid.blockchain.util.TransactionUtils;

/**
 * RawTransactionService runs on FISCO BCOS 2.0.
 *
 * @author tonychen 2019年6月26日
 */
public class RawTransactionServiceFiscoEngineFiscoV2 extends BaseEngineFisco implements
        RawTransactionServiceEngineFisco {

    private static final Logger logger = LoggerFactory
        .getLogger(RawTransactionServiceFiscoEngineFiscoV2.class);

    /**
     * WeIdentity DID contract object, for calling WeId contract.
     */
    private static WeIdContract weIdContract;

    /**
     * AuthorityIssuer contract object, for calling AuthorityIssuer contract.
     */
    private static AuthorityIssuerController authorityIssuerController;

    /**
     * cpt contract object, for calling cpt contract.
     */
    private static CptController cptController;
    
    /**
     * 构造函数.
     */
    public RawTransactionServiceFiscoEngineFiscoV2() {
        if (weIdContract == null || authorityIssuerController == null || cptController == null) {
            reload(); 
        }
    }
    
    /**
     * 重新加载静态合约对象.
     */
    public void reload() {
        weIdContract = getContractService(BaseServiceFisco.fiscoConfig.getWeIdAddress(), WeIdContract.class);
        authorityIssuerController = getContractService(BaseServiceFisco.fiscoConfig.getIssuerAddress(),
            AuthorityIssuerController.class); 
        cptController = getContractService(BaseServiceFisco.fiscoConfig.getCptAddress(), CptController.class);
    }

    /**
     * Send a transaction to blockchain through web3j instance using the transactionHex value.
     *
     * @param transactionHex the transactionHex value
     * @return the transactionReceipt
     * @throws Exception the exception
     */
    public static TransactionReceipt sendTransaction(String transactionHex)
        throws Exception {

        Client client = (Client) BaseServiceFisco.getClient();
        SendTransaction ethSendTransaction = client.sendRawTransaction(transactionHex);
        if (ethSendTransaction.hasError()) {
            logger.error("Error processing transaction request: "
                + ethSendTransaction.getError().getMessage());
            return null;
        }
        Optional<TransactionReceipt> receiptOptional =
            getTransactionReceiptRequest(client, ethSendTransaction.getTransactionHash());
        int sumTime = 0;
        try {
            for (int i = 0; i < WeIdConstant.POLL_TRANSACTION_ATTEMPTS; i++) {
                if (!receiptOptional.isPresent()) {
                    Thread.sleep((long) WeIdConstant.POLL_TRANSACTION_SLEEP_DURATION);
                    sumTime += WeIdConstant.POLL_TRANSACTION_SLEEP_DURATION;
                    receiptOptional = getTransactionReceiptRequest(client,
                        ethSendTransaction.getTransactionHash());
                } else {
                    return receiptOptional.get();
                }
            }
        } catch (Exception e) {
            throw new WeIdBaseException("Transaction receipt was not generated after "
                + ((sumTime) / 1000
                + " seconds for transaction: " + ethSendTransaction));
        }
        return null;
    }

    /**
     * Get a TransactionReceipt request from a transaction Hash.
     *
     * @param client the client instance to blockchain
     * @param transactionHash the transactionHash value
     * @return the transactionReceipt wrapper
     * @throws Exception the exception
     */
    private static Optional<TransactionReceipt> getTransactionReceiptRequest(Client client,
        String transactionHash) throws Exception {
        BcosTransactionReceipt transactionReceipt =
            client.getTransactionReceipt(transactionHash);
        if (transactionReceipt.hasError()) {
            logger.error("Error processing transaction request: "
                + transactionReceipt.getError().getMessage());
            return Optional.empty();
        }
        return transactionReceipt.getTransactionReceipt();
    }

    /**
     * Verify Authority Issuer related events.
     *
     * @param event the Event
     * @param opcode the Opcode
     * @return the ErrorCode
     */
    public static ErrorCode verifyAuthorityIssuerRelatedEvent(
        AuthorityIssuerRetLogEventResponse event,
        Integer opcode) {
        if (event == null) {
            return ErrorCode.ILLEGAL_INPUT;
        }
        if (event.addr == null || event.operation == null || event.retCode == null) {
            return ErrorCode.ILLEGAL_INPUT;
        }

        Integer eventOpcode = event.operation.intValue();
        if (eventOpcode.equals(opcode)) {
            Integer eventRetCode = event.retCode.intValue();
            return ErrorCode.getTypeByErrorCode(eventRetCode);
        } else {
            return ErrorCode.AUTHORITY_ISSUER_OPCODE_MISMATCH;
        }
    }

    /**
     * Verify Register CPT related events.
     *
     * @param transactionReceipt the TransactionReceipt
     * @param cptController cptController contract address
     * @return the ErrorCode
     */
    public static ResponseData<CptBaseInfo> resolveRegisterCptEvents(
        TransactionReceipt transactionReceipt,
        CptController cptController) {
        List<RegisterCptRetLogEventResponse> event = cptController.getRegisterCptRetLogEvents(
            transactionReceipt
        );

        if (CollectionUtils.isEmpty(event)) {
            logger.error("[registerCpt] event is empty");
            return new ResponseData<>(null, ErrorCode.CPT_EVENT_LOG_NULL);
        }

        return TransactionUtils.getResultByResolveEvent(
            event.get(0).retCode,
            event.get(0).cptId,
            event.get(0).cptVersion,
            transactionReceipt
        );
    }

    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.service.fisco.engine.RawTransactionServiceEngine
     * #createWeId(java.lang.String)
     */
    @Override
    public ResponseData<String> createWeId(String transactionHex) {
        try {
            TransactionReceipt transactionReceipt = sendTransaction(transactionHex);
            List<WeIdContract.CreateWeIdEventResponse> response =
                weIdContract.getCreateWeIdEvents(transactionReceipt);
            TransactionInfo info = new TransactionInfo(transactionReceipt);
            if (!CollectionUtils.isEmpty(response)) {
                return new ResponseData<>(Boolean.TRUE.toString(), ErrorCode.SUCCESS, info);
            }
        } catch (Exception e) {
            logger.error("[createWeId] create failed due to unknown transaction error. ", e);
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.TRANSACTION_EXECUTE_ERROR);
    }

    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.service.fisco.engine.RawTransactionServiceEngine
     * #registerAuthorityIssuer(java.lang.String)
     */
    @Override
    public ResponseData<String> registerAuthorityIssuer(String transactionHex) {
        try {
            TransactionReceipt transactionReceipt = sendTransaction(transactionHex);

            List<AuthorityIssuerRetLogEventResponse> eventList =
                authorityIssuerController.getAuthorityIssuerRetLogEvents(transactionReceipt);
            AuthorityIssuerRetLogEventResponse event = eventList.get(0);
            TransactionInfo info = new TransactionInfo(transactionReceipt);
            ErrorCode errorCode = verifyAuthorityIssuerRelatedEvent(event,
                WeIdConstant.ADD_AUTHORITY_ISSUER_OPCODE);
            Boolean result = errorCode.getCode() == ErrorCode.SUCCESS.getCode();
            return new ResponseData<>(result.toString(), errorCode, info);
        } catch (Exception e) {
            logger.error("[registerAuthorityIssuer] register failed due to transaction error.", e);
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.TRANSACTION_EXECUTE_ERROR);
    }

    /* (non-Javadoc)
     * @see com.webank.weid.blockchain.service.fisco.engine.RawTransactionServiceEngine
     * #registerCpt(java.lang.String)
     */
    @Override
    public ResponseData<String> registerCpt(String transactionHex) {
        try {
            TransactionReceipt transactionReceipt = sendTransaction(transactionHex);
            CptBaseInfo cptBaseInfo =
                resolveRegisterCptEvents(
                    transactionReceipt,
                    cptController
                ).getResult();

            TransactionInfo info = new TransactionInfo(transactionReceipt);
            if (cptBaseInfo != null) {
                return new ResponseData<>(DataToolUtils.objToJsonStrWithNoPretty(cptBaseInfo),
                    ErrorCode.SUCCESS, info);
            }
        } catch (Exception e) {
            logger.error("[registerCpt] register failed due to unknown transaction error. ", e);
        }
        return new ResponseData<>(StringUtils.EMPTY, ErrorCode.TRANSACTION_EXECUTE_ERROR);
    }

}

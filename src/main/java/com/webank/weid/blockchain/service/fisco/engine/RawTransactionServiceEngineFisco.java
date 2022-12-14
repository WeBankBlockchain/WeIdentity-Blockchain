

package com.webank.weid.blockchain.service.fisco.engine;

import com.webank.weid.blockchain.protocol.response.ResponseData;

/**
 * for rest service.
 *
 * @author tonychen 2019年6月26日
 */
public interface RawTransactionServiceEngineFisco extends ReloadStaticContract {

    /**
     * create a weid.
     *
     * @param transactionHex transactionHex
     * @return result
     */
    ResponseData<String> createWeId(String transactionHex);

    /**
     * register authority issuer.
     *
     * @param transactionHex transactionHex
     * @return result
     */
    ResponseData<String> registerAuthorityIssuer(String transactionHex);

    /**
     * register cpt.
     *
     * @param transactionHex transactionHex
     * @return result
     */
    ResponseData<String> registerCpt(String transactionHex);

}

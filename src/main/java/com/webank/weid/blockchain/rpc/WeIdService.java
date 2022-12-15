

package com.webank.weid.blockchain.rpc;

import java.util.List;

import com.webank.weid.blockchain.protocol.base.WeIdDocument;
import com.webank.weid.blockchain.protocol.base.WeIdDocumentMetadata;
import com.webank.weid.blockchain.protocol.response.ResponseData;


/**
 * Service inf for operations on WeIdentity DID.
 *
 * @author tonychen
 */
public interface WeIdService {

    /**
     * Create a WeIdentity DID without a keypair. SDK will generate a keypair for the caller.
     * @param address address of the identity
     * @param authList authentication info
     * @param serviceList sevice info
     * @param privateKey privateKey of transaction maker
     * @return a data set including a WeIdentity DID and a keypair
     */
    ResponseData<Boolean> createWeId(String address, List<String> authList,
                                     List<String> serviceList, String privateKey);

    /**
     * Query WeIdentity DID document.
     *
     * @param weId the WeIdentity DID
     * @return weId document in java object type
     */
    ResponseData<WeIdDocument> getWeIdDocument(String weId);

    /**
     * Query WeIdentity DID document metadata.
     *
     * @param weId the WeIdentity DID
     * @return weId document metadata in java object type
     */
    ResponseData<WeIdDocumentMetadata> getWeIdDocumentMetadata(String weId);

    /**
     * call weid contract to update the weid document.
     *
     * @param weIdDocument weIdDocument on blockchain
     * @param weAddress address of the identity
     * @param privateKey privateKey identity's private key
     * @return result
     */
    ResponseData<Boolean> updateWeId(
            WeIdDocument weIdDocument,
            String weAddress,
            String privateKey
    );

    /**
     * Check if the WeIdentity DID exists on chain.
     *
     * @param weId The WeIdentity DID.
     * @return true if exists, false otherwise.
     */
    ResponseData<Boolean> isWeIdExist(String weId);

    /**
     * Check if the WeIdentity DID is deactivated on chain.
     *
     * @param weId The WeIdentity DID.
     * @return true if is deactivated, false otherwise.
     */
    ResponseData<Boolean> isDeactivated(String weId);

    /**
     * query data according to block height, index location and search direction.
     * 
     * @param first the first index of weid in contract
     * @param last the last index of weid in contract
     * @return return the WeId List
     */
    ResponseData<List<String>> getWeIdList(
        Integer first,
        Integer last
    );
    
    /**
     * get total weId.
     *
     * @return total weid
     */
    ResponseData<Integer> getWeIdCount();

}



package com.webank.weid.blockchain.rpc;

import com.webank.weid.blockchain.protocol.base.AuthorityIssuer;
import com.webank.weid.blockchain.protocol.base.IssuerType;
import com.webank.weid.blockchain.protocol.base.WeIdAuthentication;
import com.webank.weid.blockchain.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.blockchain.protocol.response.ResponseData;

import java.util.List;

/**
 * Service inf for operations on Authority Issuer.
 *
 * @author chaoxinhu 2018.10
 */
public interface AuthorityIssuerService {

    /**
     * Register a new Authority Issuer on Chain.
     *
     * <p>The input argument actually includes: WeIdentity DID, Name, CreateDate, and Accumulator
     * Value. They will be stored into the 3 fields on the chain: the Bytes32 field (Name); the Int
     * field (create date); the Dynamic Bytes field (accValue). The data Read and Write sequence is
     * fixed in the above mentioned order.
     *
     * @param args the args
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> addAuthorityIssuer(RegisterAuthorityIssuerArgs args);

    /**
     * Remove a new Authority Issuer on Chain.
     *
     * @param weId the weId
     * @param privateKey the privateKey
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> removeAuthorityIssuer(String weId, String privateKey);

    /**
     * Check whether the given WeIdentity DID is an authority issuer, or not.
     *
     * @param addr the address of WeIdentity DID
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> isAuthorityIssuer(String addr);

    /**
     * Recognize this WeID to be an authority issuer.
     *
     * @param stage the stage that weather recognize
     * @param addr the address of WeIdentity DID
     * @param privateKey the private key set
     * @return true if succeeds, false otherwise
     */
    ResponseData<Boolean> recognizeWeId(Boolean stage, String addr, String privateKey);

    /**
     * Query the authority issuer information from a given WeIdentity DID.
     *
     * @param weId the WeIdentity DID
     * @return authority issuer info
     */
    ResponseData<AuthorityIssuer> queryAuthorityIssuerInfo(String weId);

    /**
     * Get all of the authority issuer.
     *
     * @param index start position
     * @param num number of returned authority issuer in this request
     * @return Execution result
     */
    ResponseData<List<String>> getAuthorityIssuerAddressList(Integer index, Integer num);

    /**
     * Register a new issuer type.
     *
     * @param privateKey the caller
     * @param issuerType the specified issuer type
     * @return Execution result
     */
    ResponseData<Boolean> registerIssuerType(String privateKey, String issuerType);

    /**
     * Marked an issuer as the specified issuer type.
     *
     * @param privateKey the caller who have the access to modify this list
     * @param issuerType the specified issuer type
     * @param issuerAddress the address of the issuer who will be marked as a specific issuer type
     * @return Execution result
     */
    ResponseData<Boolean> addIssuer(
            String privateKey,
            String issuerType,
            String issuerAddress
    );

    /**
     * Removed an issuer from the specified issuer list.
     *
     * @param privateKey the caller who have the access to modify this list
     * @param issuerType the specified issuer type
     * @param issuerAddress the address of the issuer who will be marked as a specific issuer type
     * @return Execution result
     */
    ResponseData<Boolean> removeIssuer(
            String privateKey,
            String issuerType,
            String issuerAddress
    );

    /**
     * Check if the given WeId is belonging to a specific issuer type.
     *
     * @param issuerType the issuer type
     * @param address the address
     * @return true if yes, false otherwise
     */
    ResponseData<Boolean> isSpecificTypeIssuer(
        String issuerType,
        String address
    );

    /**
     * Get all specific typed issuer in a list.
     *
     * @param issuerType the issuer type
     * @param index the start position index
     * @param num the number of issuers
     * @return the list
     */
    ResponseData<List<String>> getAllSpecificTypeIssuerList(
        String issuerType,
        Integer index,
        Integer num
    );

    /**
     * Get an issuer's WeID from its name (org ID).
     *
     * @param orgId the org id
     * @return WeID
     */
    ResponseData<String> getWeIdFromOrgId(String orgId);
    
    /**
     * get the issuer count.
     * @return the all issuer
     */
    ResponseData<Integer> getIssuerCount();

    /**
     * get the issuer count with Recognized.
     * @return the all issuer with Recognized
     */
    ResponseData<Integer> getRecognizedIssuerCount();

    /**
     * get the issuer size in issuerType.
     * @param issuerType the issuerType
     * @return the all issuer in issuerType
     */
    ResponseData<Integer> getSpecificTypeIssuerSize(String issuerType);

    /**
     * get the issuer type count.
     * @return the all issuer type
     */
    ResponseData<Integer> getIssuerTypeCount();

    /**
     * remove the issuerType.
     * @param privateKey the privateKey of caller
     * @param issuerType the issuerType name
     * @return true is success, false is fail
     */
    ResponseData<Boolean> removeIssuerType(String privateKey, String issuerType);

    /**
     * get the issuerType list.
     * @param index the start index
     * @param num the page size
     * @return the issuerType list
     */
    ResponseData<List<IssuerType>> getIssuerTypeList(Integer index, Integer num);
}

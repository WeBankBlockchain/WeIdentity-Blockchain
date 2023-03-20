

package com.webank.weid.blockchain.service.impl;

import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.protocol.base.AuthorityIssuer;
import com.webank.weid.blockchain.protocol.base.IssuerType;
import com.webank.weid.blockchain.protocol.request.RegisterAuthorityIssuerArgs;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.rpc.AuthorityIssuerService;
import com.webank.weid.blockchain.rpc.WeIdService;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Service implementations for operations on Authority Issuer.
 *
 * @author chaoxinhu 2018.10
 */
@Component("blockchain")
public class AuthorityIssuerServiceImpl extends AbstractService implements AuthorityIssuerService {

    private static final Logger logger = LoggerFactory
        .getLogger(AuthorityIssuerServiceImpl.class);

    private WeIdService weIdService = new WeIdServiceImpl();

    /**
     * add a new Authority Issuer on Chain.
     *
     * @param args the args
     * @return the Boolean response data
     */
    @Override
    public ResponseData<Boolean> addAuthorityIssuer(RegisterAuthorityIssuerArgs args) {

        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.addAuthorityIssuer(args);
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Remove a new Authority Issuer on Chain.
     *
     * @param weId the weId
     * @param privateKey the privateKey
     * @return the Boolean response data
     */
    @Override
    public ResponseData<Boolean> removeAuthorityIssuer(String weId, String privateKey) {

        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.removeAuthorityIssuer(weId, privateKey);
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Check whether the given weId is an authority issuer.
     *
     * @param addr the address of WeIdentity DID
     * @return the Boolean response data
     */
    @Override
    public ResponseData<Boolean> isAuthorityIssuer(String addr) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.isAuthorityIssuer(addr);
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Recognize this WeID to be an authority issuer.
     *
     * @param stage the stage that weather recognize
     * @param addr the address of WeIdentity DID
     * @param privateKey the private key set
     * @return true if succeeds, false otherwise
     */
    @Override
    public ResponseData<Boolean> recognizeWeId(Boolean stage, String addr,
        String privateKey) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.recognizeWeId(stage, addr, privateKey);
        }
        return new ResponseData<>(false, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Query the authority issuer information given weId.
     *
     * @param weId the WeIdentity DID
     * @return the AuthorityIssuer response data
     */
    @Override
    public ResponseData<AuthorityIssuer> queryAuthorityIssuerInfo(String weId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getAuthorityIssuerInfoNonAccValue(weId);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get all of the authority issuer.
     *
     * @param index start position
     * @param num number of returned authority issuer in this request
     * @return Execution result
     */
    @Override
    public ResponseData<List<String>> getAuthorityIssuerAddressList(Integer index,
        Integer num) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            List<String> addrList = authEngine.getAuthorityIssuerAddressList(index, num);
            return new ResponseData<>(addrList, ErrorCode.SUCCESS);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Register a new issuer type.
     *
     * @param privateKey the caller
     * @param issuerType the specified issuer type
     * @return Execution result
     */
    @Override
    public ResponseData<Boolean> registerIssuerType(
        String privateKey,
        String issuerType
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine
                    .registerIssuerType(issuerType, privateKey);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }


    /**
     * Marked an issuer as the specified issuer type.
     *
     * @param privateKey the caller who have the access to modify this list
     * @param issuerType the specified issuer type
     * @param issuerAddress the address of the issuer who will be marked as a specific issuer type
     * @return Execution result
     */
    @Override
    public ResponseData<Boolean> addIssuer(
            String privateKey,
        String issuerType,
        String issuerAddress
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.addIssuer(issuerType, issuerAddress,
                    privateKey);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Removed an issuer from the specified issuer list.
     *
     * @param privateKey the caller who have the access to modify this list
     * @param issuerType the specified issuer type
     * @param issuerAddress the address of the issuer who will be marked as a specific issuer type
     * @return Execution result
     */
    @Override
    public ResponseData<Boolean> removeIssuer(
            String privateKey,
            String issuerType,
            String issuerAddress
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.removeIssuer(
                    issuerType,
                    issuerAddress,
                    privateKey);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Check if the given WeId is belonging to a specific issuer type.
     *
     * @param issuerType the issuer type
     * @param address the address
     * @return true if yes, false otherwise
     */
    @Override
    public ResponseData<Boolean> isSpecificTypeIssuer(
        String issuerType,
        String address
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.isSpecificTypeIssuer(issuerType, address);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    /**
     * Get all specific typed issuer in a list.
     *
     * @param issuerType the issuer type
     * @param index the start position index
     * @param num the number of issuers
     * @return the list
     */
    @Override
    public ResponseData<List<String>> getAllSpecificTypeIssuerList(
        String issuerType,
        Integer index,
        Integer num
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getSpecificTypeIssuerList(issuerType, index, num);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<String> getWeIdFromOrgId(String orgId) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getWeIdFromOrgId(orgId);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getIssuerCount() {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getIssuerCount();
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getRecognizedIssuerCount() {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getRecognizedIssuerCount();
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getSpecificTypeIssuerSize(String issuerType) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getSpecificTypeIssuerSize(issuerType);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Integer> getIssuerTypeCount() {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getIssuerTypeCount();
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<Boolean> removeIssuerType(
        String privateKey,
        String issuerType
    ) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.removeIssuerType(
                    issuerType, privateKey);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID);
    }

    @Override
    public ResponseData<List<IssuerType>> getIssuerTypeList(Integer index, Integer num) {
        if(DataToolUtils.chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return authEngine.getIssuerTypeList(index, num);
        }
        return new ResponseData<>(null, ErrorCode.CHAIN_TYPE_NOT_VALID); }
}

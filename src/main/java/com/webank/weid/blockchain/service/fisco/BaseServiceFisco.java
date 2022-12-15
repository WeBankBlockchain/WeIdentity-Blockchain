

package com.webank.weid.blockchain.service.fisco;

import com.webank.weid.blockchain.config.ContractConfig;
import com.webank.weid.blockchain.service.fisco.engine.DataBucketServiceEngine;
import com.webank.weid.blockchain.service.fisco.engine.EngineFactoryFisco;
import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.constant.CnsType;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.exception.WeIdBaseException;
import com.webank.weid.blockchain.protocol.response.CnsInfo;
import com.webank.weid.blockchain.service.fisco.server.WeServer;
import com.webank.weid.blockchain.util.DataToolUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The BaseService for other RPC classes.
 *
 * @author tonychen
 */
public abstract class BaseServiceFisco {

    private static final Logger logger = LoggerFactory.getLogger(BaseServiceFisco.class);

    public static FiscoConfig fiscoConfig;

    public static String masterGroupId;

    //public static String chainType;

    protected WeServer<?, ?, ?> weServer;

    static {
        fiscoConfig = new FiscoConfig();
        if (!fiscoConfig.load()) {
            logger.error("[BaseService] Failed to load Fisco-BCOS blockchain node information.");
        }
        fiscoConfig.check();
        masterGroupId = fiscoConfig.getGroupId();
        //chainType = PropertyUtils.getProperty("blockchain.type", "FISCO_BCOS");
    }

    /**
     * Constructor.
     */
    public BaseServiceFisco() {
        weServer = getWeServer(masterGroupId);
    }

    /**
     * Constructor.
     *
     * @param groupId 群组编号
     */
    public BaseServiceFisco(String groupId) {
        weServer = getWeServer(groupId);
    }

    public static WeServer<?, ?, ?> getWeServer(String groupId) {
        return WeServer.getInstance(fiscoConfig, groupId);
    }

    public static DataBucketServiceEngine getBucket(CnsType cnsType) {
        return EngineFactoryFisco.createDataBucketServiceEngine(cnsType);
    }

    /**
     * Gets the Fisco client.
     *
     * @return the Fisco client
     */
    public static Object getClient() {
        return getClient(masterGroupId);
    }

    /**
     * Gets the Fisco client.
     *
     * @param groupId 群组ID
     * @return the Fisco client
     */
    public static Object getClient(String groupId) {
        return getWeServer(groupId).getWeb3j();
    }

    public static Object getBcosSDK() {
        return getWeServer(masterGroupId).getBcosSDK();
    }

    public static Object getBcosSDK(String groupId) {
        return getWeServer(groupId).getBcosSDK();
    }

    /**
     * Gets the client class.
     *
     * @return the client class
     */
    protected Class<?> getWeb3jClass() {
        return weServer.getWeb3jClass();
    }

    /**
     * get current blockNumber.
     *
     * @return return blockNumber
     * @throws IOException possible exceptions to sending transactions
     */
    public static int getBlockNumber() throws IOException {
        return getBlockNumber(masterGroupId);
    }

    /**
     * get current blockNumber.
     *
     * @param groupId 群组编号
     * @return return blockNumber
     * @throws IOException possible exceptions to sending transactions
     */
    public static int getBlockNumber(String groupId) throws IOException {
        return getWeServer(groupId).getBlockNumber();
    }

    /**
     * get FISCO-BCOS version.
     *
     * @return return nodeVersion
     * @throws IOException possible exceptions to sending transactions
     */
    public static String getVersion() throws IOException {
        return getWeServer(masterGroupId).getVersion();
    }

    /**
     * 查询bucket地址信息.
     *
     * @param cnsType cns类型枚举对象
     * @return 返回bucket地址
     */
    public static CnsInfo getBucketByCns(CnsType cnsType) {
        return getWeServer(masterGroupId).getBucketByCns(cnsType);
    }

    /**
     * 检查群组是否存在.
     *
     * @param groupId 被检查群组
     * @return true表示群组存在，false表示群组不存在
     */
    public static boolean checkGroupId(String groupId) {
        return getWeServer(groupId).getGroupList().contains(groupId);
    }


    /**
     * Get the Sequence parameter.
     *
     * @return the seq
     */
    protected static String getSeq() {
        return DataToolUtils.getUuId32();
    }

    /**
     * On-demand build the contract config info.
     *
     * @return the contractConfig instance
     */
    protected static ContractConfig buildContractConfig() {
        ContractConfig contractConfig = new ContractConfig();
        contractConfig.setWeIdAddress(fiscoConfig.getWeIdAddress());
        contractConfig.setCptAddress(fiscoConfig.getCptAddress());
        contractConfig.setIssuerAddress(fiscoConfig.getIssuerAddress());
        contractConfig.setEvidenceAddress(fiscoConfig.getEvidenceAddress());
        contractConfig.setSpecificIssuerAddress(fiscoConfig.getSpecificIssuerAddress());
        return contractConfig;
    }

    /**
     * 重新拉取合约地址 并且重新加载相关合约.
     */
    public static void reloadAddress() {
        fiscoConfig.load();
        String module = WeIdConstant.CNS_GLOBAL_KEY;
        CnsType cnsType = CnsType.ORG_CONFING;
        String  weIdAddress = getAddress(cnsType, module, WeIdConstant.CNS_WEID_ADDRESS);
        String  issuerAddress = getAddress(cnsType, module, WeIdConstant.CNS_AUTH_ADDRESS);
        String  specificAddress = getAddress(cnsType, module, WeIdConstant.CNS_SPECIFIC_ADDRESS);
        String  evidenceAddress = getAddress(cnsType, module, WeIdConstant.CNS_EVIDENCE_ADDRESS);
        String  cptAddress = getAddress(cnsType, module, WeIdConstant.CNS_CPT_ADDRESS);
        String  chainId = getAddress(cnsType, module, WeIdConstant.CNS_CHAIN_ID);
        fiscoConfig.setChainId(chainId);
        fiscoConfig.setWeIdAddress(weIdAddress);
        fiscoConfig.setCptAddress(cptAddress);
        fiscoConfig.setIssuerAddress(issuerAddress);
        fiscoConfig.setSpecificIssuerAddress(specificAddress);
        fiscoConfig.setEvidenceAddress(evidenceAddress);
        if (!fiscoConfig.checkAddress()) {
            throw new WeIdBaseException(
                "can not found the contract address, please enable by admin. ");
        }
    }

    private static String getAddress(CnsType cnsType, String hash, String key) {
        return getBucket(cnsType).get(hash, key).getResult();
    }

    /**
     * 获取chainId.
     * @return 返回chainId
     */
    public static String getChainId() {
        if (StringUtils.isBlank(fiscoConfig.getChainId())) {
            reloadAddress();
        }
        return fiscoConfig.getChainId();
    }
}
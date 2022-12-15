

package com.webank.weid.blockchain.deploy;

import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.deploy.v2.DeployContractV2;
import com.webank.weid.blockchain.deploy.v2.DeployEvidenceV2;
import com.webank.weid.blockchain.deploy.v3.DeployContractV3;
import com.webank.weid.blockchain.deploy.v3.DeployEvidenceV3;
import com.webank.weid.blockchain.exception.WeIdBaseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class DeployContract.
 *
 * @author tonychen
 */
public abstract class DeployEvidence {

    /**
     * log4j.
     */
    private static final Logger logger = LoggerFactory.getLogger(DeployEvidence.class);

    /**
     * The Fisco Config bundle.
     */
    protected static final FiscoConfig fiscoConfig;

    static {
        fiscoConfig = new FiscoConfig();
        if (!fiscoConfig.load()) {
            logger.error("[BaseService] Failed to load Fisco-BCOS blockchain node information.");
            System.exit(1);
        }
    }

    /**
     * The main method.
     *
     * @param args the arguments
     */
    public static void main(String[] args) {

        // args = new String[] {"2"};

        if (args == null || args.length == 0) {
            logger.error("input param illegal");
            System.exit(1);
        }

        String groupStr = args[0];
        String groupId = groupStr;

        String privateKey = null;
        if (args != null && args.length > 1) {
            privateKey = args[1];
        }

        deployContract(privateKey, groupId, true);
        System.exit(0);
    }
    
    /**
     * 部署evidence合约.
     * 
     * @param privateKey 私钥地址
     * @param groupId 群组编号
     * @param instantEnable 是否即时启用
     * @return 返回部署的hash值
     */
    public static String deployContract(String privateKey, String groupId, boolean instantEnable) {
        if (fiscoConfig.getVersion().startsWith(WeIdConstant.FISCO_BCOS_1_X_VERSION_PREFIX)) {
            throw new WeIdBaseException(ErrorCode.THIS_IS_UNSUPPORTED);
        } else if (fiscoConfig.getVersion().startsWith(WeIdConstant.FISCO_BCOS_2_X_VERSION_PREFIX)) {
            logger.info("deployContract v2");
            return DeployEvidenceV2.deployContract(fiscoConfig, privateKey, groupId, instantEnable);
        } else if (fiscoConfig.getVersion().startsWith(WeIdConstant.FISCO_BCOS_3_X_VERSION_PREFIX)) {
            logger.info("deployContract v3");
            return DeployEvidenceV3.deployContract(fiscoConfig, privateKey, groupId, instantEnable);
        }
        return StringUtils.EMPTY;
    }
}

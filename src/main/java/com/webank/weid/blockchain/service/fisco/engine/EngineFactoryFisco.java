

package com.webank.weid.blockchain.service.fisco.engine;

import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.constant.CnsType;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.service.fisco.engine.fiscov2.*;
import com.webank.weid.blockchain.service.fisco.engine.fiscov2.CptServiceFiscoEngineFiscoV2;
import com.webank.weid.blockchain.service.fisco.engine.fiscov3.*;
import com.webank.weid.blockchain.service.fisco.engine.fiscov3.CptServiceFiscoEngineFiscoV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 加上V2
 */
public class EngineFactoryFisco {

    private static final Logger logger = LoggerFactory.getLogger(EngineFactoryFisco.class);

    /**
     * The Fisco Config bundle.
     */
    protected static final FiscoConfig fiscoConfig;

    protected static final Boolean isVer2;

    static {
        fiscoConfig = new FiscoConfig();
        if (!fiscoConfig.load()) {
            logger.error("[BaseService] Failed to load Fisco-BCOS blockchain node information.");
            System.exit(1);
        }
        isVer2 = fiscoConfig.getVersion().startsWith(WeIdConstant.FISCO_BCOS_2_X_VERSION_PREFIX);
    }

    /**
     * create WeIdServiceEngine.
     * @return WeIdServiceEngine object
     */
    public static WeIdServiceEngineFisco createWeIdServiceEngine() {
        if (isVer2) {
            return new WeIdServiceFiscoEngineFiscoV2();
        } else {
            return new WeIdServiceFiscoEngineFiscoV3();
        }
    }

    /**
     * create CptServiceEngine.
     * @return CptServiceEngine object
     */
    public static CptServiceEngineFisco createCptServiceEngine() {
        if (isVer2) {
            return new CptServiceFiscoEngineFiscoV2();
        } else {
            return new CptServiceFiscoEngineFiscoV3();
        }
    }

    /**
     * create CptServiceEngine.
     * @return CptServiceEngine object
     */
    public static AuthorityIssuerServiceEngine createAuthorityIssuerServiceEngine() {
        if (isVer2) {
            return new AuthorityIssuerEngineV2();
        } else {
            return new AuthorityIssuerEngineV3();
        }
    }

    /**
     * create EvidenceServiceEngine.
     * @param groupId 群组编号
     * @return EvidenceServiceEngine object
     */
    public static EvidenceServiceEngineFisco createEvidenceServiceEngine(String groupId) {
        if (isVer2) {
            return new EvidenceServiceFiscoEngineFiscoV2(groupId);
        } else {
            return new EvidenceServiceFiscoEngineFiscoV3(groupId);
        }
    }

    /**
     * create RawTransactionServiceEngine.
     * @return RawTransactionServiceEngine object
     */
    public static RawTransactionServiceEngineFisco createRawTransactionServiceEngine() {
        if (isVer2) {
            return new RawTransactionServiceFiscoEngineFiscoV2();
        } else {
            return new RawTransactionServiceFiscoEngineFiscoV3();
        }
    }
    
    /**
     * create DataBucketServiceEngine.
     * @param cnsType cns类型枚举
     * @return DataBucketServiceEngine object
    */
    public static DataBucketServiceEngine createDataBucketServiceEngine(CnsType cnsType) {
        if (isVer2) {
            return new DataBucketServiceFiscoEngineV2(cnsType);
        } else {
            return new DataBucketServiceFiscoEngineV3(cnsType);
        }
    }
}

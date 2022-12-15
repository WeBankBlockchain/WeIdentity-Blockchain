package com.webank.weid.blockchain.service.impl;

import com.webank.weid.blockchain.service.fisco.engine.*;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;


public abstract class AbstractService{
    
    protected static WeIdServiceEngineFisco weIdServiceEngineFisco;
    
    protected static RawTransactionServiceEngineFisco rawEngineFisco;
    
    protected static CptServiceEngineFisco cptServiceEngineFisco;
    
    protected static AuthorityIssuerServiceEngine authEngine;
    
    static {
        if (!BaseServiceFisco.fiscoConfig.checkAddress()) {
            BaseServiceFisco.reloadAddress();
        }
        weIdServiceEngineFisco = EngineFactoryFisco.createWeIdServiceEngine();
        rawEngineFisco = EngineFactoryFisco.createRawTransactionServiceEngine();
        cptServiceEngineFisco = EngineFactoryFisco.createCptServiceEngine();
        authEngine = EngineFactoryFisco.createAuthorityIssuerServiceEngine();
    }
    
    public AbstractService() { }
    
    /*public AbstractService(String groupId) {
        super(groupId);
    }*/

    //由上层调用reloadContract方法再执行CacheManager.clearAll()，这里删除了CacheManager模块
    protected void reloadContract() {
        BaseServiceFisco.reloadAddress();
        weIdServiceEngineFisco.reload();
        rawEngineFisco.reload();
        cptServiceEngineFisco.reload();
        authEngine.reload();
        //重载合约, 需要清理缓存，避免缓存问题
        //CacheManager.clearAll();
    }
}

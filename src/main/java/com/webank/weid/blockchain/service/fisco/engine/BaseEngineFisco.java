/*
 *       Copyright© (2018-2019) WeBank Co., Ltd.
 *
 *       This file is part of weid-java-sdk.
 *
 *       weid-java-sdk is free software: you can redistribute it and/or modify
 *       it under the terms of the GNU Lesser General Public License as published by
 *       the Free Software Foundation, either version 3 of the License, or
 *       (at your option) any later version.
 *
 *       weid-java-sdk is distributed in the hope that it will be useful,
 *       but WITHOUT ANY WARRANTY; without even the implied warranty of
 *       MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *       GNU Lesser General Public License for more details.
 *
 *       You should have received a copy of the GNU Lesser General Public License
 *       along with weid-java-sdk.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.webank.weid.blockchain.service.fisco.engine;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.blockchain.exception.LoadContractException;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;

public abstract class BaseEngineFisco extends BaseServiceFisco {

    private static final Logger logger = LoggerFactory.getLogger(BaseEngineFisco.class);

    public BaseEngineFisco() {
        super();
    }

    public BaseEngineFisco(String groupId) {
        super(groupId);
    }

    private <T> T loadContract(
        String contractAddress,
        Object credentials,
        Class<T> cls) throws NoSuchMethodException, IllegalAccessException,
        InvocationTargetException {
        Class clazz = "2".equals(BaseServiceFisco.fiscoConfig.getVersion()) ? CryptoKeyPair.class : org.fisco.bcos.sdk.v3.crypto.keypair.CryptoKeyPair.class;
        Object contract;
        Method method = cls.getMethod(
            "load",
            String.class,
            getWeb3jClass(),
            clazz // 获取ECDSAKeyPair或者SM2KeyPair的父类CryptoKeyPair
        );
        Object obj = weServer.getWeb3j();
        contract = method.invoke(
            null,
            contractAddress,
            obj,
            credentials
        );
        return (T) contract;
    }

    /**
     * Reload contract.
     *
     * @param contractAddress the contract address
     * @param privateKey the privateKey of the sender
     * @param cls the class
     * @param <T> t
     * @return the contract
     */
    protected <T> T reloadContract(
        String contractAddress,
        String privateKey,
        Class<T> cls) {

        T contract = null;
        try {
            // load contract
            contract = loadContract(contractAddress, weServer.createCredentials(privateKey), cls);
            logger.info(cls.getSimpleName() + " init succ");
        } catch (Exception e) {
            logger.error("load contract :{} failed. Error message is :{}",
                cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        }

        if (contract == null) {
            throw new LoadContractException();
        }
        return contract;
    }

    /**
     * Gets the contract service.
     *
     * @param contractAddress the contract address
     * @param cls the class
     * @param <T> t
     * @return the contract service
     */
    protected <T> T getContractService(String contractAddress, Class<T> cls) {

        T contract = null;
        try {
            contract = loadContract(contractAddress, weServer.getCredentials(), cls);
            logger.info(cls.getSimpleName() + " init succ");

        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            logger.error("load contract :{} failed. Error message is :{}",
                cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        } catch (Exception e) {
            logger.error("load contract Exception:{} failed. Error message is :{}",
                cls.getSimpleName(), e.getMessage(), e);
            throw new LoadContractException(e);
        }

        if (contract == null) {
            throw new LoadContractException();
        }
        return contract;
    }
}

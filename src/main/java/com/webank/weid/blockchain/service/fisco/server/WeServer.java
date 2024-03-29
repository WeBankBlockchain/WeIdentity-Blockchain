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

package com.webank.weid.blockchain.service.fisco.server;

import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.constant.CnsType;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.exception.WeIdBaseException;
import com.webank.weid.blockchain.protocol.response.AmopResponse;
import com.webank.weid.blockchain.protocol.response.CnsInfo;
import com.webank.weid.blockchain.service.fisco.server.v2.WeServerV2;
import com.webank.weid.blockchain.protocol.amop.AmopCommonArgs;
import com.webank.weid.blockchain.service.fisco.server.v3.WeServerV3;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BcosSDK,Client,CryptoKeyPair
 * @param <B> BcosSDK
 * @param <W> client
 * @param <C> cryptoKeyPair
 */
public abstract class WeServer<B, W, C> {

    /*
     * Maximum Timeout period in milliseconds.
     */
    public static final int MAX_AMOP_REQUEST_TIMEOUT = 50000;

    /**
     * log4j.
     */
    private static final Logger logger = LoggerFactory.getLogger(WeServer.class);

    /**
     * WeServer对象上下文.
     */
    private static ConcurrentHashMap<String, WeServer<?, ?, ?>>  weServerContext =
        new ConcurrentHashMap<>();

    /**
     * bucket地址映射Map.
     */
    private static ConcurrentHashMap<String, CnsInfo> bucketAddressMap =
        new ConcurrentHashMap<>();

    /**
     * FISCO配置对象.
     */
    protected FiscoConfig fiscoConfig;

    /**
     * AMOP回调处理注册器.
     */
    //protected RegistCallBack pushCallBack;

    /**
     * 构造WeServer对象,此时仅为初始化做准备.
     *
     * @param fiscoConfig FISCO配置对象
    // * @param pushCallBack 默认的AMOP回调处理类对象
     */
    //protected WeServer(FiscoConfig fiscoConfig, RegistCallBack pushCallBack) {
    protected WeServer(FiscoConfig fiscoConfig) {
        this.fiscoConfig = fiscoConfig;
        //this.pushCallBack = pushCallBack;
        //registDefaultCallback();
    }

    /**
     * 初始化WeServer服务,进行多线程安全保护,确保整个应用只初始化一次 并且根据配置FISCO的版本来自动初始化对应版本的服务.
     *
     * @param fiscoConfig FISCO配置对象
     * @param groupId 群组ID
     * @param <B> BcosSDK
     * @param <W> Web3j对象
     * @param <C> Credential对象
     * @return 返回WeServer对象
     */
    public static synchronized <B, W, C> WeServer<B, W, C> getInstance (
        FiscoConfig fiscoConfig,
        String groupId
    ) {
        WeServer<?, ?, ?> weServer = weServerContext.get(groupId);
        if (weServer == null) {
            synchronized (WeServer.class) {
                weServer = weServerContext.get(groupId);
                if (weServer == null) {
                    if (fiscoConfig.getVersion()
                        .startsWith(WeIdConstant.FISCO_BCOS_2_X_VERSION_PREFIX)) {
                        weServer = new WeServerV2(fiscoConfig);
                        weServer.initWeb3j(groupId);
                        weServerContext.put(groupId, weServer);
                    } else {
                        // v3
                        weServer = new WeServerV3(fiscoConfig);
                        weServer.initWeb3j(groupId);
                        weServerContext.put(groupId, weServer);
                    }
                }
            }
        }
        return (WeServer<B, W, C>) weServer;
    }

    /**
     * 注册默认的callback.
     */
    /*private void registDefaultCallback() {
        pushCallBack.registAmopCallback(
            AmopMsgType.GET_ENCRYPT_KEY.getValue(),
            new KeyManagerCallback()
        );
        pushCallBack.registAmopCallback(
            AmopMsgType.COMMON_REQUEST.getValue(),
            new CommonCallback()
        );
    }*/

    /**
     * 获取PushCallback对象，用于给使用者注册callback处理器.
     *
     * @return 返回RegistCallBack
     */
    /*public RegistCallBack getPushCallback() {
        return pushCallBack;
    }*/

    /**
     * 获取超时时间，如果超时时间非法，则返回默认的超时时间.
     *
     * @param timeOut 调用对应AMOP请求接口的超时时间,毫秒单位.
     * @return 返回正确有效的超时时间
     */
    protected int getTimeOut(int timeOut) {
        if (timeOut > MAX_AMOP_REQUEST_TIMEOUT || timeOut < 0) {
            logger.error("invalid timeOut : {}", timeOut);
            return MAX_AMOP_REQUEST_TIMEOUT;
        } else {
            return timeOut;
        }
    }

    /**
     * 获取AMOP监听的topic.
     *
     * @return 返回topic集合，目前sdk只支持单topic监听
     */
    protected Set<String> getTopic() {
        Set<String> topics = new HashSet<String>();
        if (StringUtils.isNotBlank(FiscoConfig.topic)) {
            topics.add(fiscoConfig.getAmopId() + "_" + FiscoConfig.topic);
        } else {
            topics.add(fiscoConfig.getAmopId());
        }
        return topics;
    }

    /**
     * 获取Web3j对象.
     *
     * @return 返回Web3j对象
     */
    public abstract W getWeb3j();

    /**
     * 返回Bcos SDK 实例
     * @return BcosSDK
     */
    public abstract B getBcosSDK();


    /**
     * 获取Web3j对象所属的类型,此处是为了给动态加载合约使用.
     *
     * @return Web3j的Class
     */
    public abstract Class<?> getWeb3jClass();


    /**
     * 获取Credentials对象.
     *
     * @return 返回Credentials对象
     */
    public abstract C getCredentials();

    /**
     * 根据传入的私钥(10进制数字私钥)，进行动态创建Credentials对象.
     *
     * @param privateKey 数字私钥 decimal
     * @return 返回Credentials对象
     */
    public abstract C createCredentials(String privateKey);

    /**
     * 初始化Web3j. todo 不用初始化多个bcosSDK
     *
     * @param groupId 群组Id
     */
    protected abstract void initWeb3j(String groupId);

    /**
     * 发送AMOP消息.
     *
     * @param amopCommonArgs AMOP请求体
     * @param timeOut AMOP请求超时时间
     * @return 返回AMOP响应体.
     */
    public abstract AmopResponse sendChannelMessage(AmopCommonArgs amopCommonArgs, int timeOut);

    /**
     * 获取当前块高.
     *
     * @return 返回块高
     * @throws IOException 可能出现的异常.
     */
    public abstract int getBlockNumber() throws IOException;

    /**
     * 获取FISCO-BCOS版本.
     *
     * @return 返回版本信息
     * @throws IOException 可能出现的异常.
     */
    public abstract String getVersion() throws IOException;

    /**
     * 查询bucketAddress.
     *
     * @param cnsType cns类型枚举
     * @return 返回CnsInfo
     * @throws WeIdBaseException 查询合约地址异常
     */
    protected abstract CnsInfo queryCnsInfo(CnsType cnsType) throws WeIdBaseException;

    /**
     * 获取链上群组列表
     * @return groupList
     */
    public abstract Set<String> getGroupList();


    /**
     * 获取Bucket地址.
     *
     * @param cnsType cns类型枚举
     * @return 返回bucket地址
     */
    public CnsInfo getBucketByCns(CnsType cnsType) {
        CnsInfo cnsInfo = bucketAddressMap.get(cnsType.toString());
        if (cnsInfo == null) {
            cnsInfo = this.queryCnsInfo(cnsType);
            if (cnsInfo != null) {
                bucketAddressMap.put(cnsType.toString(), cnsInfo);
            } else {
                logger.error("getBucketByCns cnsInfo is still null of [{}]", cnsType.toString());
            }
        }
        return cnsInfo;
    }
}

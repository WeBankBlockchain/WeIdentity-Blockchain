

package com.webank.weid.blockchain.service.fisco.server.v2;

import com.webank.weid.blockchain.config.FiscoConfig;
import com.webank.weid.blockchain.exception.PrivateKeyIllegalException;
import com.webank.weid.blockchain.exception.WeIdBaseException;
import com.webank.weid.blockchain.protocol.response.AmopResponse;
import com.webank.weid.blockchain.protocol.amop.AmopCommonArgs;
import com.webank.weid.blockchain.service.fisco.server.WeServer;

import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.webank.weid.blockchain.constant.ErrorCode;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.amop.AmopMsgOut;
import org.fisco.bcos.sdk.amop.AmopResponseCallback;
import org.fisco.bcos.sdk.amop.topic.TopicType;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.config.ConfigOption;
import org.fisco.bcos.sdk.config.model.AmopTopic;
import org.fisco.bcos.sdk.config.model.ConfigProperty;
import org.fisco.bcos.sdk.contract.precompiled.cns.CnsInfo;

import org.fisco.bcos.sdk.contract.precompiled.cns.CnsService;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.webank.weid.blockchain.constant.CnsType;

public class WeServerV2 extends WeServer<BcosSDK, Client, CryptoKeyPair> {

    /**
     * log4j.
     */
    private static final Logger logger = LoggerFactory.getLogger(WeServerV2.class);

    private BcosSDK bcosSdk;

    private Client client;

    private CnsService cnsService;

    /**
     * 获取Client对象所属的类型,此处是为了给动态加载合约使用.
     *
     * @return Client的Class
     */
    public Class<?> getClientClass() {
        return Client.class;
    }

    /**
     * 构造WeServer对象,此时仅为初始化做准备.
     *
     * @param fiscoConfig FISCO配置对象
     */
    public WeServerV2(FiscoConfig fiscoConfig) {
        super(fiscoConfig);
        initWeb3j(fiscoConfig.getGroupId());
    }

    /**
     * 获取Client对象.
     *
     * @return 返回Client对象
     */
    @Override
    public Client getWeb3j() {
        return client;
    }

    public Client getWeb3j(String groupId) {
        logger.debug("getWeb3j groupId{}", groupId);
        return bcosSdk.getClient(Integer.parseInt(groupId));
    }


    @Override
    public Class<?> getWeb3jClass() {
        return Client.class;
    }

    @Override
    public CryptoKeyPair getCredentials() {
        Client client = this.getWeb3j(fiscoConfig.getGroupId());
        return client.getCryptoSuite().getCryptoKeyPair();
    }

    @Override
    public CryptoKeyPair createCredentials(String privateKey) {
        logger.info("createCredentials v2 {}", privateKey);
        return client.getCryptoSuite().getKeyPairFactory().createKeyPair(new BigInteger(privateKey));
    }

    @Override
    public void initWeb3j(String masterGroupId) {
        //this.pushCallBack = new OnNotifyCallbackV2();
        logger.info("[WeServer] begin load property.");
        ConfigProperty configProperty = loadConfigProperty(fiscoConfig);
        logger.info("[WeServer] begin init bcos sdk.");
        initBcosSdk(configProperty);
        logger.info("[WeServer] begin init CnsService.");
        initCnsService();
        logger.info("[WeServer] begin init initAmopCallBack.");
        /*initAmopCallBack(fiscoConfig);
        logger.info("[WeServer] WeServer init successfully.");*/
    }


    /**
     * 发送AMOP消息.
     *
     * @param amopCommonArgs AMOP请求体
     * @param timeOut        AMOP请求超时时间
     * @return 返回AMOP响应体.
     */
    @Override
    public AmopResponse sendChannelMessage(AmopCommonArgs amopCommonArgs, int timeOut) {
        AmopMsgOut out = new AmopMsgOut();
        out.setType(TopicType.NORMAL_TOPIC);
        out.setContent(amopCommonArgs.getMessage().getBytes());
        out.setTimeout(getTimeOut(timeOut));
        out.setTopic(amopCommonArgs.getTopic());
        ArrayBlockingQueue<AmopResponse> queue = new ArrayBlockingQueue<>(1);
        bcosSdk.getAmop().sendAmopMsg(out, new AmopResponseCallback() {
            @Override
            public void onResponse(org.fisco.bcos.sdk.amop.AmopResponse response) {
                AmopResponse amopResponse = new AmopResponse();
                amopResponse.setMessageId(response.getMessageID());
                amopResponse.setErrorCode(response.getErrorCode());
                if (response.getAmopMsgIn() != null) {
                    amopResponse.setResult(new String(response.getAmopMsgIn().getContent()));
                }
                amopResponse.setErrorMessage(response.getErrorMessage());
                queue.add(amopResponse);
            }
        });
        try {
            AmopResponse response = queue.poll(out.getTimeout(), TimeUnit.MILLISECONDS);
            if (response == null) {
                response = new AmopResponse();
                response.setErrorCode(102);
                response.setMessageId(amopCommonArgs.getMessageId());
                response.setErrorMessage("Amop timeout");
            }
            if (StringUtils.isBlank(response.getResult())) {
                response.setResult("{}");
            }
            return response;
        } catch (Exception e) {
            logger.error("[sendChannelMessage] wait for callback has error.", e);
            throw new WeIdBaseException(ErrorCode.UNKNOW_ERROR);
        }
    }


    @Override
    public int getBlockNumber() {
        return this.getWeb3j(fiscoConfig.getGroupId()).getBlockNumber().getBlockNumber().intValue();
    }

    /**
     * 获取FISCO-BCOS版本.
     *
     * @return 返回版本信息
     * @throws IOException 可能出现的异常.
     */
    @Override
    public String getVersion() throws IOException {
        return this.getWeb3j().getNodeVersion().getNodeVersion().getVersion();
    }


    /**
     * 查询bucketAddress.
     *
     * @param cnsType cns类型枚举
     * @return 返回CnsInfo
     * @throws WeIdBaseException 查询合约地址异常
     */
    @Override
    protected com.webank.weid.blockchain.protocol.response.CnsInfo queryCnsInfo(CnsType cnsType) throws WeIdBaseException {
        try {
            logger.info("[queryBucketFromCns] query address by type = {}.", cnsType.getName());
            List<CnsInfo> cnsInfoList = cnsService.selectByName(cnsType.getName());
            if (cnsInfoList != null) {
                // 获取当前cnsType的大版本前缀
                String cnsTypeVersion = cnsType.getVersion();
                String preV = cnsTypeVersion.substring(0, cnsTypeVersion.indexOf(".") + 1);
                //从后往前找到相应大版本的数据
                for (int i = cnsInfoList.size() - 1; i >= 0; i--) {
                    CnsInfo cnsInfo = cnsInfoList.get(i);
                    if (cnsInfo.getVersion().startsWith(preV)) {
                        logger.info("[queryBucketFromCns] query address form CNS successfully.");
                        return new com.webank.weid.blockchain.protocol.response.CnsInfo(cnsInfo);
                    }
                }
            }
            logger.warn("[queryBucketFromCns] can not find data from CNS.");
            return null;
        } catch (Exception e) {
            logger.error("[queryBucketFromCns] query address has error.", e);
            throw new WeIdBaseException(ErrorCode.UNKNOW_ERROR);
        }
    }


    private ConfigProperty loadConfigProperty(FiscoConfig fiscoConfig) {
        ConfigProperty configProperty = new ConfigProperty();
        // init amop topic
        initAmopTopic(configProperty, fiscoConfig);
        // init netWork
        initNetWork(configProperty, fiscoConfig);
        // init ThreadPool
        initThreadPool(configProperty, fiscoConfig);
        // init CryptoMaterial
        initCryptoMaterial(configProperty, fiscoConfig);
        return configProperty;
    }


    /**
     * 这里暂时只注册一个topic
     * 需要配置一个p12私钥和一个pem公钥
     */
    private void initAmopTopic(ConfigProperty configProperty, FiscoConfig fiscoConfig) {
        logger.info("[initAmopTopic] the amopId: {}", fiscoConfig.getAmopId());
        AmopTopic amopTopic = new AmopTopic();
        amopTopic.setTopicName(fiscoConfig.getAmopId());
        // 配置amop用到的私钥文件，写入的是public keys的路径和p12私钥的路径及p12密码
        amopTopic.setPublicKeys(Arrays.asList(fiscoConfig.getAmopPubPath()));
        amopTopic.setPrivateKey(fiscoConfig.getAmopPriPath());

        amopTopic.setPassword(fiscoConfig.getAmopP12Password());
        List<AmopTopic> amop = new ArrayList<AmopTopic>();
        amop.add(amopTopic);
        configProperty.setAmop(amop);
    }


    private void initNetWork(ConfigProperty configProperty, FiscoConfig fiscoConfig) {
        List<String> nodeList = Arrays.asList(fiscoConfig.getNodes().split(","));
        logger.info("[initNetWork] the current nodes: {}.", nodeList);
        Map<String, Object> netWork = new HashMap<String, Object>();
        netWork.put("peers", nodeList);
        configProperty.setNetwork(netWork);
    }

    private void initThreadPool(ConfigProperty configProperty, FiscoConfig fiscoConfig) {
        Map<String, Object> threadPool = new HashMap<String, Object>();
        threadPool.put("channelProcessorThreadSize", fiscoConfig.getWeb3sdkMaxPoolSize());
        threadPool.put("receiptProcessorThreadSize", fiscoConfig.getWeb3sdkMaxPoolSize());
        threadPool.put("maxBlockingQueueSize", fiscoConfig.getWeb3sdkQueueSize());
        logger.info("[initThreadPool] the threadPool: {}.", threadPool);
        configProperty.setThreadPool(threadPool);
    }

    private void initCryptoMaterial(ConfigProperty configProperty, FiscoConfig fiscoConfig) {
        Map<String, Object> cryptoMaterial = new HashMap<String, Object>();
        cryptoMaterial.put("useSMCrypto", fiscoConfig.getSdkSMCrypto());
        cryptoMaterial.put("certPath", fiscoConfig.getSdkCertPath());
        logger.info("path:{} before", cryptoMaterial.get("certPath"));
        //logger.info("path:{}", cryptoMaterial.get("certPath"));
//        cryptoMaterial.put("certPath", this.getClass().getResource("classpath:").getPath());
//        cryptoMaterial.put("caCert",
//            FiscoConfig.class.getResource("classpath:" + fiscoConfig.getV2CaCrtPath()));
//        cryptoMaterial.put("sslCert",
//            FiscoConfig.class.getResource("classpath:" + fiscoConfig.getV2NodeCrtPath()));
//        cryptoMaterial.put("sslKey",
//            FiscoConfig.class.getResource("classpath:" + fiscoConfig.getV2NodeKeyPath()));
        logger.info("[initThreadPool] the cryptoMaterial: {}.", cryptoMaterial);
        configProperty.setCryptoMaterial(cryptoMaterial);
    }

    private void initBcosSdk(ConfigProperty configProperty) {
        if (bcosSdk == null) {
            synchronized (WeServer.class) {
                logger.info("[WeServer] the WeServer class is locked.");
                if (bcosSdk == null) {
                    logger.info("[WeServer] the bcosSdk is null and build BcosSDK.");
                    try {
                        bcosSdk = new BcosSDK(new ConfigOption(configProperty));
                        client = bcosSdk.getClient(Integer.valueOf(fiscoConfig.getGroupId()));
                    } catch (Exception e) {
                        logger.error("[build] the ConfigOption build fail.", e);
                        throw new WeIdBaseException("the ConfigOption build fail.");
                    }
                } else {
                    logger.info("[WeServer] the bcosSdk is not null.");
                }
                logger.info("[WeServer] the WeServer class is unlock");
                if (bcosSdk != null) {
                    logger.info("[WeServer] the bcosSdk is build successfully.");
                } else {
                    throw new WeIdBaseException("the bcosSdk build fail.");
                }
            }
        }
    }

    /*private void initAmopCallBack(FiscoConfig fiscoConfig) {
        pushCallBack.registAmopCallback(
            AmopMsgType.GET_ENCRYPT_KEY.getValue(),
            new KeyManagerCallback()
        );
        pushCallBack.registAmopCallback(
            AmopMsgType.COMMON_REQUEST.getValue(),
            new CommonCallback()
        );
        bcosSdk.getAmop().setCallback((AmopCallback) pushCallBack);
        bcosSdk.getAmop().subscribeTopic(getTopic(fiscoConfig), (AmopCallback) pushCallBack);
    }*/

    private void initCnsService() {
        Client client = this.getWeb3j(fiscoConfig.getGroupId());
        this.cnsService = new CnsService(client, client.getCryptoSuite().getCryptoKeyPair());
    }



    @Override
    public Set<String> getGroupList() {
        Set<Integer> groupIdSet = bcosSdk.getGroupManagerService().getGroupList();
        Set<String> groupList = groupIdSet.stream().map(String::valueOf).collect(Collectors.toSet());
        return groupList;
    }

    /**
     * 获取AMOP监听的topic.
     *
     * @return 返回topic集合，目前sdk只支持单topic监听
     */
    private String getTopic(FiscoConfig fiscoConfig) {
        if (StringUtils.isNotBlank(FiscoConfig.topic)) {
            return fiscoConfig.getAmopId() + "_" + FiscoConfig.topic;
        } else {
            return fiscoConfig.getAmopId();
        }
    }

    @Override
    public BcosSDK getBcosSDK() {
        return bcosSdk;
    }

    /**
     * 根据传入的私钥(16进制数字私钥)，进行动态创建Credentials对象.
     *
     * @param privateKey 数字私钥
     * @return 返回Credentials对象
     */
    public CryptoKeyPair createCryptoKeyPair(String privateKey) {
        try {
            return client.getCryptoSuite().getKeyPairFactory().createKeyPair(new BigInteger(privateKey));
        } catch (Exception e) {
            throw new PrivateKeyIllegalException(e);
        }
    }

    /**
     * 获取Credentials对象.
     *
     * @return 返回Credentials对象
     */
    public CryptoKeyPair createRandomCryptoKeyPair() {
        return client.getCryptoSuite().getKeyPairFactory().generateKeyPair();
    }

}

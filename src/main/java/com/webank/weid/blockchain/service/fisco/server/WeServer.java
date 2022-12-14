/*
 *       CopyrightÂ© (2018-2019) WeBank Co., Ltd.
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
     * WeServerĺŻąč±ˇä¸Šä¸‹ć–‡.
     */
    private static ConcurrentHashMap<String, WeServer<?, ?, ?>>  weServerContext =
        new ConcurrentHashMap<>();

    /**
     * bucketĺś°ĺť€ć? ĺ°„Map.
     */
    private static ConcurrentHashMap<String, CnsInfo> bucketAddressMap =
        new ConcurrentHashMap<>();

    /**
     * FISCOé…Ťç˝®ĺŻąč±ˇ.
     */
    protected FiscoConfig fiscoConfig;

    /**
     * AMOPĺ›žč°?ĺ¤„ç?†ćł¨ĺ†Śĺ™¨.
     */
    //protected RegistCallBack pushCallBack;

    /**
     * ćž„é€ WeServerĺŻąč±ˇ,ć­¤ć—¶ä»…ä¸şĺ?ťĺ§‹ĺŚ–ĺ?šĺ‡†ĺ¤‡.
     *
     * @param fiscoConfig FISCOé…Ťç˝®ĺŻąč±ˇ
    // * @param pushCallBack é»?č®¤çš„AMOPĺ›žč°?ĺ¤„ç?†ç±»ĺŻąč±ˇ
     */
    //protected WeServer(FiscoConfig fiscoConfig, RegistCallBack pushCallBack) {
    protected WeServer(FiscoConfig fiscoConfig) {
        this.fiscoConfig = fiscoConfig;
        //this.pushCallBack = pushCallBack;
        //registDefaultCallback();
    }

    /**
     * ĺ?ťĺ§‹ĺŚ–WeServerćśŤĺŠˇ,čż›čˇŚĺ¤šçşżç¨‹ĺ®‰ĺ…¨äżťćŠ¤,çˇ®äżťć•´ä¸Şĺş”ç”¨ĺŹŞĺ?ťĺ§‹ĺŚ–ä¸€ć¬ˇ ĺą¶ä¸”ć ąćŤ®é…Ťç˝®FISCOçš„ç‰?ćś¬ćťĄč‡ŞĺŠ¨ĺ?ťĺ§‹ĺŚ–ĺŻąĺş”ç‰?ćś¬çš„ćśŤĺŠˇ.
     *
     * @param fiscoConfig FISCOé…Ťç˝®ĺŻąč±ˇ
     * @param groupId çľ¤ç»„ID
     * @param <B> BcosSDK
     * @param <W> Web3jĺŻąč±ˇ
     * @param <C> CredentialĺŻąč±ˇ
     * @return čż”ĺ›žWeServerĺŻąč±ˇ
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
     * ćł¨ĺ†Śé»?č®¤çš„callback.
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
     * čŽ·ĺŹ–PushCallbackĺŻąč±ˇďĽŚç”¨äşŽç»™ä˝żç”¨č€…ćł¨ĺ†Ścallbackĺ¤„ç?†ĺ™¨.
     *
     * @return čż”ĺ›žRegistCallBack
     */
    /*public RegistCallBack getPushCallback() {
        return pushCallBack;
    }*/

    /**
     * čŽ·ĺŹ–č¶…ć—¶ć—¶é—´ďĽŚĺ¦‚ćžśč¶…ć—¶ć—¶é—´éťžćł•ďĽŚĺ?™čż”ĺ›žé»?č®¤çš„č¶…ć—¶ć—¶é—´.
     *
     * @param timeOut č°?ç”¨ĺŻąĺş”AMOPčŻ·ć±‚ćŽĄĺŹŁçš„č¶…ć—¶ć—¶é—´,ćŻ«ç§’ĺŤ•ä˝Ť.
     * @return čż”ĺ›žć­Łçˇ®ćś‰ć•?çš„č¶…ć—¶ć—¶é—´
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
     * čŽ·ĺŹ–AMOPç›‘ĺ?¬çš„topic.
     *
     * @return čż”ĺ›žtopicé›†ĺ??ďĽŚç›®ĺ‰ŤsdkĺŹŞć”ŻćŚ?ĺŤ•topicç›‘ĺ?¬
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
     * čŽ·ĺŹ–Web3jĺŻąč±ˇ.
     *
     * @return čż”ĺ›žWeb3jĺŻąč±ˇ
     */
    public abstract W getWeb3j();

    /**
     * čż”ĺ›žBcos SDK ĺ®žäľ‹
     * @return BcosSDK
     */
    public abstract B getBcosSDK();


    /**
     * čŽ·ĺŹ–Web3jĺŻąč±ˇć‰€ĺ±žçš„ç±»ĺž‹,ć­¤ĺ¤„ć?Żä¸şäş†ç»™ĺŠ¨ć€?ĺŠ č˝˝ĺ??çş¦ä˝żç”¨.
     *
     * @return Web3jçš„Class
     */
    public abstract Class<?> getWeb3jClass();


    /**
     * čŽ·ĺŹ–CredentialsĺŻąč±ˇ.
     *
     * @return čż”ĺ›žCredentialsĺŻąč±ˇ
     */
    public abstract C getCredentials();

    /**
     * ć ąćŤ®äĽ ĺ…Ąçš„ç§?é’Ą(10čż›ĺ?¶ć•°ĺ­—ç§?é’Ą)ďĽŚčż›čˇŚĺŠ¨ć€?ĺ?›ĺ»şCredentialsĺŻąč±ˇ.
     *
     * @param privateKey ć•°ĺ­—ç§?é’Ą decimal
     * @return čż”ĺ›žCredentialsĺŻąč±ˇ
     */
    public abstract C createCredentials(String privateKey);

    /**
     * ĺ?ťĺ§‹ĺŚ–Web3j. todo ä¸Ťç”¨ĺ?ťĺ§‹ĺŚ–ĺ¤šä¸ŞbcosSDK
     *
     * @param groupId çľ¤ç»„Id
     */
    protected abstract void initWeb3j(String groupId);

    /**
     * ĺŹ‘é€?AMOPć¶?ć?Ż.
     *
     * @param amopCommonArgs AMOPčŻ·ć±‚ä˝“
     * @param timeOut AMOPčŻ·ć±‚č¶…ć—¶ć—¶é—´
     * @return čż”ĺ›žAMOPĺ“Ťĺş”ä˝“.
     */
    public abstract AmopResponse sendChannelMessage(AmopCommonArgs amopCommonArgs, int timeOut);

    /**
     * čŽ·ĺŹ–ĺ˝“ĺ‰Ťĺť—é«?.
     *
     * @return čż”ĺ›žĺť—é«?
     * @throws IOException ĺŹŻč?˝ĺ‡şçŽ°çš„ĺĽ‚ĺ¸¸.
     */
    public abstract int getBlockNumber() throws IOException;

    /**
     * čŽ·ĺŹ–FISCO-BCOSç‰?ćś¬.
     *
     * @return čż”ĺ›žç‰?ćś¬äżˇć?Ż
     * @throws IOException ĺŹŻč?˝ĺ‡şçŽ°çš„ĺĽ‚ĺ¸¸.
     */
    public abstract String getVersion() throws IOException;

    /**
     * ćźĄčŻ˘bucketAddress.
     *
     * @param cnsType cnsç±»ĺž‹ćžšä¸ľ
     * @return čż”ĺ›žCnsInfo
     * @throws WeIdBaseException ćźĄčŻ˘ĺ??çş¦ĺś°ĺť€ĺĽ‚ĺ¸¸
     */
    protected abstract CnsInfo queryCnsInfo(CnsType cnsType) throws WeIdBaseException;

    /**
     * čŽ·ĺŹ–é“ľä¸Šçľ¤ç»„ĺ?—čˇ¨
     * @return groupList
     */
    public abstract Set<String> getGroupList();


    /**
     * čŽ·ĺŹ–Bucketĺś°ĺť€.
     *
     * @param cnsType cnsç±»ĺž‹ćžšä¸ľ
     * @return čż”ĺ›žbucketĺś°ĺť€
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

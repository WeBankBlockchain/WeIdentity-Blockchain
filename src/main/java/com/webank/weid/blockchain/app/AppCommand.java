

package com.webank.weid.blockchain.app;

import com.webank.weid.blockchain.exception.InitWeb3jException;
import com.webank.weid.blockchain.protocol.response.ResponseData;
import com.webank.weid.blockchain.rpc.WeIdService;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.blockchain.service.impl.WeIdServiceImpl;
import com.webank.weid.blockchain.constant.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;

/**
 * commands for testing.
 *
 * @author tonychen 2019年6月11日
 */
public class AppCommand {

    private static final Logger logger = LoggerFactory.getLogger(AppCommand.class);

    /**
     * commands.
     *
     * @param args input
     */
    public static void main(String[] args) {

        Integer result = 0;
        try {
            if (args.length < 2) {
                System.err.println("Parameter illegal, please check your input.");
                System.exit(1);
            }
            String command = args[0];
            if (!StringUtils.equals(command, "--checkhealth")
                && !StringUtils.equals(command, "--checkweid")
                && !StringUtils.equals(command, "--checkversion")) {
                logger.error("[AppCommand] input command :{} is illegal.", command);
                System.err.println("Parameter illegal, please check your input command.");
                System.exit(1);
            }

            switch (command) {
                case "--checkweid":
                    result = checkWeid(args[1]);
                    break;
                case "--checkversion":
                    result = checkVersion();
                    break;
                default:
                    logger.error("[AppCommand]: the command -> {} is not supported .", command);
            }
        } catch (Exception e) {
            logger.error("[AppCommand] execute command with exception.", e);
            System.exit(1);
        }
        System.exit(result);
    }

    private static int checkVersion() {
        try {
            System.setOut(new PrintStream("./sdk.out"));
            String version = BaseServiceFisco.getVersion();
            System.err.println("block chain nodes connected successfully. ");
            System.err.println("the FISCO-BCOS version is: " + version);
            int blockNumer = BaseServiceFisco.getBlockNumber();
            System.err.println("the current blockNumer is: " + blockNumer);
        } catch (InitWeb3jException e) {
            System.err.println("ERROR: initWeb3j error:" + e.getMessage());
            logger.error("[checkVersion] checkVersion with exception.", e);
        } catch (Exception e) {
            System.err.println("ERROR: unknow error:" + e.getMessage());
            logger.error("[checkVersion] checkVersion with exception.", e);
        }
        return 0;
    }

    /**
     * check if the weid exists on blockchain.
     *
     * @param weid the weid to check
     * @return ErrorCode
     */
    private static Integer checkWeid(String weid) {

        WeIdService weidService = new WeIdServiceImpl();
        ResponseData<Boolean> resp = weidService.isWeIdExist(weid);

        if (resp.getErrorCode().intValue() == ErrorCode.SUCCESS.getCode()) {
            logger.info("[checkWeid] weid --> {} exists on blockchain.", weid);
            System.out.println("[checkWeid] weid --> " + weid + "exists on blockchain.");
        } else {
            logger.error("[checkWeid] weid --> {} does not exist on blockchain. response is {}",
                weid,
                resp);
            System.out.println("[checkWeid] weid --> " + weid + " does not exist on blockchain.");
        }
        return resp.getErrorCode();
    }
}

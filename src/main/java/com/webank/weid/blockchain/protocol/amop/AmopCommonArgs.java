

package com.webank.weid.blockchain.protocol.amop;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AMOP common args.
 * @author tonychen 2019年4月16日
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class AmopCommonArgs extends AmopBaseMsgArgs {

    /**
     * 任意包体.
     */
    private String message;
}

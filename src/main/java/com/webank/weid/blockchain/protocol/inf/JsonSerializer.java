

package com.webank.weid.blockchain.protocol.inf;

import java.io.Serializable;

import com.webank.weid.blockchain.util.DataToolUtils;

public interface JsonSerializer extends Serializable {

    public default String toJson() {
        return DataToolUtils.serialize(this);
    }
}

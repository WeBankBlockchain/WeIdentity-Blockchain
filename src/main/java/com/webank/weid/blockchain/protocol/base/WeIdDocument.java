

package com.webank.weid.blockchain.protocol.base;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.webank.weid.blockchain.exception.DataTypeCastException;
import com.webank.weid.blockchain.protocol.inf.JsonSerializer;
import com.webank.weid.blockchain.util.DataToolUtils;

/**
 * The base data structure to handle WeIdentity DID Document info.
 *
 * @author afeexian 2022.8.29
 */
@Data
public class WeIdDocument implements JsonSerializer {

    private static final Logger logger = LoggerFactory.getLogger(WeIdDocument.class);

    /**
     *  the serialVersionUID.
     */
    private static final long serialVersionUID = 411522771907189878L;

    /**
     * Required: The id.
     */
    private String id;

    /**
     * Required: The authentication list.
     */
    private List<AuthenticationProperty> authentication = new ArrayList<>();

    /**
     * Required: The service list.
     */
    private List<ServiceProperty> service = new ArrayList<>();
    
    @Override
    public String toJson() {
        return DataToolUtils.addTagFromToJson(DataToolUtils.serialize(this));
    }
   
    /**
     * create WeIdDocument with JSON String.
     * @param weIdDocumentJson the weIdDocument JSON String
     * @return WeIdDocument
     */
    public static WeIdDocument fromJson(String weIdDocumentJson) {
        if (StringUtils.isBlank(weIdDocumentJson)) {
            logger.error("create WeIdDocument with JSON String failed, "
                + "the WeIdDocument JSON String is null");
            throw new DataTypeCastException("the WeIdDocument JSON String is null.");
        }
        String weIdDocumentString = weIdDocumentJson;
        if (DataToolUtils.isValidFromToJson(weIdDocumentJson)) {
            weIdDocumentString = DataToolUtils.removeTagFromToJson(weIdDocumentJson);
        }
        return DataToolUtils.deserialize(weIdDocumentString, WeIdDocument.class);
    }
}

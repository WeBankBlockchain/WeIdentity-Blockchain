package com.webank.weid.blockchain.constant;

/**
 * chain type enum.
 *
 * @author afee 2022年11月17日
 */
public enum ChainType {

    /**
     * 默认使用FISCO BCOS 2.0区块链.
     */
    FISCO_BCOS_V2("FISCO_BCOS", "v2.0"),

    FISCO_BCOS_V3("FISCO_BCOS", "v3.0");

    private static final String SPLIT_CHAR = "_";

    private String name;

    private String version;

    ChainType(String name, String version){
        this.name = name;
        this.version = version;
    }

    public String toString() {
        return name + SPLIT_CHAR + version;
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }
}

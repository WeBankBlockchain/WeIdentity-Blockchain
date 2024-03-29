
package com.webank.weid.blockchain.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.networknt.schema.SpecVersion.VersionFlag;
import com.networknt.schema.ValidationMessage;
import com.webank.weid.blockchain.protocol.base.WeIdDocument;
import com.webank.weid.blockchain.constant.ChainType;
import com.webank.weid.blockchain.constant.ErrorCode;
import com.webank.weid.blockchain.constant.WeIdConstant;
import com.webank.weid.blockchain.exception.DataTypeCastException;
import com.webank.weid.blockchain.exception.WeIdBaseException;
import com.webank.weid.blockchain.protocol.base.AuthenticationProperty;
import com.webank.weid.blockchain.protocol.response.RsvSignature;
import com.webank.weid.blockchain.service.fisco.BaseServiceFisco;
import com.webank.weid.blockchain.service.fisco.CryptoFisco;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.fisco.bcos.sdk.abi.datatypes.Address;
import org.fisco.bcos.sdk.abi.datatypes.DynamicArray;
import org.fisco.bcos.sdk.abi.datatypes.DynamicBytes;
import org.fisco.bcos.sdk.abi.datatypes.StaticArray;
import org.fisco.bcos.sdk.abi.datatypes.generated.Bytes32;
import org.fisco.bcos.sdk.abi.datatypes.generated.Int256;
import org.fisco.bcos.sdk.abi.datatypes.generated.Uint256;
import org.fisco.bcos.sdk.abi.datatypes.generated.Uint8;
import org.fisco.bcos.sdk.utils.Numeric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 数据工具类.
 *
 * @author tonychen 2019年4月23日
 */
public final class DataToolUtils {

    private static final Logger logger = LoggerFactory.getLogger(DataToolUtils.class);
    private static final String SEPARATOR_CHAR = "-";
    //private static ObjectMapper objectMapper = new ObjectMapper();

    /**
     * default salt length.
     */
    private static final String DEFAULT_SALT_LENGTH = "5";

    private static final int SERIALIZED_SIGNATUREDATA_LENGTH = 65;

    private static final int radix = 10;

    private static final String TO_JSON = "toJson";

    private static final String FROM_JSON = "fromJson";

    private static final String KEY_CREATED = "created";

    private static final String KEY_ISSUANCEDATE = "issuanceDate";

    private static final String KEY_EXPIRATIONDATE = "expirationDate";

    private static final String KEY_CLAIM = "claim";

    private static final String KEY_FROM_TOJSON = "$from";

    private static final List<String> CONVERT_UTC_LONG_KEYLIST = new ArrayList<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    //private static final ObjectWriter OBJECT_WRITER;
    //private static final ObjectReader OBJECT_READER;
    private static final ObjectWriter OBJECT_WRITER_UN_PRETTY_PRINTER;

    private static final com.networknt.schema.JsonSchemaFactory JSON_SCHEMA_FACTORY;

    public static String chainType = PropertyUtils.getProperty("blockchain.type", "FISCO_BCOS");

    public static String chainId = PropertyUtils.getProperty("chain.id", "1");

    //目前是否国密的配置选项在fisco.config文件，后面可以放在weidentity.config文件，作为所有链共用配置选项，
    //这样也需要更改build-tools。不管放在哪个文件，都可以被PropertyUtils统一捕获
    public static int cryptoType = Integer.parseInt(PropertyUtils.getProperty("sdk.sm-crypto", "0"));;

    static {
        // sort by letter
        OBJECT_MAPPER.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        // when map is serialization, sort by key
        OBJECT_MAPPER.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        // ignore mismatched fields
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        // use field for serialize and deSerialize
        OBJECT_MAPPER.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.NONE);
        OBJECT_MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

        OBJECT_WRITER_UN_PRETTY_PRINTER = OBJECT_MAPPER.writer();

        CONVERT_UTC_LONG_KEYLIST.add(KEY_CREATED);
        CONVERT_UTC_LONG_KEYLIST.add(KEY_ISSUANCEDATE);
        CONVERT_UTC_LONG_KEYLIST.add(KEY_EXPIRATIONDATE);

        //OBJECT_WRITER = OBJECT_MAPPER.writer().withDefaultPrettyPrinter();
        //OBJECT_READER = OBJECT_MAPPER.reader();

        JSON_SCHEMA_FACTORY = com.networknt.schema.JsonSchemaFactory.getInstance(VersionFlag.V4);
    }

    public static Integer getBlockNumber() throws IOException {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return BaseServiceFisco.getBlockNumber();
        }else {
            //默认使用fisco的密码学工具
            return BaseServiceFisco.getBlockNumber();
        }
    }

    public static String getVersion() throws IOException {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return BaseServiceFisco.getVersion();
        }else {
            //默认使用fisco的密码学工具
            return BaseServiceFisco.getVersion();
        }
    }

    /**
     * generate random string.
     *
     * @return random string
     */
    public static String getRandomSalt() {

        String length = PropertyUtils.getProperty("salt.length", DEFAULT_SALT_LENGTH);
        int saltLength = Integer.valueOf(length);
        String salt = RandomStringUtils.random(saltLength, true, true);
        return salt;
    }

    /**
     * Keccak-256 hash function.
     *
     * @param utfString the utfString
     * @return hash value as hex encoded string
     */
    public static String hash(String utfString) {
        return Numeric.toHexString(hash(utfString.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Sha 3.
     *
     * @param input the input
     * @return the byte[]
     */
    public static byte[] hash(byte[] input) {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return CryptoFisco.hash(input);
        }else {
            //默认使用fisco的密码学工具
            return CryptoFisco.hash(input);
        }
    }

    public static String getHash(String hexInput) {
        return hash(hexInput);
    }


    /**
     * serialize a class instance to Json String.
     *
     * @param object the class instance to serialize
     * @param <T> the type of the element
     * @return JSON String
     */
    public static <T> String serialize(T object) {
        Writer write = new StringWriter();
        try {
            OBJECT_MAPPER.writeValue(write, object);
        } catch (JsonGenerationException e) {
            logger.error("JsonGenerationException when serialize object to json", e);
        } catch (JsonMappingException e) {
            logger.error("JsonMappingException when serialize object to json", e);
        } catch (IOException e) {
            logger.error("IOException when serialize object to json", e);
        }
        return write.toString();
    }

    /**
     * Convert a private key to its default WeID.
     *
     * @param privateKey the pass-in privatekey
     * @return true if yes, false otherwise
     */
    /*public static String convertPrivateKeyToDefaultWeId(BigInteger privateKey) {
        return publicKeyStrFromPrivate(privateKey);
//        BigInteger publicKey;
//        if (encryptType.equals(String.valueOf(EncryptType.ECDSA_TYPE))) {
//            publicKey = Sign.publicKeyFromPrivate(new BigInteger(privateKey));
//        } else {
//            publicKey = Sign.smPublicKeyFromPrivate(new BigInteger(privateKey));
//        }
//        return WeIdUtils
//            .convertAddressToWeId(new org.fisco.bcos.web3j.abi.datatypes.Address(
//                Keys.getAddress(publicKey)).toString());
//        CryptoKeyPair keyPair = createKeyPairFromPrivate(new BigInteger(privateKey));
//        return WeIdUtils.convertAddressToWeId(keyPair.getAddress());
    }*/

    /**
     * Check whether the String is a valid hash.
     *
     * @param hashValue hash in String
     * @return true if yes, false otherwise
     */
    public static boolean isValidHash(String hashValue) {
        return !StringUtils.isEmpty(hashValue)
            && Pattern.compile(WeIdConstant.HASH_VALUE_PATTERN).matcher(hashValue).matches();
    }

    /**
     * deserialize a JSON String to an class instance.
     *
     * @param json json string
     * @param clazz Class.class
     * @param <T> the type of the element
     * @return class instance
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        Object object = null;
        try {
            if (isValidFromToJson(json)) {
                logger.error("this jsonString is converted by toJson(), "
                    + "please use fromJson() to deserialize it");
                throw new DataTypeCastException("deserialize json to Object error");
            }
            object = OBJECT_MAPPER.readValue(json, TypeFactory.rawClass(clazz));
        } catch (JsonParseException e) {
            logger.error("JsonParseException when deserialize json to object", e);
            throw new DataTypeCastException(e);
        } catch (JsonMappingException e) {
            logger.error("JsonMappingException when deserialize json to object", e);
            throw new DataTypeCastException(e);
        } catch (IOException e) {
            logger.error("IOException when deserialize json to object", e);
            throw new DataTypeCastException(e);
        }
        return (T) object;
    }

    /**
     * deserialize a JSON String to List.
     *
     * @param json json string
     * @param clazz Class.class
     * @param <T> the type of the element
     * @return class instance
     */
    public static <T> List<T> deserializeToList(String json, Class<T> clazz) {
        List<T> object = null;
        try {
            JavaType javaType =
                OBJECT_MAPPER.getTypeFactory()
                    .constructParametricType(ArrayList.class, TypeFactory.rawClass(clazz));
            object = OBJECT_MAPPER.readValue(json, javaType);
        } catch (JsonParseException e) {
            logger.error("JsonParseException when serialize object to json", e);
            throw new DataTypeCastException(e);
        } catch (JsonMappingException e) {
            logger.error("JsonMappingException when serialize object to json", e);
            new DataTypeCastException(e);
        } catch (IOException e) {
            logger.error("IOException when serialize object to json", e);
        }
        return object;
    }

    /**
     * Object to Json String.
     *
     * @param obj Object
     * @return String
     */
    public static String objToJsonStrWithNoPretty(Object obj) {

        try {
            return OBJECT_WRITER_UN_PRETTY_PRINTER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new DataTypeCastException(e);
        }
    }

    /**
     * Convert a Map to compact Json output, with keys ordered. Use Jackson JsonNode toString() to
     * ensure key order and compact output.
     *
     * @param map input map
     * @return JsonString
     * @throws Exception IOException
     */
    public static String mapToCompactJson(Map<String, Object> map) throws Exception {
        return OBJECT_MAPPER.readTree(serialize(map)).toString();
    }

    /**
     * Convert a Map to compact Json output, with keys ordered. Use Jackson JsonNode toString() to
     * ensure key order and compact output.
     *
     * @param map input map
     * @return JsonString
     * @throws Exception IOException
     */
    public static String stringMapToCompactJson(Map<String, String> map) throws Exception {
        return OBJECT_MAPPER.readTree(serialize(map)).toString();
    }

    /**
     * Convert a POJO to Map.
     *
     * @param object POJO
     * @return Map
     * @throws Exception IOException
     */
    public static Map<String, Object> objToMap(Object object) throws Exception {
        JsonNode jsonNode = OBJECT_MAPPER.readTree(serialize(object));
        return (HashMap<String, Object>) OBJECT_MAPPER.convertValue(jsonNode, HashMap.class);
    }

    /**
     * Convert a MAP to POJO.
     *
     * @param map the input data
     * @param <T> the type of the element
     * @param clazz the output class type
     * @return object in T type
     * @throws Exception IOException
     */
    public static <T> T mapToObj(Map<String, Object> map, Class<T> clazz) throws Exception {
        final T pojo = (T) OBJECT_MAPPER.convertValue(map, clazz);
        return pojo;
    }

    /**
     * 对象深度复制(对象必须是实现了Serializable接口).
     *
     * @param obj pojo
     * @param <T> the type of the element
     * @return Object clonedObj
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T clone(T obj) {
        T clonedObj = null;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(obj);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            clonedObj = (T) ois.readObject();
            ois.close();
        } catch (Exception e) {
            logger.error("clone object has error.", e);
        }
        return clonedObj;
    }

    /**
     * Load Json Object. Can be used to return both Json Data and Json Schema.
     *
     * @param jsonString the json string
     * @return JsonNode
     * @throws JsonProcessingException parse json fail
     */
    public static JsonNode loadJsonObject(String jsonString) throws JsonProcessingException {
        return OBJECT_MAPPER.readTree(jsonString);

    }

    /**
     * load json from resource
     * @param path class path file path
     * @return json node
     * @throws IOException load error
     */
    public static JsonNode loadJsonObjectFromResource(String path) throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
            if (inputStream == null) {
                throw new DataTypeCastException("open path to inputStream get null!");
            }
            return OBJECT_MAPPER.readTree(inputStream);
        }
    }

    /**
     * load json from file
     * @param file file of json
     * @return json node
     * @throws IOException file not found
     */
    public static JsonNode loadJsonObjectFromFile(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            return OBJECT_MAPPER.readTree(inputStream);
        } catch (FileNotFoundException e) {
            logger.error("file not found when load jsonObject:{}", file.getPath());
            throw new DataTypeCastException(e);
        }
    }


    /**
     * Validate Json Data versus Json Schema.
     *
     * @param jsonData the json data
     * @param jsonSchema the json schema
     * @return empty if yes, not empty otherwise
     * @throws Exception the exception
     */
    public static Set<ValidationMessage> checkJsonVersusSchema(String jsonData, String jsonSchema)
        throws Exception {
        JsonNode jsonDataNode = loadJsonObject(jsonData);
        JsonNode jsonSchemaNode = loadJsonObject(jsonSchema);
        // use new validator
        com.networknt.schema.JsonSchema schema = JSON_SCHEMA_FACTORY.getSchema(jsonSchemaNode);
        Set<ValidationMessage> report = schema.validate(jsonDataNode);
        if (report.size() == 0) {
            logger.info(report.toString());
        } else {
            Iterator<ValidationMessage> it = report.iterator();
            StringBuffer errorMsg = new StringBuffer();
            while (it.hasNext()) {
                ValidationMessage msg = it.next();
                errorMsg.append(msg.getCode()).append(":").append(msg.getMessage());
            }
            logger.error("Json schema validator failed, error: {}", errorMsg.toString());
        }
        return report;
//        JsonSchema schema = JsonSchemaFactory.byDefault().getJsonSchema(jsonSchemaNode);
//        ProcessingReport report = schema.validate(jsonDataNode);
//        if (report.isSuccess()) {
//            logger.info(report.toString());
//        } else {
//            Iterator<ProcessingMessage> it = report.iterator();
//            StringBuffer errorMsg = new StringBuffer();
//            while (it.hasNext()) {
//                errorMsg.append(it.next().getMessage());
//            }
//            logger.error("Json schema validator failed, error: {}", errorMsg.toString());
//        }
//        return report;
    }

    /**
     * Validate Json Schema format validity.
     *
     * @param jsonSchema the json schema
     * @return true if yes, false otherwise
     * @throws IOException Signals that an I/O exception has occurred.
     */
//    public static boolean isValidJsonSchema(String jsonSchema) {
//        return JsonSchemaFactory
//            .byDefault()
//            .getSyntaxValidator()
//            .schemaIsValid(loadJsonObject(jsonSchema));
//    }

    /**
     * validate Cpt Json Schema validity .
     *
     * @param cptJsonSchema the cpt json schema
     * @return true, if is cpt json schema valid
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static boolean isCptJsonSchemaValid(String cptJsonSchema) throws IOException {
        return StringUtils.isNotEmpty(cptJsonSchema)
//            && isValidJsonSchema(cptJsonSchema)
            && cptJsonSchema.length() <= WeIdConstant.JSON_SCHEMA_MAX_LENGTH;
    }

    /**
     * Check if this json string is in valid format.
     *
     * @param json Json string
     * @return true if yes, false otherwise
     */
    public static boolean isValidJsonStr(String json) {
        if (StringUtils.isEmpty(json)) {
            return false;
        }
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Secp256k1 sign to Signature.
     *
     * @param rawData original raw data
     * @param privateKey decimal
     * @return SignatureData for signature value
     */
    public static RsvSignature signToRsvSignature(String rawData, String privateKey) {
        RsvSignature rsvSignature = null;
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            String messageHash = CryptoFisco.hash(rawData);
            rsvSignature = CryptoFisco.sign(messageHash, privateKey);
        }else {
            //默认使用fisco的密码学工具
            String messageHash = CryptoFisco.hash(rawData);
            rsvSignature = CryptoFisco.sign(messageHash, privateKey);
        }
        return rsvSignature;
    }

    /**
     * Serialize secp256k1 signature into base64 encoded, in R, S, V (0, 1) format.
     *
     * @param sigData secp256k1 signature (v = 0,1)
     * @return base64 string
     */
    public static String SigBase64Serialization(
            RsvSignature sigData) {
        byte[] sigBytes = new byte[65];
        sigBytes[64] = sigData.getV().getValue().byteValue();
        System.arraycopy(sigData.getR().getValue(), 0, sigBytes, 0, 32);
        System.arraycopy(sigData.getS().getValue(), 0, sigBytes, 32, 32);
        return new String(base64Encode(sigBytes), StandardCharsets.UTF_8);
    }

    /**
     * De-Serialize secp256k1 signature base64 encoded string, in R, S, V (0, 1) format.
     *
     * @param signature signature base64 string
     * @return secp256k1 signature (v = 0,1)
     */
    public static RsvSignature SigBase64Deserialization(String signature) {
        byte[] sigBytes = base64Decode(signature.getBytes(StandardCharsets.UTF_8));
        if (SERIALIZED_SIGNATUREDATA_LENGTH != sigBytes.length) {
            throw new WeIdBaseException("signature data illegal");
        }
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(sigBytes, 0, r, 0, 32);
        System.arraycopy(sigBytes, 32, s, 0, 32);
        RsvSignature rsvSignature = new RsvSignature();
        rsvSignature.setR(new Bytes32(r));
        rsvSignature.setS(new Bytes32(s));
        rsvSignature.setV(new Uint8(sigBytes[64]));
        return rsvSignature;
    }

    /**
     * De-Serialize secp256k1 signature base64 encoded string, in R, S, V (0, 1) format.
     *
     * @param signature signature base64 string
     * @param publicKey publicKey
     * @return secp256k1 signature (v = 0,1)
     */
    /*public static SignatureData secp256k1SigBase64Deserialization(
        String signature,
        BigInteger publicKey) {
        byte[] sigBytes = base64Decode(signature.getBytes(StandardCharsets.UTF_8));
        byte[] r = new byte[32];
        byte[] s = new byte[32];
        System.arraycopy(sigBytes, 0, r, 0, 32);
        System.arraycopy(sigBytes, 32, s, 0, 32);

        return new SignatureData(sigBytes[64], r, s, Numeric.toBytesPadded(publicKey, 64));
    }*/

    /**
     * Verify secp256k1 signature.
     *
     * @param rawData original raw data
     * @param signatureBase64 signature string
     * @param publicKey in BigInteger format
     * @return return boolean result, true is success and false is fail
     */
    public static boolean verifySignature(
        String rawData,
        String signatureBase64,
        BigInteger publicKey
    ) {
        try {
            if (rawData == null) {
                return false;
            }
            RsvSignature rsvSignature = SigBase64Deserialization(signatureBase64);
            if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
                String messageHash = CryptoFisco.hash(rawData);
                return CryptoFisco.verifySignature(publicKey.toString(16), messageHash, rsvSignature);
            }else {
                //默认使用fisco的密码学工具
                String messageHash = CryptoFisco.hash(rawData);
                return CryptoFisco.verifySignature(publicKey.toString(16), messageHash, rsvSignature);
            }
        } catch (Exception e) {
            logger.error("Error occurred during secp256k1 sig verification: {}", e);
            return false;
        }
    }

    /**
     * Recover WeID from message and Signature (Secp256k1 type of sig only).
     *
     * @param rawData Raw Data
     * @param sigBase64 signature in base64
     * @return WeID
     */
    /*public static String recoverWeIdFromMsgAndSecp256Sig(String rawData, String sigBase64) {
        SignatureData sigData = secp256k1SigBase64Deserialization(
            sigBase64);
        byte[] hashBytes = Hash.sha3(rawData.getBytes());
        SignatureData modifiedSigData =
            new SignatureData(
                (byte) (sigData.getV() + 27),
                sigData.getR(),
                sigData.getS());

        BigInteger publicKey;
        if (encryptType.equals(String.valueOf(EncryptType.ECDSA_TYPE))) {
            ECDSASignature sig =
                new ECDSASignature(
                    org.fisco.bcos.web3j.utils.Numeric.toBigInt(modifiedSigData.getR()),
                    org.fisco.bcos.web3j.utils.Numeric.toBigInt(modifiedSigData.getS()));
            publicKey = Sign
                .recoverFromSignature(modifiedSigData.getV() - 27, sig, hashBytes);
        } else {
            // not support
            throw new WeIdBaseException("not support!");
        }
        return WeIdUtils.convertPublicKeyToWeId(publicKey.toString(10));
    }*/

    /**
     * Extract the Public Key from the message and the SignatureData.
     *
     * @param message the message
     * @param signatureData the signature data
     * @return publicKey
     * @throws SignatureException Signature is the exception.
     */
    /*public static BigInteger signatureToPublicKey(
        String message,
        Sign.SignatureData signatureData)
        throws SignatureException {
        try {
            return Sign.signedMessageToKey(sha3(message.getBytes(StandardCharsets.UTF_8)),
                    signatureData);
        } catch (Exception e) {
            e.printStackTrace();
            throw new SignatureException(e);
        }
    }*/

    /**
     * encrypt the data. todo
     *
     * @param data the data to encrypt
     * @param publicKey public key
     * @return decrypt data
     * @throws Exception encrypt exception
     */
    public static byte[] encrypt(String data, String publicKey) throws Exception {
        /*
        cryptoSuite.ECCEncrypt encrypt = new ECCEncrypt(new BigInteger(publicKey));
            return encrypt.encrypt(data.getBytes());

         */
        return data.getBytes();
    }


    /**
     * decrypt the data. todo
     *
     * @param data the data to decrypt
     * @param privateKey private key
     * @return original data
     * @throws Exception decrypt exception
     */
    public static byte[] decrypt(byte[] data, String privateKey) throws Exception {

        /*ECCDecrypt decrypt = new ECCDecrypt(new BigInteger(privateKey));
        return decrypt.decrypt(data);*/
        return data;
    }


    /**
     * hexString of pub or private key convert to decimal string
     * @param keyInHex private key or pub key in hex string
     * @return decimal string
     */
    public static String hexStr2DecStr(String keyInHex) {
        byte[] keyBytes = Numeric.hexStringToByteArray(keyInHex);
        return new BigInteger(1, keyBytes).toString(10);
    }

    /**
     * generate private key
     * @return decimal private key
     */
    public static String generatePrivateKey() {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return hexStr2DecStr(CryptoFisco.generatePrivateKey());
        }else {
            //默认使用fisco的密码学工具
            return hexStr2DecStr(CryptoFisco.generatePrivateKey());
        }
    }

    /**
     * Obtain the PublicKey from given PrivateKey.
     *
     * @param privateKey the private key
     * @return publicKey
     */
    public static BigInteger publicKeyFromPrivate(BigInteger privateKey) {
        return new BigInteger(publicKeyStrFromPrivate(privateKey));
    }

    /**
     * Obtain the PublicKey from given PrivateKey.
     *
     * @param privateKey the private key
     * @return publicKey decimal
     */
    public static String publicKeyStrFromPrivate(BigInteger privateKey) {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return hexStr2DecStr(CryptoFisco.keypairFromPrivate(privateKey).getHexPublicKey());
        }else {
            //默认使用fisco的密码学工具
            return hexStr2DecStr(CryptoFisco.keypairFromPrivate(privateKey).getHexPublicKey());
        }
    }

    /**
     * Obtain the PublicKey from given PrivateKey.
     *
     * @param privateKey the private key
     * @return publicKey
     */
    public static String addressFromPrivate(BigInteger privateKey) {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return CryptoFisco.keypairFromPrivate(privateKey).getAddress();
        }else {
            //默认使用fisco的密码学工具
            return CryptoFisco.keypairFromPrivate(privateKey).getAddress();
        }
    }

    /**
     * Obtain the PublicKey from given PrivateKey.
     *
     * @param publicKey the public key
     * @return publicKey
     */
    public static String addressFromPublic(BigInteger publicKey) {
        if(chainType.equals(ChainType.FISCO_BCOS_V2.getName())){
            return CryptoFisco.addressFromPublicKey(publicKey);
        }else {
            //默认使用fisco的密码学工具
            return CryptoFisco.addressFromPublicKey(publicKey);
        }
    }


    /**
     * The Base64 encode/decode class.
     *
     * @param base64Bytes the base 64 bytes
     * @return the byte[]
     */
    public static byte[] base64Decode(byte[] base64Bytes) {
        return Base64.decode(base64Bytes);
    }

    /**
     * Base 64 encode.
     *
     * @param nonBase64Bytes the non base 64 bytes
     * @return the byte[]
     */
    public static byte[] base64Encode(byte[] nonBase64Bytes) {
        return Base64.encode(nonBase64Bytes);
    }

    /**
     * Checks if is valid base 64 string.
     *
     * @param string the string
     * @return true, if is valid base 64 string
     */
    public static boolean isValidBase64String(String string) {
        return org.apache.commons.codec.binary.Base64.isBase64(string);
    }

    /**
     * The Serialization class of Signatures. This is simply a concatenation of bytes of the v, r,
     * and s. Ethereum uses a similar approach with a wrapping from Base64.
     * https://www.programcreek.com/java-api-examples/index.php?source_dir=redPandaj-master/src/org/redPandaLib/crypt/ECKey.java
     * uses a DER-formatted serialization, but it does not entail the v tag, henceforth is more
     * complex and computation hungry.
     *
     * @param signatureData the signature data
     * @return the byte[]
     */
    /*public static byte[] simpleSignatureSerialization(ECDSASignatureResult signatureData) {
        byte[] serializedSignatureData = new byte[65];
        serializedSignatureData[0] = signatureData.getV();
        System.arraycopy(signatureData.getR(), 0, serializedSignatureData, 1, 32);
        System.arraycopy(signatureData.getS(), 0, serializedSignatureData, 33, 32);
        return serializedSignatureData;
    }*/

    /**
     * Verify a signature (base64).
     *
     * @param rawData the rawData to be verified
     * @param signature the Signature Data in Base64 style
     * @param weIdDocument the WeIdDocument to be extracted
     * @param methodId the WeID public key ID
     * @return true if yes, false otherwise with exact error codes
     */
    public static ErrorCode verifySignatureFromWeId(
        String rawData,
        String signature,
        WeIdDocument weIdDocument,
        String methodId) {

        String foundMatchingMethodId = StringUtils.EMPTY;
        try {
            boolean result = false;
            for (AuthenticationProperty authenticationProperty : weIdDocument.getAuthentication()) {
                if (StringUtils.isNotEmpty(authenticationProperty.getPublicKey())) {
                    boolean currentResult = verifySignature(
                        rawData, signature, new BigInteger(authenticationProperty.getPublicKey()));
                    result = currentResult || result;
                    if (currentResult) {
                        foundMatchingMethodId = authenticationProperty.getId();
                        break;
                    }
                }
            }
            if (!result) {
                return ErrorCode.CREDENTIAL_VERIFY_FAIL;
            }
        } catch (Exception e) {
            logger.error("some exceptions occurred in signature verification", e);
            return ErrorCode.CREDENTIAL_EXCEPTION_VERIFYSIGNATURE;
        }
        if (!StringUtils.isEmpty(methodId)
            && !foundMatchingMethodId.equalsIgnoreCase(methodId)) {
            return ErrorCode.CREDENTIAL_VERIFY_SUCCEEDED_WITH_WRONG_PUBLIC_KEY_ID;
        }
        return ErrorCode.SUCCESS;
    }

    /**
     * Convert SignatureData to blockchain-ready RSV format.
     *
     * @param signatureData the signature data
     * @return rsvSignature the rsv signature structure
     */
    /*public static RsvSignature convertSignatureDataToRsv(
        //SignatureData signatureData) {
        ECDSASignatureResult signatureData) {
        Uint8 v = intToUnt8(Integer.valueOf(signatureData.getV()));
        Bytes32 r = bytesArrayToBytes32(signatureData.getR());
        Bytes32 s = bytesArrayToBytes32(signatureData.getS());
        RsvSignature rsvSignature = new RsvSignature();
        rsvSignature.setV(v);
        rsvSignature.setR(r);
        rsvSignature.setS(s);
        return rsvSignature;
    }*/

    /**
     * Convert an off-chain Base64 signature String to signatureData format.
     *
     * @param base64Signature the signature string in Base64
     * @return signatureData structure
     */
    //public static SignatureData convertBase64StringToSignatureData(String base64Signature) {
    /*public static ECDSASignatureResult convertBase64StringToSignatureData(String base64Signature) {
        return simpleSignatureDeserialization(
            base64Decode(base64Signature.getBytes(StandardCharsets.UTF_8))
        );
    }*/

    /**
     * Get the UUID and remove the '-'.
     *
     * @return return the UUID of the length is 32
     */
    public static String getUuId32() {
        return UUID.randomUUID().toString().replaceAll(SEPARATOR_CHAR, StringUtils.EMPTY);
    }

    /**
     * Compress JSON String.
     *
     * @param arg the compress string
     * @return return the value of compressed
     * @throws IOException IOException
     */
    public static String compress(String arg) throws IOException {
        if (null == arg || arg.length() <= 0) {
            return arg;
        }
        ByteArrayOutputStream out = null;
        GZIPOutputStream gzip = null;
        try {
            out = new ByteArrayOutputStream();
            gzip = new GZIPOutputStream(out);
            gzip.write(arg.getBytes(StandardCharsets.UTF_8.toString()));
            close(gzip);
            String value = out.toString(StandardCharsets.ISO_8859_1.toString());
            return value;
        } finally {
            close(out);
        }
    }

    /**
     * Decompression of String data.
     *
     * @param arg String data with decompression
     * @return return the value of decompression
     * @throws IOException IOException
     */
    public static String unCompress(String arg) throws IOException {
        if (null == arg || arg.length() <= 0) {
            return arg;
        }
        ByteArrayOutputStream out = null;
        ByteArrayInputStream in = null;
        GZIPInputStream gzip = null;
        try {
            out = new ByteArrayOutputStream();
            in = new ByteArrayInputStream(arg.getBytes(StandardCharsets.ISO_8859_1.toString()));
            gzip = new GZIPInputStream(in);
            byte[] buffer = new byte[256];
            int n = 0;
            while ((n = gzip.read(buffer)) >= 0) {
                out.write(buffer, 0, n);
            }
            String value = out.toString(StandardCharsets.UTF_8.toString());
            return value;
        } finally {
            close(gzip);
            close(in);
            close(out);
        }
    }

    private static void close(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                logger.error("close OutputStream error", e);
            }
        }
    }

    private static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                logger.error("close InputStream error", e);
            }
        }
    }

    /**
     * Bytes array to bytes 32.
     *
     * @param byteValue the byte value
     * @return the bytes 32
     */
    public static Bytes32 bytesArrayToBytes32(byte[] byteValue) {

        byte[] byteValueLen32 = new byte[32];
        System.arraycopy(byteValue, 0, byteValueLen32, 0, byteValue.length);
        return new Bytes32(byteValueLen32);
    }

    /**
     * String to bytes 32.
     *
     * @param string the string
     * @return the bytes 32
     */
    public static Bytes32 stringToBytes32(String string) {

        byte[] byteValueLen32 = new byte[32];
        if (StringUtils.isEmpty(string)) {
            return new Bytes32(byteValueLen32);
        }
        byte[] byteValue = string.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(byteValue, 0, byteValueLen32, 0, byteValue.length);

        return new Bytes32(byteValueLen32);
    }

    /**
     * Bytes 32 to bytes array.
     *
     * @param bytes32 the bytes 32
     * @return the byte[]
     */
    public static byte[] bytes32ToBytesArray(Bytes32 bytes32) {

        byte[] bytesArray = new byte[32];
        byte[] bytes32Value = bytes32.getValue();
        System.arraycopy(bytes32Value, 0, bytesArray, 0, 32);
        return bytesArray;
    }

    /**
     * Convert a Byte32 data to Java String. IMPORTANT NOTE: Byte to String is not 1:1 mapped. So -
     * Know your data BEFORE do the actual transform! For example, Deximal Bytes, or ASCII Bytes are
     * OK to be in Java String, but Encrypted Data, or raw Signature, are NOT OK.
     *
     * @param bytes32 the bytes 32
     * @return String
     */
    public static String bytes32ToString(Bytes32 bytes32) {

        return new String(bytes32.getValue(), StandardCharsets.UTF_8).trim();
    }

    /**
     * convert byte array to string.
     *
     * @param bytearray byte[]
     * @return String
     */
    public static String byteToString(byte[] bytearray) {
        String result = "";
        char temp;

        int length = bytearray.length;
        for (int i = 0; i < length; i++) {
            temp = (char) bytearray[i];
            result += temp;
        }
        return result;
    }

    /**
     * string to byte.
     *
     * @param value stringData
     * @return byte[]
     */
    public static byte[] stringToByteArray(String value) {
        if (StringUtils.isBlank(value)) {
            return new byte[1];
        }
        return value.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * string to byte32.
     *
     * @param value stringData
     * @return byte[]
     */
    public static byte[] stringToByte32Array(String value) {
        if (StringUtils.isBlank(value)) {
            return new byte[32];
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] newBytes = new byte[32];

        System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
        return newBytes;
    }

    /**
     * string to byte32List.
     *
     * @param data stringData
     * @param size size of byte32List
     * @return data
     */
    public static List<byte[]> stringToByte32ArrayList(String data, int size) {
        List<byte[]> byteList = new ArrayList<>();

        if (StringUtils.isBlank(data)) {
            for (int i = 0; i < size; i++) {
                byteList.add(new byte[32]);
            }
            return byteList;
        }

        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);

        if (dataBytes.length <= WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH) {
            byte[] newBytes = new byte[32];
            System.arraycopy(dataBytes, 0, newBytes, 0, dataBytes.length);
            byteList.add(newBytes);
        } else {
            byteList = splitBytes(dataBytes, size);
        }

        if (byteList.size() < size) {
            List<byte[]> addList = new ArrayList<>();
            for (int i = 0; i < size - byteList.size(); i++) {
                addList.add(new byte[32]);
            }
            byteList.addAll(addList);
        }
        return byteList;
    }

    private static synchronized List<byte[]> splitBytes(byte[] bytes, int size) {
        List<byte[]> byteList = new ArrayList<>();
        double splitLength =
            Double.parseDouble(WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH + "");
        int arrayLength = (int) Math.ceil(bytes.length / splitLength);
        byte[] result = new byte[arrayLength];

        int from = 0;
        int to = 0;

        for (int i = 0; i < arrayLength; i++) {
            from = (int) (i * splitLength);
            to = (int) (from + splitLength);

            if (to > bytes.length) {
                to = bytes.length;
            }

            result = Arrays.copyOfRange(bytes, from, to);
            if (result.length < size) {
                byte[] newBytes = new byte[32];
                System.arraycopy(result, 0, newBytes, 0, result.length);
                byteList.add(newBytes);
            } else {
                byteList.add(result);
            }
        }
        return byteList;
    }

    /**
     * convert bytesArrayList to Bytes32ArrayList.
     *
     * @param list byte size
     * @param size size
     * @return result
     */
    public static List<byte[]> bytesArrayListToBytes32ArrayList(List<byte[]> list, int size) {

        List<byte[]> bytesList = new ArrayList<>();
        if (list.isEmpty()) {
            for (int i = 0; i < size; i++) {
                bytesList.add(new byte[32]);
            }
            return bytesList;
        }

        for (byte[] bytes : list) {
            if (bytes.length <= WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH) {
                byte[] newBytes = new byte[32];
                System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
                bytesList.add(newBytes);
            }
        }

        if (bytesList.size() < size) {
            List<byte[]> addList = new ArrayList<>();
            for (int i = 0; i < size - bytesList.size(); i++) {
                addList.add(new byte[32]);
            }
            bytesList.addAll(addList);
        }
        return bytesList;
    }

    /**
     * Bytes 32 to string without trim.
     *
     * @param bytes32 the bytes 32
     * @return the string
     */
    public static String bytes32ToStringWithoutTrim(Bytes32 bytes32) {

        byte[] strs = bytes32.getValue();
        return new String(strs, StandardCharsets.UTF_8);
    }

    /**
     * Int to uint 256.
     *
     * @param value the value
     * @return the uint 256
     */
    public static Uint256 intToUint256(int value) {
        return new Uint256(new BigInteger(String.valueOf(value)));
    }

    /**
     * Uint 256 to int.
     *
     * @param value the value
     * @return the int
     */
    public static int uint256ToInt(Uint256 value) {
        return value.getValue().intValue();
    }

    /**
     * String to dynamic bytes.
     *
     * @param input the input
     * @return the dynamic bytes
     */
    public static DynamicBytes stringToDynamicBytes(String input) {

        return new DynamicBytes(input.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Dynamic bytes to string.
     *
     * @param input the input
     * @return the string
     */
    public static String dynamicBytesToString(DynamicBytes input) {
        return new String(input.getValue(), StandardCharsets.UTF_8);
    }

    /**
     * Int to int 256.
     *
     * @param value the value
     * @return the int 256
     */
    public static Int256 intToInt256(int value) {
        return new Int256(value);
    }

    /**
     * Int 256 to int.
     *
     * @param value the value
     * @return the int
     */
    public static int int256ToInt(Int256 value) {
        return value.getValue().intValue();
    }

    /**
     * Int to unt 8.
     *
     * @param value the value
     * @return the uint 8
     */
    public static Uint8 intToUnt8(int value) {
        return new Uint8(value);
    }

    /**
     * Uint 8 to int.
     *
     * @param value the value
     * @return the int
     */
    public static int uint8ToInt(Uint8 value) {
        return value.getValue().intValue();
    }

    /**
     * Long to int 256.
     *
     * @param value the value
     * @return the int 256
     */
    public static Int256 longToInt256(long value) {
        return new Int256(value);
    }

    /**
     * Int 256 to long.
     *
     * @param value the value
     * @return the long
     */
    public static long int256ToLong(Int256 value) {
        return value.getValue().longValue();
    }

    /**
     * Long array to int 256 static array.
     *
     * @param longArray the long array
     * @return the static array
     */
    public static StaticArray<Int256> longArrayToInt256StaticArray(long[] longArray) {
        List<Int256> int256List = new ArrayList<Int256>();
        for (int i = 0; i < longArray.length; i++) {
            int256List.add(longToInt256(longArray[i]));
        }
        StaticArray<Int256> in256StaticArray = new StaticArray<Int256>(int256List);
        return in256StaticArray;
    }

    /**
     * String array to bytes 32 static array.
     *
     * @param stringArray the string array
     * @return the static array
     */
    public static StaticArray<Bytes32> stringArrayToBytes32StaticArray(String[] stringArray) {

        List<Bytes32> bytes32List = new ArrayList<Bytes32>();
        for (int i = 0; i < stringArray.length; i++) {
            if (StringUtils.isNotEmpty(stringArray[i])) {
                bytes32List.add(stringToBytes32(stringArray[i]));
            } else {
                bytes32List.add(stringToBytes32(StringUtils.EMPTY));
            }
        }
        StaticArray<Bytes32> bytes32StaticArray = new StaticArray<Bytes32>(bytes32List);
        return bytes32StaticArray;
    }

    /**
     * byte array List to bytes 32 static array.
     *
     * @param bytes the byte array List
     * @return the static array
     */
    public static StaticArray<Bytes32> byteArrayListToBytes32StaticArray(List<byte[]> bytes) {
        List<Bytes32> bytes32List = new ArrayList<Bytes32>();
        for (int i = 0; i < bytes.size(); i++) {
            bytes32List.add(DataToolUtils.bytesArrayToBytes32(bytes.get(i)));
        }
        StaticArray<Bytes32> bytes32StaticArray = new StaticArray<Bytes32>(bytes32List);
        return bytes32StaticArray;
    }

    /**
     * String array to bytes 32 static array.
     *
     * @param addressArray the string array
     * @return the static array
     */
    public static StaticArray<Address> addressArrayToAddressStaticArray(Address[] addressArray) {

        List<Address> addressList = new ArrayList<>();
        for (int i = 0; i < addressArray.length; i++) {
            addressList.add(addressArray[i]);
        }
        StaticArray<Address> addressStaticArray = new StaticArray<Address>(addressList);
        return addressStaticArray;
    }

    /**
     * Bytes 32 dynamic array to string array without trim.
     *
     * @param bytes32DynamicArray the bytes 32 dynamic array
     * @return the string[]
     */
    public static String[] bytes32DynamicArrayToStringArrayWithoutTrim(
        DynamicArray<Bytes32> bytes32DynamicArray) {

        List<Bytes32> bytes32List = bytes32DynamicArray.getValue();
        String[] stringArray = new String[bytes32List.size()];
        for (int i = 0; i < bytes32List.size(); i++) {
            stringArray[i] = bytes32ToStringWithoutTrim(bytes32List.get(i));
        }
        return stringArray;
    }

    /**
     * Bytes 32 dynamic array to stringwithout trim.
     *
     * @param bytes32DynamicArray the bytes 32 dynamic array
     * @return the string
     */
    public static String bytes32DynamicArrayToStringWithoutTrim(
        DynamicArray<Bytes32> bytes32DynamicArray) {

        List<Bytes32> bytes32List = bytes32DynamicArray.getValue();
        List<byte[]> byteArraylist = new ArrayList<>();
        for (int i = 0; i < bytes32List.size(); i++) {
            byteArraylist.add(bytes32ToBytesArray(bytes32List.get(i)));
        }
        return byte32ListToString(byteArraylist, bytes32List.size());
    }

    /**
     * Int 256 dynamic array to long array.
     *
     * @param int256DynamicArray the int 256 dynamic array
     * @return the long[]
     */
    public static long[] int256DynamicArrayToLongArray(DynamicArray<Int256> int256DynamicArray) {

        List<Int256> int256list = int256DynamicArray.getValue();
        long[] longArray = new long[int256list.size()];
        for (int i = 0; i < int256list.size(); i++) {
            longArray[i] = int256ToLong(int256list.get(i));
        }
        return longArray;
    }

    /**
     * convert list to BigInteger list.
     *
     * @param list BigInteger list
     * @param size size
     * @return result
     */
    public static List<BigInteger> listToListBigInteger(List<BigInteger> list, int size) {
        List<BigInteger> bigIntegerList = new ArrayList<>();
        for (BigInteger bs : list) {
            bigIntegerList.add(bs);
        }

        List<BigInteger> addList = new ArrayList<>();
        if (bigIntegerList.size() < size) {
            for (int i = 0; i < size - bigIntegerList.size(); i++) {
                addList.add(BigInteger.ZERO);
            }
            bigIntegerList.addAll(addList);
        }
        return bigIntegerList;
    }

    /**
     * Check if the byte array is empty.
     *
     * @param byteArray the byte[]
     * @return true if empty, false otherwise
     */
    public static boolean isByteArrayEmpty(byte[] byteArray) {
        for (int index = 0; index < byteArray.length; index++) {
            if (byteArray[index] != 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * convert byte32List to String.
     *
     * @param bytesList list
     * @param size size
     * @return reuslt
     */
    public static synchronized String byte32ListToString(List<byte[]> bytesList, int size) {
        if (bytesList.isEmpty()) {
            return "";
        }

        int zeroCount = 0;
        for (int i = 0; i < bytesList.size(); i++) {
            for (int j = 0; j < bytesList.get(i).length; j++) {
                if (bytesList.get(i)[j] == 0) {
                    zeroCount++;
                }
            }
        }

        if (WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH * size - zeroCount == 0) {
            return "";
        }

        byte[] newByte = new byte[WeIdConstant.MAX_AUTHORITY_ISSUER_NAME_LENGTH * size - zeroCount];
        int index = 0;
        for (int i = 0; i < bytesList.size(); i++) {
            for (int j = 0; j < bytesList.get(i).length; j++) {
                if (bytesList.get(i)[j] != 0) {
                    newByte[index] = bytesList.get(i)[j];
                    index++;
                }
            }
        }

        return (new String(newByte)).toString();
    }

    /**
     * Get the current timestamp as the param "created". May be called elsewhere.
     *
     * @param length length
     * @return the StaticArray
     */
    public static List<BigInteger> getParamCreatedList(int length) {
        long created = DateUtils.getNoMillisecondTimeStamp();
        List<BigInteger> createdList = new ArrayList<>();
        createdList.add(BigInteger.ZERO);
        createdList.add(BigInteger.valueOf(created));
        return createdList;
    }

    /**
     * convert timestamp to UTC of json string.
     *
     * @param jsonString json string
     * @return timestampToUtcString
     */
    public static String convertTimestampToUtc(String jsonString) {
        String timestampToUtcString;
        try {
            timestampToUtcString = dealNodeOfConvertUtcAndLong(
                loadJsonObject(jsonString),
                CONVERT_UTC_LONG_KEYLIST,
                TO_JSON
            ).toString();
        } catch (IOException e) {
            logger.error("replaceJsonObj exception.", e);
            throw new DataTypeCastException(e);
        }
        return timestampToUtcString;
    }

    /**
     * convert UTC Date to timestamp of Json string.
     *
     * @param jsonString presentationJson
     * @return presentationJson after convert
     */
    public static String convertUtcToTimestamp(String jsonString) {
        String utcToTimestampString;
        try {
            utcToTimestampString = dealNodeOfConvertUtcAndLong(
                loadJsonObject(jsonString),
                CONVERT_UTC_LONG_KEYLIST,
                FROM_JSON
            ).toString();
        } catch (IOException e) {
            logger.error("replaceJsonObj exception.", e);
            throw new DataTypeCastException(e);
        }
        return utcToTimestampString;
    }

    private static JsonNode dealNodeOfConvertUtcAndLong(
        JsonNode jsonObj,
        List<String> list,
        String type) {
        if (jsonObj.isObject()) {
            return dealObjectOfConvertUtcAndLong((ObjectNode) jsonObj, list, type);
        } else if (jsonObj.isArray()) {
            return dealArrayOfConvertUtcAndLong((ArrayNode) jsonObj, list, type);
        } else {
            return jsonObj;
        }
    }

    private static JsonNode dealObjectOfConvertUtcAndLong(
        ObjectNode jsonObj,
        List<String> list,
        String type) {
        ObjectNode resJson = OBJECT_MAPPER.createObjectNode();
        jsonObj.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode obj = entry.getValue();
            if (obj.isObject()) {
                //JSONObject
                if (key.equals(KEY_CLAIM)) {
                    resJson.set(key, obj);
                } else {
                    resJson.set(key, dealObjectOfConvertUtcAndLong((ObjectNode) obj, list, type));
                }
            } else if (obj.isArray()) {
                //JSONArray 
                resJson.set(key, dealArrayOfConvertUtcAndLong((ArrayNode) obj, list, type));
            } else {
                if (list.contains(key)) {
                    if (TO_JSON.equals(type)) {
                        if (isValidLongString(obj.asText())) {
                            resJson.put(
                                key,
                                DateUtils.convertNoMillisecondTimestampToUtc(
                                    Long.parseLong(obj.asText())));
                        } else {
                            resJson.set(key, obj);
                        }
                    } else {
                        if (DateUtils.isValidDateString(obj.asText())) {
                            resJson.put(
                                key,
                                DateUtils.convertUtcDateToNoMillisecondTime(obj.asText()));
                        } else {
                            resJson.set(key, obj);
                        }
                    }
                } else {
                    resJson.set(key, obj);
                }
            }
        });
        return resJson;
    }

    private static JsonNode dealArrayOfConvertUtcAndLong(
        ArrayNode jsonArr,
        List<String> list,
        String type) {
        ArrayNode resJson = OBJECT_MAPPER.createArrayNode();
        for (int i = 0; i < jsonArr.size(); i++) {
            JsonNode jsonObj = jsonArr.get(i);
            if (jsonObj.isObject()) {
                resJson.add(dealObjectOfConvertUtcAndLong((ObjectNode) jsonObj, list, type));
            } else if (jsonObj.isArray()) {
                resJson.add(dealArrayOfConvertUtcAndLong((ArrayNode) jsonObj, list, type));
            } else {
                resJson.add(jsonObj);
            }
        }
        return resJson;
    }

    /**
     * valid string is a long type.
     *
     * @param str string
     * @return result
     */
    public static boolean isValidLongString(String str) {
        if (StringUtils.isBlank(str)) {
            return false;
        }

        long result = 0;
        int i = 0;
        int len = str.length();
        long limit = -Long.MAX_VALUE;
        long multmin;
        int digit;

        char firstChar = str.charAt(0);
        if (firstChar <= '0') {
            return false;
        }
        multmin = limit / radix;
        while (i < len) {
            digit = Character.digit(str.charAt(i++), radix);
            if (digit < 0) {
                return false;
            }
            if (result < multmin) {
                return false;
            }
            result *= radix;
            if (result < limit + digit) {
                return false;
            }
            result -= digit;
        }
        return true;
    }

    /**
     * valid the json string is converted by toJson().
     *
     * @param json jsonString
     * @return result
     */
    public static boolean isValidFromToJson(String json) {
        if (StringUtils.isBlank(json)) {
            logger.error("input json param is null.");
            return false;
        }
        JsonNode jsonObject = null;
        try {
            jsonObject = loadJsonObject(json);
        } catch (IOException e) {
            logger.error("convert jsonString to JSONObject failed." + e);
            return false;
        }
        return jsonObject.has(KEY_FROM_TOJSON);
    }

    /**
     * add tag which the json string is converted by toJson().
     *
     * @param json jsonString
     * @return result
     */
    public static String addTagFromToJson(String json) {
        JsonNode jsonObject;
        try {
            jsonObject = loadJsonObject(json);
            if (!jsonObject.has(KEY_FROM_TOJSON)) {
                ((ObjectNode) jsonObject).put(KEY_FROM_TOJSON, TO_JSON);
            }
        } catch (IOException e) {
            logger.error("addTagFromToJson fail." + e);
            return json;
        }
        return jsonObject.toString();
    }

    /**
     * remove tag which the json string is converted by toJson().
     *
     * @param json jsonString
     * @return result
     */
    public static String removeTagFromToJson(String json) {
        JsonNode jsonObject;
        try {
            jsonObject = loadJsonObject(json);
            if (jsonObject.has(KEY_FROM_TOJSON)) {
                ((ObjectNode) jsonObject).remove(KEY_FROM_TOJSON);
            }
        } catch (IOException e) {
            logger.error("removeTag fail." + e);
            return json;
        }
        return jsonObject.toString();
    }

    /**
     * check if the input string is Uft-8.
     *
     * @param string input
     * @return true, otherwise false
     */
    public static boolean isUtf8String(String string) {
        try {
            string.getBytes("UTF-8");
            return true;
        } catch (UnsupportedEncodingException e) {
            logger.error("Passed-in String is not a valid UTF-8 String.");
        }
        return false;
    }

    /**
     * Convert a hash string (0x[64Bytes]) into a byte array with 32 bytes length by compressing
     * each two nearby characters into one.
     *
     * @param hash hash String
     * @return byte array
     */
    public static byte[] convertHashStrIntoHashByte32Array(String hash) {
        if (!isValidHash(hash)) {
            return null;
        }
        byte[] originHashByte = hash.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[WeIdConstant.BYTES32_FIXED_LENGTH];
        for (int i = 0; i < WeIdConstant.BYTES32_FIXED_LENGTH; i++) {
            String hex = new String(
                new byte[]{originHashByte[2 + i * 2], originHashByte[3 + i * 2]});
            int val = Integer.parseInt(hex, 16);
            result[i] = (byte) val;
        }
        return result;
    }

    /**
     * Convert a byte array with 32 bytes into a hash String by stretching the two halfs of a hex
     * byte into two separate hex string. Padding with zeros must be kept in mind.
     *
     * @param hash hash byte array
     * @return hash String
     */
    public static String convertHashByte32ArrayIntoHashStr(byte[] hash) {
        StringBuilder convertedBackStr = new StringBuilder().append(WeIdConstant.HEX_PREFIX);
        for (int i = 0; i < WeIdConstant.BYTES32_FIXED_LENGTH; i++) {
            String hex = Integer
                .toHexString(((int) hash[i]) >= 0 ? ((int) hash[i]) : ((int) hash[i]) + 256);
            if (hex.length() == 1) {
                hex = "0" + hex;
            }
            convertedBackStr.append(hex);
        }
        return convertedBackStr.toString();
    }

    /**
     * An intermediate fix to convert Bytes32 Object List from web3sdk 2.x into a real String list.
     *
     * @param byteList Bytes32 Object list
     * @return hash String list
     */
    public static List<String> convertBytes32ObjectListToStringHashList(
        List<Bytes32> byteList) {
        List<String> strList = new ArrayList<>();
        for (int i = 0; i < byteList.size(); i++) {
            strList.add(DataToolUtils.convertHashByte32ArrayIntoHashStr(
                //((org.fisco.bcos.web3j.abi.datatypes.generated.Bytes32) (byteList.toArray()[i]))
                    ((Bytes32) (byteList.toArray()[i]))
                    .getValue()));
        }
        return strList;
    }

    /**
     * Strictly check two lists' elements existence whether items in src exists in dst list or not.
     *
     * @param src source list
     * @param dst dest list
     * @return boolean list, each true / false indicating existing or not.
     */
    public static List<Boolean> strictCheckExistence(List<String> src, List<String> dst) {
        List<Boolean> result = new ArrayList<>();
        int index = 0;
        for (int i = 0; i < src.size(); i++) {
            if (src.get(i).equalsIgnoreCase(dst.get(index))) {
                result.add(true);
                index++;
            } else {
                result.add(false);
            }
        }
        return result;
    }
}


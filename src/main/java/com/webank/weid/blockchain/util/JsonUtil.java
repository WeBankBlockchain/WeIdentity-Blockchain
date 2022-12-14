

package com.webank.weid.blockchain.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.POJONode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.codemodel.ClassType;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JType;
import com.webank.weid.blockchain.exception.DataTypeCastException;
import com.webank.weid.blockchain.protocol.base.ClaimPolicy;
import com.webank.wedpr.selectivedisclosure.PredicateType;
import com.webank.weid.blockchain.constant.CredentialConstant;
import com.webank.weid.blockchain.constant.JsonSchemaConstant;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.RuleFactory;

public class JsonUtil {

    private static final Pattern PATTERN = Pattern.compile("^\\[[0-9]{1,}\\]$");
    private static final Pattern PATTERN_ARRAY = Pattern.compile("(?<=\\[)([0-9]{1,})(?=\\])");
    private static final Pattern PATTERN_KEY = Pattern.compile(".*?(?=\\[[0-9]{1,}\\])");

    private static final String KEY_CHAR = ".";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * ??????Map?????????JsonSchema????????????????????????Key?????????(??????).
     *
     * @param cptJsonSchema Map?????????JsonSchema
     * @return ????????????Key?????????
     * @throws IOException ???????????????????????????JSON???????????????
     */
    public static List<String> extractCptProperties(Map<String, Object> cptJsonSchema)
        throws IOException {

        return extractCptProperties(DataToolUtils.serialize(cptJsonSchema));
    }

    /**
     * ??????Json?????????JsonSchema????????????????????????Key?????????(??????).
     *
     * @param cptJsonSchema Json?????????JsonSchema
     * @return ????????????Key?????????
     * @throws IOException ???????????????????????????JSON???????????????
     */
    public static List<String> extractCptProperties(String cptJsonSchema) throws IOException {

        JDefinedClass dc = getDefinedClass(cptJsonSchema);
        Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
        List<String> resultList = new ArrayList<String>();
        if (dc != null) {
            dc.fields().entrySet().forEach(entry -> buildFromDefinedClass(resultMap, entry));
            //???????????????
            //Map<String, Object> result = replenishMeta(resultMap, new CredentialPojo());
            Map<String, Object> result = replenishMeta(resultMap);
            //???map???????????????json
            String value = jsonToMonolayer(DataToolUtils.serialize(result), 10);
            //????????????Json???key?????????
            toJsonNode(value)
                .fieldNames().forEachRemaining(resultList::add);
        }
        return resultList;
    }

    /*
     * ???????????????.
     */
    private static Map<String, Object> replenishMeta(
        Map<String, Object> claim) {

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        /*if (credential != null) {
            result.put(CredentialConstant.ID, credential.getId());
            result.put(CredentialConstant.CREDENTIAL_META_KEY_CPTID, credential.getCptId());
            result.put(CredentialConstant.CREDENTIAL_META_KEY_CONTEXT, credential.getContext());
            result.put(CredentialConstant.CREDENTIAL_META_KEY_ISSUER, credential.getIssuer());
            result
                .put(CredentialConstant.CREDENTIAL_META_KEY_ISSUANCEDATE,
                    credential.getIssuanceDate());
            result.put(CredentialConstant.CREDENTIAL_META_KEY_EXPIRATIONDATE,
                credential.getExpirationDate());
            result.put("claim", claim);
        }*/
        result.put(CredentialConstant.ID, null);
        result.put(CredentialConstant.CREDENTIAL_META_KEY_CPTID, null);
        result.put(CredentialConstant.CREDENTIAL_META_KEY_CONTEXT, null);
        result.put(CredentialConstant.CREDENTIAL_META_KEY_ISSUER, null);
        result
                .put(CredentialConstant.CREDENTIAL_META_KEY_ISSUANCEDATE,
                        null);
        result.put(CredentialConstant.CREDENTIAL_META_KEY_EXPIRATIONDATE,
                null);
        result.put("claim", claim);
        return result;
    }

    /*
     * ??????SchemaMapper???????????????Class,??????Schema to  pojo.
     */
    private static JDefinedClass getDefinedClass(String cptJson) throws IOException {

        JCodeModel codeModel = new JCodeModel();
        GenerationConfig config = new DefaultGenerationConfig() {
            @Override
            public boolean isGenerateBuilders() {
                return true;
            }
        };
        SchemaMapper schemaMapper = new SchemaMapper(
            new RuleFactory(config, new Jackson2Annotator(config), new SchemaStore()),
            new SchemaGenerator());
        schemaMapper.generate(codeModel, "Example", "cpt", cptJson);
        return codeModel._getClass("cpt.Example");
    }

    /*
     * ??????JDefinedClass ???????????????????????????????????????key.
     */
    private static void buildFromDefinedClass(
        Map<String, Object> resultMap,
        Entry<String, JFieldVar> entry) {

        String key = entry.getKey();
        if (StringUtils.equals(key, JsonSchemaConstant.ADDITIONAL_PROPERTIES)) {
            return;
        }
        resultMap.put(key, null);
        JType jtype = entry.getValue().type();
        if (jtype instanceof JDefinedClass) {
            buildByType(resultMap, key, jtype);
        } else if (jtype instanceof JClass) {
            JClass jclass = (JClass) jtype;
            List<JClass> list = jclass.getTypeParameters();
            if (list.size() > 0) {
                buildByType(resultMap, key, list.get(0));
                fixArray(resultMap, key);
            }
        }
    }

    /*
     * ???????????????????????????????????????????????????.
     */
    private static void fixArray(Map<String, Object> resultMap, String key) {

        ArrayList<Object> objList = new ArrayList<>();
        IntStream.range(0, maxArraySize()).forEach(i -> objList.add(resultMap.get(key)));
        resultMap.put(key, objList);
    }

    /*
     * ??????Jtype?????????????????????????????????.
     */
    private static void buildByType(Map<String, Object> resultMap, String key, JType jt) {

        if (jt instanceof JDefinedClass) {
            JDefinedClass dc = (JDefinedClass) jt;
            if (dc.getClassType() == ClassType.CLASS) {
                Map<String, Object> map = new LinkedHashMap<String, Object>();
                dc.fields().entrySet().forEach(entry -> buildFromDefinedClass(map, entry));
                resultMap.put(key, map);
            }
        }
    }

    private static int maxArraySize() {

        return Integer.parseInt(PropertyUtils.getProperty("zkp.cpt.array.length", "-1"));
    }

    private static Map<String, String> monolayerToMap(String json) throws IOException {

        JsonNode resultNode = toJsonNode(json);
        Map<String, String> resultMap = new HashMap<String, String>();
        resultNode.fields()
            .forEachRemaining(node -> resultMap.put(node.getKey(), node.getValue().asText()));
        return resultMap;
    }

    /**
     * ???ClaimPolicy???????????????Json,???????????????????????????(??????)??????????????????????????????????????? ????????????????????????????????????.
     *
     * @param claimPolicy ????????????
     * @return ????????????????????????Json
     * @throws IOException ???????????????????????????JSON???????????????
     */
    public static String claimPolicyToMonolayer(ClaimPolicy claimPolicy) throws IOException {

        ObjectNode objectNode = (ObjectNode) toJsonNode(
            claimPolicy.getFieldsToBeDisclosed());
        return jsonToMonolayer(objectNode, maxArraySize(), 0);
    }

    /**
     * ?????????Json???????????????Json??????????????????.
     *
     * @param json ??????Json?????????
     * @param radix ?????????????????????
     * @return ????????????Json
     * @throws IOException ???????????????????????????JSON???????????????
     */
    public static String jsonToMonolayer(String json, int radix) throws IOException {

        return jsonToMonolayer(toJsonNode(json), -1, radix);
    }

    /**
     * ?????????Json???????????????Json.
     *
     * @param jsonNode ?????????JsonNode
     * @param radix ?????????????????????
     * @return ????????????Json
     */
    public static String jsonToMonolayer(JsonNode jsonNode, int radix) {

        return jsonToMonolayer(jsonNode, -1, radix);
    }

    /*
     * ?????????????????????.
     */
    private static String jsonToMonolayer(JsonNode jsonNode, int maxSize, int radix) {

        JsonNode resultJson = MAPPER.createObjectNode();
        jsonNode.fields().forEachRemaining(node ->
            parseJsonToNode(resultJson, node, new ArrayList<String>(), maxSize, radix));
        return resultJson.toString();
    }

    /*
     * ???????????????JsonNode???????????????????????????JsonNode(Object)???.
     */
    private static void parseJsonToNode(

        JsonNode resultJson,
        Entry<String, JsonNode> entry,
        List<String> names,
        int maxSize,
        int radix) {

        names.add(entry.getKey());
        processNode(resultJson, entry.getValue(), names, maxSize, radix);
    }

    /*
     * ???????????????JsonNode???????????????????????????JsonNode(Array)???.
     */
    private static void parseJsonToArray(

        JsonNode resultJson,
        JsonNode value,
        List<String> names,
        int index,
        int maxSize,
        int radix) {

        names.add("[" + index + "]");
        processNode(resultJson, value, names, maxSize, radix);
    }

    /*
     * ???????????????????????????Object???Array??????????????????.
     */
    private static void processNode(
        JsonNode resultJson,
        JsonNode value,
        List<String> names,
        int maxSize,
        int radix) {

        if (value.isObject() && !isBottom(value)) {

            value.fields().forEachRemaining(node ->
                parseJsonToNode(resultJson, node, new ArrayList<String>(names), maxSize, radix));
        } else if (value.isArray() && !isSampleArray(value)) {
            fixLengthForArrayNode(value, maxSize);
            value.forEach(consumerWithIndex((node, index) ->
                parseJsonToArray(
                    resultJson,
                    node,
                    new ArrayList<String>(names),
                    index,
                    maxSize,
                    radix)));
        } else {
            buildValue(
                (ObjectNode) resultJson,
                buildKey(names),
                value,
                ConvertType.STRING_TO_DECIMAL,
                radix);
        }
    }

    /*
     * ????????????????????????.
     */
    private static void fixLengthForArrayNode(JsonNode value, int maxSize) {

        //??????????????????
        if (maxSize != -1 && value.size() > maxSize) { //?????????????????????????????????????????????
            throw new RuntimeException("the array size:" + value.size() + ", maxSize:" + maxSize);
        }
        ArrayNode array = (ArrayNode) value;
        if (maxSize - value.size() > 0) {
            JsonNode jsonNode = cloneNodewithNullNode(array.get(array.size() - 1));
            IntStream.range(0, maxSize - value.size()).forEach(i -> array.add(jsonNode));
        }
    }

    /*
     * ??????????????????.
     */
    private static JsonNode cloneNodewithNullNode(JsonNode node) {

        if (node.isObject()) {
            ObjectNode objectNode = MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry ->
                cloneObjectNode(objectNode, entry.getKey(), entry.getValue()));
            return objectNode;
        } else if (node.isArray()) {
            ArrayNode arrayNode = MAPPER.createArrayNode();
            node.forEach(childNode -> arrayNode.add(cloneNodewithNullNode(childNode)));
            return arrayNode;
        } else {
            return TextNode.valueOf("null");
        }
    }

    /*
     * ??????????????????.
     */
    private static void cloneObjectNode(ObjectNode resultNode, String key, JsonNode value) {

        if (value.isObject()) {
            resultNode.set(key, cloneNodewithNullNode(value));
        } else if (value.isArray()) {
            ArrayNode array = resultNode.putArray(key);
            value.forEach(childNode -> array.add(cloneNodewithNullNode(childNode)));
        } else {
            if (value.isBigDecimal()) {
                resultNode.set(key, IntNode.valueOf(0));
            } else {
                resultNode.set(key, TextNode.valueOf("null"));
            }
        }
    }

    private static boolean isBottom(JsonNode value) {

        if (value.isValueNode()) {
            return true;
        } else if (!value.isObject()) {
            return false;
        }
        for (PredicateType type : PredicateType.values()) {
            if (value.has(type.toString())) {
                return true;
            }
        }
        return false;
    }

    /*
     * ????????????????????????????????????.
     */
    private static boolean isSampleArray(JsonNode value) {

        //??????????????????
        if (!value.isArray()) {
            return false;
        }
        return isSampleArray((ArrayNode) value);
    }

    /*
     * ????????????????????????????????????.
     */
    private static boolean isSampleArray(ArrayNode array) {

        for (JsonNode jsonNode : array) {
            if (jsonNode.isObject()) {
                return false;
            } else if (jsonNode.isArray()) {
                if (!isSampleArray((ArrayNode) jsonNode)) {
                    return false;
                }
            }
        }
        return true;
    }

    /*
     * ????????????????????????key???????????????key.
     */
    private static String buildKey(List<String> names) {

        StringBuilder buildKey = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            buildKey.append(names.get(i));
            if (i < names.size() - 1) {
                String keyString = names.get(i + 1);
                if (PATTERN.matcher(keyString).matches()) {
                    continue;
                }
                buildKey.append(KEY_CHAR);
            }
        }
        return buildKey.toString();
    }

    /*
     * ?????????????????????????????????.
     */
    private static void buildValue(
        ObjectNode resultJson,
        String key,
        JsonNode value,
        ConvertType type,
        int radix) {

        if (value.isObject()) {
            resultJson.putPOJO(key, value);
        } else if (value.isInt()) {
            resultJson.put(key, value.asInt());
        } else if (value.isLong()) {
            resultJson.put(key, value.asLong());
        } else if (value.isBigDecimal()) {
            resultJson.put(key, value.asDouble());
        } else {
            buildOtherValue(resultJson, key, value, type, radix);
        }
    }

    /*
     * ???????????????????????????????????????????????????????????????.
     */
    private static void buildOtherValue(
        ObjectNode resultJson,
        String key,
        JsonNode value,
        ConvertType type,
        int radix) {

        String strValue = null;
        if (!value.isNull()) {
            String nodeValue = value.asText();
            if (type == ConvertType.DECIMAL_TO_STRING) {
                strValue = decimalToString(nodeValue, radix);
                JsonNode vauleNode = toJsonNode(strValue);
                if (vauleNode != null && vauleNode.isArray()) {
                    resultJson.putPOJO(key, vauleNode);
                    return;
                }
            } else if (type == ConvertType.STRING_TO_DECIMAL) {
                if (value.isArray()) {
                    nodeValue = value.toString();
                }
                strValue = toDecimal(nodeValue, radix);
            }
        }
        resultJson.put(key, strValue);
    }

    /*
     * ?????????????????????JsonNode.
     */
    private static JsonNode toJsonNode(String jsonString)  {
        try {
            return MAPPER.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new DataTypeCastException(e);
        }
    }

    /*
     * ????????????10?????????????????????.
     */
    private static String toDecimal(String value, int radix) {

        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }
        if (radix == 0) {
            return value;
        }
        return new BigInteger(1, value.getBytes(StandardCharsets.UTF_8)).toString(radix);
    }

    /*
     * ???10??????????????????????????????????????????.
     */
    private static String decimalToString(String value, int radix) {

        if (StringUtils.isBlank(value)) {
            return StringUtils.EMPTY;
        }
        if (radix == 0) {
            return value;
        }
        return new String(new BigInteger(value, radix).toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * ????????????????????????.
     *
     * @param <T> ???????????????????????????
     * @param consumer ?????????????????????Consumer
     * @return ????????????????????????????????????????????????
     */
    public static <T> Consumer<T> consumerWithIndex(BiConsumer<T, Integer> consumer) {

        class Index {

            int index;
        }

        Index indexObj = new Index();
        return t -> {
            int index = indexObj.index++;
            consumer.accept(t, index);
        };
    }

    /**
     * ?????????Json???????????????Json.
     *
     * @param json ??????Json?????????
     * @param radix ?????????????????????
     * @return ?????????????????????Json?????????
     * @throws IOException ???????????????????????????JSON???????????????
     */
    public static String monolayerToJson(String json, int radix) throws IOException {

        return monolayerToJson(toJsonNode(json), radix);
    }

    /**
     * ?????????Json???????????????Json.
     *
     * @param jsonNode ??????JsonNode??????
     * @param radix ?????????????????????
     * @return ?????????????????????Json?????????
     */
    public static String monolayerToJson(JsonNode jsonNode, int radix) {

        ObjectNode resultJson = MAPPER.createObjectNode();
        jsonNode.fields().forEachRemaining(entry ->
            parseKeyToMap(resultJson, parseKey(entry.getKey()), entry.getValue(), radix));
        return resultJson.toString();
    }

    /*
     * ????????????key???????????????key.
     */
    private static LinkedList<String> parseKey(String keysString) {

        return new LinkedList<String>(Arrays.asList(keysString.split("\\" + KEY_CHAR)));
    }

    /*
     * ?????????key??????????????????????????????JsonNode???.
     */
    private static void parseKeyToMap(
        ObjectNode resultJson,
        LinkedList<String> keyList,
        JsonNode value,
        int radix) {

        String key = keyList.removeFirst();
        List<Integer> indexList = getIndexList(key);
        if (indexList.size() > 0) { //?????????????????????
            addArray(resultJson, getReallyKey(key), indexList, value, keyList, radix);
        } else {
            addObject(resultJson, key, value, keyList, radix);
        }
    }

    /*
     * ????????????key????????????key.
     */
    private static String getReallyKey(String arrayKey) {

        Matcher matcher = PATTERN_KEY.matcher(arrayKey);
        String reallyKey = StringUtils.EMPTY;
        if (matcher.find()) {
            reallyKey = matcher.group();
        }
        return reallyKey;
    }

    /*
     * ??????????????????Object???.
     */
    private static void addObject(
        ObjectNode resultJson,
        String key,
        JsonNode value,
        LinkedList<String> keyList,
        int radix) {

        //???????????????????????????
        if (keyList.size() == 0) {
            buildValue(resultJson, key, value, ConvertType.DECIMAL_TO_STRING, radix);
            return;
        }
        if (!resultJson.has(key)) {
            resultJson.putObject(key);
        }
        parseKeyToMap((ObjectNode) resultJson.get(key), keyList, value, radix);
    }

    /*
     * ??????????????????Array???.
     */
    private static void addArray(
        ObjectNode resultJson,
        String reallyKey,
        List<Integer> indexList,
        JsonNode value,
        LinkedList<String> keyList,
        int radix) {

        if (!resultJson.has(reallyKey)) {
            resultJson.putArray(reallyKey);
        }
        putArrayValue((ArrayNode) resultJson.get(reallyKey), 0, indexList, value, keyList, radix);
    }

    /*
     * ??????????????????.
     */
    private static void putArrayValue(
        ArrayNode jsonArray,
        int level,
        List<Integer> indexList,
        JsonNode value,
        LinkedList<String> keyList,
        int radix) {

        //??????????????????
        if (level == indexList.size() - 1) {
            //??????????????????????????????
            if (jsonArray.size() - 1 < indexList.get(level)) {
                IntStream.range(0, indexList.get(level) + 1 - jsonArray.size())
                    .forEach(i -> jsonArray.addObject());
            }
            //?????????????????????
            if (keyList.size() == 0) {
                jsonArray.set(indexList.get(level),
                    new POJONode(decimalToString(value.asText(), radix)));
            } else {
                //???????????????Map
                JsonNode jsonObj = jsonArray.get(indexList.get(level));
                parseKeyToMap((ObjectNode) jsonObj, keyList, value, radix);
                jsonArray.set(indexList.get(level), jsonObj);
            }
        } else {
            //???????????????????????????
            if (jsonArray.size() - 1 < indexList.get(level)) {
                //??????????????????????????????
                IntStream.range(0, indexList.get(level) + 1 - jsonArray.size())
                    .forEach(i -> jsonArray.addArray());
            }
            putArrayValue(
                (ArrayNode) jsonArray.get(indexList.get(level)),
                level + 1,
                indexList,
                value,
                keyList,
                radix);
        }
    }

    /*
     * ??????????????????????????????????????????????????????.
     */
    private static List<Integer> getIndexList(String key) {
        Matcher matcher = PATTERN_ARRAY.matcher(key);
        List<Integer> indexList = new ArrayList<Integer>();
        while (matcher.find()) {
            indexList.add(Integer.parseInt(matcher.group()));
        }
        return indexList;
    }

    private enum ConvertType {
        STRING_TO_DECIMAL, DECIMAL_TO_STRING
    }


}

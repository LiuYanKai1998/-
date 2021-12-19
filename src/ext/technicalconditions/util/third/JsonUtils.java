package ext.technicalconditions.util.third;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;


/**
 * 
 *
 *Zhong Binpeng May 28, 2020
 */
public class JsonUtils {

    private static final Logger log = LoggerFactory.getLogger(JsonUtils.class);
    
    private static ObjectMapper objectMapper = null;
    private static XmlMapper xmlMapper = null;

    static {
        objectMapper = initObjectMapper();
        xmlMapper = initXmlMapper();
    }
    
    private static ObjectMapper initObjectMapper() {
        ObjectMapper newObjectMapper = new ObjectMapper();
        // 设置默认日期格式
        // newObjectMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));

        newObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        newObjectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        newObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        newObjectMapper.disable(MapperFeature.USE_GETTERS_AS_SETTERS);

        return newObjectMapper;
    }
    
    private static XmlMapper initXmlMapper() {
        XmlMapper newXmlMapper = new XmlMapper();
        
        //newXmlMapper.findAndRegisterModules();
        //newXmlMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        newXmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
        newXmlMapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        newXmlMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        //newXmlMapper.setDefaultUseWrapper(false);
        newXmlMapper.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true);
        
        return newXmlMapper;
    }

    /**
     * 将对象转换成json字符串格式（默认将转换所有的属性）
     *
     * @param value
     * @return
     */
    public static String toJsonStr(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.error("Json转换失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转换成json字符串格式（默认将转换所有的属性）
     *
     * @param value
     * @return
     */
    public static byte[] toJsonBytes(Object value) {
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (JsonProcessingException e) {
            log.error("Json转换失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 将对象转换成json字符串格式
     *
     * @param value
     *            需要转换的对象
     * @param properties
     *            需要转换的属性
     */
    public static String toJsonStr(Object value, String[] properties) {
        try {
            SimpleBeanPropertyFilter sbp = SimpleBeanPropertyFilter
                    .filterOutAllExcept(properties);
            FilterProvider filterProvider = new SimpleFilterProvider()
                    .addFilter("propertyFilterMixIn", sbp);
            return objectMapper.writer(filterProvider)
                    .writeValueAsString(value);
        } catch (Exception e) {
            log.error("Json转换失败", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 将对象转换成json字符串格式
     *
     * @param value
     *            需要转换的对象
     * @param properties2Exclude
     *            需要排除的属性
     */
    public static String toJsonStrWithExcludeProperties(Object value,
                                                        String[] properties2Exclude) {
        try {
            SimpleBeanPropertyFilter sbp = SimpleBeanPropertyFilter
                    .serializeAllExcept(properties2Exclude);
            FilterProvider filterProvider = new SimpleFilterProvider()
                    .addFilter("propertyFilterMixIn", sbp);
            return objectMapper.writer(filterProvider)
                    .writeValueAsString(value);
        } catch (Exception e) {
            log.error("Json转换失败", e);
            throw new RuntimeException(e);
        }

    }

    /**
     * 将对象json格式直接写出到流对象中（默认将转换所有的属性）
     *
     * @param out
     * @return
     */
    public static void writeJsonStr(OutputStream out, Object value) {
        try {
            objectMapper.writeValue(out, value);
        } catch (Exception e) {
            log.error("Json转换失败", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 反序列化POJO或简单Collection如List<String>.
     *
     * 如果JSON字符串为Null或"null"字符串, 返回Null. 如果JSON字符串为"[]", 返回空集合.
     *
     * 如需反序列化复杂Collection如List<MyBean>, 请使用fromJson(String, JavaType)
     */
    public static <T> T fromJson(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            return objectMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            log.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 反序列化POJO或简单Collection如List<String>.
     *
     * 如果JSON字符串为Null或"null"字符串, 返回Null. 如果JSON字符串为"[]", 返回空集合.
     *
     * 如需反序列化复杂Collection如List<MyBean>, 请使用fromJson(String, JavaType)
     */
    public static <T> T fromJson(String jsonString, Class<T> clazz, Class<?>... elementClasses) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }

        try {
            if(elementClasses.length==0) {
                return objectMapper.readValue(jsonString, clazz);
            } else {
                return objectMapper.readValue(jsonString, getGenericsType(clazz, elementClasses));
            }
        } catch (IOException e) {
            log.warn("parse json string error:" + jsonString, e);
            return null;
        }
    }

    /**
     * 获取泛型的Collection Type
     *
     * @param collectionClass
     *            泛型的Collection
     * @param elementClasses
     *            元素类
     * @return JavaType Java类型
     * @since 1.0
     */
    public static JavaType getGenericsType(Class<?> collectionClass, Class<?>... elementClasses) {
        return objectMapper.getTypeFactory().constructParametricType(collectionClass, elementClasses);
    }
    
    /**
     * 对象转为XML字符串
     * @param value
     * @return
     * @throws JsonProcessingException
     */
    public static String toXMLStr(Object value) throws JsonProcessingException {
        return xmlMapper.writeValueAsString(value);
    }
    
    /**
     * 对象转xml写入流
     * @param value
     * @return
     * @throws IOException 
     */
    public static void writeXmlStr(OutputStream os, Object value) throws IOException {
        xmlMapper.writeValue(os, value);
    }
    
}

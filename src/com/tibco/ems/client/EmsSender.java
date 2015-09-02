/**
 * Created By: Comwave Project Team Created Date: 2015年6月5日
 */
package com.tibco.ems.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.jms.JMSException;
import javax.jms.Message;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Geln Yang
 * @version 1.0
 */
public class EmsSender {

    private static final Logger logger = LoggerFactory.getLogger(EmsSender.class);

    private static String serverUrl = "tcp://emsserver1:7222,tcp://emsserver2:7222";

    private static String userName = "admin";

    private static String password = "";

    static {
        try {
            InputStream is = EmsSender.class.getResourceAsStream("/tibco_ems_client.properties");
            Properties properties = new Properties();
            properties.load(is);
            String url = properties.getProperty("eaijava.jms.url");
            if (url != null) {
                serverUrl = url;
            }
            String user = properties.getProperty("eaijava.jms.user");
            if (user != null) {
                userName = user;
            }
            String pass = properties.getProperty("eaijava.jms.password");
            if (pass != null) {
                password = pass;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public static String sendString(String queue, String content) throws JMSException,
            InterruptedException {
        EmsProducer producer = new EmsProducer(serverUrl, userName, password, queue, true);
        return producer.send(content);
    }

    public static void asyncSendString(String queue, String content) throws JMSException,
            InterruptedException {
        EmsProducer producer = new EmsProducer(serverUrl, userName, password, queue, false);
        producer.send(content);
    }

    public static Message sendBytes(String queue, byte[] bytes) throws JMSException,
            InterruptedException {
        EmsProducer producer = new EmsProducer(serverUrl, userName, password, queue, true);
        return producer.sendBytes(null, bytes, null, EmsProducer.TIMEOUT_DEADLINE);
    }

    public static void asyncSendBytes(String queue, byte[] bytes) throws JMSException,
            InterruptedException {
        EmsProducer producer = new EmsProducer(serverUrl, userName, password, queue, false);
        producer.sendBytes(null, bytes, null, EmsProducer.TIMEOUT_DEADLINE);
    }

    public static void sendFileBytes(String queue, String filePath) throws JMSException,
            InterruptedException, IOException {
        sendBytes(queue, FileUtils.readFileToByteArray(new File(filePath)));
    }

    public static void asyncSendFileBytes(String queue, String filePath) throws JMSException,
            InterruptedException, IOException {
        asyncSendBytes(queue, FileUtils.readFileToByteArray(new File(filePath)));
    }

}

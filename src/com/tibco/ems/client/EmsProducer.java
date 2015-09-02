/**
 * Created By: Comwave Project Team Created Date: 2015年4月20日
 */
package com.tibco.ems.client;

/**
 * @author Geln Yang
 * @version 1.0
 */
import java.net.InetAddress;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.Session;
import javax.jms.TemporaryQueue;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;

import com.tibco.tibjms.TibjmsQueueConnectionFactory;
import com.tibco.tibjms.TibjmsTopicConnectionFactory;

/**
 * 
 * @author gelnyang
 * 
 */
public class EmsProducer {

    public static final int TIMEOUT_DEADLINE = 300 * 1000; // 300 seconds

    private String serverUrl;

    private String userName;

    private String password;

    private String destinationName;

    private boolean transacted = false;

    private int ackMode = Session.AUTO_ACKNOWLEDGE;

    private boolean isTopic = false;

    private boolean sync = true;

    private Message responseMessage = null;

    private Connection connection;

    private Session session;

    private MessageProducer producer;

    private TemporaryQueue tempDest;


    private static String hostName = "";
    static {
        try {
            hostName = InetAddress.getLocalHost().getHostName();
            hostName = hostName.replaceAll("\\W", "");
        } catch (Exception e) {
        }
    }

    /**
     * @param serverUrl
     * @param userName
     * @param password
     * @param destinationName
     * @param sync
     */
    public EmsProducer(String serverUrl, String userName, String password, String destinationName,
            boolean sync) {
        super();
        this.serverUrl = serverUrl;
        this.userName = userName;
        this.password = password;
        this.destinationName = destinationName;
        this.sync = sync;
    }

    /**
     * @param serverUrl
     * @param userName
     * @param password
     * @param destinationName
     * @param transacted
     * @param ackMode
     * @param isTopic
     * @param sync
     */
    public EmsProducer(String serverUrl, String userName, String password, String destinationName,
            boolean transacted, int ackMode, boolean isTopic, boolean sync) {
        this(serverUrl, userName, password, destinationName, true);
        this.transacted = transacted;
        this.ackMode = ackMode;
        this.isTopic = isTopic;
        this.sync = sync;
    }

    public synchronized String send(String content) throws JMSException, InterruptedException {
        return send(null, content, TIMEOUT_DEADLINE);
    }

    public synchronized String send(String correlationId, String content, int timeout)
            throws JMSException, InterruptedException {
        initalSession();

        // Now create the actual message you want to send
        TextMessage txtMessage = session.createTextMessage();
        txtMessage.setText(content);

        sendMessage(correlationId, txtMessage, timeout);

        if (sync) {
            String responseText = null;
            if (responseMessage instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) responseMessage;
                responseText = textMessage.getText();
            } else {
                responseText = responseMessage.toString();
            }
            return responseText;
        } else {
            return null;
        }
    }

    public synchronized Message sendBytes(String correlationId, byte[] bytes,
            Map<String, Object> properties, int timeout) throws JMSException, InterruptedException {
        initalSession();
        BytesMessage bytesMessage = session.createBytesMessage();
        bytesMessage.writeBytes(bytes);

        if (properties != null) {
            Set<String> keySet = properties.keySet();
            for (String key : keySet) {
                Object value = properties.get(key);
                bytesMessage.setObjectProperty(key, value);
            }
        }

        return sendMessage(correlationId, bytesMessage, timeout);
    }

    private synchronized Message sendMessage(String correlationId, Message message, int timeout)
            throws JMSException, InterruptedException {
        try {
            message.setJMSReplyTo(tempDest);
            if (correlationId == null) {
                correlationId = createRandomString();
            }
            message.setJMSCorrelationID(correlationId);
            producer.send(message);

            if (sync) {
                long wait = 0;
                /* not need to wait response if timeout is negative */
                while (timeout > 0 && responseMessage == null) {
                    if (wait > timeout) {
                        connection.close();
                        throw new JMSException("request time out!");
                    }
                    Thread.sleep(10);
                    wait += 10;
                }
                return responseMessage;
            } else {
                return null;
            }
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
            }
        }
    }

    private void initalSession() throws JMSException {
        if (isTopic) {
            connection =
                    new TibjmsTopicConnectionFactory(serverUrl).createTopicConnection(userName,
                            password);
            session = ((TopicConnection) connection).createTopicSession(transacted, ackMode);
        } else {
            connection =
                    new TibjmsQueueConnectionFactory(serverUrl).createQueueConnection(userName,
                            password);
            session = ((QueueConnection) connection).createQueueSession(transacted, ackMode);
        }
        connection.start();
        Destination adminQueue = session.createQueue(destinationName);

        producer = session.createProducer(adminQueue);

        if (sync) {
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            tempDest = session.createTemporaryQueue();
            MessageConsumer responseConsumer = session.createConsumer(tempDest);
            MessageListener messageListner = new MessageListener() {

                public void onMessage(Message message) {
                    responseMessage = message;
                }
            };
            responseConsumer.setMessageListener(messageListner);
        } else {
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        }
    }

    private synchronized String createRandomString() {
        long currentTimeMillis = System.currentTimeMillis();
        Random random = new Random(currentTimeMillis);
        long randomLong = random.nextLong();
        return hostName + Thread.currentThread().getId() + Long.toHexString(currentTimeMillis)
                + Long.toHexString(randomLong);
    }

}

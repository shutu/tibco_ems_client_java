/**
 * Created By: Comwave Project Team Created Date: 2015年9月2日
 */
package com.tibco.ems.client.cmd;

import java.io.IOException;

import javax.jms.JMSException;

import com.tibco.ems.client.EmsSender;

/**
 * @author Geln Yang
 * @version 1.0
 */
public class SendFile {

    public static void main(String[] args) throws JMSException, InterruptedException, IOException {
        EmsSender.sendFileBytes(args[0], args[1]);
    }
}

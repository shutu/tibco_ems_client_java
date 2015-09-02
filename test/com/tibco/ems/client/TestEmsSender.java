package com.tibco.ems.client;


public class TestEmsSender {

    public static void main(String[] args) throws Exception {
        EmsSender.asyncSendFileBytes("test.file.content", "C:/a.txt");
    }
}

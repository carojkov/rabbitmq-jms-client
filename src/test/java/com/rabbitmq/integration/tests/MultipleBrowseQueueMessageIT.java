/* Copyright (c) 2014-2020 VMware, Inc. or its affiliates. All rights reserved. */
package com.rabbitmq.integration.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.Enumeration;

import javax.jms.DeliveryMode;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.junit.jupiter.api.Test;

/**
 * Integration test for browsing a queue multiple times.
 */
public class MultipleBrowseQueueMessageIT extends AbstractITQueue {

    private static final String QUEUE_NAME = "test.queue." + MultipleBrowseQueueMessageIT.class.getCanonicalName();
    private static final long TEST_RECEIVE_TIMEOUT = 1000; // one second

    private static final int REPEATS = 3;

    private void messageTestBase(MessageTestType mtt) throws Exception {
        messageTestBase(mtt, null, 1);
    }

    private void messageTestBase(MessageTestType mtt, String selector, int numExpected) throws Exception {
        try {
            queueConn.start();
            QueueSession queueSession = queueConn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = queueSession.createQueue(QUEUE_NAME);

            drainQueue(queueSession, queue);

            QueueSender queueSender = queueSession.createSender(queue);
            queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            queueSender.send(mtt.gen(queueSession, (Serializable)queue));
        } finally {
            reconnect();
        }

        queueConn.start();
        QueueSession queueSession = queueConn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Queue queue = queueSession.createQueue(QUEUE_NAME);

        {// Browse queue before receiving message
            QueueBrowser queueBrowser = queueSession.createBrowser(queue, selector);

            for (int repeat=0; repeat<REPEATS; ++repeat) {
                Enumeration<?> e = queueBrowser.getEnumeration();
                int numE = 0;
                Message msg = null;
                while (e.hasMoreElements()) {
                    ++numE;
                    msg = (Message) e.nextElement();
                    mtt.check(msg, (Serializable) queue);
                }
                assertEquals(numExpected, numE, "Wrong number of messages on browse queue");
            }
        }
        QueueReceiver queueReceiver = queueSession.createReceiver(queue);

        mtt.check(queueReceiver.receive(TEST_RECEIVE_TIMEOUT), (Serializable) queue);
    }

    @Test
    public void testSendBrowseAndReceiveLongTextMessage() throws Exception {
        messageTestBase(MessageTestType.LONG_TEXT);
    }

    @Test
    public void testSendBrowseAndReceiveTextMessage() throws Exception {
        messageTestBase(MessageTestType.TEXT);
    }

    @Test
    public void testSendBrowseAndReceiveBytesMessage() throws Exception {
        messageTestBase(MessageTestType.BYTES);
    }

    @Test
    public void testSendBrowseAndReceiveMapMessage() throws Exception {
        messageTestBase(MessageTestType.MAP);
    }

    @Test
    public void testSendBrowseAndReceiveStreamMessage() throws Exception {
        messageTestBase(MessageTestType.STREAM);
    }

    @Test
    public void testSendBrowseAndReceiveObjectMessage() throws Exception {
        messageTestBase(MessageTestType.OBJECT);
    }

    @Test
    public void testBrowseWithSelectorFalse() throws Exception {
        messageTestBase(MessageTestType.TEXT, "false", 0);
    }

    @Test
    public void testBrowseWithSelectorTrue() throws Exception {
        messageTestBase(MessageTestType.TEXT, "true", 1);
    }
}

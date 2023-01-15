package com.logho;

import lombok.Data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Channel;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author logho
 * @date 2021/9/27 22:00
 */
@Data
public class SelectorThreadGroup {

    private SelectorThread[] selectorThreads;
    private ServerSocketChannel server;
    private SelectorThreadGroup workerThreadGroup;
    private AtomicInteger reqCount = new AtomicInteger(0);

    public SelectorThreadGroup (int nums) {
        selectorThreads = new SelectorThread[nums];
        for (int i = 0; i < nums; i++) {
            selectorThreads[i] = new SelectorThread(this);
            new Thread(selectorThreads[i]).start();
        }
    }

    public void bind(int port) {
        try {
            server = ServerSocketChannel.open();
            server.configureBlocking(false);
            server.bind(new InetSocketAddress(port));

            nextSelectorV2(server);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public SelectorThread next() {
        int index = reqCount.incrementAndGet() % selectorThreads.length;
        return selectorThreads[index];
    }


    //将新生成的channel放到轮训得到的线程的selector中，并打断该线程的阻塞状态
    public void nextSelector(Channel channel) {

        SelectorThread nextSelectThread = next();

        nextSelectThread.getTaskQueue().add(channel);

        nextSelectThread.getSelector().wakeup();

    }


    public SelectorThread nextV2() {
        int index = reqCount.incrementAndGet() % (selectorThreads.length - 1);
        return selectorThreads[index + 1];
    }


    //将新生成的channel放到轮训得到的线程的selector中，并打断该线程的阻塞状态
    public void nextSelectorV2(Channel channel) {
        if (channel instanceof ServerSocketChannel) {
            selectorThreads[0].getTaskQueue().add(channel);
            selectorThreads[0].getSelector().wakeup();
            return;
        }

        SelectorThread nextSelectThread = nextV2();

        nextSelectThread.getTaskQueue().add(channel);

        nextSelectThread.getSelector().wakeup();

    }


}

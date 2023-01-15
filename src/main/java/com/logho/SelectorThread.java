package com.logho;

import lombok.Data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author logho
 * @date 2021/9/27 22:00
 */
@Data
public class SelectorThread implements Runnable{

    private Selector selector;
    private LinkedBlockingQueue<Channel> taskQueue;
    private SelectorThreadGroup selectorThreadGroup;


    public SelectorThread (SelectorThreadGroup selectorThreadGroup) {
        try {
            this.selector = Selector.open();
            taskQueue = new LinkedBlockingQueue<>();
            this.selectorThreadGroup = selectorThreadGroup;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void run() {
        while (true) {
            System.out.println(Thread.currentThread().getName() + ": start");
            try {
                System.out.println(Thread.currentThread().getName() + ": before select");
                int nums = selector.select();
                System.out.println(Thread.currentThread().getName() + ": after select the nums is : "+ nums);
                if (nums > 0) {
                    Set<SelectionKey> selectionKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectionKeys.iterator();
                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();
                        if (key.isAcceptable()) {
                            acceptHandler(key);
                        } else if (key.isReadable()) {
                            readHandler(key);
                        } else if (key.isWritable()) {
                            writeHandler(key);
                        }
                    }
                }


                if (!taskQueue.isEmpty()) {
                    Channel channel = taskQueue.take();
                    if (channel instanceof ServerSocketChannel) {
                        ServerSocketChannel server = (ServerSocketChannel) channel;
                        server.register(selector, SelectionKey.OP_ACCEPT);
                        System.out.println(Thread.currentThread().getName()+": server 8080 listening....");
                    } else if (channel instanceof SocketChannel) {
                        Thread.sleep(10000);

                        SocketChannel client = (SocketChannel) channel;
                        ByteBuffer buffer = ByteBuffer.allocate(4096);
                        client.register(selector,SelectionKey.OP_READ,buffer);
                        System.out.println(Thread.currentThread().getName()+": client connection has establish...."+client.getRemoteAddress());
                    }
                }
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }




    private void acceptHandler(SelectionKey key) throws InterruptedException {
        System.out.println(Thread.currentThread().getName() + ": acceptHandler........");

        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        try {
            SocketChannel client = server.accept();
            client.configureBlocking(false);
            selectorThreadGroup.nextSelectorV2(client);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readHandler(SelectionKey key) throws InterruptedException {

        System.out.println(Thread.currentThread().getName() + ": readHandler");
        ByteBuffer buffer = (ByteBuffer) key.attachment();
        SocketChannel client = (SocketChannel) key.channel();
        buffer.clear();
        while (true) {
            try {
                int nums = client.read(buffer);
                if (nums > 0) {
                    buffer.flip();
                    while (buffer.hasRemaining()) {
                        client.write(buffer);


                    }
                    buffer.clear();
                } else if (nums == 0) {
                    break;
                } else {
                    System.out.println(Thread.currentThread().getName()+": 断开连接了......");
                    key.cancel();
                    break;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    private void writeHandler(SelectionKey key) {
        System.out.println(Thread.currentThread().getName() + ": after writeHandler");
    }
}


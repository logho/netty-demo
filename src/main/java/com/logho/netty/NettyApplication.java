package com.logho.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.CharsetUtil;
import lombok.AllArgsConstructor;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * @author logho
 * @date 2021/10/5 16:20
 */
public class NettyApplication {

    @Test
    public void clientMode() throws InterruptedException {
        NioEventLoopGroup selectorExecutors = new NioEventLoopGroup(1);
        NioSocketChannel client = new NioSocketChannel();
        selectorExecutors.register(client);

        ChannelPipeline pipeline = client.pipeline();
        pipeline.addLast(new MyInDataHandler());
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.buffer(8, 20);

        ChannelFuture channelFuture = client.connect(new InetSocketAddress("localhost",9090));

        channelFuture.sync();

        byteBuf.writeBytes("hello server ! ".getBytes(StandardCharsets.UTF_8));
//        ByteBuf buf = Unpooled.copiedBuffer("hello server".getBytes());

        ChannelFuture channelFuture1 = client.writeAndFlush(byteBuf);
//        channelFuture1.sync();

        channelFuture.channel().closeFuture().sync();
        System.out.println("server closed....");

    }

    @Test
    public void serverMode() throws InterruptedException {
        NioEventLoopGroup selectorExecutors = new NioEventLoopGroup(1);
        NioServerSocketChannel server = new NioServerSocketChannel();
        selectorExecutors.register(server);

        ChannelPipeline serverPipeline = server.pipeline();
        serverPipeline.addLast(new MyAcceptHandler(selectorExecutors,new MyInDataHandler()));

        ChannelFuture bind = server.bind(new InetSocketAddress(9090));
        bind.sync().channel().closeFuture().sync();


    }

}

class MyInDataHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client be registered...");
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("client be active...");

    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf byteBuf = (ByteBuf) msg;
        CharSequence charSequence = byteBuf.getCharSequence(0, byteBuf.readableBytes(), CharsetUtil.UTF_8);
        System.out.println(charSequence);
//        ctx.writeAndFlush("Reply From Others:" + charSequence);
        ctx.writeAndFlush(byteBuf);
    }
}



@AllArgsConstructor
class MyAcceptHandler extends ChannelInboundHandlerAdapter {
    private NioEventLoopGroup selector;
    private ChannelHandler handler;


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        SocketChannel client = (SocketChannel) msg;
        selector.register(client);

        ChannelPipeline clientPipeline = client.pipeline();
        clientPipeline.addLast(handler);

    }
}

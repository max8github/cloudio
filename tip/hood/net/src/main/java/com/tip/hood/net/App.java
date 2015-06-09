package com.tip.hood.net;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;

/**
 * Hello world!
 *
 */
public class App {

    public static void main(String[] args) {

        Channel channel = null; // Does not block ChannelFuture 
        ChannelFuture future = channel.connect(new InetSocketAddress("192.168.0.1", 25));
        future.addListener(new ChannelFutureListener() { //2 

            @Override
            public void operationComplete(ChannelFuture future) {
                if (future.isSuccess()) { //3 
                    ByteBuf buffer = Unpooled.copiedBuffer("Hello", Charset.defaultCharset()); //4 
                    ChannelFuture wf = future.channel().write(buffer);
                    //5 .... 
                } else {
                    Throwable cause = future.cause();
                    cause.printStackTrace();
                }
            }
        });
    }
}
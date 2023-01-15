package com.logho;

/**
 * @author logho
 * @date 2021/9/27 22:00
 */
public class NettyDemoApplication {

    public static void main(String[] args) {

        SelectorThreadGroup selectorThreadGroup = new SelectorThreadGroup(3);



        selectorThreadGroup.bind(8088);


    }
}

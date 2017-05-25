package com.dongnao.alan.redis;

import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

public class RedisSubscribe1 {
	@Test 
    public void subscribe_test() { 
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "192.168.1.129", 6380, 
                5000);

        Jedis jedis = pool.getResource(); 
        JedisPubSub jedisPubSub = new JedisPubSub() { 
            @Override 
            public void onUnsubscribe(String channel, int number) { 
                System.out.println("channel: "+channel); 
                System.out.println("number :"+number); 
            } 
            @Override 
            public void onSubscribe(String channel, int number) { 
                System.out.println("channel: "+channel); 
                System.out.println("number :"+number); 
            } 
            @Override 
            public void onPUnsubscribe(String arg0, int arg1) { 
            } 
            @Override 
            public void onPSubscribe(String arg0, int arg1) { 
            } 
            @Override 
            public void onPMessage(String arg0, String arg1, String arg2) { 
            } 
            @Override 
            public void onMessage(String channel, String msg) { 
                System.out.println("收到频道 : 【" + channel +" 】的消息 ：" + msg); 
            } 
        }; 
        jedis.subscribe(jedisPubSub, new String[]{"dongnao1","dongnao2"}); 
        pool.returnResource(jedis); 
    }

	//发布 
    @Test 
    public void publish_test() { 
        JedisPool pool = new JedisPool(new JedisPoolConfig(), "192.168.1.129", 6380, 
                5000);
        Jedis jedis = pool.getResource(); 
        long i = jedis.publish("dongnao1", "动脑学院的朋友们，你们好吗？"); 
        System.out.println(i+" 个订阅者接受到了 dongnao1 消息"); 
        i = jedis.publish("dongnao2", "你好呀"); 
        System.out.println(i+" 个订阅者接受到了 dongnao2 消息"); 
        pool.returnResource(jedis); 
    }

}

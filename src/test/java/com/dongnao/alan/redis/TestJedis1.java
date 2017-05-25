package com.dongnao.alan.redis;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPipeline;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.Transaction;

public class TestJedis1 {

    private static Jedis jedis;
    private static ShardedJedis sharding;
    private static ShardedJedisPool pool;
	
	@BeforeClass
	public static void setUpBeforeClass() {
		List<JedisShardInfo> shards = Arrays.asList(
                new JedisShardInfo("192.168.1.129",6380),
                new JedisShardInfo("192.168.1.129",6380)); //使用相同的ip:port,仅作测试

        jedis = new Jedis("192.168.1.129",6380);//单机
        
        sharding = new ShardedJedis(shards);//分布式

        pool = new ShardedJedisPool(new JedisPoolConfig(), shards);//连接池
	}

	@AfterClass
	public static void tearDownAfterClass() {
		jedis.disconnect();
        sharding.disconnect();
        pool.destroy();
	}
	
	@Test
	public void normal() {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			jedis.set("n" + i, "n" + i);
		}
		long end = System.currentTimeMillis();
		System.out.println("普通操作耗时----" + (end - start) / 1000 + "秒------");
	}

	@Test
	public void trans() {
		long start = System.currentTimeMillis();
		Transaction tx = jedis.multi();
		for (int i = 0; i < 100000; i++) {
			tx.set("t" + i, "t" + i);
		}
		List<Object> results = tx.exec();
		long end = System.currentTimeMillis();
		System.out.println("事务耗时----" + (end - start) / 1000.0 + "秒------");
	}

	@Test
	public void pipelined() {
		Pipeline pipeline = jedis.pipelined();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			pipeline.set("p" + i, "p" + i);
		}
		List<Object> results = pipeline.syncAndReturnAll();
		long end = System.currentTimeMillis();
		System.out.println("管道耗时----" + (end - start) / 1000.0 + "秒------");
	}
	
	@Test
	public void pipelineTrans() {
		long start = System.currentTimeMillis();
		Pipeline pipeline = jedis.pipelined();
		pipeline.multi();
		for (int i = 0; i < 100000; i++) {
			pipeline.set("" + i, "" + i);
		}
		pipeline.exec();
		List<Object> results = pipeline.syncAndReturnAll();
		long end = System.currentTimeMillis();
		System.out.println("管道耗时----" + ((end - start) / 1000.0) + "秒------");

	}
	
	@Test
	public void shardNormal() {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			String result = sharding.set("sn" + i, "n" + i);
		}
		long end = System.currentTimeMillis();
		System.out.println("分布式直连同步调用耗时----" + ((end - start) / 1000.0) + "秒------");
	}
	
	@Test
	public void shardpipelined() {
		ShardedJedisPipeline pipeline = sharding.pipelined();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			pipeline.set("sp" + i, "p" + i);
		}
		List<Object> results = pipeline.syncAndReturnAll();
		long end = System.currentTimeMillis();
		System.out.println("分布式直连异步调用耗时 ----" + ((end - start) / 1000.0) + "秒------");
	}
	
	@Test
	public void shardSimplePool() {
		ShardedJedis one = pool.getResource();

		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			String result = one.set("spn" + i, "n" + i);
		}
		long end = System.currentTimeMillis();
		pool.returnResource(one);
		System.out.println("分布式连接池同步调用----" + ((end - start) / 1000.0) + "秒------");
	}
	
	@Test
    public void shardPipelinedPool() {
        ShardedJedis one = pool.getResource();

        ShardedJedisPipeline pipeline = one.pipelined();

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            pipeline.set("sppn" + i, "n" + i);
        }
        List<Object> results = pipeline.syncAndReturnAll();
        long end = System.currentTimeMillis();
        pool.returnResource(one);
        System.out.println("分布式连接池异步调用----" + ((end - start)/1000.0) + "秒------");
    }
	
	@Test
	public void test () {
		for (int i = 0; i< 30; i++){
			boolean result = testLogin("192.168.1.45");
			if(result){
				System.out.println("正常");
			} else {
				System.err.println("受限");
			}
		}
	}
	
	public boolean testLogin(String ip) {
		String value = jedis.get(ip);
		if(value == null){
			jedis.set(ip, "1");
			jedis.expire(ip, 60*60);
		} else {
			int parseInt = Integer.parseInt(value);
			if(parseInt > 10){
				return false;
			}else{
				jedis.incr(ip);
			}
		}
		return true;
	}
	
}

package com.dongnao.alan.redis;

import java.lang.reflect.Array;
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

public class TestJedis {

	private static Jedis jedis;
	private static ShardedJedis sharding;//分布式
	private static ShardedJedisPool pool;//链接池

	@BeforeClass
	public static void beforeClass() {
		List<JedisShardInfo> shards = Arrays.asList(
				new JedisShardInfo("127.0.0.1",6379,"123456"),
				new JedisShardInfo("127.0.0.1",6379,"123456"));//仅做演示
		
		
		jedis = new Jedis("127.0.0.1", 6379);
		jedis.auth("123456");
		sharding = new ShardedJedis(shards);
		
		pool = new ShardedJedisPool(new JedisPoolConfig(), shards);//连接池
	}

	@AfterClass
	public static void afterClass() {
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
		System.out.println("普通操作时间-----：" + (end - start) / 1000 + "秒");
	}

	@Test
	public void trans() {
		long start = System.currentTimeMillis();
		Transaction tx = jedis.multi();
		/*// 实现redis乐观锁
		String key = "key";
		jedis.watch(key);// 监控，只生效一次
		Transaction tx = jedis.multi();
		jedis.incr(key);// 原子性操作
		List<Object> list = tx.exec();
		if (list != null) {// 提交事务成功，说明没人更改过
			
		}*/

		for (int i = 0; i < 100000; i++) {
			tx.set("t" + i, "t" + i);
		}
		tx.exec();

		long end = System.currentTimeMillis();
		System.out.println("事务操作时间-----：" + (end - start) / 1000.0 + "秒");

	}
	@Test
	public void pipelined () {
		long start = System.currentTimeMillis();
		Pipeline pipeline = jedis.pipelined();
		for (int i = 0; i < 100000; i++) {
			pipeline.set("p" + i, "p" + i);
		}
		
		List<Object> results = pipeline.syncAndReturnAll();
		System.out.println(results.size());
		long end = System.currentTimeMillis();
		System.out.println("事务操作时间-----：" + (end - start) / 1000.0 + "秒");
	}
	
	@Test
	public void shardNormal(){
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			sharding.set("sn" + i, "sn" + i);
		}
		long end = System.currentTimeMillis();
		System.out.println("分布式同步调用操作时间-----：" + (end - start) / 1000.0 + "秒");
	}
	
	@Test
	public void shardPipelined(){
		long start = System.currentTimeMillis();
		ShardedJedisPipeline pipeline = sharding.pipelined();
		for (int i = 0; i < 100000; i++) {
			pipeline.set("sp" + i, "sp" + i);
		}
		List<Object> results = pipeline.syncAndReturnAll();
		long end = System.currentTimeMillis();
		System.out.println("分布式异步调用操作时间使用管道-----：" + (end - start) / 1000.0 + "秒");
	}
	
	public void shardPipelinedPool () {
		ShardedJedis one = pool.getResource();
		long start = System.currentTimeMillis();
		ShardedJedisPipeline pipeline = one.pipelined();
		for (int i = 0; i < 100000; i++) {
			pipeline.set("sp" + i, "sp" + i);
		}
		List<Object> results = pipeline.syncAndReturnAll();
		
//		try {
//			pool.returnResource(one);
//		} catch (Exception e) {
//			// TODO: handle exception
//		}
		one.close();
		long end = System.currentTimeMillis();
		System.out.println("分布式异步调用操作时间-----：" + (end - start) / 1000.0 + "秒");
		
	}
	
}

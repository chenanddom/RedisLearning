# 数据特性 #
## 使用位图(bitmap) ##

```text
位图(也称为位数组或者位向量)是由比特位(bit)组成的数组。Redis中的位图并不是一种新的数据类型，它实际的底层数据类型是字符串。
因为字符串本质上是是二级制大对象(BLOB,Binary Large Object)，所有可以将其视为畏途。同时，因为位图存储的是布尔信息，所以在
某些特殊的情况下可以节约大量的内存空间。
```

位图的基本使用
```text
1. SETBIT 添加位图元素
语法:SETBIT KEY name 1/0
例子: SETBIT "user_tried_reservation" 100 1
2. GETBIT 从位图获取位于指定偏移处比特位的值
语法:GETBIT KEY name
例子： GETBIT "user_tried_reservation" 100
3.BITCOUNT 获取位图内被设置位1的比特数
语法：BITCOUNT KEY
例子: BITCOUNT "user_tried_reservation"
4.BITOP 位操作，支持四种位操作AND,OR,XOR和NOT 
语法：BITOP OPERATOR AIRMKEY map1 map2 ... 
例子:1. BITOP AND "user_tried_both_reservation_and_online_orders" "user_tried_reservation" "user_tried_reservation_online_order"
     2. BITCOUNT user_tried_both_reservation_and_online_orders  


全部：
192.168.253.1:6379> SETBIT "user_tried_reservation" 100 1
(integer) 0
192.168.253.1:6379> GETBIT "user_tried_reservation" 100
(integer) 1
192.168.253.1:6379>  SETBIT "user_tried_reservation" 101 1
(integer) 0
192.168.253.1:6379>  GETBIT "user_tried_reservation" 101
(integer) 1
192.168.253.1:6379> BITCOUNT "user_tried_reservation"
(integer) 2
192.168.253.1:6379>  SETBIT "user_tried_reservation_online_order" 100 1
(integer) 0
192.168.253.1:6379> getbit "user_tried_reservation_online_order" 100
(integer) 1
192.168.253.1:6379> bitop and "user_tried_both_reservation_and_online_orders" "user_tried_reservation" "user_tried_reservation_online_order"
(integer) 13
192.168.253.1:6379> getbit "user_tried_both_reservation_and_online_orders"
(error) ERR wrong number of arguments for 'getbit' command
192.168.253.1:6379> bitcount "user_tried_both_reservation_and_online_orders"
(integer) 1
```


## 设置过期时间 ##

```text
使用EXPIRE可以将键的超时时间设置位指定的时间
例子：
192.168.253.1:6379> set k1 v1
OK
192.168.253.1:6379> expire k1 20
(integer) 1
192.168.253.1:6379> ttl k1
(integer) 17
192.168.253.1:6379> ttl k1
(integer) 9
192.168.253.1:6379> ttl k1
(integer) 4
192.168.253.1:6379> ttl k1
(integer) -2
192.168.253.1:6379> rrl k1
(error) ERR unknown command `rrl`, with args beginning with: `k1`,
192.168.253.1:6379> ttl k1
(integer) -2
192.168.253.1:6379>
```









## 使用SORT命令 ##
```text
SORT指令可以对非sort set集合进行排序，对数值可以直接排序，对于非数值的数据需要使用alpha关键字，还可以使用limit实现固定数量的数值返回

例子：
192.168.253.1:6379> sadd collections "a" "b" "c"
(integer) 3
192.168.253.1:6379> sadd collections "f" "e"
(integer) 2
192.168.253.1:6379> smembers collections
1) "f"
2) "c"
3) "a"
4) "b"
5) "e"
192.168.253.1:6379> sort collections
(error) ERR One or more scores can't be converted into double
192.168.253.1:6379> SORT collections
(error) ERR One or more scores can't be converted into double
192.168.253.1:6379> sort collections
(error) ERR One or more scores can't be converted into double
192.168.253.1:6379> SORT collections
(error) ERR One or more scores can't be converted into double
192.168.253.1:6379> SADD collections2 100 89 101 1000 1 500
(integer) 6
192.168.253.1:6379> sort collections2
1) "1"
2) "89"
3) "100"
4) "101"
5) "500"
6) "1000"
192.168.253.1:6379> sort collections alpha
1) "a"
2) "b"
3) "c"
4) "e"
5) "f"
192.168.253.1:6379> sort collections alpha limit 0 3
1) "a"
2) "b"
3) "c"
192.168.253.1:6379>  sort collections alpha limit 2 3
1) "c"
2) "e"
3) "f"
192.168.253.1:6379>  sort collections alpha limit 3 3
1) "e"
2) "f"
192.168.253.1:6379>  sort collections alpha limit 0 3 desc
1) "f"
2) "e"
3) "c"
```

## 使用管道(pipline) ##















## 理解Redis事务 ##
```text
Redis的客户端和服务端的是通过RESP协议进行通信的。客户端和服务器之间的典型的通信过程可以看作：
1. 客户端向服务器发送一个命令
2. 服务器接收改命令并将其放入执行队列(因为Redis是单线程的执行模型)
3. 命令被执行
4. 服务器将命令执行的结果返回给客户端
上述过程耗费所有时间称为往返时延(RTT,roud trip time)。第二步和第三步的时间取决于Redis服务器，
第1步和第4步完全去解决客户端和服务器啊之间的网络延迟。如果这样执行大量的命令将会耗费大量的时间，网络
传输肯恩和会花费大量的时间.
```
例子：

```text
1. 安装dos2unix
sudo apt-get install dos2unix
brew install dos2unix
2. 创建一个pipeline.txt文件
文件内容如下：
set k001 v001
sadd set001 value001 value002
get k001
scard set001
3. 这个文件中的每一行都必须是\r\n，而不是以\n结束，我们可以使用unix2doc实现
# unix2dos pipeline.txt
4. 使用redis-cli的--pipe选项,通过管道发送命令
# cat redisRepository/pipeline.txt | redis-5.0.8/src/redis-cli -h 192.168.253.1  --pipe
All data transferred. Waiting for the last reply...
Last reply received from server.
errors: 0, replies: 4
5.在 redis-cli中可查看添加的值

```

## Redis 事务 ##

```text
Redis的事务和关系型数据库的事务还是有 一定的区别的，关系型数据的事务时一组原子化的操作，要么
都成功要么都失败。Redis的事务完全是另外一回事。我们一般通过MULTI命令开启一个事务，然后获取计数器的值
如果值无效旧使用命令DISCARD命令直接放弃该事务。
```
关系型数据库事务Redis事务之间的差别：
```text
他们之前的最大区别在于，Redis事务时没有回滚的功能的，一般来水，在一个Redis事务中可能会出现两种雷系那个的错误
针对这两种类型的错误采取不同的处理方式：

1. 命令的语法错误。这种情况下，由于命令存在语法错误，所以整个事务会快速失败，而且有命令都不会执行。
例子：
192.168.0.175:6383> multi
OK
192.168.0.175:6383> set foo bar
QUEUED
192.168.0.175:6383> got foo
(error) ERR unknown command `got`, with args beginning with: `foo`,
192.168.0.175:6383> incr mas
QUEUED
192.168.0.175:6383> exec
(error) EXECABORT Transaction discarded because of previous errors.

2. 第二章错误是所有的命令都成功的入队了，但是执行的过程中发生了错误，位于发生错误的命令之后的其他命令将继续执行，而不会回滚。

例子:
192.168.0.175:6383> multi
OK
192.168.0.175:6383> set foo bar
QUEUED
192.168.0.175:6383> incr foo
QUEUED
192.168.0.175:6383> set foo mas
QUEUED
192.168.0.175:6383> get foo
QUEUED
192.168.0.175:6383> exec
1) OK
2) (error) ERR value is not an integer or out of range
3) OK
4) "mas"
192.168.0.175:6383> get foo
"mas"




```

## 使用发布订阅(PubSub) ##

```text
发布-订阅是一种历史悠久的经典消息传递方式。发布订阅模式中想要把发布时间的发布者会把消息发送到一个频道，这个频道会把时间投递给这个频道
感兴趣的没一个订阅者。
语法：
1. 订阅：SUBSCRIBE "队列名称"
2. 发布：PUBLISH "队列名称"
SUBSCRIBE命令来监听特定频道的可用的消息。一个客户端可以使用SUBSCRIBE命令一次的订阅多个频道，也可以使用PSUBSCRIBE命令订阅匹配指定
模式的频道。要取消订阅可以使用UNSUBSCRIBR命令.PUBLISH命令用于将一条消息发送到指定的频道。
3. PUBSUB CHANNELS用于获取当前活跃的频道。
```

更多细节：

```text
1. 对于频道而言，如果频道之前未曾被订阅，那么SUBSCRIBE命令会被自动的创建频道，此外频道上没有活跃的订阅者时，频道会被删除.
2. PUBSUB相关的额机制不支持持久化。意味着如果服务器出故障很可能丢失消息。
3. 在消息没有投递和处理的场景的时候，如果频道没有订阅者，那么发布到频道上的消息将会被丢弃。
4.


```




## 使用Lua脚本 ##


## 调式Lua脚本 ##








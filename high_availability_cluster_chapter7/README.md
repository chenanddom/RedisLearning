# 配置高可用和集群

```text
    对于生产环境中的需求，Redis单实例是远远不能提供稳定高效的，具备数据冗余的和高可用的(HA,hign availablity)
能力的的键值存储服务。使用Redis的主从复制和持久化可以解决数据冗余备份的问题。但是，如果没有人工的干预，当
主实例宕机时，整个Redis服务将无法恢复。但是，如果没有人工的干预，当主实例宕机时，整个Redis服务实例将无法恢复。
尽管市面上的很多Redis高可用的解决方案，但是2.6版本以后Redis原生支持的Sentinel是使用最广泛的高可用框架。利用sentinel
,我们可以轻松的具备容错能力的Redis服务。
    由于Redis中锁存储的数据增长速度很快，一个存储大量数据(通常是16GB以上)的Redis实例的处理能力和内存容量可能会变成应用的
瓶颈。随着Redis中数据集大小的增长，在进行持久化或者主从复制时，也会越来越多的出现延迟的问题。对于这种情况，水平拓展，或者想Redis
服务中增加更多节点来实现伸缩就很有必要，从Redis的3.0版本开始支持了Redis Cluster正式针对这类问题提出了。RedisCluster可以通过将数据分布到
多个Rdis主从实例。

```


## 配置Sentinel ##

```text
  sentinel(哨兵)充当了Redis主实例和从实例的守卫者。因为单个哨兵也可能失效，所以单个哨兵实例是无法保证高可用的。
对主实例进行故障迁移的决策时基于仲裁系统的，所以至少需要三个哨兵进程才能构建一个简装的分布式系统的来监控redis主实例
的状态，如果多个哨兵的进程检测到主实例下线，其中的一个哨兵进程会被选举出来复制则推选一个从实例替换原来的主实例。如果配置得当，那么 这个过程是自动化的，无需人工干预。

    由于只有一台机器，为了能模拟处多台机器，此处选择docker做机器的切分，ip分别为x.x.0.4，x.x.0.5，x.x.0.6。其中redis服务的
配置一定要按照主从配置的方式配置号，从机器一定要配好slaveof <ip> ，这样形成一个主从的集群，然后在哨兵的配置文件上一定要配置好
sentinel monitor mymaster x.x.x.x 6380 2配置监视器，以及机器故障迁移前发现并同意主实例不可达最少哨兵数。先后启动redis,sentinel，
启动的命令分别为:
1. src/redis-server redis.conf
2. src/redis-server sentinel.conf --sentinel
```
[redis文件的配置](images/redis.conf)

[sentinel文件的配置](image/sentinel.conf)


sentinel的工作原理

```text

1. 因为Sentinel时Redis的守护进程，因此它必须监听在与Redis实例不太的端口上。Sentinel的默认端口号时26379.如果要将一个新的主实例添加
到Sentinel中进行监控，那么我们可以按照如下的格式今昔文件配置，只需要添加一行:sentinel monitor <master-name> <ip> <port> <quorum>
<quorum>表示了采取故障迁移操作前，发现并同意主实例不可达是最少哨兵数。down-fater-milliseconds选项标记实例下线前不可达的毫秒数。
默认是30s parallel-syncs参数表示有几个从实例可以同时从新的主实例进行数据同步.

2.启动方式可以选择使用 redis-server sentinel.conf --sentinel ,如果使用源码编译安装的话，可以使用redis-sentinel sentinel.conf

```



## 测试Sentinel ##

```text
    1. 模擬主实例下线:原来的master的ip为x.x.x.4，手动停掉x.x.x.4，在redis-cli客户端使用Info replication查看可以发现master节点已经转移到了x.x.x.6节点.
而且之前在x.x.x.4存入的数据都留在在x.x.x.6或者x.x.x.5都类查看到。而且我们可以发现sentinel的配置文件也发生了改变，这就是为什么对sentinel
配置文件必须有写入权限的原因。
    2. 模拟两个从实例下线:如果主实例设置了CONFIG SET MIN-SLAVES-TO-WRITE 1 那么必须需要有一个从实例才可以写入，否则会出现(error) NOREPLICAS Not enough good slaves to write
    3.哨兵下线：哨兵下线不会引起主实例的转移。
```


手动触发故障迁移的原理
```text
手动触发哨兵进行主实例的故障迁移并重新选出了一个从实例。这是通过sentinel failover <master-name>命令完成的。当我们执行命令之后，原来的
主实例x.x.x.6变成了x.x.x.5
步骤：
1. 由于是手动触发的，索引不需要其他哨兵同意就可以选择选举出leader.
2. 接下来，Sentinel挑选一个从实例将其提升为主实例
3. Sentinel会向选中的实例发送slaveof no one命令使之变成为主实例，并且停止从老的主实例复制。
4. Sentinel会重新配置老实例和从实例，让他们重新从新的主实例复制。
5.最后一步，sentinel更新主实例的信息，并且通过频道_sentinel_:hello向其他的哨兵广播这些信息，从而让所有的客户端获取得到新的主实例的信息.

```



## 管理Sentinel ##

```text

1. 学习哨兵命令
 redis-cli h x.x.x.4 -p 26379

2. 使用SENTINEL GET-MASTER_ADDR-BY-NAME <master-name>获取当前主实例的信息
例子：
x.x.0.6:26379> sentinel get-master-addr-by-name mymaster
1) "x.x.0.6"
2) "6380"

3. 使用SENTINEL MASTERS命令获取所有被监控主实例的状态

x.x.0.6:26379> sentinel masters
1)  1) "name"
    2) "mymaster"
    3) "ip"
    4) "x.x.0.6"
    5) "port"
    6) "6380"
    7) "runid"
    8) "20a19ab5159123739f7edf258b6033815c2a3c63"
    9) "flags"
   10) "master"
   11) "link-pending-commands"
   12) "0"
   13) "link-refcount"
   14) "1"
   15) "last-ping-sent"
   16) "0"
   17) "last-ok-ping-reply"
   18) "1019"
   19) "last-ping-reply"
   20) "1019"
   21) "down-after-milliseconds"
   22) "30000"
   23) "info-refresh"
   24) "9572"
   25) "role-reported"
   26) "master"
   27) "role-reported-time"
   28) "2757025"
   29) "config-epoch"
   30) "5"
   31) "num-slaves"
   32) "2"
   33) "num-other-sentinels"
   34) "2"
   35) "quorum"
   36) "2"
   37) "failover-timeout"
   38) "180000"
   39) "parallel-syncs"
   40) "1"


4. 使用SENTINEL SLAVES <master-name>命令获取一个呗监控主实例的所有从实例的信息
x.x.0.6:26379> sentinel slaves mymaster
1)  1) "name"
    2) "x.x.0.4:6380"
    3) "ip"
    4) "x.x.0.4"
    5) "port"
    6) "6380"
    7) "runid"
    8) "b8f5934aeb420c541673e1fdc2906a809fed15ca"
    9) "flags"
   10) "slave"
   11) "link-pending-commands"
   12) "0"
   13) "link-refcount"
   14) "1"
   15) "last-ping-sent"
   16) "0"
   17) "last-ok-ping-reply"
   18) "379"
   19) "last-ping-reply"
   20) "379"
   21) "down-after-milliseconds"
   22) "30000"
   23) "info-refresh"
   24) "3700"
   25) "role-reported"
   26) "slave"
   27) "role-reported-time"
   28) "2904320"
   29) "master-link-down-time"
   30) "0"
   31) "master-link-status"
   32) "ok"
   33) "master-host"
   34) "x.x.0.6"
   35) "master-port"
   36) "6380"
   37) "slave-priority"
   38) "100"
   39) "slave-repl-offset"
   40) "22723187"
2)  1) "name"
    2) "x.x.0.5:6380"
    3) "ip"
    4) "x.x.0.5"
    5) "port"
    6) "6380"
    7) "runid"
    8) "6cfa23eb6179ebe17652dd7381a278fd23f29cdc"
    9) "flags"
   10) "slave"
   11) "link-pending-commands"
   12) "0"
   13) "link-refcount"
   14) "1"
   15) "last-ping-sent"
   16) "0"
   17) "last-ok-ping-reply"
   18) "379"
   19) "last-ping-reply"
   20) "379"
   21) "down-after-milliseconds"
   22) "30000"
   23) "info-refresh"
   24) "3700"
   25) "role-reported"
   26) "slave"
   27) "role-reported-time"
   28) "2894178"
   29) "master-link-down-time"
   30) "0"
   31) "master-link-status"
   32) "ok"
   33) "master-host"
   34) "x.x.0.6"
   35) "master-port"
   36) "6380"
   37) "slave-priority"
   38) "100"
   39) "slave-repl-offset"
   40) "22723187"
5. 使用SENTINEL SET命令更新哨兵的配置
x.x.0.6:26379> sentinel set mymaster down-after-milliseconds 1000



```

## 配置Redis Cluster ##

```text
    在前面的例子當中，我们配置维护了一个RedisSentinel的高可用的架构。但是当Redis的数据急剧增长的时候，必须要要对这个架构进行分区。
对于这个场景Redis从3.0开始支持了Redis-Cluster(Redis集群)是非常有用的。我们需要按照配置，测试和维护的步骤使用Redis集群实现字段的数据分片
和高可用。
优点： 1. 自动分割数据到不同的节点上
       2. 整个集群的部分节点失败或者不可达的情况下能够继续处理命令 
```

注意：
```text
    当Redis集群运行时，每个节点都会打开两个TCP套接字。第一个套接字适用于客户端连接的标准的Redis通信协议，第二个套接字的端口时第一个端口号加上10000
被用作实例键信息交换的通信总线。值10000是硬编码，因此端口大于55536的Redis集群节点是不能启动的。
```


操作步骤
```text
1. 每个Redis实例都自己的配置文件redis.conf。弃用集群功能需要每个redis实例准备一个配置文件，然后相应的修改IP，监听的端口和log文件的路径
daemonize yes
pidfile "/redis/run/redis-6379.pid"
port 6379
dbfilename "dump-6379.rdb"
dir "/redis/data"
...
cluster-enabled yes
cluster-config-file node6379.conf
cluster-node-timeout 10000

```

示例说明

```text
由于没有足够的服务器去演示，这里使用docker来部署集群模式，确保服务能够以多实例运行。
节点分别为:

*.*.*.7:6388@16388 master - 0 1589851954741 5 connected 13654-16383
*.*.*.5:6386@16386 master - 0 1589851952000 3 connected 8194-10923
*.*.*.2:6383@16383 myself,master - 0 1589851949000 1 connected 0-2730
*.*.*.4:6385@16385 master - 0 1589851953000 2 connected 5462-8193
*.*.*.6:6387@16387 master - 0 1589851952000 4 connected 10924-13653
*.*.*.3:6384@16384 master - 0 1589851954000 0 connected 2731-5461**
```
redis集群配置文件为

[1.redis集群配置文件](images/config.tar.gz)

[2.Dockerfile](images/Dockerfile)

[3.docker-compose.yml](images/docker-compose.yml)



准备好了上面的工作之后就可以开始去创建redis集群了。
```text
-------------------------------------------------------------------------------------------
redis_cluster_1   /bin/sh -c /home/redis/src ...   Up      6379/tcp, 0.0.0.0:6383->6383/tcp
redis_cluster_2   /bin/sh -c /home/redis/src ...   Up      6379/tcp, 0.0.0.0:6384->6384/tcp
redis_cluster_3   /bin/sh -c /home/redis/src ...   Up      6379/tcp, 0.0.0.0:6385->6385/tcp
redis_cluster_4   /bin/sh -c /home/redis/src ...   Up      6379/tcp, 0.0.0.0:6386->6386/tcp
redis_cluster_5   /bin/sh -c /home/redis/src ...   Up      6379/tcp, 0.0.0.0:6387->6387/tcp
redis_cluster_6   /bin/sh -c /home/redis/src ...   Up      6379/tcp, 0.0.0.0:6388->6388/tcp
```

此时集群的所有节点都已经起来了，但是此时redis的节点之间还是无法正常的通信的需要有一个节点知道所有的其他节点的信息之后会将这些信息，
此时它也会项其他已知的节点传播它知道的节点的信息(这个就是redis文档中提到的心跳包中的exchange-of-gossip),这样其他节点就可以互相的通信了。
1. 发现的命令使用了redis-cli 执行 CLUSTER MEET，使用示例如下:
```shell script

redis/src/redis-cli -h *.*.*.2 -p 6383 CLUSTER MEET *.*.*.3 6384

redis/src/redis-cli -h *.*.*.2 -p 6383 CLUSTER MEET *.*.*.4 6385

redis/src/redis-cli -h *.*.*.2 -p 6383 CLUSTER MEET *.*.*.5 6386

redis/src/redis-cli -h *.*.*.2 -p 6383 CLUSTER MEET *.*.*.6 6387

redis/src/redis-cli -h *.*.*.2 -p 6383 CLUSTER MEET *.*.*.7 6388

```

2. 分配数据槽
我们额可以在一台主机上使用redis-cli,通过指定主机和端口号的方式进行

```shell script
for i in{0..2730};do redis/src/redis-cli -h *.*.*.2 -p 6383 CLUSTER ADDSLOTS $i;DONE
ok
...

for i in{2731..5461};do redis/src/redis-cli -h *.*.*.3 -p 6384 CLUSTER ADDSLOTS $i;DONE
ok
...

for i in{5462..8193};do redis/src/redis-cli -h *.*.*.4 -p 6385 CLUSTER ADDSLOTS $i;DONE
ok
...

for i in{8194..10923};do redis/src/redis-cli -h *.*.*.5 -p 6386 CLUSTER ADDSLOTS $i;DONE
ok
...

for i in{10924..13653};do redis/src/redis-cli -h *.*.*.6 -p 6387 CLUSTER ADDSLOTS $i;DONE
ok
...

for i in{13654..16383};do redis/src/redis-cli -h *.*.*.7 -p 6388 CLUSTER ADDSLOTS $i;DONE
ok
...

```
注意：我们指定的数据槽只能是在0-16383的范围内，否则会出现错误


执行CLUSTER NODES:

````shell script
*.*.*.7:6388@16388 master - 0 1589851954741 5 connected 13654-16383
*.*.*.5:6386@16386 master - 0 1589851952000 3 connected 8194-10923
*.*.*.2:6383@16383 myself,master - 0 1589851949000 1 connected 0-2730
*.*.*.4:6385@16385 master - 0 1589851953000 2 connected 5462-8193
*.*.*.6:6387@16387 master - 0 1589851952000 4 connected 10924-13653
*.*.*.3:6384@16384 master - 0 1589851954000 0 connected 2731-5461**

````


执行CLUSTER INFO:

```shell script
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:6
cluster_current_epoch:5
cluster_my_epoch:1
cluster_stats_messages_ping_sent:77240
cluster_stats_messages_pong_sent:73688
cluster_stats_messages_meet_sent:6
cluster_stats_messages_sent:150934
cluster_stats_messages_ping_received:73687
cluster_stats_messages_pong_received:77246
cluster_stats_messages_meet_received:1
cluster_stats_messages_received:150934

```
此时使用edis/src/redis-cli -c -h *.*.*.2 -p 6383就可以以集群模式连接到redis集群操作集群了。

## 测试Redis Cluster ##





## 管理Redis Cluster ##


































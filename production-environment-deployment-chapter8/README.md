#  生产环境部署

## 在Linux上部署Redis
 ```text
虽然我们可以通过编译源代码的方式在几乎所有现代操作系统上安装Redis，但是运行Redis，Linux是最常见的操作系统。在启动Redis实例之前，通常需要将一些Linu
讷河和操作系统的参数设置为恰当的值，以便在生产环境中发挥最高的性能。
```


### 操作步骤

1. 设置和内存相关的内核参数

```text
sysctl -w vm.overcommit_memory=1
sysctl -w vm.swappiness=0
使用如下的命令来持久化地保存这些参数
echo 1 > /proc/sys/vm/overcommit_memory
echo 0 > /proc/sys/vm/swappiness

----------------------------------------------------------------------------------------------------------------------------------
参数解释:

内核参数overcommit_memory
可选择的值为0,1,2
0：表示内核将检查是否有足够的可用的内存工应用进程使用；如果有足够的可用的内存，内存申请允许;否则内存申请失败，并且把错误返回给应用进程。
1：表示内核允许分配索引的物理内存，而不关当前的内存状态如何。
2：表示内核允许超过的所有物理内存和交换空间总和内存。

什么是overcommit 和OOM
Linux对大部分的申请内存的请求都恢复"yes",以便能跑更多的程序。因为申请内存之后，并不会马上使用内存，这种技术叫做overcommit.当linux
发现内存不足时，会发生OOM Killer(OOM=out of memory).它会选择杀死一些进程(用户态的，不是内核进程)以便释放内存。当oom-killer发生时，linux
会选择杀死哪些进程？选择进程的函数是oom_badness函数(mm/oom_killer.c中)。该函数会计算每个进程的点数(0~1000)。点数越高，这个进程越有可能被
杀死。每个进程的点数都跟oom_score_adj有关，而且oom_score_adj可以被设置(-1000最低，1000最高)。

内核参数swappiness:

该参数的默认值是60，代表党剩余的物理内存低于40%的时候(40=100-60)时，开始使用交换空间.
vm.swappiness=0
最大限度的使用物理内存，然后才是swap空间，即在内存不足的情况下，当剩余内存低于vm.min_free_kbytes_limit时，使用交换空间。
在内存禁止时有限减少RAM里文件系统缓存的大小，而使用swap空间，这是一种提高数据库性能的推荐做法。
vm.swappiness=1
内核3.5及以上，Red Hat内核版本2.6.32-303及以上，进行最少的交换，而不是禁用。
vm.swappiness=10
当系统内存足够时，推荐设置为该值以提高性能。
vm.swappiness=60
默认值
vm.swappines=100
积极的使用交换空间

```


2. 禁用透明大页(transparent huge page)功能：

```text
在 Linux 中大页分为两种： Huge pages ( 标准大页 ) 和  Transparent Huge pages( 透明大页 ) 。
内存是以块即页的方式进行管理的，当前大部分系统默认的页大小为 4096 bytes 即 4K 。 1MB 内存等于 256 页； 1GB 内存等于 256000 页。
CPU 拥有内置的内存管理单元，包含这些页面的列表，每个页面通过页表条目引用。当内存越来越大的时候， CPU 需要管理这些内存页的成本也就越高，这样会对操作系统的性能产生影响。

Huge pages:

Huge pages  是从 Linux Kernel 2.6 后被引入的，目的是通过使用大页内存来取代传统的 4kb 内存页面， 以适应越来越大的系统内存，让操作系统可以支持现代硬件架构的大页面容量功能。
Huge pages  有两种格式大小： 2MB  和  1GB ， 2MB 页块大小适合用于 GB 大小的内存， 1GB 页块大小适合用于 TB 级别的内存； 2MB 是默认的页大小。

Transparent Huge Pages:

Transparent Huge Pages  缩写  THP ，这个是 RHEL 6 开始引入的一个功能，在 Linux6 上透明大页是默认启用的。
由于 Huge pages 很难手动管理，而且通常需要对代码进行重大的更改才能有效的使用，因此 RHEL 6 开始引入了 Transparent Huge Pages （ THP ）， THP 是一个抽象层，能够自动创建、管理和使用传统大页。
THP 为系统管理员和开发人员减少了很多使用传统大页的复杂性 ,  因为 THP 的目标是改进性能 ,  因此其它开发人员  ( 来自社区和红帽 )  已在各种系统、配置、应用程序和负载中对  THP  进行了测试和优化。这样可让  THP  的默认设置改进大多数系统配置性能。但是 ,  不建议对数据库工作负载使用  THP 。
这两者最大的区别在于 :  标准大页管理是预分配的方式，而透明大页管理则是动态分配的方式。
```

```shell script

1. 查看是否使用了透明大页
[root@91f972f07892 vm]# cat /sys/kernel/mm/transparent_hugepage/enabled 
[always] madvise never

2. 关闭的透明大页的方法
 echo never > /sys/kernel/mmtransparent_hugepage/enabled
 开启的是echo always > /sys/kernel/mmtransparent_hugepage/enabled

关闭透明大页的原因：
简单来说就是 Oracle Linux team 在测试的过程中发现，如果 linux 开启透明巨页THP，则 I/O 读写性能降低 30%；如果关闭透明巨页 THP，I/O 读写性能则恢复正常。另，建议在 Oracle Database 中不要使用 THP。



```



3. 对于网络的优化。

```text

sysctl -w net.core.somaxconn=65535
sysctl -w net.ipv4.tcp_max_syn_backlog=65535
持久化可以使用一下的方式
echo "net.core.somaxconn=65535" >> /etc/sysctl.conf
echo "net.ipv4.tcp_max_syn_backlog=65535" >> /etc/sysctl.conf

参数解释：
somaxconn:该内核参数默认值一般是128，对于负载很大的服务程序来说大大的不够。一般会将它修改为2048或者更大。
echo 2048 >   /proc/sys/net/core/somaxconn    但是这样系统重启后保存不了
在/etc/sysctl.conf中添加如下
net.core.somaxconn = 2048然后在终端中执行sysctl -p留个记录

tcp_max_syn_backlog  SYN队列的长度，时常称之为未建立连接队列。系统内核维护着这样的一个队列，用于容纳状态为SYN_RESC的TCP连接(half-open connection),即那些依然尚未得到客户端确认(ack)的TCP连接请求。加大该值，可以容纳更多的等待连接的网络连接数。



```

4. 将进程能够打开的文件数设置为更加高得数值，我们需要先切换到启动Redis进程得用户，然后执行ulimit命令

```shell script
ulimit -n 288000
注意：
我们必须要将nofile设置为一个小于/proc/sys/fs/file-max 的值。因此设置之前我们需要使用cat命令查看/proc/sys/fs/file-max 的值的大小
[sei@cms ipv4]$ ulimit -Hn -Sn
open files                      (-n) 1024
open files                      (-n) 1024
```



### 工作原理





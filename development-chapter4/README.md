# 使用REDIS进行开发

## Redis的阻塞操作 ##
```text
KEYS * ,FLUSHDB,HDEL和DEL等命令都是阻塞REDIS服务器的，这些操作的时间复杂度都是O(n)的API,
在操作这些命令的时候应该特别的小心

```

## 使用Java连接到Redis ##


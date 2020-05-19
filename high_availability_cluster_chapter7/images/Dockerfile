FROM chendom/centos-jdk8:v1.0.0   
MAINTAINER chendom "77252571631@qq.com"
WORKDIR /home
RUN yum install -y wget gcc && \      
        rpm --rebuilddb && \
        yum -y install gcc automake autoconf libtool make && \
        yum -y install net-tools && \
        yum -y install tar && \
        wget http://download.redis.io/redis-stable.tar.gz && \
        tar -xvzf redis-stable.tar.gz && \
        mv redis-stable/ redis && \
        rm -f redis-stable.tar.gz && \
        yum clean all && \
        cd redis && \
        make && make install        
EXPOSE 6379
ENTRYPOINT /home/redis/src/redis-server /home/node/redis.conf
CMD ["redis-server"]

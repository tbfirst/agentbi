# Redis集群帮助文档

以当前项目要创建的分片集群，在虚拟环境中的docker中创建为例

1. 首先确保你有至少一个的redis镜像
2. 然后cd到要创建docker compose文件的目录去
3. 在当前目录创建docker compose文件
   注意镜像记得修改，端口号也可按照自己的情况进行修改
  ```bash
version: '3.8'

networks:
  redis-cluster:
    driver: bridge

services:
  redis-7000:
    image: redis:latest
    container_name: redis-7000
    restart: always
    networks:
      - redis-cluster
    ports:
      - "7000:7000"
      - "17000:17000"
    command: >
      redis-server 
      --port 7000
      --cluster-enabled yes 
      --cluster-config-file nodes.conf 
      --cluster-node-timeout 5000 
      --cluster-announce-ip redis-7000
      --cluster-announce-port 7000
      --cluster-announce-bus-port 17000
      --appendonly yes
      --bind 0.0.0.0
      --protected-mode no
    volumes:
      - redis-data-7000:/data  # 使用命名卷

  redis-7001:
    image: redis:latest
    container_name: redis-7001
    restart: always
    networks:
      - redis-cluster
    ports:
      - "7001:7001"
      - "17001:17001"
    command: >
      redis-server 
      --port 7001
      --cluster-enabled yes 
      --cluster-config-file nodes.conf 
      --cluster-node-timeout 5000 
      --cluster-announce-ip redis-7001
      --cluster-announce-port 7001
      --cluster-announce-bus-port 17001
      --appendonly yes
      --bind 0.0.0.0
      --protected-mode no
    volumes:
      - redis-data-7001:/data

  redis-7002:
    image: redis:latest
    container_name: redis-7002
    restart: always
    networks:
      - redis-cluster
    ports:
      - "7002:7002"
      - "17002:17002"
    command: >
      redis-server 
      --port 7002
      --cluster-enabled yes 
      --cluster-config-file nodes.conf 
      --cluster-node-timeout 5000 
      --cluster-announce-ip redis-7002
      --cluster-announce-port 7002
      --cluster-announce-bus-port 17002
      --appendonly yes
      --bind 0.0.0.0
      --protected-mode no
    volumes:
      - redis-data-7002:/data

  redis-7003:
    image: redis:latest
    container_name: redis-7003
    restart: always
    networks:
      - redis-cluster
    ports:
      - "7003:7003"
      - "17003:17003"
    command: >
      redis-server 
      --port 7003
      --cluster-enabled yes 
      --cluster-config-file nodes.conf 
      --cluster-node-timeout 5000 
      --cluster-announce-ip redis-7003
      --cluster-announce-port 7003
      --cluster-announce-bus-port 17003
      --appendonly yes
      --bind 0.0.0.0
      --protected-mode no
    volumes:
      - redis-data-7003:/data

  redis-7004:
    image: redis:latest
    container_name: redis-7004
    restart: always
    networks:
      - redis-cluster
    ports:
      - "7004:7004"
      - "17004:17004"
    command: >
      redis-server 
      --port 7004
      --cluster-enabled yes 
      --cluster-config-file nodes.conf 
      --cluster-node-timeout 5000 
      --cluster-announce-ip redis-7004
      --cluster-announce-port 7004
      --cluster-announce-bus-port 17004
      --appendonly yes
      --bind 0.0.0.0
      --protected-mode no
    volumes:
      - redis-data-7004:/data

  redis-7005:
    image: redis:latest
    container_name: redis-7005
    restart: always
    networks:
      - redis-cluster
    ports:
      - "7005:7005"
      - "17005:17005"
    command: >
      redis-server 
      --port 7005
      --cluster-enabled yes 
      --cluster-config-file nodes.conf 
      --cluster-node-timeout 5000 
      --cluster-announce-ip redis-7005
      --cluster-announce-port 7005
      --cluster-announce-bus-port 17005
      --appendonly yes
      --bind 0.0.0.0
      --protected-mode no
    volumes:
      - redis-data-7005:/data

volumes:  # 声明命名卷
  redis-data-7000:
  redis-data-7001:
  redis-data-7002:
  redis-data-7003:
  redis-data-7004:
  redis-data-7005:

  ```
4. 创建容器

```bash
docker compose up -d
```

5\. 创建集群

```bash
# 选择可正确网络连接的容器进行测试
docker exec -it redis-7000 redis-cli -p 7000 PING
# 预计返回PONG

# 创建集群
docker exec -it redis-7000 redis-cli --cluster create   redis-7000:7000 redis-7001:7001 redis-7002:7002   redis-7003:7003 redis-7004:7004 redis-7005:7005   --cluster-replicas 1
```


6\. 验证集群
```bash
docker exec -it redis-7000 redis-cli -c -p 7000 CLUSTER INFO

for port in 7003 7004 7005; do   echo "Slave $port:";   docker exec redis-$port redis-cli -p $port INFO replication | grep master_link_status; done
```

7\. 修改hosts文件

以管理员身份运行hosts文件，添加以下内容即可：
```bash
# Redis Cluster
127.0.0.1 redis-7000
127.0.0.1 redis-7001
127.0.0.1 redis-7002
127.0.0.1 redis-7003
127.0.0.1 redis-7004
127.0.0.1 redis-7005
```
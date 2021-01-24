# BLOCKCHAIN

## 0.环境要求

​	配置文件夹的gradle 6.8（解压+环境变量设置https://blog.csdn.net/wenwujava/article/details/79499465）

​	java 1.8+即可（之前作业有做 应该就有环境）

​	下面的运行在su下面做 以防出现奇奇怪怪的东西

​	修改java文件后，直接重复步骤2、3即可



## 1.首先按照教程运行节点

```
cd fisco\nodes\127.0.0.1
bash start_all.sh
```



## 2.编译项目

```bash
cd fisco/asset-app
./gradlew build
```



## 3.运行代码

```bash
cd fisco/asset-app/dist
bash asset_run.sh deploy
```


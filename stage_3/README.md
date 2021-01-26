# BLOCKCHAIN FINAL PROJECT

## 一、环境要求

- Linux 系统（如Centos7、Ubuntu）
- 配置`Blockchain-Final-Project/stage_3/blockchain_final/gradle-6.8-bin`
  - （解压+环境变量设置，可参考https://blog.csdn.net/wenwujava/article/details/79499465）
- Java 1.8+
- FISCO BCOS 2.0
- Golang + Gotty

​	

## 二、运行方法

下面的运行请在 su 下面做（进入 root 管理员权限）

### 1. 运行区块链节点

```
cd fisco/nodes/127.0.0.1
bash start_all.sh
```

<img src="README.assets/image-20210125144553484.png" alt="image-20210125144553484" style="zoom:67%;" />



### 2. 编译并运行实验项目

```bash
# 编译实验项目，可选，本仓库的
# cd fisco/asset-app
# ./gradlew build

# 运行实验项目
gotty -w -p 8081 --permit-arguments /bin/bash >/dev/null 2>&1 &

# 浏览器打开 localhost:8081 或 127.0.0.1:8081 或 ip:8081
cd fisco/asset-app/dist
bash asset_run.sh deploy
```



## 三、运行实例

- 具体项目实验报告可见`大作业实验报告.md`

  通过项目简介、项目具体设计（链端、后端和后端）、实验环境、实验测试等来详细介绍本次区块链期末项目。

- 具体项目运行实例可见`大作业实验项目演示视频_车企tql_轮胎厂hjdl_轮毂厂hdl.mp4`

  根据实验报告中的基于区块链的供应链金融平台的实例，通过车企`tql`、轮胎厂`hjdl`、轮毂厂`hdl`和银行`bank`的金融交易，来进行实验项目的演示。



## 附录：Stage 3目录

| stage_3                                                   |                                                              |
| --------------------------------------------------------- | ------------------------------------------------------------ |
| `README.md`                                               | 项目安装部署流程                                             |
| `大作业实验项目演示视频_车企tql_轮胎厂hjdl_轮毂厂hdl.mp4` | 项目演示视频                                                 |
| `大作业实验报告.md`                                       | 本次区块链大作业的实验报告                                   |
| 部分代码文件【文件夹】                                    | 包含 前端+后端+链端 的关键代码文件                           |
| blockchain_final【文件夹】                                | 完整的项目文件（基于区块链的供应链金融平台）                                                                                                      包括已编译好的实验项目`fisco`；`gradle-6.8.zip`；`目录树`（fisco文件夹的目录结构） |
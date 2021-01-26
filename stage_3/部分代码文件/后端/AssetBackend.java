package org.fisco.bcos.asset.client;

import java.util.*;
import java.io.*;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.Properties;

import org.fisco.bcos.asset.contract.Asset.CreateTableEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.RegisterEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.TransferEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.CreateReceiptEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.DivideReceiptEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.FinanceReceiptEventEventResponse;
import org.fisco.bcos.asset.contract.Asset.RepayReceiptEventEventResponse;
import org.fisco.bcos.asset.contract.Asset;
import org.fisco.bcos.sdk.BcosSDK;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple2;
import org.fisco.bcos.sdk.abi.datatypes.generated.tuples.generated.Tuple3;
import org.fisco.bcos.sdk.client.Client;
import org.fisco.bcos.sdk.crypto.keypair.CryptoKeyPair;
import org.fisco.bcos.sdk.model.TransactionReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;


public class AssetBackend {

  static Logger logger = LoggerFactory.getLogger(AssetBackend.class);
  private BcosSDK bcosSDK;
  private Client client;
  private CryptoKeyPair cryptoKeyPair;

//记录资产地址
  public void recordAssetAddr(String address) throws FileNotFoundException, IOException {
    Properties prop = new Properties();
    prop.setProperty("address", address);
    final Resource contractResource = new ClassPathResource("contract.properties");
    FileOutputStream fileOutputStream = new FileOutputStream(contractResource.getFile());
    prop.store(fileOutputStream, "contract address");
  }

//加载资产地址
  public String loadAssetAddr() throws Exception {
    // load Asset contact address from contract.properties
    Properties prop = new Properties();
    final Resource contractResource = new ClassPathResource("contract.properties");
    prop.load(contractResource.getInputStream());

    String contractAddress = prop.getProperty("address");
    if (contractAddress == null || contractAddress.trim().equals("")) {
      throw new Exception("Load Asset contract address failed, please deploy it first. ");
    }
    logger.info("Load Asset address from contract.properties, address is {}", contractAddress);
    return contractAddress;
  }

//初始化服务
  public void initialize() throws Exception {

    // init the Service
    @SuppressWarnings("resource")
    ApplicationContext context = new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
    bcosSDK = context.getBean(BcosSDK.class);
    client = bcosSDK.getClient(1);
    cryptoKeyPair = client.getCryptoSuite().createKeyPair();

    client.getCryptoSuite().setCryptoKeyPair(cryptoKeyPair);
    logger.debug("Create client for group1, account address is " + cryptoKeyPair.getAddress());
  }

//部署合约并记录地址
  public void deployAssetAndRecordAddr() {

    try {
      Asset asset = Asset.deploy(client, cryptoKeyPair);
      System.out.println("部署合约成功，合约地址: " + asset.getContractAddress());

      recordAssetAddr(asset.getContractAddress());

    } catch (Exception e) {
      System.out.println("部署合约失败，错误信息: " + e.getMessage());
    }
  }

// 四个基础功能：1.注册账户、2.查询账户余额、3.查询交易、4.资产转移
//1.注册账户
  public void registerAssetAccount(String assetAccount, BigInteger asset_value) {
    try {
      String contractAddress = loadAssetAddr();
      Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);

      TransactionReceipt receipt = asset.register(assetAccount, asset_value);
      List<Asset.RegisterEventEventResponse> response = asset.getRegisterEventEvents(receipt);

      if (!response.isEmpty()) {
        if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
          System.out.printf("注册账户成功，账户名为 %s，信用额度为 %s \n", assetAccount, asset_value);
        } else if (response.get(0).ret.compareTo(new BigInteger("-1")) == 0) {
          System.out.printf("注册账户失败，账户已存在\n");
        }
        else {
          System.out.printf("注册账户失败，其他错误\n");
        }
      } else {
        System.out.println("抱歉，您输入的交易ID已存在于区块链系统中\n请重新尝试输入其他交易ID，谢谢");
      }
    } catch (Exception e) {
      logger.error(" registerAssetAccount exception, error message is {}", e.getMessage());
      System.out.println("注册账户异常，错误信息: " + e.getMessage());
    }
  }

//2.查询账户余额
  public void queryAssetAmount(String assetAccount) {
    try {
      String contractAddress = loadAssetAddr();
      Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);

      Tuple2<BigInteger, BigInteger> result = asset.selectAccount(assetAccount);

      if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
        System.out.printf("账户 %s 的信用额度为：%s \n", assetAccount, result.getValue2());
      } else {
        System.out.printf("账户 %s 不存在\n", assetAccount);
      }
    } catch (Exception e) {
      logger.error(" queryAssetAmount exception, error message is {}", e.getMessage());
      System.out.println("查询信用额度异常，错误信息: " + e.getMessage());
    }
  }

//3.查询交易
public void queryAssetTransaction(String t_id) {
  try {
    String contractAddress = loadAssetAddr();
    Asset asset = Asset.load(contractAddress, client,  cryptoKeyPair);

    Tuple3<BigInteger, List<BigInteger>, List<String>> result = asset.selectReceipt(t_id);

    if (result.getValue1().compareTo(new BigInteger("0")) == 0) {
            String temp1 = new String(result.getValue3().get(0));
            String temp2 = new String(result.getValue3().get(1));
            System.out.printf("交易\n ID: " + t_id + "; 借债人: " + temp1 + "; 债权人: " + temp2 + "; 原始金额: "
                + result.getValue2().get(0) + "; 剩余待还: " + result.getValue2().get(1) + "\n");
    } else {
      System.out.printf("交易 %s 不存在\n", t_id);
    }
  } catch (Exception e) {
    logger.error(" queryAssetTransaction exception, error message is {}", e.getMessage());
    System.out.println("查询交易异常，错误信息: " + e.getMessage());
  }
}

//4.资产转移
public void transferAsset(String fromAssetAccount, String toAssetAccount, BigInteger asset_value) {
  try {
    String contractAddress = loadAssetAddr();
    Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);

    TransactionReceipt receipt = asset.transfer(fromAssetAccount, toAssetAccount, asset_value);
    List<Asset.TransferEventEventResponse> response = asset.getTransferEventEvents(receipt);

    if (!response.isEmpty()) {
      if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
        System.out.printf("资产转移成功，从 %s 转移到 %s %s 额度\n",
            fromAssetAccount, toAssetAccount, asset_value);
      } else {
        System.out.printf("资产转移失败，ret code : %s \n", response.get(0).ret.toString());
        System.out.printf("-1 转移资产账户存在\n-2 接收资产账户不存在\n-3 金额不足\n-4 金额溢出\n-5 其他错误\n");
      }
    } else {
      System.out.println("抱歉，您输入的交易ID已存在于区块链系统中\n请重新尝试输入其他交易ID，谢谢");
    }
  } catch (Exception e) {
    logger.error(" transferAsset exception, error message is {}", e.getMessage());
    System.out.println("资产转移异常，错误信息: " + e.getMessage());
  }
}

// 四个交易功能：1.添加交易、2.拆分交易、3.融资、4.结算交易
//1.添加交易(签发应收账款交易上链)
  public void createAssetTransaction(String t_id, String borrower, String debtee, BigInteger money) {
		try {
			String contractAddress = loadAssetAddr();
      Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);
      
			TransactionReceipt receipt = asset.createReceipt(t_id, borrower, debtee, money);
      List<CreateReceiptEventEventResponse> response = asset.getCreateReceiptEventEvents(receipt);
      
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf("添加交易成功! ID : " + t_id + " 借债人: " + borrower + " 债权人: " + debtee + " 额度: " + money +"\n");
				} else {
					System.out.printf("添加交易失败, ret code: %s \n", response.get(0).ret.toString());
          System.out.printf("-1 交易已存在\n-2 资产转移错误\n-3 借债人资产额度不够\n-4 其他错误\n");
				}
			} else {
				System.out.println("抱歉，您输入的交易ID已存在于区块链系统中\n请重新尝试输入其他交易ID，谢谢");
			}
		} catch (Exception e) {
			logger.error(" createAssetTransaction exception, error message is {}", e.getMessage());
			System.out.println("添加交易异常，错误信息: " + e.getMessage());
		}
  }

//2.拆分交易(应收账款的转让上链)
public void divideAssetTransaction(String original_id, String new_id, String newDebtee, BigInteger divide_money) {
  try {
    String contractAddress = loadAssetAddr();
    Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);

    TransactionReceipt receipt = asset.divideReceipt(original_id, new_id, newDebtee, divide_money);
    List<DivideReceiptEventEventResponse> response = asset.getDivideReceiptEventEvents(receipt);

    if (!response.isEmpty()) {
      if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
        System.out.printf("拆分交易成功! original_id: "+ original_id + " new_id: " + new_id + " 新债权人: " 
                          + newDebtee + " 拆分金额: " + divide_money +"\n");
      } else {
        System.out.printf("拆分交易失败, ret code : %s \n", response.get(0).ret.toString());
        System.out.printf("-1 转让的交易不存在\n-2 账户不存在\n-3 转让的金额大于原交易未结算的金额\n-4 资产返还错误\n-5 新交易创建不成功\n");
      }
    } else {
      System.out.println("抱歉，您输入的交易ID已存在于区块链系统中\n请重新尝试输入其他交易ID，谢谢");
    }
  } catch (Exception e) {
    logger.error(" divideAssetTransaction, error message is {}", e.getMessage());
    System.out.println("拆分交易异常，错误信息: " + e.getMessage());
  }
}

//3.融资(利用应收账款向银行融资上链)
  public void financeAssetTransaction(String f_id, String financer, BigInteger finance_money) {
    try {
      String contractAddress = loadAssetAddr();
      Asset asset = Asset.load(contractAddress, client, cryptoKeyPair);
    
      TransactionReceipt receipt = asset.financeReceipt(f_id, financer,  finance_money);
      List<FinanceReceiptEventEventResponse> response = asset.getFinanceReceiptEventEvents(receipt);
    
      if (!response.isEmpty()) {
        if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
          System.out.printf("融资成功! ID : " + f_id + " 融资人: " + financer + " 额度: " + finance_money +"\n");
        } else {
          System.out.printf("融资失败, ret code: %s \n", response.get(0).ret.toString());
          System.out.printf("-1 融资人不存在\n-2 融资额大于融资人目前的资产额度\n-3 其他错误\n");
        }
      } else {
        System.out.println("抱歉，您输入的交易ID已存在于区块链系统中\n请重新尝试输入其他交易ID，谢谢");
      }
    } catch (Exception e) {
      logger.error(" financeAssetTransaction exception, error message is {}", e.getMessage());
      System.out.println("融资异常，错误信息: " + e.getMessage());
    }
  }

//4.结算交易(应收账款支付结算上链)
  public void repayAssetTransaction(String t_id, BigInteger repay_money) {
		try {
			String contractAddress = loadAssetAddr();
      Asset asset = Asset.load(contractAddress, client,  cryptoKeyPair);

			TransactionReceipt receipt = asset.repayReceipt(t_id, repay_money);
			List<RepayReceiptEventEventResponse> response = asset.getRepayReceiptEventEvents(receipt);
			
			if (!response.isEmpty()) {
				if (response.get(0).ret.compareTo(new BigInteger("0")) == 0) {
					System.out.printf("成功支付结算交易. ID: "+ t_id + " 额度 :" + repay_money + "\n" );
				} else {
          System.out.printf("支付结算失败, ret code: %s\n", response.get(0).ret.toString());
          System.out.printf("-1 交易不存在\n-2 还债金额大于借债\n-3 其他错误\n-4 资产返还错误\n");
				}
			} else {
				System.out.println("抱歉，您输入的交易ID已存在于区块链系统中\n请重新尝试输入其他交易ID，谢谢");
			}
		} catch (Exception e) {

			logger.error(" repayAssetTransaction exception, error message is {}", e.getMessage());
			System.out.println("支付结算交易异常，错误信息: " + e.getMessage());
		}
  }
}


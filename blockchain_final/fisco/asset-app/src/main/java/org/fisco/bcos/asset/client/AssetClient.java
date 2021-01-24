package org.fisco.bcos.asset.client;

import java.util.*;

import java.io.*;
import java.math.BigInteger;

import org.fisco.bcos.asset.client.AssetBackend;

public class AssetClient {
    // 用户密码对应关系
    private Map<String, String> passwordMap;
    //用户状态
    private boolean status; 
    //当前登录用户
    private String current; 
    private String user_info_path;

    private Scanner scanner;

    public AssetClient() {
        user_info_path = "info.txt";
        passwordMap = new HashMap<String, String>();
        scanner = new Scanner(System.in);
        status = false;
    }

    public String getCurrentUser(){
        return this.current;
    }

    public void setCurrentNull(){
        this.current = null;
    }

    public boolean getStatus(){
        return this.status;
    }

    public void read_file(){
        try{
            FileReader fd = new FileReader(user_info_path);
            BufferedReader br = new BufferedReader(fd);
            String s1 = null;
            while ((s1 = br.readLine()) != null) {
                String[] temp = s1.split("  ");
                passwordMap.put(temp[0],temp[1]);
            }
           br.close();
           fd.close();
        } catch (IOException e) {
            System.out.println("Error:" + e.getMessage());
        }
    }

    public void write_file()
	  {
		    try{
            File file = new File(user_info_path);
            FileWriter fw = new FileWriter(file,false);
            for (String key : passwordMap.keySet()) {
                String temp = key + "  " + passwordMap.get(key);
                fw.write(temp + "\n");
            }
            fw.flush();
            fw.close();   
        } catch (IOException e) {
            System.out.println("Error:" + e.getMessage());
        }
	  }

    public boolean login(AssetBackend backend)
    {
        read_file();
        int select;
        String name, password, password_again;
        System.out.print("请输入数字：1）登录已有账户 2）注册新账户\n");
        select = scanner.nextInt();

        if (select == 1) {
            name = (String)scanner.nextLine();
            System.out.print("请输入用户名: ");
            name = (String)scanner.nextLine();
            System.out.print("用户" + name + ",请输入密码: ");
            password = (String)scanner.nextLine();
            if (passwordMap.get(name) != null && passwordMap.get(name).compareTo(password) == 0) {
                current = name;
                System.out.print("登录成功\n");
                status = true;
                return false;
            }
            else {
                System.out.print("登录失败\n");
                status = false;
                return false;
            }
        }

        else if (select == 2) {
            name = (String)scanner.nextLine();
            System.out.print("请输入注册的用户名: ");
            name = (String)scanner.nextLine();
            if (passwordMap.get(name) != null) {
                System.out.print("用户名已经被使用过\n");
                status = false;
                return false;
            }
            else {
                System.out.print("用户" + name + ",请输入密码: ");
                password = (String)scanner.nextLine();
                System.out.print("请再一次输入密码: ");
                password_again = (String)scanner.nextLine();
                if (passwordMap.get(name) == null && password.compareTo(password_again) == 0) {
                    passwordMap.put(name, password);
                    System.out.print("请输入资产额度: ");
                    String asset_value = (String)scanner.nextLine();
                    backend.registerAssetAccount(name, new BigInteger(asset_value));
                    write_file();
                    read_file();
                    System.out.print("注册成功\n");
                    status = false;
                    return false;
                }
                else {
                    System.out.print("输入错误\n");
                    status = false;
                    return false;
                }
            }
        }
        return false;
    }

    public void clear() {
        for (int i = 0; i < 50; i++) System.out.print("\n");
    }

    public void msg() {
        System.out.print("尊敬的" + current + ", 接下来你想做什么呢?\n");
        System.out.println("0: 查询个人信用额度\n1: 查询交易\n2: 添加交易\n3: 拆分交易\n4: 向银行贷款融资\n5: 支付结算交易\n6: 退出系统\n\n");
    }
    
      //运行前端
  public static void main(String[] args) throws Exception {

    Scanner scanner;
    AssetBackend backend = new AssetBackend();
    backend.initialize();
    backend.deployAssetAndRecordAddr();
    scanner = new Scanner(System.in);
    
    AssetClient client = new AssetClient ();

    boolean flag = true;
    while (flag) {
      while (client.login(backend) == false) {
        System.out.print("已登录! \n");
        if (client.getStatus() == false) {
          System.out.print("Status is false!\n");
          continue;
        }
        break;
      }

      client.clear();
      boolean flag_cmd = true;
      String waitkey, debtee, account;
      String money;
      int trans_id = 5000;
      String trans_id_str, old_trans_id_str;

      while(flag_cmd)
      {
        client.msg();
        int select_cmd;

        if(scanner.hasNextInt()){
          select_cmd = scanner.nextInt();
          switch(select_cmd){
            case 0:   //查询账户信用额度
              System.out.print("查询账户信用额度：\n");
              account = (String)scanner.nextLine();
              System.out.print("请输入要查询的账户: ");
              account = (String)scanner.nextLine();

              backend.queryAssetAmount(account);
              
              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
              break;

            case 1:   //查询交易
              System.out.print("查询交易\n");
              trans_id_str = (String)scanner.nextLine();
              System.out.print("请输入交易ID: ");
              trans_id_str = (String)scanner.nextLine();

              backend.queryAssetTransaction(trans_id_str);
              
              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
              break;

            case 2:   //添加交易
              System.out.print("添加交易\n");
              debtee = (String)scanner.nextLine();
              System.out.print("请选择债权人账户: ");
              debtee = (String)scanner.nextLine();

              System.out.print("请输入交易额度: ");
              money = (String)scanner.nextLine();

              trans_id_str = trans_id+"";
              trans_id += 1;

              backend.createAssetTransaction(trans_id_str, client.getCurrentUser(), debtee, new BigInteger(money));

              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
              break;
            
            case 3:   //拆分交易
              System.out.print("拆分交易\n");
              old_trans_id_str = (String)scanner.nextLine();
              System.out.print("请输入交易ID: ");
              old_trans_id_str = (String)scanner.nextLine();

              System.out.print("请输入新创建交易的债权人账户: ");
              debtee = (String)scanner.nextLine();

              System.out.print("请输入拆分额度: ");
              money = (String)scanner.nextLine();

              trans_id_str = trans_id+"";
              trans_id += 1;

              backend.divideAssetTransaction(old_trans_id_str, trans_id_str, debtee, new BigInteger(money));

              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
              break;

            case 4:   //向银行贷款融资
              System.out.print("向银行贷款融资\n");
              money = (String)scanner.nextLine();
              System.out.print("请输入额度: ");
              money = (String)scanner.nextLine();

              trans_id_str = trans_id+"";
              trans_id += 1;

              backend.financeAssetTransaction(trans_id_str, client.getCurrentUser(), new BigInteger(money));

              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
              break;

            case 5:   //支付结算交易
              System.out.print("支付结算交易\n");
              trans_id_str = (String)scanner.nextLine();
              System.out.print("请输入交易ID: ");
              trans_id_str = (String)scanner.nextLine();

              System.out.print("请输入额度: ");
              money = (String)scanner.nextLine();

              backend.repayAssetTransaction(trans_id_str, new BigInteger(money));

              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
              break;
              
            case 6: // 退出系统
			        client.setCurrentNull();
		          flag_cmd = false;
              break;

            default:
              System.out.print("非法输入\n");
              System.out.print("等待下一步操作.....");
              waitkey = (String)scanner.nextLine();
              waitkey = (String)scanner.nextLine();
              client.clear();
							break;
          }
        }
      }
    }
    client.clear();
    client.write_file();
    System.out.println("退出登陆");
    System.exit(0);
  }
}

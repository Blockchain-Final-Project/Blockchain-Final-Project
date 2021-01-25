pragma solidity ^0.4.25;
pragma experimental ABIEncoderV2;

import "./Table.sol";

contract Asset {

    //State variables
    string Bank_name = "bank";
    int256 Bank_asset_value = 99999;

    // event
    // 建表
    event CreateTableEvent(int count);
    // 资产注册
    event RegisterEvent(int256 ret, string account, int256 asset_value);
     // 资产额度转移
    event TransferEvent(int256 ret, string from_account, string to_account, int256 asset_value);
    // 签发应收账款交易上链（添加交易）
    event CreateReceiptEvent(int256 ret, string id, string borrower, string debtee, int256 money);
    // 应收账款的转让上链（拆分交易）
    event DivideReceiptEvent(int256 ret, string original_id, string new_id, string newDebtee, int256 money);
    // 利用应收账款向银行融资上链（融资）
    event FinanceReceiptEvent(int256 ret, string id, string financer, int256 money);
    // 应收账款支付结算上链（返还交易）
    event RepayReceiptEvent(int256 ret, string id, int256 money);
    

    constructor() public {
        // 构造函数中创建t_asset表
        createTable();
        // 注册银行账号
        register(Bank_name, Bank_asset_value);
    } 


    function createTable() public returns(int) {
        int count = 0;
        TableFactory tf = TableFactory(0x1001);
        // 资产管理表, key : account, field : asset_value
        // |   资产账户(主键)      |     资产额度       |
        // |-------------------- |-------------------|
        // |        account      |    asset_value    |
        // |---------------------|-------------------|
        // 
        // 创建资产管理表
        count += tf.createTable("t_asset", "account", "asset_value");

        // 交易记录表, key: id, field: borrower, debtee, money, status
        // | 交易单号(key) | 借债人 | 债权人 | 债务金额 |   状态   |
        // |-------------|------|-------|---------|---------|
        // |     id      | borrower | debtee  |  money  | status  |
        // |-------------|------|-------|---------|---------|
        // 创建交易记录表
        count += tf.createTable("t_receipt", "id","borrower, debtee, money, status");
        emit CreateTableEvent(count);
        return count;
    }


    // 返回资产管理表
    function openAssetTable() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_asset");
        return table;
    }


    // 返回交易记录表
    function openReceiptTable() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_receipt");
        return table;
    }


     /*
    描述 : 资产注册
    参数 ： 
            account :      资产账户
            asset_value  : 资产金额
    返回值：
            0  资产注册成功
            -1 资产账户已存在
            -2 其他错误
    */
    function register(string account, int256 asset_value) public returns(int256){
        int256 ret_code = 0;
        int256 ret = 0;
        int256 temp_asset_value = 0;
        // 查询账户是否存在
        (ret, temp_asset_value) = selectAccount(account);
        if(ret != 0) {
            Table table = openAssetTable();
            
            Entry entry_register = table.newEntry();
            entry_register.set("account", account);
            entry_register.set("asset_value", int256(asset_value));
            // 插入
            int count = table.insert(account, entry_register);
            if (count == 1) {
                // 成功
                ret_code = 0;
            } else {
                // 失败? 无权限或者其他错误
                ret_code = -2;
                table.remove(account, table.newCondition());
            }
        } else {
            // 账户已存在
            ret_code = -1;
        }

        emit RegisterEvent(ret_code, account, asset_value);

        return ret_code;
    }


    /*
    描述 : 根据资产账户查询资产金额
    参数 ： 
            account : 资产账户
    返回值：
            参数一： 成功返回 0, 账户不存在返回 -1
            参数二： 第一个参数为 0 时有效，代表资产金额
    */
    function selectAccount(string account) public constant returns(int256, int256) {
        // 打开表
        Table table = openAssetTable();
        // 查询
        Entries entries_account = table.select(account, table.newCondition());
        int256 asset_value = 0;
        if (0 == int256(entries_account.size())) {
            return (-1, asset_value);
        } else {
            Entry entry = entries_account.get(0);
            return (0, int256(entry.getInt("asset_value")));
        }
    }


    /*
    描述 : 根据交易 ID 查询交易信息
    参数 ： 
            id : 交易编号
    返回值：
            参数一： 若成功返回 0
            参数二： 若成功数组首个元素为初始交易金额, 第二个为未结清金额
            参数三： 若成功则第一个元素为债权人, 第二个为借债人
    */
    function selectReceipt(string id) public constant returns(int256, int256[], string[]) {
        // 打开表
        Table table = openReceiptTable();
        // 查询
        Entries entries_receipt = table.select(id, table.newCondition());

        // ret_code
        int256 ret_code = 0;
        // money, status
        int256[] memory info_list = new int256[](2);
        // borrower, debtee   
        string[] memory user_list = new string[](2);   
        if (0 == int256(entries_receipt.size())) {
            ret_code = -1;
            return (ret_code, info_list, user_list);
        } else {
            Entry entry = entries_receipt.get(0);
            info_list[0] = entry.getInt("money");
            info_list[1] = entry.getInt("status");
            user_list[0] = entry.getString("borrower");
            user_list[1] = entry.getString("debtee");
            return (ret_code, info_list, user_list);
        }
    }


    /*
    描述 : 资产转移
    参数 ： 
            from_account : 转移资产账户
            to_account ：  接收资产账户
            asset_value ： 转移金额
    返回值：
            0  资产转移成功
            -1 转移资产账户不存在
            -2 接收资产账户不存在
            -3 金额不足 
            -4 金额溢出
            -5 其他错误
    */
    function transfer(string from_account, string to_account, int256 asset_value) public returns(int256) {
        // 查询转移资产账户信息
        int ret_code = 0;
        int256 ret = 0;
        int256 from_asset_value = 0;
        int256 to_asset_value = 0;
        
        // 转移账户是否存在?
        (ret, from_asset_value) = selectAccount(from_account);
        if(ret != 0) {
            ret_code = -1;
            // 转移账户不存在
            emit TransferEvent(ret_code, from_account, to_account, asset_value);
            return ret_code;

        }

        // 接受账户是否存在?
        (ret, to_asset_value) = selectAccount(to_account);
        if(ret != 0) {
            ret_code = -2;
            // 接收资产的账户不存在
            emit TransferEvent(ret_code, from_account, to_account, asset_value);
            return ret_code;
        }

        if(from_asset_value < asset_value) {
            ret_code = -3;
            // 转移资产的账户金额不足
            emit TransferEvent(ret_code, from_account, to_account, asset_value);
            return ret_code;
        } 

        if (to_asset_value + asset_value < to_asset_value) {
            ret_code = -4;
            // 接收账户金额溢出
            emit TransferEvent(ret_code, from_account, to_account, asset_value);
            return ret_code;
        }

        Table table = openAssetTable();

        Entry entry_from = table.newEntry();
        entry_from.set("account", from_account);
        entry_from.set("asset_value", int256(from_asset_value - asset_value));
        // 更新转账账户
        int count = table.update(from_account, entry_from, table.newCondition());
        if(count != 1) {
            ret_code = -5;
            // 失败? 无权限或者其他错误?
            emit TransferEvent(ret_code, from_account, to_account, asset_value);
            return ret_code;
        }

        Entry entry_to = table.newEntry();
        entry_to.set("account", to_account);
        entry_to.set("asset_value", int256(to_asset_value + asset_value));
        // 更新接收账户
        table.update(to_account, entry_to, table.newCondition());

        emit TransferEvent(ret_code, from_account, to_account, asset_value);

        return ret_code;
    }


    /*
    描述 : 签发应收账款交易上链（添加交易）
    参数 ： 
            id :       交易编号
            borrower : 借债人
            debtee :   债权人
            transaction_money:     初始交易金额
    返回值：
            0  交易添加成功
            -1 交易已存在
            -2 资产转移错误
            -3 借债人资产额度不够
            -4 其他错误
    */
    function createReceipt(string id, string borrower, string debtee, int256 transaction_money) public returns(int256){
        int256 ret_code = 0;
        int256 ret = 0;
        int256 asset_value = 0;

        int isExist = 0;
        int256[] memory info_list = new int256[](2);
        string[] memory user_list = new string[](2);
        
        // 查询交易是否存在
        (isExist, info_list, user_list) = selectReceipt(id);
        if(isExist != int256(0)) {
            // 打开表
            Table table = openReceiptTable();
            // 查询
            Entry entry_receipt = table.newEntry();
            entry_receipt.set("id", id);
            entry_receipt.set("borrower", borrower);
            entry_receipt.set("debtee", debtee);
            entry_receipt.set("money", int256(transaction_money));
            entry_receipt.set("status", int256(transaction_money));
            
            // 插入
            int count = table.insert(id, entry_receipt);
            if (count == 1) {
                // 查询借债人的资产额度
                (ret, asset_value) = selectAccount(borrower);
                if (asset_value > 1000) {
                    // 将借债人的资产额度转移一部分给债权人
                    ret = transfer(borrower,debtee,transaction_money);
                    // 资产额度转让失败
                    if(ret != 0) {
                       ret_code = -2;
                    } else {
                       ret_code = 0;
                    }
                } else {
                    // 借债人资产额度不够;
                    ret_code = -3;
                }
            } else {
                // 失败? 无权限或者其他错误
                ret_code = -4;
            }
            
        } else {
            // 交易已存在
            ret_code = -1;
        }

        // 如果交易创建失败，将记录清除
        if(ret_code != 0) {
            table.remove(id, table.newCondition());
        }

        emit CreateReceiptEvent(ret_code, id, borrower, debtee, transaction_money);

        return ret_code;
    }

 /*
    描述 ： 应收账款的转让上链（拆分交易）
    参数 ：
            original_id:  需要转让的交易 ID
            new_id:       新创建的交易 ID
            newDebtee:    新创建交易的债权人
            divide_money:        交易拆分的金额
    返回值 :
             0 交易转让成功
            -1 转让的交易不存在
            -2 账户不存在
            -3 转让的金额大于原交易未结算的金额
            -4 资产返还错误
            -5 新交易创建不成功
    */
    function divideReceipt(string original_id, string new_id, string newDebtee, int256 divide_money) public returns(int256) {
        int256 ret_code = 0;
        int256 ret = 0;
        

        int256 isExist = 0;
        int256[] memory info_list = new int256[](2);
        string[] memory user_list = new string[](2);
        // 查询该欠条是否存在
        (isExist, info_list, user_list) = selectReceipt(original_id);

        if(isExist == 0) {
            int temp = 0;
            // 查询账户是否存在
            (ret, temp) = selectAccount(newDebtee);
            if(ret != 0) {
                ret_code = -2;
                emit DivideReceiptEvent(ret_code, original_id, new_id, newDebtee, divide_money);
                return ret_code;
            }

            // 转让的金额大于原交易未结算的金额
            if(divide_money > info_list[1]){    
                ret_code = -3;
                emit DivideReceiptEvent(ret_code, original_id, new_id, newDebtee, divide_money);
                return ret_code;
            }

            // debtee 把 borrower 借的债转让给 newDebtee，debtee 资产额度转移给 newDebtee，分为两步
            // 1. borrower 还债给 debtee 
            ret = repayReceipt(original_id, divide_money);
            if (ret != 0) {
                ret_code = -4;
                emit DivideReceiptEvent(ret_code, original_id, new_id, newDebtee, divide_money);
                return ret_code;
            }

            // 2. borrower 向 newDebtee 借债
            ret = createReceipt(new_id, user_list[0], newDebtee, divide_money);
            if (ret != 0) {
                // 新交易创建不成功
                ret_code = -5;
                emit DivideReceiptEvent(ret_code, original_id, new_id, newDebtee, divide_money);
                return ret_code;
            }

        } else {    // 转让的交易不存在
            ret_code = -1;
        }

        emit DivideReceiptEvent(ret_code, original_id, new_id, newDebtee, divide_money);
        return ret_code;
    }

    /*
    描述 : 利用应收账款向银行融资上链（融资）
    参数 ： 
            id :           融资编号
            financer :     融资人
            finance_money: 金额
    返回值：
             0 融资成功
            -1 融资人不存在
            -2 融资额大于融资人目前的资产额度
            -3 其他错误
    */
    function financeReceipt(string id, string financer , int256 finance_money) public returns(int256) {
        int256 ret_code = 0;
        int256 ret = 0;

        int256 isExist = 0;
        int256 asset_value = 0;

        // 查询融资人是否存在
        (isExist, asset_value) = selectAccount(financer);

        if(isExist == 0) { // 融资人存在
            // 融资人目前的资产额度大于等于融资额
            if (asset_value >= finance_money) {
                // 向银行申请融资
                ret = createReceipt(id, financer, "bank", finance_money);
                if (ret != 0) {
                    ret_code = -3;
                    emit FinanceReceiptEvent(ret_code, id, financer, finance_money);
                    return ret_code;
                }
            } else {
                ret_code = -2;
                emit FinanceReceiptEvent(ret_code, id, financer, finance_money);
                return ret_code;
            }
            
        } else { // 融资人不存在
            ret_code = -1;
        }
        emit FinanceReceiptEvent(ret_code, id, financer, finance_money);
        return ret_code;
    }
    


    /*
    描述 : 应收账款支付结算上链（结算交易）
    参数 ： 
            id : 交易编号
            repay_money: 金额
    返回值：
             0 交易结算成功
            -1 交易不存在
            -2 还债金额大于借款
            -3 其他错误
            -4 资产返还错误
    */
    function repayReceipt(string id, int256 repay_money) public returns(int256){
        int256 ret_code = 0;

        int256 isExist = 0;
        int256[] memory info_list = new int256[](2);
        string[] memory user_list = new string[](2);
        // 查询该交易是否存在
        (isExist, info_list, user_list) = selectReceipt(id);

        if(isExist == 0) { // 交易存在

            // 还款金额大于欠款
            if(repay_money > info_list[1]){
                ret_code = -2;
                emit RepayReceiptEvent(ret_code, id, repay_money);
                return (ret_code);
            }

            // 更新交易状态
            Table table = openReceiptTable();
            Entry entry0 = table.newEntry();
            entry0.set("id", id);
            entry0.set("borrower", user_list[0]);
            entry0.set("debtee", user_list[1]);
            entry0.set("money", info_list[0]);
            // 未结算金额减少
            entry0.set("status", (info_list[1] - repay_money));

            // 更新交易信息
            int count = table.update(id, entry0, table.newCondition());
            if(count != 1) {
                ret_code = -3;
                // 失败? 无权限或者其他错误?
                emit RepayReceiptEvent(ret_code, id, repay_money);
                return (ret_code);
            }

            // 资产返还
            int256 temp = transfer(user_list[1], user_list[0], repay_money);
            if(temp != 0){
                ret_code = -4;
                emit RepayReceiptEvent(ret_code, id, repay_money);
                return (ret_code);
            }

            ret_code = 0;
      
        } else { // 交易不存在
            ret_code = -1;
        }
        emit RepayReceiptEvent(ret_code, id, repay_money);

        return ret_code;
    }

}


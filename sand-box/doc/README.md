## SandBox 说明文档

### 智能合约项目结构

-- root

    -- lib
    -- src
        -- main
            --java
            -- resources
                -- META-INF
                     -- contract.meta.conf (智能合约元信息配置文件)
        --test
        
### 智能合约元信息配置文件

##### 名称
`contract.meta.conf`

##### 内容
```
contract {
  owner = "${account id}"

  name = "${contract name}"

  version = "${contract.version}"

  interfaces {
    # 模板  methodAlias = qualifiedClassName#methodName(Context,arguments*)
    
    registerBanana = "com.fssi.contract.BananaContract#registerBanana(Context,String,double)"
    
    transferBanana = "com.fssi.contract.BananaContract#transferBanana(Context,String,String)"
  }
}
```
#### 说明
* Context类型为智能合约执行的上下文信息，全类名为: `fssi.contract.lib.Context` ，必须以首个参数出现
* 智能合约方法参数只能支持`java`中`int` 、`long` 、`float` 、`double` 、`boolean` 、 `String` 7种类型

### 约束

#### 文件约束
* META-INF 文件夹下面只能存放contract.meta.conf文件

#### 包名约束
* 智能合约的package不能以fssi开头

#### 类型约束
* 不可用使用 `java.util.concurrent` 包下所有类型
* 不可用使用 `java.lang.reflect` 包下所有类型
* 不可用使用 `javax` 包下所有类型
* 不可用使用 `java.net` 包下所有类型
* 不可用使用 `java.sql` 包下所有类型
* 不可用使用 `sun` 包下所有类型
* 不可用使用 `java.lang.Thread` 类
* 不可用使用 `java.lang.Class` 类

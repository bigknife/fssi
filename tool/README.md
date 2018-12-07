# FSSI TOOL

A Command Line Tool to help to use FSSI.

## Usage
After getting a runnable jar by using `sbt packInstallTool`, we can run the jar with different input arguments to get
different outputs which can be used in fssi.

### CreateAccount
```
./tool CreateAccount -s fssi -af /tmp/fssi/account -kf /tmp/fssi/secret
```

### CreateContractProject
```
./tool CreateContractProject -pd /tmp/fssi/contract
```

### CompileContract
```
./tool CompileContract -af /tmp/fssi/account -kf /tmp/fssi/secret -pd /tmp/fssi/contract -of /tmp/fssi/test_contract -sv 1.0.0
```

### CreateTransaction

#### Transfer
```
./tool CreateTransaction transfer -af /tmp/fssi/account -kf /tmp/fssi/secret -pi 18yqbuuBHmp5gc5gwUUVk88JMRDDXbwbJp -t 100Sweet -o /tmp/fssi/transfer
```

#### Deploy
```
./tool CreateTransaction deploy -af /tmp/fssi/account -kf /tmp/fssi/secret -cf /tmp/fssi/test_contract -o /tmp/fssi/deploy
```

#### Run
```
./tool CreateTransaction run -af /tmp/fssi/account -kf /tmp/fssi/secret -name test_contract -version 1.0.0 -m tokenQuery -p [\"1FBMudzfMCWBHYJSxN7fE3GGXKJzErU78Q\"] -o /tmp/fssi/run
```

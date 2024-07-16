# How to generate the dylib

```shell
cd ~
git clone https://github.com/loongs-zhang/ACOCA2024-PlayWithWASM.git
cd ~/ACOCA2024-PlayWithWASM/jvmti
mvn clean package
```

then you will see the dylib file in `~/ACOCA2024-PlayWithWASM/jvmti/target/classes/libJniLibrary.dylib`

If your OS is linux, you will see the so file;

If your OS is windows, you will see the dll file;
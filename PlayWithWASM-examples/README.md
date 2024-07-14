# How to build the wasm file

1. install rustup

2. install rust

3. generate the wasm file

```shell
cd {PlayWithWASM-examples}/src/main/rust-wasm-plugin
cargo build --target wasm32-wasi --release
```

then you will see the wasm file
in `{PlayWithWASM-examples}/src/main/rust-wasm-plugin/target/wasm32-wasi/release/rust_wasm_plugin.wasm`

4. rename the wasm file

rename the file to `org.apache.shenyu.plugin.rust.RustHttpClientPlugin.wasm`
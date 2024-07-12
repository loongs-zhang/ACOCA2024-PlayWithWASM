# How to build the wasm file

1. install rustup

2. install rust

3. generate the wasm file

```shell
cd {PlayWithWASM-examples}/src/main/rust-meta-data-handler
cargo build --target wasm32-wasi --release
```

then you will see the wasm file
in `{PlayWithWASM-examples}/src/main/rust-meta-data-handler/target/wasm32-wasi/release/rust_wasm_discovery_handler_plugin.wasm`

4. rename the wasm file

rename the file to `org.apache.shenyu.plugin.brpc.BrpcMetadataHandler.wasm`
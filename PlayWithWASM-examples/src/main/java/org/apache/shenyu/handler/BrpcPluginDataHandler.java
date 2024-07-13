package org.apache.shenyu.handler;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;
import org.apache.shenyu.plugin.wasm.base.handler.AbstractWasmPluginDataHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author loongs-zhang
 * @date 2024/7/13 17:43
 */
public class BrpcPluginDataHandler extends AbstractWasmPluginDataHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BrpcPluginDataHandler.class);

    @Override
    protected Map<String, Func> initWasmCallJavaFunc(final Store<Void> store) {
        Map<String, Func> funcMap = new HashMap<>();
        funcMap.put("get_args", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
                (argId, addr, len) -> {
                    String config = "hello from java " + argId;
                    LOG.info("java side->" + config);
                    ByteBuffer buf = super.getBuffer();
                    for (int i = 0; i < len && i < config.length(); i++) {
                        buf.put(addr.intValue() + i, (byte) config.charAt(i));
                    }
                    return Math.min(config.length(), len);
                }));
        funcMap.put("put_result", WasmFunctions.wrap(store, WasmValType.I64, WasmValType.I64, WasmValType.I32, WasmValType.I32,
                (argId, addr, len) -> {
                    ByteBuffer buf = super.getBuffer();
                    byte[] bytes = new byte[len];
                    for (int i = 0; i < len; i++) {
                        bytes[i] = buf.get(addr.intValue() + i);
                    }
                    String result = new String(bytes, StandardCharsets.UTF_8);
                    LOG.info("java side->" + result);
                    return 0;
                }));
        return funcMap;
    }

    @Override
    public String pluginNamed() {
        return "brpc";
    }
}

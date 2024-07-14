package org.apache.shenyu.plugin.rust;

import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmValType;
import org.apache.shenyu.plugin.api.ShenyuPluginChain;
import org.apache.shenyu.plugin.wasm.api.AbstractWasmPlugin;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author loongs-zhang
 * @date 2024/7/13 17:28
 */
public class RustHttpClientPlugin extends AbstractWasmPlugin {

    private static final Map<Long, String> RESULTS = new ConcurrentHashMap<>();

    @Override
    protected Mono<Void> doExecute(final ServerWebExchange exchange, final ShenyuPluginChain chain, final Long argumentId) {
        final String result = RESULTS.get(argumentId);
        return chain.execute(exchange);
    }

    @Override
    protected Long getArgumentId(final ServerWebExchange exchange, final ShenyuPluginChain chain) {
        UUID uuid = UUID.randomUUID();
        return uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
    }

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
                    RESULTS.put(argId, result);
                    LOG.info("java side->" + result);
                    return 0;
                }));
        return funcMap;
    }

    @Override
    public int getOrder() {
        return 210;
    }

    @Override
    public String named() {
        return "rustHttpClient";
    }
}

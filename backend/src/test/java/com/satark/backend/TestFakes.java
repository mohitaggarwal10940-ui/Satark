package com.satark.backend;

import com.satark.backend.model.Analysis;
import com.satark.backend.repository.AnalysisRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Phase 14 test helper: in-memory {@link AnalysisRepository} double.
 * Assigns an ID on {@code save} and captures the entity — no MongoDB,
 * no Mockito needed.
 */
public final class TestFakes {

    private TestFakes() {
    }

    public static AnalysisRepository savingRepository(AtomicReference<Analysis> saved) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if (method.getName().equals("save") && args != null && args.length == 1) {
                Analysis a = (Analysis) args[0];
                if (a.getId() == null) {
                    a.setId(UUID.randomUUID().toString());
                }
                saved.set(a);
                return a;
            }
            throw new UnsupportedOperationException(method.getName());
        };
        return (AnalysisRepository) Proxy.newProxyInstance(
                TestFakes.class.getClassLoader(), new Class<?>[] {AnalysisRepository.class}, handler);
    }
}

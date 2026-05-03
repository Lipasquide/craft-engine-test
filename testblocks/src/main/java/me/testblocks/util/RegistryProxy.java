package me.testblocks.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;

@ReflectionProxy(name = "net.minecraft.core.Registry")
public interface RegistryProxy {
    RegistryProxy INSTANCE = ASMProxyFactory.create(RegistryProxy.class);

    @MethodInvoker(name = "register")
    Object register(Object registry, Object id, Object entry);
}

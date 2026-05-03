package me.testblocks.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;

@ReflectionProxy(name = "net.minecraft.core.registries.BuiltInRegistries")
public interface BuiltInRegistriesProxy {
    BuiltInRegistriesProxy INSTANCE = ASMProxyFactory.create(BuiltInRegistriesProxy.class);

    @MethodInvoker(name = "BLOCK", isStatic = true)
    Object BLOCK();
}

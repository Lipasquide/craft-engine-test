package me.testblocks.util;

import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;

@ReflectionProxy(name = "net.minecraft.resources.ResourceLocation")
public interface ResourceLocationProxy {
    ResourceLocationProxy INSTANCE = ASMProxyFactory.create(ResourceLocationProxy.class);

    @MethodInvoker(name = "parse", isStatic = true)
    Object parse(String path);
}

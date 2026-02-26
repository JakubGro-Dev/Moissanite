package com.crussion.moissanite.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

@Mixin(RenderType.class)
public interface RenderTypeAccessor {
	@Accessor("state")
	RenderSetup moissanite$getState();

	@Invoker("create")
	static RenderType moissanite$invokeCreate(String name, RenderSetup setup) {
		throw new AssertionError("Mixin invoker not transformed");
	}
}

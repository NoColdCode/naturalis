package dev.naturalis.client;



import dev.naturalis.client.screen.MorphQuickSlotAssignScreen;

import dev.naturalis.morph.quickslot.MorphQuickSlotClientActions;

import net.minecraft.client.Minecraft;

import net.minecraft.client.gui.screens.Screen;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.entity.EntityType;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;



import java.lang.reflect.Field;

import java.lang.reflect.Method;



/**

 * Shift-right-click assign hook for Remorphed shape widgets.

 * Kept outside the mixin to avoid synthetic accessor crashes on pseudo-mixins.

 */

public final class MorphQuickSlotAssignSupport {



    private MorphQuickSlotAssignSupport() {

    }



    public static void handleShiftRightClick(Object widget, int button, CallbackInfoReturnable<Boolean> cir) {

        if (button != 1 || !Screen.hasShiftDown()) {

            return;

        }



        ResourceLocation morphId = resolveMorphId(widget);

        if (morphId == null) {

            return;

        }



        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null) {

            return;

        }



        MorphQuickSlotClientActions.requestResync();

        minecraft.execute(() -> minecraft.setScreen(new MorphQuickSlotAssignScreen(morphId)));

        cir.setReturnValue(true);

    }



    public static ResourceLocation resolveMorphId(Object widget) {

        Object entity = readField(widget, "entity");

        if (entity instanceof LivingEntity living) {

            return BuiltInRegistries.ENTITY_TYPE.getKey(living.getType());

        }



        ResourceLocation fromType = resolveShapeType(readField(widget, "type"));

        if (fromType != null) {

            return fromType;

        }



        return resolveShapeType(invokeNoArg(widget, "getType", "getShapeType"));

    }



    private static ResourceLocation resolveShapeType(Object shapeType) {

        if (shapeType == null) {

            return null;

        }



        Object entityType = invokeNoArg(shapeType, "getEntityType");

        if (entityType instanceof EntityType<?> type) {

            return BuiltInRegistries.ENTITY_TYPE.getKey(type);

        }



        if (shapeType instanceof EntityType<?> type) {

            return BuiltInRegistries.ENTITY_TYPE.getKey(type);

        }



        return null;

    }



    private static Object invokeNoArg(Object target, String... methodNames) {

        if (target == null) {

            return null;

        }

        for (String methodName : methodNames) {

            try {

                Method method = target.getClass().getMethod(methodName);

                return method.invoke(target);

            } catch (ReflectiveOperationException ignored) {

                // Try next name.

            }

        }

        return null;

    }



    private static Object readField(Object target, String fieldName) {

        for (Class<?> type = target.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {

            try {

                Field field = type.getDeclaredField(fieldName);

                field.setAccessible(true);

                return field.get(target);

            } catch (ReflectiveOperationException ignored) {

                // Try superclass.

            }

        }

        return null;

    }

}



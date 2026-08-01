package dev.naturalis.client;



import com.mojang.blaze3d.buffers.GpuBuffer;

import com.mojang.blaze3d.buffers.Std140Builder;

import com.mojang.blaze3d.opengl.GlBuffer;

import net.minecraft.client.renderer.PostChain;

import net.minecraft.client.renderer.PostPass;



import java.lang.reflect.Field;

import java.lang.reflect.Method;

import java.nio.ByteBuffer;

import java.util.List;

import java.util.Map;



/**

 * Writes morph vision parameters into 1.21.x post-chain {@code VisionPaletteConfig} GPU buffers.

 */

public final class MorphPostEffectUniformWriter {



    private static final String UBO_NAME = "VisionPaletteConfig";



    private MorphPostEffectUniformWriter() {

    }



    public static boolean upload(

        Object postChain,

        MorphVisionPaletteDefaults.Palette palette,

        float chromaticMode,

        float photoStress,

        float kaleidoStrength,

        float kaleidoFoldCount,

        float spectralProfile,

        float motionTrail,

        float motionUx,

        float motionUz,

        float strengthBoost

    ) {

        if (postChain == null || palette == null) {

            return false;

        }



        GpuBuffer buffer = findPaletteBuffer(postChain);

        if (buffer == null) {

            return false;

        }



        ByteBuffer target = resolveWritableBuffer(buffer);

        if (target == null) {

            return false;

        }



        if (target.capacity() < 144) {
            return false;
        }



        target.clear();

        Std140Builder builder = Std140Builder.intoBuffer(target);

        builder.putVec3(palette.axisA()[0], palette.axisA()[1], palette.axisA()[2]);

        builder.putVec3(palette.axisB()[0], palette.axisB()[1], palette.axisB()[2]);

        builder.putVec3(palette.axisC()[0], palette.axisC()[1], palette.axisC()[2]);

        builder.putVec3(palette.colorA()[0], palette.colorA()[1], palette.colorA()[2]);

        builder.putVec3(palette.colorB()[0], palette.colorB()[1], palette.colorB()[2]);

        builder.putVec3(palette.colorC()[0], palette.colorC()[1], palette.colorC()[2]);

        builder.putFloat(palette.strength() * strengthBoost);

        builder.putFloat(palette.shadowLift());

        builder.putFloat(palette.lumaPreserve());

        builder.putFloat(chromaticMode);

        builder.putFloat(photoStress);

        builder.putFloat(kaleidoStrength);

        builder.putFloat(kaleidoFoldCount);

        builder.putFloat(spectralProfile);

        builder.putFloat(motionTrail);

        builder.putFloat(motionUx);

        builder.putFloat(motionUz);

        target.position(0);

        flushBuffer(buffer);

        return true;

    }



    private static void flushBuffer(GpuBuffer buffer) {

        for (String method : new String[] {"upload", "flush", "markDirty", "sync"}) {

            try {

                Method m = buffer.getClass().getMethod(method);

                m.invoke(buffer);

                return;

            } catch (ReflectiveOperationException ignored) {

            }

        }

    }



    @SuppressWarnings("unchecked")

    private static GpuBuffer findPaletteBuffer(Object postChain) {

        if (postChain instanceof PostChain chain) {

            return findPaletteBufferInPasses(readPasses(chain));

        }

        return findPaletteBufferInPasses(readPassesReflect(postChain));

    }



    private static List<PostPass> readPasses(PostChain chain) {

        try {

            Field f = PostChain.class.getDeclaredField("passes");

            f.setAccessible(true);

            return (List<PostPass>) f.get(chain);

        } catch (ReflectiveOperationException ignored) {

            return List.of();

        }

    }



    @SuppressWarnings("unchecked")

    private static List<PostPass> readPassesReflect(Object postChain) {

        try {

            Field f = postChain.getClass().getDeclaredField("passes");

            f.setAccessible(true);

            Object raw = f.get(postChain);

            if (raw instanceof List<?> list) {

                return (List<PostPass>) list;

            }

        } catch (ReflectiveOperationException ignored) {

        }

        return List.of();

    }



    private static GpuBuffer findPaletteBufferInPasses(List<PostPass> passes) {

        for (PostPass pass : passes) {

            GpuBuffer buffer = readCustomUniform(pass, UBO_NAME);

            if (buffer != null) {

                return buffer;

            }

        }

        return null;

    }



    @SuppressWarnings("unchecked")

    private static GpuBuffer readCustomUniform(PostPass pass, String blockName) {

        try {

            Field f = PostPass.class.getDeclaredField("customUniforms");

            f.setAccessible(true);

            Object raw = f.get(pass);

            if (raw instanceof Map<?, ?> map) {

                Object buffer = map.get(blockName);

                if (buffer instanceof GpuBuffer gpu) {

                    return gpu;

                }

            }

        } catch (ReflectiveOperationException ignored) {

        }

        return null;

    }



    private static ByteBuffer resolveWritableBuffer(GpuBuffer buffer) {

        if (buffer instanceof GlBuffer glBuffer) {

            for (String fieldName : new String[] {"persistentBuffer", "buffer", "data"}) {

                try {

                    Field f = GlBuffer.class.getDeclaredField(fieldName);

                    f.setAccessible(true);

                    Object raw = f.get(glBuffer);

                    if (raw instanceof ByteBuffer bb) {

                        return bb;

                    }

                } catch (ReflectiveOperationException ignored) {

                }

            }

        }

        try {

            Method map = buffer.getClass().getMethod("map", int.class, int.class);

            Object mapped = map.invoke(buffer, 0, 256);

            if (mapped != null) {

                for (String fieldName : new String[] {"data", "buffer"}) {

                    try {

                        Field f = mapped.getClass().getDeclaredField(fieldName);

                        f.setAccessible(true);

                        Object raw = f.get(mapped);

                        if (raw instanceof ByteBuffer bb) {

                            return bb;

                        }

                    } catch (ReflectiveOperationException ignored) {

                    }

                }

            }

        } catch (ReflectiveOperationException ignored) {

        }

        return null;

    }

}



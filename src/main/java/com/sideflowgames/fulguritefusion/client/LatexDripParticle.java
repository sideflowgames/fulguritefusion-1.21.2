package com.sideflowgames.fulguritefusion.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

// A drop of latex, using the vanilla drip sprites tinted latex-white: it swells on the spout for a moment, falls
// straight down (no drift), and splats briefly where it lands.
public class LatexDripParticle extends net.minecraft.client.particle.SingleQuadParticle {
    private final SpriteSet sprites;
    private final int hangTicks;
    private boolean landed;

    LatexDripParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z, sprites.get(0, 2));
        this.sprites = sprites;
        this.hangTicks = 10 + random.nextInt(10);
        this.lifetime = 200;
        this.gravity = 0;
        this.xd = this.yd = this.zd = 0;
        this.quadSize = 0.07f;
        setSize(0.01f, 0.01f);
        setColor(0.93f, 0.91f, 0.84f);
        setSprite(sprites.get(0, 2));
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        if (landed) return;
        if (age < hangTicks) return;
        if (age == hangTicks) setSprite(sprites.get(1, 2));
        yd -= 0.04;
        yd *= 0.98;
        xd = zd = 0;
        move(0, yd, 0);
        if (onGround) {
            landed = true;
            setSprite(sprites.get(2, 2));
            lifetime = age + 6;
            yd = 0;
        }
    }

    @Override
    protected Layer getLayer() {
        return Layer.OPAQUE;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz, net.minecraft.util.RandomSource random) {
            return new LatexDripParticle(level, x, y, z, sprites);
        }
    }
}

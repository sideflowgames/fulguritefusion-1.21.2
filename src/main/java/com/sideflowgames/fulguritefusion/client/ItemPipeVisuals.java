package com.sideflowgames.fulguritefusion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.sideflowgames.fulguritefusion.transport.PipeTravel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

// Draws items sliding through Item Pipes along the routes the server reports (transport/PipeTravel). Drawn before
// the pipes' glass, so the items show through the windows.
public final class ItemPipeVisuals {
    private ItemPipeVisuals() {}

    private record Trip(List<Vec3> points, double length, ItemStack item, double speed, long start) {}

    private static final List<Trip> TRIPS = new ArrayList<>();
    private static final int MAX_TRIPS = 400;

    public static void init() {
        PipeTravel.clientHandler = ItemPipeVisuals::add;
        NeoForge.EVENT_BUS.addListener(ItemPipeVisuals::render);
    }

    private static void add(PipeTravel.Payload p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || p.path().isEmpty() || p.item().isEmpty()) return;
        Direction from = Direction.from3DDataValue(p.fromSide()), to = Direction.from3DDataValue(p.toSide());
        List<Vec3> pts = new ArrayList<>();
        BlockPos first = p.path().get(0), last = p.path().get(p.path().size() - 1);
        pts.add(Vec3.atCenterOf(first).add(Vec3.atLowerCornerOf(from.getUnitVec3i()).scale(0.5)));
        for (BlockPos b : p.path()) pts.add(Vec3.atCenterOf(b));
        pts.add(Vec3.atCenterOf(last).add(Vec3.atLowerCornerOf(to.getUnitVec3i()).scale(0.5)));
        double len = 0;
        for (int i = 1; i < pts.size(); i++) len += pts.get(i).distanceTo(pts.get(i - 1));
        double speed = p.tier() == 3 ? 0.8 : p.tier() == 2 ? 0.45 : 0.2; // blocks a tick
        TRIPS.add(new Trip(pts, len, p.item(), speed, mc.level.getGameTime()));
        if (TRIPS.size() > MAX_TRIPS) TRIPS.remove(0);
    }

    private static Vec3 at(Trip t, double d) {
        List<Vec3> pts = t.points();
        for (int i = 1; i < pts.size(); i++) {
            double seg = pts.get(i).distanceTo(pts.get(i - 1));
            if (d <= seg || i == pts.size() - 1) return pts.get(i - 1).lerp(pts.get(i), seg <= 0 ? 1 : Math.min(1, d / seg));
            d -= seg;
        }
        return pts.get(pts.size() - 1);
    }

    private static void render(RenderLevelStageEvent.AfterOpaqueFeatures event) {
        if (TRIPS.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        double now = mc.level.getGameTime() + partial;
        Vec3 cam = event.getLevelRenderState().cameraRenderState.pos;
        PoseStack ps = event.getPoseStack();
        var features = mc.gameRenderer.getFeatureRenderDispatcher();
        Iterator<Trip> it = TRIPS.iterator();
        while (it.hasNext()) {
            Trip t = it.next();
            double d = (now - t.start()) * t.speed();
            if (d > t.length()) {
                it.remove();
                continue;
            }
            Vec3 p = at(t, d);
            if (p.distanceToSqr(cam) > 64 * 64) continue;
            ps.pushPose();
            ps.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
            ps.mulPose(Axis.YP.rotationDegrees((float) (now * 6 % 360)));
            ps.scale(0.28f, 0.28f, 0.28f);
            int light = LevelRenderer.getLightCoords(mc.level, BlockPos.containing(p));
            net.minecraft.client.renderer.item.ItemStackRenderState item = new net.minecraft.client.renderer.item.ItemStackRenderState();
            mc.getItemModelResolver().updateForTopItem(item, t.item(), ItemDisplayContext.FIXED, mc.level, null, 0);
            item.submit(ps, features.getSubmitNodeStorage(), light, OverlayTexture.NO_OVERLAY, 0);
            ps.popPose();
        }
        features.renderAllFeatures();
    }
}

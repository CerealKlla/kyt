package com.github.cerealklla.kyt.structure.client;

import com.github.cerealklla.kyt.structure.KytConfigurationEntity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.armorstand.ArmorStandModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * Renders a {@link KytConfigurationEntity} using vanilla's own armor-stand model/texture, so it
 * visually matches a real {@code ArmorStand} without this entity actually being one (see that
 * class's own doc for why). Not a subclass of vanilla's {@code ArmorStandRenderer} -- that class is
 * hard-tied to {@code LivingEntityRenderer<ArmorStand, ArmorStandRenderState, ...>} and reads real
 * {@code ArmorStand}-only fields (pose arrays, marker/small flags, "last hit" wiggle timer) directly
 * off the entity. Instead this builds the same {@code ArmorStandModel} directly from {@link
 * ModelLayers#ARMOR_STAND} and drives it with a fixed default {@link ArmorStandRenderState} (not a
 * custom render-state class -- reusing vanilla's own, since {@code ArmorStandModel#setupAnim}
 * already takes exactly that type and its fields default to vanilla's own neutral pose constants).
 */
public class KytConfigurationRenderer extends EntityRenderer<KytConfigurationEntity, ArmorStandRenderState> {

    private static final Identifier SKIN_LOCATION = Identifier.withDefaultNamespace("textures/entity/armorstand/armorstand.png");

    private final ArmorStandModel model;

    public KytConfigurationRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new ArmorStandModel(context.bakeLayer(ModelLayers.ARMOR_STAND));
    }

    @Override
    public ArmorStandRenderState createRenderState() {
        return new ArmorStandRenderState();
    }

    @Override
    public void extractRenderState(KytConfigurationEntity entity, ArmorStandRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.yRot = Mth.rotLerp(partialTicks, entity.yRotO, entity.getYRot());
        state.isMarker = false;
        state.isSmall = false;
        state.showArms = false;
        state.showBasePlate = true;
        state.headPose = ArmorStand.DEFAULT_HEAD_POSE;
        state.bodyPose = ArmorStand.DEFAULT_BODY_POSE;
        state.leftArmPose = ArmorStand.DEFAULT_LEFT_ARM_POSE;
        state.rightArmPose = ArmorStand.DEFAULT_RIGHT_ARM_POSE;
        state.leftLegPose = ArmorStand.DEFAULT_LEFT_LEG_POSE;
        state.rightLegPose = ArmorStand.DEFAULT_RIGHT_LEG_POSE;
        state.wiggle = 10.0F; // > 5 -- no "just hit" wiggle animation, see ArmorStandModel#setupAnim's caller.
    }

    @Override
    public void submit(ArmorStandRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - state.yRot));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);

        RenderType renderType = model.renderType(SKIN_LOCATION);
        submitNodeCollector.submitModel(model, state, poseStack, renderType, state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor, null);

        poseStack.popPose();
    }

    public Identifier getTextureLocation(ArmorStandRenderState state) {
        return SKIN_LOCATION;
    }
}

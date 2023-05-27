package io.github.mattidragon.extendeddrawers.client.renderer;

import io.github.mattidragon.extendeddrawers.config.ExtendedDrawersConfig;
import io.github.mattidragon.extendeddrawers.config.old.ClientConfig;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnstableApiUsage")
public abstract class AbstractDrawerBlockEntityRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {
    private static final Quaternionf ITEM_LIGHT_ROTATION_3D = RotationAxis.POSITIVE_X.rotationDegrees(-15).mul(RotationAxis.POSITIVE_Y.rotationDegrees(15));
    private static final Quaternionf ITEM_LIGHT_ROTATION_FLAT = RotationAxis.POSITIVE_X.rotationDegrees(-45);

    private final ItemRenderer itemRenderer;
    private final TextRenderer textRenderer;

    public AbstractDrawerBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
        this.itemRenderer = context.getItemRenderer();
        this.textRenderer = context.getTextRenderer();
    }
    
    public void renderSlot(ItemVariant item, @Nullable Long amount, boolean small, List<Sprite> icons, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay, int seed, BlockPos pos, World world) {
        var player = MinecraftClient.getInstance().player;
        var playerPos = player == null ? Vec3d.ofCenter(pos) : player.getPos();
        var config = ExtendedDrawersConfig.get().client();
    
        if (pos.isWithinDistance(playerPos, config.textRenderDistance()) && amount != null)
            renderText(amount, small, light, matrices, vertexConsumers);
        if (pos.isWithinDistance(playerPos, config.iconRenderDistance()))
            renderIcons(icons, small, light, matrices, vertexConsumers, overlay);
        if (pos.isWithinDistance(playerPos, config.itemRenderDistance()))
            renderItem(item, small, light, matrices, vertexConsumers, world, seed);
    }
    
    protected final boolean shouldRender(T drawer, Direction facing) {
        var world = drawer.getWorld();
        if (world == null) return false;
        var pos = drawer.getPos();
        var state = drawer.getCachedState();
        
        return Block.shouldDrawSide(state, world, pos, facing, pos.offset(facing));
    }
    
    protected void renderIcons(List<Sprite> icons, boolean small, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int overlay) {
        var increment = 1.0 / (icons.size() + 1.0);
        matrices.push();
        if (small) matrices.scale(0.5f, 0.5f, 1);
        matrices.translate(-0.5, 0, 0);

        for (var icon : icons) {
            matrices.translate(increment, 0, 0);
            renderIcon(icon, light, matrices, vertexConsumers, overlay);
        }
        matrices.pop();
    }

    private void renderIcon(Sprite sprite, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int overlay) {
        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
        matrices.translate(-0.125, -0.24, -0.5);
        matrices.scale(0.25f, 0.25f, 0.25f);
        var emitter = Objects.requireNonNull(RendererAccess.INSTANCE.getRenderer()).meshBuilder().getEmitter();
        emitter.square(Direction.UP, 0, 0, 1, 1, 0);
        emitter.spriteBake(sprite, MutableQuadView.BAKE_LOCK_UV);
        vertexConsumers.getBuffer(RenderLayer.getCutout()).quad(matrices.peek(), emitter.toBakedQuad(sprite), 1, 1, 1, light, overlay);
        matrices.pop();
    }

    protected void renderItem(ItemVariant item, boolean small, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers, World world, int seed) {
        if (item.isBlank()) return;
        var itemScale = ExtendedDrawersConfig.get().client().itemScale(small);

        matrices.push();
        matrices.scale(itemScale, itemScale, 1);
        matrices.scale(0.75f, 0.75f, 1);
        matrices.multiplyPositionMatrix(new Matrix4f().scale(1, 1, 0.01f));

        var stack = item.toStack();
        var model = itemRenderer.getModel(stack, world, null, seed);

        // Stolen from storage drawers
        if (model.isSideLit()) {
            matrices.peek().getNormalMatrix().mul(new Matrix3f().set(ITEM_LIGHT_ROTATION_3D));
        } else {
            matrices.peek().getNormalMatrix().mul(new Matrix3f().set(ITEM_LIGHT_ROTATION_FLAT));
        }

        itemRenderer.renderItem(stack, ModelTransformationMode.GUI, false, matrices, vertexConsumers, light, OverlayTexture.DEFAULT_UV, model);

        matrices.pop();
    }
    
    protected void renderText(long amount, boolean small, int light, MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        var config = ExtendedDrawersConfig.get().client();

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));
        if (small) {
            matrices.translate(0, 0.25, -0.01);
        } else {
            matrices.translate(0, 0.5, -0.01);
        }
        matrices.scale(config.textScale(small), config.textScale(small), 1);
        matrices.translate(0, -config.textOffset(), -0.01);

        matrices.scale(0.02f, 0.02f, 0.02f);
        var text = Long.toString(amount);
        textRenderer.draw(text, -textRenderer.getWidth(text) / 2f, 0, 0xffffff, false, matrices.peek().getPositionMatrix(), vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0x000000, light);
        matrices.pop();
    }

    protected void alignMatrices(MatrixStack matrices, Direction dir) {
        var pos = dir.getUnitVector();
        matrices.translate(pos.x / 2 + 0.5, pos.y / 2 + 0.5, pos.z / 2 + 0.5);
        matrices.multiply(dir.getRotationQuaternion());
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90));
        matrices.translate(0, 0, 0.01);
    }
}

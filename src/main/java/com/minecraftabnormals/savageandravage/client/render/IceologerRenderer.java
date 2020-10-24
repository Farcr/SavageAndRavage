package com.minecraftabnormals.savageandravage.client.render;

import com.minecraftabnormals.savageandravage.client.model.IceologerModel;
import com.minecraftabnormals.savageandravage.common.entity.IceologerEntity;
import com.minecraftabnormals.savageandravage.core.SavageAndRavage;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.HeadLayer;
import net.minecraft.util.ResourceLocation;

/**
 * @author Ocelot
 */
public class IceologerRenderer extends MobRenderer<IceologerEntity, IceologerModel>
{
    private static final ResourceLocation TEXTURE_LOCATION = new ResourceLocation(SavageAndRavage.MODID, "textures/entity/iceologer.png");

    public IceologerRenderer(EntityRendererManager renderManagerIn)
    {
        super(renderManagerIn, new IceologerModel(), 0.5F);
        this.addLayer(new HeadLayer<>(this));
    }

    @Override
    public ResourceLocation getEntityTexture(IceologerEntity entity)
    {
        return TEXTURE_LOCATION;
    }
}
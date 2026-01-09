package doom.module.impl.render;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelDragonWings extends ModelBase {
    // Główne
    public ModelRenderer wings;
    public ModelRenderer main;
    public ModelRenderer wings2;
    public ModelRenderer main3;

    // Części składowe
    public ModelRenderer texture1_r1, txt2_r1, cube_r1, bone, texture2_r1, texture5_r1, cube_r2, txt7_r1;
    public ModelRenderer bone4, cube_r3, cube_r4, cube_r5;
    public ModelRenderer bone5, cube_r6, cube_r7, cube_r8;
    public ModelRenderer bone2, texture3_r1, bone6, cube_r9, cube_r10, cube_r11, bone3, cube_r12, cube_r13, texture4_r1;

    public ModelRenderer texture2_r2, txt3_r1, cube_r14, bone13, texture3_r2, texture6_r1, cube_r15, txt8_r1;
    public ModelRenderer bone14, cube_r16, cube_r17, cube_r18;
    public ModelRenderer bone15, cube_r19, cube_r20, cube_r21;
    public ModelRenderer bone16, texture4_r2, bone17, cube_r22, cube_r23, cube_r24, bone18, cube_r25, cube_r26, texture5_r2;

    public ModelDragonWings() {
        this.textureWidth = 64;
        this.textureHeight = 64;

        // ================= LEWE SKRZYDŁO =================
        wings = new ModelRenderer(this);
        wings.setRotationPoint(-2.85F, 5.0F, 2.0F);
        setRotationAngle(wings, 0.0873F, 0.0F, 0.0F);

        main = new ModelRenderer(this);
        main.setRotationPoint(1.0F, -1.0F, 0.0F);
        wings.addChild(main);
        setRotationAngle(main, 0.0F, -0.5672F, 0.0F);

        texture1_r1 = new ModelRenderer(this, 0, 7);
        texture1_r1.setRotationPoint(-4.75F, -0.25F, 0.0F);
        main.addChild(texture1_r1);
        setRotationAngle(texture1_r1, 0.0F, 0.0F, -1.5272F);
        texture1_r1.addBox(-10.0F, -1.25F, 0.0F, 11, 6, 0); // Flat

        txt2_r1 = new ModelRenderer(this, 22, 11);
        txt2_r1.setRotationPoint(-2.8511F, -3.1595F, 0.0F);
        main.addChild(txt2_r1);
        setRotationAngle(txt2_r1, 0.0F, 0.0F, 0.5236F);
        txt2_r1.addBox(-2.0F, -1.5F, 0.0F, 7, 3, 0); // Flat

        cube_r1 = new ModelRenderer(this, 22, 7);
        cube_r1.setRotationPoint(-1.0F, 1.0F, 0.0F);
        main.addChild(cube_r1);
        setRotationAngle(cube_r1, 0.0F, 0.0F, 0.5236F);
        cube_r1.addBox(-5.0F, -2.0F, -1.0F, 6, 2, 2); // 3D!

        bone = new ModelRenderer(this);
        bone.setRotationPoint(-5.25F, -1.0F, 0.0F);
        main.addChild(bone);
        setRotationAngle(bone, 0.0F, -0.5236F, 0.0F);

        texture2_r1 = new ModelRenderer(this, 0, 0);
        texture2_r1.setRotationPoint(-4.75F, 0.25F, 0.0F);
        bone.addChild(texture2_r1);
        setRotationAngle(texture2_r1, 0.0F, 0.0F, -1.5272F);
        texture2_r1.addBox(-10.0F, -1.25F, 0.0F, 11, 7, 0); // Flat

        texture5_r1 = new ModelRenderer(this, 22, 0);
        texture5_r1.setRotationPoint(-5.0F, -0.5F, 0.0F);
        bone.addChild(texture5_r1);
        setRotationAngle(texture5_r1, 0.0F, 0.0F, -1.5272F);
        texture5_r1.addBox(-10.0F, -1.25F, 0.0F, 11, 4, 0); // Flat

        cube_r2 = new ModelRenderer(this, 22, 4);
        cube_r2.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone.addChild(cube_r2);
        setRotationAngle(cube_r2, 0.0F, 0.0F, 0.0873F);
        cube_r2.addBox(-7.0F, -2.25F, -0.25F, 8, 2, 1); // 3D!

        txt7_r1 = new ModelRenderer(this, 22, 14);
        txt7_r1.setRotationPoint(-4.3511F, -3.4095F, 0.0F);
        bone.addChild(txt7_r1);
        setRotationAngle(txt7_r1, 0.0F, 0.0F, 0.0873F);
        txt7_r1.addBox(-2.0F, -1.5F, 0.0F, 7, 3, 0); // Flat

        bone4 = new ModelRenderer(this);
        bone4.setRotationPoint(0.5F, -0.5F, 0.0F);
        bone.addChild(bone4);

        cube_r3 = new ModelRenderer(this, 20, 25);
        cube_r3.setRotationPoint(0.0F, 8.0F, 0.0F);
        bone4.addChild(cube_r3);
        setRotationAngle(cube_r3, 0.0F, 0.0F, -2.0071F);
        cube_r3.addBox(-3.0F, -1.25F, -0.5F, 4, 1, 1); // 3D

        cube_r4 = new ModelRenderer(this, 10, 24);
        cube_r4.setRotationPoint(-0.75F, 4.5F, 0.0F);
        bone4.addChild(cube_r4);
        setRotationAngle(cube_r4, 0.0F, 0.0F, -1.7017F);
        cube_r4.addBox(-3.0F, -1.25F, -0.5F, 4, 1, 1); // 3D

        cube_r5 = new ModelRenderer(this, 22, 17);
        cube_r5.setRotationPoint(-0.25F, 1.0F, 0.0F);
        bone4.addChild(cube_r5);
        setRotationAngle(cube_r5, 0.0F, 0.0F, -1.309F);
        cube_r5.addBox(-3.0F, -1.25F, -0.5F, 4, 1, 1); // 3D

        bone5 = new ModelRenderer(this);
        bone5.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone.addChild(bone5);

        cube_r6 = new ModelRenderer(this, 22, 23);
        cube_r6.setRotationPoint(-5.5F, 8.0F, 0.0F);
        bone5.addChild(cube_r6);
        setRotationAngle(cube_r6, 0.0F, 0.0F, -1.1345F);
        cube_r6.addBox(-2.25F, -1.0F, -0.5F, 4, 1, 1); // 3D

        cube_r7 = new ModelRenderer(this, 22, 21);
        cube_r7.setRotationPoint(-4.0F, 3.75F, 0.0F);
        bone5.addChild(cube_r7);
        setRotationAngle(cube_r7, 0.0F, 0.0F, -1.3526F);
        cube_r7.addBox(-2.25F, -1.0F, -0.5F, 4, 1, 1); // 3D

        cube_r8 = new ModelRenderer(this, 22, 19);
        cube_r8.setRotationPoint(-2.0F, 0.5F, 0.0F);
        bone5.addChild(cube_r8);
        setRotationAngle(cube_r8, 0.0F, 0.0F, -0.7418F);
        cube_r8.addBox(-2.25F, -1.25F, -0.5F, 4, 1, 1); // 3D

        bone2 = new ModelRenderer(this);
        bone2.setRotationPoint(-6.5F, -1.0F, 0.0F);
        bone.addChild(bone2);
        setRotationAngle(bone2, 0.0F, -0.48F, 0.0F);

        texture3_r1 = new ModelRenderer(this, 0, 19);
        texture3_r1.setRotationPoint(-3.5F, 0.5F, 0.0F);
        bone2.addChild(texture3_r1);
        setRotationAngle(texture3_r1, 0.0F, 0.0F, -1.5272F);
        texture3_r1.addBox(-10.0F, -1.25F, 0.0F, 11, 5, 0); // Flat

        bone6 = new ModelRenderer(this);
        bone6.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone2.addChild(bone6);

        cube_r9 = new ModelRenderer(this, 10, 26);
        cube_r9.setRotationPoint(-3.75F, 5.25F, 0.0F);
        bone6.addChild(cube_r9);
        setRotationAngle(cube_r9, 0.0F, 0.0F, -0.8727F);
        cube_r9.addBox(-1.5F, -1.0F, -0.5F, 4, 1, 1); // 3D

        cube_r10 = new ModelRenderer(this, 0, 28);
        cube_r10.setRotationPoint(-2.25F, 3.5F, 0.0F);
        bone6.addChild(cube_r10);
        setRotationAngle(cube_r10, 0.0F, 0.0F, -1.2217F);
        cube_r10.addBox(-0.5F, -1.0F, -0.5F, 3, 1, 1); // 3D

        cube_r11 = new ModelRenderer(this, 20, 27);
        cube_r11.setRotationPoint(-1.25F, 1.5F, 0.0F);
        bone6.addChild(cube_r11);
        setRotationAngle(cube_r11, 0.0F, 0.0F, -0.7418F);
        cube_r11.addBox(-0.5F, -1.25F, -0.5F, 3, 1, 1); // 3D

        bone3 = new ModelRenderer(this);
        bone3.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone2.addChild(bone3);

        cube_r12 = new ModelRenderer(this, 0, 26);
        cube_r12.setRotationPoint(-4.5F, -1.25F, 0.0F);
        bone3.addChild(cube_r12);
        setRotationAngle(cube_r12, 0.0F, 0.0F, 0.0873F);
        cube_r12.addBox(-3.0F, -1.5F, -0.5F, 4, 1, 1); // 3D

        cube_r13 = new ModelRenderer(this, 0, 24);
        cube_r13.setRotationPoint(-1.0F, -0.25F, 0.0F);
        bone3.addChild(cube_r13);
        setRotationAngle(cube_r13, 0.0F, 0.0F, 0.3054F);
        cube_r13.addBox(-3.0F, -1.75F, -0.5F, 4, 1, 1); // 3D

        texture4_r1 = new ModelRenderer(this, 0, 13);
        texture4_r1.setRotationPoint(-5.75F, -1.75F, 0.0F);
        bone3.addChild(texture4_r1);
        setRotationAngle(texture4_r1, 0.0F, 0.0F, -1.5272F);
        texture4_r1.addBox(-10.0F, -1.25F, 0.0F, 11, 7, 0); // Flat

        // ================= PRAWE SKRZYDŁO =================
        wings2 = new ModelRenderer(this);
        wings2.setRotationPoint(2.85F, 5.0F, 2.0F);
        setRotationAngle(wings2, 0.0873F, 0.0F, 0.0F);

        main3 = new ModelRenderer(this);
        main3.setRotationPoint(-1.0F, -1.0F, 0.0F);
        wings2.addChild(main3);
        setRotationAngle(main3, 0.0F, 0.5672F, 0.0F);

        texture2_r2 = new ModelRenderer(this, 0, 7);
        texture2_r2.mirror = true;
        texture2_r2.setRotationPoint(4.75F, -0.25F, 0.0F);
        main3.addChild(texture2_r2);
        setRotationAngle(texture2_r2, 0.0F, 0.0F, 1.5272F);
        texture2_r2.addBox(-1.0F, -1.25F, 0.0F, 11, 6, 0); // Flat

        txt3_r1 = new ModelRenderer(this, 22, 11);
        txt3_r1.mirror = true;
        txt3_r1.setRotationPoint(2.8511F, -3.1595F, 0.0F);
        main3.addChild(txt3_r1);
        setRotationAngle(txt3_r1, 0.0F, 0.0F, -0.5236F);
        txt3_r1.addBox(-5.0F, -1.5F, 0.0F, 7, 3, 0); // Flat

        cube_r14 = new ModelRenderer(this, 22, 7);
        cube_r14.mirror = true;
        cube_r14.setRotationPoint(1.0F, 1.0F, 0.0F);
        main3.addChild(cube_r14);
        setRotationAngle(cube_r14, 0.0F, 0.0F, -0.5236F);
        cube_r14.addBox(-1.0F, -2.0F, -1.0F, 6, 2, 2); // 3D

        bone13 = new ModelRenderer(this);
        bone13.setRotationPoint(5.25F, -1.0F, 0.0F);
        main3.addChild(bone13);
        setRotationAngle(bone13, 0.0F, 0.5236F, 0.0F);

        texture3_r2 = new ModelRenderer(this, 0, 0);
        texture3_r2.mirror = true;
        texture3_r2.setRotationPoint(4.75F, 0.25F, 0.0F);
        bone13.addChild(texture3_r2);
        setRotationAngle(texture3_r2, 0.0F, 0.0F, 1.5272F);
        texture3_r2.addBox(-1.0F, -1.25F, 0.0F, 11, 7, 0); // Flat

        texture6_r1 = new ModelRenderer(this, 22, 0);
        texture6_r1.mirror = true;
        texture6_r1.setRotationPoint(5.0F, -0.5F, 0.0F);
        bone13.addChild(texture6_r1);
        setRotationAngle(texture6_r1, 0.0F, 0.0F, 1.5272F);
        texture6_r1.addBox(-1.0F, -1.25F, 0.0F, 11, 4, 0); // Flat

        cube_r15 = new ModelRenderer(this, 22, 4);
        cube_r15.mirror = true;
        cube_r15.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone13.addChild(cube_r15);
        setRotationAngle(cube_r15, 0.0F, 0.0F, -0.0873F);
        cube_r15.addBox(-1.0F, -2.25F, -0.25F, 8, 2, 1); // 3D

        txt8_r1 = new ModelRenderer(this, 22, 14);
        txt8_r1.mirror = true;
        txt8_r1.setRotationPoint(4.3511F, -3.4095F, 0.0F);
        bone13.addChild(txt8_r1);
        setRotationAngle(txt8_r1, 0.0F, 0.0F, -0.0873F);
        txt8_r1.addBox(-5.0F, -1.5F, 0.0F, 7, 3, 0); // Flat

        bone14 = new ModelRenderer(this);
        bone14.setRotationPoint(-0.5F, -0.5F, 0.0F);
        bone13.addChild(bone14);

        cube_r16 = new ModelRenderer(this, 20, 25);
        cube_r16.mirror = true;
        cube_r16.setRotationPoint(0.0F, 8.0F, 0.0F);
        bone14.addChild(cube_r16);
        setRotationAngle(cube_r16, 0.0F, 0.0F, 2.0071F);
        cube_r16.addBox(-1.0F, -1.25F, -0.5F, 4, 1, 1); // 3D

        cube_r17 = new ModelRenderer(this, 10, 24);
        cube_r17.mirror = true;
        cube_r17.setRotationPoint(0.75F, 4.5F, 0.0F);
        bone14.addChild(cube_r17);
        setRotationAngle(cube_r17, 0.0F, 0.0F, 1.7017F);
        cube_r17.addBox(-1.0F, -1.25F, -0.5F, 4, 1, 1); // 3D

        cube_r18 = new ModelRenderer(this, 22, 17);
        cube_r18.mirror = true;
        cube_r18.setRotationPoint(0.25F, 1.0F, 0.0F);
        bone14.addChild(cube_r18);
        setRotationAngle(cube_r18, 0.0F, 0.0F, 1.309F);
        cube_r18.addBox(-1.75F, -1.25F, -0.5F, 4, 1, 1); // 3D

        bone15 = new ModelRenderer(this);
        bone15.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone13.addChild(bone15);

        cube_r19 = new ModelRenderer(this, 22, 23);
        cube_r19.mirror = true;
        cube_r19.setRotationPoint(5.5F, 8.0F, 0.0F);
        bone15.addChild(cube_r19);
        setRotationAngle(cube_r19, 0.0F, 0.0F, 1.1345F);
        cube_r19.addBox(-2.5F, -1.0F, -0.5F, 4, 1, 1); // 3D

        cube_r20 = new ModelRenderer(this, 22, 21);
        cube_r20.mirror = true;
        cube_r20.setRotationPoint(4.0F, 3.75F, 0.0F);
        bone15.addChild(cube_r20);
        setRotationAngle(cube_r20, 0.0F, 0.0F, 1.3526F);
        cube_r20.addBox(-2.5F, -1.0F, -0.5F, 4, 1, 1); // 3D

        cube_r21 = new ModelRenderer(this, 22, 19);
        cube_r21.mirror = true;
        cube_r21.setRotationPoint(2.0F, 0.5F, 0.0F);
        bone15.addChild(cube_r21);
        setRotationAngle(cube_r21, 0.0F, 0.0F, 0.7418F);
        cube_r21.addBox(-2.5F, -1.25F, -0.5F, 4, 1, 1); // 3D

        bone16 = new ModelRenderer(this);
        bone16.setRotationPoint(6.5F, -1.0F, 0.0F);
        bone13.addChild(bone16);
        setRotationAngle(bone16, 0.0F, 0.48F, 0.0F);

        texture4_r2 = new ModelRenderer(this, 0, 19);
        texture4_r2.mirror = true;
        texture4_r2.setRotationPoint(3.5F, 0.5F, 0.0F);
        bone16.addChild(texture4_r2);
        setRotationAngle(texture4_r2, 0.0F, 0.0F, 1.5272F);
        texture4_r2.addBox(-1.0F, -1.25F, 0.0F, 11, 5, 0); // Flat

        bone17 = new ModelRenderer(this);
        bone17.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone16.addChild(bone17);

        cube_r22 = new ModelRenderer(this, 10, 26);
        cube_r22.mirror = true;
        cube_r22.setRotationPoint(3.75F, 5.25F, 0.0F);
        bone17.addChild(cube_r22);
        setRotationAngle(cube_r22, 0.0F, 0.0F, 0.8727F);
        cube_r22.addBox(-2.5F, -1.0F, -0.5F, 4, 1, 1); // 3D

        cube_r23 = new ModelRenderer(this, 0, 28);
        cube_r23.mirror = true;
        cube_r23.setRotationPoint(2.25F, 3.5F, 0.0F);
        bone17.addChild(cube_r23);
        setRotationAngle(cube_r23, 0.0F, 0.0F, 1.2217F);
        cube_r23.addBox(-2.5F, -1.0F, -0.5F, 3, 1, 1); // 3D

        cube_r24 = new ModelRenderer(this, 20, 27);
        cube_r24.mirror = true;
        cube_r24.setRotationPoint(1.25F, 1.5F, 0.0F);
        bone17.addChild(cube_r24);
        setRotationAngle(cube_r24, 0.0F, 0.0F, 0.7418F);
        cube_r24.addBox(-2.5F, -1.25F, -0.5F, 3, 1, 1); // 3D

        bone18 = new ModelRenderer(this);
        bone18.setRotationPoint(0.0F, 0.0F, 0.0F);
        bone16.addChild(bone18);

        cube_r25 = new ModelRenderer(this, 0, 26);
        cube_r25.mirror = true;
        cube_r25.setRotationPoint(4.5F, -1.25F, 0.0F);
        bone18.addChild(cube_r25);
        setRotationAngle(cube_r25, 0.0F, 0.0F, -0.0873F);
        cube_r25.addBox(-1.0F, -1.5F, -0.5F, 4, 1, 1); // 3D

        cube_r26 = new ModelRenderer(this, 0, 24);
        cube_r26.mirror = true;
        cube_r26.setRotationPoint(1.0F, -0.25F, 0.0F);
        bone18.addChild(cube_r26);
        setRotationAngle(cube_r26, 0.0F, 0.0F, -0.3054F);
        cube_r26.addBox(-1.0F, -1.75F, -0.5F, 4, 1, 1); // 3D

        texture5_r2 = new ModelRenderer(this, 0, 13);
        texture5_r2.mirror = true;
        texture5_r2.setRotationPoint(5.75F, -1.75F, 0.0F);
        bone18.addChild(texture5_r2);
        setRotationAngle(texture5_r2, 0.0F, 0.0F, 1.5272F);
        texture5_r2.addBox(-1.0F, -1.25F, 0.0F, 11, 7, 0); // Flat
    }

    @Override
    public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
        if(wings != null) wings.render(f5);
        if(wings2 != null) wings2.render(f5);
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.rotateAngleX = x;
        modelRenderer.rotateAngleY = y;
        modelRenderer.rotateAngleZ = z;
    }

    public void setRotationAngles(float partialTicks) {
        float speed = 0.5f;
        float cycle = (float) (Math.sin(System.currentTimeMillis() * 0.003 * speed) + 1.0F) / 2.0F;

        // LEWE
        if (this.wings != null) {
            this.wings.rotateAngleX = toRad(-3.213F * cycle);
            this.wings.rotateAngleY = toRad(18.0937F * cycle);
            this.wings.rotateAngleZ = toRad(-10.4855F * cycle);
        }
        if (this.main != null) this.main.rotateAngleX = toRad(-12.5F * cycle);
        if (this.bone != null) {
            this.bone.rotateAngleX = toRad(-2.0F * cycle);
            this.bone.rotateAngleY = toRad(65.0F * cycle);
        }
        if (this.bone2 != null) this.bone2.rotateAngleY = toRad(70.0F * cycle);
        if (this.bone4 != null) this.bone4.rotateAngleX = toRad(4.5F * cycle);

        // PRAWE
        if (this.wings2 != null) {
            this.wings2.rotateAngleX = toRad(-3.213F * cycle);
            this.wings2.rotateAngleY = toRad(-18.0937F * cycle);
            this.wings2.rotateAngleZ = toRad(10.4855F * cycle);
        }
        if (this.main3 != null) this.main3.rotateAngleX = toRad(-12.5F * cycle);
        if (this.bone13 != null) {
            this.bone13.rotateAngleX = toRad(-2.0F * cycle);
            this.bone13.rotateAngleY = toRad(-65.0F * cycle);
        }
        if (this.bone16 != null) this.bone16.rotateAngleY = toRad(-70.0F * cycle);
        if (this.bone14 != null) this.bone14.rotateAngleX = toRad(4.5F * cycle);
    }

    private float toRad(float degrees) {
        return degrees * (float) Math.PI / 180.0F;
    }
}
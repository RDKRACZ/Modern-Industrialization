/*
 * MIT License
 *
 * Copyright (c) 2020 Azercoco & Technici4n
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package aztech.modern_industrialization.pipes.fluid;

import aztech.modern_industrialization.MIIdentifier;
import aztech.modern_industrialization.api.ReiDraggable;
import aztech.modern_industrialization.machines.MachineScreenHandlers;
import aztech.modern_industrialization.pipes.gui.PipeScreen;
import aztech.modern_industrialization.pipes.impl.PipePackets;
import aztech.modern_industrialization.util.*;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;

public class FluidPipeScreen extends PipeScreen<FluidPipeScreenHandler> {
    private static final Identifier TEXTURE = new MIIdentifier("textures/gui/pipe/fluid.png");

    public FluidPipeScreen(FluidPipeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title, FluidPipeScreenHandler.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();

        addNetworkFluidButton();
        addConnectionTypeButton(148, 22, handler.iface);
        addPriorityWidgets(33, 42, handler.iface, "transfer");
    }

    @Override
    protected Identifier getBackgroundTexture() {
        return TEXTURE;
    }

    private void addNetworkFluidButton() {
        addDrawableChild(new NetworkFluidButton(72 + this.x, 20 + this.y, widget -> updateNetworkFluid(), (button, matrices, mouseX, mouseY) -> {
            List<Text> lines = new ArrayList<>();
            lines.add(FluidHelper.getFluidName(handler.iface.getNetworkFluid(), false));
            if (!handler.iface.getNetworkFluid().isBlank()) {
                lines.add(new TranslatableText("text.modern_industrialization.network_fluid_help_clear").setStyle(TextHelper.GRAY_TEXT));
            } else {
                lines.add(new TranslatableText("text.modern_industrialization.network_fluid_help_set").setStyle(TextHelper.GRAY_TEXT));
            }
            renderTooltip(matrices, lines, mouseX, mouseY);
        }, handler.iface));
    }

    private void updateNetworkFluid() {
        FluidPipeInterface iface = handler.iface;
        FluidVariant targetFluid = null;
        if (iface.getNetworkFluid().isBlank()) {
            // Want to set the fluid
            ItemStack cursorStack = handler.getCursorStack();
            FluidVariant fluid = StorageUtil.findStoredResource(ContainerItemContext.ofPlayerCursor(client.player, handler).find(FluidStorage.ITEM),
                    null);
            if (fluid != null && !fluid.isBlank()) {
                targetFluid = fluid;
            }
        } else if (hasShiftDown()) {
            targetFluid = FluidVariant.blank();
        }
        if (targetFluid != null) {
            setNetworkFluid(targetFluid);
        }
    }

    private void setNetworkFluid(FluidVariant fluidKey) {
        handler.iface.setNetworkFluid(fluidKey);
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(handler.syncId);
        fluidKey.toPacket(buf);
        ClientPlayNetworking.send(PipePackets.SET_NETWORK_FLUID, buf);
    }

    private class NetworkFluidButton extends ButtonWidget implements ReiDraggable {
        private final FluidPipeInterface iface;

        public NetworkFluidButton(int x, int y, PressAction onPress, TooltipSupplier tooltipSupplier, FluidPipeInterface iface) {
            super(x, y, 16, 16, null, onPress, tooltipSupplier);
            this.iface = iface;
        }

        @Override
        public void renderButton(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            // Render fluid slot
            RenderSystem.setShaderTexture(0, MachineScreenHandlers.SLOT_ATLAS);
            drawTexture(matrices, x - 1, y - 1, 18, 0, 18, 18);
            // Render the fluid itself
            if (!iface.getNetworkFluid().isBlank()) {
                RenderHelper.drawFluidInGui(matrices, iface.getNetworkFluid(), x, y);
            }
            // Render the white hover effect
            if (isHovered()) {
                RenderSystem.disableDepthTest();
                RenderSystem.colorMask(true, true, true, false);
                this.fillGradient(matrices, x, y, x + 16, y + 16, -2130706433, -2130706433);
                RenderSystem.colorMask(true, true, true, true);
                RenderSystem.enableDepthTest();
            }
            // Render the tooltip
            if (isHovered()) {
                renderTooltip(matrices, mouseX, mouseY);
            }
        }

        @Override
        public boolean dragFluid(FluidVariant fluidKey, Simulation simulation) {
            if (simulation.isActing()) {
                setNetworkFluid(fluidKey);
            }
            return true;
        }

        @Override
        public boolean dragItem(ItemVariant itemKey, Simulation simulation) {
            return false;
        }
    }
}

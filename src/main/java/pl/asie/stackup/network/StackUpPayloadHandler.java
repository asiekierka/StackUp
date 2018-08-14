/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * This file is part of StackUp.
 *
 * StackUp is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * StackUp is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with StackUp.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.stackup.network;

import io.netty.buffer.Unpooled;
import net.minecraft.item.Item;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.util.ResourceLocation;
import org.dimdev.rift.listener.CustomPayloadHandler;
import pl.asie.stackup.StackUp;

public class StackUpPayloadHandler implements CustomPayloadHandler {
	public static final ResourceLocation CHANNEL = new ResourceLocation("stackup", "synchronize");

	public static void sendPacket(NetHandlerPlayServer server) {
		PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
		buffer.writeInt(StackUp.maxStackSize);
		buffer.writeInt(StackUp.oldStackValues.size());
		for (Item i : StackUp.oldStackValues.keySet()) {
			buffer.writeInt(Item.REGISTRY.getIDForObject(i));
			buffer.writeVarInt(i.getItemStackLimit());
		}

		SPacketCustomPayload packetCustomPayload = new SPacketCustomPayload(CHANNEL, buffer);
		server.sendPacket(packetCustomPayload);
	}

	@Override
	public boolean clientHandlesChannel(ResourceLocation channelName) {
		return CHANNEL.equals(channelName);
	}

	@Override
	public void clientHandleCustomPayload(ResourceLocation channelName, PacketBuffer bufferData) {
		StackUp.maxStackSize = bufferData.readInt();
		int changes = bufferData.readInt();
		for (int i = 0; i < changes; i++) {
			Item item = Item.REGISTRY.getObjectById(bufferData.readInt());
			int newLimit = bufferData.readVarInt();
			if (item != null) {
				StackUp.setMaxStackSize(item, newLimit);
			}
		}
	}

	@Override
	public boolean serverHandlesChannel(ResourceLocation channelName) {
		return false;
	}

	@Override
	public void serverHandleCustomPayload(ResourceLocation channelName, PacketBuffer bufferData) {

	}
}

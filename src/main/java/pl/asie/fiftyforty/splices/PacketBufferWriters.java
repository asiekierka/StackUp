/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * This file is part of FiftyForty.
 *
 * FiftyForty is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FiftyForty is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FiftyForty.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.fiftyforty.splices;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import pl.asie.fiftyforty.Constants;

import java.io.IOException;

public class PacketBufferWriters extends PacketBuffer {
	public PacketBufferWriters(ByteBuf wrapped) {
		super(wrapped);
	}

	@Override
	public ItemStack readItemStack() throws IOException {
		int id = this.readShort();

		if (id < 0) {
			return ItemStack.EMPTY;
		} else {
			int count = this.readByte();
			if (count == Constants.COUNT_MAGIC) {
				count = this.readInt();
			}
			int damage = this.readShort();
			ItemStack itemstack = new ItemStack(Item.getItemById(id), count, damage);
			itemstack.getItem().readNBTShareTag(itemstack, this.readCompoundTag());
			return itemstack;
		}
	}

	@Override
	public PacketBuffer writeItemStack(ItemStack stack) {
		if (stack.isEmpty()) {
			this.writeShort(-1);
		} else {
			this.writeShort(Item.getIdFromItem(stack.getItem()));
			if (stack.getCount() >= 0 && stack.getCount() <= 64) {
				this.writeByte(stack.getCount());
			} else {
				this.writeByte(Constants.COUNT_MAGIC);
				this.writeInt(stack.getCount());
			}
			this.writeShort(stack.getMetadata());
			NBTTagCompound tag = null;

			if (stack.getItem().isDamageable() || stack.getItem().getShareTag()) {
				tag = stack.getItem().getNBTShareTag(stack);
			}

			this.writeCompoundTag(tag);
		}

		return this;
	}
}

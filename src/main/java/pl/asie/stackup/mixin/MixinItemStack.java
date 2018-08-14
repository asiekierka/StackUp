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

package pl.asie.stackup.mixin;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import pl.asie.stackup.StackUp;

import java.util.List;

@Mixin(ItemStack.class)
public abstract class MixinItemStack {
	@Inject(method = "getTooltip", at = @At("TAIL"))
	private void getTooltip(EntityPlayer player, ITooltipFlag flag, CallbackInfoReturnable ci) {
		//noinspection ConstantConditions
		String count = Integer.toString(((ItemStack) (Object) this).getCount());
		String countA = StackUp.abbreviate(count);
		//noinspection StringEquality
		if (count != countA) {
			List l = ((List) ci.getReturnValue());
			//noinspection unchecked
			l.set(0, new TextComponentTranslation("%s %s", l.get(0), new TextComponentString("x " + count)));

		}
	}
}

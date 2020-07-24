/*
 * Copyright (c) 2018, 2020 Adrian Siekierka
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

package pl.asie.stackup.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import pl.asie.stackup.StackUp;
import pl.asie.stackup.StackUpConfig;
import pl.asie.stackup.StackUpHelpers;

public class StackUpTextGenerator {
	public static class AbbreviationResult {
		private String text;
		private final float scaleFactor;
		private final boolean fits;
		private final boolean abbreviated;

		public AbbreviationResult(String text, float scaleFactor, boolean fits, boolean abbreviated) {
			this.text = text;
			this.scaleFactor = scaleFactor;
			this.fits = fits;
			this.abbreviated = abbreviated;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		public float getScaleFactor() {
			return scaleFactor;
		}

		public boolean isFits() {
			return fits;
		}

		public boolean isAbbreviated() {
			return abbreviated;
		}
	}

	public static int getStringLenWithoutFmtCodes(String count) {
		int i = 0;
		while (count.codePointAt(i) == 0xA7) {
			i += 2;
		}
		return count.length() - i;
	}

	public static AbbreviationResult abbreviate(FontRenderer fr, String count, int maxWidth, boolean justCheckAbbreviation) {
		String oldString = count;

		StringBuilder fmtCodes = new StringBuilder();
		int fmtCodeCount = 0;
		while (count.codePointAt(0) == 0xA7) {
			fmtCodes.append(count, 0, 2);
			fmtCodeCount++;
			count = count.substring(2);
		}

		AbbreviationResult result = abbreviateInner(fr, count, maxWidth, justCheckAbbreviation);
		if (fmtCodeCount > 0) {
			result.setText(fmtCodes.append(result.getText()).toString());
		}
		return result;
	}

	private static AbbreviationResult abbreviateInner(FontRenderer fr, String count, int maxWidth, boolean justCheckAbbreviation) {
		int countI;
		int paddedCountI;
		int maxScaleFactor = StackUp.proxy.getCurrentScaleFactor();
		AbbreviationResult result;

		try {
			countI = Integer.parseInt(count);
		} catch (NumberFormatException e) {
			countI = -1;
		}

		if (countI < 0) {
			// don't handle negative or invalid numbers
			return tryFitString(fr, maxWidth, count, count, maxScaleFactor, false);
		}

		// pad with zeroes
		{
			int tmpCountI = countI;
			paddedCountI = 1;
			while (tmpCountI >= 10) {
				tmpCountI /= 10;
				paddedCountI *= 10;
			}
			paddedCountI *= tmpCountI;
		}

		result = tryFitString(fr, maxWidth, Integer.toString(paddedCountI), count, maxScaleFactor, false);
		if (!result.isFits()) {
			result = tryFitString(fr, maxWidth, abbreviateInnerLong(paddedCountI), abbreviateInnerLong(countI), maxScaleFactor, true);
			if (!result.isFits()) {
				result = tryFitString(fr, maxWidth, abbreviateInnerShort(paddedCountI), abbreviateInnerShort(countI), maxScaleFactor, true);
			}
		}

		return result;
	}

	private static AbbreviationResult tryFitString(FontRenderer fr, int maxWidth, String comparedText, String text, int maxScaleFactor, boolean abbreviated) {
		int strWidth = Math.max(fr.getStringWidth(text), fr.getStringWidth(comparedText));

		if (StackUpConfig.scaleTextLinearly) {
			float scaleFactor = ((float) maxWidth / strWidth);
			boolean fits = true;
			if (scaleFactor > 1.0f) {
				scaleFactor = 1.0f;
			} else if (scaleFactor < StackUpConfig.lowestScaleDown) {
				scaleFactor = StackUpConfig.lowestScaleDown;
				fits = false;
			}
			return new AbbreviationResult(text, scaleFactor, fits, abbreviated);
		} else {
			// This could probably be optimized.
			int currScaleFactor = maxScaleFactor;
			while (currScaleFactor >= 1 && ((float) currScaleFactor / maxScaleFactor) >= StackUpConfig.lowestScaleDown) {
				float scaledStrWidth = ((float) currScaleFactor / maxScaleFactor) * strWidth;
				if (scaledStrWidth <= maxWidth) {
					return new AbbreviationResult(text, ((float) currScaleFactor / maxScaleFactor), true, abbreviated);
				}

				// try with lower scale factor
				currScaleFactor--;
			}

			return new AbbreviationResult(text, ((float) currScaleFactor / maxScaleFactor), false, abbreviated);
		}
	}

	private static String abbreviateInnerShort(int countI) {
		if (countI >= 1000 && countI <= 99999) {
			return (countI / 1000) + "K";
		} else if (countI >= 100000 && countI <= 999999) {
			return "." + (countI / 100000) + "M";
		} else if (countI >= 1000000 && countI <= 99999999) {
			return (countI / 1000000) + "M";
		} else if (countI >= 100000000 && countI <= 999999999) {
			return "." + (countI / 100000000) + "B";
		} else if (countI >= 1000000000) {
			return (countI / 1000000000) + "B";
		} else {
			return Integer.toString(countI);
		}
	}


	private static String abbreviateInnerLong(int countI) {
		if (countI >= 100000 && countI <= 999999) {
			return (countI / 1000) + "K";
		} else if (countI >= 1000000 && countI <= 9999999) {
			int a = (countI / 10000);
			return (a / 100) + "." + String.format("%02d", (a % 100)) + "M";
		} else if (countI >= 10000000 && countI <= 99999999) {
			int a = (countI / 100000);
			return (a / 10) + "." + (a % 10) + "M";
		} else if (countI >= 100000000 && countI <= 999999999) {
			int a = (countI / 1000000);
			return a + "M";
		} else if (countI >= 1000000000) {
			int a = (countI / 10000000);
			return (a / 100) + "." + String.format("%02d", (a % 100)) + "B";
		} else {
			return Integer.toString(countI);
		}
	}
}

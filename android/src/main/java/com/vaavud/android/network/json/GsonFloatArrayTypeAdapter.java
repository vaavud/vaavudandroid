package com.vaavud.android.network.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class GsonFloatArrayTypeAdapter implements JsonSerializer<Float[]> {

	@Override
	public JsonElement serialize(Float[] object, Type typeOfSrc, JsonSerializationContext context) {
		if (object == null) {
			return JsonNull.INSTANCE;
		}
		boolean isFirst = true;
		JsonArray jsonArray = new JsonArray();
		for (Float f : object) {
			if (isFirst) {
				isFirst = false;
				jsonArray.add(new JsonPrimitive(roundToThreeDigits(f)));
			}
			else {
				jsonArray.add(new JsonPrimitive(roundToOneDigit(f)));
			}
		}
		return jsonArray;
	}

	private float roundToOneDigit(float f) {
		return Math.round(f * 10F) / 10F;
	}

	private float roundToTwoDigits(float f) {
		return Math.round(f * 100F) / 100F;
	}

	private float roundToThreeDigits(float f) {
		return Math.round(f * 1000F) / 1000F;
	}
}

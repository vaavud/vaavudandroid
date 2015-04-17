package com.vaavud.util;

import java.util.Locale;

public final class AlgorithmConstantsUtil {

	private static enum PhoneModel {

		GS4(1.09D, new String[] {"GT-I9500", "SHV-E300", "GT-I9505", "SGH-I337", 
				"SGH-M919", "SCH-I545", "SPH-L720", "SCH-R970", "GT-I9508", "SCH-I959", "GT-I9502", 
				"SGH-N045"}),
				
		GS4MINI(1.09D, new String[] {"GT-I919"}),
				
		GS3(1.09D, new String[] {"GT-I9300", "GT-I9305", "SHV-E210", "SGH-T999", 
				"SGH-I747", "SGH-N064", "SGH-N035", "SCH-J021", "SCH-R530", 
				"SCH-I535", "SPH-L710", "GT-I9308", "SCH-I939"}),
				
		GS2(1.05D, new String[] {"GT-I9100", "GT-I9210", "GT-I9210", "SGH-I757",
				"SGH-I727", "SGH-I927", "SGH-T989", "GT-I9108", "ISW11",
				"MODEL SC-02", "SHW-M250", "SGH-I777", "SGH-I927", "SPH-D710", "SGH-T989",
				"SCH-R760", "GT-I9105"});
		
		private final double frequencyFactor;
		private final String[] models;
		
		private PhoneModel(double frequencyFactor, String[] models) {
			this.frequencyFactor = frequencyFactor;
			this.models = models;
		}
		
		public boolean matches(String model) {
			for (String canonicalModel : models) {
				if (model.toUpperCase(Locale.US).startsWith(canonicalModel.toUpperCase(Locale.US))) {
					return true;
				}
			}
			return false;
		}
		
		public double getFrequencyFactor() {
			return frequencyFactor;
		}
	}
	
	private static double STANDARD_FREQUENCY_START = 0.238D;
	private static double STANDARD_FREQUENCY_FACTOR = 1.07D;
	
	public static double getFrequencyFactor(String model) {
		for (PhoneModel phoneModel : PhoneModel.values()) {
			if (phoneModel.matches(model)) {
				return phoneModel.getFrequencyFactor();
			}
		}
		return STANDARD_FREQUENCY_FACTOR;
	}

	public static double getFrequencyStart(String model) {
		return STANDARD_FREQUENCY_START;
	}

	private AlgorithmConstantsUtil() {
	}
}

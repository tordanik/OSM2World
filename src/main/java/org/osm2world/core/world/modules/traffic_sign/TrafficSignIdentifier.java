package org.osm2world.core.world.modules.traffic_sign;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * the identifier of a particular type of traffic sign.
 * Either a simple human-readable value, such as "city_limit", or a country-specific identifier such as "GB:956".
 * Can also include brackets with a parameter, such as "DE:265[3.8]".
 */
public class TrafficSignIdentifier {

	public final @Nullable String country;
	public final String sign;
	public final @Nullable String bracketText;

	public TrafficSignIdentifier(String country, String sign, String bracketText) {
		this.country = country;
		this.sign = sign;
		this.bracketText = bracketText;
	}

	public TrafficSignIdentifier(String country, String signValue) {

		this.country = country;

		Pattern pattern = Pattern.compile("\\[(.*)\\]");
		Matcher matcher = pattern.matcher(signValue);

		if (matcher.find()) {
			this.bracketText = matcher.group(1);
			this.sign = signValue.replace("[" + this.bracketText + "]", "");
		} else {
			this.sign = signValue;
			this.bracketText = null;
		}

	}

	@Override
	public String toString() {
		return (country == null ? "" : (country + ":"))
				+ sign
				+ (bracketText == null ? "" : ("[" + bracketText + "]"));
	}

	/**
	 * Some jurisdiction's traffic sign ids have sign names separated into a type and sub-type.
	 * For example, "50" is the sub-type in "DE:274-50"
	 * This method returns the sub-type of this identifier, if any.
	 */
	public @Nullable String subType() {

		Pattern pattern = Pattern.compile("\\d*-(\\d+)[A-Za-z]*");
		Matcher matcher = pattern.matcher(sign);

		if (matcher.matches()) {
			return matcher.group(1);
		} else {
			return null;
		}

	}

	/**
	 * returns the string used to look up properties in a configuration file.
	 * Looks like "SIGN_DE_265" for the "DE:265[3.8]" identifier example.
	 */
	public String configKey() {

		String result = "SIGN_";

		if (country != null) {
			result += country + "_";
		}

		result += sign.replace('-', '_').toUpperCase();

		return result;

	}

	/**
	 * variation of {@link #configKey()} that omits the {@link #subType()} (if any).
	 * This is useful because there will often not be a separate image file available for each sub-type.
	 */
	public String configKeyWithoutSubType() {
		if (subType() == null) {
			return configKey();
		} else {
			return configKey().replace("_" + subType(), "");
		}
	}

	public static List<TrafficSignIdentifier> parseTrafficSignValue(String tagValue) {

		if (tagValue.isEmpty()) return emptyList();

		/* split the traffic sign value into its components */

		String[] signs = {};
		String country;

		if (tagValue.contains(":")) {

			//if country prefix is used
			String[] countryAndSigns = tagValue.split(":", 2);
			if (countryAndSigns.length != 2)
				return emptyList();
			country = countryAndSigns[0];
			signs = countryAndSigns[1].split("[;,]");

		} else {
			//human-readable value
			signs = tagValue.split("[;,]");
			country = null;
		}

		/* create the resulting list */

		return stream(signs)
				.map(String::trim)
				.map(sign -> new TrafficSignIdentifier(country, sign))
				.collect(toList());

	}

}

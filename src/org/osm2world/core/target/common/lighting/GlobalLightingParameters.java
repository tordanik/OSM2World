package org.osm2world.core.target.common.lighting;

import java.awt.Color;
import java.util.Calendar;

import org.osm2world.core.math.VectorXYZ;

/**
 * parameters that describe lighting affecting the entire scene; immutable
 */
public class GlobalLightingParameters {
	
	// TODO Make it so these don't have to be public and mutable
	// Construct one of these from `Configuration` and pass it through
	// to everything that needs it instead of exposing it publicly
	public Color globalAmbientColor;
	
	/**
	 * source of the scene's directional lighting;
	 * null disables it and leaves only ambient lighting
	 */
	public VectorXYZ lightFromDirection;
	public float intensity = 1.0f;

	public Color lightColorDiffuse;
	public Color lightColorSpecular;
	public Color scatterColor = new Color(0.55f, 0.7f, 0.8f);
	
	private GlobalLightingParameters(
			Color globalAmbientLight, VectorXYZ lightFromDirection,
			Color lightColorDiffuse, Color lightColorSpecular) {
		
		this.globalAmbientColor = globalAmbientLight;
		this.lightFromDirection = lightFromDirection;
		this.lightColorDiffuse = lightColorDiffuse;
		this.lightColorSpecular = lightColorSpecular;

		Calendar c = Calendar.getInstance();
		c.set(2001,0,1,12,0,0);
		setTime(c);
		
	}

	public Color setTime(Calendar date)
	{
		double lon = 0;//48.75;
		double lat = 0;//13.46;

		System.out.println("Calculating sun position on: " + date.getTime().toString() + "...");
		int days = date.get(Calendar.DAY_OF_YEAR);

		double time = date.get(Calendar.HOUR_OF_DAY) + (date.get(Calendar.MINUTE) / 60.0);

		double solarMeridian = Math.round(lon / 15) * 15;

		// Correction due to earth's position in orbit
		double b = Math.toRadians(360.0/365.0 * (days - 81));
		double equationOfTime = 9.87 * Math.sin(2 * b) - 7.53 * Math.cos(b) - 1.5 * Math.sin(b);

		// Correction due to longitudinal variations
		double timeCorrection = 4 * (lon - solarMeridian) + equationOfTime;

		double solarTime = time + (timeCorrection / 60.0);

		double hourAngle = Math.toRadians(15) * (solarTime - 12);

		double declinationAngle = Math.asin(Math.sin(Math.toRadians(23.45)) * Math.sin(b));

		double elevationAngle = Math.asin(Math.sin(declinationAngle) * Math.sin(lat)
						+ Math.cos(declinationAngle) * Math.cos(lat) * Math.cos(hourAngle));

		double zenithAngle = (Math.PI / 2.0) - elevationAngle;

		// From https://en.wikipedia.org/wiki/Solar_azimuth_angle
		double azimuthAngle = Math.acos((Math.sin(declinationAngle) - Math.cos(zenithAngle) * Math.sin(lat))
						/ (Math.sin(zenithAngle) * Math.cos(lat)));

		if(hourAngle > 0) azimuthAngle = (Math.PI * 2 - azimuthAngle);

		System.out.println("Elevation Angle: " + Math.toDegrees(elevationAngle));
		System.out.println("Azimuth Angle: " + Math.toDegrees(azimuthAngle));
		System.out.println(elevationAngle > 0?"Day":"Night");

		double x = Math.cos(elevationAngle) * Math.sin(azimuthAngle);
		double y = Math.cos(elevationAngle) * Math.cos(azimuthAngle);
		double z = Math.sin(elevationAngle);

		System.out.println("Sun Vector: <" + x +", " + y +", " + z + ">");
		System.out.println("------------------");
		System.out.println("");

		// OSM to World uses y up coordinate system, so switch y and z
		lightFromDirection = new VectorXYZ(x, z, y);


		//Color night = new Color(16.0f/255.0f, 78.0f/255.0f, 0.5f);
		Color night = new Color(0.0f, 0f, 0.2f);
		Color twilight = new Color(0.2f, 0.2f, 0.4f);
		Color day = new Color(1.0f, 1.0f, 0.95f);
		Color sunset = new Color(1.0f, 197.0f/255.0f, 143.0f/255.0f);

		if(Math.toDegrees(elevationAngle) > 8)
			globalAmbientColor = day;
		else if(Math.toDegrees(elevationAngle) > 0)
			globalAmbientColor = sunset;
		else if(Math.toDegrees(elevationAngle) > -19)
			globalAmbientColor = twilight;
		else
			globalAmbientColor = night;

		// Moonlight
		// if(Math.toDegrees(elevationAngle) < 0) {
		//	lightFromDirection = new VectorXYZ(-x, -z, y);
		//	intensity = 0.3f;
		// }
		// else
		// 	intensity = 1.0f;

		if(Math.toDegrees(elevationAngle) > 8)
			return new Color(64, 156, 255);
		else if(Math.toDegrees(elevationAngle) > 0)
			return new Color(255, 147, 41);
		else if(Math.toDegrees(elevationAngle) > -8)
			return new Color(39, 0, 148);
		else
			return new Color(20, 0, 80);

	}

	
	public static final GlobalLightingParameters DEFAULT =
			new GlobalLightingParameters(
					new Color(1.0f, 1.0f, 1.0f),
					new VectorXYZ(0.0, 1.0, 0.0),
					Color.WHITE,
					Color.WHITE);
	
}

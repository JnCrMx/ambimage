package de.jcm.ambimage;

import java.awt.*;

public enum ApplicationSpecification
{
	DISCORD(new Color(0x36393f), new Color(0xffffff), 268),
	CUSTOM(null,null, 0)
	;

	private Color darkColor;
	private Color lightColor;
	private int maxWidth;

	ApplicationSpecification(Color darkColor, Color lightColor, int maxWidth)
	{
		this.darkColor = darkColor;
		this.lightColor = lightColor;
		this.maxWidth = maxWidth;
	}

	public Color getDarkColor()
	{
		return darkColor;
	}

	public Color getLightColor()
	{
		return lightColor;
	}

	public int getMaxWidth()
	{
		return maxWidth;
	}

	public void setDarkColor(Color darkColor)
	{
		if(this!=CUSTOM)
			throw new IllegalStateException("only custom specification may be manipulated");
		this.darkColor = darkColor;
	}

	public void setLightColor(Color lightColor)
	{
		if(this!=CUSTOM)
			throw new IllegalStateException("only custom specification may be manipulated");
		this.lightColor = lightColor;
	}

	public void setMaxWidth(int maxWidth)
	{
		if(this!=CUSTOM)
			throw new IllegalStateException("only custom specification may be manipulated");
		this.maxWidth = maxWidth;
	}
}

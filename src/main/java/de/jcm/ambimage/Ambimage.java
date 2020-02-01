package de.jcm.ambimage;

import joptsimple.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

public class Ambimage
{
	public static void main(String[] args) throws IOException
	{
		OptionParser parser = new OptionParser();
		ArgumentAcceptingOptionSpec<ApplicationSpecification> applicationSpec =
				parser.accepts("application", "Application to generate ambivalent image for.")
				.withRequiredArg().withValuesConvertedBy(new ValueConverter<ApplicationSpecification>()
		{
			@Override
			public ApplicationSpecification convert(String value)
			{
				return ApplicationSpecification.valueOf(value);
			}

			@Override
			public Class<? extends ApplicationSpecification> valueType()
			{
				return ApplicationSpecification.class;
			}

			@Override
			public String valuePattern()
			{
				return "String";
			}
		}).defaultsTo(ApplicationSpecification.DISCORD);

		ValueConverter<Color> colorValueConverter = new ValueConverter<Color>()
		{
			@Override
			public Color convert(String value)
			{
				String[] parts = value.split(",");

				int r = Integer.parseInt(parts[0]);
				int g = Integer.parseInt(parts[1]);
				int b = Integer.parseInt(parts[2]);

				return new Color(r, g, b);
			}

			@Override
			public Class<? extends Color> valueType()
			{
				return Color.class;
			}

			@Override
			public String valuePattern()
			{
				return "r,g,b";
			}
		};

		OptionSpecBuilder lbg =
				parser.accepts("light_background", "Specify light-themed background.")
				.availableUnless("application");
		OptionSpecBuilder dbg =
				parser.accepts("dark_background", "Specify dark-themed background.")
				.availableUnless("application");
		OptionSpecBuilder mw =
				parser.accepts("max_width", "Specify maximal width.")
				.availableUnless("application");

		lbg.requiredIf(dbg, mw);
		dbg.requiredIf(lbg, mw);
		mw.requiredIf(lbg, dbg);

		ArgumentAcceptingOptionSpec<Color> lbga =
				lbg.withRequiredArg().withValuesConvertedBy(colorValueConverter);
		ArgumentAcceptingOptionSpec<Color> dbga =
				dbg.withRequiredArg().withValuesConvertedBy(colorValueConverter);
		ArgumentAcceptingOptionSpec<Integer> mwa =
				mw.withRequiredArg().ofType(Integer.class);

		ArgumentAcceptingOptionSpec<Strategy> strategySpec =
				parser.accepts("strategy", "Strategy for overlaying images")
				.withRequiredArg().withValuesConvertedBy(new ValueConverter<Strategy>()
		{
			@Override
			public Strategy convert(String value)
			{
				return Strategy.valueOf(value);
			}

			@Override
			public Class<? extends Strategy> valueType()
			{
				return Strategy.class;
			}

			@Override
			public String valuePattern()
			{
				return "String";
			}
		}).defaultsTo(Strategy.WEAVE);

		ArgumentAcceptingOptionSpec<File> inputDark =
				parser.accepts("input_dark", "Image for dark-theme users")
						.withRequiredArg().ofType(File.class);
		ArgumentAcceptingOptionSpec<File> inputLight =
				parser.accepts("input_light", "Image for light-theme users")
						.withRequiredArg().ofType(File.class);
		ArgumentAcceptingOptionSpec<File> output =
				parser.accepts("output", "Output file")
						.withRequiredArg().ofType(File.class).defaultsTo(new File("output.png"));
		ArgumentAcceptingOptionSpec<String> outputFormat =
				parser.accepts("output_format", "Output format")
						.withRequiredArg().defaultsTo("png");

		parser.accepts("help", "Print this help").forHelp();
		parser.accepts("applications", "Print all applications").forHelp();
		parser.accepts("strategies", "Print all strategies").forHelp();

		OptionSet optionSet = parser.parse(args);
		if(optionSet.has("help"))
		{
			parser.printHelpOn(System.out);
			System.exit(0);
		}
		if(optionSet.has("applications"))
		{
			Stream.of(ApplicationSpecification.values())
					.filter(a->a!=ApplicationSpecification.CUSTOM)
					.forEach(a -> System.out.println(a.name()));
			System.exit(0);
		}
		if(optionSet.has("strategies"))
		{
			Stream.of(Strategy.values()).forEach(s -> System.out.println(s.name()));
			System.exit(0);
		}

		if(!optionSet.has(inputDark) || !optionSet.has(inputLight))
		{
			System.err.println("Input options are required!");
			System.exit(2);
		}

		BufferedImage dark = ImageIO.read(inputDark.value(optionSet));
		BufferedImage light = ImageIO.read(inputLight.value(optionSet));

		ApplicationSpecification specification = applicationSpec.value(optionSet);
		if(optionSet.has(lbg))
		{
			specification=ApplicationSpecification.CUSTOM;
			specification.setDarkColor(dbga.value(optionSet));
			specification.setLightColor(lbga.value(optionSet));
			specification.setMaxWidth(mwa.value(optionSet));
		}

		assert dark.getWidth()==light.getWidth();
		assert dark.getHeight()==light.getHeight();

		int width = specification.getMaxWidth();
		int height = (int)(((double)dark.getHeight()/(double)dark.getWidth())*((double)width));

		BufferedImage sDark, sLight;
		if(dark.getWidth()<=width && dark.getHeight()<=height)
		{
			sDark = dark;
			sLight = light;
			width = dark.getWidth();
			height = dark.getHeight();
		}
		else
		{
			sDark = scale(dark, width, height);
			sLight = scale(light, width, height);
		}
		System.out.println("Output will be "+width+ " x "+height+"!");

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		Strategy strategy = strategySpec.value(optionSet);
		switch (strategy)
		{
			case FAIR:
				combineFair(sDark, sLight, result, specification);
				break;
			case WEAVE:
				combineWeave(sDark, sLight, result, specification);
				break;
		}

		ImageIO.write(result, outputFormat.value(optionSet), output.value(optionSet));
		System.out.println("Output written to "+output.value(optionSet).getAbsolutePath()+"!");
	}

	private static void combineFair(BufferedImage dark, BufferedImage light,
	                                     BufferedImage result,
	                                     ApplicationSpecification specification)
	{
		int width = result.getWidth();
		int height = result.getHeight();

		Color lightColor = specification.getLightColor();
		Color darkColor = specification.getDarkColor();

		for(int y=0; y<height; y++)
		{
			int last = y%2;
			for(int x=0; x<width; x++)
			{
				if(last%2==0)
				{
					if(dark.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, lightColor.getRGB());
						last++;
					}
					else if(light.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, darkColor.getRGB());
					}
				}
				else
				{
					if(light.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, darkColor.getRGB());
						last++;
					}
					else if(light.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, lightColor.getRGB());
					}
				}
			}
		}
	}

	private static void combineWeave(BufferedImage dark, BufferedImage light,
	                                BufferedImage result,
	                                ApplicationSpecification specification)
	{
		int width = result.getWidth();
		int height = result.getHeight();

		Color lightColor = specification.getLightColor();
		Color darkColor = specification.getDarkColor();

		for(int y=0; y<height; y++)
		{
			for(int x=0; x<width; x++)
			{
				if((x+y)%2==0)
				{
					if(dark.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, lightColor.getRGB());
					}
					else if(light.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, darkColor.getRGB());
					}
				}
				else
				{
					if(light.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, darkColor.getRGB());
					}
					else if(light.getRGB(x, y)!=-1)
					{
						result.setRGB(x, y, lightColor.getRGB());
					}
				}
			}
		}
	}

	private static BufferedImage scale(BufferedImage src, int width, int height)
	{
		int w = src.getWidth();
		int h = src.getHeight();
		BufferedImage dst = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		AffineTransform at = new AffineTransform();
		at.scale(((double)width/(double)w), ((double)height/(double)h));
		AffineTransformOp scaleOp =
				new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
		dst = scaleOp.filter(src, dst);
		return dst;
	}
}

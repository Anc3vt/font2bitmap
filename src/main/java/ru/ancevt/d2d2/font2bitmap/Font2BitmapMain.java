package ru.ancevt.d2d2.font2bitmap;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import ru.ancevt.util.args.Args;

public class Font2BitmapMain {

	private static final String VERSION = "0.0.1.0";

	private static final int DEFAULT_FONT_SIZE = 44;
	private static final int DEFAULT_ATLAS_SIZE = 256;

	public static void main(String[] args) {
		final Args arguments = new Args(args);

		if (arguments.contains("-v", "--version")) {
			System.out.println(VERSION);
			System.exit(0);
		} else
		if (arguments.contains("--help", "-h")) {
			printHelp();
			System.exit(0);
		}

		String inputFilePath = arguments.getString(new String[] { "--input", "-i" });
		String outputFilePath = arguments.getString(new String[] { "--output", "-o" });
		boolean guiMode;
		boolean bold;
		boolean italic;
		int atlasWidth = DEFAULT_ATLAS_SIZE;
		int atlasHeight = DEFAULT_ATLAS_SIZE;

		if (inputFilePath == null) {
			error("No input font file ( --input \"fontFile.ttf\" )");
			System.exit(1);
		}

		final File inputFile = new File(inputFilePath);
		if (!inputFile.exists()) {
			error("No such input font file \"" + inputFile.getAbsolutePath() + "\"");
			System.exit(1);
		}

		if (outputFilePath == null) {
			final String fileName = inputFile.getName();

			final File outDir = new File("out/");
			if(!outDir.exists()) {
				outDir.mkdir();
			}
			
			outputFilePath = "out/" + inputFile.getName().substring(0, fileName.indexOf('.')) + ".bmf";
		}

		guiMode = arguments.contains("--gui", "-g");
		bold = arguments.contains("--bold", "-B");
		italic = arguments.contains("--italic", "-I");

		if (arguments.contains("--atlas-size", "-a")) {
			final String atlasSizeString = arguments.getString(new String[] { "--atlas-size", "-a" });
			final String[] splitted = atlasSizeString.split("x");

			atlasWidth = Integer.parseInt(splitted[0]);
			atlasHeight = Integer.parseInt(splitted[1]);
		}

		final int fontSize = arguments.contains("--font-size", "-s") ? arguments.getInt("--font-size", "-s")
				: DEFAULT_FONT_SIZE;

		final boolean debugTitleFloating = arguments.contains("--debug-title-floating", "-F");

		trace("Input: " + inputFilePath);
		trace("Output: " + outputFilePath);
		trace("Font size: " + fontSize);
		trace("Atlas size: " + atlasWidth + "x" + atlasHeight);

		new Font2BitmapMain(guiMode, inputFile, outputFilePath, fontSize, bold, italic, atlasWidth, atlasHeight,
				debugTitleFloating);
	}
	
	private static final void printHelp() {
		trace("Usage:");
		trace("\tjava -jar font2bitmap.jar --input /path/fontfile.ttf --output /path/fontfile.bmf");
		trace("");
		trace("Additional parameters:");
		trace("\t--help, -h        prints this help page");
		trace("\t--version, -v     prints version of font2bitmap");
		trace("\t--font-size, -s   font size in points");
		trace("\t--bold, -B        make bold font");
		trace("\t--italic, -I      make italic font");
		trace("\t--atlas-size, -a  size of result atlas (example: -a 44x256)");
		trace("\t--gui, -g         run program in GUI mode (with manual resizing atlas)");
	}

	private static final void trace(Object o) {
		System.out.println(o == null ? null : o.toString());
	}

	private static final void error(Object o) {
		System.err.println(o);
	}

	private String outputFilePath;
	private boolean guiMode;

	public Font2BitmapMain(boolean guiMode, File inputFile, String outputFilePath, int fontSize, boolean bold,
			boolean italic, int atlasWidth, int atlasHeight, boolean debugTitleFloating) {

		this.guiMode = guiMode;

		this.outputFilePath = outputFilePath;

		final Window window = new Window() {
			private static final long serialVersionUID = -3026963495591088690L;

			@Override
			public void onRedraw(CharInfo[] charInfos, BufferedImage bufferedImage) {
				Font2BitmapMain.this.onRedraw(charInfos, bufferedImage);
			}
		};

		if (guiMode) {
			if (debugTitleFloating)
				window.setTitle("floating");
			window.setVisible(true);
		}

		String fontName = null;

		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		try {
			Font font = Font.createFont(Font.TRUETYPE_FONT, inputFile);
			fontName = font.getName();
			ge.registerFont(font);
		} catch (FontFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		final String string = 
				"\n !\"#$%&'()*+,-./\\0123456789:;<=>@ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmno" + 
				"pqrstuvwxyz[]_{}АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮ" + 
				"Яабвгдеёжзийклмнопрстуфхцчшщъыьэюя?^~";

		final int fontStyle = Font.PLAIN | (bold ? Font.BOLD : Font.PLAIN) | (italic ? Font.ITALIC : Font.PLAIN);

		window.getCanvas().draw(string, new Font(fontName, fontStyle, fontSize), atlasWidth, atlasHeight);
	}

	private final void onRedraw(final CharInfo[] charInfos, final BufferedImage bufferedImage) {

		/*
		 * The BMF file format specification:
		 * 
		 * 1. short: the size of meta info 
		 * 2. meta info: 
		 * 		char: char 
		 * 		short: charX 
		 * 		short: charY 
		 * 		short: charWidth 
		 * 		short: charHeight 
		 * 			then repeats...
		 * 
		 * 3. PNG-data of atlas
		 */

		// Calculates the meta data info:

		int metaSize = 0;
		for (int i = 0; i < charInfos.length; i++) {
			if (charInfos[i] != null) {
				metaSize += Character.BYTES;
				metaSize += Short.BYTES * 4;
			}
		}

		System.out.println("metaSize: " + metaSize);

		// Writes data

		try {
			final DataOutputStream fo = new DataOutputStream(new FileOutputStream(new File(outputFilePath)));

			fo.writeShort(metaSize);

			for (int i = 0; i < charInfos.length; i++) {
				final CharInfo c = charInfos[i];
				if (c == null)
					break;

				fo.writeChar(c.character);
				fo.writeShort(c.x);
				fo.writeShort(c.y);
				fo.writeShort(c.width);
				fo.writeShort(c.height);
			}

			ImageIO.write(bufferedImage, "png", fo);

			ImageIO.write(bufferedImage, "png", new File("temp.png"));

			fo.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!guiMode)
			System.exit(0);

	}
}

package ru.ancevt.d2d2.font2bitmap;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MainReadTest {

	public static void main(String[] args) {
		try {
			final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(new File("out/consola.bmf")));
			
			final int metaSize = dataInputStream.readUnsignedShort();
			
			System.out.println("metaSize: " + metaSize);
			
			int metaSizeCounter = 0;
			
			while(dataInputStream.available() > 0) {
				final StringBuilder sb = new StringBuilder();
				
				sb.append("char: " 		+ dataInputStream.readChar() + " ");
				sb.append("x: " 		+ dataInputStream.readUnsignedShort() + " ");
				sb.append("y: " 		+ dataInputStream.readUnsignedShort() + " ");
				sb.append("width: " 	+ dataInputStream.readUnsignedShort() + " ");
				sb.append("height: " 	+ dataInputStream.readUnsignedShort() + " ");
				
				metaSizeCounter += Character.BYTES;
				metaSizeCounter += Short.BYTES * 4;

				System.out.println(sb.toString());
				
				if(metaSizeCounter >= metaSize) break;
			}
			
			dataInputStream.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

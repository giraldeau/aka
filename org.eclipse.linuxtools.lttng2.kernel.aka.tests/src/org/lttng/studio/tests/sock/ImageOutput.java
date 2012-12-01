package org.lttng.studio.tests.sock;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.junit.After;
import org.junit.Before;

public class ImageOutput {

	static int imgWidth = 1200;
	static int imgHeight = 400;
	static int border = 40;
	Image img;
	GC gc;

	@Before
	public void setup() {
		img = new Image(null, imgWidth, imgHeight);
		gc = new GC(img);
	}

	@After
	public void teardown() {
		img.dispose();
		gc.dispose();
	}

	public void saveImage(Image img, String name) {
		ImageLoader loader = new ImageLoader();
		loader.data = new ImageData[] { img.getImageData() };
		loader.save(name + ".png", SWT.IMAGE_PNG);
	}


}

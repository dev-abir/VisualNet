package org.devabir.dataset;

import org.devabir.common.GlArray;

import java.io.IOException;

public interface ImageDataset {
    /**
     * @return the name of the dataset
     */
    String getName();

    /**
     * @return the Download URL of the dataset
     */
    String getDownloadUrl();

    /**
     * Convert the raw dataset image files to a GlArray.
     * This should be called <b>after downloading</b> the dataset to local,
     * using some internal (private method) of the implementing class.
     * <p>
     * This array will probably be of shape: [nImages, width, height]
     *
     * @return GlArray representation of the images in the raw dataset
     */
    GlArray getImagesGlArray() throws IOException;

    /**
     * Convert the raw dataset labels to a GlArray.
     * This should be called <b>after downloading</b> the dataset to local,
     * using some internal (private method) of the implementing class.
     * <p>
     * This array will probably be of shape: [nImages]
     *
     * @return GlArray representation of the labels in the raw dataset
     */
    GlArray getLabelsGlArray() throws IOException;
}

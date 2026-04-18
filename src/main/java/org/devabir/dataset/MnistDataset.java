package org.devabir.dataset;

import org.devabir.common.GlArray;
import org.devabir.common.IdxFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.*;

public class MnistDataset implements ImageDataset {
    public static final String TRAIN_IMAGES_FILENAME = "train-images.idx3-ubyte";
    public static final String TRAIN_LABLES_FILENAME = "train-labels.idx1-ubyte";

    private final String kaggleUserName;
    private final String kaggleApiKey;
    private final String destinationDirectory;

    private final static Logger log = LoggerFactory.getLogger(MnistDataset.class);

    public MnistDataset(String kaggleUserName, String kaggleApiKey, String destinationDirectory) {
//        TODO: Externalize these things.
        this.kaggleUserName = kaggleUserName;
        this.kaggleApiKey = kaggleApiKey;
        this.destinationDirectory = destinationDirectory;
    }

    @Override
    public String getName() {
        return "MNIST Training";
    }

    @Override
    public String getDownloadUrl() {
        return "https://www.kaggle.com/api/v1/datasets/download/hojjatk/mnist-dataset";
    }

    @Override
    public GlArray getImagesGlArray() throws IOException {
        // String location = downloadDatasetToLocal();
        var idxFile = this.parseIdxFiles(TRAIN_IMAGES_FILENAME, 3);
        log.info("lol idx {} {}", idxFile.getMagicNumber(), idxFile.getDimensions());
        var data = idxFile.getData();
        var dimensions = idxFile.getDimensions();
        var nItems = dimensions[0];
        var nRows = dimensions[1];
        var nCols = dimensions[2];
        var targetIdx = 20;
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                float pixel = data[(targetIdx * nRows * nCols) + i * nCols + j];

//                System.out.print(pixel + " ");
                if (pixel > .2001)      System.out.print("#");
                else if (pixel > .1001) System.out.print("*");
                else if (pixel > .501)  System.out.print(".");
                else                  System.out.print(" ");
            }
            System.out.println();
        }
        return null;
        // return new GlArray("mnist_train_array", new int[]{2, 2, 2}, GL_DYNAMIC_DRAW);
    }

    public IdxFile hack() throws IOException {
        // String location = downloadDatasetToLocal();
        var idxFile = this.parseIdxFiles(TRAIN_IMAGES_FILENAME, 3);
        log.info("lol idx {} {}", idxFile.getMagicNumber(), idxFile.getDimensions());
//        var data = idxFile.getData();
//        var dimensions = idxFile.getDimensions();
//        var nItems = dimensions[0];
//        var nRows = dimensions[1];
//        var nCols = dimensions[2];
//        var targetIdx = 20;
//        for (int i = 0; i < nRows; i++) {
//            for (int j = 0; j < nCols; j++) {
//                float pixel = data[(targetIdx * nRows * nCols) + i * nCols + j];
//
////                System.out.print(pixel + " ");
//                if (pixel > .2001)      System.out.print("#");
//                else if (pixel > .1001) System.out.print("*");
//                else if (pixel > .501)  System.out.print(".");
//                else                  System.out.print(" ");
//            }
//            System.out.println();
//        }
        return idxFile;
    }

    @Override
    public GlArray getLabelsGlArray() throws IOException {
        return null;
    }

    private String downloadDatasetToLocal() throws IOException {
        var kClient = new KaggleClient(this.kaggleUserName, this.kaggleApiKey);
        kClient.downloadDataset(this.getDownloadUrl(), "mnist-dataset.zip");
        kClient.unzip("mnist-dataset.zip", this.destinationDirectory);
        return this.destinationDirectory;
    }

    /**
     *
     * @param filename    The IDX file name
     * @param nDimensions The dimensions of data (1 - for 1D data, 2 - for 2D and so on)
     * @return The extracted IDX file data along with its metadata
     * @throws IOException Resources:
     *                     https://github.com/sunsided/mnist
     *                     https://stackoverflow.com/a/20383900
     */
    private IdxFile parseIdxFiles(String filename, int nDimensions) throws IOException {
        try (
                var fis = new FileInputStream(this.destinationDirectory + File.separator + filename);
                var dis = new DataInputStream(fis)
        ) {
            int magicNumber = dis.readInt();
            // nElements is a part of the dimension only
            // int nElements = dis.readInt();

            int[] dimensions = new int[nDimensions];
            int dataSize = 1;
            for (int i = 0; i < nDimensions; i++) {
                dimensions[i] = dis.readInt();
                dataSize *= dimensions[i];
            }

            byte[] raw = new byte[dataSize];
            dis.readFully(raw);
            float[] data = new float[dataSize];
            for (int i = 0; i < dataSize; i++) {
                // NOTE: The dataset contains only unsigned bytes.
                // If we do data[i] / 255.0f directly - it'll cast to a float and
                // java will assume these bytes as negative if it starts with 1
                // raw[i] & 0xFF will cast it to 4 byte int and
                // fill the MSBs (first 3 bytes) with 0, thus it stays a positive integer.
                // Then, we are okay to divide and convert to float.
                // Alternative: data[i] = Byte.toUnsignedInt(raw[i]);
                data[i] = (raw[i] & 0xFF) / 255.0f;
            }

//            for (int i = 0; i < dataSize; i++) {
//                // NOTE: This data is an unsigned single byte.
//                // Probably & with OxFF is redundant (test++)
//                data[i] = (dis.readUnsignedByte() & 0xFF) / 255.0f;
//
//                if (i % updateInfoFreq == 0) log.info("Extracted {} bytes out of {}", i + 1, dataSize);
//            }

            return new IdxFile(magicNumber, dimensions, data);
        }
    }
}

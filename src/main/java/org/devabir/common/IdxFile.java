package org.devabir.common;

/**
 * Immutable representation of an IDX file.
 * <p>
 * IDX files are binary, big-endian data containers used by datasets such as MNIST.
 * The file consists of a magic number, element count, dimension metadata,
 * followed by raw unsigned byte data.
 * <p>
 * Resources:
 * https://github.com/sunsided/mnist
 * https://stackoverflow.com/a/20383900
 */
public final class IdxFile {

    /**
     * IDX magic number identifying the file type
     */
    private final int magicNumber;

    /**
     * Dimensions of each element (e.g. [nElements, 28, 28] for MNIST images)
     */
    private final int[] dimensions;

    /**
     * Normalized (0-1 range) IDX data stored as float
     */
    private final float[] data;

    /**
     * Creates a new immutable IDX file container.
     *
     * @param magicNumber  IDX magic number
     * @param dimensions   dimension sizes of each element (e.g. [nElements, 28, 28] for MNIST images)
     * @param data         Normalized (0-1 range) float data
     */
    public IdxFile(int magicNumber, int[] dimensions, float[] data) {
        this.magicNumber = magicNumber;
        this.dimensions = dimensions.clone();
        this.data = data;
    }

    public int getMagicNumber() {
        return magicNumber;
    }

    public int[] getDimensions() {
        return dimensions.clone();
    }

    public float[] getData() {
        return data;
    }
}



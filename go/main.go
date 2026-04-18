package main

import (
	"log"
	"math"
	"math/rand/v2"
	"runtime"
	"strconv"
	"unsafe"

	"archive/zip"
	"encoding/base64"
	"encoding/binary"
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"

	"github.com/joho/godotenv"

	"github.com/go-gl/gl/v4.3-core/gl"
	"github.com/go-gl/glfw/v3.3/glfw"
)

// func main() {
// 	err := glfw.Init()
// 	if err != nil {
// 		panic(err)
// 	}
// 	defer glfw.Terminate()

// 	glfw.WindowHint(glfw.Resizable, glfw.False)
// 	glfw.WindowHint(glfw.ContextVersionMajor, 4)
// 	glfw.WindowHint(glfw.ContextVersionMinor, 3)
// 	glfw.WindowHint(glfw.OpenGLProfile, glfw.OpenGLCoreProfile)
// 	glfw.WindowHint(glfw.OpenGLForwardCompatible, glfw.True)
// 	window, err := glfw.CreateWindow(640, 480, "Testing", nil, nil)
// 	if err != nil {
// 		panic(err)
// 	}

// 	// gl.BindBufferBase()

// 	window.MakeContextCurrent()
// 	glfw.SwapInterval(1)

// 	window.SetKeyCallback(func(w *glfw.Window, key glfw.Key, scancode int, action glfw.Action, mods glfw.ModifierKey) {
// 		if glfw.Press == action {
// 			switch key {
// 			case glfw.KeyEscape:
// 				window.SetShouldClose(true)
// 			}
// 		}
// 	})

// 	if err := gl.Init(); err != nil {
// 		log.Fatalln("failed to initialize OpenGL:", err)
// 	}

// 	for !window.ShouldClose() {
// 		gl.ClearColor(0.2, 0.2, 0.2, 0.2)
// 		gl.Clear(gl.COLOR_BUFFER_BIT | gl.DEPTH_BUFFER_BIT)
// 		// Do OpenGL stuff.
// 		window.SwapBuffers()
// 		glfw.PollEvents()
// 	}
// }

// ==================================================
// ==================================================
// Constants - will be loaded from .env file
// TODO: just pass around in functions rather than global?
// ==================================================
// ==================================================
type Config struct {
	KaggleUsername string
	KaggleKey      string
	MnistURL       string
	DataDir        string
}

type MNISTDataset struct {
	// Array of shape [nImages][28x28]
	Images [][]float32

	// Array of shape [nLabels]
	Labels []uint8
}

type MNIST struct {
	Train MNISTDataset
	Test  MNISTDataset
}

// func createLayerProgram(
// 	shaderPath string,
// 	layer int,
// 	inSize, outSize uint32,
// ) (uint32, error) {

// 	srcBytes, err := os.ReadFile(shaderPath)
// 	if err != nil {
// 		return 0, err
// 	}

// 	header := buildShaderHeader(map[string]string{
// 		"LAYER":    strconv.Itoa(layer),
// 		"IN_SIZE":  strconv.FormatUint(uint64(inSize), 10),
// 		"OUT_SIZE": strconv.FormatUint(uint64(outSize), 10),
// 	})

// 	fullSource := header + string(srcBytes)
// 	return createComputeProgram(fullSource)
// }

// func createSGDProgram(
// 	shaderPath string,
// 	layer int,
// 	weightSize, biasSize uint32,
// ) (uint32, error) {

// 	srcBytes, err := os.ReadFile(shaderPath)
// 	if err != nil {
// 		return 0, err
// 	}

// 	header := buildShaderHeader(map[string]string{
// 		"LAYER":       strconv.Itoa(layer),
// 		"WEIGHT_SIZE": strconv.FormatUint(uint64(weightSize), 10),
// 		"BIAS_SIZE":   strconv.FormatUint(uint64(biasSize), 10),
// 	})

// 	fullSource := header + string(srcBytes)
// 	return createComputeProgram(fullSource)
// }

// func createSoftmaxProgram(shaderPath string) (uint32, error) {
// 	srcBytes, err := os.ReadFile(shaderPath)
// 	if err != nil {
// 		return 0, err
// 	}

// 	header := "#version 430\n"
// 	return createComputeProgram(header + string(srcBytes))
// }

// func BuildLayer(
// 	layer int,
// 	inSize, outSize uint32,
// 	weights, biases uint32,
// 	activations uint32,
// 	dZ, dW, dB uint32,
// ) (*Layer, error) {

// 	forwardProg, err := createLayerProgram(
// 		"shaders/compute/forward.comp",
// 		layer,
// 		inSize,
// 		outSize,
// 	)
// 	if err != nil {
// 		return nil, err
// 	}

// 	backpropProg, err := createLayerProgram(
// 		"shaders/compute/backprop.comp",
// 		layer,
// 		inSize,
// 		outSize,
// 	)
// 	if err != nil {
// 		return nil, err
// 	}

// 	sgdProg, err := createSGDProgram(
// 		"shaders/compute/sgd.comp",
// 		layer,
// 		inSize*outSize,
// 		outSize,
// 	)
// 	if err != nil {
// 		return nil, err
// 	}

// 	return NewLayer(
// 		inSize,
// 		outSize,
// 		weights,
// 		biases,
// 		activations,
// 		dZ,
// 		dW,
// 		dB,
// 		forwardProg,
// 		backpropProg,
// 		sgdProg,
// 	), nil
// }

// func initNetwork() (*Network, error) {
// 	l1, err := BuildLayer(1, 28*28, 16*16, w1, b1, a1, dZ1, dW1, dB1)
// 	if err != nil {
// 		return nil, err
// 	}

// 	l2, err := BuildLayer(2, 16*16, 8*8, w2, b2, a2, dZ2, dW2, dB2)
// 	if err != nil {
// 		return nil, err
// 	}

// 	l3, err := BuildLayer(3, 8*8, 10, w3, b3, logits, dZ3, dW3, dB3)
// 	if err != nil {
// 		return nil, err
// 	}

// 	return &Network{Layers: []*Layer{l1, l2, l3}}, nil
// }

// func (l *Layer) Delete() {
// 	gl.DeleteProgram(l.ForwardProg)
// 	gl.DeleteProgram(l.BackpropProg)
// 	gl.DeleteProgram(l.SGDProg)
// }

// func (n *Network) Delete() {
// 	for _, l := range n.Layers {
// 		l.Delete()
// 	}
// }

func buildShaderHeader(defines map[string]int, version *string) string {
	header := "#version 430\n"
	if version != nil {
		header = "#version " + *version + "\n"
	}
	for k, v := range defines {
		header += "#define " + k + " " + strconv.Itoa(v) + "\n"
	}
	return header + "\n"
}

type Programs struct {
	Forward1  uint32
	Forward2  uint32
	Forward3  uint32
	Softmax   uint32
	Backprop3 uint32
	Backprop2 uint32
	Backprop1 uint32
	SGD3      uint32
	SGD2      uint32
	SGD1      uint32
}

func (p *Programs) Delete() {
	gl.UseProgram(0)

	gl.DeleteProgram(p.Forward1)
	gl.DeleteProgram(p.Forward2)
	gl.DeleteProgram(p.Forward3)

	gl.DeleteProgram(p.Softmax)

	gl.DeleteProgram(p.Backprop3)
	gl.DeleteProgram(p.Backprop2)
	gl.DeleteProgram(p.Backprop1)

	gl.DeleteProgram(p.SGD3)
	gl.DeleteProgram(p.SGD2)
	gl.DeleteProgram(p.SGD1)
}

func init() {
	// This is needed to arrange that main() runs on main thread (OpenGL and GLFW requirement).
	runtime.LockOSThread()
}

func main() {
	err := godotenv.Load()
	if err != nil {
		log.Panicln("Please define the settings as env. vars. or create a .env file!")
	}

	cfg := Config{
		KaggleUsername: os.Getenv("KAGGLE_USERNAME"),
		KaggleKey:      os.Getenv("KAGGLE_KEY"),
		MnistURL:       os.Getenv("MNIST_URL"),
		DataDir:        os.Getenv("DATA_DIR"),
	}

	mnist := prepareDataset(&cfg)
	images := mnist.Train.Images
	// log.Printf("Shape: (%d, %d)\n", len(images), len(images[0]))
	// log.Println(images[0])

	testId := 2

	for i := range 28 {
		for j := range 28 {
			if images[testId][i*28+j] > 0.5 {
				fmt.Printf("%c ", '█')
			} else {
				fmt.Printf("%c ", ' ')
			}
		}
		fmt.Println()
	}

	log.Println(mnist.Train.Labels[testId])

	if err := glfw.Init(); err != nil {
		log.Fatalln(err)
	}
	defer glfw.Terminate()

	glfw.WindowHint(glfw.Resizable, glfw.False)
	glfw.WindowHint(glfw.ContextVersionMajor, 4)
	glfw.WindowHint(glfw.ContextVersionMinor, 3)
	glfw.WindowHint(glfw.OpenGLProfile, glfw.OpenGLCoreProfile)
	glfw.WindowHint(glfw.OpenGLForwardCompatible, glfw.True)

	glfw.WindowHint(glfw.Visible, glfw.False)
	window, _ := glfw.CreateWindow(500, 500, "Test", nil, nil)
	window.MakeContextCurrent()

	if err := gl.Init(); err != nil {
		log.Fatalln(err)
	}
	defer gl.Finish()

	numImages := len(mnist.Train.Images)
	numLabels := len(mnist.Train.Labels)
	const imageSize = 28 * 28
	const h1Size = 16 * 16
	const h2Size = 8 * 8
	const logitsSize = 10

	const batchSize = 64

	w1 := make([]float32, imageSize*h1Size)
	w2 := make([]float32, h1Size*h2Size)
	w3 := make([]float32, h2Size*logitsSize)

	b1 := make([]float32, h1Size)
	b2 := make([]float32, h2Size)
	b3 := make([]float32, logitsSize)

	// Random weights and biases
	heInit(w1, imageSize)
	heInit(w2, h1Size)
	// heInit(w3, h2Size)
	xavierInit(w3, h2Size)

	// for i := range w1 {
	// 	w1[i] = float32(i)
	// }
	// for i := range w2 {
	// 	w2[i] = float32(i)
	// }
	// for i := range w3 {
	// 	w3[i] = float32(i)
	// }

	// biases should be initiallized to 0, by default

	allImages := make([]float32, numImages*imageSize)

	for i := range numImages {
		copy(allImages[i*imageSize:], mnist.Train.Images[i])
	}

	/*
		OpenGL buffer usage hints:

		DRAW  : CPU writes → GPU reads (weights, uploads)
		READ  : GPU writes → CPU reads (results back to CPU)
		COPY  : GPU writes → GPU reads (activations, gradients)

		Suffixes: STATIC = once, DYNAMIC = sometimes, STREAM = every frame
	*/
	// Create buffers
	inputBuf := createSSBO(0, len(allImages)*4, gl.Ptr(allImages), gl.STATIC_DRAW)
	w1Buf := createSSBO(1, len(w1)*4, gl.Ptr(w1), gl.DYNAMIC_DRAW)
	w2Buf := createSSBO(2, len(w2)*4, gl.Ptr(w2), gl.DYNAMIC_DRAW)
	w3Buf := createSSBO(3, len(w3)*4, gl.Ptr(w3), gl.DYNAMIC_DRAW)
	b1Buf := createSSBO(4, len(b1)*4, gl.Ptr(b1), gl.DYNAMIC_DRAW)
	b2Buf := createSSBO(5, len(b2)*4, gl.Ptr(b2), gl.DYNAMIC_DRAW)
	b3Buf := createSSBO(6, len(b3)*4, gl.Ptr(b3), gl.DYNAMIC_DRAW)
	a1Buf := createSSBO(7, int(batchSize)*h1Size*4, nil, gl.DYNAMIC_COPY)
	a2Buf := createSSBO(8, int(batchSize)*h2Size*4, nil, gl.DYNAMIC_COPY)
	logitsBuf := createSSBO(9, int(batchSize)*logitsSize*4, nil, gl.DYNAMIC_COPY)
	labelsBuf := createSSBO(10, numLabels*1, gl.Ptr(mnist.Train.Labels), gl.STATIC_DRAW)
	gradBuf := createSSBO(11, int(batchSize)*logitsSize*4, nil, gl.DYNAMIC_COPY)
	probabilitiesBuf := createSSBO(12, int(batchSize)*logitsSize*4, nil, gl.STATIC_READ)
	dZ2Buf := createSSBO(13, int(batchSize)*logitsSize*4, nil, gl.DYNAMIC_COPY)
	dZ1Buf := createSSBO(14, int(batchSize)*logitsSize*4, nil, gl.DYNAMIC_COPY)
	dWBuf := createSSBO(15, int(batchSize)*logitsSize*4, nil, gl.DYNAMIC_COPY)
	dBBuf := createSSBO(16, int(batchSize)*logitsSize*4, nil, gl.DYNAMIC_COPY)

	// Nuke them
	buffers := []uint32{
		inputBuf, w1Buf, w2Buf, w3Buf,
		b1Buf, b2Buf, b3Buf,
		a1Buf, a2Buf, logitsBuf, labelsBuf,
		gradBuf, probabilitiesBuf,
		dZ2Buf, dZ1Buf, dWBuf, dBBuf,
	}
	defer gl.DeleteBuffers(int32(len(buffers)), &buffers[0])

	// defer gl.DeleteBuffers(1, &inputBuf)
	// defer gl.DeleteBuffers(1, &w1Buf)
	// defer gl.DeleteBuffers(1, &w2Buf)
	// defer gl.DeleteBuffers(1, &w3Buf)
	// defer gl.DeleteBuffers(1, &a1Buf)
	// defer gl.DeleteBuffers(1, &a2Buf)
	// defer gl.DeleteBuffers(1, &logitsBuf)
	// defer gl.DeleteBuffers(1, &labelsBuf)
	// defer gl.DeleteBuffers(1, &gradBuf)
	// defer gl.DeleteBuffers(1, &probabilitiesBuf)
	// defer gl.DeleteBuffers(1, &dZ2Buf)
	// defer gl.DeleteBuffers(1, &dZ1Buf)
	// defer gl.DeleteBuffers(1, &dWBuf)
	// defer gl.DeleteBuffers(1, &dBBuf)

	// To ignore the golang unused var error
	// In case if you use them in future,
	// just comment out one of these lines.
	// _ = inputBuf
	// _ = w1Buf
	// _ = w2Buf
	// _ = w3Buf
	// _ = b1Buf
	// _ = b2Buf
	// _ = b3Buf
	// _ = a1Buf
	// _ = a2Buf
	// _ = labelsBuf
	// _ = gradBuf
	// _ = probabilitiesBuf
	// _ = dZ2Buf
	// _ = dZ1Buf
	// _ = dWBuf
	// _ = dBBuf

	// shaderForward1, err := createComputeProgram(filepath.Join("shaders", "compute", "forward.comp"), 1)

	programs, err := createAllPrograms(imageSize, h1Size, h2Size, logitsSize, batchSize, 0.01)
	if err != nil {
		panic(err)
	}
	defer programs.Delete()

	batchIdxLocs := [3]int32{
		gl.GetUniformLocation(programs.Forward1, gl.Str("batchIdx\x00")),
		gl.GetUniformLocation(programs.Forward2, gl.Str("batchIdx\x00")),
		gl.GetUniformLocation(programs.Forward3, gl.Str("batchIdx\x00")),
	}

	for epoch := range 3 {
		for i := range len(mnist.Train.Images) / batchSize {
			forward(programs, batchIdxLocs, i, batchSize)

			gl.UseProgram(programs.Softmax)
			gl.DispatchCompute(uint32(batchSize), 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			clearFloatSSBO(dWBuf)
			clearFloatSSBO(dBBuf)
			clearFloatSSBO(dZ1Buf)
			clearFloatSSBO(dZ2Buf)

			gl.UseProgram(programs.Backprop1)
			gl.DispatchCompute(uint32(logitsSize), 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			gl.UseProgram(programs.Backprop2)
			gl.DispatchCompute(1, 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			gl.UseProgram(programs.Backprop3)
			gl.DispatchCompute(4, 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			gl.UseProgram(programs.SGD3)
			gl.DispatchCompute(uint32(batchSize), 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			gl.UseProgram(programs.SGD2)
			gl.DispatchCompute(uint32(batchSize), 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			gl.UseProgram(programs.SGD1)
			gl.DispatchCompute(uint32(batchSize), 1, 1)
			gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

			gl.Finish()
			// log.Println("Batch", i + 1, "done")
		}

		log.Println("Epoch", epoch+1, "done")
	}

	testImage := make([]float32, 1*imageSize)
	copy(testImage, mnist.Test.Images[0])
	// gl.BufferSubData(gl.SHADER_STORAGE_BUFFER, 0, len(input)*4, gl.Ptr(input))
	gl.BufferData(gl.SHADER_STORAGE_BUFFER, imageSize, gl.Ptr(testImage), gl.DYNAMIC_DRAW)

	forward(programs, batchIdxLocs, 0, 1)

	gl.UseProgram(programs.Softmax)
	gl.DispatchCompute(uint32(batchSize), 1, 1)
	gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

	// Read output
	gl.BindBuffer(gl.SHADER_STORAGE_BUFFER, probabilitiesBuf)
	ptr := gl.MapBuffer(gl.SHADER_STORAGE_BUFFER, gl.READ_ONLY)
	final := (*[10]float32)(ptr)
	// for i := range 16 {
	// 	for j := range 16 {
	// 		fmt.Printf("%2f ", math.Round(10000.0 * float64(final[i*16+j])) / 10000)
	// 	}
	// 	fmt.Println()
	// }
	log.Println("Raw output:", final, "Actual:", mnist.Test.Labels[0])
	gl.UnmapBuffer(gl.SHADER_STORAGE_BUFFER)
}

func clearFloatSSBO(buffer uint32) {
	gl.BindBuffer(gl.SHADER_STORAGE_BUFFER, buffer)
	zero := 0.0
	gl.ClearBufferData(gl.SHADER_STORAGE_BUFFER, gl.R32F, gl.RED, gl.FLOAT, gl.Ptr(&zero))
	gl.BindBuffer(gl.SHADER_STORAGE_BUFFER, 0)
}

func forward(programs *Programs, batchIdxLocs [3]int32, batchId int, batchSize int) {
	// Layer 1
	gl.UseProgram(programs.Forward1)
	gl.Uniform1ui(batchIdxLocs[0], uint32(batchId))
	gl.DispatchCompute(4, uint32(batchSize), 1) // (4 * 64) threads = (16 * 16) neurons across x
	gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

	// Layer 2
	gl.UseProgram(programs.Forward2)
	gl.Uniform1ui(batchIdxLocs[1], uint32(batchId))
	gl.DispatchCompute(1, uint32(batchSize), 1) // (1 * 64) threads = (8 * 8) neurons across x
	gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)

	// Layer 3
	gl.UseProgram(programs.Forward3)
	gl.Uniform1ui(batchIdxLocs[2], uint32(batchId))
	gl.DispatchCompute(1, uint32(batchSize), 1) // 256 threads, only 10 used across x
	gl.MemoryBarrier(gl.SHADER_STORAGE_BARRIER_BIT)
}

func createSSBO(binding uint32, size int, data unsafe.Pointer, usage uint32) uint32 {
	var buffer uint32
	gl.GenBuffers(1, &buffer)
	gl.BindBuffer(gl.SHADER_STORAGE_BUFFER, buffer)
	gl.BufferData(gl.SHADER_STORAGE_BUFFER, size, data, usage)
	gl.BindBufferBase(gl.SHADER_STORAGE_BUFFER, binding, buffer)
	gl.BindBuffer(gl.SHADER_STORAGE_BUFFER, 0)

	return buffer
}

func createAllPrograms(inputSize int, hidden1Size int, hidden2Size int, logitsSize int, batchSize int, learningRate float32) (*Programs, error) {
	var err error
	p := &Programs{}

	header := buildShaderHeader(map[string]int{
		"LAYER":    1,
		"IN_SIZE":  inputSize,
		"OUT_SIZE": hidden1Size,
	}, nil)
	p.Forward1, err = createComputeProgram(filepath.Join("shaders", "compute", "forward.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Forward1)
	batchSizeLoc := gl.GetUniformLocation(p.Forward1, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))
	header = buildShaderHeader(map[string]int{
		"LAYER":    2,
		"IN_SIZE":  hidden1Size,
		"OUT_SIZE": hidden2Size,
	}, nil)
	p.Forward2, err = createComputeProgram(filepath.Join("shaders", "compute", "forward.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Forward2)
	batchSizeLoc = gl.GetUniformLocation(p.Forward2, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))
	header = buildShaderHeader(map[string]int{
		"LAYER":    3,
		"IN_SIZE":  hidden2Size,
		"OUT_SIZE": logitsSize,
	}, nil)
	p.Forward3, err = createComputeProgram(filepath.Join("shaders", "compute", "forward.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Forward3)
	batchSizeLoc = gl.GetUniformLocation(p.Forward3, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))

	p.Softmax, err = createComputeProgram(filepath.Join("shaders", "compute", "softmax.comp"), nil)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Softmax)
	batchSizeLoc = gl.GetUniformLocation(p.Softmax, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))
	logitsSizeLoc := gl.GetUniformLocation(p.Softmax, gl.Str("logitsSize\x00"))
	gl.Uniform1ui(logitsSizeLoc, uint32(logitsSize))

	header = buildShaderHeader(map[string]int{
		"LAYER":    3,
		"IN_SIZE":  logitsSize,
		"OUT_SIZE": hidden2Size,
	}, nil)
	p.Backprop1, err = createComputeProgram(filepath.Join("shaders", "compute", "backprop.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Backprop3)
	batchSizeLoc = gl.GetUniformLocation(p.Backprop3, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))
	header = buildShaderHeader(map[string]int{
		"LAYER":    2,
		"IN_SIZE":  hidden2Size,
		"OUT_SIZE": hidden1Size,
	}, nil)
	p.Backprop2, err = createComputeProgram(filepath.Join("shaders", "compute", "backprop.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Backprop2)
	batchSizeLoc = gl.GetUniformLocation(p.Backprop2, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))
	header = buildShaderHeader(map[string]int{
		"LAYER":    1,
		"IN_SIZE":  hidden1Size,
		"OUT_SIZE": inputSize,
	}, nil)
	p.Backprop1, err = createComputeProgram(filepath.Join("shaders", "compute", "backprop.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.Backprop1)
	batchSizeLoc = gl.GetUniformLocation(p.Backprop1, gl.Str("batchSize\x00"))
	gl.Uniform1ui(batchSizeLoc, uint32(batchSize))

	header = buildShaderHeader(map[string]int{
		"LAYER":       3,
		"WEIGHT_SIZE": hidden2Size * logitsSize,
		"BIAS_SIZE":   logitsSize,
	}, nil)
	p.SGD3, err = createComputeProgram(filepath.Join("shaders", "compute", "sgd.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.SGD3)
	learningRateLoc := gl.GetUniformLocation(p.SGD3, gl.Str("learningRate\x00"))
	gl.Uniform1f(learningRateLoc, float32(learningRate))
	header = buildShaderHeader(map[string]int{
		"LAYER":       2,
		"WEIGHT_SIZE": hidden1Size * hidden2Size,
		"BIAS_SIZE":   hidden2Size,
	}, nil)
	p.SGD2, err = createComputeProgram(filepath.Join("shaders", "compute", "sgd.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.SGD2)
	learningRateLoc = gl.GetUniformLocation(p.SGD2, gl.Str("learningRate\x00"))
	gl.Uniform1f(learningRateLoc, float32(learningRate))
	header = buildShaderHeader(map[string]int{
		"LAYER":       1,
		"WEIGHT_SIZE": hidden1Size * inputSize,
		"BIAS_SIZE":   hidden1Size,
	}, nil)
	p.SGD1, err = createComputeProgram(filepath.Join("shaders", "compute", "sgd.comp"), &header)
	if err != nil {
		return nil, err
	}
	gl.UseProgram(p.SGD1)
	learningRateLoc = gl.GetUniformLocation(p.SGD1, gl.Str("learningRate\x00"))
	gl.Uniform1f(learningRateLoc, float32(learningRate))

	gl.UseProgram(0)

	return p, nil
}

func xavierInit(weights []float32, fanIn int) {
	std := float32(math.Sqrt(1.0 / float64(fanIn)))
	for i := range weights {
		weights[i] = float32(rand.NormFloat64() * float64(std))
	}
}

func heInit(weights []float32, fanIn int) {
	std := float32(math.Sqrt(2.0 / float64(fanIn)))
	for i := range weights {
		weights[i] = float32(rand.NormFloat64() * float64(std))
	}
}

func createComputeProgram(path string, header *string) (uint32, error) {
	source, err := readFile(path)
	if err != nil {
		return 0, err
	}

	if header != nil {
		source = *header + source
	}

	shader, err := compileShader(source, gl.COMPUTE_SHADER)
	if err != nil {
		return 0, err
	}

	program := gl.CreateProgram()
	gl.AttachShader(program, shader)
	gl.LinkProgram(program)

	gl.DeleteShader(shader) // safe after linking

	var status int32
	gl.GetProgramiv(program, gl.LINK_STATUS, &status)
	if status == gl.FALSE {
		var logLength int32
		gl.GetProgramiv(program, gl.INFO_LOG_LENGTH, &logLength)

		buf := make([]byte, logLength)
		gl.GetProgramInfoLog(program, logLength, nil, &buf[0])
		panic(string(buf))
	}

	return program, nil
}

func compileShader(source string, shaderType uint32) (uint32, error) {
	shader := gl.CreateShader(shaderType)

	csources, free := gl.Strs(source + "\x00")
	gl.ShaderSource(shader, 1, csources, nil)
	free()

	gl.CompileShader(shader)

	var status int32
	gl.GetShaderiv(shader, gl.COMPILE_STATUS, &status)
	if status == gl.FALSE {
		var logLength int32
		gl.GetShaderiv(shader, gl.INFO_LOG_LENGTH, &logLength)

		buf := make([]byte, logLength)
		gl.GetShaderInfoLog(shader, logLength, nil, &buf[0])
		panic(string(buf))
	}

	return shader, nil
}

func readFile(path string) (string, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return "", err
	}
	return string(data), nil
}

func prepareDataset(cfg *Config) MNIST {
	err := os.MkdirAll(cfg.DataDir, os.ModePerm)
	if err != nil {
		panic(err)
	}

	zipPath := filepath.Join(cfg.DataDir, "mnist.zip")

	if datasetExists(cfg.DataDir) {
		log.Println("Dataset already extracted. Skipping download.")
	} else {
		if fileExists(zipPath) {
			log.Println("Zip file already exists. Skipping download.")
		} else {
			log.Println("Downloading dataset...")
			downloadFile(cfg.MnistURL, cfg.KaggleUsername, cfg.KaggleKey, zipPath)
		}

		log.Println("Extracting dataset...")
		unzip(zipPath, cfg.DataDir)
	}

	mnist := MNIST{
		Train: MNISTDataset{
			Images: parseImages(filepath.Join(cfg.DataDir, "train-images.idx3-ubyte")),
			Labels: parseLabels(filepath.Join(cfg.DataDir, "train-labels.idx1-ubyte")),
		},
		Test: MNISTDataset{
			Images: parseImages(filepath.Join(cfg.DataDir, "t10k-images.idx3-ubyte")),
			Labels: parseLabels(filepath.Join(cfg.DataDir, "t10k-labels.idx1-ubyte")),
		},
	}

	log.Printf(
		"MNIST loaded: train=%d samples (%dx%d), test=%d samples",
		len(mnist.Train.Images),
		28, 28,
		len(mnist.Test.Images),
	)

	return mnist
}

func datasetExists(dir string) bool {
	requiredFiles := []string{
		"train-images.idx3-ubyte",
		"train-labels.idx1-ubyte",
		"t10k-images.idx3-ubyte",
		"t10k-labels.idx1-ubyte",
	}

	for _, f := range requiredFiles {
		if !fileExists(filepath.Join(dir, f)) {
			return false
		}
	}
	return true
}

func fileExists(path string) bool {
	_, err := os.Stat(path)
	return err == nil
}

func downloadFile(url, username, key, output string) {
	req, _ := http.NewRequest("GET", url, nil)

	credentials := username + ":" + key
	auth := "Basic " + base64.StdEncoding.EncodeToString([]byte(credentials))
	req.Header.Set("Authorization", auth)

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		panic(err)
	}
	defer resp.Body.Close()

	out, _ := os.Create(output)
	defer out.Close()

	io.Copy(out, resp.Body)

	log.Println("Download done")
}

func unzip(src, dest string) {
	r, err := zip.OpenReader(src)
	if err != nil {
		panic(err)
	}
	defer r.Close()

	for _, f := range r.File {
		path := filepath.Join(dest, f.Name)

		rc, _ := f.Open()
		out, _ := os.Create(path)

		io.Copy(out, rc)

		out.Close()
		rc.Close()
	}

	log.Println("Extraction done")
}

func parseImages(path string) [][]float32 {
	file, err := os.Open(path)
	if err != nil {
		panic(err)
	}
	defer file.Close()

	// MNIST Parsing resources:
	// https://github.com/sunsided/mnist
	// https://stackoverflow.com/a/20383900

	var magic, num, rows, cols uint32

	binary.Read(file, binary.BigEndian, &magic)
	binary.Read(file, binary.BigEndian, &num)
	binary.Read(file, binary.BigEndian, &rows)
	binary.Read(file, binary.BigEndian, &cols)

	size := int(rows * cols)
	totalBytes := int(num) * size

	raw := make([]byte, totalBytes)
	if _, err := io.ReadFull(file, raw); err != nil {
		panic(err)
	}

	images := make([][]float32, num)

	// Convert bytes → float32
	for i := 0; i < int(num); i++ {
		start := i * size
		end := start + size

		img := make([]float32, size)
		for j, b := range raw[start:end] {
			// Normalize [0-255] -> [0-1]
			img[j] = float32(b) * (1.0 / 255.0)
		}
		images[i] = img
	}

	return images
}

func parseLabels(path string) []uint8 {
	file, err := os.Open(path)
	if err != nil {
		panic(err)
	}
	defer file.Close()

	var magic, num uint32

	binary.Read(file, binary.BigEndian, &magic)
	binary.Read(file, binary.BigEndian, &num)

	labels := make([]uint8, num)

	if _, err := io.ReadFull(file, labels); err != nil {
		panic(err)
	}

	return labels
}

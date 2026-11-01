# Image Averaging with Concurrency

Pixelates a JPG image by replacing every square block with its average color.
The processing is shown live in a window, square by square, and can run
single-threaded or multi-threaded.

## Requirements

- Java 21 or newer (JDK, not only a JRE). Check with `java -version` and `javac -version`.
- No external libraries. Works on Windows, macOS and Linux.

## How to run

From the project root folder:

```
javac -d out -sourcepath src src/Main.java
java -cp out Main <file.jpg> <square size> <S|M>
```

Example:

```
java -cp out Main photo.jpg 20 M
```

| Argument      | Meaning                                                 |
|---------------|---------------------------------------------------------|
| `file.jpg`    | Path to the input JPG image                             |
| `square size` | Side of the averaging square in pixels (positive integer) |
| `S` or `M`    | `S` = single-threaded, `M` = multi-threaded             |

The result is saved as `result.jpg` in the folder you run the command from.
The window stays open after processing; close it to exit.

For very large images, give Java more memory: `java -Xmx4g -cp out Main photo.jpg 20 M`

## How it works

### Averaging
For each square, the red, green and blue values of all its pixels are summed
separately and divided by the number of pixels. The whole square is then filled
with that average color. Squares at the right and bottom edges are smaller when
the image size is not a multiple of the square size.

### Single-threaded mode (S)
One thread processes the squares from left to right, top to bottom.

### Multi-threaded mode (M)
The image is split into horizontal bands, one per CPU core
(`Runtime.availableProcessors()`). Each band is processed by its own thread,
left to right and top to bottom, so several regions fill in at the same time.

- **Band boundaries follow the square grid**, so a square is never split
  between two threads.
- **No locks are needed**: each thread writes only to the pixels of its own
  band, so threads never modify the same data.
- The main thread waits for all workers with `join()` before saving the result.
  This also guarantees that all pixel writes made by the workers are visible to it.
- The number of threads never exceeds the number of rows of squares
  (e.g. a large square size may need only 2 threads).

### Display
- The window shows the image scaled to fit the usable screen area if it is
  larger than the screen. **The processing always works on the original
  full-size image**; only the drawing is scaled.
- Processing runs outside Swing's UI thread, so the window stays responsive.
  Worker threads only call `repaint()`, which is thread-safe; all drawing is
  done by Swing's UI thread. This keeps the program safe on macOS.
- Processing is deliberately slowed down so the progress is visible: a
  single-threaded run takes about 15 seconds regardless of image size. The
  multi-threaded mode uses the same per-square pace in every thread, so it
  finishes about N times faster with N threads.

## Example timing

Same image, square size 20, on a 16-thread CPU:

| Mode | Time   |
|------|--------|
| S    | 15.00 s |
| M    | 1.09 s  |

These times include the visualization delay described above.

## Project structure

```
src/
  Main.java            Argument parsing, window setup, saving the result
  ImagePanel.java      Draws the image scaled to the window size
  ImageProcessor.java  Square averaging, single- and multi-threaded processing
```
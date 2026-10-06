# 3D fractal viewer

A **JavaFX** desktop app that generates fractals and displays them on a **3D sphere or plane** you can rotate, with image export.

> School project, DUT Informatique, University of Bordeaux (2017), by Laurent Genty and Luis Palluel.

## Features

- Three fractals: **Mandelbrot set**, **Lévy C curve**, and a recursive **square** fractal.
- Configurable parameters (iterations, size…) with input validation.
- Fractal rendered as a texture on a **3D sphere** or on a flat plane, rotatable with the mouse.
- Export of the generated image to PNG.

## Architecture

Classic **MVC**:

```
PalluelGenty/src/
  application/Application.java   # entry point
  modele/Modele.java             # model (observable)
  vue/SphereViewer.java          # JavaFX view: 3D scene, controls
  controleur/                    # one controller per fractal + MainController
  exceptions/                    # input validation errors
```

## Getting started

Requirements: Java 8 with JavaFX (or a recent JDK + [OpenJFX](https://openjfx.io/)).

The project was developed with Eclipse: import `PalluelGenty/` as an existing project, then run `application.Application`.

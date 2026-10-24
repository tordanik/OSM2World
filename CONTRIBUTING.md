# Contributing to OSM2World

Thank you for your interest in OSM2World! OSM2World turns OpenStreetMap data into 3D models of the world, and it has grown over more than a decade thanks to the help of many people. Contributions of all kinds are welcome, and you do not need to be a Java developer to make a difference.

This document gives you an overview of the ways you can help and explains how to get started.

- Project website and documentation: <https://osm2world.org/>
- Source code and issue tracker: <https://github.com/tordanik/OSM2World>
- Default visual style (textures, models, configuration): <https://github.com/tordanik/OSM2World-default-style>

## Ways to contribute

### Without writing code

**Report bugs and rendering problems.**
If something looks wrong in OSM2World's output, please [open an issue](https://github.com/tordanik/OSM2World/issues). The most helpful reports include:

- the OSM element(s) involved (e.g. a link like `https://www.openstreetmap.org/way/123456`) or the coordinates of the area,
- what you expected and what you see instead (a screenshot helps a lot),
- how you ran OSM2World (version, command line, config file, output format), and any error messages or log output.

Before reporting, it is worth checking whether the OSM data itself is correct and up to date. A small `.osm` file that reproduces the problem is ideal.

**Suggest features.**
Is there a type of object OSM2World doesn't render yet, or an OSM tag it ignores? Open an issue describing the feature and link to the relevant [OpenStreetMap wiki](https://wiki.openstreetmap.org/) page for the tags involved.

**Improve the map data.**
OSM2World can only be as good as the data it gets. Adding 3D-relevant information to OpenStreetMap – building heights, roof shapes, levels, colors and materials (see [Simple 3D Buildings](https://wiki.openstreetmap.org/wiki/Simple_3D_Buildings)), or details like street furniture, trees and barriers – improves OSM2World and every other 3D renderer at the same time.

**Create textures and 3D models.**
The look of OSM2World comes from its default style, which lives in a [separate repository](https://github.com/tordanik/OSM2World-default-style). It contains a configuration file, textures and glTF models. If you create textures or models, you can contribute them there – please make sure you have the right to release them under CC0.

**Add country-specific details.**
Things like traffic signs and the driving side differ between countries. The default style's `locales` directory contains country-specific configuration files that map traffic sign names to their national designs. Adding a locale for your own country, along with matching sign textures, is a great way to contribute local knowledge.

**Use and share.**
Try OSM2World on the area you know best, with different output formats and use cases. Feedback about what works and what doesn't will help the project. So will showing off what you've created!

### Writing code

Code contributions are submitted as pull requests on GitHub. For larger changes, please open an issue first (or comment on an existing one) to discuss your idea. This avoids duplicated work and makes it more likely that your change fits well into the project.

Good first steps include introducing support for additional OSM tags, or fixing a rendering bug you've found in your own area.

## Getting started with the code

### Prerequisites

- Java 17 (JDK)
- Maven
- Git 
 
Any Java IDE (IntelliJ IDEA, Eclipse, ...) can import the project as a Maven project.

### Building

```
git clone https://github.com/tordanik/OSM2World.git
cd OSM2World
mvn package
```

This compiles all modules and runs the tests. The runnable desktop application ends up in `desktop/target/` (with its dependencies in `desktop/target/lib/`):

```
java -jar desktop/target/osm2world-desktop-<version>.jar --help
```

To get output that looks like the official OSM2World renders, you will also want the [default style](https://github.com/tordanik/OSM2World-default-style).
Place it into OSM2World's root directory or pass the location of its configuration file with `--config`. Local style files, textures and models in the project root are ignored by Git, so you can keep them in your working copy.

### Project structure

OSM2World is a multi-module Maven project:

| Module      | Purpose |
|-------------|---------|
| `core`      | The platform-independent core library: reading map data, building the 3D world, and most output formats. |
| `core-jvm`  | JVM-specific additions to the core (e.g. file formats and image handling). **Also contains the tests for the core.** |
| `core-web`  | A variant of the core that is compiled to run in web browsers (using [TeaVM](https://teavm.org/)). |
| `opengl`    | OpenGL rendering via JOGL, used for the viewer and image output. |
| `desktop`   | The command line interface and the graphical viewer. |

Because `core` also runs in the browser, functionality that depends on a specific platform is placed behind interfaces in `org.osm2world.util.platform` and implemented separately in `core-jvm` and `core-web`.

Within the core (`org.osm2world`), the conversion roughly flows through these packages:

1. **`osm`** – reading raw OpenStreetMap data from various sources.
2. **`map_data`** – OSM2World's own representation of the map: nodes, way segments and areas with their tags.
3. **`map_elevation`** – elevation data and terrain.
4. **`world`** – the 3D world. **`world.modules`** contains the *modules* that turn map elements into 3D objects: `BuildingModule`, `RoadModule`, `TreeModule`, `StreetFurnitureModule`, `WaterModule` and many more. This is where most new features are added.
5. **`scene`** – meshes, materials, textures and models that make up the final scene.
6. **`output`** – writing the scene to a format such as glTF/glb, OBJ, POV-Ray or 3D tiles.

The entry point for using OSM2World as a library is `O2WConverter` (in `core-jvm`), which builds on `O2WConverterImpl` in `core`. `O2WConverterImpl` also contains the list of modules that are active by default. Visual appearance (materials, textures, etc.) is controlled by the style configuration, see the `style` package.

### Adding support for a new feature

Most features are implemented inside a module in `org.osm2world.world.modules`. A module extends `AbstractModule` and overrides methods such as `applyToNode`, `applyToWaySegment` or `applyToArea`. There, it checks the element's tags and attaches a *representation* – a world object that knows how to build its own geometry. You can look at existing `WorldObject` implementations to see how they work. (Best start with a small one.)

### Code style

Please follow the style of the surrounding code:

- Indent with tabs.
- Use descriptive names and add Javadoc comments for classes and non-obvious methods.
- Keep changes focused. Unrelated reformatting or refactoring makes pull requests harder to review.

### Tests

Tests use JUnit 4. Tests for the `core` module live in `core-jvm/src/test/java`, mirroring the package structure of the code under test. `MapDataBuilder` makes it easy to set up small test datasets in code, and `org.osm2world.test.TestUtil` contains helpful assertions. Please add tests for new functionality and bug fixes where it's practical, and make sure `mvn package` succeeds before you submit. CI runs the same build for every push.

### Submitting a pull request

1. Fork the repository and create a branch for your change, based on `master`.
2. Make your changes in small, logical commits with clear commit messages following the style of other messages in this repo.
3. Make sure the project builds and all tests pass.
4. Open a pull request on GitHub describing what you changed and why. Link related issues, and add screenshots for visual changes.

Expect some discussion and possibly requests for changes during review, that's a normal part of the process.

## Questions?

If you are unsure about anything, reach out! For ways to get in touch, see the [contact page](https://osm2world.org/contact) on the project website.

Thank you for helping to build an open 3D model of the world!

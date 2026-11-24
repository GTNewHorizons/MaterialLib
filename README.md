# MaterialLib

A library for easily adding and managing material-based items.
Designed to be highly flexible and crossmod-friendly.

## Primitives
MaterialLib's design is built on several primitives:
- **Material:** A named substance, like Iron or Gold, that
carries Properties and generates Shapes.
- **Shape:** Represents an actual item, block, or fluid that is
made of a Material. Shapes may possess Variants (such as ores of
different stones), an abstraction for easier handling of nearly
identical Shapes.
- **Family:** A collection of Materials, useful for sharing
Shape generation or Properties among Materials.
- **Property:** A feature of a Material, used however the mod
needs, such as processing voltage for recipes.
- **ShapeConsumer:** A callback that runs once for every (Shape,
Material) pair for a given Shape. Useful for recipe or tool
generation.

## API
Mods enter the API through `MaterialLibAPI` or
`MaterialLibClient`. `MaterialLibAPI` provides builders and
registration methods for the primitives, while
`MaterialLibClient` handles certain client behaviors like
renderer registration. For most simple Shapes, the builders
should suffice; however, more complex Shapes can extend
`ShapeItem`/`ShapeBlock`/`ShapeFluid`/`ShapeFluidInContainer` to
add custom behavior and register them through the API.

Registrations are handled through a `MaterialRegistrationEvent`
handler. Subscribe the handler during `FMLConstructionEvent` and
declare `required-after:materiallib`. MaterialLib fires the
event and locks the registry within its `preInit`, so dependent
mods can read the registry from their own `preInit` onward.
`ShapeConsumer`s can be registered to fire during either
MaterialLib's `init` or `postInit` phase.

The mod's items and blocks are registered as one ID per Shape,
with Materials having a global metadata index (same on every
Shape). [Postea](https://github.com/GTNewHorizons/Postea) is
used to automatically handle shifts in the metadata mapping
between mod versions or mod lists.

Where two mods register conflicting Materials/Shapes (by name),
MaterialLib handles the conflict by first choosing the canonical
one, which is by alphabetical mod ID, ascending. Once a
canonical mod is chosen for an instance, it is persistent as
long as that mod is used in that instance. MaterialLib unifies
non-canonical declarations onto the canonical. For Materials,
any non-conflicting Properties, Shape generation, Families, and
tooltips are merged in. For Shapes, just any non-conflicting
Properties. Canonical registrations win any Property conflicts,
and only the canonical Shape's oredict tags are registered
(differences are logged). Mods can register edits through the
API (`MaterialEdit`/`ShapeEdit`/`FamilyEdit`), which run after
the conflict resolution.

> [!NOTE]
>
> Shape unification has a special case for variants. Shapes that
> share a name must declare identical variants, or MaterialLib
> will throw an error on launch.

> [!TIP]
>
> Consider having your Shape names match their primary oredict.
> MaterialLib then handles the classic issue of many mods adding
> their own items like `ingotCopper` through unification.
>
> Properties intended for other mods to read/use should be
> declared in a publicly accessible way.

> [!CAUTION]
>
> One should never rely on their Materials or Shapes taking
> priority over other mods'. It is far better to handle crossmod
> behavior explicitly, through the edits or changing behavior
> after checking if the other mod is loaded.

## Textures
There are two supported texture systems. Both rely on
`TextureSet`s. A `TextureSet` defines the textures for Shapes,
with a base, layers that are recolored above it, and a final
untinted overlay texture.
- **Tinting:** Materials all carry a `TINT` Property, which
tints the base layer of their Shapes. Layers are tinted by a
`LAYER_TINTS` property. Layers without a matching tint are
untinted.
- **Palettes:** Palette PNGs can be provided for Materials,
which will then be used instead of tinting for items and blocks.
The palette must have two columns of pixels. The first column
is a key, and the second is an associated value
pixel. The palette is used to recolor Shape textures by
replacing all key pixels (by HSV value) with their value pixel,
maintaining the alpha of the original texture. If a key pixel
in the Shape texture is missing in the palette, the last entry
in the palette is used.

In either system, individual Shapes of a specific Material can
be given custom textures through textures at
`assets/materiallib/textures/<items|blocks>/mloverrides/<material>/<shape>.png`.
Names match by exact case. An override is not tinted or
palette-colored. Overrides can also replace layer and overlay
textures.

> [!NOTE]
>
> MaterialLib's textures rely on specific file paths.
> - TextureSet paths are
> `assets/<modid>/textures/<items|blocks>/materials/<set>/<shape>[_LAYER<n>|_OVERLAY].png`,
> where n is the nth layer, starting at 1. Fluids read from
> `blocks`.
> - Palettes are placed in
> `assets/<modid>/textures/palettes/<MaterialName>.png` -- the
> modid and the name's case must match the Material's.
> - Shape textures may be animated with standard `.mcmeta`
> files. The base, layer, and overlay textures of a Shape,
> whether from a texture set or an override, **_do not_** need
> to match each other's frames or speed.

## Examples
`ExampleContent` contains examples on using various API
features. The content can be viewed in-game by setting
`registerExamples` to `true` in MaterialLib's config.

## Dependencies
- [GTNHLib](https://github.com/GTNewHorizons/GTNHLib)
- [Postea](https://github.com/GTNewHorizons/Postea)
- [EndlessIDs](https://github.com/GTMEGA/EndlessIDs) and
[ChunkAPI](https://github.com/LegacyModdingMC/ChunkAPI) if using
block Shapes and more than 16 Materials.

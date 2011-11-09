/*
 * This is an example for a POVRay include file that can be used to configure the rendering
 * of .pov files generated with OSM2World (http://osm2world.org/).
 *
 * You can leave out any definition from this file, the rendering will then fall back
 * to (mostly very basic) default materials and shapes.
 * Please be aware that many possible definitions are not listed in this example file.
 * Examine the "material and object definitions" section of a .pov file generated
 * by OSM2World to see what definitions are used by that particular scene.
 */

#include "metals.inc"
#include "woods.inc"
#include "textures.inc"
#include "colors.inc"

// chooses the season - possible values: spring, summer, autumn, winter
// (has no effect on the generated scenes yet, could be used in custom model declarations)
#declare season="summer"

// chooses the time of day - possible values: day, night
// (has no effect on the generated scenes yet, could be used in custom model declarations)
#declare time="day"

// texture definitions

#declare texture_ASPHALT=texture { pigment { color rgb <0.2, 0.2, 0.2> } }
#declare texture_WATER=texture { T_Chrome_2D normal { waves 0.2 frequency 1000.0 scale 4.0 turbulence 0.1 rotate <0,-45,0> } }
#declare texture_WOOD=texture { T_Wood5 }
#declare texture_TERRAIN_DEFAULT=texture { pigment { color rgb <0.2, 0.73, 0.2> } finish { ambient 0.5 diffuse 0.5 } }

// sky sphere definition

// activating this creates a pure blue sky.
// This is useful when generating tiles. With patterned skies,
// there will often be visible tile borders in reflecting water.
//#declare sky_sphere_def = sky_sphere { pigment { rgb <0.0, 0.0, 1.0> } }


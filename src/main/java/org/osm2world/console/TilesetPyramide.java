package org.osm2world.console;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.locationtech.jts.util.Assert;
import org.osm2world.core.target.gltf.tiles_data.TilesetAsset;
import org.osm2world.core.target.gltf.tiles_data.TilesetEntry;
import org.osm2world.core.target.gltf.tiles_data.TilesetParentEntry;
import org.osm2world.core.target.gltf.tiles_data.TilesetRoot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TilesetPyramide {

    private static final class TilesetTreeBuilder {

        private record FileRegionTpl(File file, double[] region, int geomErr) {
        }

        private record TileZXY(int z, int x, int y) {
            @Override
            public final String toString() {
                return z + "_" + x + "_" + y;
            }
        }

        private static double tile2lon(int x, int z) {
            return x / Math.pow(2.0, z) * 360.0 - 180;
        }
    
        private static double tile2lat(int y, int z) {
            double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
            return Math.toDegrees(Math.atan(Math.sinh(n)));
        }

        public static int[] getTileNumber(final double lat, final double lon, final int zoom) {
            int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
            int ytile = (int) Math
                    .floor((1 - Math.log(Math.tan(Math.toRadians(lat)) + 1 / Math.cos(Math.toRadians(lat))) / Math.PI) / 2
                            * (1 << zoom));
            if (xtile < 0)
                xtile = 0;
            if (xtile >= (1 << zoom))
                xtile = ((1 << zoom) - 1);
            if (ytile < 0)
                ytile = 0;
            if (ytile >= (1 << zoom))
                ytile = ((1 << zoom) - 1);

            return new int[] {zoom, xtile, ytile};
        }

        public static double[] extendRegion(double[] a, double[] b) {
            
            double west = Math.min(a[0], b[0]);
            double south = Math.min(a[1], b[1]);
            
            double east = Math.max(a[2], b[2]);
            double north = Math.max(a[3], b[3]);

            double minh = Math.min(a[4], b[4]);
            double maxh = Math.max(a[5], b[5]);

            return new double[] {
                west, south, east, north, minh, maxh
            };
        }

        private File outDir;
        private List<File> tilesetFiles;
        private String regexPattern;
        private int srcLevel = 14;

        public TilesetTreeBuilder(File outDir, int srcLevel, List<File> tilesetFiles, String pathPattern) {
            this.outDir = outDir;
            this.srcLevel = srcLevel;
            this.tilesetFiles = tilesetFiles;
            this.regexPattern = pathPattern.replaceAll("\\{z\\}", "(?<z>[0-9]+)");
            this.regexPattern = this.regexPattern.replaceAll("\\{x\\}", "(?<x>[0-9]+)");
            this.regexPattern = this.regexPattern.replaceAll("\\{y\\}", "(?<y>[0-9]+)");
        }
        
        public void build() {
            System.out.println("Build tileset tree for " + this.tilesetFiles.size() + " tileset files");

            Pattern pattern = Pattern.compile(this.regexPattern, Pattern.CASE_INSENSITIVE);
            
            Map<TileZXY, List<File>> buckets16 = new HashMap<>();

            this.tilesetFiles.stream().forEach(f -> {
                Matcher m = pattern.matcher(f.toString());
                if (m.matches()) {
                    String zs = m.group("z");
                    String xs = m.group("x");
                    String ys = m.group("y");
                
                    if (xs != null && ys != null && zs != null) {
                        int x = Integer.valueOf(xs);
                        int y = Integer.valueOf(ys);
                        int z = Integer.valueOf(zs);

                        if (z == this.srcLevel) {
                            TileZXY key16 = getParentZXY(new TileZXY(z, x, y), 2);
    
                            Assert.equals(key16.z, z - 2, "Unexpected z value " + f.toString());
                            
                            if (buckets16.get(key16) == null) {
                                buckets16.put(key16, new ArrayList<>(16));
                            }
    
                            buckets16.get(key16).add(f);
                        }
                    }
                }
                
            });

            Gson gson = new GsonBuilder().create();
            
            Map<TileZXY, List<FileRegionTpl>> metaTilesetBuckets = new HashMap<>();

            buckets16.forEach((TileZXY bucketZXY, List<File> tiles) -> {

                try {
                    TilesetRoot parentTileSet = createEmbeddedChildrenTileset(tiles);

                    File parentTileJsonFile = new File(outDir, bucketZXY.toString() + ".tileset.json");
                    FileUtils.write(parentTileJsonFile, gson.toJson(parentTileSet));

                    TileZXY parentZXY = getParentZXY(bucketZXY, 3);
                    Assert.equals(bucketZXY.z - 3, parentZXY.z, "Unexpected z value for " + bucketZXY.toString());
                    
                    if (metaTilesetBuckets.get(parentZXY) == null) {
                        metaTilesetBuckets.put(parentZXY, new ArrayList<>(64));
                    }

                    metaTilesetBuckets.get(parentZXY).add(new FileRegionTpl(
                        parentTileJsonFile,
                        parentTileSet.getRoot().getBoundingVolume().getRegion(),
                        parentTileSet.getRoot().getGeometricError().intValue()));
                    
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            System.out.println(
                this.tilesetFiles.size() +
                " tilesets were embedded into " +
                buckets16.size() +
                " level 2 tilesets and written to " +
                outDir.toString() + "/" +
                buckets16.keySet().iterator().next().z + "_*_*.tileset.json");

            Map<TileZXY, List<FileRegionTpl>> subtreeTilesets = generateTreeLevel(metaTilesetBuckets);
            System.out.println(
                metaTilesetBuckets.size() +
                " level 3 tilesets written to " +
                outDir.toString() + "/" +
                metaTilesetBuckets.keySet().iterator().next().z + "_*_*.tileset.json");
            
            int total = subtreeTilesets.entrySet().stream().map(e -> e.getValue()).collect(Collectors.summingInt(List::size));

            while (total > Math.pow(4, 3)) {
                System.out.println("Generate tree over " + subtreeTilesets.size() + " subtree tilesets");
                subtreeTilesets = generateTreeLevel(subtreeTilesets);
            }

            List<FileRegionTpl> rootTiles = subtreeTilesets.values().stream().flatMap(List::stream).collect(Collectors.toList());
            TilesetRoot rootTileset = createParentTilesetForBucket(rootTiles, "REPLACE");

            File rootTilesetFile = new File(outDir, "root.tileset.json");
            try {
                FileUtils.write(rootTilesetFile, gson.toJson(rootTileset));
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            System.out.println("Root tileset file: " + rootTilesetFile.toString());
        }

        /**
         * For Files in list create parent tileset, conetents of
         * children tiles will be embedded into parent tileset
         */
        private TilesetRoot createEmbeddedChildrenTileset(List<File> childrenFiles) throws IOException {
            // TODO: Add option for deleting embedded files
            Gson gson = new GsonBuilder().create();

            TilesetRoot parentTileSet = new TilesetRoot();
            parentTileSet.setAsset(new TilesetAsset("1.0"));
            TilesetParentEntry parent = new TilesetParentEntry();

            parentTileSet.setRoot(parent);

            for (File f : childrenFiles) {
                String tileJson = FileUtils.readFileToString(f);
                TilesetRoot tileset = gson.fromJson(tileJson, TilesetRoot.class);

                TilesetParentEntry child = tileset.getRoot();
                parent.setGeometricError(child.getGeometricError().intValue() * 16);

                parent.addChild(child);

                if (parent.getBoundingVolume() == null) {
                    parent.setBoundingVolume(child.getBoundingVolume());
                }
                else {
                    double[] parentRegion = extendRegion(
                        parent.getBoundingVolume().getRegion(),
                        child.getBoundingVolume().getRegion());

                    parent.setBoundingVolume(parentRegion);
                }
            }

            return parentTileSet;
        }

        private static TileZXY getParentZXY(TileZXY zxy, int levels) {
            double lat = tile2lat(zxy.y, zxy.z);
            double lon = tile2lon(zxy.x, zxy.z);

            // 64 subtile
            int[] parentZXY = getTileNumber(lat, lon, Math.max(zxy.z - levels, 0));
            return new TileZXY(parentZXY[0], parentZXY[1], parentZXY[2]);
        }

        private Map<TileZXY, List<FileRegionTpl>> generateTreeLevel(Map<TileZXY, List<FileRegionTpl>> currentLayerBuckets) {
            Map<TileZXY, List<FileRegionTpl>> parentTiles = new HashMap<>();

            currentLayerBuckets.forEach((TileZXY bucketZXY, List<FileRegionTpl> tilesData) -> {

                TilesetRoot bucketTileSet = createParentTilesetForBucket(tilesData, "ADD");

                Gson gson = new GsonBuilder().create();

                String filename = bucketZXY.toString() + ".tileset.json";
                File bucketTilesetFile = new File(outDir, filename);
                try {
                    FileUtils.write(bucketTilesetFile, gson.toJson(bucketTileSet));
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }

                TileZXY parentBucketZXY = getParentZXY(bucketZXY, 3);

                if (parentTiles.get(parentBucketZXY) == null) {
                    parentTiles.put(parentBucketZXY, new ArrayList<>());
                }
                parentTiles.get(parentBucketZXY).add(new FileRegionTpl(
                    bucketTilesetFile,
                    bucketTileSet.getRoot().getBoundingVolume().getRegion(),
                    bucketTileSet.getRoot().getGeometricError().intValue())
                );
            });
            
            return parentTiles;
        }

        private TilesetRoot createParentTilesetForBucket(List<FileRegionTpl> tilesData, String refine) {
            TilesetRoot bucketTileSet = new TilesetRoot();
            bucketTileSet.setAsset(new TilesetAsset("1.0"));
            TilesetParentEntry parent = new TilesetParentEntry();
            parent.setRefine(refine);

            bucketTileSet.setRoot(parent);

            for (FileRegionTpl tileData: tilesData) {
                TilesetEntry child = new TilesetEntry();
                child.setGeometricError(tileData.geomErr);
                child.setBoundingVolume(tileData.region);
                child.setContent(tileData.file.getName());

                parent.setGeometricError(tileData.geomErr * 4);

                parent.addChild(child);

                if (parent.getBoundingVolume() == null) {
                    parent.setBoundingVolume(child.getBoundingVolume());
                }
                else {
                    double[] parentRegion = extendRegion(
                        parent.getBoundingVolume().getRegion(),
                        child.getBoundingVolume().getRegion());

                    parent.setBoundingVolume(parentRegion);
                }
            }

            return bucketTileSet;
        }
    }

    public static void main(String[] args) {
        String pathPattern = args[0];
        String[] pathEntries = pathPattern.split("[/\\\\]");

        File base = null;
        List<String> templates = new ArrayList<>();

        for (String p : pathEntries) {
            if (templates.isEmpty() && !isTemplated(p)) {
                base = base == null ? new File(p) : new File(base, p);
            }
            else {
                templates.add(p);
            }
        }

        List<File> tilesets = new ArrayList<>();
        
        List<File> files = new ArrayList<>();
        files.add(base);

        for (String template : templates) {
            String pattern = template.replace("{z}", "([0-9]+)");
            pattern = pattern.replace("{x}", "([0-9]+)");
            pattern = pattern.replace("{y}", "([0-9]+)");

            System.out.println("Checking for " + pattern);

            Pattern regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);

            List<File> dirs = new ArrayList<>();

            for (File parentf : files) {
                
                File[] matched = parentf.listFiles(new FileFilter() {
        
                    @Override
                    public boolean accept(File pathname) {
                        return regex.matcher(pathname.getName()).matches();
                    }
                    
                });

                Arrays.stream(matched)
                    .filter(f -> f.isDirectory()).forEach(dirs::add);

                Arrays.stream(matched)
                    .filter(f -> !f.isDirectory()).forEach(tilesets::add);
            }

            files = dirs;
        }

        TilesetTreeBuilder builder = new TilesetTreeBuilder(base, 14, tilesets, pathPattern);
        builder.build();
    }

    private static boolean isTemplated(String p) {
        return p.contains("{z}") || p.contains("{x}") || p.contains("{y}");
    }
    
}

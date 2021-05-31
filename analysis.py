#blender script
import bpy
import csv
import os, sys

filepath = bpy.data.filepath
directory = os.path.dirname(filepath)
reader = csv.reader(open(os.path.join(directory,"roadpoints.csv")),quoting=csv.QUOTE_NONNUMERIC)
count=0
# make mesh
vertices = []
edges = []
faces = []
for row in reader:
	z=5
	if count%3==0:
		z=10;
	if count%3==2:
		faces.append([count-2,count-1,count])
	print(len(row))
	print(count)
	vertices.append((row[0],row[2],row[1]))
	count=count+1

new_mesh = bpy.data.meshes.new('new_mesh')
new_mesh.from_pydata(vertices, edges, faces)
new_mesh.update()
# make object from mesh
new_object = bpy.data.objects.new('new_object', new_mesh)
# make collection
new_collection = bpy.data.collections.new('new_collection')
bpy.context.scene.collection.children.link(new_collection)
# add object to scene collection
new_collection.objects.link(new_object)
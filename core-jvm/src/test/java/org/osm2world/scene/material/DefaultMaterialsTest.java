package org.osm2world.scene.material;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class DefaultMaterialsTest {

	/**
	 * Checks that {@link DefaultMaterials#getDefaultMaterials()} actually returns all fields.
	 * The test is implemented using reflection.
	 * The tested method itself cannot be implemented using reflection because it needs to be compatible with TeaVM.
	 */
	@Test
	public void testGetDefaultMaterials() throws IllegalAccessException {

		Set<MaterialRef> methodResult = new HashSet<>(DefaultMaterials.getDefaultMaterials());

		Set<MaterialRef> reflectionResult = new HashSet<>();
		for (Field field : DefaultMaterials.class.getFields()) {
			if (field.getType().equals(MaterialRef.class)) {
				reflectionResult.add((MaterialRef) field.get(null));
			}
		}

		assertEquals(reflectionResult, methodResult);

	}

}

